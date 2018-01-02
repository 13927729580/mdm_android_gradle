package com.lightspeedsystems.mdm.profile;

import java.util.Date;

import org.json.JSONObject;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;

import com.lightspeedsystems.mdm.CommandResult;
import com.lightspeedsystems.mdm.Constants;
import com.lightspeedsystems.mdm.Controller;
import com.lightspeedsystems.mdm.DeviceAdminProvider;
import com.lightspeedsystems.mdm.Profiles;
import com.lightspeedsystems.mdm.util.LSLogger;

/** Provides Passcode Policy settings support; similar to passcode_policy_android_payload.rb on the server.
 */
public class PasscodePolicy extends ProfileItem {
		private final static String TAG = "Profiles.PasscodePolicy";
		static final String displayName = "PasscodePolicy";
		//static final String payloadType = PAYLOADTYPE_PasscodePolicy;
		private JSONObject profileSnapshot;
			
		long pwExpirationMultFactor = (60000 * 60 * 24); // #of seconds in a day
		long maxTimeToLockMultFactor = 1000;  // #millisecs in a second 
		
		public PasscodePolicy(Context context, JSONObject data) {
			super(context, PrfConstants.PROFILETYPE_passcode, PrfConstants.PAYLOADTYPE_PasscodePolicy, false);
			prfstate = PrfConstants.PROFILESTATE_snapshot;
			jdata = data;
		}
		
/*		public PasscodePolicy(Context context) {
			super(PrfConstants.PROFILETYPE_passcode, PrfConstants.PAYLOADTYPE_PasscodePolicy, false);
			prfstate = PrfConstants.PROFILESTATE_snapshot;
			this.context = context;
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

		
		/*
		 * Apply the policy definitions from the given json object.
		 */
		private boolean applyProfile(JSONObject jsondata, CommandResult cmdResult) {
			boolean bIsOK = true;
			DeviceAdminProvider admin = Controller.getInstance(context).getDeviceAdmin();
			profileSnapshot = createSnapshot(admin); // get snapshot of previous values.
			long timeApplied = new Date().getTime();
			//LSLogger.debug(TAG, "Applying profile for "+getName());
			try {
				// process each of the possible values in the profile:
				
				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_required)) {
					Boolean b = jsondata.getBoolean(PrfConstants.PAYLOADVALUE_pw_required);
					boolean prev = admin.setPasscodeRequired(b.booleanValue());
					// log/save what we changed:
					if (prev != b.booleanValue())
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_required, b.toString(), Boolean.toString(prev));
				}

				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_maxfailattempts)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_maxfailattempts);
					int prevValue = admin.setPasscodeWipeMaxFailAttempts(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_maxfailattempts, 
							Integer.toString(newValue), Integer.toString(prevValue));
				}
				
/*** -- this causes the screen to be locked out **/				
				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_maxtimetolock)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_maxtimetolock);
					if (newValue < PrfConstants.PWMAXTIMETOLOCKMIN && newValue != 0) {
						LSLogger.debug(TAG, "Overriding MaxTimeToLock value from "+newValue+" to "+PrfConstants.PWMAXTIMETOLOCKMIN);
						newValue = PrfConstants.PWMAXTIMETOLOCKMIN;
						
					}
					long newValueAdjusted = newValue*maxTimeToLockMultFactor;//1000;
					long prevValue = admin.setPasscodeMaxTimeToLock(newValueAdjusted);
					if (newValueAdjusted != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_maxtimetolock, 
							Long.toString(newValueAdjusted), Long.toString(prevValue));
				}
