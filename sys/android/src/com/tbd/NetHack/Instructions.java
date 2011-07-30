package com.tbd.NetHack;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Window;

public class Instructions extends PreferenceActivity
{
	// ____________________________________________________________________________________
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// turn off the window's title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.instructions);
	}
}
