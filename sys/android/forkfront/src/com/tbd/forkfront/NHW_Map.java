package com.tbd.forkfront;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.text.TextPaint;
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
	private static final double ZOOM_BASE = 1.005;
	private static final float PIE_SLICE = (float)Math.sqrt(2)-1;// tan(pi/8)
	private static final float SELF_RADIUS_FACTOR = 25;
	private static final float MIN_TILE_SIZE_FACTOR = 5;
	private static final float MAX_TILE_SIZE_FACTOR = 100;

	// y k u
	// \ | /
	// h- . -l
	// / | \
	// b j n
	
	private char getLEFT() {
		return mNHState.isNumPadOn() ? '4' : 'h';
	}

	private char getRIGHT() {
		return mNHState.isNumPadOn() ? '6' : 'l';
	}

	private char getUP() {
		return mNHState.isNumPadOn() ? '8' : 'k';
	}

	private char getDOWN() {
		return mNHState.isNumPadOn() ? '2' : 'j';
	}

	private char getUL() {
		return mNHState.isNumPadOn() ? '7' : 'y';
	}

	private char getUR() {
		return mNHState.isNumPadOn() ? '9' : 'u';
	}

	private char getDL() {
		return mNHState.isNumPadOn() ? '1' : 'b';
	}

	private char getDR() {
		return mNHState.isNumPadOn() ? '3' : 'n';
	}
	
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

	private enum TouchResult
	{
		SEND_POS,
		SEND_MY_POS,
		SEND_DIR
	}

	private Activity mContext;
	private UI mUI;
	private Tile[][] mTiles;
	private float mScale;
	private float mDisplayDensity;
	private float mMinTileH;
	private float mMaxTileH;
	private float mSelfRadius;
	private float mSelfRadiusSquared;
	private float mScaleCount;
	private float mMinScaleCount;
	private float mMaxScaleCount;
	private float mZoomStep;
	private float mLockTopMargin;
	private int mStickyZoom;
	private boolean mIsStickyZoom;
	private final Tileset mTileset;
	private PointF mViewOffset;
	private RectF mCanvasRect;
	private Point mPlayerPos;
	private Point mCursorPos;
	private boolean mIsRogue;
	private NHW_Status mStatus;
	private int mHealthColor;
	private boolean mIsVisible;
	private boolean mIsBlocking;
	private int mWid;
	private NH_State mNHState;
	private final ByteDecoder mDecoder;
	private int mBorderColor;

	// ____________________________________________________________________________________
	public NHW_Map(Activity context, Tileset tileset, NHW_Status status, NH_State nhState, ByteDecoder decoder)
	{
		mNHState = nhState;
		mDecoder = decoder;
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
		mBorderColor = 0;
		clear();
		setContext(context);
	}

	// ____________________________________________________________________________________
	@Override
	public String getTitle()
	{
		return "NHW_Map";
	}
	
	// ____________________________________________________________________________________
	@Override
	public void setContext(Activity context)
	{
		if(mContext == context)
			return;
		mContext = context;
		mDisplayDensity = context.getResources().getDisplayMetrics().density;
		mMinTileH = MIN_TILE_SIZE_FACTOR * mDisplayDensity;
		mMaxTileH = MAX_TILE_SIZE_FACTOR * mDisplayDensity;
		mSelfRadius = SELF_RADIUS_FACTOR * mDisplayDensity;
		mSelfRadiusSquared = mSelfRadius * mSelfRadius;
		mLockTopMargin = mStatus.getHeight();
		mUI = new UI();
		if(mIsVisible)
			show(mIsBlocking);
		else
			hide();
		updateZoomLimits();
	}

	// ____________________________________________________________________________________
	@Override
	public void show(boolean bBlocking)
	{
		mIsVisible = true;
		setBlocking(bBlocking);
		mUI.showInternal();
	}

	// ____________________________________________________________________________________
	private void hide()
	{
		mIsVisible = false;
		mUI.hideInternal();
	}

	// ____________________________________________________________________________________
	@Override
	public void destroy()
	{
		hide();
	}

	// ____________________________________________________________________________________
	public void setId(int wid)
	{
		mWid = wid;
	}
	
	// ____________________________________________________________________________________
	@Override
	public int id()
	{
		return mWid;
	}
	
	// ____________________________________________________________________________________
	@Override
	public void printString(int attr, String str, int append, int color)
	{
	}

	// ____________________________________________________________________________________
	@Override
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

		float ofsX, ofsY;
		if(shouldLockView(tileW, tileH))
		{
			ofsX = mCanvasRect.left + (mCanvasRect.width() - tileW * TileCols) * .5f;

			float hDiff = mCanvasRect.height() - tileH * TileRows;
			float margin = Math.min(mLockTopMargin, hDiff);
			ofsY = mCanvasRect.top + (hDiff + margin) * .5f;
		}
		else
		{
			ofsX = mCanvasRect.left + (mCanvasRect.width() - tileW) * .5f - tileW * tileX;
			ofsY = mCanvasRect.top + (mCanvasRect.height() - tileH) * .5f - tileH * tileY;
		}

		if (mViewOffset.x != ofsX || mViewOffset.y != ofsY)
		{
			mViewOffset.set(ofsX, ofsY);
			mUI.invalidate();
		}
	}

	// ____________________________________________________________________________________
	private boolean shouldLockView()
	{
		return shouldLockView(mUI.getScaledTileWidth(), mUI.getScaledTileHeight());
	}

	// ____________________________________________________________________________________
	private boolean shouldLockView(float tileW, float tileH)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if(!prefs.getBoolean("lockView", true))
			return false;

		return tileW * TileCols	<= mCanvasRect.width() && tileH * TileRows <= mCanvasRect.height();
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
	}

	// ____________________________________________________________________________________
	public int dxFromKey(int c)
	{
		int d;
		int l = Character.toLowerCase(c);
		if(l == getUL() || l == getLEFT() || l == getDL())
			d = -1;
		else if(l == getUR() || l == getRIGHT() || l == getDR())
			d = 1;
		else
			d = 0;
		if(l != c)
			return d * 8;
		return d;
	}

	// ____________________________________________________________________________________
	public int dyFromKey(int c)
	{
		int d;
		int l = Character.toLowerCase(c);
		if(l == getUL() || l == getUP() || l == getUR())
			d = -1;
		else if(l == getDL() || l == getDOWN() || l == getDR())
			d = 1;
		else
			d = 0;
		if(l != c)
			return d * 8;
		return d;
	}

	// ____________________________________________________________________________________
	public void zoom(float amount)
	{
		if(amount == 0)
			return;
		zoomForced(amount);
	}

	// ____________________________________________________________________________________
	public void zoomForced(float amount)
	{
		if(mUI == null)
			return;

		float ofsX = (mViewOffset.x - mCanvasRect.left - mCanvasRect.width() * 0.5f) / mUI.getViewWidth();
		float ofsY = (mViewOffset.y - mCanvasRect.top - mCanvasRect.height() * 0.5f) / mUI.getViewHeight();

		mScaleCount = Math.min(Math.max(mScaleCount + amount, mMinScaleCount), mMaxScaleCount);

		mScale = (float) Math.pow(ZOOM_BASE, mScaleCount);

		if(canPan())
		{
			ofsX = mCanvasRect.left + ofsX * mUI.getViewWidth() + mCanvasRect.width() * 0.5f;
			ofsY = mCanvasRect.top + ofsY * mUI.getViewHeight() + mCanvasRect.height() * 0.5f;

			mViewOffset.set(ofsX, ofsY);
		}
		else
		{
			centerView(0, 0);
		}
		mUI.invalidate();
	}

	// ____________________________________________________________________________________
	private void resetZoom()
	{
		zoom(-mScaleCount);
		centerView(mCursorPos.x, mCursorPos.y);
	}

	// ____________________________________________________________________________________
	public void updateZoomLimits()
	{
		float minScale = mMinTileH / mUI.getBaseTileHeight();
		float maxScale = mMaxTileH / mUI.getBaseTileHeight();

		float amount;
		if(mMaxScaleCount - mMinScaleCount < 1)
			amount = 0.5f;
		else
			amount = (mScaleCount - mMinScaleCount) / (mMaxScaleCount - mMinScaleCount);

		mMinScaleCount = (float)(Math.log(minScale) / Math.log(ZOOM_BASE));
		mMaxScaleCount = (float)(Math.log(maxScale) / Math.log(ZOOM_BASE));

		mZoomStep = (mMaxScaleCount - mMinScaleCount) / 20;
		mScaleCount = mMinScaleCount + amount * (mMaxScaleCount - mMinScaleCount);

		zoomForced(0);
	}

	// ____________________________________________________________________________________
	@Override
	public void preferencesUpdated(SharedPreferences prefs) {
		int borderOpacity = prefs.getInt("borderOpacity", 50);
		int borderColor = Color.rgb(0xc9*borderOpacity/256, 0xc9*borderOpacity/256, 0xf9*borderOpacity/256);
		if(borderColor != mBorderColor)
		{
			mBorderColor = borderColor;
			mUI.invalidate();
		}
	}

	// ____________________________________________________________________________________
	public void pan(float dx, float dy)
	{
		if(canPan())
		{
			mViewOffset.offset(dx, dy);
			mUI.invalidate();
		}
	}

	// ____________________________________________________________________________________
	private boolean canPan()
	{
		return travelAfterPan() || !shouldLockView();
	}

	// ____________________________________________________________________________________
	enum Travel
	{
		Never,
		AfterPan,
		Always
	}

	// ____________________________________________________________________________________
	private boolean travelAfterPan()
	{
		return getTravelOption() == Travel.AfterPan;
	}

	// ____________________________________________________________________________________
	private Travel getTravelOption()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		// Convert old option
		if(prefs.contains("travelAfterPan"))
		{
			boolean oldValue = prefs.getBoolean("travelAfterPan", true);
			SharedPreferences.Editor editor = prefs.edit();
			editor.remove("travelAfterPan");
			editor.putString("travelOnClick", oldValue ? "1" : "0");
			editor.commit();
		}
		int setting = Util.parseInt(prefs.getString("travelOnClick", "1"), 1);
		if(setting == 0)
			return Travel.Never;
		if(setting == 1)
			return Travel.AfterPan;
		return Travel.Always;
	}

	// ____________________________________________________________________________________
	public static int clamp(int i, int min, int max)
	{
		return Math.min(Math.max(min, i), max);
	}

	// ____________________________________________________________________________________
	public void setRogueLevel(boolean bIsRogueLevel)
	{
		if(mIsRogue != bIsRogueLevel)
		{
			mIsRogue = bIsRogueLevel;
			updateZoomLimits();
		}
	}

	// ____________________________________________________________________________________
	public boolean isTTY()
	{
		return mIsRogue || !mTileset.hasTiles();
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
	@Override
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
		mTiles[y][x].ch[0] = mDecoder.decode(ch);
		mTiles[y][x].color = col;
		mTiles[y][x].overlay = (short)special;
		mUI.invalidateTile(x, y);
	}

	// ____________________________________________________________________________________
	@Override
	public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Input.Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(keyCode == KeyAction.ZoomIn || keyCode == KeyAction.ZoomOut)
		{
			float scale = mScaleCount;
			zoom(keyCode == KeyAction.ZoomIn ? mZoomStep : -mZoomStep);
			if(Math.abs(mScaleCount - scale) < 0.1 && repeatCount == 0)
				resetZoom();
			saveZoomLevel();
			return KeyEventResult.HANDLED;
		}

		return mUI.handleKeyDown(nhKey, keyCode) ? KeyEventResult.HANDLED : KeyEventResult.IGNORED;
	}

	// ____________________________________________________________________________________
	public boolean handleKeyUp(int keyCode)
	{
		return mUI.handleKeyUp(keyCode);
	}

	// ____________________________________________________________________________________
	public void setHealthColor(int color)
	{
		if(color != mHealthColor)
		{
			mHealthColor = color;
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
		prefs.edit().putFloat("zoomLevel", mScaleCount).commit();
	}
	
	// ____________________________________________________________________________________
	public void loadZoomLevel() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		float zoomLevel = 0;
		try
		{
			zoomLevel = prefs.getFloat("zoomLevel", 0.f);
		}
		catch(Exception e)
		{
		}
		zoom(zoomLevel - mScaleCount);
	}

	// ____________________________________________________________________________________ // 
	//																						// 
	// ____________________________________________________________________________________ // 
	private class UI extends View
	{
		private CountDownTimer mPressCountDown;
		private TextPaint mPaint;
		private final PointF mPointer0;
		private final PointF mPointer1;
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
		private final float mBaseTextSize;

		// ____________________________________________________________________________________
		public UI()
		{
			super(mContext);
			setFocusable(false);
			setFocusableInTouchMode(false);

			((ViewGroup)mContext.findViewById(R.id.map_frame)).addView(this, 0);
			mPaint = new TextPaint();
			mTypeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/monobold.ttf");
			mPaint.setTypeface(mTypeface);
			mPaint.setTextAlign(Align.LEFT);
			mPaint.setFilterBitmap(false);
			mBaseTextSize = 32.f;
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

			drawBorder(canvas);

			if(isTTY())
				drawAscii(canvas);
			else
				drawTiles(canvas);

			//drawGuides(canvas);
		}

		// ____________________________________________________________________________________
		protected void drawTiles(Canvas canvas)
		{			
			Rect dst = new Rect();

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

			float left = (float)Math.floor(mViewOffset.x + minTileX * tileW);
			float x = left;
			float y = (float)Math.floor(mViewOffset.y + minTileY * tileH);

			mPaint.setAntiAlias(false);

			for(int tileY = minTileY; tileY <= maxTileY; tileY++)
			{
				for(int tileX = minTileX; tileX <= maxTileX; tileX++)
				{
					dst.set((int)x, (int)y, (int)(x + tileW), (int)(y + tileH));
					Tile tile = mTiles[tileY][tileX];
					if(tile.glyph >= 0)
					{
						mPaint.setColor(0xffffffff);
						mTileset.drawTile(canvas, tile.glyph, dst, mPaint);
						Bitmap ovl = mTileset.getTileOverlay(tile.overlay);
						if(ovl != null)
							canvas.drawBitmap(ovl, mTileset.getOverlayRect(tile.overlay), dst, mPaint);
					}
					else
					{
						mPaint.setColor(0xff000000);
						canvas.drawRect(dst, mPaint);
					}

					x += tileW;
				}
				x = left;
				y += tileH;
			}

			drawCursor(canvas, tileW, tileH);
		}

		// ____________________________________________________________________________________
		protected void drawCursor(Canvas canvas, float tileW, float tileH)
		{
			float x = (float)Math.floor(mViewOffset.x);
			float y = (float)Math.floor(mViewOffset.y);

			if(mCursorPos.x >= 0 && mHealthColor != 0)
			{
				mPaint.setColor(mHealthColor);
				mPaint.setStyle(Style.STROKE);
				mPaint.setStrokeWidth(2);
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
			
			float x = (float)Math.floor(mViewOffset.x + minTileX * tileW);
			float y = (float)Math.floor(mViewOffset.y + minTileY * tileH);

			dst.set(x, y, x + tileW, y + tileH);

			// setup paint
			mPaint.setAntiAlias(true);

			for(int tileY = minTileY; tileY <= maxTileY; tileY++)
			{
				for(int tileX = minTileX; tileX <= maxTileX; tileX++)
				{
					Tile tile = mTiles[tileY][tileX];
					int fgColor = tile.color;
					int bgColor = 0xff000000;
					if(tileX == mCursorPos.x && tileY == mCursorPos.y)
					{
						if(mHealthColor != 0) {
							bgColor = mHealthColor;
							fgColor = 0xff000000;
						} else {
							bgColor = 0x00000000;
							fgColor = 0xffffffff;
						}
					}
					else if(tile.overlay != 0 && tile.glyph >= 0)
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
		private void drawBorder(Canvas canvas)
		{
			if((mBorderColor & 0xffffff) == 0)
				return;

			float tileW = getScaledTileWidth();
			float tileH = getScaledTileHeight();

			float borderSize = 2;

			Rect clipRect = new Rect();
			if (!canvas.getClipBounds(clipRect)) {
				clipRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
			}

			float x = (float)Math.floor(mViewOffset.x - 2 * borderSize);
			float y = (float)Math.floor(mViewOffset.y - 2 * borderSize);
			float w = (float)Math.ceil(tileW * TileCols + 4 * borderSize);
			float h = (float)Math.ceil(tileH * TileRows + 4 * borderSize);

			mPaint.setAntiAlias(false);
			mPaint.setColor(mBorderColor);
			mPaint.setStrokeWidth(borderSize);
			mPaint.setStyle(Style.STROKE);

			canvas.drawRect(x, y, x + w, y + h, mPaint);

			mPaint.setStyle(Style.FILL);
		}

		// ____________________________________________________________________________________
		private void drawGuides(Canvas canvas)
		{
			mPaint.setAntiAlias(true);

			float tileW = getScaledTileWidth();
			float tileH = getScaledTileHeight();

			float cx = mPlayerPos.x * tileW + mViewOffset.x + tileW * 0.5f;
			float cy = mPlayerPos.y * tileH + mViewOffset.y + tileH * 0.5f;

			float radius0 = mSelfRadius;
			float radius1 = 5 * radius0;

			mPaint.setColor(0x20ffffff);
			canvas.drawCircle(cx, cy, radius1, mPaint);

			mPaint.setColor(0xffffffff);
			mPaint.setStyle(Style.STROKE);
			mPaint.setStrokeWidth(2);
			canvas.drawCircle(cx, cy, radius0, mPaint);

			// r^2=x^2+y^2
			// PIE_SLICE=y/x
			// y = sqrt(r^2/(1+1/PIE_SLICE^2))
			float y0 = (float)Math.sqrt(radius0*radius0 / (1 + 1 / (PIE_SLICE * PIE_SLICE)));
			float y1 = (float)Math.sqrt(radius1*radius1 / (1 + 1 / (PIE_SLICE * PIE_SLICE)));
			float x0 = y0/ PIE_SLICE;
			float x1 = y1/ PIE_SLICE;
			canvas.drawLine( x0+cx, y0+cy, x1+cx, y1+cy, mPaint);
			canvas.drawLine( x0+cx,-y0+cy, x1+cx,-y1+cy, mPaint);
			canvas.drawLine(-x0+cx, y0+cy,-x1+cx, y1+cy, mPaint);
			canvas.drawLine(-x0+cx,-y0+cy,-x1+cx,-y1+cy, mPaint);

			canvas.drawLine( y0+cx,-x0+cy, y1+cx,-x1+cy, mPaint);
			canvas.drawLine( y0+cx, x0+cy, y1+cx, x1+cy, mPaint);
			canvas.drawLine(-y0+cx,-x0+cy,-y1+cx,-x1+cy, mPaint);
			canvas.drawLine(-y0+cx, x0+cy,-y1+cx, x1+cy, mPaint);

			mPaint.setStyle(Style.FILL);
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
					dir = event.getY() >= axis1 ? getDR() : event.getY() <= -axis1 ? getUR() : getRIGHT();
				else if(event.getX() <= -axis0)
					dir = event.getY() >= axis1 ? getDL() : event.getY() <= -axis1 ? getUL() : getLEFT();
				else if(event.getY() >= axis0)
					dir = event.getX() >= axis1 ? getDR() : event.getX() <= -axis1 ? getDL() : getDOWN();
				else if(event.getY() <= -axis0)
					dir = event.getX() >= axis1 ? getUR() : event.getX() <= -axis1 ? getUL() : getUP();

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
				float zoomAmount = (int)(1.5f * (newDist - mPointerDist) / mDisplayDensity);
				int newScale = (int)(mScaleCount + zoomAmount);

				if(zoomAmount != 0)
				{
					if(mIsStickyZoom && ((int)mScaleCount ^ newScale) < 0 && mStickyZoom == 0 || mStickyZoom != 0 && (mStickyZoom ^ (int)zoomAmount) >= 0)
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
			return (float)Math.sqrt(dx * dx + dy * dy);
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
				mIsViewPanned = false;
				return;
			}

			float tileW = getScaledTileWidth();
			float tileH = getScaledTileHeight();

			int tileX = (int)((x - mViewOffset.x) / tileW);
			int tileY = (int)((y - mViewOffset.y) / tileH);

			float cx = mPlayerPos.x * tileW + mViewOffset.x + tileW * 0.5f;
			float cy = mPlayerPos.y * tileH + mViewOffset.y + tileH * 0.5f;

			float dx = x - cx;
			float dy = y - cy;

			float distFromSelfSquared = dx*dx+dy*dy;

			switch(getTouchResult(tileX, tileY, distFromSelfSquared))
			{
				case SEND_MY_POS:
					tileX = mPlayerPos.x;
					tileY = mPlayerPos.y;
				// fall through
				case SEND_POS:
					tileX = clamp(tileX, 0, TileCols - 1);
					tileY = clamp(tileY, 0, TileRows - 1);
					if(mNHState.isMouseLocked())
						setCursorPos(tileX, tileY);
					mNHState.sendPosCmd(tileX, tileY);
				break;

				case SEND_DIR:
					if(allowDirectionalInput())
					{
						char dir = getDir(tileX, tileY, dx, dy, distFromSelfSquared);

						if(bLongClick && !mNHState.expectsDirection())
						{
							mNHState.sendKeyCmd('g');
							mNHState.sendKeyCmd(dir);
						}
						else
						{
							mNHState.sendDirKeyCmd(dir);
						}
					}
				break;
			}
			mIsViewPanned = false;
		}

		// ____________________________________________________________________________________
		private char getDir(int tileX, int tileY, float dx, float dy, float distFromSelfSquared)
		{
			if(mPlayerPos.equals(tileX, tileY) || distFromSelfSquared < mSelfRadiusSquared)
				return '.';

			float adx = Math.abs(dx);
			float ady = Math.abs(dy);

			char dir;
			if(ady < PIE_SLICE * adx)
				dir = dx > 0 ? getRIGHT() : getLEFT();
			else if(adx < PIE_SLICE * ady)
				dir = dy < 0 ? getUP() : getDOWN();
			else if(dx > 0)
				dir = dy < 0 ? getUR() : getDR();
			else
				dir = dy < 0 ? getUL() : getDL();

			return dir;
		}

		// ____________________________________________________________________________________
		private void sendDirKeyCmd(int c)
		{
			if(mIsBlocking || mNHState.isMouseLocked() || mIsdPadCenterDown || mIsTrackBallDown)
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
		private boolean allowDirectionalInput()
		{
			if(!mNHState.isDPadVisible())
				return true;

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			return  prefs.getBoolean("allowMapDir", false);
		}

		// ____________________________________________________________________________________
		private TouchResult getTouchResult(int tileX, int tileY, float distFromSelfSquared)
		{
			if(mNHState.isMouseLocked())
				return TouchResult.SEND_POS;

			if(mNHState.expectsDirection())
				return TouchResult.SEND_DIR;

			if(mPlayerPos.equals(tileX, tileY) || distFromSelfSquared < mSelfRadiusSquared)
				return TouchResult.SEND_MY_POS;

			Travel travelOption = getTravelOption();

			if(travelOption == Travel.Never)
				return TouchResult.SEND_DIR;

			if(travelOption == Travel.Always)
				return TouchResult.SEND_POS;

			if(!mIsViewPanned)
				return TouchResult.SEND_DIR;

			// Don't send pos command if clicking within a few tiles from the player

			int dx = Math.abs(tileX - mPlayerPos.x);
			int dy = Math.abs(tileY - mPlayerPos.y);

			// . . . . .
			// . . . . .
			// . . @ . .
			// . . . . .
			// . . . . .
			if(dx <= 3 && dy <= 3)
				return TouchResult.SEND_DIR;

			return TouchResult.SEND_POS;
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

			if(mNHState.isMouseLocked())
			{
				if(mPickChars.contains((char)nhKey))
				{
					onCursorPosClicked();
					return true;
				}
				if(mCancelKeys.contains((char)nhKey))
				{
					mNHState.sendDirKeyCmd(nhKey);
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
				return (float)Math.floor(w) - 1;
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
				return (float)Math.floor(metrics.descent - metrics.ascent);
			}

			return mTileset.getTileHeight() * mScale;
		}

		// ____________________________________________________________________________________
		private float getBaseTileHeight()
		{
			if(isTTY())
				return mBaseTextSize;
			return mTileset.getTileHeight();
		}

	}
}
