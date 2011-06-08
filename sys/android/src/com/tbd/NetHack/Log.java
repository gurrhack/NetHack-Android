package com.tbd.NetHack;

public class Log
{

	public static void print(String string)
	{
		if(DEBUG.IsOn())
			android.util.Log.i("NetHack", string);
	}

	public static void print(int i)
	{
		if(DEBUG.IsOn())
			print(Integer.toString(i));
	}
	

}
