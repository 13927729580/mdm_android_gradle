package com.lightspeedsystems.mdm;

import java.io.File;

import com.lightspeedsystems.mdm.util.LSLogger;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

/**
 * Contains information about an application. Includes applications defined from reading in Package info,
 * for defining an application being managed for a command, etc.
 */
public class App {
	private static String TAG = "App";
	
	public final static String VALUE_DBID 	 	 	 = "AppDBID";
	public final static String VALUE_APPCLASS 	 	 = "AppClass";
	public final static String VALUE_APPNAME 	 	 = "AppName";
	public final static String VALUE_PACKAGENAME 	 = "PackageName";
	public final static String VALUE_PACKAGEFILENAME = "PackageFileName";
	public final static String VALUE_PACKAGEFILEPATH = "PackageFilePath";
	public final static String VALUE_PACKAGESTOREURL = "PackageStoreUrl";
	public final static String VALUE_PACKAGESVRURL   = "PackageServerUrl";
	public final static String VALUE_MDMAPPTYPE  	 = "MdmAppType";
//	public final static String INTENT_EXTRA_APPDATA  = Constants.PACKAGE_NAME+".App";
	
	public final static int MDMAPPTYPE_self       = 0x01000000; // app is ourself, this mdm app 
	public final static int MDMAPPTYPE_perreq_gsf = 0x02000000; // app is a prereq/gsf app: 
	
	// - install source locations; identifies where the app came from:
	/** App was installed from a local file, from a SD card, email, or other local location. */
	public final static int INSTALLTYPE_LOCAL = 1;
	/** App was installed from remote server file, downloaded locally and then installed. */
	public final static int INSTALLTYPE_REMOTEFILE = 2;
	/** App was installed from an online store, such as Google Play. */
	public final static int INSTALLTYPE_ONLINESTORE = 3;
	
	// installation state: identifies the installation state of the application.
	public final static int INSTALLSTATE_default = 0;
	
	
	public final static int INSTALLSTATE_installmask 		= 0x0000FF;
	/** An install has been requested but has not yet started. */
	public final static int INSTALLSTATE_installpending 	= 0x10;
	/** Install downloading of the installer file is occurring. */
	public final static int INSTALLSTATE_installdownloading = 0x20;
	/** Install process has started. May be waiting for user confirmation. */
	public final static int INSTALLSTATE_installing 		= 0x01;
	/** Installation completed successfully. The app is now installed. */
	public final static int INSTALLSTATE_installed 			= 0x02;
	/** Installation failed due to error. See installreason for error details. */
	public final static int INSTALLSTATE_installerror 		= 0x04;
	/** Installation cancelled by user interaction. */
	public final static int INSTALLSTATE_installcancelled 	= 0x08;

	public final static int INSTALLSTATE_updatemask 		= 0x00FF00;
	/** An update has been requested but has not yet started. */
	public final static int INSTALLSTATE_updatepending 		= 0x1000;
	/** Update downloading of a new installer file is occurring. */
	public final static int INSTALLSTATE_updatedownloading 	= 0x2000;
	/** Update process has started. May be waiting for user confirmation. */
	public final static int INSTALLSTATE_updating 			= 0x0100;
	/** Update completed successfully. The app is now installed. */
	public final static int INSTALLSTATE_updated 			= 0x0200;
	/** Update failed due to error. See installreason for error details. */
	public final static int INSTALLSTATE_updateerror 	 	= 0x0400;
	/** Update cancelled by user interaction. */
	public final static int INSTALLSTATE_updatecancelled 	= 0x0800;
	
	public final static int INSTALLSTATE_uninstallmask 	= 0xFF0000;
	/** An uninstall has been requested but has not yet started. */
	public final static int INSTALLSTATE_uninstallpending 	= 0x100000;
	/** Install process has started. May be waiting for user confirmation. */
	public final static int INSTALLSTATE_uninstalling 		= 0x010000;
	/** The uninstall completed successfully. The app is now installed. */
	public final static int INSTALLSTATE_uninstalled 	 	= 0x020000;
	/** Uninstall failed due to error. See installreason for error details. */
	public final static int INSTALLSTATE_uninstallerror 	= 0x040000;
	/** Uninstall was cancelled by user interaction. */
	public final static int INSTALLSTATE_uninstallcancelled = 0x080000;
	
	public final static int INSTALLSTATE_apptypemask = 0x0F000000;
	
	
	private String appName;			// name of the app
	private String versionName;		// version description
	private int    versionCode;		// version
	private String versionCodeStr;	// versionCode as a string
	private String packageName;		// name of the package
	private String pkgfilename;		// name (name-only,no-path) of local package file for the app installation package
	private long   sizeOfPkg;		// number of bytes in the .apk package file
	private long   dataSize;		// number of data bytes used by the app
	private Drawable appIcon;		// app's icon
	private Drawable appLogo;		// app's logo
	
