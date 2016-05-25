package com.lightspeedsystems.mdm;

import org.json.JSONObject;
import android.content.Context;

import com.google.android.gcm.GCMRegistrar;
import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Handles prerequisite requirements to running MDM, including required software, accounts,
 *  settings, and device capabilities.
 *
 */
public class Prerequisites {
	private final static String TAG = "Prerequisites";
	
	private final static String gsfAppName = "GoogleServicesFramework";
	private final static String gsfPackageName = "com.google.android.gsf";
	
	private static int prereqstate_gsf = 0;
	
	public final static int PREREQSTATE_prereqsok = -1;
	public final static int PREREQSTATE_gsfmissing    	= 1;
	public final static int PREREQSTATE_gsfinstalling 	= 2;
	public final static int PREREQSTATE_gsfinstall_ok 	= 3;
	public final static int PREREQSTATE_gsfinstall_completed = 4;
	public final static int PREREQSTATE_gsfinstall_failed = 5;
	
	
	/**
	private Controller controller;
	
	public Prerequisites(Controller controller) {
		this.controller = controller;
	}
	**/
	
	/**
	 * Checks if Google Services Framework is present or needing to be installed.
	 * @return true of GSF is installed/present, false if needing to be installed or had an error.
	 */
	public static boolean isGsfInstalled() {
		if (prereqstate_gsf == 0)
			checkGcmPrerequisites(Utils.getApplicationContext());
		return (prereqstate_gsf == PREREQSTATE_gsfinstall_ok);
	}
	
	/**
	 * Gets the Google Services Framework installation state, one of the PREREQSTATE_gsf... values.
	 */
	public static int getGsfState() {
		return prereqstate_gsf;
	}
	
	/**
	 * Clears/resets the GSF install state. Subsequent calls to @checkGcmPrerequisites will re-check the state.
	 */
	public static void clearGsfInstallState() {
		prereqstate_gsf = 0;
	}
	
	/**
	 * Sets the Google Services Framework installation state. This is non-persisted, and is
	 * normally set when this app starts up as needed. The state can be changed from various processing.
	 * @param state new state value, a PREREQSTATE_gsf... value.
	 */
	public static void setGsfState(int state) {
		prereqstate_gsf = state;
	}

	/** 
	 * Checks if gcm prerequisites are satisfied, including device installed software
	 * for Google Services Framework, and proper application manifest settings.
	 * @param context application context
	 * @return 0 if ok, or the resource ID of an error message.
	 */
	public static int checkGcmPrerequisites(Context context) {
		int rc=0;
		// Make sure the device has the proper dependencies and things are set up for GCM:
		try {
			GCMRegistrar.checkDevice(context);    // throws UnsupportedOperationException
        	GCMRegistrar.checkManifest(context);  // throws IllegalStateException
        	// now check for proper permissions. This is needed for handling special cases, mainly in Kindle:
    		// - uses-permission:'com.google.android.c2dm.permission.RECEIVE'
    		if (context.checkCallingOrSelfPermission("com.google.android.c2dm.permission.RECEIVE") != 0) {
    			rc=Constants.GCM_ERROR_permissions;
    			LSLogger.debug(TAG, "Permission for c2dm Receive is denied.");
    		}
        	
		} catch (UnsupportedOperationException iex) {
			// GSF not installed
			rc=Constants.GCM_ERROR_no_gsf;
			// set gsf state to missing if we havent already set it to something else.
			if (prereqstate_gsf == 0)
				prereqstate_gsf = PREREQSTATE_gsfmissing;
        } catch (IllegalStateException iex) {
        	// app manifest error
        	rc=Constants.GCM_ERROR_manifest;
        }
		
		return rc;
	}
	
	
	/**
	 * Initiates the installation of Google Services Framework (GSF), required for GCM.
	 * Some custom Android OS's do not include GSF as part of the system. For MobileManager
	 * to work, GSF needs to be present. This code initiates the installation of GSF by
	 * downloading it from a specific location and then installing it.
	 * @param controller
	 * @return true if the install was initialized, false if was an error.
	 */
	public static boolean installGSF(Controller controller, CommandResult results) {
		boolean bsuccess = false;		
		prereqstate_gsf = PREREQSTATE_gsfinstalling;
		App appInstall = null;
		String cmd = Constants.CMD_INSTALLAPP;
		JSONObject json = new JSONObject();
		
		try {			
			/**
			// --query server url from mdm server as a managed app:
			String serverUrl = ServerUrlProvider.getLSAppQueryUrl(controller.getSettingsInstance(), gsfAppName);
			ServerComm serverComm = new ServerComm();
			HttpCommResponse response = 
					serverComm.getFromServer(serverUrl, null);
			if (response != null) {
				if (response.isOK()) {
					// got a successful response from server
					LSLogger.debug(TAG, "LSApp Query Result from server: " + response.getResultStr());
					JSONObject jresp = new JSONObject(response.getResultStr());
					if (jresp != null && jresp.length() > 0) {
						// build the json needed to define the app to be installed:
						json.put(Constants.CMD_cmdtag, Constants.CMD_INSTALLAPP);
						json.put(Constants.CMD_nametag, gsfPackageName); // we use the package name as the name
						// extract the file url from the response:
						json.put(Constants.CMD_fileurltag, jresp.get(Constants.PARAM_APP_URL));
												
						appInstall = Apps.getAppInstanceFromInstallParams(json);
						if (appInstall != null) {
							appInstall.setMdmAppType(App.MDMAPPTYPE_perreq_gsf);
							// enqueue the command to install the app:
							bsuccess = controller.enqueueManagedAppAction(cmd, appInstall, null, results, false);
							LSLogger.debug(TAG, "GSF install request created: result="+results.toString());
						} else {
							LSLogger.error(TAG, "GSF install: unable to parse app info from params.");
						}
					} 
				} else {
					// update check failed; response has error info:
					LSLogger.error(TAG, "GSF app query failed: " + response.getResultReason());
				}
			}
			**/
			
			// -- use hard-coded url to download gsf from:
			// (used in V3. mz)
			json.put(Constants.CMD_cmdtag, Constants.CMD_INSTALLAPP);
			json.put(Constants.CMD_nametag, gsfPackageName); // we use the package name as the name
			// use static file url from constant value:
			json.put(Constants.CMD_fileurltag, Constants.LSMDM_GSF_DOWNLOAD_URL);									
			appInstall = Apps.getAppInstanceFromInstallParams(json);
			if (appInstall != null) {
				appInstall.setMdmAppType(App.MDMAPPTYPE_perreq_gsf);
				// enqueue the command to install the app:
				bsuccess = controller.enqueueManagedAppAction(cmd, appInstall, null, results, false);
				LSLogger.debug(TAG, "GSF install request created: result="+results.toString());
			} else {
				LSLogger.error(TAG, "GSF install: unable to parse app info from params.");
			}

			
		} catch (Exception ex) {
			LSLogger.exception(TAG, "InstallGSF error:", ex);
		}
		if (!bsuccess)
			prereqstate_gsf = PREREQSTATE_gsfinstall_failed;
		return bsuccess;
	}


}
