package com.lightspeedsystems.mdm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import com.lightspeedsystems.mdm.util.LSLogger;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Provides non-managed apps features, including obtaining all the apps installed on the device 
 * and maintaining a list of attributes about them in a sqlite database table.
 * 
 * This class is a subclass of Apps, and uses some of the same methods, but provides its own
 * database persistence class (DeviceAppsDB) to store app data in a deviceapps table.
 * 
 * The general purpose of this class is to:
 * - store a list of the installed apps in a database, so that we know which apps were in place since we last checked.
 * - get the list of installed apps currently on the device.
 * - compare the list of current apps on the device to the list of apps saved in the database, and
 *  generate a delta/difference of apps info that can be sent to the MDM server.
 *  - update changes in the database to reflect the last-reported changes that were sent to the MDM server.
 */
public class DeviceApps extends Apps {
	private static String TAG = "DeviceApps";

	// note: the apps collection is in the base class, and holds the list of all the current apps found on the device
	private Vector<App> devicedbapps; // list holding contents of the deviceapps database entries.
 
	
	public DeviceApps(Context context) {
		super(context);
		devicedbapps = new Vector<App>(10);
	}
	
	
	/**
	 * Gets the JSON-formatted set of data for device apps. 
	 * Either gets all apps or a delta since the last time called.
	 * 
	 * The json produced is formatted as follows:
	 * Top-level data:
	 *  {"initial":"true", "apps":[ {app1}, {app2} ] }
	 *  
	 *  each app contains the following values:
	 *  {"status":"add" or "update" or "remove", // indicates the status of this record. only present in deltas.
	 *  "device_udid":"0f56ac7640e",  // udid of the device
	 *  "identifier":"package name",  // (such as "com.lightspeedsystems.mdm")
 	 *  "name":"application display name", // (such as "Mobile Manager")
 	 * "version":"full version string",                                 // [string]
 	 * "short_version":"short version",                                 // [string]
 	 * "bundle_size": "size in bytes of the installation package file", // [sent as int, so cant be bytes]
 	 * "dynamic_size":"size occupied on the system"                     // [sent as int, so cant be bytes]
 	 * "managed":"true" or "false" indicating if the app is in the device's list of managed apps
	 * }
	 * 
	 * @param bAll if true, gets all apps installed on the device. if false, gets a delta since the last delta query.
	 * @return JSONObject, may be empty if no delta exists, or will have content if all apps or a delta is present.
	 */
	public JSONObject getDeviceAppsJSON(boolean bAll) {
		JSONObject json = new JSONObject();
		List<App> list = null;
		Apps managedApps = Controller.getInstance(context).getAppsInstance();
		boolean bManaged = false;
		
		if (bAll) {
			apps.clear();
			list = getAllInstalledApps();
		} else {
			list = getDeltaApps();
		}
		//take the appropriate list and convert to json
		if (list != null && list.size() > 0) {
			// create array of apps:
			String deviceudid = Settings.getInstance(context).getDeviceUdid();
			JSONArray jarray = new JSONArray();
			Iterator<App> iter = list.iterator();
			while (iter.hasNext()) {
				App app = iter.next();
				try {
					JSONObject japp = new JSONObject();
					if (!bAll) { // for delta's, get the status
						switch (app.getInstallState()) {
							case App.INSTALLSTATE_updated:
								japp.put(Constants.PARAM_DEVICEAPP_STATUS, Constants.PARAM_DEVICEAPP_STATUS_update);
								break;
							case App.INSTALLSTATE_uninstalled:
								japp.put(Constants.PARAM_DEVICEAPP_STATUS, Constants.PARAM_DEVICEAPP_STATUS_remove);
								break;
							default: // INSTALLSTATE_installed
								japp.put(Constants.PARAM_DEVICEAPP_STATUS, Constants.PARAM_DEVICEAPP_STATUS_add);
								break;
						}
					}
					japp.put(Constants.PARAM_DEVICEAPP_UDID, deviceudid);
					japp.put(Constants.PARAM_DEVICEAPP_PKGNAME, app.getPackageName());
					japp.put(Constants.PARAM_DEVICEAPP_APPNAME, app.getDisplayName());
					japp.put(Constants.PARAM_DEVICEAPP_FULLVER, app.getVersionName());
					japp.put(Constants.PARAM_DEVICEAPP_SHORTVER,app.getVersionCodeStr());
					japp.put(Constants.PARAM_DEVICEAPP_PKGSIZE, app.getSizeOfPkg());
					japp.put(Constants.PARAM_DEVICEAPP_DATASIZE,app.getDataSize());
					
					// see if the app is managed or not: (look in the managed apps instance for the package name)
					bManaged = (managedApps!=null && managedApps.findAppByPackageName(app.getPackageName(),0)!=null);
					japp.put(Constants.PARAM_DEVICEAPP_MANAGED, bManaged);
					
					jarray.put(japp);					
				} catch (Exception ex) {
					LSLogger.exception(TAG, "GetDeviceApps JSON App error:", ex);
					break;
				}
			}
			// now, if we added any apps, create the outer json:
			if (jarray.length() > 0) {
				try {
					if (bAll) { // indicate this is an all list 
						json.put(Constants.PARAM_DEVICEAPPS_APPS_ALL, Constants.CMD_value_true);
					}
					json.put(Constants.PARAM_DEVICEAPPS_APPS, jarray);
				} catch (Exception ex) {
					LSLogger.exception(TAG, "GetDeviceApps JSON Apps error:", ex);
				}
			}
		}
		return json;
	}

