package com.tbd.NetHack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import com.tbd.NetHack.Input.Modifier;

public interface Cmd
{
	void execute();

	boolean hasLabel();

	String getCommand();

	String getLabel();

	// ____________________________________________________________________________________
	class ToggleKeyboard implements Cmd
	{
		private NH_State mState;
		private String mLabel = "";

		// ____________________________________________________________________________________
		public ToggleKeyboard(NH_State state, String label)
		{
			mState = state;
			mLabel = label;
		}

		// ____________________________________________________________________________________
		public void execute()
		{
			mState.showKeyboard();
		}

		// ____________________________________________________________________________________
		@Override
		public String toString()
		{
			if(mLabel.length() > 0)
				return mLabel;
			return "...";
		}

		// ____________________________________________________________________________________
		@Override
		public boolean hasLabel()
		{
			return mLabel.length() > 0;
		}

		// ____________________________________________________________________________________
		@Override
		public String getCommand()
		{
			return "...";
		}

		// ____________________________________________________________________________________
		@Override
		public String getLabel()
		{
			return mLabel;
		}
	}

	// ____________________________________________________________________________________
	public class OpenMenu implements Cmd
	{
		private Activity mContext;
		private String mLabel = "";

		// ____________________________________________________________________________________
		public OpenMenu(Activity context, String label)
		{
			mContext = context;
			mLabel = label;
		}

		// ____________________________________________________________________________________
		public void execute()
		{
			Intent prefsActivity = new Intent(mContext.getBaseContext(), Settings.class);
			mContext.startActivityForResult(prefsActivity, 42);
		}

		// ____________________________________________________________________________________
		@Override
		public String toString()
		{
			if(mLabel.length() > 0)
				return mLabel;
			return "menu";
		}

		// ____________________________________________________________________________________
		@Override
		public boolean hasLabel()
		{
			return mLabel.length() > 0;
		}

		// ____________________________________________________________________________________
		@Override
		public String getCommand()
		{
			return "menu";
		}

		// ____________________________________________________________________________________
		@Override
		public String getLabel()
		{
			return mLabel;
		}
	}

	// ____________________________________________________________________________________
	public class KeySequnece implements Cmd
	{
		private class KeyCmd
		{
			public KeyCmd(char ch, EnumSet<Modifier> mod)
			{
				this.ch = ch;
				this.mod = mod;
			}

			public EnumSet<Input.Modifier> mod;
			public char ch;
		}

		private NH_State mState;
		private boolean mExecuting;
		private String mLabel = "";
		private ArrayList<KeyCmd> mSeq = new ArrayList<KeyCmd>();
		private String mCommand;

		// ____________________________________________________________________________________
		public KeySequnece(NH_State state, String command, String label)
		{
			mState = state;
			mLabel = label;
			mCommand = command;
		}

		// ____________________________________________________________________________________
		public void setCommand(String command)
		{
			if(!mCommand.equals(command))
			{
				mCommand = command;
				mSeq.clear();
			}
		}

		// ____________________________________________________________________________________
		public void setLabel(String label)
		{
			mLabel = label;
		}

		// ____________________________________________________________________________________
		public String toString()
		{
			if(mLabel.length() > 0)
				return mLabel;
			return mCommand;
		}

		// ____________________________________________________________________________________
		public void execute()
		{
			// Prevent re-entry while already executing the command!
			if(mExecuting)
				return;
			if(mSeq.isEmpty())
				rebuildSequence();
			if(mSeq.isEmpty())
				return;

			mExecuting = true;

			// Handle response from NetHack for each key, before posting the next
			final Handler handler = mState.getHandler();
			@SuppressWarnings("unchecked")
			final ArrayList<KeyCmd> seq = (ArrayList<KeyCmd>)mSeq.clone();
			handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					char ch = seq.get(0).ch;
					EnumSet<Modifier> mod = seq.get(0).mod;
					Log.print("cmdpanel: " + Character.toString(ch));
					mState.handleKeyDown(ch, Input.nhKeyFromMod(ch, mod), Input.toKeyCode(ch), mod, 0, true);
					seq.remove(0);
					if(seq.size() > 0)
					{
						mState.waitReady();
						handler.post(this);
					}
					else
						mExecuting = false;
				}
			});
		}

		// ____________________________________________________________________________________
		private void rebuildSequence()
		{
			mSeq.clear();
			for(int i = 0; i < mCommand.length(); i++)
			{
				EnumSet<Input.Modifier> mod = Input.modifiers();
				char ch = mCommand.charAt(i);
				if(ch == '^' && mCommand.length() - i >= 2)
				{
					char n = mCommand.charAt(i + 1);
					if(n != ' ')
					{
						mod.add(Input.Modifier.Control);
						ch = n;
						i++;
					}
				}
				else if(ch == 'M' && mCommand.length() - i >= 3)
				{
					char n = mCommand.charAt(i + 2);
					if(n != ' ' && mCommand.charAt(i + 1) == '-')
					{
						mod.add(Input.Modifier.Meta);
						ch = n;
						i += 2;
					}
				}
				mSeq.add(new KeyCmd(ch, mod));
			}
		}

		// ____________________________________________________________________________________
		@Override
		public boolean hasLabel()
		{
			return mLabel.length() > 0;
		}

		// ____________________________________________________________________________________
		@Override
		public String getCommand()
		{
			return mCommand;
		}

		// ____________________________________________________________________________________
		@Override
		public String getLabel()
		{
			return mLabel;
		}

	}
}
