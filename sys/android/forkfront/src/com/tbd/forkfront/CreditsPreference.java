package com.tbd.forkfront;

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
import com.tbd.forkfront.Util;

public class CreditsPreference extends Preference
{
	// ____________________________________________________________________________________
	public CreditsPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setPersistent(false);
		setLayoutResource(R.layout.textwindow);
	}

	// ____________________________________________________________________________________
	@Override
	protected View onCreateView(ViewGroup parent)
	{
		View view = super.onCreateView(parent);//Util.inflate(getContext(), R.layout.textwindow);
		TextView text = (TextView)view.findViewById(R.id.text_view);

		text.setText(Html.fromHtml(getContext().getString(R.string.credits)), TextView.BufferType.SPANNABLE);

		text.setMovementMethod(LinkMovementMethod.getInstance());

		Spannable span = (Spannable)text.getText();
		span.setSpan(new AlignmentSpan.Standard(Alignment.ALIGN_CENTER), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		return view;
	}
}
