package com.tbd.forkfront;

import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.view.View;
import com.tbd.forkfront.*;

public class NHW_Status implements NH_Window
{
	private SpannableStringBuilder[] mRows;
	private int mCurRow;
	private NetHackIO mIO;
	private UI mUI;
	private boolean mIsVisible;
	private int mWid;
	private int mOpacity;
	boolean mOldMode; // TODO get rid of this by fixing proper cursor movement and remove append parameter to putString

	// ____________________________________________________________________________________
	public NHW_Status(Activity context, NetHackIO io)
	{
		mIO = io;
		mRows = new SpannableStringBuilder[2];
		mRows[0] = new SpannableStringBuilder();
		mRows[1] = new SpannableStringBuilder();
		mCurRow = 0;
		mOldMode = context.getResources().getBoolean(R.bool.oldStatusMode);
		setContext(context);
	}

	// ____________________________________________________________________________________
	@Override
	public String getTitle()
	{
		return "NHW_Status";
	}
	
	// ____________________________________________________________________________________
	@Override
	public void setContext(Activity context)
	{
		mUI = new UI(context);
		if(mIsVisible)
			mUI.showInternal();
		else
			mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	@Override
	public void show(boolean bBlocking)
	{
		mIsVisible = true;
		mUI.showInternal();
		if(bBlocking)
		{
			// unblock immediately
			mIO.sendKeyCmd(' ');
		}
	}

	// ____________________________________________________________________________________
	@Override
	public void destroy()
	{
		hide();
	}

	// ____________________________________________________________________________________
	public void hide()
	{
		mIsVisible = false;
		mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	public void setId(int wid)
	{
		mWid = wid;
	}
	
	// ____________________________________________________________________________________
	@Override
	public int id()
	{
		return mWid;
	}

	// ____________________________________________________________________________________
	@Override
	public void clear()
	{
		mRows[0] = new SpannableStringBuilder();
		mRows[1] = new SpannableStringBuilder();
		mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	@Override
	public void printString(int attr, String str, int append, int color)
	{
		if(mOldMode) {
			if(append == 0 || str.length() < mRows[mCurRow].length()) {
				mRows[mCurRow] = new SpannableStringBuilder(TextAttr.style(str, attr, color));
			} else {
				int nToAppend = str.length() - mRows[mCurRow].length();
				if(nToAppend > 0)
					mRows[mCurRow].append(TextAttr.style(str.substring(mRows[mCurRow].length(), str.length()), attr, color));
			}
		} else {
			mRows[mCurRow].append(TextAttr.style(str, attr, color));
		}
	}

	public void redraw() {
		mUI.update();
	}

	// ____________________________________________________________________________________
	@Override
	public void setCursorPos(int x, int y)
	{
		mCurRow = y == 0 ? 0 : 1;
	}

	// ____________________________________________________________________________________
	@Override
	public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		return KeyEventResult.IGNORED;
	}

	// ____________________________________________________________________________________
	public float getHeight()
	{
		return mUI.getHeight();
	}

	// ____________________________________________________________________________________
	@Override
	public void preferencesUpdated(SharedPreferences prefs)
	{
		mOpacity = prefs.getInt("statusOpacity", 0);
		mUI.updateOpacity();
	}

	// ____________________________________________________________________________________ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private AutoFitTextView[] mViews;

		// ____________________________________________________________________________________
		public UI(Activity context)
		{
			mViews = new AutoFitTextView[2];
			mViews[0] = (AutoFitTextView)context.findViewById(R.id.nh_stat0);
			mViews[1] = (AutoFitTextView)context.findViewById(R.id.nh_stat1);
			updateOpacity();
		}

		// ____________________________________________________________________________________
		public void showInternal()
		{
			//update();
			mViews[0].setVisibility(View.VISIBLE);
			mViews[1].setVisibility(View.VISIBLE);
		}

		// ____________________________________________________________________________________
		public void hideInternal()
		{
			mViews[0].setVisibility(View.GONE);
			mViews[1].setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		public void update()
		{
			mViews[0].setText(mRows[0]);
			mViews[1].setText(mRows[1]);
			mRows[0] = new SpannableStringBuilder();
			mRows[1] = new SpannableStringBuilder();
		}

		// ____________________________________________________________________________________
		public float getHeight()
		{
			return mViews[0].getMinTextSize() * 2;
		}

		// ____________________________________________________________________________________
		public void updateOpacity()
		{
			mViews[0].setBackgroundColor(mOpacity << 24);
			mViews[1].setBackgroundColor(mOpacity << 24);
		}
	}
}