	/**
	 * Gets a collection of App instances that represent the differences since the last time a difference
	 * check was performed.  
	 * @return a thread-safe list of App instances with InstallState set to the current difference, 
	 * or an empty list if no differences were found. (The list is created each time this is called, returning
	 * a difference instance.)
	 */
	// Note that when complete, the devicedbapps list can be dirty or out of sync. 
	protected List<App> getDeltaApps() {
		App app;
		App searchApp;
		Vector<App> deltaapps = new Vector<App>(10); // list of apps that are different from the database vs. what currently exists on the device. 
		DeviceAppsDB db = null;  // Database support instance 
		// get apps list with all current apps, keep it in this instance.
		apps.clear();
		getAllInstalledApps(); 
		// now we need to find differences:
		// - entries in apps that are not in devicedbapps are newly-installed apps
		// - entries in apps with different versions than those in devicedbapps are updated apps
		// - entries in devicedbapps that are not in apps are apps that were uninstalled and need removing from db
		try {
			db = new DeviceAppsDB(context);
			loadDeviceDbApps(db);    // fills devicedbapps with records from our database

			// first, make sure apps in the db still exist in the list of apps installed, and for
			//  any found, compare versions to detect version changes:
			Iterator<App> iter = devicedbapps.iterator();
			while (iter.hasNext()) {
				app = iter.next();  // "app" instance is an instance in the db list, not the list of current apps.
				searchApp = findAppByPackageName(apps, app.getPackageName(), 0); // searchApp is from current apps.
				if (searchApp == null) { // app was uninstalled, so remove it from the db and add to delta list.
					app.setInstallState(App.INSTALLSTATE_uninstalled);
					deltaapps.add(app);					
					db.deleteApp(app);
				} else { // compare the app versions; if different, update the info and add to delta list.
					// compare versions; if we detect a difference, handle it. use the searchApp as the new version.
					if ( !app.compareVersions(searchApp)) {
						searchApp.setDBID(app.getDbID());
						searchApp.setInstallState(App.INSTALLSTATE_updated);
						deltaapps.add(searchApp);
						db.updateAppValues(searchApp);
					}
				}
			}
			
			// now, search through the current list of all apps, and compare to the dbapps; items not found
			//  represent apps installed since the last time this was done.
			iter = apps.iterator();
			while (iter.hasNext()) {
				app = iter.next();
				searchApp = findAppByPackageName(devicedbapps, app.getPackageName(), 0);
				if (searchApp == null) { // app was installed, so add it to the db and add to delta list.
					app.setInstallState(App.INSTALLSTATE_installed);
					deltaapps.add(app);	
					db.insert(app);
				}
			}
			
		} catch (Exception ex) {
			LSLogger.exception(TAG, "GetDeltaApps error: ", ex);
		} finally {
			if (db != null) {
				try {
					db.close();
				} catch (Exception ex) {};
			}
		}
		
		return deltaapps;		
	}
	
