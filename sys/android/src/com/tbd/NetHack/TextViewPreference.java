package com.tbd.NetHack;

import android.content.Context;
import android.preference.Preference;
import android.text.Html;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.AlignmentSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TextViewPreference extends Preference
{
	// ____________________________________________________________________________________
	public TextViewPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setPersistent(false);
	}

	// ____________________________________________________________________________________
	@Override
	protected View onCreateView(ViewGroup parent)
	{
		String title = getTitle().toString();
		int res;
		if(title.equalsIgnoreCase("license"))
			res = R.string.license;
		else if(title.equalsIgnoreCase("history"))
			res = R.string.history;
		else
			res = R.string.credits;
		
		View view = Util.Inflate(R.layout.textwindow);
		TextView text = (TextView)view.findViewById(R.id.text_view);
		text.setText(Html.fromHtml(NetHack.get().getString(res)), TextView.BufferType.SPANNABLE);
		
		// bah
		if(res == R.string.credits)
		{
			text.setMovementMethod(LinkMovementMethod.getInstance());

			Spannable span = (Spannable)text.getText();
			span.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_CENTER), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		return view;
	}
}
