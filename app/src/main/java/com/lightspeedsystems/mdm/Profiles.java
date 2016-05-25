
package com.lightspeedsystems.mdm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import com.lightspeedsystems.mdm.profile.ProfileItem;
import com.lightspeedsystems.mdm.util.LSLogger;
import org.json.JSONObject;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 *  Provides Profile package support, which for Android, is a set of settings that are to be applied to the device,
 *  typically specific to security and policy related items.
 *  
 *  Generally, an instance of this class should be created every time a set of profiles is to be applied; i.e., when
 *  a set_profile command is being processed, a collection of various profiles is being set; each time such a command
 *  is received, it should create and use a new instance of this, as it reads from the lsprofiles database table 
 *  upon creation, to get the current lisrt of managed profiles.
 *  
 *  A profile is typically constructed of one or more "payload" sections, each of which defines characteristics
 *  for a specific "PayloadType". The implementation in this class constructs a separate inner class for each
 *  payload type, thereby keeping the data and functionality structured to mirror that of profile payload contents.
 *(Note: the current implementation assumes all payload sections are received at the same time, and those sections
 * with no data indicate that no managed profile(s) exist for that section, and any previously-defined profiles of
 * those types are to be removed.)
 *  
 *  The expected structure follows this JSON structure, consisting of a name-type, followed by an array [] or the contents:
 *  { "card_dav": [],
 *    "wifi": [],
 *    "restrictions": {},
 *    "passcode_policy": {},
 *    "email": [],
 *    "cal_dav": [],
 *    "calendar_subscription": [],
 *    "exchange": [] }
 *  
 *  Example with data: 
 *  { 
 *   "card_dav": [],
 *   "wifi": [
 *      {
 *       "proxy_type": "Manual",
 *       "proxy_password": "",
 *       "ssid_str": "test8proxyandwpa",
 *       "payload_version": 1,
 *       "proxy_user_name": "",
 *       "payload_organization": "Lightspeed Systems",
 *       "hidden_network": false,
 *       "payload_type": "com.apple.wifi.managed",
 *       "auto_join": true,
 *       "password": "12345678",
 *       "payload_identifier": "com.lsmdm.payload.8df26861-3ed2-4fca-ae38-33393566a418",
 *       "enable_ipv6": true,
 *       "proxy_pac_url": "",
 *       "payload_uuid": "8df26861-3ed2-4fca-ae38-33393566a418",
 *       "proxy_server_port": 443,
 *       "proxy_server": "new.myprozxy.mdmls.com",
 *       "encryption_type": "WPA",
 *       "payload_display_name": "Wi-Fi"
 *     }
 *   ],
 *   "restrictions": {
 *     "force_encrypted_storage": false,
 *     "allow_camera": true
 *   },
 *   "passcode_policy": {
 *     "max_inactivity": 600,
 *     "passcode_quality": "none",
 *     "min_non_letter_chars": 0,
 *     "max_failed_attempts": 10,
 *     "min_length": 5,
 *     "min_lower_case_chars": 0,
 *     "min_upper_case_chars": 0,
 *     "max_pin_age_in_days": 0,
 *     "min_complex_chars": 0,
 *     "min_numeric_chars": 0,
 *     "min_letters": 0,
 *     "pin_history": 0
 *   },
 *   "email": [],
 *   "cal_dav": [],
 *   "calendar_subscription": [],
 *   "exchange": [] 
 *  }
 *  
 * - the top level consists of name-value pairs in a standard JSONObject.
 * - items are either a JSONObject or a JSONArray, consisting of a single or collection of JSONObjects, 
 * each of which is of a specific payload type and profile definition.
 * 
 * Empty payloads, such as "email":"{}", indicate that no profile for that type of data is used, and as
 * such, any previously-defined values are to be undone and removed.
 * 
 * For processing, we extract the PayloadContent value as an array, and iterate through each of those
 *  and instantiate the proper inner-class implementation for the type.
 */
public class Profiles {
	private final static String TAG = "Profiles";
	
