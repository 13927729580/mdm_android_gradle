package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.DataChangeListener;
import com.lightspeedsystems.mdm.util.LSLogger;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Main UI for the MDM app.
 */
public class MainActivity extends Activity implements ProgressCallbackInterface, DataChangeListener,
												ErrorCallbackInterface, 
												//.MainWindowNavigationInterface,
												PopupMessageDlg.PopupMessageCallbackInterface,
												UserAuthenticationView.UserAuthLoginCallback {
    private static final String TAG = "MainActivity";
    
    private Controller controller;
    private Context context;
    private MainActivity mainActivityInstance;

    private TextView orgNameCtrl;
    private TextView imageHintCtrl;
    private Button loginBtn;
    private ImageView enrollBtn;
    private TextView statusMsgCtrl;

    private boolean systemInitialized;
    private boolean bDisplayEnroll; // flag used on a new startup where enrollment needs to be popped up
    private Handler handler;
    private ErrorInfo currentError;
    private String statusMsgText;
    private boolean bIsActive;
    private boolean bEnrollForcedLogin; // if true, user tried to change enrollment, but needs to login as admin first.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mainActivityInstance = this;
        context = getApplicationContext();
        Utils.setApplicationContext(context);
        handler=new Handler();
        controller = Controller.getInstance(context);
        controller.setProgressCallback(this);
        controller.setErrorCallback(this);
        
        LSLogger.debug(TAG, "Main Activity starting...(instance="+this.toString()+") UID="+Process.myUid());
        bDisplayEnroll = true;
        systemInitialized = controller.isMdmReady();
        
        setContentView(R.layout.main);
      //  setContentView(R.layout.main_multiapps);
        
        try {
	        loginBtn = (Button)findViewById(R.id.buttonLogin);
	        enrollBtn = (ImageView)findViewById(R.id.buttonEnroll);
	        orgNameCtrl = (TextView)findViewById(R.id.orgnameText);
	        imageHintCtrl=(TextView)findViewById(R.id.imagehintText);	        
	        statusMsgCtrl = (TextView)findViewById(R.id.statusText);
	        
        } catch (Exception ex) {
        	LSLogger.exception(TAG, ex);
        }
        
        if (!controller.getDeviceAdmin().isActiveAdmin()) {
        	//LSLogger.debug(TAG, "Requesting to activate admin.");
        	controller.getDeviceAdmin().activateAdmin(this);
        	LSLogger.debug(TAG, "Requested to activate admin.");
        }
		checkPermissions();

    }

	void checkPermissions(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int permission = ActivityCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

			if (permission != PackageManager.PERMISSION_GRANTED){
/*
				if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

				}
*/
				ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},122);

			}
			if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},0);
			}
			if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE},0);
			}


		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	try {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
	    } catch (Exception ex) {
	    	LSLogger.exception(TAG, "onCreateOptionsMenu error:", ex);
	    }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            /*
             * Typically, an application registers automatically, so options
             * below are disabled. Uncomment them if you want to manually
             * register or unregister the device (you will also need to
             * uncomment the equivalent options on options_menu.xml).
             */
            /*
            case R.id.options_register:
                GCMRegistrar.register(this, SENDER_ID);
                return true;
            case R.id.options_unregister:
                GCMRegistrar.unregister(this);
                return true;
            case R.id.options_clear:
                mDisplay.setText(null);
                return true;
             */
        
		    case R.id.options_about:
			    DialogFragment newFragment = new AboutMdmDlg();
			    newFragment.show(getFragmentManager(), "lsmdm_about");
			   return true;
        
            case R.id.options_exit:
            	// note: we don't ever really exit, and that's ok. we want to run in background.
               shutdownmdm();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
    @Override
    /**
     * Handle other activity completions. Currently, we look for a results from:
     *  - DeviceAdminActivation or deactivation (requestCode DeviceAdminProvider.REQUEST_CODE_ENABLE_ADMIN) 
     *   
     *   The resultCode determines if the action completed successfully or did not; a value of -1
     *   indicates success for both install and uninstall (RESULT_OK). 
     *   Activity.RESULT_xxx values apply as possible values; RESULT_OK, RESULT_CANCELLED, RESULT_FIRSTUSER.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      	LSLogger.debug(TAG, "ActivtyResult received. req="+requestCode+" res="+resultCode);
    	if (requestCode == DeviceAdminProvider.REQUEST_CODE_ENABLE_ADMIN) {
    		controller.adminActiveStateChanged();
    		if (!controller.getDeviceAdmin().isActiveAdmin()) {
    			onErrorCallback(Constants.ERRORTYPE_FATAL,
    					context.getResources().getString(R.string.mdm_errormsg_title),
    					context.getResources().getString(R.string.status_deviceadminneeded));
    		}
    	}
   }

    
    /**
	private final int SUPERUSER_REQUEST = 2323; // arbitrary number of your choosing
    
    private void reqSuperUser() {
    	try {
    	Intent intent = new Intent("android.intent.action.superuser"); // superuser request
    	intent.putExtra("name", "Mobile Manager"); // tell Superuser the name of the requesting app
    	intent.putExtra("packagename", Constants.PACKAGE_NAME); // tel Superuser the name of the requesting package
    	startActivityForResult(intent, SUPERUSER_REQUEST); // make the request!
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "reqsuper error", ex);
    	}
    }
    **/
    
    
    private void updateStatusMsg(String msg, boolean bshow) {
    	if (statusMsgCtrl != null) {
    		if (msg == null)
    			msg = "";
    		statusMsgCtrl.setText(msg);
    		statusMsgCtrl.setVisibility((bshow?View.VISIBLE:View.GONE));
    	}
    }

    // The activity has become visible (it is now "resumed").
    @Override
    protected void onResume() {
        super.onResume();
        LSLogger.debug(TAG, "resuming activity");
        bIsActive = true;
        try {
        	//clearToast();
	        setLoginState();
	        updateState();
	        if (currentError != null && handler != null)
	        	handler.post(PopupErrorMsgHandler);
	        //setEnrollState();
        } catch (Exception ex) {
        	LSLogger.exception(TAG, "OnResume exception.",ex);
        }

    }
    
    @Override
    protected void onPause() {
        super.onPause();
        bIsActive = false;
    }
    
    // if any Toast messages were popped, this makes sure they don't get reshown.
 /*   private void clearToast() {
    	//LSLogger.debug(TAG, "clearing previous toasts");
    	Utils.clearNotificationMsg(); 	
    }
  */   
    
    private void updateState() {
    	if (!controller.isSystemReady()) {
    		if (statusMsgText != null && statusMsgText.length()>0)
    			updateStatusMsg(statusMsgText, true);
    		else
    			updateStatusMsg(context.getResources().getString(R.string.status_initializing), true);
    		enrollBtn.setVisibility(View.GONE);
	        orgNameCtrl.setVisibility(View.GONE);
	        imageHintCtrl.setVisibility(View.GONE);
    	} else { // system is ready-enough for enrollment.
    		updateStatusMsg(null, false);
    		enrollBtn.setVisibility(View.VISIBLE);
	        orgNameCtrl.setVisibility(View.VISIBLE);
	        imageHintCtrl.setVisibility(View.VISIBLE);
	        setEnrollState();
    	}
    	systemInitialized = controller.isMdmReady();
    }
    
    private void setEnrollState() {
    	//if (bEnrolling) {
    	//	bEnrolling = false;
    	//}
    	// set the org name, if we have it.
    	
    	String orgname = controller.getSettingsInstance().getOrganizationName();
    	if (orgname != null && orgNameCtrl != null) {
    		orgNameCtrl.setText(orgname);    		
    		LSLogger.debug(TAG, "Org name set to:" + orgNameCtrl.getText());
    	} else if (bDisplayEnroll) { // if set and app is starting and we're missing org info, show enroll.
    		 //showEnroll(null);
    		startupEnroll();
    	}
    	bDisplayEnroll = false;
    }
    
    private void setLoginState() {
        // Update the contents of the window based on the user's login state:
        if (loginBtn != null) {
        	if (controller.getUserAuthority().isLoggedOn()) { // user is logged in, set button to logout.
        		loginBtn.setText(R.string.button_logout);
        		imageHintCtrl.setVisibility(View.VISIBLE);
        	} else { // user is currently logged out, set the button to be logged back in:
        		loginBtn.setText(R.string.button_login);
        		imageHintCtrl.setVisibility(View.GONE);
        	}
        	loginBtn.invalidate();
        	//LSLogger.debug(TAG, "Login button text set to: " + loginBtn.getText());
        }
    	
    }

    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onNavigateUp() {
    	//LSLogger.debug(TAG, "Handling onNavUp event.");
    	if (controller.getUserAuthority().isLoggedOn())
    		return super.onNavigateUp();
    	return true;
    }
    
    @Override
    public void onBackPressed () {
    	//LSLogger.debug(TAG, "Handling onBackPressed event.");
    	if (controller.getUserAuthority().isLoggedOn())
    		super.onBackPressed();
    }
    
    // internal method that ensures this activity is top on the stack.
    
    // return true if the activty was already active, false if it wasnt.
    private boolean ensureActivityActive() {
    	boolean bres = bIsActive;  // return initial state of if we are or are not active.
    	if (!bIsActive) {
    		bIsActive = true;
        	try {
    	    	Intent i = new Intent(context, MainActivity.class);
    	   		i.setFlags( Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	    	startActivity(i);
    	    	//LSLogger.debug(TAG, "EnsureActivityActive started intent="+i);
        	} catch (Exception ex) {
        		LSLogger.exception(TAG, "EnsureActive-NavigateToMain error:", ex);
        	}
    	} else {
    		//LSLogger.debug(TAG, "Main activity is already active.");
    	}
    	return bres;
    }
        
    /**
     * Called as a result of pressing the login button on the main window.
     * Enters the application's login menu window.
     * @param view
     */
    public void showLogin(View view) {
    	LSLogger.debug(TAG, "Login selected.");
    	if (controller.getUserAuthority().isLoggedOn()) {
    		LSLogger.info(TAG, "Logging out user.");
    		// user is already logged in. log them off and update the button.
    		controller.getUserAuthority().logoffUser();
    		setLoginState();
    		
    	} else {
    		UserAuthenticationView.showDialog(this);
    		LSLogger.debug(TAG, "Showed user login dialog.");
    	}
    	//if (Controller.getInstance(this).getUserAuthority().isLoggedOn())
    		//Utils.NavigateToActivity(context, MainNavList.class);
    	
    	//Utils.NavigateToActivity(context, UserAuthenticationView.class); // MainNavList.class);
    } 
    
    /**
     * 
     * Called as a result of pressing the enter-application button on the main window.
     * Enters the application's navigation menu window.
     * @param view
     */
    public void showNavList(View view) {
    	//LSLogger.debug(TAG, "Nav List selected.");
     	if (controller.getUserAuthority().isLoggedOn()) {
    		Utils.NavigateToActivity(context, MainNavList.class);
     	} else {
     		showLogin(view);
     	}
    }

    /**
     * 
     * Called as a result of pressing the enroll button on the main window.
     * Enters the application's navigation menu window.
     * @param view
     */
    public void showEnroll(View view) {
    	if (!controller.getSettingsInstance().isEnrolled() ||
    		 controller.getUserAuthority().isLoggedOn()) {
    		bEnrollForcedLogin = false;
	     	LSLogger.debug(TAG, "Show Enrollment window");
	     	EnrollmentDlg newFragment = new EnrollmentDlg();
		    newFragment.registerDataChangeListener(this);
		    newFragment.setShowsDialog(true);
		    newFragment.show(getFragmentManager(), "lsmdm_enroll");    	
    	} else { // you must login before enrolling
    		bEnrollForcedLogin = true;
    		showLogin(null);
    	}
    }
    

    // --------------------------------------
    // Enrollment prompt window popup support during a new-install-startup.
    // When this activity is started, it needs to be allowed to come up and process
    //  its messages, before jumping to the enroll prompt dialog; otherwise, the entry
    //  fields in the enroll dialog cannot get input focus and you can't type into them.
    // My solution is to invoke a handler-post from a thread, then message back to the
    //  activity to pop up the dialog prompt, thereby allowing focus to work properly.
    // --------------------------------------
    
    private void startupEnroll() {
    	 if (handler==null)
    		 handler=new Handler();
		 // use a thread to post a message to run the enroll prompt.
		 Thread t = new Thread() {
			    public void run() {
					 try {
						 //LSLogger.debug(TAG, "startupthread");
					 } catch (Exception ex) {}
			        handler.post(PopupEnrolOnStartupHandler);
			    }
		 };
		 t.start();
    }    
    private final Runnable PopupEnrolOnStartupHandler = new Runnable() {
        public void run() {
        	showEnroll(null);
        }
    };
    
    // -----------------------------------------------------

/*    
    public interface WindowNavigationInterface {
    	public void moveToMainWindow();
    }
*/    
    /**
     * Changes current window to be this window.
     * This may be called from a background thread, so we use a handler to get the proper display.
     */
   /* 
    public void navToMainWindow() {
		 Thread t = new Thread() {
			    public void run() {
			        handler.post(NavigateToMainWindowHandler);
			    }
		 };
		 t.start();
    }
    private final Runnable NavigateToMainWindowHandler = new Runnable() {
        public void run() {
        	try {
        		LSLogger.debug(TAG, "navigating up to min window.");
        		Intent intent = new Intent(mainActivityInstance.context, MainActivity.class);
        		mainActivityInstance.navigateUpTo(intent);
        		LSLogger.debug(TAG, "navigated up to intent " +intent.toString());
        	} catch (Exception ex) {
        		LSLogger.exception(TAG, "navToMainWindow error:", ex);
        	}
        }
    };
    */
    // --------------------------------------
    
    
    public void showMDMWebView(View view) {
    	Utils.NavigateToActivity(context, MdmHomeWebView.class); 
    }
 
    public void showMBCWebView(View view) {
    	Utils.NavigateToActivity(context, MyBigCampusWebView.class);
    }

	/** 
	 * Callback for listener activity so that it can be notified upon completion of the Login dialog window.
     * Called when the user has logged in successfully or canceled out of the UserAuthenticationView window.
     * @param userIsAuthenticated true if the user is now logged in, false otherwise.
	 */
	public void userAuthLoginCompleted(boolean userIsAuthenticated) {
		setLoginState();
		if (userIsAuthenticated) {
			if (bEnrollForcedLogin) { // user was changing enrollment and had to login; go back to showing enroll.
				bEnrollForcedLogin = false;
				showEnroll(null);
			} else { // nav to main selection list
				Utils.NavigateToActivity(context, MainNavList.class);
			}
		}
	}
    

	/**
	 * DataChangeListener callback for notifying when a data change event has occurred.
	 * @param identifier optional string for identifying the data.
	 * @param newValue new value.
	 */
	public void dataChanged(String identifier, String newValue) {
		if (identifier != null) {
			if (identifier.equals(Constants.PARAM_ORGID)) 
				setEnrollState();
		}
	}
	
	
	// --------------------------------------------
	//  Error Message Callback handling, initiaited from other threads
	// --------------------------------------------
	
	/**
	 * ErrorCallbackInterface implementation:
	 * Handles a error callback with a string error message or a resource id to retrieve as the message.
	 * @param errorType - an Constants.ERROR_TYPE constant value
	 * @param errorMessage - error message, or null.
	 * @param errorTitle - string to show as a message title, or null if not needed.
	 */
	public void onErrorCallback(int errorType, String errorTitle, String errorMessage) {
		 // this may be called from a background thread, so use a handler for it:
		 if (handler==null)
			handler = new Handler();
		 LSLogger.debug(TAG, "ErrorCallback received: type="+errorType+ " " + (errorTitle==null?"null":errorTitle) +
					    " - "+ (errorMessage==null?"null":errorMessage));
		 currentError = new ErrorInfo(errorType, errorTitle, errorMessage);
		 ensureActivityActive();

		 Thread t = new Thread() {
			    public void run() {
					 try {
						 //LSLogger.debug(TAG, "startupthread-error-sleeping 2 secs to giv eui time to settle");
						 Thread.sleep(500);
					 } catch (Exception ex) {}					
					 handler.post(PopupErrorMsgHandler);
			    }
		 };
		 t.start();
	}
	private final Runnable PopupErrorMsgHandler = new Runnable() {
        public void run() {
        		
         	ErrorInfo e = currentError;
         	if (e.errorType == Constants.ERRORTYPE_TOASTMSG) {
    			currentError = null;
    			// ... show a toast message...uses the error message value
    			Utils.showNotificationMsg(e.errorMessage);
         	} else if (e != null) {
        		/*
        		AlertDialog.Builder builder = new AlertDialog.Builder(context);

        		if (e.errorMessage != null)
        			builder = builder.setMessage(e.errorMessage);
        		if (e.errorTitle != null)
        			builder = builder.setTitle(e.errorTitle);
        		builder = builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	               // User clicked OK button
        	           }
        	       });
        		builder = builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
					}
        		});
        		
        		LSLogger.debug(TAG, "Creating error dialog="+builder.toString());
        		AlertDialog dialog = builder.create();
        		*/
        		
        		PopupMessageDlg dialog = new PopupMessageDlg(PopupMessageDlg.DLGTYPE_ok, e.errorTitle, e.errorMessage, mainActivityInstance);
           		LSLogger.debug(TAG, "Creating error dialog="+dialog.toString());
           	    dialog.show(getFragmentManager(), "mdm_popup");
        		
        		LSLogger.debug(TAG, "Error shown");
        	}
        }
    };
    
    public void popupMessageCompleteCallback(int identifier, int status) {
		// if fatal error, we are done; so shut app down.
    	LSLogger.debug(TAG,"popup callback");
		if (currentError.errorType == Constants.ERRORTYPE_FATAL) {
			///Process.killProcess(Process.myPid());
			currentError = null; 
			controller.terminate();
			shutdownmdm();
		} else  if (currentError.errorType == Constants.ERRORTYPE_REBOOT) {
			currentError = null; 
			controller.terminate();
			rebootdevice();
		} else if (currentError.errorType == Constants.ERRORTYPE_REINSTALL) {
			currentError = null; 
			attemptReinstall();
		} else if (currentError.errorType == Constants.ERRORTYPE_TOASTMSG) {
			// ... show a toast message...uses the error message value
			Utils.showNotificationMsg(currentError.errorMessage);
			currentError = null;
		} else {
			LSLogger.warn(TAG, "Unknown error type in PopupMessageCompletion.");
		}
    }

     private class ErrorInfo {
    	int errorType;
    	String errorTitle;
    	String errorMessage;
    	public ErrorInfo(int errorType, String errorTitle, String errorMessage) {
    		this.errorType=errorType;
    		this.errorTitle=errorTitle;
    		this.errorMessage=errorMessage;
    	}
    }
 
    
    

    
    // ------------------------------------------
    // ProgressCallbackInterface implementations:
    // ------------------------------------------
    
    // internal handler control for status/progress:
    private void scheduleStatusMsg() {
   	 if (handler==null)
   		 handler=new Handler();
		 // use a thread to post a message to show the msg.
		 Thread t = new Thread() {
			    public void run() {
			        handler.post(StatusMsgHandler);
			    }
		 };
		 t.start();
   }    
   private final Runnable StatusMsgHandler = new Runnable() {
       public void run() {
    	   if (systemInitialized)
    		   updateStatusMsg(statusMsgText,(statusMsgText!=null && statusMsgText.length()>0));
    	   else
    		   updateState();
       }
   };
  
    
	/**
	 * Handles a string status message
	 * @param message - text message to handle.
	 */
	public void statusMsg(String message) {		
		LSLogger.info(TAG, "Status msg: "+(message==null?"(null)":message));
		statusMsgText = message;
		scheduleStatusMsg();
	    //Toast.makeText(getApplicationContext(), message, message.length()).show();
	}
	
	/**
	 * Handles progress of a task
	 * @param message
	 * @param percentage 0-100 of completion 
	 */
	public void progressMsg(String message, int percentage) {
		//mDisplay.append(message + "\n");
		LSLogger.info(TAG, message);
	}

	// ------------------------------
	// Device control functions
	// ------------------------------
	
    private void rebootdevice() {
     	try {
     		LSLogger.debug(TAG, "Reboot requested.");
     		PowerManager powermgr = (PowerManager)context.getSystemService(POWER_SERVICE);
     		powermgr.reboot(null);
  ///   	shutdowndevice();
     	} catch (Exception ex) {
     		LSLogger.exception(TAG, "Reboot error:",ex);
     	}
 		// if we get an error or not, at least shutdown the app.
 		shutdownmdm();    	 
     }
     
     private void shutdownmdm() {
      	try {
     		LSLogger.debug(TAG, "Shutting down MDM.");
            finish();
            controller.terminate();
            try{
             Thread.sleep(300);
            }catch(Exception ex){}
     		System.exit(0);
 		//android.os.Process.killProcess(android.os.Process.myPid());
     	} catch (Exception ex) {
     		LSLogger.exception(TAG, "Shutdown error:",ex);
     	}
   	 
     }
    
     
     private void attemptReinstall() {
    	 LSLogger.info(TAG, "Attempting Reinstall of MDM...");
    	 if (Utils.resintallMdmApp(context)) {
    		 LSLogger.info(TAG, "Reinstall of MDM started.");
    	 } else { // reinstall failed, post up a critical error message.
    		 String title = context.getResources().getString(R.string.gcmerror_msgtitle_reinstallmdm);
    		 String msg = context.getResources().getString(R.string.gcmerror_permissions_resintallmanually);
    		 onErrorCallback(Constants.ERRORTYPE_FATAL, title, msg);
    	 }
     }
}
    

