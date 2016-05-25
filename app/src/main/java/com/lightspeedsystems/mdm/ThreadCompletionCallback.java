package com.lightspeedsystems.mdm;

/**
 * Callback for thread and process completion processing.
 */
public interface ThreadCompletionCallback {
	/**
	 * Called when a thread completes, within the thread's thread context.
	 * @param obj optional parameter passed back to the implementor. 
	 */
	public void onThreadComplete(Object obj);
}
