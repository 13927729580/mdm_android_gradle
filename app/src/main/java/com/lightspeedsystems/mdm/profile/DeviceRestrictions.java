package com.lightspeedsystems.mdm.profile;

import java.util.Date;

import org.json.JSONObject;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import com.lightspeedsystems.mdm.CommandResult;
import com.lightspeedsystems.mdm.Constants;
import com.lightspeedsystems.mdm.Controller;
import com.lightspeedsystems.mdm.DeviceAdminProvider;
import com.lightspeedsystems.mdm.Profiles;
import com.lightspeedsystems.mdm.util.LSLogger;

public class DeviceRestrictions extends ProfileItem {

	// Supports Restrictions Policy values; similar to restrictions_payload.rb on the server.
		private final static String TAG = "Profiles.DeviceRestrictions";
		static final String displayName = "Restrictions";
		//static final String payloadType = PAYLOADTYPE_Restrictions;
		private JSONObject profileSnapshot;
		
		
		public DeviceRestrictions(Context context, JSONObject data) {
			super(context, PrfConstants.PROFILETYPE_restrictions, PrfConstants.PAYLOADTYPE_Restrictions, false);
			prfstate = PrfConstants.PROFILESTATE_snapshot;
			jdata = data;
		}
		/*
		public DeviceRestrictions(Context context) {
			setContext(context);
		}
		*/

		/**
		 * Abstract method: applies the current profile settings as defined in the json data used during instance creation.
		 * @param cmdResult details of the processing, primarily for capturing errors. 
		 * Can be null if detailed results are not needed.
		 * @return true if the operation succeeded, false if errors occurred, with details in cmdResult.
		 */
		public boolean applyProfile(CommandResult cmdResult) {
			return applyProfile(jdata, cmdResult);
		}
		
		
		/* Internal method for handling setting profile values. */
		private boolean applyProfile(JSONObject jsondata, CommandResult cmdResult) {
			boolean bOk = true;
			DeviceAdminProvider admin = Controller.getInstance(context).getDeviceAdmin();
			profileSnapshot = createSnapshot(admin); // get previous values.
			long timeApplied = new Date().getTime();
			try {
				// process each of the values in the profile:
				// enable/disable camera:
				if (jsondata.has(PrfConstants.PAYLOADVALUE_rs_cameraEnable)) {
					Boolean b = jsondata.getBoolean(PrfConstants.PAYLOADVALUE_rs_cameraEnable);
					boolean prev = admin.enableCamera(b.booleanValue());
					// log/save what we changed:
					if (prev != b.booleanValue())
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_rs_cameraEnable, b.toString(), Boolean.toString(prev));
				}

				//enable/disable storage encryption:
				if (jsondata.has(PrfConstants.PAYLOADVALUE_rs_encryptionEnable)) {
					Boolean b = jsondata.getBoolean(PrfConstants.PAYLOADVALUE_rs_encryptionEnable);
					boolean prev = admin.enableStorageEncryption(b.booleanValue());
					if (prev != b.booleanValue())
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_rs_encryptionEnable, b.toString(), Boolean.toString(prev));
				}

				//enable/disable location reporting:
				if (jsondata.has(PrfConstants.PAYLOADVALUE_rs_androidlocationenable)) {
					Boolean b = jsondata.getBoolean(PrfConstants.PAYLOADVALUE_rs_androidlocationenable);
					int dist = 0;
					int time = 5;
					if (jsondata.has(PrfConstants.PAYLOADVALUE_rs_androidlocationdistance))
						dist = jsondata.getInt(PrfConstants.PAYLOADVALUE_rs_androidlocationdistance);
					if (jsondata.has(PrfConstants.PAYLOADVALUE_rs_androidlocationtime))
						time = jsondata.getInt(PrfConstants.PAYLOADVALUE_rs_androidlocationtime);

					boolean prev = Controller.getInstance(getContext()).getLocationReportEnable();
					if (prev != b.booleanValue())
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_rs_androidlocationenable, b.toString(), Boolean.toString(prev));
					if( dist != Controller.getInstance(getContext()).getLocationDistance())
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_rs_androidlocationdistance, Integer.toString(dist), Integer.toString(Controller.getInstance(getContext()).getLocationDistance()));
					if( time != Controller.getInstance(getContext()).getLocationTime())
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_rs_androidlocationtime, Integer.toString(time), Integer.toString(Controller.getInstance(getContext()).getLocationTime()));

					Controller.getInstance(getContext()).enableLocationReports(b,time,dist);
				}

			} catch (Exception ex) {
				cmdResult.setException(ex);
				bOk = false;
			}
			return bOk;
		}
		
		/**
		 * Abstract method for restoring previously-saved profile values, overwriting current values
		 *  with those values stored in here in the json data, if any.
		 * @param cmdResult details of the processing, primarily for capturing errors. 
		 * Can be null if detailed results are not needed.
		 * @return true if the replacement succeeded, false if errors occurred, with details in cmdResult.
		 */
		public boolean restoreProfile(CommandResult cmdResult) {
			boolean bIsOK = true;
			// apply the profile if we have it and can restore it:
			if (isRestorable())
				bIsOK = applyProfile(getProfileJSON(), cmdResult);//getProfileRestorableJSON
			return bIsOK;
		}
		
		/**
		 * Abstract method for removing profile values, thereby removing the settings applicable to it.
		 * (Note: does not apply to this class; use @restoreProfile to reset values to prior settings.)
		 * @param cmdResult details of the processing, primarily for capturing errors. 
		 * Can be null if detailed results are not needed.
		 * @return true.
		 */
		public boolean removeProfile(CommandResult cmdResult) {
			// n/a, removal is not applicable for this class.
			return true;
		}

		
		/**
		 * Gets a persistable string representation of the profile that is to be stored in the database,
		 * representing previous values.
		 * @return the string representation of the json to be saved with the profile. 
		 */
		@Override
		public String getPersistableProfileStr() {
			String s = Constants.EMPTY_JSON;
			if (profileSnapshot != null) {
				s = profileSnapshot.toString();
			}
			return s;
		}

		
		/** 
		 * Creates a ProfileItem for the current restriction settings, as a profile.
		 * Includes camera and encryption settings.
		 */
		private JSONObject createSnapshot(DeviceAdminProvider adminprovider) {
			JSONObject jdata = null;
			try {
				DeviceAdminProvider admin = Controller.getInstance(context).getDeviceAdmin();
				DevicePolicyManager dpm = admin.getDevicePolicyManager();
				jdata = new JSONObject();				
				// camera:
				jdata.put (PrfConstants.PAYLOADVALUE_rs_cameraEnable, !dpm.getCameraDisabled(null));				
				// storage encryption:
				jdata.put(PrfConstants.PAYLOADVALUE_rs_encryptionEnable, dpm.getStorageEncryption(null));
			} catch (Exception ex) {
				LSLogger.exception(TAG, "CreateSnapshot error:", ex);
			}
			return jdata;
		}
	}


