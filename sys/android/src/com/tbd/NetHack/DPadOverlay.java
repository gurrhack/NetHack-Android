package com.tbd.NetHack;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

public class DPadOverlay
{
	private NH_State mNHState;
	private boolean mIsVisible;
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
	public void setVisible(boolean bVisible)
	{
		mIsVisible = bVisible;
		mHideForced = false;
		mUI.updateVisibleState();
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
			
			setTransparent();
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
			if(mHideForced)
				mDPad.setVisibility(View.GONE);
			else if(mAlwaysShow || mIsVisible)
			{
				if(mIsVisible)
					setDirMode();
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
		private void setDirMode()
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
				
				final float density = mContext.getResources().getDisplayMetrics().density;
				float margin = density * 18;
				float x = event.getX();
				float y = event.getY();
				boolean outside = x < -margin || y < -margin || x > v.getWidth() + margin || y > v.getHeight() + margin;
				
				Log.print((int)margin);
				
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
					mNHState.sendKeyCmd(k);
				v.getBackground().setAlpha(mOpacity);
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
