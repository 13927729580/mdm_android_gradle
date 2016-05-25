package com.lightspeedsystems.mdm.profile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;

import com.lightspeedsystems.mdm.CommandResult;
import com.lightspeedsystems.mdm.Constants;
import com.lightspeedsystems.mdm.Profiles;
import com.lightspeedsystems.mdm.Utils;
import com.lightspeedsystems.mdm.util.LSLogger;

public class WifiPolicy extends ProfileItem {
		private final static String TAG = "Profiles.WifiPolicy";
		static final String displayName = "WifiPolicy";
		//static final String payloadType = PAYLOADTYPE_WiFi;
		private JSONObject previousCfgJson; // is a previous version of the config exists, this is what it was.
		
		public WifiPolicy(Context context, JSONObject data) {
			super(context, PrfConstants.PROFILETYPE_wifi, PrfConstants.PAYLOADTYPE_Wifi, true);
			jdata = data;
			// extract name from the json data; for wifi, we use the SSID as the name:
			if (jdata != null)
				name = extractFromJSON(PrfConstants.PAYLOADVALUE_wf_ssid, jdata);
		}
		
		public void setExists(boolean state) {
			super.setExists(state);
			// if this is new config, it will not have existed, and and such, we set the state as the snapshot.
			if (!state && (previousCfgJson!=null))
				setState(PrfConstants.PROFILESTATE_snapshot);
		}
		
		/**
		 * Gets a persistable string representation of the WiFi configuration this one is replacing, or {}.
		 * This is assumed to get called only when adding this instance to the database, and either the config
		 * it represents is replacing an existing config (state was already set to 'snapshot'), or it is a new config
		 * (and state is set to 'managed'). Note the state must have been set at an earlier time, and not in here.
		 * @return the string representation of the json to be saved with the profile. 
		 */
		@Override
		public String getPersistableProfileStr() {
			String s = Constants.EMPTY_JSON;
			if (!exists() && previousCfgJson != null) {
				s = previousCfgJson.toString();
			}
			return s;
		}