	private boolean definitionDirty; // when true, values that define the app are dirty and need saving.
	
	// additional persistence-related values:
	private int appDbID;	  // local DB ID, identifies the row in the local database
	private int appMdmID;	  // ID of the app on the mdm server, if known; 0 for default
	private long installTime; // time when install was started/queued, or when install completed successfully
	private int installType;  // one of the INSTALLTYPE_ values, identifies where the app came from
	private int installState; // state of the app's, its installation, or uninstallation. INSTALLSTATE_ flag values
	private long uninstallTime;  // time when uninstall was initiated.
	private long updateTime;     // time of last update (when started/queued), or when update completed
	private String installReason; // failure information of last attempted install or uninstall.
	private String displayState; // optionally-used display state for showing the state as a single action.
	private String displayTime;  // gets the display time, related to displayState.
	// - stats:
	private long timeActionReceived; // time of the current action (install, uninstall, etc), 
									 //  when command was received and/or was queued up.
	private long timeActionStarted;  // time the current action started; 0=not started
	private long timeActionFinished; // time the current action completed or was cancelled; 0=not finished
	
	// information related to app installation vs. reading from package data:
	private String installSource; // based on the installType, is tied to one of these other 'source' locations of the install file.
	private String pkgfilepath;		// path to local package file for the app installation package
	private String pkgsvrurl;		// url to package file on download server; includes the file name
	private String storeurl;		// path to remote store-based url to get the app from
	
	private boolean bIsMdmApp;     // true when updating ourself.
	private int mdmAppType;
	
	/**
	 * Creates an empty App instance.
	 */
	public App() {		
	}
	
	/**
	 * Creates an App instance for reading from the Apps database, using a DB ID for the app.
	 * @param dbID 
	 */
	public App(int dbID) {
		appDbID = dbID;
	}
	
	/**
	 * Creates an app instance for a given package name. 
	 * @param packageName
	 */
	public App(String packageName) {
		this.packageName = packageName;
	}
	
	/**
	 * Creates an App instance from a PackageInfo instance, describing the contents of an app.
	 * @param packageInfo package information
	 * @param context application context
	 */
	public App(PackageInfo packageInfo, Context context, boolean getFilesizeData) {
		ApplicationInfo appInfo = loadPackageInfo(packageInfo, context);
		// get file size information:
		if (getFilesizeData) {
			try {
				File pkgFile = new File(appInfo.publicSourceDir);
				if (pkgFile != null) 
					sizeOfPkg = pkgFile.length();
				dataSize = Utils.getDirectoryTreeSize(appInfo.dataDir);
			} catch (Exception ex) {
				LSLogger.exception(TAG, ex);
			}
		}
	}
	
	/**
	 * Gets an apps package-related information; these are values that exist in the package file or
	 * when the package is installed.
	 * @param packageInfo PackageInfo instance to extract information from, or null to get it herein.
	 * @param context application context.
	 * @return ApplicationInfo instance reference from the packageInfo, or null if error.
	 */
	protected ApplicationInfo loadPackageInfo(PackageInfo packageInfo, Context context) {
		ApplicationInfo appInfo = null;
		if (packageInfo == null) {
			try {
				packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
			} catch (Exception ex) {
				LSLogger.exception(TAG, "loadPackageInfo error getting package details:", ex);
			}
		} 
		if (packageInfo != null) {
			appInfo = packageInfo.applicationInfo;					
		} 
		if (appInfo != null) {
			// use loadLabel to read's the app's name
			appName = (String)appInfo.loadLabel(context.getPackageManager());
//			appIcon = appInfo.loadIcon(context.getPackageManager());
			appLogo = appInfo.loadLogo(context.getPackageManager());
			versionName = packageInfo.versionName;
			versionCode = packageInfo.versionCode;
			versionCodeStr = Integer.toString(versionCode);
			packageName = packageInfo.packageName;
			definitionDirty = true;
		}
		return appInfo;		
	}
	
	/**
	 * Creates an App instance from a Bundle.
	 * @param bundle Bundle containing app info.
	 */
	public App(Bundle bundle) {
		if (bundle != null) {
			appDbID     = bundle.getInt(VALUE_DBID);
			appName     = bundle.getString(VALUE_APPNAME);
			packageName = bundle.getString(VALUE_PACKAGENAME);
			pkgfilename = bundle.getString(VALUE_PACKAGEFILENAME);
			pkgfilepath = bundle.getString(VALUE_PACKAGEFILEPATH);
			pkgsvrurl   = bundle.getString(VALUE_PACKAGESVRURL);
			storeurl    = bundle.getString(VALUE_PACKAGESTOREURL);
			setMdmAppType(bundle.getInt(VALUE_MDMAPPTYPE));
		}
	}

