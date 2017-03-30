package com.lightspeedsystems.mdm.profile;

// Constant definitions for Profiles.

public class PrfConstants {
	// payload-type definitions:
	public final static String PAYLOADTYPE_PasscodePolicy = "passcode_policy";//"com.apple.mobiledevice.passwordpolicy";
	public final static String PAYLOADTYPE_Restrictions   = "restrictions";   //"com.apple.applicationaccess";
	public final static String PAYLOADTYPE_Wifi           = "wifi";   
	public final static String PAYLOADTYPE_Email          = "email";   
	public final static String PAYLOADTYPE_Exchange       = "exchange";   
	public final static String PAYLOADTYPE_Card_dav       = "card_dav";  
	public final static String PAYLOADTYPE_Cal_dav        = "cal_dav";
	public final static String PAYLOADTYPE_Calendar       = "calendar_subscription";
	public final static String PAYLOADTYPE_WebClips       = "web_clips";
	public final static String PAYLOADTYPE_AppLauncher    = "android_app_launcher";

	// profile types as integers:
	public final static int PROFILETYPE_restrictions = 1;
	public final static int PROFILETYPE_passcode = 2;
	public final static int PROFILETYPE_wifi = 3;
	public final static int PROFILETYPE_email = 4;
	public final static int PROFILETYPE_exchange = 5;
	public final static int PROFILETYPE_card_dav = 6;
	public final static int PROFILETYPE_cal_dav = 7;
	public final static int PROFILETYPE_calendar = 8;
	public final static int PROFILETYPE_webclips = 9;
	public final static int PROFILETYPE_applauncher = 10;

	
	// payload key name tags (label names used to identify common/static sections in a profile):
//	private final static String PAYLOADKEY_payloadContent = "PayloadContent";
//	private final static String PAYLOADKEY_payloadType 	  = "PayloadType";
	
	// payload value tags:
	public final static String PAYLOADVALUE_pw_required 		= "force_pin";
	public final static String PAYLOADVALUE_pw_maxfailattempts	= "max_failed_attempts";
	public final static String PAYLOADVALUE_pw_maxtimetolock	= "max_inactivity";
	public final static String PAYLOADVALUE_pw_expiration		= "max_pin_age_in_days";
	public final static String PAYLOADVALUE_pw_expiration_msecs= "max_pin_age_in_msecs"; // internal value
	public final static String PAYLOADVALUE_pw_history			= "pin_history";
	public final static String PAYLOADVALUE_pw_minlength		= "min_length";
	public final static String PAYLOADVALUE_pw_minnumletters	= "min_letters";
	public final static String PAYLOADVALUE_pw_minlowercasechars= "min_lower_case_chars";
	public final static String PAYLOADVALUE_pw_minuppercasechars= "min_upper_case_chars";
	public final static String PAYLOADVALUE_pw_mincomplexchars  = "min_complex_chars";
	public final static String PAYLOADVALUE_pw_minnumericchars	 = "min_numeric_chars";
	public final static String PAYLOADVALUE_pw_minnonletterchars= "min_non_letter_chars";
	
	public final static String PAYLOADVALUE_pw_passcodequality = "passcode_quality";
	public final static String PAYLOADVALUE_pwquality_none		= "none";
	public final static String PAYLOADVALUE_pwquality_any 		= "any";
	public final static String PAYLOADVALUE_pwquality_numeric 	= "numeric";
	public final static String PAYLOADVALUE_pwquality_alpha 	= "alpha";
	public final static String PAYLOADVALUE_pwquality_alphanum = "alphanum";
	public final static String PAYLOADVALUE_pwquality_complex 	= "complex";
	public final static String PAYLOADVALUE_pwquality_biometric= "biometric";
	
	public final static String PAYLOADVALUE_rs_cameraEnable 	= "allow_camera";
	public final static String PAYLOADVALUE_rs_encryptionEnable= "force_encrypted_storage";

	public final static String PAYLOADVALUE_wf_ssid      		= "ssid_str";
	public final static String PAYLOADVALUE_wf_hidden			= "hidden_network";
	public final static String PAYLOADVALUE_wf_autojoin		= "auto_join";
	//public final static String PAYLOADVALUE_wf_enable_ipv6		= "enable_ipv6";
	public final static String PAYLOADVALUE_wf_encryption_type	= "encryption_type";
	public final static String PAYLOADVALUE_wf_encryption_pwd  = "password";
	public final static String PAYLOADVALUE_wf_proxy_type		= "proxy_type";
	public final static String PAYLOADVALUE_wf_proxy_server	= "proxy_server";
	public final static String PAYLOADVALUE_wf_proxy_serverport= "proxy_server_port";
	//public final static String PAYLOADVALUE_wf_proxy_username	= "proxy_user_name";
	//public final static String PAYLOADVALUE_wf_proxy_password	= "proxy_password";
	//public final static String PAYLOADVALUE_wf_proxy_pac_url	= "proxy_pac_url";
	
	public final static String PAYLOADVALUE_wf_proxytype_None	= "None";
	public final static String PAYLOADVALUE_wf_proxytype_Auto	= "Auto";
	public final static String PAYLOADVALUE_wf_proxytype_Manual= "Manual";
	
