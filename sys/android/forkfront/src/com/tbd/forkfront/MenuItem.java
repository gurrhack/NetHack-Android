package com.tbd.forkfront;

import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import com.tbd.forkfront.TextAttr;

public class MenuItem
{
	private final int mTile;
	private final int mIdent;
	private char mAccelerator;
	private final char mGroupacc;
	private final int mAttr;
	private String mName;
	private final Spanned mText;
	private final Spanned mSubText;
	private Spanned mAccText;
	private int mCount;
	private int mMaxCount;
	private View mView;

	public MenuItem(int tile, int ident, int accelerator, int groupacc, int attr, String str, int selected, Integer color)
	{
		mTile = tile;
		mIdent = ident;
		mAccelerator = (char)accelerator;
		mGroupacc = (char)groupacc;

		if(mAccelerator == 0 && mIdent == 0 && attr == TextAttr.ATTR_BOLD)
			mAttr = TextAttr.ATTR_INVERSE; // Special case for group headers
		else
			mAttr = attr; // Else regular entry

		String text = str;
		int lsp = text.lastIndexOf(" (") + 1;
		int rsp = text.lastIndexOf(')');
		int lwp = text.lastIndexOf('{');
		int rwp = text.lastIndexOf('}');
		if(!isHeader() && (lsp > 0 || lwp > 0) && (rsp > lsp && rsp == text.length() - 1 || rwp > lwp && rwp == text.length() - 1))
		{
			boolean hasStatus = rsp > lsp;
			mName = text.substring(0, hasStatus ? lsp : lwp);
			String subText = "";
			if(hasStatus)
				subText = text.substring(lsp + 1, rsp);
			if(rwp > lwp)
			{
				if(hasStatus)
					subText += "; ";
				subText += "w:" + text.substring(lwp + 1, rwp);
			}
			mSubText = TextAttr.style(subText, TextAttr.ATTR_DIM);
		}
		else
		{
			mName = text;
			mSubText = null;
		}
		if(color == null)
			mText = TextAttr.style(mName, mAttr);
		else
			mText = TextAttr.style(mName, mAttr, color);

		setAcc(mAccelerator);

		mCount = selected > 0 ? -1 : 0;
		mMaxCount = 0;
		
		int i;
		for(i = 0; i < mName.length(); i++)
		{
			char c = mName.charAt(i);
			if(c < '0' || c > '9')
				break;
			mMaxCount = mMaxCount * 10 + c - '0';
		}
		if(i > 0 && mMaxCount > 0)
			mName = mName.substring(i).trim();
		else
			mMaxCount = 1;
	}

	// ____________________________________________________________________________________
	public String getName()
	{
		return mName;
	}

	// ____________________________________________________________________________________
	public Spanned getText()
	{
		return mText;
	}

	// ____________________________________________________________________________________
	public Spanned getSubText()
	{
		return mSubText;
	}

	// ____________________________________________________________________________________
	public Spanned getAccText()
	{
		return mAccText;
	}

	// ____________________________________________________________________________________
	public boolean hasSubText()
	{
		return mSubText != null && mSubText.length() > 0;
	}

	// ____________________________________________________________________________________
	public boolean isHeader()
	{
		return mAccelerator == 0 && mIdent == 0 && mAttr == TextAttr.ATTR_INVERSE;
	}

	// ____________________________________________________________________________________
	public int getId()
	{
		return mIdent;
	}

	// ____________________________________________________________________________________
	public boolean hasTile()
	{
		return mTile >= 0;
	}

	// ____________________________________________________________________________________
	public int getTile()
	{
		return mTile;
	}

	// ____________________________________________________________________________________
	public void setCount(int c)
	{
		if(c < 0 || c >= mMaxCount)
			mCount = -1;
		else
			mCount = c;
	}

	// ____________________________________________________________________________________
	public int getCount()
	{
		return mCount;
	}

	// ____________________________________________________________________________________
	public int getMaxCount()
	{
		return mMaxCount;
	}

	// ____________________________________________________________________________________
	public boolean isSelected()
	{
		return mCount != 0;
	}

	// ____________________________________________________________________________________
	public boolean hasAcc()
	{
		return mAccelerator != 0;
	}

	// ____________________________________________________________________________________
	public char getAcc()
	{
		return mAccelerator;
	}

	// ____________________________________________________________________________________
	public void setAcc(char acc)
	{
		mAccelerator = acc;
		if(acc != 0)
			mAccText = new SpannableString(Character.toString(acc) + "   ");
		else
			mAccText = new SpannableString("    ");
	}

	// ____________________________________________________________________________________
	public char getGroupAcc()
	{
		return mGroupacc;
	}

	// ____________________________________________________________________________________
	public boolean isSelectable()
	{
		return mIdent != 0;
	}

	public void setView(View view)
	{
		mView = view;
	}

	public View getView()
	{
		return mView;
	}
}
