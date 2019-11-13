/**
 * Provides device-level features.
 */
package com.lightspeedsystems.mdm;

import java.util.Locale;

import com.google.firebase.iid.FirebaseInstanceId;
import com.lightspeedsystems.mdm.util.LSLogger;
import com.lightspeedsystems.mdm.util.StorageUtil;

import android.content.Context;
import android.content.ContentResolver;
import android.telephony.TelephonyManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings.Secure;
import org.json.JSONObject;


/*
These are the server-side device data fields that are currently defined. Some are provided by the device,
 some by the server. Noted below is where values come from:

 server-provided and maintained:
 'active`, `checked_in_at`, `checked_out`, `checked_out_at`, `created_at`, machine_guid`, 
 'enrolling`, `group_id`,  `information_updated_at`, `updated_at`, `user_id`
 
 from configuration data: (external to this class)
  `organization_id` 
  `asset_tag`,  
  `token`,		(gcm ID)
  
 from batteryinfoprovider (external to this class):  
  'battery_level`, 

 from device:
 `udid`					 (deviceID)
 `imei` or `meid`,       (telephony.deviceid, set to imei or meid based on phonetype)
 `cellular_technology`,  (telephony.iPhoneType - integer phone type value as defined in device.rb on server)
  
  from os.Build: 
 `build_version`, 
 `device_name`, 
 `model_name`, 
 `model_number`, 
 `os_version`, 
 `product_name`, 

 from file system:
 `available_device_capacity`, 
 `device_capacity`,  

unknown:
 `modem_firmware_version`, 


 n/a to android:
 `push_magic`, `serial_number`, `unlock_token`, 
*/
	

/**
 * Contains device-level data. Uses various APIs to access Android device settings and features.
 */
public class Device {
	private final static String TAG = "Device";	
	private final static String STR_SERIALNUMBER_none    = "(none)";
	private final static String STR_SERIALNUMBER_unknown = "unknown";
	private final static int SERIALNUMBER_MIN_LENGTH = 8;
	private Context context;
	private Settings settings; // reference to local settings storage
	
	private TelephonySettings telephonySettings; // device's telephony-related settings and properties
	private String deviceID;
	private String deviceUDID; // local-cached UDID value for the device.
	
	/**
	 * Constructs a Device instance.
	 * @param context Android application context.
	 * @param settings reference to application Settings, used for storing several key device values.
	 */
	public Device(Context context, Settings settings) {
		this.context = context;
		this.settings = settings;
		telephonySettings = new TelephonySettings(context);
	}
	
	/**
	 * Gets the device unique identifier (UDID). 
	 * 
	 * First looks in Settings to see if the ID was already obtained, and uses that value if present so
	 * that we ensure the same UDID is always used for a device. If the value is not there, queries the
	 * device settings and gets it, saving the value back to Settings if found.
	 * @return the device's UDID, or null if one can't be obtained.
	 */
	public String getDeviceID() {
		if (deviceID == null) {
			deviceID = settings.getDeviceUdid();
			if (deviceID == null) {
				// device ID is not set yet, so let's get it.
				getDeviceSettings();
				if (deviceID != null)
					settings.setDeviceUdid(deviceID);
			}
		}
		return deviceID;
	}
	
	/* Gets device-specific UDID. */
	public String getDeviceUDID() {
		ContentResolver cr = context.getContentResolver();
		// Gets the device UDID (log a warning if the UDID has changed, which should not normally happen.)
		if (deviceUDID == null)
			deviceUDID = Secure.getString(cr, Secure.ANDROID_ID);
		return deviceUDID;
	}

	
	/**
	 * Gets the operating system version. 
	 * @return the OS version, such as 4.2.1
	 */
	public String getOSVersion() {
		return Build.VERSION.RELEASE;
	}
	
	/**
	 * Gets the unique build, consisting of: brand/product/name:osversion/display/build
	 * @return the build version.
	 */
	public String getOSBuild() {
		return Build.FINGERPRINT;
	}
	
	/**
	 * Gets the operating system SDK framework version. 
	 * @return the OS SDK version.
	 */
	public String getOSSDKVersion() {
		return Integer.toString(Build.VERSION.SDK_INT);
	}

	/**
	 * Gets the operating system bootloader version. 
	 * @return the bootloader version.
	 */
	public String getOSBootloaderVersion() {
		return Build.BOOTLOADER;
	}

	/**
	 * Gets the brand (such as the carrier) the system is customized for, if any 
	 * @return the device brand name, such as "google" on a Nexus 7.
	 */
	public String getDeviceBrand() {
		return Build.BRAND;
	}
	
