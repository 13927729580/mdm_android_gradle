/**
 *  Data class for managing the Properties and Settings for the MDM application. 
 *  Reads/writes values to the local Sqlite database private to MDM.
 *  
 *  Values are generally stored as name-value pairs.
 */
package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Hashtable;
import java.util.Properties;

import org.json.JSONObject;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipFile;


/**
 * Provides application settings and preferences, with a wrapper for underlying data storage.
 * 
 * Uses SharedPreferences as the data store, keeping the values in an xml file under the
 * application's private data directory. 
 * Internally a hashtable is used for quick reference instead of needing to go the the data
 *  storage for each value used. Reads in the values once upon creation.
 */
public class Settings {
	private final static String TAG = "Settings";

	// Schema/storage version
	public final static int    SETTINGS_CURRENT_DB_VERSION  = 1;
	public final static String SETTINGS_CURRENT_VERSION_STR  = "1";
	public final static String UPDATECFG_VERSION = "UPDATECFG_VERSION";
	
	// value names
	public final static String ENROLLMENTSERVER_URL = "ENROLLMENT_SERVER";
	public final static String ENROLLMENT_CODE      = "ENROLLMENT_CODE";

	public final static String MDMSERVER_ADDRESS  = "MDMSERVER_ADDRESS";
	public final static String MDMSERVER_PORT 	  = "MDMSERVER_PORT";
	
	public final static String MDMSVRRESULTS_ADDRESS = "MDMSVRRESULTS_ADDRESS";
	public final static String MDMSVRRESULTS_PORT = "MDMSVRRESULTS_PORT";

	public static final String MDMUTILS_SERVER_URL  = "MDMUTILS_SERVER_URL"; //utilities server (for updates, etc.)

	public static final String MDMAUTH_SERVER_URL   = "MDMAUTH_SERVER_URL";
	public static final String MDMAUTH_REDIRECT_URL = "MDMAUTH_REDIRECT_URL";
	public static final String MDMAUTH_APPID	    = "AUTHI";
	public static final String MDMAUTH_APPSECRETKEY = "AUTHK";
	public static final String MDMAUTH_MBCTOKEN     = "AUTHMBCTK";

	public final static String DEVICE_UDID		  = "DEVICE_UDID";
	public final static String DEVICE_SERIALNUM	  = "DEVICE_SERIALNUM";
	public final static String LASTSYNC  		  = "LASTSYNC";
	public final static String NEXTSYNC  		  = "NEXTSYNC";
	public final static String LASTAPPSSYNC  	  = "LASTAPPSSYNC"; // time of last apps sync with server
	public final static String GCM_REG_ID		  = "GCM_REG_ID";
	public final static String ORGANIZATION_ID	  = "ORG_ID";
	public final static String ORGANIZATION_NAME  = "ORG_NAME";
	public final static String GROUP_ID	  		  = "GROUP_ID";
	public final static String GROUP_NAME  		  = "GROUP_NAME";
	public final static String PARENT_ID	  	  = "PARENT_ID";
	public final static String PARENT_NAME  	  = "PARENT_NAME";
	public final static String PARENT_TYPE	  	  = "PARENT_TYPE";
	public final static String SENT_GROUP_ID	  = "SENT_GROUP_ID"; // last group ID sent to server
	public final static String ASSETTAG			  = "ASSET_TAG";
	public final static String USER_ID	  		  = "USER_ID";
	public final static String INITSTATE		  = "INITSTATE";
	public final static String UPDATECHECK		  = "UPDATECHECK";
	public final static String DISPLAYNOTIFS	  = "DISPLAYNOTIFS";
	public final static String DATA_VERSION	  	  = "DATA_VERSION";    // when set by the server, the server-provided version
	public final static String SETTINGS_VERSION	  = "SETTINGSVERSION";
	public final static String APPLICATION_VERSION= "APPLICATIONVERSION";
	 
