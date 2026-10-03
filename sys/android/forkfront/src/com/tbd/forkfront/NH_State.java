package com.tbd.forkfront;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.*;
import com.tbd.forkfront.*;
import com.tbd.forkfront.Hearse.Hearse;

public class NH_State
{
	private enum CmdMode
	{
		Panel,
		Keyboard,
	}

	private Activity mContext;
	private NetHackIO mIO;
	private NHW_Message mMessage;
	private NHW_Status mStatus;
	private NHW_Map mMap;
	private NH_GetLine mGetLine;
	private NH_Question mQuestion;
	private ArrayList<NH_Window> mWindows;
	private Tileset mTileset;
	private CmdPanelLayout mCmdPanelLayout;
	private DPadOverlay mDPad;
	private boolean mIsDPadActive;
	private boolean mStickyKeyboard;
	private boolean mHideQuickKeyboard;
	private CmdMode mMode;
	private SoftKeyboard mKeyboard;
	private boolean mControlsVisible;
	private boolean mNumPad;
	private boolean mIsMouseLocked;
	private Hearse mHearse;
	private SoftKeyboard.KEYBOARD mRegularKeyboard;
	private SoundPlayer mSoundPlayer;

	// ____________________________________________________________________________________
	public NH_State(Activity context, ByteDecoder decoder)
	{
		mIO = new NetHackIO(context, NhHandler, decoder);
		mTileset = new Tileset(context);
		mWindows = new ArrayList<>();
		mGetLine = new NH_GetLine(mIO, this);
		mQuestion = new NH_Question(mIO, this);
		mMessage = new NHW_Message(context, mIO);
		mStatus = new NHW_Status(context, mIO);
		mMap = new NHW_Map(context, mTileset, mStatus, this, decoder);
		mCmdPanelLayout = (CmdPanelLayout)context.findViewById(R.id.cmdPanelLayout1);
		mDPad = new DPadOverlay(this);
		mKeyboard = new SoftKeyboard(context, this);
		mSoundPlayer = new SoundPlayer();
		mMode = CmdMode.Panel;

		setContext(context);
	}

	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		mContext = context;
		for(NH_Window w : mWindows)
			w.setContext(context);
		mGetLine.setContext(context);
		mQuestion.setContext(context);
		mMessage.setContext(context);
		mStatus.setContext(context);
		mCmdPanelLayout.setContext(context, this);
		mDPad.setContext(context);
		mMap.setContext(context);
		mTileset.setContext(context);
	}

	// ____________________________________________________________________________________
	public void startNetHack(String path)
	{
		mIO.start(path);

		preferencesUpdated();
		updateVisibleState();

		mMap.loadZoomLevel();

		// I have preferences already, might as well pass them in...
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mHearse = new Hearse(mContext, prefs, path);
	}

	// ____________________________________________________________________________________
	private String getLastUsername()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		return prefs.getString("lastUsername", "");
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

		mCmdPanelLayout.setOrientation(newConfig.orientation);
		mDPad.setOrientation(newConfig.orientation);
	}

	// ____________________________________________________________________________________
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void preferencesUpdated()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		mCmdPanelLayout.preferencesUpdated(prefs);
		mDPad.preferencesUpdated(prefs);
		mMap.preferencesUpdated(prefs);
		mStatus.preferencesUpdated(prefs);
		mMessage.preferencesUpdated(prefs);
		for(NH_Window w : mWindows)
		{
			if(w != mMap && w != mStatus && w != mMessage)
				w.preferencesUpdated(prefs);
		}

		if(mMode == CmdMode.Panel)
			mCmdPanelLayout.show();

		mTileset.updateTileset(prefs, mContext.getResources());
		mMap.updateZoomLimits();
		updateSystemUiVisibilityFlags(prefs);
	}

	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v)
	{
		mCmdPanelLayout.onCreateContextMenu(menu, v);
	}

	// ____________________________________________________________________________________
	public void onContextMenuClosed() {
		mCmdPanelLayout.onContextMenuClosed();
		updateSystemUiVisibilityFlags(PreferenceManager.getDefaultSharedPreferences(mContext));
	}

	// ____________________________________________________________________________________
	private void updateSystemUiVisibilityFlags(SharedPreferences prefs)
	{
		boolean isFullscreen = prefs.getBoolean("fullscreen", false);
		int fullscreenFlag = isFullscreen ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0;
		mContext.getWindow().setFlags(fullscreenFlag, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			boolean isImmersive = prefs.getBoolean("immersive", false);
			int uiVisibilityFlags = isImmersive ?
					(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
					: 0;
			mContext.getWindow().getDecorView().setSystemUiVisibility(uiVisibilityFlags);
		}
	}

	// ____________________________________________________________________________________
	public void onContextItemSelected(android.view.MenuItem item)
	{
		mCmdPanelLayout.onContextItemSelected(item);
	}

	// ____________________________________________________________________________________
	public boolean handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK && isKeyboardMode())
		{
			hideKeyboard();
			restoreRegularKeyboard();
			return true;
		}

		if(repeatCount > 0) switch(keyCode) {
			case KeyAction.Keyboard:
				if(mMode == CmdMode.Keyboard)
					mStickyKeyboard = false;
			case KeyAction.Control:
			case KeyAction.Meta:
			case KeyEvent.KEYCODE_ESCAPE:
				// Ignore repeat on these actions
				return true;
		}

		KeyEventResult ret = mGetLine.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);

		if(ret == KeyEventResult.IGNORED)
			ret = mQuestion.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);

		for(int i = mWindows.size() - 1; ret == KeyEventResult.IGNORED && i >= 0; i--)
		{
			NH_Window w = mWindows.get(i);
			ret = w.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);
		}

		if(ret == KeyEventResult.HANDLED)
			return true;
		if(ret == KeyEventResult.RETURN_TO_SYSTEM)
			return false;

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
			mStickyKeyboard = true;
			toggleKeyboard();
			return true;
		}
		else if(keyCode == KeyAction.Control || keyCode == KeyAction.Meta)
		{
			if(!Util.hasPhysicalKeyboard(mContext))
			{
				saveRegularKeyboard();
				if(mMode != CmdMode.Keyboard)
					mHideQuickKeyboard = true;
				showKeyboard();
				if(keyCode == KeyAction.Control)
					setCtrlKeyboard();
				else
					setMetaKeyboard();
			}
			return true;
		}
		if(DEBUG.runTrace() && keyCode == KeyEvent.KEYCODE_BACK)
			Debug.stopMethodTracing();
		return sendKeyCmd(nhKey);
	}

	// ____________________________________________________________________________________
	public NH_Window getWindow(int wid)
	{
		int i = getWindowI(wid);
		return i >= 0 ? mWindows.get(i) : null;
	}

	// ____________________________________________________________________________________
	public int getWindowI(int wid)
	{
		for(int i = 0; i < mWindows.size(); i++)
			if(mWindows.get(i).id() == wid)
				return i;
		return -1;
	}

	// ____________________________________________________________________________________
	public NH_Window toFront(int wid)
	{
		int i = getWindowI(wid);
		NH_Window w = null;
		if(i >= 0)
		{
			w = mWindows.get(i);
			if(i < mWindows.size() - 1)
			{
				mWindows.remove(i);
				mWindows.add(w);
			}
		}
		return w;
	}

	// ____________________________________________________________________________________
	public boolean handleKeyUp(int keyCode)
	{
		if(mMap.handleKeyUp(keyCode))
			return true;

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
				restoreRegularKeyboard();
			}

			mHideQuickKeyboard = false;
			return true;
		}
		return false;
	}

	// ____________________________________________________________________________________
	public boolean isMouseLocked()
	{
		return mIsMouseLocked;
	}

	// ____________________________________________________________________________________
	public void saveAndQuit()
	{
		mIO.saveAndQuit();
	}

	// ____________________________________________________________________________________
	public void saveState()
	{
		mIO.saveState();
	}

	// ____________________________________________________________________________________
	public Handler getHandler()
	{
		return mIO.getHandler();
	}

	// ____________________________________________________________________________________
	public void waitReady()
	{
		mIO.waitReady();
	}

	// ____________________________________________________________________________________
	public boolean sendKeyCmd(int key)
	{
		if(key <= 0 || key > 0xff)
			return false;
		mIO.sendKeyCmd((char)key);
		return true;
	}

	// ____________________________________________________________________________________
	public boolean sendDirKeyCmd(int key)
	{
		if(key <= 0 || key > 0xff)
			return false;
		if(key == 0x80 || key == '\033')
			mIsMouseLocked = false;
		if(mIsDPadActive)
			mIO.sendKeyCmd((char)key);
		else
			mIO.sendDirKeyCmd((char)key);
		return true;
	}

	// ____________________________________________________________________________________
	public void sendPosCmd(int x, int y)
	{
		mIsMouseLocked = false;
		mIO.sendPosCmd(x, y);
	}

	// ____________________________________________________________________________________
	public void clickCursorPos()
	{
		mMap.onCursorPosClicked();
	}

	// ____________________________________________________________________________________
	public boolean expectsDirection()
	{
		return mIsDPadActive;
	}

	// ____________________________________________________________________________________
	public boolean isDPadVisible()
	{
		return mDPad.isVisible();
	}

	// ____________________________________________________________________________________
	public void showControls()
	{
		mControlsVisible = true;
		updateVisibleState();
	}

	// ____________________________________________________________________________________
	public void hideControls()
	{
		mControlsVisible = false;
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
	public void setMetaKeyboard()
	{
		mKeyboard.setMetaKeyboard();
	}

	// ____________________________________________________________________________________
	private void saveRegularKeyboard()
	{
		mRegularKeyboard = mKeyboard.getKeyboard();
	}

	// ____________________________________________________________________________________
	private void restoreRegularKeyboard()
	{
		if(mRegularKeyboard != null)
			mKeyboard.setKeyboard(mRegularKeyboard);
		mRegularKeyboard = null;
	}

	// ____________________________________________________________________________________
	public void setCtrlKeyboard()
	{
		mKeyboard.setCtrlKeyboard();
	}

	// ____________________________________________________________________________________
	private boolean isKeyboardMode()
	{
		return mMode == CmdMode.Keyboard && mControlsVisible;
	}

	// ____________________________________________________________________________________
	public void updateVisibleState()
	{
		if(mControlsVisible)
		{
			if(mMode == CmdMode.Panel)
			{
				mKeyboard.hide();
				if(mIsDPadActive)
				{
					mDPad.showDirectional(true);
					mCmdPanelLayout.hide();
				}
				else
				{
					mDPad.showDirectional(false);
					mCmdPanelLayout.show();
				}
			}
			else
			{
				mKeyboard.show();
				mCmdPanelLayout.hide();
				//mDPad.setVisible(false);
				mDPad.forceHide();
			}
		}
		else
		{
			mCmdPanelLayout.hide();
			mKeyboard.hide();
			mDPad.forceHide();
		}
	}

	// ____________________________________________________________________________________
	public void viewAreaChanged(Rect viewRect)
	{
		mMap.viewAreaChanged(viewRect);
	}

	// ____________________________________________________________________________________
	public boolean isNumPadOn()
	{
		return mNumPad;
	}

	// ____________________________________________________________________________________
	public void startPreferences()
	{
		Intent prefsActivity = new Intent(mContext.getBaseContext(), Settings.class);
		mContext.startActivityForResult(prefsActivity, 42);
	}

	// ____________________________________________________________________________________
	private NH_Handler NhHandler = new NH_Handler() {
		@Override
		public void setLastUsername(String username) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			prefs.edit().putString("lastUsername", username).commit();
		}

		// ____________________________________________________________________________________
		@Override
		public void setCursorPos(int wid, int x, int y) {
			NH_Window wnd = getWindow(wid);
			if(wnd != null)
				wnd.setCursorPos(x, y);
		}

		// ____________________________________________________________________________________
		@Override
		public void putString(int wid, int attr, String msg, int append, int color) {
			NH_Window wnd = getWindow(wid);
			if(wnd == null) {
				Log.print("[no wnd] " + msg);
				mMessage.printString(attr, msg, append, color);
			} else
				wnd.printString(attr, msg, append, color);
		}

		// ____________________________________________________________________________________
		@Override
		public void setHealthColor(int color) {
			if(mMap != null)
				mMap.setHealthColor(color);
		}

		// ____________________________________________________________________________________
		@Override
		public void rawPrint(int attr, String msg) {
			mMessage.printString(attr, msg, 0, -1);
		}

		// ____________________________________________________________________________________
		@Override
		public void printTile(int wid, int x, int y, int tile, int ch, int col, int special) {
			mMap.printTile(x, y, tile, ch, col, special);
		}

		// ____________________________________________________________________________________
		@Override
		public void ynFunction(String question, byte[] choices, int def) {
			mQuestion.show(mContext, question, choices, def);
		}

		// ____________________________________________________________________________________
		@Override
		public void getLine(String title, int nMaxChars, boolean showLog) {
			if(showLog)
				mGetLine.show(mContext, mMessage.getLogLine(2) + title, nMaxChars);
			else
				mGetLine.show(mContext, title, nMaxChars);
		}

		// ____________________________________________________________________________________
		@Override
		public void askName(int nMaxChars, String[] saves) {
			String last = getLastUsername();
			List<String> list = new ArrayList<>();
			for(String s : saves) {
				if(last.equals(s))
					list.add(0, s);
				else
					list.add(s);
			}
			mGetLine.showWhoAreYou(mContext, nMaxChars, list);
		}

		// ____________________________________________________________________________________
		@Override
		public void loadSound(String filename)
		{
			mSoundPlayer.load(filename);
		}

		@Override
		public void playSound(String filename, int volume)
		{
			mSoundPlayer.play(filename, volume);
		}

		// ____________________________________________________________________________________
		@Override
		public void createWindow(int wid, int type)
		{
			switch(type)
			{
			case 1: // #define NHW_MESSAGE 1
				mMessage.setId(wid);
				mWindows.add(mMessage);
			break;

			case 2: // #define NHW_STATUS 2
				mStatus.setId(wid);
				mWindows.add(mStatus);
			break;

			case 3: // #define NHW_MAP 3
				mMap.setId(wid);
				mWindows.add(mMap);
			break;

			case 4: // #define NHW_MENU 4
				mWindows.add(new NHW_Menu(wid, mContext, mIO, mTileset));
			break;

			case 5: // #define NHW_TEXT 5
				mWindows.add(new NHW_Text(wid, mContext, mIO));
			break;
			}
		}

		// ____________________________________________________________________________________
		@Override
		public void displayWindow(final int wid, final int bBlocking)
		{
			NH_Window win = toFront(wid);
			if(win != null)
				win.show(bBlocking != 0);
		}

		// ____________________________________________________________________________________
		@Override
		public void clearWindow(final int wid, final int isRogueLevel)
		{
			NH_Window wnd = getWindow(wid);
			if(wnd != null)
			{
				wnd.clear();
				if(wnd == mMap)
					mMap.setRogueLevel(isRogueLevel != 0);
			}
		}

		// ____________________________________________________________________________________
		@Override
		public void destroyWindow(final int wid)
		{
			int i = getWindowI(wid);
			mWindows.get(i).destroy();
			mWindows.remove(i);
		}

		// ____________________________________________________________________________________
		@Override
		public void startMenu(final int wid)
		{
			((NHW_Menu)getWindow(wid)).startMenu();
		}

		// ____________________________________________________________________________________
		@Override
		public void addMenu(int wid, int tile, int id, int acc, int groupAcc, int attr, String text, int bSelected, int color)
		{
			((NHW_Menu)getWindow(wid)).addMenu(tile, id, acc, groupAcc, attr, text, bSelected, color);
		}

		// ____________________________________________________________________________________
		@Override
		public void endMenu(int wid, String prompt)
		{
			((NHW_Menu)getWindow(wid)).endMenu(prompt);
		}

		// ____________________________________________________________________________________
		@Override
		public void selectMenu(int wid, int how)
		{
			((NHW_Menu)toFront(wid)).selectMenu(MenuSelectMode.fromInt(how));
		}

		// ____________________________________________________________________________________
		@Override
		public void cliparound(int x, int y, int playerX, int playerY)
		{
			mMap.cliparound(x, y, playerX, playerY);
		}

		// ____________________________________________________________________________________
		@Override
		public void showLog(final int bBlocking)
		{
			mMessage.showLog(bBlocking != 0);
		}

		// ____________________________________________________________________________________
		@Override
		public void editOpts()
		{
		}

		// ____________________________________________________________________________________
		@Override
		public void lockMouse()
		{
			mIsMouseLocked = true;
		}

		// ____________________________________________________________________________________
		@Override
		public void showDPad()
		{
			mIsDPadActive = true;
			updateVisibleState();
		}

		// ____________________________________________________________________________________
		@Override
		public void hideDPad()
		{
			mIsDPadActive = false;
			updateVisibleState();
		}

		// ____________________________________________________________________________________
		@Override
		public void setNumPadOption(boolean numPadOn) {
			mNumPad = numPadOn;
			mDPad.updateNumPadState();
		}

		// ____________________________________________________________________________________
		@Override
		public void redrawStatus()
		{
			mStatus.redraw();
		}
	};
}