	/**
	 * Gets a Bundle instance representing the parsable attributes of an App, needed for intents and activities.
	 * @return bundle instance with appropriate values.
	 */
	public Bundle getBundle() {
		Bundle bundle = new Bundle();
		bundle.putString(VALUE_APPCLASS, App.class.getName());
		bundle.putInt(VALUE_DBID, appDbID);
		bundle.putInt(VALUE_MDMAPPTYPE, getMdmAppType());
		if (appName != null)     bundle.putString(VALUE_APPNAME, appName);
		if (packageName != null) bundle.putString(VALUE_PACKAGENAME, packageName);
		if (pkgfilename != null) bundle.putString(VALUE_PACKAGEFILENAME, pkgfilename);
		if (pkgfilepath != null) bundle.putString(VALUE_PACKAGEFILEPATH, pkgfilepath);
		if (pkgsvrurl != null)   bundle.putString(VALUE_PACKAGESVRURL, pkgsvrurl);
		if (storeurl != null)    bundle.putString(VALUE_PACKAGESTOREURL, storeurl);
		return bundle;
	}
	
	public void setIsMdmApp() {
		bIsMdmApp = true;
		mdmAppType = MDMAPPTYPE_self; // default app-type to self.
	}
	/** returns true if the instance represents this MDM app; used for updating. */
	public boolean isMdmApp() {
		return bIsMdmApp;
	}
	
	/**
	 * Returns true if the app is the mdm app itself.
	 */
	public boolean isMdmAppSelf() {
		return (mdmAppType == MDMAPPTYPE_self);
	}

	public void setMdmAppType(int type) {
		bIsMdmApp = true;
		mdmAppType = type;
	}
	public int getMdmAppType() {
		return mdmAppType;
	}

	// 'getters': generated from within the IDE

	public String getAppName() {
		return appName;
	}
	public void setAppName(String name) {
		if (appName != name)
			definitionDirty = true;
		appName = name;
	}

	public String getVersionName() {
		return versionName;
	}
	public void setVersionName(String name) {
		versionName = name;
	}

	public int getVersionCode() {
		return versionCode;
	}
	public void setVersionCode(int code) {
		versionCode = code;
	}

	public String getVersionCodeStr() {
		return versionCodeStr;
	}
	public void setVersionCodeStr(String str) {
		versionCodeStr = str;
	}

	public String getPackageName() {
		return packageName;
	}
	
	public void setPackageName(String name) {
		if (packageName != name)
			definitionDirty = true;
		packageName = name;
	}

	public long getSizeOfPkg() {
		return sizeOfPkg;
	}

	public void setSizeOfPkg(long size) {
		sizeOfPkg = size;
	}

	public long getDataSize() {
		return dataSize;
	}
	
	public void setDataSize(long newsize) {
		dataSize = newsize;
	}

	public Drawable getAppIcon() {
		return appIcon;
	}
	public void setAppIcon(Drawable icon) {
		appIcon = icon;
	}

	public Drawable getAppLogo() {
		return appLogo;
	}
	
	public boolean isDefDirty() {
		return definitionDirty;
	}
	
	public void setDefDirty(boolean dirty) {
		definitionDirty = dirty;
	}
	
	/**
	 * Gets the name of the local package file that is to be installed.
	 * @return name and path of the package file or null if not defined (will not be an empty string).
	 */
	public String getPackageFilePath() {
		return pkgfilepath;
	}
	
	/**
	 * Sets the filepath name of the local package file that is to be installed.
	 * @param filepath File name and path of the package file. The path is on the local device.
	 */
	public void setPackageFilePath(String filepath) {
		if (filepath != null && filepath.length()==0)
			filepath = null;  // make value null instead of zero-length string
		if (pkgfilepath != filepath)
			definitionDirty = true;
		pkgfilepath = filepath;
	}
	
	/**
	 * Gets the name of the local package file that is to be installed.
	 * @return name of the package file
	 */
	public String getPackageFileName() {
		return pkgfilename;
	}
	
	/**
	 * Sets the name of the local package file that is to be installed.
	 * @param filename File name (no path) of the package file. 
	 */
	public void setPackageFileName(String filename) {
		//if (pkgfilename != filename)
			definitionDirty = true;
		pkgfilename = filename;
		if (installType==INSTALLTYPE_LOCAL)
			installSource = pkgfilepath;
	}

	/**
	 * Gets the url of the remote package file that is to be installed; the url points to an accessible file on a server.
	 * @return url name and path of the package file on a remote file server. 
	 * Contains the entire url, including file name, or null if not defined (will not be an empty string).
	 */
	public String getPackageServerUrl() {
		return pkgsvrurl;
	}
	
	/**
	 * Sets the name of the remote package file that is to be installed. The file is on a file server.
	 * @param url File name and path url of the package file on a download server.
	 */
	public void setPackageServerUrl(String url) {
		if (url != null && url.length()==0)
			url = null; // make value null instead of zero-length string
		//if (pkgsvrurl != null && url != null && !pkgsvrurl.equals(url))
			definitionDirty = true;
		pkgsvrurl = url;
		installSource = pkgsvrurl;
		//LSLogger.debug(TAG, "setPackageServerUrl="+url);
	}
	
