package com.tbd.NetHack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.widget.TextView;

public class UpdateAssets extends AsyncTask<Void, Void, Void>
{
	private AssetManager mAM;
	private SharedPreferences mPrefs;
	//private ProgressDialog m_initDialog;
	private boolean mIsInitiating;
	private ProgressDialog mProgress;
	private File mDstPath;
	private String mError;
	private boolean mWipeUserdata;
	private long mRequiredSpace;
	private long mTotalRead;
	private NetHack mNetHack;
	volatile Boolean mWipeConfirmed = null;

	// ____________________________________________________________________________________
	public UpdateAssets(NetHack nethack)
	{
		mNetHack = nethack;
		mPrefs = mNetHack.getPreferences(Activity.MODE_PRIVATE);
		mAM = mNetHack.getResources().getAssets();
		//m_initDialog = ProgressDialog.show(m_nethack, "", "Initiating. Please wait...", true);
		mIsInitiating = true;
		mTotalRead = 0;
		mRequiredSpace = 0;
	}

	// ____________________________________________________________________________________
	@Override
	protected void onPostExecute(Void unused)
	{
		//if(m_initDialog != null)
		//	m_initDialog.dismiss();
		if(mProgress != null)
			mProgress.dismiss();
		if(mDstPath == null)
		{
			if(mWipeConfirmed == null || mWipeConfirmed == true)
				showError();
			else
				mNetHack.finish();
		}
		else
		{
			Log.print("Starting on: " + mDstPath.getAbsolutePath());			
			mNetHack.start(mDstPath);
		}
	}

	// ____________________________________________________________________________________
	@Override
	protected Void doInBackground(Void... params)
	{
		mDstPath = load();
		return null;
	}

	// ____________________________________________________________________________________
	@Override
    protected void onProgressUpdate(Void... progress)
	{
		if(mTotalRead > 0 && mIsInitiating)// m_initDialog != null)
		{
			//m_initDialog.dismiss();
			//m_initDialog = null;
			mIsInitiating = false;
			
			mProgress = new ProgressDialog(mNetHack);
			mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgress.setMax((int)mRequiredSpace);
			mProgress.setMessage("Preparing content...");
			mProgress.setCancelable(false);
			mProgress.show();
		}
		mProgress.setProgress((int)mTotalRead);
    }
	
	// ____________________________________________________________________________________
	private File load()
	{
		try
		{
			File dstPath = new File(mPrefs.getString("datadir", ""));
			if(!isUpToDate(dstPath))
			{
				dstPath = findDataPath();
	
				if(mWipeUserdata)
				{
					if(confirmWipe(dstPath))
						deleteDirContent(dstPath);
					else
						return null;
				}
				
				if(dstPath == null)
					mError = String.format("Not enough space. %.2fMb required", (float)(mRequiredSpace)/(1024.f*1024.f));
				else {

					long startns = System.nanoTime();
					updateFiles(dstPath);
					long endns = System.nanoTime();

					// Show progess dialog at least 1 second just to make it readable
					try {
						int sleepms = Math.max(1000-(int)((endns-startns)/1000000), 0);
						Thread.sleep(sleepms);
					} catch(InterruptedException e) {
					}
				}
			}
			
			if(dstPath == null)
				return null;
			
			File saveDir = new File(dstPath, "save");
			if(saveDir.exists() && !saveDir.isDirectory())
				saveDir.delete();
			if(!saveDir.exists())
				saveDir.mkdir();

			return dstPath;
		}
		catch(IOException e)
		{
			e.printStackTrace();
			mError = "Unkown error while preparing content";
			return null;
		}
	}

