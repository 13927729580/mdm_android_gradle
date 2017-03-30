package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;

/**
 * Collection and convenience methods for applications (App instances).
 * This base class is implemented to support managed apps. Subclassed implementations (ie, DeviceApps) 
 * provide methods for other collections of apps and relevant data.
 */
public class Apps {
	private static String TAG = "Apps";
	
	protected Vector<App> apps;
	protected Context context;
	protected AppsDataChangeListener dataObserver; // observer for watching for data changes; note that we should use a list of these.

	/**
	 * Constructor. Apps will be loaded or added in the current thread. 
	 * @param context application context, needed for getting application data.
	 */
	public Apps(Context context) {
		this.context = context;
		apps = new Vector<App>(10);
	}
	
	/**
	 * Adds the observer for data change notifications.
	 * @param observer instance to call back to upon changes to data.
	 */
	public void registerDataChangeListener(AppsDataChangeListener observer) {
		dataObserver = observer;
	}
	
	/**
	 * Removed the observer for data change notifications.
	 * @param observer instance to call back to upon changes to data.
	 */
	public void unregisterDataChangeListener(AppsDataChangeListener observer) {
		if (dataObserver == observer)
			dataObserver = null;
	}
	
	// completes any pending or in-process updates, such as self-updating
	protected void completePendingUpdates() {
		App mdmapp = findAppByPackageName(Constants.PACKAGE_NAME, 0);
		if (mdmapp!= null && mdmapp.isStateUpdating()) {
			LSLogger.debug(TAG, "Completing MDM update status.");
			setInstallCompletionResults(mdmapp, Constants.ACTIONID_INSTALL, Activity.RESULT_OK);
			saveApp(mdmapp);
		}
	}
	
	/**
	 * Gets the count of the number of apps in the list.
	 * @return number of app instances, 0 if none.
	 */
	public int getAppsCount() {
		return apps.size();
	}
	
	/**
	 * Gets the list of apps. Note that the list may be empty if nothing was done to read them.
	 * @return
	 */
	public List<App> getApps() {
		return apps;
	}
	
	/**
	 * Gets the App at the specified index position.
	 * @param index 0-based index into the collection
	 * @return the App instance at the location, or null of index is out of bounds or no App exists at that index.
	 */
	public App getAppAt(int index) {
		if (apps==null || apps.isEmpty() || index >= apps.size() || index < 0)
			return null;
		return apps.elementAt(index);
	}
	
	/**
	 * Adds the App instance to the list of applications.
	 * @param app App instance
	 */
	public void addApp(App app) {
		apps.add(app);
	}
	
	/**
	 * Search for an existing App instance with a given database ID. 
	 * @param id local database ID of the app to look for.
	 * @return app instance if found, null if not found.
	 */
	public App findAppByID(int id) {
		App app = null;
		// search apps list for the given app record.	
		Iterator<App> iter = apps.iterator();
		while (iter.hasNext()) {
			app = iter.next();
			if (app.getDbID() == id)
				break;
			else
				app = null;
		}
		return app;
	}
	
	/**
	 * Search for an existing App instance with a given package name. 
	 * @param packageName name of the package to look for.
	 * @param excludeState if not 0, excludes any instances with the flag mask given. 
	 * For example, using App.INSTALLSTATE_uninstallmask will exclude any app that 
	 * has any uninstall flags set, thereby preventing finding an instance marked for
	 * or undergoing an uninstall.
	 * @return app instance if found, null if not found.
	 */
	public App findAppByPackageName(String packageName, int excludeState) {
		return findAppByPackageName(apps, packageName, excludeState);
	}

	/**
	 * Search for an existing App instance with a given package name within a given existing list.
	 * @param list vector of Apps instances to search. Can be null and can be empty.
	 * @param packageName name of the package to look for.
	 * @param excludeState if not 0, excludes any instances with the flag mask given. 
	 * For example, using App.INSTALLSTATE_uninstallmask will exclude any app that 
	 * has any uninstall flags set, thereby preventing finding an instance marked for
	 * or undergoing an uninstall.
	 * @return app instance if found, null if not found.
	 */
	protected App findAppByPackageName(Vector<App> list, String packageName, int excludeState) {
		App app = null;
		String name;
		// search given list for the given app record.	
		if (list != null && list.size()>0) {
			Iterator<App> iter = list.iterator();
			while (iter.hasNext()) {
				app = iter.next();
				name = app.getPackageName();
				if (name != null && name.equalsIgnoreCase(packageName)) {
					if (excludeState == 0) // we match on any name match.
						break;
					else if ((excludeState & app.getInstallState()) == 0) {
						break; // none of the excludeState flags match, so we found it
					}
				}
				app = null;
			}
		}
		return app;
	}
	
	
	/**
	 * Reads all applications from the system.
	 * @return list of App instances, each instance representing an app.
	 */
	public List<App> getAllInstalledApps() {
		try {
			List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
			if (packages != null && !packages.isEmpty()) {
				LSLogger.debug(TAG, "Loaded " + packages.size() + " app packages.");
				// get each package and create an app instance from it.
				Iterator<PackageInfo> iter = packages.iterator();
				while (iter.hasNext()) {
					PackageInfo pkg = (PackageInfo)iter.next();
					if (pkg.versionName != null) 
						addApp(new App(pkg, context, true));
				}
			} else {
				LSLogger.debug(TAG, "No app packages found.");
			}
		} catch (Exception ex) {
			LSLogger.exception(TAG, "GetAllInstalledApps error: ", ex);
		}
		//notifyObservers();
		return apps;
	}

	/**
	 * Gets the list of managed applications.
	 * @return list of App instances, each instance representing a managed app.
	 */
	public List<App> getManagedApps() {
		//notifyObservers();
		return apps;
	}
	
