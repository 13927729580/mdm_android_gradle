package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

/**
 * IntentService responsible for handling GCM messages.
 * When a message is received, it is generally resent as a new Intent to the Controller for processing.
 * This is desirable over directly calling a method in Controller because the Controller will be running
 * as a separate thread from this Service, and we want to keep it separated that way.
 */
public class GCMIntentService extends GCMBaseIntentService {

    private static final String TAG = "GCMIntentService";
     
    /** Intent used to gcm action notification to controller: */
    static final String GCM_NOTIFICATION_MESSAGE =
             "com.lightspeedsystems.mdm.GCM_NOTIFICATION_MESSAGE";
    /** Intent's extra that contains the message to be displayed. */
    static final String EXTRA_MSGTYPE = "MSGTYPE";
    static final String EXTRA_MSGDATA = "MSGDATA";
    static final String EXTRA_MSGCODE = "MSGCODE";
    static final String ACTION_REGISTERED 	= "GCMREGISTERED";
    static final String ACTION_UNREGISTERED = "GCMUNREGISTERED";
    static final String ACTION_MESSAGE 		= "GCMMESSAGE";
    static final String ACTION_DISPLAYMSG 	= "GCMDISPLAYMESSAGE";
    static final String ACTION_ERROR 		= "GCMERROR";
    static final String ACTION_RECOVERABLEERROR	= "ACTION_RECOVERABLEERROR";
    
    
    public GCMIntentService() {
        super(Constants.GCM_SENDER_ID);
    }

    @Override
    protected void onRegistered(Context context, String registrationId) {
        LSLogger.info(TAG, "GCM Device registered. regId = " + registrationId);
        GCMRegistrar.setRegisteredOnServer(context, true);    
        
        notifyController(context, ACTION_REGISTERED, registrationId, 0);        
     //   Controller.getInstance(context).setGcmIDRegistered(registrationId);
        
        //displayMessage(context, getString(R.string.gcm_registered));
//        ServerUtilities.register(context, registrationId);
        
   
    }

    @Override
    protected void onUnregistered(Context context, String registrationId) {
    	LSLogger.info(TAG, "GCM Device unregistered.");
    	    	
        //displayMessage(context, getString(R.string.gcm_unregistered));
        /**/
        if (GCMRegistrar.isRegisteredOnServer(context)) {
        	GCMRegistrar.setRegisteredOnServer(context, false);
         ///   ServerUtilities.unregister(context, registrationId);
        } else {
            // This callback results from the call to unregister made on
            // ServerUtilities when the registration to the server failed.
            Log.i(TAG, "Ignoring unregister callback");
        }
        /**/
        notifyController(context, ACTION_UNREGISTERED, registrationId, 0);        
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
    	LSLogger.info(TAG, "GCM Received message");
    	      
		String data = intent.getStringExtra("data");
		notifyController(context, ACTION_MESSAGE, data, 0);
    }

    @Override
    protected void onDeletedMessages(Context context, int total) {
    	LSLogger.info(TAG, "GCM Received deleted messages notification.");
        String message = getString(R.string.gcm_deleted, total);        
    	notifyController(context, ACTION_DISPLAYMSG, message, 0);   	
     }

    @Override
    public void onError(Context context, String errorId) {
    	LSLogger.error(TAG, "GCM Received error: " + errorId);    	
        notifyController(context, ACTION_ERROR, errorId, getMessageCodeFromErrorMsg(errorId));
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
    	LSLogger.info(TAG, "Received recoverable error: " + errorId);
        notifyController(context, ACTION_RECOVERABLEERROR, errorId, getMessageCodeFromErrorMsg(errorId));
     //   displayMessage(context, getString(R.string.gcm_recoverable_error, errorId));
        return super.onRecoverableError(context, errorId);
    }

    /**
     * Notifies Controller to handle a gcm message.
     * <p>
     * This method is defined in the common helper because it's used both by
     * the UI and the background service.
     *
     * @param context application's context.
     * @param actiontype a GCM action type string.
     * @param data message to be displayed. Can be informational or error.
     * @param msgcode optional message code for an error.
     */
    private void notifyController(Context context, String actiontype, String data, int msgcode) {
    	// first, make sure the controller thread is started up: (it registers to receive gcm messages)
    	// (simply get the instance for it, that makes sure it's there and started:)
    	Controller controller = Controller.getInstance(context);
    	/**
    	if (controller != null && !controller.isMdmReady() && controller.isSystemReady()) {
    		LSLogger.debug(TAG, "Waiting for controller to initialize.");
    		// controller was just created, so we need to give it a little time to get ready.
    		for (int i=10; i>0 && !controller.isMdmReady(); i--) {
    			try {
    				Thread.sleep(1000);
    			} catch (Exception ex) {
    				LSLogger.exception(TAG, "notifyController wait exception:", ex);
    			}
    		}
    		if (!controller.isMdmReady())
    			LSLogger.warn(TAG, "Controller is not ready. GCM message may be ignored.");
    	}
    	**/
    	// now, create an intent and send it so the controller can receive it in its own thread:
        Intent intent = new Intent(GCM_NOTIFICATION_MESSAGE);
        intent.putExtra(EXTRA_MSGTYPE, actiontype);
        if (data != null)
        	intent.putExtra(EXTRA_MSGDATA, data);
        if (msgcode != 0)
        	intent.putExtra(EXTRA_MSGCODE, Integer.toString(msgcode));
        context.sendBroadcast(intent);
    }
    
    /* Gets a message code resource string identifier from a given string message. */
    private int getMessageCodeFromErrorMsg(String msg) {
    	int msgcode = 0;
    	if (msg != null && msg.length()>0) {
    		if (msg.contains(Constants.GCM_ERRORSTR_authentication))
    			msgcode = Constants.GCM_ERROR_authentication;
    		else if (msg.contains(Constants.GCM_ERRORSTR_acctmissing))
    			msgcode = Constants.GCM_ERROR_acctmissing;
    		else if (msg.contains(Constants.GCM_ERRORSTR_servicenotavailable))
    			msgcode = Constants.GCM_ERROR_servicenotavailable;
    	}
    	return msgcode;
    }
    
    /**
     * Issues a notification to inform the user that server has sent a message.
     */
    /*
    private static void generateNotification(Context context, String message) {
        int icon = R.drawable.ic_stat_gcm;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        String title = context.getString(R.string.app_name);
        Intent notificationIntent = new Intent(context, MainActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }
    */
    
    
    /**
     * Notifies UI to display a message.
     * <p>
     * This method is defined in the common helper because it's used both by
     * the UI and the background service.
     *
     * @param context application's context.
     * @param message message to be displayed.
     */
    /*
    private void displayMessage(Context context, String message) {
    	
        Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.sendBroadcast(intent);
        
    	//notifyController(context, ACTION_DISPLAYMSG, message);
    }
    */
 
    
     
    
}