	// call getAllInstalledApps   to get all apps found on the system
	
	/**
	 * Loads the list of stored applications into this instance, reading them from the database
	 * @return returns reference to the list of the apps. Do not alter the list directly!
	 */
	public List<App> loadDeviceDbApps(DeviceAppsDB deviceDB) {
		devicedbapps.clear();
		DeviceAppsDB db = deviceDB;
		try {
			if (deviceDB == null)
				db = new DeviceAppsDB(context);
			db.readAppRecords(devicedbapps, DBStorage.DBORDER_Ascending, null);
			if (deviceDB == null)
				db.close();			
			LSLogger.debug(TAG, "Number of installed apps found = " + apps.size());
		} catch (Exception ex) {
			LSLogger.exception(TAG, "LoadDeviceDbApps error: ", ex);
		}		
		return devicedbapps;
	}

	
	/**
	 * Static method to get the SQL used to create the database table for device apps.
	 * @return the SQL string used to create the table.
	 */
    public static String getDeviceAppsSqlCreateTable() {
    	return DeviceAppsDB.getSqlCreateTable();
    }
	
	
	// -----------------------------------------------------------------------------
	// --- Inner-class for handling Device Apps persistence in a sqlite database  --
	// -----------------------------------------------------------------------------
	/**
     * A SQLite database is used to store Device Apps in the deviceapps table.
	 * 
	 * Rows are added for each app that is found installed since the last check.
	 * Rows are removed for apps that are uninstalled. 
	 * Data for an app is updated as needed, as version and other stats info changes.
	 * 
	 * In general, this table is initially populated with all apps installed on the system.
	 * Over time, the installed apps is compared with the current list of apps on the system,
	 * and differences are sent to the mdm server and are updated in this table. 
	 * 
	 * Each row includes: 
	 * id - locally-generated row ID of the app.
	 * mdmid - ID of the app on the MDM server; will be 0 if not known.
	 * appname - display name of the app
	 * packagename - name of the package, typically something like com.lightspeedsystems.mdm
	 * vername - version name string, from package.
	 * vercodestr - version code string, from package
	 * vercodeint - version code integer, from package
	 * pkgsize - size of the package file
	 * datasize - amount of the data storage used by the app for data and local storage
	 */
	private static class DeviceAppsDB extends DBStorage {
		private final static String DVAPP_DB_TABLENAME = "deviceapps";
		private final static String COLUMN_ID        = "id";  // internally-generated ID, from db when added.
		private final static String COLUMN_MDMID     = "mdmid";
		private final static String COLUMN_APPNAME   = "appname";
		private final static String COLUMN_PKGNAME   = "packagename";
		private final static String COLUMN_VERSIONNAME  = "vername";
		private final static String COLUMN_SVERSIONCODE = "vercodestr";
		private final static String COLUMN_IVERSIONCODE = "vercodeint";
		private final static String COLUMN_PACKAGESIZE  = "pkgsize";
		private final static String COLUMN_DATASIZE     = "datasize";
				
