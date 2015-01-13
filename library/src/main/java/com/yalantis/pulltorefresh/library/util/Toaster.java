package com.yalantis.pulltorefresh.library.util;

import android.content.Context;
import android.widget.Toast;

public class Toaster {

    public static void showShort(Context context, String value) {
        if (context != null)
            Toast.makeText(context, value, Toast.LENGTH_SHORT).show();
    }

    public static void showLong(Context context, String value) {
        if (context != null)
            Toast.makeText(context, value, Toast.LENGTH_LONG).show();
    }

}
