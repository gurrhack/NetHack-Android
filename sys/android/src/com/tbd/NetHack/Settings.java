package com.tbd.NetHack;

import com.tbd.NetHack.R;
import com.tbd.NetHack.R.xml;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.Window;
import android.widget.Toast;

public class Settings extends PreferenceActivity
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
		
		//this.findPreference("wizard").setOnPreferenceChangeListener(requiresRestart);
		this.findPreference("username").setOnPreferenceChangeListener(requiresRestart);
	}
	
	// ____________________________________________________________________________________
	OnPreferenceChangeListener requiresRestart = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			mToast.show();
			return true;
		}
	};
}
