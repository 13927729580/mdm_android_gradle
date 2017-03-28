package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;
import java.util.List;
import java.util.Vector;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Handles processing of various commands to do something, including commands received from the MDM server.
 */
public class CommandProcessor {

	private final static String TAG = "CommandProcessor";
	
	/**
	 * Processes a received GCM Intent. Typically, this is the result of a received message or 
	 * as a result of a registration message from GCM.
	 * @param gcmIntent Intent received by the app. For notification messages, the data value has the details.
	 */
	public static void handleGcmCommand(Intent gcmIntent) {
		// check for message key:
		//String msg = gcmIntent.getStringExtra("msg");
		// check for command key:
		String data = gcmIntent.getStringExtra(MdmFirebaseMessagingService.EXTRA_MSGDATA);
		if (data == null)
			data = gcmIntent.getStringExtra(Constants.CMD_datatag);
		
		if (data != null) { // Process cmd action within the GCM payload message:
			LSLogger.info(TAG, "Received intent data="+data);
			try {
				JSONObject jdata = new JSONObject(data);
				if (jdata.has(Constants.CMD_cmdtag)) {
					// Note: this directly-calls command processing; some commands are really best-suited
					// for processing later and separately, while some should be immediate.
					// (For example, a command to download an install an app should not be processed here, 
					//  because the amount of time needed to handle the command can be lengthy; those type of
					//  commands are usually queued up on the server and are obtained during wake-up processing.)
					// Generally though, commands received through GCM will only be 'wake-up' and maybe
					// any urgent-immediate commands, so this should be OK.
					processMDMCommand(jdata.getString(Constants.CMD_cmdtag), jdata, null);
					
				} else {
					LSLogger.error(TAG, "Unknown GCM message: "+data);
				}
			} catch (JSONException jex) {
				LSLogger.error(TAG, "JSON Exception parsing string '" + data + "'. Error: " + jex.getMessage());
			}
		} else {
			LSLogger.error(TAG, "Unknown intent received: action=" + gcmIntent.getAction());
		}
	}
	
	/* Process a MDM command initiated by the MDM server. 
	 * @param deferredCmds if not null is used to store commands that need to be deferred to later; if this is null, 
	 * processes the command right away.  This is intended for commands such as wipe, where the wipe needs to be done
	 * but after all other commands (and sending results to server) have completed.
	 * */
	
