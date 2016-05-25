package com.lightspeedsystems.mdm.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.preference.EditTextPreference;


/**
 * Creates a line in a Preferences list with an editable text string for displaying a value along with the typical Text information (title and subtitle).
 * The text value is shown to the right side, and is edited by clicking on the preference line in the view. 
 * This is a custom Preference, which inherits from EditTextPreference, so that editing support is provided in a separate popup dialog, 
 * as standard preference value editing does. Otherwise, this class is very similar to @StaticTextPrevView.
 * 
 * Instances of this class are instantiated by creating a reference to the class in the layout:
 * <com.lightspeedsystems.mdm.util.EditTextPrefView
            	android:id="@+id/your_id"
                android:title="your_title_ref"
                android:summary="your_subtitle_text"
                android:widgetLayout="@layout/textvaluepref_widget" />
        
 * As shown above, this uses the textvaluepref_widget layout to define the text control with a id of textCtrl; 
 * this class reference that identifier to find the TextView holding and showing the text value.
 * 
 * Note that the fragment or activity that is the parent of this is responsible for setting the value to be displayed.
 * Due to lifecycle and timing, the value may be set before the TextView is known; internally, that is handled by
 * saving references to the TextView control and the string value, and thereby sets the value when the time is right.
 */
public class EditTextPrefView extends EditTextPreference {
	private static String TAG = "EditTextPrefView";
	private String textValue = "";
	private TextView textView;

	/* Constructors; just ensures proper instantiation with call to superclass. */
	public EditTextPrefView(Context context) {
		super(context);
	}
	public EditTextPrefView(Context context, AttributeSet attrs) {
		super(context,attrs);
	}
	public EditTextPrefView(Context context, AttributeSet attrs, int defStyle) {
		super(context,attrs,defStyle);
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
	public void setText(String text) {
		super.setText(text);
		textValue = text;
		setTextViewValue(textValue);
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
	
	protected void onDialogClosed (boolean positiveResult) {
		notifyChanged();
		super.onDialogClosed(positiveResult);
		LSLogger.debug(TAG, "Dialog closed: " + (positiveResult?"OK":"Cancel"));
	}
}
