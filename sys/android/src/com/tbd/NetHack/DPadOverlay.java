package com.tbd.NetHack;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DPadOverlay
{
	private NetHackIO m_io;
	private View m_dpad;

	// ____________________________________________________________________________________
	public DPadOverlay(NetHackIO io)
	{
		m_io = io;

		m_dpad = NetHack.get().findViewById(R.id.dpad);

		m_dpad.findViewById(R.id.dpad0).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad1).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad2).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad3).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad4).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad5).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad6).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad7).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad8).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad9).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad10).setOnClickListener(OnDPad);
		m_dpad.findViewById(R.id.dpad_esc).setOnClickListener(OnDPad);

		m_dpad.setVisibility(View.GONE);
	}

	// ____________________________________________________________________________________
	public void Show()
	{
		m_dpad.setVisibility(View.VISIBLE);
	}

	// ____________________________________________________________________________________
	public void Hide()
	{
		m_dpad.setVisibility(View.GONE);
	}

	// ____________________________________________________________________________________
	public boolean HandleKeyDown(int keyCode, KeyEvent event)
	{
		if(m_dpad.getVisibility() != View.VISIBLE)
			return false;

		char ch = 0;
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_BACK:
		case KeyEvent.KEYCODE_SPACE:
			ch = '\033';
		break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			ch = 'h';
		break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			ch = 'l';
		break;
		case KeyEvent.KEYCODE_DPAD_UP:
			ch = 'k';
		break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			ch = 'j';
		break;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			ch = '.';
		break;
		default:
			ch = Character.toLowerCase((char)event.getUnicodeChar());
		}

		switch(ch)
		{
		case '\033':
		case '.':
		case 'k':
		case 'y':
		case 'u':
		case 'h':
		case 'l':
		case 'b':
		case 'j':
		case 'n':
		case '<':
		case '>':
			m_io.SendKeyCmd(ch);
		}
		return true;
	}

	// ____________________________________________________________________________________
	private OnClickListener OnDPad = new OnClickListener()
	{
		public void onClick(View v)
		{
			if(m_dpad.getVisibility() == View.VISIBLE)
			{
				if(v.getId() == R.id.dpad_esc)
					m_io.SendKeyCmd('\033');
				else
					m_io.SendKeyCmd(((Button)v).getText().charAt(0));
			}
		}
	};

}