		/**
		 * Applies the current policy in this instance.
		 * If a policy with the same name exists, overwrites that policy.
		 * An entry in the lsprofiles database is created as well as needed:
		 *  - if the profile exists in the database already, nothing is done.
		 *  - if the profile entry (with the ssid name) does not exist in the database,
		 *    an entry is added as follows: The content of the saved profile is:
		 *  -- if an existing profile is being replaced, a representation of the existing
		 *    profile if created, and then saved in the database. This handles sending in
		 *    a profile that has a name that already exists, and thus allows us to revert
		 *    to the previous profile settings as defined in the saved profile definition.
		 *  -- if this is a new profile, an entry is created, but the contents of the profile
		 *    is set to empty; if the profile is removed, nothing is to be restored, since the
		 *    profile is new and did not replace an existing profile. A "{}" is saved instead.
		 *  Returns the ProfileItem instance representing this config if it was added or updated
		 *   an existing config, or null if the profile could not be applied or had an error.
		 */		
		/**
		 * Abstract method: applies the current profile settings as defined in the json data used during instance creation.
		 * @param cmdResult details of the processing, primarily for capturing errors. 
		 * Can be null if detailed results are not needed.
		 * @return true if the operation succeeded, false if errors occurred, with details in cmdResult.
		 */
		public boolean applyProfile(CommandResult cmdResult) {
			return applyProfile(jdata, cmdResult);
		}
		
		
		/* Internal method for handling setting profile values. */
		private boolean applyProfile(JSONObject jsondata, CommandResult cmdResult) {
			boolean bOk = true;
			WifiManager wmgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			boolean wifiIsEnabled = wmgr.isWifiEnabled();
			if (!wifiIsEnabled)
				wifiIsEnabled = wmgr.setWifiEnabled(true);
			if (!wifiIsEnabled)
				LSLogger.warn(TAG, "Wifi is not enabled nor was able to be enabled. Wifi profile settings may fail to get set.");
						
			try {
				if (LSLogger.isLoggingEnabled())
					LSLogger.debug(TAG, "Applying profile from json: " + Utils.filterProtectedContent(jsondata.toString()));
				
				// extract the SSID and set it; for string names, we have to enclose the string in explicit quotes.
		 		if (jsondata.has(PrfConstants.PAYLOADVALUE_wf_ssid)) {
					name = Utils.encloseString(jsondata.getString(PrfConstants.PAYLOADVALUE_wf_ssid), "\""); // wrap a quote around the SSID name; needed by the OS like that.
				}
				
				WifiConfiguration srchConfig = findWifiConfig(name);
				if (srchConfig != null) {   // found an existing instance, so remove it, and save a copy of it as json.
					
					if (LSLogger.isLoggingEnabled())
						dumpConfig(srchConfig, "Found existing wifi config: ");						
					
					//..or, just capture the previous config, in case we need it.
					// then, if this is a new config, we'll mark it as a snapshot if the previousCfgJSon exists,
					// (which will occur when setExists() is called after applying the profile if it was new)
					previousCfgJson = getJSONProfileFromConfig(srchConfig);
					/***			
					setState(PrfConstants.PROFILESTATE_snapshot);
					***/			
					
					// dont need to disable; we'll remove it below anyways =>: wmgr.disableNetwork(srchConfig.networkId);
					
					// -> first, we need to remove a possible existing config with the same name:
					boolean bRemoved = wmgr.removeNetwork(srchConfig.networkId);
					LSLogger.debug(TAG, "Removed network " + srchConfig.SSID + " success="+bRemoved);
				}
			} catch (Exception e) {
				LSLogger.exception(TAG, "Wifi.applyPolicy-search", e); 
			}
			
			
			try {
				WifiConfiguration wifiConfig = new WifiConfiguration(); // create the working default instance.				
				wifiConfig.SSID = Utils.encloseString(name, "\"");;
				
				// clear any existing valued and set current values:
				setDefaultConfigValues(wifiConfig);

				if (jsondata.has(PrfConstants.PAYLOADVALUE_wf_hidden)) {
					wifiConfig.hiddenSSID = jsondata.getBoolean(PrfConstants.PAYLOADVALUE_wf_hidden);
					//LSLogger.debug(TAG, " -set Hidden="+wifiConfig.hiddenSSID);
				}
				
				// set the encryption type and optional password
				if (jsondata.has(PrfConstants.PAYLOADVALUE_wf_encryption_type)) {
					setEncryption(wifiConfig, jsondata, cmdResult);
				}

				// set the proxy config:
				if (jsondata.has(PrfConstants.PAYLOADVALUE_wf_proxy_type)) {
					setProxy(wifiConfig, jsondata, cmdResult);
				}
				
				// now apply the values:
				int rc=-1;
				if (configIsNew(wifiConfig)) { // we have a new configuration, so add it
					rc=wmgr.addNetwork(wifiConfig);
					LSLogger.debug(TAG, "Adding wifiprofile SSID="+wifiConfig.SSID);
				} else {  // we have an existing configuration; update it
					rc=wmgr.updateNetwork(wifiConfig);
					LSLogger.debug(TAG, "Updating wifiprofile SSID="+wifiConfig.SSID);
				}
								
				if (LSLogger.isLoggingEnabled())
					dumpConfig(wifiConfig, "After applying: (rc="+rc+"):"); 

				if (rc==-1) { // failed to save profile.
					String emsg = "Failed to save WiFi profile for SSID "+wifiConfig.SSID;
					if (cmdResult != null)
						cmdResult.setErrorMessage(emsg);
					LSLogger.error(TAG, emsg);
					bOk = false;
				} else { // profile added or updated successfully.
					long timeApplied = new Date().getTime();
					Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_wf_ssid, wifiConfig.SSID, null);
					if (jsondata.has(PrfConstants.PAYLOADVALUE_wf_autojoin)) {
						boolean autojoin = jsondata.getBoolean(PrfConstants.PAYLOADVALUE_wf_autojoin);
						if (autojoin) { 
							//boolean brc = 
							wmgr.enableNetwork(wifiConfig.networkId, false);
							//LSLogger.debug(TAG, "Wifi.applyPolicy: auto-join enabled for SSID "+ wifiConfig.SSID+" (rc="+brc+")");
						}
						/***	-- do not do this -- it does not always work and you can lose the config.					
						// now, if the profile is active, we need to stop/restart the wifi service:
						if (bNeedReconnect) {
							//boolean bdisc = wmgr.disconnect();
							boolean brecn = wmgr.reconnect();
							LSLogger.debug(TAG, "WifiConfig "+wifiConfig.SSID+" is active; reconnected="+brecn);
						}
						***/						
					}
				}				
				
			} catch (Exception ex) {
				cmdResult.setException(ex);
				LSLogger.exception(TAG, "WifiProfile.applyPolicy error:", ex);
				bOk = false;
			}
			return bOk;
		}
		
		
		
		/**
		 * Abstract method for restoring previously-saved profile values, overwriting current values
		 *  with those values stored in here in the json data, if any.
		 * @param cmdResult details of the processing, primarily for capturing errors. 
		 * Can be null if detailed results are not needed.
		 * @return true if the replacement succeeded, false if errors occurred, with details in cmdResult.
		 */
		public boolean restoreProfile(CommandResult cmdResult) {
			boolean bIsOK = true;
			// apply the profile if we have it and can restore it:
			if (isRestorable() && (prfstate == PrfConstants.PROFILESTATE_snapshot)) {
				LSLogger.debug(TAG, "Restoring WiFi profile "+getName());
 				bIsOK = applyProfile(getProfileJSON(), cmdResult); //getProfileRestorableJSON()
			}
			return bIsOK;
		}
		
		/**
		 * Abstract method for removing profile values, thereby removing the settings applicable to it.
		 * For this class, the WiFi configuration with the SSID is removed/deleted.
		 * @param cmdResult details of the processing, primarily for capturing errors. 
		 * Can be null if detailed results are not needed.
		 * @return true.
		 */
		public boolean removeProfile(CommandResult cmdResult) {
			boolean bRemoved = false;
			try {
				WifiConfiguration wifiCfg = findWifiConfig(getName());
				if (wifiCfg != null) {
					WifiManager wmgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
					bRemoved = wmgr.removeNetwork(wifiCfg.networkId);
					if (!bRemoved && cmdResult != null)
						cmdResult.setErrorMessage("Failed to remove WiFi profile "+ getName());
					
				} else {
					bRemoved = true; // wifi config no longer present, so, return that we removed it.
					setRestorable(false); // don't bother trying to restore this
					LSLogger.debug(TAG, "Attempting to remove WiFi profile "+getName()+", but the profile was not found.OK.");
				}

			} catch (Exception ex) {
				LSLogger.exception(TAG, "RemoveProfile error:", ex);
			}
			return bRemoved;
		}
		
		
		/* Finds a WiFi config with the given SSID name (non-case-sensitive), or null if one with the name is not found. */
		private WifiConfiguration findWifiConfig(String ssidName) {
			WifiConfiguration wifiConfig = null;
			if (ssidName != null) {
			  try {
			    // make sure the name string is wrapped in quote chars:
			 	ssidName = Utils.encloseString(ssidName, "\""); //"\"" + name + "\"";
				WifiManager wmgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);			
				// get a list of currently-configured networks, then look for the one for thre given name
			    List<WifiConfiguration> list = wmgr.getConfiguredNetworks();
			    if (list!=null) {
				  Iterator<WifiConfiguration> it = list.iterator();
				  while (it.hasNext()) {
					WifiConfiguration cfg = (WifiConfiguration)it.next();
					//if (LSLogger.isLoggingEnabled()) dumpConfig(cfg, "Found:"); 
					if (cfg.SSID.equalsIgnoreCase(ssidName)) {
						wifiConfig = cfg;  // found an existing instance, so use it, and save a copy of it as json.
						break;
					}
				  }
			    }
			  } catch (Exception ex) {
				LSLogger.exception(TAG, "FindWifiConfig error:", ex);
			  }
			}
		    return wifiConfig;
		}
		
		
		// sets the wifi encryption mode and password fields in the given wifiConfig instance.
		private void setEncryption(WifiConfiguration wifiConfig, JSONObject jdata, CommandResult cmdResult) {
			try {
				String enctype = jdata.getString(PrfConstants.PAYLOADVALUE_wf_encryption_type);
				String encpwd = null;
				if (jdata.has(PrfConstants.PAYLOADVALUE_wf_encryption_pwd)) 
					encpwd = Utils.encloseString(jdata.getString(PrfConstants.PAYLOADVALUE_wf_encryption_pwd), "\"");
				if (encpwd != null) {
					LSLogger.debug(TAG, "Password in WIFI profile: lenght="+encpwd.length()+" pwd="+encpwd);
				} else {
					LSLogger.debug(TAG, "No password in WIFI profile.");
				}
				if (enctype == null) 
					enctype = "";

				if (enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_none)) {				
					LSLogger.debug(TAG, "-processing Wifi NONE for "+wifiConfig.SSID);
					wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.NONE );
					
				} else {  // process other types:
					wifiConfig.allowedGroupCiphers.set( WifiConfiguration.GroupCipher.CCMP );
					wifiConfig.allowedGroupCiphers.set( WifiConfiguration.GroupCipher.TKIP );
					wifiConfig.allowedPairwiseCiphers.set( WifiConfiguration.PairwiseCipher.CCMP );
					wifiConfig.allowedPairwiseCiphers.set( WifiConfiguration.PairwiseCipher.TKIP );
										
					// handle the different types of encryption:
					if (enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wep_ent) ||
						enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wep_entv3) ||
						enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wep)) {
						LSLogger.debug(TAG, "-processing Wifi WEP for "+wifiConfig.SSID);
						wifiConfig.wepKeys[0] = encpwd;					
						wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.NONE );
						wifiConfig.allowedAuthAlgorithms.set( WifiConfiguration.AuthAlgorithm.SHARED );
						wifiConfig.allowedGroupCiphers.set( WifiConfiguration.GroupCipher.WEP104 );
						wifiConfig.allowedGroupCiphers.set( WifiConfiguration.GroupCipher.WEP40 );					
						
					} else if (enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wpa_ent) ||
							   enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wpa_entv3)) {
						LSLogger.debug(TAG, "-processing Wifi WPA-Ent for "+wifiConfig.SSID);
						wifiConfig.allowedAuthAlgorithms.set( WifiConfiguration.AuthAlgorithm.LEAP );
						wifiConfig.allowedAuthAlgorithms.set( WifiConfiguration.AuthAlgorithm.OPEN );
						wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.WPA_EAP );
						wifiConfig.preSharedKey = encpwd;
						
					} else if (enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wpa)) {
						LSLogger.debug(TAG, "-processing Wifi WPA for "+wifiConfig.SSID);
						wifiConfig.allowedAuthAlgorithms.set( WifiConfiguration.AuthAlgorithm.OPEN );
						wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.WPA_PSK );
						wifiConfig.preSharedKey = encpwd;					
						
					} else if (enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_any_per) ||
							   enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_any_ent) ||
							   enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_any_entv3) ||
							   enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_any)) {
						// any encryption is allowed; add in values not set by defaults:
						LSLogger.debug(TAG, "-processing Wifi ANY for "+wifiConfig.SSID);
						wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.NONE );
						wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.WPA_PSK );
						wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.WPA_EAP );
						wifiConfig.allowedKeyManagement.set( WifiConfiguration.KeyMgmt.IEEE8021X );
						wifiConfig.allowedGroupCiphers.set( WifiConfiguration.GroupCipher.WEP104 );
						wifiConfig.allowedGroupCiphers.set( WifiConfiguration.GroupCipher.WEP40 );					
						wifiConfig.preSharedKey = encpwd;
						wifiConfig.wepKeys[0] = encpwd;	
						
					} else {
						LSLogger.warn(TAG, "Unknown Wifi encryption/security type.");
					}
				}
								
			} catch (Exception ex) {
				cmdResult.setException(ex);
				LSLogger.exception(TAG, "WifiProfile.setEncryption error:", ex);
			}
		}

		// sets or clears the wifi proxy values in the given wifiConfig instance.
		// For a proxy config of "none", if the config is not new, clears any previous setting.
		// For a proxy config of "manual", gets the proxy info from the json data and sets it.
		private void setProxy(WifiConfiguration wifiConfig, JSONObject jdata, CommandResult cmdResult) {
			try {
				String proxyType = jdata.getString(PrfConstants.PAYLOADVALUE_wf_proxy_type);
				if (proxyType!=null) {
					WifiProxy proxy = new WifiProxy(wifiConfig);
					if (proxyType.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_proxytype_None)) {
						proxy.ClearWifiProxySettings();
					} else if (proxyType.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_proxytype_Manual)) {
						String serverUrl = null;
						int serverPort = 0;
						//String username = null;
						//String password = null;
						if (jdata.has(PrfConstants.PAYLOADVALUE_wf_proxy_server))
							serverUrl = jdata.getString(PrfConstants.PAYLOADVALUE_wf_proxy_server);
						if (jdata.has(PrfConstants.PAYLOADVALUE_wf_proxy_serverport))
							serverPort = jdata.getInt(PrfConstants.PAYLOADVALUE_wf_proxy_serverport);
						/*
						// ... username and password to proxy server are not supported/used right now...
						if (jdata.has(PAYLOADVALUE_wf_proxy_username))
							username = jdata.getString(PAYLOADVALUE_wf_proxy_username);
						if (jdata.has(PAYLOADVALUE_wf_proxy_password))
							password = jdata.getString(PAYLOADVALUE_wf_proxy_password);
						*/
						proxy.SetWifiProxySettings(serverUrl, serverPort);

					} else if (proxyType.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_proxytype_Auto)) {
						/*
						String serverUrl = null;
						if (jdata.has(PAYLOADVALUE_wf_proxy_pac_url))
							serverUrl = jdata.getString(PAYLOADVALUE_wf_proxy_pac_url);
						*/
						// .. now what...i suppose we could query the server?
						
					} else {
						LSLogger.warn(TAG, "Unrecognized Wifi.Proxy tag: "+PrfConstants.PAYLOADVALUE_wf_proxy_type+"="+proxyType);
					}
				} else {
					LSLogger.debug(TAG, "No WifiProxy tag found.");
				}
			} catch (Exception ex) {
				cmdResult.setException(ex);
				LSLogger.exception(TAG, "WifiProfile.setProxy error:", ex);
			}
		}
		
		
		/* Clears common config values. */
		private void clearConfigValues(WifiConfiguration wifiConfig) {
			// first, clear out any config settings:
			wifiConfig.allowedAuthAlgorithms.clear();
			wifiConfig.allowedKeyManagement.clear();
			wifiConfig.allowedGroupCiphers.clear();
			wifiConfig.allowedPairwiseCiphers.clear();
			wifiConfig.allowedProtocols.clear();
			wifiConfig.preSharedKey = null;
			wifiConfig.wepKeys[0] = null;
			wifiConfig.wepTxKeyIndex = 0;
		}
		
		// clears any existing bitset flags and sets common default ones applicable to all configurations.
		private void setDefaultConfigValues(WifiConfiguration wifiConfig) {
			// first, clear out any config settings:
			clearConfigValues(wifiConfig);

			// set common values:
			wifiConfig.allowedProtocols.set( WifiConfiguration.Protocol.RSN );
			wifiConfig.allowedProtocols.set( WifiConfiguration.Protocol.WPA );

			// For new configurations, set these:
			if (configIsNew(wifiConfig)) {
				wifiConfig.priority = 0;
				wifiConfig.status = WifiConfiguration.Status.ENABLED;
			}
		}
		
		
		// gets a json-object representation for the given wifi profile.
		// this can then be stored in the profileitem to save a previous or current wifi profile with the current ssid;
		//  mainly, this gets a storable-json representation of an existing profile, so we can re-apply it as needed.
		private JSONObject getJSONProfileFromConfig(WifiConfiguration wifi) {
			JSONObject json = new JSONObject();
			try {
				String enctype = PayloadEncryptionTypeFromEncType(getConfigAuthType(wifi));
				json.put(PrfConstants.PAYLOADVALUE_wf_encryption_type, enctype);
				
				//	json.put(PrfConstants.PAYLOADVALUE_wf_directattributes, true);
				String wfname = Utils.trimString(wifi.SSID, "\"");
				json.put(PrfConstants.PAYLOADVALUE_wf_ssid, wfname);
				json.put(PrfConstants.PAYLOADVALUE_wf_hidden, wifi.hiddenSSID);
				json.put(PrfConstants.PAYLOADVALUE_wf_priority, wifi.priority);
				json.put(PrfConstants.PAYLOADVALUE_wf_autojoin, 
						((wifi.status == WifiConfiguration.Status.ENABLED) ||
						 (wifi.status == WifiConfiguration.Status.CURRENT)) );
		//		Utils.addStringToJSON(json, 	PrfConstants.PAYLOADVALUE_wf_bssid, wifi.BSSID);
				
				if (!enctype.equals(PrfConstants.PAYLOADVALUE_wf_enctype_none)) {
					String encpwd = extractEncryptionPassword(wifi, enctype);
					if (encpwd != null && !encpwd.isEmpty()) {
						json.put(PrfConstants.PAYLOADVALUE_wf_encryption_pwd, encpwd);
						json.put(PrfConstants.PAYLOADVALUE_wf_wepkeyindex, wifi.wepTxKeyIndex);		
					}
				}
				/**
				// for the bitset values, use their to_string methods, and store them like "{0.2.3}" of the bits that are set:
				json.put(PrfConstants.PAYLOADVALUE_wf_authalgs, 	wifi.allowedAuthAlgorithms.toString());
				json.put(PrfConstants.PAYLOADVALUE_wf_groupciphers, wifi.allowedGroupCiphers.toString());
				json.put(PrfConstants.PAYLOADVALUE_wf_keymgmt, 		wifi.allowedKeyManagement.toString());
				json.put(PrfConstants.PAYLOADVALUE_wf_pwciphers, 	wifi.allowedPairwiseCiphers.toString());
				json.put(PrfConstants.PAYLOADVALUE_wf_protos, 		wifi.allowedProtocols.toString());
				**/
				
				// get proxy settings; if any exist, we force it to manual-mode.
				WifiProxy proxy = new WifiProxy(wifi);
				if (proxy.ReadWifiProxySettings()) {  // returns true if we found proxy values for the config.
					String svr = proxy.getServerUrl();
					String port = proxy.getServerPort();
					json.put(PrfConstants.PAYLOADVALUE_wf_proxy_type, PrfConstants.PAYLOADVALUE_wf_proxytype_Manual);
					json.put(PrfConstants.PAYLOADVALUE_wf_proxy_server, svr);
					if (port != null)
						json.put(PrfConstants.PAYLOADVALUE_wf_proxy_serverport, port);
				}

				// debug: show the resulting instance:
				if (LSLogger.isLoggingEnabled())
				LSLogger.debug(TAG, "GetJsonProfileFromConfig = " + json.toString() +"\nWifi config: "+wifi.toString());

			} catch (Exception ex) {
				LSLogger.exception(TAG, "GetJsonProfile error:", ex);
			}
			
			return json;
		}

		/**
		 * Sets the Wifi config with values from json, applied directly to accessile attributes in the config.
		 * This is used when restoring a config from previously-extracted values from another config, and 
		 * now needs to set those values back into here directly and explicitly.
		 * @param wifiConfig
		 * @param jsondata
		 */
		/**
		private void setJSONProfileIntoConfig(WifiConfiguration wifiConfig, JSONObject jsondata) {
			try {
				clearConfigValues(wifiConfig);
				wifiConfig.SSID = Utils.encloseString(jsondata.getString(PrfConstants.PAYLOADVALUE_wf_ssid), "\"");
				wifiConfig.hiddenSSID = jsondata.getBoolean(PrfConstants.PAYLOADVALUE_wf_hidden);
				wifiConfig.priority = jsondata.getInt(PrfConstants.PAYLOADVALUE_wf_priority);
				
				// set each of the bitset values using specific methods for each category
				Utils.setBitsetStrIntoBitset(wifiConfig.allowedProtocols, 	   jsondata.getString(PrfConstants.PAYLOADVALUE_wf_protos));
				Utils.setBitsetStrIntoBitset(wifiConfig.allowedAuthAlgorithms, jsondata.getString(PrfConstants.PAYLOADVALUE_wf_authalgs));
				Utils.setBitsetStrIntoBitset(wifiConfig.allowedKeyManagement,  jsondata.getString(PrfConstants.PAYLOADVALUE_wf_keymgmt));
				Utils.setBitsetStrIntoBitset(wifiConfig.allowedGroupCiphers,   jsondata.getString(PrfConstants.PAYLOADVALUE_wf_groupciphers));
				Utils.setBitsetStrIntoBitset(wifiConfig.allowedPairwiseCiphers,jsondata.getString(PrfConstants.PAYLOADVALUE_wf_pwciphers));
				
				if (jsondata.has(PrfConstants.PAYLOADVALUE_wf_bssid))
					wifiConfig.BSSID = jsondata.getString(PrfConstants.PAYLOADVALUE_wf_bssid);
				if (jsondata.has(PrfConstants.PAYLOADVALUE_wf_presharedkey))
					wifiConfig.preSharedKey = jsondata.getString(PrfConstants.PAYLOADVALUE_wf_presharedkey);
				if (jsondata.has(PrfConstants.PAYLOADVALUE_wf_wepkeys))
					Utils.setJsonArrayIntoStringArray(wifiConfig.wepKeys, jsondata.getJSONArray(PrfConstants.PAYLOADVALUE_wf_wepkeys));
				wifiConfig.wepTxKeyIndex = jsondata.getInt(PrfConstants.PAYLOADVALUE_wf_wepkeyindex);
								
				if (LSLogger.isLoggingEnabled())
					dumpConfig(wifiConfig, "Applied saved values into wifi config: ");
				
			} catch (Exception ex) {
				LSLogger.exception(TAG, "SetPresavedConfigValues error:", ex);
			}
		}
		**/
		
		/* gets a PrfConstants.WFENCTYPE_ value for a given wifi config. uses the key bit values to determine the type.
		 * wpa has auths={} and keymgmt={1}
		 * wep has auths={0} and keymgmt={0}
		 */
		private int getConfigAuthType(WifiConfiguration cfg) {
			int type = PrfConstants.WFENCTYPE_none;
			if (cfg.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
				type = PrfConstants.WFENCTYPE_wpa;
	///		} else if (cfg.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
	///			type = PrfConstants.WFENCTYPE_wpaent; // KeyMgmt.WPA2_PSK;
			} else if (cfg.allowedKeyManagement.get(KeyMgmt.WPA_EAP)) {
				type = PrfConstants.WFENCTYPE_wpaent;//KeyMgmt.WPA_EAP;
		//	} else if (cfg.allowedAuthAlgorithms.isEmpty()) {  //auths={}
			} else if (cfg.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.OPEN)) {	
				// required for wpa/wpa2
				type = PrfConstants.WFENCTYPE_wep;
			} else if (cfg.allowedAuthAlgorithms.get(WifiConfiguration.AuthAlgorithm.SHARED)) {
				// requires static wep keys
				type = PrfConstants.WFENCTYPE_wep;
			}
			return type;
		}
		
		/* gets a payloadvalue_wf_enctype string value from a given encryption type value. */
		private String PayloadEncryptionTypeFromEncType(int encType) {
			String s = PrfConstants.PAYLOADVALUE_wf_enctype_none; // default to 'None'
			switch (encType) {
				case PrfConstants.WFENCTYPE_wpa:    s = PrfConstants.PAYLOADVALUE_wf_enctype_wpa; break;
				case PrfConstants.WFENCTYPE_wpaent: s = PrfConstants.PAYLOADVALUE_wf_enctype_wpa_ent; break;
				case PrfConstants.WFENCTYPE_wep:    s = PrfConstants.PAYLOADVALUE_wf_enctype_wep; break;
				case PrfConstants.WFENCTYPE_wepent: s = PrfConstants.PAYLOADVALUE_wf_enctype_wep_ent; break;
				case PrfConstants.WFENCTYPE_anyent: s = PrfConstants.PAYLOADVALUE_wf_enctype_any_ent; break;
				case PrfConstants.WFENCTYPE_any:    s = PrfConstants.PAYLOADVALUE_wf_enctype_any; break;
			}
			//LSLogger.debug(TAG, "EncryptionType '"+s+"' from type="+encType);
			return s;
		}


		// get encryption password from config data. Note that if this cannot get full password,
		//  the attributes will return a "*" as the password; passwords must be at least 8 characters
		//  for some wifi types, so we ensure at least 8 characters are returned for asterisks.
		private String extractEncryptionPassword(WifiConfiguration wifiConfig, String enctype) {
			String encpwd=null;
			try {
				/**
				Parcel parcel = Parcel.obtain();
				wifiConfig.writeToParcel(parcel, 0);
		//		parcel.setDataPosition(0); // move r/w position to the top
		//		byte[] bytes = parcel.marshall();
		//		bundle = parcel.readBundle();
				
				parcel.setDataPosition(0); // move r/w position to the top
				// read back values from parcel
				int n=parcel.readInt(); // networkid
				int s=parcel.readInt(); // status
				int d=parcel.readInt(); // disablereason
				String ss=parcel.readString(); //ssid
				String bb=parcel.readString(); // bssid
				String preSharedKey = parcel.readString();
				String[] keys = new String[4];
				for (int i=0; i<4; i++)
					keys[i] = parcel.readString();
				//int keyIndex = parcel.readInt();
				//parcel.readInt(); //priority
				//parcel.readInt(); // hiddenssid 0:1
				// bitsets
				
				LSLogger.debug(TAG, "Bundle="+(bundle==null?"null":bundle.toString()+" size="+bundle.size()));
				LSLogger.debug(TAG, "parcel: n="+n+" s="+s+" d="+d+" ss="+(ss==null?"null":ss)+" bb="+(bb==null?"null":bb));
				**/
				String[] keys = wifiConfig.wepKeys;
				String preSharedKey = wifiConfig.preSharedKey;
				
				// just find a value where you can. non-available values will be null.
				if (preSharedKey != null)
					encpwd = preSharedKey;
				else if (wifiConfig.wepTxKeyIndex>=0 && wifiConfig.wepTxKeyIndex<=3)
					encpwd = keys[wifiConfig.wepTxKeyIndex];
				
				/*
				// handle the different types of encryption:
				if (enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wep_ent) ||
					enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wep)) {
					encpwd = keys[wifiConfig.wepTxKeyIndex];										
				} else if (enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wpa_ent)) {
					encpwd = preSharedKey;
				} else if (enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_wpa)) {
					encpwd = preSharedKey;	
				} else if (enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_any_per) ||
						   enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_any_ent) ||
						   enctype.equalsIgnoreCase(PrfConstants.PAYLOADVALUE_wf_enctype_any)) {
					// any encryption is allowed; 
					encpwd = preSharedKey;
					if (encpwd==null || encpwd.length()==0)
						encpwd = keys[wifiConfig.wepTxKeyIndex];					
				}
				*/
				//	LSLogger.debug(TAG, "Extract password for "+enctype+": shredkey="+(preSharedKey==null?"null":preSharedKey)+
				//			" keys[0]="+(keys[0]==null?"null":keys[0] + " encpwd="+(encpwd==null?"null":encpwd)));
			} catch (Exception ex) {
				LSLogger.exception(TAG, "ExtractPassword error:", ex);
			}
			// pwd should be enclosed in quotes; remove them.
			if (encpwd != null) {
				if (encpwd.equals("*") || encpwd.equals("\"*\""))
					encpwd="********";
				Utils.trimString(encpwd, "\"");
			}
			return encpwd;
		}
	
		// debug-prints the contents of a config. prefixtext must not be null.
		private void dumpConfig(WifiConfiguration wifiConfig, String prefixTxt) {
			LSLogger.debug(TAG, prefixTxt + " Wifi SSID='"+wifiConfig.SSID 
					 + "' netid="+wifiConfig.networkId 
					 + " status="+wifiConfig.status
					 + " Bitsets: authalgs="+wifiConfig.allowedAuthAlgorithms
					 + " keymgmt="+wifiConfig.allowedKeyManagement
					 + " for grpcphr="+wifiConfig.allowedGroupCiphers
					 + " pairwise="+wifiConfig.allowedPairwiseCiphers
					 + " protos="+wifiConfig.allowedProtocols);
					 

		}
		
		// tests if the config is new or an existing one; looks at the networkId field; <=0 is new.
		private boolean configIsNew(WifiConfiguration wifiConfig) {
			return (wifiConfig.networkId <= 0);
		}
		
		
	
	
	// WifiProxy Support. Uses undocumented methods in WifiConfiguration.linkProperties.
	protected class WifiProxy {
		private final static String TAG = "WifiProxy";
		private WifiConfiguration config;
		private String serverUrl;
		private String serverPort;
		
		public WifiProxy(WifiConfiguration wifiConfig) {
			config = wifiConfig;
		}
		
		public String getServerUrl() { return serverUrl; }
		public String getServerPort() { return serverPort; }
		
		public Object getField(Object obj, String name)
				throws SecurityException, NoSuchFieldException, 
						IllegalArgumentException, IllegalAccessException {
		    Field f = obj.getClass().getField(name);
		    Object out = f.get(obj);
		    return out;
		}

		public Object getDeclaredField(Object obj, String name)
				throws SecurityException, NoSuchFieldException,
						IllegalArgumentException, IllegalAccessException {
		    Field f = obj.getClass().getDeclaredField(name);
		    f.setAccessible(true);
		    Object out = f.get(obj);
		    return out;
		}  

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void setEnumField(Object obj, String value, String name)
				throws SecurityException, NoSuchFieldException, 
					IllegalArgumentException, IllegalAccessException{
		    Field f = obj.getClass().getField(name);
		    f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
		}

		public void setProxySettings(String assign , WifiConfiguration wifiConf)
				throws SecurityException, IllegalArgumentException, 
					NoSuchFieldException, IllegalAccessException{
		    setEnumField(wifiConf, assign, "proxySettings");     
		}

		// Sets the wifiproxy to a given server and port. Returns false if there was an error, true if ok.
		@SuppressWarnings({ "rawtypes", "unchecked" })
		boolean SetWifiProxySettings(String serverUrl, int serverPort) {
			boolean brc = false;
		    try {
		        //get the link properties from the wifi configuration
		        Object linkProperties = getField(config, "linkProperties");
		        if (linkProperties == null) {
		        	LSLogger.error(TAG, "setWifiProxySetting: linkProperties not found.");
		            return false;
		        }
	
		        //get the setHttpProxy method for LinkProperties
		        Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
		        Class[] setHttpProxyParams = new Class[1];
		        setHttpProxyParams[0] = proxyPropertiesClass;
		        Class lpClass = Class.forName("android.net.LinkProperties");
		        Method setHttpProxy = lpClass.getDeclaredMethod("setHttpProxy", setHttpProxyParams);
		        setHttpProxy.setAccessible(true);
	
		        //get ProxyProperties constructor
		        Class[] proxyPropertiesCtorParamTypes = new Class[3];
		        proxyPropertiesCtorParamTypes[0] = String.class;
		        proxyPropertiesCtorParamTypes[1] = int.class;
		        proxyPropertiesCtorParamTypes[2] = String.class;
	
		        Constructor proxyPropertiesConstructor = proxyPropertiesClass.getConstructor(proxyPropertiesCtorParamTypes);
	
		        //create the parameters for the constructor
		        Object[] proxyPropertiesObjs = new Object[3];
		        proxyPropertiesObjs[0] = serverUrl;
		        proxyPropertiesObjs[1] = serverPort;
		        proxyPropertiesObjs[2] = null;
	
		        //create a new object using the params
		        Object proxySettings = proxyPropertiesConstructor.newInstance(proxyPropertiesObjs);
	
		        //pass the new object to setHttpProxy
		        Object[] params = new Object[1];
		        params[0] = proxySettings;
		        setHttpProxy.invoke(linkProperties, params);
	
		        setProxySettings("STATIC", config);
		        brc = true;
		        
		        LSLogger.debug(TAG, "Proxy set in WifiConfig: SSID="+config.SSID
		        		+" server="+(serverUrl==null?"null":serverUrl)+" port="+serverPort);
		        
		    } catch(Exception ex) {
		    	LSLogger.exception(TAG, "SetWifiProxySetttings error:", ex);
		    }
		    return brc;
 		}
		
		
		// Gets the wifiproxy settings, storing into local server and port values. 
		// Returns false if there was an error or no severurl found, true if serverurl is not null.
		// Call getServerUrl and getServerPort to get the values.
		//  we want to call this in LinkProperites:
		// public ProxyProperties getHttpProxy() { return mHttpProxy;  }
		// then we call these on the resulting ProxyProperties instance:
		// public String getHost() { return mHost;  }
		// public int getPort() { return mPort;  }
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public boolean ReadWifiProxySettings() {
			boolean brc = false;
		    try {
		        //get the link properties from the wifi configuration
		        Object linkProperties = getField(config, "linkProperties");
		        if (linkProperties == null) {
		        	LSLogger.error(TAG, "getWifiProxySetting: linkProperties not found.");
		            return false;
		        }
			        
		        //get the getHttpProxy method from LinkProperties instance:
		        Class lpClass = Class.forName("android.net.LinkProperties");
		        Method getHttpProxy = lpClass.getDeclaredMethod("getHttpProxy", (Class[])null);//setHttpProxyParams);		        		        
		        getHttpProxy.setAccessible(true);
		        
		        Object[] params = new Object[0]; // use this empty object array for params declarations:
		        
		        // invoke getHttpProxy method in linkproperties instance:
		        Object objresult = getHttpProxy.invoke(linkProperties, params);
		        
		        // we expect an instance of ProxyProperties as the result object:
		        if (objresult != null && objresult.getClass().getName().equals("android.net.ProxyProperties")) {
		        	LSLogger.debug(TAG, "--got result from gethttpproxy; class="+objresult.getClass().getName());
		        	// the host and port are in the ProxyProperties instance; invoke methods on it
			        Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
		        	Method ppgetHostMethod = proxyPropertiesClass.getDeclaredMethod("getHost", (Class[])null);
		        	Method ppgetPortMethod = proxyPropertiesClass.getDeclaredMethod("getPort", (Class[])null);
		        	serverUrl = (String)  ppgetHostMethod.invoke(objresult, params);
		        	Integer serverPortInt = (Integer)  ppgetPortMethod.invoke(objresult, params);
		        	if (serverPortInt != null)
		        		serverPort = serverPortInt.toString();		        	
		        }
	
		        //setProxySettings("STATIC", config);
		        brc = (serverUrl != null);
		        
		        LSLogger.debug(TAG, "Proxy get in WifiConfig: SSID="+config.SSID
		        		+" server="+(serverUrl==null?"null":serverUrl)+" port="+(serverPort==null?"null":serverPort));
		        
		    } catch(Exception ex) {
		    	LSLogger.exception(TAG, "GetWifiProxySetttings error:"+ex.getMessage(), ex);
		    }
		    return brc;
 		}

			
		//Clears the wifiproxy values. Returns false if there was an error, true if ok.
		@SuppressWarnings({ "unchecked", "rawtypes" })
		boolean ClearWifiProxySettings() {	
			boolean brc = false;
		    try {
		        //get the link properties from the wifi configuration
		        Object linkProperties = getField(config, "linkProperties");
		        if (linkProperties == null) {
		        	LSLogger.error(TAG, "ClearWifiProxySetting: linkProperties not found.");
		            return false;
		        }
	
		        //get the setHttpProxy method for LinkProperties
		        Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
		        Class[] setHttpProxyParams = new Class[1];
		        setHttpProxyParams[0] = proxyPropertiesClass;
		        Class lpClass = Class.forName("android.net.LinkProperties");
		        Method setHttpProxy = lpClass.getDeclaredMethod("setHttpProxy", setHttpProxyParams);
		        setHttpProxy.setAccessible(true);
	
		        //pass null as the proxy
		        Object[] params = new Object[1];
		        params[0] = null;
		        setHttpProxy.invoke(linkProperties, params);
	
		        setProxySettings("NONE", config);
		        brc = true;
		        
		        LSLogger.debug(TAG, "Proxy cleared in WifiConfig: SSID="+config.SSID);

		    } catch(Exception ex) {
		    	LSLogger.exception(TAG, "ClearWifiProxySetttings error:", ex);
		    }
		    return brc;
		 }

		 
		/**
		WifiConfiguration GetCurrentWifiConfiguration(WifiManager manager)
		{
		    if (!manager.isWifiEnabled()) 
		        return null;

		    List<WifiConfiguration> configurationList = manager.getConfiguredNetworks();
		    WifiConfiguration configuration = null;
		    int cur = manager.getConnectionInfo().getNetworkId();
		    for (int i = 0; i < configurationList.size(); ++i)
		    {
		        WifiConfiguration wifiConfiguration = configurationList.get(i);
		        if (wifiConfiguration.networkId == cur)
		            configuration = wifiConfiguration;
		    }

		    return configuration;
		}
		**/

	}

}