	/**
	 * Loads the list of managed applications into this instance.
	 * @return returns list of the apps.
	 */
	public List<App> loadManagedApps() {
		apps.clear();
		try {
			AppsDB db = new AppsDB(context);
			db.readAppRecords(apps, DBStorage.DBORDER_Ascending, null);
			db.close();			
			LSLogger.debug(TAG, "Number of Managed apps found = " + apps.size());
		} catch (Exception ex) {
			LSLogger.exception(TAG, "LoadManagedApps error: ", ex);
		}
		
		// Note: we loaded the list of managed apps from the database; but, it is possible
		//  that the app was uninstalled externally from settings, manually.
		// We therefore need to check each app to make sure it is still installed, and if
		//  not, we can delete it from the database, or at least mark it as uninstalled.
		// This is needed to keep the managed apps list accurate as to what is installed on the device.
		
		// .. to be done..
		
		//notifyObservers();
		return apps;
	}

	/**
	 * Notifies any observers of a change in the data.
	 */
	private void notifyObservers() {
		if (dataObserver != null) {
			//LSLogger.debug(TAG, "notifying dataObserver of AppsDataChanged");
			dataObserver.onAppsDataChanged();
		}		
	}
	
	/**
	 * Interface for calling back an app data change event to.
	 * @author mikezrubek
	 *
	 */
	public interface AppsDataChangeListener {
		public void onAppsDataChanged();
	}
	
	// -----------------------------------------------------------------------
	// --- Methods for managing an app, such as installing, removing, etc. ---
	// -----------------------------------------------------------------------

	private void deleteApp(App app) {
		try {
			if (app != null && app.getDbID() != 0) {
				AppsDB db = new AppsDB(context);
				db.deleteApp(app);
				db.close();			
			}
		} catch (Exception ex) {
			app.setReason( LSLogger.exception(TAG, "deleteApp error: ", ex) );
		}
		apps.remove(app);
	}
	
	/**
	 * Saves updates to an app instance. If the app is new, added to the apps list as well.
	 * @param app
	 */
	public void saveApp(App app) {
		try {
			if (app != null) {
				AppsDB db = new AppsDB(context);
				if (app.getDbID() == 0) { // adding new instance
					addApp(app);
					db.insert(app);
				} else {
					if (db.updateAppValues(app) == 0)  // app wasn't found, so try to insert it
						db.insert(app);
				}
				db.close();		
				//LSLogger.debug(TAG, "Saved app "+app.toString());
			}
		} catch (Exception ex) {
			app.setReason( LSLogger.exception(TAG, "saveApp error: ", ex) );
		}
		notifyObservers();
	}

	public void uninstallComplete(App app) {
		deleteApp(app);
		notifyObservers();
	}
	
	/**
	 * Gets app's details from the database. Assumes the app instance has data needed for finding it,
	 * such as a DBID, package name, app name, or mdm DB ID.
	 * @param context
	 * @param app
	 * @return true if the app's data was found, false if not or if error, with error info 
	 * stored in the app's installReason attribute.
	 */
	/*
	public static boolean getLatestAppFromDB(Context context, App app) {
		boolean found = false;
		try {
			if (context == null)
				context = Utils.getApplicationContext();
			if (app != null) {
				AppsDB db = new AppsDB(context);
				found = db.readApp(app);
				db.close();			
			}
		} catch (Exception ex) {
			app.setReason( LSLogger.exception(TAG, "getLatestAppFromDB error: ", ex) );
		}
		return found;		
	}
	*/
	
	/**
	 * Installs the given application or initiates the process to begin application installation.
	 * The package file with the App instance is assumed to point to the local installer file.
	 * @param app application instance, containing the package name and file path of the app to be installed.
	 * @param results installation progress or success. 
	 * @return true if the application was installed or the installation process was started, false if an error occurred. 
	 */
	public static boolean installPackageFile(App app, CommandResult results) {
		String  appUrlStr = "file://"+app.getPackageFilePath();
		return installPackage(app, appUrlStr, results);
	}
	
	/**
	 * Installs an app from a store URL, such as from Google Play or some other online app store.
	 * @param app App instance to be installed
	 * @param results output results instance, gets the results of the installation operation.
	 * @return true if installed or the installation process was started, false if an error occurred.
	 */
	public boolean installPackageFromStore(App app, CommandResult results) {
		String googleStoreUrlReference = "market://details?id=";
		String appUriStr = null;
		String storeurl = app.getPackageStoreUrl();
		if (storeurl != null) {
			// if we have a store URL and it has the ://, meaning it is a full url, we'll use it.
			if (storeurl.contains("://"))
				appUriStr = storeurl;
			else  // otherwise use the store url a a google play reference
				appUriStr = googleStoreUrlReference+storeurl;
		} else {
			// no store url given; default to using the package name to find it on google play:
	  	  	appUriStr = googleStoreUrlReference+app.getPackageName();
		}

		app.setInstallProcessingStartedState();
		// save the app's state; we have to do it here since processing may take awhile
		// and we want the app's state to be accurate as soon as possible.
		saveApp(app);

		return installPackage(app, appUriStr, results);
	}

