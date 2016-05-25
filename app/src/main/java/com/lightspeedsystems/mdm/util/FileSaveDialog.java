package com.lightspeedsystems.mdm.util;

import java.io.File;

import com.lightspeedsystems.mdm.CommandResult;
import com.lightspeedsystems.mdm.Constants;
import com.lightspeedsystems.mdm.PopupMessageDlg;
import com.lightspeedsystems.mdm.R;
import com.lightspeedsystems.mdm.Utils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FileSaveDialog extends DialogFragment implements PopupMessageDlg.PopupMessageCallbackInterface {
	private static String TAG = "FileSaveDialog";
	private Button btnSave;
	private Button btnCancel;
	private Button btnBrowse;
	private TextView filepathView;
	private TextView filenameView;
	private String savefilePath;
	private File initialPath;
	private int  titleResourceId = R.string.filesavedlg_title;
	private Activity activity;
	private FileSaveCallback filesavecallback;
	private int saveokNotifResource = R.string.notif_filesaved_ok;
	private boolean bHideBrowse = true;

	private static int FILEOVERWRITECONFIRM = 1;
	private static int STATE_FILEOVERWRITECONFIRMED = 1; // when a file exists and an overwrite was confirmed.
	
	public FileSaveDialog(Activity parentActivity) {
		activity = parentActivity;
	}
	
	/**
	 * Sets the initial target path for the file. Browsing can occur only in sub-folders (children of) this path.
	 * @param path Absolute path to store the file in.
	 */
	public void setPath(File path) {
		initialPath = path;
	}
	
	/**
	 * Sets the string resource ID of the title to be shown.
	 * @param resourceID String resource identifier.
	 */
	public void setTitleResource(int resourceID) {
		titleResourceId = resourceID;
	}
	
	/**
	 * Sets the message resource to be shown when a successful save has been processed.
	 * @param resourceID String resource ID of message.
	 */
	public void setSuccessNotificationResource(int resourceID) {
		saveokNotifResource = resourceID;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    // Get the layout inflater
	    LayoutInflater inflater = activity.getLayoutInflater();
	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
	    View view = inflater.inflate(R.layout.filesave_dlg, null);

	    builder.setView(view)
	    	   .setIcon(R.drawable.ic_mdmapp)
	    	   .setTitle(titleResourceId)
	    	   .setInverseBackgroundForced(true) ;
	    
	    Dialog dlg = builder.create();
	    
		filepathView = (TextView) view.findViewById(R.id.textFilepath);
		filenameView = (TextView) view.findViewById(R.id.textFilename);
		if (initialPath != null)
			filepathView.setText(initialPath.getAbsolutePath());
		
		btnSave = (Button)view.findViewById(R.id.btnOK);
		btnSave.setOnClickListener(new OnSaveButtonHandler());
		btnCancel = (Button)view.findViewById(R.id.btnCancel);
		btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
         	   dismiss();
            }
        });
		
		btnBrowse = (Button)view.findViewById(R.id.btnBrowse);
		btnBrowse.setOnClickListener(new OnBrowseButtonHandler());
		if (bHideBrowse)
			btnBrowse.setVisibility(View.GONE);

	    return dlg;
	}	


	/**
	 * Inner class for handling the OK button click.
	 * This validates the file, prompts if it exists, and initiates the write callback, displaying a status 
	 * message as needed.
	 * Upon success, closes the dialog. Upon failure, shows error message and leaves the dialog up.
	 */
	class OnSaveButtonHandler implements View.OnClickListener  {
		 @Override
       public void onClick(View v) {
	        processSaveAction(0); 
       };
	}

	
 	
	/**
	 * Inner class for handling the OK button click.
	 */
	class OnBrowseButtonHandler implements View.OnClickListener  {
		 @Override
       public void onClick(View v) {
         	LSLogger.debug(TAG, "Browse button pressed, but not implemented. File="+savefilePath);
        };
	}
	
	
	/**
	 * Callback for popup messages. Handles confirmation and actions that need action before a save can be done.
	 */
	public void popupMessageCompleteCallback(int identifier, int status) {
			// if fatal error, we are done; so shut app down.
		LSLogger.debug(TAG,"popup callback identifier="+identifier + " status="+status);
		if (identifier == FILEOVERWRITECONFIRM) {
			if (status == PopupMessageDlg.BUTTON_yes || status == PopupMessageDlg.BUTTON_ok) {
				processSaveAction(STATE_FILEOVERWRITECONFIRMED);
			}
		}
		btnSave.setEnabled(true);
	}
		
	/*
	 * Internal method to handle file saving action. The state is used to alter behavior.
	 * This can get called from a callback from an overwrite confirmation dialog, with a different actionstate.
	 * 
	 * This method takes the path and filename fields, validates the filename, and then tries
	 * to save the file. If the file exists, a prompt is shown. If the file save goes on, the callback to do
	 * the save is called, passing the File intance to the callback. The results are obtained from the callback, 
	 * and an error is shown or a confirmation message is shown.
	 */
	private void processSaveAction(int actionState) {
		btnSave.setEnabled(false);	
		CommandResult commandResult = new CommandResult();
 		Resources resources = activity.getResources();
		 
 		String filename = filenameView.getText().toString();
 		if (filename == null || filename.trim().length() == 0) {
    		Utils.showPopupMessage(activity, resources.getString(R.string.filesave_error_title), 
     				resources.getString(R.string.filesave_error_filename));     	
    		btnSave.setEnabled(true);	
    		return;
 		}
 		
    	try {
	     	savefilePath = filepathView.getText().toString(); //path.getAbsolutePath();
	     	
	     	@SuppressWarnings("unused")
			File fpath = new File(savefilePath);
	     	//LSLogger.debug(TAG, "path writeabe="+fpath.canWrite()+"  readable="+fpath.canRead() + " actionState="+actionState);
	     	
	     	if (savefilePath != null && savefilePath.length()>1)
	     		savefilePath = Utils.endStringWith(savefilePath, "/");
	     	else
	     		savefilePath="./";
	     	savefilePath += filename;

 
      		// validate file:
     		File file = new File(savefilePath);
     		if (file.exists() && (actionState != STATE_FILEOVERWRITECONFIRMED)) {
     			//commandResult.setErrorMessage("File Exists");
        		PopupMessageDlg dialog = 
        				new PopupMessageDlg(PopupMessageDlg.DLGTYPE_okcancel, //yesnocancel, 
        						resources.getString(R.string.filesave_overwriteconfirm_title),
        						resources.getString(R.string.filesave_msg_overwriteconfirm), this);
           		//LSLogger.debug(TAG, "Creating file overwrite confirmation dialog="+dialog.toString());
           		dialog.setDialogIdentifier(FILEOVERWRITECONFIRM);
           	    dialog.show(getFragmentManager(), "mdm_popup");
           	    return;
     		} 

     		if (!file.canWrite()) {
     			file.setWritable(true);
     		}
     		//if (!file.canWrite()) {
     		//	commandResult.setErrorMessage("Unable to create file.");        			
     		//} //else

 			// save the file
 			LSLogger.info(TAG, "Saving to file "+savefilePath);
 			         			
 			// do the write if we can: 
 			if (filesavecallback != null) {
 				//boolean bSaved = 
 				filesavecallback.onFileSave(Constants.ACTIONSTATUS_OK, file, commandResult);
 			} else {
 				commandResult.setErrorMessage(resources.getString(R.string.internalerror));
 				LSLogger.error(TAG, "No callback defined in processSaveAction; nothing to do.");
 			}
 			if (commandResult.isSuccess()) {
 				dismiss();
 				Utils.showPopupMessage(activity, 
 						resources.getString(R.string.filesave_completed_title),
 						resources.getString(saveokNotifResource));
 				return;
 			}
     		
     	} catch (Exception ex) {
     		commandResult.setException(ex);
     	}
     	
     	if (commandResult.hasErrorMessage())
     		Utils.showPopupMessage(activity, resources.getString(R.string.filesave_error_title), 
     				commandResult.getErrorMessage());
     	
     	btnSave.setEnabled(true);	 
	}
	
	/**
	 * Sets a callback that gets called to do the file saving action.
	 * @param callback a FileSaveCallback implementation, or null to clear a previous callback.
	 */
	public void registerFileSaveCallback(FileSaveCallback callback) {
		filesavecallback = callback;
	}
	
	/**
	 * Callback interface for notification when things occur or are completed.
	 */
	public interface FileSaveCallback {
		
		/**
		 * Callback when a file-save or other file-save-related operation is ready to occur or has occurred.
		 * The action code defines the state of events of the callback invocation.
		 * @param action a Cosntants.ACTIONSTATUS_ value, indicating why this is getting called. 
		 * Possible values include:
		 *  ACTIONSTATUS_OK: the file is ready to be written to or operated upon.
		 *  ACTIONSTATUS_CANCELLED: user cancelled the action without saving the file.
		 *  ACTIONSTATUS_FAILED: a critical error or failure occurred.
		 * @param file File instance of the target file to be written to or operated on.
		 * @param commandResult optional CommandResult instance for holding error and result data.
		 * @return true if the action completed successfully, false if not.
		 */
		public boolean onFileSave(int action, File file, CommandResult commandResult);
	}
	
}
