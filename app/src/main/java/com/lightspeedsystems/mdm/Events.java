package com.lightspeedsystems.mdm;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lightspeedsystems.mdm.util.LSLogger;

public class Events {
	private static String TAG = "Events";
	
	private Vector<Event> events;
	private Context context;
	private EventsDataChangeListener dataObserver; // observer for watching for data changes; note that we should use a list of these.
	private boolean bEventsLoaded;
	
	/**
	 * Constructor. Events will be loaded or added in the current thread. 
	 * @param context application context, needed for getting event data.
	 */
	public Events(Context context) {
		this.context = context;
		events = new Vector<Event>(10);
		bEventsLoaded = false;
	}
	
	/**
	 * Adds the observer for data change notifications.
	 * @param observer instance to call back to upon changes to data.
	 */
	public void registerDataChangeListener(EventsDataChangeListener observer) {
		dataObserver = observer;
	}
	
	/**
	 * Removes the observer for data change notifications.
	 * @param observer instance to call back to upon changes to data.
	 */
	public void unregisterDataChangeListener(EventsDataChangeListener observer) {
		dataObserver = null;
	}

	/**
	 * Returns true if the events have been loaded from storage (database), false if not.
	 */
	public boolean isEventsLoaded() {
		return bEventsLoaded;
	}
	
	/**
	 * Gets the count of the number of apps in the list.
	 * @return number of app instances, 0 if none.
	 */
	public int getEventsCount() {
		return events.size();
	}
	
	/**
	 * Gets the list of events. Note that the list may be empty if nothing was done to read them or no events exist.
	 * @return
	 */
	public List<Event> getEvents() {
		return events;
	}
	
	/**
	 * Gets the Event at the specified index position.
	 * @param index 0-based index into the collection
	 * @return the Event instance at the location, 
	 * or null of index is out of bounds or no event exists at that index.
	 */
	public Event getEventAt(int index) {
		Event e = null;
		if (events==null || events.isEmpty() || index >= events.size() || index < 0)
			e = null;
		else {
			try {
				e = events.elementAt(index);
			} catch (Exception ex) {
				LSLogger.exception(TAG, "getElementAt error:", ex);
			}
		}
		return e;
	}
	
	/**
	 * Adds the Event instance to the list of applications.
	 * @param event Event instance
	 */
	public void addEvent(Event event) {
		events.insertElementAt(event, 0);  // add to the front of the list
	}
	
