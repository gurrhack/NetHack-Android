package com.tbd.NetHack;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

class TileDrawable extends Drawable
{
	private Tileset m_tileset;
	private int m_iTile;

	// ____________________________________________________________________________________
	public TileDrawable(Tileset tileset, int iTile)
	{
		m_tileset = tileset;
		m_iTile = iTile;
	}

	// ____________________________________________________________________________________
	@Override
	public int getIntrinsicWidth()
	{
		return m_tileset.GetTileWidth();
	}

	// ____________________________________________________________________________________
	@Override
	public int getIntrinsicHeight()
	{
		return m_tileset.GetTileHeight();
	}

	// ____________________________________________________________________________________
	@Override
	public void draw(Canvas canvas)
	{
		int ofs = m_tileset.GetTileBitmapOffset(m_iTile);

		Rect src = new Rect();
		src.left = (ofs >> 16) & 0xffff;
		src.top = ofs & 0xffff;
		src.right = src.left + m_tileset.GetTileWidth();
		src.bottom = src.top + m_tileset.GetTileHeight();

		// Rect dst = new Rect(0, 0, src.width(), src.height());

		// ?android:attr/listPreferredItemHeight
		canvas.drawBitmap(m_tileset.GetBitmap(), src, getBounds(), null);
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
