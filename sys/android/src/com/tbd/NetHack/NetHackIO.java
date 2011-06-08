package com.tbd.NetHack;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.View;

public class NetHackIO implements Runnable
{
	private Handler m_handler;
	private Thread nhThread;
	private NHW_Message m_message;
	private NHW_Status m_status;
	private NHW_Map m_map;
	private DPadOverlay m_dPad;
	private boolean m_bdPadVisible;
	private HashMap<Integer, NH_Window> m_windows;
	private int m_nextWinId;
	private ConcurrentLinkedQueue<Integer> m_cmdQue;
	private Tileset m_tileset;
	private String m_dataDir;
	private Thread m_nhThread;

	// ____________________________________________________________________________________
	public NetHackIO(Tileset tileset, String dataDir)
	{
		m_dataDir = dataDir;
		m_cmdQue = new ConcurrentLinkedQueue<Integer>();
		m_windows = new HashMap<Integer, NH_Window>();
		m_nextWinId = 1;
		m_tileset = tileset;
		m_tileset.SetOnChangedListener(OnTilesetChanged);
		NetHack.get().setContentView(R.layout.mainwindow);

		m_message = new NHW_Message(NetHack.get(), this);
		m_message.Clear();

		m_status = new NHW_Status(NetHack.get(), this);
		m_status.Clear();

		m_dPad = new DPadOverlay(this);

		m_map = (NHW_Map)NetHack.get().findViewById(R.id.nh_map);
		m_map.Init(NetHack.get(), this, m_tileset, m_dPad, m_status);

		m_handler = new Handler();

		m_nhThread = new Thread(this);
		m_nhThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler()
		{
			public void uncaughtException(Thread thread, Throwable ex)
			{
				Log.print("native process threw exception");
			}
		});
		m_nhThread.start();
		PreferencesUpdated();
	}

	// ____________________________________________________________________________________
	public void onConfigurationChanged(Configuration newConfig)
	{
		m_map.onConfigurationChanged(newConfig);
	}

	// ____________________________________________________________________________________
	public void SendAutoPickup(boolean bAutoPickup, String pickupTypes)
	{
		m_cmdQue.add(AutoPickupCmd);
		m_cmdQue.add(bAutoPickup ? 1 : 0);
		SendLineCmd(pickupTypes.trim());
	}

	// ____________________________________________________________________________________
	public void SaveState()
	{
		m_cmdQue.add(SaveStateCmd);
		// give it some time
		try
		{
			Thread.sleep(40);
		}
		catch(InterruptedException e)
		{
		}
	}

	// ____________________________________________________________________________________
	public void SaveAndExit()
	{
		m_cmdQue.add(SaveAndExitCmd);
		/*SendKeyCmd('\033');
		// give it some time
		try
		{
			Thread.sleep(40);
		}
		catch(InterruptedException e)
		{
		}*/
	}

	// ____________________________________________________________________________________
	Tileset.OnChangedListener OnTilesetChanged = new Tileset.OnChangedListener()
	{
		public void OnChanged()
		{
			m_map.ResetZoom();
		}
	};

	// ____________________________________________________________________________________
	static
	{
		System.loadLibrary("nethack");
	}

	private native void RunNetHack(String path, String username, boolean bWizard);

	private native void SaveNetHackState();

	private native void SaveNetHackAndExit();

	private native void SetAutoPickup(int bAutoPickup, String pickupTypes);

	// ____________________________________________________________________________________
	public void run()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NetHack.get());
		String username = prefs.getString("username", "").trim();
		boolean bWizard = prefs.getBoolean("wizard", false);
		RunNetHack(m_dataDir, username, bWizard);
		Log.print("native process finished");
		Quit();
	}

	// ____________________________________________________________________________________
	public void PreferencesUpdated()
	{
		m_map.PreferencesUpdated();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(NetHack.get());

		String pickupTypes = prefs.getString("autoPickupTypes", "");

		// take care of special characters
		pickupTypes = pickupTypes.replace('\'', '`');
		pickupTypes = pickupTypes.replace('\u00B4', '`');
		pickupTypes = pickupTypes.replace('\u2018', '`');
		pickupTypes = pickupTypes.replace('\u2019', '`');
		pickupTypes = pickupTypes.replace('\u201C', '"');
		pickupTypes = pickupTypes.replace('\u201D', '"');

		boolean bAutoPickup = prefs.getBoolean("autoPickup", false);
		SendAutoPickup(bAutoPickup, pickupTypes);
	}

	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		m_map.onCreateContextMenu(menu, v, menuInfo);
	}

	// ____________________________________________________________________________________
	public void onContextItemSelected(android.view.MenuItem item)
	{
		m_map.onContextItemSelected(item);
	}

	// ____________________________________________________________________________________
	public boolean HandleKeyDown(int keyCode, KeyEvent event)
	{
		if(m_dPad.HandleKeyDown(keyCode, event))
			return true;
		for(NH_Window w : m_windows.values())
		{
			if(w != m_map && w.HandleKeyDown(keyCode, event))
				return true;
		}
		return m_map.HandleKeyDown(keyCode, event);
	}

	// ____________________________________________________________________________________
	public boolean HandleKeyUp(int keyCode, KeyEvent event)
	{
		return m_map.HandleKeyUp(keyCode);
	}

	// ____________________________________________________________________________________
	int VerifyData(int d)
	{
		if(DEBUG.IsOn() && d != 0 && (d & DataMask) == 0)
			throw new IllegalArgumentException();
		return d;
	}

	// ------------------------------------------------------------------------------------
	// Send commands called from UI thread
	// ------------------------------------------------------------------------------------
	private static final int KeyCmd = 0x80000000;
	private static final int PosCmd = 0x90000000;
	private static final int LineCmd = 0xa0000000;
	private static final int SelectCmd = 0xb0000000;
	private static final int SaveStateCmd = 0xc0000000;
	private static final int SaveAndExitCmd = 0xd0000000;
	private static final int AutoPickupCmd = 0xe0000000;
	private static final int CmdMask = 0xf0000000;
	private static final int DataMask = 0x0fffffff;

	// ____________________________________________________________________________________
	public void SendKeyCmd(char key)
	{
		m_map.HideDPad();
		m_cmdQue.add(KeyCmd);
		m_cmdQue.add((int)key);
	}

	// ____________________________________________________________________________________
	public void SendPosCmd(int x, int y)
	{
		m_map.HideDPad();
		m_cmdQue.add(PosCmd);
		m_cmdQue.add(VerifyData(x));
		m_cmdQue.add(VerifyData(y));
	}

	// ____________________________________________________________________________________
	public void SendLineCmd(String str)
	{
		Log.print("send line: " + str);
		m_cmdQue.add(LineCmd);
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			if(c == '\n')
				break;
			if(c < 0xff)
				m_cmdQue.add((int)c);
		}
		m_cmdQue.add((int)'\n');
	}

	// ____________________________________________________________________________________
	public void SendSelectCmd(int id, int count)
	{
		m_cmdQue.add(SelectCmd);
		m_cmdQue.add(1);
		m_cmdQue.add(VerifyData(id));
		m_cmdQue.add(VerifyData(count));
	}

	public void SendSelectCmd(ArrayList<MenuItem> items)
	{
		m_cmdQue.add(SelectCmd);
		m_cmdQue.add(VerifyData(items.size()));
		for(MenuItem i : items)
		{
			m_cmdQue.add(VerifyData(i.GetId()));
			m_cmdQue.add(VerifyData(-1/* i.GetCount() */));
		}
	}

	public void SendSelectNoneCmd()
	{
		m_cmdQue.add(SelectCmd);
		m_cmdQue.add(0);
	}

	public void SendCancelSelectCmd()
	{
		m_cmdQue.add(SelectCmd);
		m_cmdQue.add(-1);
	}

	// ------------------------------------------------------------------------------------
	// Receive commands called from nethack thread
	// ------------------------------------------------------------------------------------

	// ____________________________________________________________________________________
	private int RemoveFromQue()
	{
		Integer c = m_cmdQue.poll();
		while(c == null)
		{
			try
			{
				Thread.sleep(50);
			}
			catch(InterruptedException e)
			{
			}
			c = m_cmdQue.poll();
		}
		return c;
	}

	// ____________________________________________________________________________________
	private void HandleSpecialCmds(int cmd)
	{
		switch(cmd)
		{
		case SaveStateCmd:
			SaveNetHackState();
		break;
		case SaveAndExitCmd:
			SaveNetHackAndExit();
		break;
		case AutoPickupCmd:
			int bAuto = RemoveFromQue();
			String types = WaitForLine();
			SetAutoPickup(bAuto, types);
		break;
		}
	}

	// ____________________________________________________________________________________
	private int DiscardUntil(int cmd0)
	{
		int cmd = 0;
		do
		{
			cmd = RemoveFromQue();
			HandleSpecialCmds(cmd);
		} while(cmd != cmd0);
		return cmd;
	}

	// ____________________________________________________________________________________
	private int DiscardUntil(int cmd0, int cmd1)
	{
		int cmd = 0;
		do
		{
			cmd = RemoveFromQue();
			HandleSpecialCmds(cmd);
		} while(cmd != cmd0 && cmd != cmd1);
		return cmd;
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private int ReceiveKeyCmd()
	{
		int key = '\033';

		if(DiscardUntil(KeyCmd) != KeyCmd)
			return key;

		key = RemoveFromQue();

		return key;
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private int ReceivePosKeyCmd(int lockMouse, int[] pos)
	{
		if(lockMouse != 0)
		{
			m_handler.post(new Runnable()
			{
				public void run()
				{
					m_map.LockMouse();
				}
			});
		}

		int cmd = DiscardUntil(KeyCmd, PosCmd);

		if(cmd == 0)
			return '\033';

		int key = 0;
		if(cmd == KeyCmd)
		{
			key = RemoveFromQue();
		}
		else
		{
			pos[0] = RemoveFromQue(); // x
			pos[1] = RemoveFromQue(); // y
		}
		return key;
	}

	// ------------------------------------------------------------------------------------
	// Functions called by nethack thread to schedule an operation on UI thread
	// ------------------------------------------------------------------------------------

	@SuppressWarnings("unused")
	private void DebugLog(final String msg)
	{
		Log.print(msg);
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void SetCursorPos(final int wid, final int x, final int y)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				NH_Window wnd = m_windows.get(wid);
				if(wnd == m_map)
					m_map.SetCursorPos(x, y);
				else if(wnd == m_status)
					m_status.SetRow(y); // cursor x position not supported in status window
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void PutString(final int wid, final int attr, final String msg, final int hp, final int hpMax)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				NH_Window wnd = m_windows.get(wid);
				if(wnd == null)
					m_message.PrintString(TextAttr.FromNative(attr), msg);
				else
					wnd.PrintString(TextAttr.FromNative(attr), msg);
				if(m_map != null)
					m_map.SetHealthLevel(hp, hpMax);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void RawPrint(final int attr, final String msg)
	{
		Log.print(msg);
		m_handler.post(new Runnable()
		{
			public void run()
			{
				m_message.PrintString(TextAttr.FromNative(attr), msg);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void PrintTile(final int wid, final int x, final int y, final int tile, final int ch, final int col, final int special)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				m_map.PrintTile(x, y, tile, ch, col, special);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void YNFunction(final String question, final byte[] choices, final int def)
	{
		m_handler.post(new NH_Question(NetHack.get(), this, question, choices, def));
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private String GetLine(final String title, final int nMaxChars)
	{
		m_handler.post(new NH_GetLine(this, title, nMaxChars));
		return WaitForLine();
	}

	// ____________________________________________________________________________________
	private String WaitForLine()
	{
		if(DiscardUntil(LineCmd) != LineCmd)
			return "";

		StringBuilder builder = new StringBuilder();
		while(true)
		{
			int c = RemoveFromQue();
			if(c == '\n')
				break;
			builder.append((char)c);
		}

		return builder.toString();
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void UpdatePositionbar(byte[] sym)
	{
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void Bell()
	{
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private int DoPrevMessage()
	{
		return 0;
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private int GetExtCmd()
	{
		return 0;
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void DelayOutput()
	{
		// sleep on UI thread?
		try
		{
			Thread.sleep(50);
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	// ____________________________________________________________________________________
	// @SuppressWarnings("unused")
	private int CreateWindow(final int type)
	{
		final int wid = m_nextWinId++;
		final NetHackIO io = this;
		m_handler.post(new Runnable()
		{
			public void run()
			{
				switch(type)
				{
				case 1: // #define NHW_MESSAGE 1
					m_windows.put(wid, m_message);
				break;

				case 2: // #define NHW_STATUS 2
					m_windows.put(wid, m_status);
				break;

				case 3: // #define NHW_MAP 3
					m_windows.put(wid, m_map);
				break;

				case 4: // #define NHW_MENU 4
					m_windows.put(wid, new NHW_Menu(NetHack.get(), io, m_tileset));
				break;

				case 5: // #define NHW_TEXT 5
					m_windows.put(wid, new NHW_Text(NetHack.get(), io));
				break;
				}
			}
		});
		return wid;
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void DisplayWindow(final int wid, final int bBlocking)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				m_windows.get(wid).Show(bBlocking != 0);
			}
		});

		if(bBlocking != 0)
			ReceiveKeyCmd();
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void ClearWindow(final int wid, final int isRogueLevel)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				NH_Window wnd = m_windows.get(wid);
				wnd.Clear();
				if(wnd == m_map)
					m_map.SetRogueLevel(isRogueLevel != 0);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void DestroyWindow(final int wid)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				// better hide it before we remove it. never know when the GC decides to kick in
				m_windows.get(wid).Hide();
				m_windows.remove(wid);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void StartMenu(final int wid)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				((NHW_Menu)m_windows.get(wid)).StartMenu();
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void AddMenu(final int wid, final int tile, final int id, final int acc, final int groupAcc, final int attr, final String text, final int bSelected)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				((NHW_Menu)m_windows.get(wid)).AddMenu(tile, id, acc, groupAcc, TextAttr.FromNative(attr), text, bSelected);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void EndMenu(final int wid, final String prompt)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				((NHW_Menu)m_windows.get(wid)).EndMenu(prompt);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private int[] SelectMenu(final int wid, final int how)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				((NHW_Menu)m_windows.get(wid)).SelectMenu(NHW_Menu.SelectMode.FromInt(how));
			}
		});
		return WaitForSelect();
	}

	// ____________________________________________________________________________________
	private int[] WaitForSelect()
	{
		if(DiscardUntil(SelectCmd) != SelectCmd)
			return null;

		int nItems = RemoveFromQue();
		if(nItems < 0)
			return null;

		int[] items = new int[nItems * 2];
		if(nItems > 0)
		{
			for(int i = 0; i < items.length;)
			{
				items[i++] = RemoveFromQue(); // id
				items[i++] = RemoveFromQue(); // count
			}
		}
		return items;
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void UpdateInventory()
	{
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void Cliparound(final int x, final int y, final int playerX, final int playerY)
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				m_map.Cliparound(x, y, playerX, playerY);
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void AskDirection()
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				m_map.ShowDPad();
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void ShowPrevMessage()
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				m_message.ShowPrev();
			}
		});
	}

	// ____________________________________________________________________________________
	@SuppressWarnings("unused")
	private void ShowLog()
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				m_message.ShowLog();
			}
		});
	}

	// ____________________________________________________________________________________
	private void Quit()
	{
		m_handler.post(new Runnable()
		{
			public void run()
			{
				NetHack.get().finish();
			}
		});
	}
}
