package com.tbd.forkfront;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import com.tbd.forkfront.Log;

public class EditFilePreference extends Preference implements DialogInterface.OnDismissListener
{
	private EditText mEditText;
	private Dialog mDialog;
	private boolean mSave;

	// ____________________________________________________________________________________
	public EditFilePreference(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setSummary("Manually specify options in " + getContext().getResources().getString(R.string.defaultsFile));
	}

	public EditFilePreference(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	private void showDialog()
	{
		Context context = getContext();

		mSave = false;
		mDialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
		mDialog.setContentView(R.layout.edit_file);
		mEditText = (EditText)mDialog.findViewById(R.id.edittext);
		((Button)mDialog.findViewById(R.id.save)).setOnClickListener(onButton);
		((Button)mDialog.findViewById(R.id.discard)).setOnClickListener(onButton);

		mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		
		mDialog.setOnDismissListener(this);
		mDialog.show();
	}

	@Override
	protected void onClick()
	{
		showDialog();
		loadFile();
	}

	private OnClickListener onButton = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			if(mDialog != null && mDialog.isShowing())
			{
				mSave = v == mDialog.findViewById(R.id.save);
				mDialog.dismiss();
			}
		}
	};

	public void onDismiss(DialogInterface dialog)
	{
		mDialog = null;
		if(mSave)
		{
			String value = mEditText.getText().toString();
			if(callChangeListener(value))
				saveFile(value);
		}
	}

	@Override
	protected Parcelable onSaveInstanceState()
	{
		final Parcelable superState = super.onSaveInstanceState();
		if(mDialog == null || !mDialog.isShowing())
		{
			return superState;
		}

		final SavedState myState = new SavedState(superState);
		myState.isDialogShowing = mDialog != null && mDialog.isShowing();
		if(myState.isDialogShowing)
			myState.text = mEditText.getText().toString();
		else
			myState.text = "";

		return myState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state)
	{
		if(state == null || !state.getClass().equals(SavedState.class))
		{
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState myState = (SavedState)state;
		super.onRestoreInstanceState(myState.getSuperState());
		if(myState.isDialogShowing)
			showDialog();
		mEditText.setText(myState.text);
	}

	private static class SavedState extends BaseSavedState
	{
		boolean isDialogShowing;
		String text;

		public SavedState(Parcel source)
		{
			super(source);
			isDialogShowing = source.readInt() == 1;
			text = source.readString();
		}

		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			super.writeToParcel(dest, flags);
			dest.writeInt(isDialogShowing ? 1 : 0);
			dest.writeString(text);
		}

		public SavedState(Parcelable superState)
		{
			super(superState);
		}
	}

	private void loadFile()
	{
		File dir = new File(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("datadir", ""));
		File file = new File(dir, getContext().getResources().getString(R.string.defaultsFile));

		try
		{
			FileInputStream input = new FileInputStream(file);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] data = new byte[1024];
			int n;
			while((n = input.read(data)) != -1)
				output.write(data, 0, n);
			input.close();
			data = output.toByteArray();
			mEditText.setText(new String(data));
		}
		catch(FileNotFoundException e)
		{
		}
		catch(IOException e)
		{
		}
	}

	private void saveFile(String text)
	{
		File dir = new File(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("datadir", ""));
		File file = new File(dir, getContext().getResources().getString(R.string.defaultsFile));

		try
		{
			FileOutputStream output = new FileOutputStream(file, false);
			byte[] data = text.getBytes();
			output.write(data);
			output.close();
		}
		catch(FileNotFoundException e)
		{
		}
		catch(IOException e)
		{
		}
	}

}
