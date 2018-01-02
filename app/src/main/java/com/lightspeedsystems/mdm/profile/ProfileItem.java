package com.lightspeedsystems.mdm.profile;

import java.util.Date;
import java.util.List;
import java.util.Vector;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import com.lightspeedsystems.mdm.CommandResult;
import com.lightspeedsystems.mdm.Constants;
import com.lightspeedsystems.mdm.Utils;
import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Base abstract class for profile and policy classes. This class holds common values and provides common methods.
 */
public abstract class ProfileItem {
		private final static String TAG = "ProfileItem";
		protected int dbID;
		protected long timecreated;
		protected String name;
		protected String sprofile;
		protected JSONObject jprofile;
		protected JSONObject restorableJson; // is jprofile wrapped inside a type-name structure appropriate to the type.
		protected boolean bExists; // true if there is an existing reprentation of the profile (such as a wifi profile), false if not.
		protected boolean removeBeforeRestore=true; // true (default) is to remove the profile before restoring; false=restore first,then remove.
		protected boolean brestorable = true; // true to allow restoring if has restorable data, false to block restoring completely
		
		// values internally-generated based on the type:
		// type-specific values, depends on the implementing class:
		protected int prftype;
		protected int prfstate = PrfConstants.PROFILESTATE_managed; // state of the instance in the DB.
		protected String  jsonLabel;  // label for the outer json wrapper
		protected boolean jsonArray;  // true if the structure should be encased in an array.
		
		// instance-related attributes:
		protected JSONObject jdata;
		protected Context context;

	
		/**
		 * Constructor to be used from an implementing class; provides the basic class definition values, but no data.
		 * @param context application context
		 * @param profileType a PROFILETYPE value
		 * @param useJsonLabel String label used to name and identify this type of profile in a JSON of data; name:value.
		 * This also serves as the default name of the profile. For single-instance profiles, this would always be the
		 * same (such as restrictions and passcodes), but for multiple-instance profiles, the name should get set to
		 * the appropriate data's name.
		 * @param useJsonArray true if generated JSON should be enclosed within an array (ie, "[{...profile...}]"),
		 * or false to create JSON as a json object (ie, "{...profile...}")
		 */
		protected ProfileItem(Context context, int profileType, String useJsonLabel, boolean useJsonArray) {
			this.context = context;
			prftype   = profileType;
			jsonLabel = useJsonLabel;
			jsonArray = useJsonArray;
			name 	  = jsonLabel;  // set the default name to the json label.
			timecreated = new Date().getTime();
		}
		
		
		
		/** Constructor from a json string. */
		public static ProfileItem getInstance(Context context, int prftype, String prfname, JSONObject profile) {
			ProfileItem item = getInstanceForType(context, prftype);
			if (item != null) {			
				item.name = prfname;
				item.setProfileJSON(profile);
			}
			return item;
		}
		
		/** Constructor using DB values. */
		public static ProfileItem getInstance(Context context, int dbid, int prftype, int prfstate, long datetime, String prfname, String profile) {
			ProfileItem item = getInstanceForType(context, prftype);
			if (item != null) {			
				item.dbID = dbid;
				item.prftype = prftype;
				item.prfstate = prfstate;
				item.timecreated = datetime;
				item.name = prfname;
				item.setProfileString(profile);
			}
			return item;
		}

		public void setProfileString(String profile) {
			sprofile = profile;
			try {
				if (sprofile != null && sprofile.length()>2) 
					// parse json representation of the profile
					jprofile = new JSONObject(sprofile);
				else
					jprofile = new JSONObject();
			} catch (Exception ex) {
				LSLogger.exception(TAG, ex);
			}
		}
		
		// sets the json string in this profileitem. Note: do NOT extract the name value in here.
		// (this is intended to be used to save a json snapshot of some profile, whether it is the
		//  same one or one from a previous state snapshot.)
		public void setProfileJSON(JSONObject json) {
			jprofile = json;
			try {
				if (jprofile != null)
					sprofile = jprofile.toString();
				else
					sprofile = Constants.EMPTY_JSON;
			} catch (Exception ex) {
				LSLogger.exception(TAG, ex);
			}
		}
		
		/** Returns true of the profile is restorable; i.e., if there is non-empty json. */
		public boolean isRestorable() {
			boolean b = (brestorable && (jsonLabel != null && sprofile != null && !sprofile.equals(Constants.EMPTY_JSON)) );
			//LSLogger.debug(TAG, "IsRestorable="+b+": brestorable="+brestorable+" jsonlabel="+
			//		(jsonLabel==null?"null":jsonLabel)+" sprofile="+(sprofile==null?"null":sprofile));
			return b;
		}
		
