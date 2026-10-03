package com.tbd.forkfront;

import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.view.*;
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
	@Override
	public String getTitle()
	{
		return "NHW_Text";
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
	public void clear()
	{
		mBuilder = new SpannableStringBuilder();
		mUI.update();
	}

	// ____________________________________________________________________________________
	/*public void printString(Spanned str)
	{
		if(mBuilder.length() > 0)
			mBuilder.append('\n');
		mBuilder.append(str);
		mUI.update();
	}*/

	// ____________________________________________________________________________________
	@Override
	public void printString(int attr, String str, int append, int color)
	{
		if(mBuilder.length() > 0)
			mBuilder.append('\n');
		mBuilder.append(TextAttr.style(str, attr));
		mUI.update();
	}

	// ____________________________________________________________________________________
	@Override
	public void setCursorPos(int x, int y)
	{
	}

	@Override
	public void preferencesUpdated(SharedPreferences prefs) {
	}

	// ____________________________________________________________________________________
	@Override
	public void show(boolean bBlocking)
	{
		mIsBlocking = bBlocking;
		mIsVisible = true;
		mUI.showInternal();
	}

	// ____________________________________________________________________________________
	private void hide()
	{
		mIsVisible = false;
		mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	@Override
	public void destroy()
	{
		close();
	}

	// ____________________________________________________________________________________
	@Override
	public int id()
	{
		return mWid;
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
	@Override
	public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(isVisible())
			return mUI.handleKeyDown(ch, nhKey, keyCode, modifiers, bSoftInput);
		return KeyEventResult.IGNORED;
	}

	// ____________________________________________________________________________________
	public boolean isVisible()
	{
		return mIsVisible;
	}

	// ____________________________________________________________________________________ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private final Activity mContext;
		private TextView mTextView;
		private ScrollView mScroll;
		private boolean mIsScrolling;

		public UI(Activity context)
		{
			mContext = context;
			mScroll = (ScrollView)Util.inflate(context, R.layout.textwindow, R.id.dlg_frame);
			mTextView = (TextView)mScroll.findViewById(R.id.text_view);

			mTextView.setOnTouchListener(mTouchListener);
			mScroll.setOnTouchListener(mTouchListener);
			hideInternal();
		}
		// ____________________________________________________________________________________
		private int getActionIndex(MotionEvent event)
		{
			return (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
		}

		// ____________________________________________________________________________________
		private int getAction(MotionEvent event)
		{
			return event.getAction() & MotionEvent.ACTION_MASK;
		}

		View.OnTouchListener mTouchListener = new View.OnTouchListener() {
			Integer mPointerId;
			float mPointerX, mPointerY;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(getAction(event)) {
					case MotionEvent.ACTION_DOWN:
						mPointerId = event.getPointerId(getActionIndex(event));
						mPointerX = event.getRawX();
						mPointerY = event.getRawY();
					break;

					case MotionEvent.ACTION_MOVE:
						int pointerId = event.getPointerId(getActionIndex(event));
						if(mPointerId == pointerId) {
							float posX = event.getRawX();
							float posY = event.getRawY();

							float dx = posX - mPointerX;
							float dy = posY - mPointerY;

							float th = ViewConfiguration.get(mContext).getScaledTouchSlop();

							if(Math.abs(dx) > th || Math.abs(dy) > th) {
								mIsScrolling = true;
							}
						}
					break;

					case MotionEvent.ACTION_UP:
						mPointerId = null;
						if(!mIsScrolling) {
							close();
						}
						mIsScrolling = false;
					break;
				}
				return false;
			}
		};

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
					@Override
					public void run()
					{
						mScroll.fullScroll(ScrollView.FOCUS_DOWN);
					}
				});
			}
		}

		// ____________________________________________________________________________________
		public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, boolean bSoftInput)
		{
			if(ch == '<')
				keyCode = KeyEvent.KEYCODE_PAGE_UP;
			else if(ch == '>')
				keyCode = KeyEvent.KEYCODE_PAGE_DOWN;

			switch(keyCode)
			{
			case KeyEvent.KEYCODE_ENTER:
			case KeyEvent.KEYCODE_BACK:
			case KeyEvent.KEYCODE_ESCAPE:
			case KeyEvent.KEYCODE_DPAD_CENTER:
				close();
			break;

			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_VOLUME_UP:
				mScroll.scrollBy(0, -mTextView.getLineHeight());
			break;

			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				mScroll.scrollBy(0, mTextView.getLineHeight());
			break;

			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_PAGE_UP:
				mScroll.pageScroll(View.FOCUS_UP);
			break;

			case KeyEvent.KEYCODE_SPACE:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case KeyEvent.KEYCODE_PAGE_DOWN:
				if(isScrolledToBottom())
					close();
				else
					mScroll.pageScroll(View.FOCUS_DOWN);
			break;
			}
			return KeyEventResult.HANDLED;
		}

		public boolean isScrolledToBottom()
		{
			int count = mScroll.getChildCount();
			if(count > 0) {
				View view = mScroll.getChildAt(count - 1);
				if(mScroll.getScrollY() + mScroll.getHeight() >= view.getBottom())
					return true;
			}
			return false;
		}
	}
}
