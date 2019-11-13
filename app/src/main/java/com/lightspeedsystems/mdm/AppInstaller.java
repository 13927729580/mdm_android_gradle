package com.lightspeedsystems.mdm;


import com.lightspeedsystems.mdm.util.LSLogger;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
//import android.content.pm.IPackageInstallObserver;

/**
 * Manages app installation, un-installation, and updates. 
 * 
 * This class is to be used and works as follows.
 * 
 * Create an instance of this class for an app (package) that is to be installed.
 * Invoke the install method, an this class invokes the installPackage method, providing
 *  this instance as the callback.
 */

public class AppInstaller extends Activity  {
	private static final String TAG = "AppInstaller";
    
    // View for install progress
    //View mInstallConfirm;
    // Buttons to indicate user acceptance
    //private Button mOk;
    //private Button mCancel;
    
    private static boolean bShowWindow = true; // set to true to show, in the background, a window for this activity.
    
    private Apps appsInstance;
    private App currentApp;  // app instance we're working with; must get it from initial intent,
    						 //  and use later in results.
    
 	public AppInstaller() {
		super();
	}

    @Override
    /**
     * Handle sub-activity completion;  this is called when the 
     *  Android InstallAppProgress (during install) or UninstallAppProgress (during uninstall)
     *  is executing, showing, and driving the install or uninstall or an app.
     *  The resultCode determines if the action completed successfully or did not; a value of -1
     *   indicates success for both install and uninstall (RESULT_OK). 
     *   Activity.RESULT_xxx values apply as possible values; RESULT_OK, RESULT_CANCELLED, RESULT_FIRSTUSER.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// the intent should contain a bundle that is an App instance; lets see if we still have it.
     	App app = currentApp;
     	/*
     	Intent launchIntent = getIntent();
    	if (launchIntent != null) {
    		Bundle bundle = launchIntent.getExtras();
    		if (bundle != null && (bundle.getString(App.VALUE_APPCLASS) != null) ) {
	    	  app = new App(bundle);
	    	  // the app instance should already exist; so get it from the controller's Apps instance.
	    	  app = appsInstance.findAppByID(app.getDbID());
    		}
    	}
    	*/
    	LSLogger.debug(TAG, "ActivtyResult received. req="+requestCode+" res="+resultCode+" app="+(app==null?"null":app.toString()));
    	if (app != null) {
    		appsInstance.setInstallCompletionResults(app, requestCode, resultCode);
    	} else {
    		LSLogger.warn(TAG, "App instance not found after install/uninstall.");
    	}
    	Controller.getInstance(this).notifyManagedAppActionComplete(app, resultCode);
    	
    	finish();
    }
	

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // get intent information
        final Intent intent = getIntent();
                
        LSLogger.info(TAG, "onCreate AppInstaller " + this.toString() + " intent="+(intent==null?"(null!)":intent.toString()));
    	appsInstance = Controller.getInstance().getAppsInstance();

       
        //set view (note: not setting a view keeps the screen empty; that's fine for this.)
  ////      setContentView(R.layout.install_pkg);

        /*
        mOk = (Button)findViewById(R.id.btnOK);
        mCancel = (Button)findViewById(R.id.btnCancel);
        
        mOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //	startInstall();
              //  finish();
            }});

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
	        setResult(RESULT_CANCELED);
	        LSLogger.debug(TAG, "Cancel pressed.");
	        finish();
            }});
         */

        
