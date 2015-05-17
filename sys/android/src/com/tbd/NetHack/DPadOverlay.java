package com.tbd.NetHack;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.FrameLayout;

public class DPadOverlay
{
	private NH_State mNHState;
	private boolean mShowDirectional;
	private boolean mHideForced;
	private boolean mAlwaysShow;
	private boolean mPortAlwaysShow;
	private boolean mLandAlwaysShow;
	private UI mUI;
	private int mPortLoc;
	private int mLandLoc;
	public int mOrientation;
	private int mOpacity;

	// ____________________________________________________________________________________
	public DPadOverlay(NH_State nhState)
	{
		mNHState = nhState;
	}

	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		mOrientation = context.getResources().getConfiguration().orientation;
		mUI = new UI(context);		
		mUI.updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void showDirectional(boolean showDirectional)
	{
		mShowDirectional = showDirectional;
		mHideForced = false;
		mUI.updateVisibleState();
	}

	// ____________________________________________________________________________________
	public boolean isVisible()
	{
		return (mAlwaysShow || mShowDirectional) && !mHideForced;
	}

	// ____________________________________________________________________________________
	private boolean isNormalMode()
	{
		return mAlwaysShow && !mShowDirectional;
	}

	// ____________________________________________________________________________________
	public void forceHide()
	{
		mHideForced = true;
		mUI.updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void preferencesUpdated(SharedPreferences prefs)
	{
		mPortAlwaysShow = prefs.getBoolean("ovlPortAlways", false);
		mLandAlwaysShow = prefs.getBoolean("ovlLandAlways", false);
		mPortLoc = getGravity(prefs.getString("ovlPortLoc", "1"));
		mLandLoc = getGravity(prefs.getString("ovlLandLoc", "1"));
		mOpacity = prefs.getInt("ovlOpacity", 255);
		mUI.setTransparent();
		setOrientation(mOrientation);
	}
	
	// ____________________________________________________________________________________
	private int getGravity(String val)
	{
		int loc = Integer.parseInt(val);
		if(loc == 0)
			return Gravity.LEFT;
        if(loc == 1)
        	return Gravity.CENTER;
        if(loc == 2)
        	return Gravity.RIGHT;
        if(loc == 3)
			return Gravity.BOTTOM | Gravity.LEFT;
        if(loc == 4)
        	return Gravity.BOTTOM;
        if(loc == 5)
			return Gravity.BOTTOM | Gravity.RIGHT;
		return Gravity.CENTER;
	}

	// ____________________________________________________________________________________
	public void setOrientation(int orientation)
	{
		mUI.setOrientation(orientation);			
	}

	// ____________________________________________________________________________________
	public void updateNumPadState()
	{
		mUI.updateNumPadState();
	}

	// ____________________________________________________________________________________ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private View mDPad;
		private View mExtra;
		private Button[] mButtons;
		private Activity mContext;
		private ColorStateList mDefaultTextColor;
		private boolean mLongClick;

		public UI(Activity context)
		{
			mContext = context;
			mDPad = context.findViewById(R.id.dpad);

			mButtons = new Button[12];
			mButtons[0] = (Button)mDPad.findViewById(R.id.dpad0);
			mButtons[1] = (Button)mDPad.findViewById(R.id.dpad1);
			mButtons[2] = (Button)mDPad.findViewById(R.id.dpad2);
			mButtons[3] = (Button)mDPad.findViewById(R.id.dpad3);
			mButtons[4] = (Button)mDPad.findViewById(R.id.dpad4);
			mButtons[5] = (Button)mDPad.findViewById(R.id.dpad5);
			mButtons[6] = (Button)mDPad.findViewById(R.id.dpad6);
			mButtons[7] = (Button)mDPad.findViewById(R.id.dpad7);
			mButtons[8] = (Button)mDPad.findViewById(R.id.dpad8);
			mButtons[9] = (Button)mDPad.findViewById(R.id.dpad9);
			mButtons[10] = (Button)mDPad.findViewById(R.id.dpad10);
			mButtons[11] = (Button)mDPad.findViewById(R.id.dpad_esc);
			
			mDefaultTextColor = mButtons[0].getTextColors();
			
			mExtra = (View)mButtons[11].getParent();
			
			for(Button b : mButtons)
			{
				b.setOnTouchListener(onDPadTouch);
				b.setOnClickListener(onDPad);
			}

			// g<dir> on long press
			mButtons[0].setOnLongClickListener(onDPadLong);
			mButtons[1].setOnLongClickListener(onDPadLong);
			mButtons[2].setOnLongClickListener(onDPadLong);
			mButtons[3].setOnLongClickListener(onDPadLong);
			mButtons[5].setOnLongClickListener(onDPadLong);
			mButtons[6].setOnLongClickListener(onDPadLong);
			mButtons[7].setOnLongClickListener(onDPadLong);
			mButtons[8].setOnLongClickListener(onDPadLong);

			setTransparent();
			updateNumPadState();
		}

		// ____________________________________________________________________________________
		public void updateNumPadState()
		{
			if(mNHState.isNumPadOn())
			{
				mButtons[0].setText("7");
				mButtons[1].setText("8");
				mButtons[2].setText("9");
				mButtons[3].setText("4");
				mButtons[5].setText("6");
				mButtons[6].setText("1");
				mButtons[7].setText("2");
				mButtons[8].setText("3");
			}
			else
			{
				mButtons[0].setText("y");
				mButtons[1].setText("k");
				mButtons[2].setText("u");
				mButtons[3].setText("h");
				mButtons[5].setText("l");
				mButtons[6].setText("b");
				mButtons[7].setText("j");
				mButtons[8].setText("n");
			}
		}

		// ____________________________________________________________________________________
		public void setOrientation(int orientation)
		{
			mOrientation = orientation;
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)mDPad.getLayoutParams();
			if(orientation == Configuration.ORIENTATION_PORTRAIT)
			{
				params.gravity = mPortLoc;
				mAlwaysShow = mPortAlwaysShow;
			}
			else
			{
				params.gravity = mLandLoc;
				mAlwaysShow = mLandAlwaysShow;
			}
			mDPad.setVisibility(View.GONE);
			updateVisibleState();
		}

