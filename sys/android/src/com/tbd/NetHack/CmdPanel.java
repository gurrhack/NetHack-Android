package com.tbd.NetHack;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.tbd.NetHack.Cmd.KeySequnece;

public class CmdPanel
{
	private NetHack mContext;
	private NH_State mState;
	private LinearLayout mBtnPanel;
	private Button mContextView;
	private int mMinBtnW;
	private int mMinBtnH;
	private LayoutParams mParams;
	private NH_Dialog mEditDlg;
	private EditText mInput;
	private int mItemId;
	private CmdPanelLayout mLayout;
	private int mOpacity;

	// ____________________________________________________________________________________
	public CmdPanel(NetHack context, NH_State state, CmdPanelLayout layout, String cmds, int opacity)
	{
		mContext = context;
		mState = state;
		mLayout = layout;
		mBtnPanel = new LinearLayout(context);
		mBtnPanel.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mOpacity = opacity;
		loadCmds(cmds);
	}

	// ____________________________________________________________________________________
	private void loadCmds(String cmds)
	{
		final float density = mContext.getResources().getDisplayMetrics().density;
		mMinBtnW = (int)(50 * density + 0.5f);
		mMinBtnH = (int)(50 * density + 0.5f);
		mParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		mBtnPanel.removeAllViews();

		Pattern p = Pattern.compile("\\s*((\\\\\\s|[^\\s])+)");
		Pattern p1 = Pattern.compile("((\\\\\\||[^\\|])+)");
		Matcher m = p.matcher(cmds);
		ArrayList<String> cmdList = new ArrayList<String>();
		while(m.find())
			cmdList.add(m.group(1));		
		for(String c : cmdList)
		{
			m = p1.matcher(c);
			if(!m.find())
				continue;
			String cmd = m.group(1);
			String label = "";
			if(m.find())
				label = m.group(1);
			cmd = cmd.replace("\\ ", " ");
			label = label.replace("\\ ", " ");
			View v = createCmdButtonFromString(cmd, label);
			mBtnPanel.addView(v);
		}
	}

