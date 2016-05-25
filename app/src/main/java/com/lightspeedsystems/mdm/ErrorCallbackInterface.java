package com.lightspeedsystems.mdm;

/**
 * Interface for reporting errors from running tasks.
 * Intended to define the common interface methods that can be supported by various implementations
 * that will show progress, log status, etc.
 */
public interface ErrorCallbackInterface {
	public static int ERRORTYPE_FATAL = 1;
	
	/**
	 * Handles a error callback with a string error message and/or title.
	 * @param errorTye - an Constants.ERROR_TYPE constant value
	 * @param errorMessage - error message, or null.
	 * @param errorTitle - string to show as a message title, or null if not needed.
	 */
	public void onErrorCallback(int errorType, String errorTitle, String errorMessage);
	
}
