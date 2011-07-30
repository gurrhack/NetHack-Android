package com.tbd.NetHack;

import java.util.EnumSet;

import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.SystemClock;
import android.view.KeyEvent;

import com.tbd.NetHack.Input.Modifier;

public class SoftKeyboard implements OnKeyboardActionListener
{
	private static final int KEYCODE_CTRL = -7;
	private static final int KEYCODE_ESC = -8;
	private static final int KEYCODE_SYMBOLS = -9;
	private static final int KEYCODE_META = -10;
	private static final int KEYCODE_ABC = -11;

	private NetHack mContext;
	private KeyboardView mInputView;
	private Keyboard mSymbolsKeyboard;
	private Keyboard mQwertyKeyboard;
	private Keyboard mMetaKeyboard;
	private Keyboard mCtrlKeyboard;
	private CmdPanel mCmdPanel;
	private NH_State mState;
	private boolean mIsShifted;
	
	// ____________________________________________________________________________________
	public SoftKeyboard(NetHack context, NH_State state, CmdPanel cmdPanel, KeyboardView keyboardView)
	{
		mContext = context;
		
		mState = state;
		mCmdPanel = cmdPanel;

		mQwertyKeyboard = new Keyboard(context, R.xml.qwerty);
		mMetaKeyboard = new Keyboard(context, R.xml.meta);
		mCtrlKeyboard = new Keyboard(context, R.xml.ctrl);
		mSymbolsKeyboard = new Keyboard(context, R.xml.symbols);

		mInputView = keyboardView;
		mInputView.setOnKeyboardActionListener(this);
		mInputView.setKeyboard(mQwertyKeyboard);
	}

	// ____________________________________________________________________________________
	public void setMetaKeyboard()
	{
		setKeyboard(mMetaKeyboard);
	}
	
	// ____________________________________________________________________________________
	public void setQwertyKeyboard()
	{
		setKeyboard(mQwertyKeyboard);
	}

	// ____________________________________________________________________________________
	public void setCtrlKeyboard()
	{
		setKeyboard(mCtrlKeyboard);
	}
	
	// ____________________________________________________________________________________
	public void onPress(int primaryCode)
	{
	}

	// ____________________________________________________________________________________
	public void onRelease(int primaryCode)
	{
	}

	// ____________________________________________________________________________________
	public void onKey(int primaryCode, int[] keyCodes)
	{
		switch(primaryCode)
		{
		case KEYCODE_META:
			setKeyboard(mMetaKeyboard);
		break;

		case KEYCODE_CTRL:
			setKeyboard(mCtrlKeyboard);
		break;
		
		case KEYCODE_ABC:
			setKeyboard(mQwertyKeyboard);
		break;
		
		case KEYCODE_SYMBOLS:
			setKeyboard(mSymbolsKeyboard);
		break;

		case Keyboard.KEYCODE_SHIFT:
			mInputView.setKeyboard(mQwertyKeyboard);
			mIsShifted = !mIsShifted;
			mInputView.setShifted(mIsShifted);
		break;

		case Keyboard.KEYCODE_CANCEL:			
			handleClose();
		break;

		case KEYCODE_ESC:
			mState.handleKeyDown('\033', '\033', 111 /*KeyEvent.KEYCODE_ESC*/, Input.modifiers(), 0, true);
		break;

		case Keyboard.KEYCODE_DELETE:
			mState.handleKeyDown((char)0x7f, 0x7f, KeyEvent.KEYCODE_DEL, Input.modifiers(), 0, true);
		break;

		default:
			EnumSet<Modifier> mod = Input.modifiers();
			if(mInputView.getKeyboard() == mQwertyKeyboard && mIsShifted)
			{
				// shiftOff(); only on shift release if key is pressed while shift is down
				mod.add(Input.Modifier.Shift);
				primaryCode = Character.toUpperCase(primaryCode);
			}
			mState.handleKeyDown((char)primaryCode, primaryCode, Input.toKeyCode((char)primaryCode), mod, 0, true);
		}
	}

	// ____________________________________________________________________________________
	private void setKeyboard(Keyboard keyboard)
	{
		setShift(keyboard, mIsShifted);
		mInputView.setKeyboard(keyboard);
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
	//	mInputView.invalidateAllKeys();
	}

	// ____________________________________________________________________________________
	private void handleClose()
	{
		mCmdPanel.hideKeyboard();
	}

	// ____________________________________________________________________________________
	public void swipeLeft()
	{
	}

	// ____________________________________________________________________________________
	public void swipeDown()
	{
		handleClose();
	}

	// ____________________________________________________________________________________
	public void swipeRight()
	{
	}

	// ____________________________________________________________________________________
	public void swipeUp()
	{
	}

	// ____________________________________________________________________________________
	public void onText(CharSequence text)
	{
	}
}
