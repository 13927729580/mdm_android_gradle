package com.lightspeedsystems.mdm;

/**
 * Defines various constants used in the application
 */
public class Constants {
	static final int APPLICATION_VERSION_major = 1;
	static final int APPLICATION_VERSION_minor = 2;
	static final int APPLICATION_VERSION_build = 2016;  // aug 15, 2014
	
	public static final String APPLICATION_VERSION_STR = "01.02.2016b9";

	public static final String PACKAGE_NAME = "com.lightspeedsystems.mdm";
	
	public static final String DEVICE_TYPE_ANDROID = "android";
	
	// static location from where to download GoogleServicesFramework: 
	protected static final String LSMDM_GSF_DOWNLOAD_URL = "http://lsmdm-production.s3.amazonaws.com/public/GoogleServicesFramework.apk";
	
	// *** NOTE: make sure the config.xml is not overriding these:
	public static final String DEFAULT_MDMSERVER_URL  = "https://devices.lsmdm.com";  // url and port of default server
	public static final String DEFAULT_MDMSERVER_PORT = "";
	
	public static final String DEFAULT_MDMUTILS_SERVER_URL  = "https://api.mybigcampus.com";  // url and port of default utilities server
	public static final String DEFAULT_MDMAUTH_SERVER_URL   = "https://api.mybigcampus.com";  // url and port of default oauth server
	public static final String DEFAULT_MDMAUTH_REDIRECT_URL = "urn:ietf:wg:oauth:2.0:oob";
	public static final String DEFAULT_MDMAUTH_APPID = "98705279f7733342a2cd590016b7acaa293632420ed1a1435f6d966a230bd018";
	public static final String DEFAULT_MDMAUTH_APPKEY= "ee5270c420c160a027358e889a5ffb57d01c8569e9b361024bee34fd90d763eb";
	public static final String DEFAULT_MDMAUTH_MBCTOKEN = "7f37894f336f494dabcfe5d4766b6321";
	
	public static final String ACTION_APPINSTALL = "com.lightspeedsystems.mdm.INSTALLAPP";
	public static final String ACTION_APPUNINSTALL = "com.lightspeedsystems.mdm.UNINSTALLAPP";
	public static final String ACTION_INSTALL_SOURCE = "com.lightspeedsystems.mdm.INSTALLSOURCE";
	public final static int ACTIONID_INSTALL = 1;
	public final static int ACTIONID_UNINSTALL = 2;
	public final static int ACTIONID_ERROR = 99;

	
    /** Google API project id registered to use GCM.  */
	public static final String GCM_SENDER_ID = "904579169767";
	/** GCM prerequisite check errors and callback errors. */
	public static final int GCM_ERROR_no_gsf   = R.string.gcmerror_nogsf;
	public static final int GCM_ERROR_manifest = R.string.gcmerror_manifest;
	public static final int GCM_ERROR_permissions = R.string.gcmerror_permissions;
	public static final int GCM_ERROR_authentication = R.string.gcmerror_authfailed;
	public static final int GCM_ERROR_acctmissing = R.string.gcmerror_noaccount;
	public static final int GCM_ERROR_servicenotavailable = R.string.gcmerror_service_not_available;
	public static final String GCM_ERRORSTR_authentication = "AUTHENTICATION_FAILED";
	public static final String GCM_ERRORSTR_acctmissing = "ACCOUNT_MISSING";
	public static final String GCM_ERRORSTR_servicenotavailable = "SERVICE_NOT_AVAILABLE";
	
	/** Name of specific download directory, used for apps and other file downloads. */
	public static final String DOWNLOADS_DIR = "downloads";
    
