package com.tbd.NetHack;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

public class NHW_Map implements NH_Window
{
	public static final int TileCols = 80;
	public static final int TileRows = 21;

	// y k u
	// \ | /
	// h- . -l
	// / | \
	// b j n
	public static char LEFT = 'h';
	public static char RIGHT = 'l';
	public static char UP = 'k';
	public static char DOWN = 'j';
	public static char UL = 'y';
	public static char UR = 'u';
	public static char DL = 'b';
	public static char DR = 'n';

	public static final int Corpse = 0x01;
	public static final int Invisible = 0x02;
	public static final int Detect = 0x04;
	public static final int Pet = 0x08;
	public static final int Ridden = 0x10;

	private enum ZoomPanMode
	{
		Idle, Pressed, Panning, Zooming,
	}

	private class Tile
	{
		public int glyph;
		public short overlay;
		public char[] ch = { 0 };
		public int color;
	}

	private Activity mContext;
	private UI mUI;
	private Tile[][] mTiles;
	private float mScale;
	private int mScaleCount;
	private int mStickyZoom;
	private boolean mIsStickyZoom;
	private Tileset mTileset;
	private PointF mViewOffset;
	private boolean mIsMouseLocked;
	private RectF mCanvasRect;
	private Point mPlayerPos;
	private Point mCursorPos;
	private boolean mIsRogue;
	private NHW_Status mStatus;
	private int mHealthLevel;
	private boolean mIsVisible;
	private boolean mIsBlocking;
	private int mWid;
	private NH_State mNHState;

	// ____________________________________________________________________________________
	public NHW_Map(Activity context, Tileset tileset, NHW_Status status, NH_State nhState)
	{
		mNHState = nhState;
		mTileset = tileset;
		mTiles = new Tile[TileRows][TileCols];
		for(Tile[] row : mTiles)
		{
			for(int i = 0; i < row.length; i++)
				row[i] = new Tile();
		}
		mScale = 1.f;
		mViewOffset = new PointF();
		mCanvasRect = new RectF();
		mPlayerPos = new Point();
		mCursorPos = new Point(-1, -1);
		mStatus = status;
		clear();
		setContext(context);
	}

	// ____________________________________________________________________________________
	public String getTitle()
	{
		return "NHW_Map";
	}
	
	// ____________________________________________________________________________________
	public void setContext(Activity context)
	{
		if(mContext == context)
			return;
		mContext = context;
		mUI = new UI();
		if(mIsVisible)
			show(mIsBlocking);
		else
			hide();
	}

	// ____________________________________________________________________________________
	public void show(boolean bBlocking)
	{
		mIsVisible = true;
		setBlocking(bBlocking);
		mUI.showInternal();
	}