	private Context context;
	private Vector<ProfileItem> profiles;	// list of profileitems, created from the contents of the database
	
	/**
	 * Creates Profiles instance
	 * @param ctx context
	 */
	public Profiles(Context ctx) {
		context = ctx;
		profiles = dbGetProfileItems(0); // get the managed profile items
	}
	
	// internal method; adds the profile to the local profiles list and to the database.
	// returns true if the profile was added to the database; only adds to the list if the db add was ok.
	private boolean addProfile(ProfileItem profile) {
		boolean added = dbAddProfileItem(profile);
		if (added)
			profiles.add(profile);
		return added;
	}
	
	// internal method; removes the profile from the local profiles list and from the database.
	private void removeProfile(ProfileItem profile) {
		dbDeleteProfileItem(profile);
		if (!profiles.remove(profile))
			LSLogger.warn(TAG, "Profile " + profile.getName() + " was NOT REMOVED from Profiles list.");
	}

	// internal method; finds a profile by name and profile type, searching in the local profiles list.
	private ProfileItem findProfileByName(String name, int profileType) {
		return findProfileByName(profiles, name, profileType);
	}

	// internal method; finds a profile by name and type from the given list of profileitem instances.
	private ProfileItem findProfileByName(List<ProfileItem>items, String name, int profileType) {
		ProfileItem profile = null;
		String prfname;
		if (items != null && name != null && profileType != 0) {
			Iterator<ProfileItem> iter = items.iterator();
			if (iter != null) {
				while (iter.hasNext()) {
					ProfileItem item = iter.next();
					if (item.getType() == profileType) {
						prfname = item.getName();
						if (prfname != null && prfname.equalsIgnoreCase(name)) {
							profile = item;
							break;
						}
					}
				}
			}
		}
		return profile;
	}
	
	
	/**
	 * Sets the device 'profile' settings based on the JSON-formatted profile object passed in.
	 * @param jsonData Profile values; assumes this is not NULL.
	 * @param mode operating mode. 0=invoked externally, such as from command processing; non-0
	 *  is an internal-invocation such as in restoring a previously-saved profile.
	 * @return CommandResult instance with results of the processing.
	 */
	public CommandResult setProfile(JSONObject jsonData) {
		boolean bSuccess;
		CommandResult cmdResult = new CommandResult();
		cmdResult.setSuccess(true);

		// first, parse out the sections and create instances of ProfileItems into a list;
		// these contain profile/policy settings we'll apply from this command
		// (empty sections are excluded)
		List<ProfileItem> cmdProfiles = ProfileItem.getProfileItemsFromCommand(context, jsonData, cmdResult);
		
		// iterate through the list of profiles to process, calling each one's methid to apply itself
		if (cmdProfiles != null) {
			Iterator<ProfileItem> iter = cmdProfiles.iterator();
			if (iter != null) {
				while (iter.hasNext()) {
					ProfileItem profile = iter.next();
					bSuccess = profile.applyProfile(cmdResult);
					// if the profile was applied, see if it exists 
					if (bSuccess && (findProfileByName(profile.getName(), profile.getType())==null)) {
						profile.setExists(false);
						bSuccess = addProfile(profile);						
						LSLogger.debug(TAG, "Applied and added profile: "+ profile);
					} else if (bSuccess) {
						LSLogger.debug(TAG, "Applied updated (existing) profile: "+ profile);
					} else {
						LSLogger.debug(TAG, "Failed to apply profile: "+ profile);
					}
				}
			}
		}
		
		// now go through and see if we need to delete and/or restore any previous values:
		// - make a copy of the master profiles list, and iterate through it. The cmdProfiles list
		//  that was created here has all the current profile settings that should still be applied;
		//  and, the items in the profiles list contain both previously-set profile items and any new
		//  items added from the above processing. As a result, any profile item that is in the master
		//  profiles list but is not longer in the cmdProfiles list, that is a profile that has been
		//  removed by the administrator, and as such, should be undone and deleted from the master
		//  list of profiles and from the profiles database. 
		// This process below gets each profile in the master list, looks for it by name in the
		//  cmdProcess list, and anything that is not in both lists is removed.
		// (We have to use a clone if we alter the actual collection while iterating through it.)
		@SuppressWarnings("unchecked")
		Vector<ProfileItem> profilesClone = (Vector<ProfileItem>) profiles.clone();
		Iterator<ProfileItem> iter = profilesClone.iterator();
		if (iter != null) {
			while (iter.hasNext()) {
				ProfileItem profile = iter.next();
				try {
				  // search for the profile by its name and type:
				  if (findProfileByName(cmdProfiles, profile.getName(), profile.getType()) == null) {
					// profile is in master list but is not in cmd list; this profile is to be deleted/removed.
					// A profile that has been applied over existing settings may be able to restore those
					//  prior settings; if such values can be restored, do so:
					LSLogger.debug(TAG, "Removing profile: " + profile.getName());
					
					if (profile.isRemoveBeforeRestore()) {  // remove first, then restore:
						// first, remove the profile from the system:  (undo settings, delete the object, etc.) 
						bSuccess = profile.removeProfile(cmdResult);							
						if (bSuccess && profile.isRestorable()) {
							bSuccess = profile.restoreProfile(cmdResult);
							LSLogger.debug(TAG, "Restored profile (remove before restore); success="+bSuccess+" profile:" + profile.getName());//)+" restored_to:"+profile.getProfileRestorableJSON().toString());
						}
						
					} else { // restore first, then remove:
						if (profile.isRestorable()) {
							bSuccess = profile.restoreProfile(cmdResult);
							LSLogger.debug(TAG, "Restored profile (remove after restore); success="+bSuccess+" profile:" + profile.getName());//+" restored_to:"+profile.getProfileRestorableJSON().toString());
						} else {
							bSuccess=true;
						}
						if (bSuccess) {
							// now, remove the profile from the system:  (undo settings, delete the object, etc.) 
							bSuccess = profile.removeProfile(cmdResult);	
						}
					}
					// lastly, remove the profile from the DB and profiles list:
					if (bSuccess)
						removeProfile(profile);
					if (LSLogger.isLoggingEnabled()) {
						if (bSuccess)
							LSLogger.debug(TAG, "Removed profile: " + profile);
						else
							LSLogger.error(TAG, "Failed to remove profile: "+ profile+
									" error:"+cmdResult.getErrorMessage()==null?"(null)":cmdResult.getErrorMessage());
					}
				  }
				} catch (Exception ex) {
					LSLogger.exception(TAG, "SetProfile error on removing/restoring profile:",ex);
				}
								
			} // end while
		}
		
		return cmdResult;
	}


	
	/**
	 * Parses an XML profile into its supported components and values.
	 * @param xml
	 */
	/**
	public JSONObject parseXmlProfile(String xml, CommandResult cmdResult) {
		JSONObject jo = null;
		try {
			jo = XML.toJSONObject(xml);
		} catch (Exception ex) {
			if (cmdResult != null)
				cmdResult.setException(ex);
		}
		return jo;
	}
	**/
	
	
	/**
	 * Logs the applied profile change. 
	 * (The change could be stored to a database for later recall, or just ignored. Current implementation
	 * just logs the change and does not store it.)
	 * 
	 * @param timeApplied The time the change was applied. All changed within a given profile category use
	 * this same value, so that they can be grouped together to recall when they were applied as a group.
	 * @param category Profile categorical description
	 * @param name Name of the value that was changed,  typically a PAYLOADVALUE_ string.
	 * @param newValue value that was applied and set
	 * @param prevValue previous value that was replaced; may be the same as the new value.
	 */
	public static void logProfileChange(long timeApplied, String category, String name, String newValue, String prevValue) {
		if (newValue == null) newValue = "";
		if (prevValue==null) prevValue = "";
		
		LSLogger.info(TAG, "Applied profile value " + category+"."+name+"="+newValue+ " (prev="+prevValue+")");		
	}

	
	// --------------------------------------------------------------------------------
	// Database-related routines:
	// --------------------------------------------------------------------------------
	