		public void setRestorable(boolean allowrestore) {
			brestorable = allowrestore;
		}
		
		public void setContext(Context context) { this.context = context; }
		public Context getContext() { return this.context; }

		public int getDbID() { return dbID; }
		public void setDbID(int id) { dbID=id; }
		public int getType() { return prftype; }
		public int getState(){ return prfstate;}
		protected void setState(int newstate) { prfstate = newstate; }
		public long getCreateTime() { return timecreated; }
		public String getName() { return name; }
		public String getProfileStr() { return sprofile; }
		public JSONObject getProfileJSON() { return jprofile; }
		public boolean exists() { return bExists; }
		public void setExists(boolean state) { bExists = state; }
		public boolean isRemoveBeforeRestore() { return removeBeforeRestore; }
		
		/**
		 * Simple helper method for getting a single value from a JSON object; captures possible exceptions.
		 * @param tag identifier tag of the value to get
		 * @param jobj json object to search within for the tag name.
		 * @return String value if found, null otherwise. Logs exceptions if any.
		 */
		protected String extractFromJSON(String tag, JSONObject jobj) {
			String s = null;
			try {
				if (jobj != null && jobj.has(tag)) {
					s = jobj.getString(tag);
				}
			} catch (Exception ex) {
				LSLogger.exception(TAG, "ExtractFromJSON error:", ex);
			}
			return s;
		}
		
		/**
		 * Gets a persistable string representation of the profile that is to be stored in the database,
		 * as the value of the instance. This should represent previous values if the data can be undone,
		 * or empty json {} if there is nothing to undo.
		 * Implementations can override as needed.
		 * @return the string representation of the json to be saved with the profile. By default, this is the
		 * string that was used to create the profile in the first place. 
		 */
		public String getPersistableProfileStr() {
			String s = sprofile;
			if (s==null || s.length() == 0)
				s = Constants.EMPTY_JSON;
			return s;
		}
		
		/** get the josn for a restorable profile; consists of a json label and the json data wrapped together. */
		public JSONObject getProfileRestorableJSON() {
			if (restorableJson == null && jsonLabel != null && jprofile != null && jprofile.length()>0) {
				// build the json needed, consisting of the wrapper and enclosing json object as needed:
				try {
					JSONObject jobj = new JSONObject();
					if (jsonArray) { // enclose the json in an array, and add it:
						JSONArray jarry = new JSONArray();
						jarry.put(jprofile);
						jobj.put(jsonLabel, jarry);
					} else { // add the existing json directly
						jobj.put(jsonLabel, jprofile);
					}
					restorableJson = jobj;
				} catch (Exception ex) {
					LSLogger.exception(TAG, "GetProfileRestorableJSON error:", ex);
				}
			}
			return restorableJson;
		}
		
		private static ProfileItem getInstanceForType(Context context, int profileType) {
			ProfileItem item = null;
			switch (profileType) {
			case PrfConstants.PROFILETYPE_restrictions:
				item = new DeviceRestrictions(context, null);
				break;
			case PrfConstants.PROFILETYPE_passcode:
				item = new PasscodePolicy(context, null);
				break;
			case PrfConstants.PROFILETYPE_wifi:
				item = new WifiPolicy(context, null);
				break;
			case PrfConstants.PROFILETYPE_email:
	///			item = new EmailProfile(context, null);
				break;
			case PrfConstants.PROFILETYPE_exchange:
	///			item = new ExchangeProfile(context, null);
				break;
			case PrfConstants.PROFILETYPE_card_dav:
				break;
			case PrfConstants.PROFILETYPE_cal_dav:
				break;
			case PrfConstants.PROFILETYPE_calendar:
				break;
			case PrfConstants.PROFILETYPE_webclips:
				break;
			}
			return item;
		}
		
