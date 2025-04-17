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
import java.net.InetAddress;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;



public class MonitorService extends Service {

    private Handler handler;
    private Runnable runnable;
    // private static final int INTERVALO = 15 * 60 * 1000; // 15 minutos en milisegundos
    private static final int INTERVALO = 60 * 1000; // 15 minutos en milisegundos

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
    try {
        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("CapacitorStorage", Context.MODE_PRIVATE);
        String dispositivosJson = prefs.getString("dispositivos_guardados", null);

        if (dispositivosJson == null) {
            System.out.println("No hay dispositivos guardados.");
            return;
        }

        JSONObject data = new JSONObject(dispositivosJson);
        String wifiGuardada = data.getString("wifi_ssid");
        JSONArray dispositivos = data.getJSONArray("devices");

        // Obtener el SSID actual
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentSSID = wifiInfo.getSSID();

        if (currentSSID.startsWith("\"") && currentSSID.endsWith("\"")) {
            currentSSID = currentSSID.substring(1, currentSSID.length() - 1);
        }

        System.out.println("SSID actual: " + currentSSID);

        // Comparamos el SSID
        if (!wifiGuardada.equals(currentSSID)) {
            System.out.println("Red WiFi diferente, no se ejecutan pings.");
            return;
        }

        // Ahora hacemos ping a cada dispositivo
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

private void lanzarAlarma(String ip) {
    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    String channelId = "alarma_dispositivo";

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel channel = new NotificationChannel(
            channelId,
            "Alarmas de dispositivos caídos",
            NotificationManager.IMPORTANCE_HIGH // 🔥 Importancia alta
        );
        channel.setDescription("Notificaciones urgentes de dispositivos desconectados");

        // Configurar vibración y sonido del canal
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 1000, 500}); // patrón vibración
        channel.setSound(
            android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, // 🔔 Sonido de alarma predeterminado
            null
        );

        notificationManager.createNotificationChannel(channel);
    }

    Notification.Builder builder = new Notification.Builder(this)
        .setContentTitle("¡Dispositivo desconectado!")
        .setContentText("No se puede alcanzar el dispositivo IP: " + ip)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setAutoCancel(true);

    // Asignar el canal de notificación en Android O y superior
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        builder.setChannelId(channelId);
    }

    Notification notification = builder.build();
    notificationManager.notify((int) System.currentTimeMillis(), notification);

    // Vibración manual adicional (por si quieres más fuerte)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(android.os.VibratorManager.class)
            .getDefaultVibrator()
            .vibrate(android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
    } else {
        android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(1000); // vibrar 1 segundo
        }
    }

    System.out.println("ALERTA: El dispositivo " + ip + " está inalcanzable!");
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
