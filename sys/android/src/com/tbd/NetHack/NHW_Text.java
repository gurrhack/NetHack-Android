package com.tbd.NetHack;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ScrollView;
import android.widget.TextView;

public class NHW_Text implements NH_Window
{
	private NetHackIO mIO;
	private boolean mIsBlocking;
	private boolean mIsVisible;
	private SpannableStringBuilder mBuilder;
	private UI mUI;
	private int mWid;
	
	// ____________________________________________________________________________________
	public NHW_Text(int wid, Activity context, NetHackIO io)
	{
		mWid = wid;
		mIO = io;
		mBuilder = new SpannableStringBuilder();
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
	public void clear()
	{
		mBuilder = new SpannableStringBuilder();
		mUI.update();
	}

	// ____________________________________________________________________________________
	public void printString(Spanned str)
	{
		if(mBuilder.length() > 0)
			mBuilder.append('\n');
		mBuilder.append(str);
		mUI.update();
	}

	// ____________________________________________________________________________________
	public void printString(TextAttr attr, String str, int append)
	{
		if(mBuilder.length() > 0)
			mBuilder.append('\n');
		mBuilder.append(attr.style(str));
		mUI.update();
	}

	// ____________________________________________________________________________________
	public void show(boolean bBlocking)
	{
		mIsBlocking = bBlocking;
		mIsVisible = true;
		mUI.showInternal();
	}

	// ____________________________________________________________________________________
	public void hide()
	{
		mIsVisible = false;
		mUI.hideInternal();
	}
	
	// ____________________________________________________________________________________
	public int id()
	{
		return mWid;
	}

	// ____________________________________________________________________________________
	public boolean isBlocking()
	{
		return mIsBlocking;
	}

	// ____________________________________________________________________________________
	private void close()
	{
		if(mIsBlocking)
			mIO.sendKeyCmd(' ');
		mIsBlocking = false;
		hide();
	}

	// ____________________________________________________________________________________
	public void scrollToEnd()
	{
		mUI.scrollToEnd();
	}

	// ____________________________________________________________________________________
	public int handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		return mUI.handleKeyDown(ch, nhKey, keyCode, modifiers, bSoftInput) ? 1 : 0;
	}

	// ____________________________________________________________________________________
	public boolean isVisible()
	{
		return mIsVisible;
	}
	
	// ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private TextView mTextView;
		private ScrollView mScroll;

		public UI(Activity context)
		{
			mScroll = (ScrollView)Util.inflate(context, R.layout.textwindow, R.id.dlg_frame);
			mTextView = (TextView)mScroll.findViewById(R.id.text_view);

			mTextView.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					if(isVisible())
						close();
				}
			});

			hideInternal();
		}

		// ____________________________________________________________________________________
		public void update()
		{
			if(isVisible())
			{
				if(mBuilder.length() > 0)
					mTextView.setText(mBuilder);
				else
					mTextView.setText(null);
			}
		}

		// ____________________________________________________________________________________
		public void showInternal()
		{
			update();
			mScroll.setVisibility(View.VISIBLE);
		}

		// ____________________________________________________________________________________
		public void hideInternal()
		{
			mScroll.setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		public void scrollToEnd()
		{
			if(isVisible())
			{
				mScroll.post(new Runnable() // gives the view a chance to update itself
				{
					public void run()
					{
						mScroll.fullScroll(ScrollView.FOCUS_DOWN);
					}
				});
			}
		}

		// ____________________________________________________________________________________
		public boolean handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, boolean bSoftInput)
		{
			switch(keyCode)
			{
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_BACK:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				if(isVisible())
				{
					close();
					return true;
				}

			case KeyEvent.KEYCODE_DPAD_UP:
				mScroll.scrollBy(0, -mTextView.getLineHeight());
				return true;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				mScroll.scrollBy(0, mTextView.getLineHeight());
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				mScroll.pageScroll(View.FOCUS_UP);
				return true;
			case KeyEvent.KEYCODE_SPACE:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				mScroll.pageScroll(View.FOCUS_DOWN);
				return true;
			}
			return false;
		}
	}
}
