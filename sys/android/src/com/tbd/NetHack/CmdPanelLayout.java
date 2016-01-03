package com.tbd.NetHack;

import java.util.ArrayList;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.*;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

public class CmdPanelLayout extends FrameLayout
{
	private class Panel
	{
		int idx;
		CmdPanel panel;
		ViewGroup portScrollView;
		ViewGroup landScrollView;
		boolean portActive;
		boolean landActive;
		public String cmds;
		public int pLoc;
		public int lLoc;
		public int opacity;
	}

	private ArrayList<Panel> mPanelCmds = new ArrayList<Panel>();
	private NetHack mContext;
	private NH_State mState;
	private boolean mPortraitMode;
	private boolean mShowPanels = true;
	private Rect mViewRect = new Rect();
	private View mViewArea;

	// ____________________________________________________________________________________
	public CmdPanelLayout(Context context)
	{
		super(context);
	}

	// ____________________________________________________________________________________
	public CmdPanelLayout(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	// ____________________________________________________________________________________
	public CmdPanelLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	// ____________________________________________________________________________________
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int count = getChildCount();

		int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
		int viewHeight = MeasureSpec.getSize(heightMeasureSpec);

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		if(widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED) {

			Point size = new Point();
			if(android.os.Build.VERSION.SDK_INT >= 13) {
				mContext.getWindowManager().getDefaultDisplay().getSize(size);
			} else if(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
				size.x = mContext.getWindowManager().getDefaultDisplay().getWidth();
				size.y = mContext.getWindowManager().getDefaultDisplay().getHeight();
			}

			if(widthMode == MeasureSpec.UNSPECIFIED)
				viewWidth = size.x;
			if(heightMode == MeasureSpec.UNSPECIFIED)
				viewHeight = size.y;
		}

		int contentWidth = viewWidth;
		int contentHeight = viewHeight;

		int paddingWidth = getPaddingLeft() + getPaddingRight();
		int paddingHeight = getPaddingTop() + getPaddingBottom();

		contentWidth -= paddingWidth;
		contentHeight -= paddingHeight;

		for(int i = count - 1; i >= 0; i--)
		{
			final View child = getChildAt(i);

			if(child.getVisibility() == GONE)
				continue;

			measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

			final int childWidth = child.getMeasuredWidth();
			final int childHeight = child.getMeasuredHeight();
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();
			final int gravity = lp.gravity;

			if(gravity != -1)
			{
				switch(gravity & Gravity.HORIZONTAL_GRAVITY_MASK)
				{
					case Gravity.LEFT:
						if((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL)
							contentWidth -= childWidth;
						break;
					case Gravity.RIGHT:
						if((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL)
							contentWidth -= childWidth;
						break;
				}

				switch(gravity & Gravity.VERTICAL_GRAVITY_MASK)
				{
					case Gravity.TOP:
						if((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL)
							contentHeight -= childHeight;
						break;
					case Gravity.BOTTOM:
						if((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL)
							contentHeight -= childHeight;
						break;
				}
			}

			if(lp.width == LayoutParams.MATCH_PARENT || lp.height == LayoutParams.MATCH_PARENT)
			{
				int childWidthMeasureSpec;
				int childHeightMeasureSpec;

				if(lp.width == LayoutParams.MATCH_PARENT) {
					childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(contentWidth - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
				} else {
					childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, paddingWidth + lp.leftMargin + lp.rightMargin, lp.width);
				}

				if(lp.height == LayoutParams.MATCH_PARENT) {
					childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(contentHeight - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
				} else {
					childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, paddingHeight + lp.topMargin + lp.bottomMargin, lp.height);
				}

				child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
			}
		}

		setMeasuredDimension(viewWidth, viewHeight);
	}

	// ____________________________________________________________________________________
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		final int count = getChildCount();

		int parentLeft = getPaddingLeft();
		int parentRight = right - left - getPaddingRight();

		int parentTop = getPaddingTop();
		int parentBottom = bottom - top - getPaddingBottom();

		for(int i = count - 1; i >= 0; i--)
		{
			final View child = getChildAt(i);

			// Special case to keep track of the map's viewable area
			if(child == mViewArea)
			{
				if(mViewRect.left != parentLeft || mViewRect.right != parentRight || mViewRect.top != parentTop || mViewRect.bottom != parentBottom)
				{
					mViewRect.set(parentLeft, parentTop, parentRight, parentBottom);
					mState.viewAreaChanged(mViewRect);
				}
			}

			if(child.getVisibility() == GONE)
				continue;

			final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)child.getLayoutParams();

			int childLeft, childRight, childTop, childBottom;

			int childWidth = child.getMeasuredWidth();
			int childHeight = child.getMeasuredHeight();

			final int gravity = lp.gravity;

			if(gravity != -1)
			{
				switch(gravity & Gravity.HORIZONTAL_GRAVITY_MASK)
				{
				case Gravity.LEFT:
					childLeft = parentLeft;
					childRight = parentLeft + childWidth;
					if((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL)
						parentLeft = childRight;
				break;
				case Gravity.RIGHT:
					childLeft = parentRight - childWidth;
					childRight = parentRight;
					if((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL)
						parentRight = childLeft;
				break;
				case Gravity.FILL_HORIZONTAL:
					childLeft = parentLeft;
					childRight = parentRight;
				break;
				default:
					childLeft = parentLeft + parentRight - childWidth >> 1;
					childRight = childLeft + childWidth;
				}

				switch(gravity & Gravity.VERTICAL_GRAVITY_MASK)
				{
				case Gravity.TOP:
					childTop = parentTop;
					childBottom = parentTop + childHeight;
					if((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL)
						parentTop = childBottom;
				break;
				case Gravity.BOTTOM:
					childTop = parentBottom - childHeight;
					childBottom = parentBottom;
					if((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL)
						parentBottom = childTop;
				break;
				case Gravity.FILL_VERTICAL:
					childTop = parentTop;
					childBottom = parentBottom;
				break;
				default:
					childTop = parentTop + parentBottom - childHeight >> 1;
					childBottom = childTop + childHeight;					
				}
			}
			else
			{
				childLeft = parentLeft;
				childRight = parentRight;
				childTop = parentTop;
				childBottom = parentBottom;
			}

			child.layout(childLeft, childTop, childRight, childBottom);
		}
	}

	// ____________________________________________________________________________________
	public void preferencesUpdated(SharedPreferences prefs)
	{
		loadPanels(prefs);
	}

	// ____________________________________________________________________________________
	public void show()
	{
		mShowPanels = true; 
		for(Panel p : mPanelCmds)
		{
			if(mPortraitMode)
			{
				p.landScrollView.setVisibility(View.GONE);
				if(p.portActive)
					p.portScrollView.setVisibility(View.VISIBLE);
				else
					p.portScrollView.setVisibility(View.GONE);
			}
			else
			{
				p.portScrollView.setVisibility(View.GONE);
				if(p.landActive)
					p.landScrollView.setVisibility(View.VISIBLE);
				else
					p.landScrollView.setVisibility(View.GONE);
			}
		}
	}

	// ____________________________________________________________________________________
	public void hide()
	{
		mShowPanels = false;
		for(Panel p : mPanelCmds)
		{
			p.portScrollView.setVisibility(View.GONE);
			p.landScrollView.setVisibility(View.GONE);
		}
	}

	// ____________________________________________________________________________________
	public void setContext(NetHack context, NH_State nhState)
	{
		mContext = context;
		mState = nhState;
		mPortraitMode = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		mViewArea = findViewById(R.id.viewArea);
		mViewArea.setVisibility(View.GONE);
	}

	// ____________________________________________________________________________________
	private void loadPanels(SharedPreferences prefs)
	{
		Editor editor = prefs.edit();

		boolean bResetPanel = prefs.getBoolean("reset", false);
		if(bResetPanel)
		{
			Log.print("reset panel");
			editor.putBoolean("reset", false);
			editor.remove("cmdLayout");
			resetPanels(editor);
		}

		// get rid of old showCmdPanel
		if(prefs.contains("showCmdPanel"))
		{
			boolean bActive = prefs.getBoolean("showCmdPanel", true);
			Log.print("old showCmdPanel: " + Boolean.toString(bActive));
			editor.putBoolean("pPortActive0", bActive);
			editor.putBoolean("pLandActive0", bActive);
			editor.remove("showCmdPanel");
		}

		//
		boolean forceAddMenu = prefs.getBoolean("forceAddMenu", true);
		if(forceAddMenu)
		{
			editor.putBoolean("forceAddMenu", false);
			forceAddMenu(prefs, editor);
		}

		editor.commit();

		ArrayList<Panel> panelCmds = new ArrayList<Panel>();
		for(int iPanel = 0; iPanel < 6; iPanel++)
		{
			String idx = Integer.toString(iPanel);
			boolean bPortActive = prefs.getBoolean("pPortActive" + idx, false);
			boolean bLandActive = prefs.getBoolean("pLandActive" + idx, false);
			int opacity = prefs.getInt("pOpacity" + idx, 255);
			Log.print("panel" + idx + ": " + Boolean.toString(bPortActive));
			if(bPortActive || bLandActive)
			{
				Panel panel = new Panel();
				panel.cmds = prefs.getString("pCmdString" + idx, "");
				panel.pLoc = Integer.parseInt(prefs.getString("pPortLoc" + idx, "3"));
				panel.lLoc = Integer.parseInt(prefs.getString("pLandLoc" + idx, "3"));
				panel.idx = iPanel;
				panel.portActive = bPortActive;
				panel.landActive = bLandActive;
				panel.opacity = opacity;
				panelCmds.add(panel);
			}
		}

		if(panelsChanged(mPanelCmds, panelCmds))
		{
			Log.print("panels were changed");
			for(Panel p : mPanelCmds)
			{
				((ViewGroup)p.portScrollView.getParent()).removeView(p.portScrollView);
				if(p.portScrollView != p.landScrollView)
					((ViewGroup)p.landScrollView.getParent()).removeView(p.landScrollView);
			}
			
			for(int i = panelCmds.size() - 1; i >= 0; i--)
			{
				Panel panel = panelCmds.get(i);
				panel.portScrollView = createScrollView(panel.pLoc);
				if(panel.pLoc == panel.lLoc)
					panel.landScrollView = panel.portScrollView;
				else
					panel.landScrollView = createScrollView(panel.lLoc);
				
				panel.panel = new CmdPanel(mContext, mState, this, panel.cmds, panel.opacity);
				if(mPortraitMode)
				{
					if(mShowPanels && panel.portActive)
					{
						Log.print("Activating panel" + Integer.toString(panel.idx));
						panel.portScrollView.setVisibility(View.VISIBLE);
					}
					panel.panel.attach(panel.portScrollView, panel.portScrollView instanceof HorizontalScrollView);
				}
				else
				{
					if(mShowPanels && panel.landActive)
						panel.landScrollView.setVisibility(View.VISIBLE);
					panel.panel.attach(panel.landScrollView, panel.landScrollView instanceof HorizontalScrollView);
				}
			}
			mPanelCmds = panelCmds;
		}
		else
			Log.print("panels were NOT changed");
	}

	// ____________________________________________________________________________________
	private boolean panelsChanged(ArrayList<Panel> a, ArrayList<Panel> b)
	{
		if(a.size() != b.size())
			return true;
		for(int i = 0; i < a.size(); i++)
		{
			Panel pa = a.get(i);
			Panel pb = b.get(i);
			if(!pa.cmds.equals(pb.cmds))
				return true;
			if(pa.portActive != pb.portActive)
				return true;
			if(pa.landActive != pb.landActive)
				return true;
			if(pa.pLoc != pb.pLoc)
				return true;
			if(pa.lLoc != pb.lLoc)
				return true;
			if(pa.opacity != pb.opacity)
				return true;
		}
		return false;
	}

	// ____________________________________________________________________________________
	public void savePanelCmds(CmdPanel panel)
	{
		Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
		for(Panel p : mPanelCmds)
		{
			if(p.panel == panel)
			{
				String cmds = panel.getCmds();
				String idx = Integer.toString(p.idx);
				editor.putString("pCmdString" + idx, cmds);
			}
		}
		editor.commit();
	}

	// ____________________________________________________________________________________
	private ViewGroup createScrollView(int location)
	{
		ViewGroup v;
		// left, right, top, bottom
		if(location == 0 || location == 1)
		{
			v = new ScrollView(mContext);
			FrameLayout.LayoutParams params = generateDefaultLayoutParams();
			params.gravity = (location == 0 ? Gravity.LEFT : Gravity.RIGHT) | Gravity.FILL_VERTICAL;
			params.width = LayoutParams.WRAP_CONTENT;
			params.height = LayoutParams.MATCH_PARENT;
			v.setLayoutParams(params);
		}
		else
		{
			v = new HorizontalScrollView(mContext);
			FrameLayout.LayoutParams params = generateDefaultLayoutParams();
			params.gravity = (location == 2 ? Gravity.TOP : Gravity.BOTTOM) | Gravity.FILL_HORIZONTAL;
			params.width = LayoutParams.MATCH_PARENT;
			params.height = LayoutParams.WRAP_CONTENT;
			v.setLayoutParams(params);
		}
		v.setVisibility(View.GONE);
		addView(v);
		return v;
	}

	// ____________________________________________________________________________________
	private void resetPanels(Editor editor)
	{
		String s = "menu ... # 20s . : ; , e d r z Z q t f w x i E Q P R W T o ^d ^p a A ^t D F p ^x ^o ?";
		editor.putBoolean("pPortActive0", true);
		editor.putBoolean("pLandActive0", true);
		editor.putString("pCmdString0", s);
		editor.putString("pPortLoc0", "3");
		editor.putString("pLandLoc0", "3");

		for(int iPanel = 1; iPanel < 6; iPanel++)
		{
			String idx = Integer.toString(iPanel);
			editor.putBoolean("pPortActive" + idx, false);
			editor.putBoolean("pLandActive" + idx, false);
		}
	}

	// ____________________________________________________________________________________
	private void forceAddMenu(SharedPreferences prefs, Editor editor)
	{
		// Add menu command to first non-empty active panel

		// Unless some of them already has it
		for(int iPanel = 0; iPanel < 6; iPanel++)
		{
			if((" " + prefs.getString("pCmdString" + iPanel, "") + " ").contains(" menu "))
				return;
		}

		for(int iPanel = 0; iPanel < 6; iPanel++)
		{
			String idx = Integer.toString(iPanel);
			boolean bPortActive = prefs.getBoolean("pPortActive" + idx, false);
			boolean bLandActive = prefs.getBoolean("pLandActive" + idx, false);
			if(bPortActive || bLandActive)
			{
				String cmds = prefs.getString("pCmdString" + idx, "");
				if(cmds.length() > 0)
				{
					cmds = "menu " + cmds;
					editor.putString("pCmdString" + idx, cmds);
					break;
				}
			}
		}

	}

	// ____________________________________________________________________________________
	public void onCreateContextMenu(ContextMenu menu, View v)
	{
		for(Panel p : mPanelCmds)
			p.panel.onCreateContextMenu(menu, v);
	}

	// ____________________________________________________________________________________
	public void onContextItemSelected(MenuItem item)
	{
		for(Panel p : mPanelCmds)
			p.panel.onContextItemSelected(item);
	}

	// ____________________________________________________________________________________
	public void setOrientation(int orientation)
	{
		boolean bPortrait = orientation == Configuration.ORIENTATION_PORTRAIT;
		if(mPortraitMode != bPortrait)
		{
			mPortraitMode = bPortrait;
			if(mShowPanels)
				show();
			for(Panel p : mPanelCmds)
			{
				if(mPortraitMode)
					p.panel.attach(p.portScrollView, p.portScrollView instanceof HorizontalScrollView);
				else
					p.panel.attach(p.landScrollView, p.landScrollView instanceof HorizontalScrollView);
			}
		}
	}
}
