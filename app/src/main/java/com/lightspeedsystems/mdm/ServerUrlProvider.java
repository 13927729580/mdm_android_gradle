package com.lightspeedsystems.mdm;


/**
 * Provides specific URLs to the MDM server, keeping all the url definitions in one place: here.
 */
public class ServerUrlProvider {

	/**
	 * Gets the URL where a device enrolls itself to the MDM server. Reads enrollment url 
	 * and enrollment code values from settings.
	 * @param settings Local settings instance to build url from.
	 * @return <server>/<enrollment_code>.json
	 */
	public static String getEnrollToServerUrl(Settings settings) {
		String serverUrl = settings.getEnrollmentServer();
		String code = settings.getEnrollmentCode();
		String url = Utils.endStringWith(serverUrl, "/")
				+ code + ".json"; 
	    return url;
	}

	/**
	 * Gets the URL where a device checks-in and/or registers itself to the MDM server.
	 * @param settings Local settings instance to build url from.
	 * @return <server>/organizations/<orgID>/devices/checkin_android
	 */
	public static String getCheckinUrl(Settings settings) {
		String serverUrl = settings.getServerUrl();
		String orgID = settings.getOrganizationID();
		String url = Utils.endStringWith(serverUrl, "/")
				+ "organizations/" + orgID
				+ "/devices/checkin_android"; 
	    return url;
	}
	
	/**
	 * Gets the URL to get "next" commands from the MDM server.
	 * @param settings Local settings instance to build url from.
	 * @return <server>command_dispatch/android
	 */
	public static String getCommandsQueryUrl(Settings settings) {
		String serverUrl = settings.getServerUrl();
		String url = Utils.endStringWith(serverUrl, "/")
				+ "command_dispatch/android"; 
	    return url;
	}
	
	/**
	 * Gets the URL to use t post apps data sent to the MDM server.
	 * @param settings Local settings instance to build url from.
	 * @return <server>command_dispatch/android
	 */
	public static String getAppsDataCheckinUrl(Settings settings) {
		String serverUrl = settings.getServerUrl();
		String url = Utils.endStringWith(serverUrl, "/")
				+ "command_dispatch/android"; 
	    return url;
	}
	
	
	/**
	 * Gets the URL to get check for update of this app from the MDM utilities server.
	 * @param settings Local settings instance to build url from.
	 * @return <server>/3/ls_apps/android_mdm/app_version
	 */
	public static String getCheckUpdateUrl(Settings settings) {
		String serverUrl = settings.getUtilsServerUrl(); //  getServerUrl();
		//String orgID = settings.getOrganizationID();
		String url = Utils.endStringWith(serverUrl, "/")
				+ "3/ls_apps/android_mdm/app_version";		 
		return url;
	}

	/**
	 * Gets the URL to get app download and version info from the MDM server.
	 * @param settings Local settings instance to build url from.
	 * @return <server>/ls_app/<appname>/version
	 */
	public static String getLSAppQueryUrl(Settings settings, String appName) {
		String serverUrl = settings.getServerUrl();
		String url = Utils.endStringWith(serverUrl, "/")
				+ "ls_app/" + appName + "/version"; 
	    return url;
	}
	
	
	
	/**
	 * Gets the URL to authenticate with a MBC server.
	 * @param settings Local settings instance to build url from.
	 * @return  <auth_server>/3/sessions/admin_only
	 */
	public static String getMBCAuthenticationUrl(Settings settings) {
		String serverUrl = settings.getOAuthServerUrl();
		if (serverUrl == null)
			serverUrl =	settings.getServerUrl();
		//String orgID = settings.getOrganizationID();
		String url = Utils.endStringWith(serverUrl, "/")
				+ "3/sessions/admin_only"; 
	    return url;
	}	
	
	/**
	 * Gets the URL to authenticate with a MDM server.
	 * @param settings Local settings instance to build url from.
	 * @return <auth_server>/oauth/token
	 */
	public static String getOAuthenticationUrl(Settings settings) {
		String serverUrl = settings.getOAuthServerUrl();
		if (serverUrl == null)
			serverUrl =	settings.getServerUrl();
		//String orgID = settings.getOrganizationID();
		String url = Utils.endStringWith(serverUrl, "/")
		 		+ "oauth/token"; 
	    return url;
	}
}