	// ____________________________________________________________________________________
	public void hide()
	{
		mIsVisible = false;
		mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	public void setId(int wid)
	{
		mWid = wid;
	}
	
	// ____________________________________________________________________________________
	public int id()
	{
		return mWid;
	}
	
	// ____________________________________________________________________________________
	public boolean isBlocking()
	{
		return mIsBlocking;
	}

	// ____________________________________________________________________________________
	public void printString(TextAttr attr, String str, int append)
	{
	}

	// ____________________________________________________________________________________
	public void clear()
	{
		for(Tile[] row : mTiles)
		{
			for(int i = 0; i < row.length; i++)
				row[i].glyph = -1;
		}
	}

	// ____________________________________________________________________________________
	public void cliparound(final int tileX, final int tileY, final int playerX, final int playerY)
	{
		mPlayerPos.x = playerX;
		mPlayerPos.y = playerY;

		centerView(tileX, tileY);
	}

	// ____________________________________________________________________________________
	public void centerView(final int tileX, final int tileY)
	{
		float tileW = mUI.getScaledTileWidth();
		float tileH = mUI.getScaledTileHeight();

		float ofsX = mCanvasRect.left + (mCanvasRect.width() - tileW) * .5f - tileW * tileX;
		float ofsY = mCanvasRect.top + (mCanvasRect.height() - tileH) * .5f - tileH * tileY;

		if(mViewOffset.x != ofsX || mViewOffset.y != ofsY)
		{
			mViewOffset.set(ofsX, ofsY);
			mUI.invalidate();
		}
	}

	// ____________________________________________________________________________________
	public void onCursorPosClicked()
	{
		if(mIsBlocking)
		{
			setBlocking(false);
			return;
		}

		Log.print(String.format("cursor pos clicked: %dx%d", mCursorPos.x, mCursorPos.y));
		mNHState.sendPosCmd(mCursorPos.x, mCursorPos.y);
		mIsMouseLocked = false;
	}

	// ____________________________________________________________________________________
	public static int dxFromKey(int c)
	{
		int d;
		int l = Character.toLowerCase(c);
		if(l == UL || l == LEFT || l == DL)
			d = -1;
		else if(l == UR || l == RIGHT || l == DR)
			d = 1;
		else
			d = 0;
		if(l != c)
			return d * 8;
		return d;
	}

	// ____________________________________________________________________________________
	public static int dyFromKey(int c)
	{
		int d;
		int l = Character.toLowerCase(c);
		if(l == UL || l == UP || l == UR)
			d = -1;
		else if(l == DL || l == DOWN || l == DR)
			d = 1;
		else
			d = 0;
		if(l != c)
			return d * 8;
		return d;
	}

	// ____________________________________________________________________________________
	public void lockMouse()
	{
		mIsMouseLocked = true;
	}

	// ____________________________________________________________________________________
	public void zoom(int amount)
	{
		if(amount == 0 || mUI == null)
			return;

		float ofsX = (mViewOffset.x - mCanvasRect.left - mCanvasRect.width() * 0.5f) / mUI.getViewWidth();
		float ofsY = (mViewOffset.y - mCanvasRect.top - mCanvasRect.height() * 0.5f) / mUI.getViewHeight();

		mScaleCount += amount;
		if(mScaleCount > 139)
			mScaleCount = 139;
		if(mScaleCount < -200)
			mScaleCount = -200;

		mScale = (float)Math.pow(1.005, mScaleCount);

		if(mScale > 2.0f)
			mScale = 2.0f;

		ofsX = mCanvasRect.left + ofsX * mUI.getViewWidth() + mCanvasRect.width() * 0.5f;
		ofsY = mCanvasRect.top + ofsY * mUI.getViewHeight() + mCanvasRect.height() * 0.5f;

		mViewOffset.set(ofsX, ofsY);
		mUI.invalidate();
	}

	// ____________________________________________________________________________________
	private void resetZoom()
	{
		zoom(-mScaleCount);
		centerView(mCursorPos.x, mCursorPos.y);
	}

	// ____________________________________________________________________________________
	public void pan(float dx, float dy)
	{
		mViewOffset.offset(dx, dy);
		mUI.invalidate();
	}

	// ____________________________________________________________________________________
	public static int clamp(int i, int min, int max)
	{
		return Math.min(Math.max(min, i), max);
	}

	// ____________________________________________________________________________________
	public void setRogueLevel(boolean bIsRogueLevel)
	{
		mIsRogue = bIsRogueLevel;
	}

	// ____________________________________________________________________________________
	public boolean isTTY()
	{
		return mIsRogue || !mTileset.hasTiles() || mTileset == null;
	}

	// ____________________________________________________________________________________
	public void setBlocking(boolean bBlocking)
	{
		if(bBlocking)
		{
			hideControls();
			mUI.setBlockingInternal(bBlocking);
		}
		else
		{
			showControls();
			if(mIsBlocking)
				mNHState.sendKeyCmd(' ');
			mUI.setBlockingInternal(bBlocking);
		}
		mIsBlocking = bBlocking;
	}

	// ____________________________________________________________________________________
	private void hideControls()
	{
		mStatus.hide();
		mNHState.hideControls();
	}

	// ____________________________________________________________________________________
	private void showControls()
	{
		mStatus.show(false);
		mNHState.showControls();
	}

	// ____________________________________________________________________________________
	public void setCursorPos(int x, int y)
	{
		if(mCursorPos.x != x || mCursorPos.y != y)
		{
			// InvalidateTile(m_cursorPos.x, m_cursorPos.y);
			mCursorPos.x = clamp(x, 0, TileCols - 1);
			mCursorPos.y = clamp(y, 0, TileRows - 1);
			mUI.invalidateTile(mCursorPos.x, mCursorPos.y);
		}
	}

	// ____________________________________________________________________________________
	public void printTile(final int x, final int y, final int tile, final int ch, final int col, final int special)
	{
		mTiles[y][x].glyph = tile;
		mTiles[y][x].ch[0] = CP437.decode(ch);
		mTiles[y][x].color = col;
		mTiles[y][x].overlay = (short)special;
		mUI.invalidateTile(x, y);
	}

	// ____________________________________________________________________________________
	public int handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(keyCode == KeyAction.ZoomIn || keyCode == KeyAction.ZoomOut)
		{
			int scale = mScaleCount;
			zoom(keyCode == KeyAction.ZoomIn ? 20 : -20);
			if(mScaleCount == scale && repeatCount == 0)
				resetZoom();
			saveZoomLevel();
			return 1;
		}

		return mUI.handleKeyDown(nhKey, keyCode) ? 1 : 0;
	}

