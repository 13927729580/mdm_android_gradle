/**
 * Provides local Android Device Administration support, enabling the 
 * Android policy and device administration capabilities. 
 * 
 * This class includes:
 * - DeviceAdminProvider - provides the Activity that gets used to display Admin activation for this app.
 * - DeviceAdminProviderReceiver - implementation of DeviceAdminReceiver
 */
package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Wrapper class for DevicePolicyManager and other Device Aministration features and capabilities.
 * Consumers of an instance of this class use it to make calls into the policy and settings that
 * are managed by the Android DevicePoilicyManager class, to provide a central location where all those
 * features are implemented.
 *
 */
public class DeviceAdminProvider { 
	private final static String TAG="DeviceAdminProvider";
	
	private Context context;
    private DevicePolicyManager devicePolicyMgr;
    private ComponentName deviceAdminReceiver;

    public static final int REQUEST_CODE_ENABLE_ADMIN = 199;
   // private static final int REQUEST_CODE_START_ENCRYPTION = 2;

    /**
     * Constructor for creating an instance.
     * @param context application context
     */
    public DeviceAdminProvider(Context context) { 
    	this.context = context;
        devicePolicyMgr = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdminReceiver = new ComponentName(context, DeviceAdminProviderReceiver.class);
    }
    
    /**
     * Gets the Device Policy Manager instance.
     */
    public DevicePolicyManager getDevicePolicyManager() {
    	return devicePolicyMgr;
    }
    
    /**
     * Gets the application's Device Administrator component.
     */
    public ComponentName getAdminComponent() {
    	return deviceAdminReceiver;
    }

    /**
    * Determines if the mdm app is an active admin provider. 
    * This must be active to perform the policy-related management actions.
    * @return true if Administration is enabled, false if not enabled.
    */
    protected boolean isActiveAdmin() {
       return devicePolicyMgr.isAdminActive(deviceAdminReceiver);
    }
    
