package com.lightspeedsystems.mdm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.lightspeedsystems.mdm.util.DataChangeListener;
import com.lightspeedsystems.mdm.util.LSLogger;

public class ServerUrlDlg extends DialogFragment {
	private static String TAG = "ServerUrlDlg";
	private EditText urltextCtrl; // edit control
	private String urltextValue;  // buffer for holding previous value
	private DataChangeListener listener;
	
	/**
	 * Sets the url value to be edited. 
	 * @param url
	 */
	public void setUrlTextValue(String url) {
		urltextValue = url;
	}
	
	/**
	 * Sets the callback listener for notifying when data changes.
	 */
	public void setChangeListener(DataChangeListener l) {
		listener = l;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    // Get the layout inflater
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
	    View view = inflater.inflate(R.layout.serverurl_dlg, null);

	    builder.setView(view)
	    	   .setIcon(R.drawable.ic_mdmapp)
	    	   .setTitle(R.string.serverurl_dlg_title)
	    	   .setInverseBackgroundForced(true)	    	   
	    	   // Add action button:
	           .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	            	   // process the OK button:
	            	   String url = urltextCtrl.getText().toString();
	            	   
	            	   // If the text is empty or all blanks, revert to the default url!
	            	   if (url != null && url.trim().length()==0)
	            		   url = Constants.DEFAULT_MDMSERVER_URL;
	            	   
	            	   if (urltextValue == null || (url != null && !urltextValue.equals(url))) {
	            		   	LSLogger.debug(TAG, "URL set to:" + url);	            		   	
	            		   	if (listener != null)
	            		   		listener.dataChanged("URL", url);
	            	   }
	               }
	           })
	           .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int id) {
	         	   // process the cancel button:
	         	   //dismiss(); - does this for us.
	            }
	        });
	    Dialog dlg = builder.create();
	    
	    urltextCtrl = (EditText) view.findViewById(R.id.textUrl);

		try {
			urltextCtrl.setText( urltextValue );
			
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}

	    return dlg;
	}	
	

}
