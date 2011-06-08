package com.tbd.NetHack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class Util
{
	static public View Inflate(int layoutId)
	{
		LayoutInflater vi = (LayoutInflater)NetHack.get().getLayoutInflater(); //SystemService(Context.LAYOUT_INFLATER_SERVICE);
		return vi.inflate(layoutId, null);
	}

	// ____________________________________________________________________________________
	public static String ObjectToString(Serializable obj)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(new Base64OutputStream(baos, Base64.NO_PADDING | Base64.NO_WRAP));
			oos.writeObject(obj);
			oos.close();
			return baos.toString("UTF-8");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	// ____________________________________________________________________________________
	public static Object StringToObject(String str)
	{
		try
		{
			return new ObjectInputStream(new Base64InputStream(new ByteArrayInputStream(str.getBytes()), Base64.NO_PADDING | Base64.NO_WRAP)).readObject();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	// ____________________________________________________________________________________
	public static void ShowKeyboard(final EditText input)
	{
		// Never explicitly open soft keyboard if physical keyboard is present 
		int keyboard = NetHack.get().getResources().getConfiguration().keyboard;
		if(keyboard == Configuration.KEYBOARD_NOKEYS)
		{
			input.post(new Runnable()
			{
				public void run()
				{
					InputMethodManager imm = (InputMethodManager)NetHack.get().getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.showSoftInput(input, InputMethodManager.SHOW_FORCED);
				}
			});
		}
	}

	// ____________________________________________________________________________________
	public static void HideKeyboard(EditText input)
	{
		InputMethodManager imm = (InputMethodManager)NetHack.get().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
	}
}
