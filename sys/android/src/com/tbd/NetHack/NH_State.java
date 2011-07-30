package com.tbd.NetHack;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;

public class NH_State
{
	private Activity mContext;
	private NetHackIO mIO;
	private NHW_Message mMessage;
	private NHW_Status mStatus;
	private NHW_Map mMap;
	private NH_GetLine mGetLine;
	private NH_Question mQuestion;
	private ArrayList<NH_Window> mWindows;
	private Tileset mTileset;
	String mDataDir;
	String mUsername;
	boolean mIsWizard;
	private CmdPanel mCmdPanel;

	// ____________________________________________________________________________________
	public NH_State(NetHack context)
	{
		mIO = new NetHackIO(this);
		mTileset = new Tileset();
		mWindows = new ArrayList<NH_Window>();
		mGetLine = new NH_GetLine(mIO);
		mQuestion = new NH_Question(mIO);
		mMessage = new NHW_Message(context, mIO);
		mStatus = new NHW_Status(context, mIO);
		mCmdPanel = new CmdPanel(this, mIO);
		mMap = new NHW_Map(context, mTileset, mStatus, mCmdPanel);

		setContext(context);
	}

	// ____________________________________________________________________________________
	public void setContext(NetHack context)
	{
		mContext = context;
		for(NH_Window w : mWindows)
			w.setContext(context);
		mGetLine.setContext(context);
		mQuestion.setContext(context);
		mMessage.setContext(context);
		mStatus.setContext(context);
		mCmdPanel.setContext(context, mMap);
		mMap.setContext(context);
	}

