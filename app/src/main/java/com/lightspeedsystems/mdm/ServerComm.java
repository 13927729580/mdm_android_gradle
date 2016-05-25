/**
 * Provides MDM-specific communications with the MDM server, handling sending and receiving data.
 * Uses the MDM HttpComm base class for http support, and wraps that with additional MDM-specifics.
 */
package com.lightspeedsystems.mdm;

import org.json.JSONObject;

import android.content.Context;

import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Server communications wrapper class.
 */
public class ServerComm extends HttpComm {
	private static String TAG = "ServerComm";
	private Context context;
	
	private static int wifiConnectWaittimeSecs = 60;
	
	/**
	 *  
	 */
	public ServerComm() {
		super();
		context = Utils.getApplicationContext();
	}

	public ServerComm(Context ctx) {
		super();
		context = ctx;
	}
	
//	public void postToServer(String serverUrl, String params) {
//		sendPost(serverUrl, params);
//	}
	
	public HttpCommResponse postToServer(String serverUrl, JSONObject jparams) {
		HttpCommResponse resp = new HttpCommResponse();
		sendPost(serverUrl, jparams, resp);
		if (resolveErrors(resp)) {
			LSLogger.debug(TAG, "Post error occurred but was resolved; retrying...");
			sendPost(serverUrl, jparams, resp);
		}
		return resp;
	}
	
	public HttpCommResponse getFromServer(String serverUrl, JSONObject jparams) {
		HttpCommResponse resp = new HttpCommResponse();
		return getFromServer(serverUrl, jparams, resp);
	}

	public HttpCommResponse getFromServer(String serverUrl, JSONObject jparams, HttpCommResponse resp) {
		sendGet(serverUrl, jparams, resp);
		if (resolveErrors(resp)) {
			LSLogger.debug(TAG, "Get error occurred but was resolved; retrying...");
			sendGet(serverUrl, jparams, resp);
		}
		return resp;
	}

	/*
	 * INternal method for attempting to resolve communications issues.
	 * @return true if the problem may have been resolved and the command should be
	 * tried again (and the response is cleared), false if unable to resolve the problem.
	 */
	private boolean resolveErrors(HttpCommResponse resp) {
		boolean bresolved = false;
		
		if (resp != null && !resp.isOK()) {
			// handle connection errors due to wifi turned off...
			LSLogger.debug(TAG, "Resolving http error: exceptiontype=" +resp.getExceptionType()+" resultcode="+ resp.getResultCode());			

			if (resp.getExceptionType() == HttpCommResponse.EXCEPTIONTYPE_connectFailed ||
				resp.getExceptionType() == HttpCommResponse.EXCEPTIONTYPE_unknownHost) {
				// possible connection error: make sure wifi is connected:
				if (!WifiUtils.wifiIsConnected(context)) {
					bresolved = WifiUtils.ensureConnected(context, wifiConnectWaittimeSecs, false);
				}
			}
			
			if (bresolved)
				resp.clear();
		}
		return bresolved;
	}

}
