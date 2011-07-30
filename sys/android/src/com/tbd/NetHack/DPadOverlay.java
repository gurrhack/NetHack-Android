package com.tbd.NetHack;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DPadOverlay
{
	private CmdPanel mCmdPanel;
	private boolean mIsVisible;
	private UI mUI;

	// ____________________________________________________________________________________
	public DPadOverlay(CmdPanel cmdPanel)
	{
		mCmdPanel = cmdPanel;
	}

	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		mUI = new UI(context);
		setVisible(mIsVisible);
	}

	// ____________________________________________________________________________________
	public void setVisible(boolean bVisible)
	{
		mIsVisible = bVisible;
		mUI.setVisible(bVisible);
	}

	// ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private View mDPad;

		public UI(Activity context)
		{
			mDPad = context.findViewById(R.id.dpad);

			mDPad.findViewById(R.id.dpad0).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad1).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad2).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad3).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad4).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad5).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad6).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad7).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad8).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad9).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad10).setOnClickListener(onDPad);
			mDPad.findViewById(R.id.dpad_esc).setOnClickListener(onDPad);
		}

		// ____________________________________________________________________________________
		public void setVisible(boolean bVisible)
		{
			mDPad.setVisibility(bVisible ? View.VISIBLE : View.GONE);
		}

		// ____________________________________________________________________________________
		private OnClickListener onDPad = new OnClickListener()
		{
			public void onClick(View v)
			{
				if(mIsVisible)
				{
					if(v.getId() == R.id.dpad_esc)
						mCmdPanel.sendKeyCmd('\033');
					else
						mCmdPanel.sendKeyCmd(((Button)v).getText().charAt(0));
				}
			}
		};
	}
}