	/**
	 * Gets the URL of the package file that is to be installed. The URL exists externally in some store.
	 * @return URL of the package file
	 */
	public String getPackageStoreUrl() {
		return storeurl;
	}

	/**
	 * Sets the URL of the package file that is to be installed. The URL exists externally in some store.
	 * @param url URL location to the package file, or null if not defined (will not be an empty string).
	 */
	public void setPackageStoreUrl(String url) {
		if (url != null && url.length()==0)
			url = null;  // make value null instead of zero-length string
		//if (storeurl != url)
			definitionDirty = true;
		storeurl = url;
		installSource = storeurl;
		//LSLogger.debug(TAG, "setPackageStoreUrl="+url);
	}
	
	// DB-persistence attributes:
	// (also, appname and packagename are persisted, but are defined already above)
	
	public void setDBID(int id) {
		appDbID = id;
	}
	
	public int getDbID() {
		return appDbID;
	}
	
	public void setMdmDbID(int id) {
		appMdmID = id;
	}
	
	public int getMdmDbID() {
		return appMdmID;
	}
	
	public void setInstallTime(long t) {
		installTime = t;
	}
	
	public long getInstallTime() {
		return installTime;
	}
	
	public void setInstallType(int type) {
		installType = type;
	}
	
	public int getInstallType() {
		return installType;
	}
	
	public void setInstallState(int state) {
		installState = state;
		displayState = null;
		// if the install state has a apptype value, set its value.
		int mdmapptype = (state & INSTALLSTATE_apptypemask);
		if (mdmapptype != 0)
			setMdmAppType(mdmapptype);
	}
	
	public int getInstallState() {
		return installState;
	}
	
	public void setUpdateTime(long t) {
		updateTime = t;
	}
	
	public long getUpdateTime() {
		return updateTime;
	}
	
	public void setUninstallTime(long t) {
		uninstallTime = t;
	}
	
	public long getUninstallTime() {
		return uninstallTime;
	}
	
	public void setSource(String source) {
		installSource = source;
		switch (installType) {		
			case INSTALLTYPE_LOCAL: pkgfilepath = source; break;
			case INSTALLTYPE_REMOTEFILE: pkgsvrurl = source; break;
			case INSTALLTYPE_ONLINESTORE: storeurl = source; break;
		}
		//LSLogger.debug(TAG, toString()+";setSource="+installSource);
	}
	
	public String getSource() {
		if (installSource == null) {
			// set source based on the install type.
			switch (installType) {		
				case INSTALLTYPE_LOCAL: installSource = pkgfilepath; break;
				case INSTALLTYPE_REMOTEFILE: installSource = pkgsvrurl; break;
				case INSTALLTYPE_ONLINESTORE: installSource = storeurl; break;
			}			
		}
		return installSource;
	}
	
	public void setReason(String reason) {
		installReason = reason;
	}
	
	public String getReason() {
		return installReason;
	}
	
	/**
	 * Returns true if an installation, update, or uninstall attempt for the app failed or was canceled.
	 */
	public boolean hasInstallError() {
		return (installReason != null);
	}
	
	
	// - install stats:
	/**
	 * Sets the time when the action to install, update, or uninstall was received. 
	 * Setting this clears the started and finished values and sets them to 0.
	 * @param t time value when the install, update, or uninstall action was received for processing.
	 */
	public void setTimeActionReceived(long t) {
		timeActionReceived = t;
		timeActionStarted = 0;
		timeActionFinished = 0;		
	}
	
	public long getTimeActionReceived() {
		return timeActionReceived;
	}
	
	/**
	 * Sets the time when the action to install, update, or uninstall was started. 
	 * @param t time value when the install, update, or uninstall action's processing began.
	 */
	public void setTimeActionStarted(long t) {
		timeActionStarted = 0;
	}
	
	public long getTimeActionStarted() {
		return timeActionStarted;
	}
	
	/**
	 * Sets the time when the action to install, update, or uninstall completed, either successfully or with error. 
	 * Setting this clears the started and finished values and sets them to 0.
	 * @param t time value when the install, update, or uninstall action's processing finished.
	 */
	public void setTimeActionFinished(long t) {
		timeActionFinished = 0;
	}
	
	public long getTimeActionFinished() {
		return timeActionFinished;
	}
	
