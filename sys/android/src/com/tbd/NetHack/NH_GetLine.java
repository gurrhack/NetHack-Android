package com.tbd.NetHack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;

public class NH_GetLine implements Runnable
{
	private NetHackIO m_io;
	private final String m_title;
	private Dialog m_dialog;
	private final int m_nMaxChars;
	private EditText m_input;

	// ____________________________________________________________________________________
	public NH_GetLine(NetHackIO io, final String title, final int nMaxChars)
	{
		m_io = io;
		m_title = title;
		m_nMaxChars = nMaxChars;
	}

	// ____________________________________________________________________________________
	public void run()
	{
		ViewGroup v = (ViewGroup)Util.Inflate(R.layout.dialog_getline);
		m_input = (EditText)v.findViewById(R.id.input);
//		v.removeView(m_input);

		m_input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(m_nMaxChars) });
		m_input.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if(event.getAction() != KeyEvent.ACTION_DOWN)
					return false;

				if(keyCode == KeyEvent.KEYCODE_ENTER)
					Ok();
				else if(keyCode == KeyEvent.KEYCODE_BACK)
					Cancel();
				return false;
			}
		});

		AlertDialog.Builder builder = new AlertDialog.Builder(NetHack.get());
		builder.setTitle(m_title);
		builder.setView(v);
		builder.setCancelable(true);
		builder.setOnCancelListener(new OnCancelListener()
		{
			public void onCancel(DialogInterface dialog)
			{
				Cancel();
			}
		});
		builder.setPositiveButton("Ok", new OnClickListener()
		{
			public void onClick(DialogInterface arg0, int arg1)
			{
				Ok();
			}
		});
		builder.setNegativeButton("Cancel", new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				Cancel();
			}
		});
	
		m_dialog = builder.create();
		m_dialog.show();

		Util.ShowKeyboard(m_input);
	}

	// ____________________________________________________________________________________
	private void Ok()
	{
		if(m_dialog != null)
		{
			String text = m_input.getText().toString().trim();
			m_io.SendLineCmd(text);
			Util.HideKeyboard(m_input);
			m_dialog.dismiss();
			m_dialog = null;
		}
	}

	// ____________________________________________________________________________________
	private void Cancel()
	{
		if(m_dialog != null)
		{
			m_io.SendLineCmd("\033");
			Util.HideKeyboard(m_input);
			m_dialog.dismiss();
			m_dialog = null;
		}
	}
}
