package com.tbd.NetHack;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.widget.Toast;

public class Tileset
{
	private Bitmap mBitmap;
	private Bitmap mOverlay;
	private int mTileW;
	private int mTileH;
	private String mTilesetName = "";
	private int mnCols;
	private Context mContext;

	// ____________________________________________________________________________________
	public Tileset(NetHack context)
	{
		mContext = context;
	}

	// ____________________________________________________________________________________
	public void setContext(Context context)
	{
		mContext = context;
	}

	// ____________________________________________________________________________________
	public void updateTileset(SharedPreferences prefs, Resources r)
	{
		String tilesetName = prefs.getString("tileset", "TTY");

		boolean TTY = tilesetName.equals("TTY");
		int tileW = prefs.getInt("tileW", 32);
		int tileH = prefs.getInt("tileH", 32);

		if(mTilesetName.equals(tilesetName) && tileW == mTileW && tileH == mTileH)
			return;
		mTilesetName = tilesetName;

		if(!TTY)
		{
			mTileW = tileW;
			mTileH = tileH;

			if(prefs.getBoolean("customTiles", false))
				loadFromFile(tilesetName, prefs);
			else
				loadFromResources(tilesetName, r);

			BitmapDrawable bmpDrawable = (BitmapDrawable)r.getDrawable(R.drawable.overlays);
			mOverlay = bmpDrawable.getBitmap();

			if(mBitmap == null || mOverlay == null)
				TTY = true;
			else
				mnCols = mBitmap.getWidth() / mTileW;
		}

		if(TTY)
		{
			clearBitmap();
			mTileW = 0;
			mTileH = 0;
			mnCols = 0;
		}
	}

	// ____________________________________________________________________________________
	private void clearBitmap()
	{
		if(mBitmap != null)
			mBitmap.recycle();
		mBitmap = null;
	}

	// ____________________________________________________________________________________
	private void loadFromFile(String tilesetName, SharedPreferences prefs)
	{
		clearBitmap();
		try
		{
			mBitmap = BitmapFactory.decodeFile(tilesetName);
			if(mBitmap == null)
				Toast.makeText(mContext, "Error loading tileset " + tilesetName, Toast.LENGTH_LONG).show();
		}
		catch(Exception e)
		{
			Toast.makeText(mContext, "Error loading " + tilesetName + ": " + e.toString(), Toast.LENGTH_LONG).show();
		}
		catch(OutOfMemoryError e)
		{
			Toast.makeText(mContext, "Error loading " + tilesetName + ": Out of memory", Toast.LENGTH_LONG).show();
		}
	}

	// ____________________________________________________________________________________
	private void loadFromResources(String tilesetName, Resources r)
	{
		int id = r.getIdentifier(tilesetName, "drawable", "com.tbd.NetHack");

		clearBitmap();
		if(id > 0)
		{
			BitmapDrawable bmpDrawable = (BitmapDrawable) r.getDrawable(id);
			mBitmap = bmpDrawable.getBitmap();
		}
	}

	// ____________________________________________________________________________________
	public int getTileBitmapOffset(int iTile)
	{
		if(mBitmap == null)
			return 0;
		
		int iRow = iTile / mnCols;
		int iCol = iTile - iRow * mnCols;

		int x = iCol * mTileW;
		int y = iRow * mTileH;
		
		return (x << 16) | y;
	}
	
	// ____________________________________________________________________________________
	public int getTileWidth()
	{
		return mTileW;
	}
	
	// ____________________________________________________________________________________
	public int getTileHeight()
	{
		return mTileH;
	}
	
	// ____________________________________________________________________________________
	public Bitmap getBitmap()
	{
		return mBitmap;
	}

	// ____________________________________________________________________________________
	public Rect getOverlayRect(short overlay)
	{
		return new Rect(0, 0, 32, 32);
	}

	// ____________________________________________________________________________________
	public Bitmap getTileOverlay(short overlay)
	{
		if(overlay == 8)
			return mOverlay;
		return null;
	}

	// ____________________________________________________________________________________
	public boolean hasTiles()
	{
		return mBitmap != null;
	}
}