	public final static String APPSYNC_TYPE  = "APPSYNCTYPE";
	public final static String APPSYNC_all   = "all";
	public final static String APPSYNC_delta = "delta";
	public final static String APPSYNC_default = APPSYNC_all;
	 
	
	public final static String LASTUPDATECHECKTIME = "LASTUPDATECHECKTIME";
	public final static String NEXTUPDATERETRYTIME = "NEXTUPDATERETRYTIME";
	public final static String NEXTUPDATERETRYVER  = "NEXTUPDATERETRYVERSION";
	
	public final static String MDM_FILEDOWNLOAD_URL = "MDMSERVER_FILES_URL";
		
	public final static String SVRCONNECTTIMEOUT     = "SVR_CONNECT_TIMEOUT";
	public final static String SVRCONNECTREADTIMEOUT = "SVR_CONNECTREAD_TIMEOUT";
	
	public final static String SVRSENDCMDRESULTSOSERVER = "SVR_SENDCMDRESULTS";
	
	public final static String LOGLEVEL			  = "LOGLEVEL";

	public final static String LOCATION_ENABLE = "LOCATION_ENABLE";
	public final static String LOCATION_TIME = "LOCATION_TIME";
	public final static String LOCATION_DISTANCE = "LOCATION_DISTANCE";

	// name of the file used to store the settings data.
	//private static final String PREFERENCES_DATASTORE = "settings";
	  
	// singleton:
	private static Settings settingsInstance;
	
	private SharedPreferences prefs;   // data stored as local 'private' preferences.
	private Map<String,String> values; // key-value pairs of settings, kept in memory for optimized performance. 
	private Context context;
	private App thisApp;			  // app instance containing info about this app.
	
	/**
	 * Gets the Settings instance (as a singleton, creating it as needed).
	 * @return application's instance of Settings.
	 */
	public static Settings getInstance(Context context) {
		if (settingsInstance == null) {
			synchronized (Settings.class) {
				if (settingsInstance == null) 
					settingsInstance = new Settings(context);
			}			
		}			
		return settingsInstance;
	}
	 
	/** Creates instance. */ 
	@SuppressWarnings("unchecked")
	private Settings(Context context) {
		this.context = context;
	//	values = new HashMap<String,String>();
		try {
			prefs = getDatastorePreferences(context);
			// get existing settings
			values =  (Map<String, String>) prefs.getAll();
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}
	    if (values == null)
	    	values = new Hashtable<String,String>();
	    if (values.size()==0) { 
	    	// create defaults if none are present:
	    	setDefaults();
	    	// and then, read customized settings from file in the package if it's there.
	    	initFromConfigFile();
	    }
	    // see if we need to read in any updates: (compare stored version with current app version)
	    String updateVer = getSetting(UPDATECFG_VERSION);
	    if (updateVer == null || updateVer.compareTo(Constants.APPLICATION_VERSION_STR)!=0)
	    	updateFromConfigFile();
	}
	
	/**
	 * Gets the application version name string, the android:versionName value from the manifest file.
	 */
	public String getAppVersion() {
		App app = getAppInstance();
		return app.getVersionName();
		//return getSetting(APPLICATION_VERSION);
	}
	
	// convenience methods for specific values:
	public String getServerUrl() {
		String port = getSetting(MDMSERVER_PORT);
		if (port == null || port.length()==0)
			return getSetting(MDMSERVER_ADDRESS);
		return getSetting(MDMSERVER_ADDRESS) + ":" + port;
	}
	
	public String getUtilsServerUrl() {
		String url = getSetting(MDMUTILS_SERVER_URL);
		if (url == null)
			url = Constants.DEFAULT_MDMUTILS_SERVER_URL;
		return url;
	}
	
	public String getEnrollmentServer() {
		// note: always use the same configured mdm server; if store the enroll server, there is 
		//  not a way to change it if the server url gets changed ahead of time.
		String s = getSetting(ENROLLMENTSERVER_URL);
		if (s==null)
			s = getServerUrl();
		return s;
		//return getServerUrl();
	}
	