	/**
	 * Gets a list of current profile items from the database, either all of them or only for a specific type.
	 * @param profileType 
	 * @return a list of ProfileItems, or an empty list if error or none found in the DB.
	 */
	private Vector<ProfileItem> dbGetProfileItems(int profileType) {
		Vector<ProfileItem> items = new Vector<ProfileItem>();
		ProfilesDB db = null;
		try {
			db = new ProfilesDB(context);
			db.readProfileRecords(items, 0);			
		} catch (Exception ex) {
			LSLogger.exception(TAG, "Error reading Profiles from DB:", ex);
		} finally {
			if (db != null)
				db.closeDB(); // use of closeDB handles any possible close() exceptions.
		}
		return items;
	}
	
	/**
	 * Adds the ProfileItem to the database.
	 * @param profile ProfileItem to add.
	 * @return true if the item was added, false if not.
	 */
	private boolean dbAddProfileItem(ProfileItem profile) {
		boolean bAdded = false;
		ProfilesDB db = null;
		try {
			db = new ProfilesDB(context);
			// a successfully-saved profile will have its dbID set to non-0:
			if (db.insert(profile) > 0) {
				bAdded = true;
				//LSLogger.debug(TAG, "Profile saved to DB: "+ profile.toString());
			//} else {
				//LSLogger.error(TAG, "Failed to save Profile to DB: "+profile.toString());
			}
			
		} catch (Exception ex) {
			LSLogger.exception(TAG, "Profile Add to DB error:", ex);
		} finally {
			if (db != null)
				db.closeDB();
		}
		return bAdded;
	}

