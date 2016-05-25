/**
 * 
 */
package com.lightspeedsystems.mdm;

//import com.lightspeedsystems.mdm.util.LSLogger;
import com.lightspeedsystems.mdm.util.LSLogger;

import android.os.Bundle;
import android.view.MenuItem;
import android.app.Activity;

/**
 * Provides a view containing the list of managed apps.
 */
public class ManagedAppsView extends Activity {
	private static String TAG = "ManagedAppsView";
	
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);    
    	setContentView(R.layout.managed_apps);
		try {
			getActionBar().setHomeButtonEnabled(true);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "actionbar error:", ex);
		}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      	case R.id.action_clear:
	    case android.R.id.home:
    	  //LSLogger.debug(TAG, "clicked on home icon");
    	  Utils.NavigateToMain(this, 0);
          return true;
  
        default:
        	break;
      }
      return super.onOptionsItemSelected(item);
    }

    
}