	/**
	 * Creates and adds a new event.
	 * @param eventType an EVENT.EVENTTYPE_ value
	 * @param eventAction an EVENT.EVENTACTION_ value
	 * @param displayResourceID description details to use, as a resource. Pass in 0 to use a details parameter.
	 * @param artifactName object name of what was affected or done, such as "Application" or "Passcode".
	 * @param details description about the event
	 */
	public void logEvent(int eventType, int eventAction, int displayResourceID, String artifactName, String details) {
		// add to current events list:
		Event event = new Event(0, eventType, eventAction, displayResourceID, artifactName, details, 0);
		addEvent(event); 
		// persist it:
		try {
			EventsDB db = new EventsDB(context);
			db.insert(event);
			db.close();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "LogEvent error: ", ex);
		}
		notifyObservers();
	}
	
	/**
	 * Adds a new event.
	 * @param event Event instance to add.
	 */
	public void logEvent(Event event) {
		// add to current events list:
		addEvent(event); 
		// persist it:
		try {
			EventsDB db = new EventsDB(context);
			db.insert(event);
			db.close();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "LogEvent error: ", ex);
		}
		notifyObservers();
	}
	
	/**
	 * Reads all MDM events.
	 * @return list of Event instances, each instance representing an event.
	 */
	public List<Event> getAllEvents() {
		Vector<Event> e = new Vector<Event>(10);
		try {
			EventsDB db = new EventsDB(context);
			db.readEventRecords(e, DBStorage.DBORDER_Descending, null);
			db.close();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "GetAllEvents error: ", ex);
		}
		return e;
	}
	
	/**
	 * Loads or reloads all MDM events, storing them in the internal list instance.
	 * @param forceRelaod if true, forces the contents to be reloaded. Otherwise, this
	 * will only load if not already loaded.
	 * @return list of Event instances, each instance representing an event.
	 */
	public synchronized void loadEvents(boolean forceReload) {
		if (!bEventsLoaded || forceReload) {
			try {
				EventsDB db = new EventsDB(context);
				events.clear();
				db.readEventRecords(events, DBStorage.DBORDER_Descending, null);
				bEventsLoaded = true;
				db.close();
			} catch (Exception ex) {
				LSLogger.exception(TAG, "LoadEvents error: ", ex);
			}
			LSLogger.debug(TAG, "Loaded " + events.size() + " Events.");
		} else {
			LSLogger.debug(TAG, "Events already loaded. Using cached Events list.");
		}
	}
	
	public void deleteAllEvents() {
		try {
			EventsDB db = new EventsDB(context);
			db.deleteAll();
			db.close();
			events.clear();
		} catch (Exception ex) {
			LSLogger.exception(TAG, "DeleteEvents error: ", ex);
		}
		notifyObservers();
	}


	/**
	 * Notifies any observers of a change in the data.
	 */
	private void notifyObservers() {
		if (dataObserver != null) {
			LSLogger.info(TAG, "notifying dataObserver of EventsDataChanged");
			dataObserver.onEventsDataChanged();
		}
	}
	
	/**
	 * Callback interface for notification of event data changes.
	 *
	 */
	public interface EventsDataChangeListener {
		public void onEventsDataChanged();
	}
	
	
	/**
	 * Static method to get the SQL used to create the Events database table.
	 * @return the SQL string used to create the table.
	 */
    public static String getEventsSqlCreateTable() {
    	return EventsDB.getSqlCreateTable();
    }

	
	// -----------------------------------------------------------------------
	// --- Inner-class for handling Event persistence in a sqlite database  --
	// -----------------------------------------------------------------------
	/**
     * A SQLite database is used to store Events in the lsevents table.
	 * 
	 * Rows are added for each event message. Each item has a long timestamp value that
	 * is used as the key, and also for sorting chronologically. 
	 * Each row also includes: 
	 * logtype - defines the type of message as warning, error, info, debug, etc (1-letter e,w,i,d)
	 * tid - thread ID the message was generated from; useful for multithreading diagnosis
	 * source - class where the message originated
	 * msg - message logged. For exceptions, this includes the exception details.	
	 */
	private static class EventsDB extends DBStorage {
		private final static String EVENT_DB_TABLENAME = "lsevents";
		private final static String COLUMN_ID     = "id";
		private final static String COLUMN_TIME   = "evttime";
		private final static String COLUMN_TYPE   = "evttype";
		private final static String COLUMN_ACTION = "evtaction";
		private final static String COLUMN_RESID  = "resid"; // string name of a resource id.
		private final static String COLUMN_OBJECT = "object";
		private final static String COLUMN_DESC   = "description";
				
		// SQL command used to create the log entries table:
		private final static String EVENT_DB_CREATE_SQL =  
                "CREATE TABLE " +EVENT_DB_TABLENAME + " (" +
                		COLUMN_ID     + " INTEGER PRIMARY KEY, " +
                		COLUMN_TIME   + " LONG, " +
                		COLUMN_TYPE   + " INTEGER, " +
                		COLUMN_ACTION + " INTEGER, " +
                		COLUMN_RESID  + " TEXT, " +
                		COLUMN_OBJECT + " TEXT, " +
                		COLUMN_DESC   + " TEXT);";
		// index locations of the data in the query, for improved processing speeds:
		private final static int COLUMN_ID_INDEX     = 0;
		private final static int COLUMN_TIME_INDEX   = 1;
		private final static int COLUMN_TYPE_INDEX   = 2;
		private final static int COLUMN_ACTION_INDEX = 3;
		private final static int COLUMN_RESID_INDEX  = 4;
		private final static int COLUMN_OBJECT_INDEX = 5;
		private final static int COLUMN_DESC_INDEX   = 6;

		// SQL command to read all rows from the database
		private final static String EVENT_DB_QUERY_GETALL_desc =
				"select * from " + EVENT_DB_TABLENAME + " order by " + COLUMN_TIME + " desc;";
		private final static String EVENT_DB_QUERY_GETALL_asc =
				"select * from " + EVENT_DB_TABLENAME + " order by " + COLUMN_TIME + " asc;";
		// SQL to get rows for a specific type
		private final static String EVENT_DB_QUERY_GETTYPE_desc =
				"select * from " + EVENT_DB_TABLENAME + " where " + COLUMN_TYPE + "=? order by " + COLUMN_TIME + " desc;";
		private final static String EVENT_DB_QUERY_GETTYPE_asc =
				"select * from " + EVENT_DB_TABLENAME + " where " + COLUMN_TYPE + "=? order by " + COLUMN_TIME + " asc;";
		  
		
		/**
		 * Creates Database persistence instance, where data will be stored.
		 * @param context
		 */
		public EventsDB(Context context) {
	        super(context);
	        setLoggingPersistence(false);
		}

		/**
		 * Abstract method to get the name of the database table.
		 * @return
		 */
	    public String getSqlTableName() {
	    	return EVENT_DB_TABLENAME;
	    }

		/**
		 * Static method to get the SQL used to create the Events database table.
		 * @return the SQL string used to create the table.
		 */
	    public static String getSqlCreateTable() {
	    	return EVENT_DB_CREATE_SQL;
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
	        map.put(COLUMN_TIME,  COLUMN_TIME);
	        map.put(COLUMN_TYPE,  COLUMN_TYPE);
	        map.put(COLUMN_ACTION,COLUMN_ACTION);
	        map.put(COLUMN_RESID, COLUMN_RESID);
	        map.put(COLUMN_OBJECT,COLUMN_OBJECT);
	        map.put(COLUMN_DESC,  COLUMN_DESC);
	        return map;
	    }
	    
	    // -- data management methods --
	    
	    protected void closeDB() {
	    	super.closeDB();
	    }
	    
	    protected long readEventRecords(Vector<Event> list, int sortOrder, String msgTypeFilter) {
	    	int result = -1;
	        Cursor cursor = null;
	    	try {
	    		SQLiteDatabase db = openForReading();
	    		String sql;
	    		String[] args = null;
	    		if (msgTypeFilter == null) {
	    			sql = ((sortOrder==DBORDER_Descending) ? 
	    				EVENT_DB_QUERY_GETALL_desc : EVENT_DB_QUERY_GETALL_asc);
	    		} else {
	    			sql = ((sortOrder==DBORDER_Descending) ? 
	    					EVENT_DB_QUERY_GETTYPE_desc : EVENT_DB_QUERY_GETTYPE_asc);
	    			args = new String[] { msgTypeFilter };	    			
	    		}
	    		//debug:
	    		LSLogger.debug(TAG, "SQL=" + sql);
	    		LSLogger.debug(TAG, "args=" + ((args==null)?"null":args.toString()));
	    		
	    		cursor = db.rawQuery(sql, args);
	    		
	    		if (cursor != null && cursor.moveToFirst()) {
	    			result = 0;
	                do {
	                    Event item = new Event(cursor.getInt(COLUMN_ID_INDEX),
	                    		cursor.getInt(COLUMN_TYPE_INDEX),
	                    		cursor.getInt(COLUMN_ACTION_INDEX),
	                    		0,
	                    		cursor.getString(COLUMN_OBJECT_INDEX),
	                    		cursor.getString(COLUMN_DESC_INDEX),
	                    		cursor.getLong(COLUMN_TIME_INDEX));
	                    item.setResourceIDname( cursor.getString(COLUMN_RESID_INDEX) );
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
	    protected long insert(Event item) {
	    	ContentValues mapping  = new ContentValues(6);
	    	mapping.put(COLUMN_TIME, 	Long.valueOf(item.getEventTime()));
	    	mapping.put(COLUMN_TYPE, 	Integer.valueOf(item.getType()));
	    	mapping.put(COLUMN_ACTION,  Integer.valueOf(item.getAction()));
	    	mapping.put(COLUMN_RESID, 	item.getResourceIDname());
	    	mapping.put(COLUMN_OBJECT,  item.getObjectName());
	    	mapping.put(COLUMN_DESC,  	item.getDescription());
	    	return super.insertRow(EVENT_DB_TABLENAME, mapping);
	    }
	    
	    /**
	     * Deletes all logs from the database.
	     */
	    protected void deleteAll() {
	    	try {
	    		SQLiteDatabase db = openForWriting();
	    		db.delete(EVENT_DB_TABLENAME, null, null);
	    	} catch (Exception ex) {
	    		LSLogger.exception(TAG, "Error Deleting all Events from DB: ", ex);
	    	}
	    }
	    
	}

	
}
