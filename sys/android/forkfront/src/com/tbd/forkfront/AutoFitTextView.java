package com.tbd.forkfront;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;

public class AutoFitTextView extends NH_TextView {
	private static final float MIN_SIZE_SP = 10;

	private boolean mTextChanged;
	private int mLastMeasuredWidth;
	private boolean mIsMonospaceMode;
	private float mMeasuredTextSize = getOriginalTextSize();

	// ____________________________________________________________________________________
	public AutoFitTextView(Context context)
	{
		super(context);
	}

	// ____________________________________________________________________________________
	public AutoFitTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	// ____________________________________________________________________________________
	public AutoFitTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	// ____________________________________________________________________________________
	@Override
	protected void onTextChanged(final CharSequence text, final int start, final int before, final int after)
	{
		mTextChanged = true;
	}

	// ____________________________________________________________________________________
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		if(w != oldw || h != oldh)
			mTextChanged = true;
	}

	// ____________________________________________________________________________________
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int viewW = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
		int viewH = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

		if(viewW > 0 && viewH > 0 && (mTextChanged || mLastMeasuredWidth != viewW))
		{
			mTextChanged = false;
			mLastMeasuredWidth = viewW;
			fitText(viewW);
			// Redo measure for a new height value
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	// ____________________________________________________________________________________
	private void fitText(int viewW)
	{
		CharSequence text = getText();
		if(text.length() <= 0)
			return;

		float minSize = getMinTextSize();
		TextPaint paint = new TextPaint();
		paint.set(super.getPaint());
		float textSize = getOriginalTextSize();
		paint.setTextSize(textSize);
		float textW = paint.measureText(text, 0, text.length());
		while(textSize > minSize && textW > viewW) // fast enough
		{
			textSize--;
			paint.setTextSize(textSize);
			textW = paint.measureText(text, 0, text.length());
		}

		mMeasuredTextSize = textSize;

		if(!mIsMonospaceMode) {
			super.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		}
	}

	// ____________________________________________________________________________________
	public float getMinTextSize()
	{
		float scaledDensity = getContext().getResources().getDisplayMetrics().scaledDensity;
		return MIN_SIZE_SP * scaledDensity;
	}

	@Override
	protected void updateMode(boolean monospaceMode, Typeface typeface, float size) {
		mIsMonospaceMode = monospaceMode;
		setTypeface(typeface);
		if(mIsMonospaceMode)
			setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
		else
			setTextSize(TypedValue.COMPLEX_UNIT_PX, mMeasuredTextSize);
	}
}
