package com.lightspeedsystems.mdm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Provides a popup dialog box, of different styles. Used for obtaining a selection or just a simple 'ok'.
 * The creator calls this identifying what buttons are to be shown, along with a callback for receiving 
 * completion notification.
 */
public class PopupMessageDlg extends DialogFragment {
		//private final static String TAG = "PopupMessageDlg";
		
		/** Displays a popup message with an OK button. */
		public final static int DLGTYPE_ok 	  = 0x001;
		/** Displays a popup message with an OK button and a Cancel button. */
		public final static int DLGTYPE_okcancel = 0x003;
		/** Displays a popup message with Yes and No buttons. */
		public final static int DLGTYPE_yesno    = 0x010;
		/** Displays a popup message with Yes, No, and Cancel buttons. */
		public final static int DLGTYPE_yesnocancel = 0x020;
		
		private final static int DLGTYPE_DEFAULT = DLGTYPE_ok; // this is the default dialog type
		
		// buttons pressed
		public final static int BUTTON_ok 	  = 0x001;
		public final static int BUTTON_cancel = 0x002;
		public final static int BUTTON_yes    = 0x010;
		public final static int BUTTON_no     = 0x020;
		public final static int BUTTON_neutral= 0x080;
	
		private String msgText;
		private String msgTitle;
	    private PopupMessageCallbackInterface callback;
		private int dlgIdentifier = 0;
		private int btnSelected;
		private int positiveButtonIdentifier;
		private int negativeButtonIdentifier;
		private int neutralButtonIdentifier;
		private int positiveButtonResourceID;
		private int negativeButtonResourceID = 0;
		private int neutralButtonResourceID = 0;
		private int iconResourceID = 0;
		
		/**
		 * Creates an instance of the dialog.
		 * @param dialogStyle one of the DLGTYPE_ values
		 * @param title string for the title.
		 * @param msg string for the message.
		 * @param c optional callback to get called when a button is pressed.
		 */
		public PopupMessageDlg(int dialogStyle, String title, String msg, PopupMessageCallbackInterface c) {
			super();
			msgTitle = title;
			msgText = msg;
			callback = c;
			if (dialogStyle == 0)
				dialogStyle = DLGTYPE_DEFAULT;
			setCharacteristics(dialogStyle);
		}
		
		// internal method for setting the style of the dialog, including buttons, etc.
		private void setCharacteristics(int dialogType) {
			//LSLogger.debug(TAG, "Setting up dialog type="+dialogType);
			
			switch (dialogType) {
				// Handle yes/no/cancel dialogs:
				case DLGTYPE_yesnocancel:
					neutralButtonResourceID = android.R.string.cancel;
					neutralButtonIdentifier = BUTTON_neutral;
				case DLGTYPE_yesno:
					negativeButtonResourceID = R.string.button_no;
					negativeButtonIdentifier = BUTTON_no;
					positiveButtonResourceID = R.string.button_yes;
					positiveButtonIdentifier = BUTTON_yes;
					iconResourceID = android.R.drawable.ic_dialog_alert;
					break;
					
				// handle OK and OK/Cancel dialogs:
				case DLGTYPE_okcancel:
					negativeButtonResourceID = android.R.string.cancel;
					negativeButtonIdentifier = BUTTON_cancel;
				case DLGTYPE_ok:
				default:
					positiveButtonResourceID = android.R.string.ok;
					positiveButtonIdentifier = BUTTON_ok;
					iconResourceID = android.R.drawable.ic_dialog_info;
					break;
			}
		}
		
		/**
		 * Sets the string resource ID of the positive button (ok, yes, go, etc). 
		 * the default is the android ok button.
		 * @param resourceID
		 */
		public void setPositiveButtonResourceID(int resourceID) {
			positiveButtonResourceID = resourceID;
		}
		
		/**
		 * Sets the string resource ID of the cancel or no button, if shown.
		 * @param resourceID
		 */
		public void setNegitiveButtonResourceID(int resourceID) {
			negativeButtonResourceID = resourceID;
		}
		
		/**
		 * Sets the string resource ID of the center/neutral button, if shown.
		 * @param resourceID
		 */
		public void setNeutralButtonResourceID(int resourceID) {
			neutralButtonResourceID = resourceID;
		}
		
		/**
		 * Sets the resource ID of a drawable icon.
		 * @param resourceID
		 */
		public void setIconResourceID(int resourceID) {
			iconResourceID = resourceID;
		}
		
		/**
		 * Sets an identifier that can be used to associate a dialog with a callback result.
		 * @param id dialog ID.
		 */
		public void setDialogIdentifier(int id) {
			dlgIdentifier = id;
		}
		
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        		builder.setMessage(msgText)
	        		.setTitle(msgTitle)
	                .setPositiveButton(positiveButtonResourceID, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   btnSelected = positiveButtonIdentifier;
	                   }
	               });
	        		
	        if (negativeButtonResourceID != 0)
	        	builder.setNegativeButton(negativeButtonResourceID, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       // User cancelled the dialog
	                	   btnSelected = negativeButtonIdentifier;
	                   }
	               });
	        if (neutralButtonResourceID != 0)
	        	builder.setNeutralButton(neutralButtonResourceID, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                	   btnSelected = neutralButtonIdentifier;
	                   }
	               });
	        if (iconResourceID != 0)
	        	builder.setIcon(iconResourceID);
	        
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	    
	    @Override
	    public void onDismiss(DialogInterface dialog) {
	    	super.onDismiss(dialog);
			if (callback != null)
				callback.popupMessageCompleteCallback(dlgIdentifier, btnSelected);
	    }
	    
	    
	    public interface PopupMessageCallbackInterface {
	    	/**
	    	 * Callback to provide the results of which button was pressed.
	    	 * @param identifier the dialog identifer, if one was set via @setDialogIdentifier. 
	    	 * @param status the button selected, one of the BUTTON_ values. Depends on the style of the dialog.
	    	 */
	        public void popupMessageCompleteCallback(int identifier, int status);
	    }
}
