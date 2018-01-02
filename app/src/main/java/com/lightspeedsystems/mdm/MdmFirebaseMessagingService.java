package com.lightspeedsystems.mdm;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Created by Robert T. Wilson on 5/26/16.
 */
public class MdmFirebaseMessagingService  extends FirebaseMessagingService{

    private static final String TAG = "MdmFirebaseMessagingService";

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


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if(remoteMessage != null) {
            String msg = remoteMessage.getData().toString();
            LSLogger.debug(TAG, "onMessageReceived  getData "+remoteMessage.getData().toString());
            if(remoteMessage.getNotification() != null) {
                msg = remoteMessage.getNotification().getBody();
                LSLogger.debug(TAG, "onMessageReceived  getNotification "+remoteMessage.getNotification().getBody());
            }
            if(!TextUtils.isEmpty(msg)) {
                if(msg.contains("{data={")) {
                    LSLogger.debug(TAG, "onMessageReceived  notifyController "+msg.substring(6,msg.length()-1));
                    notifyController(this.getBaseContext(), ACTION_MESSAGE, msg.substring(6,msg.length()-1), 0);
                }
            }
        }

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

         if (controller != null && !controller.isMdmReady() && controller.isSystemReady()) {
             LSLogger.debug(TAG, "Waiting for controller to initialize.");
             // controller was just created, so we need to give it a little time to get ready.
             for (int i=10; i>0 && !controller.isMdmReady(); i--) {
                 try {
                     Thread.sleep(500);
                 } catch (Exception ex) {
                     LSLogger.exception(TAG, "notifyController wait exception:", ex);
                 }
             }
             if (!controller.isMdmReady())
                 LSLogger.warn(TAG, "Controller is not ready. GCM message may be ignored.");
         }

        LSLogger.debug(TAG, "Sending Broadcast.");
        // now, create an intent and send it so the controller can receive it in its own thread:
        Intent intent = new Intent(GCM_NOTIFICATION_MESSAGE);
        intent.putExtra(EXTRA_MSGTYPE, actiontype);
        if (data != null)
            intent.putExtra(EXTRA_MSGDATA, data);
        if (msgcode != 0)
            intent.putExtra(EXTRA_MSGCODE, Integer.toString(msgcode));
        context.sendBroadcast(intent);
    }


}
