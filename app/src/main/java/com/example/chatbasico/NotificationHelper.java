package com.example.chatbasico;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_ID = "chat_message_channel";
    private static final String CHANNEL_NAME = "Mensajes de Chat";
    private static final String CHANNEL_DESCRIPTION = "Notificaciones de mensajes nuevos en el chat";

    /**
     * Crea el canal de notificaciones para Android 8.0+
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "=== CREANDO CANAL DE NOTIFICACIONES ===");

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // ALTA importancia
            );

            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.BLUE);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000}); // Vibración más larga
            channel.setShowBadge(true);

            // Configurar sonido personalizado
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Canal de notificaciones creado con IMPORTANCE_HIGH");
            }
        }
    }

    /**
     * Muestra una notificación local mejorada
     */
    public static void showLocalNotification(Context context, String title, String message, String notificationId) {
        Log.d(TAG, "=== MOSTRANDO NOTIFICACIÓN LOCAL ===");
        Log.d(TAG, "Título: " + title);
        Log.d(TAG, "Mensaje: " + message);
        Log.d(TAG, "ID: " + notificationId);

        try {
            // Crear intent para abrir la app al tocar la notificación
            Intent intent = new Intent(context, MainChats.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Crear la notificación con configuración mejorada
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de tener este ícono
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Texto expandible
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad ALTA
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE) // Categoría de mensaje
                    .setAutoCancel(true) // Se elimina al tocarla
                    .setContentIntent(pendingIntent)
                    .setDefaults(NotificationCompat.DEFAULT_ALL) // Sonido, vibración y luces por defecto
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible en pantalla de bloqueo
                    .setShowWhen(true) // Mostrar timestamp
                    .setWhen(System.currentTimeMillis());

            // Configurar sonido personalizado si es necesario
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(soundUri);

            // Configurar vibración personalizada
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Para Android 8.0+, la vibración se maneja en el canal
                Log.d(TAG, "Vibración configurada en el canal de notificación");
            } else {
                // Para versiones anteriores, configurar vibración manualmente
                builder.setVibrate(new long[]{0, 1000, 500, 1000});
            }

            // Mostrar la notificación
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // Generar ID único para cada notificación
            int uniqueId = (int) System.currentTimeMillis();
            notificationManager.notify(uniqueId, builder.build());

            Log.d(TAG, "✅ Notificación mostrada con ID: " + uniqueId);

            // Vibrar manualmente para asegurar que funcione
            vibrate(context);

        } catch (SecurityException e) {
            Log.e(TAG, "❌ Error de permisos al mostrar notificación", e);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error general al mostrar notificación", e);
        }
    }

    /**
     * Activa la vibración manualmente
     */
    private static void vibrate(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Para Android 8.0+
                    VibrationEffect effect = VibrationEffect.createWaveform(
                            new long[]{0, 500, 250, 500}, // Patrón
                            -1 // No repetir
                    );
                    vibrator.vibrate(effect);
                } else {
                    // Para versiones anteriores
                    vibrator.vibrate(new long[]{0, 500, 250, 500}, -1);
                }
                Log.d(TAG, "✅ Vibración activada");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error activando vibración", e);
        }
    }
}
