package com.tbd.NetHack;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextPaint;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Tileset
{
	public final int OVERLAY_DETECT = 0x04;
	public final int OVERLAY_PET = 0x08;
	public final int OVERLAY_OBJPILE = 0x40;

	private static final String LOCAL_TILESET_NAME = "custom_tileset";

	private Bitmap mBitmap;
	private Bitmap mOverlay;
	private int mTileW;
	private int mTileH;
	private String mTilesetName = "";
	private int mnCols;
	private Context mContext;
	private boolean mFallbackRenderer;
	private final Map<Integer, Bitmap> mTileCache = new HashMap<Integer, Bitmap>();

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

		mFallbackRenderer = prefs.getBoolean("fallbackRenderer", false);

		String tilesetName = prefs.getString("tileset", "TTY");

		boolean TTY = tilesetName.equals("TTY");
		int tileW = prefs.getInt("tileW", 32);
		int tileH = prefs.getInt("tileH", 32);

		if(mTilesetName.equals(tilesetName) && tileW == mTileW && tileH == mTileH)
			return;
		mTilesetName = tilesetName;

		if(!TTY && (tileW <= 0 || tileH <= 0))
		{
			Toast.makeText(mContext, "Invalid tile dimensions (" + mTileW + "x" + mTileH + ")", Toast.LENGTH_LONG).show();
			TTY = true;

		}

		if(!TTY)
		{
			mTileW = tileW;
			mTileH = tileH;

			if(prefs.getBoolean("customTiles", false))
				loadCustomTileset(tilesetName);
			else
				loadFromResources(tilesetName, r);

			BitmapDrawable bmpDrawable = (BitmapDrawable)r.getDrawable(R.drawable.overlays);
			mOverlay = bmpDrawable.getBitmap();

			if(mBitmap == null || mOverlay == null)
				TTY = true;
			else
				mnCols = mBitmap.getWidth() / mTileW;

			if(mnCols <= 0)
			{
				Toast.makeText(mContext, "Invalid tileset settings '" + tilesetName + "' (" + mTileW + "x" + mTileH + ")", Toast.LENGTH_LONG).show();
				TTY = true;
			}
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
		mTileCache.clear();
		mBitmap = null;
	}

	// ____________________________________________________________________________________
	private void loadCustomTileset(String tilesetName) {
		clearBitmap();
		mBitmap = tryLoadBitmap(getLocalTilesetFile().getPath(), false);
		// Fallback if coming from an old version
		if(mBitmap == null)
			mBitmap = tryLoadBitmap(tilesetName, true);
		if(mBitmap == null)
			Toast.makeText(mContext, "Error loading custom tileset", Toast.LENGTH_LONG).show();
	}

	// ____________________________________________________________________________________
	private Bitmap tryLoadBitmap(String path, boolean logFailure)
	{
		Bitmap bitmap = null;
		try
		{
			bitmap = BitmapFactory.decodeFile(path);
		}
		catch(Exception e)
		{
			if(logFailure)
				Toast.makeText(mContext, "Error loading custom tileset: " + e.toString(), Toast.LENGTH_LONG).show();
		}
		catch(OutOfMemoryError e)
		{
			if(logFailure)
				Toast.makeText(mContext, "Error loading custom tileset: Out of memory", Toast.LENGTH_LONG).show();
		}
		return bitmap;
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
	private int getTileBitmapOffset(int iTile)
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
	private Bitmap getTile(int iTile)
	{
		if(mBitmap == null)
			return null;
		Bitmap bitmap = mTileCache.get(iTile);
		if(bitmap == null)
		{
			int ofs = getTileBitmapOffset(iTile);

			int x = ofs >> 16;
			int y = ofs & 0xffff;

			try
			{
				bitmap = Bitmap.createBitmap(mBitmap, x, y, mTileW, mTileH);
			}
			catch(Exception e)
			{
				bitmap = Bitmap.createBitmap(mBitmap, 0, 0, 1, 1);
			}
			mTileCache.put(iTile, bitmap);
		}
		return bitmap;
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
	public Rect getOverlayRect(short overlay)
	{
		return new Rect(0, 0, 32, 32);
	}

	// ____________________________________________________________________________________
	public Bitmap getTileOverlay(short overlay)
	{
		if((overlay & OVERLAY_PET) != 0)
			return mOverlay;
		return null;
	}

	// ____________________________________________________________________________________
	public boolean hasTiles()
	{
		return mBitmap != null;
	}

	// ____________________________________________________________________________________
	public void drawTile(Canvas canvas, int glyph, Rect dst, TextPaint paint)
	{
		if(mBitmap == null)
			return;
		Rect src = new Rect();
		if(mFallbackRenderer)
		{
			Bitmap bitmap = getTile(glyph);
			src.left = 0;
			src.top = 0;
			src.right = getTileWidth();
			src.bottom = getTileHeight();
			canvas.drawBitmap(bitmap, src, dst, paint);
		}
		else
		{
			int ofs = getTileBitmapOffset(glyph);
			src.left = (ofs >> 16) & 0xffff;
			src.top = ofs & 0xffff;
			src.right = src.left + getTileWidth();
			src.bottom = src.top + getTileHeight();
			canvas.drawBitmap(mBitmap, src, dst, paint);
		}
	}

	// ____________________________________________________________________________________
	public static File getLocalTilesetFile() {
		File dir = NetHack.getApplicationDir();
		File file = new File(dir, LOCAL_TILESET_NAME);
		return file;
	}
}
