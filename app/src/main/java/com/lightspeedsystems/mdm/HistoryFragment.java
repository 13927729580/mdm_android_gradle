package com.lightspeedsystems.mdm;

import android.app.ListFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.lightspeedsystems.mdm.util.LSLogger;



/**
 * Displays the list of historical mdm activity actions on the device, 
 * and supports tasks that can be done with them.
 * The items are shown in a Fragment so they can be included in a full-screen or split-screen view.
 */
public class HistoryFragment extends ListFragment implements Events.EventsDataChangeListener {
	private static String TAG = "HistoryFragment";
	private Context context;
	private Events events;
	private int bLoadStatus;  // One of the Constants.OPSTATUS values, for keeping track of the apps list building state.
	private BaseAdapter listAdapter;
	private TextView textStatus;  // status text field, shoen only when the list is getting created at the start.
	private Handler updateHandler;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.history_frag, container);
		context = view.getContext();
		events = Controller.getInstance(context).getEventsInstance();
		events.registerDataChangeListener(this);
		
		textStatus = (TextView)view.findViewById(R.id.textStatus);
		LSLogger.debug(TAG, "onCreateView - setting adapter later...");
		listAdapter = new EventsListAdapter(context);

		// note: defer setting the adapter until after the data is loaded: prevents race conditions
		//       that on nexus devices can crash the app.
		// setListAdapter(listAdapter);
		
		updateHandler = new Handler();
		// Kick off the process to build the history list asynchronously:
		if (bLoadStatus == 0) {
			bLoadStatus = Constants.OPSTATUS_INITIALIZING;
			if (textStatus != null) {
				textStatus.setText(R.string.status_loading);
			}
			new LoadEventsTask().execute();
		}

		return view;
	}

	
	
	
/*	
	public void onStart() {
		super.onStart();
		LSLogger.debug(TAG, "onStart " + this.toString());
	}
	public void onResume() {
		super.onResume();
		LSLogger.debug(TAG, "onResume " + this.toString());
	}
*/	
    @Override
    public void onDestroy() {
    	// unregister things:
        if (events != null)
        	events.unregisterDataChangeListener(this);
    	super.onDestroy();
    }	
    
	// Notification when an item on the list is clicked.
	// Here, we get the selected app, then display its details by invoking the built-in android preferences screen.
	@Override
	public void onListItemClick (ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		/*
		App app = (App)getListView().getItemAtPosition(position);

		try {
			LSLogger.debug(TAG, "Creating activity to show details for app " + app.getPackageName());
			startActivity(new Intent(//"android.settings.APPLICATION_DETAILS_SETTINGS",
						android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
						Uri.parse("package:"+app.getPackageName())));
			//LSLogger.debug(TAG, "Created activity to show details for app " + app.getPackageName());
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}
		*/
	}

 	
	// for now, our background task will collect any 'session history' from the controller.
	// this lets me do things, then show them in the list.
	
	/**
	 * Background task for loading the Managed events list.
	 * I use a background task because it could take awhile to load the events and build the list.
	 * In the interim, a "Loading" message is displayed until the list loads, then when done loading,
	 * the text message is removed.
	 */
	private class LoadEventsTask extends AsyncTask<Void, Void, Void> {
		protected void onPreExecute() {
			bLoadStatus = Constants.OPSTATUS_RUNNING;
		}
		protected Void doInBackground(Void... params) {
			LSLogger.debug(TAG, "Executing History load task on events instance=" + events.toString());
			events.loadEvents(false);
			LSLogger.debug(TAG, "History load task complete; events instance=" + events.toString());
			return null;
		}
		protected void onPostExecute(Void result) {
			bLoadStatus = Constants.OPSTATUS_COMPLETE;
			//LSLogger.debug(TAG, "after events load task, notifying listAdapter instance " + listAdapter.toString());
			if (textStatus != null) {
				textStatus.setVisibility(View.GONE);
				textStatus.setText("");
			}
			LSLogger.debug(TAG, "onPostexecute .. setting list adapter.");
			setListAdapter(listAdapter);	
		}
		
	}
	
    // Create runnable for posting from the onThreadComplete callback,
	// which is called when the sync process is completed.
	final Runnable mUpdateDataChange = new Runnable() {
	    public void run() {
	        //LSLogger.debug(TAG,"updateHandler running");
	        try {
	        	listAdapter.notifyDataSetChanged();
	        } catch (Exception ex) {
	        	LSLogger.exception(TAG, "UpdateDataChange error.",ex);
	        }
	    }
	};
			
    /**
     * Implementation of events data change listener interface, 
     * called back when some data has changed.
     * Invokes message handler to post message back to UI thread for notification processing.
     */
    public void onEventsDataChanged() {
    	//LSLogger.debug(TAG, "Events list change notification received.");
    	updateHandler.post(mUpdateDataChange);
    }
		
	
  // ---------------------------------------------------------------------------------------------------
  // inner-class Adapter for handling data; this is roughly copied from the List14.java from ApiDemo sample.
  // ---------------------------------------------------------------------------------------------------
  private class EventsListAdapter extends BaseAdapter {
	//private String TAG = "HistoryFragment.EventsListAdapter";
    private LayoutInflater mInflater;

    public EventsListAdapter(Context context) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        mInflater = LayoutInflater.from(context);        
     }

    /**
     * The number of items in the list is determined by the number of items in the collection.
     *
     * @see android.widget.ListAdapter#getCount()
     */
    public int getCount() {
        return  events.getEventsCount();
    }

    /**
     * Gets the Event at the 0-based index position in the list.
     *
     * @see android.widget.ListAdapter#getItem(int)
     */
    public Object getItem(int position) {
        return events.getEventAt(position);
    }

    /**
     * Use the array index as a unique id.
     *
     * @see android.widget.ListAdapter#getItemId(int)
     */
    public long getItemId(int position) {
        return position;
    }
    
    public void notifyDataSetChanged () {
    	LSLogger.debug(TAG, "executing notifyDataSetChanged method.");
    	super.notifyDataSetChanged();
    }

    /**
     * Make a view to hold each row.
     *
     * @see android.widget.ListAdapter#getView(int, android.view.View,
     *      android.view.ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        // An AppViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row.
        EventViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
        	//LSLogger.info(TAG, "getView view is null; position="+Integer.toString(position)+" activitystate="+activitystate);
        	
        	/*
            convertView = mInflater.inflate(R.layout.list_item_icon_text, null);
            holder = new EventViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.text1 = (TextView) convertView.findViewById(R.id.text1);
            holder.text2 = (TextView) convertView.findViewById(R.id.text2);
			*/
        	convertView = mInflater.inflate(R.layout.logging_list_item, null);
            holder = new EventViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.text1 = (TextView) convertView.findViewById(R.id.textMsg);
            holder.text2 = (TextView) convertView.findViewById(R.id.textIdentifier);
			
            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the View objects.
        	//LSLogger.info(TAG, "getView view present position="+Integer.toString(position));
            holder = (EventViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        Event event = events.getEventAt(position);
        if (event != null) {
        	//LSLogger.debug(TAG, "Event data at postion " + position + " event="+event.toString());
        	holder.text1.setText(event.toString());
        	holder.text2.setText(event.getEventTimeAsString());
        	holder.icon.setImageDrawable(event.getIcon());
        }
        
        return convertView;
    }

    // internal class for holding the contents of a line in the list view
    private class EventViewHolder {
        ImageView icon;
        TextView text1;
        TextView text2;
    }
}
  
}