	/**
	 * Gets a displayable name of the device. 
	 * @return the user name and device model info, such as "Mike - Nexus7". 
	 * If there is no user, creates the device from the model name and part of the 
	 * device's serial number, such as "Nexus7-12345678"
	 */
	public String getDisplayableDeviceName() {
		// get user name, then append with model_number:
		// .. Build.MODEL;
		
		UserAuthority um = Controller.getInstance(context).getUserAuthority();
		if (um != null) {
			String username = um.getUserName();
			if (username != null)
				return username + " - " + Build.MODEL;
		}
		
		// at this point, we need to build the name from model and serial number:
		int numsnchars = 8; // number of characters from serial number to use.
		String nm = Build.MODEL;
		String sn = getDeviceSerialNumber();
		if (sn != null && !sn.isEmpty()) {
			if (sn.length() > numsnchars) {
				// serial number is longer than the length; get those last 'n' characters:
				nm += "-" + sn.substring(sn.length()-numsnchars);
			} else { // use the entire serial number
				nm += "-"+sn; 
			}
		}
		return nm;
	}
	
	/**
	 * Gets the industrial name of the device. 
	 * @return the name, such as "grouper" on a Nexus 7.
	 */
	public String getDeviceName() {
		return Build.DEVICE;
	}

	/**
	 * Gets the build ID string. 
	 * @return the display build ID, such as"JOP4OD'.
	 */
	public String getDeviceDisplay() {
		return Build.DISPLAY;
	}
	
	/**
	 * Gets the manufacturer. 
	 * @return the manufacturer of the device, such as "asus" on a Nexus 7.
	 */
	public String getDeviceManufacturer() {
		return Build.MANUFACTURER;
	}

	/**
	 * Gets the end-user visible model name of the device. 
	 * @return the model name, such as "Nexus 7".
	 */
	public String getDeviceModel() {
		return Build.MODEL;
	}

	/**
	 * Gets the product name of the overall device. 
	 * @return the product name, such as "nakasi" on a Nexus 7.
	 */
	public String getDeviceProduct() {
		return Build.PRODUCT;
	}

	
	/* Device Serial Number notes: most devices will have a serial number; but a few,
	 *  such as Rapid7 devices with Android 4.1.1, will report an "unknown" serial number.
	 * MDM needs a stable identifier; so if we have none, we need to create one that will
	 * likely be the same across device wipes. 
	 * First of all, the serial number is not definitively needed until the device enrolls,
	 *  at which time the device should have an organization identifier,
	 *  which can be used as part of a serial number. Another part of
	 *  a serial number can be the mac address of the device; it should remain the same
	 *  within the same network after device wiped (it did in development testing). All or
	 *  any of these can be used to create a serial number that should be consistent
	 *  across device wipes.
	 * Since the serial number is not needed until enrollment and check-in, any query
	 *  to serial number, such as when displaying device info from within mdm, can
	 *  display either 'unknown' or some standard 'waiting-to-enroll-to-be-created'
	 *  message, and then when the device enrolls, it creates the serial number.
	 * To read the serial number that is displayed, call getDeviceSerialNumber; it 
	 *  may return an invalid serial, such as "unknown", until a serial number is created.
	 * To force serial number creation if one does not exist, use method 
	 *  getOrCreateDeviceSerialNumber to get one or create one, which is saved in settings.
	 * There can be logic to determine if the serial is valid or not, by looking first in
	 *  settings, and using any value that is there, but otherwise checking for non-hex
	 *   characters in the serial number string and a short length of text. */
	
	/**
	 * Gets the device's hardware serial number, if available. If the serial number was
	 *  obtained or created at some time, the saved value is retrieved and returned.
	 * @return the hardware serial number of the device. 
	 * May return "unknown" or similar if no serial number is present (on some devices).
	 * Will return some string, never a null nor empty string.
	 */
	public String getDeviceSerialNumber() {
		String sn = null;
		if (settings != null)
			sn = settings.getDeviceSerialNumber();
		if (sn == null)
			sn = Build.SERIAL;
		if (sn == null || sn.isEmpty())
			sn = STR_SERIALNUMBER_none;
		return sn;
	}
	