    /* Command constants: */
    public final static String CMD_cmdtag     = "cmd";
    public final static String CMD_response   = "response";
    //public final static String CMD_cmdidtag   = "cmdid";
    public final static String CMD_activities  = "activities";
    public final static String CMD_activitytag = "activity_id";
    public final static String CMD_commandtag  = "command_id";
    public final static String CMD_successtag  = "success";
    public final static String CMD_errortag    = "error";
    public final static String CMD_datatag     = "data";
    public final static String CMD_nametag	   = "name";
    public final static String CMD_storeurltag = "storeurl";
    public final static String CMD_fileurltag  = "fileurl";
    public final static String CMD_filelocaltag= "filelocal";
    public final static String CMD_filenametag = "filename";
    public final static String CMD_successvalue= "ok";
    public final static String CMD_pendingvalue= "pending";
    public final static String CMD_failedvalue = "failed";
    public final static String CMD_value  	   = "value";
    public final static String CMD_value_true  = "true";
    public final static String CMD_value_false = "false";
   
    public final static String CMD_WAKEUP      = "wakeup"; // wakeup command
    public final static String CMD_CHECKIN     = "checkin";
    public final static String CMD_UNENROLL    = "unenroll";
    public final static String CMD_SETCONFIG   = "setconfig";
    public final static String CMD_SETPROFILE  = "setprofile";
    public final static String CMD_LOCKNOW     = "lock";
    public final static String CMD_WIPECLEAN   = "wipeclean";  // full wipe-clean
    public final static String CMD_WIPEMANAGED = "wipemanaged";// wipe the managed things (apps, passcodes, etc.)
    public final static String CMD_RESETPASSWORD = "passcodereset";
    public final static String CMD_CLEARPASSWORD = "passcodeclear";
    public final static String CMD_EXPIREPASSWORDNOW= "passcodeexpirenow"; 
    public final static String CMD_EXPIREPASSWORDAFTER= "passcodeexpireafter"; // expire in 'value' minutes, or 0 to disable
    public final static String CMD_INSTALLAPP    = "appinstall";
    public final static String CMD_UNINSTALLAPP  = "appremove";
    public final static String CMD_GETAPPS  	 = "applist";
    public final static String CMD_GETAPPS_ALL   = "all";
    public final static String CMD_UPDATE        = "mdmupdate";  // update this app

    public final static String CMD_profiletag     = "profile";
    public final static String CMD_configtag      = "config";
    public final static String CMD_passwordtag    = "pc";

    public final static String PARAM_ACTIVITY_TYPE	  = "activity_type";  
    public final static String PARAM_TYPE_DEVICEAPPINFO="DeviceAppInformation";
	/* Communications parameter value names: */    
	public final static String PARAM_DEVICE_UDID	  = "udid";
	public final static String PARAM_DEVICE_TYPE	  = "device_type";
	public final static String PARAM_DEVICE_SERIALNUM = "serial_number";
	public final static String PARAM_DEVICE_OSVERSION = "os_version";
	public final static String PARAM_DEVICE_OSBUILD   = "build_version";
	public final static String PARAM_DEVICE_NAME	  = "device_name";
	public final static String PARAM_DEVICE_MODELNUM	= "model_number";
	public final static String PARAM_DEVICE_MODELNAME	= "model_name";
	public final static String PARAM_DEVICE_PRODUCTNAME	= "product_name";
	public final static String PARAM_DEVICE_WIFI_MAC	= "wifi_mac";
	
	public static final String PARAM_APP_URL		    = "app_url";
	public final static String PARAM_DEVICEAPPS_APPS    = "apps";
	public final static String PARAM_DEVICEAPPS_APPS_ALL= "manifest";
	public final static String PARAM_DEVICEAPP_STATUS   = "status";
	public final static String PARAM_DEVICEAPP_STATUS_add    = "add";
	public final static String PARAM_DEVICEAPP_STATUS_update = "update";
	public final static String PARAM_DEVICEAPP_STATUS_remove = "remove";
	public final static String PARAM_DEVICEAPP_UDID	    = "device_udid";
	public final static String PARAM_DEVICEAPP_PKGNAME	= "identifier";
	public final static String PARAM_DEVICEAPP_APPNAME	= "name";
	public final static String PARAM_DEVICEAPP_FULLVER	= "version";
	public final static String PARAM_DEVICEAPP_SHORTVER	= "short_version";
	public final static String PARAM_DEVICEAPP_PKGSIZE	= "bundle_size";
	public final static String PARAM_DEVICEAPP_DATASIZE	= "dynamic_size";
	public final static String PARAM_DEVICEAPP_MANAGED 	= "managed";
	