    /**
    * Activates this app as an admin provider. 
    * @return true if Administration is enabled, false if not enabled.
    */
    public boolean activateAdmin(Activity activity) {
    	// note that this implementation uses the default android approach for using an Intent to trigger device activation.
    	// if there is an alternative implementation (such as one of our own), we'd call into that instead.
        // Launch the activity to have the user enable our admin.
    	try {
	        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
	        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminReceiver);
	        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
	                		context.getString(R.string.device_admin_activate_description));
	        if (activity != null) {
	        	activity.startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
	        } else {
	        	context.startActivity(intent);
	        }
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "ActivateAdmin", ex);
    	}
    	// now lastly, return the state of activation.
       return devicePolicyMgr.isAdminActive(deviceAdminReceiver);
    }
    
    /**
     * Deactivates this app's admin provider support.
     */
    public void deactivateAdmin() {
    	devicePolicyMgr.removeActiveAdmin(deviceAdminReceiver);
    }
    
    // -------------------------
    // ---- Policy actions -----
    // -------------------------
    
    /**
     * Simplified indication of whether a passcode is required or not. 
     * Applications should use @setPasscodeQuality instead of this method to identify the specific passcode requirements.
     * However, this can be used to provide a simple and quick on/off function for passcode enablement.
     * 
     * Calling this method with a value of 'false' simply calls @setPasscodeQuality with a 
     * level value of DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED. 
     * 
     * Calling this method with a value of 'true' will do one of two things: (1) if the current quality is 
     * at DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, the quality is changed 
     * to DevicePolicyManager.PASSWORD_QUALITY_SOMETHING so that some kind of passcode is required. 
     * (2) if the current quality is some other value, a passcode is already required, so this does nothing.
     * 
     * @param required true if a passcode is required, false if not
     * @return previous setting, false if a passcode was not required before calling this method 
     * (if the quality level was DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED); otherwise, returns true.
     */
    public boolean setPasscodeRequired(boolean required) {
    	int prevQuality = devicePolicyMgr.getPasswordQuality(deviceAdminReceiver);
    	
    	if (required) { // a passcode is being indiocated as needed; if none is required, make it need something:
    		if (prevQuality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
    			setPasscodeQuality(DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
    			
    	} else { // a passcode is not required, so change the quality level to reflect this:
    		setPasscodeQuality(DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
    	}

    	return (prevQuality != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
    }
    
    /**
     * Sets the passcode quality to one of the DevicePolicyManager.PASSWORD_QUALITY_ values.
     * @param qualityLevel new level to set to.
     * @return previous quality setting.
     */
    public int setPasscodeQuality(int qualityLevel) {
    	int prev = devicePolicyMgr.getPasswordQuality(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordQuality(deviceAdminReceiver, qualityLevel);
    	return prev;
    }
    
    
    /**
     * Specifies how many times a user can enter the wrong password before the device wipes its data. 
     * @param count number of times to allow. 0 sets this to unlimited.
     * @return previous value that was in place.
     */
    public int setPasscodeWipeMaxFailAttempts(int count) {
    	int prev = devicePolicyMgr.getMaximumFailedPasswordsForWipe(deviceAdminReceiver);
    	devicePolicyMgr.setMaximumFailedPasswordsForWipe(deviceAdminReceiver, count);
    	return prev;
    }
    
    /**
     * Sets the number of millisecods the user sees as the maximum-allowed configurable value for setting the
     * autolocking on their device. 
     * @param newtime maximum time in milliseconds
     * @return previous value
     */
    public long setPasscodeMaxTimeToLock(long newtime) {
    	long prev = devicePolicyMgr.getMaximumTimeToLock(deviceAdminReceiver);
    	if (prev != newtime)
    		devicePolicyMgr.setMaximumTimeToLock(deviceAdminReceiver, newtime);
    	return prev;
    }
    
    /**
     * Sets the duration when the user's passcode will expire, starting from the previous last-set time.
     * @param msecs length of time in milliseconds, or 0 for no expiration.
     * @return previous value
     */
    public long setPasscodeExpirationMSecs(long msecs) {
    	long prev = devicePolicyMgr.getPasswordExpirationTimeout(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordExpirationTimeout(deviceAdminReceiver, msecs);
    	return prev;
    }
    
    /**
     * Sets the maximum number of passcodes kept in history so as to prevent reusing passcodes.
     * @param length number of unique passcodes that must be used before one can be reused.
     * @return previous value
     */
    public int setPasscodeHistoryLength(int length) {
    	int prev = devicePolicyMgr.getPasswordHistoryLength(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordHistoryLength(deviceAdminReceiver, length);
    	return prev;
    }
    
    /** 
     * Sets minimum length of a passcode.
     * @param length number of characters required in a passcode
     * @return previous value
     */
    public int setPasscodeMinLength(int length) {
    	int prev = devicePolicyMgr.getPasswordMinimumLength(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordMinimumLength(deviceAdminReceiver, length);
    	return prev;
    }
    
    /** 
     * Sets minimum number of alphabetic letters that must exist in a passcode.
     * @param num number of letters required in a passcode
     * @return previous value
     */
    public int setPasscodeNumberOfLetters(int num) {
    	int prev = devicePolicyMgr.getPasswordMinimumLetters(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordMinimumLetters(deviceAdminReceiver, num);
    	return prev;
    }
    
    /** 
     * Sets minimum number of symbol characters that must exist in a passcode.
     * @param num number of symbols required in a passcode
     * @return previous value
     */
   public int setPasscodeMinNumberComplexChars(int num) {
    	int prev = devicePolicyMgr.getPasswordMinimumSymbols(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordMinimumSymbols(deviceAdminReceiver, num);
    	return prev;
    }
    
   /** 
    * Sets minimum number of numeric characters that must exist in a passcode.
    * @param num number of digits required in a passcode
    * @return previous value
    */
    public int setPasscodeMinNumberNumericChars(int num) {
    	int prev = devicePolicyMgr.getPasswordMinimumNumeric(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordMinimumNumeric(deviceAdminReceiver, num);
    	return prev;
    }
       
    /** 
     * Sets minimum number of non-alphabetic letters that must exist in a passcode, which 
     * include numbers and special characters
     * @param num number of non-alphabetic characters required in a passcode
     * @return previous value
     */
    public int setPasscodeMinNumberNonLetterChars(int num) {
    	int prev = devicePolicyMgr.getPasswordMinimumNonLetter(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordMinimumNonLetter(deviceAdminReceiver, num);
    	return prev;
    }
    
    /** 
     * Sets minimum number of upper-case characters that must exist in a passcode.
     * @param num number of upper-case characters required in a passcode
     * @return previous value
     */
    public int setPasscodeMinNumberUppercaseChars(int num) {
    	int prev = devicePolicyMgr.getPasswordMinimumUpperCase(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordMinimumUpperCase(deviceAdminReceiver, num);
    	return prev;
    }

    /** 
     * Sets minimum number of lower-case characters that must exist in a passcode.
     * @param num number of lower-case characters required in a passcode
     * @return previous value
     */
    public int setPasscodeMinNumberLowercaseChars(int num) {
    	int prev = devicePolicyMgr.getPasswordMinimumLowerCase(deviceAdminReceiver);
    	devicePolicyMgr.setPasswordMinimumLowerCase(deviceAdminReceiver, num);
    	return prev;
    }
    
    
     /**
     * Sets the camera enabled state.
     * @param enabled true to enable the camera, false to disable it.
     * @return the previous enable state of the camera.
     */
    public boolean enableCamera(boolean enabled) {
    	// The camera apis are 'disable'-oriented, whereas this method 'enabled'-oriented.
    	// As such, we reverse the previous value and reverse the passed-in enabled flag, in
    	// relation to the camera apis as they get called:
    	boolean prevSetting = !devicePolicyMgr.getCameraDisabled(null);
    	devicePolicyMgr.setCameraDisabled(deviceAdminReceiver, !enabled);
    	return prevSetting;
    }
    
    /**
     * Sets the storage encryption state.
     * @param encrypt true to enable storage encryption, false to disable it.
     * @return the previous encryption enabled state.
     */
    public boolean enableStorageEncryption(boolean encrypt) {
    	boolean prevSetting = devicePolicyMgr.getStorageEncryption(null);
    	devicePolicyMgr.setStorageEncryption(deviceAdminReceiver, encrypt);
    	return prevSetting;
    }
    
    
    // -------------------------
    // ----- MDM actions -------
    // -------------------------
    /**
     * Resets the passcode on the device. 
     * @param newPassword new password
     * @return true if the password was applied, false if it doesn't meet passcode requirements.
     */
    public boolean resetPassword(String newPassword) {
    	return devicePolicyMgr.resetPassword(newPassword, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
    }

    /**
     * Clears the passcode on the device.
     * @return true if the password was cleared, false if not.
     */
    public boolean removePassword() {
    	return devicePolicyMgr.resetPassword("", 0);
    }

    /**
     * Immediately expires the passcode on the device.
     * @return true if the password was set to be expired, false if not.
     */
    public boolean expirePasswordImmediate() {
    	// default to 1 ms as the time to expire so as to 'expire now';
    	devicePolicyMgr.setPasswordExpirationTimeout(deviceAdminReceiver, 1);
    	return true; 
    }
    
    /**
     * Sets the passcode expiration policy the occur after the given number of minutes, or clears the
     * policy (removes it) if the minutes value is 0.
     * @param minutesUntilExpiration optional number of minutes to set the device to automatically
     * expire the current password, if set. A value of 0 means to clear the expiration policy and do
     * not used automatic time-based expiration.
     * @return previous timeout value. Note that a negative value means the current passcode is already
     * timed-out. 
     */
    public long expirePasswordAfter(long minutesUntilExpiration) {
       	long prev = devicePolicyMgr.getPasswordExpirationTimeout(deviceAdminReceiver);
     	//if a minutes value has been provided, convert to milliseconds and use it.
    	if (minutesUntilExpiration >= 0) {
    		long msToExp = 0; // a value of 0 disables expiration, ie, passwords dont expire.
    		if (minutesUntilExpiration > 0)
    			msToExp = minutesUntilExpiration * 60000;
    		devicePolicyMgr.setPasswordExpirationTimeout(deviceAdminReceiver, msToExp);
    	}
    	return prev; 
    }
   
    /**
     * Locks the device immediately.
     * @return true if the device was locked (or realistically, if the lock api did not fail. 
     * May throw a security exception if lock is not allowed.
     */
    public boolean lockNow() {
    	devicePolicyMgr.lockNow();
    	return true;
    }
    
    /**
     * Wipes the device clean.
     * May throw a security exception if wipe is not allowed.
     */
    public void wipeAll() {
    	devicePolicyMgr.wipeData(0);
    }
    
    // ---------------------------
    // --- end of MDM actions ----
    // ---------------------------

	/**
	 * Implementation of a DeviceAdminReceiver. 
	 *
	 * All callbacks are on the UI thread and your implementations should not engage in any
	 * blocking operations, including disk I/O.
	 */
	public static class DeviceAdminProviderReceiver extends DeviceAdminReceiver {
		private final static String TAG = "DeviceAdminProviderReceiver";

		public DeviceAdminProviderReceiver() {
			super();
		}
		
	    @Override
	    public CharSequence onDisableRequested(Context context, Intent intent) {
	        return context.getString(R.string.device_admin_disable_warning);
	    }
	
	    /*
		void showToast(Context context, String msg) {
	        String status = context.getString(R.string.admin_receiver_status, msg);
	        Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
	        
	    }
	    */
		
	    @Override
	    public void onEnabled(Context context, Intent intent) {
	    	//super.onEnabled(context, intent);
	        //showToast(context, context.getString(R.string.admin_receiver_status_enabled));
	    	LSLogger.info(TAG, "DeviceAdmin enabled.");
	    }
	
	    @Override
	    public void onDisabled(Context context, Intent intent) {
	    	//super.onDisabled(context, intent);
	        //showToast(context, context.getString(R.string.admin_receiver_status_disabled));
	    	LSLogger.info(TAG, "DeviceAdmin disabled.");
	    }
	
	    @Override
	    public void onPasswordChanged(Context context, Intent intent) {
	    	//super.onPasswordChanged(context, intent);
	        //showToast(context, context.getString(R.string.admin_receiver_status_pw_changed));
	    	LSLogger.info(TAG, "Password Changed.");
	    }
	
	    @Override
	    public void onPasswordFailed(Context context, Intent intent) {
	    	//super.onPasswordFailed(context, intent);
	        //showToast(context, context.getString(R.string.admin_receiver_status_pw_failed));
	    	LSLogger.info(TAG, "Password Change failed.");
	    }
	
	    @Override
	    public void onPasswordSucceeded(Context context, Intent intent) {
	    	//super.onPasswordSucceeded(context, intent);
	        //showToast(context, context.getString(R.string.admin_receiver_status_pw_succeeded));
	    	LSLogger.info(TAG, "Password Change succeeded.");
	    }
	
	    @Override
	    public void onPasswordExpiring(Context context, Intent intent) {
	    	LSLogger.info(TAG, "Password is expiring notification.");
	    	/*
	        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
	                Context.DEVICE_POLICY_SERVICE);
	        long expr = dpm.getPasswordExpiration(
	                new ComponentName(context, DeviceAdminProviderReceiver.class));
	        long delta = expr - System.currentTimeMillis();
	        boolean expired = delta < 0L;
	        String message = context.getString(expired ?
	                R.string.expiration_status_past : R.string.expiration_status_future);
	        showToast(context, message);
	        Log.v(TAG, message);
	        */
	    }
	}

}