	private boolean confirmWipe(File datapath) {

		String[] saves = new File(datapath, "save").list();
		if(saves == null || saves.length == 0)
			return true;

		mNetHack.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final TextView text = new TextView(mNetHack);

				SpannableStringBuilder b = new SpannableStringBuilder(
						"Your saved games are not compatible with this version. If you continue they will be deleted.\n\n"
						+ "Old releases can be found ");

				Spannable url = new SpannableString("at GitHub");
				url.setSpan(new URLSpan("https://github.com/gurrhack/NetHack-Android"), 0, url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

				b.append(url);

				text.setText(b, TextView.BufferType.SPANNABLE);
				text.setMovementMethod(LinkMovementMethod.getInstance());

				final float density = mNetHack.getResources().getDisplayMetrics().density;
				int padding = (int)(5 * density);
				text.setPadding(3*padding, padding, 3*padding, 2*padding);

				AlertDialog.Builder builder = new AlertDialog.Builder(mNetHack);
				builder.setTitle("This is NetHack 3.6.0").setView(text)
						.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								mWipeConfirmed = true;
							}
						})
						.setNegativeButton("Quit", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								mWipeConfirmed = false;
							}
						}).setOnCancelListener(new DialogInterface.OnCancelListener() {
							public void onCancel(DialogInterface dialog) {
								mWipeConfirmed = false;
							}
						});

				builder.create().show();
			}
		});

		while(mWipeConfirmed == null) {
			try {
				Thread.sleep(10);
			} catch(InterruptedException e) {
			}
		}

		return mWipeConfirmed;
	}

	// ____________________________________________________________________________________
	private void showError()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(mNetHack);
		builder.setMessage(mError).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				mNetHack.finish();
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				mNetHack.finish();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	// ____________________________________________________________________________________
	private boolean isUpToDate(File dstPath) throws IOException
	{
		if(!dstPath.exists() || !dstPath.isDirectory())
		{
			Log.print("Update required. '" + dstPath + "' doesn't exist");
			return false;
		}

		long verDat = mPrefs.getLong("verDat", 0);
		long srcVer = mPrefs.getLong("srcVer", 0);

		Scanner s = new Scanner(mAM.open("ver"));
		long curVer = s.nextLong();

		if(verDat == 0 || srcVer != curVer)
		{
			Log.print("Update required. old version");
			mWipeUserdata = true;
			return false;
		}

		String[] files = mAM.list("nethackdir");
		for(String file : files)
		{
			File dst = new File(dstPath, file);
			if(!dst.exists())
			{
				Log.print("Update required. '" + file + "' doesn't exist");
				return false;
			}
			
			if(!file.equals("defaults.nh") && dst.lastModified() > verDat)
			{
				Log.print("Update required. '" + file + "' has been tampered with");
				return false;
			}
		}
		Log.print("Data is up to date");
		return true;
	}

	// ____________________________________________________________________________________
	private void updateFiles(File dstPath) throws IOException
	{
		Log.print("Updating files...");
		if(!dstPath.exists())
			dstPath.mkdirs();

		byte[] buf = new byte[10240];
		String[] files = mAM.list("nethackdir");

		for(String file : files)
		{
			File dstFile = new File(dstPath, file);

			InputStream is = mAM.open("nethackdir/" + file);
			OutputStream os = new FileOutputStream(dstFile, false);

			while(true)
			{
				int nRead = is.read(buf);
				if(nRead > 0)
					os.write(buf, 0, nRead);
				else
					break;
				mTotalRead += nRead;
				publishProgress((Void[])null);
			}

			os.flush();
			os.close();
		}

		// update version and date
		SharedPreferences.Editor edit = mPrefs.edit();

		Scanner s = new Scanner(mAM.open("ver"));
		edit.putLong("srcVer", s.nextLong());

		// add a few seconds just in case
		long lastMod = new File(dstPath, files[files.length - 1]).lastModified() + 1000 * 60;
		edit.putLong("verDat", lastMod);

		edit.putString("datadir", dstPath.getAbsolutePath());

		edit.commit();
	}

	// ____________________________________________________________________________________
	private File findDataPath() throws IOException
	{
		File external = getExternalDataPath();
		File internal = getInternalDataPath();

		// File.getFreeSpace is not supported in API level 8. Assume there's enough
		// available, and use sdcard if it's mounted
		
		// clear out old/corrupt data
//		DeleteDirContent(external);
//		DeleteDirContent(internal);

		getRequiredSpace();

		// prefer external
//		if(external.getFreeSpace() > m_requiredSpace)
		if(external != null)
		{
			Log.print("Using sdcard");
			return external;
		}
		
//		if(internal.getFreeSpace() > m_requiredSpace)
		{
			Log.print("Using internal storage");
			return internal;
		}


//		return null;
	}

	// ____________________________________________________________________________________
	private File getExternalDataPath()
	{
		File dataDir = null;
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state))
			dataDir = new File(Environment.getExternalStorageDirectory(), "/Android/data/com.tbd.NetHack");
		return dataDir;
	}

	// ____________________________________________________________________________________
	private File getInternalDataPath()
	{
		return mNetHack.getFilesDir();
	}

	// ____________________________________________________________________________________
	private void getRequiredSpace() throws IOException
	{
		mRequiredSpace = 0;
		String[] files = mAM.list("nethackdir");
		for(String file : files)
		{
			InputStream is = mAM.open("nethackdir/" + file);
			mRequiredSpace += is.skip(0x7fffffff);
		}
	}

	// ____________________________________________________________________________________
	void deleteDirContent(File dir)
	{
		if(dir.exists() && dir.isDirectory())
		{
			for(String n : dir.list())
			{
				File file = new File(dir, n);
				deleteDirContent(file);
				file.delete();
			}
		}
	}
}
