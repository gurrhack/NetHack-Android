package com.tbd.NetHack;

import java.util.Set;

import android.app.Activity;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.tbd.NetHack.Input.Modifier;

public class AmountSelector
{
	public interface Listener
	{
		void onDismissCount(MenuItem item, int amount);
	}
	
	private View mRoot;
	private Tileset mTileset;
	private TextView mAmountText;
	private int mMax;
	private int mItemId;
	private MenuItem mItem;
	private Activity mContext;
	private Listener mListener;

	// ____________________________________________________________________________________
	public AmountSelector(Listener listener, Activity context, NetHackIO io, Tileset tileset, MenuItem item)
	{
		mItem = item;
		mListener = listener;
		mContext = context;
		mTileset = tileset;
		mMax = item.getMaxCount();
		mItemId = item.getId();

		mRoot = Util.inflate(mContext, R.layout.amount_selector, R.id.dlg_frame);
		ImageView tileView = (ImageView)mRoot.findViewById(R.id.amount_tile);
		final SeekBar seek =((SeekBar)mRoot.findViewById(R.id.amount_slider)); 
		if(item.getTile() != 0 && mTileset.hasTiles())
		{
			tileView.setVisibility(View.VISIBLE);
			tileView.setImageDrawable(new TileDrawable(mTileset, item.getTile()));
		}
		else
		{
			tileView.setVisibility(View.GONE);
		}
		
		((TextView)mRoot.findViewById(R.id.amount_title)).setText(" " + item.getName().toString());
		
		mAmountText = (TextView)mRoot.findViewById(R.id.amount);
		int pad = 9;
		while(pad <= mMax)
			pad = pad * 10 + 9;
		int w = (int)FloatMath.floor(mAmountText.getPaint().measureText(" " + Integer.toString(pad)));
		mAmountText.setWidth(w);
		
		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{			
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}
			
			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}
			
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				mAmountText.setText(Integer.toString(progress));
			}
		});
		seek.setMax(mMax);
		seek.setProgress(mMax);
		
		mRoot.findViewById(R.id.btn_0).setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if(mRoot != null)
				{
					dismiss(seek.getProgress());
				}
			}
		});
		mRoot.findViewById(R.id.btn_1).setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				dismiss(-1);
			}
		});
		
		seek.requestFocus();
		seek.requestFocusFromTouch();
	}

	// ____________________________________________________________________________________
	public int handleKeyDown(char ch, int nhKey, int keyCode, Set<Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(mRoot == null)
			return 0;
		
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_BACK:
			dismiss(-1);
		break;

		default:
			return 2;// let system handle
		}
		return 1;
	}

	public void dismiss(int amount)
	{
		if(mRoot != null)
		{
			mRoot.setVisibility(View.GONE);
			((ViewGroup)mRoot.getParent()).removeView(mRoot);
			mRoot = null;
			mListener.onDismissCount(mItem, amount);
		}
	}
}
