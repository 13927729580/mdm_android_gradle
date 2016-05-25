package com.lightspeedsystems.mdm;

import org.json.JSONException;
import org.json.JSONObject;

import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Provides MobileManager app update support, for updating itself.
 * 
 * Generally, the same install/update process as a managed app is used. This class helps to
 * provide a little extra functionality for the updating process.
 * 
 */
public class Updater {
	private final static String TAG = "Updater";
	private Controller controller;
	private long lastUpdateCheckTime;    // when the last update check occurred
//	private long nextUpdateAttemptTime;  // when an update retry should occur, if any
//	private String updateAttemptVersion; 
	private App appUpdate;				 // app instance with update information
	private int checkIncrement;			 // counter to limit how often the update check is done.
	private static int checkIncrementValue = 5; 
	
	/* Update internal constants; these roughly match Constants values with similar names,
	 * but these values are in number-of-days (approximating monthly number) for easier 
	 * internal processing calculations. */
	private final static int UPDATECHECK_never     = 0;  // update check is off.
	private final static int UPDATECHECK_daily     = 1;
	private final static int UPDATECHECK_weekly    = 7;
	private final static int UPDATECHECK_monthly   = 31;
	private final static int UPDATECHECK_quarterly = 92;
	private final static int UPDATECHECK_yearly    = 365;

	
	public Updater(Controller controller) {
		this.controller = controller;
		checkIncrement = 0;
	}

	// callback when an update completed. Note that this would likely only get called when
	// an update failed or was cancelled by user interaction.
	// param resultcode is appInstall result code: ok, cancelled, error.
	public void updateComplete(App app, CommandResult commandResult, int resultCode) {
		
		LSLogger.debug(TAG, "Update attempt completed. result="+resultCode);
		if (commandResult != null) {
			if (commandResult.hasErrorMessage())
				LSLogger.debug(TAG, "Update error: "+commandResult.getErrorMessage());
		}
		
		// (note: not implementing this yet; here, we'd add the two settings to save,
		//  but only if the install/update was cancelled or failed.)
		// ...and schedule the retry .......add the 2 settings...
		
		// see if we need to set up a retry:
		//if (commandResult != null && commandResult.wasCanceledOrDidNotComplete ???? )
		// .. or, if this was set...
		// app.setUpdateCompletedState(App.INSTALLSTATE_updatecancelled   or is INSTALLSTATE_updateerror
		
		// regardless, the current update is complete, so set our app instance to null.
		appUpdate = null;
	}
	
	/**
	 * Checks if it's time to schedule an update, and if so, handles it.
	 * If an update is already in process (appUpdate instance will be set), 
	 * ignores the already-in-process update.
	 * @param bForceCheck when true, forces the udpate check to occur now. 
	 * False means to wait until the scheduled time.
	 * @return true if an update was scheduled/started, false if the app is up to date.
	 */
	public synchronized boolean updateCheck(boolean bForceCheck) {
		boolean checknow = bForceCheck;
		boolean updateStarted = false;
		if (!checknow) {
			if (checkIncrement <= 0) {
				checknow = true;
				checkIncrement = checkIncrementValue; // initialize to reduce how often the update check is done
			} else
				checkIncrement--;
		}
		if (checknow && (appUpdate == null) && isUpdateNeeded(bForceCheck)) {
			LSLogger.debug(TAG, "Checking for updates");
			// schedule the update event:
			if (appUpdate != null) {
				// log the event that the updating is starting: in description, include the version in parens.
				try {
					String versionStr = "(" + controller.getSettingsInstance().getAppVersion() + ")";
					Event event = new Event(0, Event.EVENTTYPE_app, Event.EVENTACTION_update, 
							  R.string.event_mdmupdatestarted, Constants.PACKAGE_NAME, versionStr, 0);
			      	controller.getEventsInstance().logEvent(event);
				} catch (Exception ex) {
					LSLogger.exception(TAG, "UpdateCheck event error:", ex);
				}
				// now schedule the update:
				updateStarted = controller.enqueueMdmUpdateAction(appUpdate);
				LSLogger.info(TAG, "MobileManager Updating started.");
			}
		} else if (bForceCheck)
			LSLogger.debug(TAG, "Force-Check for updates ignored; update alreaady in process="+(appUpdate == null));

		// return true if we started the update, or an update is already in progress.
		return (updateStarted || (appUpdate != null));
	}
	
