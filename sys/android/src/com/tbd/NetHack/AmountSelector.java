package com.tbd.NetHack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class AmountSelector
{
	private AlertDialog m_dialog;
	private Tileset m_tileset;
	private String m_text;
	private TextView m_textView;
	private int m_amount;
	private int m_nMax;

	// ____________________________________________________________________________________
	public AmountSelector(NetHackIO io, Tileset tileset, String text, int nMax, int tile)
	{
		m_tileset = tileset;
		m_text = text;
		m_nMax = nMax;
		m_amount = -1;

		View v = Util.Inflate(R.layout.amount_selector);
		ImageView tileView = (ImageView)v.findViewById(R.id.amount_tile);
		if(tile != 0)
		{
			tileView.setVisibility(View.VISIBLE);
			tileView.setImageDrawable(new TileDrawable(m_tileset, tile));
		}
		else
		{
			tileView.setVisibility(View.GONE);
		}
		m_textView = (TextView)v.findViewById(R.id.amount_title);
		final SeekBar seek =((SeekBar)v.findViewById(R.id.amount_slider)); 
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
				m_textView.setText(String.format("%d %s", progress, m_text));
			}
		});
		seek.setMax(m_nMax);
		seek.setProgress(m_nMax);

		AlertDialog.Builder builder = new AlertDialog.Builder(NetHack.get());
		builder.setTitle("Select amount");
		builder.setView(v);
		builder.setPositiveButton("Ok", new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				if(m_dialog.isShowing())
				{
					m_amount = seek.getProgress();
					m_dialog.dismiss();
				}
			}
		});
		builder.setNegativeButton("Back", null);
		builder.setCancelable(true);
		m_dialog = builder.create();
		m_dialog.show();
	}

	// ____________________________________________________________________________________
	public void setOnDismissListener(DialogInterface.OnDismissListener listener)
	{
		m_dialog.setOnDismissListener(listener);
	}
	
	// ____________________________________________________________________________________
	public int GetAmount()
	{
		return m_amount;
	}
}