	/**
	 * Installs the given application or initiates the process to begin application installation.
	 * Uses the appUrlStr to identify the app to be installed, which may be a local file or remote store url.
	 * @param app application instance, containing the package name and information about the app to be installed.
	 * @param appUriStr string for the app the be installed. Is file://packagename or market://details?id=package_name_market_id.
	 * @param results installation progress or success. 
	 * @return true if the application was installed or the installation process was started, false if an error occurred. 
	 */
	private static boolean installPackage(App app, String appUriStr, CommandResult results) {
		boolean bInstalled = false;
		
		/**
		if (checkInstallPermission()) {
			// we have direct installation permission; directly perform the installation:
			// ... to do ...
		}
		**/
		
		if (!bInstalled) { // revert to the default interactive installation process:
			try {
			
				Uri fileuri = Uri.parse(appUriStr);
				 
				// code to directly trigger our 'installer' activity internally:
				Intent intent = new Intent(Constants.ACTION_APPINSTALL);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setData(fileuri);
				intent.setClass(Utils.getApplicationContext(), AppInstaller.class);
				intent.putExtras(app.getBundle());
				//intent.putExtra(App.INTENT_EXTRA_APPDATA, app.getBundle());
				

				/*
				// code to kick-off a request to use our activity as the preferred one for the app:
				//Intent intent = new Intent("com.lightspeedsystems.mdm.INSTALLAPP");
				Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_FROM_BACKGROUND|Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				intent.setDataAndType(fileuri, "application/vnd.android.package-archive");
				intent.addCategory("com.lightspeedsystems.mdm");
				intent.setClassName(Constants.PACKAGE_NAME, Constants.PACKAGE_NAME+".AppInstaller") ;
				Uri uri = intent.getData();
				LSLogger.debug(TAG, "Uri in intent="+(uri==null?"(null)":uri.toString()));
				*/
				
				 
				// code for directly-starting the default interactive installer process; 
				// This prompts the user to confirm installing or updating the app.
				/*
				Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
				intent.setType("application/vnd.android.package-archive");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
						| Intent.FLAG_FROM_BACKGROUND
						| Intent.FLAG_ACTIVITY_FORWARD_RESULT);
				intent.setData(fileuri);
				*/
				 
				
				//LSLogger.debug(TAG, "Starting activity to install uri " + fileuri.toString());
				LSLogger.debug(TAG, " -intent: " + intent.toString());
				Utils.getApplicationContext().startActivity(intent);
				
				results.setPending(true);
				bInstalled = true;
				LSLogger.debug(TAG, "Intent sent to run install in AppInstall Activity.");

			} catch (Exception ex) {
				results.setException(ex);
				app.setReason( LSLogger.exception(TAG, "Install App error: ", ex) );
			}
		}
		
		return bInstalled;
	}	
	
	
	/**
	 * Uninstalls an application. Similar to installing, this may use a non-interactive process
	 * or the default android interactive process of prompting the user before uninstalling.
	 * 
	 * @param app app to be uninstalled
	 * @param results results of the uninstall or error information.
	 * @return true if the app was uninstalled or the unintall process started, false if error.
	 */
	public static boolean uninstallPackage(App app, CommandResult results) {
		boolean bUninstalled = false;
		
			/**
			if (checkInstallPermission()) {
				// we have direct installation permission; directly perform the installation:
				// ... to do ...
			}
			**/
			
			if (!bUninstalled) { // revert to the default uninstall process:
				try {
				
					Uri fileuri = Uri.parse(app.getPackageName());
					 
					// code to directly trigger our 'installer' activity internally:
					Intent intent = new Intent(Constants.ACTION_APPUNINSTALL);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setData(fileuri);
					intent.setClass(Utils.getApplicationContext(), AppInstaller.class);
					intent.putExtras(app.getBundle());
					//intent.putExtra(App.INTENT_EXTRA_APPDATA, app.getBundle());
					
					LSLogger.debug(TAG, " -intent: " + intent.toString());
					Utils.getApplicationContext().startActivity(intent);
					
					results.setPending(true);
					bUninstalled = true;
					LSLogger.debug(TAG, "Intent sent to run uninstall in AppInstall Activity.");

				} catch (Exception ex) {
					app.setReason( LSLogger.exception(TAG, "Unistall App error: ", ex) );
				}
			}
		return bUninstalled;
	}

	/**
	 * Installs an app from a remote file that is to be downloaded to the device and then installed.
	 * The file must be a .apk that can be run and installed. 
	 * 
	 * Downloads to any place that can accept local file storage; 
	 * preference is to cache and local data storage, but will also download to a SD card.
	 * @param app app to be installed. This instance contains the URL to the file on some server.
	 * @param results operational results object. Used to indicate success or error and for error details.
	 */
	public void downloadAndInstallApp(App app, CommandResult results) {
		try {
			// update the state to indicate the file is being downloaded:
			app.setInstallDownloadingState();
			saveApp(app);
			// create the complete name of the file for the local file name:
			File path = Utils.getPreferredDownloadDir();
			if (path == null || !path.exists() || !path.canWrite()) {
				LSLogger.debug(TAG, "-preferred download path is "+path.toString());
				if (path != null){
					LSLogger.debug(TAG, "-path exists="+path.exists());
					LSLogger.debug(TAG, "-path can write="+path.canWrite());
				}
				LSLogger.debug(TAG, "-using alternate download path.");
				path = Utils.getAlternateDownloadDir();
			}
			if (path == null || !path.exists() || !path.canWrite()) {
				LSLogger.debug(TAG, "-using SDCard download path.");
				path = Utils.getSDCardDownloadDir();
			}
			if (path == null) {
				results.setErrorMessage("Unable to access a download path on device.");
				LSLogger.error(TAG, results.getErrorMessage());
			} else {
				// make sure the path exists
				if (!path.exists())
					path.mkdirs();	
				
				// Build the full target path to the download file:
				// -For the download file name, use the specific-file name, but if not present, default to
				//  the package name being the name of the file.
				String packageFileName = app.getPackageFileName();
				if (packageFileName == null)
					packageFileName = app.getPackageName();
				// -build the path to the file:
				String downloadFileUristr = path.getPath();
				downloadFileUristr = Utils.endStringWith(downloadFileUristr, "/") + packageFileName;
				// -save the modified file name back into the app instance for later reference:
				app.setPackageFilePath(downloadFileUristr);
		
				// Get the location of the file on the server:
				String serverUrl = app.getPackageServerUrl();
				//LSLogger.debug(TAG, app.toString()+"; serverurl="+serverUrl);
				if (serverUrl == null) {
					Settings settings = Controller.getInstance().getSettingsInstance();
					serverUrl = settings.getSetting(Settings.MDM_FILEDOWNLOAD_URL);
					if (serverUrl == null)
						serverUrl = settings.getServerUrl();
					if (serverUrl != null) 
						serverUrl = Utils.endStringWith(serverUrl, "/") + packageFileName;
				}

				if (serverUrl == null) {
					results.setErrorMessage("Server Download URL not defined.");
					LSLogger.error(TAG, results.getErrorMessage());
					
				} else {
					serverUrl=serverUrl.replace("[","%5B");
					serverUrl=serverUrl.replace("]","%5D");
					// Download the file:
					HttpComm httpComm = new HttpComm();
					HttpCommResponse resp = httpComm.downloadFile(serverUrl, downloadFileUristr);
					if (resp.isOK()) {
						LSLogger.debug(TAG, "-downloaded file "+downloadFileUristr);
						// Upon successful file download, now invoke the installation process:
						// first, update the state noe that downloading is complete.
						app.setInstallProcessingStartedState();
						saveApp(app);
						// and now, kick off the installation of the file:
						results.setSuccess( Apps.installPackageFile(app, results) );
					} else {
						// download failed; error info should be in the response data:
						if (resp.hasException())
							results.setException(resp.getException());
						else
							results.setErrorMessage(resp.getResultReason());	
						LSLogger.error(TAG, "!- download file failed: "+results.getErrorMessage());
					}
				}
			}
		} catch (Exception ex) {
			results.setException(ex);
			app.setReason( LSLogger.exception(TAG, "AppInstall-DownloadFile error: ", ex) );
		}

	}
	
