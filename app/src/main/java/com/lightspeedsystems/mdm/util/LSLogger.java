package com.lightspeedsystems.mdm.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.json.JSONObject;

import com.lightspeedsystems.mdm.DBStorage;
import com.lightspeedsystems.mdm.Settings;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Logs application messages to LogCat and to application database storage.
 *
 */
public class LSLogger {
	private static String TAG = "LSLogger";

	// message types, used to categorize the type of the message:
	public final static String MSGTYPE_error = "e";
	public final static String MSGTYPE_warn  = "w";
	public final static String MSGTYPE_info  = "i";
	public final static String MSGTYPE_debug = "d";
	// message types as char values (for faster processing):
	public final static char MSGTYPEC_error = 'e';
	public final static char MSGTYPEC_warn  = 'w';
	public final static char MSGTYPEC_info  = 'i';
	public final static char MSGTYPEC_debug = 'd';
	public final static char MSGTYPEC_unknown='X';
	
	/** Log list is sorted date-ascending (earliest dates/times first). */
	public final static int LOGORDER_Ascending  = 0;
	/** Log list is sorted date-descending (latest dates/times first). */
	public final static int LOGORDER_Descending = 1;

	// numeric logging indicators; from least-logging to most-logging. 
	// A level includes all levels below it. For example, at level 3, it includes 3, 2, and 1.
	public final static int LOGLEVEL_none  = 0;
	public final static int LOGLEVEL_error = 1;
	public final static int LOGLEVEL_warn  = 2;
	public final static int LOGLEVEL_info  = 3;
	public final static int LOGLEVEL_debug = 4;
	public final static int LOGLEVEL_all   = 9;
	
	private final static int LOGLEVEL_default = LOGLEVEL_none;  // the default logging level 
	
	private static int logLevel = LOGLEVEL_default; // current logging level, one of the LOGLEVEL_ values
	private static boolean doLogcat = true;			// when true, logs to the system logger so that logcat can display the messages
	
	private static LSLogger logger;
	private static SimpleDateFormat logItemDateFormatter; // single instance for quick-formatting of log item dates.
	
	private static Context context;
	private Vector<LSLogItem> logItems;	// list of current logging information items in the current logItemsSortOrder
	private Vector<LSLogItem> reverseLogItems; // list of logging items in opposite sorting order
	private int logItemsSortOrder;		// default/current date-sort order of the items in the logItems list.
	private LoggerDB loggerDB;
	private LoggerDataChangeListener dataObserver; // observer for watching for data changes; note that we should use a list of these.
	
	/** Initializes the logging system. To be called even if logging is currently off. */
	public static void initialize(Context appcontext) {
		context = appcontext;
		Settings settings = Settings.getInstance(context);
		if (settings != null)
			logLevel = settings.getSettingInt(Settings.LOGLEVEL, LOGLEVEL_default);
	}
	
	/** Shuts down the logging system. */
	public static void terminate() {
		logLevel = LOGLEVEL_none;
		if (logger != null) {
			try {
				if (logger.loggerDB != null) {
					logger.loggerDB.closeDB();
					logger.loggerDB = null;
				}
			} catch (Exception e) {
				// ignore shut-down errors.
			}
		}
	}

	/** Gets the Logger instance, creating it as needed. */
	public static LSLogger getInstance() {
		if (logger == null) {
			synchronized (LSLogger.class) {
				if (logger == null)  {
					logger = new LSLogger();
				}
			}			
		}	
		return logger;
	}
	
	/**
	 * Adds the observer for data change notifications.
	 * @param observer instance to call back to upon changes to data.
	 */
	public void registerDataChangeListener(LoggerDataChangeListener observer) {
		dataObserver = observer;
	}
	
	/**
	 * Removed the observer for data change notifications.
	 * @param observer instance to call back to upon changes to data.
	 */
	public void unregisterDataChangeListener(LoggerDataChangeListener observer) {
		if (dataObserver == observer)
			dataObserver = null;
	}