///        mInstallConfirm = findViewById(R.id.install_confirm_panel);
///        mInstallConfirm.setVisibility(View.INVISIBLE);
///        final PackageUtil.AppSnippet as = PackageUtil.getAppSnippet(
///                this, mPkgInfo.applicationInfo, sourceFile);
///        PackageUtil.initSnippetForNewApp(this, as, R.id.app_snippet);

    	// Get the app instance this intent is for; it'll have the flags set in it and all other values.
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
	    	currentApp = new App(bundle);
	    	if (!currentApp.isMdmAppSelf())
	    		currentApp = appsInstance.findAppByID(currentApp.getDbID());
        }
        
        if (currentApp != null) {
	        // determine the action: install or uninstall.
	        if (intent.getAction().equalsIgnoreCase(Constants.ACTION_APPUNINSTALL))
	        	uninstallUsingDefaultProcessing(currentApp);
	        else
	        	installUsingDefaultProcessing(currentApp, intent);
        } else {
        	LSLogger.error(TAG, "Internal error: No Bundle or App info.");
        	//if (intent.getAction().equalsIgnoreCase(Constants.ACTION_APPUNINSTALL))
        	//	onActivityResult(Constants.ACTIONID_UNINSTALL, Activity.RESULT_FIRST_USER, null);
        	//else
        	//	onActivityResult(Constants.ACTIONID_INSTALL, Activity.RESULT_FIRST_USER, null);
        	finish();
        }
    }
    
    public void onStart() {
    	super.onStart();
    	//LSLogger.debug(TAG, "onStart AppInstaller " + this.toString());
    	setVisible(bShowWindow);	
    }
    
    public void onResume() {
    	super.onResume();
    	//LSLogger.debug(TAG, "onResume AppInstaller " + this.toString());
    	setVisible(bShowWindow);	
    }
    
    public void onDestroy() {
    	super.onDestroy();
    	LSLogger.debug(TAG, "Destroying AppInstaller " + this.toString());
    }


    @SuppressWarnings("deprecation") // allow depreceated intent value for installs previous/equal to sdk 16
	private void installUsingDefaultProcessing(App app, Intent sourceIntent) {
    	// code for directly-starting the default interactive installer process; 
		// This prompts the user to confirm installing or updating the app.
    	try {
	    	
    		// get the package or file uri from the source intent; this defines the remote or local package.
	    	Uri fileuri = sourceIntent.getData(); 
	    	
	    	Intent installIntent;
	    	if (app.getInstallType() == App.INSTALLTYPE_ONLINESTORE) {
	    		installIntent = new Intent(Intent.ACTION_VIEW);
	    	} else { // installing by file; we can use the package installer for this.
				installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
	    		installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
	    		installIntent.setType("application/vnd.android.package-archive");
	    	}
	//		installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
	//				| Intent.FLAG_FROM_BACKGROUND );
	//				| Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			installIntent.setData(fileuri);
			installIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
			
			if (app.isMdmAppSelf()) { // if is ourself being updated/installed, don't prompt for replace.
	  			installIntent.putExtra(Intent.EXTRA_ALLOW_REPLACE, true); 
				installIntent.putExtra(Intent.EXTRA_REPLACING, true); 
			}

			startActivityForResult(installIntent, Constants.ACTIONID_INSTALL);
			
			LSLogger.debug(TAG, "Started intent to install package. " + installIntent.toString());
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "AppInstallDefaultProcessing error. ", ex);
    	}
    }
    
    private void uninstallUsingDefaultProcessing(App app) {
    	// code for directly-starting the default interactive uninstaller process; 
		// This prompts the user to confirm uninstalling the app.
    	try {
	    	Uri fileuri = Uri.parse("package:" + app.getPackageName());
	    	
	    	Intent installIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
			installIntent.setType("application/vnd.android.package-archive");
			installIntent.setData(fileuri);
			installIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
			
			//if (bundle != null)
			//	installIntent.putExtras(bundle);
			
			startActivityForResult(installIntent, Constants.ACTIONID_UNINSTALL);
			
			LSLogger.debug(TAG, "Started intent to uninstall package. " + installIntent.toString());
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "AppUninstallDefaultProcessing error. ", ex);
    	}
    }
   
