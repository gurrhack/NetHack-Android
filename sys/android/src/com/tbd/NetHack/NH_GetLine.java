package com.tbd.NetHack;

import java.util.Set;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;


public class NH_GetLine
{
	private UI mUI;
	private NetHackIO mIO;
	private String mTitle;
	private String mLastLine = "";
	private int mMaxChars;
	private NH_State mState;
	public boolean mSaveLastLine;

	// ____________________________________________________________________________________
	public NH_GetLine(NetHackIO io, NH_State state)
	{
		mIO = io;
		mState = state;
	}

	// ____________________________________________________________________________________
	public void show(Activity context, final String title, final int nMaxChars)
	{
		mTitle = title;
		mMaxChars = nMaxChars;
		mUI = new UI(context);
	}

	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		if(mUI != null)
			mUI = new UI(context);
	}

	// ____________________________________________________________________________________
	public int handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(mUI == null)
			return 0;
		return mUI.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);
	}
	
	// ____________________________________________________________________________________ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private Context mContext;
		private EditText mInput;
		//private NH_Dialog mDialog;
		private View mRoot;

		// ____________________________________________________________________________________
		public UI(Activity context)
		{
			mContext = context;

			mRoot = (View)Util.inflate(context, R.layout.dialog_getline, R.id.dlg_frame);
			mInput = (EditText)mRoot.findViewById(R.id.input);
			mInput.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(mMaxChars) });
			mInput.setOnKeyListener(new OnKeyListener()
			{
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					if(event.getAction() != KeyEvent.ACTION_DOWN)
						return false;

					if(keyCode == KeyEvent.KEYCODE_ENTER)
						ok();
					else if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == 111/*KeyEvent.KEYCODE_ESCAPE*/)
						cancel();
					else if(keyCode == KeyEvent.KEYCODE_SEARCH) // This is doing weird stuff, might as well block it 
						return true;
					return false;
				}
			});

			((TextView)mRoot.findViewById(R.id.title)).setText(mTitle);
			
			mRoot.findViewById(R.id.btn_0).setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					if(v != null)
					{
						ok();
					}
				}
			});
			mRoot.findViewById(R.id.btn_1).setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					cancel();
				}
			});

			mState.hideControls();
			mInput.requestFocus();
			
			// special case
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String lastUsername = prefs.getString("lastUsername", "");
			if(mTitle.equals("Who are you?") && lastUsername.length() > 0)
			{
				mSaveLastLine = false;
				mInput.setText(lastUsername);
				mInput.selectAll();
			}
			else
			{
				mSaveLastLine = true;
				mInput.setText(mLastLine);
				mInput.selectAll();
				Util.showKeyboard(context, mInput);
			}
		}

		// ____________________________________________________________________________________
		public int handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
		{
			if(mRoot == null)
				return 0;

			switch(keyCode)
			{
			case KeyEvent.KEYCODE_BACK:
				cancel();
			break;

			case KeyEvent.KEYCODE_ENTER:
				ok();
			break;
			
			default:
				if(ch == '\033')
					cancel();
				else
					return 2;
			}
			return 1;
		}
		
		// ____________________________________________________________________________________
		public void dismiss()
		{
			Util.hideKeyboard(mContext, mInput);
			if(mRoot != null)
			{
				mRoot.setVisibility(View.GONE);
				((ViewGroup)mRoot.getParent()).removeView(mRoot);
				mRoot = null;
				mState.showControls();
			}
			mUI = null;
		}

		// ____________________________________________________________________________________
		private void ok()
		{
			if(mRoot != null)
			{
				String text = mInput.getText().toString();
				mIO.sendLineCmd(text);
				if(mSaveLastLine)
					mLastLine = text;
				dismiss();
			}
		}

		// ____________________________________________________________________________________
		private void cancel()
		{
			if(mRoot != null)
			{
				mIO.sendLineCmd("\033");
				dismiss();
			}
		}
	}
}
