package com.tbd.NetHack;

import com.tbd.NetHack.R;
import com.tbd.NetHack.R.xml;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class Settings extends PreferenceActivity
{
	private Toast m_toast;

	// ____________________________________________________________________________________
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		m_toast = Toast.makeText(Settings.this, "This will take effect the next time you start NetHack", 1000);
		
		this.findPreference("wizard").setOnPreferenceChangeListener(requiresRestart);
		this.findPreference("username").setOnPreferenceChangeListener(requiresRestart);
	}
	
	// ____________________________________________________________________________________
	OnPreferenceChangeListener requiresRestart = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			m_toast.show();
			return true;
		}
	};
}
