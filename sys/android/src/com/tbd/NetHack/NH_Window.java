package com.tbd.NetHack;

import android.view.KeyEvent;

public interface NH_Window
{
	public void Show(boolean bBlocking);
	public void Hide();
	public void Clear();
	public void PrintString(TextAttr attr, String str);
	public boolean HandleKeyDown(int keyCode, KeyEvent event);
}
