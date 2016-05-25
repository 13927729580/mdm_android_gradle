package com.lightspeedsystems.mdm.util;

/**
 * Generic interface for notification of a data change.
 */
public interface DataChangeListener {
	/**
	 * Callback for notifying when a data change event has occurred.
	 * @param identifier optional string for identifying the data; can be null if not needed.
	 * @param newValue optional new value; can be null if not needed. 
	 */
	public void dataChanged(String identifier, String newValue);

}
