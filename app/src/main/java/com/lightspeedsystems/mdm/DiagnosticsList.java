package com.lightspeedsystems.mdm;

//import com.lightspeedsystems.mdm.util.LSLogger;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class DiagnosticsList extends Activity {
	//private static String TAG = "DiagnosticsList";
	
	   /* list of menu items string IDs; used to define the navigation indexes into the list
	    * and for dynamically building the array of strings that represent the selection items.
	    */
	   private int[] selectionItems = {
			   R.string.diaglist_servertest, 
			   R.string.diaglist_logging
			//   R.string.diaglist_resetgcm,
			   };
	   
	   private ListView listView;
	   
	   @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        setContentView(R.layout.diagnostics_list);
	        
	        listView = (ListView) findViewById(R.id.listView1);
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
	    }
	   
	    
	   public void onListItemClick(ListView l, View v, int position, long id) {
		   //LSLogger.debug(TAG, "List selection: position=" + Integer.toString(position) +
			//	   " id="+Long.toString(id));
		   if (position >= 0 && position < selectionItems.length) {
			   int selection = selectionItems[position];  
			   // gets the resource ID of the selection; now, navigate based on that
			   switch (selection) {
				   case R.string.diaglist_servertest:
					   //Utils.NavigateToActivity(getApplicationContext(), ServerTestView.class);
					   ServerTestView.showDialog(this);
					   break;
				   case R.string.diaglist_logging:
					   Utils.NavigateToActivity(getApplicationContext(), LoggingInfoView.class);
					   break;
				   case R.string.diaglist_resetgcm:
//					   Utils.NavigateToActivity(getApplicationContext(), MainNavList.class);
					   break;
			   }
			   
		   } // else selection index out of range or invalid, so ignore it.
	   }


}
