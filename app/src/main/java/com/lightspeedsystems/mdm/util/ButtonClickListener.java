package com.lightspeedsystems.mdm.util;

import android.view.View;

/** Defines the interface for button clicked events. */
public interface ButtonClickListener {
	/** 
	 * Callback when a button is pressed. 
	 * @param v View the button is in
	 * @param buttonID identifier for the button, or 0 if no ID was defined.
	 */
	public void onButtonClick(View v, int buttonID);
}