	/**
	 * Compares the app to the given app, checking if any values which may have been 
	 * set from server command data are different, such as server url, type of source, etc.
	 * @param compare App instance to compare to.
	 * @return true if the two are the same, false if there is any difference.
	 */
	public boolean compareSourceValues(App compare) {
		boolean same = true;
		if (compare == null) {
			same = false;
		} else if (compare.getInstallType() != installType) {
			same = false;
		} else  {
			// compare source's; that's an easy way to compare the files, server urls, and store urls:
			// look for a difference in value or if one or the other is null.
			String cmpSource = compare.getSource();
			if (installSource != null && cmpSource != null && !installSource.equals(cmpSource))
				same = false;
			else if ((installSource == null && cmpSource != null) ||
				     (installSource != null && cmpSource == null))
				same = false;
		}
		return same;
	}
	
	/**
	 * Sets managed app command source values from those in the given app.
	 * This is used to change the value of the app source (file or store package name) 
	 * or where to get it from.
	 * @param app App with the values to be used and copied into this instance.
	 */
	public void setSourceValues(App app) {
		installType = app.getInstallType();
		setSource(app.getSource());
	}
	
	/**
	 * Compares the given package name to this app's package name.
	 * @param pkgCmp package name to compare to
	 * @return true if the names are the same, ignoring case; false if different or 
	 * if either package name is null.
	 */
	public boolean equalsPackageName(String pkgCmp) {
		boolean bsame = false;
		if (packageName != null && pkgCmp != null)
			bsame = packageName.equalsIgnoreCase(pkgCmp);
		return bsame;
	}
	
	/**
	 * Compares the version data between this instance and another App instance.
	 * @param cmp App instance to compare with.
	 * @return true if the version data is the same, false of there is any differences, including null 
	 * values in one instance and non-null values in another. If cmp is null, returns false.
	 */
	public boolean compareVersions(App cmp) {
		// first, check simple version codes; if they differ, we dont need to check strings
		boolean bsame = true;
		if (cmp != null) {
			bsame = (versionCode == cmp.getVersionCode());
			if (bsame) {
				// version codes are the same, but let's check version strings and version names
				String cmpverstr = cmp.getVersionCodeStr();
				String cmpvername= cmp.getVersionName();
				if ( (versionCodeStr != null && cmpverstr != null &&
						 !versionCodeStr.equalsIgnoreCase(cmpverstr)) ||
					 (versionCodeStr != null && cmpverstr == null) ||
					 (versionCodeStr == null && cmpverstr != null) ) {
					bsame = false;
				} else if ( (versionName != null && cmpvername != null && 
						        !versionName.equalsIgnoreCase(cmpvername)) ||
						 (versionName != null && cmpvername == null) ||
						 (versionName == null && cmpvername != null) ) {
					bsame = false;
				}
			}
		} else { // cmp is null, so they cant be the same.
			bsame = false;
		}
		return bsame;
	}
	
	/**
	 * Compares the storage size(s) of this app to a given app.
	 * @param cmp App instance to compare.
	 * @return true if the sizes are the same or if cmp is null, false if different.
	 */
	public boolean compareStorageSize(App cmp) {
		boolean bdiffers = true;
		if (cmp != null)
			bdiffers = (dataSize != cmp.getDataSize());
		return bdiffers;
	}
	
	/**
	 * Compares the given version string to the app's version string.
	 * @param versionCompare version string to compare to. 
	 * @return true if the app needs updating, false if not. If app version or
	 * param versionCompare string is null, assumes an update is needed.
	 */
	public boolean isUpdateNeeded(String versionCompare) {
		boolean bUpdate = true;
		if (versionCodeStr != null && versionCompare != null) {
			LSLogger.debug(TAG, "--comparing update of this version=" +versionCodeStr +" to version="+versionCompare);
			if (versionCodeStr.equals(versionCompare)) {
				// for now just assume if the versions differ, an update is needed.
				bUpdate = false;  // false means versions are the same, so we dont need updating.
			} else {
				// ..otherwise, compare the values and update only if newer				
				/* 
				// Version should be MM.mm.bbbbl
				// MM=major number .  mm=minor number . bbbb=calendar date l=revision letter
				String[] vthis = versionCodeStr.split(".");
				String[] vcomp = versionCompare.split(".");
				*/
			}
		}
		return bUpdate;
	}
	
	// -------------------------------------------------------------
	// --- convenience methods for state and processing transitions:
	// -------------------------------------------------------------
	
	/**
	 * Gets the pending state of the app.
	 * @return true if an install, update, or uninstall is pending.
	 */
	public synchronized boolean isStatePending() {
		return ((installState & INSTALLSTATE_installpending)!=0 || 
				(installState & INSTALLSTATE_uninstallpending)!=0 ||
				(installState & INSTALLSTATE_installpending)!=0);
	}
	
	/**
	 * Gets the processing state of the app.
	 * @return true if an install, update, or uninstall is currently underway.
	 */
	public synchronized boolean isStateProcessing() {
		return ((installState & INSTALLSTATE_installing)!=0 || 
				(installState & INSTALLSTATE_uninstalling)!=0 ||
				(installState & INSTALLSTATE_installing)!=0);
	}
	