	public String getEnrollmentCode() {
		return getSetting(ENROLLMENT_CODE);
	}
		
	// get server results callback url destination, for sending command processing results to, if defined; 
	// defaults to Server Url if not found.
	public String getServerResultsUrl() {
		String url  = getSetting(MDMSVRRESULTS_ADDRESS);
		String port = getSetting(MDMSVRRESULTS_PORT); 
		if (url != null) {
			if (port != null)
				url = url + ":" + port;
		} else {
			url = getServerUrl();
		}
		return url;
	}
	public String getOrganizationID() {
		return getSetting(ORGANIZATION_ID);
	}
	public boolean isEnrolled() {
		return (getOrganizationID() != null);
	}
	public String getOrganizationName() {
		return getSetting(ORGANIZATION_NAME);
	}
	public String getGroupID() {
		return getSetting(GROUP_ID);
	}
	public String getSentGroupID() {
		return getSetting(SENT_GROUP_ID);
	}
	public String getGroupName() {
		return getSetting(GROUP_NAME);
	}
	public String getParentID() {
		return getSetting(PARENT_ID);
	}
	public String getParentName() {
		return getSetting(PARENT_NAME);
	}
	public String getParentType() {
		return getSetting(PARENT_TYPE);
	}
	
	public String getDeviceSerialNumber() {
		return getSetting(DEVICE_SERIALNUM);
	}

	
	public String getGcmID() {
		return getSetting(GCM_REG_ID);
	}
	public int getInitializationState() {
		int initState = 0;
		String sInit = getSetting(INITSTATE);
		if (sInit != null)
			initState = Integer.valueOf(sInit);
	    return initState;
	}
	
	public String getDeviceUdid() {
		return getSetting(DEVICE_UDID);
	}
	
	public long getLastSyncTime() {
		return getSettingLong(LASTSYNC, 0);
	}
	
	public long getLastAppsSyncTime() {
		return getSettingLong(LASTAPPSSYNC, 0);
	}
	
	public long getNextSyncTime() {
		return getSettingLong(NEXTSYNC, 0);
	}
	
	/**
	 * Gets the last-sync'd date/time as a string using the default format for the current Locale.
	 * @return
	 */
	public String getLastSyncAsString() {
		String syncval = getSetting(LASTSYNC);
		if (syncval != null) {
			try {
				long ltime = Long.valueOf(syncval);
				if (ltime != 0) {
					syncval = //DateFormat.getDateTimeInstance().format(new Date(ltime));//use default date-locale formatting to convert time-value to a string
							Utils.formatLocalizedDateUTC(ltime);
				}
			} catch (Exception ex) {
				LSLogger.exception(TAG, "Settings.getLastSyncAsString error", ex);
			}
		}
		return syncval;
	}
	/**
	 * Gets the next-sync'd date/time as a string using the default format for the current Locale.
	 * @return
	 */
	public String getNextSyncAsString() {
		String syncval = getSetting(NEXTSYNC);
		if (syncval != null) {
			try {
				long ltime = Long.valueOf(syncval);
				if (ltime != 0) {
					syncval = //DateFormat.getDateTimeInstance().format(new Date(ltime));//use default date-locale formatting to convert time-value to a string
							Utils.formatLocalizedDateUTC(ltime);
				}
			} catch (Exception ex) {
				LSLogger.exception(TAG, "Settings.getNextSyncAsString error", ex);
			}
		}
		return syncval;
	}

	public boolean isLastLocationEnable() {
		return getSettingBoolean(LOCATION_ENABLE, false);
	}

	public int getLastLocationTime() {
		return getSettingInt(LOCATION_TIME,10);
	}

	public int getLastLocationDistance() {
		return getSettingInt(LOCATION_DISTANCE,0);
	}

	public void setLastLocationEnable(boolean enable) {
		setSetting(LOCATION_ENABLE,String.valueOf(enable));
	}

	public void setLastLocationTime(int time) {
		setSetting(LOCATION_TIME,time);
	}

