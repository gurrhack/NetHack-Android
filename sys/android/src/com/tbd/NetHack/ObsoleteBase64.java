package com.tbd.NetHack;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import android.util.Base64;
import android.util.Base64InputStream;

public class ObsoleteBase64
{
	private static boolean mAvailable = false;

	static
	{
		try
		{
			// These are not available on API level 7
			Class.forName("android.util.Base64");
			Class.forName("android.util.Base64InputStream");
			Class.forName("android.util.Base64OutputStream");
			mAvailable = true;
		}
		catch(Exception ex)
		{
			mAvailable = false;
		}
	}

	// ____________________________________________________________________________________
	public static Object stringToObject(String str)
	{
		if(!mAvailable)
			return null;
		try
		{
			Base64InputStream b64i = new Base64InputStream(new ByteArrayInputStream(str.getBytes()), Base64.NO_PADDING | Base64.NO_WRAP);
			ObjectInputStream ois = new ObjectInputStream(b64i);
			Object o = ois.readObject();
			return o;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

}
