package com.lightspeedsystems.mdm;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Created by Robert T. Wilson on 5/26/16.
 */
public class MdmFirebaseInstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = "MdmFirebaseInstanceIdService";

    @Override
    public void onTokenRefresh() {
        String refreshToken = FirebaseInstanceId.getInstance().getToken();

        LSLogger.debug(TAG, "onTokenRefresh  refreshToken "+refreshToken);

    }
}
