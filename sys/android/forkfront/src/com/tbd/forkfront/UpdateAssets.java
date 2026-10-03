package com.tbd.forkfront;

import java.io.*;
import java.util.Date;
import java.util.Scanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

public class UpdateAssets extends AsyncTask<Void, Void, Void>
{
	public interface Listener
	{
		void onAssetsReady(File path);
	}

	private static String DATADIR_KEY = "datadir";
	private static String VERDAT_KEY = "verDat";
	private static String SRCVER_KEY = "srcVer";

	enum FileStatus {
		UP_TO_DATE,
		CORRUPT,
		OLD_VERSION,
		INCOMPATIBLE_VERSION,
	}

	private AssetManager mAM;
	private SharedPreferences mPrefs;
	private boolean mIsInitiating;
	private ProgressDialog mProgress;
	private File mDstPath;
	private String mError;
	private FileStatus mFileStatus;
	private boolean mBackupDefaultsFile;
	private boolean mDefaultsFileBackedUp;
	private long mRequiredSpace;
	private long mTotalRead;
	private Activity mActivity;
	private final Listener mListener;
	private final String mNativeDataDir;
	private final String mNamespace;
	private final String mDefaultsFile;

	// ____________________________________________________________________________________
	public UpdateAssets(Activity activity, Listener listener)
	{
		convertFromOldPreferences(activity);
		mActivity = activity;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
		mAM = mActivity.getResources().getAssets();
		mIsInitiating = true;
		mTotalRead = 0;
		mRequiredSpace = 0;
		mListener = listener;
		mNativeDataDir = activity.getResources().getString(R.string.nativeDataDir);
		mNamespace = activity.getResources().getString(R.string.namespace);
		mDefaultsFile = activity.getResources().getString(R.string.defaultsFile);
	}

	// ____________________________________________________________________________________
	private static void convertFromOldPreferences(Activity activity)
	{
		// Old versions used a different preference store than the rest of the app
		String oldActivityName = activity.getResources().getString(R.string.oldActivityName);
		if(oldActivityName == null || oldActivityName.length() == 0)
			return;

		SharedPreferences oldPrefs = activity.getSharedPreferences(oldActivityName, Activity.MODE_PRIVATE);
		SharedPreferences newPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
		SharedPreferences.Editor oldEditor = oldPrefs.edit();
		SharedPreferences.Editor newEditor = newPrefs.edit();

		boolean hasDataDir = oldPrefs.contains(DATADIR_KEY);
		boolean hasVerDat = oldPrefs.contains(VERDAT_KEY);
		boolean hasSrcVer = oldPrefs.contains(SRCVER_KEY);
		if(hasDataDir)
		{
			newEditor.putString(DATADIR_KEY, oldPrefs.getString(DATADIR_KEY, ""));
			oldEditor.remove(DATADIR_KEY);
		}
		if(hasVerDat)
		{
			newEditor.putLong(VERDAT_KEY, oldPrefs.getLong(VERDAT_KEY, 0));
			oldEditor.remove(VERDAT_KEY);
		}
		if(hasSrcVer)
		{
			newEditor.putLong(SRCVER_KEY, oldPrefs.getLong(SRCVER_KEY, 0));
			oldEditor.remove(SRCVER_KEY);
		}
		if(hasDataDir || hasVerDat || hasSrcVer)
		{
			newEditor.commit();
			oldEditor.commit();
		}
	}

