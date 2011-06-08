package com.tbd.NetHack;

import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.view.KeyEvent;

public class NH_Question implements Runnable
{
	private Activity m_context;
	private NetHackIO m_io;
	private final String m_question;
	private final char[] m_choices;
	private int m_defIdx;

	// ____________________________________________________________________________________
	public NH_Question(Activity context, NetHackIO io, final String question, final byte[] choices, final int def)
	{
		m_context = context;
		m_io = io;
		m_question = question;
		m_choices = new char[choices.length];
		for(int i = 0; i < choices.length; i++)
			m_choices[i] = (char)choices[i];
		m_defIdx = AlertDialog.BUTTON_POSITIVE;
		switch(m_choices.length)
		{
		case 2:
			if(m_choices[1] == def)
				m_defIdx = AlertDialog.BUTTON_NEGATIVE;
		break;
		case 3:
			if(m_choices[1] == def)
				m_defIdx = AlertDialog.BUTTON_NEUTRAL;
			else if(m_choices[2] == def)
				m_defIdx = AlertDialog.BUTTON_NEGATIVE;
		break;
		}

	}

	// ____________________________________________________________________________________
	public void run()
	{
		Log.print("Build alert dialog");
		AlertDialog.Builder builder = new AlertDialog.Builder(m_context);
		builder.setMessage(m_question);
		builder.setCancelable(false);

		builder.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
			{
				if(event.getAction() != KeyEvent.ACTION_DOWN)
					return false;

				int ch = event.getUnicodeChar();
				if(keyCode == KeyEvent.KEYCODE_BACK)
					ch = '\033';

				if(ValidInput(ch))
				{
					m_io.SendKeyCmd((char)ch);
					dialog.dismiss();
					return true;
				}
				return false;
			}
		});

		switch(m_choices.length)
		{
		case 1:
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					Log.print("dialog dismissed");
					m_io.SendKeyCmd(' ');
				}
			});
		break;

		case 2:
			builder.setPositiveButton(CmdToString(m_choices[0]), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					Log.print("'yes' pressed");
					m_io.SendKeyCmd(m_choices[0]);
					dialog.dismiss();
				}
			});

			builder.setNegativeButton(CmdToString(m_choices[1]), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					Log.print("'no' pressed");
					m_io.SendKeyCmd(m_choices[1]);
					dialog.dismiss();
				}
			});
		break;

		case 3:
			builder.setPositiveButton(CmdToString(m_choices[0]), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					Log.print("'yes' pressed");
					m_io.SendKeyCmd(m_choices[0]);
					dialog.dismiss();
				}
			});

			builder.setNeutralButton(CmdToString(m_choices[1]), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					Log.print("'cancel' pressed");
					m_io.SendKeyCmd(m_choices[1]);
					dialog.dismiss();
				}
			});

			builder.setNegativeButton(CmdToString(m_choices[2]), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					Log.print("'no' pressed");
					m_io.SendKeyCmd(m_choices[2]);
					dialog.dismiss();
				}
			});
		break;
		}
		final AlertDialog alert = builder.create();
		alert.show();
		alert.getButton(m_defIdx).post(new Runnable()
		{
			public void run()
			{
				// don't know if both are needed...
				alert.getButton(m_defIdx).requestFocus();
				alert.getButton(m_defIdx).requestFocusFromTouch();
			}
		});
	}

	// ____________________________________________________________________________________
	public String CmdToString(char cmd)
	{
		if((cmd & 0x80) > 0)
			return "^" + Character.toString((char)(cmd & 0x7f));
		return Character.toString(cmd);
	}

	// ____________________________________________________________________________________
	public boolean ValidInput(int ch)
	{
		if(ch == '\033')
			return true;
		if(ch < 0 || ch > 255)
			return false;
		for(char c : m_choices)
			if(c == ch)
				return true;
		return false;
	}
}
