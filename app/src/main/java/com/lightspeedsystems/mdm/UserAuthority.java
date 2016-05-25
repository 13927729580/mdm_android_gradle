package com.lightspeedsystems.mdm;


// **** TO DISABLE LOGGING IN, COMMENT OUT THE LINE BELOW "debug/bypass:" in constructor ****


import org.json.JSONObject;

import com.lightspeedsystems.mdm.profile.AccountUtils;
import com.lightspeedsystems.mdm.util.LSLogger;
import android.content.Context;

/**
 * Defines a user and their privileges, along with authentication support. 
 *
 */
public class UserAuthority {
	private static String TAG = "UserAuthority";
	private Context context;
	private Settings settings;
	private String userID;	// current/last userID
	private String userPassword; // current login password
	private String userName; // current user on the device.
	private OAuthValues oauthValues;
	
	private boolean bLoggedOn;
	private String lastLoginError;
	private int lastLoginErrorCode;
	
	private final static String OAUTH_PARAM_clientid = "client_id";
	private final static String OAUTH_PARAM_clientsecret = "client_secret";
	private final static String OAUTH_PARAM_userid = "username";
	private final static String OAUTH_PARAM_password = "password";
	//private final static String OAUTH_PARAM_token = "token";
	private final static String OAUTH_PARAM_granttype = "grant_type";
	private final static String OAUTH_PARAM_granttypepwd = "password";
	private final static String OAUTH_PARAM_redirecturi = "redirect_uri";
	
	private final static String OAUTH_VALUE_accesstoken  = "access_token";
	private final static String OAUTH_VALUE_refreshtoken = "refresh_token";
	private final static String OAUTH_VALUE_tokentype = "token_type";
	private final static String OAUTH_VALUE_expires = "expires_in";
	private final static String OAUTH_VALUE_scope = "scope";
	
	/**
	 * Creates instance; this does not define a specific user. Use loginUser to assign the user.
	 * @param context
	 */
	public UserAuthority(Context context) {		
		this.context = context;
		bLoggedOn = false;
		oauthValues = new OAuthValues();
		// retrieve last-used user ID
		settings = Settings.getInstance(context);
		// retrieve previous userID:  (note: see loginuser() below to enable code to save the id as needed)
		// - for now, comment these 3 lines out so they do not retrieve a possible previous id -
		///if (settings != null) {
		///	userID = settings.getSetting(Settings.USER_ID);
		///}
// debug/bypass:  -- comment this line out to not require login
//bLoggedOn = true;
	}
	
	/**
	 * Sets the user's credentials that will be used to log in with.
	 * @param username
	 * @param password
	 */
	public void setUserCredentials(String username, String password) {
			userID = username;
			userPassword = password;	
	}
	
	/**
	 * Initiates login process.
	 * @return state of the login results, true if logged in, false if not.
	 */
	public boolean loginUser() {
		// First, reset any state data:
		lastLoginError = null;
		lastLoginErrorCode = 0;
		// Perform authentication:		
		if (userID != null && userPassword != null) {
			authenticateMBCAdmin(userID, userPassword);
			//v1: authenticateOAuthAdmin(userID, userPassword);
			if (bLoggedOn) {						
				// save login state: (keeps current user ID to make it easier to log in next time)
				// note: do not do this. We could use a setting to 'remember me' to keep and re-use
				//  the userid and pre-populate it; but, an admin would not usually want their login
				//  to be left showing on a device's login screen. 
				//  As such, never save it (the next 2 lines save it upon successful login).
				///if (settings != null) 
				///	settings.setSetting(Settings.USER_ID, userID);
			}
		} 
		return bLoggedOn;		
	}
	
	/**
	 * Authenticates (logs-in) the given user and password credentials.
	 * @param username user ID to log in.
	 * @param password password for the user
	 * @param reason if login failed, holds the buffer with an error message that can be shown to the user.
	 * @return true if the user was logged in, false if the login failed. use @getLoginFailure 
	 * to get the last failure reason.
	 */
	public boolean loginUser(String username, String password, StringBuffer reason) {
		if (username != null)
			userID = username;
		if (password != null)
			userPassword = password;
		
		// Perform authentication:		
		loginUser();
		if (lastLoginError != null && reason != null)
			reason.append(lastLoginError);
		return bLoggedOn;
	}
	
