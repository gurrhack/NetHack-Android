package com.tbd.NetHack;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;

public class NetHackIO implements Runnable
{
	private Handler mHandler;
	private Thread mThread;
	private NH_State mState;
	private ConcurrentLinkedQueue<Integer> mCmdQue;
	private int mNextWinId;
	private int mMessageWid;
	private volatile Integer mIsReady = 0;
	private Object mReadyMonitor = new Object();

	// ____________________________________________________________________________________ //
	// Send commands																		//
	// ____________________________________________________________________________________ //
	private static final int KeyCmd = 0x80000000;
	private static final int PosCmd = 0x90000000;
	private static final int LineCmd = 0xa0000000;
	private static final int SelectCmd = 0xb0000000;
	private static final int SaveStateCmd = 0xc0000000;
	private static final int AbortCmd = 0xd0000000;

	private static final int FlagCmd = 0xe0000000;
	private static final int CmdMask = 0xf0000000;
	private static final int DataMask = 0x0fffffff;

	// ____________________________________________________________________________________
	public NetHackIO(NH_State state)
	{
		mState = state;
		mNextWinId = 1;
		mCmdQue = new ConcurrentLinkedQueue<Integer>();
		mHandler = new Handler();
/*		Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler()
		{			
			@Override
			public boolean queueIdle()
			{
				//Log.print("idling...");
				waitOut(2000);
				while(!mHandler.empty())
				{
					flushIn();
					waitOut(2000);
				}
				return true;
			}
		});*/
	}

	// ____________________________________________________________________________________
	public void start()
	{
		if(mThread != null)
			throw new IllegalStateException();
		mThread = new Thread(this, "nh_thread");
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
	public void run()
	{
		Log.print("start native process");
		try
		{
			System.loadLibrary("nethack");
			RunNetHack(mState.getDataDir());
		}
		catch(Exception e)
		{
			Log.print("EXCEPTED");
		}
		Log.print("native process finished");
		System.exit(0);
	}

	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v)
	{
		mState.onCreateContextMenu(menu, v);
	}

