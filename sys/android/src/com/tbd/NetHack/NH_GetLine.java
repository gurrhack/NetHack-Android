package com.tbd.NetHack;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.EditText;

public class NH_GetLine
{
	private UI mUI;
	private NetHackIO mIO;
	private String mTitle;
	private int mMaxChars;

	// ____________________________________________________________________________________
	public NH_GetLine(NetHackIO io)
	{
		mIO = io;
	}

	// ____________________________________________________________________________________
	public void show(Context context, final String title, final int nMaxChars)
	{
		mTitle = title;
		mMaxChars = nMaxChars;
		mUI = new UI(context);
	}

	// ____________________________________________________________________________________
	public void setContext(Context context)
	{
		if(mUI != null)
			mUI = new UI(context);
	}

	// ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI
	{
		private Context mContext;
		private EditText mInput;
		private NH_Dialog mDialog;

		// ____________________________________________________________________________________
		public UI(Context context)
		{
			mContext = context;

			ViewGroup v = (ViewGroup)Util.inflate(context, R.layout.dialog_getline);
			mInput = (EditText)v.findViewById(R.id.input);
			mInput.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(mMaxChars) });
			mInput.setOnKeyListener(new OnKeyListener()
			{
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					if(event.getAction() != KeyEvent.ACTION_DOWN)
						return false;

					if(keyCode == KeyEvent.KEYCODE_ENTER)
						ok();
					else if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == 111/*KeyEvent.KEYCODE_ESCAPE*/)
						cancel();
					return false;
				}
			});

			mDialog = new NH_Dialog(context);
			mDialog.setTitle(mTitle);
			mDialog.setView(v);
			mDialog.setCancelable(true);
			mDialog.setPositiveButton("Ok", new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					ok();
				}
			});
			mDialog.setNegativeButton("Cancel", new OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					cancel();
				}
			});
			mDialog.setOnCancelListener(new OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					cancel();
				}
			});
			
			mDialog.show();

			Util.showKeyboard(context, mInput);
		}

		// ____________________________________________________________________________________
		public void dismiss()
		{
			Util.hideKeyboard(mContext, mInput);
			if(mDialog != null)
			{
				mDialog.dismiss();
				mDialog = null;
			}
			mUI = null;
		}

		// ____________________________________________________________________________________
		private void ok()
		{
			if(mDialog != null)
			{
				String text = mInput.getText().toString().trim();
				mIO.sendLineCmd(text);
				dismiss();
			}
		}

		// ____________________________________________________________________________________
		private void cancel()
		{
			if(mDialog != null)
			{
				mIO.sendLineCmd("\033");
				dismiss();
			}
		}
	}
}
