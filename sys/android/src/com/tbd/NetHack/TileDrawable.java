package com.tbd.NetHack;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

class TileDrawable extends Drawable
{
	private Tileset mTileset;
	private int mTile;

	// ____________________________________________________________________________________
	public TileDrawable(Tileset tileset, int iTile)
	{
		mTileset = tileset;
		mTile = iTile;
	}

	// ____________________________________________________________________________________
	@Override
	public int getIntrinsicWidth()
	{
		return mTileset.getTileWidth();
	}

	// ____________________________________________________________________________________
	@Override
	public int getIntrinsicHeight()
	{
		return mTileset.getTileHeight();
	}

	// ____________________________________________________________________________________
	@Override
	public void draw(Canvas canvas)
	{
		mTileset.drawTile(canvas, mTile, getBounds(), null);
	}

	// ____________________________________________________________________________________
	@Override
	public int getOpacity()
	{
		return PixelFormat.OPAQUE;
	}

	// ____________________________________________________________________________________
	@Override
	public void setAlpha(int alpha)
	{
	}

	// ____________________________________________________________________________________
	@Override
	public void setColorFilter(ColorFilter cf)
	{
	}
}
