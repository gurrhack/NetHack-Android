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

public class UpdateAssets extends AsyncTask<Void, Void, Void>
{
	private AssetManager m_am;
	private SharedPreferences m_prefs;
	//private ProgressDialog m_initDialog;
	private boolean m_bInitiating;
	private ProgressDialog m_progress;
	private File m_dstPath;
	private String m_error;
	private long m_requiredSpace;
	private long m_nTotalRead;

	// ____________________________________________________________________________________
	public UpdateAssets()
	{
		m_prefs = NetHack.get().getPreferences(Activity.MODE_PRIVATE);
		m_am = NetHack.get().getResources().getAssets();
		//m_initDialog = ProgressDialog.show(NetHack.get(), "", "Initiating. Please wait...", true);
		m_bInitiating = true;
		m_nTotalRead = 0;
		m_requiredSpace = 0;
	}
	
	// ____________________________________________________________________________________
	@Override
	protected void onPostExecute(Void unused)
	{
		//if(m_initDialog != null)
		//	m_initDialog.dismiss();
		if(m_progress != null)
			m_progress.dismiss();
		if(m_dstPath == null)
		{
			ShowError();
		}
		else
		{
			Log.print("Starting on: " + m_dstPath.getAbsolutePath());			
			NetHack.get().Start(m_dstPath);
		}
	}

	// ____________________________________________________________________________________
	@Override
	protected Void doInBackground(Void... params)
	{
		m_dstPath = Load();
		return null;
	}

	// ____________________________________________________________________________________
	@Override
    protected void onProgressUpdate(Void... progress)
	{
		if(m_nTotalRead > 0 && m_bInitiating)// m_initDialog != null)
		{
			//m_initDialog.dismiss();
			//m_initDialog = null;
			m_bInitiating = false;
			
			m_progress = new ProgressDialog(NetHack.get());
			m_progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			m_progress.setMax((int)m_requiredSpace);
			m_progress.setMessage("Preparing content for first time use...");
			m_progress.setCancelable(false);
			m_progress.show();
		}
		m_progress.setProgress((int)m_nTotalRead);
    }
	
	// ____________________________________________________________________________________
	private File Load()
	{
		try
		{
			File dstPath = new File(m_prefs.getString("datadir", ""));
			if(!IsUpToDate(dstPath))
			{
				dstPath = FindDataPath();
	
				if(dstPath == null)
					m_error = String.format("Not enough space. %.2fMb required", (float)(m_requiredSpace)/(1024.f*1024.f));
				else
					UpdateFiles(dstPath);
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
			m_error = "Unkown error while preparing content";
			return null;
		}
	}

	// ____________________________________________________________________________________
	private void ShowError()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(NetHack.get());
		builder.setMessage(m_error).setPositiveButton("Ok", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				NetHack.get().finish();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	// ____________________________________________________________________________________
	private boolean IsUpToDate(File dstPath) throws IOException
	{
		if(!dstPath.exists() || !dstPath.isDirectory())
			return false;

		long verDat = m_prefs.getLong("verDat", 0);
		long srcVer = m_prefs.getLong("srcVer", 0);

		Scanner s = new Scanner(m_am.open("ver"));
		long curVer = s.nextLong();

		if(verDat == 0 || srcVer != curVer)
			return false;

		String[] files = m_am.list("nethackdir");
		for(String file : files)
		{
			File dst = new File(dstPath, file);
			if(!dst.exists())
				return false;
			
			if(dst.lastModified() > verDat)
			{
				Log.print("Update required. '" + file + "' has been tampered with");
				return false;
			}
		}
		Log.print("Data is up to date");
		return true;
	}

	// ____________________________________________________________________________________
	private void UpdateFiles(File dstPath) throws IOException
	{
		Log.print("Updating files...");
		if(!dstPath.exists())
			dstPath.mkdirs();

		byte[] buf = new byte[10240];
		String[] files = m_am.list("nethackdir");

		for(String file : files)
		{
			File dstFile = new File(dstPath, file);

			InputStream is = m_am.open("nethackdir/" + file);
			OutputStream os = new FileOutputStream(dstFile, false);

			while(true)
			{
				int nRead = is.read(buf);
				if(nRead > 0)
					os.write(buf, 0, nRead);
				else
					break;
				m_nTotalRead += nRead;
				publishProgress((Void[])null);
			}

			os.flush();
		}

		// update version and date
		SharedPreferences.Editor edit = m_prefs.edit();

		Scanner s = new Scanner(m_am.open("ver"));
		edit.putLong("srcVer", s.nextLong());

		// add a few seconds just in case
		long lastMod = new File(dstPath, files[files.length - 1]).lastModified() + 1000 * 60;
		edit.putLong("verDat", lastMod);

		edit.putString("datadir", dstPath.getAbsolutePath());

		edit.commit();
	}

	// ____________________________________________________________________________________
	private File FindDataPath() throws IOException
	{
		File external = GetExternalDataPath();
		File internal = GetInternalDataPath();

		// File.getFreeSpace is not supported in API level 8. Assume there's enough
		// available, and use sdcard if it's mounted
		
		// clear out old/corrupt data
//		DeleteDirContent(external);
//		DeleteDirContent(internal);

		GetRequiredSpace();

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
	private File GetExternalDataPath()
	{
		File dataDir = null;
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state))
			dataDir = new File(Environment.getExternalStorageDirectory(), "/Android/data/com.tbd.NetHack");
		return dataDir;
	}

	// ____________________________________________________________________________________
	private File GetInternalDataPath()
	{
		return NetHack.get().getFilesDir();
	}

	// ____________________________________________________________________________________
	private void GetRequiredSpace() throws IOException
	{
		m_requiredSpace = 0;
		String[] files = m_am.list("nethackdir");
		for(String file : files)
		{
			InputStream is = m_am.open("nethackdir/" + file);
			m_requiredSpace += is.skip(0x7fffffff);
		}
	}

	// ____________________________________________________________________________________
	void DeleteDirContent(File dir) throws IOException
	{
		if(dir.exists() && dir.isDirectory())
		{
			for(File file : dir.listFiles())
			{
				DeleteDirContent(file);
				if(!file.delete())
					throw new IOException();
			}
		}
	}
}
