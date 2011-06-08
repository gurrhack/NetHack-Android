package com.tbd.NetHack;

import java.util.Scanner;

import android.text.Spanned;

public class MenuItem
{
	private final int m_tile;
	private final int m_ident;
	private char m_accelerator;
	private final char m_groupacc;
	private final TextAttr m_attr;
	private String m_name;
	private final Spanned m_text;
	private final Spanned m_subText;
	private Spanned m_accText;
	private int m_count;
	private int m_maxCount;
	private boolean m_bSelected;

	// ____________________________________________________________________________________
	public MenuItem(final int tile, final int ident, final int accelerator, final int groupacc, final TextAttr attr, final String str, final int selected)
	{
		m_tile = tile;
		m_ident = ident;
		m_accelerator = (char)accelerator;
		m_groupacc = (char)groupacc;
		m_attr = attr == TextAttr.Bold ? TextAttr.Inverse : attr;		

		String text = str.trim();
		int lp = text.indexOf('(');
		int rp = text.indexOf(')');
		if(accelerator != 0 && lp > 0 && lp != rp && rp == text.length() - 1)
		{
			m_name = text.substring(0, lp);
			m_subText = TextAttr.Dim.Style(text.substring(lp + 1, rp));
		}
		else
		{
			m_name = str;
			m_subText = null;
		}
		m_text = m_attr.Style(m_name);

		SetAcc(m_accelerator);
		
		m_count = -1;
		try
		{
			Scanner scanner = new Scanner(m_name);
			m_maxCount = scanner.nextInt();
			m_name = scanner.nextLine().trim();
		}
		catch(Exception e)
		{
			m_maxCount = 0;
		}

		m_bSelected = selected != 0;
	}

	// ____________________________________________________________________________________
	public String GetName()
	{
		return m_name;
	}

	// ____________________________________________________________________________________
	public Spanned GetText()
	{
		return m_text;
	}

	// ____________________________________________________________________________________
	public Spanned GetSubText()
	{
		return m_subText;
	}

	// ____________________________________________________________________________________
	public Spanned GetAccText()
	{
		return m_accText;
	}

	// ____________________________________________________________________________________
	public boolean HasSubText()
	{
		return m_subText != null && m_subText.length() > 0;
	}

	// ____________________________________________________________________________________
	public boolean IsHeader()
	{
		return m_attr == TextAttr.Inverse;
	}

	// ____________________________________________________________________________________
	public int GetId()
	{
		return m_ident;
	}

	// ____________________________________________________________________________________
	public boolean HasTile()
	{
		return m_tile >= 0;
	}

	// ____________________________________________________________________________________
	public int GetTile()
	{
		return m_tile;
	}

	// ____________________________________________________________________________________
	public void SetCount(int c)
	{
		m_count = NHW_Map.clamp(c, 0, m_maxCount);
		m_bSelected = m_count > 0;
	}

	// ____________________________________________________________________________________
	public int GetMaxCount()
	{
		return m_maxCount;
	}

	// ____________________________________________________________________________________
	public void Toggle()
	{
		m_bSelected = !m_bSelected;
	}

	// ____________________________________________________________________________________
	public boolean IsSelected()
	{
		return m_bSelected;
	}

	// ____________________________________________________________________________________
	public boolean HasAcc()
	{
		return m_accelerator != 0;
	}

	// ____________________________________________________________________________________
	public char GetAcc()
	{
		return m_accelerator;
	}

	public void SetAcc(char acc)
	{
		m_accelerator = acc;
		if(acc != 0)
			m_accText = m_attr.Style(Character.toString(acc));
		else
			m_accText = null;
	}
}