		// SQL command used to create the log entries table:
		private final static String DVAPP_DB_CREATE_SQL =  
                "CREATE TABLE " +DVAPP_DB_TABLENAME + " (" +
                		COLUMN_ID    		  + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                		COLUMN_MDMID 		  + " INTEGER, " +
                		COLUMN_APPNAME   	  + " TEXT, " +
                		COLUMN_PKGNAME 		  + " TEXT, " +
                 		COLUMN_VERSIONNAME 	  + " TEXT, " +
                		COLUMN_SVERSIONCODE   + " TEXT, " +
                		COLUMN_IVERSIONCODE   + " INTEGER, " +
                		COLUMN_PACKAGESIZE    + " LONG, " +
                		COLUMN_DATASIZE  	  + " LONG);";
		// index locations of the data in the query, for improved processing speeds:
		private final static int COLUMN_ID_INDEX        = 0;
		private final static int COLUMN_MDMID_INDEX     = 1;
		private final static int COLUMN_APPNAME_INDEX   = 2;
		private final static int COLUMN_PKGNAME_INDEX   = 3;
		private final static int COLUMN_VERSIONNAME_INDEX  = 4;
		private final static int COLUMN_SVERSIONCODE_INDEX = 5;
		private final static int COLUMN_IVERSIONCODE_INDEX = 6;
		private final static int COLUMN_PACKAGESIZE_INDEX  = 7;
		private final static int COLUMN_DATASIZE_INDEX     = 8;
		
		// SQL command to read all rows from the database
		private final static String APP_DB_QUERY_GETALL_desc =
				"select * from " + DVAPP_DB_TABLENAME + " order by " + COLUMN_APPNAME + " desc;";
		private final static String APP_DB_QUERY_GETALL_asc =
				"select * from " + DVAPP_DB_TABLENAME + " order by " + COLUMN_APPNAME + " asc;";
		/** not used.
		// SQL to get a row based on a specific field value:
	 	private final static String APP_DB_QUERY_DBID =
	 			"select * from " + DVAPP_DB_TABLENAME + " where " + COLUMN_ID + "=?;";
	 	private final static String APP_DB_QUERY_MDMDBID =
	 			"select * from " + DVAPP_DB_TABLENAME + " where " + COLUMN_MDMID + "=?;";
	 	private final static String APP_DB_QUERY_PACKAGENAME =
	 			"select * from " + DVAPP_DB_TABLENAME + " where " + COLUMN_PKGNAME + "=?;";
	 	private final static String APP_DB_QUERY_APPNAME =
	 			"select * from " + DVAPP_DB_TABLENAME + " where " + COLUMN_APPNAME + "=?;";
		**/
		
		/**
		 * Creates Database persistence instance, where data will be stored.
		 * @param context
		 */
		public DeviceAppsDB(Context context) {
	        super(context);
	        setLoggingPersistence(false);
		}

		/**
		 * Abstract method to get the name of the database table.
		 * @return
		 */
	    public String getSqlTableName() {
	    	return DVAPP_DB_TABLENAME;
	    }

		/**
		 * Static method to get the SQL used to create the database table.
		 * @return the SQL string used to create the table.
		 */
	    public static String getSqlCreateTable() {
	    	return DVAPP_DB_CREATE_SQL;
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
	        map.put(COLUMN_ID,     COLUMN_ID);
	        map.put(COLUMN_MDMID,  COLUMN_MDMID);
	        map.put(COLUMN_APPNAME,COLUMN_APPNAME);
	        map.put(COLUMN_PKGNAME,COLUMN_PKGNAME);
	        map.put(COLUMN_VERSIONNAME, COLUMN_VERSIONNAME);
	        map.put(COLUMN_SVERSIONCODE,COLUMN_SVERSIONCODE);
	        map.put(COLUMN_IVERSIONCODE,COLUMN_IVERSIONCODE);
	        map.put(COLUMN_PACKAGESIZE, COLUMN_PACKAGESIZE);
	        map.put(COLUMN_DATASIZE,    COLUMN_DATASIZE);
	        return map;
	    }
	    
	    // -- data management methods --
	    
	    protected void closeDB() {
	    	super.closeDB();
	    }
	    
