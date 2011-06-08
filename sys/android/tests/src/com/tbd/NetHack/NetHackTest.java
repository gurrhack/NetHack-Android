package com.tbd.AndroidUI;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.tbd.AndroidUI.AndroidUITest \
 * com.tbd.AndroidUI.tests/android.test.InstrumentationTestRunner
 */
public class AndroidUITest extends ActivityInstrumentationTestCase<AndroidUI> {

    public AndroidUITest() {
        super("com.tbd.AndroidUI", AndroidUI.class);
    }

}