	/**
	 * Interface for calling back an app data change event to.
	 */
	public interface LoggerDataChangeListener {
		public void onLoggerDataChanged(LSLogItem item);
	}
	
	/**
	 * Notifies any observers of a change in the data.
	 */
	private void notifyObservers(LSLogItem item) {
		if (dataObserver != null) {
			//LSLogger.debug(TAG, "notifying dataObserver of LoggerDataChanged", false);
			dataObserver.onLoggerDataChanged(item);
		}
		
	}

	
	/**
	 * Gets the Logging level setting, one of the LOGLEVEL_ values. 
	 * A LOGLEVEL_none means logging is disabled.
	 * @return The logging level as a LOGLEVEL_ constant value.
	 */
	public static int getLogLevel() {
		return LSLogger.logLevel;
	}
	
	/**
	 * Sets the logging level to be the new value given. New log messages of this level or lower 
	 * will be captured and stored. Use LOGLEVEL_none to turn logging completely off.
	 * 
	 * The logger will save the value in Settings as needed to preserve it.
	 * This new setting does not change the contents of any existing log data, but only affects 
	 * new log entries.
	 * @param newlevel a LOGLEVEL_ value.
	 */
	public static void setLogLevel(int newlevel) {
		getInstance().setLoggingLevel(newlevel);
	}
	
	/**
	 * Gets a reference to the list of LogItem instances. Do NOT alter the contents of this list.
	 * @param sortOrder One of the LOGORDER_ flag values for indicating the date-ordering of the returned list.
	 * @return list of items, which might be an empty list.
	 */
	public static List<LSLogItem> getLogItems(int sortOrder) {
		LSLogger logger = getInstance();
		return logger.getAllLogs(sortOrder);
		//if (logger.logItemsSortOrder == sortOrder)
		//	return logger.logItems;
		//return logger.reverseLogItems;		
	}
	
	/**
	 * Gets a reference to the list of LogItem instances. Do NOT alter the contents of this list.
	 * The list is in the default sort order.
	 * @return list of items, which might be an empty list.
	 */
	public static List<LSLogItem> getLogItems() {
		LSLogger logger = getInstance();
		return getLogItems(logger.logItemsSortOrder);
	}

	/**
	 * Gets a list of LogItem instances for a specific log level and only for that level.
	 * @param sortOrder One of the LOGORDER_ flag values for indicating the date-ordering of the returned list.
	 * @param msgLevel  a LOGLEVEL_ value, internally is translated to one of the specific message types, or all if
	 * the level is unknown.
	 * @return list of items, which might be an empty list.
	 */
	public static List<LSLogItem> getLogItemsForMsgLevel(int sortOrder, int msgLevel) {
		LSLogger logger = getInstance();
		return logger.getLogsForMsgType(sortOrder, msgTypeFromLogLevel(msgLevel));
	}
	
	/**
	 * Clears all log data from memory and from file storage.
	 */
	public static void clearLogs() {
		getInstance().clearAllLogs();
	}
	
	
	public static boolean isLoggingEnabled() {
		return (logLevel > LOGLEVEL_none);
	}
	
	/**
	 * Gets a MSGTYPE_ value from the given log level. 
	 * @param logLevel a LOGLEVEL_ value for debug, warning, info, or error.
	 * @return a MSGTYPE_ string value, or null of not one of the LOGLEVEL_ values 
	 * ("none" and "all" will return a null).
	 * @return
	 */
	public static String msgTypeFromLogLevel(int logLevel) {
		String s = null;
		switch (logLevel) {
			case LOGLEVEL_error: s = MSGTYPE_error; break;
			case LOGLEVEL_warn:  s = MSGTYPE_warn; break;
			case LOGLEVEL_info:  s = MSGTYPE_info; break;
			case LOGLEVEL_debug: s = MSGTYPE_debug; break;
		}
		return s;
	}
	
	// ----------------------
	// -- instance methods --
	// ----------------------
	
