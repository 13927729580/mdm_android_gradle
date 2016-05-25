package com.lightspeedsystems.mdm;

import java.io.File;
import java.lang.reflect.Method;
import java.text.DateFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * General application utilities and helper methods.
 * 
 */
public class Utils {
	
	private final static String TAG = "Utils";
	
	private static Context appContext;
	private static Toast lastToast = null; // holder for last toast message
	
	
	/**
	 * Gets the size in bytes of a directory tree.
	 * @param path to the directory path as a string
	 * @return size in bytes of the directory structure, if found, or 0 if not found
	 */
	public static long getDirectoryTreeSize(String path) {
		long len = 0;
		try {
			File file = new File(path);
			len = getDirectoryTreeSize(file);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "File.getdirectorytreesize string name error:", ex);
		}
		return len;
	}
	/**
	 * Gets the size in bytes of a directory tree.
	 * @param path to the directory path as a File
	 * @return size in bytes of the directory structure, if found, or 0 if not found or empty
	 */
	public static long getDirectoryTreeSize(File path) {
		long len = 0;
		try {
			File f;
			if (path.isDirectory()) {
				File[] files = path.listFiles();
				if (files != null) {
					for (int i=0; i<files.length; i++) {
						f = files[i];
						if (f.isDirectory()) {
							if (!f.getName().equals(".") && !f.getName().equals("..")) {
								len += getDirectoryTreeSize(f);
							}
						} else if (f.isFile()) {
							len += f.length();
						}
					}
				}
			} else if (path.isFile()) {
				len += path.length();
			}
		} catch (Exception ex) {
			LSLogger.exception(TAG, "File.getdirectorytreesie error:", ex);
		}
		return len;
	}
	
	/**
	 * Sets the application's context into a global static variable. This allows
	 * access to the context from any code that needed it.
	 * This only sets the context once. Other attempts to change or set it log a warning.
	 * (This can happen if the app is started from background and foreground separately.)
	 * @param ctx Context
	 */
	public static void setApplicationContext(Context ctx) {
		if (appContext == null)
			appContext = ctx;
		else if (appContext != ctx && ctx != null && LSLogger.isLoggingEnabled())
			LSLogger.warn(TAG, "Ignoring attempt to change global context from " + appContext + " to " + ctx);
	}
	
	/**
	 * Gets the application's context.
	 * @return application context
	 */
	public static Context getApplicationContext() {
		return appContext;
	}
	
	
	public static File getSDCardDownloadDir() {
		File f = Environment.getExternalStorageDirectory();
		if (f != null) {
			String path = endStringWith(f.getPath(), "/") + Constants.DOWNLOADS_DIR;
			f = new File(path);
		}
		return f;
	}

	public static File getPreferredDownloadDir() {
		return Environment.getDownloadCacheDirectory(); //.getPath(); // getExternalStorageDirectory().getPath();
	}
	
	public static File getAlternateDownloadDir() {
		return Environment.getDataDirectory(); 
	}
		
	/**
	 * Ensures a string ends with the given ending, appending it as needed.
	 * @param source string to check
	 * @param ending the desired end of the source string. Appends to source as needed to ensure source ends with this string.
	 * @return source string with the ending string, or just the ending string if source is null.
	 */
	public static String endStringWith(String source, String ending) {
		String s = source;
		if (s == null)
			s = ending;
		else if ((ending != null) && !s.endsWith(ending))
			s = s + ending;
		return s;
	}
	
	/**
	 * Encloses the given string with the end string. For example, if ends is a ", wraps str in a beginning and ending ".
	 * Only adds the ends string at the beginning and end if not already there. So a call with ("test", "x") will result in "xtestx",
	 *  but a call to ("xtest","x") will result in "xtestx".
	 * @param str string to add the ends to
	 * @param ends string to add to the beginning and end of str if not already there
	 * @return modified string, or str if no changes were made
	 */
	public static String encloseString(String str, String ends) {
		String s = str;
		if (s != null && s.length()>0) {
			if (!s.endsWith(ends))
				s = s + ends;
			if (!s.startsWith(ends))
				s = ends + s;
		}
		return s;	
	}
	
	/**
	 * Trims the given end string from the given string, removes the string from the ends. This is essentially a string.trim(ends) function.  
	 * For example, if ends is a ", this will remove leading and trailing " characters.
	 * Only removes the ends string at the beginning and end if not already there. So a call with ("xtest", "x") will result in "test",
	 *  but a call to ("xtestx","x") will result in "test". A call with "xxtexxx", "x" will return "xtexx", only removing the outer-most "x".
	 * @param str string to trim the ends string from
	 * @param ends string to remove from the beginning and end of str.
	 * @return modified string, or str if no changes were made
	 */
	public static String trimString(String str, String ends) {
		String s = str; 
		if (s != null && s.length()>0) {
			if (s.endsWith(ends))
				s = s.substring(0, s.length()-ends.length());
			if (s.startsWith(ends))
				s = s.substring(ends.length());
		}
		return s;	
	}

	
	/**
	 * Filters out certain values in the string, hiding their actual value. 
	 * Typically used for logging and anywhere something can be shown or saved/exported that
	 * may contain data that should not be exposed, such as passwords.
	 * @param s input string to check
	 * @return filtered string, or the samve string instance if no changes were needed.
	 */
	public static String filterProtectedContent(String s) {
		if (s != null && s.length()>0) {
			s = hideJSONValue(s, "\"password\":");
			s = hideJSONValue(s, "\"pc\":");
		}
		return s;
	}
	private static String hideJSONValue(String s, String label) {
		int index = s.indexOf(label);
		while (index >= 0) {
			int startindex = index+label.length(); // where to start looking for the value from
			if (label.endsWith("\""))
				startindex++;
			int indexreplacestart = s.indexOf('"', startindex);
			if (indexreplacestart > 0) {
				indexreplacestart++; // move past the opening quote
				int indexreplaceend = s.indexOf('"', indexreplacestart+1);
				if (indexreplaceend > indexreplacestart) {
					String fill = "";
					for (int x=indexreplaceend-indexreplacestart; x>0; x--)
						fill = fill + "*";
					// we have the length and positions to replace the string with
					s = s.substring(0, indexreplacestart) + fill + s.substring(indexreplaceend);
				}
			}
			// search for next occurrance:
			index = s.indexOf(label, index+2);
		}
		return s;
	}
	
	/**
	 * Adds the given string value to the json, under the given name, if the value is not null and not empty.
	 * Otherwise, does nothing.
	 * @param json JSON instance to receive the name-value pair
	 * @param name name to assign to the string
	 * @param value string value to add. Can be null or 0-length, in which case, the name-value is not stored.
	 */
	public static void addStringToJSON(JSONObject json, String name, String value) {
		try {
			if (value != null && value.length()>0 && name != null)
				json.put(name, value);
		} catch (Exception ex) {
			LSLogger.exception(TAG,  ex);
		}
	}
	/**
	 * Adds the given String array's values to the json, under the given name, if the array is not null and not 0-length.
	 * Otherwise, does nothing.
	 * @param json JSON instance to receive the name-value pair
	 * @param name name to assign to the string
	 * @param strary array of string values to add. Can be null or 0-length, in which case, the name-value is not stored.
	 */
	public static void addStringArrayToJSON(JSONObject json, String name, String[] strary) {
		try {
			if (strary != null && strary.length>0 && name != null) {
				JSONArray jary = new JSONArray();
				for (int i=0; i < strary.length; i++) {
					jary.put(i,strary[i]);
				}
				json.put(name, jary);
			}
		} catch (Exception ex) {
			LSLogger.exception(TAG,  ex);
		}
	}
	
	/**
	 * Sets a BitSet string representation back into a BitSet instance.
	 * @param target BitStr to set bits inside of.
	 * @param bsstr String representation of the bits to set, from a previous bitset.toString() call.
	 * A bitset string will look like "{0, 2, 4}" , with the set-to-true indices listed in the set.
	 */
	/**
	public static void setBitsetStrIntoBitset(BitSet target, String bsstr) {
		String ival;
 		if (bsstr != null && target != null) {
 			String[] values = bsstr.split("[{}, ]"); // split on {. }, and commas, and spaces
 			for (int i=0; i<values.length; i++) {
 				ival = values[i];
 				if (ival != null) {
 					if (ival.length()>0) {
 						try {
 							int index = Integer.parseInt(ival);
 							if (index == 0 && !ival.equals("0"))
 								index = -1;
 							if (index >= 0)
 								target.set(index);
 						} catch (Exception e) {} // ignore numberformatexception
 					}
 				}
 			} 			
 		}		
	}
	**/
	
	
	public static void setJsonArrayIntoStringArray(String[] strary, JSONArray jarry) {
		if (jarry != null && strary != null) {
			try {
			for (int i=0; i<strary.length && i<jarry.length(); i++) {
				strary[i] = jarry.getString(i);
			}
			} catch (Exception ex) {
				LSLogger.exception(TAG, "SetJsonArrayIntoStringArray error:", ex);
			}
		}
	}
	
	/**
	 * Formats a given GMT time value to a localized date-time format.
	 * Most (or all) dates/times in this app are created from the new Date().getTime() call,
	 * which gives the number of milleseconds in GMT time. The actual current timezone that
	 * should be disolayed to a user (or formatetd in data) can use this method to get a localized
	 * current-timezone relative date-time value for displaying.
	 * 
	 * @param gmtTime long time value (represented in milliseconds since January 1, 1970 00:00:00 GMT)
	 * @return Date and time formatted string
	 */
	public static String formatLocalizedDateGMT(long gmtTime) {
		String s = "";
		//SimpleDateFormat sdf = new SimpleDateFormat();
		//TimeZone timezone = TimeZone.getDefault(); // get current timezone for where the app is running
		//int offset = timezone.getOffset(gmtTime);
		//long adjustedTime = gmtTime; /// - offset;
		//LSLogger.debug(TAG, "FormatLocaqlizedDateGMT - gmtTime="+gmtTime+" offsert="+offset+" adjusted="+adjustedTime+" tz="+timezone.getDisplayName());
		try {
			s = DateFormat.getDateTimeInstance().format(gmtTime); //adjustedTime);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "FormatLocalizedDateGMT error: ", ex);
		}
		
		return s;
	}

	/**
	 * Formats a given UTC time value to a localized date-time format.
	 * 
	 * @param utcTime long time value (represented in milliseconds since January 1, 1970 00:00:00 UTC)
	 * @return Date and time formatted string
	 */
	public static String formatLocalizedDateUTC(long utcTime) {
		String s = "";
		try {
			//SimpleDateFormat sdf = new SimpleDateFormat();
			//s = sdf.format(new Date(utcTime));
			s = DateFormat.getDateTimeInstance().format(utcTime);
		} catch (Exception ex) {
			LSLogger.exception(TAG, "FormatLocalizedDateUTC error: ", ex);
		}
		
		return s;
	}
	
	
	/** 
	 * Display notification 'Toast' messages, but only if the settings allow them to be shown.
	 * @param msgResourceID message string for the application resources; gets the string as needed.
	 */
	public static void showNotificationMsg(int msgResourceID) {
		Context context = Utils.appContext; 
		String msg = context.getResources().getString(msgResourceID);
		showNotificationMsg(msg);
	}

	/** 
	 * Display notification 'Toast' messages, but only if the settings allow them to be shown.
	 * @param msg message string to display.
	 */
	public static void showNotificationMsg(String msg) {
		try {
			//no! clearNotificationMsg(); // clear any previous notif
			Context context = Utils.appContext; 
			Settings settings = Settings.getInstance(context);
			if (settings != null && settings.getSettingBoolean(Settings.DISPLAYNOTIFS, false)) {
				if (msg != null && msg.length()>0) {
					//LSLogger.debug(TAG, "toast msg: "+msg);
					Toast t = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
					t.show();
					//lastToast = t;
					LSLogger.debug(TAG, "toast msg: "+msg+" instance="+t.toString());
				}
			}
		} catch (Exception ex) { // this can happen if toast tries to display in a wrong thread.
			LSLogger.exception(TAG, "ShowNotificationMsg:", ex);
		}
	}

	/**
	 * Ensures any lastly-shown Toast notification messages are no longer available nor shown.
	 * This also fixes an issue where toast messages seem to pop up even after being shown.
	 */
	public static void clearNotificationMsg() {		
		Toast t = lastToast;
		lastToast = null;
		if (t != null) {
			LSLogger.debug(TAG, "clearing previous toast "+t.toString());
			t.cancel();
		}
	}

	public static void showPopupMessage(Context context, String title, String msg) {
		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		// 2. Chain together various setter methods to set the dialog characteristics
		builder.setMessage(msg)
		       .setTitle(title);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	               // User clicked OK button
	           }
	       });

		// 3. Get the AlertDialog from create()
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	
	/**
	 * Navigates to the window supported by the given activity class.
	 * @param context application context 
	 * @param classname name of the class to navigate to.
	 */
	@SuppressWarnings("rawtypes")
	public static void NavigateToActivity(Context context, Class classname) {
    	//LSLogger.debug(TAG, "navigating to " + classname.getSimpleName());
    	try {
	    	Intent i = new Intent(context, classname);
	    	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	context.startActivity(i);
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, ex);
    	}
	}

	/**
	 * Navigates to the main app window.
	 * @param context application context 
	 * @param flags additional optional flags to include in the intent.
	 */
	public static void NavigateToMain(Context context, int flags) {
    	//LSLogger.debug(TAG, "navigating to main window.");
		
    	try {
	    	Intent i = new Intent(context, MainActivity.class);
	   		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | flags);
	    	context.startActivity(i);
    	} catch (Exception ex) {
    		LSLogger.exception(TAG, "NavigateToMain error:", ex);
    	}
	}

	/**
	 * Reinstalls the current app from the existing source package file.
	 * @param context
	 * @return true if the reintall was initiated, false if there was any error.
	 */
	public static boolean resintallMdmApp(Context context) {
		boolean bRes = false;
		try {
			App mdmapp = App.getMdmAppPackage(context);
			LSLogger.debug(TAG, "ResintallingMdmApp from "+mdmapp.getPackageFilePath());
			CommandResult results = new CommandResult();
			boolean bstarted=Apps.installPackageFile(mdmapp, results);
			LSLogger.debug(TAG, "ResintallingMdmApp from "+mdmapp.getPackageFilePath()+ " started="+bstarted+" results="+results);
			bRes=bstarted;
		} catch (Exception ex) {
			LSLogger.exception(TAG, "Reinstall MDM error:", ex);
		}
		return bRes;
	}
	
	
	/**
	 * Given an array of string resource IDs, looks of the strings and creates
	 * a corresponding array of strings. 
	 * @param stringIds array of resource string identifiers.
	 * @return the array of strings, or an empty array if there are no string Ids.
	 */
	public static String[] buildStringArrayFromResources(int[] stringIds) {
		if (stringIds == null || stringIds.length==0) {
			return new String[0];
		} else {
			String[] s = new String[stringIds.length];
			Resources res =  appContext.getResources();
			
			for (int i=0; i<stringIds.length; i++) {
				s[i] = res.getString(stringIds[i]);
				if (s[i] == null)
					s[i] = "";
			}		
			return s;
		}
	}

	/**
	 * Dumps to the log the hierarchy and parts of the given View. Used for debugging/development.
	 * @param view
	 */
	public static void logLayouts(View view) {
		logLayouts(view, "") ;
	}

	public static void logLayouts(View view, String indent) {
		if (view != null) {
			if (indent==null)
				indent="";
			if (view.getClass().getSimpleName().equals("LinearLayout")) {
				logLinearLayout((LinearLayout)view, indent);
			} else if (view.getClass().getSimpleName().equals("RelativeLayout")) {
				logRelativeLayout((RelativeLayout)view, indent);
			} else if (view.getClass().getSimpleName().equals("TextView")) {
				TextView tv = (TextView)view;
				LSLogger.debug(TAG, indent + "TextView: text=" + tv.getText() + " id=" + Integer.toString(tv.getId()));
			} else if (view.getClass().getSimpleName().equals("Button")) {
				Button tv = (Button)view;
				LSLogger.debug(TAG, indent + "Button View: text=" + tv.getText() + " id=" + Integer.toString(tv.getId()));
			} else if (view.getClass().getSimpleName().equals("ImageView")) {
				ImageView tv = (ImageView)view;
				LSLogger.debug(TAG, indent + "ImageView:  id=" + Integer.toString(tv.getId()));
			} else 
				LSLogger.debug(TAG, indent + view.getClass().getSimpleName());
		}
	}
	
	private static void logLinearLayout(LinearLayout view, String indent) {
		if (view != null) {
			int c = view.getChildCount();
			LSLogger.debug(TAG, indent + "LinearLayout Child count is " + Integer.toString(c));
				if (c>0) {
					for (int i=0;i<c;i++) {
						View cv = view.getChildAt(i);
						logLayouts(cv, indent+"  "); 
				}
			}
		}		
	}
	
	private static void logRelativeLayout(RelativeLayout view, String indent) {
		if (view != null) {
			int c = view.getChildCount();
			LSLogger.debug(TAG, indent + "RelativeLayout Child count is " + Integer.toString(c));
				if (c>0) {
					for (int i=0;i<c;i++) {
						View cv = view.getChildAt(i);
						logLayouts(cv, indent+"  "); 
				}
			}
		}
	}
	
	// ------------------------
	// -- Refelction utilities:
	// ------------------------
	
	@SuppressWarnings("rawtypes")
	public static Method findMethod(Class c, String methodName, int numberOfArgs) {
		Method m = null;
		Method[] meths = c.getMethods();
		LSLogger.debug(TAG, "Getting methods for class " + c.getName());
		if (meths != null) {
			int limit = meths.length;
			int argcnt;
			for (int i=0; i<limit; i++) {
				argcnt = meths[i].getParameterTypes().length;
				LSLogger.debug(TAG, "Method name="+meths[i].getName() + " #args="+argcnt);
				if (meths[i].getName().equalsIgnoreCase(methodName) && 
						(numberOfArgs<0 || (numberOfArgs>=0 && numberOfArgs==argcnt)) ) {
					m = meths[i];
					break;
				}
			}
		}
		return m;
	}
	
}