	private static CommandResult processMDMCommand(String cmd, JSONObject jdata, List<MdmDeferredCommand> deferredCmds)
	{
		CommandResult results = null;
		try { // wrap everything in this exception handler to ensure robustness.
		  if (cmd != null) {
			results = new CommandResult();
			LSLogger.debug(TAG, "Processing command " + cmd);
			
			if (cmd.equalsIgnoreCase(Constants.CMD_WAKEUP)) {
				//start the processing to read/process commands to be read in from the server
				// simply tell the controller that it needs to do it:
				Controller.getInstance().startProcessServerCommands();
				results.setSuccess(true);

			} else if (cmd.equalsIgnoreCase(Constants.CMD_CHECKIN)) {
				// initiate a check-in. Queue the request to make it to happen.
				Controller.getInstance().requestServerSync(false, null);
				results.setSuccess(true);
				
			} else if (cmd.equalsIgnoreCase(Constants.CMD_LOCKNOW)) {
				Controller.getInstance().getDeviceAdmin().lockNow();
				results.setSuccess(true);
				LSLogger.debug(TAG, "Locked the device.");
				Event.log(Event.EVENTTYPE_control, Event.EVENTACTION_execute, 
						R.string.event_devicelock, R.string.device);

			} else if (cmd.equalsIgnoreCase(Constants.CMD_SETPROFILE)) {
				Profiles profiles = new Profiles(Controller.getContext());
				if (jdata.has(Constants.CMD_profiletag)) {
					results = profiles.setProfile(jdata.getJSONObject(Constants.CMD_profiletag));
					Event.log(Event.EVENTTYPE_control, Event.EVENTACTION_execute, 
						R.string.event_profileset, R.string.profile);
				} else {// create error for response to server
					setLabelMissingError(results, cmd, Constants.CMD_profiletag);
				}
								
			} else if (cmd.equalsIgnoreCase(Constants.CMD_RESETPASSWORD)) {
				if (jdata.has(Constants.CMD_passwordtag)) {
					String newPassword = jdata.getString(Constants.CMD_passwordtag);
					// ..decode password...to do...
					boolean bRC = Controller.getInstance().getDeviceAdmin().resetPassword(newPassword);
					results.setSuccess(bRC);
					LSLogger.debug(TAG, (bRC?"Passcode changed.":"Failed to change passcode.") );
					Event.log(Event.EVENTTYPE_control, Event.EVENTACTION_execute, 
							R.string.notif_passcodereset, R.string.passcode);
					Utils.showNotificationMsg(R.string.notif_passcodereset);
				} else {
					setLabelMissingError(results, cmd, Constants.CMD_passwordtag);
				}
				
			} else if (cmd.equalsIgnoreCase(Constants.CMD_CLEARPASSWORD)) {
				boolean bRC = Controller.getInstance().getDeviceAdmin().removePassword();
				results.setSuccess(bRC);
				if (bRC) {
					LSLogger.debug(TAG, "Passcode cleared.");
					Event.log(Event.EVENTTYPE_control, Event.EVENTACTION_execute, 
							R.string.notif_passcodecleared, R.string.passcode);
					Utils.showNotificationMsg(R.string.notif_passcodecleared);
				} else {
					LSLogger.debug(TAG,"Failed to remove passcode.");
				}
				
			} else if (cmd.equalsIgnoreCase(Constants.CMD_EXPIREPASSWORDNOW)) {
				boolean bRC = Controller.getInstance().getDeviceAdmin().expirePasswordImmediate();
				results.setSuccess(bRC);
				LSLogger.debug(TAG, "Passcode set to expire immediately.");
				Event.log(Event.EVENTTYPE_control, Event.EVENTACTION_execute, 
						R.string.event_passcodeexpirenow, R.string.passcode);
				Utils.showNotificationMsg(R.string.notif_passcodeexpired);
				
/*			} else if (cmd.equalsIgnoreCase(Constants.CMD_EXPIREPASSWORDAFTER)) {
				long minutes = 0;
				if (jdata.has(Constants.CMD_value)) {
					try {
	// ---- NOTE --- move this to within Profiles, as it is techincally a PasscodePolicy that needs 
	// to have its previous values logged...
						minutes = jdata.getInt(Constants.CMD_value);
						boolean bRC = Controller.getInstance().getDeviceAdmin().expirePasswordAfter(minutes);
						results.setSuccess(bRC);
						LSLogger.debug(TAG, (bRC?"Passcode expires in "+minutes+" minutes.":"Failed to expire passcode.") );
						//if (minutes == 0)
						//	Utils.showNotificationMsg(R.string.notif_passcodeexpired);

					} catch (Exception jex) {
						LSLogger.exception(TAG, "Password Expirtion Policy Exception: ", jex);
						results.setException(jex);
					}
				} else {
					results.setErrorMessage("Passcode Policy Timeout error: missing 'value' parameter.");
					LSLogger.error(TAG, results.getErrorMessage());
				}
*/				
				
								
			} else if (cmd.equalsIgnoreCase(Constants.CMD_WIPECLEAN)) {
				if (deferredCmds != null) {
					// defer this action until either the response was sent to server or until all other things are done.
					deferredCmds.add( new MdmDeferredCommand(cmd, jdata, Event.EVENTTYPE_control) );
					results.setSuccess(true);
					results.setPending(true);
					LSLogger.debug(TAG, "Queued request to wipe the device.");
				} else { // execute the wipe right away.
					Controller.getInstance().getDeviceAdmin().wipeAll();
					results.setSuccess(true);
					LSLogger.debug(TAG, "Wiped the device.");
				}

				
			// --- handle non-control commands: these can be queued up for later processing, after all other 
			//      possible action/control commands have been handled. 
			} else if (cmd.equalsIgnoreCase(Constants.CMD_INSTALLAPP) || 
					   cmd.equalsIgnoreCase(Constants.CMD_UNINSTALLAPP)) {
				// install or uninstall an app; queue it for handling the action.
				if ( Controller.getInstance().enqueueManagedAppAction(cmd, jdata, results, true) ) {
					results.setPending(true);  // the install or uninstall was scheduled or is starting
					results.setSuccess(true);
				}
			
			// handle request to get list of apps:
			} else if (cmd.equalsIgnoreCase(Constants.CMD_GETAPPS)) {
				boolean getAll = false;
				if (jdata.has(Constants.CMD_GETAPPS_ALL)) 
					getAll = jdata.getBoolean(Constants.CMD_GETAPPS_ALL);
				Controller.getInstance().requestAppsListSync(getAll, null);
				results.setSuccess(true);
				
			// handle self-updates to this mdm app:
			} else if (cmd.equalsIgnoreCase(Constants.CMD_UPDATE)) {
				Controller.getInstance().requestUpdateCheck(null);

			// set local app configuration settings:
			} else if (cmd.equalsIgnoreCase(Constants.CMD_SETCONFIG)) {
				// the json consists of a config {} object that holds the configuration data; get it
				// and pass it to the settings instance for parsing/processing:
				if (jdata.has(Constants.CMD_configtag)) {
					JSONObject jcfg = jdata.getJSONObject(Constants.CMD_configtag);
					if (jcfg.has(Constants.CMD_errortag)) {
						results.setErrorMessage(jcfg.getString(Constants.CMD_errortag));
					} else {
						Controller.getInstance().getSettingsInstance().setValues(jcfg, results);
						Event.log(Event.EVENTTYPE_config, Event.EVENTACTION_update, 
								R.string.event_configset, R.string.config);
					}
				} else {// create error for response to server
					setLabelMissingError(results, cmd, Constants.CMD_configtag);
				}				

			} else if (cmd.equalsIgnoreCase(Constants.CMD_UNENROLL)) {
				Controller.getInstance().unenroll();
				results.setSuccess(true);
				results.setAbort(true);
	
			} else {
				results.setErrorMessage("Unknown command received: " + cmd);
			}
			
			LSLogger.debug(TAG, "Processed command '" + cmd + "' - results: " + results.toString());
		  }
		} catch (Exception ex) {
			LSLogger.exception(TAG, "processMDMcommand exception for '" + (cmd==null?"null":cmd) +"': ", ex);
			if (results!=null)
				results.setException(ex);
		}
		return results;
	}