	// constructs a local instance of the logger
	private LSLogger() {
		logItemsSortOrder = LOGORDER_Descending;
		logItems = new Vector<LSLogItem>();
		reverseLogItems = new Vector<LSLogItem>();
		loggerDB = new LoggerDB(context);
		logItemDateFormatter = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss:SSS");
	}
	
	
	/**
	 * Gets a list of all the log entries in the sort order indicated.
	 * @param sortOrder one of the sort order values, LOGORDER_Ascending or LOGORDER_Descending.
	 * @return list of log items or an empty list if no items were read.
	 */
	public List<LSLogItem> getAllLogs(int sortOrder) {
		// create a new list and read the items from the database.
		Vector<LSLogItem> list = new Vector<LSLogItem>();
		if (loggerDB != null)
			loggerDB.readLogRecords(list, sortOrder, null);
		return list;
	}
	
	/**
	 * Gets a list of all the log entries in the sort order indicated for a specific Message Type.
	 * @param sortOrder one of the sort order values, LOGORDER_Ascending or LOGORDER_Descending.
	 * @param msgType MSGTYPE_ value to search for, or null to get all records.
	 * @return list of log items or an empty list if no items were read.
	 */
	public List<LSLogItem> getLogsForMsgType(int sortOrder, String msgType) {
		// create a new list and read the items from the database.
		Vector<LSLogItem> list = new Vector<LSLogItem>();
		if (loggerDB != null)
			loggerDB.readLogRecords(list, sortOrder, msgType);
		return list;
	}

	
	private void setLoggingLevel(int level) {
		if (level >= LOGLEVEL_none && level <= LOGLEVEL_all) {
			logLevel = level;
			Settings settings = Settings.getInstance(context);
			if (settings != null)
				settings.setSetting(Settings.LOGLEVEL, level);
		}
	}
	
	/**
	 * clears all log data and deletes persisted log data from storage.
	 */
	private void clearAllLogs() {
		if (loggerDB != null)
			loggerDB.deleteAll();
		logItems.clear();
		reverseLogItems.clear();
	}
	
	// adds the logitem instance to the list of items and performs any needed persistence.
	private void addItem(LSLogItem item, boolean doPersist) {
		// persist the item as a row in the logger database:
		if (doPersist && loggerDB != null) {
			loggerDB.insert(item);
		} else {  // store the items in the local cache lists only
			if (logItemsSortOrder == LOGORDER_Ascending) {
				// add to bottom of the main list, then top of the reverse list
				logItems.add(item);
				reverseLogItems.insertElementAt(item, 0);
			} else {
				// add to the top of the main list, then to the bottom of the reverse list
				logItems.insertElementAt(item, 0);
				reverseLogItems.add(item);
			}			
		}
		notifyObservers(item);
	}

	
	// --------------------------------------------------------------------------------
	// -- static methods for calling into the logging system for adding log messages --
	// --------------------------------------------------------------------------------
	
	// internal method to add a log item to the logger instance so that it can be persisted
	private static void addLogItem(String tag, String msgType, String msg, boolean doPersist) {
		getInstance().addItem(  new LSLogItem(tag, msgType, msg), doPersist );		
	}

	/**
	 * Logs an informational message
	 * @param tag identifier of the source of the message.
	 * @param msg message string to log.
	 */
	public static void info(String tag, String msg)
	{
		info(tag, msg, true);
	}
	
	public static void info(String tag, String msg, boolean doPersist)
	{
		if (doLogcat)
			Log.i(tag, msg);
		if (logLevel >= LOGLEVEL_info) 
			addLogItem(tag, MSGTYPE_info, msg, doPersist);
	}

	
	/**
	 * Logs a debug message
	 * @param tag identifier of the source of the message.
	 * @param msg message string to log.
	 */
	public static void debug(String tag, String msg)
	{
		debug(tag, msg, true);
	}
	
	public static void debug(String tag, String msg, boolean doPersist)
	{
		if (doLogcat)
			Log.d(tag, msg);
		if (logLevel >= LOGLEVEL_debug) 
			addLogItem(tag, MSGTYPE_debug, msg, doPersist);
	}
 
