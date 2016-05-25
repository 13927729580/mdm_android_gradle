package com.lightspeedsystems.mdm;

// command result, used to handle various processing results and execution control needs. 

public class CommandResult {
	private boolean abort;				// when true, stops further commands processing (no more commands or doing a wipe clean)
	private boolean pending;			// when true, the command was deferred/queued-up for processing; refer to success flag if it started ok.
	private boolean success;			// true if the command was processed and completed successfully, false if something failed or is not done.
	private boolean waitForCompletion;	// true if this command needs more time or user interaction to be able to complete 
	private long completionWaitTime;	// if waiting for completion, the time in seconds to wait. -1 means indefinitely-wait
	private int errorCode;				// internal error code
	private String errorMessage;		// information about why the command did not complete or why it failed
	private Exception exception;		// if an exception occurred, this is what was caught.
	
	public CommandResult() {
	}

	/**
	 * Creates an instance from an HttpCommResponse result.
	 * @param hresp
	 */
	public CommandResult(HttpCommResponse hresp) {
		if (hresp != null) {
			setSuccess(hresp.isOK());
			if (!success) { // there was an error; get the details:
				setErrorCode(hresp.getResultCode());
				if (hresp.hasException())
					setException(hresp.getException());
				else 
					setErrorMessage(hresp.getResultReason());
			}
		}
	}
	
	/**
	 * Sets exception and related error information.
	 * @param ex the exception
	 */
	public void setException(Exception ex) {
		exception = ex;
		String msg = ex.getMessage();
		if (msg == null)
			msg = ex.toString();
		errorMessage = "Error: " + msg;
		success = false;
	}
	
	public Exception getException() {
		return exception;
	}


	public boolean isAbort() {
		return abort;
	}

	public void setAbort(boolean abort) {
		this.abort = abort;
	}

	public boolean isPending() {
		return pending;
	}

	public void setPending(boolean pending) {
		this.pending = pending;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public boolean isWaitForCompletion() {
		return waitForCompletion;
	}

	public void setWaitForCompletion(boolean waitForCompletion) {
		this.waitForCompletion = waitForCompletion;
	}

	public long getCompletionWaitTime() {
		return completionWaitTime;
	}

	public void setCompletionWaitTime(long completionWaitTime) {
		this.completionWaitTime = completionWaitTime;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public boolean hasErrorMessage() {
		return (errorMessage!=null);
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
		success = false;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder((success?"OK":"Fail-"));
		if (!success && errorMessage != null)
			sb.append(errorMessage);
		if (abort)
			sb.append(" abort=true");
		return sb.toString();
	}
	
}
