package com.lightspeedsystems.mdm.profile;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.lightspeedsystems.mdm.CommandResult;
import com.lightspeedsystems.mdm.Constants;
import com.lightspeedsystems.mdm.Controller;
import com.lightspeedsystems.mdm.DeviceAdminProvider;
import com.lightspeedsystems.mdm.Profiles;
import com.lightspeedsystems.mdm.Settings;
import com.lightspeedsystems.mdm.util.LSLogger;
import com.lightspeedsystems.mdm.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;

/**
 * Created by RWilson on 10/14/15.
 */
public class WebClipsPolicy extends ProfileItem  {
    private final static String TAG = "Profiles.WebClips";
    static final String displayName = "WebClips";

    private JSONArray profileSnapshot;
    private JSONArray jarray;

    public WebClipsPolicy(Context context, JSONArray data) {
        super(context, PrfConstants.PROFILETYPE_webclips, PrfConstants.PAYLOADTYPE_WebClips, false);
        prfstate = PrfConstants.PROFILESTATE_snapshot;
        jarray = data;
    }

    public boolean applyProfile(CommandResult cmdResult) {
        return applyProfile(jarray, cmdResult);
    }


    /* Internal method for handling setting profile values. */
    private boolean applyProfile(JSONArray jsondata, CommandResult cmdResult) {
        boolean bOk = true;

        try {
            profileSnapshot=null;
            String savedWebClips=Settings.getInstance(context).getSetting("WEB_CLIPS");
            if (savedWebClips != null){
                profileSnapshot = new JSONArray(Settings.getInstance(context).getSetting("WEB_CLIPS"));
                removeShortcuts(profileSnapshot);
            }
            long timeApplied = new Date().getTime();

            LSLogger.info("WEB_CLIPS",jsondata.toString());

            installShortcuts(jsondata);

            Settings.getInstance(context).setSetting("WEB_CLIPS",jsondata.toString());

        } catch (JSONException e) {
            cmdResult.setException(e);
            bOk = false;
        }

/*
        DeviceAdminProvider admin = Controller.getInstance(context).getDeviceAdmin();
        profileSnapshot = createSnapshot(admin); // get previous values.
        long timeApplied = new Date().getTime();
        try {
            // process each of the values in the profile:
            // enable/disable camera:
            if (jsondata.has(PrfConstants.PAYLOADVALUE_rs_cameraEnable)) {
                Boolean b = jsondata.getBoolean(PrfConstants.PAYLOADVALUE_rs_cameraEnable);
                boolean prev = admin.enableCamera(b.booleanValue());
                // log/save what we changed:
                if (prev != b.booleanValue())
                    Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_rs_cameraEnable, b.toString(), Boolean.toString(prev));
            }

            //enable/disable storage encryption:
            if (jsondata.has(PrfConstants.PAYLOADVALUE_rs_encryptionEnable)) {
                Boolean b = jsondata.getBoolean(PrfConstants.PAYLOADVALUE_rs_encryptionEnable);
                boolean prev = admin.enableStorageEncryption(b.booleanValue());
                if (prev != b.booleanValue())
                    Profiles.logProfileChange(timeApplied, displayName, PrfConstants.PAYLOADVALUE_rs_encryptionEnable, b.toString(), Boolean.toString(prev));
            }

        } catch (Exception ex) {
            cmdResult.setException(ex);
            bOk = false;
        }
*/
        return bOk;
    }

    private void removeShortcuts(JSONArray webClips) throws JSONException {
        if(webClips.length()>0){
            for (int i=0;i<webClips.length();i++){
                JSONObject webClip=webClips.getJSONObject(i);
                if(webClip.length()>0){

                    String urlStr = webClip.getString("url");
                    Intent shortcutIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlStr));
                    // shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    Intent intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    // Sets the custom shortcut's title
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, webClip.getString("label"));


                    // add the shortcut
                    intent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
                    context.sendBroadcast(intent);
                    LSLogger.info("Shortcut", webClip.getString("label")+" removed");
                }
            }
        }
    }

    private void installShortcuts(JSONArray webClips) throws JSONException {
        if(webClips.length()>0){
            for (int i=0;i<webClips.length();i++){
                JSONObject webClip=webClips.getJSONObject(i);
                if(webClip.length()>0){
                    String urlStr = webClip.getString("url");
                    if(urlStr.indexOf("://") == -1){
                        urlStr="http://"+urlStr;
                    }
                    Intent shortcutIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlStr));
                    // shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    Intent intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    // Sets the custom shortcut's title
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, webClip.getString("label"));

                    String iconUrlStr = null;
                    if(webClip.has("icon_url")){
                        iconUrlStr = webClip.getString("icon_url");
                    }

                    if(iconUrlStr != null) {
                        iconUrlStr = iconUrlStr.replace("\\/","/");
                        LSLogger.info("Shortcut", "Getting image " + iconUrlStr);
                        try {
                            URL url = new URL(iconUrlStr);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            InputStream input = connection.getInputStream();
                            Bitmap myBitmap = BitmapFactory.decodeStream(input);
                            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, myBitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_mdmapp));
                        }
                    } else {
                        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.drawable.ic_mdmapp));
                    }
                    // Set the custom shortcut icon
                    intent.putExtra("duplicate", false);

                    // add the shortcut
                    intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    context.sendBroadcast(intent);
                    LSLogger.info("Shortcut", webClip.getString("label")+" added");
                }
            }
        }
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
        if (isRestorable())
            bIsOK = applyProfile(profileSnapshot, cmdResult);//getProfileRestorableJSON
        return bIsOK;
    }

    /**
     * Abstract method for removing profile values, thereby removing the settings applicable to it.
     * (Note: does not apply to this class; use @restoreProfile to reset values to prior settings.)
     * @param cmdResult details of the processing, primarily for capturing errors.
     * Can be null if detailed results are not needed.
     * @return true.
     */
    public boolean removeProfile(CommandResult cmdResult) {
        // n/a, removal is not applicable for this class.
        return true;
    }


    /**
     * Gets a persistable string representation of the profile that is to be stored in the database,
     * representing previous values.
     * @return the string representation of the json to be saved with the profile.
     */
    @Override
    public String getPersistableProfileStr() {
        String s = Constants.EMPTY_JSON;
        if (profileSnapshot != null) {
            s = profileSnapshot.toString();
        }
        return s;
    }


    /**
     * Creates a ProfileItem for the current restriction settings, as a profile.
     * Includes camera and encryption settings.
     */
    private JSONObject createSnapshot(DeviceAdminProvider adminprovider) {
        JSONObject jdata = null;
        try {
            DeviceAdminProvider admin = Controller.getInstance(context).getDeviceAdmin();
            DevicePolicyManager dpm = admin.getDevicePolicyManager();
            jdata = new JSONObject();
            // camera:
            jdata.put (PrfConstants.PAYLOADVALUE_rs_cameraEnable, !dpm.getCameraDisabled(null));
            // storage encryption:
            jdata.put(PrfConstants.PAYLOADVALUE_rs_encryptionEnable, dpm.getStorageEncryption(null));
        } catch (Exception ex) {
            LSLogger.exception(TAG, "CreateSnapshot error:", ex);
        }
        return jdata;
    }

}
