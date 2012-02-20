package com.tbd.NetHack;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.Window;
import android.widget.Toast;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	private Toast mToast;

	// ____________________________________________________________________________________
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// turn off the window's title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		mToast = Toast.makeText(Settings.this, "This will take effect the next time you start NetHack", 1000);

		findPreference("wizard").setOnPreferenceChangeListener(requiresRestart);
		findPreference("username").setOnPreferenceChangeListener(requiresRestart);
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
	}

	// ____________________________________________________________________________________
	@Override
	protected void onPause()
	{
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	// ____________________________________________________________________________________
	OnPreferenceChangeListener requiresRestart = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			if(!newValue.equals(preference.getSharedPreferences().getAll().get(preference.getKey())))
				mToast.show();
			return true;
		}
	};

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