	/**
	 * Deletes the ProfileItem from the database.
	 * @param profile ProfileItem to delete.
	 */
	private void dbDeleteProfileItem(ProfileItem profile) {
		ProfilesDB db = null;
		try {
			db = new ProfilesDB(context);
			db.deleteProfile(profile);			
		} catch (Exception ex) {
			LSLogger.exception(TAG, "Profile Delete from DB error:", ex);
		} finally {
			if (db != null)
				db.closeDB(); // use of closeDB handles any exception.
		}
	}

	
	/**
	 * Static method to get the SQL used to create the database table for managed profiles.
	 * @return the SQL string used to create the table.
	 */
    public static String getProfilesSqlCreateTable() {
    	return ProfilesDB.getSqlCreateTable();
    }

	
	// -------------------------------------------------------------------------
	// --- Inner-class for handling Profile persistence in a sqlite database  --
	// -------------------------------------------------------------------------
	/**
     * A SQLite database is used to store Profiles in the lsprofiles table.
     * This is mainly used to capture initial state of the device before a profile
     * has been applied to first time, so that the device can be reverted back to
     * that state if a profile is removed. But, the exact use may differ depending 
     * on the needs of what occurs when removing a profile. For example, a passcode
     * profile can simply be undone by applying previously-saved values representing
     * the original values read from the system before any profile was applied.
     * But for wifi, removal would likely mean just removing the added or changed values
     * that were set as a result of applying a wifi profile (wifi profiles usually add
     * connections and related information while leaving any existing predefined connections
     * intact and as-is, unless they have the same name); removing a wifi profile would 
     * therefore remove the added wifi access connections while leaving previous connections
     * intact.
     * The purpose of this table is to only capture initial (and optionally, modified) states
     * of the device or that of specific profiles.
	 * 
	 * Rows are added for each profile entry, per profile type. 
	 * The entry (each row) contains json representing the values supported, all in the profile.
	 * Each type of profile has its own row (ie, passcode, wifi, etc).
	 * 
	 * Each row consists of: 
	 * id - db id of the row - is saved in Settings for quick-reference to a specific row 
	 *   for the specific type
	 * prftype - defines the type of profile this applies to (passcode, wifi, etc)
	 * prfstate - defines the state or usage of this entry (initial, delta, etc.);
	 * name - optional name or identifier of the profile; may be needed for named-profiles
	 *      in future implementations
	 * datetime - when the entry was created
	 * profile - contents of the profile as a JSON string.
	 */
	private static class ProfilesDB extends DBStorage {
		private final static String TAG = "ProfilesDB";
		private final static String PROFILE_DB_TABLENAME = "lsprofiles";
		private final static String COLUMN_ID     	= "id";
		private final static String COLUMN_TYPE   	= "prftype";
		private final static String COLUMN_STATE 	= "prfstate";
		private final static String COLUMN_DATETIME = "datetime";
		private final static String COLUMN_NAME 	= "name";
		private final static String COLUMN_PROFILE  = "profile"; 
				
