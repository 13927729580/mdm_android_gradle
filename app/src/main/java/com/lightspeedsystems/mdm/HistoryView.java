package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

/**
 * Provides a historical view of mdm actions.
 *
 */
public class HistoryView extends Activity {
	private static String TAG = "HistoryView";
	
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);    
    	setContentView(R.layout. history); 
		try {
			getActionBar().setHomeButtonEnabled(true);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "actionbar error:", ex);
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.historylist_menu, menu);
 //     SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
 //     searchView.setOnQueryTextListener(this); 
      
       return true;
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
    
    /**
     * Called when the Clear history action is invoked.
     * @param item
     */
    public void onClearHistory(MenuItem item) {
    	Events events = Controller.getInstance().getEventsInstance();
    	if (events.getEventsCount() > 0) {
	    	// prompt user before clearing.
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setTitle(R.string.clearhistory_confirm_title) 
	    		   .setMessage(R.string.clearhistory_confirm_msg)
	    		   .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	 // User cancelled the dialog
	    	           }
	    	       });
	    	builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	   // User clicked YES button; clear the current list and delete all stored log entries:
	    	        	   Events events = Controller.getInstance().getEventsInstance();
	    	        	   events.deleteAllEvents();
	    	           }
	    	       });
	    	AlertDialog dialog = builder.create();
	    	dialog.show();
    	}
    }
	
   
    
}