package com.lightspeedsystems.mdm;

//import com.lightspeedsystems.mdm.util.LSLogger;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class Event {
	//private final static String TAG = "Event";
	
	/** The event represents a control action.*/
	public final static int EVENTTYPE_control = 1;
	/** The event represents a profile-related action, such as setting passcode policy. */
	public final static int EVENTTYPE_profile = 2;
	/** The event represents an application-related action, such as installing an app. */
	public final static int EVENTTYPE_app     = 3;
	/** The event represents a configuration-related action, including enrollment. */
	public final static int EVENTTYPE_config  = 4;
	
	public final static int EVENTACTION_add = 1;     // something was added
	public final static int EVENTACTION_change = 2;  // something was changed
	public final static int EVENTACTION_delete = 3;  // removal of something
	public final static int EVENTACTION_update = 5;  // mainly for apps, updating to a newer version

	public final static int EVENTACTION_execute = 9; // execution of a control command or action
	
	private int dbID; 
	private int type;
	private int action;
	private int resourceID; // if there should be a string associated with the event, this is the resource ID for it. 0 if not used.
	private String resourceIDname; // the strin name of the resource id
	private String name;
	private String description;
	private long eventTime;
	private Drawable icon;
	private String displayStr; // 

	
	public static void log(int type, int action, int resourceID, int artifactResID) {
		Controller.getInstance().getEventsInstance().logEvent( type, action, resourceID, 
				Utils.getApplicationContext().getResources().getString(artifactResID), null);
	}
	
	/**
	 * Convenience method for creating a new event and logging it.
	 * @param type
	 * @param action
	 * @param resourceID
	 * @param artifactName
	 * @param description
	 * @param time
	 */
	public static void log(int type, int action, int resourceID, String artifactName, String description) {
		Controller.getInstance().getEventsInstance().logEvent( type, action, resourceID, artifactName, description);
	}

	
	public Event(int dbID, int type, int action, int resourceID, String artifactName, String description, long time) {
		this.dbID = dbID;
		this.type = type;
		this.action = action;
		this.resourceID = resourceID;
		this.description = description;
		name = artifactName;
		eventTime = time;
		
		if (eventTime == 0)
			eventTime = System.currentTimeMillis();
		
		// get icon for the action and/or type:
		int iconResID = 0;
		switch (action) {
			case EVENTACTION_add: iconResID = android.R.drawable.ic_menu_add; break;
			case EVENTACTION_change: 
			case EVENTACTION_update: iconResID = android.R.drawable.ic_menu_edit; break;
			case EVENTACTION_delete: iconResID = android.R.drawable.ic_menu_delete; break;
			case EVENTACTION_execute: iconResID = android.R.drawable.ic_menu_manage; break;
		}
		if (iconResID != 0)
			icon = Utils.getApplicationContext().getResources().getDrawable(iconResID);
		
	}
	
	public int getDBID() {
		return dbID;
	}
	
	public int getType() {
		return type;
	}
	public int getAction() {
		return action;
	}
	public int getResourceID() {
		return resourceID;
	}
	public String getResourceIDname() {
		if (resourceID != 0) {
			Resources res = Utils.getApplicationContext().getResources();
			resourceIDname = res.getResourceName(resourceID);
			//LSLogger.debug(TAG, "getResourceIDname = "+resourceIDname+" resid="+resourceID);
		}
		return resourceIDname;
	}
	public void setResourceIDname(String resourceName) {
		resourceIDname = resourceName;
		Resources res = Utils.getApplicationContext().getResources();
		resourceID = res.getIdentifier(resourceName, null, null);
		//LSLogger.debug(TAG, "setResourceIDname = "+resourceName+" resid="+resourceID);
	}
	public long getEventTime() {
		return eventTime;
	}
	public String getObjectName() {
		return name;
	}
	public String getDescription() {
		return description;
	}
	
	public Drawable getIcon() {
		return icon;
	}
	
	public String getEventTimeAsString() {
		return Utils.formatLocalizedDateGMT(eventTime);
	}
	
	public String toString() {
		if (displayStr == null)
			buildDisplayString();
		return displayStr;
	}
	
	private void buildDisplayString() {
		String resStr = null;
		if (resourceID != 0)
			resStr = Utils.getApplicationContext().getResources().getString(resourceID);
		if (description == null) 
			description = resStr;
		else 
			description = resStr + " " + description;
		
		displayStr = (name==null?"":name) + ": " + (description==null?"":description);
	}
	
}
