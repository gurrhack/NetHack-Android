package com.tbd.NetHack;

import android.app.Activity;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

public class NHW_Status implements NH_Window
{
	private TextView[] m_rows = new TextView[2];
	private int m_iRow;
	private NetHackIO m_io;

	// ____________________________________________________________________________________
	public NHW_Status(Activity context, NetHackIO io)
	{
		m_io = io;
		m_iRow = 0;
		m_rows[0] = (TextView)context.findViewById(R.id.nh_stat0);
		m_rows[1] = (TextView)context.findViewById(R.id.nh_stat1);
		Hide();
	}

	// ____________________________________________________________________________________
	public void Show(boolean bBlocking)
	{
		m_rows[0].setVisibility(View.VISIBLE);
		m_rows[1].setVisibility(View.VISIBLE);
		if(bBlocking)
		{
			// unblock immediatly
			m_io.SendKeyCmd(' ');
		}
	}

	// ____________________________________________________________________________________
	public void Hide()
	{
		m_rows[0].setVisibility(View.GONE);
		m_rows[1].setVisibility(View.GONE);
	}

	// ____________________________________________________________________________________
	public void Clear()
	{
		m_rows[0].setText("");
		m_rows[1].setText("");
		Hide();
	}

	// ____________________________________________________________________________________
	public void PrintString(TextAttr attr, String str)
	{
		m_rows[m_iRow].setText(attr.Style(str));
	}

	// ____________________________________________________________________________________
	public void SetRow(int y)
	{
		m_iRow = y == 0 ? 0 : 1;
	}

	// ____________________________________________________________________________________
	public boolean HandleKeyDown(int keyCode, KeyEvent event)
	{
		return false;
	}
}
