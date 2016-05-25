
package com.lightspeedsystems.mdm;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import com.lightspeedsystems.mdm.util.FileSaveDialog;
import com.lightspeedsystems.mdm.util.LSLogger;
import com.lightspeedsystems.mdm.util.LSLogger.LSLogItem;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;

/**
 * Activity for displaying and managing logging features.
 *
 */
public class LoggingInfoView extends Activity implements LSLogger.LoggerDataChangeListener,
														 FileSaveDialog.FileSaveCallback { 
													// for searching, add -> implements OnQueryTextListener {
	private static final String TAG = "LoggingInfoView";
	
	private BaseAdapter listAdapter;
	private TextView textStatus;  // status text field, shown only when the list is getting created at the start.
	private ListView listView;
	private List<LSLogger.LSLogItem> logItems;
	private int bLoadStatus; 
	private Handler updateHandler;
	private LoggingInfoView viewInstance;

	private static int filterLevel = LSLogger.LOGLEVEL_all; // filtering selection; we'll use a loglevel value for this.
	
	//   TextView mSearchText;
	//   int mSortMode = -1;

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);    
    	viewInstance = this;
    	setContentView(R.layout.logging_info);    	
		logItems = new Vector<LSLogger.LSLogItem>(); // create an empty list, we need this for the data handler
		textStatus = (TextView)findViewById(R.id.textStatus);
		listView = (ListView)findViewById(R.id.listView);
		listAdapter = new LoggerListAdapter(this);
		listView.setAdapter(listAdapter);
		listView.setScrollingCacheEnabled(false);
		
		updateHandler = new Handler();
		/**
		listView.setOnScrollListener(new AbsListView.OnScrollListener() { 
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				//LSLogger.debug(TAG, "List Scrolled notification.");
				listView.invalidateViews();
				
			}
			public void onScrollStateChanged(AbsListView view, int scrollState) {};
			});
		**/
		
		try {
			getActionBar().setHomeButtonEnabled(true);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "actionbar error:", ex);
		}
		
		loadLoggingList();
    }
	
    public void onDestroy() {
    	// unregister things:
        LSLogger.getInstance().unregisterDataChangeListener(this);
    	super.onDestroy();
    }	

	// internal process to load the log entries in the background. 
	// Used to load the initial list and to refresh (reload) the list as needed.
	private void loadLoggingList() {
		logItems.clear();
		if (textStatus != null) {
			textStatus.setVisibility(View.VISIBLE);
			textStatus.setText(R.string.status_loading);
			textStatus.invalidate();
		}
		listView.invalidate();
		new LoadLoggerDataTask().execute();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.logginglist_menu, menu);
 //     SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
 //     searchView.setOnQueryTextListener(this); 
      
      // set initial selection of the log level:
      MenuItem item = menu.findItem( menuidFromLoglevel(LSLogger.getLogLevel()) );
      if (item != null)
    	  item.setChecked(true);
      
      // set initial selection of the filter view: (choose the 'all' selection)
      item = menu.findItem( menuidFromFilterlevel( filterLevel ));
      if (item != null)
    	  item.setChecked(true);
      return true;
    }   
    
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	/*
        if (mSortMode != -1) {
            Drawable icon = menu.findItem(mSortMode).getIcon();
            menu.findItem(R.id.action_sort).setIcon(icon);
        }
        */
        return super.onPrepareOptionsMenu(menu);
    }

    
    
    /**
    // sort selection handler
    public void onSort(MenuItem item) {
        //mSortMode = item.getItemId();
        // Request a call to onPrepareOptionsMenu so we can change the sort icon
        invalidateOptionsMenu();
    }
    **/

   /** -- searching code: 
    // The following callbacks are called for the SearchView.OnQueryChangeListener
    // For more about using SearchView, see src/.../view/SearchView1.java and SearchView2.java
    public boolean onQueryTextChange(String newText) {
        newText = newText.isEmpty() ? "" : "Query so far: " + newText;
        mSearchText.setText(newText);
        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        LSLogger.info(TAG, "Searching for: " + query + "...", false);
        return true;
    }
    **/
    
    /**
     * Callback from a selection in the Logging action menu
     * @param item selected item
     */
    public void onLogLevel(MenuItem item) {
        //LSLogger.info(TAG, "Selected LogLevel sub-Item: " + item.getTitle(), false);
        switch (item.getItemId()) {
        	case R.id.action_loglevel_none:
        		LSLogger.setLogLevel(LSLogger.LOGLEVEL_none);
        		item.setChecked(true);
        		break;
        	case R.id.action_loglevel_error:
        		LSLogger.setLogLevel(LSLogger.LOGLEVEL_error);
        		item.setChecked(true);
        		LSLogger.info(TAG, "Logging errors only.");
        		break;
        	case R.id.action_loglevel_warning:
        		LSLogger.setLogLevel(LSLogger.LOGLEVEL_warn);
        		item.setChecked(true);
        		LSLogger.info(TAG, "Logging warnings and errors.");
        		break;
        	case R.id.action_loglevel_info:
        		LSLogger.setLogLevel(LSLogger.LOGLEVEL_info);
        		item.setChecked(true);
        		LSLogger.info(TAG, "Logging info, warnings, and errors.");
        		break;
        	case R.id.action_loglevel_debug:
        		LSLogger.setLogLevel(LSLogger.LOGLEVEL_debug);
        		item.setChecked(true);
        		LSLogger.info(TAG, "Logging debug (everything).");
        		break;
        	case R.id.action_loglevel_all:
        		LSLogger.setLogLevel(LSLogger.LOGLEVEL_all);
        		item.setChecked(true);
        		LSLogger.info(TAG, "Logging everything.");
        		break;
        }
     }
    
    // gets the menu ID that is to be selected based on the given log level.
    private int menuidFromLoglevel(int level) {
    	int menuid = R.id.action_loglevel_none;
    	switch (level) {
    		case LSLogger.LOGLEVEL_error: menuid = R.id.action_loglevel_error; break;
    		case LSLogger.LOGLEVEL_warn: menuid = R.id.action_loglevel_warning; break;
    		case LSLogger.LOGLEVEL_info: menuid = R.id.action_loglevel_info; break;
    		case LSLogger.LOGLEVEL_debug: menuid = R.id.action_loglevel_debug; break;
    		case LSLogger.LOGLEVEL_all: menuid = R.id.action_loglevel_all; break;
    	}
    	return menuid;
    }
    
    /** 
     * Callback from the filtered view action menu.
     * @param item selected view option
     */
    public void onFilter(MenuItem item) {
    	int prevFilterLevel = filterLevel;
        LSLogger.info(TAG, "Selected Filter sub-Item: " + item.getTitle(), false);
        switch (item.getItemId()) {
    	case R.id.action_filter_error:
    		filterLevel = LSLogger.LOGLEVEL_error;
    		item.setChecked(true);
    		break;
    	case R.id.action_filter_warning:
    		filterLevel = LSLogger.LOGLEVEL_warn;
    		item.setChecked(true);
    		break;
    	case R.id.action_filter_info:
    		filterLevel = LSLogger.LOGLEVEL_info;
    		item.setChecked(true);
    		break;
    	case R.id.action_filter_debug:
    		filterLevel = LSLogger.LOGLEVEL_debug;
    		item.setChecked(true);
    		break;
    	case R.id.action_filter_all:
    		filterLevel = LSLogger.LOGLEVEL_all;
    		item.setChecked(true);
    		break;
        }
        if ((filterLevel > 0) && (prevFilterLevel != filterLevel))
        	loadLoggingList(); // reload list
    }
    
    // gets the menu ID that is to be selected based on the given filtering level.
    private int menuidFromFilterlevel(int level) {
    	int menuid = 0;
    	switch (level) {
    		case LSLogger.LOGLEVEL_error: menuid = R.id.action_filter_error; break;
    		case LSLogger.LOGLEVEL_warn: menuid = R.id.action_filter_warning; break;
    		case LSLogger.LOGLEVEL_info: menuid = R.id.action_filter_info; break;
    		case LSLogger.LOGLEVEL_debug: menuid = R.id.action_filter_debug; break;
    		case LSLogger.LOGLEVEL_all: menuid = R.id.action_filter_all; break;
    	}
    	return menuid;
    }

    
    public void onSaveToLog(MenuItem item) {
    	LSLogger.debug(TAG, "Saving log to file...");
    	
    	
    	/*
     	EnrollmentDlg newFragment = new EnrollmentDlg();
	    newFragment.registerDataChangeListener(this);
	    newFragment.setShowsDialog(true);
	    newFragment.show(getFragmentManager(), "lsmdm_enroll");    	
		*/
        File mPath = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        
        /*
        if (!mPath.canWrite())
        	new File(Environment.getExternalStorageDirectory() + "//downloads");
        if (!mPath.canWrite())
        	mPath = new File(Environment.getExternalStorageDirectory() + "//download");
        */
        
        FileSaveDialog fileSaveDialog = new FileSaveDialog(this);
        fileSaveDialog.setPath(mPath);
        fileSaveDialog.setSuccessNotificationResource(R.string.notif_logexported_ok);
        fileSaveDialog.registerFileSaveCallback(this);
        //fileDialog.setFileEndsWith(".txt");
        
        fileSaveDialog.show(getFragmentManager(), "lsmdm_logexport"); 
         
    	//LSLogger.debug(TAG, "Saving log to file...dialog shown.");
    }
    
    // ----- FileSaveCallback -----
	/**
	 * Callback when a file-save or other file-save-related operation is ready to occur or has occurred.
	 * The action code defines the state of events of the callback invocation.
	 * @param action a Cosntants.ACTIONSTATUS_ value, indicating why this is getting called. 
	 * Possible values include:
	 *  ACTIONSTATUS_OK: the file is ready to be written to or operated upon.
	 *  ACTIONSTATUS_CANCELLED: user cancelled the action without saving the file.
	 *  ACTIONSTATUS_FAILED: a critical error or failure occurred.
	 * @param file File instance of the target file to be written to or operated on.
	 * @param commandResult optional CommandResult instance for holding error and result data.
	 * @return true if the action completed successfully, false if not.
	 */
	public boolean onFileSave(int action, File file, CommandResult commandResult) {
		//LSLogger.debug(TAG, "FileSaveCallback for file: " + file.getAbsolutePath());
		String logseparator = LSLogger.LSLogItem.getExportedSeparator();
		// save the file:
		PrintWriter pw = null;
		try {
			     FileWriter fw = new FileWriter(file, false);  // false means to NOT append.
			     pw = new PrintWriter(fw);
			     // print header info:
			     pw.println(logseparator);
			     // print date and device info, then header for contents
			     String devinfo = Utils.formatLocalizedDateUTC(System.currentTimeMillis());
			     pw.println(devinfo);
			     pw.println("Mobile Manager Version: " + Constants.APPLICATION_VERSION_STR);
			     pw.println("Android UDID: " + Settings.getInstance(this).getDeviceUdid());
			     //pw.println("Serial Number: "+ Settings.getInstance(this).getDeviceSerialNumber());
			     pw.println(devinfo);
			     pw.println(LSLogger.LSLogItem.getExportedLogHeader());
			     pw.println(logseparator);
			     
			     // print out each items:
			     Iterator<LSLogger.LSLogItem> iter = logItems.iterator();
			     while (iter.hasNext()) {
			    	 LSLogger.LSLogItem item = iter.next();
			    	 pw.println(item.toLogExportString());
			     }
			     
			     // print footer:
			     pw.println(logseparator);
			     
			     commandResult.setSuccess(true);
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
			commandResult.setException(ex);
		} finally {
		     if (pw != null) {
		        pw.close();
		     }
		}
		return commandResult.isSuccess();
	}

    // ----------------------------
    
    
    
    
    /**
     * Called when the Clear Log action is invoked.
     * @param item
     */
    public void onClearLog(MenuItem item) {
    	 if (logItems != null && logItems.size() > 0) {
	    	// prompt user before clearing.
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	builder.setTitle(R.string.clearlog_confirm_title) 
	    		   .setMessage(R.string.clearlog_confirm_msg)
	    		   .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	 // User cancelled the dialog
	    	           }
	    	       });
	    	builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
	    	           public void onClick(DialogInterface dialog, int id) {
	    	        	   // User clicked YES button; clear the current list and delete all stored log entries:
	    	        	   logItems.clear();
	    	        	   LSLogger.clearLogs();
	    	        	   listAdapter.notifyDataSetChanged();
	    	        	   //listView.invalidateViews();
	    	           }
	    	       });
	    	AlertDialog dialog = builder.create();
	    	dialog.show();
    	}
    }
 
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
      LSLogger.info(TAG, "Selected Item: " + item.getTitle(), false);
      switch (item.getItemId()) {
      	case R.id.action_clear:
    	  LSLogger.info(TAG, "Clear action selected.", false);
    	  break;
      	case R.id.action_refresh:
          LSLogger.info(TAG, "Logging List Reshresh action selected.", true);
          loadLoggingList();
          break;
	    case android.R.id.home:
    	  //LSLogger.debug(TAG, "clicked on home icon");
    	  Utils.NavigateToMain(this, 0);
          return true;
  
        default:
          return super.onOptionsItemSelected(item);

          //default:
      //  break;
      }

      return true;
    }

    
	/**
	 * Background task for loading the persisted Logs list data.
	 * I use a background task because it could take awhile to load and build the list.
	 * In the interim, a "Loading" message is displayed until the list loads, then when done loading,
	 * the text message is removed.
	 */
	private class LoadLoggerDataTask extends AsyncTask<Void, Void, Void> {
		protected void onPreExecute() {
			bLoadStatus = Constants.OPSTATUS_RUNNING;
		}
		protected Void doInBackground(Void... params) {
			//LSLogger.info(TAG, "executing logger load task... ", false);
			if (filterLevel > LSLogger.LOGLEVEL_none  &&  filterLevel < LSLogger.LOGLEVEL_all)
				logItems = LSLogger.getLogItemsForMsgLevel(LSLogger.LOGORDER_Descending, filterLevel);
			else // default to getting all the items
				logItems = LSLogger.getLogItems(LSLogger.LOGORDER_Descending);
			return null;
		}
		protected void onPostExecute(Void result) {
			bLoadStatus = Constants.OPSTATUS_COMPLETE;
			//LSLogger.info(TAG, "after logger load task, notifying listAdapter instance " + listAdapter.toString());
			if (textStatus != null) {
				textStatus.setVisibility(View.GONE);
				textStatus.setText("");
				textStatus.invalidate();
			}
			
			//listAdapter.notifyDataSetChanged();
			listView.invalidate();//Views();
			LSLogger.getInstance().registerDataChangeListener(viewInstance);
		}
	}
	
    /*
    // Create runnable for posting from the onThreadComplete callback,
	// which is called when the sync process is completed.
    private Runnable mUpdateDataChange = new Runnable() {
        public void run() {
            //LSLogger.debug(TAG,"updateHandler running");
            try {
            	listAdapter.notifyDataSetChanged();
            } catch (Exception ex) {
            	LSLogger.exception(TAG, "UpdateDataChange error.",ex, false);
            }
        }
    };
    */
	
	/** 
	 * Local Runnable for Handler callback processing. This class is used to create an instance with
	 * a specific log item entry that is to be added to the list, upon then the list view is updated.
	 * This needs to be done like this to keep ui and background threading intact and clean.
	 */
    private class LoggerUpdater implements Runnable {
    	LSLogItem logItem;
    	public LoggerUpdater(LSLogItem item) {
    		logItem = item;
    	}
        public void run() {
            try {
            	// default to LSLogger.LOGORDER_Descending  order by inserting at the top of the list:
            	logItems.add(0, logItem);
            	listAdapter.notifyDataSetChanged();
            } catch (Exception ex) {
            	LSLogger.exception(TAG, "UpdateDataChange error.",ex, false);
            }
        }
    };
		
    /**
     * Implementation of Logger data change listener interface, 
     * called back when some data has changed.
     * Invokes message handler to post message back to UI thread for notification processing.
     * @param item LSLogItem instance for the data being logged. This should be added to the local
     * collection that is displaying logger data.
     */
    public void onLoggerDataChanged(LSLogItem item) {
     	updateHandler.post(new LoggerUpdater(item));  
    }
    
    
    // ---------------------------------------------------------------------------------------------------
    // inner-class Adapter for handling data; this is roughly copied from the List14.java from ApiDemo sample.
    // ---------------------------------------------------------------------------------------------------
    private class LoggerListAdapter extends BaseAdapter {
  	//private String TAG = "ManagedAppsFragment.AppsListAdapter";
      private LayoutInflater mInflater;
      private Drawable iconDebug;
      private Drawable iconInfo;
      private Drawable iconWarn;
      private Drawable iconError;
      

      public LoggerListAdapter(Context context) {
          // Cache the LayoutInflate to avoid asking for a new one each time.
          mInflater = LayoutInflater.from(context);        
          iconDebug = context.getResources().getDrawable(R.drawable.debug_icon);//android.R.drawable.ic_menu_view);
          iconInfo  = context.getResources().getDrawable(R.drawable.info_icon); //ic_dialog_info);
          iconWarn  = context.getResources().getDrawable(R.drawable.warning_icon);//android.R.drawable.ic_dialog_alert);
          iconError = context.getResources().getDrawable(R.drawable.error_icon);//android.R.drawable.stat_sys_warning);
       }

      /**
       * The number of items in the list is determined by the number of log items loaded.
       * Note that if the list is loading, we return a count of 0; this prevents some strange
       * timing issues on some devices (such as nexus7).
       */
      public int getCount() {
    	  if (bLoadStatus != Constants.OPSTATUS_COMPLETE)
    		  return 0;
          return logItems.size();
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
          return logItems.get(position);
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
              convertView = mInflater.inflate(R.layout.logging_list_item, null);

              // Creates a ViewHolder and store references to the two children views
              // we want to bind data to.
              holder = new AppViewHolder();
              holder.icon = (ImageView) convertView.findViewById(R.id.icon);
              holder.textMsg = (TextView) convertView.findViewById(R.id.textMsg);
              holder.textID  = (TextView) convertView.findViewById(R.id.textIdentifier);

              convertView.setTag(holder);
          } else {
              // Get the ViewHolder back to get fast access to the View objects.
              holder = (AppViewHolder) convertView.getTag();
          }

          // Bind the data efficiently with the holder.
          LSLogger.LSLogItem item = logItems.get(position);
          if (item != null) {
          	holder.textMsg.setText(item.getMessage());
          	holder.textID.setText(item.getIdentifier());          	
          	switch (item.getMsgTypeChar()) {
          		case LSLogger.MSGTYPEC_info: holder.icon.setImageDrawable(iconInfo); break;
          		case LSLogger.MSGTYPEC_warn: holder.icon.setImageDrawable(iconWarn); break;
          		case LSLogger.MSGTYPEC_error: holder.icon.setImageDrawable(iconError); break;
          		case LSLogger.MSGTYPEC_debug: holder.icon.setImageDrawable(iconDebug); break;
          		default: break; // do nothing if not one of the above.
          	}
          	
          	/*
          	// ...some sample code to show all the icon types, regardless of the actual msg type ...
          	int res = position % 4;
          	switch (res) {
	      		case 0: holder.icon.setImageDrawable(iconInfo); break;
	      		case 1: holder.icon.setImageDrawable(iconWarn); break;
	      		case 2: holder.icon.setImageDrawable(iconError); break;
	      		case 3: holder.icon.setImageDrawable(iconDebug); break;
          	}
          	// end of sample.
          	*/
          	
          }

          return convertView;
      }

      // internal class for holding the contents of a line in the list view
      class AppViewHolder {
          ImageView icon;
          TextView textMsg; // log message content
          TextView textID;  // message source and identification info, including timestamp, etc.
      }
  }
    
    
}
