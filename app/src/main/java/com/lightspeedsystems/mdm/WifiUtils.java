package com.lightspeedsystems.mdm;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Utilities for Wifi connectivity and other Wifi operations.
 * 
 * Note that the WifiManager is used to access Wifi info and connectivity state.
 *   Wifi states are defined as WifiManager.WIFI_STATE_ values;
 *      0=disabling 1=disabled 2=enabling 3=enabled 4=unknown error
 *      
 * WiFi can be in one of the enabling states (and either on or off), 
 * but still not be connected. 
 * The state is the best way to tell if a connection is active or attempting to connect.
 */
public class WifiUtils {
	private static String TAG = "WifiUtils";
	
	/**
	 * Checks to see if WiFi is enabled.
	 * @param context Application context
	 * @return true if wifi is enabled or on, false if disabled or off.
	 */
	public static boolean wifiIsEnabled(Context context) {
		boolean wifiIsEnabled = false;
		try {
			WifiManager wmgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			wifiIsEnabled = wmgr.isWifiEnabled();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "WifiIsEnabled error:",ex);
		}
		return wifiIsEnabled;
	}

	/**
	 * Enables or disables WiFi.
	 * @param context Application context
	 * @param enable true to enable wifi, false to disable wifi.
	 * @return true if wifi is enabled or on, false if disabled or off.
	 */
	public static boolean wifiEnable(Context context, boolean enable) {
		boolean wifiIsEnabled = false;
		try {
			WifiManager wmgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			wifiIsEnabled = wmgr.isWifiEnabled();
			if (wifiIsEnabled != enable) // if not already in the requested state, set it.
				wifiIsEnabled = wmgr.setWifiEnabled(enable);
			LSLogger.debug(TAG, "WiFi connected state="+wmgr.getWifiState());
			
		} catch (Exception ex) {
			LSLogger.exception(TAG, "WifiIsEnabled error:",ex);
		}
		return wifiIsEnabled;
	}

	/**
	 * Checks to see if WiFi is connected (WIFI_STATE_ENABLED).
	 * @param context Application context
	 * @return true if wifi is connected, false if not connected (may be disabled or still connecting).
	 */
	public static boolean wifiIsConnected(Context context) {
		boolean bConnected = false;
		try {
			WifiManager wmgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			bConnected = (wmgr.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "WifiIsConnected error:",ex);
		}
		return bConnected;
	}

	/**
	 * Checks to see if WiFi is trying to connect (WIFI_STATE_ENABLING).
	 * @param context Application context
	 * @return true if wifi is connecting, false if not trying to connect
	 *  (may be disabled or already connected).
	 */
	public static boolean wifiIsConnecting(Context context) {
		boolean bConnected = false;
		try {
			WifiManager wmgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			bConnected = (wmgr.getWifiState() == WifiManager.WIFI_STATE_ENABLING);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "WifiIsConnecting error:",ex);
		}
		return bConnected;
	}
	
	
	/**
	 * Makes sure Wifi is enabled, enabling it and waiting for it as needed.
	 * In addition to starting up the connection, needs to make sure we get an IP address.
	 * @param waittime - maximum time to wait for wifi, in seconds; 
	 * 0 or a negative value to not wait
	 * @param showToasts true allows this to display a Toast message when Wifi state is changed
	 *  (ie, it was disabled and is trying to enable it); false hides all messages
	 * @return true if wifi is connected and enabled, false otherwise.
	 */
	public static boolean ensureConnected(Context context, int waittime, boolean showToasts) {
		boolean connected = false;
		if (!wifiIsConnected(context)) {
			try {
				WifiManager wmgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				if (!(wmgr.getWifiState() == WifiManager.WIFI_STATE_ENABLED)) {
					// not connected, try to enable:
					// - first, make sure it is starting up:
					if (!wmgr.isWifiEnabled()) {
						boolean b = wmgr.setWifiEnabled(true);
						LSLogger.debug(TAG, "Wifi is being started..."+b);
						if (showToasts) { // show message that wifi is being started
							Controller.getInstance(context).showToastMessage(null,R.string.wifi_starting);
						}
					}
					// if we are to wait until connected, do that now
					if (waittime > 0) {
						long maxtime = System.currentTimeMillis() + (waittime*1000);
						LSLogger.debug(TAG, "Waiting for Wifi to start...up to " + waittime + " seconds.");
						while ((wmgr.getWifiState() == WifiManager.WIFI_STATE_ENABLING)) {
							if (maxtime > System.currentTimeMillis()) {
								try {
									Thread.sleep(300);
								} catch (Exception e) {}
							} else {
								LSLogger.debug(TAG, "WifiWait timed out after " + waittime + " seconds.");
								break;
							}
						}
					}
					// now, lets check and see if we need to wait on getting an IP address.
					WifiInfo wifiinfo = wmgr.getConnectionInfo();
					if (wifiinfo != null) {
						//LSLogger.debug(TAG, "Wifi IP address="+wifiinfo.getIpAddress());
						int tries = 45; // wait up to 45 seconds
						while (wifiinfo.getIpAddress()==0 && tries>0) { 
								// && wifiinfo.getSupplicantState()!=SupplicantState.COMPLETED) {
							try {
								Thread.sleep(1000);
							} catch (Exception e) {}
							tries--;
							// get new instance to get updated ip info:
							wifiinfo = wmgr.getConnectionInfo();
						}
						LSLogger.debug(TAG, "Wifi IP address now="+wifiinfo.getIpAddress()+" supstate="+wifiinfo.getSupplicantState());
					}
				}
				connected = (wmgr.getWifiState() == WifiManager.WIFI_STATE_ENABLED);
				LSLogger.debug(TAG, "Wifi check: is wifi connected? "+connected);
			} catch (Exception ex) {
				LSLogger.exception(TAG, "WifiIsConnecting error:",ex);
			}
		}
		return connected;
	}
	
	
}
