package com.tbd.NetHack;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.tbd.NetHack.NHW_Menu.SelectMode;

public class MenuItemAdapter extends ArrayAdapter<MenuItem>
{
	private ArrayList<MenuItem> m_items;
	private Context m_context;
	Tileset m_tileset;
	SelectMode m_how;

	// ____________________________________________________________________________________
	public MenuItemAdapter(Activity context, int textViewResourceId, ArrayList<MenuItem> items, Tileset tileset, SelectMode how)
	{
		super(context, textViewResourceId, items);
		m_items = items;
		m_context = context;
		m_tileset = tileset;
		m_how = how;
	}

	// ____________________________________________________________________________________
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View v = convertView;
		if(v == null)
		{
			LayoutInflater vi = (LayoutInflater)m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.menu_item, null);
		}
		MenuItem item = m_items.get(position);
		if(item != null)
		{
			if(item.IsHeader())
			{
				v.setBackgroundColor(Color.WHITE);
				v.setMinimumHeight(0);
			}
			else
			{
				v.setBackgroundColor(Color.TRANSPARENT);
				final float density = m_context.getResources().getDisplayMetrics().density;
				int minH = (int)(35 * density + 0.5f);
				v.setMinimumHeight(minH);
			}

			TextView tt = (TextView)v.findViewById(R.id.item_text);
			tt.setText(item.GetText());
			tt.setVisibility(View.VISIBLE);
			
			TextView at = (TextView)v.findViewById(R.id.item_acc);
			at.setText(item.GetAccText());
			
			TextView st = (TextView)v.findViewById(R.id.item_sub);
			st.setText(item.GetSubText());
			
			if(item.HasSubText())
				st.setVisibility(View.VISIBLE);
			else
				st.setVisibility(View.GONE);

			ImageView tile = (ImageView)v.findViewById(R.id.item_tile);
			if(item.HasTile())
			{
				tile.setVisibility(View.VISIBLE);
				tile.setImageDrawable(new TileDrawable(m_tileset, item.GetTile()));
				v.findViewById(R.id.item_sub).setVisibility(View.VISIBLE);
			}
			else
			{
				if(item.IsHeader())
					tile.setVisibility(View.GONE);
				else
				{
					tile.setVisibility(View.INVISIBLE);
				}
			}
			CheckBox cb = (CheckBox)v.findViewById(R.id.item_check);
			if(m_how == SelectMode.PickMany && !item.IsHeader())
			{
				cb.setVisibility(View.VISIBLE);
				cb.setChecked(item.IsSelected());
			}
			else
				cb.setVisibility(View.GONE);
		}
		return v;
	}
}