	/**
	 * Logs off the current user.
	 */
	public void logoffUser() {
		//userID = null;
		bLoggedOn = false;
		oauthValues.clear();
	}
	
	/**
	 * Gets the ID of the current logged-in MDM user.
	 * @return user's ID name, or null if the user is not logged in.
	 */
	public String getUserID() {
		return userID;
	}
	
	/**
	 * Returns true if the user is logged on, false if not.
	 * Also logs off user if the authentication token has expired.
	 * @return
	 */
	public boolean isLoggedOn() {
		
		return bLoggedOn;
	}
	
	/**
	 * Gets the reason for the last login failure, or null if the login was successful.
	 * @return
	 */
	public String getLastLoginError() {
		return lastLoginError;
	}
	
	/**
	 * Gets the last login error code, if present. A 0 means no error was detected.
	 * @return
	 */
	public int getLastLoginErrorCode() {
		return lastLoginErrorCode;
	}
	
	/**
	 * Gets the user's name as set in the device. This is from Contact info.
	 * Note that this should only be used in ice cream sandwich and later versions.
	 * @return user's name, or null if could not get it.
	 */
	public String getUserName() {
		if (userName == null) {
			userName = AccountUtils.getUserNameFromContacts(context);
		}
		return userName;		
	}
	
	
	/**
	 * Authenticates the admin user's credentials to a my big campus login server.
	 * @return
	 */
	private void authenticateMBCAdmin(String userid, String pwd) {
		HttpCommResponse resp = null;
		try {
			// build the params for the request:
			JSONObject params = new JSONObject();
			params.put(OAUTH_PARAM_userid, userid);
			params.put(OAUTH_PARAM_password, pwd);
						
			String serverUrl = ServerUrlProvider.getMBCAuthenticationUrl(settings);
			ServerComm serverComm = new ServerComm();
			serverComm.setIncludeHeaderAuth(true);
			serverComm.setAuthTokenKey(settings.getAuthMBCToken());
			
			// attempt to authenticate:
			resp = serverComm.postToServer(serverUrl, params);
			
		} catch (Exception ex) {
			lastLoginError = ex.getLocalizedMessage();
			if (lastLoginError == null)
				lastLoginError = ex.toString();
			LSLogger.exception(TAG, "MBC-Auth login error:", ex);
		}
		
		if (resp != null) {
			lastLoginErrorCode = resp.getResultCode();
			if (resp.isOK()) {
				lastLoginErrorCode = 0;
				bLoggedOn = oauthValues.parseAuthenticationValues(resp.getResultStr());
				if (!bLoggedOn)
					lastLoginError = context.getResources().getString(R.string.logindenied);
			} else if (resp.getResultCode() == 401) { // authentication failed
				lastLoginError = context.getResources().getString(R.string.logindenied);
			} else { // was some other error.
				lastLoginError = resp.getResultReason();
				if (lastLoginError == null)
					lastLoginError = resp.getResultStr();
				lastLoginError = context.getResources().getString(R.string.loginerror) + lastLoginError;
			}
		} else { // we had an error, but, no exception.
			lastLoginError = context.getResources().getString(R.string.loginerror);
		}
	}
	
	
	
	
	/**
	 * Authenticates the user credentials to a oauth login server.
	 * @return
	 */
	private void authenticateOAuthAdmin(String userid, String pwd) {
		HttpCommResponse resp = null;
		try {
			// build the params for the request:
			JSONObject params = new JSONObject();
			params.put(OAUTH_PARAM_userid, userid);
			params.put(OAUTH_PARAM_password, pwd);
			params.put(OAUTH_PARAM_granttype, OAUTH_PARAM_granttypepwd);			
			params.put(OAUTH_PARAM_clientid, settings.getOAuthAppID());
			params.put(OAUTH_PARAM_clientsecret, settings.getOAuthSecret());
			params.put(OAUTH_PARAM_redirecturi, settings.getOAuthRedirectUrl());
			
			String serverUrl = ServerUrlProvider.getOAuthenticationUrl(settings);
			ServerComm serverComm = new ServerComm();
			
			// attempt to authenticate:
			resp = serverComm.postToServer(serverUrl, params);
			
		} catch (Exception ex) {
			lastLoginError = ex.getLocalizedMessage();
			if (lastLoginError == null)
				lastLoginError = ex.toString();
			LSLogger.exception(TAG, "OAuth login error:", ex);
		}
		
		if (resp != null) {
			lastLoginErrorCode = resp.getResultCode();
			if (resp.isOK()) {
				lastLoginErrorCode = 0;
				bLoggedOn = oauthValues.parseAuthenticationValues(resp.getResultStr());
				if (!bLoggedOn)
					lastLoginError = context.getResources().getString(R.string.logindenied);
			} else if (resp.getResultCode() == 401) { // authentication failed
				lastLoginError = context.getResources().getString(R.string.logindenied);
			} else { // was some other error.
				lastLoginError = resp.getResultReason();
				if (lastLoginError == null)
					lastLoginError = resp.getResultStr();
				lastLoginError = context.getResources().getString(R.string.loginerror) + lastLoginError;
			}
		} else { // we had an error, but, no exception.
			lastLoginError = context.getResources().getString(R.string.loginerror);
		}
	}
	
	
	/**
	 * Internal class for holding OAuth tokens and various values.
	 */
	@SuppressWarnings("unused")
	private class OAuthValues {
		private final static String TAG = "UserAuthority.OAuthValues";
		String access_token;
		String refresh_token;
		String token_type;
		String scope;
		private long expires_in;
		private long expire_start; // time when the expiration timer starts.
		long expire_time; // time when the token expires
		
