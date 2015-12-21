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
	private ArrayList<MenuItem> mItems;
	private Context mContext;
	private Tileset mTileset;
	private SelectMode mHow;

	// ____________________________________________________________________________________
	public MenuItemAdapter(Activity context, int textViewResourceId, ArrayList<MenuItem> items, Tileset tileset, SelectMode how)
	{
		super(context, textViewResourceId, items);
		mItems = items;
		mContext = context;
		mTileset = tileset;
		mHow = how;
	}

	// ____________________________________________________________________________________
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View v = convertView;
		if(v == null)
		{
			LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.menu_item, null);
		}
		MenuItem item = mItems.get(position);
		if(item != null)
		{
			if(item.isHeader())
			{
				v.setBackgroundColor(Color.WHITE);
				v.setMinimumHeight(0);
			}
			else
			{
				v.setBackgroundColor(Color.TRANSPARENT);
				final float density = mContext.getResources().getDisplayMetrics().density;
				int minH = (int)(35 * density + 0.5f);
				v.setMinimumHeight(minH);
			}

			TextView tt = (TextView)v.findViewById(R.id.item_text);
			tt.setText(item.getText());
			
			TextView at = (TextView)v.findViewById(R.id.item_acc);
			at.setText(item.getAccText());
			
			TextView st = (TextView)v.findViewById(R.id.item_sub);
			st.setText(item.getSubText());
			if(item.hasSubText())
				st.setVisibility(View.VISIBLE);
			else
				st.setVisibility(View.GONE);

			TextView ic = (TextView)v.findViewById(R.id.item_count);
			if(item.getCount() > 0)
				ic.setText(Integer.toString(item.getCount()));
			else
				ic.setText("");
			
			ImageView tile = (ImageView)v.findViewById(R.id.item_tile);
			if(item.hasTile() && mTileset.hasTiles())
			{
				tile.setVisibility(View.VISIBLE);
				tile.setImageDrawable(new TileDrawable(mTileset, item.getTile()));
				v.findViewById(R.id.item_sub).setVisibility(View.VISIBLE);
			}
			else
			{
				if(item.isHeader())
					tile.setVisibility(View.GONE);
				else
					tile.setVisibility(View.INVISIBLE);
			}
			CheckBox cb = (CheckBox)v.findViewById(R.id.item_check);
			if(mHow == SelectMode.PickMany && !item.isHeader())
			{
				cb.setVisibility(View.VISIBLE);
				cb.setChecked(item.isSelected());
			}
			else
				cb.setVisibility(View.GONE);

			boolean enabled = item.isHeader() || item.isSelectable();
			tt.setEnabled(enabled);
			at.setEnabled(enabled);
			st.setEnabled(enabled);
			ic.setEnabled(enabled);
			tile.setEnabled(enabled);
			cb.setEnabled(enabled);

			item.setView(v);
		}
		return v;
	}
}
