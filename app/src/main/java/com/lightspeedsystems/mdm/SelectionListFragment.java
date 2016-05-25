/**
 * 
 */
package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;
import android.os.Bundle;
import android.app.Activity;
import android.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


/**
 * Provides common selection list implementation as a fragment, used in various layouts.
 * 
 * This class defines the SelectionListener interface that must be implemented by activities
 * which use this fragment.
 * 
 * Instances of this class get created as part of a layout, then get called various lifecycle
 * methods. During onActivityCreated, gets the list of strings as an array from integers from
 * the activity, then fills the list with those IDs as string resource locators, and assigns
 * the array to the ListView this instance instantiates.
 * 
 * As items are selected, callbacks through the SelectionListener interface are made.
 * 
 */
public class SelectionListFragment extends ListFragment {
	private static String TAG = "SelectionListFragment";
	private SelectionListInterface mListener;

	// lifecycle method called when the activity this is under is known during creation.
	// we grab the instance and use it as the callback.
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SelectionListInterface) activity;
        } catch (ClassCastException e) {
        	LSLogger.exception(TAG, activity.getClass().getSimpleName() + " class must implement SelectionListInterface.", e);
           // throw new ClassCastException(activity.toString() + "Class must implement SelectionListFragment.SelectionListener");
        }
    }
    
		

    // lifecylce method called when the activity this fragment belongs to has been created;
    // note that onAttach is called before this.
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (mListener != null) {
        	String[] items = mListener.getSelectionItems();

	        // Populate list with our static array of titles.
	        setListAdapter(new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_activated_1, 
                items));
                //Utils.buildStringArrayFromResources(items) ));
	        //LSLogger.info(TAG, "Created items for list.");
        } else {
        	LSLogger.warn(TAG, "No SELECTION ITEMS were found.");
        }
	}
	
	
	
	// method called from the OS when an item in the list is selected.
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
         mListener.onListItemSelected(position, id, this);
    }

    /**
     * Reloads the current list with the given list of new strings. 
     * @param strings array of strings to display, null for an empty list.
     */
    public void reloadList(String[] strings) {
    	try {
    		// create a new instance of the listadapter with the contents of what is to be shown.
    		setListAdapter(new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_activated_1, 
                    strings));
    		LSLogger.debug(TAG,"reloaded list.");
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "SelectionList Error:", ex);
    	}
    }
	
}