	/**
	 * Checks if it's time to schedule an update, and if so, handles it.
	 * Note that we refer to Settings to get the current value; settings can change, so no need
	 *  to cache some of those possibly-changing values in this instance, instead we re-read
	 *  from settings as needed.
	 *  @param bForceUpdateCheck when true, checks for updates now. When false, only checks for
	 *  updates based on the update frequency settings.
	 *  @return true if an update is needed, and has created the app instance to manage the install/update.
	 */
	private boolean isUpdateNeeded(boolean bForceUpdateCheck) {
		Settings settings = controller.getSettingsInstance();
		boolean doupdate = false;
		
		if (bForceUpdateCheck) {
			doupdate = checkServerForUpdate(settings);
		} else {
			if (settings != null) {
				// the 'updatetype' is also a value representing the number of days in the selection, so
				// that we can use that to calculate the the time duration it represents.
				int updateType = updateTypeFromSetting(settings.getUpdateCheck());
				if (updateType != 0) { // we check for updates per settings, so do the check
					long currentTime = System.currentTimeMillis();
					
					
					// See if we have a retry-time, and then if it is time to do the retry:
					/**
					nextUpdateAttemptTime = settings.getSettingLong(Settings.NEXTUPDATERETRYTIME, 0);
					updateAttemptVersion = settings.getSetting(Settings.NEXTUPDATERETRYVER);
					// check if we had a cancellation and need to recheck for doing the update install
					if (nextUpdateAttemptTime != 0) {
						// first we have to check to see if the retry version applies to this version;
						//  if an update was cancelled, that could happen. If the update was ok, the
						//  value will not exist or will be different.
						if (updateAttemptVersion != null && 
								!updateAttemptVersion.equals(settings.getAppVersion())) {
							// we have old values; remove them from settings:
							settings.removeSetting(Settings.NEXTUPDATERETRYTIME);
							settings.removeSetting(Settings.NEXTUPDATERETRYVER);
						} else if (nextUpdateAttemptTime <= currentTime) {
							doupdate = true; 
						}
					} 
					**/  // end of retry-time logic.
					
					if (!doupdate) {
						
						// see if it is time to check for an update, using a stored lastupdatechecktime:
						// - if we do not know when we had the last update check, get it form settings;
						//  note that the resulting value will be 0 if no updatecheck was done yet, so it could be 0.
						if (lastUpdateCheckTime <= 0)
							lastUpdateCheckTime = settings.getSettingLong(Settings.LASTUPDATECHECKTIME, 0);
						// handle the first point in time to check for an update; that point in time will start
						//  from now, and we need to save that value.
						if (lastUpdateCheckTime == 0) {
							lastUpdateCheckTime = currentTime;
							settings.setSetting(Settings.LASTUPDATECHECKTIME, lastUpdateCheckTime);
						}
	
						// the following calculation takes the last updatecheck time and adds the setting-defined
						//  duration to that time, coming up with a time value that, once reached, the update
						//  check should occur. 
						long nextUpdateCheckTime = lastUpdateCheckTime +
								(updateType * Constants.NUMBER_MILLISECONDS_IN_DAY);
						if (nextUpdateCheckTime <= currentTime) {
							LSLogger.debug(TAG, "-Checking for updates: check time="+Utils.formatLocalizedDateUTC(nextUpdateCheckTime)
									+" current_time="+Utils.formatLocalizedDateUTC(currentTime));
							// if the current time is past the time, it's time to check for an update from the server:
							doupdate = checkServerForUpdate(settings);
						}
					}
				}
			}
		}
		return doupdate;
	}
	
