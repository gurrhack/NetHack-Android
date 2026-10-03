package com.tbd.forkfront;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.Set;

public interface NH_Window
{
	int id();
	void show(boolean bBlocking);
	void destroy();
	void clear();
	void printString(int attr, String str, int append, int color);
	KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput);
	void setContext(Activity context);
	String getTitle();
	void setCursorPos(int x, int y);
	void preferencesUpdated(SharedPreferences prefs);
}
