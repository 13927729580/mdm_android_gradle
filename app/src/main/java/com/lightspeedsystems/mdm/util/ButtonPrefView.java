package com.lightspeedsystems.mdm.util;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Creates a line in a Preferences list with a button, along with the typical Text information (title and subtitle).
 * This is a custom Preference, instantiated by creating a reference to the class in the layout:
 * <com.lightspeedsystems.mdm.util.ButtonPrefView
            	android:id="@+id/your_id"
                android:title="your_title_ref"
                android:summary="your_subtitle_text"
                android:widgetLayout="@layout/buttonpref_widget" />
        
 * As shown above, this uses the buttonpref_widget layout to define the button control with a id of buttonCtrl; 
 * this class references that identifier to find the Button view holding button control.
 * 
 * Note that the fragment or activity that is the parent of this is responsible for setting the button label
 * and icon (if any) to be displayed.
 */

public class ButtonPrefView extends Preference {
	private static String TAG = "ButtonPrefView";
	private Button btnView;
	private String btnText;  // label for the button
	private ButtonClickHandler buttonClickHandler;

	/* Constructors; just ensures proper instantiation with calls to superclass. */
	public ButtonPrefView(Context context) {
		super(context);
		buttonClickHandler = new ButtonClickHandler();
	}
	public ButtonPrefView(Context context, AttributeSet attrs) {
		super(context,attrs);
		buttonClickHandler = new ButtonClickHandler();
	}
	public ButtonPrefView(Context context, AttributeSet attrs, int defStyle) {
		super(context,attrs,defStyle);
		buttonClickHandler = new ButtonClickHandler();
	}
	
	/**
	 * Sets an internal button identifier.
	 * @param id identifier
	 */
	public void setButtonID(int id) {
		if (buttonClickHandler != null)
			buttonClickHandler.setButtonID(id);
	}
	
	/**
	 * Gets the button identifier that was set from @setButton.
	 * @return button identifier, or 0 if no identifier was set.
	 */
	public int getButtonID() {
		if (buttonClickHandler != null)
			return buttonClickHandler.getButtonID();
		return 0;
	}
	
	/**
	 * Registers a listener for Button Clicked events.
	 * @param listener instance to register.
	 */
	public void registerButtonClickListener(ButtonClickListener listener) {
		if (buttonClickHandler != null)
			buttonClickHandler.addListener(listener);
	}
	
    // 
	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = super.onCreateView(parent);
		return view;
	}
	
	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		try {
			//Utils.logLayouts(view);
			btnView = (Button)view.findViewById(com.lightspeedsystems.mdm.R.id.buttonCtrl);
			if (btnView != null) {
				btnView.setOnClickListener(buttonClickHandler);
				// set the button's text: has to be done here, via this late-binding lifecylce method.
				if (btnText != null)
					btnView.setText(btnText);
			} else
				buttonClickHandler = null;
			
			//LSLogger.debug(TAG, "OnBindView - Button view  btnView="+(btnView==null?"null":btnView.toString()));
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}
	}
	
	/**
	 * Sets the button's text label. 
	 * @param text String label to set.
	 */
	public void setButtonLabel(String text) {
		//LSLogger.info(TAG, "setButtonLabel - Button view  btnView="+(btnView==null?"null":btnView.toString()));

		btnText = text;
		if (btnView != null) {
			btnView.setText(text);
			//LSLogger.debug(TAG, "BUTTON LABEL SET to: " + (text==null?"(null)":text) + " in " + this.toString());
		} else {
			//LSLogger.warn(TAG, "BUTTON TEXT SET to: " + (text==null?"(null)":text) + " (buttonView not present) in " + this.toString());
		}
	}
		
}