/***/
				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_expiration)) {
					long multFactor = pwExpirationMultFactor;// (60000 * 60 * 24);  // # of milleseconds in a day
					int newValueDays = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_expiration);
					long newValueMSecs = newValueDays * multFactor;
					long prevValueMSecs = admin.setPasscodeExpirationMSecs(newValueMSecs);
					if (prevValueMSecs != newValueMSecs) {
						int prevValueDays = (int)(prevValueMSecs / multFactor); 
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_expiration, 
							Integer.toString(newValueDays), Integer.toString(prevValueDays));
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_expiration_msecs, 
							Long.toString(newValueMSecs), Long.toString(prevValueMSecs));
					}
				}
				
				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_history)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_history);
					int prevValue = admin.setPasscodeHistoryLength(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_history, 
							Integer.toString(newValue), Integer.toString(prevValue));
				}
				
				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_minlength)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_minlength);
					int prevValue = admin.setPasscodeMinLength(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minlength, 
							Integer.toString(newValue), Integer.toString(prevValue));
				}
				
				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_minnumletters)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_minnumletters);
					int prevValue = admin.setPasscodeNumberOfLetters(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minnumletters, 
							Integer.toString(newValue), Integer.toString(prevValue));
				}
				
				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_minlowercasechars)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_minlowercasechars);
					int prevValue = admin.setPasscodeMinNumberLowercaseChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minlowercasechars, 
							Integer.toString(newValue), Integer.toString(prevValue));
				}

				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_minuppercasechars)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_minuppercasechars);
					int prevValue = admin.setPasscodeMinNumberUppercaseChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minuppercasechars, 
							Integer.toString(newValue), Integer.toString(prevValue));
				}

				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_mincomplexchars)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_mincomplexchars);
					int prevValue = admin.setPasscodeMinNumberComplexChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_mincomplexchars, 
							Integer.toString(newValue), Integer.toString(prevValue));
				}

				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_minnumericchars)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_minnumericchars);
					int prevValue = admin.setPasscodeMinNumberNumericChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minnumericchars, 
							Integer.toString(newValue), Integer.toString(prevValue));
				}

				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_minnonletterchars)) {
					int newValue = jsondata.getInt(PrfConstants.PAYLOADVALUE_pw_minnonletterchars);
					int prevValue = admin.setPasscodeMinNumberNonLetterChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minnonletterchars, 
							Integer.toString(newValue), Integer.toString(prevValue));
				}

				// passcode quality: (converts from a string-value to a predefined android int value)
				if (jsondata.has(PrfConstants.PAYLOADVALUE_pw_passcodequality)) {
					String newValue  = jsondata.getString(PrfConstants.PAYLOADVALUE_pw_passcodequality);
					int qualityLevel = passcodeQualityIntFromStr(newValue);
					int prevLevel    = admin.setPasscodeQuality(qualityLevel);
					if (qualityLevel != prevLevel) {
						String prevValue = passcodeQualityStrFromInt(prevLevel);
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_passcodequality, 
							newValue, prevValue);
					}
				} else {
					int qualityLevel = passcodeQualityIntFromStr("none");
					int prevLevel    = admin.setPasscodeQuality(qualityLevel);
					if (qualityLevel != prevLevel) {
						String prevValue = passcodeQualityStrFromInt(prevLevel);
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_passcodequality,
								"none", prevValue);
					}

					int newValue = 0;
					int prevValue = admin.setPasscodeMinLength(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minlength,
								Integer.toString(newValue), Integer.toString(prevValue));

					prevValue = admin.setPasscodeNumberOfLetters(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minnumletters,
								Integer.toString(newValue), Integer.toString(prevValue));
					prevValue = admin.setPasscodeMinNumberLowercaseChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minlowercasechars,
								Integer.toString(newValue), Integer.toString(prevValue));
					prevValue = admin.setPasscodeMinNumberUppercaseChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minuppercasechars,
								Integer.toString(newValue), Integer.toString(prevValue));
					prevValue = admin.setPasscodeMinNumberComplexChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_mincomplexchars,
								Integer.toString(newValue), Integer.toString(prevValue));
					prevValue = admin.setPasscodeMinNumberNumericChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minnumericchars,
								Integer.toString(newValue), Integer.toString(prevValue));
					prevValue = admin.setPasscodeMinNumberNonLetterChars(newValue);
					if (newValue != prevValue)
						Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_pw_minnonletterchars,
								Integer.toString(newValue), Integer.toString(prevValue));
				}

				Controller.getInstance(context).getDeviceAdmin().showPasswordPromptCheck();


			} catch (Exception ex) {
				cmdResult.setException(ex);
				bIsOK = false;
			}			
			return bIsOK;
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
		 * Gets a password quality value from a string description.
		 * @param s a PrfConstants.PAYLOADVALUE_pwquality_ value.
		 * @return a DevicePolicyManager.PASSWORD_QUALITY_ value, 
		 * or PASSWORD_QUALITY_UNSPECIFIED if the s param is not recognized or is null.
		 */
		public int passcodeQualityIntFromStr(String s) {
			int level = DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
			if (s != null) {
				if (s.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_pwquality_complex))
					level = DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
				else if (s.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_pwquality_alphanum))
					level = DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC;
				else if (s.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_pwquality_alpha))
					level = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
				else if (s.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_pwquality_numeric))
					level = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
				else if (s.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_pwquality_any))
					level = DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
				else if (s.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_pwquality_biometric))
					level = DevicePolicyManager.PASSWORD_QUALITY_BIOMETRIC_WEAK;
			}
			return level;
		}
		
		/**
		 * Gets a password quality string from a value.
		 * @param level a DevicePolicyManager.PASSWORD_QUALITY_ value.
		 * @return a PrfConstants.PAYLOADVALUE_pwquality_ value,
		 * or PAYLOADVALUE_pwquality_none if the level param is not valid.
		 */
		public String passcodeQualityStrFromInt(int level) {
			String s = PrfConstants.PAYLOADVALUE_pwquality_none; // default to none
			switch (level) {
				case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING: 
					s = PrfConstants.PAYLOADVALUE_pwquality_any; break;
				case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
					s = PrfConstants.PAYLOADVALUE_pwquality_numeric; break;
				case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
					s = PrfConstants.PAYLOADVALUE_pwquality_alpha; break;
				case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
					s = PrfConstants.PAYLOADVALUE_pwquality_alphanum; break;
				case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
					s = PrfConstants.PAYLOADVALUE_pwquality_complex; break;
				case DevicePolicyManager.PASSWORD_QUALITY_BIOMETRIC_WEAK:
					s = PrfConstants.PAYLOADVALUE_pwquality_biometric; break;
			}
			return s;
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


		
		// create ProfileItem json for the current passcode settings as a profile.
		private JSONObject createSnapshot(DeviceAdminProvider adminprovider) {
			JSONObject jdata = null;
			try {
				DeviceAdminProvider admin = adminprovider;
				DevicePolicyManager dpm = admin.getDevicePolicyManager();
				jdata = new JSONObject();				
				//-no need to set this one, using passcodequality instead:	jdata.put(PAYLOADVALUE_pw_required, 				
				jdata.put(PrfConstants.PAYLOADVALUE_pw_maxfailattempts,   dpm.getMaximumFailedPasswordsForWipe(null));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_maxtimetolock, 
						(dpm.getMaximumTimeToLock(null)/maxTimeToLockMultFactor));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_expiration,		 
						(dpm.getPasswordExpirationTimeout(null)/pwExpirationMultFactor));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_history,			  dpm.getPasswordHistoryLength(null));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_minlength,		  dpm.getPasswordMinimumLength(null));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_minnumletters,     dpm.getPasswordMinimumLetters(null));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_minlowercasechars, dpm.getPasswordMinimumLowerCase(null));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_minuppercasechars, dpm.getPasswordMinimumUpperCase(null));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_mincomplexchars,	  dpm.getPasswordMinimumSymbols(null));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_minnumericchars,   dpm.getPasswordMinimumNumeric(null));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_minnonletterchars, dpm.getPasswordMinimumNonLetter(null));
				jdata.put(PrfConstants.PAYLOADVALUE_pw_passcodequality,   dpm.getPasswordQuality(null));						 					
			} catch (Exception ex) {
				LSLogger.exception(TAG, "CreateSnapshot error:", ex);
			}
			return jdata;
		}
	

}
