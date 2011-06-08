package com.tbd.NetHack;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class AutoFitTextView extends TextView
{
	private float m_textSize;
	private float m_lsAdd;
	private float m_lsMul;

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
		m_textSize = getTextSize();
		m_lsAdd = 0.f;
		m_lsMul = 1.f;
	}

	// ____________________________________________________________________________________
	public void MeasureText()
	{
		int viewW = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
		int viewH = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

		if(viewW <= 0 || viewH <= 0)
			return;

		CharSequence text = getText();
		if(text.length() <= 0)
			return;

		TextPaint paint = super.getPaint();
		paint.setTextSize(m_textSize);
		float textSize = m_textSize;
		float textW = paint.measureText(text, 0, text.length());
		while(textSize > 10.f && textW > viewW)
		{
			textSize--;
			paint.setTextSize(textSize);
			textW = paint.measureText(text, 0, text.length());
		}
		super.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		super.setLineSpacing(m_lsAdd, m_lsMul);
	}
	
	// ____________________________________________________________________________________
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		//super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		MeasureText();
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	// ____________________________________________________________________________________
	@Override
	public void setTextSize(float size)
	{
		super.setTextSize(size);
		m_textSize = size;
	}

	// ____________________________________________________________________________________
	@Override
	public void setTextSize(int unit, float size)
	{
		super.setTextSize(unit, size);
		m_textSize = getTextSize();
	}

	// ____________________________________________________________________________________
	@Override
	public void setLineSpacing(float add, float mult)
	{
		super.setLineSpacing(add, mult);
		m_lsAdd = add;
		m_lsMul = mult;
	}
}
