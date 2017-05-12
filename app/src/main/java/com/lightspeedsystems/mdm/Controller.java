package com.lightspeedsystems.mdm;

import java.io.File;
import java.util.Vector;

//import com.google.android.gcm.GCMRegistrar;
import com.google.firebase.iid.FirebaseInstanceId;
import com.lightspeedsystems.mdm.util.LSLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//import com.google.android.gcm.GCMRegistrar;

/**
 *  Application controller, provides top-level functional control and support of actions and tasks.
 *  Manages devices settings and configuration values required for identification, communications with
 *  a MDM server, and MDM operations.
 */
/*
 * App and systems configuration must be properly set up and established before this app can fully
 * manage the device. There are a few stages and steps that must be performed and completed to get 
 * the device to the proper operational state. The following states/steps are needed:
 * System Readiness: ensuring the underlying system has required dependency software and/or settings,
 *  if any. The MDM app relies on these to run properly, so such things must be in place, or if not
 *  present, automatically or manually installed or configured. These include:
 * > GCM Registration and Communications support:
 *    - requires Google Services Framework presence
 *    - requires an account on the device to google or gmail with valid credentials
 *  
 * MDM Readiness: the application and system environment is complete and ready; but other MDM
 *  application-specific settings are required. Where possible, minimal this is accomplished
 *  automatically or via minimal user interaction. 
 *  These items depend on System Readiness before they can be established. These include:
 * > device enrollment with an MDM server
 *    - establishes the MDM server url for MDM actions (defined in Settings)
 *    - establishes Organizational information (defined in Settings)
 *    
 * Once the above are present, the device can be managed and will automatically check in with
 *  the MDM server it is registered (enrolled) to, check for MDM app updates, etc.
 *  
 * 
 * During initialization, various installation and configuration status flags are used to know
 * when something has been completed or is still required, as follows:
 * 
 * GCM - to make sure we have a valid GCM ID, the FLAG_GCMREG_ok flag in the INITSTATE
 * storage value is used: when the gcm id is present and that flag bit is set, we have 
 * a registration id and do not need to do anything else. If either the flag is not set
 * or the gcm id is not present, we need to register (or if this is not the first time the
 * app is run, perform re-registration) to get a gcm id. Once we have an ID, the state flag
 * is set and stored.
 * Using this approach, two different ways can be used to cause a new gcm id to be created:
 * either remove the gcm id from settings, or clear the init flag for it.
 * 
 * (GCM actually has 3 major states: "ok"/ready, where the GCM ID has been obtained; GSF is
 *   not present and is or has attempted to be installed; and GCM account error. On most
 *   platforms, only the first state is encountered, and the GCM ID is easily obtained. 
 *   On Kindle and potentially others however, underlying software and configuration dependencies
 *   may not be present and as a result, prevent GCM from getting properly set up.
 *   This software resolves this by first attempting to just get the GCM ID, and as needed, 
 *   handle prerequisites and assist in getting them in place; worst-case, the app will end if
 *   the dependencies are not present and have not or cannot be installed or set up.)
 * 
 * Registration to mdm server (enrollment) - the device needs to register itself with the mdm server.
 * The device uses it's unique device identifier (UDID) to identify itself with the server.
 * This is a value that is provided by the device and is independent of our software.
 * Similar to GCM, a FLAG_SVRREG_ok flag is used to know when we have successfully registered
 * with a MDM server; until this flag is set, the device is not completely configured.
 */

public class Controller extends Thread { //implements ServiceConnection {

	private static String TAG = "Controller";

	private static Context context;
	private Settings settings;
	private Device device;
	private Events events;
	private Apps apps;      // manager for managed apps.
	private UserAuthority userAuth;
	private DeviceAdminProvider deviceAdminProvider;
	private BatteryInfo batteryInfoProvider;
	private CheckinProcessor checkinProcessor;
	private Updater appUpdater;
	///	private AppBlocker appBlocker;
	private ProgressCallbackInterface msgDisplayCallback;
	private ErrorCallbackInterface errorCallback;
	private boolean ending;
	private boolean terminated;
	private boolean bControllerAbort;
	private boolean bGcmRegistered; // true when the app/device is regitered to receive gcm
	private boolean bGcmRegisterAttempted;
	private boolean bGcmControllerRegistered; // true when the controller is registered to receive gcm


	private Vector<ControllerCommand> queuedCommands;
	private Vector<ControllerCommand> deferredCommands;
	//private Vector<ControllerCommand> backgroundCommands;
	private BackgroundQueueHandler backgroundQueueHandler;

	// status of things that need to be completed during initialization 
	private boolean initializedGCM;
	private boolean registerGCMinprogress;
	private boolean initializedServerRegistration;


	// status of things that can run after initialization
//	private boolean statusGetServerCommands;
//	private boolean statusSyncToServer;

	// initialization status flags:
	private int FLAG_GCMREG_ok = 0x001;  // gcm is set up and ready
	private int FLAG_GCMREG_gsfinstalled = 0x002;  // gsf was installed from mdm
	private int FLAG_GCMREG_permsreboot = 0x004;  // gcm is giving permissions errors; first time, we set this,2nd time we reboot.
	private int FLAG_GCMREG_appreinstall = 0x008;  // app is being reisntalled due to gcm error.
	private int FLAG_SVRREG_ok = 0x010;  // mdm server is registered and has been communicated with (enrolled)

	private static boolean bDidInit = false;    // this is used to ensure init is called only once.
	private static boolean bForceInit = false;  // used to force re-initialization of settings (when set to true).

	private static Controller controllerInstance = null; // singleton instance of the controller

	/**
	 * Gets the context used by the application. This method makes getting the context possible for 
	 * various methods that have no context available but need to use one for various things.
	 * @return the application's context
	 */
	public static Context getContext() {
		if (context == null)
			context = Utils.getApplicationContext();
		return context;
	}

	/**
	 * Gets the application's Controller instance (a singleton).
	 * @return Controller instance.
	 */
	protected static Controller getInstance() {
		return getInstance(null);
	}

	/**
	 * Gets the updater, which handles updating this app.
	 * @return Updater instance.
	 */
	protected Updater getUpdaterInstance() {
		return appUpdater;
	}

	/**
	 * Gets the application's Controller instance (a singleton).
	 * @param context application context, must not be null.
	 * @return Controller instance
	 */
	/* dev notes: THis method is called many times from many places. We want to be sure
	 * that only one instance gets created, and during that creation process, it gets
	 * initialized and started. 
	 * So first, if an instance is not created, we synchronize on the class and enter the
	 * protected block where the instance gets created (and initialized); this ensures only
	 * one thread is in the process of creating the instance.
	 * Once inside the synchronized block, we get the controller instance, and check to make sure
	 * it is still not created; this handles the situations where multiple calls from different
	 * threads call in to this, the controller is not yet created, so the other threads block
	 * on the synchroized section and wait until other threads finish, but then once executing,
	 * the thread needs to be sure the instance did not already get created by another thread.
	 * So then, if the instance is not yet created, it is created and initialized. 
	 * Lastly, thr global controllerInstance is set to the new instance; this assignment must be
	 * set late and last becasue if controllerInstance is assigned the new instance as its created, 
	 * other threads may make a call to getInstance and get the created instance, but the instance
	 * might not have yet been initialized. Therefore, the assignment is performed after the instance
	 * has been initialized and started.
	 * 
	 * Generally, the above described contentions and timing should occur seldom if at all. Currently, 
	 * there are only 2 possible threads that could make a call into this at the same time: that's the
	 * main app activity ui and the GCM service thread; if both attempt to get the instance at the exact
	 * same time, initialization issues could occur. This is difficult to test for, but would happen if
	 * both the UI is being started at the same time a GCM message was received and is being processed.
	 */
	public static Controller getInstance(Context context) {
		if (controllerInstance == null) {
			Controller newInstance = null;
			if (context == null)
				context = Utils.getApplicationContext();
			synchronized (Controller.class) {
				Utils.setApplicationContext(context); // make sure we have a context.
				newInstance = controllerInstance;
				if (newInstance == null) {
					newInstance = new Controller(context);
					newInstance.initialize();
					controllerInstance = newInstance;
				}
			}
		}
		// debug check:
		//if (context != controllerInstance.context && context!=null)
		//	LSLogger.warn(TAG, "WARNING: Context mis-match. instance="+controllerInstance.context.toString()+
		//			" param="+context.toString());

		return controllerInstance;
	}

	/* Constructor for singleton instance. */
	private Controller(Context context) {
		Controller.context = context;
		LSLogger.initialize(context);
	}

	/**
	 * Sets the messaging progress callback instance, which can be used for 
	 * notification of information that should be shown to the user.
	 * @param msgcallback instance to call back to. Can be null if no callback is needed.
	 */
	public void setProgressCallback(ProgressCallbackInterface msgcallback) {
		msgDisplayCallback = msgcallback;
	}


	public void setErrorCallback(ErrorCallbackInterface errorcallback) {
		errorCallback = errorcallback;
	}


	/**
	 * Gets the Settings instance.
	 * @return Settings instance.
	 */
	public Settings getSettingsInstance() {
		return settings;
	}

	/**
	 * Gets the Device instance, containing detailed information about the device.
	 * @return Device instance, or null if the controller is not yet initialized.
	 */
	public Device getDeviceInstance() {
		return device;
	}

	/**
	 * Gets the Events instance, the data provider of current and previous mdm event storage.
	 * @return Events instance, or null if the controller is not initialized.
	 */
	public Events getEventsInstance() {
		return events;
	}

	/**
	 * Gets a master Apps instance, used for working with managed App instances.
	 * @return Apps instance, created it as needed.
	 */
	public Apps getAppsInstance() {
		if (apps == null) {
			synchronized (this) {
				if (apps == null) {
					apps = new Apps(context);
					// we're using Apps for managed apps, so load up the existing list of apps into it.
					apps.loadManagedApps();
				}
			}
		}
		return apps;
	}

	/**
	 * Gets the user authoritative instance.
	 * @return UserAuthority for the current user and session.
	 */
	public UserAuthority getUserAuthority() {
		return userAuth;
	}

	public BatteryInfo getBatteryInfoProvider() {
		return batteryInfoProvider;
	}

	public DeviceAdminProvider getDeviceAdmin() {
		return deviceAdminProvider;
	}

	public void showToastMessage(String message, int resourceID) {
		String msg = message;
		if (msg == null && resourceID != 0)
			msg = context.getResources().getString(resourceID);
		if (msg != null && errorCallback != null)
			errorCallback.onErrorCallback(Constants.ERRORTYPE_TOASTMSG, null, msg);
	}