	public void setLastLocationDistance(int distance) {
		setSetting(LOCATION_DISTANCE,distance);
	}

	/**
	 * Returns true if the system is to post command processing results to a server,
	 * false if not. Uses the SVRSENDCMDRESULTSOSERVER setting value.
	 */
	public boolean isSendCmdResultsToServer() {
		return getSettingBoolean(SVRSENDCMDRESULTSOSERVER, false);
	}
	
	public String getUpdateCheck() {
		return getSetting(UPDATECHECK);
	}
	public String getUpdateConfigVersion() {
		return getSetting(UPDATECFG_VERSION);
	}
	
	public void setOAuthServerUrl(String url) {
		setSetting(MDMAUTH_SERVER_URL, url);
	}
	public String getOAuthServerUrl() {
		String url = getSetting(MDMAUTH_SERVER_URL);
		if (url == null)
			url = Constants.DEFAULT_MDMAUTH_SERVER_URL;
		return url;
	}
	public String getOAuthRedirectUrl() {
		String url = getSetting(MDMAUTH_REDIRECT_URL);
		if (url == null)
			url = Constants.DEFAULT_MDMAUTH_REDIRECT_URL;
		return url;
	}
	public String getOAuthAppID() {
		String s = getSetting(MDMAUTH_APPID);
		if (s == null)
			s = Constants.DEFAULT_MDMAUTH_APPID;
		return s;
	}
	public String getOAuthSecret() {
		String s = getSetting(MDMAUTH_APPSECRETKEY);
		if (s == null)
			s = Constants.DEFAULT_MDMAUTH_APPKEY;
		return s;
	}
	public String getAuthMBCToken() {
		String s = getSetting(MDMAUTH_MBCTOKEN);
		if (s == null)
			s = Constants.DEFAULT_MDMAUTH_MBCTOKEN;
		return s;
	}

	/** 
	 * Gets the applications data sync type, as a Constants.APPSYNCTYPE value. 
	 * This defines how much installed-apps data is sent upon a checkin. 
	 * @return 0 or a APPSYNCTYPE_value.
	 */
	public int getAppSyncType() {
		String s = getSetting(APPSYNC_TYPE);
		if (s == null)
			s = APPSYNC_default;
		int type = 0;
		if (s != null) {
			if (s.equalsIgnoreCase(APPSYNC_all))
				type = Constants.APPSYNCTYPE_all;
			else if (s.equalsIgnoreCase(APPSYNC_delta))
				type = Constants.APPSYNCTYPE_delta;
		}
		return type;
	}

	
	/**
	 * Sets the server and port values from the given url string; can throw a uri exception if there are errors.
	 * @param url
	 */
	public void setMdmServerFromUrl(String url) {
		Uri uri = Uri.parse(url);
		String server = url;
		String portStr = null;
		int port = uri.getPort(); // -1 is invalid or not present
		if (port > 0) { // port is present, chop it off the server.
			portStr = Integer.toString(port);
			int indx = server.indexOf(portStr);
			if (indx > 1) 
				server = server.substring(0, indx-1);
		}
		setSetting(MDMSERVER_ADDRESS, server);
		setSetting(MDMSERVER_PORT, portStr);
	}
	
