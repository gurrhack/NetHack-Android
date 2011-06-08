package com.tbd.NetHack;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class NHW_Text implements NH_Window
{
	private NetHackIO m_io;
	private boolean m_bBlocking;
	private TextView m_textView;
	private ScrollView m_scroll;
	private StringBuilder m_builder;

	// ____________________________________________________________________________________
	public NHW_Text(Activity context, final NetHackIO io)
	{
		ViewGroup root = ((ViewGroup)context.findViewById(R.id.base_frame));

		m_io = io;
		LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		m_scroll = (ScrollView)vi.inflate(R.layout.textwindow, null);
		m_textView = (TextView)m_scroll.findViewById(R.id.text_view);

		m_textView.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if(IsVisible())
					Close();
			}
		});

		root.addView(m_scroll);

		m_builder = new StringBuilder();
		Hide();
	}

	// ____________________________________________________________________________________
	public void Clear()
	{
		m_builder = new StringBuilder();
		m_textView.setText("");
	}

	// ____________________________________________________________________________________
	public void PrintString(Spanned str)
	{
		m_builder.append(str);
		m_builder.append('\n');
		m_textView.setText(m_builder.toString());
	}

	// ____________________________________________________________________________________
	public void PrintString(TextAttr attr, String str)
	{
		Log.print(str);
		m_builder.append(attr.Style(str));
		m_builder.append('\n');
		m_textView.setText(m_builder.toString());
	}

	// ____________________________________________________________________________________
	public void Show(boolean bBlocking)
	{
		m_bBlocking = bBlocking;
		m_scroll.setVisibility(View.VISIBLE);
	}

	// ____________________________________________________________________________________
	public void Hide()
	{
		m_scroll.setVisibility(View.GONE);
	}

	// ____________________________________________________________________________________
	public void ScrollToEnd()
	{
		if(IsVisible())
		{
			m_scroll.post(new Runnable() // gives the view a chance to update itself
			{
				public void run()
				{
					m_scroll.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		}
	}

	// ____________________________________________________________________________________
	public boolean IsVisible()
	{
		return m_scroll.getVisibility() == View.VISIBLE;
	}

	// ____________________________________________________________________________________
	public boolean HandleKeyDown(int keyCode, KeyEvent event)
	{
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_ENTER:
		case KeyEvent.KEYCODE_BACK:
		case KeyEvent.KEYCODE_SPACE:
		case KeyEvent.KEYCODE_DPAD_CENTER:
			if(IsVisible())
			{
				Close();
				return true;
			}

		case KeyEvent.KEYCODE_DPAD_UP:
			m_scroll.scrollBy(0, -m_textView.getLineHeight());
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			m_scroll.scrollBy(0, m_textView.getLineHeight());
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			m_scroll.pageScroll(View.FOCUS_UP);
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			m_scroll.pageScroll(View.FOCUS_DOWN);
			return true;
		}
		return false;
	}

	// ____________________________________________________________________________________
	private void Close()
	{
		if(m_bBlocking)
			m_io.SendKeyCmd(' ');
		Hide();
	}
}
