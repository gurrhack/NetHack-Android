/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.tbd.forkfront;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SliderPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
	private static final String androidns = "http://schemas.android.com/apk/res/android";
	private static final String thisns = "forkfront";

	private SeekBar mSeekBar;
	private TextView mSplashText, mValueText;
	private Context mContext;

	private String mDialogMessage, mSuffix;
	private int mDefault, mMin, mMax, mValue;

	public SliderPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;

		mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
		mSuffix = attrs.getAttributeValue(androidns, "text");
		mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
		mMin = attrs.getAttributeIntValue(thisns, "min", 0);
		mMax = attrs.getAttributeIntValue(androidns, "max", 100);
	}

	@Override
	protected View onCreateDialogView()
	{
		LinearLayout.LayoutParams params;
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);

		mSplashText = new TextView(mContext);
		if(mDialogMessage != null)
			mSplashText.setText(mDialogMessage);
		layout.addView(mSplashText);

		mValueText = new TextView(mContext);
		mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
		mValueText.setTextSize(32);
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(mValueText, params);

		mValue = getPersistedInt(mDefault);

		mSeekBar = new SeekBar(mContext);
		mSeekBar.setMax(mMax - mMin);
		mSeekBar.setProgress(mValue - mMin);
		mSeekBar.setOnSeekBarChangeListener(this);
		layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		onProgressChanged(mSeekBar, mValue - mMin, false);

		return layout;
	}

	@Override
	protected void onBindDialogView(View v)
	{
		super.onBindDialogView(v);
		mSeekBar.setMax(mMax - mMin);
		mSeekBar.setProgress(mValue - mMin);
	}

	@Override
	protected void onSetInitialValue(boolean restore, Object defaultValue)
	{
		super.onSetInitialValue(restore, defaultValue);
		if(restore)
			mValue = getPersistedInt(mDefault);
		else
			mValue = (Integer)defaultValue;
	}

	@Override
	public void onProgressChanged(SeekBar seek, int progress, boolean fromTouch)
	{
		mValue = progress + mMin;
		String t = String.valueOf(mValue);
		mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
		if(shouldPersist())
			persistInt(mValue);
		callChangeListener(new Integer(mValue));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seek)
	{
	}

	@Override
	public void onStopTrackingTouch(SeekBar seek)
	{
	}
}
