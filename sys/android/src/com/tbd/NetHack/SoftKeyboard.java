package com.tbd.NetHack;

import android.content.res.Configuration;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.view.ViewGroup;
import android.widget.TableLayout.LayoutParams;

public class SoftKeyboard implements OnKeyboardActionListener
{
	private KeyboardView m_inputView;
	private Keyboard m_symbolsKeyboard;
	private Keyboard m_qwertyKeyboard;
	private NetHackIO m_io;
	private CmdPanel m_cmdPanel;
	private boolean m_bCtrl;

	public SoftKeyboard(NetHackIO io, CmdPanel cmdPanel, KeyboardView keyboardView)
	{
		m_io = io;
		m_cmdPanel = cmdPanel;

		m_qwertyKeyboard = new Keyboard(NetHack.get(), R.xml.qwerty);
		m_symbolsKeyboard = new Keyboard(NetHack.get(), R.xml.symbols);

		m_inputView = keyboardView;
		m_inputView.setOnKeyboardActionListener(this);
		m_inputView.setKeyboard(m_qwertyKeyboard);
	}

	public void onPress(int primaryCode)
	{
		Log.print(primaryCode);
	}

	static final int KEYCODE_CTRL = -7;
	static final int KEYCODE_ESC = -8;
	static final int KEYCODE_TOGGLE = -9;

	public void onRelease(int primaryCode)
	{
		Log.print(primaryCode);
		//getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		//getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
	}

	public void onKey(int primaryCode, int[] keyCodes)
	{
		switch(primaryCode)
		{
		case Keyboard.KEYCODE_SHIFT:
			handleShift();
		break;

		case Keyboard.KEYCODE_CANCEL:			
			handleClose();
		break;

		case KEYCODE_CTRL:
			handleCtrl();
		break;

		case KEYCODE_ESC:
			m_io.SendKeyCmd('\033');
		break;

		case KEYCODE_TOGGLE:
			if(m_inputView.getKeyboard() == m_qwertyKeyboard)
				m_inputView.setKeyboard(m_symbolsKeyboard);
			else
				m_inputView.setKeyboard(m_qwertyKeyboard);
		break;

		case Keyboard.KEYCODE_DELETE:
			m_io.SendKeyCmd('\007');
		break;

		default:
			if(m_bCtrl && primaryCode >= 'a' && primaryCode <= 'z')
			{
				handleCtrl();
				primaryCode = 0x1f & primaryCode;
			}
			else if(m_inputView.isShifted())
			{
				shiftOff();
				primaryCode = Character.toUpperCase(primaryCode);
			}
			m_io.SendKeyCmd((char)primaryCode);
			//getCurrentInputConnection().commitText(String.valueOf((char)primaryCode), 1);
		}
	}

	private void handleCtrl()
	{
		m_bCtrl = !m_bCtrl;
		if(m_inputView.getKeyboard() == m_qwertyKeyboard)
		{
			for(Keyboard.Key k : m_qwertyKeyboard.getKeys())
			{
				int primaryCode = k.codes[0];

				if(primaryCode == KEYCODE_CTRL)
					k.on = m_bCtrl;
				else if(primaryCode >= 'a' && primaryCode <= 'z')
				{
					k.label = Character.toString((char)primaryCode);
					if(m_bCtrl)
						k.label = "^" + k.label;
				}
			}
			m_inputView.invalidateAllKeys();
		}
	}

	private void shiftOff()
	{
		m_inputView.setShifted(false);
		if(m_inputView.getKeyboard() == m_qwertyKeyboard)
		{
			for(Keyboard.Key k : m_qwertyKeyboard.getKeys())
			{
				if(k.codes[0] == Keyboard.KEYCODE_SHIFT)
				{
					k.on = false;
					break;
				}
			}
			m_inputView.invalidateAllKeys();
		}
	}

	private void handleShift()
	{
		if(m_inputView.getKeyboard() == m_qwertyKeyboard)
		{
			m_inputView.setShifted(!m_inputView.isShifted());
		}
	}

	private void handleClose()
	{
		if(m_cmdPanel.IsKeyboardShowing())
			m_cmdPanel.ToggleKeyboard();
	}

	public void swipeLeft()
	{
	}

	public void swipeDown()
	{
		handleClose();
	}

	public void swipeRight()
	{
	}

	public void swipeUp()
	{
	}

	public void onText(CharSequence text)
	{
	}

	public void onConfigurationChanged(Configuration newConfig)
	{
		/*
		final ViewGroup parent = (ViewGroup)m_inputView.getParent();		
		Log.print("WTF?? None of this works");
		m_inputView.post(new Runnable()
		{
			public void run()
			{
				m_inputView.setWillNotCacheDrawing(true);
				m_inputView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
				m_inputView.invalidate();
				m_inputView.requestLayout();
				m_inputView.refreshDrawableState();
				m_inputView.forceLayout();
				parent.invalidate();
				parent.requestLayout();
				parent.refreshDrawableState();
				parent.forceLayout();
			}
		});*/
	}
}
