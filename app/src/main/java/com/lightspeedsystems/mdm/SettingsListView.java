package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;

import android.os.Bundle;
import android.view.MenuItem;
import android.app.Activity;

public class SettingsListView extends Activity {// implements SelectionListInterface {
	private static String TAG = "SettingsListView";

	/*
	  private int[] selectionItems = {
			   R.string.menulist_about,
			   R.string.menulist_diagnostics
			   };
	*/
	
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);    
		try {
	    	setContentView(R.layout.settings_list);
			getActionBar().setHomeButtonEnabled(true);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "contentview or actionbar error:", ex);
		}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      	case R.id.action_clear:
	    case android.R.id.home:
    	  Utils.NavigateToMain(this, 0);
          return true;
  
        default:
        	break;
      }
      return super.onOptionsItemSelected(item);
    }

    
    // ----------------------------------------------------
    // ---- SelectionListInterface implementation ----
    // ----------------------------------------------------
   
	/**
	 * Gets the list of resource identifiers for building the contents of the selection list.
	 * @return array of int resource string identifiers.
	 */
    /*
    public int[] getSelectionItems() {
    	return selectionItems;
    }
    */
    
	/**
	 * Called when an item in the list is selected.
	 * @param position position index into list of the selections (typically is the same as the id parameter).
	 * @param id 0-based row index into list of the selections.
	 */
    /*
	public void onListItemSelected(int position, long id) {
	   LSLogger.info(TAG, "List selection: position=" + Integer.toString(position) +
			   " id="+Long.toString(id));
	   if (position >= 0 && position < selectionItems.length) {
		   int selection = selectionItems[position];  
		   // gets the resource ID of the selection; now, navigate based on that
		   switch (selection) {
			   case R.string.menulist_settings:
				   Utils.NavigateToActivity(getApplicationContext(), SettingsListView.class);
				   break;
			   case R.string.menulist_deviceinfo:
				   Utils.NavigateToActivity(getApplicationContext(), DeviceInfoView.class);
				   break;
			   case R.string.menulist_apps:
				   Utils.NavigateToActivity(getApplicationContext(), ManagedAppsView.class);
				   break;
			   case R.string.menulist_diagnostics:
				   Utils.NavigateToActivity(getApplicationContext(), DiagnosticsList.class);
				   break;
			   case R.string.menulist_about:
				   // .. to do ..
				   break;
		   }
		   
	   } // else selection index out of range or invalid, so ignore it.
   }
	   */

}
