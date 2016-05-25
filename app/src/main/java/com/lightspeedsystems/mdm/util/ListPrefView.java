package com.lightspeedsystems.mdm.util;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Custom view control of a ListPreference. Adds a viewable text value to the control, for
 * displaying the current setting value (ListPreference does not show the current value in the main view).
 */
public class ListPrefView extends ListPreference {
	private static String TAG = "ListPrefView";
	private String textValue = "";
	private TextView textView;

	/* Constructors; just ensures proper instantiation with call to superclass. */
	public ListPrefView(Context context) {
		super(context);
	}
	public ListPrefView(Context context, AttributeSet attrs) {
		super(context,attrs);
	}
	
    // 
	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = super.onCreateView(parent);
	//	LSLogger.info(TAG, "OnCreateView - logging view:");
	//	Utils.logLayouts(view);
		return view;
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		try {
			//LSLogger.info(TAG, "OnBindView - logging view:");
			//Utils.logLayouts(view);
			textView = (TextView)view.findViewById(com.lightspeedsystems.mdm.R.id.textCtrl);
			//LSLogger.info(TAG, "TextView control found " + textView.toString());
		    setTextViewValue(textValue); // update the data to be this value.
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}
	}

	/** Gets the text value from the text information field widget, on the right side of the line.
	 * @return String value or null if not set. May return an empty string as well.
	 */
	public String getText() {
		return textValue;
	}

	/**
	 * Sets the text information field, on the right side of the displayed line. 
	 * @param text String value to set, or null to clear the value.
	 */
	public void setText(CharSequence text) {
		//super.setText(text);
		if (text == null)
			textValue = "";
		else
			textValue = text.toString();
		setTextViewValue(textValue);
	}
	
	/**
	 * Sets the text information field, on the right side of the displayed line. 
	 * @param text String value to set, or null to clear the value.
	 */
	public void setText(String text) {
		//super.setText(text);
		textValue = text;
		setTextViewValue(textValue);
		//LSLogger.debug(TAG, "Setting text to "+textValue);		
	}

	// internal method for actually setting the value.
	private void setTextViewValue(String s) {
		if (s==null)
			s="";
		if (textView != null) {
			textView.setText(textValue);
			//LSLogger.info(TAG, "TEXT SET to: " + (textValue==null?"(null)":textValue) + " in " + this.toString());
		} else {
			//LSLogger.info(TAG, "TEXT SET to: " + (textValue==null?"(null)":textValue) + " (textView not present) in " + this.toString());
		}
	}

}
