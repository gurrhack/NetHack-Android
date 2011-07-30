package com.tbd.NetHack;

import java.util.Set;

import android.app.Activity;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

public class NHW_Status implements NH_Window
{
	private Spanned[] mRows;
	private int mCurRow;
	private NetHackIO mIO;
	private UI mUI;
	private boolean mIsVisible;
	private int mWid;

	// ____________________________________________________________________________________
	public NHW_Status(Activity context, NetHackIO io)
	{
		mIO = io;
		mRows = new Spanned[2];
		mCurRow = 0;
		setContext(context);
	}

	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		mUI = new UI(context);
		if(mIsVisible)
			mUI.showInternal();
		else
			mUI.hideInternal();
	}

	// ____________________________________________________________________________________
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
	public int id()
	{
		return mWid;
	}

	// ____________________________________________________________________________________
	public boolean isBlocking()
	{
		return false;
	}

	// ____________________________________________________________________________________
	public void clear()
	{
		mRows[0] = null;
		mRows[1] = null;
		mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	public void printString(TextAttr attr, String str, int append)
	{
		mRows[mCurRow] = attr.style(str);
		mUI.update();
	}

	// ____________________________________________________________________________________
	public void setRow(int y)
	{
		mCurRow = y == 0 ? 0 : 1;
	}

	// ____________________________________________________________________________________
	public int handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		return 0;
	}

	// ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private TextView[] mViews;

		// ____________________________________________________________________________________
		public UI(Activity context)
		{
			mViews = new TextView[2];
			mViews[0] = (TextView)context.findViewById(R.id.nh_stat0);
			mViews[1] = (TextView)context.findViewById(R.id.nh_stat1);
		}

		// ____________________________________________________________________________________
		public void showInternal()
		{
			update();
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
		}
	}
}
