package com.tbd.forkfront;

import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/**
 * From Android 15 (targetSdk 35) apps are drawn edge-to-edge, behind the system bars.
 * Pads a root view by the system bar, cutout and keyboard insets so content stays visible.
 */
final class Insets
{
	private Insets() {}

	static void applyTo(View root)
	{
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM)
			return;
		root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener()
		{
			@Override
			public WindowInsets onApplyWindowInsets(View v, WindowInsets insets)
			{
				android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars()
						| WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
				v.setPadding(i.left, i.top, i.right, i.bottom);
				return WindowInsets.CONSUMED;
			}
		});
		root.requestApplyInsets();
	}
}
