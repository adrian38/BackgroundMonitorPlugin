package com.adriannieves.backgroundmonitor;

import android.util.Log;

public class BackgroundMonitorPlugin {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
