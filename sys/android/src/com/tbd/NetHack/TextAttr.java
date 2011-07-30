package com.tbd.NetHack;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

public enum TextAttr
{
	None
	{
		Spanned style(String str)
		{
			return new SpannedString(str);
		}

		int bgCol()
		{
			return Color.BLACK;
		}
	},

	Bold
	{
		Spanned style(String str)
		{
			Spannable span = new SpannableString(str);
			span.setSpan(new StyleSpan(Typeface.BOLD), 0, str.length(), 0);
			return span;
		}

		int bgCol()
		{
			return Color.BLACK;
		}
	},

	Dim
	{
		Spanned style(String str)
		{
			Spannable span = new SpannableString(str);
			span.setSpan(new ForegroundColorSpan(Color.GRAY), 0, str.length(), 0);
			return span;
		}

		int bgCol()
		{
			return Color.BLACK;
		}
	},

	ULine
	{
		Spanned style(String str)
		{
			Spannable span = new SpannableString(str);
			span.setSpan(new UnderlineSpan(), 0, str.length(), 0);
			return span;
		}

		int bgCol()
		{
			return Color.BLACK;
		}
	},

	Blink
	{
		Spanned style(String str)
		{
			Spannable span = new SpannableString(str);
			span.setSpan(new ForegroundColorSpan(0xFFF88017), 0, str.length(), 0);
			return span;
		}

		int bgCol()
		{
			return Color.BLACK;
		}
	},

	Inverse
	{
		Spanned style(String str)
		{
			Spannable span = new SpannableString(str);
			span.setSpan(new BackgroundColorSpan(bgCol()), 0, str.length(), 0);
			span.setSpan(new ForegroundColorSpan(Color.BLACK), 0, str.length(), 0);
			return span;
		}

		int bgCol()
		{
			return Color.WHITE;
		}
	};

	abstract Spanned style(String str);

	abstract int bgCol();

	// ____________________________________________________________________________________
	public static TextAttr fromNative(int a)
	{
		switch(a)
		{
		case 1:
			return Bold;
		case 3:
			return Dim;
		case 4:
			return ULine;
		case 5:
			return Blink;
		case 7:
			return Inverse;
		default:
			return None;
		}
	}
}