	    /**
	     * Reads db data into the given App instance. Uses various attributes in the app to try
	     * to find it, including: packageName, appName, DbID, and MdmDbID.
	     * @param app App instance with at least one value to search on. Fills in other DB values
	     * in the instance if it is found within the database. (check the DbID value, as it gets
	     * set if the App was found, or left 0 if not found).
	     * @return true if the app was found, false if the app does not exist in the database.
	     */
	    /***  note used.
	    @SuppressWarnings("unused")
		protected boolean readApp(App app) {
	    	boolean bFound = false;
	        Cursor cursor = null;
	    	try {
	    		SQLiteDatabase db = openForReading(); //Writing();
	    		String sql;
	    		String[] args = null;
	    		if (app.getDbID() != 0) {
	    			sql = APP_DB_QUERY_DBID;
	    			args = new String[] { Integer.toString(app.getDbID()) };
	    		} else if (app.getMdmDbID() != 0) {
	    			sql = APP_DB_QUERY_MDMDBID;
	    			args = new String[] { Integer.toString(app.getMdmDbID()) };
	    		} else if (app.getPackageName() != null) {
	    			sql = APP_DB_QUERY_PACKAGENAME;
	    			args = new String[] { app.getPackageName() };
	    		} else if (app.getAppName() != null) {
	    			sql = APP_DB_QUERY_APPNAME;
	    			args = new String[] { app.getAppName() };
	    		} else { // we have no values to search on; bail out.
	    			throw new Exception("No values to search on.");
	    		}
	    		
	    		//debug:
	    		LSLogger.debug(TAG, "Query single App SQL=" + sql);
	    		LSLogger.debug(TAG, "Query single App args=" + ((args==null)?"null":args.toString()));
	    		
	    		cursor = db.rawQuery(sql, args);
	    		
	    		if (cursor != null && cursor.moveToFirst()) {
	    			// read the data; assume it's only one row and we're reading that row.
	                readAppRow(app, cursor);
	    		}
	    		bFound = true;
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Reading DB in single App query: ", ex, false);
	    	} finally {
	    		if (cursor != null && !cursor.isClosed())
	    			cursor.close();
	    	}
	    	return bFound;
	    }
	    ***/
	    
