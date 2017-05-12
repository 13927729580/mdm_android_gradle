package com.lightspeedsystems.mdm.profile;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;

import com.lightspeedsystems.mdm.CommandResult;
import com.lightspeedsystems.mdm.Constants;
import com.lightspeedsystems.mdm.Controller;
import com.lightspeedsystems.mdm.DeviceAdminProvider;
import com.lightspeedsystems.mdm.Settings;
import com.lightspeedsystems.mdm.util.LSLogger;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Robert T. Wilson on 3/28/17.
 */
public class AppLauncherPolicy extends ProfileItem  {
    private final static String TAG = "Profiles.AppLauncher";

    private JSONObject profileSnapshot;
    private JSONObject jarray;
    private Context thisContext;

    public AppLauncherPolicy(Context context, JSONObject data) {
        super(context, PrfConstants.PROFILETYPE_applauncher, PrfConstants.PAYLOADTYPE_AppLauncher, false);

        prfstate = PrfConstants.PROFILESTATE_snapshot;
        jarray = data;
    }

    public boolean applyProfile(CommandResult cmdResult) {

        return applyProfile(jarray, cmdResult);
    }


    /* Internal method for handling setting profile values. */
    private boolean applyProfile(JSONObject jsondata, CommandResult cmdResult) {
        boolean bOk = true;

        profileSnapshot=null;
        String appLauncherData = Settings.getInstance(context).getSetting("APP_LAUNCHER");
        if(appLauncherData != null) {
            try {
                profileSnapshot = new JSONObject(appLauncherData);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage("com.lightspeedsystems.lightspeedsecurelauncher");
        getContext().startActivity(launchIntent);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        Intent appLauncherIntent = new Intent();
        appLauncherIntent.setPackage("com.lightspeedsystems.lightspeedsecurelauncher");
        appLauncherIntent.setAction("com.lightspeedsystems.lightspeedsecurelauncher.action.mdm");
        appLauncherIntent.putExtra("EXTRA_APP_LAUNCH_DATA",jsondata.toString());
        getContext().sendBroadcast(appLauncherIntent);


        LSLogger.info("APP_LAUNCHER",jsondata.toString());

        Settings.getInstance(context).setSetting("APP_LAUNCHER",jsondata.toString());

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

}