	/*
	 * Gets or creates the serial number. First attempts to read a serial number
	 *  from settings. If not found, gets the device's serial number and checks it,
	 *  and if needed, creates a serial number. Worst-case, use the UDID. Then saves
	 *  the serial number in settings so that next time it is needed, it is obtained
	 *  from settings.
	 * @return serial number for the device
	 */
	private String getOrCreateDeviceSerialNumber() {
		String sn = null;
		if (settings != null) {
			sn = settings.getDeviceSerialNumber();
			if (sn == null) {
				String savedSerialId = StorageUtil.getSerialId();
				if(savedSerialId.isEmpty()) {
					savedSerialId  = FirebaseInstanceId.getInstance().getToken();
					StorageUtil.saveSerialId(savedSerialId);
				}
				sn = savedSerialId;
/*
				// we have no serial number; read it from the device:
				sn = Build.SERIAL;
				// if the serial number is not valid, we'll create a good one:
				if (!isSerialNumberValid(sn)) {
					sn = generateSerialNumber();
					if (sn == null)  {
						// we didnt create a serial number; revert to UDID, 
						//  which is not good, but better than nothing (it can change on a wipe).
						LSLogger.warn(TAG, "WARNING: Failed to get Device Serial Number; reverting to UDID.");
						sn = getDeviceUDID();
					}
				}
*/
				// since we now have a serial number, save it!
				settings.setDeviceSerialNumber(sn);
			} // else we use the previously-saved serial number, whatever it is.
		} else {
			sn = getDeviceSerialNumber();  // default to get any serial number, even a "none".
			LSLogger.error(TAG, "No Settings instance in getOrCreteDeviceSerialNumber; serial number found is: "+sn);
		}
		return sn;
	}
	
	/* checks a given serial number for validity. */
	private boolean isSerialNumberValid(String sn) {
		boolean isvalid = true;
		if (sn == null || sn.length() < SERIALNUMBER_MIN_LENGTH ||
				sn.equalsIgnoreCase(STR_SERIALNUMBER_unknown) ||
				sn.equalsIgnoreCase(STR_SERIALNUMBER_none) )
			isvalid = false;
		// note: could also check each digit to make sure it is numeric or a-f
		return isvalid;
	}
	
	/* Create a serial number from various values that should be available. 
	 * */
	private String generateSerialNumber() {		
		String sn = null;
		try {
			if (settings != null) {
				String groupid  = settings.getGroupID();
				int ngroup  = 0;
				
				// get mac address as a hex string:
				String macaddr = convertMacToString(getWifiMAC().toLowerCase());
				
				try {
					if (groupid != null)
						ngroup = 0x000FFFFF & Integer.valueOf(groupid);
				} catch (NumberFormatException nfex) { 
					LSLogger.exception(TAG, "Exception getting numeric value for group "+groupid, nfex);
				}
					
				// now put it all together: use groupid if possible:
				if (ngroup != 0)
					sn = String.format(Locale.US, "g%05x%s", ngroup, macaddr);
				else // default to mac address and a fixed prefix.					
					sn = "g0fff0" + macaddr;

				LSLogger.debug(TAG, "Created device serial number: " + sn);
			}
		} catch (Exception ex) {
			LSLogger.exception(TAG, "Exception getting Device Serial Number.", ex);
		}
		return sn;
	}
	
