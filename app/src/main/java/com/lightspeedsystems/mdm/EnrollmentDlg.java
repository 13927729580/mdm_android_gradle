package com.lightspeedsystems.mdm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lightspeedsystems.mdm.util.DataChangeListener;
import com.lightspeedsystems.mdm.util.LSLogger;

public class EnrollmentDlg extends DialogFragment 
		implements ThreadCompletionCallback, DataChangeListener {
	private static String TAG = "EnrollmentDlg";
	private EditText enrollCodeCtrl;
	private TextView enrollUrlCtrl;
	private TextView statusMsgCtrl;
	private Button  btnEnroll;
	private Handler handler;
	private ThreadCompletionCallback callback;
	private DataChangeListener urllistener;
	private DataChangeListener parentListener; // caller/parent registered to listen to a callback.
	private Object callbackResultObject;
	private Settings settings;
	
	/**
	 * Sets a DataChangeListener callback for notifying when the enrolment completes.
	 * @param l Listener to register, null to clear.
	 */
	public void registerDataChangeListener(DataChangeListener l) {
		parentListener = l;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    // Get the layout inflater
	    LayoutInflater inflater = getActivity().getLayoutInflater();

	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
	    View view = inflater.inflate(R.layout.enroll_dlg, null);

	    builder.setView(view)
	    	   .setIcon(R.drawable.ic_mdmapp)
	    	   .setTitle(R.string.enroll_title)
	    	   .setInverseBackgroundForced(true)	    	   
	    	   // Add action buttons:
	    	   ;/*
	           .setPositiveButton(R.string.button_enroll, new EnrollButtonHandler())
	           .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	            	   dismiss();
	            	   
	               }
	           });*/
 
	    Dialog dlg = builder.create();
	    
	    try {
		    callback = this;
		    urllistener = this;
			enrollCodeCtrl = (EditText) view.findViewById(R.id.enrollCode);
			enrollUrlCtrl  = (TextView) view.findViewById(R.id.enrollServer);
			statusMsgCtrl  = (TextView) view.findViewById(R.id.statusMsg);
			
			btnEnroll = (Button)view.findViewById(R.id.btnEnroll);
			btnEnroll.setOnClickListener(new EnrollButtonClickHandler());
	
			settings = Controller.getInstance().getSettingsInstance();
			String ecode = settings.getEnrollmentCode();
			if (ecode != null)
				enrollCodeCtrl.setText(ecode);
			enrollUrlCtrl.setText(settings.getEnrollmentServer());
			
			enrollUrlCtrl.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
                {
            		LSLogger.debug(TAG, "Showing url edit dlg");
            		try {
            			clearStatusText();
	            	    ServerUrlDlg urlDlg = new ServerUrlDlg();
	            	    urlDlg.setUrlTextValue(Controller.getInstance().getSettingsInstance().getEnrollmentServer());
	            	    urlDlg.setChangeListener(urllistener);
	            	    urlDlg.show(getFragmentManager(), "lsmdm_svrurl_enrl");
            		} catch (Exception ex) {
            			LSLogger.exception(TAG, ex);
            		}
                }
        });
		
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}
	    return dlg;
	}	
	
	/*
	 * Internal method to clear the status text field.
	 */
	private void clearStatusText() {
		if (statusMsgCtrl != null)
			statusMsgCtrl.setText("");
	}
	
	/**
	 * DataChangeListener callback for notifying when a data change event has occurred.
	 * @param identifier optional string for identifying the data; can be null if not needed.
	 * @param newUrlValue new value. 
	 */
	public void dataChanged(String identifier, String newUrlValue) {
		LSLogger.debug(TAG, "serverurl changed; updating it to: " + (newUrlValue==null?"null":newUrlValue));
		try {
			settings.setMdmServerFromUrl(newUrlValue);
			settings.setEnrollmentServer(newUrlValue);
			enrollUrlCtrl.setText(newUrlValue);
		} catch (Exception ex) {
			statusMsgCtrl.setText(ex.getLocalizedMessage());
			LSLogger.exception(TAG, ex);
		}		
	}
	
	
	/*
	 * The process for performing an enrollment is as follows:
	 * - get the enrollment code and server url
	 * - start a controller command to do the enrollment; this queues up a background process in a separate thread.
	 * - controller process executes, calling the enrollment handling in CommandProcessor, which:
	 *   -- sends a enrollment command to the server
	 *   -- gets the results, applies them directly to settings if there are no errors.
	 * - the command callback back into this class occurs, to onThreadComplete
	 * - the handler is invoked, to call mEnrollCompleteMsgHandler
	 * - mEnrollCompleteMsgHandler is run, which does the ui updates and closes the window upon success
	 * - if successful, this dialog is closed and control is returned to the main window, which updates
	 *  the org name info if it was set.
	 */
	
	/*
	 * validates user input. returns null if input is ok, error message if there is an error.
	 */
	private String checkInputValid(String enrollCode, String enrollServer) {
		String msg = null;
		if (enrollCode == null || enrollCode.length()==0) {
			msg = getActivity().getResources().getString(R.string.enrollcode_missing);
		} else { // validate enrollment code: must contain no spaces, just alhpa's and numerics.
			char[] chars = enrollCode.toCharArray();
			for (int i=0; i<chars.length; i++) {
				if (!Character.isLetterOrDigit(chars[i]) && chars[i] != '-') {
					msg = getActivity().getResources().getString(R.string.enrollcode_invalid);
					break;
				}
			}
		}
		if (msg == null) {
			if (enrollServer == null || enrollServer.length()==0) {
				msg = getActivity().getResources().getString(R.string.enrollsvr_missing);
			} else { // validate enrollment serer url, althoguh it should already be valid.:
			}
		}
		return msg;
	}
	
	class EnrollButtonClickHandler implements View.OnClickListener  {
		 @Override
        public void onClick(View v) {
        	try {
        		clearStatusText();
    			String enrollcode = enrollCodeCtrl.getText().toString();
    			String enrollsvr = enrollUrlCtrl.getText().toString();
    			String msg = checkInputValid(enrollcode, enrollsvr);
    			if (msg != null) {
    				LSLogger.error(TAG, msg);
    				// display message in status area:
    				statusMsgCtrl.setText(msg);
    				
    			} else {
    	        	 LSLogger.debug(TAG, "Enrolling " + enrollcode + " to " + enrollsvr );    	        	 
    				 btnEnroll.setEnabled(false);
    				 // display 'enrolling' status msg
    				 statusMsgCtrl.setText(R.string.status_enrolling);
    				 Controller controller = Controller.getInstance();
    				 // save the values we're using:
    				 controller.getSettingsInstance().setEnrollmentCode(enrollcode);
    				 controller.getSettingsInstance().setEnrollmentServer(enrollsvr);
    				 // ensure the callback handler is allocated and ready:
    				 if (handler == null)
    					 handler = new Handler();

    				 controller.requestEnrollment(callback);
    				 /**
    				 // start a background thread to 
    				 Thread t = new Thread() {
    					    public void run() {
    					    	Controller.getInstance().requestEnrollment(callback);    					    	
    					        
    					    }
    				 };
    				 t.start();
    				 **/
    			}
       		
        	} catch (Exception ex) {
        		LSLogger.exception(TAG, ex);
        	}
        };
	}	

	/**
	 * Called when a thread completes, within the thread's thread context.
	 * Starts up a handler to notify the Ui thread that the enrollment process is complete.
	 * Note that this method will execute in a background thread, hence it cannot directly set ui values.
	 * @param obj optional parameter passed back from the enrollment.
	 */
	public void onThreadComplete(Object obj) {
		callbackResultObject = obj;
		handler.post(mEnrollCompleteMsgHandler);
	}
	
	/*
	 * Message handler callback, gets called from handler from background thread.
	 */
    private final Runnable mEnrollCompleteMsgHandler = new Runnable() {
        public void run() {
        	//enrollProcessingComplete();
    		LSLogger.debug(TAG, "Enroll completion callback. obj=" +(callbackResultObject==null?"null":callbackResultObject.toString()));
    		btnEnroll.setEnabled(true);
    		// handle result of callback...
    		if (callbackResultObject != null && (callbackResultObject instanceof CommandResult)) {
    			CommandResult resp = (CommandResult)callbackResultObject;
    			if (resp.isSuccess()) {
    				//  complete; exit and revert control back to the caller.
    				dismiss();
    				if (parentListener != null)
    					parentListener.dataChanged(Constants.PARAM_ORGID, null);
    			} else {
    				//String msg = getActivity().getResources().getString(R.string.status_error) + " " + resp.getErrorMessage();
    				//statusMsgCtrl.setText(msg);
    				statusMsgCtrl.setText(resp.getErrorMessage()); //just show the returned message.
    				// ... could show a status message icon here, too...for error, busy, etc....
    			}
    		} else {
    			clearStatusText();
    		}
     
        }
    };
	
}
