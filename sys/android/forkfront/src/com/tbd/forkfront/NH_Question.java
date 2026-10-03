package com.tbd.forkfront;

import java.util.Set;

import android.app.Activity;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.tbd.forkfront.Input.Modifier;

public class NH_Question
{
	private NetHackIO mIO;
	private String mQuestion;
	private char[] mChoices;
	private int mDefIdx;
	private int mDefCh;
	private UI mUI;
	private int[] mBtns = new int[]{R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3};
	private NH_State mState;

	// ____________________________________________________________________________________
	public NH_Question(NetHackIO io, NH_State state)
	{
		mIO = io;
		mState = state;
	}

	// ____________________________________________________________________________________
	public void show(Activity context, String question, byte[] choices, int def)
	{
		if(mUI != null)
			mUI.dismiss();
	
		mDefCh = def;
		mQuestion = question;
		mChoices = new char[choices.length];
		mDefIdx = 0;
		for(int i = 0; i < choices.length; i++)
		{
			mChoices[i] = (char)choices[i];
			if(mChoices[i] == def)
				mDefIdx = i;
		}

		mUI = new UI(context);
	}

	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		if(mUI != null)
			mUI = new UI(context);
	}

	// ____________________________________________________________________________________
	public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(mUI == null)
			return KeyEventResult.IGNORED;
		return mUI.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);
	}

	// ____________________________________________________________________________________ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private View mRoot;
		private boolean mIsDisabled;

		// ____________________________________________________________________________________
		public UI(Activity context)
		{
			switch(mChoices.length)
			{
			case 1:
				mRoot = Util.inflate(context, R.layout.dialog_question_1, context.findViewById(R.id.dlg_frame));
			break;
			case 2:
				mRoot = Util.inflate(context, R.layout.dialog_question_2, context.findViewById(R.id.dlg_frame));
			break;
			case 3:
				mRoot = Util.inflate(context, R.layout.dialog_question_3, context.findViewById(R.id.dlg_frame));
			break;
			case 4:
				mRoot = Util.inflate(context, R.layout.dialog_question_4, context.findViewById(R.id.dlg_frame));
			break;
			}

			if(mChoices.length == 1)
			{
				mRoot.findViewById(R.id.btn_0).setOnClickListener(new OnClickListener()
				{
					public void onClick(View v)
					{
						select(mChoices[0]);
					}
				});
			}
			else
			{
				for(int i = 0; i < mChoices.length; i++)
				{
					final int a = i;
					Button btn = (Button)mRoot.findViewById(mBtns[i]);
					btn.setText(Character.toString(mChoices[i]));
					btn.setOnClickListener(new OnClickListener()
					{
						public void onClick(View v)
						{
							Log.print("select: " + Integer.toString(mChoices[a]));
							select(mChoices[a]);
						}
					});
					btn.setOnKeyListener(mKeyListener);
				}
			}

			((TextView)mRoot.findViewById(R.id.title)).setText(mQuestion);

			mRoot.setOnKeyListener(mKeyListener);

			// mRoot.setClickable(true);
			// mRoot.setFocusable(true);
			// mRoot.setFocusableInTouchMode(true);

			final View def = mRoot.findViewById(mBtns[mDefIdx]);
			if(def != null)
			{
				def.requestFocus();
				def.requestFocusFromTouch();

				maybeDisableInput();
			}
			else {
				mRoot.requestFocus();
			}

			mState.hideControls();
		}

		private void maybeDisableInput() {
			if(mChoices.length == 2 && mQuestion.startsWith("Really")) {
				// Disable the non-default choice for a while. Gives the player a chance to react if moving quickly
				final View nonDef = mRoot.findViewById(mBtns[mDefIdx^1]);
				if(nonDef != null) {
					mIsDisabled = true;
					nonDef.setEnabled(false);
					mState.getHandler().postDelayed(new Runnable() {
						@Override
						public void run() {
							mIsDisabled = false;
							nonDef.setEnabled(true);
						}
					}, 500);
				}
			}
		}

		OnKeyListener mKeyListener = new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				Log.print("QUES ONKEY");
				if(event.getAction() != KeyEvent.ACTION_DOWN || mIsDisabled)
					return false;
/*
				int ch = event.getUnicodeChar();
				if(keyCode == KeyEvent.KEYCODE_BACK)
					ch = '\033';

				ch = mapInput(ch);
				if(ch != 0)
				{
					select(ch);
					return true;
				}*/
				return false;
			}
		};

		// ____________________________________________________________________________________
		public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Modifier> modifiers, int repeatCount, boolean bSoftInput)
		{
			if(mRoot == null || mIsDisabled)
				return KeyEventResult.IGNORED;
			
			switch(keyCode)
			{
			case KeyEvent.KEYCODE_BACK:
				select(mapInput('\033'));
			break;

			default:
				if(keyCode == KeyEvent.KEYCODE_BACK)
					ch = '\033';

				ch = (char)mapInput(ch);
				if(ch != 0)
				{
					select(ch);
					return KeyEventResult.HANDLED;
				}
				return KeyEventResult.RETURN_TO_SYSTEM;
			}
			return KeyEventResult.HANDLED;
		}

		// ____________________________________________________________________________________
		public void select(int ch)
		{
			if(mRoot != null)
			{
				mIO.sendKeyCmd((char)ch);
				dismiss();
			}
		}

		// ____________________________________________________________________________________
		public void dismiss()
		{
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
		private int mapInput(int ch)
		{
			switch(ch)
			{
			case ' ':
			case '\n':
			case '\r':
				return getFocusedChoice();
				
			case '\033':
				if(hasChoice('q'))
					return 'q';
				if(hasChoice('n'))
					return 'n';
				return mDefCh;
				
			default:
				if(hasChoice(ch))
					return ch;
				return 0;
			}
		}
		
		private int getFocusedChoice()
		{
			try
			{
				Button focus = (Button)mRoot.findFocus();
				if(focus != null)
					return focus.getText().charAt(0);
			}
			catch(Exception e)
			{
			}
			return 0;
		}

		public boolean hasChoice(int ch)
		{
			for(char c : mChoices)
				if(c == ch)
					return true;
			return false;
		}
	}
}
