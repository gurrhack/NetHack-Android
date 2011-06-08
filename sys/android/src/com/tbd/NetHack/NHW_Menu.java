package com.tbd.NetHack;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class NHW_Menu implements NH_Window
{
	private Activity m_context;
	private NetHackIO m_io;
	private ArrayList<MenuItem> m_items;
	private String m_title;
	private Dialog m_dialog;
	private ListView m_listView;
	private TextView m_text;
	private SpannableStringBuilder m_builder;
	private Tileset m_tileset;
	private SelectMode m_how;
	private boolean m_bBlocking;

	// ____________________________________________________________________________________
	public enum SelectMode
	{
		PickNone, PickOne, PickMany;

		public static SelectMode FromInt(int i)
		{
			if(i == 2)
				return PickMany;
			if(i == 1)
				return PickOne;
			return PickNone;
		}
	}

	// ____________________________________________________________________________________
	public NHW_Menu(Activity context, NetHackIO io, Tileset tileset)
	{
		m_context = context;
		m_io = io;
		m_tileset = tileset;
		Clear();
	}

	// ____________________________________________________________________________________
	public void Clear()
	{
		m_items = new ArrayList<MenuItem>();
		m_builder = null;
	}

	// ____________________________________________________________________________________
	public void Show(boolean bBlocking)
	{
		if(m_builder != null && m_dialog == null)
		{
			CreateTextDlg();
			m_text.setText(m_builder.toString());
		}
		if(m_dialog != null)
			m_dialog.show();
		m_bBlocking = bBlocking;
	}

	// ____________________________________________________________________________________
	public void Hide()
	{
		if(m_dialog != null)
			m_dialog.hide();
	}

	// ____________________________________________________________________________________
	public void PrintString(final TextAttr attr, final String str)
	{
		if(m_builder == null)
		{
			m_builder = new SpannableStringBuilder(str);
		}
		else
		{
			m_builder.append('\n');
			m_builder.append(attr.Style(str));
		}
	}

	// ____________________________________________________________________________________
	public void StartMenu()
	{
		Clear();
	}

	// ____________________________________________________________________________________
	public void AddMenu(final int tile, final int ident, final int accelerator, final int groupacc, final TextAttr attr, final String str, final int preselected)
	{
		Log.print("add menu: " + str);
		if(str.length() == 0 && tile < 0)
			return;
		m_items.add(new MenuItem(tile, ident, accelerator, groupacc, attr, str, preselected));
	}

	// ____________________________________________________________________________________
	public void EndMenu(final String prompt)
	{
		Log.print(prompt);
		m_title = prompt;
	}

	// ____________________________________________________________________________________
	private void SendSelectNone()
	{
		// This prevents multiple clicks if OS is lagging
		if(m_dialog.isShowing())
		{
			m_io.SendSelectNoneCmd();
			m_dialog.hide();
			m_dialog.dismiss();
		}
	}

	// ____________________________________________________________________________________
	private void SendSelectOne(int id, int count)
	{
		if(m_dialog.isShowing())
		{
			m_io.SendSelectCmd(id, count);
			m_dialog.hide();
			m_dialog.dismiss();
		}
	}

	// ____________________________________________________________________________________
	private void SendSelectChecked()
	{
		if(m_dialog.isShowing())
		{
			ArrayList<MenuItem> items = new ArrayList<MenuItem>();
			for(MenuItem i : m_items)
				if(i.IsSelected())
					items.add(i);
			m_io.SendSelectCmd(items);
			m_dialog.hide();
			m_dialog.dismiss();
		}
	}

	// ____________________________________________________________________________________
	private void SendCancelSelect()
	{
		if(m_dialog.isShowing())
		{
			m_io.SendCancelSelectCmd();
			m_dialog.hide();
			m_dialog.dismiss();
		}
	}

	// ____________________________________________________________________________________
	private void ToggleItemAt(int itemPos)
	{
		if(m_how != SelectMode.PickMany)
			return;

		// Toggle the item itself
		if(itemPos >= 0 && itemPos < m_items.size())
		{
			MenuItem item = m_items.get(itemPos);
			if(item.IsHeader())
				return;
			item.Toggle();
		}

		// Toggle the checkbox if it's currently visible
		int listPos = itemPos - m_listView.getFirstVisiblePosition();
		if(listPos >= 0 && listPos < m_listView.getChildCount())
		{
			View view = m_listView.getChildAt(listPos);
			CheckBox chck = (CheckBox)view.findViewById(R.id.item_check);
			chck.toggle();
		}
	}

	// ____________________________________________________________________________________
	private boolean MenuSelect(char acc)
	{
		if(m_dialog.isShowing() && m_how != SelectMode.PickNone)
		{
			int i;
			for(i = 0; i < m_items.size(); i++)
			{
				MenuItem item = m_items.get(i);
				if(item.GetAcc() == acc && !item.IsHeader())
				{
					ToggleItemAt(i);
					if(m_how == SelectMode.PickOne)
						SendSelectOne(item.GetId(), -1);
					break;
				}
			}
			return i < m_items.size();
		}
		return false;
	}

	// ____________________________________________________________________________________
	public int GetNumSelected()
	{
		int n = 0;
		for(MenuItem i : m_items)
			if(i.IsSelected())
				n++;
		return n;
	}

	// ____________________________________________________________________________________
	public void CreateTextDlg()
	{
		LayoutInflater inflater = (LayoutInflater)m_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.dialog_text, null);
		m_text = (TextView)v.findViewById(R.id.text_view);

		v.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				TextClose();
			}
		});

		m_dialog = new Dialog(m_context, R.style.MenuNoTitle);
		m_dialog.setContentView(v);
		m_dialog.setCancelable(true);
		m_dialog.setCanceledOnTouchOutside(false);
		m_dialog.show();

		m_dialog.setOnCancelListener(new OnCancelListener()
		{
			public void onCancel(DialogInterface dialog)
			{
				TextClose();
			}
		});

		m_dialog.setOnKeyListener(new DialogInterface.OnKeyListener()
		{
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
			{
				if(event.getAction() != KeyEvent.ACTION_DOWN)
					return false;
				switch(keyCode)
				{
				case KeyEvent.KEYCODE_ENTER:
				case KeyEvent.KEYCODE_BACK:
				case KeyEvent.KEYCODE_SPACE:
					TextClose();
					return true;
				}
				return false;
			}
		});
	}

	// ____________________________________________________________________________________
	public void TextClose()
	{
		if(m_bBlocking)
			m_io.SendKeyCmd(' ');
		m_bBlocking = false;
		m_dialog.hide();
		m_dialog.dismiss();
	}

	// ____________________________________________________________________________________
	public boolean HandleKeyDown(int keyCode, KeyEvent event)
	{
		return false;
	}

	// ____________________________________________________________________________________
	public void SelectMenu(final SelectMode how)
	{
		m_how = how;
		View v = null;
		switch(how)
		{
		case PickNone:
			v = Util.Inflate(R.layout.dialog_menu0);
		break;

		case PickOne:
			GenerateAccelerators();
			v = Util.Inflate(R.layout.dialog_menu1);
		break;

		case PickMany:
			GenerateAccelerators();
			v = Util.Inflate(R.layout.dialog_menu3);
		break;
		}

		View btn = v.findViewById(R.id.btn_ok);
		if(btn != null)
			btn.setOnClickListener(new OnClickListener()
			{
				public void onClick(View view)
				{
					MenuOk();
				}
			});

		btn = v.findViewById(R.id.btn_cancel);
		if(btn != null)
			btn.setOnClickListener(new OnClickListener()
			{
				public void onClick(View view)
				{
					MenuCancel();
				}
			});

		m_listView = (ListView)v.findViewById(R.id.menu_list);
		m_listView.setAdapter(new MenuItemAdapter(m_context, R.layout.menu_item, m_items, m_tileset, how));

		m_listView.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				switch(how)
				{
				case PickNone:
					SendSelectNone();
				break;
				case PickOne:
					MenuItem item = m_items.get(position);
					if(!item.IsHeader())
						SendSelectOne(item.GetId(), -1);
				break;
				case PickMany:
					ToggleItemAt(position);
				break;
				}
			}
		});

		m_listView.setOnItemLongClickListener(new OnItemLongClickListener()
		{
			public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id)
			{
				final MenuItem item = m_items.get(position);
				if(item.GetMaxCount() < 2 || how != SelectMode.PickOne)
					return false;
				final AmountSelector selector = new AmountSelector(m_io, m_tileset, item.GetName().toString(), item.GetMaxCount(), item.GetTile());
				selector.setOnDismissListener(new OnDismissListener()
				{
					public void onDismiss(DialogInterface dialog)
					{
						int count = selector.GetAmount();
						if(count > 0)
							SendSelectOne(item.GetId(), count);
						else if(count == 0)
							SendCancelSelect();
					}
				});
				return true;
			}
		});

		m_listView.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if(event.getAction() != KeyEvent.ACTION_DOWN)
					return false;

				if(keyCode == KeyEvent.KEYCODE_ENTER)
					MenuOk();
				else if(keyCode == KeyEvent.KEYCODE_BACK)
					MenuCancel();
				else if(keyCode == KeyEvent.KEYCODE_SPACE)
					ToggleItemAt(m_listView.getSelectedItemPosition());
				else
					return MenuSelect((char)event.getUnicodeChar());
				return true;
			}
		});

		if(m_title.length() > 0)
		{
			m_dialog = new Dialog(m_context);
			m_dialog.setTitle(m_title);
			// titles are apparently single lines. fix it
			TextView title = (TextView)m_dialog.findViewById(android.R.id.title);
			title.setSingleLine(false);
			title.setMaxLines(2);
		}
		else
			m_dialog = new Dialog(m_context, R.style.MenuNoTitle);
		
		m_dialog.setContentView(v);
		m_dialog.setCancelable(true);
		m_dialog.setCanceledOnTouchOutside(false);
		m_dialog.setOnCancelListener(new OnCancelListener()
		{
			public void onCancel(DialogInterface dialog)
			{
				MenuCancel();
			}
		});
		m_dialog.show();
	}

	// ____________________________________________________________________________________
	private void GenerateAccelerators()
	{
		for(MenuItem i : m_items)
			if(i.HasAcc())
				return;
		char acc = 'a';
		for(MenuItem i : m_items)
		{
			if(!i.IsHeader() && acc != 0)
			{
				i.SetAcc(acc);
				acc++;
				if(acc == 'z' + 1)
					acc = 'A';
				else if(acc == 'Z' + 1)
					acc = 0;
			}
		}
	}

	// ____________________________________________________________________________________
	private void MenuOk()
	{
		switch(m_how)
		{
		case PickNone:
			SendSelectNone();
		break;
		case PickOne:
			int itemPos = m_listView.getSelectedItemPosition();
			if(itemPos >= 0 && itemPos < m_items.size())
				SendSelectOne(m_items.get(itemPos).GetId(), -1);
		break;
		case PickMany:
			SendSelectChecked();
		break;
		}
	}

	// ____________________________________________________________________________________
	private void MenuCancel()
	{
		SendCancelSelect();
	}
}
