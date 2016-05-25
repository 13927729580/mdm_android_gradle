package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.ButtonClickListener;
import com.lightspeedsystems.mdm.util.LSLogger;
import com.lightspeedsystems.mdm.util.ProgressBarPrefView;
import com.lightspeedsystems.mdm.util.StaticTextPrefView;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DeviceInfoFragment extends PreferenceFragment 
								implements ButtonClickListener, ThreadCompletionCallback, 
										   BatteryInfo.BatteryUpdateListener {
		private static String TAG = "DeviceInfoFragment";
	private StaticTextPrefView 	view_UDID;
	private StaticTextPrefView 	view_OSVer;
	private StaticTextPrefView 	view_OSSdkVer;
	private StaticTextPrefView 	view_SyncStatus;
	private StaticTextPrefView 	view_NextSyncStatus;
	private ProgressBarPrefView view_Battery;
	
	private StaticTextPrefView 	view_SerialNum;
	private StaticTextPrefView 	view_OSBuild;
	private StaticTextPrefView 	view_OSBootloader;
	
	private StaticTextPrefView 	view_Brand;
	private StaticTextPrefView 	view_DeviceName;
	private StaticTextPrefView 	view_Display;
	private StaticTextPrefView 	view_Mfgr;
	private StaticTextPrefView 	view_Model;
	private StaticTextPrefView 	view_Product;

	private Context context;
	private Handler syncHandler;
	private BatteryInfo batteryInfo;
	
	private final static int ctrlID_syncicon = 1;
	private final static int ctrlID_nextsyncicon = 2;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences layout resource:
        addPreferencesFromResource(R.layout.device_info_frag); 
               
        // get the Preference view implementations from the layout resource, using the name:
        view_UDID  = (StaticTextPrefView)   findPreference("DEV_UDID");
        view_OSVer = (StaticTextPrefView)   findPreference("DEV_OSVER");
        view_OSSdkVer = (StaticTextPrefView)   findPreference("DEV_OSSDKVER");
        
        view_SyncStatus = (StaticTextPrefView) findPreference("LASTSYNC");
        if (view_SyncStatus != null)
        	view_SyncStatus.registerIconButtonClickListener(this);
        view_NextSyncStatus = (StaticTextPrefView) findPreference("NEXTSYNC");
        
        view_Battery = (ProgressBarPrefView) findPreference("DEV_BATTERY");

        view_SerialNum = (StaticTextPrefView)   findPreference("DEV_SERIALNUM");
        view_OSBuild = (StaticTextPrefView)     findPreference("DEV_OSBUILD");
        view_OSBootloader = (StaticTextPrefView) findPreference("DEV_OSBOOTLOADER");
        
        view_Brand = (StaticTextPrefView)   findPreference("DEV_BRAND");
        view_DeviceName = (StaticTextPrefView)   findPreference("DEV_DEVICENAME");
        view_Display = (StaticTextPrefView)   findPreference("DEV_DISPLAY");
        view_Mfgr = (StaticTextPrefView)   findPreference("DEV_MFGR");
        view_Model = (StaticTextPrefView)   findPreference("DEV_MODEL");
        view_Product = (StaticTextPrefView)   findPreference("DEV_PRODUCT");

    }
    
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View view = super.onCreateView ( inflater,  container,  savedInstanceState);
    	//LSLogger.debug(TAG, "onCreateView - setting View values...");
    	context = view.getContext();
        batteryInfo = Controller.getInstance(context).getBatteryInfoProvider();
        if (batteryInfo != null)
        	batteryInfo.registerListener(this);
    	setViewValues(view);
    	return view;
    }
    
    @Override
    public void onDestroy() {
    	// unregister things:
        if (batteryInfo != null)
        	batteryInfo.unregisterListener(this);
    	super.onDestroy();
    }
    
    /**
     * Interface Callback for when a preference value was changed.
     * @return true to accept the change, false to not accept it (this always returns true).
     */
    /*
    public boolean onPreferenceChange(Preference preference, Object newValue) {
    	// we don't do any changes, but this method is currently part of the listener interface.
    	return true;
    }
    */
    

    // sets the values in the view from the current preferences, needed for custom 
    private void setViewValues(View view) {
    	try {
	    	Controller controller = Controller.getInstance(context);
	    	Device device = controller.getDeviceInstance();
	    	if (device != null) {
	    		if (view_UDID != null)
	    			view_UDID.setText(device.getDeviceUDID());
	       		if (view_OSVer != null)
	    			view_OSVer.setText(device.getOSVersion());
	       		if (view_OSSdkVer != null)
	    			view_OSSdkVer.setText(device.getOSSDKVersion());
	
	       		if (view_SerialNum != null)
	       			view_SerialNum.setText(device.getDeviceSerialNumber());
	       		if (view_OSBuild != null)
	       			view_OSBuild.setText(device.getOSBuild());
	       		if (view_OSBootloader != null)
	       			view_OSBootloader.setText(device.getOSBootloaderVersion());
	
	       		if (view_Brand != null)
	       			view_Brand.setText(device.getDeviceBrand());
	       		if (view_DeviceName != null)
	       			view_DeviceName.setText(device.getDeviceName());
	       		if (view_Display != null)
	       			view_Display.setText(device.getDeviceDisplay());
	       		if (view_Mfgr != null)
	       			view_Mfgr.setText(device.getDeviceManufacturer());
	       		if (view_Model != null)
	       			view_Model.setText(device.getDeviceModel());
	       		if (view_Product != null)
	       			view_Product.setText(device.getDeviceProduct());
	
	    	//} else {
	    		//LSLogger.warn(TAG, "Warning! Device instance not initialized.");
	    	}
	    	Settings settings = Settings.getInstance(context);
	    	setSyncInfo(settings);
	    	
	    	setBatteryInfo();
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "setViewValues error:", ex);
    	}
    }
    
    // sets Sync status values.
    private void setSyncInfo(Settings settings) {
    	try {
	    	if (settings == null)
	    	    settings = Settings.getInstance(context);
	    	if (settings != null) {
	    		if (view_SyncStatus != null) {
	    			String s=settings.getLastSyncAsString();
	    			view_SyncStatus.setText(s);
	    			view_SyncStatus.setIconID(ctrlID_syncicon);
	    			view_SyncStatus.setIcon(android.R.drawable.ic_menu_rotate);	
	    		} 
	    		if (view_NextSyncStatus != null) {
	    			view_NextSyncStatus.setText(settings.getNextSyncAsString());
	    			view_NextSyncStatus.setIconID(ctrlID_nextsyncicon);
	    			view_NextSyncStatus.setIcon(android.R.drawable.ic_menu_today);	
	    		}
	    	} 
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "setSyncInfo error:", ex);
    	}
    }
    
    // sets battery data values.
    private void setBatteryInfo() {
    	try {
    		if (view_Battery != null && batteryInfo != null) {
	    		view_Battery.setPrimaryProgress(batteryInfo.getLevel());
	    		// wrap the icon code here in try-catch; on some platforms, the resource might
	    		//  not be found, so we need to revert to a known-present default if that occurs.
 	    		
	    		try {
	    			view_Battery.setIcon(batteryInfo.getStateResourceID());
	    		} catch (Exception ex) {
	    			LSLogger.warn(TAG, "Failed to load battery level state image; reverting to a default image.");
		try {
			view_Battery.setIcon(batteryInfo.getDefaultBatteryResource());
		} catch (Exception ex2) {
			LSLogger.exception(TAG, "battery info exception on the image (defaulting to no image)", ex2);
			view_Battery.setIcon(0);
		}
	    		}
	    		


	    		view_Battery.setShowText(true);
	    	}
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "setBatteryInfo error:", ex);
    	}
    }
    
	/**
	 * Callback called when the state of the the battery has changed.
	 */
	public void onBatteryStateChange() {
		setBatteryInfo();
	}
   
    
	/** 
	 * Callback when a button is pressed. 
	 * @param v View the button is in
 	 * @param buttonID identifier for the button, or 0 if no ID was defined.
	 */
	public void onButtonClick(View v, int buttonID) {
		switch (buttonID) {
			case ctrlID_syncicon:
				LSLogger.debug(TAG, "device last sync icon clicked.");
				if (syncHandler == null)
					syncHandler = new Handler();
				Controller.getInstance().requestServerSync(true, this);
				break;
			default:
				LSLogger.warn(TAG, "Unknown buttonClick event for button="+buttonID+" (view id="+ v.getId()+")");
				break;
		}
	}

	
	
    // Create runnable for posting from the onThreadComplete callback,
	// which is called when the sync process is completed.
    final Runnable mUpdateSyncData = new Runnable() {
        public void run() {
        	LSLogger.debug(TAG,"syncupdater running");
            setSyncInfo(null);
        }
    };
		
	/**
	 * Called when a thread completes, within the thread's thread context.
	 * @param obj optional parameter passed back to the implementor. 
	 */
	public void onThreadComplete(Object obj) {
		//LSLogger.debug(TAG, "onThreadCompleted callback....");
		if (syncHandler != null)
			syncHandler.post(mUpdateSyncData);
	}

}