	public void setEnrollmentServer(String svr) {
		setSetting(ENROLLMENTSERVER_URL, svr);
	}
	public void setEnrollmentCode(String ecode) {
		setSetting(ENROLLMENT_CODE, ecode);
	}
	protected void setOrganizationID(String id) {
		setSetting(ORGANIZATION_ID, id);
	}
	protected void setOrganizationName(String n) {
		setSetting(ORGANIZATION_NAME, n);
	}
	protected void setGroupID(String id) {
		setSetting(GROUP_ID, id);
	}
	protected void setSentGroupID(String id) {
		setSetting(SENT_GROUP_ID, id);
	}
	protected void setGroupName(String n) {
		setSetting(GROUP_NAME, n);
	}
	protected void setGcmID(String id) {
		setSetting(GCM_REG_ID, id);
	}
	protected void removeGcmID() {
		removeSetting(GCM_REG_ID);
	}
	protected void setDeviceUdid(String id) {
		setSetting(DEVICE_UDID, id);
	}
	public void setDeviceSerialNumber(String sn) {
		setSetting(DEVICE_SERIALNUM, sn);
	}	
	protected void setLastSyncTime(long time) {
		setSetting(LASTSYNC, Long.toString(time));
	}
	protected void setLastAppsSyncTime(long time) {
		setSetting(LASTAPPSSYNC, Long.toString(time));
	}
	protected void setNextSyncTime(long time) {
		setSetting(NEXTSYNC, Long.toString(time));
	}
	protected void setUpdateCheck(String s) {
		setSetting(UPDATECHECK, s);
	}

	protected synchronized void setInitializationStateFlag(int newflag) {
		int state = getInitializationState() | newflag;
		setInitializationState(state);
	}
	protected synchronized void clearInitializationStateFlag(int clearflag) {
		int flagmask = ~clearflag;
		int state = getInitializationState() & flagmask;
		setInitializationState(state);
	}
	private void setInitializationState(int newstate) {
		setSetting(INITSTATE, Integer.toString(newstate));
	}
	
	/**
	 * Ensures all values are present that are needed for mdm server communications. These are: 
	 * server address, server port, and organization id.
	 * @return true if all the value are present, false if any one is missing.
	 */
    public boolean hasRequiredMdmServerCommunicationsValues() {
    	return (getSetting(MDMSERVER_ADDRESS) != null  &&
    			//getSetting(MDMSERVER_PORT) != null &&  // port can be null or blank. no biggie.
    			getSetting(ORGANIZATION_ID) != null);
    }
	
    /**
     * Removes the organization and group settings. Deletes existing values if they exist.
     */
    public void deleteOrganizationInfo() {
    	removeSetting(ORGANIZATION_ID);
    	removeSetting(ORGANIZATION_NAME);
    	removeSetting(GROUP_ID);
    	removeSetting(GROUP_NAME);
    }
    
        
    
    // -------------------------------------------------
	// generic methods for handling non-specific values:
	// -------------------------------------------------
    
	/**
	 * Gets the value with the given name.
	 * @param name identifier of the setting to retrieve.
	 * @return the value if found, null if the value was not found or the value is an empty string.
	 */
	public String getSetting(String name) {
		String value = values.get(name);
		if (value != null && value.length()>0)
			return value;
		return null;
	}
	
	/**
	 * Gets the value with the given name as an int value.
	 * @param name identifier of the setting to retrieve.
	 * @param defaultValue value to use if the setting is not found.
	 * @return value if found, 0 if the value was not found.
	 */
	public int getSettingInt(String name, int defaultValue) {
		int i = defaultValue;
		String value = values.get(name);
		if (value != null && value.length()>0) {
			try {
				i = Integer.valueOf(value);
			} catch (NumberFormatException nfex) { 
				LSLogger.exception(TAG, "Exception getting int value for " +name, nfex);
			}
		}
		return i;
	}
	
	/**
	 * Gets the value with the given name as a long value.
	 * @param name identifier of the setting to retrieve.
	 * @param defaultValue value to use if the setting is not found.
	 * @return value if found, 0 if the value was not found.
	 */
	public long getSettingLong(String name, long defaultValue) {
		long i = defaultValue;
		String value = values.get(name);
		if (value != null && value.length()>0) {
			try {
				i = Long.valueOf(value);
			} catch (NumberFormatException nfex) { 
				LSLogger.exception(TAG, "Exception getting long value for " +name, nfex);
			}
		}
		return i;
	}

	
	/**
	 * Gets the value with the given name as a boolean value.
	 * @param name identifier of the setting to retrieve.
	 * @param defaultValue value to use if the setting is not found.
	 * @return value if found, false if the value was not found.
	 */
	// note that to set a boolean value, pass in "true" or "false" as the value for the string.
	public boolean getSettingBoolean(String name, boolean defaultValue) {
		boolean i = defaultValue;
		String value = values.get(name);
		if (value != null && value.length()>0) {
			try {
				i = Boolean.valueOf(value);
			} catch (NumberFormatException nfex) { 
				LSLogger.exception(TAG, "Exception getting boolean value for " +name, nfex);
			}
		}
		return i;
	}
	

