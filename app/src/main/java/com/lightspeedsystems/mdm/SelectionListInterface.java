package com.lightspeedsystems.mdm;

import android.app.ListFragment;

/**
 * Selection List callback methods for consumers of SelectionList classes. Typically, this will be implemented by an activity.
 */
public interface SelectionListInterface {

		/**
		 * Called when an item in the list is selected.
		 * @param position position index into list of the selections (typically is the same as the id parameter).
		 * @param id 0-based row index into list of the selections.
		 * @param fragmentInstance implementation of the fragment that is providing this notification; may be null.
		 */
		public void onListItemSelected(int position, long id, ListFragment fragmentInstance);
		
		/**
		 * Gets the list of string items to fill the contents of the list.
		 * @return array of string values.
		 */
		public String[] getSelectionItems();

}
