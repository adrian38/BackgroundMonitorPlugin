package com.adriannieves.backgroundmonitor;

import android.content.Context;
import android.content.Intent;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat; // 👈 Import necesario para ContextCompat

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

    @PluginMethod
    public void stopAlarm(PluginCall call) {
        detenerAlarma();
        call.resolve();
    }

    @PluginMethod
    public void restartService(PluginCall call) {
        Intent stopIntent = new Intent(getContext(), MonitorService.class);
        getContext().stopService(stopIntent);

        Intent startIntent = new Intent(getContext(), MonitorService.class);
        ContextCompat.startForegroundService(getContext(), startIntent);
        call.resolve();
    }

    // 🚀 Aquí agregamos el método detenerAlarma()
    private void detenerAlarma() {
        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(1); // 👈 ID que usamos para lanzar la notificación de alarma
            Log.i("BackgroundMonitorPlugin", "Alarma detenida manualmente.");
        }
    }
}
