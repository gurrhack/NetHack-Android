package com.tbd.forkfront;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class MenuItemAdapter extends ArrayAdapter<MenuItem>
{
	private ArrayList<MenuItem> mItems;
	private Context mContext;
	private Tileset mTileset;
	private MenuSelectMode mHow;
	private boolean mMenuHasTiles;
	private boolean mIsMonospaceMode;

	// ____________________________________________________________________________________
	public MenuItemAdapter(Activity context, int textViewResourceId, ArrayList<MenuItem> items, Tileset tileset, MenuSelectMode how)
	{
		super(context, textViewResourceId, items);
		mItems = items;
		mContext = context;
		mTileset = tileset;
		mHow = how;
		mMenuHasTiles = menuHasTiles(items);
		updateMonospaceFlag();
	}

	private void updateMonospaceFlag() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mIsMonospaceMode = prefs.getBoolean("monospace", false);
	}

	// ____________________________________________________________________________________
	private static boolean menuHasTiles(ArrayList<MenuItem> items) {
		for(MenuItem item : items) {
			if(item.hasTile())
				return true;
		}
		return false;
	}

	// ____________________________________________________________________________________
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final float density = mContext.getResources().getDisplayMetrics().density;
		final int clickableItemMinH = (int)(35 * density + 0.5f);
		final int clickableHeaderMinH = (int)(25 * density + 0.5f);

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
				if(mHow == MenuSelectMode.PickMany)
					v.setMinimumHeight(clickableHeaderMinH);
				else
					v.setMinimumHeight(0);
			}
			else
			{
				v.setBackgroundColor(Color.TRANSPARENT);
				if(mHow == MenuSelectMode.PickNone)
					v.setMinimumHeight(0);
				else
					v.setMinimumHeight(clickableItemMinH);
			}

			TextView tt = (TextView)v.findViewById(R.id.item_text);
			tt.setText(item.getText());

			TextView at = (TextView)v.findViewById(R.id.item_acc);
			if(item.isHeader()) {
				at.setVisibility(View.GONE);
			} else {
				at.setVisibility(View.VISIBLE);
				at.setText(item.getAccText());
				if(mIsMonospaceMode) {
					at.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
					at.requestLayout();
					at.invalidate();
				} else {
					int fixedW = (int)((mMenuHasTiles && mTileset.hasTiles() ? 20 : 40) * density);
					at.setWidth(fixedW);
				}
			}

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
			if(mMenuHasTiles && mTileset.hasTiles() && !item.isHeader())
			{
				if(item.hasTile()) {
					tile.setVisibility(View.VISIBLE);
					tile.setImageDrawable(new TileDrawable(mTileset, item.getTile()));
					v.findViewById(R.id.item_sub).setVisibility(View.VISIBLE);
				} else {
					// Don't show it but reserve its space for alignment
					tile.setVisibility(View.INVISIBLE);
				}
			} else {
				tile.setVisibility(View.GONE);
			}

			CheckBox cb = (CheckBox)v.findViewById(R.id.item_check);
			if(mHow == MenuSelectMode.PickMany && !item.isHeader())
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

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		updateMonospaceFlag();
	}
}
