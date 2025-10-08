package com.example.chatbasico

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCMService"

    override fun onCreate() {
        super.onCreate()
        // Escuchar notificaciones pendientes en Firestore
        listenForNotifications()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            sendNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM: $token")
        updateTokenInFirestore(token)
    }

    private fun updateTokenInFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "Token actualizado en Firestore")
                }
        }
    }

    /**
     * Escucha cambios en la colección de notificaciones para el usuario actual
     */
    private fun listenForNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "Usuario no autenticado, no se pueden escuchar notificaciones")
            return
        }

        Log.d(TAG, "Iniciando escucha de notificaciones para usuario: $userId")

        FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("recipientId", userId)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando notificaciones", error)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val data = change.document.data
                        val senderName = data["senderName"] as? String ?: "Alguien"
                        val messageText = data["messageText"] as? String ?: "Te envió un mensaje"

                        Log.d(TAG, " Nueva notificación de: $senderName")

                        // Mostrar notificación
                        sendNotification("$senderName te envió un mensaje:", messageText)

                        // Marcar como leída
                        change.document.reference.update("read", true)
                            .addOnSuccessListener {
                                Log.d(TAG, "Notificación marcada como leída")
                            }
                    }
                }
            }
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, Class.forName("com.example.chatbasico.MainChats"))
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "chat_message_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title ?: "Nuevo mensaje")
            .setContentText(messageBody ?: "")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal de notificación (para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificaciones de chat",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Canal de mensajes del chat"
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        Log.d(TAG, "✅ Notificación mostrada")
            .

    }
}
