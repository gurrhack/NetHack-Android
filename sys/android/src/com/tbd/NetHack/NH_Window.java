package com.tbd.NetHack;

import android.app.Activity;

import java.util.Set;

public interface NH_Window
{
	public int id();
	public void show(boolean bBlocking);
	public void destroy();
	public void clear();
	public void printString(TextAttr attr, String str, int append);
	public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput);
	public void setContext(Activity context);
	public String getTitle();
}