	/**
	 * Converts a mac address string to a string without the ":" separators.
	 * @param mac string to parse
	 * @return the mac address without the separator characters, or empty string if no address.
	 */
	public static String convertMacToString(String mac) {
		StringBuilder sb = new StringBuilder();
		if (mac != null && mac.length()>0) {
			String vals[] = mac.split(":");
			if (vals != null) {
				for (int i=0; i<vals.length; i++)
					sb.append(vals[i]);
			}
			LSLogger.debug(TAG, "Converted mac address from '" + mac + "' to '"+sb.toString()+"'");
		}
		return sb.toString();
	}
	
	
	/**
	 * Gets relevant static/fixed data values as a collection of json name-value pairs for parameters 
	 * to be sent to server.
	 * @param json existing instance to add to, or null to create a new one.
	 * @param includeAll if true, includes everything about the device; if false, only gets minimum
	 * device identification values.
	 * @return json instance with device values added to it.
	 */
	public JSONObject getStaticJSONparams(JSONObject json, Settings settings, boolean includeAll) {
		if (json == null)
		  json = new JSONObject();
		try {
			json.put(Constants.PARAM_DEVICE_TYPE, Constants.DEVICE_TYPE_ANDROID);
			json.put(Constants.PARAM_DEVICE_UDID, getDeviceID());		
			json.put(Constants.PARAM_DEVICE_NAME, getDisplayableDeviceName());
			if (settings != null) {
				// For group ID, we send it only if we have not sent it before, or
				//  if the group ID has changed since the last submission
				String groupid = settings.getGroupID();
				if (groupid != null) {					
					String sentID = settings.getSentGroupID();
					if (sentID == null || !sentID.equals(groupid))
						json.put(Constants.PARAM_GROUPID, groupid);
				}
				String parentid = settings.getParentID();
				String parentname = settings.getParentName();
				String parenttype = settings.getParentType();
				String enrollmentcode = settings.getEnrollmentCode();
				if (parenttype != null)
					json.put(Constants.PARAM_PARENTTYPE, parenttype);
				if (parentid != null)
					json.put(Constants.PARAM_PARENTID, parentid);
				if (parentname != null)
					json.put(Constants.PARAM_PARENTNAME, parentname);
				if (enrollmentcode != null)
					json.put(Constants.PARAM_ENROLLMENTCODE, enrollmentcode);
				
				json.put(Constants.PARAM_GCMTOKEN, settings.getGcmID());
				json.put(Constants.PARAM_ASSETTAG, settings.getSetting(Settings.ASSETTAG));
			}
			if (includeAll) {
				json.put(Constants.PARAM_DEVICE_SERIALNUM, 	 getDeviceSerialNumber());
				// moved the line-below to above to always include device name:
				// json.put(Constants.PARAM_DEVICE_NAME, 		 getDisplayableDeviceName());
				json.put(Constants.PARAM_DEVICE_PRODUCTNAME, getDeviceProduct());
				json.put(Constants.PARAM_DEVICE_MODELNAME,   getDeviceBrand());
				json.put(Constants.PARAM_DEVICE_MODELNUM,    getDeviceModel());
			
				telephonySettings.getJSON(json);
			}

		} catch (Exception e) {
			LSLogger.exception(TAG, "getStaticJSONparams error: ", e);
		}
		return json;
	}
	
	/**
	 * Gets data values that can change, as a collection of json name-value pairs for parameters to be sent to server.
	 * @param json existing instance to add to, or null to create a new one.
	 * @return json instance with device values added to it.
	 */
	public JSONObject getVolitileJSONparams(JSONObject json) {
		if (json == null)
		  json = new JSONObject();
		try {
			json.put(Constants.PARAM_DEVICE_OSVERSION, getOSVersion());
			json.put(Constants.PARAM_DEVICE_OSBUILD,   getOSBuild());
			json.put(Constants.PARAM_DEVICE_WIFI_MAC,  getWifiMAC());
			getFilesystemValues(json);
			
		} catch (Exception e) {
			LSLogger.exception(TAG, "getVolitileJSONparams error: ", e);
		}
		return json;
	}
	
	private void getFilesystemValues(JSONObject json) {
		try {
			// Get filesystem stats:
			// - this requires getting the possible different path values: 

			float divisorFactor = 1024 * 1024 * 1024;  // gigabytes   

			// - data path:
			StatFs fstats = new StatFs(Environment.getDataDirectory().getPath());			
			float availableBlocks = fstats.getAvailableBlocks();
			float totalBlocks = fstats.getBlockCount();
			float blockSize = fstats.getBlockSize();
			float total = (totalBlocks * blockSize);
			float avail = (availableBlocks * blockSize);
			LSLogger.debug(TAG, "Data Filespace: avail="+availableBlocks+" total="+totalBlocks+" blocksize="+blockSize+ " total="+total+" avail="+avail);
			/**
			// - system root path:
			 fstats = new StatFs(Environment.getRootDirectory().getPath());			
			 availableBlocks = fstats.getAvailableBlocks();
			 totalBlocks = fstats.getBlockCount();
			 blockSize = fstats.getBlockSize();
			 total += (totalBlocks * blockSize);
			 avail += (availableBlocks * blockSize);
			LSLogger.debug(TAG, "System Filespace: avail="+availableBlocks+" total="+totalBlocks+" blocksize="+blockSize+ " total="+total+" avail="+avail);
			// - cache path:
			fstats = new StatFs(Environment.getDownloadCacheDirectory().getPath());			
			availableBlocks = fstats.getAvailableBlocks();
			totalBlocks = fstats.getBlockCount();
			blockSize = fstats.getBlockSize();
			total += (totalBlocks * blockSize);
			avail += (availableBlocks * blockSize);
			LSLogger.debug(TAG, "Cache Filespace: avail="+availableBlocks+" total="+totalBlocks+" blocksize="+blockSize+ " total="+total+" avail="+avail);
			// - external storage:
			 */
			
			total = total / divisorFactor;
			avail = avail / divisorFactor;

			json.put(Constants.PARAM_DEVICE_CAPACITY, 			Float.valueOf(total));
			json.put(Constants.PARAM_DEVICE_AVAILABLE_CAPACITY, Float.valueOf(avail));
			
		    //LSLogger.debug(TAG, "Total Filespace: avail="+avail+" total="+total);

		} catch (Exception e) {
			LSLogger.exception(TAG, "getFilesystemValues error: ", e);
		
		}
	}
	
