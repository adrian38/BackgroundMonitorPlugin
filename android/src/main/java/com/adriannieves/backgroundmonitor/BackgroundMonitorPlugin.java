package com.adriannieves.backgroundmonitor;

import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "BackgroundMonitorPlugin")
public class BackgroundMonitorPlugin extends Plugin {

    @PluginMethod
    public void startService(PluginCall call) {
        Intent serviceIntent = new Intent(getContext(), MonitorService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(serviceIntent);
        } else {
            getContext().startService(serviceIntent);
        }

        call.resolve();
    }

    @PluginMethod
    public void stopService(PluginCall call) {
        Intent serviceIntent = new Intent(getContext(), MonitorService.class);
        getContext().stopService(serviceIntent);
        call.resolve();
    }
}
