package com.tbd.NetHack;

import java.util.Set;

import android.app.Activity;
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
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.tbd.NetHack.Cmd.KeySequnece;

public class CmdPanel
{
	private enum CmdMode
	{
		Panel,
		Keyboard,
	}

	private UI mUI;
	private NetHack mContext;
	private NetHackIO mIO;
	private boolean mIsWizard;
	private boolean mIsPanelEnabled;
	private boolean mIsVisible;
	private CmdMode mMode;
	private boolean mIsDPadActive;
	private DPadOverlay mDPad;
	private NH_State mState;
	private boolean mStickyKeyboard;
	private boolean mHideQuickKeyboard;

	// ____________________________________________________________________________________
	public CmdPanel(NH_State state, NetHackIO io)
	{
		mState = state;
		mIO = io;
		mDPad = new DPadOverlay(this);
		mMode = CmdMode.Panel;
	}

	// ____________________________________________________________________________________
	public void setContext(NetHack context, NHW_Map map)
	{
		mContext = context;
		mDPad.setContext(context);
		mUI = new UI(context, map);
		updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void setWizard(boolean bWizard)
	{
		if(mIsWizard != bWizard)
		{
			mIsWizard = bWizard;
			mUI.loadPanelLayout();
		}
	}

	// ____________________________________________________________________________________
	public void onConfigurationChanged(Configuration newConfig)
	{
		mUI.onConfigurationChanged(newConfig);
	}

	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		mUI.onCreateContextMenu(menu, v, menuInfo);
	}

	// ____________________________________________________________________________________
	public void onContextItemSelected(MenuItem item)
	{
		mUI.onContextItemSelected(item);
	}

	// ____________________________________________________________________________________
	public void preferencesUpdated(SharedPreferences prefs)
	{
		boolean bResetPanel = prefs.getBoolean("reset", false);
		if(bResetPanel)
		{
			prefs.edit().putBoolean("reset", false).commit();
			prefs.edit().remove("cmdLayout").commit();
			mUI.loadPanelLayout();
		}

		mIsPanelEnabled = prefs.getBoolean("showCmdPanel", true);

		if(mMode == CmdMode.Panel)
		{
			if(mIsPanelEnabled)
				mUI.showPanel();
			else
				mUI.hidePanel();
		}
	}

