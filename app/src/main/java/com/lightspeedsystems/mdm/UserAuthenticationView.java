package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Provides the user authentication user interface.
 */
public class UserAuthenticationView extends DialogFragment {
	private static String TAG = "UserAuthenticationView";
	
	private UserAuthority userAuth;
	private TextView view_uid; // user name/id field
	private TextView view_pwd; // password field
	private TextView status_msg;
	private Button btnLogin;
	private Button btnCancel;
	private Context context;
	private UserAuthLoginCallback parentCallback;
	private Handler loginHandler;
	//private 
	
	/** 
	 * Callback for listener activity so that it can be notified upon completion of the Login dialog window.
	 */
	public interface UserAuthLoginCallback {
		/**
		 * Called when the user has logged in successfully or canceled out of the UserAuthenticationView window.
		 * @param userIsAuthenticated true if the user is now logged in, false otherwise.
		 */
		public void userAuthLoginCompleted(boolean userIsAuthenticated);
	};
	
	public static void showDialog(Activity parentActivity) {
		UserAuthenticationView newFragment = new UserAuthenticationView();
		newFragment.context = parentActivity;
		newFragment.parentCallback = (UserAuthLoginCallback)parentActivity;
		//newFragment.updateDisplayValues();
		newFragment.show(parentActivity.getFragmentManager(), "lsmdm_usrlogin");
	}
	
	// constructor for DialogFragment
	public UserAuthenticationView() {
		super();		
	}
	
	public void updateDisplayValues() {
		LSLogger.debug(TAG, "updating display values");
        // initialize userID from prior value:
        if (view_uid != null && userAuth != null) {
        	view_uid.setText(userAuth.getUserID()); 
        }
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_loggin, container);
		view_uid = (TextView) view.findViewById(R.id.textUserID);
		view_pwd = (TextView) view.findViewById(R.id.textPassword);
		status_msg = (TextView) view.findViewById(R.id.statusMsg);
		
		btnLogin = (Button)view.findViewById(R.id.btnLogin);
		btnCancel = (Button)view.findViewById(R.id.btnCancel);

		userAuth = Controller.getInstance(context).getUserAuthority();
		
        getDialog().setTitle(R.string.login_title);  
        btnCancel.setOnClickListener(new View.OnClickListener() {
	               @Override
	               public void onClick(View v) {
	            	   dismiss();
	            	   if (parentCallback != null)
	            		   parentCallback.userAuthLoginCompleted(false);
	               }});
        btnLogin.setOnClickListener(new LoginButtonHandler()); 
       
        view_uid.setOnKeyListener(new View.OnKeyListener() {			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				clearStatusMsg();
				return false;
			}
		});
        view_pwd.setOnKeyListener(new View.OnKeyListener() {			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				clearStatusMsg();
				return false;
			}
		});
        
        return view;
    }
	
	@Override
	public void onStart() {
		super.onStart();
//		updateDisplayValues();
	}
	
	private void clearStatusMsg() {
		if (status_msg != null)
			status_msg.setText("");
	}
	
	private void setStatusMsg(String msg, int resID) {
		if (status_msg != null) {
			try {
				if (msg != null)
					status_msg.setText(msg);
				else if (resID != 0)
					status_msg.setText(resID);
				else // clear msg if nothing is given to show.
					status_msg.setText("");
			} catch (Exception ex) {
				LSLogger.exception(TAG, "SetStatusMsg error:", ex);
			}
		}
	}
	
	class LoginButtonHandler implements View.OnClickListener  {
		 @Override
         public void onClick(View v) {
         	LSLogger.info(TAG, "Login button pressed.");
 		    
 		    // attempt login:
 		    String u = view_uid.getText().toString();
 		    String p = view_pwd.getText().toString();
 		    //StringBuffer errorBuf = new StringBuffer();
 		    
 		    // validate input:
 		    if (u==null || u.trim().isEmpty()) {
 		    	setStatusMsg(null, R.string.msg_missingusername);
 		    } else if (p==null || p.trim().isEmpty()) {
 		    	setStatusMsg(null, R.string.msg_missingpassword);

		    	// check wifi, make sure it is active:
 	/*	    } else if (!Utils.wifiEnable(context, true)) {
 		    		LSLogger.warn(TAG, "Wifi is not enabled.");
 		    		setStatusMsg(null, R.string.error_wifi_not_enabled);
 	*/	    	
 		    } else {

	 		    // proceed to login:
	 		    if (btnLogin != null)
	 		    	btnLogin.setEnabled(false);
	 		    
	    		UserAuthority user = Controller.getInstance(getActivity()).getUserAuthority();
	 			user.setUserCredentials(u, p);
	 			
				 if (loginHandler == null)
					 loginHandler = new Handler();
	
	 		     setStatusMsg(null, R.string.logininprogress);
	 		     
				 Thread t = new Thread() {
					    public void run() {
					    	try {
					    		UserAuthority user = Controller.getInstance(getActivity()).getUserAuthority();
					    		user.loginUser();
					    	} catch (Exception ex) {
					    		LSLogger.exception(TAG, "Login thread exception:", ex);
					    	}				    	
					    	loginHandler.post(mLoginCompletedHandler);
					    }
					 };
				t.start();
 		    }
        };
	}
	
	   private final Runnable mLoginCompletedHandler = new Runnable() {
	        public void run() {
	    		UserAuthority user = Controller.getInstance(getActivity()).getUserAuthority();
	 		    if (user.isLoggedOn()) {
	 		    	String loginokmsg = context.getResources().getString(R.string.loginsucceeded);
	 		    	setStatusMsg(loginokmsg, 0);
	 		    	LSLogger.debug(TAG, loginokmsg);
	 		    	// user logged in successfully.
	 		    	dismiss();
	         	    if (parentCallback != null)
	        		   parentCallback.userAuthLoginCompleted(true);
	 		    } else {
	 			   // login failed.show message.
	 		    	setStatusMsg(user.getLastLoginError(), 0);
	 		    	LSLogger.error(TAG, user.getLastLoginError());
	 		    	if (btnLogin != null)
	 	 		    	btnLogin.setEnabled(true);
	 		    }
	        }
	    };


}