	public synchronized boolean isStateInstalling() {
		return ((installState & INSTALLSTATE_installpending)!=0 || 
				(installState & INSTALLSTATE_installdownloading)!=0 || 
				(installState & INSTALLSTATE_installing)!=0);
	}
	
	public synchronized boolean isStateInstalled() {
		return ((installState & INSTALLSTATE_installed)!=0);
	}

	public synchronized boolean isStateUpdating() {
		return ((installState & INSTALLSTATE_updatepending)!=0 ||
				(installState & INSTALLSTATE_updatedownloading)!=0 || 
				(installState & INSTALLSTATE_updating)!=0);
	}
	
	public synchronized boolean isStateUpdated() {
		return ((installState & INSTALLSTATE_updated)!=0);
	}
	
	public synchronized boolean isStateUninstalling() {
		return ((installState & INSTALLSTATE_uninstallpending)!=0 || 
				(installState & INSTALLSTATE_uninstalling)!=0);
	}

	public synchronized boolean isStateUninstalled() {
		return ((installState & INSTALLSTATE_uninstalled)!=0);
	}

	/**
	 * Initializes state and time values for an install or update. 
	 * This is called when an install command is received from the MDM server and the process
	 * to install gets queued up for processing. This sets the state to installpending and clears
	 * previous error and stat values, setting time-markers to the current time.
	 */
	public synchronized void setInstallCommandReceivedState() {
		long currentTime = System.currentTimeMillis();
		// clear any prior current-processing values (stats, error info, etc.):
		installReason = null;
		clearErrorStateFlags();
		
		// set proper state. If the app is installed or was previously updated, this is an update:
		// if this is a new instance, with no prior data nor a db id, then we know it's a new app install.
		if (!bIsMdmApp && (appDbID == 0 || installTime == 0 || 
				(((installState & INSTALLSTATE_installed)==0) && !isStateInstalling())) ) {
			installTime = currentTime;
			setTimeActionReceived(currentTime);
			installState &= (~INSTALLSTATE_installmask); // clear any previous install failure attempts
			installState |= INSTALLSTATE_installpending;
		} else { // we're updating
			updateTime = currentTime;
			installState &= (~INSTALLSTATE_updatemask);
			installState |= INSTALLSTATE_updatepending;
		}
		displayState = null;
		displayTime = null;
	}
	
	/**
	 * Set other internal values for indicating either an install or update's processing is starting.
	 * Install or update was already queued up, so the app's state is already set up to indicate install
	 * or update.
	 */
	public synchronized void setInstallProcessingStartedState() {		
		clearErrorStateFlags();
		if ((installState & INSTALLSTATE_installpending) != 0) { // then we're installing
			installState -= INSTALLSTATE_installpending;
			installState |=	INSTALLSTATE_installing;
		} else if ((installState & INSTALLSTATE_installdownloading) != 0) { // then we're installing
				installState -= INSTALLSTATE_installdownloading;
				installState |=	INSTALLSTATE_installing;
		} else if ((installState & INSTALLSTATE_updatepending) != 0) {  // we're updating
			installState -= INSTALLSTATE_updatepending;
			installState |=	INSTALLSTATE_updating;
			setTimeActionReceived(updateTime);
		} else if ((installState & INSTALLSTATE_updatedownloading) != 0) {  // we're updating
			installState -= INSTALLSTATE_updatedownloading;
			installState |=	INSTALLSTATE_updating;
		}
		timeActionStarted = System.currentTimeMillis();
		displayState = null;
		displayTime = null;
	}

	/**
	 * Set other internal values for indicating either an install or update's file downloading is starting.
	 * Install or update was already queued up, so the app's state is already set up to indicate install
	 * or update.
	 */
	public synchronized void setInstallDownloadingState() {		
		clearErrorStateFlags();
		if ((installState & INSTALLSTATE_installpending) != 0) { // then we're installing
			installState -= INSTALLSTATE_installpending;
			installState |=	INSTALLSTATE_installdownloading;
		} else if ((installState & INSTALLSTATE_updatepending) != 0) {  // we're updating
			installState -= INSTALLSTATE_updatepending;
			installState |=	INSTALLSTATE_updatedownloading;
			setTimeActionReceived(updateTime);
		}
		displayState = null;
		displayTime = null;
	}

	/**
	 * Sets the install or update completion state and values. 
	 * @param completionState an INSTALLSTATE_ value to set the completion state to.
	 * @param reason optional error or reason why the install did not properly finish (error, cancel, etc.)
	 */
	public synchronized void setInstallCompletedState(int completionState, String reason) {
		timeActionFinished = System.currentTimeMillis();
		clearErrorStateFlags();
		if ((installState & INSTALLSTATE_installing) != 0) { // then we're installing
			installState -= INSTALLSTATE_installing;
			installState |=	completionState;
		} else if ((installState & INSTALLSTATE_installdownloading) != 0) { // downloading installer
				installState -= INSTALLSTATE_installdownloading;
				installState |=	completionState;
		} else if ((installState & INSTALLSTATE_updating) != 0) {  // we're updating
			installState -= INSTALLSTATE_updating;
			installState |=	completionState;
		} else if ((installState & INSTALLSTATE_updatedownloading) != 0) {  // downloading an update
			installState -= INSTALLSTATE_updatedownloading;
			installState |=	completionState;
		} else
			installState |=	completionState;
		installReason = reason;  
		displayState = null;
		displayTime = null;
	}
	
