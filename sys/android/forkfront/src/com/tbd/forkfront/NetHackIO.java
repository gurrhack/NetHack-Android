package com.tbd.forkfront;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import com.tbd.forkfront.*;

public class NetHackIO
{
	private final Handler mHandler;
	private final NH_Handler mNhHandler;
	private final Thread mThread;
	private final ByteDecoder mDecoder;
	private final String mLibraryName;
	private final ConcurrentLinkedQueue<Integer> mCmdQue;
	private int mNextWinId;
	private int mMessageWid;
	private volatile Integer mIsReady = 0;
	private final Object mReadyMonitor = new Object();
	private String mDataDir;

	// ____________________________________________________________________________________ //
	// Send commands																		//
	// ____________________________________________________________________________________ //
	private static final int KeyCmd = 0x80000000;
	private static final int PosCmd = 0x90000000;
	private static final int LineCmd = 0xa0000000;
	private static final int SelectCmd = 0xb0000000;
	private static final int SaveStateCmd = 0xc0000000;
	private static final int AbortCmd = 0xd0000000;
	private static final int DataMask = 0x0fffffff;

	// ____________________________________________________________________________________
	public NetHackIO(Activity context, NH_Handler nhHandler, ByteDecoder decoder)
	{
		mNhHandler = nhHandler;
		mDecoder = decoder;
		mLibraryName = context.getResources().getString(R.string.libraryName);
		mNextWinId = 1;
		mCmdQue = new ConcurrentLinkedQueue<>();
		mHandler = new Handler();
		mThread = new Thread(ThreadMain, "nh_thread");
	}

	// ____________________________________________________________________________________
	public void start(String path)
	{
		mDataDir = path;
		mThread.start();
	}

	// ____________________________________________________________________________________
	public void saveState()
	{
		mCmdQue.add(SaveStateCmd);
		// give it some time
		for(int i = 0; i < 5; i++)
		{
			try
			{
				Thread.sleep(150);
				break;
			}
			catch(InterruptedException e)
			{
			}
		}
	}

	// ____________________________________________________________________________________
	public void saveAndQuit()
	{
		// send a few abort commands to cancel ongoing operations
		sendAbortCmd();
		sendAbortCmd();
		sendAbortCmd();
		sendAbortCmd();

		// give it some time
		for(int i = 0; i < 5; i++)
		{
			try
			{
				Thread.sleep(150);
				break;
			}
			catch(InterruptedException e)
			{
			}
		}
	}

	// ____________________________________________________________________________________
	public void waitReady()
	{
		// flush out queue   
		long endTime = System.currentTimeMillis() + 1000;			
		while(mCmdQue.peek() != null && endTime - System.currentTimeMillis() > 0)
			Thread.yield();

		// boolean test = mIsReady == 0;
		// if(test)
		// 	Log.print("TEST:TEST:TEST:");
		
		synchronized(mReadyMonitor)
		{
			try
			{
				// wait until nethack is ready for more input
				do
					mReadyMonitor.wait(10);
				while(mIsReady == 0);
			}
			catch(InterruptedException e)
			{
			}
		}
	}
	
	// ____________________________________________________________________________________
	public Handler getHandler()
	{
		return mHandler;
	}

	// ____________________________________________________________________________________
	private Runnable ThreadMain = new Runnable()
	{
		@Override
		public void run()
		{
			Log.print("start native process");

			try
			{
				System.loadLibrary(mLibraryName);
				RunNetHack(mDataDir);
			}
			catch(Exception e)
			{
				Log.print("EXCEPTED");
			}
			Log.print("native process finished");
			System.exit(0);
		}
	};

	// ____________________________________________________________________________________
	int verifyData(int d)
	{
		if(DEBUG.isOn() && d != 0 && (d & DataMask) == 0)
			throw new IllegalArgumentException();
		return d;
	}

	// ____________________________________________________________________________________
	public void sendKeyCmd(char key)
	{
		mNhHandler.hideDPad();
		mCmdQue.add(KeyCmd);
		mCmdQue.add((int)key);
	}

	// ____________________________________________________________________________________
	public void sendDirKeyCmd(char key)
	{
		mNhHandler.hideDPad();
		mCmdQue.add(PosCmd);
		mCmdQue.add((int)key);
		mCmdQue.add(0);
		mCmdQue.add(0);
	}

	// ____________________________________________________________________________________
	public void sendPosCmd(int x, int y)
	{
		mNhHandler.hideDPad();
		mCmdQue.add(PosCmd);
		mCmdQue.add(0);
		mCmdQue.add(verifyData(x));
		mCmdQue.add(verifyData(y));
	}

