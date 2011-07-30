package com.tbd.NetHack;

import java.util.LinkedList;
import java.util.Set;

import android.app.Activity;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class NHW_Message implements NH_Window
{
	private NetHackIO mIO;
	private Activity mContext;
	private LinkedList<Spanned> mLog;
	private int mPrevCount;
	private int mDispCount;
	private final int MaxLog = 100;
	private UI mUI;
	private NHW_Text mLogView;
	private boolean mIsVisible;
	private int mWid;

	// ____________________________________________________________________________________
	public NHW_Message(Activity context, NetHackIO io)
	{
		mIO = io;
		mLog = new LinkedList<Spanned>();
		for(int i = 0; i < MaxLog; i++)
			mLog.add(null);
		mPrevCount = 0;
		mDispCount = 0;
		setContext(context);
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
		mDispCount = 0;
		mUI.clear();
	}

	// ____________________________________________________________________________________
	public void printString(TextAttr attr, String str, int append)
	{
		if(append < 0)
		{
			append++;
			Spanned l = mLog.getLast();
			if(append < -l.length())
				append = -l.length();
			l = (Spanned)l.subSequence(0, l.length() + append);
			if(str.length() > 0)
				l = new SpannedString(TextUtils.concat(l, str));
			mLog.removeLast();
			mLog.add(l);
		}
		else if(append > 0)
		{
			if(str.length() > 0)
			{
				Spanned l = new SpannedString(TextUtils.concat(mLog.getLast(), attr.style(str)));
				mLog.removeLast();
				mLog.add(l);
			}
		}
		else
		{
			mLog.add(attr.style(str));
			mLog.remove();
			if(mDispCount < mLog.size() - 1)
				mDispCount++;
			mPrevCount = 0;
		}
		mUI.update();
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
		if(mLog.getLast() == null)
			return;

		if(mDispCount > 0)
		{
			clear();
			mPrevCount = 0;
		}
		
		if(++mPrevCount > mLog.size() || mLog.get(mLog.size() - mPrevCount) == null)
			mPrevCount = 1;

		mUI.update();
	}

	// ____________________________________________________________________________________
	public void showLog()
	{
		if(mLogView == null)
			mLogView = new NHW_Text(0, mContext, mIO);

		mLogView.clear();
		for(Spanned s : mLog)
		{
			if(s != null)
				mLogView.printString(s);
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
	
	// ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾ //
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
			if(mPrevCount > 0)
			{
				m_view.setText(mLog.get(mLog.size() - mPrevCount));
			}
			else
			{
				if(mDispCount <= 4 && !mTextUpdaterRunning)
				{
					mTextUpdaterRunning = true;
					m_view.post(mTextUpdater);
				}
				if(mDispCount > 3)
					m_more.setVisibility(View.VISIBLE);
				else
					m_more.setVisibility(View.GONE);
			}
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
				if(mDispCount > 0)
				{
					m_view.setText(mLog.get(mLog.size() - mDispCount));
					int e = mDispCount > 3 ? mDispCount - 3 : 0;
					for(int i = mDispCount - 1; i > e; i--)
					{
						m_view.append("\n");
						m_view.append(mLog.get(mLog.size() - i));
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