	public final static int WFENCTYPE_none = 0;
	public final static int WFENCTYPE_wep    = 1;
	public final static int WFENCTYPE_wepent = 2;
	public final static int WFENCTYPE_wpa    = 3;
	public final static int WFENCTYPE_wpaent = 4;
	public final static int WFENCTYPE_any    = 5;
	public final static int WFENCTYPE_anyent = 6;
	public final static String PAYLOADVALUE_wf_enctype_none	= "None";
	public final static String PAYLOADVALUE_wf_enctype_wep		= "WEP";
	public final static String PAYLOADVALUE_wf_enctype_wpa		= "WPA";
	public final static String PAYLOADVALUE_wf_enctype_any_per	= "Any (Personal)";
	public final static String PAYLOADVALUE_wf_enctype_wep_ent	= "WEP (Enterprise)";
	public final static String PAYLOADVALUE_wf_enctype_wep_entv3= "EWEP"; // mdm v3 wep enterprise selection
	public final static String PAYLOADVALUE_wf_enctype_wpa_ent	= "WPA / WPA2 (Enterprise)";
	public final static String PAYLOADVALUE_wf_enctype_wpa_entv3= "EWPA"; // mdm v3 wpa enterprise selection
	public final static String PAYLOADVALUE_wf_enctype_any_ent	= "Any (Enterprise)";
	public final static String PAYLOADVALUE_wf_enctype_any_entv3= "EAny";
	public final static String PAYLOADVALUE_wf_enctype_any		= "Any";
	// other wifi proxy values, object attribute names:
	public final static String PAYLOADVALUE_wf_directattributes = "attributes"; // my flag value
	public final static String PAYLOADVALUE_wf_bssid      		= "bssid_str";
	public final static String PAYLOADVALUE_wf_presharedkey = "preSharedKey";
	public final static String PAYLOADVALUE_wf_priority = "priority";
	public final static String PAYLOADVALUE_wf_wepkeys = "wepKeys";
	public final static String PAYLOADVALUE_wf_wepkeyindex = "wepTxKeyIndex";
	public final static String PAYLOADVALUE_wf_authalgs = "allowedAuthAlgorithms";
	public final static String PAYLOADVALUE_wf_groupciphers = "allowedGroupCiphers";
	public final static String PAYLOADVALUE_wf_keymgmt = "allowedKeyManagement";
	public final static String PAYLOADVALUE_wf_pwciphers = "allowedPairwiseCiphers";
	public final static String PAYLOADVALUE_wf_protos = "allowedProtocols";

	public final static String PAYLOADVALUE_wc_webclips = "web_clips";
	public final static String PAYLOADVALUE_wc_label = "label";
	public final static String PAYLOADVALUE_wc_url = "url";
	public final static String PAYLOADVALUE_wc_isremovable = "is_removable";
	public final static String PAYLOADVALUE_wc_iconurl = "icon_url";

	// email payload values:
	/*** email and exchange constants; currently not used.
	public final static String PAYLOADVALUE_em_accountname  = "email_account_name";
	public final static String PAYLOADVALUE_em_accountdesc  = "email_account_description";
	public final static String PAYLOADVALUE_em_emailaddress	= "email_address";
	public final static String PAYLOADVALUE_em_accountype      = "email_account_type"; //: "EmailTypeIMAP",
	public final static String PAYLOADVALUE_em_disablerecentsync = "disable_mail_recents_syncing";
	public final static String PAYLOADVALUE_em_preventmove     = "prevent_move"; //: false,
	
	public final static String PAYLOADVALUE_em_insvr_authtype  = "incomming_mail_server_authentication";//: "EmailAuthPassword",
	public final static String PAYLOADVALUE_em_insvr_username  = "incoming_mail_server_user_name";
	public final static String PAYLOADVALUE_em_insvr_imapprefi = "incoming_mail_server_imap_path_prefix";
	public final static String PAYLOADVALUE_em_insvr_port      = "incoming_mail_server_port_number";
	public final static String PAYLOADVALUE_em_insvr_hostname  = "incoming_mail_server_host_name";
	public final static String PAYLOADVALUE_em_insvr_usessl    = "incoming_mail_server_use_ssl"; //: false,
	public final static String PAYLOADVALUE_em_insvr_password  = "incoming_password";	

	public final static String PAYLOADVALUE_em_outsvr_username = "outgoing_mail_server_user_name";
	public final static String PAYLOADVALUE_em_outsvr_authtype = "outgoing_mail_server_authentication";//: "EmailAuthPassword",
	public final static String PAYLOADVALUE_em_outsvr_port     = "outgoing_mail_server_port_number"; //: 587,
	public final static String PAYLOADVALUE_em_ousvr_hostname  = "outgoing_mail_server_host_name";	
	public final static String PAYLOADVALUE_em_outsvr_password = "outgoing_password";
	public final static String PAYLOADVALUE_em_outsvr_usessl   = "outgoing_mail_server_use_ssl";
	public final static String PAYLOADVALUE_em_outpwsameasin   = "outgoing_password_same_as_incoming_password";
	
	public final static String PARAMEMAILTYPE_imap = "EmailTypeIMAP";
	public final static String PARAMEMAILTYPE_pop = "EmailTypePOP";	
	public final static String PARAMEMAILTYPE_exchange = "EmailTypeEXCHANGE";	
	public final static int EMAILTYPE_pop = 1;
	public final static int EMAILTYPE_imap = 2;
	public final static int EMAILTYPE_exchange = 3;
		
	// pre-defined android account types 
	public final static String AccountType_Email  = "com.lightspeedsystems.email";
	public final static String AccountType_Google = "com.google";
	***/
	
//	public final static int SETPROFILEMODE_command = 0;
//	public final static int SETPROFILEMODE_restore = 1;

	public final static int PROFILESTATE_snapshot = 1; // profile is an initially-saved snapshot
	public final static int PROFILESTATE_managed  = 2; // profile is managed, applied from command, server, etc.
	
	public final static int PWMAXTIMETOLOCKMIN = 30; // minimum allowed time in seconds for the user to set time to lock.

}
