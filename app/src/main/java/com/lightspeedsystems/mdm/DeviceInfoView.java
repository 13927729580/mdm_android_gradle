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
 * @author mikezrubek
 *
 */
public class DeviceInfoView extends Activity {
	private static String TAG = "DeviceInfoView";
	
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);    
    	setContentView(R.layout. device_info); //device_info_frag );//
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
