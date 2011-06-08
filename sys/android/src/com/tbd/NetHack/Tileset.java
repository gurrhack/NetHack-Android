package com.tbd.NetHack;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;

public class Tileset
{
	private OnChangedListener m_changedListener;
	private Bitmap m_bitmap;
	private Bitmap m_overlay;
	private int m_tileW;
	private int m_tileH;
	private String m_tilesetName;

	// ____________________________________________________________________________________
	public interface OnChangedListener
	{
		void OnChanged();
	};
		
	// ____________________________________________________________________________________
	public Tileset()
	{
		PreferencesUpdated();
	}
	
	// ____________________________________________________________________________________
	public void SetOnChangedListener(OnChangedListener listener)
	{
		m_changedListener = listener;
	}
	
	// ____________________________________________________________________________________
	public void PreferencesUpdated()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NetHack.get());
		String tilesetName = prefs.getString("tileset", "default_32");
		
		Log.print("Changing tileset");
		
		int id = NetHack.get().getResources().getIdentifier(tilesetName, "drawable", "com.tbd.NetHack");
		if(id == 0)
		{
			tilesetName = "default_32";
			id = NetHack.get().getResources().getIdentifier(tilesetName, "drawable", "com.tbd.NetHack");
		}
				
		if(m_tilesetName == tilesetName)
			return;
		m_tilesetName = tilesetName;
		
		if(id > 0)
		{
			BitmapDrawable bmpDrawable = (BitmapDrawable)NetHack.get().getResources().getDrawable(id);
			m_bitmap = bmpDrawable.getBitmap();
			bmpDrawable = (BitmapDrawable)NetHack.get().getResources().getDrawable(R.drawable.overlays);
			m_overlay = bmpDrawable.getBitmap();
			if(tilesetName.endsWith("32"))
			{
				m_tileW = 32;
				m_tileH = 32;
			}
			else
			{
				m_tileW = 12;
				m_tileH = 20;
			}
		}
		else
		{
			m_bitmap = null;
			m_tileW = 0;
			m_tileH = 0;
		}

		if(m_changedListener != null)
			m_changedListener.OnChanged();
	}

	// ____________________________________________________________________________________
	public int GetTileBitmapOffset(int iTile)
	{
		if(m_bitmap == null)
			return 0;
		
		int nCols = m_bitmap.getWidth() / m_tileW;
		int iRow = iTile / nCols;
		int iCol = iTile - iRow * nCols;

		int x = iCol * m_tileW;
		int y = iRow * m_tileH;
		
		return (x << 16) | y;
	}
	
	// ____________________________________________________________________________________
	public Bitmap GetTile(int iTile)
	{
		if(m_bitmap == null)
			return null;
		
		int ofs = GetTileBitmapOffset(iTile);
		
		int x = ofs >> 16;
		int y = ofs & 0xffff;		
		
		return Bitmap.createBitmap(m_bitmap, x, y, m_tileW, m_tileH);
	}

	// ____________________________________________________________________________________
	public int GetTileWidth()
	{
		return m_tileW;
	}
	
	// ____________________________________________________________________________________
	public int GetTileHeight()
	{
		return m_tileH;
	}
	
	// ____________________________________________________________________________________
	public Bitmap GetBitmap()
	{
		return m_bitmap;
	}

	// ____________________________________________________________________________________
	public Rect getOverlayRect(short overlay)
	{
		return new Rect(0, 0, 32, 32);
	}

	// ____________________________________________________________________________________
	public Bitmap GetTileOverlay(short overlay)
	{
		if(overlay == 8)
			return m_overlay;
		return null;
	}
}