	// ____________________________________________________________________________________
	public void sendLineCmd(String str)
	{
		mCmdQue.add(LineCmd);
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			if(c == '\n')
				break;
			if(c < 0xff)
				mCmdQue.add((int)c);
		}
		mCmdQue.add((int)'\n');
	}

	// ____________________________________________________________________________________
	public void sendSelectCmd(int id, int count)
	{
		mCmdQue.add(SelectCmd);
		mCmdQue.add(1);
		mCmdQue.add(verifyData(id));
		mCmdQue.add(verifyData(count));
	}

	// ____________________________________________________________________________________
	public void sendSelectCmd(ArrayList<MenuItem> items)
	{
		mCmdQue.add(SelectCmd);
		mCmdQue.add(verifyData(items.size()));
		for(MenuItem i : items)
		{
			mCmdQue.add(verifyData(i.getId()));
			mCmdQue.add(verifyData(i.getCount()));
		}
	}

	// ____________________________________________________________________________________
	public void sendSelectNoneCmd()
	{
		mCmdQue.add(SelectCmd);
		mCmdQue.add(0);
	}

	// ____________________________________________________________________________________
	public void sendCancelSelectCmd()
	{
		mCmdQue.add(SelectCmd);
		mCmdQue.add(-1);
	}

	// ____________________________________________________________________________________
	private void sendAbortCmd()
	{
		mCmdQue.add(AbortCmd);
	}

	// ------------------------------------------------------------------------------------
	// Receive commands called from nethack thread
	// ------------------------------------------------------------------------------------

	// ____________________________________________________________________________________
	private int removeFromQue()
	{
		Integer c = mCmdQue.poll();
		while(c == null)
		{
			try
			{
				Thread.sleep(50);
			}
			catch(InterruptedException e)
			{
			}
			c = mCmdQue.poll();
		}
		return c;
	}

	// ____________________________________________________________________________________
	private void handleSpecialCmds(int cmd)
	{
		switch(cmd)
		{
		case SaveStateCmd:
			SaveNetHackState();
		break;
		}
	}

	// ____________________________________________________________________________________
	private int discardUntil(int cmd0)
	{
		int cmd;
		do
		{
			cmd = removeFromQue();
			handleSpecialCmds(cmd);
		}while(cmd != cmd0 && cmd != AbortCmd);
		return cmd;
	}

	// ____________________________________________________________________________________
	private int discardUntil(int cmd0, int cmd1)
	{
		int cmd;
		do
		{
			cmd = removeFromQue();
			handleSpecialCmds(cmd);
		}while(cmd != cmd0 && cmd != cmd1 && cmd != AbortCmd);
		return cmd;
	}

	// ____________________________________________________________________________________
	private void incReady()
	{
		synchronized(mReadyMonitor)
		{
			if(mIsReady++ == 0)
				mReadyMonitor.notify();
		}
	}

	// ____________________________________________________________________________________
	private void decReady()
	{
		mIsReady--;
		if(mIsReady < 0)
			throw new RuntimeException();
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private int receiveKeyCmd()
	{
		int key = 0x80;

		incReady();
		
		if(discardUntil(KeyCmd) == KeyCmd)
			key = removeFromQue();

		decReady();
		return key;
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private int receivePosKeyCmd(int lockMouse, int[] pos)
	{
		incReady();

		if(lockMouse != 0)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					mNhHandler.lockMouse();
				}
			});
		}

		int cmd = discardUntil(KeyCmd, PosCmd);

		int key = 0x80;

		if(cmd != AbortCmd)
		{			
			key = removeFromQue();
			if(cmd == PosCmd)
			{
				pos[0] = removeFromQue(); // x
				pos[1] = removeFromQue(); // y
			}
		}
		
		decReady();		
		return key;
	}

	// ------------------------------------------------------------------------------------
	// Functions called by nethack thread to schedule an operation on UI thread
	// ------------------------------------------------------------------------------------

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void debugLog(final byte[] cmsg)
	{
		Log.print(mDecoder.decode(cmsg));
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void setCursorPos(final int wid, final int x, final int y)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.setCursorPos(wid, x, y);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void putString(final int wid, final int attr, final byte[] cmsg, final int append, final int color)
	{
		final String msg = mDecoder.decode(cmsg);
		if(wid == mMessageWid)
			Log.print(msg);

		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.putString(wid, attr, msg, append, color);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void setHealthColor(final int color)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.setHealthColor(color);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void redrawStatus()
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.redrawStatus();
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void rawPrint(final int attr, final byte[] cmsg)
	{
		final String msg = mDecoder.decode(cmsg);
		Log.print(msg);
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.rawPrint(attr, msg);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void printTile(final int wid, final int x, final int y, final int tile, final int ch, final int col, final int special)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.printTile(wid, x, y, tile, ch, col, special);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void ynFunction(final byte[] cquestion, final byte[] choices, final int def)
	{
		final String question = mDecoder.decode(cquestion);
		//Log.print("nhthread: ynFunction");
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				//Log.print("uithread: ynFunction");
				mNhHandler.ynFunction(question, choices, def);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private String getLine(final byte[] title, final int nMaxChars, final int showLog, int reentry)
	{
		if(reentry == 0)
		{
			final String msg = mDecoder.decode(title);
			//Log.print("nhthread: getLine");
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					//Log.print("uithread: getLine");
					mNhHandler.getLine(msg, nMaxChars, showLog != 0);
				}
			});
		}
		return waitForLine();
	}

	// ____________________________________________________________________________________
	private String waitForLine()
	{
		incReady();
		
		StringBuilder builder = new StringBuilder();

		if(discardUntil(LineCmd) == LineCmd)
		{
			while(true)
			{
				int c = removeFromQue();
				if(c == '\n')
					break;
				// prevent injecting special abort character
				if(c == 0x80)
					c = '?';
				builder.append((char)c);
			}
		}
		else
			builder.append((char)0x80);
		
		decReady();
		return builder.toString();
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void delayOutput()
	{
		try
		{
			Thread.sleep(50);
		}
		catch(InterruptedException e)
		{
		}
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private int createWindow(final int type)
	{
		final int wid = mNextWinId++;
		if(type == 1)
			mMessageWid = wid;
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.createWindow(wid, type);
			}
		});
		return wid;
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void displayWindow(final int wid, final int bBlocking)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.displayWindow(wid, bBlocking);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void clearWindow(final int wid, final int isRogueLevel)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.clearWindow(wid, isRogueLevel);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void destroyWindow(final int wid)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.destroyWindow(wid);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void startMenu(final int wid)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.startMenu(wid);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void addMenu(final int wid, final int tile, final int id, final int acc, final int groupAcc, final int attr, final byte[] text, final int bSelected, final int color)
	{
		final String msg = mDecoder.decode(text);
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.addMenu(wid, tile, id, acc, groupAcc, attr, msg, bSelected, color);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void endMenu(final int wid, final byte[] prompt)
	{
		final String msg = mDecoder.decode(prompt);
		//Log.print("nhthread: endMenu");
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				//Log.print("uithread: endMenu");
				mNhHandler.endMenu(wid, msg);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private int[] selectMenu(final int wid, final int how, final int reentry)
	{
		//Log.print("nhthread: selectMenu");
		if(reentry == 0)
		{
			mHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					//Log.print("uithread: selectMenu");
					mNhHandler.selectMenu(wid, how);
				}
			});
		}
		return waitForSelect();
	}

	// ____________________________________________________________________________________
	private int[] waitForSelect()
	{
		incReady();
		
		int[] items = null;
		
		int cmd = discardUntil(SelectCmd);
		if(cmd == SelectCmd)
		{
			int nItems = removeFromQue();
			if(nItems >= 0)
			{
				items = new int[nItems * 2];
				for(int i = 0; i < items.length;)
				{
					items[i++] = removeFromQue(); // id
					items[i++] = removeFromQue(); // count
				}				
			}
		}
		else if(cmd == AbortCmd)
		{
			items = new int[1];
		}
		
		decReady();
		return items;
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void cliparound(final int x, final int y, final int playerX, final int playerY)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.cliparound(x, y, playerX, playerY);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void askDirection()
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.showDPad();
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void showLog(final int bBlocking)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.showLog(bBlocking);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void editOpts()
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.editOpts();
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void setUsername(final byte[] username)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.setLastUsername(new String(username));
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void setNumPadOption(final int num_pad)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.setNumPadOption(num_pad != 0);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private String askName(final int nMaxChars, final String[] saves)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.askName(nMaxChars, saves);
			}
		});
		return waitForLine();
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void loadSound(final byte[] filename)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.loadSound(new String(filename));
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void playSound(final byte[] filename, final int volume)
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mNhHandler.playSound(new String(filename), volume);
			}
		});
	}

	@SuppressWarnings("unused")
	private String getDumplogDir()
	{
		String path = "";
		try
		{
			File file = new File(Environment.getExternalStorageDirectory(), "Documents" + File.separator + "nethack");
			if(!file.exists())
				file.mkdirs();
			path = file.getAbsolutePath();
		} catch(Exception e) {}
		return path;
	}

	// ____________________________________________________________________________________
	private native void RunNetHack(String path);
	private native void SaveNetHackState();
}
