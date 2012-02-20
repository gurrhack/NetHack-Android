package com.tbd.NetHack;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class AutoFitTextView extends TextView
{
	private boolean mDoMeasureText;
	private float mTextSize;
	private float mAdd;
	private float mMul;
	private final float minSize;

	// ____________________________________________________________________________________
	public AutoFitTextView(Context context)
	{
		this(context, null, 0);
	}

	// ____________________________________________________________________________________
	public AutoFitTextView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	// ____________________________________________________________________________________
	public AutoFitTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		mTextSize = getTextSize();
		mAdd = 0.f;
		mMul = 1.f;
		final float density = context.getResources().getDisplayMetrics().density;
		minSize = 11.f * density;
	}

	// ____________________________________________________________________________________
	@Override
	protected void onTextChanged(final CharSequence text, final int start, final int before, final int after)
	{
		mDoMeasureText = true;
	}

	// ____________________________________________________________________________________
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		if(w != oldw || h != oldh)
			mDoMeasureText = true;
	}

	// ____________________________________________________________________________________
	public void measureText()
	{
		mDoMeasureText = false;

		int viewW = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
		int viewH = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

		if(viewW <= 0 || viewH <= 0)
			return;

		CharSequence text = getText();
		if(text.length() <= 0)
			return;

		TextPaint paint = super.getPaint();
		paint.setTextSize(mTextSize);
		float textSize = mTextSize;
		float textW = paint.measureText(text, 0, text.length());
		while(textSize > minSize && textW > viewW)
		{
			textSize--;
			paint.setTextSize(textSize);
			textW = paint.measureText(text, 0, text.length());
		}
		super.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		super.setLineSpacing(mAdd, mMul);
	}

	// ____________________________________________________________________________________
	@Override
	public void setTextSize(float size)
	{
		super.setTextSize(size);
		mTextSize = getTextSize();
	}

	// ____________________________________________________________________________________
	@Override
	public void setTextSize(int unit, float size)
	{
		super.setTextSize(unit, size);
		mTextSize = getTextSize();
	}

	// ____________________________________________________________________________________
	@Override
	public void setLineSpacing(float add, float mult)
	{
		super.setLineSpacing(add, mult);
		mAdd = add;
		mMul = mult;
	}

	// ____________________________________________________________________________________
	@Override
	protected void onDraw(Canvas canvas)
	{
		if(mDoMeasureText)
			measureText();
		super.onDraw(canvas);
	}
}