/*    
    private boolean isInstallingUnknownAppsAllowed() {
        return Settings.Secure.getInt(getContentResolver(), 
            Settings.Secure.INSTALL_NON_MARKET_APPS, 0) > 0;
    }
*/
    
    
/*** -- code for running the installer process directly; this is applicable when the process has
        the priviledge to install packages. Otherwise we use the interaction processing used above.    

    
    void setPmResult(int pmResult) {
        Intent result = new Intent();
        result.putExtra("EXTRA_INSTALL_RESULT", pmResult);
        setResult(pmResult == 1 //PackageManager.INSTALL_SUCCEEDED
                ? RESULT_OK : RESULT_FIRST_USER, result);
    }

    
    private void initiateInstall(Intent intent) {
    	PackageManager mPm = getPackageManager();
    	PackageInfo mPkgInfo = mPm.getPackageArchiveInfo(mPackageURI.getPath(), 0);//PackageUtil.getPackageInfo(sourceFile);
        //ApplicationInfo mSourceInfo;
    	
        String pkgName = mPkgInfo.packageName;
        LSLogger.debug(TAG, "initiateInstall pkgname="+pkgName);
        
        // Check if there is already a package on the device with this name
        // but it has been renamed to something else.
        String[] oldName = mPm.canonicalToCurrentPackageNames(new String[] { pkgName });
        if (oldName != null && oldName.length > 0 && oldName[0] != null) {
            pkgName = oldName[0];
            mPkgInfo.packageName = pkgName;
        }
        // Check if package is already installed. display confirmation dialog if replacing pkg
        try {
            mAppInfo = mPm.getApplicationInfo(pkgName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            mAppInfo = null;
        } 
        // Start subactivity to actually install the application
    	LSLogger.debug(TAG, "Starting intall.");
    	
   	
    	
        boolean bok = false;
            
    	try {
    		
	        Intent newIntent = new Intent();
	        newIntent.putExtra(PackageUtil.INTENT_ATTR_APPLICATION_INFO,
	                mPkgInfo.applicationInfo);
	        newIntent.setData(mPackageURI);
	        // try this:
	        newIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | 
	        		Intent.FLAG_GRANT_READ_URI_PERMISSION | 
	         		Intent.FLAG_FROM_BACKGROUND |
	        		Intent.FLAG_ACTIVITY_NEW_TASK);
	        newIntent.setPackage("com.android.packageinstaller");
	        
	        // need to get the package-installer's package and/or name.
	        
	        String mdmInstaller = mPm.getInstallerPackageName(Constants.PACKAGE_NAME);
	        if (mdmInstaller==null) {
	        	LSLogger.debug(TAG, "setting mdminstaler to hard-coded string");
	        	mdmInstaller = "com.android.packageinstaller";
	        }
	        LSLogger.debug(TAG, "Mdmisntaller="+mdmInstaller);
	        
	        // set the target class for the intent:	        
        	// This ends up with a securityexception because the intent exists inside a package and cant be called.
	        newIntent.setClassName(mdmInstaller, "com.android.packageinstaller.InstallAppProgress") ;
	        
        	// ..and,this one throws an exception, as it should, because the class name is NOT in this application context! .setClass(this, Class.forName("com.android.packageinstaller.InstallAppProgress")); 
	        String installerPackageName = getIntent().getStringExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME);
	        if (installerPackageName != null) {
	            newIntent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, installerPackageName);
	        }
	 ///       if (getIntent().getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false)) {
	            newIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
	      //zzcommentedoutduetoerrorit causes      newIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
	 ///       }
	        LSLogger.info(TAG, "downloaded app uri="+mPackageURI);
	        startActivityForResult(newIntent, 1);   
	        LSLogger.debug(TAG, "Activity-intent started");
	        bok = true;
	        
    	//} catch (ClassNotFoundException e) {
    	//	LSLogger.exception(TAG, "startInstall error: class not found exception. ", e);
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "startInstall error: "+ex.getMessage()+" cause="+(ex.getCause()!=null?ex.getCause().toString():"(null)"), ex);
    		ex.printStackTrace();
    	}
    	
    	if (!bok)
    		launchDirect();
    }
    
    private void launchDirect() {
    	try {
    		PackageManager mPm = getPackageManager();
    		Method imeth = Utils.findMethod(mPm.getClass(), "installPackage", -3);
        	if (imeth != null) {
        		LSLogger.debug(TAG, "METHOD FOUND! " + imeth.toString());
        		// declaration:
        		//public void installPackage(final Uri packageURI, 
        		//		final IPackageInstallObserver observer, 
        		//		final int flags) {
        		
        		imeth.setAccessible(true);
        		LSLogger.debug(TAG, "accessing method... with uri="+mPackageURI.toString());
         		imeth.invoke(mPm, mPackageURI, null, 0, null);
         		LSLogger.debug(TAG, "method invoked!");
        	}
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "launchDirect error: "+ex.getMessage()+" cause="+
    				(ex.getCause()!=null?ex.getCause().toString():"(null)"), ex);
    		ex.printStackTrace();
    	}

    }
 ****/   
    

    
    /**	    
    private class PackageUtil {
          public static final String PREFIX="com.android.packageinstaller.";
          public static final String INTENT_ATTR_INSTALL_STATUS = PREFIX+"installStatus";
          public static final String INTENT_ATTR_APPLICATION_INFO=PREFIX+"applicationInfo";
          public static final String INTENT_ATTR_PERMISSIONS_LIST=PREFIX+"PermissionsList";
          //intent attribute strings related to uninstall
          public static final String INTENT_ATTR_PACKAGE_NAME=PREFIX+"PackageName";

    	// Utility method to get application information for a given java.io.File
      public static ApplicationInfo getApplicationInfo(File sourcePath) {
              final String archiveFilePath = sourcePath.getAbsolutePath();
              PackageParser packageParser = new PackageParser(archiveFilePath);
              File sourceFile = new File(archiveFilePath);
              DisplayMetrics metrics = new DisplayMetrics();
              metrics.setToDefaults();
              PackageParser.Package pkg = packageParser.parsePackage(
                    sourceFile, archiveFilePath, metrics, 0);
              if (pkg == null) {
                  return null;
              }
              return pkg.applicationInfo;
         }

    	    
    	// Utility method to get package information for a given java.io.File
       public static Package.Package getPackageInfo(File sourceFile) {
              final String archiveFilePath = sourceFile.getAbsolutePath();
              PackageParser packageParser = new PackageParser(archiveFilePath);
              DisplayMetrics metrics = new DisplayMetrics();
              metrics.setToDefaults();
              PackageParser.Package pkg =  packageParser.parsePackage(sourceFile,
                      archiveFilePath, metrics, 0);
              // Nuke the parser reference.
              packageParser = null;
              return pkg;
          }
    }
    **/
    
    
}