		// SQL command used to create the log entries table:
		private final static String PROFILE_DB_CREATE_SQL =  
                "CREATE TABLE " +PROFILE_DB_TABLENAME + " (" +
                		COLUMN_ID       + " INTEGER PRIMARY KEY, " +
                		COLUMN_TYPE     + " INTEGER, " +
                		COLUMN_STATE    + " INTEGER, " +
                   		COLUMN_DATETIME + " LONG, " +
                		COLUMN_NAME     + " TEXT, " +
                		COLUMN_PROFILE  + " TEXT);";
		// index locations of the data in the query, for improved processing speeds:
		private final static int COLUMN_ID_INDEX     = 0;
		private final static int COLUMN_TYPE_INDEX   = 1;
		private final static int COLUMN_STATE_INDEX  = 2;
		private final static int COLUMN_DATETIME_INDEX = 3;
		private final static int COLUMN_NAME_INDEX = 4;
		private final static int COLUMN_PROFILE_INDEX = 5;

		// SQL command to read all rows from the database
		private final static String PROFILE_DB_QUERY_GETALL =
				"select * from " + PROFILE_DB_TABLENAME+";";
		// SQL to get rows for a specific type; get entries in ascending datetime order
		private final static String PROFILE_DB_QUERY_GETTYPE =
				"select * from " + PROFILE_DB_TABLENAME + " where " + COLUMN_TYPE + "=? order by " + COLUMN_DATETIME + " asc;";
		  
		
		/**
		 * Creates Database persistence instance, where data will be stored.
		 * @param context
		 */
		public ProfilesDB(Context context) {
	        super(context);
	        setLoggingPersistence(false);
		}

		/**
		 * Abstract method to get the name of the database table.
		 * @return
		 */
	    public String getSqlTableName() {
	    	return PROFILE_DB_TABLENAME;
	    }

		/**
		 * Static method to get the SQL used to create the Events database table.
		 * @return the SQL string used to create the table.
		 */
	    public static String getSqlCreateTable() {
	    	return PROFILE_DB_CREATE_SQL;
	    }

	    /**
	     * Builds a map for all columns that may be requested, which will be given to the 
	     * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include 
	     * all columns, even if the value is the key. This allows the ContentProvider to request
	     * columns w/o the need to know real column names and create the alias itself.
	     */
	@SuppressWarnings("unused")
		private HashMap<String,String> buildColumnMap() {
	        HashMap<String,String> map = new HashMap<String,String>();
	        map.put(COLUMN_ID,    COLUMN_ID);
	        map.put(COLUMN_TYPE,  COLUMN_TYPE);
	        map.put(COLUMN_STATE, COLUMN_STATE);
	        map.put(COLUMN_DATETIME,COLUMN_DATETIME);
	        map.put(COLUMN_NAME,    COLUMN_NAME);
	        map.put(COLUMN_PROFILE, COLUMN_PROFILE);
	        return map;
	    }
	    
	    // -- data management methods --
	    
	    protected void closeDB() {
	    	super.closeDB();
	    }
	    