		// sets some internal values relevant to the profile type
		/*
		private void setValuesForType() {
			jsonArray = true;
			switch (prftype) {
				case PrfConstants.PROFILETYPE_restrictions:
					jsonLabel = PrfConstants.PAYLOADTYPE_Restrictions;
					jsonArray = false;
					break;
				case PrfConstants.PROFILETYPE_passcode:
					jsonLabel = PrfConstants.PAYLOADTYPE_PasscodePolicy;
					jsonArray = false;
					break;
				case PrfConstants.PROFILETYPE_wifi:
					jsonLabel = PrfConstants.PAYLOADTYPE_Wifi;
					break;
				case PrfConstants.PROFILETYPE_email:
					jsonLabel = PrfConstants.PAYLOADTYPE_Email;
					break;
				case PrfConstants.PROFILETYPE_exchange:
					jsonLabel = PrfConstants.PAYLOADTYPE_Exchange;
					break;
				case PrfConstants.PROFILETYPE_card_dav:
					jsonLabel = PrfConstants.PAYLOADTYPE_Card_dav;
					break;
				case PrfConstants.PROFILETYPE_cal_dav:
					jsonLabel = PrfConstants.PAYLOADTYPE_Cal_dav;
					break;
				case PrfConstants.PROFILETYPE_calendar:
					jsonLabel = PrfConstants.PAYLOADTYPE_Calendar;
					break;
			}
		}
		*/
		
		
		/**
		 * Gets a string representation of an instance, including various values.
		 */
		public String toString() {
			return ((name==null?"(null)":name)+" - ID:"+dbID+";Type:"+prftype+";State:"+prfstate+";"+super.toString());
		}
		
		
		/**
		 * Builds a list of ProfileItem implementation instances from the given command profile JSON structure.
		 * Refer to @Profiles class documentation for expected formats. 
		 * @param context application context
		 * @param jsonData JSON data containing the entire profile/policy structure
		 * @param cmdResult CommandResult instance that is to receive errors and result status.
		 * @return
		 */
		public static List<ProfileItem> getProfileItemsFromCommand(Context context, JSONObject jsonData, CommandResult cmdResult) {
			Vector<ProfileItem> list = new Vector<ProfileItem>();

			if (jsonData != null && jsonData.length()>0) {
			  try {			
				LSLogger.debug(TAG, "GetProfileItemsFromCommand with data: " + Utils.filterProtectedContent(jsonData.toString(2)) );
				
				// get the specific values and create ProfileItem implementation instances of them:
				// (we use exception handling in each section so that other things can be applied.)
				
				// Process restrictions:
				if (jsonData.has(PrfConstants.PAYLOADTYPE_Restrictions))  {
					try {
						JSONObject profileItem = new JSONObject(jsonData.getString(PrfConstants.PAYLOADTYPE_Restrictions));
						if (profileItem != null && profileItem.length()>0) {
							DeviceRestrictions dr = new DeviceRestrictions(context, profileItem);
							list.add(dr);
						}
					} catch (Exception ex2) {
						LSLogger.exception(TAG, "GetProfileItemsFromCommand Restrictions exception:", ex2);
						cmdResult.setException(ex2);
					}
				}

				  // Process passcode policy settings:
				  if (jsonData.has(PrfConstants.PAYLOADTYPE_PasscodePolicy))  {
					  try {
						  JSONObject profileItem = new JSONObject(jsonData.getString(PrfConstants.PAYLOADTYPE_PasscodePolicy));
						  if (profileItem != null) {
							  PasscodePolicy pp = new PasscodePolicy(context, profileItem);
							  list.add(pp);
						  }
					  } catch (Exception ex2) {
						  LSLogger.exception(TAG, "GetProfileItemsFromCommand PasscodePolicy exception:", ex2);
						  cmdResult.setException(ex2);
					  }
				  }
				  // Process webclips policy settings:
				  if (jsonData.has(PrfConstants.PAYLOADTYPE_WebClips))  {
					  try {
						  JSONArray profileItem = new JSONArray(jsonData.getString(PrfConstants.PAYLOADTYPE_WebClips));
						  if (profileItem != null && profileItem.length()>0) {
							  WebClipsPolicy pp = new WebClipsPolicy(context, profileItem);
							  list.add(pp);
						  }
					  } catch (Exception ex2) {
						  LSLogger.exception(TAG, "GetProfileItemsFromCommand WebClipsPolicy exception:", ex2);
						  cmdResult.setException(ex2);
					  }
				  }

				  // Process app_launcher policy settings:
				  if (jsonData.has(PrfConstants.PAYLOADTYPE_AppLauncher))  {
					  try {

						  JSONObject profileItem = jsonData.getJSONObject(PrfConstants.PAYLOADTYPE_AppLauncher);
						  LSLogger.debug(TAG, "GetProfileItemsFromCommand app launcher: " + "profile set to "+ profileItem.toString());
						  if (profileItem != null && profileItem.length()>0) {
							  AppLauncherPolicy pp = new AppLauncherPolicy(context, profileItem);
							  list.add(pp);
						  }
					  } catch (Exception ex2) {
						  LSLogger.exception(TAG, "GetProfileItemsFromCommand AppLauncherPolicy exception:", ex2);
						  cmdResult.setException(ex2);
					  }
				  }

				  // Process wifi policy settings:
				// We expect to receive an array of wifi profiles, such as:
				// { ... "wifi":[ {"ssid_str":"network1", ...},{"ssid_str":"network2",...} ], .... }
				if (jsonData.has(PrfConstants.PAYLOADTYPE_Wifi))  {
					try {
						JSONArray profiles = new JSONArray(jsonData.getString(PrfConstants.PAYLOADTYPE_Wifi));
						if (profiles != null && profiles.length() > 0) {
							// process each profile item by creating an instance for it and adding it to the collection:
							for (int i=0; i<profiles.length(); i++) {
								JSONObject profileItem = profiles.getJSONObject(i);
								if (profileItem != null && profileItem.length()>0) {
									WifiPolicy wfp = new WifiPolicy(context, profileItem);
									list.add(wfp);
								}							
							}
						} 
					} catch (Exception ex2) {
						LSLogger.exception(TAG, "GetProfileItemsFromCommand WifiPolicy exception:", ex2);
						cmdResult.setException(ex2);
					}
				}
				

				// other possible payload types that are not yet supported:
				
				// Process email policy settings:
				if (jsonData.has(PrfConstants.PAYLOADTYPE_Email))  {
					try {
						JSONArray profiles = new JSONArray(jsonData.getString(PrfConstants.PAYLOADTYPE_Email));
						if (profiles != null && profiles.length() > 0) {
							// process each profile item by creating an instance for it and adding it to the collection:
					LSLogger.warn(TAG, "EMail Policies not supported in this version.");
					/** -- commented out for EmailProfile implementation that is deferred. (MZ 9/13)
							for (int i=0; i<profiles.length(); i++) {
								JSONObject profileItem = profiles.getJSONObject(i);
								if (profileItem != null && profileItem.length()>0) {
									EmailProfile emp = new EmailProfile(context, profileItem);
									list.add(emp);
								}							
							}
					**/
						}
					} catch (Exception ex2) {
						LSLogger.exception(TAG, "GetProfileItemsFromCommand EmailPolicy exception:", ex2);
						cmdResult.setException(ex2);
					}
				}
				
				// Process exchange policy settings:
				if (jsonData.has(PrfConstants.PAYLOADTYPE_Exchange))  {
					try {
						JSONArray profiles = new JSONArray(jsonData.getString(PrfConstants.PAYLOADTYPE_Exchange));
						if (profiles != null && profiles.length() > 0) {
							// process each profile item by creating an instance for it and adding it to the collection:
					LSLogger.warn(TAG, "Exchange Policies not supported in this version.");
					/** -- commented out for ExchangeProfile implementation that is deferred. (MZ 9/13)
							for (int i=0; i<profiles.length(); i++) {
								JSONObject profileItem = profiles.getJSONObject(i);
								if (profileItem != null && profileItem.length()>0) {
									ExchangeProfile exp = new ExchangeProfile(context, profileItem);
									list.add(exp);
								}							
							}
					**/
						} 

					} catch (Exception ex2) {
						LSLogger.exception(TAG, "GetProfileItemsFromCommand ExchangePolicy exception:", ex2);
						cmdResult.setException(ex2);
					}
				}
								
				// Process calendar policy settings:
				//if (jsonData.has(PrfConstants.PAYLOADTYPE_Calendar))  {
				
				// Process card policy settings:
				//if (jsonData.has(PrfConstants.PAYLOADTYPE_Card_dav))  {
				
				// Process cal_dav policy settings:
				//if (jsonData.has(PrfConstants.PAYLOADTYPE_cal_dav))  {
				
			  } catch (Exception ex) {
				LSLogger.exception(TAG, "GetProfileItemsFromCommand error:", ex);  
			  }
			}
			
			return list;
		}
		
		// ------------------
		// abstract methods:
		// ------------------
		
		/** 
		 * Abstract method: applies the current profile settings as defined in the json data used during instance creation.
		 * @param cmdResult details of the processing, primarily for capturing errors. 
		 * Can be null if detailed results are not needed.
		 * @return true if the operation succeeded, false if errors occurred, with details in cmdResult.
		 */
		public abstract boolean applyProfile(CommandResult cmdResult);


		/**
		 * Abstract method for restoring previously-saved profile values, overwriting current values
		 *  with those values stored in here in the json data, if any.
		 * @param cmdResult details of the processing, primarily for capturing errors. 
		 * Can be null if detailed results are not needed.
		 * @return true if the replacement succeeded, false if errors occurred, with details in cmdResult.
		 */
		public abstract boolean restoreProfile(CommandResult cmdResult);
		
		/**
		 * Abstract method for removing profile values, thereby removing the settings applicable to it.
		 * @param cmdResult details of the processing, primarily for capturing errors. 
		 * Can be null if detailed results are not needed.
		 * @return true if the removal succeeded, false if errors occurred, with details in cmdResult.
		 */
		public abstract boolean removeProfile(CommandResult cmdResult);

		
	}
	

