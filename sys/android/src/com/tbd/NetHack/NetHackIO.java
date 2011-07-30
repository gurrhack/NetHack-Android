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

	// ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾ //
	// Send commands																		//
	// ____________________________________________________________________________________ //
	private static final int KeyCmd = 0x80000000;
	private static final int PosCmd = 0x90000000;
	private static final int LineCmd = 0xa0000000;
	private static final int SelectCmd = 0xb0000000;
	private static final int SaveStateCmd = 0xc0000000;

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
	}

	// ____________________________________________________________________________________
	public void start()
	{
		if(mThread != null)
			throw new IllegalStateException();
		mThread = new Thread(this);
		mThread.start();
	}

	// ____________________________________________________________________________________
	public void sendFlags(boolean bAutoMenu, boolean bAutoPickup, String pickupTypes)
	{
		// take care of special characters
		pickupTypes = pickupTypes.replace('\'', '`');
		pickupTypes = pickupTypes.replace('\u00B4', '`');
		pickupTypes = pickupTypes.replace('\u2018', '`');
		pickupTypes = pickupTypes.replace('\u2019', '`');
		pickupTypes = pickupTypes.replace('\u201C', '"');
		pickupTypes = pickupTypes.replace('\u201D', '"');

		mCmdQue.add(FlagCmd);
		mCmdQue.add(bAutoMenu ? 1 : 0);
		mCmdQue.add(bAutoPickup ? 1 : 0);
		sendLineCmd(pickupTypes.trim());
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
	public void run()
	{
		Log.print("start native process");
		try
		{
			System.loadLibrary("nethack");
			RunNetHack(mState.getDataDir(), mState.getUsername(), mState.isWizard());
		}
		catch(Exception e)
		{
			Log.print("EXCEPTED");
		}
		Log.print("native process finished");
		// Quit();
	}

	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		mState.onCreateContextMenu(menu, v, menuInfo);
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
		case FlagCmd:
			int autoMenu = removeFromQue();
			int autoPickup = removeFromQue();
			String types = waitForLine();
			SetFlags(autoPickup, types, autoMenu);
		break;
		}
	}

	// ____________________________________________________________________________________
	private int discardUntil(int cmd0)
	{
		int cmd = 0;
		do
		{
			cmd = removeFromQue();
			handleSpecialCmds(cmd);
		}while(cmd != cmd0);
		return cmd;
	}

	// ____________________________________________________________________________________
	private int discardUntil(int cmd0, int cmd1)
	{
		int cmd = 0;
		do
		{
			cmd = removeFromQue();
			handleSpecialCmds(cmd);
		}while(cmd != cmd0 && cmd != cmd1);
		return cmd;
	}

	// ____________________________________________________________________________________
	private int receiveKeyCmd()
	{
		int key = '\033';

		if(discardUntil(KeyCmd) != KeyCmd)
			return key;

		key = removeFromQue();

		return key;
	}

	// ____________________________________________________________________________________
	private int receivePosKeyCmd(int lockMouse, int[] pos)
	{
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

		if(cmd == 0)
			return '\033';

		int key = removeFromQue();
		if(cmd == PosCmd)
		{
			pos[0] = removeFromQue(); // x
			pos[1] = removeFromQue(); // y
		}
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
	private void putString(final int wid, final int attr, final byte[] cmsg, final int append, final int hp, final int hpMax)
	{
		final String msg = CP437.decode(cmsg);
		if(wid == mMessageWid)
			Log.print(msg);

		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.putString(wid, attr, msg, append, hp, hpMax);
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
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.ynFunction(question, choices, def);
			}
		});
	}

	// ____________________________________________________________________________________
	private String getLine(final byte[] title, final int nMaxChars)
	{
		final String msg = CP437.decode(title);
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.getLine(msg, nMaxChars);
			}
		});
		return waitForLine();
	}

	// ____________________________________________________________________________________
	private String waitForLine()
	{
		if(discardUntil(LineCmd) != LineCmd)
			return "";

		StringBuilder builder = new StringBuilder();
		while(true)
		{
			int c = removeFromQue();
			if(c == '\n')
				break;
			builder.append((char)c);
		}

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

		if(bBlocking != 0)
			receiveKeyCmd();
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
	private void addMenu(final int wid, final int tile, final int id, final int acc, final int groupAcc, final int attr, final byte[] text, final int bSelected)
	{
		final String msg = CP437.decode(text);
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.addMenu(wid, tile, id, acc, groupAcc, attr, msg, bSelected);
			}
		});
	}

	// ____________________________________________________________________________________
	private void endMenu(final int wid, final byte[] prompt)
	{
		final String msg = CP437.decode(prompt);
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.endMenu(wid, msg);
			}
		});
	}

	// ____________________________________________________________________________________
	private int[] selectMenu(final int wid, final int how)
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.selectMenu(wid, how);
			}
		});
		return waitForSelect();
	}

	// ____________________________________________________________________________________
	private int[] waitForSelect()
	{
		if(discardUntil(SelectCmd) != SelectCmd)
			return null;

		int nItems = removeFromQue();
		if(nItems < 0)
			return null;

		int[] items = new int[nItems * 2];
		if(nItems > 0)
		{
			for(int i = 0; i < items.length;)
			{
				items[i++] = removeFromQue(); // id
				items[i++] = removeFromQue(); // count
			}
		}
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
	private void showPrevMessage()
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.showPrevMessage();
			}
		});
	}

	// ____________________________________________________________________________________
	private void showLog()
	{
		mHandler.post(new Runnable()
		{
			public void run()
			{
				mState.showLog();
			}
		});
	}

	// ____________________________________________________________________________________
	private native void RunNetHack(String path, String username, boolean bWizard);

	private native void SaveNetHackState();

	private native void SetFlags(int bAutoPickup, String pickupTypes, int bAutoMenu);

	// ____________________________________________________________________________________
	/*static
	{
		System.loadLibrary("nethack");
	}*/
}