		// ____________________________________________________________________________________
		private void updateVisibleState()
		{
			if(isVisible())
			{
				if(mShowDirectional)
					setDirectionalMode();
				else
					setNormalMode();
				mDPad.setVisibility(View.VISIBLE);
			}
			else
				mDPad.setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		private void setNormalMode()
		{
			mExtra.setVisibility(View.GONE);
			mButtons[4].setText(" ");
		}

		// ____________________________________________________________________________________
		private void setDirectionalMode()
		{
			mExtra.setVisibility(View.VISIBLE);
			mButtons[4].setText(".");
		}

		private final int OPAQUE = 0xff;
		
		// ____________________________________________________________________________________
		private OnTouchListener onDPadTouch = new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				int action = event.getAction() & MotionEvent.ACTION_MASK;

				if(action == MotionEvent.ACTION_DOWN)
					mLongClick = false;

				final float density = mContext.getResources().getDisplayMetrics().density;
				float margin = density * 18;
				float x = event.getX();
				float y = event.getY();
				boolean outside = x < -margin || y < -margin || x > v.getWidth() + margin || y > v.getHeight() + margin;
				
				if(action == MotionEvent.ACTION_DOWN)
					v.getBackground().setAlpha(OPAQUE);
				else if(action == MotionEvent.ACTION_UP || outside) 
					v.getBackground().setAlpha(mOpacity);
				v.invalidate();

				return false;
			}
		};

		// ____________________________________________________________________________________
		private OnClickListener onDPad = new OnClickListener()
		{
			public void onClick(View v)
			{
				int k = ((Button)v).getText().charAt(0);
				if(k == ' ')
					mNHState.clickCursorPos();
				else if(v.getId() == R.id.dpad_esc)
					mNHState.sendKeyCmd('\033');
				else
				{
					if(mLongClick)
						mNHState.sendKeyCmd('g');
					mNHState.sendKeyCmd(k);
				}
				mLongClick = false;
				v.getBackground().setAlpha(mOpacity);
			}
		};

		// ____________________________________________________________________________________
		private int getRunCmd(int dir)
		{
			switch(dir) {
				case '4': case 'h': return 'H';
				case '6': case 'l': return 'L';
				case '8': case 'k': return 'K';
				case '2': case 'j': return 'J';
				case '7': case 'y': return 'Y';
				case '9': case 'u': return 'U';
				case '1': case 'b': return 'B';
				case '3': case 'n': return 'N';
			}
			return '\033';
		}

		// ____________________________________________________________________________________
		private View.OnLongClickListener onDPadLong = new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				if(isNormalMode())
				{
					if(mNHState.isMouseLocked())
					{
						// Only cursor will be moved. Execute immediately
						int k = ((Button)v).getText().charAt(0);
						k = getRunCmd(k);
						mNHState.sendKeyCmd(k);
						return true;
					}

					// Don't execute run command until button is released. Gives the player a chance to abort
					mLongClick = true;
					v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
				}
				return false;
			}
		};

		// ____________________________________________________________________________________
		private void setTransparent()
		{
			for(Button b : mButtons)
			{
				b.getBackground().setAlpha(mOpacity);
				if(mOpacity > 127)
					b.setTextColor(mDefaultTextColor);
				else
					b.setTextColor(0xffffffff);
			}
		}
	}
}
