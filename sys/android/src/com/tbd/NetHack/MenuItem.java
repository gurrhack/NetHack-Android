package com.tbd.NetHack;

import android.text.Spanned;

public class MenuItem
{
	private final int mTile;
	private final int mIdent;
	private char mAccelerator;
	private final char mGroupacc;
	private final TextAttr mAttr;
	private String mName;
	private final Spanned mText;
	private final Spanned mSubText;
	private Spanned mAccText;
	private int mCount;
	private int mMaxCount;
	private boolean mIsSelected;

	public MenuItem(MenuItem item)
	{
		mTile = item.mTile;
		mIdent = item.mIdent;
		mAccelerator = item.mAccelerator;
		mGroupacc = item.mGroupacc;
		mAttr = item.mAttr;
		mName = item.mName;
		mText = item.mText;
		mSubText = item.mSubText;
		mAccText = item.mAccText;
		mCount = item.mCount;
		mMaxCount = item.mMaxCount;
		mIsSelected = item.mIsSelected;
	}
	
	// ____________________________________________________________________________________
	public MenuItem(final int tile, final int ident, final int accelerator, final int groupacc, final TextAttr attr, final String str, final int selected)
	{
		mTile = tile;
		mIdent = ident;
		mAccelerator = (char)accelerator;
		mGroupacc = (char)groupacc;
		mAttr = attr == TextAttr.Bold ? TextAttr.Inverse : attr;

		String text = str;
		int lp = text.indexOf('(');
		int rp = text.indexOf(')');
		if(accelerator != 0 && lp > 0 && lp != rp && rp == text.length() - 1)
		{
			mName = text.substring(0, lp);
			mSubText = TextAttr.Dim.style(text.substring(lp + 1, rp));
		}
		else
		{
			mName = text;
			mSubText = null;
		}
		mText = mAttr.style(mName);

		setAcc(mAccelerator);
		
		mCount = -1;
		mMaxCount = 0;
		
		int i;
		for(i = 0; i < mName.length(); i++)
		{
			char c = mName.charAt(i);
			if(c < '0' || c > '9')
				break;
			mMaxCount = mMaxCount * 10 + c - '0';
		}
		if(i > 0)
			mName = mName.substring(i).trim();

		mIsSelected = selected != 0;
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
		return mAttr == TextAttr.Inverse;
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
			mCount = NHW_Map.clamp(c, 0, mMaxCount);
		mIsSelected = mCount != 0;
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
	public void toggle()
	{
		mIsSelected = !mIsSelected;
	}

	// ____________________________________________________________________________________
	public boolean isSelected()
	{
		return mIsSelected;
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
			mAccText = mAttr.style(Character.toString(acc));
		else
			mAccText = null;
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

}
