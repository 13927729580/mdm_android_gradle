package com.lightspeedsystems.mdm;

import com.lightspeedsystems.mdm.util.LSLogger;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MdmHomeWebView extends Activity {

	private static String TAG = "MdmHomeWebView";
	private WebView webView;
	private String homeUrl = "http://www.lightspeedsystems.com"; // default page to dislay
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.website_view);

        webView = (WebView) findViewById(R.id.webview);
        if (webView != null) {
        	// set webview options:
        	WebSettings webSettings = webView.getSettings();
        	webSettings.setJavaScriptEnabled(true);  // enable javascript
        	
        	// get the mdm server home url address from settings, or revert to the default defined above:
        	Settings settings = Settings.getInstance(this);
        	if (settings != null) { 
        		homeUrl = settings.getServerUrl();
        	} 
        	LSLogger.debug(TAG, "Showing web page: " + homeUrl);
        	// show the url
        	webView.loadUrl(homeUrl);
        } else {
        	LSLogger.error(TAG, "Failed to find web view.");
        }
        
    }
    
}