	    protected long readAppRecords(Vector<App> list, int sortOrder, String msgTypeFilter) {
	    	int result = -1;
	        Cursor cursor = null;
	    	try {
	    		SQLiteDatabase db = openForReading(); //Writing();
	    		String sql;
	    		String[] args = null;
	    //		if (msgTypeFilter == null) {
	    			sql = ((sortOrder==DBORDER_Descending) ? 
	    				APP_DB_QUERY_GETALL_desc : APP_DB_QUERY_GETALL_asc);
	    //		} else {
	    //			sql = ((sortOrder==DBORDER_Descending) ? 
	    //					APP_DB_QUERY_GETTYPE_desc : APP_DB_QUERY_GETTYPE_asc);
	    //			args = new String[] { msgTypeFilter };	    			
	    //		}
	    		//debug:
	    		LSLogger.debug(TAG, "SQL=" + sql);
	    		//LSLogger.debug(TAG, "args=" + ((args==null)?"null":args.toString()));
	    		
	    		cursor = db.rawQuery(sql, args);
	    		
	    		if (cursor != null && cursor.moveToFirst()) {
	    			result = 0;
	                do {
	                    App app = new App();
	                    readAppRow(app, cursor);
	                    list.add(app);
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
	    
	    /*
	     * Fills in an app instance from an open cursor.
	     * Throws any cursor exceptions back to caller.
	     */
	    private void readAppRow(App app, Cursor cursor) {
                app.setDBID( cursor.getInt(COLUMN_ID_INDEX) );
                app.setMdmDbID(	cursor.getInt(COLUMN_MDMID_INDEX) );
                app.setAppName( cursor.getString(COLUMN_APPNAME_INDEX) ); 
                app.setPackageName( cursor.getString(COLUMN_PKGNAME_INDEX) );                
                app.setVersionName( cursor.getString(COLUMN_VERSIONNAME_INDEX) );
                app.setVersionCodeStr( cursor.getString(COLUMN_SVERSIONCODE_INDEX) );
                app.setVersionCode( cursor.getInt(COLUMN_IVERSIONCODE_INDEX) );
                app.setSizeOfPkg( cursor.getLong(COLUMN_PACKAGESIZE_INDEX) );
                app.setDataSize( cursor.getLong(COLUMN_DATASIZE_INDEX) );
	    }
	    
	    /**
	     * Adds the App instance's data to the database.
	     * @param app App instance to add.
	     * @return the row ID of the inserted row and sets the ID into the app instance, 
	     * or returns -1 if error.
	     */
	    protected long insert(App app) {
	    	ContentValues mapping  = new ContentValues(8);
	    	mapping.put(COLUMN_MDMID,         Integer.valueOf(app.getMdmDbID()));
	    	mapping.put(COLUMN_APPNAME,       app.getAppName());
	    	mapping.put(COLUMN_PKGNAME,       app.getPackageName());
	    	mapping.put(COLUMN_VERSIONNAME,	  app.getVersionName());
	    	mapping.put(COLUMN_SVERSIONCODE,  app.getVersionCodeStr());
	    	mapping.put(COLUMN_IVERSIONCODE,  Integer.valueOf(app.getVersionCode()));
			mapping.put(COLUMN_PACKAGESIZE,	  Long.valueOf(app.getSizeOfPkg()));
	    	mapping.put(COLUMN_DATASIZE,      Long.valueOf(app.getDataSize()));
	    	app.setDefDirty(false);	    	
	    	int id = (int)super.insertRow(DVAPP_DB_TABLENAME, mapping);
	    	if (id > 0)
	    		app.setDBID(id);
	    	return id;
	    }
	    
	    /**
	     * Updates the App's DB data for updateable-related values (versions and sizes).
	     * @param app App instance to update
	     * @return 1 if the app was updated, 0 if the app was not found, -1 for error.
	     */
	    protected int updateAppValues(App app) {
	    	String args[] = new String[] { Integer.toString(app.getDbID()) };
	    	String whereClause = COLUMN_ID + "=?";
	    	// set the values to be updated: unless marked as dirty, we only need to update certain columns:
	    	ContentValues mapping;
	    	if (app.isDefDirty()) {
	    		mapping  = new ContentValues(7);
		    	mapping.put(COLUMN_APPNAME, app.getAppName());
		    	mapping.put(COLUMN_PKGNAME, app.getPackageName());
	    		app.setDefDirty(false);
	    	} else {
	    		mapping  = new ContentValues(5);
	    	}
	    	mapping.put(COLUMN_VERSIONNAME,	  app.getVersionName());
	    	mapping.put(COLUMN_SVERSIONCODE,  app.getVersionCodeStr());
	    	mapping.put(COLUMN_IVERSIONCODE,  Integer.valueOf(app.getVersionCode()));
			mapping.put(COLUMN_PACKAGESIZE,	  Long.valueOf(app.getSizeOfPkg()));
	    	mapping.put(COLUMN_DATASIZE,      Long.valueOf(app.getDataSize()));
	    	return (int)super.updateRow(DVAPP_DB_TABLENAME, mapping, whereClause, args);
	    }
	    

	    /**
	     * Deletes all apps from the database.
	     */
	    /** not used at this time; keep here for if/when needed.
	    protected void deleteAll() {
	    	try {
	    		SQLiteDatabase db = openForWriting();
	    		db.delete(APP_DB_TABLENAME, null, null);
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Deleting all Apps from DB: ", ex);
	    	}
	    }
		**/
	    
	    /**
	     * Deletes an app from the database.
	     */
	    protected void deleteApp(App app) {
	    	try {
		    	String args[] = new String[] { Integer.toString(app.getDbID()) };
		    	String whereClause = COLUMN_ID + "=?";
	    		SQLiteDatabase db = openForWriting();
	    		db.delete(DVAPP_DB_TABLENAME, whereClause, args);
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Deleting DeviceApp from DB:", ex);
	    	}
	    }

	}
	
	
}
