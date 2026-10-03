package com.tbd.forkfront;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class CmdPanelSizeOpacity {

	private final CmdPanel mThisPanel;
	private final CmdPanelLayout mPanelLayout;
	private final CmdPanelResizeHandler mHandler;
	private View mRoot;
	private final CheckBox mApplyToAll;
	private final boolean mOpacityMode;
	private int mCurrentValue;
	private final TextView mValue;

	public interface CmdPanelResizeHandler {
		void onDismiss();
	}
	public CmdPanelSizeOpacity(Context context, View parent, CmdPanelLayout panelLayout, CmdPanel panel,
	                           boolean opacityMode, CmdPanelResizeHandler handler) {
		mThisPanel = panel;
		mPanelLayout = panelLayout;
		mHandler = handler;
		mOpacityMode = opacityMode;

		mRoot = Util.inflate(context, R.layout.panel_size_opacity, parent);
		final SeekBar seek = ((SeekBar)mRoot.findViewById(R.id.amount_slider));
		if(mOpacityMode) {
			seek.setMax(255);
			mCurrentValue = panel.getOpacity();
			seek.setProgress(mCurrentValue);
		} else {
			seek.setMax(20);
			mCurrentValue = panel.getRelSize();
			seek.setProgress(10 + panel.getRelSize());
		}
		seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mCurrentValue = progress;
				if(!mOpacityMode)
					mCurrentValue -= 10;
				updatePanels(false);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		mApplyToAll = (CheckBox)mRoot.findViewById(R.id.resize_mode);
		mApplyToAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(mOpacityMode)
					mPanelLayout.resetPanelOpacity();
				else
					mPanelLayout.resetPanelSize();
				updatePanels(false);
			}
		});

		mValue = (TextView)mRoot.findViewById(R.id.value);

		mRoot.findViewById(R.id.btn_0).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(mRoot != null)
				{
					updatePanels(true);
					dismiss();
				}
			}
		});

		mRoot.findViewById(R.id.btn_1).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				dismiss();
			}
		});

		updatePanels(false);

		seek.requestFocus();
		seek.requestFocusFromTouch();
	}

	public void dismiss()
	{
		if(mOpacityMode)
			mPanelLayout.resetPanelOpacity();
		else
			mPanelLayout.resetPanelSize();
		mRoot.setVisibility(View.GONE);
		((ViewGroup)mRoot.getParent()).removeView(mRoot);
		mRoot = null;
		if(mHandler != null)
			mHandler.onDismiss();
	}

	private void updatePanels(boolean permanent)
	{
		mValue.setText("" + mCurrentValue);
		boolean applyToAll = mApplyToAll.isChecked();
		if(applyToAll)
		{
			if(mOpacityMode)
				mPanelLayout.setAllPanelsOpacity(mCurrentValue, permanent);
			else
				mPanelLayout.resizeAllPanels(mCurrentValue, permanent);
		}
		else
		{
			if(mOpacityMode)
				mPanelLayout.setPanelOpacity(mThisPanel, mCurrentValue, permanent);
			else
				mPanelLayout.resizePanel(mThisPanel, mCurrentValue, permanent);
		}
	}
}
