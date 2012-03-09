/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tbd.NetHack;

import java.io.File;
import java.util.EnumSet;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import com.tbd.NetHack.Input.Modifier;

public class NetHack extends Activity
{
	private static NH_State nhState;
	private static File mAppDir;
	private boolean mCtrlDown;
	private boolean mMetaDown;

	// ____________________________________________________________________________________
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.print("onCreate");

		// turn off the window's title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setDefaultKeyMode(DEFAULT_KEYS_DISABLE);
		// takeKeyEvents(true);

		setContentView(R.layout.mainwindow);

		if(nhState == null)
		{
			nhState = new NH_State(this);
			new UpdateAssets(this).execute((Void[])null);
		}
		else
		{
			Log.print("restoring state");
			nhState.setContext(this);
		}
	}

	// ____________________________________________________________________________________
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		Log.print("onConfigurationChanged");
		nhState.onConfigurationChanged(newConfig);
		super.onConfigurationChanged(newConfig);
	}
	
	// ____________________________________________________________________________________
	public void start(File path)
	{
		mAppDir = path;
		
		// Create save directory if it doesn't exist
		File nhSaveDir = new File(path, "save");
		if(!nhSaveDir.exists())
			nhSaveDir.mkdir();

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(prefs.getBoolean("firsttime", true))
		{
			prefs.edit().putBoolean("firsttime", false).commit();
			Intent prefsActivity = new Intent(getBaseContext(), Instructions.class);
			startActivity(prefsActivity);
		}
		
		nhState.startNetHack(path.getAbsolutePath());
	}

	// ____________________________________________________________________________________
	@Override
	protected void onStart()
	{
		mCtrlDown = false;
		mMetaDown = false;

		Log.print("onStart");
		if(DEBUG.runTrace())
			Debug.startMethodTracing("nethack");
		super.onStart();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onResume()
	{
		mCtrlDown = false;
		mMetaDown = false;

		Log.print("onResume");
		super.onResume();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onPause()
	{
		mCtrlDown = false;
		mMetaDown = false;

		super.onPause();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onStop()
	{
		mCtrlDown = false;
		mMetaDown = false;

		Log.print("onStop");
		super.onStop();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onDestroy()
	{
		mCtrlDown = false;
		mMetaDown = false;
		
		Log.print("onDestroy()");
		if(nhState != null)
			nhState.saveAndQuit();
		
		super.onDestroy();
	}

	// ____________________________________________________________________________________
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		mCtrlDown = false;
		mMetaDown = false;

		Log.print("onCreateOptionsMenu");
		menu.add(0, 1, 0, "Settings");

		return super.onCreateOptionsMenu(menu);
	}

	// ____________________________________________________________________________________
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		mCtrlDown = false;
		mMetaDown = false;

		Log.print(String.format("onOptionsItemSelected(item=%d)", item.getItemId()));
		if(item.getItemId() == 1)
		{
			Intent prefsActivity = new Intent(getBaseContext(), Settings.class);
			startActivityForResult(prefsActivity, 42);
			return true;
		}

		return false;
	}

	// ____________________________________________________________________________________
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		mCtrlDown = false;
		mMetaDown = false;

		super.onCreateContextMenu(menu, v, menuInfo);
		nhState.onCreateContextMenu(menu, v, menuInfo);
	}

	// ____________________________________________________________________________________
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		mCtrlDown = false;
		mMetaDown = false;

		nhState.onContextItemSelected(item);
		return super.onContextItemSelected(item);
	}

	// ____________________________________________________________________________________
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		mCtrlDown = false;
		mMetaDown = false;

		Log.print(String.format("onActivityResult(requestCode=%d, resultCode=%d)", requestCode, resultCode));
		if(requestCode == 42)
		{
			nhState.preferencesUpdated();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	// ____________________________________________________________________________________
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		mCtrlDown = false;
		mMetaDown = false;

		Log.print("onSaveInstanceState(Bundle outState)");
		if(nhState != null)
			nhState.saveState();
	};

	// ____________________________________________________________________________________
	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if(event.getAction() == KeyEvent.ACTION_DOWN)
		{
			Log.print("dispatchKeyEvent: " + Integer.toString(event.getKeyCode()));
			if(handleKeyDown(event))
				return true;
		}
		return super.dispatchKeyEvent(event);
	}
	
	// ____________________________________________________________________________________
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		Log.print("onKeyDown: " + Integer.toString(keyCode));
		return super.onKeyDown(keyCode, event);
	}
	
	// ____________________________________________________________________________________
	public boolean handleKeyDown(KeyEvent event)
	{
		int keyCode = event.getKeyCode();
		
		int fixedCode = Input.keyCodeToAction(keyCode, this);
		
		if(fixedCode == KeyAction.Control)
			mCtrlDown = true;
		else if(fixedCode == KeyAction.Meta)
			mMetaDown = true;
		
		EnumSet<Modifier> modifiers = Input.modifiersFromKeyEvent(event);
		if(mCtrlDown)
			modifiers.add(Input.Modifier.Control);
		else if(mMetaDown)
			modifiers.add(Input.Modifier.Meta);
		
		char ch = (char)event.getUnicodeChar();
		
		int nhKey = Input.nhKeyFromKeyCode(fixedCode, ch, modifiers);
		
		if(nhState.handleKeyDown(ch, nhKey, fixedCode, modifiers, event.getRepeatCount(), false))
			return true;

		if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			// Prevent default system sound from playing
			return true;
		}
		return false;//super.onKeyDown(keyCode, event);
	}

	// ____________________________________________________________________________________
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		int fixedCode = Input.keyCodeToAction(keyCode, this);
		
		if(fixedCode == KeyAction.Control)
			mCtrlDown = false;
		else if(fixedCode == KeyAction.Meta)
			mMetaDown = false;

		if(nhState.handleKeyUp(Input.keyCodeToAction(keyCode, this)))
			return true;
		if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			// Prevent default system sound from playing
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	public static File getApplicationDir()
	{
		return mAppDir;
	}
}
