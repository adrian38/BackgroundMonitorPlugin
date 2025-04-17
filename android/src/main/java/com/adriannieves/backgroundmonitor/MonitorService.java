package com.adriannieves.backgroundmonitor;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.os.AsyncTask;
import android.content.Context;
import android.media.AudioAttributes;
import android.util.Log;

import java.net.InetAddress;

import org.json.JSONArray;
import org.json.JSONObject;

public class MonitorService extends Service {

    private Handler handler;
    private Runnable runnable;
    private static final int INTERVALO = 60 * 1000; // 1 minuto

    private final String CHANNEL_ID_MONITOR = "MonitorServiceChannel";
    private final String CHANNEL_ID_ALARMA = "alarma_dispositivo";

    @Override
    public void onCreate() {
        super.onCreate();

        crearCanalesNotificacion();
        lanzarNotificacionMonitor(); // 🚨 Lanzar notificación de foreground inmediata

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                ejecutarTarea();
                handler.postDelayed(this, INTERVALO);
            }
        };
        handler.post(runnable);
    }

    private void crearCanalesNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Canal para el monitoreo general
            NotificationChannel monitorChannel = new NotificationChannel(
                CHANNEL_ID_MONITOR,
                "Monitor Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            monitorChannel.setDescription("Notificación de monitoreo activo");
            manager.createNotificationChannel(monitorChannel);

            // Canal para alarmas de dispositivos
            NotificationChannel alarmaChannel = new NotificationChannel(
                CHANNEL_ID_ALARMA,
                "Alarmas de dispositivos caídos",
                NotificationManager.IMPORTANCE_HIGH
            );
            alarmaChannel.setDescription("Alertas críticas de dispositivos desconectados");
            alarmaChannel.enableLights(true);
            alarmaChannel.enableVibration(true);
            alarmaChannel.setVibrationPattern(new long[]{0, 1000, 1000, 1000});
            alarmaChannel.setSound(
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI,
                new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );
            manager.createNotificationChannel(alarmaChannel);
        }
    }

    private void lanzarNotificacionMonitor() {
        Notification.Builder builder = new Notification.Builder(this)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Monitor activo")
            .setContentText("Vigilando dispositivos...")
            .setOngoing(true) // No deslizable
            .setAutoCancel(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID_MONITOR);
        }

        Notification notification = builder.build();
        startForeground(1, notification); // 🚀 Servicio foreground desde el inicio
    }

    private void lanzarAlarma(String ip) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(this)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Dispositivo desconectado")
            .setContentText("No se puede alcanzar el dispositivo IP: " + ip)
            .setPriority(Notification.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_ALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID_ALARMA);
        }

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_INSISTENT; // 🔥 Vibración y sonido en loop

        // ⚡️ ACTUALIZAMOS la notificación ID=1 (no se cambia foreground)
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notification);
    }

    private void ejecutarTarea() {
        try {
            SharedPreferences prefs = getApplicationContext().getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
            String dispositivosJson = prefs.getString("dispositivos_guardados", null);

            if (dispositivosJson == null) {
                Log.i("MonitorService", "No hay dispositivos guardados.");
                return;
            }

            JSONObject data = new JSONObject(dispositivosJson);
            String wifiGuardada = data.getString("wifi_ssid");
            JSONArray dispositivos = data.getJSONArray("devices");

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String currentSSID = wifiInfo.getSSID();

            if (currentSSID.startsWith("\"") && currentSSID.endsWith("\"")) {
                currentSSID = currentSSID.substring(1, currentSSID.length() - 1);
            }

            Log.i("MonitorService", "SSID actual: " + currentSSID);

            if (!wifiGuardada.equals(currentSSID)) {
                Log.i("MonitorService", "Red WiFi diferente, no se ejecutan pings.");
                return;
            }

            for (int i = 0; i < dispositivos.length(); i++) {
                JSONObject device = dispositivos.getJSONObject(i);
                String ip = device.getString("ip");

                new AsyncTask<String, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(String... ips) {
                        try {
                            InetAddress inet = InetAddress.getByName(ips[0]);
                            return inet.isReachable(1000); // 1 segundo timeout
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }

                    @Override
                    protected void onPostExecute(Boolean reachable) {
                        if (!reachable) {
                            lanzarAlarma(ip);
                        }
                    }
                }.execute(ip);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
