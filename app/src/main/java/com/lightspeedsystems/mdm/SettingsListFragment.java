package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;
import com.lightspeedsystems.mdm.util.ListPrefView;
import com.lightspeedsystems.mdm.util.StaticTextPrefView;
import com.lightspeedsystems.mdm.util.EditTextPrefView;
import com.lightspeedsystems.mdm.util.ButtonPrefView;
import com.lightspeedsystems.mdm.util.ButtonClickListener;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/** 
 * Supports displaying and interacting with the settings window
 */
public class SettingsListFragment extends PreferenceFragment 
								implements OnPreferenceChangeListener, ButtonClickListener { 
	private static String TAG = "SettingsListFragment";
	private Context context;
	
	private EditTextPrefView 	view_svrUrl;
	private EditTextPrefView 	view_svrPort;
 //	private EditTextPrefView 	view_globalProxy;
	private StaticTextPrefView 	view_globalProxy;
	private ButtonPrefView		view_svrTestBtn;
	private StaticTextPrefView 	view_orgID;
	private StaticTextPrefView 	view_parentName;
	private StaticTextPrefView 	view_parentType;
	private EditTextPrefView 	view_assetTag;
	private CheckBoxPreference  view_dspnotifs;
	
	private ListPrefView      view_updatechk;
	private int updatechkResources[] = {
			R.string.settingslist_update_never,
			R.string.settingslist_update_daily,
			R.string.settingslist_update_weekly,
			R.string.settingslist_update_monthly,
			R.string.settingslist_update_quarterly,
			R.string.settingslist_update_yearly
	};
	private CharSequence updatechkInternalValues[] = {
			Constants.UPDATECHECK_NEVER,
			Constants.UPDATECHECK_DAILY,
			Constants.UPDATECHECK_WEEKLY,
			Constants.UPDATECHECK_MONTHLY,
			Constants.UPDATECHECK_QUARTERLY,
			Constants.UPDATECHECK_YEARLY
	};

	private final static int ctrlID_svrTestButton = 2;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      try {
        // Load the preferences layout resource:
        addPreferencesFromResource(R.layout.settings_frag); 
        
        //LSLogger.debug(TAG, "layout and preferences loaded during create.");
                
        view_svrUrl = (EditTextPrefView)   findPreference(Settings.MDMSERVER_ADDRESS);
        view_svrPort= (EditTextPrefView)   findPreference(Settings.MDMSERVER_PORT);
    //    view_globalProxy=(EditTextPrefView)findPreference("GLOBAL_PROXY");
        view_globalProxy=(StaticTextPrefView)findPreference("GLOBAL_PROXY");
        view_svrTestBtn= (ButtonPrefView)  findPreference("MDMSERVER_TESTBUTTON");
    	view_orgID  = (StaticTextPrefView) findPreference(Settings.ORGANIZATION_ID);
    	view_parentName = (StaticTextPrefView) findPreference(Settings.PARENT_NAME);
    	view_parentType = (StaticTextPrefView) findPreference(Settings.PARENT_TYPE);
        view_assetTag=(EditTextPrefView)   findPreference(Settings.ASSETTAG);   
        view_dspnotifs=(CheckBoxPreference)findPreference("DISPLAYNOTIFS");   
        view_updatechk=(ListPrefView)      findPreference("UPDATECHECK");   
    	
        //LSLogger.debug(TAG, "oncreate - setting listeners...");
        
    	if (view_svrUrl != null) {
    		view_svrUrl.setOnPreferenceChangeListener(this);
    		//view_svrUrl.setIcon(android.R.drawable.ic_popup_sync ); //ic_menu_upload);
    	}
       	if (view_svrPort != null) {
    		view_svrPort.setOnPreferenceChangeListener(this);
    		//view_svrPort.setIcon(android.R.drawable.ic_popup_sync ); //ic_menu_upload);
    	} 
       	if (view_globalProxy != null) {
       		view_globalProxy.setOnPreferenceChangeListener(this);
       	} 
       	
       	//LSLogger.debug(TAG, "oncreate - setting button listener...");
       	
      	if (view_svrTestBtn != null) {
      		view_svrTestBtn.setButtonID(1);
        	view_svrTestBtn.registerButtonClickListener(this);
       	} 
      	
     /* if (view_orgID != null) {
       		view_orgID.setOnPreferenceChangeListener(this);
     	} 
     */
      	
    	if (view_dspnotifs != null) {
    		view_dspnotifs.setOnPreferenceChangeListener(this);
    	} 
    	
    	//LSLogger.debug(TAG, "oncreate - setting updatecheck values...");
    	
    	if (view_updatechk != null) {
    		view_updatechk.setEntryValues(updatechkInternalValues);
    		view_updatechk.setEntries(
    				Utils.buildStringArrayFromResources(updatechkResources));
    		view_updatechk.setOnPreferenceChangeListener(this);
    	} 

    	//LSLogger.debug(TAG, "oncreate - setting assettag listeners...");
    	
      	if (view_assetTag != null) {
      		view_assetTag.setOnPreferenceChangeListener(this);
    		//view_assetTag.setIcon(android.R.drawable.ic_popup_sync ); //ic_menu_upload);
    	}
      	
      } catch (Exception ex) {
			LSLogger.exception(TAG, "onCreate error:", ex);
      }
      //LSLogger.debug(TAG, "oncreate complete.");
    }
    
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View view = super.onCreateView ( inflater,  container,  savedInstanceState);
    	try {
    		context = view.getContext();
    		//LSLogger.debug(TAG, "onCreateView - setting View values...");
    		setViewValues(view);
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "onCreateView error:", ex);
    	}
    	return view;
    }
    
    /**
     * Interface Callback for when a preference value was changed.
     * @return true to accept the change, false to not accept it (this always returns true).
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      try {
    	String prefKey = preference.getKey();
    	LSLogger.debug(TAG, "Preference '" + prefKey + "' value set to: " + newValue.toString());
    	Settings settings = Settings.getInstance(context);
    	if (settings != null) {
    		// convert boolean to string as needed: (we store only string values)
    		if (newValue != null && (newValue instanceof Boolean)) {
    			newValue = new String(newValue.toString());
    			//LSLogger.debug(TAG,	"-converted setting value from boolean to string value="+newValue);
    		}
    		settings.setSetting(preference.getKey(), newValue.toString());
    		
	    	// for these prefs, when the value changes, update the displayed values:
	    	if (prefKey.equalsIgnoreCase(Settings.UPDATECHECK)) {
	    		setUpdateCheckValues(settings);
	    	} // else if...
    	}
      } catch (Exception ex) {
    	LSLogger.exception(TAG, "onPreferenceChange error:", ex);	
      }
      return true;
    }

    // sets the values in the view from the current preferences, needed for custom 
    private void setViewValues(View view) {
    	//LSLogger.debug(TAG, "begin setViewValues");
    	try {
	    	Settings settings = Settings.getInstance(context);
	    	if (settings != null) { // display settings-stored values:
	    		if (view_svrUrl != null)
	    			view_svrUrl.setText(settings.getSetting(Settings.MDMSERVER_ADDRESS));
	       		if (view_svrPort != null)
	    			view_svrPort.setText(settings.getSetting(Settings.MDMSERVER_PORT));
	    		if (view_orgID != null)
	    			view_orgID.setText(settings.getOrganizationName());
	       		if (view_parentName != null)
	    			view_parentName.setText(settings.getParentName());
	      		if (view_parentType != null)
	      			view_parentType.setText(settings.getParentType());
	       		if (view_assetTag != null)
	       			view_assetTag.setText(settings.getSetting(Settings.ASSETTAG));
	       		if (view_dspnotifs != null)
	       			view_dspnotifs.setChecked(settings.getSettingBoolean(Settings.DISPLAYNOTIFS, false));
	       		if (view_updatechk != null) 
	       			setUpdateCheckValues(settings);	       		
	    	} 
	      	if (view_svrTestBtn != null) {
	       		String label = getActivity().getResources().getString(R.string.settingslist_servertest_btnlabel); 
	       		view_svrTestBtn.setButtonLabel(label);
	       		view_svrTestBtn.setButtonID(ctrlID_svrTestButton);
	      	}
	      	if (view_globalProxy != null) { // display current proxy setting, or none if nothing found.
	      		GlobalProxy gp = new GlobalProxy(context);
	      		String proxystr = gp.getServerAndPort();
	      		if (proxystr == null || proxystr.trim().length()==0) // default to "None" if nothing found.
	      			proxystr = context.getResources().getString(R.string.label_none);
	      		view_globalProxy.setText(proxystr);
	      	}
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "SetViewValues error:", ex);
    	}
    	//LSLogger.debug(TAG, "ending setViewValues");
    }
    

    private void setUpdateCheckValues(Settings settings) {
    	try {
			int indx = view_updatechk.findIndexOfValue(settings.getUpdateCheck());
			if (indx >= 0) {
   			view_updatechk.setValueIndex(indx);
   			view_updatechk.setText( view_updatechk.getEntries()[indx] );
			}
		} catch (Exception ex) {
			LSLogger.exception(TAG, "setUpdateCheckValues error:", ex);
		}
    }
    
	/** 
	 * ButtonClickListener interface implementation: this callback is invoked a button is pressed. 
	 * @param v View the button is in.
	 * @param buttonID identifier for the button, or 0 if no ID was defined.
	 */
	public void onButtonClick(View v, int buttonID) {
		LSLogger.debug(TAG, "Handling button click for buttonID=" + buttonID + " (viewid="+Integer.toString(v.getId()));
		if (buttonID == ctrlID_svrTestButton) {
			// show the server test dialog:
			ServerTestView.showDialog(getActivity());
		}
	}
    
}
