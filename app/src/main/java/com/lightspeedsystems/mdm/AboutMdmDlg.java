package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;
import android.os.Bundle;
import android.os.Handler;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Provides the application's About popup window, shown as a dialog.
 * Displays version and relevant support information.
 */
public class AboutMdmDlg extends DialogFragment {
	private static String TAG = "AboutMdmDlg";
	private Button btnUpdateCheck;
	private Handler updateHandler; // handler for background processing of update request
	private boolean bIsUpdating;
	private boolean bProcessingCheck;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    // Get the layout inflater
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
	    View view = inflater.inflate(R.layout.about_dlg, null);

	    builder.setView(view)
	    	   .setIcon(R.drawable.ic_mdmapp)
	    	   .setTitle(R.string.menulist_about)
	    	   .setInverseBackgroundForced(true)	    	   
	    	   // Add action button:
	           .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	            	   // we don't need to process the OK button. Do nothing here.
	               }
	           });
	    Dialog dlg = builder.create();
	    
		TextView viewVersion = (TextView) view.findViewById(R.id.textVersion);
		TextView installDate = (TextView) view.findViewById(R.id.textInstallDate);
		TextView updateDate  = (TextView) view.findViewById(R.id.textUpdatedDate);
		btnUpdateCheck = (Button)view.findViewById(R.id.btnUpdate);

		btnUpdateCheck.setOnClickListener(new UpdateCheckButtonHandler());

		// get this app's info:
		Context context = getActivity();
		try {
			PackageInfo mdmPackage = context.getPackageManager().getPackageInfo(Constants.PACKAGE_NAME, 0);
			
			if (mdmPackage != null) {
				if (viewVersion != null) {
					viewVersion.setText(mdmPackage.versionName);
					LSLogger.debug(TAG, "AboutMdm: Version="+mdmPackage.versionName);
				}
				//SimpleDateFormat sdf = new SimpleDateFormat(); // use the local formatting options;
				// We use the default date-locale formatting to convert time-values to a date/time string.

				if (installDate != null) { 
					if (mdmPackage.firstInstallTime != 0) {
						//installDate.setText( sdf.format(new Date(mdmPackage.firstInstallTime)) );
						installDate.setText( Utils.formatLocalizedDateGMT(mdmPackage.firstInstallTime) ); 
						   //DateFormat.getDateTimeInstance().format(new Date(mdmPackage.firstInstallTime)) );
					}
				}
				if (updateDate != null) { 
					if (mdmPackage.lastUpdateTime != 0) {
						updateDate.setText( Utils.formatLocalizedDateGMT(mdmPackage.lastUpdateTime) ); 
								//DateFormat.getDateTimeInstance().format(new Date(mdmPackage.lastUpdateTime)) );
					} else {
						updateDate.setText(R.string.status_never);
					}
				}
			}
			
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}

	    return dlg;
	}	
	
	
	/*
	 * The process for performing an updatecheck is as follows.
	 * - Create a handler instance, if not already created.
	 * - Start a thread for doing the update checking (the update check calls to the server via http, 
	 *    and that needs to be in the thread vs.in the ui thread).
	 * - After the update check, use the handler to call back to the message handler (mUpdateCheckHandler),
	 *   back in the ui thread,
	 * - mUpdateCheckHandler calls updateCheckComplete to display messages.
	 */
	
	/**
	 * Inner class for handling the Update Check button click.
	 */
	class UpdateCheckButtonHandler implements View.OnClickListener  {
		 @Override
       public void onClick(View v) {
			 btnUpdateCheck.setEnabled(false);	
			 LSLogger.info(TAG, "Update-Check button pressed. Manually checking for updates...");
			 // handle the update check:
			 if (updateHandler == null)
				 updateHandler = new Handler();
			 
			 // -- pop up a message that the update check is occurring
			 Utils.showNotificationMsg(R.string.msg_updatecheck_inprogress);
			 bProcessingCheck = true;
			 Thread t = new Thread() {
			    public void run() {
			    	try {
			    		Updater updater = Controller.getInstance().getUpdaterInstance();
			    		if (updater != null) {
			    			bIsUpdating = updater.updateCheck(true);
			    		}
			    	} catch (Exception ex) {
			    		LSLogger.exception(TAG, "Update check thread exception:", ex);
			    	}
			    	
			        updateHandler.post(mUpdateCheckHandler);
			    }
			 };
			 t.start();
			 LSLogger.debug(TAG, "started background update check thread.");

       };
	}
	
	
	/*
	 * Message handler callback, gets called from handler from background thread.
	 */
    private final Runnable mUpdateCheckHandler = new Runnable() {
        public void run() {
        	updateCheckComplete();
        }
    };
		
    /**
     * Callback for handling the update check completion.
     */
    public void updateCheckComplete() {
		 // -- allow update to occur, or show 'up to date' message.
		 if (bIsUpdating) {  // app is updating itself
			 Utils.showNotificationMsg(R.string.msg_updatecheck_updating);
		 } else {   // app is up to date
			 Utils.showNotificationMsg(R.string.msg_updatecheck_complete);
		 }
		 
		 // now wait until completed before enabling the button again.
		 Thread t = new Thread() {
			    public void run() {
					 try {
						 do {
							 Thread.sleep(3000);
						 } while (bProcessingCheck); 
					 } catch (Exception ex) {}
			        updateHandler.post(mUpdateCheckPostDelayHandler);
			    }
		 };
		 t.start();

		 bProcessingCheck = false;
		 //btnUpdateCheck.setEnabled(true);	
   }

    // enable the button again.
    private final Runnable mUpdateCheckPostDelayHandler = new Runnable() {
        public void run() {
        	btnUpdateCheck.setEnabled(true);
        }
    };


}
