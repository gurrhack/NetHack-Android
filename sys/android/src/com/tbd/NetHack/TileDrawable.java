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
		int ofs = mTileset.getTileBitmapOffset(mTile);

		Rect src = new Rect();
		src.left = (ofs >> 16) & 0xffff;
		src.top = ofs & 0xffff;
		src.right = src.left + mTileset.getTileWidth();
		src.bottom = src.top + mTileset.getTileHeight();

		// This check fixes a crash if ASCII mode is enabled while a menu is opened
		// The menu doesn't look right after this, but it's better than a crash
		Bitmap bmp = mTileset.getBitmap();
		if(bmp != null)
			canvas.drawBitmap(bmp, src, getBounds(), null);
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
