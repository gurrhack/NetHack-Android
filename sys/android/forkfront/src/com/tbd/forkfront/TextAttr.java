package com.tbd.forkfront;

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

public class TextAttr
{
	public static final int ATTR_NONE = (1<<0);
	public static final int ATTR_BOLD = (1<<1);
	public static final int ATTR_DIM = (1<<2);
	public static final int ATTR_ULINE = (1<<4);
	public static final int ATTR_BLINK = (1<<5);
	public static final int ATTR_INVERSE = (1<<7);
	public static final int ATTR_UNDEFINED = (1<<8);

	public static Spanned style(String str, int attr) {
		if( attr == 0 || attr == ATTR_NONE || attr == ATTR_UNDEFINED)
			return new SpannedString(str);

		if( (attr & ATTR_INVERSE) != 0 )
			return style(str, attr, Color.WHITE);

		if( (attr & ATTR_DIM) != 0 )
			return style(str, attr, Color.GRAY);

		if( (attr & ATTR_BLINK) != 0 )
			return style(str, attr, 0xFFF88017);

		return style(str, attr, Color.WHITE);
	}

	public static Spanned style(String str, int attr, int color) {

		Spannable span = new SpannableString(str);

		if( (attr & ATTR_INVERSE) == 0 )
			span.setSpan(new ForegroundColorSpan(color), 0, str.length(), 0);
		else {
			span.setSpan(new BackgroundColorSpan(color), 0, str.length(), 0);
			span.setSpan(new ForegroundColorSpan(Color.BLACK), 0, str.length(), 0);
		}

		if( (attr & ATTR_BOLD) != 0 )
			span.setSpan(new StyleSpan(Typeface.BOLD), 0, str.length(), 0);

		if( (attr & ATTR_ULINE) != 0 )
			span.setSpan(new UnderlineSpan(), 0, str.length(), 0);

		return span;
	}

}