	/**
	 * Performs a device enrollment request to server, then handles the response which should
	 *  consist of a "config" set of data relevant to the organization.
	 * To enroll, we send an enrollment code to the server, add a .json to the url string, and
	 *  as a response, expect a set of organization-specific config data, which is applied into
	 *  settings to set various org-specific values, including org and group info.
	 * @param controller Controller instance this was invoked from
	 * @return CommandResult containing the results of the enrollment processing.
	 */
	public static CommandResult processEnrollmentToServer(Controller controller) {
		String serverUrl = ServerUrlProvider.getEnrollToServerUrl(controller.getSettingsInstance());
		ServerComm serverComm = new ServerComm();
		HttpCommResponse resp = serverComm.getFromServer(serverUrl, null);
		CommandResult result = new CommandResult(resp);

		// if we got a good response, we should have a json result set with configuration data.
		if (resp.isOK()) {
			// now process the result by setting the configuration data:
			try {
				JSONObject jcfg = new JSONObject(resp.getResultStr());
				if (jcfg.has(Constants.CMD_errortag)) {
					result.setErrorMessage(jcfg.getString(Constants.CMD_errortag));
				} else {
					controller.getSettingsInstance().setValues(jcfg, result);
					Event.log(Event.EVENTTYPE_config, Event.EVENTACTION_add, 
						R.string.event_configset, R.string.config);
				}
			} catch (Exception ex) {
				LSLogger.exception(TAG, "ProcessEnrollmentToServer error:", ex);
			}
			
		} //else   // there was a server error; data was already set into result during its creation from resp.
		return result;
	}
	
	// Helper method for setting error information into results when a required tag or label is missing in the command. 
	// This is a server-destined error and does not need translating.
	private static void setLabelMissingError(CommandResult result, String cmd, String label) {
		result.setErrorMessage("Invalid command format for " + cmd + ": " + Constants.CMD_profiletag + " label is missing.");	
	}
	
		
	// Inner class for defining a deferred command for later processing.
	private static class MdmDeferredCommand {
		String cmd;
		JSONObject json;
		@SuppressWarnings("unused")
		int deferredType;  // type of the 'event', and Event type value
		