	/**
	 * Starts up the controller.
	 */
	private void initialize() {
		if (!bDidInit && !ending) {
			bDidInit = true;
			ending = false;

			LSLogger.debug(TAG, "Initializing Controller...");

			// Load settings:
			settings = Settings.getInstance(context);
			settings.logValues();

			// Initialize needed instances and values:
			deviceAdminProvider = new DeviceAdminProvider(context);
			HttpComm.initFromSettings(settings);
			userAuth = new UserAuthority(context);
			device = new Device(context, settings);
			events = new Events(context);
			appUpdater = new Updater(this);
			queuedCommands = new Vector<ControllerCommand>();
			deferredCommands = new Vector<ControllerCommand>();
///	        appBlocker = new AppBlocker(context);
///	        appBlocker.initialize();

			// Initialize receivers:
			batteryInfoProvider = new BatteryInfo(context);

			// see if there are any updates that need status changes, etc.
			getAppsInstance();
			apps.completePendingUpdates();

			// Get installation/configuration status:
			initInstallStatus();

			// if we are restarting and gcm is already set up, let's start up receivers:
//			if (isGcmReady())
				initializeGcmReceivers();

			/*
			if (!Utils.wifiEnable(context, true)) 
		    	LSLogger.warn(TAG, "Wifi is not enabled.");
			else
				LSLogger.info(TAG, "Wifi is ebabled.");
			*/

			initializeLocationFromSettings();

			start();
		}
	}

	private void initializeGcmReceivers() {
//		if (!bGcmControllerRegistered) {
			context.registerReceiver(mHandleGcmMessageReceiver,
					new IntentFilter(MdmFirebaseMessagingService.GCM_NOTIFICATION_MESSAGE));
			bGcmControllerRegistered = true;
			LSLogger.debug(TAG, "Initialized GCM receivers.");
//		}
	}

	private void terminateGcmReceivers() {
		LSLogger.debug(TAG, "-terminating GCM receivers");
		if (bGcmControllerRegistered) {
			bGcmControllerRegistered = false;
			context.unregisterReceiver(mHandleGcmMessageReceiver);
		}
		if (bGcmRegistered || bGcmRegisterAttempted) {
			bGcmRegistered = false;
			bGcmRegisterAttempted = false;
			try {
				unregisterGcm(true);
				////GCMRegistrar.unregister(context); //GCMRegistrar.onDestroy(context);
			} catch (Exception ex) {
				LSLogger.exception(TAG, "TerminateGCMReceivers error:", ex);
			}
		}
	}