	/**
	 * Sets or adds the value with the given name.
	 * @param name identifier of the setting to be set.
	 * @param value value of the setting.
	 */
	public void setSetting(String name, String value) {
		setSetting(name, value, null);
	}

	/**
	 * Sets or adds the integer value with the given name.
	 * @param name identifier of the setting to be set.
	 * @param intvalue integer value of the setting.
	 */
	public void setSetting(String name, int intvalue) {
		setSetting(name, Integer.toString(intvalue), null);
	}
	
	/**
	 * Sets or adds the long value with the given name.
	 * @param name identifier of the setting to be set.
	 * @param longvalue long value of the setting.
	 */
	public void setSetting(String name, long longvalue) {
		setSetting(name, Long.toString(longvalue), null);
	}
	
	/**
	 * Sets or updates the setting without persisting it. This is typically used from views that will automatically
	 * persist and save the settings data, so no need to do it twice.
	 * @param name identifier of the setting to be set.
	 * @param value value of the setting.
	 */
	public void setSettingInternal(String name, Object value) {
		String newstr = value.toString();
		values.put(name, newstr);
		//LSLogger.debug(TAG, "Set "+name+" to "+(value!=null?value:"null"));
	}
	
	/*
	 * Replaces or adds the value with the given name-value pair. 
	 * @param editor the properties editor instance to add the value to, used when 
	 * adding or setting multiple values. The caller must call apply() or commit() on the
	 * editor when the additions are complete. Or just pass in null here to do one save at a time.
	 */
	private void setSetting(String name, String value, Editor editor) {
		values.put(name, value);
		LSLogger.info(TAG, "Set '" + name +"' to '" + value + "'");
		// save the value or add it to the existing editor instance:
		if (editor != null) {
			editor.putString(name, value);  // just add the value, do not commit it.
		} else { // commit after the change:
			editor = prefs.edit();
			editor.putString(name, value);
			editor.commit();		
		}

		//if (persist)
		//	save();
	}
	
	/**
	 * Removes the indicated setting.
	 * @param name name of the value to remove.
	 */
	public void removeSetting(String name) {
		values.remove(name);
		// remove from persistence:
        Editor editor = prefs.edit();
        editor.remove(name);
        editor.commit();		
	}

	/**
	 * Removes all settings.
	 */
	public void removeAll() {
		values.clear();
		Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
	}
	
	/**
	 * Loads settings from the default configuration properties XML file (config.xml located in /assets in the package).
	 */
	protected void initFromConfigFile() {
		String cfgfile = "config.xml";
		try {
	 		addFromAssetFile(cfgfile, true, true);
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}
	}

	/**
	 * Loads settings from the update configuration properties XML file (update.xml located in /assets in the package).
	 */
	protected void updateFromConfigFile() {
		String cfgfile = "update.xml";
		LSLogger.debug(TAG, "Updating settings for version " + Constants.APPLICATION_VERSION_STR);
		try {
	 		addFromAssetFile(cfgfile, true, true);
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}
	}
	
	/**
	 * Sets values from the given json name-value pairs, where names are settings names, and values
	 *  are string or other storable values. The names should be recognized settings names, but this
	 *   will store any name, even if it is undefined.
	 *   
	 * Named-values with no value (null or empty string) results in previous values being deleted.
	 * @param jdata name-value pairings in json
	 * @param result optional CommandResult, for gettin error information passed back into.
	 */
	protected void setValues(JSONObject jdata, CommandResult result) {
		if (jdata != null && jdata.length() > 0) {
			Editor editor = prefs.edit();
			try {
				// get each item and set its value:
				Iterator<?> names = jdata.keys();
				while (names.hasNext()) {
					String name = (String) names.next();
					String value = jdata.getString(name);
					setSetting(name.toUpperCase(Locale.US), value);
				}	
			} catch (Exception ex) {
				LSLogger.exception(TAG, "setValues error:", ex);
				if (result != null)
					result.setException(ex);
			}
			editor.commit();
		}
	}
	