		public MdmDeferredCommand(String command, JSONObject json, int eventType) {
			cmd = command;
			this.json = json;
			deferredType = eventType;
		}
	}
	
	/**
	 * Adds command results json-entry for a command to the json array.
	 * @param jarray results collection to add to.
	 * @param jCmd json data from the command. Gets identification values from it if present.
	 * @param cmdresult
	 */
	protected static void addCommandResult(JSONArray jarray, JSONObject jCmd, CommandResult cmdresult) {
		if (cmdresult!=null && jarray!=null && jCmd != null) {
		  try {
			String prevCommandID = null; // identifier of the previous command for chaining calls together one after another.
			String prevActivityID = null; // identifier of the previous activity for chaining calls together one after another.

			if (jCmd.has(Constants.CMD_activitytag))
				prevActivityID = jCmd.getString(Constants.CMD_activitytag);
			else if (jCmd.has(Constants.CMD_commandtag))
				prevCommandID = jCmd.getString(Constants.CMD_commandtag);
			
			JSONObject jentry = new JSONObject();

			/**  -- use strings as the values instead of integers -- */
			if (prevActivityID != null)
				jentry.put(Constants.CMD_activitytag, prevActivityID);
			else if (prevCommandID != null)
				jentry.put(Constants.CMD_commandtag, prevCommandID);
			/**/
			
			/**  -- use integers as the values instead of strings -- * 
			try {
				if (prevActivityID != null)
					jentry.put(Constants.CMD_activitytag, Integer.parseInt(prevActivityID));
				else if (prevCommandID != null)
					jentry.put(Constants.CMD_commandtag, Integer.parseInt(prevCommandID));
			} catch (Exception ex) {
				LSLogger.exception(TAG, "AddCommandResult Integer conversion error:", ex);
			}
			 **/
			
			// if we added an identifier to the result, add the success/fail result details
			if (jentry.length() > 0) {
				if (cmdresult.isPending()) {
					jentry.put(Constants.CMD_successtag, Constants.CMD_pendingvalue);
				} else if (cmdresult.isSuccess()) {
					jentry.put(Constants.CMD_successtag, Constants.CMD_successvalue);
				} else {
					jentry.put(Constants.CMD_successtag, Constants.CMD_failedvalue);
					String errormsg = cmdresult.getErrorMessage();
					if (errormsg != null)
						jentry.put(Constants.CMD_errortag, errormsg);
				}
				// and add the json object to the given results array
				jarray.put(jentry);
			}
				
		  } catch (Exception ex) {
			LSLogger.exception(TAG, "addCommandResult:", ex);
		  }
		}
	}
	
	protected static void addIdentifierValuesToJSON(JSONObject json) {
		if (json != null) {
			try {
				json.put(Constants.PARAM_DEVICE_TYPE, Constants.DEVICE_TYPE_ANDROID);
				String deviceID = Controller.getInstance().getDeviceInstance().getDeviceID();
				json.put(Constants.PARAM_DEVICE_UDID, deviceID);		
			} catch (Exception ex) {
				LSLogger.exception(TAG, ex);
			}
		}
	}
	
	/**
	 * Sends the results data to the results server.
	 * @param settings Application Settings instance.
	 * @param results array of Activity result values to send to the results servers.
	 * @return true if the data was sent, false if an error occurred or data not sent.
	 */
	protected static boolean sendServerCommandResults(Settings settings, JSONArray results) {
		boolean bResult = false;
		if (settings.isSendCmdResultsToServer()) {
			String serverCmd = "command_dispatch/android";
			
			String resultsUrl = settings.getServerResultsUrl();
			resultsUrl = Utils.endStringWith(resultsUrl, "/") + serverCmd;
			
			if (results != null && results.length() > 0) {
				try {
					// build results data:
					JSONObject jdata = new JSONObject();
					addIdentifierValuesToJSON(jdata);
					jdata.put(Constants.CMD_activities, results);
					jdata.put(Constants.PARAM_REPORT_RESULTS, Constants.CMD_value_true);
					
					ServerComm serverComm = new ServerComm();				
					HttpCommResponse response = serverComm.postToServer(resultsUrl, jdata);
					if (response.isOK()) {
						bResult = true;
						LSLogger.debug(TAG, "SendServerCommandResults ok.");
					} else {
						LSLogger.error(TAG, "SendServerCommandResults error: " + response.getResultStr());
					}
					
				} catch (JSONException jex) {
					LSLogger.exception(TAG, "JSON Exception sending server results:", jex);
				} catch (Exception ex) {
					LSLogger.exception(TAG, "SendServerResults error:", ex);
				}			
			}
		}
		return bResult;
	}
	
