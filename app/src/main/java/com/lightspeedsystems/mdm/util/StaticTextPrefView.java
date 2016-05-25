package com.lightspeedsystems.mdm.util;


import com.lightspeedsystems.mdm.util.ButtonClickListener;
import com.lightspeedsystems.mdm.util.ButtonClickHandler;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Creates a line in a Preferences list with a static text string for displaying a value along with the typical Text information (title and subtitle).
 * This is a custom Preference, instantiated by creating a reference to the class in the layout:
 * <com.lightspeedsystems.mdm.util.StaticTextPrefView
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
public class StaticTextPrefView extends Preference {
	private static String TAG = "StaticTextPrefView";
	private String textValue = "";
	private TextView textView;
	private ImageView iconView;  // the icon part of the view
	private ButtonClickHandler iconClickHandler;

	/* Constructors; just ensures proper instantiation with call to superclass. */
	public StaticTextPrefView(Context context) {
		super(context);
		iconClickHandler = new ButtonClickHandler();
	}
	public StaticTextPrefView(Context context, AttributeSet attrs) {
		super(context,attrs);
		iconClickHandler = new ButtonClickHandler();
	}
	public StaticTextPrefView(Context context, AttributeSet attrs, int defStyle) {
		super(context,attrs,defStyle);
		iconClickHandler = new ButtonClickHandler();
	}
	
    // 
	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = super.onCreateView(parent);
		//LSLogger.info(TAG, "OnCreateView - logging layouts:");
		//Utils.logLayouts(view);
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

			// get the icon control:
			iconView = (ImageView)view.findViewById(android.R.id.icon);
			//LSLogger.debug(TAG, "Image button is: "+ (iconView==null?"null":iconView.toString()));
			if (iconView != null && iconView.getDrawable()!=null) 
				iconView.setOnClickListener(iconClickHandler);
			else // no need to keep the handler around if we dont have the icon
				iconClickHandler = null;
		    
			setTextViewValue(textValue); // update the data to be this value.
		
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}
	}

	/** 
	 * Registers a button click listener for detecting clicking on the optional icon.
	 * Consumers can detect the click event based on the view info that gets passed to the callback.
	 * @param listener ButtonClickListener implementation
	 */
	public void registerIconButtonClickListener(ButtonClickListener listener) {
		if (iconClickHandler != null)
			iconClickHandler.addListener(listener);
	}
	
	public void setIconID(int id) {
		if (iconClickHandler != null)
			iconClickHandler.setButtonID(id);
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
		//LSLogger.debug(TAG, "call to setText");
		textValue = text;
		setTextViewValue(textValue);
	}

	// internal method for actually setting the value.
	private void setTextViewValue(String s) {
		if (s==null)
			s="";
		if (textView != null) {
			textView.setText(s);
			//LSLogger.debug(TAG, "TEXT SET to: " + (s==null?"(null)":s) + " in " + this.toString());
		} else {
			//LSLogger.warn(TAG, "TEXT SET to: " + (s==null?"(null)":s) + " (textView not present) in " + this.toString());
		}
	}
	
	
}