	// ____________________________________________________________________________________
	public void onContextItemSelected(android.view.MenuItem item)
	{
		mState.onContextItemSelected(item);
	}

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
		mState.hideDPad();
		mCmdQue.add(KeyCmd);
		mCmdQue.add((int)key);
	}

	// ____________________________________________________________________________________
	public void sendDirKeyCmd(char key)
	{
		mState.hideDPad();
		mCmdQue.add(PosCmd);
		mCmdQue.add((int)key);
		mCmdQue.add(0);
		mCmdQue.add(0);
	}

	// ____________________________________________________________________________________
	public void sendPosCmd(int x, int y)
	{
		mState.hideDPad();
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
	private int receivePosKeyCmd(int lockMouse, int[] pos)
	{
		incReady();

		if(lockMouse != 0)
		{
			mHandler.post(new Runnable()
			{
				public void run()
				{
					mState.lockMouse();
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
	private void debugLog(final byte[] cmsg)
	{
		Log.print(CP437.decode(cmsg));
	}

	// ____________________________________________________________________________________
	private void setCursorPos(final int wid, final int x, final int y)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.setCursorPos(wid, x, y);
			}
		});
	}

	// ____________________________________________________________________________________
	private void putString(final int wid, final int attr, final byte[] cmsg, final int append, final int color)
	{
		final String msg = CP437.decode(cmsg);
		if(wid == mMessageWid)
			Log.print(msg);

		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.putString(wid, attr, msg, append, color);
			}
		});
	}

	// ____________________________________________________________________________________
	private void setHealthColor(final int color)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.setHealthColor(color);
			}
		});
	}

	// ____________________________________________________________________________________
	private void redrawStatus()
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.redrawStatus();
			}
		});
	}

	// ____________________________________________________________________________________
	private void rawPrint(final int attr, final byte[] cmsg)
	{
		final String msg = CP437.decode(cmsg);
		Log.print(msg);
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.rawPrint(attr, msg);
			}
		});
	}

	// ____________________________________________________________________________________
	private void printTile(final int wid, final int x, final int y, final int tile, final int ch, final int col, final int special)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.printTile(wid, x, y, tile, ch, col, special);
			}
		});
	}

	// ____________________________________________________________________________________
	private void ynFunction(final byte[] cquestion, final byte[] choices, final int def)
	{
		final String question = CP437.decode(cquestion);
		//Log.print("nhthread: ynFunction");
		mHandler.post(new Runnable()
		{
			public void run()
			{
				//Log.print("uithread: ynFunction");
				mState.ynFunction(question, choices, def);
			}
		});
	}

	// ____________________________________________________________________________________
	private String getLine(final byte[] title, final int nMaxChars, final int showLog, int reentry)
	{
		if(reentry == 0)
		{
			final String msg = CP437.decode(title);
			//Log.print("nhthread: getLine");
			mHandler.post(new Runnable()
			{
				public void run()
				{
					//Log.print("uithread: getLine");
					mState.getLine(msg, nMaxChars, showLog != 0);
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
	// @SuppressWarnings("unused")
	private int createWindow(final int type)
	{
		final int wid = mNextWinId++;
		if(type == 1)
			mMessageWid = wid;
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.createWindow(wid, type);
			}
		});
		return wid;
	}

	// ____________________________________________________________________________________
	private void displayWindow(final int wid, final int bBlocking)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.displayWindow(wid, bBlocking);
			}
		});
	}

	// ____________________________________________________________________________________
	private void clearWindow(final int wid, final int isRogueLevel)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.clearWindow(wid, isRogueLevel);
			}
		});
	}

	// ____________________________________________________________________________________
	private void destroyWindow(final int wid)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.destroyWindow(wid);
			}
		});
	}

	// ____________________________________________________________________________________
	private void startMenu(final int wid)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.startMenu(wid);
			}
		});
	}

	// ____________________________________________________________________________________
	private void addMenu(final int wid, final int tile, final int id, final int acc, final int groupAcc, final int attr, final byte[] text, final int bSelected, final int color)
	{
		final String msg = CP437.decode(text);
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.addMenu(wid, tile, id, acc, groupAcc, attr, msg, bSelected, color);
			}
		});
	}

	// ____________________________________________________________________________________
	private void endMenu(final int wid, final byte[] prompt)
	{
		final String msg = CP437.decode(prompt);
		//Log.print("nhthread: endMenu");
		mHandler.post(new Runnable()
		{
			public void run()
			{
				//Log.print("uithread: endMenu");
				mState.endMenu(wid, msg);
			}
		});
	}

	// ____________________________________________________________________________________
	private int[] selectMenu(final int wid, final int how, final int reentry)
	{
		//Log.print("nhthread: selectMenu");
		if(reentry == 0)
		{
			mHandler.post(new Runnable()
			{
				public void run()
				{
					//Log.print("uithread: selectMenu");
					mState.selectMenu(wid, how);
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
	private void cliparound(final int x, final int y, final int playerX, final int playerY)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.cliparound(x, y, playerX, playerY);
			}
		});
	}

	// ____________________________________________________________________________________
	private void askDirection()
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.showDPad();
			}
		});
	}

	// ____________________________________________________________________________________
	private void showLog(final int bBlocking)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.showLog(bBlocking);
			}
		});
	}

	// ____________________________________________________________________________________
	private void editOpts()
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.editOpts();
			}
		});
	}

	// ____________________________________________________________________________________
	private void setUsername(final byte[] username)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.setLastUsername(new String(username));
			}
		});
	}

	// ____________________________________________________________________________________
	private void setNumPadOption(final int num_pad)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.setNumPadOption(num_pad != 0);
			}
		});
	}
	
	// ____________________________________________________________________________________
	private String askName(final int nMaxChars, final String[] saves)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.askName(nMaxChars, saves);
			}
		});
		return waitForLine();
	}
	
	// ____________________________________________________________________________________
	private native void RunNetHack(String path);
	private native void SaveNetHackState();
}
