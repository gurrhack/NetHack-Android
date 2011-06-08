package com.tbd.NetHack;

import java.util.LinkedList;

import android.app.Activity;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class NHW_Message implements NH_Window
{
	private TextView m_view;
	private View m_more;
	private int m_nVisibleLines;
	private LinkedList<Spanned> m_log;
	private int m_nPrev;
	private Activity m_context;
	private NetHackIO m_io;
	private NHW_Text m_logView;
	private final int MaxLog = 100;

	// ____________________________________________________________________________________
	public NHW_Message(final Activity context, final NetHackIO io)
	{
		m_context = context;
		m_io = io;
		m_view = (TextView)context.findViewById(R.id.nh_message);
		m_more = context.findViewById(R.id.more);
		m_more.setVisibility(View.GONE);
		m_more.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				ShowLog();
			}
		});
		m_log = new LinkedList<Spanned>();
		for(int i = 0; i < MaxLog; i++)
			m_log.add(null);
		m_nVisibleLines = 0;
		m_nPrev = 0;
	}

	// ____________________________________________________________________________________
	public boolean HandleKeyDown(int keyCode, KeyEvent event)
	{
		if(m_logView != null && m_logView.IsVisible() && m_logView.HandleKeyDown(keyCode, event))
			return true;
		return false;
	}

	// ____________________________________________________________________________________
	public void Clear()
	{
		m_more.setVisibility(View.GONE);
		m_view.setText("");
		m_nVisibleLines = 0;
	}

	// ____________________________________________________________________________________
	public void PrintString(TextAttr attr, String str)
	{
		if(m_nVisibleLines < 3)
		{
			m_more.setVisibility(View.GONE);
			if(m_nVisibleLines > 0)
				m_view.append("\n" + str);
			else
				m_view.setText(str);
			m_nVisibleLines++;
		}
		else
		{
			m_more.setVisibility(View.VISIBLE);
		}
		m_log.add(attr.Style(str));
		m_log.remove();
		m_nPrev = 0;
	}

	// ____________________________________________________________________________________
	public void Show(boolean bBlocking)
	{
		m_view.setVisibility(View.VISIBLE);
		if(bBlocking)
		{
			// unblock immediatly
			m_io.SendKeyCmd(' ');
		}
	}

	// ____________________________________________________________________________________
	public void Hide()
	{
		m_view.setVisibility(View.INVISIBLE);
		m_more.setVisibility(View.GONE);
	}

	// ____________________________________________________________________________________
	public void ShowPrev()
	{
		if(m_log.getLast() == null)
			return;

		if(m_nVisibleLines > 0)
			Clear();
		if(++m_nPrev >= m_log.size() || m_log.get(m_nPrev) == null)
			m_nPrev = 0;
		m_view.setText(m_log.get(m_log.size() - 1 - m_nPrev));
	}

	// ____________________________________________________________________________________
	public void ShowLog()
	{
		if(m_logView == null)
			m_logView = new NHW_Text(m_context, m_io);

		m_logView.Clear();
		for(Spanned s : m_log)
		{
			if(s != null)
				m_logView.PrintString(s);
		}
		m_logView.Show(false);
		m_logView.ScrollToEnd();
		m_more.setVisibility(View.GONE);
	}
}