	// processes the commands from a server. 
	// This runs from within the given Controller's thread.
	// returns false if all things are complete, true if things didnt finish.
	/*
	 * The general logic is this:
	 * do get commands from server
	 *   do get each command in the results from server
	 *     process the command, save results in results objects
	 *   while not done with processing each command from the server results
	 * while not done
	 * process any pending actions
	 */
	public static boolean processServerCommands(Controller controller) {
		LSLogger.debug(TAG, "Begin reading server commands...");
		
		boolean done = false;
		Vector<MdmDeferredCommand> deferredCommands = new Vector<MdmDeferredCommand>();
		Settings settings = controller.getSettingsInstance();
		String deviceUDID = settings.getDeviceUdid();
		
		// get the full url to send to the server for querying the next commands:
		String serverUrl = ServerUrlProvider.getCommandsQueryUrl(settings);		
		
		ServerComm serverComm = new ServerComm();
		
		CommandResult cmdresult = null; // previous command's processing result; is used to pass results to server
		
		do {
			JSONArray jsonResults = new JSONArray();
			try {
				done = true;
				
				// get commands from server; if we have previous results from prior iteration through
				//  this loop, we can include those in the query:
				
				///JSONObject jparams = null;  // for command parameters; not used on the first pass through.
				// Create params; add device udid to every call.
				JSONObject jparams = new JSONObject();
				jparams.put(Constants.PARAM_DEVICE_UDID, deviceUDID);
				/*** -- add previous results to the request --
				if (cmdresult != null && (prevCommandID != null || prevActivityID != null)) {
					// build previous-command's results as params to the command:
					///jparams = new JSONObject();
					if (prevCommandID != null)
						jparams.put(Constants.CMD_commandtag, prevCommandID);
					if (prevActivityID != null)
						jparams.put(Constants.CMD_activitytag, prevActivityID);
					if (cmdresult.isPending()) {
						jparams.put(Constants.CMD_successtag, Constants.CMD_pendingvalue);
					} else if (cmdresult.isSuccess()) {
						jparams.put(Constants.CMD_successtag, Constants.CMD_successvalue);
					} else {
						jparams.put(Constants.CMD_successtag, Constants.CMD_failedvalue);
						if (cmdresult.hasErrorMessage())
							jparams.put(Constants.CMD_errortag, cmdresult.getErrorMessage());
					}	
				}
				**/
				
				cmdresult = null;
				HttpCommResponse response = serverComm.getFromServer(serverUrl, jparams);
				
				// process the response:
				if (response.isOK()) {
					done = false;
					try {
						if (LSLogger.isLoggingEnabled())
							LSLogger.debug(TAG, "Result from server: " + 
									Utils.filterProtectedContent(response.getResultStr()));
						String resultstr = response.getResultStr();
						JSONArray jarray = null;
						JSONObject jdata = null;
						if (resultstr.startsWith("[")) {
							jarray = new JSONArray( resultstr );
							//LSLogger.debug(TAG, "Created jsonArray " + jarray.toString());
							if (jarray.length() > 0) {
								jdata = jarray.getJSONObject(0);
								//LSLogger.debug(TAG, "Extracted jsonobject " + jdata.toString());
							} 
						} else if (resultstr.startsWith("{")) {
							jdata = new JSONObject( resultstr );
							//LSLogger.debug(TAG, "Created jsonobject " + jdata.toString());
						} else { // else response is not recognized:
							LSLogger.error(TAG, "Invalid command query response format: " + resultstr);
						}
						
						if ((jdata != null && jdata.length()==0) || 
						    (jarray!=null && jarray.length()==0)) {
							//LSLogger.debug(TAG, "No more commands to process.");
							done = true;
							
						} else {  
							 
							 // process the commands if we have any:
							int indx = 0;
							while (jdata != null && !done) {
								if (LSLogger.isLoggingEnabled())
									LSLogger.debug(TAG, "Extracted jsonobject("+(indx+1)+"): "+ 
												Utils.filterProtectedContent(jdata.toString()));
								// extract current command and process it:
								if (jdata.has(Constants.CMD_cmdtag)) {
								    cmdresult = processMDMCommand(jdata.getString(Constants.CMD_cmdtag), jdata, deferredCommands);
									addCommandResult(jsonResults, jdata, cmdresult);
									if (cmdresult.isAbort())
										done = true;
									
								} else {
									LSLogger.error(TAG, "Unknown server response: "+ (jdata==null?"null":jdata.toString()) );
									done = true;
								}	
							
								// get next value from array, if we have one: 
								indx++;
								if (jarray != null && indx < jarray.length())
									jdata = jarray.getJSONObject(indx);
								else
									jdata = null;
							} // end-while 
						
						}
						
						
					} catch (JSONException jex) {
						LSLogger.exception(TAG, "JSON Exception processing server command: ", jex);
						done = true; // we're getting improper results from server; abort processing.
					} catch (Exception ex) {
						LSLogger.exception(TAG, "Server command error: ", ex);
						done = true; // we're getting unexpected errors, so stop the outer loop
					}
					
				} else { // else response from http request was an error or exception and is not ok:
					LSLogger.debug(TAG, "Aborting server command queries due to response error: " + response.getResultStr());
					done = true;
				}
			} catch (Exception ex) {
				LSLogger.exception(TAG, "Processing Server Command", ex);
				done = true;
			}
			
			// if we have any results, send those to the server:
			if (jsonResults != null && jsonResults.length() > 0) {
				sendServerCommandResults(settings, jsonResults);
			}
			
		} while (!done);
		
		LSLogger.debug(TAG, "Completed reading server commands.");
		
		// process any deferred commands:
		if (deferredCommands.size() > 0) {
			LSLogger.debug(TAG, "Processing deferred server commands...");
			JSONArray jsonResults = new JSONArray();
			for (int i=0; i<deferredCommands.size(); i++) {
				MdmDeferredCommand cmd = deferredCommands.get(i);
				CommandResult cmdResult = processMDMCommand(cmd.cmd, cmd.json, null);
				addCommandResult(jsonResults, cmd.json, cmdResult);
				if (cmdResult != null && cmdResult.isAbort())
					break;
			}			
			LSLogger.debug(TAG, "Completed deferred server commands.");
			
			// if we have any results, send those to the server:
			if (jsonResults.length() > 0) {
				sendServerCommandResults(settings, jsonResults);
			}
			
		}
		
		return false; // false=all finished, true=more items to process
		
	}

	
	
	
	
	
	
