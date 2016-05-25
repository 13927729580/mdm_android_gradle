package com.lightspeedsystems.mdm;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;

import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Provides Global Proxy settings support.
 *
 * Global device proxy settings are stored in Settings.Global settings. 
 * NOTE: Android SDK 17 or higher must be used for access to this; be sure to check version before calling it!
 * 
 * We use a ContentResolver
 * to access that data, and read and write the HTTP_PROXY setting.
 * 
 * Note that the app must have manifest permission android.permission.WRITE_SECURE_SETTINGS,
 * but that is reserved for system-signed apps. Without the permission, we can only read the existing setting.
 */
public class GlobalProxy {
	private static final String TAG = "GlobalProxy";
    private Context context;
    private String proxySetting; // the stored setting, is server:port
    private String proxyServer;  // separated server
    private String proxyPort;    // separated port
	
	public GlobalProxy(Context context) {
		this.context = context;
	}
	
	/**
	 * Gets server plus port for the proxy setting.
	 * @return server:port if server and port are found, null if nothing found, or server by itself if no port.
	 */
	public String getServerAndPort() {
		proxySetting = null;
		readProxy();
		setInternalValues();
		return proxySetting;
	}
	
	/**
	 * Gets server part of the proxy.
	 * @return Server name or address, null if not set.
	 */
	public String getServer() {
		getServerAndPort(); // get latest values
		return proxyServer;
	}
	
	/**
	 * Gets the port part of the proxy, if present.
	 * @return port value, or null if not set.
	 */
	public String getPort() {
		return proxyPort;
	}
	
	/* sets server and port from combined url value; first clears existing values, setting to null. */
	private void setInternalValues() {
		proxyServer = null;
		proxyPort = null;
		// parse out the sever and port:
		if (proxySetting != null) {
			String[] splits = proxySetting.split(":");
			if (splits == null || splits.length < 2) {
				proxyServer = proxySetting;
			} else {
				proxyServer = splits[0];
				proxyPort = splits[1];
			}
		}
	}
	
	
	/**
	 * Reads the current global proxy setting from the device.
	 */
	private void readProxy() {
        try {
        	ContentResolver cr = context.getContentResolver();
        	// read current proxy setting:
    	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        	    // this is applicable only in sdk 17 and later:
	        	proxySetting = android.provider.Settings.Global.getString(cr, android.provider.Settings.Global.HTTP_PROXY);
	        	LSLogger.debug(TAG, "ReadGlobalProxy="+(proxySetting==null?"null":proxySetting));
    	    } else {
	        	LSLogger.debug(TAG, "ReadGlobalProxy: not supported in SDK version "+ Build.VERSION.SDK_INT);   	    	
    	    }
        } catch (Exception ex) {
        	LSLogger.exception(TAG, "ReadGlobalProxy error:", ex);
        }		
	}
	

	/**
	 * Sets the proxy url to the value given.
	 * @param serverUrl server:port string, or null to clear the value.
	 * @return true if the value was set, false if error.
	 */
	public boolean setProxy(String serverUrl) {
		boolean bres = false;
        try {
    	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {

        	ContentResolver cr = context.getContentResolver();
        	bres = android.provider.Settings.Global.putString(cr, android.provider.Settings.Global.HTTP_PROXY, 
        			serverUrl);
        	LSLogger.debug(TAG, "WriteGlobalProxy="+bres+" set to="+(serverUrl==null?"null":serverUrl));
        	if (bres) {
        		proxySetting = serverUrl;
        	} else {
        		proxySetting = null;
        	}
        	setInternalValues();
    	    } else {
    	    	LSLogger.debug(TAG, "SetProxy: GlobalProxy not supported in SDK " + Build.VERSION.SDK_INT);
    	    }
        } catch (Exception ex) {
        	LSLogger.exception(TAG, "SetGlobalProxy error:", ex);
        }		
		return bres;
	}
	
	
	
	
	// temporary test code for reading and writing global proxy setting.
	public static void test(Context context) {
        try {
        	ContentResolver cr = context.getContentResolver();
        	// to test read access, lets read a known-present setting, such as if bluetooth is on or off.
        	String ep=android.provider.Settings.Global.getString(cr, android.provider.Settings.Global.BLUETOOTH_ON);
        	LSLogger.debug(TAG, "Testing ContentResolver - getexsiting bluetooth="+(ep==null?"null":ep));
        	
        	// read current proxy setting:
        	ep=android.provider.Settings.Global.getString(cr, android.provider.Settings.Global.HTTP_PROXY);
        	LSLogger.debug(TAG, "Testing ContentResolver - getexsitingproxy="+(ep==null?"null":ep));
        	boolean bres = android.provider.Settings.Global.putString(cr, android.provider.Settings.Global.HTTP_PROXY, "192.168.1.101:3000");
        	LSLogger.debug(TAG, "Testing ContentResolver - putstring result="+bres);
        	if (bres) {
        		//see if it is there now:
        		ep=android.provider.Settings.Global.getString(cr, android.provider.Settings.Global.HTTP_PROXY);
            	LSLogger.debug(TAG, " -shoud be there now: getexsitingproxy="+(ep==null?"null":ep));
            	// delete it:
               	bres = android.provider.Settings.Global.putString(cr, android.provider.Settings.Global.HTTP_PROXY, null);
            	LSLogger.debug(TAG, "Testing deleting proxy result="+bres);
            	// check it
            	ep=android.provider.Settings.Global.getString(cr, android.provider.Settings.Global.HTTP_PROXY);
            	LSLogger.debug(TAG, " -shoud be gone now: getexsitingproxy="+(ep==null?"null":ep));            	
        	}
        } catch (Exception ex) {
        	LSLogger.exception(TAG, "GlobalProxy error:", ex);
        }		
	}
	
}