	// ____________________________________________________________________________________
	@Override
	protected void onPostExecute(Void unused)
	{
		if(mProgress != null)
			mProgress.dismiss();
		if(mDstPath == null)
		{
			showError();
		}
		else
		{
			if(mDefaultsFileBackedUp) {
				String symLinkedPath = "/sdcard/Android/data/" + mNamespace + "/";
				showMessage("Your " + mDefaultsFile + " file was replaced during the update. A backup is saved in:\n" + symLinkedPath);
			}
			Log.print("Starting on: " + mDstPath.getAbsolutePath());
			mListener.onAssetsReady(mDstPath);
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
		if(mTotalRead > 0 && mIsInitiating)
		{
			mIsInitiating = false;
			
			mProgress = new ProgressDialog(mActivity);
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
			File dstPath = new File(mPrefs.getString(DATADIR_KEY, ""));
			mFileStatus = checkFiles(dstPath);
			if(mFileStatus != FileStatus.UP_TO_DATE)
			{
				dstPath = findDataPath();
	
				if(mFileStatus == FileStatus.INCOMPATIBLE_VERSION)
				{
					// TODO confirm dialog or just make backup of saves and bones
					deleteDirContent(dstPath);
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
			mError = "Unknown error while preparing content";
			return null;
		}
	}

	// ____________________________________________________________________________________
	private void showError()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setMessage(mError).setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				mActivity.finish();
			}
		}).setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			public void onCancel(DialogInterface dialog)
			{
				mActivity.finish();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	// ____________________________________________________________________________________
	private void showMessage(String msg)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setMessage(msg).setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	// ____________________________________________________________________________________
	private FileStatus checkFiles(File dstPath) throws IOException
	{
		if(!dstPath.exists() || !dstPath.isDirectory())
		{
			Log.print("Update required. '" + dstPath + "' doesn't exist");
			return FileStatus.CORRUPT;
		}

		long verDat = mPrefs.getLong(VERDAT_KEY, 0);
		long srcVer = mPrefs.getLong(SRCVER_KEY, 0);

		Scanner s = new Scanner(mAM.open("ver"));
		long curVer = s.nextLong();

		if(verDat == 0 || srcVer != curVer)
		{
			Log.print("Update required. old version");

			File dst = new File(dstPath, mDefaultsFile);
			if(dst.exists() && dst.lastModified() > verDat)
				mBackupDefaultsFile = true;

			if((srcVer / 100) != (curVer / 100)) {
				Log.print("Really old. Must remove incompatible save and bones files.");
				return FileStatus.INCOMPATIBLE_VERSION;
			}

			// This is a hack on top of a hack. Handle updates in some better way
			if((srcVer / 10) != (curVer / 10)) {
				Log.print("Old version with updated defaults.");
				return FileStatus.OLD_VERSION;
			}

			// Update assets but keep defaults, so no need for backup
			mBackupDefaultsFile = false;

			return FileStatus.CORRUPT;
		}

		String[] files = mAM.list(mNativeDataDir);
		for(String file : files)
		{
			File dst = new File(dstPath, file);
			if(!dst.exists())
			{
				Log.print("Update required. '" + file + "' doesn't exist");
				return FileStatus.CORRUPT;
			}
			
			if(!file.equals(mDefaultsFile) && dst.lastModified() > verDat)
			{
				Log.print("Update required. '" + file + "' has been tampered with");
				return FileStatus.CORRUPT;
			}
		}
		Log.print("Data is up to date");
		return FileStatus.UP_TO_DATE;
	}

	// ____________________________________________________________________________________
	private void updateFiles(File dstPath) throws IOException
	{
		Log.print("Updating files...");
		if(!dstPath.exists())
			dstPath.mkdirs();

		byte[] buf = new byte[10240];
		String[] files = mAM.list(mNativeDataDir);

		if(mBackupDefaultsFile)
		{
			doDefaultsBackup(dstPath, buf);
		}

		for(String file : files)
		{
			File dstFile = new File(dstPath, file);

			// Don't overwrite defaults file if we're just fixing corruption
			if(file.equals(mDefaultsFile) && dstFile.exists() && mFileStatus == FileStatus.CORRUPT) {
				InputStream is = mAM.open(mNativeDataDir + "/" + file);
				mTotalRead += is.skip(0x7fffffff);
				continue;
			}

			InputStream is = mAM.open(mNativeDataDir + "/" + file);
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
		edit.putLong(SRCVER_KEY, s.nextLong());

		// add a few seconds just in case
		long lastMod = new File(dstPath, files[files.length - 1]).lastModified() + 1000 * 60;
		edit.putLong(VERDAT_KEY, lastMod);

		// TODO don't store absolute path.
		edit.putString(DATADIR_KEY, dstPath.getAbsolutePath());

		edit.commit();
	}

	private void doDefaultsBackup(File dstPath, byte[] buf) {
		try {
			File srcFile = new File(dstPath, mDefaultsFile);
			if(!srcFile.exists())
				return;

			File dstFile = new File(dstPath, mDefaultsFile + ".bak");

			InputStream is = new FileInputStream(srcFile);
			OutputStream os = new FileOutputStream(dstFile, false);

			while(true) {
				int nRead = is.read(buf);
				if(nRead > 0)
					os.write(buf, 0, nRead);
				else
					break;
			}

			os.flush();
			os.close();

			mDefaultsFileBackedUp = true;
		} catch(IOException e) {
			Log.print("Failed to backup defaults file: " + e.toString());
		}
	}

	// ____________________________________________________________________________________
	private File findDataPath() throws IOException
	{
		// TODO remove support for internal storage.
		// TODO migrate files to new storage if possible.

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
		if(Environment.MEDIA_MOUNTED.equals(state)) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				dataDir = mActivity.getExternalFilesDir(null);
			} else {
				dataDir = new File(Environment.getExternalStorageDirectory(), "/Android/data/" + mNamespace);
			}
		}
		return dataDir;
	}

	// ____________________________________________________________________________________
	private File getInternalDataPath()
	{
		return mActivity.getFilesDir();
	}

	// ____________________________________________________________________________________
	private void getRequiredSpace() throws IOException
	{
		mRequiredSpace = 0;
		String[] files = mAM.list(mNativeDataDir);
		for(String file : files)
		{
			InputStream is = mAM.open(mNativeDataDir + "/" + file);
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
