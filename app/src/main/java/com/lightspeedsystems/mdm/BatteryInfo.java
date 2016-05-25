package com.lightspeedsystems.mdm;

import java.util.Iterator;
import java.util.Vector;

import org.json.JSONObject;

import com.lightspeedsystems.mdm.util.LSLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;

/**
 * Gets battery information and supports battery status updates.
 *
 */
public class BatteryInfo {
	private static String TAG = "BatteryInfo";
	private Context context;
	private BatteryInfoReceiver batteryReceiver;
	private Vector<BatteryUpdateListener> listeners;
	private int batteryLevelMax = 100;
	
	public BatteryInfo(Context ctx) {
		context = ctx;
		batteryReceiver = new BatteryInfoReceiver();
	    IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	    context.registerReceiver(batteryReceiver, filter);
	    listeners = new Vector<BatteryUpdateListener>(2);
	}
	

	/**
	 * Implemented by a class that wishes to be notified of battery state changes.
	 */
	public interface BatteryUpdateListener {
		/**
		 * Called when the state of the the battery has changed.
		 */
		public void onBatteryStateChange();		
	}
	
	/**
	 * Registers the listener to receive battery change updates.
	 * @param listener implementation of BatteryUpdateListener
	 */
	public void registerListener(BatteryUpdateListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Unregisters the listener for receiving battery change updates.
	 * @param listener implementation of BatteryUpdateListener
	 */
	public void unregisterListener(BatteryUpdateListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Gets the battery upper scale. This is typically 100.
	 * @return the upper scale
	 */
	public int getScale() {
		return batteryLevelMax;
	}

	/**
	 * Gets the current battery level, as a value from 0 to the @getScale() maximum value.
	 * @return battery level
	 */
	public int getLevel() {
		if (batteryReceiver.level > 0)
			return batteryReceiver.level;
		return 0;
	}
	
	/**
	 * Gets the battery voltage
	 * @return
	 */
	public int getVoltage() {
		return batteryReceiver.voltage;
	}
	
	/**
	 * Gets the battery temperature.
	 * @return
	 */
	public int getTemperature() {
		return batteryReceiver.temp;
	}
		
	/**
	 * Gets a resource ID of the current battery state.
	 * The default is to use a fixed icon that we provide; but if available in 
	 * the battery info, we'll use the resource provided in the status intent.
	 * @return
	 */
	public int getStateResourceID() {
		int resID = batteryReceiver.state_icon_id;
		if (resID == 0)
			resID = getDefaultBatteryResourceID();
		return resID;
	}
	
	/**
	 * Gets an internal-resource ID of a generic battery image. This is to be used 
	 * if the built-in android battery state icon cannot be loaded.
	 * @return ID of R.drawable.icon_battery.
	 */
	public int getDefaultBatteryResourceID() {
		return context.getResources().getInteger(R.drawable.icon_battery);
	}

	/**
	 * Gets the generic battery image. This is to be used 
	 * if the built-in android battery state icon cannot be loaded.
	 * @return Drawable from R.drawable.icon_battery.
	 */
	public Drawable getDefaultBatteryResource() {
		return context.getResources().getDrawable(R.drawable.icon_battery);
	}

	
	/**
	 * Gets battery values as json name-value pairs for parameters to be sent to the mdm server.
	 * @param json existing instance to add to, or null to create a new one.
	 * @return json instance with device values added to it.
	 */
	public JSONObject getStatusAsJSONparams(JSONObject json) {
		if (json == null)
		  json = new JSONObject();
		try {
			float batLevel = getLevel();
			float maxLevel = getScale();
			
			if (batLevel > 0)
				batLevel = batLevel / maxLevel;
			
			//LSLogger.debug(TAG, "Battery level = " + Float.valueOf(batLevel * 100).toString() + "%  -- max="+batteryLevelMax);

			json.put(Constants.PARAM_BATTERY_LEVEL, Float.valueOf(batLevel));
						
		} catch (Exception e) {
			LSLogger.exception(TAG, "getStatusAsJSONparams error: ", e);
		}
		return json;
	}
	

	/*
	 * Internal class for getting and storing battery info from the device.
	 * The system calls onReceive when battery info is available and changes.
	 */
    private class BatteryInfoReceiver extends BroadcastReceiver {
	        int scale = -1;
	        public int level = -1;
	        int voltage = -1;
	        int temp = -1;
	        int state_icon_id = 0; // resource icon id
	        
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	try {
		        	state_icon_id = intent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0);
		            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		            temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
		            voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
		            if (scale > 0 && scale != batteryLevelMax)
		            	batteryLevelMax = scale;
		            Iterator<BatteryUpdateListener> iter = listeners.iterator();
		            while (iter.hasNext()) {
		            	BatteryUpdateListener l = (BatteryUpdateListener)iter.next();
		            	l.onBatteryStateChange();
		            }
	        	} catch (Exception ex) {
	        		LSLogger.exception(TAG, "BatteryChange-onReceive: ", ex);
	        	}
	        	
	            //LSLogger.info(TAG, "level is "+level+"/"+scale+", temp is "+temp+", voltage is "+voltage +"  icon="+state_icon_id);
	        }
	}
	
}
