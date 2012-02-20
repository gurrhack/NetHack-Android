package com.tbd.NetHack;

import java.util.EnumSet;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import com.tbd.NetHack.Input.Modifier;

public class SoftKeyboard implements OnKeyboardActionListener
{
	private static final int KEYCODE_CTRL = -7;
	private static final int KEYCODE_ESC = -8;
	private static final int KEYCODE_SYMBOLS = -9;
	private static final int KEYCODE_META = -10;
	private static final int KEYCODE_ABC = -11;

	private NetHack mContext;
	private ViewGroup mKeyboardFrame;
	private KeyboardView mKeyboardView;
	private Keyboard mSymbolsKeyboard;
	private Keyboard mQwertyKeyboard;
	private Keyboard mMetaKeyboard;
	private Keyboard mCtrlKeyboard;
	private NH_State mState;
	private int mCurrent;
	private boolean mIsShifted;
	
	// ____________________________________________________________________________________
	public SoftKeyboard(NetHack context, NH_State state)
	{
		mContext = context;
		mState = state;
		mCurrent = 0;

		mKeyboardFrame = (ViewGroup)mContext.findViewById(R.id.kbd_frame);

		mKeyboardView = (KeyboardView)Util.inflate(mContext, R.layout.input);
		mKeyboardView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
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
			
			if(mCurrent == 1)
				setKeyboard(mSymbolsKeyboard);
			else if(mCurrent == 2)
				setKeyboard(mCtrlKeyboard);
			else if(mCurrent == 3)
				setKeyboard(mMetaKeyboard);
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
			mCurrent = 0;
			mKeyboardView.setKeyboard(mQwertyKeyboard);
			mIsShifted = !mIsShifted;
			mKeyboardView.setShifted(mIsShifted);
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
	private void setKeyboard(Keyboard keyboard)
	{
		setShift(keyboard, mIsShifted);
		mKeyboardView.setKeyboard(keyboard);
		if(keyboard == mQwertyKeyboard)
			mCurrent = 0;
		else if(keyboard == mSymbolsKeyboard)
			mCurrent = 1;
		else if(keyboard == mCtrlKeyboard)
			mCurrent = 2;
		else if(keyboard == mMetaKeyboard)
			mCurrent = 3;
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
