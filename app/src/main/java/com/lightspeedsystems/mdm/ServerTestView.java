package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ServerTestView extends DialogFragment {
	private static String TAG = "ServerTestView";
	private int testState;	// operational state of the testing process.
	
	private TextView view_url;
	private TextView view_results;
	private Button btnDone;
	private Button btnStop;
	private Button btnRun;
	private String serverUrlStr;
	private ServerTestTask testRunner;
	
	public static void showDialog(Activity parentActivity) {
		   DialogFragment newFragment = new ServerTestView();
		   newFragment.show(parentActivity.getFragmentManager(), "lsmdm_svrtest");
	}
	
	// constructor for DialogFragment
	public ServerTestView() {
		super();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.server_test, container);
		view_url = (TextView) view.findViewById(R.id.textSvrUrl);
		view_results = (TextView) view.findViewById(R.id.textResult);
		
		btnDone = (Button)view.findViewById(R.id.buttonDone);
		btnStop = (Button)view.findViewById(R.id.buttonStop);
		btnRun  = (Button)view.findViewById(R.id.buttonRun);
		
		Settings settings = Settings.getInstance(getActivity());
		if (settings != null) {
			serverUrlStr = settings.getServerUrl();
			if (view_url != null)
				view_url.setText(serverUrlStr);
		}

        getDialog().setTitle(R.string.diaglist_servertest);  
        btnDone.setOnClickListener(new View.OnClickListener() {
	               @Override
	               public void onClick(View v) {
	            	   dismiss();
	               }});
        btnStop.setOnClickListener(new StopTestButtonHandler()); 
        btnRun.setOnClickListener(new RunTestButtonHandler());
        updateButtonStates();
        return view;
    }
	
	
	class StopTestButtonHandler implements View.OnClickListener  {
		 @Override
         public void onClick(View v) {
         	LSLogger.info(TAG, "Stop button pressed. Interrupting processing.");
         	try {
         		// note: it's possible that testRunner could get set to null before this
         		// code can fully complete. Hence the possible exception we could get is
         		// a nullpointerexception. If that happens, it's not an error and we really
         		// don't care, since the test will have stopped or completed already.
         		if (testRunner != null && !testRunner.isCancelled()) {
         			testState = Constants.OPSTATUS_INTERRUPTED;
         			testRunner.cancel(true);
         		}
         	} catch (Exception ex) {
         		LSLogger.exception(TAG, "Stop Server Test Exception", ex);
         	}
         	btnStop.setEnabled(false);
         };
	}

	class RunTestButtonHandler implements View.OnClickListener  {
		 @Override
        public void onClick(View v) {
        	LSLogger.info(TAG, "Run button pressed.");
			testState = Constants.OPSTATUS_INITIALIZING;
			updateButtonStates();
			testRunner = new ServerTestTask();
			view_results.setText("");
			testRunner.execute();
        };
	}

	/**
	 * Updates the state of the buttons, so based on what is occurring in the testing.
	 * The Stop button is enabled only during the running of the test, at which time the
	 * other buttons are disabled.
	 */
	private void updateButtonStates() {
		boolean activateStop = ((testState==Constants.OPSTATUS_RUNNING) ||
						     (testState==Constants.OPSTATUS_INITIALIZING));
		boolean activateRunCancel = !activateStop;
		btnDone.setEnabled(activateRunCancel);
		btnRun.setEnabled(activateRunCancel);
		btnStop.setEnabled(activateStop);		
	}
	
	/**
	 * Background task for loading the Managed Applications' list.
	 * I use a background task because it could take awhile to load the apps and build the list.
	 * In the interim, a "Loading" message is displayed until the list loads, then when done loading,
	 * the text message is removed.
	 */
	private class ServerTestTask extends AsyncTask<Void, Void, Void> implements ProgressCallbackInterface {
		HttpCommResponse resp = null;
		ServerComm server = null;
		protected void onPreExecute() {
			testState = Constants.OPSTATUS_RUNNING;
		}
		protected Void doInBackground(Void... params) {
			if (serverUrlStr == null) {
				LSLogger.warn(TAG, "Failed to execute server test: server url is null.");
			} else {
				LSLogger.info(TAG, "Executing server test to " + serverUrlStr);
			
				// get Server Communications instance, then send 
				server = new ServerComm();	
			    resp = new HttpCommResponse(getActivity());
				resp.setInstrumentation(true);
				resp.setProgressListener(this);
			    resp = server.getFromServer(serverUrlStr, null, resp);
			    LSLogger.info(TAG, "Completed server test to " + serverUrlStr);
			}
			return null;
		}
		
		// @Override
	    protected void onProgressUpdate(Void... values) {
	    	LSLogger.info(TAG, "progress msg received.");
	    	if (resp != null)
	    		view_results.setText(resp.getInstrumentationCurrentStatusMsg());
	    }
	       
	    // Called when the thread task completes and posts its results back to the UI thread
	    // (here, 'Post' has nothing to do with HTTP posting; the callback result is Posted to the UI thread.)
		protected void onPostExecute(Void result) {
			LSLogger.debug(TAG, "onPostExecute");
			//if (testState != Constants.OPSTATUS_INTERRUPTED) 
			testState = Constants.OPSTATUS_COMPLETE;
			if (resp != null) {
				LSLogger.info(TAG, "Http Get result code=" + Integer.toString(resp.getResultCode()) + " - " + resp.getResultReason());
				//LSLogger.info(TAG, "-get data="+resp.getResultStr());
				if (resp.isOK()) {
					String iMsg = resp.getInstrumentationFinalSummaryMsg();
					Resources res = getActivity().getResources(); 
					view_results.setText(res.getString(R.string.servertest_pass) +
							    (iMsg==null?"":"\n"+iMsg)); //resp.getResultReason());							
				} else {
					// with errors, there are 2 types: a server-returned error will return an http code;
					// or an exception will leave the result code 0 but have exception details.
					if (resp.getResultCode() != 0) {
						view_results.setText(Integer.toString(resp.getResultCode()) + " - " +resp.getResultReason());
					} else if (resp.hasException()) {
						Exception ex = resp.getException();
						String msg = ex.getLocalizedMessage();
						if (msg == null || msg.length()==0)
							msg = ex.getMessage();
						if (msg == null || msg.length()==0)
							msg = ex.toString();
						view_results.setText(msg);
					} else {
						// unknown error information
						view_results.setText(R.string.servertest_fail);
					}
				}
			}
			
			testRunner = null;
			updateButtonStates();
		}
		
		protected void onCancelled() {
			testState = Constants.OPSTATUS_COMPLETE;
			updateButtonStates();
			view_results.setText(R.string.servertest_interrupted);
		}
		
		/*protected void interruptProcessing() {
			if (server != null) {
				try {
					this.notifyAll();
				} catch (Exception ex) {
					LSLogger.exception(TAG, "AsyncTask interruption exception.", ex);
				}
			}
		}*/
		
		// progresslistener interface methods:
		
		/**
		 * Handles a string status message
		 * @param message - text message to handle.
		 */
		public void statusMsg(String message) {
			publishProgress();
		}
		
		/**
		 * Handles progress of a task
		 * @param message
		 * @param percentage 0-100 of completion 
		 */
		public void progressMsg(String message, int percentage) {
			publishProgress();
		}

	}
	
	
	
	
	
	// --- old and not used ---
	
