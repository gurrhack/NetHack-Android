package com.tbd.NetHack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class TilesetPreference extends Preference implements PreferenceManager.OnActivityResultListener
{
	private static final int GET_IMAGE_REQUEST = 342;
	private final String TTY = "TTY";

	private List<String> mEntries;
	private List<String> mEntryValues;
	private TextView mTilesetPath;
	private EditText mTileW;
	private EditText mTileH;
	private ViewGroup mTilesetUI;
	private LinearLayout mRoot;
	private Settings mSettings;
	private String mCustomTilesetPath;
	private Bitmap mCustomTileset;
	private ImageButton mBrowse;
	private boolean mTileWFocus;
	private boolean mTileHFocus;

	public TilesetPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		mEntries = Arrays.asList(context.getResources().getStringArray(R.array.tileNames));
		mEntryValues = Arrays.asList(context.getResources().getStringArray(R.array.tileValues));
	}

	@Override
	protected View onCreateView(ViewGroup parent)
	{
		mRoot = (LinearLayout)super.onCreateView(parent);

		createChoices();

		mTilesetUI = (ViewGroup)mRoot.findViewById(R.id.customTilesUI);
		mTileW = (EditText)mRoot.findViewById(R.id.tileW);
		mTileH = (EditText)mRoot.findViewById(R.id.tileH);
		((RadioButton)mRoot.findViewById(R.id.custom_tiles)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				setCustomUIEnabled(isChecked);
			}
		});
		mBrowse = (ImageButton)mRoot.findViewById(R.id.browse);
		mBrowse.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				choseCustomTilesetImage();
			}
		});
		mTilesetPath = (TextView)mRoot.findViewById(R.id.image_path);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Workaround for weird focus problem
		// When the keyboard is opened because an input field receive focus the entire view is recreated,
		// which makes the field lose focus again
		mTileW.setSelectAllOnFocus(true);
		mTileH.setSelectAllOnFocus(true);

		TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if(actionId == EditorInfo.IME_ACTION_DONE)
				{
					mTileWFocus = false;
					mTileHFocus = false;
				}
				return false;
			}
		};
		mTileW.setOnEditorActionListener(onEditorActionListener);
		mTileH.setOnEditorActionListener(onEditorActionListener);

		mTileW.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				mTileWFocus = hasFocus;
			}
		});

		mTileH.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				mTileHFocus = hasFocus;
			}
		});
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		return mRoot;
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);

		SharedPreferences prefs = getSharedPreferences();
		String currentValue = prefs.getString("tileset", TTY);

		int i = mEntryValues.indexOf(currentValue);
		if(i < 0)
			i = mEntryValues.indexOf(TTY);
		if(!prefs.getBoolean("customTiles", false))
			((RadioButton)mRoot.getChildAt(i)).setChecked(true);

		mTilesetPath.setText(prefs.getString("customTileset", ""));
		mTileW.setText(Integer.toString(prefs.getInt("customTileW", 32)));
		mTileH.setText(Integer.toString(prefs.getInt("customTileH", 32)));
		updateTileIcon();

		mTileW.addTextChangedListener(updateCustom);
		mTileH.addTextChangedListener(updateCustom);
		mTilesetPath.addTextChangedListener(updateCustom);

		if(mTileWFocus)
			mTileW.requestFocus();
		else if(mTileHFocus)
			mTileH.requestFocus();

		mTileWFocus = false;
		mTileHFocus = false;
	}

	private void choseCustomTilesetImage()
	{
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		mSettings.startActivityForResult(Intent.createChooser(intent, "Select Tileset Image"), GET_IMAGE_REQUEST);
	}

	@Override
	public boolean onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == Activity.RESULT_OK && requestCode == GET_IMAGE_REQUEST)
		{
			if(createCustomTilesetLocalCopy(data.getData()))
			{
			String path = queryPath(data.getData());
			mTilesetPath.setText(path);
		}
		}
		return requestCode == GET_IMAGE_REQUEST;
	}

	private boolean createCustomTilesetLocalCopy(Uri from)
	{
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			inputStream = getContext().getContentResolver().openInputStream(from);
			File file = Tileset.getLocalTilesetFile();
			outputStream = new FileOutputStream(file, false);
			Util.copy(inputStream, outputStream);
			return true;
		} catch(Exception e) {
			Toast.makeText(getContext(), "Error loading tileset", Toast.LENGTH_LONG).show();
		} finally {
			if(inputStream != null)
				try { inputStream.close(); } catch(IOException e) {}
			if(outputStream != null)
				try { inputStream.close(); } catch(IOException e) {}
		}
		return false;
	}

	public String queryPath(Uri uri)
	{
		String[] projection = {MediaStore.Images.Media.DATA};
		Cursor cursor = mSettings.managedQuery(uri, projection, null, null, null);
		if(cursor != null)
		{
			int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
			if(column_index >= 0)
			{
				cursor.moveToFirst();
				String path = cursor.getString(column_index);
				if(path != null && path.length() > 0)
					return path;
			}
		}

		return uri.getPath();
	}

	private void setCustomUIEnabled(boolean enabled)
	{
		if(enabled)
			persistCustom();
		setTreeEnabled(mTilesetUI, enabled);
		mTilesetPath.setEnabled(false);

		if(!enabled)
		{
			mTileW.clearFocus();
			mTileH.clearFocus();
			mTileWFocus = false;
			mTileHFocus = false;
		}
	}

	private void setTreeEnabled(View view, boolean enabled)
	{
		view.setEnabled(enabled);
		if(view instanceof ViewGroup)
		{
			ViewGroup group = (ViewGroup)view;
			for(int i = 0; i < group.getChildCount(); i++)
				setTreeEnabled(group.getChildAt(i), enabled);
		}
	}

	private void createChoices()
	{
		for(int i = mEntries.size() - 1; i >= 0; i--)
		{
			RadioButton button = new RadioButton(getContext());
			button.setText(mEntries.get(i));
			button.setTag(mEntryValues.get(i));
			button.setOnCheckedChangeListener(tilesetChecked);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			mRoot.addView(button, 0, params);
		}
	}

	private void persistTileset(String id, int tileW, int tileH, boolean custom)
	{
		SharedPreferences.Editor editor = getEditor();
		if(editor != null)
		{
			editor.putString("tileset", id);
			editor.putInt("tileW", tileW);
			editor.putInt("tileH", tileH);
			editor.putBoolean("customTiles", custom);
			if(custom)
			{
				editor.putString("customTileset", id);
				editor.putInt("customTileW", tileW);
				editor.putInt("customTileH", tileH);
			}
			if(shouldCommit())
			{
				editor.commit();
			}
		}
	}

	private CompoundButton.OnCheckedChangeListener tilesetChecked = new CompoundButton.OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			if(isChecked)
			{
				String id = (String)buttonView.getTag();
				int tileW;
				int tileH;
				if(TTY.equals(id)) {
					tileW = 0;
					tileH = 0;
				} else {
					int iw = id.lastIndexOf('_') + 1;
					int ih = id.lastIndexOf('x');
					if(ih < iw)
						ih = id.length();
					tileW = Integer.parseInt(id.substring(iw, ih));
					tileH = tileW;
					if(ih != id.length())
						tileH = Integer.parseInt(id.substring(ih + 1, id.length()));
				}
				persistTileset(id, tileW, tileH, false);
			}
		}
	};

	private TextWatcher updateCustom = new TextWatcher()
	{
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{

		}

		@Override
		public void afterTextChanged(Editable s)
		{
			persistCustom();
		}
	};

	private void persistCustom()
	{
		try
		{
			String id = mTilesetPath.getText().toString();
			int tileW = Integer.parseInt(mTileW.getText().toString());
			int tileH = Integer.parseInt(mTileH.getText().toString());
			persistTileset(id, tileW, tileH, true);
			updateTileIcon();
		}
		catch(NumberFormatException e)
		{

		}
	}

	private void updateTileIcon()
	{
		String newPath = mTilesetPath.getText().toString();
		if(!newPath.equals(mCustomTilesetPath))
		{
			mCustomTilesetPath = newPath;
			if(newPath.length() > 0)
			{
				try
				{
					File localFile = Tileset.getLocalTilesetFile();
					if(!localFile.exists())
					{
						// User deleted local copy, or coming from an old version
						createCustomTilesetLocalCopy(Uri.fromFile(new File(newPath)));
					}

					mCustomTileset = BitmapFactory.decodeFile(Tileset.getLocalTilesetFile().getPath());
					if(mCustomTileset == null)
						Toast.makeText(getContext(), "Error loading: " + newPath, Toast.LENGTH_LONG).show();

				}
				catch(Exception e)
				{
					Toast.makeText(getContext(), "Error loading " + newPath + ": " + e.toString(), Toast.LENGTH_LONG).show();
					mCustomTileset = null;
				}
				catch(OutOfMemoryError e)
				{
					Toast.makeText(getContext(), "Error loading " + newPath + ": Out of memory", Toast.LENGTH_LONG).show();
					mCustomTileset = null;
				}
			}
			else
			{
				mCustomTileset = null;
			}
		}

		Bitmap tile = getTile();
		if(tile == null)
		{
			Drawable drawable = getContext().getResources().getDrawable(android.R.drawable.ic_menu_gallery);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			mBrowse.setImageDrawable(drawable);
		}
		else
		{
			BitmapDrawable drawable = new BitmapDrawable(getContext().getResources(), tile);
			drawable.setBounds(0, 0, tile.getWidth(), tile.getHeight());
			mBrowse.setImageDrawable(drawable);
		}
	}

	private Bitmap getTile()
	{
		if(mCustomTileset == null)
			return null;

		try
		{
			int tileW = Integer.parseInt(mTileW.getText().toString());
			int tileH = Integer.parseInt(mTileH.getText().toString());

			return Bitmap.createBitmap(mCustomTileset, 0, 0, tileW, tileH);
		}
		catch(IllegalArgumentException e)
		{
		}
		return null;
	}

	public void setActivity(Settings settings)
	{
		mSettings = settings;
	}
}