	/** Gets the device's Wifi Mac address. */
	public String getWifiMAC() {
		String addr = null;
		try {
			WifiManager wmgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			if (wmgr != null) {
				WifiInfo wi = wmgr.getConnectionInfo();
				if (wi != null) {
					addr = wi.getMacAddress();
					if (addr == null || addr.isEmpty())
						addr = wi.getBSSID();
				}
			}
		} catch (Exception ex) {
			LSLogger.exception(TAG, "getWifiMac error:", ex);
		}
		return addr;
	}
	
	/* Gets device-specific values from Android APIs calls. */
	private void getDeviceSettings() {
		/** -- change (mz): do not get the device UDID; instead, use serial number:
		ContentResolver cr = context.getContentResolver();
		// Gets the device UDID (log a warning if the UDID has changed, which should not normally happen.)
		String newdeviceID = Secure.getString(cr, Secure.ANDROID_ID);
		if (deviceID != null && !deviceID.equals(newdeviceID))
			LSLogger.warn(TAG, "Device UDID has CHANGED!");
		deviceID = newdeviceID;
		**/

		deviceID = getOrCreateDeviceSerialNumber(); // sets device id to serial number
		
		// ..others...
		/// note, other settings are in android.provider.Settings.Global		
	}
	
		

	/* Inner-class for holding telephony settings: */
	class TelephonySettings {
		private final String TAG = "Devices.TelephonySettings";
		
		boolean hasData;
		String telDeviceID;
		String MEID;
		String IMEI;
		String phoneType; // cellular technology type
		
		private int iPhoneType;
		private Context context;
		
		/* Instance constructor. */
		protected TelephonySettings(Context ctx) {
			context = ctx;
		}
		
		/* Adds device's telephony-related values to the given json object. */
		protected void getJSON(JSONObject json) {
			// make sure we have the data:
			if (!hasData)
				getTelephonySettings();
			// get the values that are present:
			try {
				if (phoneType != null) // note: mdmserver uses the iPhoneType integer value instead of the string name.
					//json.put(Constants.PARAM_CELLULAR_TYPE, phoneType);
					json.put(Constants.PARAM_CELLULAR_TYPE, Integer.toString(iPhoneType));
				if (IMEI != null)
					json.put(Constants.PARAM_IMEI, IMEI);
				if (MEID != null)
					json.put(Constants.PARAM_MEID, MEID);
			} catch (Exception e) {
				LSLogger.exception(TAG, e);
			}			
		}

		/*
		 * Obtains device's telephony settings and features. Stores value in this TelephonySettings object.
		 */
		protected void getTelephonySettings() {
			TelephonyManager tmgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			if (tmgr != null) {
				try {
					// (note: do this before getting the deviceID, so it can set the IMEI or MEID at the same time)
					setPhoneType(tmgr.getPhoneType());
					
					// Gets the unique device ID, for the IMEI for GSM and the MEID or ESN for CDMA phones.
					setDeviceID(tmgr.getDeviceId()); 
					
					hasData = true;
					
				} catch (Exception ex) {
					LSLogger.exception(TAG, ex);
				}
			}
		}
		
		
		/* Sets Telephony DeviceID. If the phone type is already set, also sets the imei or meid to the same value.*/
		protected void setDeviceID(String id) {
			telDeviceID = id;
			// using the phone type, we also set either the IMEI or MEID:
			if (iPhoneType == TelephonyManager.PHONE_TYPE_CDMA)
				MEID = id;
			else if	(iPhoneType == TelephonyManager.PHONE_TYPE_GSM)
				IMEI = id;
		}
		
		/* Sets the integer phone type value, and converts the value to an internal string name. */
		protected void setPhoneType(int type) {
			iPhoneType = type;
			switch (type) {
				case TelephonyManager.PHONE_TYPE_CDMA: 
					phoneType = "CDMA";
					break;
				case TelephonyManager.PHONE_TYPE_GSM:
					phoneType = "GSM";
					break;
				case TelephonyManager.PHONE_TYPE_SIP: 
					phoneType = "SIP";
					break;
				case TelephonyManager.PHONE_TYPE_NONE: 
					phoneType = "None";
					break;
				default:
					phoneType = "Unknown";
					break;
			}			
		}
	}
	
}