//	@Override
	public Dialog x_onCreateDialog(Bundle savedInstanceState) {
	   // AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    // Get the layout inflater
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
	    View view = inflater.inflate(R.layout.server_test, null);

	    /*
	    builder.setView(view)
	    	   .setIcon(R.drawable.ic_mdmapp)
	    	   .setTitle(R.string.diaglist_servertest)
	    	   .setInverseBackgroundForced(true)	
	    	   .setCancelable(false)
	    	   // Add action button:
	    	   .setNegativeButton(R.string.button_done, new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	            	   dismiss();
	               }
	           })
	    	   .setNeutralButton(R.string.button_stop, new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	            	   LSLogger.info(TAG, "Stop button pressed.");
	               }
	           })
	           .setPositiveButton(R.string.button_go, new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int id) {
	            	   LSLogger.info(TAG, "Go button pressed.");
	               }
	           });
	    Dialog dlg = builder.create();
	    */
	    
		TextView view_url = (TextView) view.findViewById(R.id.textSvrUrl);
		//TextView view_results = (TextView) view.findViewById(R.id.textResult);
		
		Settings settings = Settings.getInstance(getActivity());
		if (settings != null) {
			serverUrlStr = settings.getServerUrl();
			if (view_url != null)
				view_url.setText(serverUrlStr);
		}

		
	    return null;//  dlg;
	}	


}