	/***
	public static boolean older_processServerCommands(Controller controller) {
		LSLogger.debug(TAG, "Begin reading server commands...");
		
		boolean done = false;
		Vector<MdmDeferredCommand> deferredCommands = new Vector<MdmDeferredCommand>();
		Settings settings = controller.getSettingsInstance();
	   //String orgID = settings.getOrganizationID();
		String deviceUDID = settings.getDeviceUdid();
		
		// build the command url for getting the device's commands:
		//String serverCmd = "organizations/"+orgID+"/commands/android/query";
		String serverCmd = "command_dispatch/android";
		
		// create the full url to send to the server:
		String serverUrl = settings.getServerUrl();
		serverUrl = Utils.endStringWith(serverUrl, "/") + serverCmd;
		
		ServerComm serverComm = new ServerComm();
		
		String prevCommandID = null; // identifier of the previous command for chaining calls together one after another.
		String prevActivityID = null; // identifier of the previous activity for chaining calls together one after another.
		CommandResult cmdresult = null; // prevoius command's processing result; is used to pass results to server

		do {
			
			try {
				done = true;
				///JSONObject jparams = null;  // for command parameters; not used on the first pass through.
				// Create params; add device udid to every call.
				JSONObject jparams = new JSONObject();
				jparams.put(Constants.PARAM_DEVICE_UDID, deviceUDID);
				if (cmdresult != null && (prevCommandID != null || prevActivityID != null)) {
					// build previous-command's results as params to the command:
					///jparams = new JSONObject();
					if (prevCommandID != null)
						jparams.put(Constants.CMD_commandtag, prevCommandID);
					if (prevActivityID != null)
						jparams.put(Constants.CMD_activitytag, prevActivityID);
					if (cmdresult.isPending()) {
						jparams.put(Constants.CMD_successtag, Constants.CMD_pendingvalue);
					} else if (cmdresult.isSuccess()) {
						jparams.put(Constants.CMD_successtag, Constants.CMD_successvalue);
					} else {
						jparams.put(Constants.CMD_successtag, Constants.CMD_failedvalue);
						if (cmdresult.hasErrorMessage())
							jparams.put(Constants.CMD_errortag, cmdresult.getErrorMessage());
					}	
				}
				
				prevCommandID = null;
				prevActivityID = null;
				cmdresult = null;
				HttpCommResponse response = serverComm.getFromServer(serverUrl, jparams);
				done = false;
				// process the response:
				if (response.isOK()) {
					try {
						LSLogger.debug(TAG, "Result from server: " + response.getResultStr());
						String resultstr = response.getResultStr();
						JSONArray jarray = null;
						JSONObject jdata = null;
						if (resultstr.startsWith("[")) {
							jarray = new JSONArray( resultstr );
							LSLogger.debug(TAG, "Created jsonArray " + jarray.toString());
							if (jarray.length() > 0) {
							jdata = jarray.getJSONObject(0);
							LSLogger.debug(TAG, "Extracted jsonobject " + jdata.toString());
							} 
						} else if (resultstr.startsWith("{")) {
							jdata = new JSONObject( resultstr );
							LSLogger.debug(TAG, "Created jsonobject " + jdata.toString());
						}
						
						
						//JSONObject jdata = new JSONObject( response.getResultStr() );
						if (jdata != null && jdata.has(Constants.CMD_cmdtag)) {
							if (jdata.has(Constants.CMD_commandtag))
								prevCommandID = jdata.getString(Constants.CMD_commandtag);
							if (jdata.has(Constants.CMD_activitytag))
								prevActivityID = jdata.getString(Constants.CMD_activitytag);
						    cmdresult = processMDMCommand(jdata.getString(Constants.CMD_cmdtag), jdata, deferredCommands);
							if (cmdresult.isAbort())
								done = true;
						} else if ((jdata != null && jdata.length()==0) || 
								   (jarray!=null && jarray.length()==0)) {
							done = true;
							LSLogger.debug(TAG, "No more commands to process.");
						} else {
							LSLogger.error(TAG, "Unknown server response: "+ (jdata==null?"null":jdata.toString()) );
							done = true;
						}			
					} catch (JSONException jex) {
						LSLogger.exception(TAG, "JSON Exception processing server command: ", jex);
						done = true; // we're getting improper results from server; abort processing.
					} catch (Exception ex) {
						LSLogger.exception(TAG, "Server command error: ", ex);
					}
				} else {
					LSLogger.debug(TAG, "Aborting server command queries due to response error: " + response.getResultStr());
				}
			} catch (Exception ex) {
				LSLogger.exception(TAG, "Processing Server Command", ex);
				//done = true;
			}
		} while (!done);
		
		
		if (prevCommandID != null) { 
			// this gets executed if processing was aborted and more cmds exist; the last cmd results needs to be sent back.
			LSLogger.debug(TAG, "..to do: log last message results to server.");
			// ... to do .... log last result's back to server 
		}
		
		LSLogger.debug(TAG, "Completed reading server commands.");
		
		// process any deferred commands:
		if (deferredCommands.size() > 0) {
			LSLogger.debug(TAG, "Processing deferred server commands...");
			for (int i=0; i<deferredCommands.size(); i++) {
				MdmDeferredCommand cmd = deferredCommands.get(i);
				CommandResult cmdResult = processMDMCommand(cmd.cmd, cmd.json, null);
				// theoretically, all commands would have been processed.
				if (cmdResult != null && cmdResult.isAbort())
					break;
			}			
			LSLogger.debug(TAG, "Completed deferred server commands.");
		}
		
		return false; // false=all finished, true=more items to process
		
	}
	***/



}