	    /**
	     * Gets a list of profile items from the db
	     * @param list output list to store items into; must not be null
	     * @param profileType - 0 to get all items, or a type value to get item of that type,
	     *   returned in the list in date-ascending order (oldest-first).
	     * @return count of items read, 0 for none, -1 if error.
	     */
	    protected long readProfileRecords(Vector<ProfileItem> list, int profileType) {
	    	int result = -1;
	        Cursor cursor = null;
	    	try {
	    		SQLiteDatabase db = openForReading();
	    		String sql = PROFILE_DB_QUERY_GETALL;
	    		String[] args = null;
	    		if (profileType != 0) { // query items for a specific type;
	    			sql = PROFILE_DB_QUERY_GETTYPE;
	    			args = new String[] { Integer.toString(profileType) };
	    		}
	    		//debug:
	    		LSLogger.debug(TAG, "SQL=" + sql);
	    		LSLogger.debug(TAG, "args=" + ((args==null)?"null":args.toString()));
	    		
	    		cursor = db.rawQuery(sql, args);
	    		
	    		if (cursor != null && cursor.moveToFirst()) {
	    			result = 0;
	                do {
	                    ProfileItem item = ProfileItem.getInstance(context, 
	                    		cursor.getInt(COLUMN_ID_INDEX),
	                    		cursor.getInt(COLUMN_TYPE_INDEX),
	                    		cursor.getInt(COLUMN_STATE_INDEX),
	                    		cursor.getLong(COLUMN_DATETIME_INDEX),
	                    		cursor.getString(COLUMN_NAME_INDEX),
	                    		cursor.getString(COLUMN_PROFILE_INDEX));
	                    		 
	                    list.add(item);
	                    result++;
	                } while (cursor.moveToNext());
	    		}
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Reading DB", ex, false);
	    		result = -1;
	    	} finally {
	    		if (cursor != null && !cursor.isClosed())
	    			cursor.close();
	    	}
	    	return result;
	    }
	    
	    /**
	     * Adds the Event instance's data to the database.
	     * @param item
	     * @return the row ID of the inserted row, or -1 if error.
	     */
	    protected long insert(ProfileItem item) {
	    	ContentValues mapping  = new ContentValues(5);
	    	mapping.put(COLUMN_TYPE, 	 Integer.valueOf(item.getType()));
	    	mapping.put(COLUMN_STATE,    Integer.valueOf(item.getState()));
	    	mapping.put(COLUMN_DATETIME, Long.valueOf(item.getCreateTime()));
	    	mapping.put(COLUMN_NAME, 	 item.getName());
	    	mapping.put(COLUMN_PROFILE,  item.getPersistableProfileStr());
	    	int id = (int) super.insertRow(PROFILE_DB_TABLENAME, mapping);
	    	if (id > 0)
	    		item.setDbID(id);
	    	//LSLogger.debug(TAG, "ADDED to DATABASE profile name="+item.getName()+" persistableprofile="+item.getPersistableProfileStr());
	    	return (long)id;
	    }
	    
	    /**
	     * Deletes a specific profile entry from the database. 
	     * Uses the dbID to identify the item to delete.
	     */
	    protected void deleteProfile(ProfileItem item) {
	    	try {
		    	String args[] = new String[] { Integer.toString(item.getDbID()) };
		    	String whereClause = COLUMN_ID + "=?";
	    		SQLiteDatabase db = openForWriting();
	    		int count = db.delete(PROFILE_DB_TABLENAME, whereClause, args);
	    		LSLogger.debug(TAG, "Delete of profile " + item.getName() + " == "+count+" (1=success,0=failed)");
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Deleting Profile from DB: ", ex);
	    	}
	    }
	    
	    /**
	     * Deletes all entries from the database.
	     */
	    /** - not used at this time -
	    protected void deleteAll() {
	    	try {
	    		SQLiteDatabase db = openForWriting();
	    		db.delete(PROFILE_DB_TABLENAME, null, null);
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Deleting all Profiles from DB: ", ex);
	    	}
	    }
	    */
	    
	}

	
	
}
