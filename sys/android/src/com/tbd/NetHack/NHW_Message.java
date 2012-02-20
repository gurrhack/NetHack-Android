package com.tbd.NetHack;

import java.util.Set;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class NHW_Message implements NH_Window
{
	private class LogEntry
	{
		public Spanned msg;
		public int repeat;
	}
	private NetHackIO mIO;
	private Activity mContext;
	private final int MaxLog = 256;
	private LogEntry[] mLog = new LogEntry[MaxLog];
	private int mCurrentIdx;
	private int mLogCount;
	private int mDispCount;
	private UI mUI;
	private NHW_Text mLogView;
	private boolean mIsVisible;
	private int mWid;

	// ____________________________________________________________________________________
	public NHW_Message(Activity context, NetHackIO io)
	{
		mIO = io;
		for(int i = 0; i < mLog.length; i++)
			mLog[i] = new LogEntry();
		setContext(context);
	}

	// ____________________________________________________________________________________
	public String getTitle()
	{
		return "NHW_Message";
	}
	
	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		if(mContext == context)
			return;
		mContext = context;
		mUI = new UI();
		if(mIsVisible)
			mUI.showInternal();
		else
			mUI.hideInternal();
		if(mLogView != null)
			mLogView.setContext(context);
	}

	// ____________________________________________________________________________________
	public int handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		int ret;
		if(isLogShowing() && (ret = mLogView.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput)) > 0)
			return ret;
		return mUI.handleKeyDown(ch, nhKey, keyCode, modifiers, bSoftInput) ? 1 : 0;
	}

	// ____________________________________________________________________________________
	public void clear()
	{
		// mCurrentIdx = getIndex(mLogCount - 1);
		mDispCount = 0;
		mUI.clear();
	}

	// ____________________________________________________________________________________
	private int getIndex(int i)
	{
		if(mLogCount == 0)
			return 0;
		return i & (MaxLog - 1);
	}
	
	// ____________________________________________________________________________________
	private int getOldestIndex()
	{
		if(mLogCount <= MaxLog)
			return 0;
		return getIndex(mLogCount + 1);
	}
	
	// ____________________________________________________________________________________
	public void printString(TextAttr attr, String str, int append)
	{
		mCurrentIdx = getIndex(mLogCount - 1);
		
		if(append < 0 && mLogCount > 0)
		{
			append++;
			Spanned l = mLog[mCurrentIdx].msg;
			if(append < -l.length())
				append = -l.length();
			l = (Spanned)l.subSequence(0, l.length() + append);
			mLog[mCurrentIdx].msg = new SpannedString(TextUtils.concat(l, attr.style(str)));
		}
		else if(append > 0 && mLogCount > 0)
		{
			if(str.length() > 0)
				mLog[mCurrentIdx].msg = new SpannedString(TextUtils.concat(mLog[mCurrentIdx].msg, attr.style(str)));
		}
		else
		{
			Spanned newMsg = attr.style(str);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			boolean grouping = prefs.getBoolean("groupLog", false);
			if(grouping && mLogCount > 1)
			{
				Spanned prev = mLog[mCurrentIdx].msg;
				if(prev.toString().equals(newMsg.toString()))
				{
					mLog[mCurrentIdx].repeat++;
					mDispCount++;
				}
				else
					addMessage(newMsg);
			}
			else
				addMessage(newMsg);
		}
		mUI.update();
	}

	// ____________________________________________________________________________________
	private void addMessage(Spanned newMsg) 
	{		
		mCurrentIdx = getIndex(mCurrentIdx + 1);
		mLog[mCurrentIdx].msg = newMsg;
		mLog[mCurrentIdx].repeat = 1;
		mDispCount++;
		mLogCount++;
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
	public boolean isBlocking()
	{
		return false;
	}

	// ____________________________________________________________________________________
	public void showPrev()
	{
		if(mCurrentIdx == getOldestIndex())
			return;

		mCurrentIdx = getIndex(mCurrentIdx - 1);
		mDispCount = 1;

		mUI.update();
	}

	// ____________________________________________________________________________________
	public void showLog()
	{
		if(mLogView == null)
			mLogView = new NHW_Text(0, mContext, mIO);

		mLogView.clear();
		int i = mCurrentIdx + 1;
		for(int n = 0; n < MaxLog; n++, i++)
		{
			LogEntry e = mLog[getIndex(i)];
			Spanned s = e.msg;
			if(s != null)
			{
				if(e.repeat > 1)
					mLogView.printString(new SpannedString(TextUtils.concat(s, " (" + Integer.toString(e.repeat) + ")")));
				else
					mLogView.printString(s);
			}
		}
		mLogView.show(false);
		mLogView.scrollToEnd();
		int disp = mDispCount < 3 ? mDispCount : 3;
		clear();
		mDispCount = disp;
		mUI.update();
	}

	// ____________________________________________________________________________________
	private boolean isLogShowing()
	{
		return mLogView != null && mLogView.isVisible();
	}

	// ____________________________________________________________________________________
	public void setCursorPos(int x)
	{
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
	
	// ____________________________________________________________________________________ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private TextView m_view;
		private View m_more;
		private boolean mTextUpdaterRunning;

		// ____________________________________________________________________________________
		public UI()
		{
			m_view = (TextView)mContext.findViewById(R.id.nh_message);
			m_more = mContext.findViewById(R.id.more);
			m_more.setVisibility(View.GONE);
			m_more.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					showLog();
				}
			});
		}

		// ____________________________________________________________________________________
		public void showInternal()
		{
			update();
			m_view.setVisibility(View.VISIBLE);
		}

		// ____________________________________________________________________________________
		public void hideInternal()
		{
			//	m_view.setVisibility(View.INVISIBLE);
			//	m_more.setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		public void clear()
		{
			m_more.setVisibility(View.GONE);
			m_view.setText("");
		}

		// ____________________________________________________________________________________
		public void update()
		{
			if(mDispCount <= 4 && !mTextUpdaterRunning)
			{
				//m_view.post(mTextUpdater);
				mTextUpdater.run();
			}
			if(mDispCount > 3)
				m_more.setVisibility(View.VISIBLE);
			else
				m_more.setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		public boolean handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, boolean bSoftInput)
		{
			if(m_more.getVisibility() == View.VISIBLE && ch == ' ' && !isLogShowing())
			{
				showLog();
				return true;
			}
			return false;
		}

		// ____________________________________________________________________________________
		private Runnable mTextUpdater = new Runnable()
		{
			public void run()
			{
				mTextUpdaterRunning = false;

				m_view.setText("");
				if(mDispCount > 0)
				{
					int iStart = mCurrentIdx - mDispCount + 1;
					for(int i = 0; i < mDispCount && i < 3; i++)
					{
						LogEntry e = mLog[getIndex(iStart + i)];
						if(i > 0)
							m_view.append("\n");
						if(e.repeat > 1)
							m_view.append(new SpannedString(TextUtils.concat(e.msg, " (" + Integer.toString(e.repeat) + ")")));
						else
							m_view.append(e.msg);
					}
					if(mDispCount > 3)
					{
						mDispCount--;
						m_view.postDelayed(mTextUpdater, 100);
						mTextUpdaterRunning = true;
					}
				}
			}
		};
	}
}
