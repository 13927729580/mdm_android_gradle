package com.lightspeedsystems.mdm.util;

import java.util.ArrayList;
import java.util.Iterator;

import android.view.View;

public class ButtonClickHandler implements View.OnClickListener  {
	public static String TAG = "ButtonClickHandler";
	
	private int buttonID;
	private ArrayList<ButtonClickListener> listeners;

	public ButtonClickHandler() {
		listeners = new ArrayList<ButtonClickListener>();
	}
	
	@Override
    public void onClick(View v) {
     	LSLogger.debug(TAG, "Button press detected in handler.");
     	// notify registered listeners:
     	if (listeners != null) {
     		Iterator<ButtonClickListener> iter = listeners.iterator();
     		while (iter.hasNext()) {
     			ButtonClickListener listener = iter.next();
     			listener.onButtonClick(v, buttonID);
     		}
     	}     	
    }

	/**
	 * Sets the optional ID of the button for this handler. 
	 * @param id an identifier for the button.
	 */
	public void setButtonID(int id) {
		buttonID = id;
	}
	
	/**
	 * Gets the button ID.
	 * @return button ID, or 0 is no ID is defined.
	 */
	public int getButtonID() {
		return buttonID;
	}
	
	/**
	 * Registers a listener for Button Clicked events.
	 * @param listener instance to register.
	 */
	public void addListener(ButtonClickListener listener) {
		listeners.add(listener);
		//LSLogger.debug(TAG, "Registered listener: " + listener.toString());
	}

	/**
	 * Unregisters a listener for Button Clicked events.
	 * @param listener instance to unregister.
	 */
	public void removeListener(ButtonClickListener listener) {
		listeners.remove(listener);
	}
	
}