	// ____________________________________________________________________________________
	public boolean handleKeyUp(int keyCode)
	{
		return mUI.handleKeyUp(keyCode);
	}

	// ____________________________________________________________________________________
	public void setHealthLevel(int hp, int hpMax)
	{
		int healthLevel;
		if(hp < hpMax / 2)
			healthLevel = 0;
		else if(hp < (3 * hpMax) / 4)
			healthLevel = 1;
		else
			healthLevel = 2;

		if(healthLevel != mHealthLevel)
		{
			mHealthLevel = healthLevel;
			mUI.invalidateTile(mCursorPos.x, mCursorPos.y);
		}
	}
	
	// ____________________________________________________________________________________
	public void viewAreaChanged(Rect viewRect)
	{
		mUI.viewAreaChanged(viewRect);
	}

	// ____________________________________________________________________________________
	public void saveZoomLevel() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		prefs.edit().putInt("zoomLevel", mScaleCount).commit();
	}
	
	// ____________________________________________________________________________________
	public void loadZoomLevel() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		int zoomLevel = prefs.getInt("zoomLevel", 0);
		zoom(zoomLevel - mScaleCount);
	}
	
	// ____________________________________________________________________________________ // 
	//																						// 
	// ____________________________________________________________________________________ // 
	private class UI extends View
	{
		private CountDownTimer mPressCountDown;
		private TextPaint mPaint;
		private PointF mPointer0;
		private PointF mPointer1;
		private int mPointerId0;
		private int mPointerId1;
		private float mPointerDist;
		private ZoomPanMode mZoomPanMode;
		private boolean mIsdPadCenterDown; // don't know if both of these can exist on the same device
		private boolean mIsTrackBallDown; //
		private boolean mIsPannedSinceDown;
		private boolean mIsViewPanned;
		private List<Character> mPickChars = Arrays.asList('.', ',', ';', ':');
		private List<Character> mCancelKeys = Arrays.asList('\033', (char)0x80);
		private Typeface mTypeface;
		private float mBaseTextSize;

		// ____________________________________________________________________________________
		public UI()
		{
			super(mContext);
			setFocusable(false);
			setFocusableInTouchMode(false);
			setBackgroundColor(0xff141418);

			((ViewGroup)mContext.findViewById(R.id.map_frame)).addView(this, 0);
			mPaint = new TextPaint();
			mTypeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/monobold.ttf");
			mPaint.setTypeface(mTypeface);
			mPaint.setTextAlign(Align.LEFT);
			mPaint.setFilterBitmap(false);
			final float density = mContext.getResources().getDisplayMetrics().density;
			mBaseTextSize = 24.f * density;
			mPaint.setTextSize(mBaseTextSize * mScale);
			mPointer0 = new PointF();
			mPointer1 = new PointF();
			mPointerId0 = -1;
			mPointerId1 = -1;
			mZoomPanMode = ZoomPanMode.Idle;
		}

		// ____________________________________________________________________________________
		public void invalidateTile(int tileX, int tileY)
		{
			float tileW = getScaledTileWidth();
			float tileH = getScaledTileHeight();

			int ofsX = (int)(mViewOffset.x + tileW * tileX);
			int ofsY = (int)(mViewOffset.y + tileH * tileY);

			invalidate(new Rect(ofsX - 4, ofsY - 4, (int)(ofsX + tileW) + 4, (int)(ofsY + tileH) + 4));
		}

		// ____________________________________________________________________________________
		public void showInternal()
		{
			setVisibility(View.VISIBLE);
			invalidate();
		}

		// ____________________________________________________________________________________
		public void hideInternal()
		{
			setVisibility(View.INVISIBLE);
		}

		// ____________________________________________________________________________________
		@Override
		protected void onDraw(Canvas canvas)
		{
			super.onDraw(canvas);

			if(isTTY())
				drawAscii(canvas);
			else
				drawTiles(canvas);
		}

		// ____________________________________________________________________________________
		protected void drawTiles(Canvas canvas)
		{			
			if(mTileset == null)
				return;
			
			Rect src = new Rect();
			RectF dst = new RectF();

			float tileW = getScaledTileWidth();
			float tileH = getScaledTileHeight();

			Rect clipRect = new Rect();
			if (!canvas.getClipBounds(clipRect)) {
				clipRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
			}
			
			int minTileX = clamp((int) ((clipRect.left - mViewOffset.x) / tileW), 0, TileCols - 1);
			int maxTileX = clamp((int) FloatMath.ceil((clipRect.right - mViewOffset.x) / tileW), minTileX, TileCols - 1);
			int minTileY = clamp((int) ((clipRect.top - mViewOffset.y) / tileH), 0, TileRows - 1);
			int maxTileY = clamp((int) FloatMath.ceil((clipRect.bottom - mViewOffset.y) / tileH), minTileY, TileRows - 1);
			
			float x = FloatMath.floor(mViewOffset.x + minTileX * tileW);
			float y = FloatMath.floor(mViewOffset.y + minTileY * tileH);

			dst.set(x, y, x + tileW, y + tileH);

			mPaint.setAntiAlias(false);

			for(int tileY = minTileY; tileY <= maxTileY; tileY++)
			{
				for(int tileX = minTileX; tileX < maxTileX; tileX++)
				{
					Tile tile = mTiles[tileY][tileX];
					if(tile.glyph >= 0)
					{
						int ofs = mTileset.getTileBitmapOffset(tile.glyph);
						src.left = (ofs >> 16) & 0xffff;
						src.top = ofs & 0xffff;
						src.right = src.left + mTileset.getTileWidth();
						src.bottom = src.top + mTileset.getTileHeight();
						mPaint.setColor(0xffffffff);
						canvas.drawBitmap(mTileset.getBitmap(), src, dst, mPaint);
						Bitmap ovl = mTileset.getTileOverlay(tile.overlay);
						if(ovl != null)
							canvas.drawBitmap(ovl, mTileset.getOverlayRect(tile.overlay), dst, mPaint);
					}
					else
					{
						mPaint.setColor(0xff000000);
						canvas.drawRect(dst, mPaint);
					}

					dst.offset(tileW, 0);
				}
				dst.left = x;
				dst.right = x + tileW;
				dst.offset(0, tileH);
			}

			drawCursor(canvas, tileW, tileH);
		}

		protected void drawCursor(Canvas canvas, float tileW, float tileH)
		{
			float x = FloatMath.floor(mViewOffset.x);
			float y = FloatMath.floor(mViewOffset.y);

			if(mCursorPos.x >= 0)
			{
				if(mHealthLevel <= 0)
					mPaint.setColor(0x80B00020);
				else if(mHealthLevel == 1)
					mPaint.setColor(0x80CABF00);
				else
					mPaint.setColor(0x8020A020);
				mPaint.setStyle(Style.STROKE);
				mPaint.setAntiAlias(false);
				RectF dst = new RectF();
				dst.left = x + mCursorPos.x * tileW + 0.5f;
				dst.top = y + mCursorPos.y * tileH + 0.5f;
				dst.right = dst.left + tileW - 2.0f;
				dst.bottom = dst.top + tileH - 2.0f;
				canvas.drawRect(dst, mPaint);
				mPaint.setStyle(Style.FILL);
			}
		}

		// ____________________________________________________________________________________
		protected void drawAscii(Canvas canvas)
		{
			RectF dst = new RectF();

			float tileW = getScaledTileWidth();
			float tileH = getScaledTileHeight();

			Rect clipRect = new Rect();
			if (!canvas.getClipBounds(clipRect)) {
				clipRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
			}
			
			int minTileX = clamp((int) ((clipRect.left - mViewOffset.x) / tileW), 0, TileCols - 1);
			int maxTileX = clamp((int) Math.ceil((clipRect.right - mViewOffset.x) / tileW), minTileX, TileCols - 1);
			int minTileY = clamp((int) ((clipRect.top - mViewOffset.y) / tileH), 0, TileRows - 1);
			int maxTileY = clamp((int) Math.ceil((clipRect.bottom - mViewOffset.y) / tileH), minTileY, TileRows - 1);
			
			float x = FloatMath.floor(mViewOffset.x + minTileX * tileW);
			float y = FloatMath.floor(mViewOffset.y + minTileY * tileH);

			dst.set(x, y, x + tileW, y + tileH);

			// setup paint
			mPaint.setAntiAlias(true);

			for(int tileY = minTileY; tileY <= maxTileY; tileY++)
			{
				for(int tileX = minTileX; tileX < maxTileX; tileX++)
				{
					Tile tile = mTiles[tileY][tileX];
					int fgColor = tile.color;
					int bgColor = 0xff000000;
					if(tileX == mCursorPos.x && tileY == mCursorPos.y)
					{
						// ascii-looking cursor
						if(mHealthLevel <= 0)
							bgColor = 0xFFB00000;
						else if(mHealthLevel == 1)
							bgColor = 0xFFB0B000;
						else
							bgColor = 0xFF00B000;
						/*
						if((tile.color & 0xffffff) != 0)
							bgColor = tile.color;
						else
							bgColor = 0xffffffff;*/

						fgColor = 0xff000000;
					}
					else if(tile.overlay == 8 && tile.glyph >= 0)
					{
						bgColor = fgColor;
						fgColor = 0xff000000;
					}
					
					mPaint.setColor(bgColor);
					canvas.drawRect(dst, mPaint);

					if(tile.glyph >= 0)
					{
						mPaint.setColor(fgColor);
						canvas.drawText(tile.ch, 0, 1, dst.left, dst.bottom - mPaint.descent(), mPaint);
					}

					dst.offset(tileW, 0);
				}
				dst.left = x;
				dst.right = x + tileW;
				dst.offset(0, tileH);
			}

			//drawCursor(canvas, tileW, tileH);
		}

		// ____________________________________________________________________________________
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			int w = Math.max(getViewWidth(), getSuggestedMinimumWidth());
			int h = Math.max(getViewHeight(), getSuggestedMinimumHeight());

			int wMode = MeasureSpec.getMode(widthMeasureSpec);
			int hMode = MeasureSpec.getMode(heightMeasureSpec);

			int wConstraint = MeasureSpec.getSize(widthMeasureSpec);
			int hConstraint = MeasureSpec.getSize(heightMeasureSpec);

			if(wMode == MeasureSpec.AT_MOST && w > wConstraint || wMode == MeasureSpec.EXACTLY)
				w = wConstraint;
			if(hMode == MeasureSpec.AT_MOST && h > hConstraint || hMode == MeasureSpec.EXACTLY)
				h = hConstraint;

			setMeasuredDimension(w, h);

			centerView(mCursorPos.x, mCursorPos.y);
		}

		// ____________________________________________________________________________________
		public void viewAreaChanged(Rect viewRect)
		{
			mCanvasRect.set(viewRect);
			centerView(mCursorPos.x, mCursorPos.y);
			//Log.print(Integer.toString(cmdW) + " " + Integer.toString(cmdH));
		}
		
		// ____________________________________________________________________________________
		@Override
		public boolean onTrackballEvent(MotionEvent event)
		{
			switch(getAction(event))
			{
			case MotionEvent.ACTION_DOWN:
				mIsTrackBallDown = true;
				mIsPannedSinceDown = false;
			break;

			case MotionEvent.ACTION_UP:
				if(mIsTrackBallDown && !mIsPannedSinceDown)
					onCursorPosClicked();
				mIsTrackBallDown = false;
			break;

			case MotionEvent.ACTION_MOVE:
				// TODO tweak sensitivity
				final float axis0 = 1.f;
				final float axis1 = 0.8f;

				char dir = 0;

				if(event.getX() >= axis0)
					dir = event.getY() >= axis1 ? DR : event.getY() <= -axis1 ? UR : RIGHT;
				else if(event.getX() <= -axis0)
					dir = event.getY() >= axis1 ? DL : event.getY() <= -axis1 ? UL : LEFT;
				else if(event.getY() >= axis0)
					dir = event.getX() >= axis1 ? DR : event.getX() <= -axis1 ? DL : DOWN;
				else if(event.getY() <= -axis0)
					dir = event.getX() >= axis1 ? UR : event.getX() <= -axis1 ? UL : UP;

				if(dir != 0)
					sendDirKeyCmd(dir);
			break;
			}

			return true;
		}

		// ____________________________________________________________________________________
		@Override
		public boolean onTouchEvent(MotionEvent event)
		{
			boolean bHandled = true;
			int idx;

			switch(getAction(event))
			{
			case MotionEvent.ACTION_DOWN:
				setZoomPanMode(ZoomPanMode.Pressed);
				if(mPressCountDown != null)
					mPressCountDown.cancel();
				mPressCountDown = new CountDownTimer(ViewConfiguration.getLongPressTimeout(), 10000)
				{
					@Override
					public void onTick(long millisUntilFinished)
				{
				}

					@Override
					public void onFinish()
				{
					if(mZoomPanMode == ZoomPanMode.Pressed)
						onTouched(mPointer0.x, mPointer0.y, true);
					setZoomPanMode(ZoomPanMode.Idle);
				}
				}.start();

				idx = getActionIndex(event);
				mPointerId0 = event.getPointerId(idx);
				mPointerId1 = -1;
				mPointer0.set(event.getX(idx), event.getY(idx));
			break;

			case MotionEvent.ACTION_UP:
				if(mZoomPanMode == ZoomPanMode.Pressed)
				{
					mPressCountDown.cancel();
					onTouched(mPointer0.x, mPointer0.y, false);
				}
				setZoomPanMode(ZoomPanMode.Idle);
			break;

			case MotionEvent.ACTION_POINTER_DOWN:
				if(mPointerId1 < 0 && (mZoomPanMode == ZoomPanMode.Pressed || mZoomPanMode == ZoomPanMode.Panning))
				{
					// second pointer down, enter zoom mode
					mPressCountDown.cancel();
					setZoomPanMode(ZoomPanMode.Zooming);
					mIsViewPanned = false;
					mIsStickyZoom = false;
					idx = getActionIndex(event);
					mPointerId1 = event.getPointerId(idx);
					mPointer1.set(event.getX(idx), event.getY(idx));
					mPointerDist = getPointerDist(event);
				}
			break;

			case MotionEvent.ACTION_POINTER_UP:

				idx = getActionIndex(event);

				int idx0 = event.findPointerIndex(mPointerId0);
				int idx1 = event.findPointerIndex(mPointerId1);

				if(mZoomPanMode == ZoomPanMode.Zooming)
				{
					// Released one of the first two pointers. Ignore other pointers
					if(idx == idx0)
						idx = idx1;
					else if(idx == idx1)
						idx = idx0;

					if(idx == idx0 || idx == idx1)
					{
						// Reset start position for the first pointer
						setZoomPanMode(ZoomPanMode.Panning);
						mPointer0.set(event.getX(idx), event.getY(idx));
						mPointerId0 = event.getPointerId(idx);
						mPointerId1 = -1;
					}
				}
				else if(mZoomPanMode == ZoomPanMode.Panning && idx == idx0)
				{
					// Released the last pointer of the first two. Ignore other pointers
					mPressCountDown.cancel();
					setZoomPanMode(ZoomPanMode.Idle);
				}

			break;

			case MotionEvent.ACTION_MOVE:
				if(mZoomPanMode == ZoomPanMode.Pressed)
					tryStartPan(event);
				else if(mZoomPanMode == ZoomPanMode.Panning)
					calcPan(event);
				else if(mZoomPanMode == ZoomPanMode.Zooming)
					calcZoom(event);
			break;

			default:
				bHandled = super.onTouchEvent(event);
			}

			return bHandled;
		}

		// ____________________________________________________________________________________
		private int getActionIndex(MotionEvent event)
		{
			return (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
		}

		// ____________________________________________________________________________________
		private int getAction(MotionEvent event)
		{
			return event.getAction() & MotionEvent.ACTION_MASK;
		}

		// ____________________________________________________________________________________
		private void tryStartPan(MotionEvent event)
		{
			int idx = event.findPointerIndex(mPointerId0);

			float dx = event.getX(idx) - mPointer0.x;
			float dy = event.getY(idx) - mPointer0.y;

			float th = ViewConfiguration.get(getContext()).getScaledTouchSlop();

			if(Math.abs(dx) > th || Math.abs(dy) > th)
			{
				mPressCountDown.cancel();
				if(mZoomPanMode != ZoomPanMode.Zooming)
					mIsViewPanned = true;
				setZoomPanMode(ZoomPanMode.Panning);
			}
		}

		// ____________________________________________________________________________________
		private void setZoomPanMode(ZoomPanMode mode)
		{
			if(mZoomPanMode == ZoomPanMode.Zooming)
				saveZoomLevel();
			mZoomPanMode = mode;			
		}

		// ____________________________________________________________________________________
		private void calcPan(MotionEvent event)
		{
			int idx = event.findPointerIndex(mPointerId0);

			float dx = event.getX(idx) - mPointer0.x;
			float dy = event.getY(idx) - mPointer0.y;

			pan(dx, dy);

			mPointer0.set(event.getX(idx), event.getY(idx));
		}

		// ____________________________________________________________________________________
		private void calcZoom(MotionEvent event)
		{
			int idx0 = event.findPointerIndex(mPointerId0);
			int idx1 = event.findPointerIndex(mPointerId1);

			// Calc average movement of the two cursors and pan accordingly.
			// Don't pan if the cursors move in opposite direction on respective axis.
			float dx0 = event.getX(idx0) - mPointer0.x;
			float dy0 = event.getY(idx0) - mPointer0.y;
			float dx1 = event.getX(idx1) - mPointer1.x;
			float dy1 = event.getY(idx1) - mPointer1.y;

			if(dx0 > 0 && dx1 < 0 || dx0 < 0 && dx1 > 0)
				dx0 = 0;
			else
				dx0 = (dx0 + dx1) * 0.5f;

			if(dy0 > 0 && dy1 < 0 || dy0 < 0 && dy1 > 0)
				dy0 = 0;
			else
				dy0 = (dy0 + dy1) * 0.5f;

			pan(dx0, dy0);

			// Calc dist between cursors and zoom accordingly.
			float newDist = getPointerDist(event);
			if(newDist > 5)
			{
				int zoomAmount = (int)(newDist - mPointerDist);
				int newScale = mScaleCount + zoomAmount;

				if(zoomAmount != 0)
				{
					if(mIsStickyZoom && (mScaleCount ^ newScale) < 0 && mStickyZoom == 0 || mStickyZoom != 0 && (mStickyZoom ^ zoomAmount) >= 0)
					{
						mStickyZoom += zoomAmount;
						if(Math.abs(mStickyZoom) < 50)
							zoomAmount = -mScaleCount;
						else
							mStickyZoom = 0;
					}
					else
					{
						mStickyZoom = 0;
						mIsStickyZoom = true;
					}

					zoom(zoomAmount);
				}

				mPointerDist = newDist;

				mPointer0.set(event.getX(idx0), event.getY(idx0));
				mPointer1.set(event.getX(idx1), event.getY(idx1));
			}
		}

		// ____________________________________________________________________________________
		private float getPointerDist(MotionEvent event)
		{
			int idx0 = event.findPointerIndex(mPointerId0);
			int idx1 = event.findPointerIndex(mPointerId1);
			float dx = event.getX(idx0) - event.getX(idx1);
			float dy = event.getY(idx0) - event.getY(idx1);
			return FloatMath.sqrt(dx * dx + dy * dy);
		}

		// ____________________________________________________________________________________
		public void setBlockingInternal(boolean bBlocking)
		{
			if(bBlocking)
			{
				String blockMsg = Util.hasPhysicalKeyboard(mContext) ? "Press any key to continue" : "Tap to continue";
				TextView tv = (TextView)mContext.findViewById(R.id.nh_blockmsg);
				tv.setText(blockMsg);
				tv.setVisibility(View.VISIBLE);
			}
			else
			{
				mContext.findViewById(R.id.nh_blockmsg).setVisibility(View.GONE);
				mIsdPadCenterDown = false;
				mIsTrackBallDown = false;
				mIsPannedSinceDown = false;
				mIsViewPanned = false;
			}
		}

		// ____________________________________________________________________________________
		private void onTouched(float x, float y, boolean bLongClick)
		{
			if(mIsBlocking)
			{
				if(!bLongClick)
					setBlocking(false);
				return;
			}

			float tileW = getScaledTileWidth();
			float tileH = getScaledTileHeight();

			int tileX = (int)((x - mViewOffset.x) / tileW);
			int tileY = (int)((y - mViewOffset.y) / tileH);

			if(shouldSendPosOnTouch(tileX, tileY))
			{
				tileX = clamp(tileX, 0, TileCols - 1);
				tileY = clamp(tileY, 0, TileRows - 1);
				if(mIsMouseLocked)
					setCursorPos(tileX, tileY);
				mNHState.sendPosCmd(tileX, tileY);
				mIsMouseLocked = false;
			}
			else
			{
				int dx = tileX - mPlayerPos.x;
				int dy = tileY - mPlayerPos.y;
				int adx = Math.abs(dx);
				int ady = Math.abs(dy);

				final float c = (float)Math.tan(3 * Math.PI / 8);

				char dir;
				if(adx > c * ady)
					dir = dx > 0 ? RIGHT : LEFT;
				else if(ady > c * adx)
					dir = dy < 0 ? UP : DOWN;
				else if(dx > 0)
					dir = dy < 0 ? UR : DR;
				else
					dir = dy < 0 ? UL : DL;

				if(bLongClick)
					dir = Character.toUpperCase(dir);

				Log.print("walking");
				mNHState.sendDirKeyCmd(dir);
			}
			mIsViewPanned = false;
		}

		// ____________________________________________________________________________________
		private void sendDirKeyCmd(int c)
		{
			if(mIsBlocking || mIsMouseLocked || mIsdPadCenterDown || mIsTrackBallDown)
			{
				// Log.print("pan with cursor");
				mIsPannedSinceDown = true;
				mIsViewPanned = true;
				setCursorPos(mCursorPos.x + dxFromKey(c), mCursorPos.y + dyFromKey(c));
				centerView(mCursorPos.x, mCursorPos.y);
			}
			else
			{
				mNHState.sendDirKeyCmd(c);
			}
		}

		// ____________________________________________________________________________________
		private boolean shouldSendPosOnTouch(int tileX, int tileY)
		{
			if(mIsMouseLocked)
				return true;

			if(mPlayerPos.equals(tileX, tileY))
				return true;

			if(!mIsViewPanned)
				return false;

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			if(!prefs.getBoolean("travelAfterPan", true))
				return false;

			// Don't send pos command if clicking within a few tiles from the player

			int dx = Math.abs(tileX - mPlayerPos.x);
			int dy = Math.abs(tileY - mPlayerPos.y);

			// . . . . .
			// . . . . .
			// . . @ . .
			// . . . . .
			// . . . . .
			if(dx <= 3 && dy <= 3)
				return false;

			return true;
		}

		// ____________________________________________________________________________________
		public boolean handleKeyDown(int nhKey, int keyCode)
		{
			if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
			{
				mIsdPadCenterDown = true;
				mIsPannedSinceDown = false;
				return true;
			}

			switch(keyCode)
			{
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_DOWN:
				sendDirKeyCmd(nhKey);
				return true;
			}

			if(mIsMouseLocked)
			{
				if(mPickChars.contains((char)nhKey))
				{
					onCursorPosClicked();
					return true;
				}
				if(mCancelKeys.contains((char)nhKey))
				{
					mNHState.sendDirKeyCmd(nhKey);
					mIsMouseLocked = false;
					return true;
				}
			}

			if(mIsBlocking)
			{
				setBlocking(false);
				return true;
			}

			return false;
		}

		// ____________________________________________________________________________________
		public boolean handleKeyUp(int keyCode)
		{
			if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
			{
				if(mIsdPadCenterDown && !mIsPannedSinceDown)
					onCursorPosClicked();
				mIsdPadCenterDown = false;
				return true;
			}
			return false;
		}

		// ____________________________________________________________________________________
		private int getViewWidth()
		{
			return (int)(TileCols * getScaledTileWidth() + 0.5f);
		}

		// ____________________________________________________________________________________
		private int getViewHeight()
		{
			return (int)(TileRows * getScaledTileHeight() + 0.5f);
		}

		// ____________________________________________________________________________________
		private float getScaledTileWidth()
		{
			if(isTTY())
			{
				mPaint.setTextSize(mBaseTextSize * mScale);
				float w = mPaint.measureText("\u2550");
				return FloatMath.floor(w) - 1;
			}

			return mTileset.getTileWidth() * mScale;
		}

		// ____________________________________________________________________________________
		private float getScaledTileHeight()
		{
			if(isTTY())
			{
				mPaint.setTextSize(mBaseTextSize * mScale);
				FontMetrics metrics = mPaint.getFontMetrics();
				return FloatMath.floor(metrics.descent - metrics.ascent);
			}

			return mTileset.getTileHeight() * mScale;
		}

	}
}
