package com.tbd.NetHack;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

public class NHW_Map extends View implements NH_Window
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

	private Activity m_context;
	private NetHackIO m_io;
	private Tile[][] m_tiles;
	private float m_scale;
	private int m_nScale;
	private int m_stickyZoom;
	private boolean m_bStickyZoom;
	private Paint m_paint;
	private Tileset m_tileset;
	private PointF m_pointer0;
	private PointF m_pointer1;
	private int m_pointerId0;
	private int m_pointerId1;
	private float m_pointerDist;
	private PointF m_viewOffset;
	private ZoomPanMode m_zoomPanMode;
	private boolean m_bViewIsPanned;
	private boolean m_bMouseIsLocked;
	private boolean m_bdPadCenterDown; // don't know if both of these can exist on the same device
	private boolean m_bTrackBallDown;
	private boolean m_bPannedSinceDown;
	private PointF m_canvasSize;
	private Point m_playerPos;
	private CountDownTimer m_pressCountDown;
	private Point m_cursorPos;
	private boolean m_bRogue;
	private boolean m_bBlocking;
	private CmdPanel m_cmdPanel;
	private DPadOverlay m_dPad;
	private boolean m_bdPadVisible;
	private NHW_Status m_status;
	private boolean m_bControlsHidden;
	private int m_healthLevel;

	// ____________________________________________________________________________________
	public NHW_Map(Context context)
	{
		super(context);
		Init(null, null, null, null, null);
	}

	// ____________________________________________________________________________________
	public NHW_Map(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		Init(null, null, null, null, null);
	}

	// ____________________________________________________________________________________
	public void Init(Activity context, NetHackIO io, Tileset tileset, DPadOverlay dPad, NHW_Status status)
	{
		m_context = context;
		m_io = io;
		m_tileset = tileset;
		m_tiles = new Tile[TileRows][TileCols];
		for(Tile[] row : m_tiles)
		{
			for(int i = 0; i < row.length; i++)
				row[i] = new Tile();
		}
		m_nScale = 0;
		m_scale = 1.f;
		m_pointer0 = new PointF();
		m_pointer1 = new PointF();
		m_pointerId0 = -1;
		m_pointerId1 = -1;
		m_viewOffset = new PointF();
		m_zoomPanMode = ZoomPanMode.Idle;
		m_pressCountDown = null;
		m_bViewIsPanned = false;
		m_bMouseIsLocked = false;
		m_bdPadCenterDown = false;
		m_bTrackBallDown = false;
		m_bPannedSinceDown = false;
		m_canvasSize = new PointF();
		m_playerPos = new Point();
		m_cursorPos = new Point(-1, -1);
		m_bRogue = false;
		m_paint = new Paint();
		m_dPad = dPad;
		if(io != null)
		{
			m_cmdPanel = new CmdPanel(io, this);
			m_cmdPanel.Hide();
		}
		m_status = status;
		Clear();
		setFocusable(false);
		setFocusableInTouchMode(false);
	}

	// ____________________________________________________________________________________
	public void onConfigurationChanged(Configuration newConfig)
	{
		m_cmdPanel.onConfigurationChanged(newConfig);
	}

	// ____________________________________________________________________________________
	public void Show(boolean bBlocking)
	{
		setVisibility(View.VISIBLE);
		if(!m_bControlsHidden)
			m_cmdPanel.Show();
		invalidate();
		SetBlocking(bBlocking);
	}

	// ____________________________________________________________________________________
	public void Hide()
	{
		setVisibility(View.INVISIBLE);
	}

	// ____________________________________________________________________________________
	public void Clear()
	{
		for(Tile[] row : m_tiles)
		{
			for(int i = 0; i < row.length; i++)
				row[i].glyph = -1;
		}
	}

	// ____________________________________________________________________________________
	public void ShowDPad()
	{
		m_bdPadVisible = true;
		m_cmdPanel.Hide();
		m_dPad.Show();
	}

	// ____________________________________________________________________________________
	public void HideDPad()
	{
		if(m_bdPadVisible)
		{
			m_bdPadVisible = false;
			m_cmdPanel.Show();
			m_dPad.Hide();
		}
	}

	// ____________________________________________________________________________________
	public void SetRogueLevel(boolean bIsRogueLevel)
	{
		m_bRogue = bIsRogueLevel;
	}

	// ____________________________________________________________________________________
	public void SetBlocking(boolean bBlocking)
	{
		if(bBlocking)
		{
			HideControls();
			m_context.findViewById(R.id.nh_blockmsg).setVisibility(View.VISIBLE);
		}
		else
		{
			ShowControls();
			if(m_bBlocking)
				m_io.SendKeyCmd(' ');

			m_context.findViewById(R.id.nh_blockmsg).setVisibility(View.GONE);
		}
		m_bBlocking = bBlocking;
	}

	private void HideControls()
	{
		m_bControlsHidden = true;
		m_status.Hide();
		m_cmdPanel.Hide();
	}

	private void ShowControls()
	{
		m_bControlsHidden = false;
		m_status.Show(false);
		m_cmdPanel.Show();
	}

	// ____________________________________________________________________________________
	public void SetCursorPos(int x, int y)
	{
		if(m_cursorPos.x != x || m_cursorPos.y != y)
		{
			//InvalidateTile(m_cursorPos.x, m_cursorPos.y);
			m_cursorPos.x = clamp(x, 0, TileCols - 1);
			m_cursorPos.y = clamp(y, 0, TileRows - 1);
			InvalidateTile(m_cursorPos.x, m_cursorPos.y);
			//invalidate();
		}
	}

	// ____________________________________________________________________________________
	public void PrintString(TextAttr attr, String str)
	{
		throw new UnsupportedOperationException();
	}

	// ____________________________________________________________________________________
	public void PrintTile(final int x, final int y, final int tile, final int ch, final int col, final int special)
	{
		m_tiles[y][x].glyph = tile;
		m_tiles[y][x].ch[0] = (char)ch;
		m_tiles[y][x].color = col;
		m_tiles[y][x].overlay = (short)special;
		InvalidateTile(x, y);
	}

	// ____________________________________________________________________________________
	public void InvalidateTile(int tileX, int tileY)
	{
		float tileW = GetScaledTileWidth();
		float tileH = GetScaledTileHeight();

		int ofsX = (int)(m_viewOffset.x + tileW * tileX);
		int ofsY = (int)(m_viewOffset.y + tileH * tileY);

		invalidate(new Rect(ofsX - 4, ofsY - 4, (int)(ofsX + tileW) + 4, (int)(ofsY + tileH) + 4));
		//invalidate();
	}

	// ____________________________________________________________________________________
	public void Cliparound(final int tileX, final int tileY, final int playerX, final int playerY)
	{
		m_playerPos.x = playerX;
		m_playerPos.y = playerY;

		CenterView(tileX, tileY);
	}

	// ____________________________________________________________________________________
	public void CenterView(final int tileX, final int tileY)
	{
		float tileW = GetScaledTileWidth();
		float tileH = GetScaledTileHeight();

		float ofsX = (m_canvasSize.x - tileW) * .5f - tileW * tileX;
		float ofsY = (m_canvasSize.y - tileH) * .5f - tileH * tileY;

		if(m_viewOffset.x != ofsX || m_viewOffset.y != ofsY)
		{
			m_viewOffset.set(ofsX, ofsY);
			invalidate();
		}
	}

	// ____________________________________________________________________________________
	public void LockMouse()
	{
		m_bMouseIsLocked = true;
	}

	// ____________________________________________________________________________________
	public void Zoom(int amount)
	{
		if(amount == 0)
			return;

		float ofsX = (m_viewOffset.x - m_canvasSize.x * 0.5f) / GetViewWidth();
		float ofsY = (m_viewOffset.y - m_canvasSize.y * 0.5f) / GetViewHeight();

		m_nScale += amount;
		if(m_nScale > 139)
			m_nScale = 139;
		if(m_nScale < -200)
			m_nScale = -200;

		m_scale = (float)Math.pow(1.005, m_nScale);

		if(m_scale > 2.0f)
			m_scale = 2.0f;

		ofsX = ofsX * GetViewWidth() + m_canvasSize.x * 0.5f;
		ofsY = ofsY * GetViewHeight() + m_canvasSize.y * 0.5f;

		m_viewOffset.set(ofsX, ofsY);

		invalidate();
	}

	// ____________________________________________________________________________________
	public void ResetZoom()
	{
		Zoom(-m_nScale);
		CenterView(m_cursorPos.x, m_cursorPos.y);
	}

	// ____________________________________________________________________________________
	public void Pan(float dx, float dy)
	{
		m_viewOffset.offset(dx, dy);
		invalidate();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if(m_tileset == null)
			return;

		Rect src = new Rect();
		RectF dst = new RectF();

		float tileW = GetScaledTileWidth();
		float tileH = GetScaledTileHeight();

		float x = FloatMath.floor(m_viewOffset.x);// + .5f;
		float y = FloatMath.floor(m_viewOffset.y);// + .5f;

		dst.set(x, y, x + tileW, y + tileH);

		// setup paint
		int textOfsY = 0;
		if(m_bRogue)
		{
			//m_paint.setAntiAlias(false);
			m_paint.setAntiAlias(true);
			m_paint.setFilterBitmap(false);
			m_paint.setTypeface(Typeface.DEFAULT_BOLD);
			m_paint.setTextSize(24.f * m_scale); // tileSize 
			m_paint.setTextAlign(Align.CENTER);
			textOfsY = (int)((m_paint.descent() - m_paint.ascent() - tileH) * .5f - 1);
			if(textOfsY > 0)
				textOfsY = 0;
		}
		else
		{
			m_paint.setAntiAlias(false);
			m_paint.setFilterBitmap(false);
		}

		for(Tile[] row : m_tiles)
		{
			for(Tile tile : row)
			{
				if(tile.glyph >= 0)
				{
					int ofs = m_tileset.GetTileBitmapOffset(tile.glyph);
					src.left = (ofs >> 16) & 0xffff;
					src.top = ofs & 0xffff;
					src.right = src.left + m_tileset.GetTileWidth();
					src.bottom = src.top + m_tileset.GetTileHeight();

					if(m_bRogue)
					{
						m_paint.setColor(tile.color);
						canvas.drawText(tile.ch, 0, 1, (dst.left + dst.right) * .5f, dst.bottom - m_paint.descent() + textOfsY, m_paint);
					}
					else
					{
						m_paint.setColor(0xffffffff);
						canvas.drawBitmap(m_tileset.GetBitmap(), src, dst, m_paint);
					}
					Bitmap ovl = m_tileset.GetTileOverlay(tile.overlay);
					if(ovl != null)
						canvas.drawBitmap(ovl, m_tileset.getOverlayRect(tile.overlay), dst, m_paint);
				}
				else
				{
					m_paint.setColor(0);
					canvas.drawRect(dst, m_paint);
				}

				dst.offset(tileW, 0);
			}
			dst.left = x;
			dst.right = x + tileW;
			dst.offset(0, tileH);
		}

		// draw cursor
		if(m_cursorPos.x >= 0)
		{
			if(m_healthLevel <= 0)
				m_paint.setColor(0x80B00020);
			else if(m_healthLevel == 1)
				m_paint.setColor(0x80CABF00);
			else
				m_paint.setColor(0x8020A020);
			m_paint.setStyle(Style.STROKE);
			dst.left = x + m_cursorPos.x * tileW + 0.5f;
			dst.top = y + m_cursorPos.y * tileH + 0.5f;
			dst.right = dst.left + tileW - 2.0f;
			dst.bottom = dst.top + tileH - 2.0f;
			canvas.drawRect(dst, m_paint);
			m_paint.setStyle(Style.FILL);
		}

	}

	// ____________________________________________________________________________________
	public static int clamp(int i, int min, int max)
	{
		return Math.min(Math.max(min, i), max);
	}

	// ____________________________________________________________________________________
	private boolean ShouldSendPosOnTouch(int tileX, int tileY)
	{
		if(m_playerPos.equals(tileX, tileY))
			return true;

		if(!m_bViewIsPanned)
			return false;

		// Don't send pos command if clicking a position right next to our hero, as it can do nasty stuff (like autokicking a door)
		if(m_playerPos.equals(tileX - 1, tileY - 1) || m_playerPos.equals(tileX, tileY - 1) || m_playerPos.equals(tileX + 1, tileY - 1))
			return false;
		if(m_playerPos.equals(tileX - 1, tileY + 1) || m_playerPos.equals(tileX, tileY + 1) || m_playerPos.equals(tileX + 1, tileY + 1))
			return false;
		if(m_playerPos.equals(tileX - 1, tileY) || m_playerPos.equals(tileX + 1, tileY))
			return false;

		return true;// || !m_cursorPos.equals(m_playerPos);
	}

	// ____________________________________________________________________________________
	private void OnTouched(float x, float y, boolean bLongClick)
	{
		if(m_bBlocking)
		{
			if(!bLongClick)
				SetBlocking(false);
			return;
		}

		float tileW = GetScaledTileWidth();
		float tileH = GetScaledTileHeight();

		int tileX = (int)((x - m_viewOffset.x) / tileW);
		int tileY = (int)((y - m_viewOffset.y) / tileH);

		if(m_bMouseIsLocked || ShouldSendPosOnTouch(tileX, tileY))
		{
			tileX = clamp(tileX, 0, TileCols - 1);
			tileY = clamp(tileY, 0, TileRows - 1);
			if(m_bMouseIsLocked)
				SetCursorPos(tileX, tileY);
			m_io.SendPosCmd(tileX, tileY);
			m_bMouseIsLocked = false;
		}
		else
		{
			int dx = tileX - m_playerPos.x;
			int dy = tileY - m_playerPos.y;
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

			m_io.SendKeyCmd(dir);
		}
		m_bViewIsPanned = false;
	}

	// ____________________________________________________________________________________
	private void OnCursorPosClicked()
	{
		if(m_bBlocking)
		{
			SetBlocking(false);
			return;
		}

		Log.print(String.format("cursor pos clicked: %dx%d", m_cursorPos.x, m_cursorPos.y));
		m_io.SendPosCmd(m_cursorPos.x, m_cursorPos.y);
		m_bMouseIsLocked = false;
	}

	// ____________________________________________________________________________________
	private int GetViewWidth()
	{
		return (int)(TileCols * GetScaledTileWidth() + 0.5f);
	}

	// ____________________________________________________________________________________
	private int GetViewHeight()
	{
		return (int)(TileRows * GetScaledTileHeight() + 0.5f);
	}

	// ____________________________________________________________________________________
	private float GetScaledTileWidth()
	{
		// prevent designer crash
		if(m_tileset == null)
			return 32.f;

		return m_tileset.GetTileWidth() * m_scale;
	}

	// ____________________________________________________________________________________
	private float GetScaledTileHeight()
	{
		// prevent designer crash
		if(m_tileset == null)
			return 32.f;

		return m_tileset.GetTileHeight() * m_scale;
	}

	// ____________________________________________________________________________________
	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
	}

	// ____________________________________________________________________________________
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int w = Math.max(GetViewWidth(), getSuggestedMinimumWidth());
		int h = Math.max(GetViewHeight(), getSuggestedMinimumHeight());

		int wMode = MeasureSpec.getMode(widthMeasureSpec);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);

		int wConstraint = MeasureSpec.getSize(widthMeasureSpec);
		int hConstraint = MeasureSpec.getSize(heightMeasureSpec);

		Log.print(String.format("Suggested: %d x %d", wConstraint, hConstraint));
		
		if(wMode == MeasureSpec.AT_MOST && w > wConstraint || wMode == MeasureSpec.EXACTLY)
			w = wConstraint;
		if(hMode == MeasureSpec.AT_MOST && h > hConstraint || hMode == MeasureSpec.EXACTLY)
			h = hConstraint;

		m_canvasSize.x = w;
		m_canvasSize.y = h;
		setMeasuredDimension(w, h);

		CenterView(m_cursorPos.x, m_cursorPos.y);
	}

	// ____________________________________________________________________________________
	public static int dxFromKey(char c)
	{
		c = Character.toLowerCase(c);
		if(c == UL || c == LEFT || c == DL)
			return -1;
		if(c == UR || c == RIGHT || c == DR)
			return 1;
		return 0;
	}

	// ____________________________________________________________________________________
	public static int dyFromKey(char c)
	{
		c = Character.toLowerCase(c);
		if(c == UL || c == UP || c == UR)
			return -1;
		if(c == DL || c == DOWN || c == DR)
			return 1;
		return 0;
	}

	// ____________________________________________________________________________________
	private void SendDirKeyCmd(char c)
	{
		if(m_bBlocking)
		{
			SetBlocking(false);
			return;
		}

		if(m_bMouseIsLocked || m_bdPadCenterDown || m_bTrackBallDown)
		{
			// Log.print("pan with cursor");
			m_bPannedSinceDown = true;
			m_bViewIsPanned = true;
			SetCursorPos(m_cursorPos.x + dxFromKey(c), m_cursorPos.y + dyFromKey(c));
			CenterView(m_cursorPos.x, m_cursorPos.y);
		}
		else
		{
			m_io.SendKeyCmd(c);
		}
	}

	// ____________________________________________________________________________________
	public boolean HandleKeyDown(int keyCode, KeyEvent event)
	{
		if(m_bBlocking)
		{
			SetBlocking(false);
			return true;
		}
		
		if(m_cmdPanel.HandleKeyDown(keyCode, event))
			return true;

		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
		{
			m_bdPadCenterDown = true;
			m_bPannedSinceDown = false;
		}
		else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			Zoom(20);
		}
		else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
		{
			Zoom(-20);
		}
		else
		{
			// TODO move this to NethackIO or CmdPanel
			char dir = 0;
			if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
				dir = LEFT;
			else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
				dir = RIGHT;
			else if(keyCode == KeyEvent.KEYCODE_DPAD_UP)
				dir = UP;
			else if(keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
				dir = DOWN;

			if(dir != 0)
			{
				if(event.isShiftPressed())
					dir = Character.toUpperCase(dir);
				SendDirKeyCmd(dir);
			}
			else
			{
				int u = event.getUnicodeChar();
				// TODO better filter
				if(u > 0 && u < 256)
				{
					Log.print((char)u);
					m_io.SendKeyCmd((char)u);
				}
				else
				{
					return false;
				}
			}
		}
		return true;
	}

	// ____________________________________________________________________________________
	public boolean HandleKeyUp(int keyCode)
	{
		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
		{
			if(m_bdPadCenterDown && !m_bPannedSinceDown)
				OnCursorPosClicked();
			m_bdPadCenterDown = false;
			return true;
		}
		else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
		{
			// Prevent default system sound from playing
			return true;
		}
		return false;
	}

	// ____________________________________________________________________________________
	@Override
	public boolean onTrackballEvent(MotionEvent event)
	{
		switch(getAction(event))
		{
		case MotionEvent.ACTION_DOWN:
			m_bTrackBallDown = true;
			m_bPannedSinceDown = false;
		break;

		case MotionEvent.ACTION_UP:
			if(!m_bPannedSinceDown)
				OnCursorPosClicked();
			m_bTrackBallDown = false;
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
				SendDirKeyCmd(dir);
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
			m_zoomPanMode = ZoomPanMode.Pressed;
			if(m_pressCountDown != null)
				m_pressCountDown.cancel();
			m_pressCountDown = new CountDownTimer(ViewConfiguration.getLongPressTimeout(), 10000)
			{
				@Override
				public void onTick(long millisUntilFinished)
				{
				}

				@Override
				public void onFinish()
				{
					if(m_zoomPanMode == ZoomPanMode.Pressed)
						OnTouched(m_pointer0.x, m_pointer0.y, true);
					m_zoomPanMode = ZoomPanMode.Idle;
				}
			}.start();

			idx = getActionIndex(event);
			m_pointerId0 = event.getPointerId(idx);
			m_pointerId1 = -1;
			m_pointer0.set(event.getX(idx), event.getY(idx));
		break;

		case MotionEvent.ACTION_UP:
			if(m_zoomPanMode == ZoomPanMode.Pressed)
			{
				m_pressCountDown.cancel();
				OnTouched(m_pointer0.x, m_pointer0.y, false);
			}
			m_zoomPanMode = ZoomPanMode.Idle;
		break;

		case MotionEvent.ACTION_POINTER_DOWN:
			if(m_pointerId1 < 0 && (m_zoomPanMode == ZoomPanMode.Pressed || m_zoomPanMode == ZoomPanMode.Panning))
			{
				// second pointer down, enter zoom mode
				m_pressCountDown.cancel();
				m_zoomPanMode = ZoomPanMode.Zooming;
				m_bStickyZoom = false;
				idx = getActionIndex(event);
				m_pointerId1 = event.getPointerId(idx);
				m_pointer1.set(event.getX(idx), event.getY(idx));
				m_pointerDist = GetPointerDist(event);
			}
		break;

		case MotionEvent.ACTION_POINTER_UP:

			idx = getActionIndex(event);

			int idx0 = event.findPointerIndex(m_pointerId0);
			int idx1 = event.findPointerIndex(m_pointerId1);

			if(m_zoomPanMode == ZoomPanMode.Zooming)
			{
				// Released one of the first two pointers. Ignore other pointers
				if(idx == idx0)
					idx = idx1;
				else if(idx == idx1)
					idx = idx0;

				if(idx == idx0 || idx == idx1)
				{
					// Reset start position for the first pointer
					m_zoomPanMode = ZoomPanMode.Panning;
					m_pointer0.set(event.getX(idx), event.getY(idx));
					m_pointerId0 = event.getPointerId(idx);
					m_pointerId1 = -1;
				}
			}
			else if(m_zoomPanMode == ZoomPanMode.Panning && idx == idx0)
			{
				// Released the last pointer of the first two. Ignore other pointers
				m_pressCountDown.cancel();
				m_zoomPanMode = ZoomPanMode.Idle;
			}

		break;

		case MotionEvent.ACTION_MOVE:
			if(m_zoomPanMode == ZoomPanMode.Pressed)
				TryStartPan(event);
			else if(m_zoomPanMode == ZoomPanMode.Panning)
				CalcPan(event);
			else if(m_zoomPanMode == ZoomPanMode.Zooming)
				CalcZoom(event);
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
	private void TryStartPan(MotionEvent event)
	{
		int idx = event.findPointerIndex(m_pointerId0);

		float dx = event.getX(idx) - m_pointer0.x;
		float dy = event.getY(idx) - m_pointer0.y;

		float th = ViewConfiguration.get(getContext()).getScaledTouchSlop();

		if(Math.abs(dx) > th || Math.abs(dy) > th)
		{
			m_pressCountDown.cancel();
			m_zoomPanMode = ZoomPanMode.Panning;
			m_bViewIsPanned = true;
		}
	}

	// ____________________________________________________________________________________
	private void CalcPan(MotionEvent event)
	{
		int idx = event.findPointerIndex(m_pointerId0);

		float dx = event.getX(idx) - m_pointer0.x;
		float dy = event.getY(idx) - m_pointer0.y;

		Pan(dx, dy);

		m_pointer0.set(event.getX(idx), event.getY(idx));
	}

	// ____________________________________________________________________________________
	private void CalcZoom(MotionEvent event)
	{
		int idx0 = event.findPointerIndex(m_pointerId0);
		int idx1 = event.findPointerIndex(m_pointerId1);

		// Calc average movement of the two cursors and pan accordingly.
		// Don't pan if the cursors move in opposite direction on respective axis.
		float dx0 = event.getX(idx0) - m_pointer0.x;
		float dy0 = event.getY(idx0) - m_pointer0.y;
		float dx1 = event.getX(idx1) - m_pointer1.x;
		float dy1 = event.getY(idx1) - m_pointer1.y;

		if(dx0 > 0 && dx1 < 0 || dx0 < 0 && dx1 > 0)
			dx0 = 0;
		else
			dx0 = (dx0 + dx1) * 0.5f;

		if(dy0 > 0 && dy1 < 0 || dy0 < 0 && dy1 > 0)
			dy0 = 0;
		else
			dy0 = (dy0 + dy1) * 0.5f;

		Pan(dx0, dy0);

		// Calc dist between cursors and zoom accordingly.
		float newDist = GetPointerDist(event);
		if(newDist > 5)
		{
			int zoomAmount = (int)(newDist - m_pointerDist);
			int newScale = m_nScale + zoomAmount;

			if(zoomAmount != 0)
			{
				if(m_bStickyZoom && (m_nScale ^ newScale) < 0 && m_stickyZoom == 0 || m_stickyZoom != 0 && (m_stickyZoom ^ zoomAmount) >= 0)
				{
					m_stickyZoom += zoomAmount;
					if(Math.abs(m_stickyZoom) < 50)
						zoomAmount = -m_nScale;
					else
						m_stickyZoom = 0;
				}
				else
				{
					m_stickyZoom = 0;
					m_bStickyZoom = true;
				}

				Zoom(zoomAmount);
			}
			
			m_pointerDist = newDist;

			m_pointer0.set(event.getX(idx0), event.getY(idx0));
			m_pointer1.set(event.getX(idx1), event.getY(idx1));
		}
	}

	// ____________________________________________________________________________________
	private float GetPointerDist(MotionEvent event)
	{
		int idx0 = event.findPointerIndex(m_pointerId0);
		int idx1 = event.findPointerIndex(m_pointerId1);
		float dx = event.getX(idx0) - event.getX(idx1);
		float dy = event.getY(idx0) - event.getY(idx1);
		return FloatMath.sqrt(dx * dx + dy * dy);
	}

	// ____________________________________________________________________________________
	public void PreferencesUpdated()
	{
		m_cmdPanel.PreferencesUpdated();
	}

	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		m_cmdPanel.onCreateContextMenu(menu, v, menuInfo);		
	}

	// ____________________________________________________________________________________
	public void onContextItemSelected(android.view.MenuItem item)
	{
		m_cmdPanel.onContextItemSelected(item);
	}

	// ____________________________________________________________________________________
	public void SetHealthLevel(int hp, int hpMax)
	{
		int healthLevel;		
		if(hp < hpMax / 2)
			healthLevel = 0;
		else if(hp < (3 * hpMax) / 4)
			healthLevel = 1;
		else
			healthLevel = 2;

		if(healthLevel != m_healthLevel)
		{
			m_healthLevel = healthLevel;
			InvalidateTile(m_cursorPos.x, m_cursorPos.y);
		}
	}

}