	/**
	 * Logs a warning message
	 * @param tag identifier of the source of the message.
	 * @param msg message string to log.
	 */
	public static void warn(String tag, String msg)
	{
		warn(tag, msg, true);
	}

	public static void warn(String tag, String msg, boolean doPersist)
	{
		if (doLogcat)
			Log.w(tag, msg);
		if (logLevel >= LOGLEVEL_warn) 
			addLogItem(tag, MSGTYPE_warn, msg, doPersist);
	}
	
	/**
	 * Logs an error message
	 * @param tag identifier of the source of the message.
	 * @param msg message string to log.
	 */
	public static void error(String tag, String msg)
	{
		error(tag, msg, true);
	}
	
	public static void error(String tag, String msg, boolean doPersist)
	{
		if (doLogcat)
			Log.e(tag, msg);
		if (logLevel >= LOGLEVEL_error) 
			addLogItem(tag, MSGTYPE_error, msg, doPersist);
	}
	
	/**
	 * Logs details about an exception.
	 * @param tag source of the message
	 * @param ex exception to log. 
	 */
	public static void exception(String tag, Exception ex) {
		exception(tag,null,ex);
	}
	
	/**
	 * Logs an exception error message with details about the exception.
	 * @param tag source of the message
	 * @param description optional descriptive name for the exception; uses the exception name is this param is null.
	 * @param ex exception to log. 
	 * @return the error details
	 */
	public static String exception(String tag, String name, Exception ex) {
		return exception(tag, name, ex, true);
	}

	public static String exception(String tag, String name, Exception ex, boolean doPersist) {
		String exdetails = name;
		
		if (ex == null) {
			if (exdetails == null)
				exdetails = "";
			exdetails += " (unknown: exception is null)";
		} else {
			if (name == null)
				name = "Exception: ";
			String details = ex.getMessage();
			if (details==null)
				details = ex.toString();
			if (details != null && details.length()>0)
				exdetails += " " + details;
		}
		error(tag, exdetails);
		return exdetails;
	}
	

	
	// ------------------------------------------------------------------
	// --- Inner-class for defining the contents of a single log entry --
	// ------------------------------------------------------------------
	
	public static class LSLogItem extends JSONObject {
		
		long   msgtime;	// time the message was logged, current time in milliseconds
		String msgsrc;	// source of the message, usually a class name
		String msgtype;	// one of the LSLogger.MSGTYPE_ values
		String msg;		// actual message logged
		long   threadID;
		char   msgtypec;
		String formattedDate;
		String identifier;
		
		public LSLogItem() {
			super();			
		}
		
		/**
		 * Creates a new log line instance for the given information. Automatically creates the date/time stamp as the current time.
		 * @param msgSource
		 * @param msgType
		 * @param message
		 */
		public LSLogItem(String msgSource, String msgType, String message) {
			super();
			msgtime = new Date().getTime();
			msgsrc  = msgSource;
			msgtype = msgType;
			msg     = message;		
			threadID = Thread.currentThread().getId();
			createInternalValues();
		}
		
		protected void createInternalValues() {
			msgtypec = msgtypeCfromS(msgtype);
			formattedDate = logItemDateFormatter.format(new Date(msgtime));
			identifier = "TID:" + Long.toString(threadID) + " " + formattedDate + " " + msgsrc;			
		}
		
		public String getMessage() {
			return msg;
		}
		
		public String getIdentifier() {			
			return identifier;
		}		
		public String getMsgType() {
			return msgtype;
		}
		public char getMsgTypeChar() {
			return msgtypec;
		}

		// converts msgtype string to a literal value (a char).
		private char msgtypeCfromS(String msgtype) {
			char c = MSGTYPEC_unknown;
			if (msgtype != null) {
				if (msgtype.equals(MSGTYPE_info))
					c = MSGTYPEC_info;
				else if (msgtype.equals(MSGTYPE_warn))
					c = MSGTYPEC_warn;
				else if (msgtype.equals(MSGTYPE_error))
					c = MSGTYPEC_error;
				else if (msgtype.equals(MSGTYPE_debug))
					c = MSGTYPEC_debug;
			}
			return c;
		}
		
