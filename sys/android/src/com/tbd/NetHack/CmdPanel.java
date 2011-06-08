package com.tbd.NetHack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.inputmethodservice.KeyboardView;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class CmdPanel
{
	private Button m_contextView;
	//private ArrayList<Cmd> m_cmdList;
	private NetHackIO m_io;
	private NHW_Map m_map;
	private ViewGroup m_panel;
	private int m_minBtnW;
	private int m_minBtnH;
	private LayoutParams m_params;
	private boolean m_bWizard;
	private boolean m_bPanelActive;
	private boolean m_userHidden;
	private SoftKeyboard m_keyboard;
	private View m_keyboardView;
	private ViewGroup m_keyboardGroup;

	// ____________________________________________________________________________________
	private interface Cmd
	{
		void Execute();
	}

	// ____________________________________________________________________________________
	private class ZoomCmd implements Cmd
	{
		private int m_amount;
		private char m_ch;

		public ZoomCmd(char c, int amount)
		{
			m_amount = amount;
			m_ch = c;
		}

		public void Execute()
		{
			if(m_map != null)
				m_map.Zoom(m_amount);
		}

		@Override
		public String toString()
		{
			return Character.toString(m_ch);
		}
	}

	// ____________________________________________________________________________________
	private class KeybCmd implements Cmd
	{
		public void Execute()
		{
			ToggleKeyboard();
		}

		@Override
		public String toString()
		{
			return "...";
		}
	}

	// ____________________________________________________________________________________
	private class NH_Cmd implements Cmd
	{
		private Button m_btn;

		public NH_Cmd()
		{
		}

		public void SetButton(Button btn)
		{
			m_btn = btn;
		}

		public String toString()
		{
			if(m_btn != null)
				return m_btn.getText().toString();
			return "";
		}

		public void Execute()
		{
			boolean bCtrl = false;
			CharSequence seq = m_btn.getText();
			for(int i = 0; i < seq.length(); i++)
			{
				char c = seq.charAt(i);
				if(bCtrl)
				{
					if(c == ' ')
						c = '^';
					else
						c = (char)(0x1f & c);
					bCtrl = false;
				}
				else if(c == '^')
					bCtrl = true;
				if(!bCtrl)
					m_io.SendKeyCmd(c);
			}
			if(bCtrl)
				m_io.SendKeyCmd('^');
		}
	}

	// ____________________________________________________________________________________
	public boolean IsWizCmd(String cmd)
	{
		if(cmd.length() != 2 || cmd.charAt(0) != '^')
			return false;
		char c1 = cmd.charAt(1);
		return c1 == 'e' || c1 == 'f' || c1 == 'g' || c1 == 'i' || c1 == 'o' || c1 == 'v' || c1 == 'w';
	}

	// ____________________________________________________________________________________
	public CmdPanel(NetHackIO io, NHW_Map map)
	{
		m_io = io;
		m_map = map;
		m_panel = (ViewGroup)NetHack.get().findViewById(R.id.cmdPanel);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NetHack.get());
		m_bWizard = prefs.getBoolean("wizard", false);
		m_keyboardGroup = (ViewGroup)NetHack.get().findViewById(R.id.keyboard_frame);
		LoadPanelLayout();
	}

	// ____________________________________________________________________________________
	public void onConfigurationChanged(Configuration newConfig)
	{
		//m_keyboard.onConfigurationChanged(newConfig);
		if(IsKeyboardShowing())
		{
			ToggleKeyboard();
			ToggleKeyboard();
		}
	}

	// ____________________________________________________________________________________
	public void LoadPanelLayout()
	{
		final float density = NetHack.get().getResources().getDisplayMetrics().density;
		m_minBtnW = (int)(45 * density + 0.5f);
		m_minBtnH = (int)(20 * density + 0.5f);
		m_params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		m_panel.removeAllViews();

		if(DEBUG.IsOn())
		{
			m_panel.addView(CreateCmdButton(new ZoomCmd('+', 20)));
			m_panel.addView(CreateCmdButton(new ZoomCmd('-', -20)));
		}

		String[] sadf = GetCmdList();
		for(String cmd : sadf)
		{
			View v = CreateCmdButton(cmd);
			m_panel.addView(v);
			if(IsWizCmd(cmd) && !m_bWizard)
				v.setVisibility(View.GONE);
		}
	}

	// ____________________________________________________________________________________
	public void ToggleKeyboard()
	{
		// Since the keyboard refuses to change its layout when the orientation changes
		// we recreate a new keyboard every time
		if(m_keyboardView == null)
		{
			HideInternal();
			m_keyboardView = (KeyboardView)Util.Inflate(R.layout.input);
			m_keyboardView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
			m_keyboardGroup.addView(m_keyboardView);
			m_keyboard = new SoftKeyboard(m_io, this, (KeyboardView)m_keyboardView);
		}
		else
		{
			m_keyboardGroup.removeView(m_keyboardView);
			m_keyboard = null;
			m_keyboardView = null;
			ShowInternal();
		}
	}

	// ____________________________________________________________________________________
	public void ResetPanelLayout()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NetHack.get());
		prefs.edit().remove("cmdLayout").commit();
		LoadPanelLayout();
	}

	// ____________________________________________________________________________________
	public void SavePanelLayout()
	{
		int ofs = DEBUG.IsOn() ? 3 : 1;
		int nSave = m_panel.getChildCount() - ofs;

		String s;
		if(nSave > 0)
		{
			String[] a = new String[nSave];
			for(int i = 0; i < nSave; i++)
				a[i] = ((Button)m_panel.getChildAt(i + ofs)).getText().toString();

			s = Util.ObjectToString(a);
		}
		else
			s = "";

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NetHack.get());
		if(s != null)
		{
			prefs.edit().putString("cmdLayout", s).commit();
		}
		else
		{
			prefs.edit().remove("cmdLayout").commit();
		}
	}

	// ____________________________________________________________________________________
	private String[] GetDefaultCmdList()
	{
		return NetHack.get().getResources().getStringArray(R.array.shortcutCmds);
	}

	// ____________________________________________________________________________________
	public String[] GetCmdList()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NetHack.get());
		String s = prefs.getString("cmdLayout", "");
		String[] a = null;

		if(s.length() > 0)
			a = (String[])Util.StringToObject(s);

		if(a == null)
			a = GetDefaultCmdList();

		return a;
	}

	// ____________________________________________________________________________________
	private Button CreateCmdButton(String chars)
	{
		// special case for keyboard. not very intuitive perhaps
		if(chars.equalsIgnoreCase("..."))
		{
			return CreateCmdButton(new KeybCmd());
		}

		NH_Cmd cmd = new NH_Cmd();
		Button btn = CreateCmdButton(cmd);
		cmd.SetButton(btn);
		btn.setText(chars);
		return btn;
	}

	// ____________________________________________________________________________________
	private Button CreateCmdButton()
	{
		Button btn = new Button(NetHack.get());
		btn.setTypeface(Typeface.SANS_SERIF);
		btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		btn.setMinimumWidth(m_minBtnW);
		btn.setMinimumHeight(m_minBtnH);
		btn.setLayoutParams(m_params);
		btn.setFocusable(false);
		btn.setFocusableInTouchMode(false);
		NetHack.get().registerForContextMenu(btn);
		return btn;
	}

	// ____________________________________________________________________________________
	private Button CreateCmdButton(final Cmd cmd)
	{
		Button btn = CreateCmdButton();
		btn.setText(cmd.toString());
		btn.setTag(cmd);
		btn.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				cmd.Execute();
			}
		});
		return btn;
	}

	// ____________________________________________________________________________________
	public void Show()
	{
		m_userHidden = false;
		ShowInternal();
	}

	// ____________________________________________________________________________________
	public void Hide()
	{
		m_userHidden = true;
		HideInternal();
	}

	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		MenuInflater inflater = NetHack.get().getMenuInflater();
		inflater.inflate(R.menu.customize_cmd, menu);
		menu.setHeaderTitle("Command: '" + v.getTag().toString() + "'");
		m_contextView = (Button)v;
		/*if(m_contextView == m_panel.getChildAt(0))
		{
			menu.getItem(0).setOnMenuItemClickListener(null);
			menu.getItem(0).setEnabled(false);
		}*/
	}

	// ____________________________________________________________________________________
	public void onContextItemSelected(MenuItem item)
	{
		final int itemId = item.getItemId();

		if(itemId == R.id.remove)
		{
			m_panel.removeView(m_contextView);
			SavePanelLayout();
		}
		else
		{
			final EditText input = new EditText(NetHack.get());
			input.setMaxLines(1);
			input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
			if(itemId == R.id.change)
				input.setText(m_contextView.getText());
			input.selectAll();

			AlertDialog.Builder builder = new AlertDialog.Builder(NetHack.get());
			builder.setTitle("Type command sequence");
			builder.setView(input);
			builder.setNegativeButton("Cancel", null);
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					int idx = m_panel.indexOfChild(m_contextView);
					String cmd = input.getText().toString().trim();
					if(itemId == R.id.change)
						m_panel.removeViewAt(idx);

					m_panel.addView(CreateCmdButton(cmd), idx);
					SavePanelLayout();
				}
			});
			builder.show();
		}
	}

	public void PreferencesUpdated()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NetHack.get());

		if(prefs.getBoolean("reset", false))
		{
			ResetPanelLayout();
			prefs.edit().putBoolean("reset", false).commit();
		}

		m_bPanelActive = prefs.getBoolean("showCmdPanel", true);

		if(m_bPanelActive)
			ShowInternal();
		else if(!m_bPanelActive)
			HideInternal();
	}

	private void ShowInternal()
	{
		if(m_bPanelActive && !m_userHidden && !IsKeyboardShowing())
			((ViewGroup)m_panel.getParent()).setVisibility(View.VISIBLE);
	}

	private void HideInternal()
	{
		if(m_bPanelActive)
			((ViewGroup)m_panel.getParent()).setVisibility(View.INVISIBLE);
		else
			((ViewGroup)m_panel.getParent()).setVisibility(View.GONE);
	}

	public boolean HandleKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK && IsKeyboardShowing())
		{
			ToggleKeyboard();
			return true;
		}
		return false;
	}

	public boolean IsKeyboardShowing()
	{
		return m_keyboardView != null;
	}

}
