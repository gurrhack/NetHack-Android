package com.tbd.NetHack;

import java.util.EnumSet;

import android.widget.Button;

public interface Cmd
{
	void execute();

	// ____________________________________________________________________________________
	public class Zoom implements Cmd
	{
		private int mAmount;
		private char mCh;
		private NHW_Map mMap;

		// ____________________________________________________________________________________
		public Zoom(NHW_Map map, char c, int amount)
		{
			mAmount = amount;
			mCh = c;
			mMap = map;
		}

		// ____________________________________________________________________________________
		public void execute()
		{
			mMap.zoom(mAmount);
		}

		// ____________________________________________________________________________________
		@Override
		public String toString()
		{
			return Character.toString(mCh);
		}
	}

	// ____________________________________________________________________________________
	public class ToggleKeyboard implements Cmd
	{
		private CmdPanel mPanel;
		
		// ____________________________________________________________________________________
		public ToggleKeyboard(CmdPanel panel)
		{
			mPanel = panel;
		}
		
		// ____________________________________________________________________________________
		public void execute()
		{
			mPanel.showKeyboard();
		}

		// ____________________________________________________________________________________
		@Override
		public String toString()
		{
			return "...";
		}
	}

	// ____________________________________________________________________________________
	public class KeySequnece implements Cmd
	{
		private Button mButton;
		private NH_State mState;

		// ____________________________________________________________________________________
		public KeySequnece(NH_State state)
		{
			mState = state;
		}

		// ____________________________________________________________________________________
		public void setButton(Button btn)
		{
			mButton = btn;
		}

		// ____________________________________________________________________________________
		public String toString()
		{
			if(mButton != null)
				return mButton.getText().toString();
			return "";
		}

		// ____________________________________________________________________________________
		public void execute()
		{
			CharSequence seq = mButton.getText();
			for(int i = 0; i < seq.length(); i++)
			{
				EnumSet<Input.Modifier> mod = Input.modifiers();
				char ch = seq.charAt(i);				
				if(ch == '^' && seq.length() - i >= 2)
				{
					char n = seq.charAt(i + 1);
					if(n != ' ')
					{
						mod.add(Input.Modifier.Control);
						ch = n;
						i++;
					}
				}
				else if(ch == 'M' && seq.length() - i >= 3)
				{
					char n = seq.charAt(i + 2);
					if(n != ' ' && seq.charAt(i + 1) == '-')
					{
						mod.add(Input.Modifier.Meta);
						ch = n;
						i += 2;
					}
				}
				mState.handleKeyDown(ch, Input.nhKeyFromMod(ch, mod), Input.toKeyCode(ch), mod, 0, true);
			}
		}
	}
}