	public final static String PARAM_ORGID 			= "org_id";	
	public final static String PARAM_GROUPID 		= "group_id";	
	public final static String PARAM_PARENTID 		= "parent_id";	
	public final static String PARAM_PARENTNAME		= "parent_name";	
	public final static String PARAM_PARENTTYPE		= "parent_type";	
	public final static String PARAM_GCMTOKEN 		= "gcm_token";	
	public final static String PARAM_ASSETTAG 		= "asset_tag";
	public final static String PARAM_ENROLLMENTCODE	= "enrollment_code";	
	public final static String PARAM_BATTERY_LEVEL	= "battery_level"; // as a float, decimal of 1.
	public final static String PARAM_DEVICE_CAPACITY		    = "device_capacity";  // float
	public final static String PARAM_DEVICE_AVAILABLE_CAPACITY	= "available_device_capacity";  // float

	public final static String PARAM_DEVICE_MODEMFWVER = "modem_firmware_version";

    public final static String PARAM_REPORT_RESULTS  = "report_only";

    public final static String PARAM_APPVERSION = "current_version"; 

	public final static String PARAM_CELLULAR_TYPE = "cellular_technology";
	public final static String PARAM_MEID = "meid";
	public final static String PARAM_IMEI = "imei";
	
	public final static int APPSYNCTYPE_all = 1;
	public final static int APPSYNCTYPE_delta = 2;
	
	/* error types */
	public static int ERRORTYPE_MISC  = 0;
	public static int ERRORTYPE_FATAL = 1;
	public static int ERRORTYPE_WARN  = 2;
	public static int ERRORTYPE_INFO  = 3;
	public static int ERRORTYPE_REBOOT= 5;
	public static int ERRORTYPE_TOASTMSG= 6;
	public static int ERRORTYPE_REINSTALL= 9;
	
	/* Generic status flags: */
	public final static int OPSTATUS_NEW = 0;
	public final static int OPSTATUS_INITIALIZING = 1;
	public final static int OPSTATUS_RUNNING  = 2;
	public final static int OPSTATUS_COMPLETE = 3;
	public final static int OPSTATUS_WAITING = 4;
	public final static int OPSTATUS_FIALED  = 8;
	public final static int OPSTATUS_INTERRUPTED = 9;
	
	public final static int ACTIONSTATUS_OK = 1;
	public final static int ACTIONSTATUS_FAILED = 3;
	public final static int ACTIONSTATUS_CANCELLED = 2;


	public final static String EMPTY_JSON = "{}";
	
	/* Update internal string constants; these are not shown in the ui. */
	public final static String UPDATECHECK_NEVER     = "never";  // update check is off.
	public final static String UPDATECHECK_DAILY     = "daily";
	public final static String UPDATECHECK_WEEKLY    = "weekly";
	public final static String UPDATECHECK_MONTHLY   = "monthly";
	public final static String UPDATECHECK_QUARTERLY = "quarterly";
	public final static String UPDATECHECK_YEARLY    = "yearly";
	public final static String UPDATECHECK_DEFAULT = UPDATECHECK_MONTHLY;
	
	public final static long NUMBER_SECONDS_IN_DAY      = 86400;  // number of seconds in a day (60x60x24) 
	public final static long NUMBER_MILLISECONDS_IN_DAY = 86400000;  // number of milli-seconds in a day
}
