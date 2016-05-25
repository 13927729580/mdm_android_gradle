package com.lightspeedsystems.mdm;

/**
 * Interface for handling status and progress updates from running tasks.
 * Intended to define the common interface methods that can be supported by various implementations
 * that will show progress, log status, etc.
 */
public interface ProgressCallbackInterface {

	/**
	 * Handles a string status message
	 * @param message - text message to handle.
	 */
	public void statusMsg(String message);
	
	/**
	 * Handles progress of a task
	 * @param message
	 * @param percentage 0-100 of completion 
	 */
	public void progressMsg(String message, int percentage);
}