	// ____________________________________________________________________________________
	public String getCmds()
	{
		int nButtons = mBtnPanel.getChildCount();

		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < nButtons; i++)
		{
			Button btn = (Button)mBtnPanel.getChildAt(i);
			Cmd cmd = (Cmd)btn.getTag();
			builder.append(cmd.getCommand().replace("|", "\\|").replace(" ", "\\ "));
			if(cmd.hasLabel())
				builder.append("|").append(cmd.getLabel().replace("|", "\\|").replace(" ", "\\ "));
			if(i != nButtons - 1)
				builder.append(' ');
		}
		return builder.toString();
	}

	// ____________________________________________________________________________________
	private Button createCmdButtonFromString(String chars, String label)
	{
		// special case for keyboard. not very intuitive perhaps
		if(chars.equalsIgnoreCase("..."))
		{
			return createCmdButtonFromCmd(new Cmd.ToggleKeyboard(mState, label));
		}
		// special case for menu.
		if(chars.equalsIgnoreCase("menu"))
		{
			return createCmdButtonFromCmd(new Cmd.OpenMenu(mContext, label));
		}

		KeySequnece cmd = new Cmd.KeySequnece(mState, chars, label);
		return createCmdButtonFromCmd(cmd);
	}

	// ____________________________________________________________________________________
	private Button createCmdButton()
	{
		Button btn = new Button(mContext);
		btn.setTypeface(Typeface.MONOSPACE);
		btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
		btn.setMinimumWidth(mMinBtnW);
		btn.setMinimumHeight(mMinBtnH);
		btn.setLayoutParams(mParams);
		btn.setFocusable(false);
		btn.setFocusableInTouchMode(false);
		mContext.registerForContextMenu(btn);
		return btn;
	}

	// ____________________________________________________________________________________
	private Button createCmdButtonFromCmd(final Cmd cmd)
	{
		Button btn = createCmdButton();
		btn.setText(cmd.toString());
		btn.setTag(cmd);
		if(cmd.hasLabel())
			btn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
		btn.getBackground().setAlpha(mOpacity);
		if(mOpacity <= 127)
			btn.setTextColor(0xffffffff);

		btn.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				cmd.execute();
			}
		});
		return btn;
	}

	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v)
	{
		if(v.getParent() != mBtnPanel)
			return;
		MenuInflater inflater = mContext.getMenuInflater();
		inflater.inflate(R.menu.customize_cmd, menu);
		Cmd cmd = (Cmd)v.getTag();
		String title = "Command: " + cmd.getCommand();
		if(cmd.hasLabel())
			title = title + " (" + cmd.getLabel() + ")";
		menu.setHeaderTitle(title);
		mContextView = (Button)v;
	}

	// ____________________________________________________________________________________
	public void onContextItemSelected(MenuItem item)
	{
		if(mContextView == null)
			return;

		mItemId = item.getItemId();

		if(mItemId == R.id.remove)
		{
			mBtnPanel.removeView(mContextView);
			mLayout.savePanelCmds(this);
			mContextView = null;
		}
		else if(mItemId == R.id.add_kbd)
		{
			int idx = mBtnPanel.indexOfChild(mContextView);
			mBtnPanel.addView(createCmdButtonFromString("...", ""), idx);
			mLayout.savePanelCmds(this);
			mContextView = null;
		}
		else if(mItemId == R.id.add_settings)
		{
			int idx = mBtnPanel.indexOfChild(mContextView);
			mBtnPanel.addView(createCmdButtonFromString("menu", ""), idx);
			mLayout.savePanelCmds(this);
			mContextView = null;
		}
		else if(mItemId == R.id.label)
		{
			mInput = new EditText(mContext);
			mInput.setMaxLines(1);
			mInput.setText(((Cmd)mContextView.getTag()).getLabel());
			mInput.selectAll();

			mEditDlg = new NH_Dialog(mContext);
			mEditDlg.setTitle("Type custom label");
			mEditDlg.setView(mInput);
			mEditDlg.setNegativeButton("Cancel", null);
			mEditDlg.setPositiveButton("Ok", onPositiveButton);
			mEditDlg.setOnDismissListener(onDismiss);
			mInput.setOnEditorActionListener(onEditorActionListener);
			mEditDlg.show();
		}
		else
		{
			mInput = new EditText(mContext);
			mInput.setMaxLines(1);
			if(mItemId == R.id.change)
				mInput.setText(((Cmd)mContextView.getTag()).getCommand());
			mInput.selectAll();

			mEditDlg = new NH_Dialog(mContext);
			mEditDlg.setTitle("Type command sequence");
			mEditDlg.setView(mInput);
			mEditDlg.setNegativeButton("Cancel", null);
			mEditDlg.setPositiveButton("Ok", onPositiveButton);
			mEditDlg.setOnDismissListener(onDismiss);
			mInput.setOnEditorActionListener(onEditorActionListener);
			mEditDlg.show();
		}
	}

	// ____________________________________________________________________________________
	OnDismissListener onDismiss = new OnDismissListener()
	{
		@Override
		public void onDismiss(DialogInterface dialog)
		{
			mContextView = null;
		}
	};

	// ____________________________________________________________________________________
	OnEditorActionListener onEditorActionListener = new OnEditorActionListener()
	{
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
		{
			if(event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
			{
				if(mEditDlg != null && mEditDlg.isShowing())
				{
					mEditDlg.getButton(NH_Dialog.BUTTON_POSITIVE).performClick();
					return true;
				}
			}
			return false;
		}
	};

	DialogInterface.OnClickListener onPositiveButton = new DialogInterface.OnClickListener()
	{
		public void onClick(DialogInterface dialog, int whichButton)
		{
			int idx = mBtnPanel.indexOfChild(mContextView);
			if(idx >= 0)
			{
				String cmd = "";
				String label = "";
				switch(mItemId)
				{
				case R.id.add:
					cmd = mInput.getText().toString();
				break;
				case R.id.change:
					cmd = mInput.getText().toString();
					label = ((Cmd)mContextView.getTag()).getLabel();
				break;
				case R.id.label:
					cmd = ((Cmd)mContextView.getTag()).getCommand();
					label = mInput.getText().toString();
				break;
				}
				if(mItemId == R.id.change || mItemId == R.id.label)
					mBtnPanel.removeViewAt(idx);
				mBtnPanel.addView(createCmdButtonFromString(cmd, label), idx);
				mBtnPanel.refreshDrawableState();
				mLayout.savePanelCmds(CmdPanel.this);
			}
		}
	};

	public void attach(ViewGroup newParent, boolean bHorizontal)
	{
		ViewGroup parent = (ViewGroup)mBtnPanel.getParent();
		if(parent != newParent)
		{
			if(parent != null)
				parent.removeView(mBtnPanel);
			newParent.addView(mBtnPanel);
			if(bHorizontal)
				mBtnPanel.setOrientation(LinearLayout.HORIZONTAL);
			else
				mBtnPanel.setOrientation(LinearLayout.VERTICAL);
		}

	}
}
