package com.adriannieves.backgroundmonitor;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class MonitorService extends Service {

    private Handler handler;
    private Runnable runnable;
    private static final int INTERVALO = 15 * 60 * 1000; // 15 minutos en milisegundos

    @Override
    public void onCreate() {
        super.onCreate();

        // Configurar foreground service con notificación mínima
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "MonitorServiceChannel",
                "Monitor Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(this, "MonitorServiceChannel")
                .setContentTitle("Monitor activo")
                .setContentText("Vigilando dispositivos...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

            startForeground(1, notification);
        }

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                ejecutarTarea();
                handler.postDelayed(this, INTERVALO); // Repetir cada 15 minutos
            }
        };
        handler.post(runnable);
    }

    private void ejecutarTarea() {
        // 👉 Aquí va la lógica para:
        // 1. Leer devices guardados
        // 2. Verificar SSID actual
        // 3. Hacer ping
        // 4. Lanzar alarma si es necesario

        // Por ahora solo para prueba:
        System.out.println("Ejecutando verificación de dispositivos...");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
