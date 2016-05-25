package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.Controller;
import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Handles periodic device check-in processing after a device has been registered.
 * This creates a thread that waits until it it time to check in.
 */
public class CheckinProcessor extends Thread implements ThreadCompletionCallback {
	private final static String TAG = "CheckinProcessor";
	
	private Controller controller;
	private Settings   settings;
	private boolean    ending;
	
	/** Constructor. */
	public CheckinProcessor(Controller ctrl) {
		controller = ctrl;
		if (controller != null)
			settings = controller.getSettingsInstance();
	}
	
	/** 
	 * Ends the Checkin thread.
	 */
	public void terminate() {
		try {
			if (!ending)
				this.interrupt();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "interrupt error: ", ex);
		}
		ending = true;
	}
	
	/**
	 * Request a checkin to the server.
	 */
	public void checkinNow() {
		//Long currentTime = System.currentTimeMillis();
		//settings.setLastSyncTime(currentTime);
		controller.requestServerSync(false, this);
	}
	
	/**
	 * Callback for when a server checkin completes; gets called from controller.
	 * @param obj Object reference for passing data.
	 */
	public void onThreadComplete(Object obj) {
		// do nothing; this is provided here for future possible needs.
	}
	
	/**
	 * Thread runner.
	 */
	public void run() {
		long nextCheckinTime = 0; 
		
		LSLogger.info(TAG, "Starting Checkin Scheduler...");
		
	    do {

	        // get the checkin sleep interval. we get it as minutes, then convert to millisecs.
	    	// (do it in this loop, so we get any changes and the latest sync time).
	    	Settings.SyncSettings sync = settings.getSyncSettings();
	    	
	    	long waitmins = sync.getNextSyncIntervalMinutes();
	    	
	    	if (waitmins > 0) {
	    		nextCheckinTime = waitmins * 60000; // convert to milliseconds.
		    	try {
		    		LSLogger.debug(TAG, "Checkin Scheduler waiting for " + waitmins+" minutes...");
		    		if (settings != null)
		    			settings.setNextSyncTime(System.currentTimeMillis() + nextCheckinTime);
		    		sleep(nextCheckinTime);
		    		checkinNow();
		    	} catch (InterruptedException iex) {
		    		LSLogger.debug(TAG, "Checkin Scheduler wait interrupted.");
		    	} catch (Exception ex) {
		    		LSLogger.exception(TAG, "Run-wait exception: ", ex);
		    	}
	    	} else {
	    		LSLogger.debug(TAG, "Checkin Scheduler ending due to no wait. (waitmins="+waitmins+")");
	    		ending = true;
	    	}
	    } while (!ending);
		
		LSLogger.info(TAG, "Checkin Scheduler is ending.");
	}

}