	// ____________________________________________________________________________________
	public void startNetHack(String path)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		mUsername = prefs.getString("username", "").trim();
		if(mUsername.length() == 0)
		{
			mUsername = System.getProperty("user.name").trim();
			if(mUsername.length() == 0)
				mUsername = "app34";
			prefs.edit().putString("username", mUsername).commit();
		}
		mIsWizard = prefs.getBoolean("wizard", false);
		mDataDir = path;
		mCmdPanel.setWizard(mIsWizard);
		mIO.start();
		preferencesUpdated();
	}

	// ____________________________________________________________________________________
	public void onConfigurationChanged(Configuration newConfig)
	{
		mCmdPanel.onConfigurationChanged(newConfig);
	}

	// ____________________________________________________________________________________
	public void preferencesUpdated()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		mCmdPanel.preferencesUpdated(prefs);

		String tilesetName = prefs.getString("tileset", "default_32");
		if(mTileset.updateTileset(tilesetName, mContext.getResources()))
			mMap.resetZoom();

		String pickupTypes = prefs.getString("autoPickupTypes", "");
		boolean bAutoPickup = prefs.getBoolean("autoPickup", false);
		boolean bAutoMenu = prefs.getBoolean("automenu", true);
		mIO.sendFlags(bAutoMenu, bAutoPickup, pickupTypes);

		int flag = prefs.getBoolean("fullscreen", false) ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0;		
		mContext.getWindow().setFlags(flag, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		mCmdPanel.onCreateContextMenu(menu, v, menuInfo);
	}

	// ____________________________________________________________________________________
	public void onContextItemSelected(android.view.MenuItem item)
	{
		mCmdPanel.onContextItemSelected(item);
	}

	// ____________________________________________________________________________________
	public boolean handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(keyCode == KeyEvent.KEYCODE_BACK && mCmdPanel.isKeyboardMode())
		{
			mCmdPanel.hideKeyboard();
			return true;
		}
		
		int ret = mQuestion.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);
		
		for(int i = mWindows.size() - 1; ret == 0 && i >= 0; i--)
		{
			NH_Window w = mWindows.get(i);
			ret = w.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);
		}
		
		if(ret == 1)
			return true;
		if(ret == 2)// let system handle
			return false;
		
		if(mCmdPanel.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput))
			return true;
		
		return false;
	}
	
	// ____________________________________________________________________________________
	public boolean handleKeyUp(int keyCode)
	{
		if(mMap.handleKeyUp(keyCode))
			return true;
		return mCmdPanel.handleKeyUp(keyCode);
	}

	// ____________________________________________________________________________________
	public void setCursorPos(int wid, int x, int y)
	{
		NH_Window wnd = getWindow(wid);
		if(wnd == mMap)
			mMap.setCursorPos(x, y);
		else if(wnd == mStatus)
			mStatus.setRow(y);
		else if(wnd == mMessage)
			mMessage.setCursorPos(x);
	}

	// ____________________________________________________________________________________
	public void putString(int wid, int attr, String msg, int append, int hp, int hpMax)
	{
		NH_Window wnd = getWindow(wid);
		if(wnd == null)
		{
			Log.print("[no wnd] " + msg);
			mMessage.printString(TextAttr.fromNative(attr), msg, append);
		}
		else
			wnd.printString(TextAttr.fromNative(attr), msg, append);
		if(mMap != null)
			mMap.setHealthLevel(hp, hpMax);
	}

	// ____________________________________________________________________________________
	public void rawPrint(int attr, String msg)
	{
		mMessage.printString(TextAttr.fromNative(attr), msg, 0);
	}

	// ____________________________________________________________________________________
	public void printTile(int wid, int x, int y, int tile, int ch, int col, int special)
	{
		mMap.printTile(x, y, tile, ch, col, special);
	}

	// ____________________________________________________________________________________
	public void ynFunction(String question, byte[] choices, int def)
	{
		mQuestion.show(mContext, question, choices, def);
	}

	// ____________________________________________________________________________________
	public void getLine(String title, int nMaxChars)
	{
		mGetLine.show(mContext, title, nMaxChars);
	}

	// ____________________________________________________________________________________
	public void createWindow(int wid, int type)
	{
		Log.print("creat " + Integer.toString(wid));
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
	public void displayWindow(final int wid, final int bBlocking)
	{
		NH_Window win = toFront(wid);
		if(win != null)
			win.show(bBlocking != 0);
	}

	// ____________________________________________________________________________________
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
	public void destroyWindow(final int wid)
	{
		Log.print("dest " + Integer.toString(wid));
		// better hide it before we remove it. never know when the GC
		// decides to kick in
		int i = getWindowI(wid);
		mWindows.get(i).hide();
		mWindows.remove(i);
	}

	// ____________________________________________________________________________________
	public void startMenu(final int wid)
	{
		((NHW_Menu)getWindow(wid)).startMenu();
	}

	// ____________________________________________________________________________________
	public void addMenu(int wid, int tile, int id, int acc, int groupAcc, int attr, String text, int bSelected)
	{
		((NHW_Menu)getWindow(wid)).addMenu(tile, id, acc, groupAcc, TextAttr.fromNative(attr), text, bSelected);
	}

	// ____________________________________________________________________________________
	public void endMenu(int wid, String prompt)
	{
		((NHW_Menu)getWindow(wid)).endMenu(prompt);
	}

	// ____________________________________________________________________________________
	public void selectMenu(int wid, int how)
	{
		Log.print("select " + Integer.toString(wid));
		((NHW_Menu)toFront(wid)).selectMenu(NHW_Menu.SelectMode.fromInt(how));
	}

	// ____________________________________________________________________________________
	public void cliparound(int x, int y, int playerX, int playerY)
	{
		mMap.cliparound(x, y, playerX, playerY);
	}

	// ____________________________________________________________________________________
	public void askDirection()
	{
		showDPad();
	}

	// ____________________________________________________________________________________
	public void showPrevMessage()
	{
		mMessage.showPrev();
	}

	// ____________________________________________________________________________________
	public void showLog()
	{
		mMessage.showLog();
	}

	// ____________________________________________________________________________________
	public void lockMouse()
	{
		mMap.lockMouse();
	}

	// ____________________________________________________________________________________
	public void showDPad()
	{
		mCmdPanel.showDPad();
	}

	// ____________________________________________________________________________________
	public void hideDPad()
	{
		mCmdPanel.hideDPad();
	}

	// ____________________________________________________________________________________
	public String getDataDir()
	{
		return mDataDir;
	}

	// ____________________________________________________________________________________
	public String getUsername()
	{
		return mUsername;
	}

	// ____________________________________________________________________________________
	public boolean isWizard()
	{
		return mIsWizard;
	}

	// ____________________________________________________________________________________
	public void saveState()
	{
		mIO.saveState();
	}
}