	/**
	 * Handles the cleanup and settings after an install-related action (install, update, or uninstall).
	 * Sets the flags and status in the app, and saves the changes. Also creates an Event for the action.
	 * 
	 * @param app App that was being installed, updated, or uninstalled.
	 * @param actionCode a Constants.ACTIONID_ value (ACTIONID_INSTALL or ACTIONID_UNINSTALL).
	 * @param resultCode one of the valid Activity.RESULT_ values: OK, CANCELED, or any other
	 *  value (including FIRST_USER, Constants.ACTIONID_ERROR, etc) for error conditions.
	 */
	public void setInstallCompletionResults(App app, int actionCode, int resultCode) {
    	Event event = null;    	

		//LSLogger.debug(TAG, "InstallCompletioResults - action="+actionCode+" result="+resultCode);

		if (actionCode == Constants.ACTIONID_INSTALL) {
			boolean installedOK = false;
			if (app.isStateInstalling()) { 
				//LSLogger.debug(TAG, "InstallCompletioResults - installing: result="+resultCode);
    			// application was installed or install failed; add to the managed app database and log an event.
    			switch (resultCode) {
    				case Activity.RESULT_CANCELED:
    					// app results were received as 'cancelled', but, if the app is installed, 
    					//  let's set the state as installed as well as cancelled (so the flags are right):
    					//  ...this can happen from from some remote store-downloaded apps, where the result
    					//    is received as cancelled, but the app actually got installed.
    					app.setInstallCompletedState(App.INSTALLSTATE_installcancelled, "Install cancelled by user.");
    					if (!Apps.isAppInstalled(context, app.getPackageName())) {
	    					event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_add, 
									  R.string.event_appinstallcancelled, app.getPackageName(), null, 0);
	    					break;
    					}
    					// else the app is already isntalled, so let it fall through the installed state:
    				case Activity.RESULT_OK:
    					app.setInstallCompletedState(App.INSTALLSTATE_installed, null);
    					event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_add, 
								  R.string.event_appinstalled, app.getPackageName(), null, 0);
    					installedOK = true;
    					break;
    				default: // anything else must have been an error.
     					app.setInstallCompletedState(App.INSTALLSTATE_installerror, app.getReason());
    					event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_add, 
								  R.string.event_appinstallfailed, app.getPackageName(), app.getReason(), 0);
    					break;
    			}
    		} else if (app.isStateUpdating()) {
				//LSLogger.debug(TAG, "InstallCompletioResults - updating: result="+resultCode);
    			// application was updated or update failed; add to the managed app database and log an event.
    			switch (resultCode) {
 					case Activity.RESULT_CANCELED:
    					app.setUpdateCompletedState(App.INSTALLSTATE_updatecancelled, "Update cancelled by user.");
    					event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_update, 
								  R.string.event_appupdatecancelled, app.getPackageName(), null, 0);
    					break;
       				case Activity.RESULT_OK:
    					app.setUpdateCompletedState(App.INSTALLSTATE_updated, null);
    					event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_update, 
    									  R.string.event_appupdated, app.getPackageName(), null, 0);
    					installedOK = true;
    					break;
    				default: // anything else must have been an error.
     					app.setUpdateCompletedState(App.INSTALLSTATE_updateerror, app.getReason());
    					event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_update, 
								  R.string.event_appupdatefailed, app.getPackageName(), app.getReason(), 0);
    					break;
    			}	    			
    		} else {
    			LSLogger.error(TAG, "Unknown app state in setInstallCompletionResults. app="+app.toString());
    		}
			if (installedOK) {
				// some of the app values we need to make sure we have, but can get from an installed pkg.
				if (app.getAppName() == null) {
					app.loadPackageInfo(null, context);
				}
			}
			saveApp(app);	    
			
    	} else if (actionCode == Constants.ACTIONID_UNINSTALL) {
			switch (resultCode) {
				case Activity.RESULT_OK:
					app.setUninstallCompletedState(App.INSTALLSTATE_uninstalled, null);
					event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_delete, 
							  R.string.event_appuninstalled, app.getPackageName(), null, 0);
					uninstallComplete(app);
					break;
				case Activity.RESULT_CANCELED:
					app.setUninstallCompletedState(App.INSTALLSTATE_uninstallcancelled, "Uninstall cancelled by user.");
					event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_delete, 
							  R.string.event_appuninstallcancelled, app.getPackageName(), null, 0);
					saveApp(app);
					break;
				default: // anything else must have been an error.
 					app.setUninstallCompletedState(App.INSTALLSTATE_uninstallerror, app.getReason());
					event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_delete, 
							  R.string.event_appuninstallfailed, app.getPackageName(), app.getReason(), 0);
					saveApp(app);
					break;
			}
    		
    	} else {
    		LSLogger.warn(TAG, "Unknown requestcode (" + actionCode + ") in setInstallCompletionResult.");
    	}
		
       	Events eventsInstance = Controller.getInstance().getEventsInstance();
    	if (event != null && eventsInstance != null)
    		eventsInstance.logEvent(event);
	}
		
	/**
	 * Checks if the app is installed on the system. This DOES NOT check the managed apps database
	 * for the app.
	 * @param context application context. If null, uses Utils.getApplicationContext() to get a context.
	 * @param packageName name of the application package, such as com.lightspeedsystems.mdm.
	 * @return true if the app is installed, false if not installed.
	 */
	public static boolean isAppInstalled(Context context, String packageName) {
		boolean installed = false;
		try {
			if (context == null)
				context = Utils.getApplicationContext();
			PackageManager pkm = context.getPackageManager();
			ApplicationInfo appinfo = pkm.getApplicationInfo(packageName, 0);
			installed = (appinfo != null); 
		} catch (PackageManager.NameNotFoundException nex) {
			LSLogger.debug(TAG, "NameNotFound: App " + (packageName==null?"null":packageName) + " is not installed.");
		} catch (Exception ex) {
			LSLogger.exception(TAG, "isAppInstalled error: ", ex);
		}
		return installed;
	}
	
	/**
	 * Creates an App instance from the given JSON data, consisting of params from an Install command.
	 * @param json json-formated response from server. The Constants.CMD_nametag is expected, identifying the
	 * the name of the package. For a store-downloaded file, parameter Constants.CMD_storeurltag is present.
	 * For file-based package downloads, the file name will default to the package name; if Constants.CMD_fileurltag
	 * is present, uses that value as the name of the file to download and install.
	 * @return an app instance with relevant attributes, or null if no install data was found.
	 */
	public static App getAppInstanceFromInstallParams(JSONObject json) {
		App app = null;
		try {
			String pkgname = null;
			if (json.has(Constants.CMD_nametag)) {
				// get installation package name.
				pkgname = json.getString(Constants.CMD_nametag);
				app = new App(pkgname);
				if (json.has(Constants.CMD_storeurltag)) {
					// get the store-based url to the app:
					app.setInstallType(App.INSTALLTYPE_ONLINESTORE);
					app.setPackageStoreUrl( json.getString(Constants.CMD_storeurltag) );
				} else if (json.has(Constants.CMD_fileurltag)) {
					// Server-based file reference:
					app.setInstallType(App.INSTALLTYPE_REMOTEFILE);
					app.setPackageServerUrl( json.getString(Constants.CMD_fileurltag) );
					
					if (json.has(Constants.CMD_filenametag)) {
						app.setPackageFileName( json.getString(Constants.CMD_filenametag) );
					} else {
						// extract file name part from the end of the url; we'll need it for downloading
						String fn = app.getPackageServerUrl();
						if (fn != null) {
							int indx = fn.lastIndexOf('/');
							if (indx>=0) {
								// file name may have url params at the end of it (starting with ?); 
								//  if there is, we need to chop that off.
								String fname = fn.substring(indx+1);
								int pindx = fname.indexOf('?');
								if (pindx>0)
									fname = fname.substring(0, pindx);
								app.setPackageFileName(fname);
							}
						}
					}
					LSLogger.debug(TAG, "Filename="+app.getPackageFileName()+" fileurl="+app.getPackageServerUrl());

				} else if (json.has(Constants.CMD_filelocaltag)) {
					app.setInstallType(App.INSTALLTYPE_LOCAL);
					app.setPackageFilePath(json.getString(Constants.CMD_filelocaltag));
				}
				// a little trick to getting the installSource value set within the app instance; 
				// but also, we can check here to make sure we have the source of the package:
				// --DO NOT COMMENT THIS OUT; or if you do, at least call app.getSource(); --
				if (app.getSource() == null)
					LSLogger.warn(TAG, "No install source found for app " + app.getPackageName());
			}
		} catch (JSONException ex) {
			LSLogger.exception(TAG, "GetAppInstanceFromInstallParams JSON exception. ", ex);
		}
		return app;
	}
	
	/**
	 * Checks if we have permission to install packages. 
	 * @return true if permission to install is granted, false otherwise.
	 */
    public static boolean checkInstallPermission() {
    	boolean allowed = false;
    	try {
	    	PackageManager pm = Utils.getApplicationContext().getPackageManager();
	    	int p=pm.checkPermission("android.permission.INSTALL_PACKAGES", Constants.PACKAGE_NAME);
	    	if (p == PackageManager.PERMISSION_GRANTED) {
	    		LSLogger.debug(TAG, "PERMISSION TO INSTALL IS GRANTED.");
	    		allowed = true;
	    	} else { // permission denied
	    		LSLogger.debug(TAG, "Permission to install is DENIED");
	    	}
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "CheckInstallPermission error. ", ex);
    	}
    	return allowed;
    }

    /**
     * Checks to see if the user is allowed ti install non-market apps, i.e., apps downloaded
     * as files and not obatined from google play's online store.
     * @return true if apps can be downloaded and installed, false if not.
     */
    // internally, this value is determined from the android.provider.Settings.Global.INSTALL_NON_MARKET_APPS 
    // "Whether the package installer should allow installation of apps downloaded from sources other 
    // than Google Play. 1 = allow installing from other sources 0 = only allow installing from Google Play"
    public static boolean checkSettingsAllowAppInstalls(Context context) {
    	int value = 1;
    	try {
    	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        	    // this is applicable only in sdk 17 and later:
    	    	android.provider.Settings.Global.getInt(context.getContentResolver(), 
    	    			android.provider.Settings.Global.INSTALL_NON_MARKET_APPS, 0);
    	    } else { // we have to get the value differently. Just ignore it.
    	    	LSLogger.debug(TAG, "Non-store app install checking not supported in this version of android (sdk " +
    	    			Build.VERSION.SDK_INT + ")");
    	    }
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "checkSettingsAllowAppInstalls error:", ex);
    	}
    	LSLogger.debug(TAG, "Non-store app installs allowed: "+(value!=0));
    	return (value != 0);
    }
	
	/**
	 * Static method to get the SQL used to create the database table for managed apps
	 * @return the SQL string used to create the table.
	 */
    public static String getManagedAppsSqlCreateTable() {
    	return AppsDB.getSqlCreateTable();
    }

    
	// -----------------------------------------------------------------------------
	// --- Inner-class for handling Managed App persistence in a sqlite database  --
	// -----------------------------------------------------------------------------
	/**
     * A SQLite database is used to store Managed Apps in the lsapps table.
	 * 
	 * Rows are added for each managed app that is installed. A settings value defines whether the
	 * app is deleted upon uninstall or kept around for some length of time. 
	 *
	 * Each row includes: 
	 * id - locally-generated row ID of the app.
	 * mdmid - ID of the app on the MDM server; will be 0 if not known.
	 * appname - display name of the app
	 * packagename - name of the package, typically something like com.lightspeedsystems.mdm
	 * installstate - one of the App.INSTALLSTATE_ values indicating install state.
	 * installtime - timestamp value of when the app install was initiated the 1st time.
	 * installtype - type of the installation source, where the installsource comes from, 
	 *               an App.INSTALLTYPE_ value.
	 * installsource - source location of the installer package. Is a server url for a file download,
	 *                 a store url, local file path and name, etc. Value depends on installtype.
	 * updatestate - status of the update process.
	 * updatetime - time when the last update was started, or 0 for never updated.
	 * reason - failure description upon error; blank or null if no error occurred. 
	 *           Applies to last or current action only (install, update, uninstall).
	 * stats:
	 * actionReceived - timestamp of when the last (or current) action was received and queued to start.
	 * actionStarted  - timestamp of when the last (or current) action was started. 
	 *                  For install, this is when the install process started to run. 0 if not started.
	 * actionFinished - timestamp of when the last action completed. 0 if still running.
	 */
	private static class AppsDB extends DBStorage {
		private final static String APP_DB_TABLENAME = "lsapps";
		private final static String COLUMN_ID        = "id";  // doubles as the install/create time
		private final static String COLUMN_MDMID     = "mdmid";
		private final static String COLUMN_APPNAME   = "appname";
		private final static String COLUMN_PKGNAME   = "packagename";
		private final static String COLUMN_INSTALLTIME  = "installtime";
		private final static String COLUMN_INSTALLSTATE = "installstate";
		private final static String COLUMN_INSTALLTYPE  = "installtype";
		private final static String COLUMN_SOURCE    = "installsource";		
		private final static String COLUMN_REASON    = "reason";
		private final static String COLUMN_UPDATETIME    = "updatetime";
		private final static String COLUMN_UNINSTALLTIME = "uninstalltime";
		private final static String COLUMN_TIMEACTNRECVD = "actionreceived";
		private final static String COLUMN_TIMEACTNSTART = "actionstarted";
		private final static String COLUMN_TIMEACTNFNSHD = "actionfinished";
				
		// SQL command used to create the log entries table:
		private final static String APP_DB_CREATE_SQL =  
                "CREATE TABLE " +APP_DB_TABLENAME + " (" +
                		COLUMN_ID    		  + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                		COLUMN_MDMID 		  + " INTEGER, " +
                		COLUMN_APPNAME   	  + " TEXT, " +
                		COLUMN_PKGNAME 		  + " TEXT, " +
                		COLUMN_INSTALLTIME    + " LONG, " +
                		COLUMN_INSTALLSTATE   + " INTEGER, " +
                		COLUMN_INSTALLTYPE    + " INTEGER, " +
                		COLUMN_SOURCE 		  + " TEXT, " +
                		COLUMN_REASON 		  + " TEXT, " +
                		COLUMN_UPDATETIME     + " LONG, " +
                		COLUMN_UNINSTALLTIME  + " LONG, " +
                		COLUMN_TIMEACTNRECVD  + " LONG, " +
                		COLUMN_TIMEACTNSTART  + " LONG, " +
                		COLUMN_TIMEACTNFNSHD  + " LONG);";
		// index locations of the data in the query, for improved processing speeds:
		private final static int COLUMN_ID_INDEX        = 0;
		private final static int COLUMN_MDMID_INDEX     = 1;
		private final static int COLUMN_APPNAME_INDEX   = 2;
		private final static int COLUMN_PKGNAME_INDEX   = 3;
		private final static int COLUMN_INSTTIME_INDEX  = 4;
		private final static int COLUMN_INSTSTATE_INDEX = 5;
		private final static int COLUMN_INSTTYPE_INDEX  = 6;
		private final static int COLUMN_SOURCE_INDEX    = 7;
		private final static int COLUMN_REASON_INDEX    = 8;
		private final static int COLUMN_UPDTIME_INDEX   = 9;
		private final static int COLUMN_UNINSTTIME_INDEX= 10;
		private final static int COLUMN_ACTNRECV_INDEX  = 11;
		private final static int COLUMN_ACTNSTART_INDEX = 12;
		private final static int COLUMN_ACTNFINISH_INDEX= 13;

		// SQL command to read all rows from the database
		private final static String APP_DB_QUERY_GETALL_desc =
				"select * from " + APP_DB_TABLENAME + " order by " + COLUMN_APPNAME + " desc;";
		private final static String APP_DB_QUERY_GETALL_asc =
				"select * from " + APP_DB_TABLENAME + " order by " + COLUMN_APPNAME + " asc;";
		// SQL to get a row based on a specific field value:
	 	private final static String APP_DB_QUERY_DBID =
	 			"select * from " + APP_DB_TABLENAME + " where " + COLUMN_ID + "=?;";
	 	private final static String APP_DB_QUERY_MDMDBID =
	 			"select * from " + APP_DB_TABLENAME + " where " + COLUMN_MDMID + "=?;";
	 	private final static String APP_DB_QUERY_PACKAGENAME =
	 			"select * from " + APP_DB_TABLENAME + " where " + COLUMN_PKGNAME + "=?;";
	 	private final static String APP_DB_QUERY_APPNAME =
	 			"select * from " + APP_DB_TABLENAME + " where " + COLUMN_APPNAME + "=?;";
		  
		
		/**
		 * Creates Database persistence instance, where data will be stored.
		 * @param context
		 */
		public AppsDB(Context context) {
	        super(context);
	        setLoggingPersistence(false);
		}

		/**
		 * Abstract method to get the name of the database table.
		 * @return
		 */
	    public String getSqlTableName() {
	    	return APP_DB_TABLENAME;
	    }

		/**
		 * Static method to get the SQL used to create the database table.
		 * @return the SQL string used to create the table.
		 */
	    public static String getSqlCreateTable() {
	    	return APP_DB_CREATE_SQL;
	    }

	    /**
	     * Builds a map for all columns that may be requested, which will be given to the 
	     * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include 
	     * all columns, even if the value is the key. This allows the ContentProvider to request
	     * columns w/o the need to know real column names and create the alias itself.
	     */
	@SuppressWarnings("unused")
		private HashMap<String,String> buildColumnMap() {
	        HashMap<String,String> map = new HashMap<String,String>();
	        map.put(COLUMN_ID,    COLUMN_ID);
	        map.put(COLUMN_MDMID,  COLUMN_MDMID);
	        map.put(COLUMN_APPNAME,  COLUMN_APPNAME);
	        map.put(COLUMN_PKGNAME,COLUMN_PKGNAME);
	        map.put(COLUMN_INSTALLTIME, COLUMN_INSTALLTIME);
	        map.put(COLUMN_INSTALLSTATE,COLUMN_INSTALLSTATE);
	        map.put(COLUMN_INSTALLTYPE,  COLUMN_INSTALLTYPE);
	        map.put(COLUMN_SOURCE,  COLUMN_SOURCE);
	        map.put(COLUMN_REASON,  COLUMN_REASON);
	        map.put(COLUMN_UPDATETIME,  COLUMN_UPDATETIME);
	        map.put(COLUMN_UNINSTALLTIME, COLUMN_UNINSTALLTIME);
	        map.put(COLUMN_TIMEACTNRECVD,  COLUMN_TIMEACTNRECVD);
	        map.put(COLUMN_TIMEACTNSTART,  COLUMN_TIMEACTNSTART);
	        map.put(COLUMN_TIMEACTNFNSHD,  COLUMN_TIMEACTNFNSHD);
	        return map;
	    }
	    
	    // -- data management methods --
	    
	    protected void closeDB() {
	    	super.closeDB();
	    }
	    
	    /**
	     * Reads db data into the given App instance. Uses various attributes in the app to try
	     * to find it, including: packageName, appName, DbID, and MdmDbID.
	     * @param app App instance with at least one value to search on. Fills in other DB values
	     * in the instance if it is found within the database. (check the DbID value, as it gets
	     * set if the App was found, or left 0 if not found).
	     * @return true if the app was found, false if the app does not exist in the database.
	     */
	    @SuppressWarnings("unused")
		protected boolean readApp(App app) {
	    	boolean bFound = false;
	        Cursor cursor = null;
	    	try {
	    		SQLiteDatabase db = openForReading(); //Writing();
	    		String sql;
	    		String[] args = null;
	    		if (app.getDbID() != 0) {
	    			sql = APP_DB_QUERY_DBID;
	    			args = new String[] { Integer.toString(app.getDbID()) };
	    		} else if (app.getMdmDbID() != 0) {
	    			sql = APP_DB_QUERY_MDMDBID;
	    			args = new String[] { Integer.toString(app.getMdmDbID()) };
	    		} else if (app.getPackageName() != null) {
	    			sql = APP_DB_QUERY_PACKAGENAME;
	    			args = new String[] { app.getPackageName() };
	    		} else if (app.getAppName() != null) {
	    			sql = APP_DB_QUERY_APPNAME;
	    			args = new String[] { app.getAppName() };
	    		} else { // we have no values to search on; bail out.
	    			throw new Exception("No values to search on.");
	    		}
	    		
	    		//debug:
	    		LSLogger.debug(TAG, "Query single App SQL=" + sql);
	    		LSLogger.debug(TAG, "Query single App args=" + ((args==null)?"null":args.toString()));
	    		
	    		cursor = db.rawQuery(sql, args);
	    		
	    		if (cursor != null && cursor.moveToFirst()) {
	    			// read the data; assume it's only one row and we're reading that row.
	                readAppRow(app, cursor);
	    		}
	    		bFound = true;
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Reading DB in single App query: ", ex, false);
	    	} finally {
	    		if (cursor != null && !cursor.isClosed())
	    			cursor.close();
	    	}
	    	return bFound;
	    }
	    
	    protected long readAppRecords(Vector<App> list, int sortOrder, String msgTypeFilter) {
	    	int result = -1;
	        Cursor cursor = null;
	    	try {
	    		SQLiteDatabase db = openForReading(); //Writing();
	    		String sql;
	    		String[] args = null;
	    //		if (msgTypeFilter == null) {
	    			sql = ((sortOrder==DBORDER_Descending) ? 
	    				APP_DB_QUERY_GETALL_desc : APP_DB_QUERY_GETALL_asc);
	    //		} else {
	    //			sql = ((sortOrder==DBORDER_Descending) ? 
	    //					APP_DB_QUERY_GETTYPE_desc : APP_DB_QUERY_GETTYPE_asc);
	    //			args = new String[] { msgTypeFilter };	    			
	    //		}
	    		//debug:
	    		LSLogger.debug(TAG, "SQL=" + sql);
	    		LSLogger.debug(TAG, "args=" + ((args==null)?"null":args.toString()));
	    		
	    		cursor = db.rawQuery(sql, args);
	    		
	    		if (cursor != null && cursor.moveToFirst()) {
	    			result = 0;
	                do {
	                    App app = new App();
	                    readAppRow(app, cursor);
	                    list.add(app);
	                    result++;
	                } while (cursor.moveToNext());
	    		}
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Reading DB", ex, false);
	    		result = -1;
	    	} finally {
	    		if (cursor != null && !cursor.isClosed())
	    			cursor.close();
	    	}
	    	return result;
	    }
	    
	    /*
	     * Fills in an app instance from an open cursor.
	     * Throws any cursor exceptions back to caller.
	     */
	    private void readAppRow(App app, Cursor cursor) {
                app.setDBID( cursor.getInt(COLUMN_ID_INDEX) );
                app.setMdmDbID(	cursor.getInt(COLUMN_MDMID_INDEX) );
                app.setAppName( cursor.getString(COLUMN_APPNAME_INDEX) ); 
                app.setPackageName( cursor.getString(COLUMN_PKGNAME_INDEX) );
                app.setInstallTime( cursor.getLong(COLUMN_INSTTIME_INDEX) );
                app.setInstallState( cursor.getInt(COLUMN_INSTSTATE_INDEX) );
                app.setInstallType( cursor.getInt(COLUMN_INSTTYPE_INDEX) );
                app.setSource( cursor.getString(COLUMN_SOURCE_INDEX) );
                app.setReason( cursor.getString(COLUMN_REASON_INDEX) );
                app.setUpdateTime( cursor.getLong(COLUMN_UPDTIME_INDEX) );
                app.setUninstallTime( cursor.getLong(COLUMN_UNINSTTIME_INDEX) );
                app.setTimeActionReceived( cursor.getLong(COLUMN_ACTNRECV_INDEX) );
                app.setTimeActionStarted( cursor.getLong(COLUMN_ACTNSTART_INDEX) );
                app.setTimeActionFinished( cursor.getLong(COLUMN_ACTNFINISH_INDEX) );	    		
	    }
	    
	    /**
	     * Adds the App instance's data to the database.
	     * @param app App instance to add.
	     * @return the row ID of the inserted row and sets the ID into the app instance, 
	     * or returns -1 if error.
	     */
	    protected long insert(App app) {
	    	ContentValues mapping  = new ContentValues(13);

	    	mapping.put(COLUMN_MDMID,         Integer.valueOf(app.getMdmDbID()));
	    	mapping.put(COLUMN_APPNAME,       app.getAppName());
	    	mapping.put(COLUMN_PKGNAME,       app.getPackageName());
	    	mapping.put(COLUMN_INSTALLTIME,   Long.valueOf(app.getInstallTime()));
	    	mapping.put(COLUMN_INSTALLSTATE,  Integer.valueOf(app.getInstallState()));
	    	mapping.put(COLUMN_INSTALLTYPE,   Integer.valueOf(app.getInstallType()));
	    	mapping.put(COLUMN_SOURCE,		  app.getSource());
	    	mapping.put(COLUMN_REASON,		  app.getReason());
	    	mapping.put(COLUMN_UNINSTALLTIME, Long.valueOf(app.getUninstallTime()));
	    	mapping.put(COLUMN_UPDATETIME,    Long.valueOf(app.getUpdateTime()));
	    	mapping.put(COLUMN_TIMEACTNRECVD, Long.valueOf(app.getTimeActionReceived()));
	    	mapping.put(COLUMN_TIMEACTNSTART, Long.valueOf(app.getTimeActionStarted()));
	    	mapping.put(COLUMN_TIMEACTNFNSHD, Long.valueOf(app.getTimeActionFinished()));
	    	app.setDefDirty(false);	    	
	    	int id = (int)super.insertRow(APP_DB_TABLENAME, mapping);
	    	if (id > 0)
	    		app.setDBID(id);
	    	return id;
	    }
	    
	    /**
	     * Updates the App's DB data for install, update, and uninstall related values.
	     * @param app App instance to update
	     * @return 1 if the app was updated, 0 if the app was not found, -1 for error.
	     */
	    protected int updateAppValues(App app) {
	    	String args[] = new String[] { Integer.toString(app.getDbID()) };
	    	String whereClause = COLUMN_ID + "=?";
	    	// set the values to be updated: unless marked as dirty, we only need to update certain columns:
	    	ContentValues mapping;
	    	if (app.isDefDirty()) {
	    		mapping  = new ContentValues(12);
		    	mapping.put(COLUMN_APPNAME,       app.getAppName());
		    	mapping.put(COLUMN_PKGNAME,       app.getPackageName());
		    	mapping.put(COLUMN_INSTALLTYPE,   Integer.valueOf(app.getInstallType()));
		    	mapping.put(COLUMN_SOURCE,		  app.getSource());
	    		app.setDefDirty(false);
	    	} else {
	    		mapping  = new ContentValues(8);
	    	}
	    	mapping.put(COLUMN_INSTALLTIME,   Long.valueOf(app.getInstallTime()));
	    	mapping.put(COLUMN_INSTALLSTATE,  Integer.valueOf(app.getInstallState()));
	    	mapping.put(COLUMN_UPDATETIME,    Long.valueOf(app.getUpdateTime()));
	    	mapping.put(COLUMN_UNINSTALLTIME, Long.valueOf(app.getUninstallTime()));
	    	mapping.put(COLUMN_TIMEACTNRECVD, Long.valueOf(app.getTimeActionReceived()));
	    	mapping.put(COLUMN_TIMEACTNSTART, Long.valueOf(app.getTimeActionStarted()));
	    	mapping.put(COLUMN_TIMEACTNFNSHD, Long.valueOf(app.getTimeActionFinished()));
	    	mapping.put(COLUMN_REASON,		  app.getReason());
	    	return (int)super.updateRow(APP_DB_TABLENAME, mapping, whereClause, args);
	    }
	    

	    /**
	     * Deletes all apps from the database.
	     */
	    /** not used at this time; keep here for if/when needed.
	    protected void deleteAll() {
	    	try {
	    		SQLiteDatabase db = openForWriting();
	    		db.delete(APP_DB_TABLENAME, null, null);
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Deleting all Apps from DB: ", ex);
	    	}
	    }
		**/
	    
	    /**
	     * Deletes an app from the database.
	     */
	    protected void deleteApp(App app) {
	    	try {
		    	String args[] = new String[] { Integer.toString(app.getDbID()) };
		    	String whereClause = COLUMN_ID + "=?";
	    		SQLiteDatabase db = openForWriting();
	    		db.delete(APP_DB_TABLENAME, whereClause, args);
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Deleting App from DB:", ex);
	    	}
	    }

	}

    
    
}

