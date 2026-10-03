package com.tbd.forkfront;

import java.util.ArrayList;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.tbd.forkfront.Input.Modifier;

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
	private MenuSelectMode mHow;
	private int mWid;
	private int mKeyboardCount;

	// ____________________________________________________________________________________
	public NHW_Menu(int wid, Activity context, NetHackIO io, Tileset tileset)
	{
		mWid = wid;
		mIO = io;
		mTileset = tileset;
		mType = Type.None;
		mKeyboardCount = -1;
		setContext(context);
	}

	// ____________________________________________________________________________________
	@Override
	public String getTitle()
	{
		return mTitle;
	}
	
	// ____________________________________________________________________________________
	@Override
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
	@Override
	public void clear()
	{
		throw new UnsupportedOperationException();
	}

	// ____________________________________________________________________________________
	@Override
	public void show(boolean bBlocking)
	{
		mIsVisible = true;
		if(mBuilder != null && mType == Type.None)
		{
			mType = Type.Text;
			mUI.createTextDlg();
		}
		mKeyboardCount = -1;
		mUI.showInternal();
		mIsBlocking = bBlocking;
	}

	// ____________________________________________________________________________________
	private void hide()
	{
		mIsVisible = false;
		mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	@Override
	public void destroy()
	{
		mIsVisible = false;
		mUI.closeInternal();
	}

	// ____________________________________________________________________________________
	@Override
	public int id()
	{
		return mWid;
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
		mKeyboardCount = -1;
	}

	// ____________________________________________________________________________________
	@Override
	public void printString(final int attr, final String str, int append, int color)
	{
		if(mBuilder == null)
		{
			mBuilder = new SpannableStringBuilder(str);
			mItems = null;
		}
		else
		{
			mBuilder.append('\n');
			mBuilder.append(TextAttr.style(str, attr));
		}
	}

	// ____________________________________________________________________________________
	@Override
	public void setCursorPos(int x, int y) {
	}

	// ____________________________________________________________________________________
	public void startMenu()
	{
		mItems = new ArrayList<MenuItem>(100);
		mBuilder = null;
	}

	// ____________________________________________________________________________________
	public void addMenu(int tile, int ident, int accelerator, int groupacc, int attr, String str, int preselected, int color)
	{
		if(str.length() == 0 && tile < 0)
			return;
		// start_menu is not always called
		if(mItems == null)
			startMenu();
		mItems.add(new MenuItem(tile, ident, accelerator, groupacc, attr, str, preselected, color));
	}

	// ____________________________________________________________________________________
	public void endMenu(String prompt)
	{
		mTitle = prompt;
	}

	// ____________________________________________________________________________________
	@Override
	public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
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
	public void selectMenu(MenuSelectMode how)
	{
		mType = Type.Menu;
		mKeyboardCount = -1;
		mUI.createMenu(how);
		show(false);
	}

	// ____________________________________________________________________________________
	@Override
	public void preferencesUpdated(SharedPreferences prefs) {
		mUI.preferencesUpdated(prefs);
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
		private Button mSelectAllBtn;

		public UI(Activity context)
		{
			mContext = context;
		}

		public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Modifier> modifiers, int repeatCount, boolean bSoftInput)
		{
			if(mAmountSelector != null)
				return mAmountSelector.handleKeyDown(ch, nhKey, keyCode, modifiers, repeatCount, bSoftInput);

			if(ch == '<')
				keyCode = KeyEvent.KEYCODE_PAGE_UP;
			else if(ch == '>')
				keyCode = KeyEvent.KEYCODE_PAGE_DOWN;

			if(!isShowing() || keyCode < 0)
				return KeyEventResult.IGNORED;

			if(mType == Type.Text)
			{
				switch(keyCode)
				{
				case KeyEvent.KEYCODE_ESCAPE:
				case KeyEvent.KEYCODE_ENTER:
				case KeyEvent.KEYCODE_BACK:
					closeInternal();
				break;

				case KeyEvent.KEYCODE_SPACE:
				case KeyEvent.KEYCODE_PAGE_DOWN:
					((ScrollView)mRoot.findViewById(R.id.scrollview)).pageScroll(ScrollView.FOCUS_DOWN);
				break;

				case KeyEvent.KEYCODE_PAGE_UP:
					((ScrollView)mRoot.findViewById(R.id.scrollview)).pageScroll(ScrollView.FOCUS_UP);
				break;

				default:
					return KeyEventResult.RETURN_TO_SYSTEM;
				}
				return KeyEventResult.HANDLED;
			}

			if(mType == Type.Menu)
			{
				// mListView.onKeyDown(keyCode, event);
				switch(keyCode)
				{
				case KeyEvent.KEYCODE_ESCAPE:
				case KeyEvent.KEYCODE_BACK:
					mKeyboardCount = -1;
					sendCancelSelect();
				break;

				case KeyEvent.KEYCODE_PAGE_DOWN:
					mKeyboardCount = -1;
					mListView.setSelection(mListView.getLastVisiblePosition());
					break;

				case KeyEvent.KEYCODE_PAGE_UP:
					mKeyboardCount = -1;
					if(mListView.getFirstVisiblePosition() == 0)
					{
						mListView.setSelection(0);
					}
					else
					{
						MenuItem item = (MenuItem)mListView.getItemAtPosition(mListView.getFirstVisiblePosition());
						View itemView = item.getView();
						// itemView can't really be null here, but just in case
						int itemHeight = itemView != null ? itemView.getHeight() : 0;
						// Make sure we don't get stuck on items that are taller than the entire view
						int margin = mListView.getDividerHeight() + 1;
						if(itemHeight > mListView.getHeight() - margin)
							itemHeight = mListView.getHeight() - margin;
						mListView.setSelectionFromTop(mListView.getFirstVisiblePosition(), mListView.getHeight() - itemHeight);
					}
					break;

				case KeyEvent.KEYCODE_ENTER:
					if(bSoftInput)
						menuOk();
					else
						return KeyEventResult.RETURN_TO_SYSTEM;
				break;
					
				case KeyEvent.KEYCODE_SPACE:
					if(bSoftInput)
					{						
						if(mHow == MenuSelectMode.PickNone)
							menuOk();
						else
							toggleItemOrGroupAt(mListView.getSelectedItemPosition());
					}
					else
						return KeyEventResult.RETURN_TO_SYSTEM;
				break;

				default:
					if(mHow == MenuSelectMode.PickNone)
					{
						if(getAccelerator(ch) >= 0)
						{
							menuOk();
							return KeyEventResult.HANDLED;
						}
						return KeyEventResult.RETURN_TO_SYSTEM;
					}
					else if(ch >= '0' && ch <= '9')
					{
						if(mKeyboardCount < 0)
							mKeyboardCount = 0;
						mKeyboardCount = mKeyboardCount * 10 + ch - '0';
						return KeyEventResult.HANDLED;
					}
					else if(menuSelect(ch))
						return KeyEventResult.HANDLED;
					else if(mHow == MenuSelectMode.PickMany)
					{
						if(ch == '.' || keyCode == KeyEvent.KEYCODE_PERIOD)
						{
							selectAll();
							return KeyEventResult.HANDLED;
						}
						else if(ch == '-' || keyCode == KeyEvent.KEYCODE_MINUS)
						{
							clearAll();
							return KeyEventResult.HANDLED;
						}
					}
					return KeyEventResult.RETURN_TO_SYSTEM;
				}
				return KeyEventResult.HANDLED;
			}
			return KeyEventResult.IGNORED;
		}

		// ____________________________________________________________________________________
		@Override
		public void onDismissCount(MenuItem item, int amount)
		{
			mAmountSelector = null;
			showInternal();
			if(mHow == MenuSelectMode.PickOne)
			{
				if(amount > 0)
					sendSelectOne(item, amount);
				else if(amount == 0)
					sendCancelSelect();
			}
			else
			{
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
			mKeyboardCount = -1;
			if(mRoot != null)
				mRoot.setVisibility(View.GONE);
		}

		// ____________________________________________________________________________________
		public void closeInternal()
		{
			if(mRoot != null)
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
		private void sendSelectOne(MenuItem item, int count)
		{
			if(isShowing())
			{
				int id = item.getId();
				// Avoid the "You don't have that many!" message
				if(count > 0)
					count = Math.min(count, item.getMaxCount());
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
		private void toggleItemOrGroupAt(int itemPos)
		{
			if(mHow != MenuSelectMode.PickMany)
				return;

			if(itemPos < 0 || itemPos >= mItems.size())
				return;

			MenuItem item = mItems.get(itemPos);
			if(item.isHeader())
				toggleGroupAt(itemPos);
			else
				toggleItem(item, true);
		}

		// ____________________________________________________________________________________
		private void toggleGroupAt(int itemPos)
		{
			// Cancel keyboard count first
			mKeyboardCount = -1;

			boolean select = false;

			MenuItem item;

			itemPos++;
			int lastItemPos = itemPos;
			for(; lastItemPos < mItems.size(); lastItemPos++)
			{
				item = mItems.get(lastItemPos);
				if(item.isHeader())
					break;
				if(item.isSelectable() && !item.isSelected())
					select = true;
			}

			for(; itemPos < lastItemPos; itemPos++)
			{
				item = mItems.get(itemPos);

				if(item.isHeader() || !item.isSelectable())
					continue;

				item.setCount(select ? -1 : 0);
			}

			((MenuItemAdapter)mListView.getAdapter()).notifyDataSetChanged();
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
			if(isShowing() && mHow != MenuSelectMode.PickNone)
			{
				boolean bRet = false;
				int i = getAccelerator(acc);
				if(i >= 0)
				{
					MenuItem item = mItems.get(i);
					if(mHow == MenuSelectMode.PickOne)
						sendSelectOne(item, mKeyboardCount);
					else
						toggleItem(item, false);
					bRet = true;
				}
				else if(mHow == MenuSelectMode.PickMany)
				{
					// Do group acc if none was found
					// Cancel keyboard count first
					mKeyboardCount = -1;
					for(MenuItem item : mItems)
					{
						if(item.getGroupAcc() == acc && !item.isHeader() && item.isSelectable())
						{
							toggleItem(item, false);
							bRet = true;
						}
					}
				}

				if(bRet)
					((MenuItemAdapter)mListView.getAdapter()).notifyDataSetChanged();

				return bRet;
			}
			return false;
		}

		// ____________________________________________________________________________________
		private void toggleItem(MenuItem item, boolean notifyAdapter)
		{
			if(!item.isHeader() && item.isSelectable())
			{
				if(mKeyboardCount >= 0)
					item.setCount(mKeyboardCount);
				else
					item.setCount(item.isSelected() ? 0 : -1);
				if(notifyAdapter)
					((MenuItemAdapter)mListView.getAdapter()).notifyDataSetChanged();
			}
			mKeyboardCount = -1;
		}

		// ____________________________________________________________________________________
		private void selectAll()
		{
			if(isShowing() && mHow == MenuSelectMode.PickMany)
			{
				for(int i = 0; i < mItems.size(); i++)
				{
					MenuItem item = mItems.get(i);
					if(!item.isHeader() && item.isSelectable())
						item.setCount(-1);
				}
				((MenuItemAdapter)mListView.getAdapter()).notifyDataSetChanged();
				mKeyboardCount = -1;
				if(mSelectAllBtn != null)
					mSelectAllBtn.setText("Clear all");
			}
		}

		// ____________________________________________________________________________________
		private void clearAll()
		{
			if(isShowing() && mHow == MenuSelectMode.PickMany)
			{
				for(int i = 0; i < mItems.size(); i++)
				{
					MenuItem item = mItems.get(i);
					if(!item.isHeader() && item.isSelectable())
						item.setCount(0);
				}
				((MenuItemAdapter)mListView.getAdapter()).notifyDataSetChanged();
				mKeyboardCount = -1;
				if(mSelectAllBtn != null)
					mSelectAllBtn.setText("Select all");
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
				@Override
				public void onClick(View v)
				{
					closeInternal();
				}
			});

			mRoot.setOnKeyListener(new OnKeyListener()
			{
				@Override
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
		public void createMenu(MenuSelectMode how)
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
		private void inflateLayout(MenuSelectMode how)
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

			mSelectAllBtn = (Button)mRoot.findViewById(R.id.btn_all);
			if(mSelectAllBtn != null)
				mSelectAllBtn.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View view)
					{
						if("Clear all".equals(mSelectAllBtn.getText().toString()))
							clearAll();
						else
							selectAll();
					}
				});

			View btn = mRoot.findViewById(R.id.btn_ok);
			if(btn != null)
				btn.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View view)
					{
						menuOk();
					}
				});

			btn = mRoot.findViewById(R.id.btn_cancel);
			if(btn != null)
				btn.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View view)
					{
						sendCancelSelect();
					}
				});

			mListView = (ListView)mRoot.findViewById(R.id.menu_list);

			mListView.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
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
							sendSelectOne(item, mKeyboardCount);
					break;
					case PickMany:
						toggleItemOrGroupAt(position);
					break;
					}
				}
			});

			mListView.setOnItemLongClickListener(new OnItemLongClickListener()
			{
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id)
				{
					final MenuItem item = mItems.get(position);
					if(!item.isSelectable())
						return false;
					if(item.getMaxCount() < 2 || mHow == MenuSelectMode.PickNone)
						return false;
					mAmountSelector = new AmountSelector(UI.this, mContext, mTileset, item);
					return true;
				}
			});

			mListView.setOnKeyListener(new OnKeyListener()
			{
				@Override
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
					sendSelectOne(mItems.get(itemPos), mKeyboardCount);
			break;
			case PickMany:
				sendSelectChecked();
			break;
			}
		}

		public void preferencesUpdated(SharedPreferences prefs) {
			if(mListView != null && mListView.getAdapter() != null) {
				mListView.invalidateViews();
				((MenuItemAdapter)mListView.getAdapter()).notifyDataSetChanged();
			}
		}
	}

}