	/**
	 * Internal method for calling into the server to check for an update.
	 * Returns true if an update is needed, false if not or if the check failed.
	 * If the check with the server was successful, updates the setting for lastchecktime.
	 * If an update is needed, creates the App instance with the install information.
	 */
	private boolean checkServerForUpdate(Settings settings) {
		boolean updateIsNeeded = false;		
		try {
/*			JSONObject jparams = new JSONObject();
			// add the org id, device identifier, and current app version to the command, as params:
			jparams.put(Constants.PARAM_ORGID, 		 settings.getOrganizationID());
			jparams.put(Constants.PARAM_DEVICE_UDID, settings.getDeviceUdid());
			jparams.put(Constants.PARAM_APPVERSION,  settings.getAppVersion());

*/			JSONObject jparams = null;
			
			ServerComm serverComm = new ServerComm();
			serverComm.setIncludeHeaderAuth(true);
			serverComm.setAuthTokenKey(settings.getAuthMBCToken());
			HttpCommResponse response = 
					serverComm.getFromServer(ServerUrlProvider.getCheckUpdateUrl(settings), jparams);
			if (response != null) {
				if (response.isOK()) {
					// got a successful response from server
					// - update settings and local time value, noting when we did the check:
					lastUpdateCheckTime = System.currentTimeMillis();
					settings.setSetting(Settings.LASTUPDATECHECKTIME, lastUpdateCheckTime);
					// - get response from server:
					LSLogger.debug(TAG, "Update-Check Result from server: " + response.getResultStr());
					JSONObject jresp = new JSONObject(response.getResultStr());
					if (jresp != null && jresp.length() > 0) {
						// create an App instance from the params; compare version and name;
						// if the app info version is newer and is a valid app (name is right),
						//  then see if the app instance is already in the managed apps db,
						//  and if there is, get it and set the info in it; otherwise this
						//  app instance will be the app instance to update and add.
						App app = Apps.getAppInstanceFromInstallParams(jresp);
						
/* for testing, force these values:
						if (app !=null) {
						app.setPackageName("com.something");
						app.setPackageFileName(null);
						app.setPackageFilePath(null);
						app.setPackageServerUrl(null);
						app.setInstallType(App.INSTALLTYPE_REMOTEFILE);
						}
*/						
						if (app == null) {
							LSLogger.error(TAG, "Invalid response from update server.");
						} else if (!app.equalsPackageName(Constants.PACKAGE_NAME)) {
							LSLogger.error(TAG, "Invalid package name for update. Update not performed");
						} else if (!app.isUpdateNeeded(Constants.APPLICATION_VERSION_STR)) {
							LSLogger.debug(TAG, "no update needed; current version="+Constants.APPLICATION_VERSION_STR);
						} else { // process the update
							// try to get existing MDM app instance from the Apps db:
				 			App appMdm = controller.getAppsInstance().findAppByPackageName(Constants.PACKAGE_NAME, 0);
							if (appMdm != null) {
								// copy relevant values from data read from the server to existig instance:
								LSLogger.debug(TAG, "Found existing app instance: "+appMdm.toString());
								appMdm.setSourceValues(app);
								appUpdate = appMdm;
							} else {
								appUpdate = app;
								LSLogger.debug(TAG, "Using new app update instance: "+app.toString());
							}
						}
						if (appUpdate != null) {  // we have an update to install!
							updateIsNeeded = true;
							appUpdate.setMdmAppType(App.MDMAPPTYPE_self);// setIsMdmApp();							
						}
					} 
				} else {
					// update check failed; response has error info:
					LSLogger.error(TAG, "Server update check failed: " + response.getResultReason());
				}
			}
		} catch (JSONException jex) {
			LSLogger.exception(TAG, "CheckServerForUpdate JSON error:", jex);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "CheckServerForUpdate error:", ex);
		}
		return updateIsNeeded;
	}
	
	
	/*
	 * Converts setting from a string to an internal type (and duration) value for when to check for updates.
	 */
	private int updateTypeFromSetting(String updatetype) {
		int type = UPDATECHECK_never;
		if (updatetype != null && updatetype.length()>0) {
			if (updatetype.equals(Constants.UPDATECHECK_DAILY))
				type = UPDATECHECK_daily;
			else if (updatetype.equals(Constants.UPDATECHECK_WEEKLY))
				type = UPDATECHECK_weekly;
			else if (updatetype.equals(Constants.UPDATECHECK_MONTHLY))
				type = UPDATECHECK_monthly;
			else if (updatetype.equals(Constants.UPDATECHECK_QUARTERLY))
				type = UPDATECHECK_quarterly;
			else if (updatetype.equals(Constants.UPDATECHECK_YEARLY))
				type = UPDATECHECK_yearly;
		}
		return type;
	}
	
}