	/**
	 * Sets the update completion state and values. 
	 * @param completionState an INSTALLSTATE_ value to set the completion state to.
	 * @param reason optional error or reason why the install did not properly finish (error, cancel, etc.)
	 */
	public synchronized void setUpdateCompletedState(int completionState, String reason) {
		timeActionFinished = System.currentTimeMillis();
		clearErrorStateFlags();
		if ((installState & INSTALLSTATE_updating) != 0) {  // we're updating
			installState -= INSTALLSTATE_updating;
			installState |=	completionState;
		} else if ((installState & INSTALLSTATE_updatedownloading) != 0) {  // we're updating
			installState -= INSTALLSTATE_updatedownloading;
			installState |=	completionState;	
		} else
			installState |=	completionState;
		installReason = reason;  
		displayState = null;
		displayTime = null;
	}
	
	/**
	 * Initializes state and time values for an install or update. 
	 * This is called when an install command is received from the MDM server and the process
	 * to install gets queued up for processing. This sets the state to uninstallpending and clears
	 * previous error and stat values, setting time-markers to the current time.
	 */
	public synchronized void setUninstallCommandReceivedState() {
		long currentTime = System.currentTimeMillis();
		// clear any prior current-processing values (stats, error info, etc.):
		clearErrorStateFlags();
		if (!isStateUninstalling()) {
			installReason = null;
			uninstallTime = currentTime;
			installState &= (~INSTALLSTATE_uninstallmask); // clear any failed uninstall attempt flags
			installState |= INSTALLSTATE_uninstallpending;
		}
		displayState = null;
		displayTime = null;
	}
	
	/**
	 * Set other internal values for indicate when an uninstall is starting.
	 * Install or update was already queued up, so the app's state is already set up to 
	 * indicate an uninstall.
	 */
	public synchronized void setUninstallProcessingStartedState() {
		clearErrorStateFlags();
		if ((installState & INSTALLSTATE_uninstallpending) != 0) { // then we're uninstalling
			installState -= INSTALLSTATE_uninstallpending;
			installState |=	INSTALLSTATE_uninstalling;
			setTimeActionReceived(uninstallTime);		
			timeActionStarted = System.currentTimeMillis();
		}
		displayState = null;
		displayTime = null;
	}
	
	/**
	 * Sets the uninstall completion state and values. 
	 * @param completionState an INSTALLSTATE_ value to set the completion state to.
	 * @param reason optional error or reason why the uninstall did not properly finish (error, cancel, etc.)
	 */
	public synchronized void setUninstallCompletedState(int completionState, String reason) {
		timeActionFinished = System.currentTimeMillis();
		clearErrorStateFlags();
		if ((installState & INSTALLSTATE_uninstalling) != 0) { // then we're uninstalling
			installState -= INSTALLSTATE_uninstalling;
			installState |=	completionState;
		} else
			installState |=	completionState;
		installReason = reason;  
		displayState = null;
		displayTime = null;
	}
	
	//local method to remove any error flags
	private void clearErrorStateFlags() {
		if ((installState & INSTALLSTATE_installerror) != 0) 
			installState -= INSTALLSTATE_installerror;
		if ((installState & INSTALLSTATE_updateerror) != 0) 
			installState -= INSTALLSTATE_updateerror;
		if ((installState & INSTALLSTATE_uninstallerror) != 0) 
			installState -= INSTALLSTATE_uninstallerror;
	}
	
	/**
	 * Gets a displayable name for the App. 
	 * @return Gets the appName if present, or packageName if present, or toString if neither are present.
	 */
	public String getDisplayName() {
		if (appName != null)
			return appName;
		if (packageName != null)
			return packageName;
		return toString();
	}
	
	/**
	 * Gets the time of the current completed action; applicable to installtime, updatetime, canceltime.
	 * @return
	 */
	public String getDisplayInstallTime() {
		return displayTime;
	}
	
