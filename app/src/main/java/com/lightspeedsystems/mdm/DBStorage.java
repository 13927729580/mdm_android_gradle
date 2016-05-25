/**
 * 
 */
package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Application persistence storage support in local private SqlLite database tables.
 * This is an abstract base class.
 */
public abstract class DBStorage extends SQLiteOpenHelper {
	private final static String TAG = "DBStorage";
	/** Version of local database. */
	//public final static int DBSCHEMA_VERSION  = 1;
	//public final static String DBSCHEMA_VERSION_STR  = "1";
	
	/** Results are sorted ascending (earliest/lowest-ID items first). */
	public final static int DBORDER_Ascending  = 0;
	/** Results are sorted descending (latest/lowest-ID items first). */
	public final static int DBORDER_Descending = 1;

	
	/** Name of the private database file. The file is located at /data/data/com.lightspeedsystems.mdm/databases. */
	private final static String DATABASE_NAME = "mdmdb";
	
    private static final int DATABASE_VERSION = 3;
    // version 1: initial release
    // version 2: added managedapps table
    
    //private static final String DICTIONARY_TABLE_NAME = "dictionary";
    
 	protected Context context;
    private SQLiteDatabase opendb; 
    private boolean bLoggingPersistenceEnabled; // when true, errors, warnings, and debug info are persisted to 
    		// the logger's persistence. Set to false when logging Logger persistence errors!
   
	public DBStorage(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	        this.context = context;
	        bLoggingPersistenceEnabled = true; // by default, we want to log any errors.
	}

	// Creates all DB tables if they do not exist. Gets called from constructor when creating
	//  db for the first time.
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL( Apps.getManagedAppsSqlCreateTable() );	
        db.execSQL( DeviceApps.getDeviceAppsSqlCreateTable() );	
        db.execSQL( Events.getEventsSqlCreateTable() );
        db.execSQL( LSLogger.getLogsSqlCreateTable() );
        db.execSQL( Profiles.getProfilesSqlCreateTable() );	
	}
    
    // handle the database migrations and updates.
    @Override
    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
    	try {
    		if (oldVersion < 2)
    			db.execSQL( DeviceApps.getDeviceAppsSqlCreateTable() );	
    		if (oldVersion < 3)
    			db.execSQL( Profiles.getProfilesSqlCreateTable() );	
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "DBUpgrade Error.", ex);
    	}
    }
    
    /**
     * Enables or disables the logging of errors that may occur within this class or subclasses.
     * By default, logging is enabled, allowing errors to be logged. But a class may turn off
     * all logging of errors, thereby preventing data from being written to the database.
     * This is critical for the Logger classes themselves, so that they don't cause infinite loops when
     * trying to write a log entry, a db error occurs, it tries to log that, the same db error occurs, etc.
     * @param enabled true to enable logging of errors, false to disable logging.
     */
    protected void setLoggingPersistence(boolean enabled) {
    	bLoggingPersistenceEnabled = enabled;
    }
    
    /**
     * Opens the database for reading. 
     * @return SQLiteDatabase database instance ready to read from, or
     * null if an error occured or the db is not available.
     */
    protected synchronized SQLiteDatabase openForReading() {
    	SQLiteDatabase db = null;
    	try {
    		opendb = getReadableDatabase();
    		db = opendb;
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "OpenForReading error.", ex, bLoggingPersistenceEnabled);
    	}
    	return db;
    }
    
    /**
     * Opens the database for writing. 
     * @return SQLiteDatabase database instance ready to write to, or
     * null if an error occurred or the db is not available.
     */
    protected synchronized SQLiteDatabase openForWriting() {
    	SQLiteDatabase db = null;
    	try {
    		opendb = getWritableDatabase();
    		db = opendb;
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "OpenForWriting error.", ex, bLoggingPersistenceEnabled);
    	}
    	return db;
    }
    
    /**
     * Closes the database if it is open.
     */
    protected synchronized void closeDB() {
    	try {
	    	if (opendb != null && opendb.isOpen()) {
	    		close();
	    	}
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "Close DB error.", ex, bLoggingPersistenceEnabled);
    	} finally {
    		opendb = null;
    	}
    }
    
    // -- DB transactional operations --
    
    /**
     * Adds the values to a new row in the given table name.
     * @param tableName
     * @param values
     * @return positive row number upon successful addition to the database, -1 if error
     */
    public long insertRow(String tableName, ContentValues values) {
    	long result = -1;
    	openForWriting();
    	try {
    		if (opendb != null && opendb.isOpen()) {
    			result = opendb.insert(tableName, null, values);
    		} else {
    			LSLogger.error(TAG, "Database " + tableName + " not open for writing.", bLoggingPersistenceEnabled);
    		}
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, tableName, ex, bLoggingPersistenceEnabled);
    	}
    	return result;
    }
    
    /**
     * Updates the values to a specific row in the given table name.
     * @param tableName
     * @param values
     * @return the number of rows updated, -1 if error.
     */
    public long updateRow(String tableName, ContentValues values, String whereClause, String[] whereArgs) {
    	long result = -1;
    	openForWriting();
    	try {
    		if (opendb != null && opendb.isOpen()) {
    			result = opendb.update(tableName, values, whereClause, whereArgs);
    		} else {
    			LSLogger.error(TAG, "Database " + tableName + " not open for writing.", bLoggingPersistenceEnabled);
    		}
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, tableName, ex, bLoggingPersistenceEnabled);
    	}
    	return result;
    }
    
    
    
    // -------------------------------------------------
    // abstract methods to be implemented by subclasses:
    // -------------------------------------------------
    
	/**
	 * Abstract method to get the name of the primary database table.
	 * @return the name of the table 
	 */
    public abstract String getSqlTableName();


    
	// --------------------------------------------
	// ---- DB creation and migration support -----
	// --------------------------------------------
	// Creates the database tables
	//private void createDBtables() {
	//}
	
}