		/**
		 * Create a log export text string for writing to an exported log file.
		 * @return String for the item's information. format is: date type threadID source: msg
		 */
		public String toLogExportString() {
			String s=formattedDate + " " + msgtype + " " + Long.toString(threadID) + " " + msgsrc + ": " + msg;
			return s;
		}
		
		/**
		 * Gets file header string for exported log entries.
		 */
		public static String getExportedLogHeader() {
			return "Date                 Type TID Source                 Message";
		}
		/**
		 * Gets file header separator string for exported log entries.
		 */
		public static String getExportedSeparator() {
			return "-------------------------------------------------------------------";
		}
	}
	
	/**
	 * Abstract method to get the SQL used to create the Logs database table.
	 * @return the SQL string used to create the table.
	 */
    public static String getLogsSqlCreateTable() {
    	return LoggerDB.getSqlCreateTable();
    }

	
	// -----------------------------------------------------------------------
	// --- Inner-class for handling logging persistence in sqlite database  --
	// -----------------------------------------------------------------------
	/**
	 * LoggerDB provides a database persistence layer for storing and reading log messages.
	 * 
	 * A SQLite database is used to store log entries both in and beyond the current session, 
	 * for capturing various logging details for local display (from the logging
	 * screen) and for remote queries from the server to get the logs.
	 * 
	 * Rows are added for each log message. Each item has a long timestamp value that
	 * is used as the key, and also for sorting chronologically. 
	 * Each row also includes: 
	 * logtype - defines the type of message as warning, error, info, debug, etc (1-letter e,w,i,d)
	 * tid - thread ID the message was generated from; useful for multithreading diagnosis
	 * source - class where the message originated
	 * msg - message logged. For exceptions, this includes the exception details.	
	 */
	private static class LoggerDB extends DBStorage {
		private final static String LOGGER_DB_TABLENAME = "lslogger";
		private final static String COLUMN_TIME = "id"; // "logtime";
		private final static String COLUMN_TYPE = "logtype";
		private final static String COLUMN_TID  = "tid";
		private final static String COLUMN_SRC  = "source";
		private final static String COLUMN_MSG  = "msg";
				
		// SQL command used to create the log entries table:
		private final static String LOGGER_DB_CREATE_SQL =  
                "CREATE TABLE " + LOGGER_DB_TABLENAME + " (" +
                		COLUMN_TIME + " LONG, " +
                		COLUMN_TID  + " LONG, " +
                		COLUMN_TYPE + " TEXT, " +
                		COLUMN_SRC  + " TEXT, " +
                		COLUMN_MSG  + " TEXT);";
		// index locations of the data in the query, for improved processing speeds:
		private final static int COLUMN_TIME_INDEX = 0;
		private final static int COLUMN_TID_INDEX  = 1;
		private final static int COLUMN_TYPE_INDEX = 2;
		private final static int COLUMN_SRC_INDEX  = 3;
		private final static int COLUMN_MSG_INDEX  = 4;

		// SQL command to read all rows from the database
		private final static String LOGGER_DB_QUERY_GETALLLOGS_desc =
				"select * from " + LOGGER_DB_TABLENAME + " order by " + COLUMN_TIME + " desc;";
		private final static String LOGGER_DB_QUERY_GETALLLOGS_asc =
				"select * from " + LOGGER_DB_TABLENAME + " order by " + COLUMN_TIME + " asc;";
		// SQL to get rows for a specific logtype
		private final static String LOGGER_DB_QUERY_GETTYPELOGS_desc =
				"select * from " + LOGGER_DB_TABLENAME + " where " + COLUMN_TYPE + "=? order by " + COLUMN_TIME + " desc;";
		private final static String LOGGER_DB_QUERY_GETTYPELOGS_asc =
				"select * from " + LOGGER_DB_TABLENAME + " where " + COLUMN_TYPE + "=? order by " + COLUMN_TIME + " asc;";
		  
		
		/**
		 * Creates Database persistence instance, where Logging data will be stored.
		 * @param context
		 */
		public LoggerDB(Context context) {
	        super(context);
	        setLoggingPersistence(false);
		}