	/**
	 * Gets a displayable description of the install state. 
	 * @param context application context, used to get appropriate resource strings.
	 * @return status of the app, or null if the state can't be resolved.
	 */
	public String getDisplayInstallState(Context context) {
		// note: the displayState attribute is cleared upon a state change. once it is set and the
		// state does not change, we don't need to look it up again. 
		//LSLogger.debug(TAG, "DisplayInstallState for App: "+toString());
		if ( displayState == null ) {
			// retrieve an appropriate status string for the install/update/uninstall state.
			Resources res = context.getResources();
			int state = installState;
			displayTime = null;
			// show any erros first:
			if ((state & App.INSTALLSTATE_installerror)!=0) {
				displayState = res.getString(R.string.status_installfailed);
				displayTime = Utils.formatLocalizedDateUTC(uninstallTime);
			} else if ((state & App.INSTALLSTATE_updateerror)!=0) {
				displayState = res.getString(R.string.status_updatefailed);
				displayTime = Utils.formatLocalizedDateUTC(updateTime);
			} else if ((state & App.INSTALLSTATE_uninstallerror)!=0) {
				displayState = res.getString(R.string.status_uninstallfailed);
				displayTime = Utils.formatLocalizedDateUTC(uninstallTime);
			 
			// show any active status next:
			} else if ((state & App.INSTALLSTATE_uninstalling)!=0) {
				displayState = res.getString(R.string.status_uninstalling);
				displayTime = Utils.formatLocalizedDateUTC(uninstallTime);
			} else if ((state & App.INSTALLSTATE_updating)!=0) {
				displayState = res.getString(R.string.status_updating);
				displayTime = Utils.formatLocalizedDateUTC(updateTime);
			} else if ((state & App.INSTALLSTATE_installing)!=0) {			
				displayState = res.getString(R.string.status_installing);
				displayTime = Utils.formatLocalizedDateUTC(installTime);
				
			} else if ((state & App.INSTALLSTATE_updatedownloading)!=0) {
				displayState = res.getString(R.string.status_updatedownloading);
				displayTime = Utils.formatLocalizedDateUTC(updateTime);
			} else if ((state & App.INSTALLSTATE_installdownloading)!=0) {			
				displayState = res.getString(R.string.status_installdownloading);
				displayTime = Utils.formatLocalizedDateUTC(installTime);
				
			// show any pending status next:
			} else if ((state & App.INSTALLSTATE_uninstallpending)!=0) {
				displayState = res.getString(R.string.status_uninstallpending);
			} else if ((state & App.INSTALLSTATE_updatepending)!=0) {
				displayState = res.getString(R.string.status_updatepending);
				displayTime = Utils.formatLocalizedDateUTC(updateTime);
			} else if ((state & App.INSTALLSTATE_installpending)!=0) {		
				displayState = res.getString(R.string.status_installpending);
				displayTime = Utils.formatLocalizedDateUTC(installTime);
				
			// lastly show current static install state.
			} else if ((state & App.INSTALLSTATE_uninstalled)!=0) {
				displayState = res.getString(R.string.status_uninstalled);
				displayTime = Utils.formatLocalizedDateUTC(uninstallTime);
			} else if ((state & App.INSTALLSTATE_uninstallcancelled)!=0) {
				displayState = res.getString(R.string.status_uninstallcanceled);
				displayTime = Utils.formatLocalizedDateUTC(uninstallTime);

			} else if ((state & App.INSTALLSTATE_updated)!=0) {
				displayState = res.getString(R.string.status_updated);
				displayTime = Utils.formatLocalizedDateUTC(updateTime);
			} else if ((state & App.INSTALLSTATE_updatecancelled)!=0) {
				displayState = res.getString(R.string.status_updatecanceled);
				displayTime = Utils.formatLocalizedDateUTC(updateTime);
				
			} else if ((state & App.INSTALLSTATE_installed)!=0) {
				displayState = res.getString(R.string.status_installed);
				displayTime = Utils.formatLocalizedDateUTC(installTime);
			} else if ((state & App.INSTALLSTATE_installcancelled)!=0) {
				displayState = res.getString(R.string.status_installcanceled);
				displayTime = Utils.formatLocalizedDateUTC(installTime);
			} else {
				// for default, just show a hex representation of the state:
				displayState = "(" + Integer.toHexString(installState) + ")";
			}
		}
		return displayState;
	}
	
	/**
	 * Gets a string representation of the App instance.
	 */
	public String toString() {
		return super.toString() + " (app="+packageName+" state="+Integer.toHexString(installState)+" dbid="+Integer.toString(appDbID)+")";
	}
	
	/**
	 * Gets an App instance for the current MDM app, including the package file location.
	 * @param context application context
	 * @return app instance if successfull, null otherwise.
	 */
	public static App getMdmAppPackage(Context context) {
		App mdmapp = null;
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(Constants.PACKAGE_NAME, 0);
			mdmapp = new App(packageInfo, context, false);
			mdmapp.setMdmAppType(MDMAPPTYPE_self);
			mdmapp.setInstallType(INSTALLTYPE_LOCAL);
			String mdmpath =  packageInfo.applicationInfo.sourceDir;
			mdmapp.setPackageFilePath(mdmpath);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "GetMDMAppPkg error:", ex);
		}
		return mdmapp;
	}
}