	/**
	 * Shuts down the controller. This is to be called when the app is ending.
	 */
	protected void terminate() {
		if (!terminated) {
			terminated = true;

			ending = true;
			LSLogger.info(TAG, "Terminating Controller...");
			if (checkinProcessor != null) {
				checkinProcessor.terminate();
				checkinProcessor = null;
			}
///	        if (appBlocker != null)
///	        	appBlocker.terminate();

			terminateGcmReceivers();
			if(locationManager != null) {
				if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					locationManager.removeUpdates(locationListener);
				}
			}
			//stopGcmService();
			// ...leave logger running...so it captures shutdowns.

			//   LSLogger.terminate();
		}
	}

	/** Callback for notification that admin activtion state has changed. */
	public void adminActiveStateChanged() {
		LSLogger.debug(TAG, "Admin activated.");
		interruptMainLoop();
	}

	// internal method to interrupt waiting that may be going on here.
	private void interruptMainLoop() {
		try {
			this.interrupt();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "InterruptWaiting error:", ex);
		}
	}

	// Main controll/operation thread. Initiates and/or handles background tasks.

	@SuppressWarnings("unchecked")
	public void run() {
		long sleepTimeUninitialized = 20000;
		long sleepTimeInitialized = 180000;
		long sleepTime = sleepTimeInitialized;
		boolean initialState = isInitialized();

		// make sure device admin is enabled before doing anything;
		// if not ready, we wait/loop until it is ready.
		if (!getDeviceAdmin().isActiveAdmin()) {
			LSLogger.debug(TAG, "Controller waiting for admin to be activated.");
			do {
				try {
					sleep(10000);
				} catch (InterruptedException iex) {
					LSLogger.debug(TAG, "Controller thread wait interrupted.");
				} catch (Exception ex) {
					LSLogger.exception(TAG, "Run-wait exception: ", ex);
				}
			} while (!ending && !getDeviceAdmin().isActiveAdmin());
		}


		if (!ending) {
			// now we can start the controller:
			LSLogger.info(TAG, "Starting Controller...");

			try {
//				if (isGcmReady())
					initializeGcmReceivers();

				do {

					if (!isInitialized()) {
						if (!initializeApp()) { // initialization didnt complete
							sleepTime = sleepTimeUninitialized;
						}
					}

					if (isInitialized()) {  // yes, recheck, since state may have changed above.

						if (!initialState) { // we started in a non-initialized state, but are now ok;
							initialState = true;
							statusMsg(""); // clear main window's status msg, if it was showing.
							sleepTime = sleepTimeInitialized;
						}

						// do stuff that can only be done after we are fully initialized and configured:
						if (checkinProcessor == null) {
							checkinProcessor = new CheckinProcessor(this);
							checkinProcessor.start();
						}

						// Handle any deferred commands from prior passes:
						//  (make a copy of the commands, clear the main list, then process them; this
						//   allow us to rebuild the list of deferred commands as needed while processing.)
						if (deferredCommands.size() > 0) {
							Vector<ControllerCommand> v;
							// we need to clone the list and then erase it; but, other threads could
							// potentially add to the list diring this time. To be safe, we block on
							// the list itself to get a clone copy and then to empty it. By doing this
							// blocking, any insertions that could be added at the same time are ensured
							// to be added to the original list, or will be added to the cleared/empty list
							// after the clone is made. Otherwise, there's the small potential for a command
							// that is being added to the deferred list gets added between the cloning and
							// clear and therefore gets cleared and lost. (note that although the clone()
							// method is already synchronized, clear is not, nor is the clone/clear together
							// as a complete transaction.)
							synchronized (deferredCommands) {
								v = (Vector<ControllerCommand>) deferredCommands.clone();
								deferredCommands.clear();
							}
							processQueuedCommands(v);
						}
						// next, handle any current commands:
						if (queuedCommands.size() > 0)
							processQueuedCommands(queuedCommands);

						// check for conditions and things that may need to be done:
						// - check for app updates:
						appUpdater.updateCheck(false);

						// - if any additional commands got deferred, bypass the sleeping:
						//if (deferredCommands.size() > 0)
						//	continue;
					}

					// test the 'Get' functionality
					//testGet();

					if (!bControllerAbort && !ending)
						try {
							sleep(sleepTime);
						} catch (InterruptedException iex) {
							LSLogger.debug(TAG, "Controller thread wait interrupted.");
						} catch (Exception ex) {
							LSLogger.exception(TAG, "Run-wait exception: ", ex);
						}

				} while (!ending && !bControllerAbort);
			} catch (Exception ex) {
				LSLogger.exception(TAG, "CRITICAL Controller Run Error:", ex);
			}
			ending = true;
		}

		LSLogger.info(TAG, "Controller is ending.");
	}


	private static int prevreason = 0; // previous-loop-iteration gcm failure reason
	private static int gcmretrycount = 3; // gcm retry loop count; is used to allow things to initialize and retry.

	private void resetgcmretrycount() {
		gcmretrycount = 3;
	}

	/**
	 * Handles app/device initialization and dependencies/prerequisites, ensuring the device is properly
	 * set up and configured for mdm.
	 * @return true if the app is ready and all initializations and dependencies are complete, false otherwise.
	 */
	private boolean initializeApp() {
		boolean bInitialized = initInstallStatus();
		// Check/update initialization status and handle getting things needed
		if (!bInitialized) { // we have something(s) not set up yet

			// Check GCM status. First we check the prerequisites, mainly GSF, and if not there,
			//  start installation of GSF. IF GSF is there, attempt to register and get the GCM ID.
			if (!initializedGCM) {
				LSLogger.debug(TAG, "-GCM not initialized...");
				if (!registerGCMinprogress) {
					LSLogger.debug(TAG, "-checking GCM prerequisites...");
					int reason = Prerequisites.checkGcmPrerequisites(context);

					if (prevreason != 0) {
						LSLogger.debug(TAG, "-gcm prereqs re-check: prev=" + prevreason + " reason=" + reason);
						if (prevreason == Prerequisites.PREREQSTATE_gsfmissing)
							reason = prevreason;
					}
					prevreason = reason;

					if (reason != 0) { // pre-requisites not met; handle trying to resolve them:
						bControllerAbort = handleGcmPrereqIssues(reason);
						// re-get reason:
						reason = Prerequisites.checkGcmPrerequisites(context);
					}

					// pre-requisites are met; go ahead and try to get GCM registration:
					// (we could have had a pre-req issue and since resolved it above.)
					if (reason == 0) {

						// check previous 'attempts' via flags;
						int state = settings.getInitializationState();

						// if any o fthese are set, we tried to resolve possible issues:
						if ((state & (FLAG_GCMREG_gsfinstalled | FLAG_GCMREG_appreinstall)) != 0) {
							// if we havent tried rebooting already, we'll request a reboot after first failure.
							if ((state & FLAG_GCMREG_permsreboot) == 0) {
								settings.setInitializationStateFlag(FLAG_GCMREG_permsreboot);
								gcmretrycount = 1; // reboot after the first failure.
								LSLogger.debug(TAG, "setting gcm error retry to 1 to force quicker reboot upon error.");
							}
						}

						if (gcmretrycount-- > 0) {
							initializeGcmReceivers();
							LSLogger.debug(TAG, "-GCM registration starting...");
							statusMsg(context.getResources().getString(R.string.gcmstatus_gcm_init));
							registerGCMinprogress = true;
							bGcmRegisterAttempted = true;
							registerGcm();  // any errors will come back from the gcm callback method.
						} else { // gcm is still failing...let's try a reboot.
							bControllerAbort = true;
							if (errorCallback != null)
								errorCallback.onErrorCallback(Constants.ERRORTYPE_REBOOT,
										context.getResources().getString(R.string.gcmerror_reboottitle),
										context.getResources().getString(R.string.gcmerror_authfailed_reboot));
							terminate(); // we cant run and have a fatal error or need a reboot, so stop trying.
						}
					}

				} else {
					LSLogger.debug(TAG, "-GCM registration has already started.");
					registerGCMinprogress = false;
	        		/*if (gcmregretry>0) {
	        			gcmregretry--;
	        		} else {
	        			gcmregretry = 3;
	        			registerGCMinprogress = false;
	        		}
	        		*/
				}
			}

			if (initializedGCM) { // we can do these only after we have a GCM id
				statusMsg(""); // clear any prior status msg
				if (!settings.isEnrolled()) {
					LSLogger.debug(TAG, "-Organization information not present; enrollment is needed.");
					// Let's see if we have any queued commands, which we could if an enrollment command
					//  has been queued. And, we only want to process the enroll commands, if we have any.
					if (queuedCommands.size() > 0)
						processSelectiveQueuedCommands(queuedCommands, "EnrollToServerCommand");

				} else if (!initializedServerRegistration) {
					LSLogger.debug(TAG, "-Server registration not complete.");
					if (settings.hasRequiredMdmServerCommunicationsValues()) {
						///       		if (!registerGCMinprogress && initializedGCM) {
						LSLogger.debug(TAG, "-Server registration commencing.");
						registerWithMdmServer(true);
						///        		} else {
						///        			LSLogger.debug(TAG, "--deferring server registation until GCM completes.");
						///        		}
					} else {
						LSLogger.error(TAG, "SERVER CONFIGURATION DATA IS MISSING OR NOT SET!");
						// (note: the proper way to handle this is to enforce enrollment.
						// This should be dead code, but if it does get here, it is a critical error.)
					}
				}
			}
		}
		// re-check current status, in case it was updated:
		return initInstallStatus();
	}

	// Internal handling of GCM initialization and install related problems.
	private boolean handleGcmPrereqIssues(int reason) {
		int msgtitleid = 0;
		int msgtype = Constants.ERRORTYPE_FATAL;  // default message type
		boolean bNotifyMain = false;

		CommandResult results = new CommandResult();
		if (reason == Constants.GCM_ERROR_no_gsf) {
			int gcfstate = Prerequisites.getGsfState();
			switch (gcfstate) {//Prerequisites.getGsfState()) {
				case Prerequisites.PREREQSTATE_gsfmissing:
					LSLogger.debug(TAG, "-starting GSF installation...");
					statusMsg(context.getResources().getString(R.string.gcmstatus_gsf_installing));
					//statusMsg(context.getResources().getString(R.string.gcmstatus_gsf_dowloading));
					bControllerAbort = !Prerequisites.installGSF(this, results);
					//if (!bControllerAbort) 
					//	statusMsg(context.getResources().getString(R.string.gcmstatus_gsf_installing));
					break;
				case Prerequisites.PREREQSTATE_gsfinstalling:
					LSLogger.debug(TAG, "GSF is installing; please wait...");
					break;
				case Prerequisites.PREREQSTATE_gsfinstall_ok:
				case Prerequisites.PREREQSTATE_gsfinstall_completed:
					LSLogger.debug(TAG, "GSF Install completed successfully.");
					reason = Prerequisites.checkGcmPrerequisites(context);
					if (reason != Constants.GCM_ERROR_no_gsf) { // gsf is now present.
						interruptMainLoop();
						/**
						 // jump ahead and try again...or use the code below and force a reboot...
						 settings.setInitializationStateFlag(FLAG_GCMREG_gsfinstalled);
						 statusMsg(context.getResources().getString(R.string.gcmstatus_gsf_installed));
						 // bControllerAbort = true;
						 msgtype = Constants.ERRORTYPE_REBOOT;
						 reason = R.string.gcm_reboot_after_gsfintall;
						 msgtitleid = R.string.gcmerror_reboottitle;
						 **/
						break;
					} // else fall through to the install-failed handler:
				case Prerequisites.PREREQSTATE_gsfinstall_failed:
					// the installation attempt failed.
					LSLogger.error(TAG, "GSF Install failed.");
					statusMsg(context.getResources().getString(R.string.gcmstatus_gsf_installfailed));
					bControllerAbort = true;
					reason = R.string.gcmerror_gsfinsstallfailed;
					break;
			}
			/**** 
			 if (!gsfinstallstarted) {
			 gsfinstallstarted = Prerequisites.installGSF(this);
			 } else {
			 LSLogger.debug(TAG, "GSF Install already initiated.");
			 } *****/

		} else if (reason == Constants.GCM_ERROR_permissions) {
			// permissions is not set; this is handled by doing a reboot,
			// and if we already did that, then the app needs to be updated/re-installed.

			// can we stop the service and know how that went??? try it:
			// -> no help stopGcmService();

			int state = settings.getInitializationState();
			/**  -- skip this step..we dont need to do it since the reintall will be needed anyways.
			 if ((state & FLAG_GCMREG_permsreboot) == 0) {
			 // we havent tried it already, first try a reboot
			 settings.setInitializationStateFlag(FLAG_GCMREG_permsreboot);
			 msgtype = Constants.ERRORTYPE_REBOOT;
			 bControllerAbort = true;

			 } else
			 **/
			if ((state & FLAG_GCMREG_appreinstall) == 0) {
				// otherwise, if we tried a reboot once and still get the error, 
				// now we need to update/re-install mdm; we get here if we have not yet tried to reinstall:
				settings.setInitializationStateFlag(FLAG_GCMREG_appreinstall);
				msgtype = Constants.ERRORTYPE_REINSTALL;
				msgtitleid = R.string.gcmerror_msgtitle_reinstallmdm;
				reason = R.string.gcmerror_permissions_resintallprompt;
				bNotifyMain = true;
				ending = true;

				// NOW try reboot after we reintalled and are still getting errors.
			} else if ((state & FLAG_GCMREG_permsreboot) == 0) {
				// we havent tried it already, first try a reboot
				settings.setInitializationStateFlag(FLAG_GCMREG_permsreboot);
				msgtype = Constants.ERRORTYPE_REBOOT;
				bControllerAbort = true;

			} else { // we already tried a reintall and reboot, and still can't get it to work, or the reinstall failed.
				// at this point, we have a critical failure and just need to revert to default error handling.
				LSLogger.error(TAG, "Unrecoverable GCM errors.");
				reason = R.string.gcmerror_unrecoverable;
				bControllerAbort = true;
			}


		} else {  // unhandled prerequiste or other reason error.
			// reason and msgtitle are already set to proper values.
			LSLogger.error(TAG, "Aborting due to critical GCM error.");
			bControllerAbort = true;
		}

		if (bControllerAbort || bNotifyMain) {
			if (msgtitleid == 0)
				msgtitleid = R.string.gcmerror_msgtitle;
			String msgtitle = context.getResources().getString(msgtitleid);
			String msg = context.getResources().getString(reason);
			if (results.hasErrorMessage())
				msg += "\n(" + results.getErrorMessage() + ")";
			LSLogger.error(TAG, "GCM Prereq Error (" + reason + ") title=" + msgtitle + " msg=" + msg);
			if (errorCallback != null)
				errorCallback.onErrorCallback(msgtype, msgtitle, msg);
			if (bControllerAbort)
				terminate(); // we cant run and have a fatal error or need a reboot, so stop trying.
			//return false;
		}

		return bControllerAbort;
	}


	/**
	 * Checks if the system and settings are all present to get to a fully-initialized and ready state.
	 * @return true if gcm is ready, server settings are ready, device is enrolled, and org info is present.
	 */
	private boolean isInitialized() {
		return (isMdmReady());
	}

	/**
	 * Checks if the underlying system is fully-initialized and ready. This currently only consists of GCM
	 * and getting a GCM ID, along with the prerequisites for doing that.
	 *
	 * ONLY use this method if the device has returned false from @isMdmReady or @isInitialized.
	 *
	 * @return true if gcm is ready/initialized (a GCM ID is present), false otherwise.
	 */
	public boolean isSystemReady() {
		return (initializedGCM);
	}

	/* Returns true if gcm is ready, false if not. */
	private boolean isGcmReady() {
		return (initializedGCM);
	}

	/**
	 * Checks if the MDM app and sytem are fully-initialized and ready for use.
	 * @return true if system is ready, server settings are ready, device is enrolled, and org info is present.
	 */
	public boolean isMdmReady() {
		return (isSystemReady() && initializedServerRegistration && (settings.getOrganizationID() != null));
	}


	/*
	 * Determines installation/setting requirements, and sets appropriate flags.
	 * returns true if everything is present, false if something is not completely ready.
	 */
	private boolean initInstallStatus() {
		if (bForceInit) {
			bForceInit = false;
			settings.removeAll();
			settings.setDefaults();
			settings.initFromConfigFile();
			initializedGCM = false;
			initializedServerRegistration = false;
			return false;
		}

		int initState = settings.getInitializationState();
		String gcm_id = settings.getGcmID();
		if (gcm_id != null && (FLAG_GCMREG_ok & initState) != 0) {
			// we have the gcm registration
			initializedGCM = true;
		} else {
			initializedGCM = false;
			// gcm not present or we need to re-get it.
		}
		initializedServerRegistration = ((initState & FLAG_SVRREG_ok) != 0);

		return isInitialized();
	}


	/*
	 * Device is to be unenrolled. Set values needed to make that happen.
	 */
	public void unenroll() {
		try {
			LSLogger.debug(TAG, "Unenrolling.");
			getSettingsInstance().deleteOrganizationInfo();
			// clear any other commands that may be queued or in the background;
			//  this wont affect commands that are currently executing, but will stop any queued ones. 
			deferredCommands.clear();
			queuedCommands.clear();
			if (backgroundQueueHandler != null)
				backgroundQueueHandler.clearQueuedCommands();
			// notify main activity so the ui can handle it (set the active activity to main window)
			Utils.NavigateToMain(context, Intent.FLAG_ACTIVITY_TASK_ON_HOME);
			LSLogger.debug(TAG, "Unenroll completed.");
		} catch (Exception ex) {
			LSLogger.exception(TAG, "Unenroll error:", ex);
		}
	}


	// ----------
	// GCM things:
	// ----------	

	// -- note DO NOT stop the gcm service...it wont start back up when needed!!! (startService calls fail)
	/*
	private void stopGcmService() {
		try {
 		    Intent intent = new Intent("com.google.android.gcm.intent.STOP//");
	        intent.setPackage("com.google.android.gsf");// GSF_PACKAGE="com.google.android.gsf");
	        //intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
	        //        PendingIntent.getBroadcast(context, 0, new Intent(), 0));
	        intent.putExtra(GCMConstants.EXTRA_SENDER, Constants.GCM_SENDER_ID);			
			boolean stopped = context.stopService(intent);
			LSLogger.debug(TAG, "Attempting to stop GCM service. Stopped="+stopped);
			
		} catch (Exception ex) {
			LSLogger.exception(TAG, "stop gcm service error:", ex);
		}
	}
	*/

	/*** -- not used --
	 public void onServiceConnected(ComponentName className, IBinder service) {
	 // This is called when the connection with the service has been
	 // established, giving us the service object we can use to
	 // interact with the service.  Because we have bound to a explicit
	 // service that we know is running in our own process, we can
	 // cast its IBinder to a concrete class and directly access it.
	 //mBoundService = ((LocalService.LocalBinder)service).getService();

	 // Tell the user about this for our demo.
	 LSLogger.info(TAG, "BIND ServiceConnected: name="+(className==null?"(null)":className) + " service="+
	 (service==null?"null":service.toString()));
	 }

	 public void onServiceDisconnected(ComponentName className) {
	 // This is called when the connection with the service has been
	 // unexpectedly disconnected -- that is, its process crashed.
	 // Because it is running in our same process, we should never
	 // see this happen.
	 //mBoundService = null;
	 LSLogger.info(TAG, "BIND ServiceConnected: name="+(className==null?"(null)":className));
	 }

	 private boolean bgcmSeriviceBound;

	 private boolean RegisterGcmWithBind() {
	 boolean didbind = false;
	 try {
	 Intent intent = new Intent(GCMConstants.INTENT_TO_GCM_REGISTRATION);
	 intent.setPackage("com.google.android.gsf");// GSF_PACKAGE="com.google.android.gsf");
	 intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
	 PendingIntent.getBroadcast(context, 0, new Intent(), 0));
	 intent.putExtra(GCMConstants.EXTRA_SENDER, Constants.GCM_SENDER_ID);
	 didbind=context.bindService(intent, this, Context.BIND_AUTO_CREATE);
	 LSLogger.debug(TAG, "RegisterGcmBind result="+didbind);
	 } catch (Exception ex) {
	 LSLogger.exception(TAG, "RegisterGcmBIND error:", ex);
	 }
	 bgcmSeriviceBound = didbind;
	 return didbind;
	 }

	 private void UnregisterGcmWithBind() {
	 if (bgcmSeriviceBound) {
	 bgcmSeriviceBound = false;
	 LSLogger.debug(TAG, "Unregister GCM Bind");
	 context.unbindService(this);
	 }
	 }
	 ***/

	/**
	 * Sets the GCM registration ID and related initialization state values.
	 * @param gcmID GCM registration token for the device.
	 */
	public synchronized void setGcmIDRegistered(String gcmID) {
		// Save gcm id and update the status that we have it:
		if (gcmID != null && gcmID.length() > 0) {
			settings.setSetting(Settings.GCM_REG_ID, gcmID);
			settings.setInitializationStateFlag(FLAG_GCMREG_ok);
			initializedGCM = true;
			registerGCMinprogress = false;
			this.interrupt();
		}
	}

	/**
	 * Removes the GCM registration ID, and resets related initialization state values.
	 * @param gcmID optional ID to remove; if null, deletes the one in the settings storage.
	 */
	public synchronized void setGcmIDUnregistered(String gcmID) {
		// Remove GCM ID if its the one that we already have in place;
		boolean bDoRemove = (gcmID == null);
		if (!bDoRemove) {
			String currentRegID = settings.getGcmID();
			if (currentRegID != null && currentRegID.equals(gcmID))
				bDoRemove = true;
		}
		if (bDoRemove) {
			settings.clearInitializationStateFlag(FLAG_GCMREG_ok);
			settings.removeGcmID();
			initializedGCM = false;
		}
		registerGCMinprogress = false;
	}

	/***
	 private static int step = 0;

	 private void tryRegisterGcm() {
	 try {
	 if (step++==2) { // first try unregister
	 Intent intent = new Intent(GCMConstants.INTENT_TO_GCM_REGISTRATION);
	 intent.setPackage("com.google.android.gsf");
	 intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
	 PendingIntent.getBroadcast(context, 0, new Intent(), 0));
	 boolean cn=context.stopService(intent);
	 LSLogger.error(TAG, "STOPGCm service result="+cn);

	 }
	 if (step++==4) { // first try unregister
	 Intent intent = new Intent();//GCMConstants.INTENT_TO_GCM_REGISTRATION);
	 intent.setPackage("com.google.android.gsf");
	 intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
	 PendingIntent.getBroadcast(context, 0, new Intent(), 0));
	 boolean cn=context.stopService(intent);
	 LSLogger.error(TAG, "STOPGCm service with no target result="+cn);

	 }

	 Intent intent = new Intent(GCMConstants.INTENT_TO_GCM_REGISTRATION);
	 intent = new Intent("com.google.android.gcm.intent.RETRY");
	 intent.setPackage("com.google.android.gsf");// GSF_PACKAGE="com.google.android.gsf");
	 intent.putExtra(GCMConstants.EXTRA_APPLICATION_PENDING_INTENT,
	 PendingIntent.getBroadcast(context, 0, new Intent(), 0));
	 intent.putExtra(GCMConstants.EXTRA_SENDER, Constants.GCM_SENDER_ID);
	 ComponentName cn=context.startService (intent );//, this, Context.BIND_AUTO_CREATE);
	 if (cn == null) LSLogger.error(TAG, "StartGCm service failed; null.");
	 else {
	 LSLogger.debug(TAG, "StartGCM service OK; component=" + cn.toString());
	 }


	 } catch (Exception ex) {
	 LSLogger.exception(TAG,"TryRegisterGcm error:", ex);
	 }
	 }
	 ***/
	
	
	/*
	 * Gets current local GCM registration value, or requests a new one if we don't have one yet.
	 */
	private void registerGcm() {
		// Read existing GCM ID from local GCM storage if we have it; otherwise, request it:
		try {
			String regId = FirebaseInstanceId.getInstance().getToken();
			bGcmRegistered = true;
			setGcmIDRegistered(regId);

		} catch (Exception ex) {
			LSLogger.exception(TAG, "RegisterGCm error:", ex);
			registerGCMinprogress = false;
			bGcmRegisterAttempted = false;
			unregisterGcm(true);
		}
		/***
		 // Device is already registered on GCM, check server.
		 if (GCMRegistrar.isRegisteredOnServer(context)) {
		 // Skips registration.
		 mDisplay.append(getString(R.string.already_registered) + "\n");

		 } else {
		 // Try to register again, but not in the UI thread.
		 // It's also necessary to cancel the thread onDestroy(),
		 // hence the use of AsyncTask instead of a raw thread.
		 final Context context = this;
		 mRegisterTask = new AsyncTask<Void, Void, Void>() {

		@Override protected Void doInBackground(Void... params) {
		boolean registered =
		true;
		////                 ServerUtilities.register(context, regId);
		// At this point all attempts to register with the app
		// server failed, so we need to unregister the device
		// from GCM - the app will try to register again when
		// it is restarted. Note that GCM will send an
		// unregistered callback upon completion, but
		// GCMIntentService.onUnregistered() will ignore it.
		if (!registered) {
		GCMRegistrar.unregister(context);
		}
		return null;
		}

		@Override protected void onPostExecute(Void result) {
		mRegisterTask = null;
		}

		};
		 mRegisterTask.execute(null, null, null);
		 }
		 }
		 ***/

	}

	/**
	 * Checks GCM token lifetime value stored in GCM preferences (the "registered to server" value).
	 * If it is stale and needs refreshing, initiates the re-registration process.
	 *
	 * Note that this should NOT be called as the app starts up due to possible pending 
	 * GCM messages that have been sent but not yet received by the device. Instead and 
	 * ideally, this could be called after processing any pending messages, or after being
	 * idle or something; or, call it to forcefully do a re-register.
	 *
	 * @param bforce when true, forces the re-registration to occur. If false, lets the logic
	 * decide if a re-register is  needed.
	 */
	public void checkReRegisterGcm(boolean bforce) {
		if (bforce) {
			try {
				setGcmIDUnregistered(null);

				bGcmRegistered = false;

				// now, simply re-register:
				registerGcm();
			} catch (Exception ex) {
				LSLogger.exception(TAG, "CheckReRegisterGcm error:", ex);
			}
		}
	}

	// force unregiter of gcm service.
	private void unregisterGcm(boolean bforce) {
		try {
			setGcmIDUnregistered(null);

			bGcmRegistered = false;  // may already have been set to false, but do it anyway
			LSLogger.debug(TAG, "Unregistered GCM.");
		} catch (Exception ex) {
			LSLogger.exception(TAG, "UnregisterGcm error:", ex);
		}
	}


	/**
	 * Handles notification messages from GCMIntentService.
	 */
	private BroadcastReceiver mHandleGcmMessageReceiver =
			new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String msgAction = intent.getExtras().getString(MdmFirebaseMessagingService.EXTRA_MSGTYPE);
					String msgData = intent.getExtras().getString(MdmFirebaseMessagingService.EXTRA_MSGDATA);
					LSLogger.info(TAG, "seeing GCM Message for data=" + (msgData == null ? "(null)" : msgData) + " action=" + (msgAction == null ? "(null)" : msgAction));
					int msgCode = -1;
					try {  // extract the message code string value if present.
						String msgCodeStr = intent.getExtras().getString(MdmFirebaseMessagingService.EXTRA_MSGCODE);
						if (msgCodeStr != null)
							msgCode = Integer.valueOf(msgCodeStr);
					} catch (NumberFormatException nfex) {
						LSLogger.exception(TAG, "Exception getting int value for msgCodeStr.", nfex);
					}

					if (msgAction != null) {
						//LSLogger.info(TAG, "!!GCM-Broadcast Message received: " + msgAction);
						if (msgAction.equalsIgnoreCase(MdmFirebaseMessagingService.ACTION_MESSAGE)) {
							// .. process the message or action received:
							LSLogger.info(TAG, "Received GCM Message for CommandProcessing: data=" + (msgData == null ? "(null)" : msgData));
							CommandProcessor.handleGcmCommand(intent);

						} else if (msgAction.equalsIgnoreCase(MdmFirebaseMessagingService.ACTION_DISPLAYMSG)) {
							LSLogger.info(TAG, "Received GCM Message for Display:" + (msgData == null ? "(null)" : msgData));
							if (msgDisplayCallback != null && msgData != null)
								msgDisplayCallback.statusMsg(msgData);

						} else if (msgAction.equalsIgnoreCase(MdmFirebaseMessagingService.ACTION_REGISTERED)) {
							LSLogger.info(TAG, "Received GCM Message for Registration:" + (msgData == null ? "(null)" : msgData));
							setGcmIDRegistered(msgData);

						} else if (msgAction.equalsIgnoreCase(MdmFirebaseMessagingService.ACTION_UNREGISTERED)) {
							LSLogger.info(TAG, "Received GCM Message for Unregistration:" + (msgData == null ? "(null)" : msgData));
							setGcmIDUnregistered(msgData);

						} else if (msgAction.equalsIgnoreCase(MdmFirebaseMessagingService.ACTION_ERROR)) {
							LSLogger.error(TAG, "Received GCM Message with ERROR (" + msgCode + "): "
									+ (msgData == null ? "(null)" : msgData));
							handleGcmErrorMsg(msgCode);
						} else if (msgAction.equalsIgnoreCase(MdmFirebaseMessagingService.ACTION_RECOVERABLEERROR)) {
							LSLogger.error(TAG, "Received GCM Message with RECOVERABLE ERROR (" + msgCode + "): "
									+ (msgData == null ? "(null)" : msgData));
							handleGcmErrorMsg(msgCode);
						} else {
							LSLogger.warn(TAG, "GCM-Broadcast Message received with UNKNOWN ACTION: " + msgAction);
						}
					} else {
						LSLogger.warn(TAG, "GCM-Broadcast Message received with NO ACTION.");
					}

					//reset status
					if (registerGCMinprogress)
						registerGCMinprogress = false;
				}
			};


	// handles errors from gcm (callback msgcodes, and any other gcm processing)
	private void handleGcmErrorMsg(int msgCode) {
		boolean doUnreg = true;
		if (msgCode == Constants.GCM_ERROR_acctmissing) {
			// GSF expected a google account and didnt' find one.
			// This is usually due to an older version of GSF (pre 4.0.4) or no google account present.
			// -nothing really to do right now.
		} else if (msgCode == Constants.GCM_ERROR_authentication) {
			// gcm authentication failed due to a GSF/GCM internal error or our GCM account ID is bad.
			// -nothing really to do right now.
		} else if (msgCode == Constants.GCM_ERROR_servicenotavailable) {
			// if we have previously initialized gcm, we dont want to unregister it;
			//  so, set the unregister-flag so that it is false if gcm is ready
			doUnreg = !isGcmReady();
			// now, ensure wifi is ready:
			if (!WifiUtils.wifiIsConnected(context)) {
				WifiUtils.ensureConnected(context, 60, true);
				resetgcmretrycount(); // keep trying
				LSLogger.debug(TAG, "Wifi was not enabled; enabled and waiting to start up.");
			} // else
		} else {
			doUnreg = false;
		}

		if (doUnreg) {
			LSLogger.warn(TAG, "HandleGCMError - unregistering gcm. No messages will be received.");
			unregisterGcm(false);
		}
	}

	// --------------------------------
	// MDM Server Registration Support:
	// --------------------------------

	/*
     * Sends check-in/registration request to mdm server.
     * Updates settings as needed.
     * @param sendAll when true, sends all data to the server; when false, sends minimal data.
     * @return HttpCommResponse from the request, or null if the request failed.
     */
	public HttpCommResponse registerWithMdmServer(boolean sendAll) {
		HttpCommResponse resp = null;
		// assumes we have the info needed to communicate with the server:
		// (i.e., a call to settings.hasRequiredMdmServerCommunicationsValues() retured true.)

		String postUrl = ServerUrlProvider.getCheckinUrl(settings);
		LSLogger.info(TAG, "Registering with mdm server...");

		// put together the data needed for the registration, in a json object:
		JSONObject jparams = device.getStaticJSONparams(null, settings, sendAll);
		jparams = device.getVolitileJSONparams(jparams);

		if (jparams != null && jparams.has(Constants.PARAM_DEVICE_UDID)) {
			try {
				// add other values to the json request:
				jparams = batteryInfoProvider.getStatusAsJSONparams(jparams);
			} catch (Exception ex) {
				LSLogger.exception(TAG, "registerWithMdmServer error: ", ex);
			}

			// get Server Communications instance, then send 
			ServerComm server = new ServerComm();
			resp = server.postToServer(postUrl, jparams);

			LSLogger.debug(TAG, "RegisterToServer Http Post result code=" + Integer.toString(resp.getResultCode()));

			if (resp.isOK()) {
				settings.setInitializationStateFlag(FLAG_SVRREG_ok);
				settings.setLastSyncTime(System.currentTimeMillis()); //new Date().getTime()); //
				// update the sent-group-id if we changed it, so the id is not sent on subsequent checkins.
				String groupid = settings.getGroupID();
				if (groupid != null) {
					String sentID = settings.getSentGroupID();
					if (sentID == null || !sentID.equals(groupid))
						settings.setSentGroupID(groupid);
				}

				initializedServerRegistration = true;
				LSLogger.info(TAG, "MDM Server Registration succeeded.");
			} else { // server registration failed: do we need to retry, or is this a fatal error? ->retry.
				statusMsg("MDM Server Registration error: " + resp.getResultReason());
			}
		} else {
			// we don't have the json params with device id, so can't send the request.
			LSLogger.error(TAG, "SERVER REGISTRATION ERROR: Device ID missing or no JSON data.");
		}

		return resp;
	}

	// -----------------------------------
	// MDM server check-in/re-sync support 
	// -----------------------------------


	// --------------------------------
	// MDM Server Apps Sync Support:
	// --------------------------------

	/*
     * Sends apps info data to mdm server.
     * Updates settings as needed.
     * @param sendAll when true, sends all app data to the server; when false, sends minimal data (delta).
     * Note: settings may override this to define the bdefault behavior and force always
     * sending all.
     * @return HttpCommResponse from the request, or null if the request failed.
     */
	public HttpCommResponse sendAppsToMdmServer(boolean sendAll) {
		HttpCommResponse resp = null;
		JSONObject jactivities = null;
		JSONArray arrayactivities = null;
		// assumes we have the info needed to communicate with the server:

		// sendAll override based on settings: if requesting to not send all but settings say otherwise, change it.
		if (!sendAll && settings != null && settings.getAppSyncType() == Constants.APPSYNCTYPE_all)
			sendAll = true;

		String postUrl = ServerUrlProvider.getAppsDataCheckinUrl(settings);

		LSLogger.info(TAG, "Sending App info to mdm server...(send_all=" + sendAll + ")");

		DeviceApps devapps = new DeviceApps(context);
		JSONObject jparams = devapps.getDeviceAppsJSON(sendAll);
		if (jparams.length() > 0) {

			// add in identifier data (udid, etc.)
			jparams = device.getStaticJSONparams(jparams, null, false);

			if (jparams != null && jparams.has(Constants.PARAM_DEVICE_UDID)) {


				// add other required values to the json params:
				try {
					jparams.put(Constants.PARAM_ACTIVITY_TYPE, Constants.PARAM_TYPE_DEVICEAPPINFO);

					// the jparams needs to be added in as an array into activities. Create it now:
					// - create the array to hold the 'jparams' as an elemens in the array:
					arrayactivities = new JSONArray();
					arrayactivities.put(jparams);
					// - now, add the array to the top-level jactivities:
					jactivities = new JSONObject();
					jactivities.put(Constants.CMD_activities, arrayactivities);
					jactivities = device.getStaticJSONparams(jactivities, null, false);

				} catch (Exception ex) {
					LSLogger.exception(TAG, "Apps Sync json error:: ", ex);
				}

				if (jactivities != null && arrayactivities != null) {
					// get Server Communications instance, then send 
					ServerComm server = new ServerComm();
					resp = server.postToServer(postUrl, jactivities);

					//LSLogger.debug(TAG, "AppsSync Http Post result code=" + Integer.toString(resp.getResultCode()));

					if (resp.isOK()) {
						long currenttime = System.currentTimeMillis();
						settings.setLastAppsSyncTime(currenttime); //new Date().getTime()); //
						if (LSLogger.isLoggingEnabled())
							LSLogger.debug(TAG, "Apps Sync succeeded." + Utils.formatLocalizedDateUTC(currenttime));
					} else { // server registration failed: do we need to retry, or is this a fatal error? ->retry.
						LSLogger.error(TAG, "Apps Sync error: " + resp.getResultReason());
					}
				} else {
					LSLogger.error(TAG, "SERVER APPS ERROR: No JSON activities data.");
				}

			} else {
				// we don't have the json params with device id, so can't send the request.
				LSLogger.error(TAG, "SERVER APPS SYNC ERROR: Device ID missing or no JSON data.");
			}
		} else {
			LSLogger.debug(TAG, "No app data to sync. (sendall=" + sendAll + ")");
		}

		return resp;
	}

	// -------------------------------
	// MDM server apps re-sync support 
	// -------------------------------


	// -------------
	// Misc methods:
	// -------------

	// display messaging/logging convenience methods:
	private void statusMsg(String msg) {
		if (msgDisplayCallback != null)
			msgDisplayCallback.statusMsg(msg);
		else
			LSLogger.info(TAG, msg);
	}

	@SuppressWarnings("unused")
	private void progressMsg(String msg, int percent) {
		LSLogger.info(TAG, msg);
//		if (msgDisplayCallback != null)
		//		msgDisplayCallback.progressMsg(msg, percent);
	}


	/**

	 // for testing the http Get method.
	 private void testGet() {
	 String serverUrl = settings.getServerUrl();
	 //try {
	 //	LSLogger.info(TAG, "...waiting 35 seconds..");
	 //sleep(35000);
	 //} catch (Exception e) {
	 //	LSLogger.info(TAG,"INTERUPTED " + e.toString());
	 //}

	 LSLogger.info(TAG, "Testing GET to server " + serverUrl);

	 // get Server Communications instance, then send
	 ServerComm server = new ServerComm();
	 HttpCommResponse resp = server.getFromServer(serverUrl, null);

	 LSLogger.info(TAG, "Http Get result code=" + Integer.toString(resp.getResultCode()) + " - " + resp.getResultReason());
	 LSLogger.info(TAG, "-get data="+resp.getResultStr());

	 }
	 **/

	/**
	 * Callback invoked after processing of some command.
	 * This can get called upon a successful, failed, or cancelled command. 
	 * @param controllerCmd
	 * @param results
	 */
	public void handlePostMdmCommandProcessing(ControllerCommand controllerCmd, CommandResult results) {
		if (controllerCmd != null) {
			try {
				// handle managed app actions that may require special handling.
				if (controllerCmd instanceof com.lightspeedsystems.mdm.Controller.ManagedAppCommandHandler) {
					ManagedAppCommandHandler handler = (ManagedAppCommandHandler) controllerCmd;
					App app = handler.getApp();
					if (results == null)
						results = controllerCmd.getCommandResult();
					if (app != null && app.isMdmApp() && results != null) {
						// handle special pre-req gsf app installation results:
						if (app.getMdmAppType() == App.MDMAPPTYPE_perreq_gsf) {
							if (results.isSuccess()) { // gsf installed ok
								LSLogger.debug(TAG, "GSF Install completed (handlePostMdmCommandProcessing).");
								Prerequisites.setGsfState(Prerequisites.PREREQSTATE_gsfinstall_completed);
							} else { // gsf install failed, or was cancelled.
								LSLogger.debug(TAG, "GSF Install error (handlePostMdmCommandProcessing).");
								Prerequisites.setGsfState(Prerequisites.PREREQSTATE_gsfinstall_failed);
							}
							//Prerequisites.clearGsfInstallState();
							//LSLogger.debug(TAG, "GSF installed="+Prerequisites.isGsfInstalled()+" (gsfstate="+Prerequisites.getGsfState()+")");
							interruptMainLoop();
						} else {
							LSLogger.debug(TAG, "handlePostMdmCommandProcessing-Nothing to do for apptype" + app.getMdmAppType());
						}
					} else {
						LSLogger.warn(TAG, "handlePostMdmCommandProcessing-app is null or not mdmapp or no results. nothing to do.");
					}
				}
			} catch (Exception ex) {
				LSLogger.exception(TAG, "HandlePostMdmCommandProcessing error:", ex);
			}
		}
	}

	// --------------------------------------------
	// -- Commands Queued Control -----------------
	// ControllerCommands are queud up in a vector, adding to the end, pulling from element(0),
	// thereby providing a synchronized queue structure. 
	// As elements are pulled off, they are executed and handled as needed.
	// --------------------------------------------

	/*
	 * Processes all queued commands in the given list, starting at the top and working down.
	 */
	private void processQueuedCommands(Vector<ControllerCommand> list) {
		try {
			while (list.size() > 0) {
				ControllerCommand cmd = list.remove(0);
				// process the command, and if needed requeue it in the deferred list.
				LSLogger.debug(TAG, "ProcessingQueuedCommand cmd=" + cmd.toString());
				if (cmd.execute() == ControllerCommand.RESULTACTION_reprocess) {
					LSLogger.debug(TAG, "requeuing QueuedCmd " + cmd.toString());
					requeueCommand(cmd);
				} else {
					LSLogger.debug(TAG, "handling post-processing results for QueuedCmd " + cmd.toString());
					cmd.updateProcessingResults(null);
				}
			}
		} catch (Exception ex) {
			LSLogger.exception(TAG, "ProcessQueuedCommands error:", ex);
		}

	}

	/*
	 * Processes selective queued commands in the given list, only ControllerCommands matching the onlyClass. 
	 * Instances of that class name are removed and process, and leaves other commands in the list. 
	 */
	private void processSelectiveQueuedCommands(Vector<ControllerCommand> list, String onlyClass) {
		try {
			int readindx = 0;
			while (list.size() > 0 && readindx < list.size()) {
				ControllerCommand cmd = list.get(readindx);
				if (cmd != null && cmd.getClass().getName().contains(onlyClass)) {
					// process the command; remove it from the list first.
					list.remove(readindx);
					LSLogger.debug(TAG, "ProcessingSelectivedQueuedCommand cmd=" + cmd.toString());
					// process the command, and if needed requeue it in the deferred list.
					if (cmd.execute() == ControllerCommand.RESULTACTION_reprocess) {
						LSLogger.debug(TAG, "requeuing SelectiveQueuedCmd " + cmd.toString());
						requeueCommand(cmd);
					} else {
						LSLogger.debug(TAG, "handling post-processing results for SelectiveQueuedCmd " + cmd.toString());
						cmd.updateProcessingResults(null);
					}
				} else { // ignore the command for now
					readindx++;
				}
			}
		} catch (Exception ex) {
			LSLogger.exception(TAG, "ProcessQueuedCommands error:", ex);
		}

	}

	private void enqueueCommand(ControllerCommand cmd) {
		queuedCommands.add(cmd);
	}

	//private void enqueueDeferredCommand(ControllerCommand cmd) {
	//	deferredCommands.add(cmd);
	//}

	private void requeueCommand(ControllerCommand cmd) {
		deferredCommands.add(cmd);
	}

	private void enqueueBackgroundCommand(ControllerCommand cmd) {
		addToBackgroundHandler(cmd);
	}

	// returns true if there are any queued or in-process commands of anykind anywhere, 
	// including any in background processing.
	protected boolean isAnyCommandsInProcess() {
		BackgroundQueueHandler hndlr = backgroundQueueHandler;
		return (deferredCommands.size() > 0 || queuedCommands.size() > 0 ||
				(hndlr != null && !hndlr.isFinished()));
	}

	// public convenience methods for enqueing commands:

	/**
	 * Called to start the enrollment process, which calls to the server to get a configuration based on 
	 * an enrollment code.
	 */
	public void requestEnrollment(ThreadCompletionCallback callback) {
		LSLogger.debug(TAG, "Queuing enrollment request.");
		enqueueCommand(new EnrollToServerCommand(this, callback));
		try {
			this.interrupt();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "interruptEnrollment error: ", ex);
		}
	}


	/**
	 * Called to notify the controller to check for server commands.
	 */
	public void startProcessServerCommands() {
		enqueueCommand(new Controller.RetrieveCommandsFromServer(this));
		try {
			this.interrupt();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "interruptProcessServerCommands error: ", ex);
		}
	}


	/**
	 * Called to notify the controller thread to resync with server.
	 * @param sendAllData if true, request a full-resend of all device data. 
	 * Otherwise just resyncs with changed and possibly volatile values.
	 */
	public void requestServerSync(boolean sendAllData, ThreadCompletionCallback callback) {
		enqueueCommand(new ServerSyncCommand(this, sendAllData, callback));
		// also, let's re-sync app info, just send a delta of the data.
		requestAppsListSync(false, null);
		try {
			this.interrupt();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "interruptServerSync error: ", ex);
		}
	}

	/**
	 * Called to notify the controller thread to get apps list and send to the server.
	 * @param sendAllApps if true, request a full-resend of all device data.
	 * Otherwise just resyncs with changed and possibly volitile values.
	 */
	public void requestAppsListSync(boolean sendAllApps, ThreadCompletionCallback callback) {
		enqueueCommand(new SyncAppsToServerCommand(this, sendAllApps, callback));
		try {
			this.interrupt();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "interruptAppsSync error: ", ex);
		}
	}


	/**
	 * Called to notify the controller thread to check for an update to this mdm app.
	 */
	public void requestUpdateCheck(ThreadCompletionCallback callback) {
		LSLogger.debug(TAG, "Queuing update check request.");
		enqueueCommand(new UpdateCheckCommand(this, callback));
		try {
			this.interrupt();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "interruptUpdateCheck error: ", ex);
		}
	}

	/**
	 * Adds the request for a managed app action initiated from a command.
	 * @param cmd managed app command 
	 * @param jdata entire command in json data
	 * @param results processing results instance
	 * @param bSendResultsToServer true sends the results back to the MDM results server.
	 * false to send no results back to the server.
	 * @return true if the action was queued for processing, false otherwise.
	 */
	protected boolean enqueueManagedAppAction(String cmd, JSONObject jdata, CommandResult results, boolean bSendResultsToServer) {
		return enqueueManagedAppAction(cmd, null, jdata, results, bSendResultsToServer);
	}

	/**
	 * Adds the request for a managed app action from a command or app instance.
	 * @param cmd managed app command 
	 * @param jdata entire command in json data
	 * @param results processing results instance
	 * @param bSendResultsToServer true sends the results back to the MDM results server.
	 * false to send no results back to the server.
	 * @return true if the action was queued for processing, false otherwise.
	 */
	protected boolean enqueueManagedAppAction(String cmd, App installApp, JSONObject jdata,
											  CommandResult results, boolean bSendResultsToServer) {
		boolean bRc = false;

		// build app instance from command parameters or use the instance passed in:
		App app = installApp;
		if (app == null)
			app = Apps.getAppInstanceFromInstallParams(jdata);
		App appDifferences = null; // if cmd is for an existing app and the command has differences.
		if (app == null) {
			results.setErrorMessage("Invalid command format for " + cmd + ": application identification labels are missing.");
		} else {
			Apps appsInstance = getAppsInstance();
			// look for an existing app definition, ignoring any marked for or being uninstalled:
			App existingApp = appsInstance.findAppByPackageName(app.getPackageName(), App.INSTALLSTATE_uninstallmask);
			if (existingApp != null) { // use the existing app's instance if we found something.
				if (!app.compareSourceValues(existingApp))
					appDifferences = app;
				app = existingApp;   // note that this ignores anything in the command params; that might not be desired.
			}

			boolean bInstalling = (cmd.equalsIgnoreCase(Constants.CMD_INSTALLAPP));

			// note: if we are uninstalling and we did not find an existingApp, we can ignore the request.
			//  even if there is a queud-up pending install that should be uninstalled, that request
			//  to add the app would return an existingApp instance.			
			if (!bInstalling && existingApp == null) {
				LSLogger.warn(TAG, "Ignoring command to uninstall app " + app.getPackageName() + ", since it is already being uninstalled.");

			} else { 
				/*
				if (app.isStateUninstalling()) {
					// The app is already marked to be uninstalled or is being uninstalled.
					//  We'll need to use a new app instance once that uninstall completes, so
					//  create a 'new app' by simply re-parsing the command data:
				    app = Apps.getAppInstanceFromInstallParams(jdata);
				}
				*/

				// set state and other related processing values:
				if (bInstalling)
					app.setInstallCommandReceivedState();
				else
					app.setUninstallCommandReceivedState();
				appsInstance.saveApp(app);

				// create the command and enqueue it to the low-priority processing:
				ManagedAppCommandHandler handler =
						new ManagedAppCommandHandler(this, bInstalling, app, appDifferences, jdata, bSendResultsToServer);
				enqueueBackgroundCommand(handler);
				bRc = true;
				LSLogger.debug(TAG, "Enqueued cmd=" + cmd + " for app instance=" + app.toString());
				try {
					this.interrupt();
				} catch (Exception ex) {
					LSLogger.exception(TAG, "interruptMgdAppAction error: ", ex);
				}
			}
		}
		return bRc;
	}

	/**
	 * Adds the request to update this app. 
	 * @param app mdm app  
	 * @return true if the action was queued for processing, false otherwise.
	 */
	protected boolean enqueueMdmUpdateAction(App app) {
		boolean bRc = false;
		Apps appsInstance = getAppsInstance();

		app.setInstallCommandReceivedState();
		appsInstance.saveApp(app);

		// create the command and enqueue it to the low-priority processing:
		MdmAppUpdateHandler handler = new MdmAppUpdateHandler(this, app);
		requeueCommand(handler); // add to deferred queue:
		LSLogger.debug(TAG, "Enqueued Mdm Update for app instance=" + app.toString());
		try {
			this.interrupt();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "interruptMdmUpdateAppAction error: ", ex);
		}

		return bRc;
	}


	/**
	 * Callback for notifying the controller that a managed app action is now complete.
	 * @param app
	 * @param status
	 */
	protected void notifyManagedAppActionComplete(App app, int status) {
		BackgroundQueueHandler handler = backgroundQueueHandler;
		if (handler != null)
			handler.wakeupFromCommand(app);
		if (app != null && app.isMdmApp() && app.getMdmAppType() == App.MDMAPPTYPE_self)
			appUpdater.updateComplete(app, null, status);
	}

	// logic for initiating and creating the background processing thread. Starts up a new
	// thread if not already running, otherwise lets the existing thread run and adds to its queue.
	private synchronized void addToBackgroundHandler(ControllerCommand command) {
		BackgroundQueueHandler handler = backgroundQueueHandler;
		if (handler != null && handler.addCommand(command)) {
			// command was successfully added to background queue
			LSLogger.debug(TAG, "Added command to background queue: " + handler.toString() + " command=" + command.toString());
		} else {
			// the thread ended or is ending; we need to create a new instance and start it up.
			// create a new list and pass that in.
			Vector<ControllerCommand> list = new Vector<ControllerCommand>();
			list.addElement(command);
			backgroundQueueHandler = new BackgroundQueueHandler(context, this, list);
			try {
				backgroundQueueHandler.start();
				LSLogger.debug(TAG, "Created new background queue; added command: " + backgroundQueueHandler.toString() + " command=" + command.toString());

			} catch (Exception ex) {
				LSLogger.exception(TAG, "addToBackgroundHandler error:", ex);
			}
		}
	}

	//notification that the given background handler has finished
	private synchronized void backgroundHandlerFinished(BackgroundQueueHandler handler) {
		if (backgroundQueueHandler == handler)
			backgroundQueueHandler = null;
	}


	// Thread for handling the background commands. These run synchronously, until
	//  notification of completion was received, if indicated.
	// Normally, queued commands are processed one by one by calling the execute method; 
	//  once the execute completes, the next command would be processed. 
	// For these background commands, this is changed so that if a command is marked completed,
	//  the next command is handled; but if not complete and if the execute finishes, processing
	//  waits until a notification is received that the action is complete.
	private class BackgroundQueueHandler extends Thread {
		private final static String TAG = "BackgroundQueueHandler";
		@SuppressWarnings("unused")
		private Context context;
		private Controller controller;  // controller owning this thread; calls back into it as needed.
		private Vector<ControllerCommand> cmdsQueue;
		private int opState;
		private Object referenceObject;

		public BackgroundQueueHandler(Context context, Controller controller,
									  Vector<ControllerCommand> cmdsQueue) {
			super();
			this.context = context;
			this.controller = controller;
			this.cmdsQueue = cmdsQueue;
			opState = Constants.OPSTATUS_INITIALIZING;
		}

		// return true if any pending actions have completed, false if in-process
		public boolean isFinished() {
			return (Constants.OPSTATUS_COMPLETE == opState);
		}

		public void clearQueuedCommands() {
			if (cmdsQueue != null)
				cmdsQueue.clear();
		}

		/**
		 * Adds the command to the queue.
		 * @param cmd
		 * @return true if the command was added, false if the thread has ended or is ending.
		 */
		public synchronized boolean addCommand(ControllerCommand cmd) {
			if (opState == Constants.OPSTATUS_COMPLETE) {
				return false;
			}
			cmdsQueue.addElement(cmd);
			return true;
		}

		/**
		 * Internal method used to determine if processing is complete and set the
		 * state to complete if no other commands are left to process.
		 * @return
		 */
		private synchronized boolean setStateComplete() {
			if (cmdsQueue.size() != 0)
				return false;
			opState = Constants.OPSTATUS_COMPLETE;
			return true;
		}

		/**
		 * Callback for a waiting command, telling it to continue.
		 * @param obj Object applicable to the command; may be null if not needed.
		 */
		public void wakeupFromCommand(Object obj) {
			if (opState == Constants.OPSTATUS_WAITING || opState == Constants.OPSTATUS_RUNNING) {
				if (referenceObject != obj) {
					LSLogger.warn(TAG, "Unknown or different reference object in wakep for command. ref="
							+ (referenceObject == null ? "null" : referenceObject) + " obj=" + (obj == null ? "null" : obj));
				}

				//if (referenceObject != null && referenceObject == obj) {
				synchronized (this) {
					try {
						notifyAll();
						//this.interrupt();
					} catch (Exception ex) {
						LSLogger.exception(TAG, "interrupt error (" + toString() + "):", ex);
					}
				}
				//} else {
				//	LSLogger.warn(TAG, "Unknown or different reference object in wakep for command. ref="
				//			+(referenceObject==null?"null":referenceObject)+" obj="+(obj==null?"null":obj));
				//}
			}
		}

		/**
		 * Background thread for running and processing low-priority commands and actions.
		 * App installs and updates and uninstalls are included.
		 */
		public void run() {
			opState = Constants.OPSTATUS_RUNNING;
			try {
				do {
					while (cmdsQueue.size() > 0) {
						ControllerCommand cmd = cmdsQueue.remove(0);
						referenceObject = cmd.getReferenceObject();
						// process the command:
						if (cmd.execute() == ControllerCommand.RESULTACTION_ok) {
							// Command completed or was started. If the command is
							// synchronous, see if we need to wait for it to complete.
							// (this typically applies to tasks that are started with
							//  activities or other background or interaction actions).
							if (cmd.isSynchronous() && !cmd.isCompleted()) {
								opState = Constants.OPSTATUS_WAITING;
								LSLogger.debug(TAG, "Thread (" + toString() + ") waiting for cmd " + cmd.toString());
								synchronized (this) {
									try {
										wait(); //sleep();
									} catch (InterruptedException iex) {
										LSLogger.debug(TAG, "thread wait interrupted. " + toString());
									} catch (Exception ex) {
										LSLogger.exception(TAG, "Run-wait exception (" + toString() + "):", ex);
									}
								}
								opState = Constants.OPSTATUS_RUNNING;

							}
						}
						// results from the action need to be updated into the results in cmd:
						LSLogger.debug(TAG, "handling post-processing results for cmd " + cmd.toString());
						cmd.updateProcessingResults(null);

						// send result to server:
						if (cmd.isSendResultToServer()) {
							// prepare the data to send to the server with the results.
							JSONArray jsonResults = new JSONArray();
							CommandProcessor.addCommandResult(jsonResults, cmd.getJsonCmd(), cmd.getCommandResult());
							CommandProcessor.sendServerCommandResults(settings, jsonResults);
						}
					}
				} while (!setStateComplete());
			} catch (Exception ex) {
				LSLogger.exception(TAG, ex);
			}
			opState = Constants.OPSTATUS_COMPLETE;
			LSLogger.debug(TAG, "Thread ending: " + toString());
			controller.backgroundHandlerFinished(this);
		}

		public String toString() {
			String s = super.toString() + ";state=" + opState;
			return s;
		}
	}


	// -------------------------------------------------
	// Internal Controller Command objects support
	//  - these provide specific queable commands that can be performed.
	// -------------------------------------------------

	private abstract class ControllerCommand {
		public final static int RESULTACTION_ok = 0;
		public final static int RESULTACTION_reprocess = 1;
		public final static int RESULTACTION_error = 2;
		public final static int TASKTYPE_synchronous = 0; // the task is synchronous, wait until completion
		public final static int TASKTYPE_asynchronous = 1; // the task is asynchronous, do no wait for it.

		protected Controller controller;
		protected CommandResult commandResult;
		protected int taskType = TASKTYPE_asynchronous;
		protected boolean isComplete = false;
		protected boolean bSendResultToServer = false;
		protected Object refObject;
		protected JSONObject jsonCmd;  // original command json text

		public ControllerCommand(Controller controller) {
			this.controller = controller;
		}

		// returns true if the command is synchronous; wait until completed before processing the next one.
		public boolean isSynchronous() {
			return (taskType == TASKTYPE_synchronous);
		}

		public void setCompleted() {
			isComplete = true;
		}

		public boolean isCompleted() {
			return isComplete;
		}

		public boolean isSendResultToServer() {
			return bSendResultToServer;
		}

		/** Gets the original JSON command string, including parameters. */
		public JSONObject getJsonCmd() {
			return jsonCmd;
		}

		public CommandResult getCommandResult() {
			return commandResult;
		}

		public Object getReferenceObject() {
			return refObject;
		}

		/**
		 * Callback with optional results. The contents can be used to override a previous commandresult,
		 * or ignored as needed. This mechanism is in place so that an overriding result can be used as needed.
		 * @param result
		 */
		public void updateProcessingResults(CommandResult result) {
			// stub, not wanting to be abstract.
			if (result != null)
				commandResult = result;
		}

		public abstract int execute();
	}

	// --- sepcific implementations ---


	/*
	 * Provides processing for enrolling the device to the mdm server. 
	 * this sends an enrollment request, and expects a response with org-specific configuration data.
	 * Assumes the enrollment code and enrollment server url have been added to settings.
	 */
	private class EnrollToServerCommand extends ControllerCommand {
		ThreadCompletionCallback callbackObj;

		public EnrollToServerCommand(Controller controller, ThreadCompletionCallback callback) {
			super(controller);
			callbackObj = callback;
		}

		public int execute() {
			int action = RESULTACTION_ok;
			LSLogger.debug(TAG, "EnrollToServerCommand - execute.");
			// call method in commandprocessor to handle the action. that sends the
			//  request to the server and processes the result of a profile received as a response.
			CommandResult resp = CommandProcessor.processEnrollmentToServer(controller);
			// handle response by calling back to the thread.
			if (callbackObj != null)
				callbackObj.onThreadComplete(resp);
			// now we do something a little unusual: if the enrollment succeeded and gcm is ok,
			//  let's queue up a server sync; this will make the device call into the server 
			//  immediately, which, after such an enrollment, is important to admins watching for it.
			if (resp != null && resp.isSuccess()) {
				if (initializedGCM) // && !initializedServerRegistration)
					controller.requestServerSync(true, null);
			}
			isComplete = true;
			return action;
		}
	}


	/*
	 * Provides processing for syncing with the mdm server. This sends data about the device to the server,
	 * which could be full initial data (bSendAll) or just an update of things that can change.
	 */
	private class ServerSyncCommand extends ControllerCommand {
		boolean bSendAll;
		ThreadCompletionCallback callbackObj;

		public ServerSyncCommand(Controller controller, boolean sendAllData, ThreadCompletionCallback callback) {
			super(controller);
			bSendAll = sendAllData;
			callbackObj = callback;
		}

		public int execute() {
			int action = RESULTACTION_ok;
			LSLogger.debug(TAG, "ServerSyncCommand - execute.");
			HttpCommResponse resp = controller.registerWithMdmServer(bSendAll);
			// handle response by calling back to the thread.
			if (callbackObj != null)
				callbackObj.onThreadComplete(resp);
			isComplete = true;
			return action;
		}
	}

	/*
	 * Provides processing for syncing apps with the mdm server. This sends data about device apps to the server,
	 * which could be full initial data (bSendAll) or just a delta of things that changed since the last sync.
	 */
	private class SyncAppsToServerCommand extends ControllerCommand {
		boolean bSendAll;
		ThreadCompletionCallback callbackObj;

		public SyncAppsToServerCommand(Controller controller, boolean sendAll, ThreadCompletionCallback callback) {
			super(controller);
			bSendAll = sendAll;
			callbackObj = callback;
		}

		public int execute() {
			int action = RESULTACTION_ok;
			LSLogger.debug(TAG, "SyncAppsToServerCommand - execute.");
			HttpCommResponse resp = controller.sendAppsToMdmServer(bSendAll);
			// handle response by calling back to the thread.
			if (callbackObj != null)
				callbackObj.onThreadComplete(resp);
			isComplete = true;
			return action;
		}
	}


	/*
	 * Provides processing of getting and handling commands from the server.
	 */
	private class RetrieveCommandsFromServer extends ControllerCommand {
		public RetrieveCommandsFromServer(Controller controller) {
			super(controller);
		}

		public int execute() {
			int action = RESULTACTION_ok;
			LSLogger.debug(TAG, "RetrieveCommandsFromServer - execute.");
			try {
				if (CommandProcessor.processServerCommands(controller))
					action = RESULTACTION_reprocess;
			} catch (Exception ex) {
				LSLogger.exception(TAG, "RetrieveCommandsFromServer.execute error: ", ex);
			}
			isComplete = true;
			return action;
		}
	}

	/*
	 * Provides support for installing, updating, or uninstalling an application.
	 */
	private class ManagedAppCommandHandler extends ControllerCommand {
		private final static String TAG = "Controller.ManagedAppCommandHandler";
		boolean install; // true to install the app, false to uninstall it
		boolean bDeleteWhenDone; // true to delete the installer file upon completion.
		App app;
		App newApp; // optional instance with different or new source values (url, source, etc.)

		public ManagedAppCommandHandler(Controller controller, boolean bInstall,
										App app, App appWithDifferences, JSONObject jsonCommand,
										boolean sendResultToServer) {
			super(controller);
			taskType = TASKTYPE_synchronous;
			install = bInstall;
			this.app = app;
			newApp = appWithDifferences;
			refObject = app;
			bSendResultToServer = sendResultToServer;
			jsonCmd = jsonCommand;
			// for all apps other than mdm apps, delete when done if downloaded from a server to a temp file.
			bDeleteWhenDone = (app != null && !app.isMdmApp());
		}

		public int execute() {
			int action = RESULTACTION_ok;
			int installAction = 0;
			LSLogger.debug(TAG, " - executing app " + (install ? "Install" : "Uninstall") + " of app=" + app.toString());
			CommandResult results = new CommandResult();
			Apps appsInstance = getAppsInstance();
			if (newApp != null) {
				app.setSourceValues(newApp);
				//LSLogger.debug(TAG, "-updated app's source values: "+app.toString()+" -from instance: "+newApp.toString());
			}
			try {
				//if (CommandProcessor.processServerCommands(controller))
				//	action = RESULTACTION_reprocess;

				if (install) {
					installAction = Constants.ACTIONID_INSTALL;
					// let's see if the package is installed...may need to know this:
					boolean installed = Apps.isAppInstalled(context, app.getPackageName());
					LSLogger.debug(TAG, "App " + app.getPackageName() + " installed=" + installed);

					// determine how to install the app, and then do it:
					if (app.getInstallType() == App.INSTALLTYPE_ONLINESTORE) { // install from a remote store-based url:
						LSLogger.debug(TAG, "Installing remote package: " + app.getPackageName());
						appsInstance.installPackageFromStore(app, results);

					} else {  // download the installer file, then install it. 
						LSLogger.debug(TAG, "Installing package from file: " + app.getPackageName());
						// we can check to see if the user has allowed non-store apps to be installed:
						Apps.checkSettingsAllowAppInstalls(context);
						appsInstance.downloadAndInstallApp(app, results);
					}

				} else { // uninstall
					installAction = Constants.ACTIONID_UNINSTALL;
					LSLogger.debug(TAG, "Uninstalling package: " + app.getPackageName());
					app.setUninstallProcessingStartedState();
					appsInstance.saveApp(app);
					// first, make sure the app is installed on the system 
					//  (otherwise we'll get an error when trying to uninstall it.)
					if (Apps.isAppInstalled(context, app.getPackageName())) {
						Apps.uninstallPackage(app, results);
					} else { // app is not installed; just cleanup our data.
						LSLogger.info(TAG, "Ignoring uninstall of package " + app.getPackageName() + "; package not found.");
						app.setUninstallCompletedState(App.INSTALLSTATE_uninstalled, null);
						appsInstance.uninstallComplete(app);
						setCompleted();
					}

				}

			} catch (Exception ex) {
				app.setReason(LSLogger.exception(TAG, "execute error: ", ex));
				results.setException(ex);
			}

			// if we had an error or any kind of exception, the process failed, so update its state:
			if (results.hasErrorMessage()) {
				if (app.getReason() == null)
					app.setReason(results.getErrorMessage());
				appsInstance.setInstallCompletionResults(app, installAction, Constants.ACTIONID_ERROR);
				if (app.isMdmApp())
					handlePostMdmCommandProcessing(this, results);
			}

			if (app.hasInstallError())
				action = RESULTACTION_error;

			LSLogger.debug(TAG, "execute complete for app=" + app.toString());

			commandResult = results;
			return action;
		}

		/* Extract processing info from app instance and put into command results instance. */
		public void updateProcessingResults(CommandResult newresult) {
			LSLogger.debug(TAG, "updateProcessingResults app=" + (app == null ? "null" : app.toString()) + " delete=" + bDeleteWhenDone);
			if (app != null && commandResult != null) {
				if (app.hasInstallError()) {
					commandResult.setErrorMessage(app.getReason());
				} else {
					commandResult.setSuccess(true);
					if (app.equalsPackageName("com.lightspeedsystems.lightspeedsecurelauncher")) {
						String data = Settings.getInstance(context).getSetting("APP_LAUNCHER");

						Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage("com.lightspeedsystems.lightspeedsecurelauncher");
						getContext().startActivity(launchIntent);


						if (data != null) {

							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							Intent appLauncherIntent = new Intent();
							appLauncherIntent.setPackage("com.lightspeedsystems.lightspeedsecurelauncher");
							appLauncherIntent.setAction("com.lightspeedsystems.lightspeedsecurelauncher.action.mdm");
							appLauncherIntent.putExtra("EXTRA_APP_LAUNCH_DATA",data);
							getContext().sendBroadcast(appLauncherIntent);
						}
					}
				}
				// for mdm-related apps, call this special post-processing code.
				if (app.isMdmApp())
					handlePostMdmCommandProcessing(this, newresult);
			}
			if (bDeleteWhenDone && (app != null)) {
				// ... to do...delete the app installer file from the system.
				// -- only applies to downloaded-file type; for local files, we leave them, store-urls, ignore
				if (app.getInstallType() == App.INSTALLTYPE_REMOTEFILE) {
					try {
						String fn = app.getPackageFilePath();
						if (fn != null) {
							File file = new File(fn);
							boolean bdeleted = file.delete();
							LSLogger.debug(TAG, "post-app install, delete download file result=" + bdeleted);
						}
					} catch (Exception ex) {
						LSLogger.exception(TAG, "Error deleting install file after installation:", ex);
					}
				}
			}

		}

		public App getApp() {
			return app;
		}

		public String toString() {
			return super.toString() + ";installing:" + install + ";app:" + app.toString();
		}
	}


	/*
	 * Provides processing for doing an update check; this is not the updating itself, but the checking if
	 * an update is needed. This puts the Updater.updateCheck in a thread, so it can be done in the background.
	 */
	private class UpdateCheckCommand extends ControllerCommand {
		ThreadCompletionCallback callbackObj;

		public UpdateCheckCommand(Controller controller, ThreadCompletionCallback callback) {
			super(controller);
			callbackObj = callback;
		}

		public int execute() {
			boolean isupdating = controller.getUpdaterInstance().updateCheck(true);
			LSLogger.debug(TAG, "Update checked: update is needed=" + isupdating);
			if (callbackObj != null)
				callbackObj.onThreadComplete(null);
			isComplete = true;
			return RESULTACTION_ok;
		}
	}


	/*
	 * Provides support for updating this mdm application. Subclass of ManagedAppCommandHandler.
	 * This only does an install of the mdm app, per the info in the given app instance.
	 */
	private class MdmAppUpdateHandler extends ManagedAppCommandHandler {
		private final static String TAG = "Controller.MdmAppUpdateHandler";
		private boolean bDidUpdate;

		public MdmAppUpdateHandler(Controller controller, App app) {
			super(controller, true, app, null, null, false);
			//bSendResultToServer = false;
		}

		@Override
		public int execute() {
			// first, we make sure no other commands are queued or being processed; we cannot
			//  update our app if some other thing is waiting or underway, need to wait until those are complete:
			if (controller.isAnyCommandsInProcess()) {
				// re-queue this instance as another pending command until other things are complete:
				bDidUpdate = false;
				requeueCommand(this);
				return RESULTACTION_reprocess;
			}

			bDidUpdate = true;
			int action = RESULTACTION_ok;
			int installAction = Constants.ACTIONID_INSTALL;
			LSLogger.debug(TAG, " - executing Mdm Update: app=" + app.toString());
			CommandResult results = new CommandResult();
			Apps appsInstance = getAppsInstance();
			try {
				// determine how to install the app, and then do it:
				// (yes, it is possible that we may update from the online store. someday.)
				if (app.getInstallType() == App.INSTALLTYPE_ONLINESTORE) { // install from a remote store-based url:
					LSLogger.debug(TAG, "Installing remote package: " + app.getPackageName());
					appsInstance.installPackageFromStore(app, results);

				} else {  // download the installer file, then install it. 
					LSLogger.debug(TAG, "Installing package from file: " + app.getPackageName());
					// we can check to see if the user has allowed non-store apps to be installed:
					appsInstance.downloadAndInstallApp(app, results);
				}

			} catch (Exception ex) {
				app.setReason(LSLogger.exception(TAG, "Update execute error: ", ex));
				results.setException(ex);
			}

			// if we had an error or any kind of exception, the process failed, so update its state:
			if (results.hasErrorMessage() || app.hasInstallError()) {
				if (app.getReason() == null)
					app.setReason(results.getErrorMessage());
				LSLogger.debug(TAG, "SettingInstallCompletion error state in app=" + app.toString());
				appsInstance.setInstallCompletionResults(app, installAction, Constants.ACTIONID_ERROR);
			}

			if (app.hasInstallError())
				action = RESULTACTION_error;

			LSLogger.debug(TAG, "Update execute complete for app=" + app.toString());

			commandResult = results;
			return action;
		}

		/* Callback from after updating. Note that this should theoretically not be called, but
		 * will be on a cancelled update. On a successful update, the app will restart, so this
		 * wont get called. */
		public void updateProcessingResults(CommandResult newresult) {
			if (commandResult == null)
				commandResult = newresult;
			if (app != null && commandResult != null) {
				if (app.hasInstallError()) {
					commandResult.setErrorMessage(app.getReason());
					//LSLogger.debug(TAG, "Showing app  update error...");				
					if (errorCallback != null && context != null) {
						String msg = context.getResources().getString(R.string.msg_update_error);
						errorCallback.onErrorCallback(Constants.ERRORTYPE_TOASTMSG, null, msg);
					}
				} else {
					commandResult.setSuccess(true);
				}
			}
			// we need to check if we processed the update, because if bDidUpdate is false, we re-queued this.
			if (bDidUpdate) {
				controller.getUpdaterInstance().updateComplete(app, commandResult, (commandResult == null ? 0 : commandResult.getErrorCode()));
			} else {
				LSLogger.warn(TAG, "Update did not occur, but update may have been re-queued.");
			}
		}
	}

	private LocationManager locationManager;
	private boolean enableLocation;
	private int locationTime = 0; // in minutes
	private int locationDistance = 0; // in meters

	private LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(final Location location) {
			Thread t = new Thread() {
				public void run() {

					String serverCmd = "log/location?lat=" + location.getLatitude() + "&long=" + location.getLongitude()+"&udid="+settings.getDeviceUdid();

					String resultsUrl = settings.getServerResultsUrl();
					resultsUrl = Utils.endStringWith(resultsUrl, "/") + serverCmd;

					try {
						// build results data:
						JSONObject jdata = new JSONObject();

						LSLogger.debug(TAG, "Location Reporting to "+resultsUrl);
						ServerComm serverComm = new ServerComm();
						HttpCommResponse response = serverComm.postToServer(resultsUrl, jdata);
						if (response.isOK()) {
							LSLogger.debug(TAG, "Location Reported ok.");
						} else {
							LSLogger.error(TAG, "Location Reported error: " + response.getResultStr());
						}
					} catch (Exception ex) {
						LSLogger.exception(TAG, "Location Reported error:", ex);
					}
				}
			};
			t.start();

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		@Override
		public void onProviderEnabled(String provider) {

		}

		@Override
		public void onProviderDisabled(String provider) {

		}
	};

	/*
	starts up location reporting. timeLoc is in minutes and distLoc is in meters;
	timeLoc must always have value for enable. should be at least 5 for power constraints. this
	is the minimum time interval that must pass before a report is made.
	distLoc can be 0. this is the minimum distance that must exist since last report before another
	report occurs. if this value is 0 then only the time interval is used. if this value is greater
	than 0, then both a minimum time AND a minimum distance must both happen to have a report occur.

	returns last enable setting
	 */
	public boolean enableLocationReports(boolean enableLoc, int timeLoc, int distLoc) {
		boolean lastEnable = enableLocation;
		enableLocation = enableLoc;
		settings.setLastLocationEnable(enableLocation);
		locationTime = timeLoc;
		if(locationTime <= 1) {
			locationTime = 1;
		}
		settings.setLastLocationTime(locationTime);
		locationDistance = distLoc;
		settings.setLastLocationDistance(locationDistance);
		if (enableLoc) {
			if (locationManager == null) {
				locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			}
			if (ActivityCompat.checkSelfPermission(context.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				if (ActivityCompat.checkSelfPermission(context.getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, locationTime * 60 * 1000, locationDistance, locationListener, Looper.getMainLooper());
//					locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timeLoc * 1000, distLoc, locationListener, Looper.getMainLooper());

//					locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 1000, 0.0F, locationListener, Looper.getMainLooper());
				}
			}
		} else {
			if (locationManager == null) {
				locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
			}
			if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
				locationManager.removeUpdates(locationListener);
			}
		}

		return lastEnable;

	}

	public boolean getLocationReportEnable() {
		return enableLocation;
	}

	public int getLocationDistance() {
		return locationDistance;
	}

	public int getLocationTime() {
		return locationTime;
	}

	public void initializeLocationFromSettings() {
		boolean enable = settings.isLastLocationEnable();
		int time = settings.getLastLocationTime();
		int distance = settings.getLastLocationDistance();
		if(enable){
			enableLocationReports(enable,time,distance);
		}
	}

}
