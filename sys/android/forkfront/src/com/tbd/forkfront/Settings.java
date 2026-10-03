package com.tbd.forkfront;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.Window;
import com.tbd.forkfront.TilesetPreference;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private TilesetPreference mTilesetPref;

	// ____________________________________________________________________________________
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// turn off the window's title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		Insets.applyTo(findViewById(android.R.id.content));
	}

	// ____________________________________________________________________________________
	@Override
	protected void onResume()
	{
		super.onResume();

		SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

		for(int i = 0; i < 10; i++)
		{
			char idx = (char)('0' + i);
			Preference screen = findPreference("panel" + idx);

			if(screen == null)
				break;

			String name = sharedPreferences.getString("pName" + idx, "");

			screen.setTitle(name);
		}

		sharedPreferences.registerOnSharedPreferenceChangeListener(this);

		mTilesetPref = (TilesetPreference)findPreference("tilesetPreference");
		mTilesetPref.setActivity(this);

		if(!getApplicationContext().getResources().getBoolean(R.bool.hearseAvailable))
		{
			PreferenceCategory hearseParent = (PreferenceCategory)findPreference("advanced");
			Preference hearsePref = findPreference("hearse");
			if(hearsePref != null)
				hearseParent.removePreference(hearsePref);
		}

		// Immersive mode only available on API 11 and up
		PreferenceCategory settingsCategory = (PreferenceCategory)findPreference("settings");
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
		{
			Preference fullscreenPref = findPreference("immersive");
			if(fullscreenPref != null)
				settingsCategory.removePreference(fullscreenPref);
		}
	}

	// ____________________________________________________________________________________
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		mTilesetPref.onActivityResult(requestCode, resultCode, data);
	}

	// ____________________________________________________________________________________
	@Override
	protected void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	// ____________________________________________________________________________________
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if(key.startsWith("pName"))
		{
			char idx = key.charAt(key.length() - 1);
			Preference screen = findPreference("panel" + idx);
			String name = sharedPreferences.getString("pName" + idx, "");
			screen.setTitle(name);
		}
	}
}
