package com.tbd.NetHack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;

public class NH_Dialog extends AlertDialog
{
	
	public NH_Dialog(Context context)
	{
		super(context);
	}
/*
	public NH_Dialog(Context context, int theme)
	{
		super(context, theme);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}*/
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	public void setPositiveButton(String string, OnClickListener onClickListener)
	{
		setButton(DialogInterface.BUTTON_POSITIVE, string, onClickListener);
	}

	public void setNegativeButton(String string, OnClickListener onClickListener)
	{
		setButton(DialogInterface.BUTTON_NEGATIVE, string, onClickListener);
	}

	public void setNeutralButton(String string, OnClickListener onClickListener)
	{
		setButton(DialogInterface.BUTTON_NEUTRAL, string, onClickListener);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
			return true;
		return super.onKeyDown(keyCode, event);
	};

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
			return true;
		return super.onKeyUp(keyCode, event);
	};
}
