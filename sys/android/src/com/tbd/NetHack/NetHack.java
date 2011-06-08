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

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

public class NetHack extends Activity
{

	private NetHackIO m_io;
	private Tileset m_tileset;
	private static NetHack m_instance;

	// ____________________________________________________________________________________
	public static AssetManager getAssetManager()
	{
		return m_instance.getResources().getAssets();
	}

	// ____________________________________________________________________________________
	public static NetHack get()
	{
		return m_instance;
	}

	// ____________________________________________________________________________________
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		m_instance = this;

		// turn off the window's title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		new UpdateAssets().execute((Void[])null);
	}

	// ____________________________________________________________________________________
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		if(m_io != null)
			m_io.onConfigurationChanged(newConfig);
		super.onConfigurationChanged(newConfig);
	}
	
	// ____________________________________________________________________________________
	public void Start(File path)
	{
		// Create save directory if it doesn't exist
		File nhSaveDir = new File(path, "save");
		if(!nhSaveDir.exists())
			nhSaveDir.mkdir();

		m_tileset = new Tileset();

		m_io = new NetHackIO(m_tileset, path.getAbsolutePath());
	}

	// ____________________________________________________________________________________
	@Override
	protected void onStart()
	{
		Log.print("onStart");
		super.onStart();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onResume()
	{
		Log.print("onResume");
		super.onResume();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onPause()
	{
		Log.print("onPause. cheat protected");
		// prevent cheating
		if(m_io != null)
			m_io.SaveState();
		super.onPause();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onStop()
	{
		Log.print("onStop");
		super.onStop();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onDestroy()
	{
		Log.print("onDestroy");
		super.onStop();
	}

	// ____________________________________________________________________________________
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		menu.add(0, 1, 0, "Settings");

		return super.onCreateOptionsMenu(menu);
	}

	// ____________________________________________________________________________________
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
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
		super.onCreateContextMenu(menu, v, menuInfo);
		m_io.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		m_io.onContextItemSelected(item);
		return super.onContextItemSelected(item);
	}

	// ____________________________________________________________________________________
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.print(String.format("onActivityResult(requestCode=%d, resultCode=%d)", requestCode, resultCode));
		if(requestCode == 42 && m_io != null)
		{
			m_tileset.PreferencesUpdated();
			m_io.PreferencesUpdated();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	// ____________________________________________________________________________________
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		Log.print("onSaveInstanceState(Bundle outState)");
		// m_io.SendKeyCmd('S');
		// outState.putInt("apan", 42);
	};

	// ____________________________________________________________________________________
	private void SaveAndExit()
	{
		Log.print("SaveAndExit");
		m_io.SaveAndExit();
		//finish();
		// System.runFinalizersOnExit(true);
		//System.exit(0);
	}

	// ____________________________________________________________________________________
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(m_io.HandleKeyDown(keyCode, event))
			return true;
		if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)
		{
			SaveAndExit();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	// ____________________________________________________________________________________
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(m_io.HandleKeyUp(keyCode, event))
			return true;
		return super.onKeyDown(keyCode, event);
	}
}
