
package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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


/**
 * Displays the list of managed apps and supports tasks that can be done with them.
 * The items are shown in a Fragment so they can be included in a full-screen or split-screen view.
 */
public class ManagedAppsFragment extends ListFragment implements Apps.AppsDataChangeListener {
	private static String TAG = "ManagedAppsFragment";
	private Context context;
	private Apps apps; // this is set to a pointer to the main apps list in Controller; do not modify it.
	private int bAppsLoadStatus;  // One of the Constants.OPSTATUS values, for keeping track of the apps list building state.
	private BaseAdapter listAdapter;
	private TextView textStatus;  // status text field, shoen only when the list is getting created at the start.
	private ManagedAppsFragment fragmentInstance;
	private Handler updateHandler; // handler for background notifications of when data changed.
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.managed_apps_frag, container);
		context = view.getContext();
		fragmentInstance = this;
		textStatus = (TextView)view.findViewById(R.id.textStatus);
		//LSLogger.debug(TAG, "onCreateView - setting adapter...");
		listAdapter = new AppsListAdapter(context);
		// defer setting the adapter until the data is availanble:
		//setListAdapter(listAdapter);
		updateHandler = new Handler();
		// Kick off the process to build the apps list asynchronously:
		if (bAppsLoadStatus == 0) {
			bAppsLoadStatus = Constants.OPSTATUS_INITIALIZING;
			if (textStatus != null) {
				textStatus.setText(R.string.status_loading);
			}
			new LoadAppsTask().execute();
		}
		return view;
	}
	
    public void onDestroy() {
    	// unregister things:
        if (apps != null)
        	apps.unregisterDataChangeListener(this);
    	super.onDestroy();
    }	


	// Notification when an item on the list is clicked.
	// Here, we get the selected app, then display its details by invoking the built-in android preferences screen.
	@Override
	public void onListItemClick (ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
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
	}


	/**
	 * Background task for loading the Managed Applications' list.
	 * I use a background task because it could take awhile to load the apps and build the list.
	 * In the interim, a "Loading" message is displayed until the list loads, then when done loading,
	 * the text message is removed.
	 */
	private class LoadAppsTask extends AsyncTask<Void, Void, Void> {
		protected void onPreExecute() {
			bAppsLoadStatus = Constants.OPSTATUS_RUNNING;
		}
		protected Void doInBackground(Void... params) {
			LSLogger.debug(TAG, "executing apps load task on getting apps...");
			apps = Controller.getInstance(context).getAppsInstance();
			LSLogger.debug(TAG, "apps instance=" + apps.toString()+ " - #apps=" + apps.getAppsCount());
			//apps.loadManagedApps();
			//LSLogger.debug(TAG, "Apps load task complete; apps instance=" + apps.toString()+ " - #apps=" + apps.getAppsCount());
			return null;
		}
		protected void onPostExecute(Void result) {
			bAppsLoadStatus = Constants.OPSTATUS_COMPLETE;
			if (textStatus != null) {
				textStatus.setVisibility(View.GONE);
				textStatus.setText("");
			}
			setListAdapter(listAdapter);
			LSLogger.debug(TAG, "after apps load task, notifying listAdapter instance " + listAdapter.toString());
			//listAdapter.notifyDataSetChanged();			
			if (apps != null)
				apps.registerDataChangeListener(fragmentInstance);
		}
	}

	
	
    // Create runnable for posting from the onThreadComplete callback,
	// which is called when the sync process is completed.
    private final Runnable mUpdateDataChange = new Runnable() {
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
     * Implementation of Apps data change listener interface, 
     * called back when some app data has changed.
     * Invokes message handler to post message back to UI thread for notification processing.
     */
    public void onAppsDataChanged() {
    	//LSLogger.debug(TAG, "Apps list change notification received.");
    	updateHandler.post(mUpdateDataChange);
    }
	
  // ---------------------------------------------------------------------------------------------------
  // inner-class Adapter for handling data; this is roughly copied from the List14.java from ApiDemo sample.
  // ---------------------------------------------------------------------------------------------------
  private class AppsListAdapter extends BaseAdapter {
	//private String TAG = "ManagedAppsFragment.AppsListAdapter";
    private LayoutInflater mInflater;
    private Drawable defaultIcon;

    public AppsListAdapter(Context context) {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        mInflater = LayoutInflater.from(context);
        defaultIcon = context.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
     }

    /**
     * The number of items in the list is determined by the number of speeches
     * in our array.
     *
     * @see android.widget.ListAdapter#getCount()
     */
    public int getCount() {
    	if (apps == null)
    		return 0;
        return  apps.getAppsCount();
    }

    /**
     * Since the data comes from an array, just returning the index is
     * sufficient to get at the data. If we were using a more complex data
     * structure, we would return whatever object represents one row in the
     * list.
     *
     * @see android.widget.ListAdapter#getItem(int)
     */
    public Object getItem(int position) {
    	if (apps == null)
    		return null;
        return apps.getAppAt(position);
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
        AppViewHolder holder;
        // When convertView is not null, we can reuse it directly, there is no need
        // to reinflate it. We only inflate a new View when the convertView supplied
        // by ListView is null.
        if (convertView == null) {
        	//LSLogger.info(TAG, "getView view is null; position="+Integer.toString(position));
            convertView = mInflater.inflate(R.layout.list_item_icon_text2_text, null);

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            holder = new AppViewHolder();
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.text1 = (TextView) convertView.findViewById(R.id.text1);
            holder.text2 = (TextView) convertView.findViewById(R.id.text2);
            holder.text3 = (TextView) convertView.findViewById(R.id.text3);
            holder.text4 = (TextView) convertView.findViewById(R.id.text4);

            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the View objects.
            holder = (AppViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        App app = apps.getAppAt(position);
        if (app != null) {  
        	if (app.getAppIcon() == null) {
        		app.loadPackageInfo(null, context);
        		if (app.getAppIcon() == null)
        			app.setAppIcon(defaultIcon);
        	}
        	//LSLogger.debug(TAG, "App pckg="+(app.getPackageName()==null?"null":app.getPackageName()) +
        	//		             " dispname="+app.getDisplayName()+ " position="+position);
       		holder.text1.setText(app.getDisplayName());
        	holder.text2.setText(app.getPackageName());
        	holder.text3.setText(app.getDisplayInstallState(context));
        	holder.text4.setText(app.getDisplayInstallTime());
        	holder.icon.setImageDrawable(app.getAppIcon());
        } else {
        	LSLogger.warn(TAG, "App not found at position"+position);
        }

        return convertView;
    }

    // internal class for holding the contents of a line in the list view
    class AppViewHolder {
        ImageView icon;
        TextView text1;
        TextView text2;
        TextView text3; // install state as a string
        TextView text4; // time
    }
}
  
}
