package com.tbd.NetHack;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tbd.NetHack.Input.Modifier;

public class NHW_Menu implements NH_Window
{
	private NetHackIO mIO;
	private ArrayList<MenuItem> mItems;
	private String mTitle;
	private SpannableStringBuilder mBuilder;
	private Tileset mTileset;
	private boolean mIsBlocking;
	private UI mUI;
	private boolean mIsVisible;

	private enum Type
	{
		None,
		Menu,
		Text
	}

	private Type mType;
	private SelectMode mHow;
	private int mWid;

	// ____________________________________________________________________________________
	public enum SelectMode
	{
		PickNone, PickOne, PickMany;

		public static SelectMode fromInt(int i)
		{
			if(i == 2)
				return PickMany;
			if(i == 1)
				return PickOne;
			return PickNone;
		}
	}

	// ____________________________________________________________________________________
	public NHW_Menu(int wid, Activity context, NetHackIO io, Tileset tileset)
	{
		mWid = wid;
		mIO = io;
		mTileset = tileset;
		mType = Type.None;
		setContext(context);
	}

	// ____________________________________________________________________________________
	public String getTitle()
	{
		return mTitle;
	}
	
	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		mUI = new UI(context);
		if(mIsVisible)
			show(mIsBlocking);
		else
			hide();
		if(mType == Type.Text)
			mUI.createTextDlg();
		else if(mType == Type.Menu)
			mUI.createMenu(mHow);
	}

	// ____________________________________________________________________________________
	public void clear()
	{
		throw new UnsupportedOperationException();
	}

	// ____________________________________________________________________________________
	public void show(boolean bBlocking)
	{
		mIsVisible = true;
		if(mBuilder != null && mType == Type.None)
		{
			mType = Type.Text;
			mUI.createTextDlg();
		}
		mUI.showInternal();
		mIsBlocking = bBlocking;
	}

	// ____________________________________________________________________________________
	public void hide()
	{
		mIsVisible = false;
		mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	public int id()
	{
		return mWid;
	}
	
	// ____________________________________________________________________________________
	public boolean isBlocking()
	{
		return mIsBlocking;
	}

	// ____________________________________________________________________________________
	private void close()
	{
		if(mIsBlocking)
			mIO.sendKeyCmd(' ');
		mUI.closeInternal();
		mIsBlocking = false;
		mItems = null;
		mBuilder = null;
		mType = Type.None;
	}

	// ____________________________________________________________________________________
	public void printString(final TextAttr attr, final String str, int append)
	{
		if(mBuilder == null)
		{
			mBuilder = new SpannableStringBuilder(str);
			mItems = null;
		}
		else
		{
			mBuilder.append('\n');
			mBuilder.append(attr.style(str));
		}
	}

	// ____________________________________________________________________________________
	public void startMenu()
	{
		mItems = new ArrayList<MenuItem>(100);
		mBuilder = null;
	}

	// ____________________________________________________________________________________
	public void addMenu(final int tile, final int ident, final int accelerator, final int groupacc, final TextAttr attr, final String str, final int preselected)
	{
		if(str.length() == 0 && tile < 0)
			return;
		mItems.add(new MenuItem(tile, ident, accelerator, groupacc, attr, str, preselected));
	}

	// ____________________________________________________________________________________
	public void endMenu(final String prompt)
	{
		mTitle = prompt;
	}

	// ____________________________________________________________________________________
	public int getNumSelected()
	{
		int n = 0;
		for(MenuItem i : mItems)
			if(i.isSelected())
				n++;
		return n;
	}

	// ____________________________________________________________________________________
	public int handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		return mUI.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);
	}

	// ____________________________________________________________________________________
	private void generateAccelerators()
	{
		for(MenuItem i : mItems)
			if(i.hasAcc())
				return;
		char acc = 'a';
		for(MenuItem i : mItems)
		{
			if(!i.isHeader() && i.isSelectable() && acc != 0)
			{
				i.setAcc(acc);
				acc++;
				if(acc == 'z' + 1)
					acc = 'A';
				else if(acc == 'Z' + 1)
					acc = 0;
			}
		}
	}

	// ____________________________________________________________________________________
	public void selectMenu(SelectMode how)
	{
		mType = Type.Menu;
		mUI.createMenu(how);
		show(false);
	}

	// ____________________________________________________________________________________ //
	// 																						//
	// ____________________________________________________________________________________ //
	private class UI implements AmountSelector.Listener
	{
		private Activity mContext;
		private View mRoot;
		
		private ListView mListView;
		private AmountSelector mAmountSelector;

		public UI(Activity context)
		{
			mContext = context;
		}

		public int handleKeyDown(char ch, int nhKey, int keyCode, Set<Modifier> modifiers, int repeatCount, boolean bSoftInput)
		{
			if(mAmountSelector != null)
				return mAmountSelector.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);
			
			if(!isShowing() || keyCode < 0)
				return 0;

			if(mType == Type.Text)
			{
				switch(keyCode)
				{
				case 111 /*KeyEvent.KEYCODE_ESC*/:
				case KeyEvent.KEYCODE_ENTER:
				case KeyEvent.KEYCODE_BACK:
					closeInternal();
				break;

				case KeyEvent.KEYCODE_SPACE:
					((ScrollView)mRoot.findViewById(R.id.scrollview)).pageScroll(ScrollView.FOCUS_DOWN);
				break;
				
				default:
					return 2;
				}
				return 1;
			}
			if(mType == Type.Menu)
			{
				// mListView.onKeyDown(keyCode, event);
				switch(keyCode)
				{
				case 111: // KeyEvent.KEYCODE_ESC
				case KeyEvent.KEYCODE_BACK:
					sendCancelSelect();
				break;

				case KeyEvent.KEYCODE_ENTER:
					if(bSoftInput)
						menuOk();
					else
						return 2;// let system handle
				break;
					
				case KeyEvent.KEYCODE_SPACE:
					if(bSoftInput)
					{						
						if(mHow == SelectMode.PickNone)
							menuOk();
						else
							toggleItemAt(mListView.getSelectedItemPosition());
					}
					else
						return 2;// let system handle
				break;

				default:
					if(mHow == SelectMode.PickNone)
					{
						if(getAccelerator(ch) >= 0)
						{
							menuOk();
							return 1;
						}
						return 2;// let system handle
					}
					else if(menuSelect(ch))
						return 1;
					return 2;
				}
				return 1;
			}
			return 0;
		}

		// ____________________________________________________________________________________
		public void onDismissCount(MenuItem item, int amount)
		{
			mAmountSelector = null;
			showInternal();
			if(mHow == SelectMode.PickOne)
			{
				if(amount > 0)
					sendSelectOne(item.getId(), amount);
				else if(amount == 0)
					sendCancelSelect();
			}
			else
			{
				if(amount > 0 && !item.isSelected() || amount == 0 && item.isSelected())
					toggleItemAt(mItems.indexOf(item));
				item.setCount(amount);
				((MenuItemAdapter)mListView.getAdapter()).notifyDataSetChanged();
			}
		}

		// ____________________________________________________________________________________
		public void showInternal()
		{
			if(mRoot != null)
			{
				mRoot.setVisibility(View.VISIBLE);
				mRoot.requestFocus();
			}
		}

		// ____________________________________________________________________________________
		public void hideInternal()
		{
			if(mRoot != null)
				mRoot.setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		public void closeInternal()
		{
			if(isShowing())
			{
				mRoot.setVisibility(View.GONE);
				((ViewGroup)mRoot.getParent()).removeView(mRoot);
				mRoot = null;
				close();
			}
		}

		// ____________________________________________________________________________________
		public boolean isShowing()
		{
			return mRoot != null && mRoot.getVisibility() == View.VISIBLE;
		}

		// ____________________________________________________________________________________
		private void sendSelectNone()
		{
			// This prevents multiple clicks if OS is lagging
			if(isShowing())
			{
				mIO.sendSelectNoneCmd();
				hideInternal();
			}
		}

		// ____________________________________________________________________________________
		private void sendSelectOne(int id, int count)
		{
			if(isShowing())
			{
				mIO.sendSelectCmd(id, count);
				hideInternal();
			}
		}

		// ____________________________________________________________________________________
		private void sendSelectChecked()
		{
			if(isShowing())
			{
				ArrayList<MenuItem> items = new ArrayList<MenuItem>();
				for(MenuItem i : mItems)
					if(i.isSelected())
						items.add(i);
				mIO.sendSelectCmd(items);
				hideInternal();
			}
		}

		// ____________________________________________________________________________________
		private void sendCancelSelect()
		{
			if(isShowing())
			{
				mIO.sendCancelSelectCmd();
				hideInternal();
			}
		}

		// ____________________________________________________________________________________
		private void toggleItemAt(int itemPos)
		{
			if(mHow != SelectMode.PickMany)
				return;

			// Toggle the item itself
			if(itemPos >= 0 && itemPos < mItems.size())
			{
				MenuItem item = mItems.get(itemPos);
				if(item.isHeader() || !item.isSelectable())
					return;
				item.toggle();
				if(item.getCount() != -1)
				{
					item.setCount(-1);
					((MenuItemAdapter)mListView.getAdapter()).notifyDataSetChanged();
				}
			}

			// Toggle the checkbox if it's currently visible
			int listPos = itemPos - mListView.getFirstVisiblePosition();
			if(listPos >= 0 && listPos < mListView.getChildCount())
			{
				View view = mListView.getChildAt(listPos);
				CheckBox chck = (CheckBox)view.findViewById(R.id.item_check);
				chck.toggle();
			}
		}

		// ____________________________________________________________________________________
		private int getAccelerator(char acc)
		{
			for(int i = 0; i < mItems.size(); i++)
			{
				MenuItem item = mItems.get(i);
				if(item.getAcc() == acc && !item.isHeader() && item.isSelectable())
					return i;
			}
			return -1;
		}

		// ____________________________________________________________________________________
		private boolean menuSelect(char acc)
		{
			if(acc == 0)
				return false;
			if(isShowing() && mHow != SelectMode.PickNone)
			{
				boolean bRet = false;
				int i = getAccelerator(acc);
				if(i >= 0)
				{
					MenuItem item = mItems.get(i);
					toggleItemAt(i);
					if(mHow == SelectMode.PickOne)
						sendSelectOne(item.getId(), -1);
					bRet = true;
				}
				else if(mHow == SelectMode.PickMany)
				{
					// do group acc if none was found
					for(i = 0; i < mItems.size(); i++)
					{
						MenuItem item = mItems.get(i);
						if(item.getGroupAcc() == acc && !item.isHeader() && item.isSelectable())
						{
							toggleItemAt(i);
							bRet = true;
						}
					}
				}
				return bRet;
			}
			return false;
		}

		// ____________________________________________________________________________________
		private void selectAll()
		{
			if(isShowing() && mHow == SelectMode.PickMany)
			{
				for(int i = 0; i < mItems.size(); i++)
				{
					MenuItem item = mItems.get(i);
					if(!item.isHeader() && item.isSelectable() && !item.isSelected())
						toggleItemAt(i);
				}
			}
		}

		// ____________________________________________________________________________________
		private void clearAll()
		{
			if(isShowing() && mHow == SelectMode.PickMany)
			{
				for(int i = 0; i < mItems.size(); i++)
				{
					MenuItem item = mItems.get(i);
					if(!item.isHeader() && item.isSelectable() && item.isSelected())
						toggleItemAt(i);
				}
			}
		}

		// ____________________________________________________________________________________
		public void createTextDlg()
		{
			Log.print("create text dlg");

			mRoot = Util.inflate(mContext, R.layout.dialog_text, R.id.dlg_frame);
			((TextView)mRoot.findViewById(R.id.text_view)).setText(mBuilder);

			View btn = mRoot.findViewById(R.id.btn_ok);
			btn.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					closeInternal();
				}
			});

			mRoot.setOnKeyListener(new OnKeyListener()
			{
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					Log.print("MENU ONKEY");
/*					if(event.getAction() != KeyEvent.ACTION_DOWN)
						return false;

					switch(keyCode)
					{
					case KeyEvent.KEYCODE_ENTER:
					case KeyEvent.KEYCODE_BACK:
					case KeyEvent.KEYCODE_SPACE:
						closeInternal();
						return true;
					case KeyEvent.KEYCODE_VOLUME_UP:
					case KeyEvent.KEYCODE_VOLUME_DOWN:
						return true;
					}*/
					return false;
				}
			});

			btn.requestFocus();
			btn.requestFocusFromTouch();
		}

		// ____________________________________________________________________________________
		public void createMenu(SelectMode how)
		{
			if(mRoot == null || mHow != how)
				inflateLayout(how);

			mListView.setAdapter(new MenuItemAdapter(mContext, R.layout.menu_item, mItems, mTileset, mHow));

			mRoot.requestFocus();

			if(mTitle.length() > 0)
			{
				((TextView)mRoot.findViewById(R.id.title)).setVisibility(View.VISIBLE);
				((TextView)mRoot.findViewById(R.id.title)).setText(mTitle);
			}
			else
				((TextView)mRoot.findViewById(R.id.title)).setVisibility(View.GONE);
		}
		
		// ____________________________________________________________________________________
		private void inflateLayout(SelectMode how)
		{
			mHow = how;
			switch(mHow)
			{
			case PickNone:
				mRoot = Util.inflate(mContext, R.layout.dialog_menu1, R.id.dlg_frame);
			break;

			case PickOne:
				generateAccelerators();
				mRoot = Util.inflate(mContext, R.layout.dialog_menu1, R.id.dlg_frame);
			break;

			case PickMany:
				generateAccelerators();
				mRoot = Util.inflate(mContext, R.layout.dialog_menu3, R.id.dlg_frame);
			break;
			}

			final Button selectAllBtn = (Button)mRoot.findViewById(R.id.btn_all);
			if(selectAllBtn != null)
				selectAllBtn.setOnClickListener(new OnClickListener()
				{
					public void onClick(View view)
					{
						if("Clear all".equals(selectAllBtn.getText().toString()))
						{
							clearAll();
							selectAllBtn.setText("Select all");
						}
						else
						{
							selectAll();
							selectAllBtn.setText("Clear all");
						}

					}
				});

			View btn = mRoot.findViewById(R.id.btn_ok);
			if(btn != null)
				btn.setOnClickListener(new OnClickListener()
				{
					public void onClick(View view)
					{
						menuOk();
					}
				});

			btn = mRoot.findViewById(R.id.btn_cancel);
			if(btn != null)
				btn.setOnClickListener(new OnClickListener()
				{
					public void onClick(View view)
					{
						sendCancelSelect();
					}
				});

			mListView = (ListView)mRoot.findViewById(R.id.menu_list);

			mListView.setOnItemClickListener(new OnItemClickListener()
			{
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					switch(mHow)
					{
					case PickNone:
						sendSelectNone();
					break;
					case PickOne:
						MenuItem item = mItems.get(position);
						if(!item.isHeader() && item.isSelectable())
							sendSelectOne(item.getId(), -1);
					break;
					case PickMany:
						toggleItemAt(position);
					break;
					}
				}
			});

			mListView.setOnItemLongClickListener(new OnItemLongClickListener()
			{
				public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id)
				{
					final MenuItem item = mItems.get(position);
					if(!item.isSelectable())
						return false;
					if(item.getMaxCount() < 2 || mHow == SelectMode.PickNone)
						return false;
					// hideInternal();
					mAmountSelector = new AmountSelector(UI.this, mContext, mIO, mTileset, item);
					return true;
				}
			});

			mListView.setOnKeyListener(new OnKeyListener()
			{
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					Log.print("MENU ONKEY");
/*					if(event.getAction() != KeyEvent.ACTION_DOWN)
						return false;

					switch(keyCode)
					{
					case KeyEvent.KEYCODE_ENTER:
						menuOk();
					break;

					case KeyEvent.KEYCODE_BACK:
						sendCancelSelect();
					break;

					case KeyEvent.KEYCODE_SPACE:
						if(mHow == SelectMode.PickNone)
							menuOk();
						else
							toggleItemAt(mListView.getSelectedItemPosition());
					break;

					default:
						char ch = (char)event.getUnicodeChar();
						if(mHow == SelectMode.PickNone)
						{
							if(getAccelerator(ch) >= 0)
							{
								menuOk();
								return true;
							}
							return false;
						}
						else
							return menuSelect(ch);
					}

					return true;*/
					return false;
				}
			});
		}

		// ____________________________________________________________________________________
		private void menuOk()
		{
			switch(mHow)
			{
			case PickNone:
				sendSelectNone();
			break;
			case PickOne:
				int itemPos = mListView.getSelectedItemPosition();
				if(itemPos >= 0 && itemPos < mItems.size())
					sendSelectOne(mItems.get(itemPos).getId(), -1);
			break;
			case PickMany:
				sendSelectChecked();
			break;
			}
		}
	}

}
