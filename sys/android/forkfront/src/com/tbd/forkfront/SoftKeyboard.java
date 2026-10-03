package com.tbd.forkfront;

import java.util.EnumSet;

import android.app.Activity;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.tbd.forkfront.Input.Modifier;

public class SoftKeyboard implements OnKeyboardActionListener
{
	private static final int KEYCODE_CTRL = -7;
	private static final int KEYCODE_ESC = -8;
	private static final int KEYCODE_SYMBOLS = -9;
	private static final int KEYCODE_META = -10;
	private static final int KEYCODE_ABC = -11;

	public enum KEYBOARD
	{
		QWERTY,
		SYMBOLS,
		CTRL,
		META,
	}

	private Activity mContext;
	private ViewGroup mKeyboardFrame;
	private KeyboardView mKeyboardView;
	private Keyboard mSymbolsKeyboard;
	private Keyboard mQwertyKeyboard;
	private Keyboard mMetaKeyboard;
	private Keyboard mCtrlKeyboard;
	private NH_State mState;
	private KEYBOARD mCurrent;
	private boolean mIsShifted;

	// ____________________________________________________________________________________
	public SoftKeyboard(Activity context, NH_State state)
	{
		mContext = context;
		mState = state;
		mCurrent = KEYBOARD.QWERTY;

		mKeyboardFrame = (ViewGroup)mContext.findViewById(R.id.kbd_frame);

		mKeyboardView = (KeyboardView)Util.inflate(mContext, R.layout.input);
		mKeyboardView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
		mKeyboardFrame.addView(mKeyboardView);
		mKeyboardView.setOnKeyboardActionListener(this);
	}

	// ____________________________________________________________________________________
	public void show()
	{
		if(mQwertyKeyboard == null)
		{
			mQwertyKeyboard = new Keyboard(mContext, R.xml.qwerty);
			mMetaKeyboard = new Keyboard(mContext, R.xml.meta);
			mCtrlKeyboard = new Keyboard(mContext, R.xml.ctrl);
			mSymbolsKeyboard = new Keyboard(mContext, R.xml.symbols);

			mKeyboardView.setKeyboard(mQwertyKeyboard);
			mKeyboardView.setShifted(mIsShifted);

			switch(mCurrent)
			{
			case SYMBOLS:
				setKeyboard(KEYBOARD.SYMBOLS);
			break;
			case CTRL:
				setKeyboard(KEYBOARD.CTRL);
			break;
			case META:
				setKeyboard(KEYBOARD.META);
			break;
			}
			mKeyboardFrame.setVisibility(View.VISIBLE);
		}
	}

	// ____________________________________________________________________________________
	public void hide()
	{
		if(mQwertyKeyboard != null)
		{
			Util.hideKeyboard(mContext, mKeyboardView);
			mKeyboardFrame.setVisibility(View.GONE);
			mQwertyKeyboard = null;
			mMetaKeyboard = null;
			mCtrlKeyboard = null;
			mSymbolsKeyboard = null;
		}
	}

	// ____________________________________________________________________________________
	public void setMetaKeyboard()
	{
		setKeyboard(KEYBOARD.META);
	}

	// ____________________________________________________________________________________
	public void setCtrlKeyboard()
	{
		setKeyboard(KEYBOARD.CTRL);
	}

	// ____________________________________________________________________________________
	@Override
	public void onPress(int primaryCode)
	{
	}

	// ____________________________________________________________________________________
	@Override
	public void onRelease(int primaryCode)
	{
	}

	// ____________________________________________________________________________________
	@Override
	public void onKey(int primaryCode, int[] keyCodes)
	{
		switch(primaryCode)
		{
		case KEYCODE_META:
			setKeyboard(KEYBOARD.META);
		break;

		case KEYCODE_CTRL:
			setKeyboard(KEYBOARD.CTRL);
		break;

		case KEYCODE_ABC:
			setKeyboard(KEYBOARD.QWERTY);
		break;

		case KEYCODE_SYMBOLS:
			setKeyboard(KEYBOARD.SYMBOLS);
		break;

		case Keyboard.KEYCODE_SHIFT:
			if(mQwertyKeyboard != null)
			{
				mCurrent = KEYBOARD.QWERTY;
				mKeyboardView.setKeyboard(mQwertyKeyboard);
				mIsShifted = !mIsShifted;
				mKeyboardView.setShifted(mIsShifted);
			}
		break;

		case Keyboard.KEYCODE_CANCEL:
			handleClose();
		break;

		case KEYCODE_ESC:
			mState.handleKeyDown('\033', '\033', KeyEvent.KEYCODE_ESCAPE, Input.modifiers(), 0, true);
		break;

		case Keyboard.KEYCODE_DELETE:
			mState.handleKeyDown((char)0x7f, 0x7f, KeyEvent.KEYCODE_DEL, Input.modifiers(), 0, true);
		break;

		default:
			EnumSet<Modifier> mod = Input.modifiers();
			if(mKeyboardView.getKeyboard() == mQwertyKeyboard && mIsShifted)
			{
				// shiftOff(); only on shift release if key is pressed while shift is down
				mod.add(Input.Modifier.Shift);
				primaryCode = Character.toUpperCase(primaryCode);
			}
			mState.handleKeyDown((char)primaryCode, primaryCode, Input.toKeyCode((char)primaryCode), mod, 0, true);
		}
	}

	// ____________________________________________________________________________________
	public void setKeyboard(KEYBOARD name)
	{
		mCurrent = name;
		Keyboard keyboard = null;
		switch(mCurrent)
		{
		case QWERTY:
			keyboard = mQwertyKeyboard;
		break;
		case SYMBOLS:
			keyboard = mSymbolsKeyboard;
		break;
		case CTRL:
			keyboard = mCtrlKeyboard;
		break;
		case META:
			keyboard = mMetaKeyboard;
		break;
		}
		if(keyboard != null)
		{
			setShift(keyboard, mIsShifted);
			mKeyboardView.setKeyboard(keyboard);
		}
	}

	// ____________________________________________________________________________________
	public KEYBOARD getKeyboard()
	{
		return mCurrent;
	}

	// ____________________________________________________________________________________
	private void setShift(Keyboard keyboard, boolean on)
	{
		for(Keyboard.Key k : keyboard.getKeys())
		{
			if(k.codes[0] == Keyboard.KEYCODE_SHIFT)
			{
				k.on = on;
				break;
			}
		}
	//	mKeyboardView.invalidateAllKeys();
	}

	// ____________________________________________________________________________________
	private void handleClose()
	{
		mState.hideKeyboard();
	}

	// ____________________________________________________________________________________
	@Override
	public void swipeLeft()
	{
	}

	// ____________________________________________________________________________________
	@Override
	public void swipeDown()
	{
		handleClose();
	}

	// ____________________________________________________________________________________
	@Override
	public void swipeRight()
	{
	}

	// ____________________________________________________________________________________
	@Override
	public void swipeUp()
	{
	}

	// ____________________________________________________________________________________
	@Override
	public void onText(CharSequence text)
	{
	}
}
