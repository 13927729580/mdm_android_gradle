package com.lightspeedsystems.mdm.profile;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.lightspeedsystems.mdm.util.LSLogger;

/**
 * Utilities for user accounts, including email, contacts, and other accounts that may exist on the device.
 */
public class AccountUtils {
	private static String TAG = "AccountUtils";

	/**
	 * Gets the user's name as set in the device. This is from Contact info.
	 * Note that this should only be used in ice cream sandwich and later versions.
	 * @return user's name, or null if could not get it.
	 */
	public static String getUserNameFromContacts(Context context) {
		String userName = null;
		try {
			ContentResolver cr = context.getContentResolver();
		    Cursor cur = cr.query(
		            ContactsContract.Profile.CONTENT_URI, null, null, null, null);
		    if (cur != null) { 
		    	//LSLogger.debug(TAG, "Cursor count="+cur.getCount());
		        if (cur.getCount() > 0) 
		        while (cur.moveToNext()) {
		            String name = cur
		                    .getString(cur
		                            .getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
		            
		            if (name != null) {   
						userName = name;
						break;
		            } else LSLogger.debug(TAG, "UserContacts DisplayName not found.");
		        }

		        cur.close();
		    }
		} catch (Exception ex) {
			LSLogger.exception(TAG, "getUserNameFromContacts error:", ex);
		}
		return userName;
	}

	/**
	 * Searches for an account by name and optionally by type.
	 * @param context application context
	 * @param acctName name to search for. the name is non-case sensitive.
	 * @param acctType account type name to search; if null, searches for all account types. 
	 * (Hint: use one of the PrfConstants.AccountType_ values).
	 * @return Existing Account instance if found, null if not found.
	 */
	public static Account findAccountByName(Context context, String acctName, String acctType) {
		Account acct = null;
//		LSLogger.debug(TAG, "Finding account...");
		try {
		    AccountManager acctMgr = AccountManager.get(context);
		    Account[] accounts = acctMgr.getAccountsByType(acctType);
		    if (accounts != null && acctName != null) {
		    	for (int i=0; i<accounts.length; i++) {
		    		Account a = accounts[i];
//		    		dumpAccountInfo(context, a, false);
		    		if (a.name.equalsIgnoreCase(acctName)) {
		    			LSLogger.debug(TAG, "FindAccountByName - found: accountname="+a.name+" type="+a.type);
		    			acct = a;
		    			break;
		    		}
		    	}
		    }
		} catch (Exception ex) {
			LSLogger.exception(TAG, "FindAccountByName error:", ex);
		}
		return acct;
	}
	
	/** - not currently used, but is a sample of looking into account details.
	// dumps contents of the account to the logger. Shows name and type only; if showAll is true, gets all of it.
	private static void dumpAccountInfo(Context context, Account account, boolean showAll) {
		if (account != null) {
			LSLogger.debug(TAG, "Account: name="+account.name + " type="+account.type + " instance="+account.toString());
			if (showAll) { // show details: 
				// .. ToDo ..
			}
			//Bundle b = account.
			try {
			AccountManager accMgr = AccountManager.get(context);
			String em=accMgr.getUserData(account, "EMAIL");
			String inc=accMgr.getUserData(account, "INCOMING");
			String otg=accMgr.getUserData(account, "OUTGOING");
			String dft=accMgr.getUserData(account, "DEFAULT");
			LSLogger.debug(TAG, "-Account Values: email="+(em==null?"null":em)+" in="+(inc==null?"null":inc)+" out="+(otg==null?"null":otg)+" default="+(dft==null?"null":dft));
			} catch (Exception ex) {
				LSLogger.exception(TAG,"dumpAccountInfo error:", ex);
			}
		}
	}
	**/
	
	
	/** sample code of how to get account info and email info:
	  private static Account getAccount(AccountManager accountManager) {
		    Account[] accounts = accountManager.getAccountsByType("com.google");
		    Account account;
		    if (accounts.length == 0) 
		    	accounts = accountManager.getAccounts();
		    if (accounts.length > 0) {
		      account = accounts[0];
		    } else {
		      account = null;
		    }
		    return account;
		  }

		  public String getUserEmail() {
		    AccountManager accountManager = AccountManager.get(context);
		    Account account = getAccount(accountManager);

		    if (account == null) {
		      return null;
		    } else {
		      return account.name;
		    }
		  }
	 */
	
	
	/*sample code:--using account mgr and extracting from email address: not what I want to use.
    AccountManager manager = AccountManager.get(context);
    Account account = getAccount(manager);
    
    if (account == null) {
      return "";
    } else {
      String email = account.name;
      String[] parts = email.split("@");
      if (parts.length > 0 && parts[0] != null)
    	  userName = parts[0];
      else
        return "";
    }
     */
	

	
}
