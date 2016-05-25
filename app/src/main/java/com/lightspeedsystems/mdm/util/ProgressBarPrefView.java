package com.lightspeedsystems.mdm.util;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;


public class ProgressBarPrefView extends Preference {
	private static String TAG = "ProgessBarPrefView";
	private ProgressBar barView;
	private TextView textView; // for displaying the percentage as a string
	private int maxRange = 100;
	private int primaryProgress=-1;
	private int secondaryProgress=-1;
	private boolean bShowText;
	
	/* Constructors; just ensures proper instantiation with calls to superclass. */
	public ProgressBarPrefView(Context context) {
		super(context);
	}
	public ProgressBarPrefView(Context context, AttributeSet attrs) {
		super(context,attrs);
	}
	public ProgressBarPrefView(Context context, AttributeSet attrs, int defStyle) {
		super(context,attrs,defStyle);
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
			barView = (ProgressBar)view.findViewById(com.lightspeedsystems.mdm.R.id.progressbarCtrl);
			textView = (TextView)view.findViewById(com.lightspeedsystems.mdm.R.id.progresstextCtrl);
			barView.setMax(maxRange);
			if (primaryProgress >= 0) {
				barView.setProgress(primaryProgress);
				showPecentageText();
			}
			if (secondaryProgress >= 0)
				barView.setSecondaryProgress(secondaryProgress);
		} catch (Exception ex) {
			LSLogger.exception(TAG, ex);
		}
	}
	
	public void setShowText(boolean show) {
		bShowText = show;
	}
	
	public void setMaxRange(int max) {
		maxRange = max;
		if (barView != null)
			barView.setMax(max);
	}
	
	public void setPrimaryProgress(int progress) {
		primaryProgress = progress;
		if (barView != null) {
			barView.setProgress(primaryProgress);
			showPecentageText();
		}
	}
		
	public void setSecondaryProgress(int progress) {
		secondaryProgress = progress;
		if (barView != null)
			barView.setSecondaryProgress(secondaryProgress);
	}	
	
	private void showPecentageText() {
		if (bShowText && (textView != null)) {
			if (primaryProgress >= 0) {
				String text = Integer.toString(primaryProgress) + " %";
				textView.setText(text);
				textView.setVisibility(View.VISIBLE);
				//LSLogger.debug(TAG, "Set progress text to: "+text);
			} else {
				textView.setVisibility(View.GONE); //INVISIBLE);
				//LSLogger.debug(TAG, "Set progress text to hidden.");
			}
		}
	}
	
}