		/**
		 * Abstract method to get the name of the database table.
		 * @return
		 */
	    public String getSqlTableName() {
	    	return LOGGER_DB_TABLENAME;
	    }

		/**
		 * Abstract method to get the SQL used to create the Logs database table.
		 * @return the SQL string used to create the table.
		 */
	    public static String getSqlCreateTable() {
	    	return LOGGER_DB_CREATE_SQL;
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
	        map.put(COLUMN_TIME, COLUMN_TIME);
	        map.put(COLUMN_TID,  COLUMN_TID);
	        map.put(COLUMN_TYPE, COLUMN_TYPE);
	        map.put(COLUMN_SRC,  COLUMN_SRC);
	        map.put(COLUMN_MSG,  COLUMN_MSG);
	        return map;
	    }
	    
	    // -- data management methods --
	    
	    protected void closeDB() {
	    	super.closeDB();
	    }
	    
	    protected long readLogRecords(Vector<LSLogItem> list, int sortOrder, String msgTypeFilter) {
	    	int result = -1;
	        Cursor cursor = null;
	    	try {
	    		SQLiteDatabase db = openForReading(); //Writing();
	    		String sql;
	    		String[] args = null;
	    		if (msgTypeFilter == null) {
	    			sql = ((sortOrder==LOGORDER_Descending) ? 
	    				LOGGER_DB_QUERY_GETALLLOGS_desc : LOGGER_DB_QUERY_GETALLLOGS_asc);
	    		} else {
	    			sql = ((sortOrder==LOGORDER_Descending) ? 
		    				LOGGER_DB_QUERY_GETTYPELOGS_desc : LOGGER_DB_QUERY_GETTYPELOGS_asc);
	    			args = new String[] { msgTypeFilter };	    			
	    		}
	    		//debug:
	    		//LSLogger.debug(TAG, "SQL=" + sql, false);
	    		//LSLogger.debug(TAG, "args=" + ((args==null)?"null":args.toString()), false);
	    		
	    		cursor = db.rawQuery(sql, args);
	    		
	    		if (cursor != null && cursor.moveToFirst()) {
	    			result = 0;
	                do {
	                    LSLogItem item = new LSLogItem(); 
	                    item.msgtime  = cursor.getLong(COLUMN_TIME_INDEX);
	                    item.threadID = cursor.getLong(COLUMN_TID_INDEX);
	                    item.msgtype = cursor.getString(COLUMN_TYPE_INDEX);
	                    item.msgsrc  = cursor.getString(COLUMN_SRC_INDEX);
	                    item.msg     = cursor.getString(COLUMN_MSG_INDEX);
	                    item.createInternalValues();
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
	     * Adds the LSLogItem instance's data to the database.
	     * @param item
	     * @return the row ID of the inserted row, or -1 if error.
	     */
	    protected long insert(LSLogItem item) {
	    	ContentValues mapping  = new ContentValues(5);
	    	mapping.put(COLUMN_TIME, Long.valueOf(item.msgtime));
	    	mapping.put(COLUMN_TID,  Long.valueOf(item.threadID));
	    	mapping.put(COLUMN_TYPE, item.msgtype);
	    	mapping.put(COLUMN_SRC,  item.msgsrc);
	    	mapping.put(COLUMN_MSG,  item.msg);
	    	return super.insertRow(LOGGER_DB_TABLENAME, mapping);
	    }
	    
	    /**
	     * Deletes all logs from the database.
	     */
	    protected void deleteAll() {
	    	try {
	    		SQLiteDatabase db = openForWriting();
	    		db.delete(LOGGER_DB_TABLENAME, null, null);
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Deleting all from Logging DB", ex, false);
	    	}
	    }
	    
	}
}