	/**
	 * Adds settings from the given file name located in the current app's package. 
	 * If values with the same name exist, they are replaced.
	 * Values for names that do not exist in the settings are added as new settings. ...maybe
	 * @param filename name and path to the file within the application's package, 
	 * to read property values from. The file must be an XML file in Java Properties format.
	 * @return The number of properties added, or throws an exception if an error occurred.
	 */
	/* -- deprecated (mz) -- use files from assets instead of this method.
	protected int addFromPackageFile(String filename, boolean bAdd, boolean bReplace) {
		int numProps = 0;
		Properties props = new Properties();
		InputStream inputStream = null;
		try {
			ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);

			File pkgFile = new File(appInfo.sourceDir); // this should be the application's package file and path
			if (pkgFile == null || !pkgFile.exists()) {
				LSLogger.error(TAG, "Package file not found. Package: " + context.getPackageName() + " File: " +
								(appInfo.sourceDir==null?"(null)":appInfo.sourceDir));
			} else {
				ZipFile zipFile = new ZipFile(pkgFile);
				LSLogger.info(TAG, "Zipfile count="+Integer.toString(zipFile.size()));
				Enumeration  en = zipFile.entries();
				while (en.hasMoreElements()) {
					LSLogger.info(TAG, " -"+en.nextElement().toString());
					
				}
				
				ZipEntry zipEntry = zipFile.getEntry(filename);
				if (zipEntry != null) {
				    inputStream = zipFile.getInputStream(zipEntry);
					props.loadFromXML(inputStream);
				} else {
					LSLogger.error(TAG, "File " + filename + " not found in package " + context.getPackageName());
				}
			}
		} catch (Exception e) {
			LSLogger.exception(TAG, "Settings.AddFromPackageFile error.", e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception iex) {
					LSLogger.exception(TAG, "AddFromPackageFile-cleanup", iex);
				}
			}
		}
		
		if (!props.isEmpty()) {
			// add items to settings
			Enumeration keys = props.keys();
			while (keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				String value = props.getProperty(key);
				setSetting(key, value);
				LSLogger.info(TAG, "-Property: " + key + "="+value);
			}
		} else {
			LSLogger.info(TAG, "NO properties found from file " + filename);
		}
		return numProps;
	}

	
	/**
	 * Adds settings from the given asset file. If values with the same name exist, they are replaced.
	 * Values for names that do not exist in the settings are added as new settings. 
	 * @param filepath relative path within /assets to the file to read settings from. 
	 * The file must be a Java XML Properties file.
	 * @return throws exception if there is a file or xml error. 
	 */
	protected void addFromAssetFile(String filepath, boolean bAdd, boolean bReplace) throws Exception {
		Properties props = new Properties();
		InputStream is = null;
		try {
			/* .. from a raw file, you'd use this: ..
			File f = new File(filepath);
			if (!f.exists()) {
				LSLogger.error(TAG, "File " + filepath + " not found.");
			} else {
				is = new FileInputStream(f);
				props.loadFromXML(is);
			}
			.. */
			
			is = context.getAssets().open(filepath);
			props.loadFromXML(is);
		} catch (Exception e) {
			LSLogger.exception(TAG, "Error opening or reading asset file " + filepath +".", e);
			throw e;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception iex) {
					LSLogger.exception(TAG, "AddFromAssetFile-cleanup", iex);
				}
			}
		}
		
		if (!props.isEmpty()) {
			// add items to settings
			Editor editor = prefs.edit();

			Enumeration<?> keys = props.keys();
			while (keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				String value = props.getProperty(key);
				setSetting(key.toUpperCase(Locale.US), value, editor);
				//LSLogger.info(TAG, "-Property: " + key + "="+value);
			}
			editor.commit();
		} else {
			LSLogger.info(TAG, "NO properties found.");
		}
	}
	
	// gets the app instance representing info about this app. Useful for version info and such.
	private App getAppInstance() {
		if (thisApp == null) {
			synchronized(this) {
				if (thisApp == null) {
					thisApp = new App(Constants.PACKAGE_NAME);
					thisApp.loadPackageInfo(null, context);
				}
			}
		}
		return thisApp;
	}
	
	// logs the name-value pairs to logging, for informational and debug use.
	public void logValues() {
		if (LSLogger.isLoggingEnabled()) {
			Iterator<String> iter = values.keySet().iterator();
			LSLogger.info(TAG, "--Begin Settings--");		
			while (iter.hasNext()) {
				String key = iter.next();
				String value = values.get(key);
				LSLogger.info(TAG, key+" = '"+ (value==null?"(null)":value) + "'");
			}
			LSLogger.info(TAG, "--End Settings--");
		}
	}
	
	//private void save() {
	//}
	
	// Gets the storage instance for SharedPreferences; located in /data/data/com/lightspeedsystems.mdm/shared_prefs
	private static SharedPreferences getDatastorePreferences(Context context) {
        //return context.getSharedPreferences(PREFERENCES_DATASTORE, Context.MODE_PRIVATE);
		return PreferenceManager.getDefaultSharedPreferences(context);
    }
	
	/**
	 * Creates default/initial settings.
	 */
	protected void setDefaults() {
		String mdmsvr = this.getServerUrl();
		
		Editor editor = prefs.edit();
		setSetting(SETTINGS_VERSION, SETTINGS_CURRENT_VERSION_STR, editor);
		setSetting(APPLICATION_VERSION, Constants.APPLICATION_VERSION_STR, editor);
		setSetting(DISPLAYNOTIFS, "true", editor); 
		setSetting(UPDATECHECK, Constants.UPDATECHECK_DEFAULT, editor); 
		
		if (mdmsvr == null) {
			setSetting(MDMSERVER_ADDRESS, Constants.DEFAULT_MDMSERVER_URL, editor);
			if (Constants.DEFAULT_MDMSERVER_PORT != null && !Constants.DEFAULT_MDMSERVER_PORT.isEmpty())
				setSetting(MDMSERVER_PORT, Constants.DEFAULT_MDMSERVER_PORT, editor);
		}
		//setSetting(MDMSERVER_PORT,    "3000", editor);				
		//setSetting(MDMSERVER_ADDRESS, "http://192.168.1.109", editor);
		setSetting(MDMAUTH_SERVER_URL, 	 Constants.DEFAULT_MDMAUTH_SERVER_URL, editor);
		setSetting(MDMAUTH_REDIRECT_URL, Constants.DEFAULT_MDMAUTH_REDIRECT_URL, editor);
		//setSetting(MDMAUTH_APPID, 		 Constants.DEFAULT_MDMAUTH_APPID, editor);
		//setSetting(MDMAUTH_APPSECRETKEY, Constants.DEFAULT_MDMAUTH_APPKEY, editor);
		
		//setSetting(ORGANIZATION_ID, "1", editor);		
		//setSetting(ASSETTAG, "Default", editor);

		// save the values:
		editor.commit();
		//save();
	}

	
	public SyncSettings getSyncSettings() {
		return new SyncSettings();
	}
	
	// Convenience class for containing sync / checkin settings.
	public class SyncSettings {
		public int interval; // minutes between checkins.
		public int intervalType;
		
		private SyncSettings() {
			interval = 720;  // 720 minutes = 60 minutes x 12 hours
			
		}
		// gets the amount of time as the next interface setting.
		public long getNextSyncIntervalMinutes() {
			return interval;
		}
	}
	
}