		public OAuthValues() {
		}
		
		/** Returns true if the authentication credentials have expired. */
		public boolean isExpired() {
			return (System.currentTimeMillis() >= expire_time);
		}
		
		// clears any previous values.
		public void clear() {
			access_token = null;
		    refresh_token = null;
			token_type = null;
			scope = null;
			expires_in = 0;
			expire_time = 0;
		}
		
		//parses out various security values from the given string, which is expected to be in json.
		//  the response data, if successful, will contain some values of interest,like:
		// "{\"access_token\":\"5eb355b1b3d1fb76e0bb765f784145845c6074420268e46dc704d2ebf53ce52c\",
		//  \"token_type\":\"bearer\",\"expires_in\":null,\"refresh_token\":null,\"scope\":\"\"}"
		// If the response is not a json response with an access_token, we bail out; this can happen
		//  when the response is the generic login page requesting a login, with a 200 ok response, but
		//  that is not a valid authentication.
		public boolean parseAuthenticationValues(String values) {
			clear();
			try {
				LSLogger.debug(TAG, values);								
				JSONObject json = new JSONObject(values);
				if (json.has(Constants.CMD_response))
					json = json.getJSONObject(Constants.CMD_response);
				if (json.has(OAUTH_VALUE_accesstoken))
					access_token = json.getString(OAUTH_VALUE_accesstoken);
				if (json.has(OAUTH_VALUE_refreshtoken))
					refresh_token = json.getString(OAUTH_VALUE_refreshtoken);
				if (json.has(OAUTH_VALUE_tokentype))
					token_type = json.getString(OAUTH_VALUE_tokentype);
				if (json.has(OAUTH_VALUE_scope))
					scope = json.getString(OAUTH_VALUE_scope);
				if (!json.isNull(OAUTH_VALUE_expires)) { // can be null value or not have it, so check for it. 
					expire_start = System.currentTimeMillis();
					expires_in = json.getLong(OAUTH_VALUE_expires); // value is in minutes
					expire_time = expire_start + expires_in;
				}
				
			} catch (Exception ex) {
				LSLogger.exception(TAG, "parseAuth error", ex);
			}
			// return true if we have a access_token, false otherwise.
			return (access_token != null && !access_token.isEmpty());
		}
	}

}
