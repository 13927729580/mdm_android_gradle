package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;
import android.os.Bundle;
import android.view.MenuItem;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.ListFragment;


/**
 * Displays the MDM Main actions/menu list of things that can be selected.
 *
 */
public class MainNavList extends Activity implements SelectionListInterface {
	private static String TAG = "MainNavList";
	
   /* list of menu items string IDs; used to define the navigation indexes into the list
    * and for dynamically building the array of strings that represent the selection items,
    * which the actual string contents will change for each language, and this lets us 
    * build them on-the-fly without hard-coding them into a array resource.
    */
   private int[] selectionItems = {
//		   R.string.mdmweb_admin_home,
//		   R.string.mbcweb_home,
		   R.string.menulist_settings, 
		   R.string.menulist_deviceinfo,
		   R.string.menulist_apps,
		   R.string.menulist_history,
		   R.string.menulist_diagnosticslogging,
		   //R.string.menulist_diagnostics,
		    //R.string.menulist_servertest,
		    //R.string.menulist_logging,
		   R.string.menulist_about
   };
   
 //  private ListView listView;
   
   @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main_nav_list);
        
		try {
			getActionBar().setHomeButtonEnabled(true);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "actionbar error:", ex);
		}

       // listView = (ListView) findViewById(R.id.listView);
        
        //...instance is null here, even though it is within the sel_list_fragment ..would have to get the frag, then the list.
      //  LSLogger.info(TAG, "Listview instance: " + (listView==null?"null":listView.toString()));
        
        /*
        ListAdapter adapter = (new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, 
                Utils.buildStringArrayFromResources(selectionItems) ));
        listView.setAdapter(adapter);        
        listView.setTextFilterEnabled(true);
        
        listView.setOnItemClickListener(new OnItemClickListener() {
        	  @Override
        	  public void onItemClick(AdapterView<?> parent, View view,
        	    int position, long id) {
        		  onListItemClick((ListView)parent, view, position, id);
        	  }
        	}); 
        */
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
	 * Gets the list of selection strings as the contents of the selection list.
	 * @return array of strings.
	 */
    public String[] getSelectionItems() {
    	return Utils.buildStringArrayFromResources(selectionItems);
    }
    
	/**
	 * Called when an item in the list is selected.
	 * @param position position index into list of the selections (typically is the same as the id parameter).
	 * @param id 0-based row index into list of the selections.
	 */
	public void onListItemSelected(int position, long id, ListFragment fragment) {
	   LSLogger.debug(TAG, "MainList selection: position=" + Integer.toString(position) +
			   " id="+Long.toString(id));
	   if (position >= 0 && position < selectionItems.length) {
		   int selection = selectionItems[position];  
		   // gets the resource ID of the selection; now, navigate based on that
		   switch (selection) {
		    /*
		   	   case R.string.mdmweb_admin_home:
		   		   Utils.NavigateToActivity(getApplicationContext(), MdmHomeWebView.class);
		   		   break;
		   	   case R.string.mbcweb_home:
		   		   Utils.NavigateToActivity(getApplicationContext(), MyBigCampusWebView.class);
		   		   break;
		    */
		   	   case R.string.menulist_settings:
				   Utils.NavigateToActivity(getApplicationContext(), SettingsListView.class);
				   break;
			   case R.string.menulist_deviceinfo:
				   Utils.NavigateToActivity(getApplicationContext(), DeviceInfoView.class);
				   break;
			   case R.string.menulist_apps:
				   Utils.NavigateToActivity(getApplicationContext(), ManagedAppsView.class);
				   break;
			   case R.string.menulist_history:
				   Utils.NavigateToActivity(getApplicationContext(), HistoryView.class);
				   break;
				   
			   case R.string.menulist_diagnostics:
				   Utils.NavigateToActivity(getApplicationContext(), DiagnosticsList.class);
				   break;
			   case R.string.menulist_servertest:
				   //Utils.NavigateToActivity(getApplicationContext(), ServerTestView.class);
				   ServerTestView.showDialog(this);
				   break;
			   case R.string.menulist_logging:
			   case R.string.menulist_diagnosticslogging:
				   Utils.NavigateToActivity(getApplicationContext(), LoggingInfoView.class);
				   break;
				   
			   case R.string.menulist_about:
				    DialogFragment newFragment = new AboutMdmDlg();
				    newFragment.show(getFragmentManager(), "lsmdm_about");
				   break;
		   }
		   
	   } // else selection index out of range or invalid, so ignore it.
   }

}