	// ____________________________________________________________________________________
	public boolean handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)
		{
			if(mMode == CmdMode.Keyboard)
			{
				hideKeyboard();
				return true;
			}
			
			if(mIsDPadActive)
				return sendKeyCmd('\033');
		}
		else if(keyCode == KeyAction.Keyboard)
		{
			if(repeatCount == 0)
			{
				mStickyKeyboard = true;
				toggleKeyboard();
			}
			else if(mMode == CmdMode.Keyboard)
				mStickyKeyboard = false;
			return true;
		}
		else if(keyCode == KeyAction.Control || keyCode == KeyAction.Meta)
		{
			if(repeatCount == 0 && !Util.hasPhysicalKeyboard(mContext))
			{
				if(mMode != CmdMode.Keyboard)
					mHideQuickKeyboard = true;
				showKeyboard();
				if(keyCode == KeyAction.Control)
					mUI.setCtrlKeyboard();
				else
					mUI.setMetaKeyboard();
			}
			return true;
		}

		return sendKeyCmd(nhKey);
	}

	// ____________________________________________________________________________________
	public boolean handleKeyUp(int keyCode)
	{
		if(keyCode == KeyAction.Keyboard)
		{
			if(!mStickyKeyboard && mMode == CmdMode.Keyboard)
				hideKeyboard();
			mStickyKeyboard = false;
			return true;
		}
		else if(keyCode == KeyAction.Control || keyCode == KeyAction.Meta)
		{
			if(mMode == CmdMode.Keyboard)
			{
				if(mHideQuickKeyboard)
					hideKeyboard();
				else
					mUI.setQwertyKeyboard();
			}
					
			mHideQuickKeyboard = false;
			return true;
		}
		return false;
	}

	// ____________________________________________________________________________________
	public int getCurPanelHeight()
	{
		if(mIsPanelEnabled && mMode == CmdMode.Panel)
			return mUI.getPanelHeight();
		return 0;
	}

	// ____________________________________________________________________________________
	public boolean isWizCmd(String cmd)
	{
		if(cmd.length() != 2 || cmd.charAt(0) != '^')
			return false;
		char c1 = cmd.charAt(1);
		return c1 == 'e' || c1 == 'f' || c1 == 'g' || c1 == 'i' || c1 == 'o' || c1 == 'v' || c1 == 'w';
	}

	// ____________________________________________________________________________________
	public void show()
	{
		mIsVisible = true;
		updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void hide()
	{
		mIsVisible = false;
		updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void showKeyboard()
	{
		mMode = CmdMode.Keyboard;
		updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void hideKeyboard()
	{
		mMode = CmdMode.Panel;
		updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void toggleKeyboard()
	{
		if(mMode == CmdMode.Panel)
			showKeyboard();
		else
			hideKeyboard();
	}

	// ____________________________________________________________________________________
	public void showDPad()
	{
		mIsDPadActive = true;
		updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void hideDPad()
	{
		mIsDPadActive = false;
		updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void updateVisibleState()
	{
		if(mIsVisible)
		{
			if(mMode == CmdMode.Panel)
			{
				mUI.hideKeyboard();
				if(mIsDPadActive)
				{
					mDPad.setVisible(true);
					mUI.hidePanel();
				}
				else
				{
					mDPad.setVisible(false);
					if(mIsPanelEnabled)
						mUI.showPanel();
					else
						mUI.hidePanel();
				}
			}
			else
			{
				mUI.showKeyboard();
				mUI.hidePanel();
				mDPad.setVisible(false);
			}
		}
		else
		{
			mUI.hidePanel();
			mUI.hideKeyboard();
			mDPad.setVisible(false);
		}
	}

	// ____________________________________________________________________________________
	public boolean sendKeyCmd(int key)
	{
		if(key <= 0 || key > 0xff)
			return false;
		hideDPad();
		//Log.print(Integer.toHexString(key&0x1f));
		mIO.sendKeyCmd((char)key);
		return true;
	}

	// ____________________________________________________________________________________
	public boolean sendDirKeyCmd(int key)
	{
		if(key <= 0 || key > 0xff)
			return false;
		if(mIsDPadActive)
			mIO.sendKeyCmd((char)key);
		else
			mIO.sendDirKeyCmd((char)key);
		hideDPad();
		return true;
	}

	// ____________________________________________________________________________________
	public void sendPosCmd(int x, int y)
	{
		hideDPad();
		mIO.sendPosCmd(x, y);
	}

	// ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private ViewGroup mPanel;
		private Button mContextView;
		private SoftKeyboard mKeyboard;
		private View mKeyboardView;
		private ViewGroup mKeyboardGroup;
		private int mMinBtnW;
		private int mMinBtnH;
		private LayoutParams mParams;
		private NHW_Map mMap;

		// ____________________________________________________________________________________
		public UI(Activity context, NHW_Map map)
		{
			mMap = map;
			mPanel = (ViewGroup)mContext.findViewById(R.id.cmdPanel);
			mKeyboardGroup = (ViewGroup)mContext.findViewById(R.id.kbd_frame);
			loadPanelLayout();
		}

		// ____________________________________________________________________________________
		private void showPanel()
		{
			((ViewGroup)mPanel.getParent()).setVisibility(View.VISIBLE);
		}

		// ____________________________________________________________________________________
		private void hidePanel()
		{
			if(mIsPanelEnabled)
				((ViewGroup)mPanel.getParent()).setVisibility(View.INVISIBLE);
			else
				((ViewGroup)mPanel.getParent()).setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		private void loadPanelLayout()
		{
			final float density = mContext.getResources().getDisplayMetrics().density;
			mMinBtnW = (int)(45 * density + 0.5f);
			mMinBtnH = (int)(20 * density + 0.5f);
			mParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

			mPanel.removeAllViews();

			if(DEBUG.isOn())
			{
				mPanel.addView(createCmdButton(new Cmd.Zoom(mMap, '+', 20)));
				mPanel.addView(createCmdButton(new Cmd.Zoom(mMap, '-', -20)));
			}

			String[] cmdList = getCmdList();
			for(String cmd : cmdList)
			{
				View v = createCmdButton(cmd);
				mPanel.addView(v);
				if(isWizCmd(cmd) && !mIsWizard)
					v.setVisibility(View.GONE);
			}
		}

		// ____________________________________________________________________________________
		public void showKeyboard()
		{
			if(mKeyboardView == null)
			{
				mKeyboardView = (KeyboardView)Util.inflate(mContext, R.layout.input);
				mKeyboardView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
				mKeyboardGroup.addView(mKeyboardView);
				mKeyboard = new SoftKeyboard(mContext, mState, CmdPanel.this, (KeyboardView)mKeyboardView);
			}
		}
		
		// ____________________________________________________________________________________
		public void hideKeyboard()
		{
			if(mKeyboardView != null)
			{
				if(true)
					Util.hideKeyboard(mContext, mKeyboardView);
				mKeyboardGroup.removeView(mKeyboardView);
				mKeyboard = null;
				mKeyboardView = null;
			}
		}

		// ____________________________________________________________________________________
		public void setMetaKeyboard()
		{
			if(mKeyboard != null)
				mKeyboard.setMetaKeyboard();
		}

		// ____________________________________________________________________________________
		public void setQwertyKeyboard()
		{
			if(mKeyboard != null)
				mKeyboard.setQwertyKeyboard();
		}

		// ____________________________________________________________________________________
		public void setCtrlKeyboard()
		{
			if(mKeyboard != null)
				mKeyboard.setCtrlKeyboard();
		}

		// ____________________________________________________________________________________
		public void onConfigurationChanged(Configuration newConfig)
		{
			if(mMode == CmdMode.Keyboard)
			{
				// Since the keyboard refuses to change its layout when the orientation changes
				// we recreate a new keyboard every time
				hideKeyboard();
				showKeyboard();
			}
		}

		// ____________________________________________________________________________________
		public void savePanelLayout()
		{
			int ofs = DEBUG.isOn() ? 2 : 0;
			int nSave = mPanel.getChildCount() - ofs;

			String s;
			if(nSave > 0)
			{
				String[] a = new String[nSave];
				for(int i = 0; i < nSave; i++)
					a[i] = ((Button)mPanel.getChildAt(i + ofs)).getText().toString();

				s = Util.objectToString(a);
			}
			else
				s = "";

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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
		private String[] getDefaultCmdList()
		{
			return mContext.getResources().getStringArray(R.array.shortcutCmds);
		}

		// ____________________________________________________________________________________
		public String[] getCmdList()
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			String s = prefs.getString("cmdLayout", "");
			String[] a = null;

			if(s.length() > 0)
				a = (String[])Util.stringToObject(s);

			if(a == null)
				a = getDefaultCmdList();

			return a;
		}

		// ____________________________________________________________________________________
		private Button createCmdButton(String chars)
		{
			// special case for keyboard. not very intuitive perhaps
			if(chars.equalsIgnoreCase("..."))
			{
				return createCmdButton(new Cmd.ToggleKeyboard(CmdPanel.this));
			}

			KeySequnece cmd = new Cmd.KeySequnece(mState);
			Button btn = createCmdButton(cmd);
			cmd.setButton(btn);
			btn.setText(chars);
			return btn;
		}

		// ____________________________________________________________________________________
		private Button createCmdButton()
		{
			Button btn = new Button(mContext);
			btn.setTypeface(Typeface.SANS_SERIF);
			btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			btn.setMinimumWidth(mMinBtnW);
			btn.setMinimumHeight(mMinBtnH);
			btn.setLayoutParams(mParams);
			btn.setFocusable(false);
			btn.setFocusableInTouchMode(false);
			mContext.registerForContextMenu(btn);
			return btn;
		}

		// ____________________________________________________________________________________
		private Button createCmdButton(final Cmd cmd)
		{
			Button btn = createCmdButton();
			btn.setText(cmd.toString());
			btn.setTag(cmd);
			btn.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					cmd.execute();
				}
			});
			return btn;
		}

		// ____________________________________________________________________________________
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
		{
			MenuInflater inflater = mContext.getMenuInflater();
			inflater.inflate(R.menu.customize_cmd, menu);
			menu.setHeaderTitle("Command: '" + v.getTag().toString() + "'");
			mContextView = (Button)v;
		}

		// ____________________________________________________________________________________
		public void onContextItemSelected(MenuItem item)
		{
			final int itemId = item.getItemId();

			if(itemId == R.id.remove)
			{
				mPanel.removeView(mContextView);
				savePanelLayout();
			}
			else if(itemId == R.id.add_kbd)
			{
				int idx = mPanel.indexOfChild(mContextView);
				mPanel.addView(createCmdButton("..."), idx);
				savePanelLayout();
			}
			else
			{
				final EditText input = new EditText(mContext);
				input.setMaxLines(1);
				input.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(20) });
				if(itemId == R.id.change)
					input.setText(mContextView.getText());
				input.selectAll();

				final NH_Dialog dlg = new NH_Dialog(mContext);
				dlg.setTitle("Type command sequence");
				dlg.setView(input);
				dlg.setNegativeButton("Cancel", null);
				dlg.setPositiveButton("Ok", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						int idx = mPanel.indexOfChild(mContextView);
						if(idx >= 0)
						{
							String cmd = input.getText().toString().trim();
							if(itemId == R.id.change)
								mPanel.removeViewAt(idx);
					
							mPanel.addView(createCmdButton(cmd), idx);
							savePanelLayout();
						}
					}
				});
				input.setOnEditorActionListener(new OnEditorActionListener()
				{
					public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
					{
						if(event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
						{
							if(dlg != null && dlg.isShowing())
							{
								dlg.getButton(NH_Dialog.BUTTON_POSITIVE).performClick();
								return true;
							}
						}
						return false;
					}
				});
				dlg.show();
			}
		}

		// ____________________________________________________________________________________
		public int getPanelHeight()
		{
			return mPanel.getHeight();
		}
	}

	public boolean isKeyboardMode()
	{
		return mMode == CmdMode.Keyboard;
	}
}
