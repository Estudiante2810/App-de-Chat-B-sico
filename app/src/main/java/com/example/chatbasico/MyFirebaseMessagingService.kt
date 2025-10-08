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
    private val CHANNEL_ID = "chat_message_channel"

    companion object {
        private val displayedNotifications = mutableSetOf<String>()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "🚀 MyFirebaseMessagingService iniciado")

        // Debug: Verificar si el servicio se está ejecutando
        Log.d(TAG, "🔍 DEBUG: Servicio FCM creado exitosamente")
        Log.d(TAG, "🔍 DEBUG: Canal de notificación ID: $CHANNEL_ID")
    }

    /**
     * Se ejecuta cuando llega una notificación push desde Firebase Functions
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "📨 Notificación recibida desde: ${remoteMessage.from}")
        Log.d(TAG, "🔍 DEBUG: ===== NOTIFICACIÓN FCM RECIBIDA =====")

        // Debug completo de la notificación
        Log.d(TAG, "🔍 DEBUG: Notification data: ${remoteMessage.notification?.title} - ${remoteMessage.notification?.body}")
        Log.d(TAG, "🔍 DEBUG: Data payload: ${remoteMessage.data}")
        Log.d(TAG, "🔍 DEBUG: Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "🔍 DEBUG: TTL: ${remoteMessage.ttl}")

        // Datos de la notificación
        val title = remoteMessage.notification?.title ?: "Nuevo mensaje"
        val body = remoteMessage.notification?.body ?: ""

        // Datos adicionales
        val senderId = remoteMessage.data["senderId"]
        val senderName = remoteMessage.data["senderName"]
        val conversationId = remoteMessage.data["conversationId"]
        val messageType = remoteMessage.data["messageType"]

        Log.d(TAG, "📋 Datos de notificación:")
        Log.d(TAG, "- Título: $title")
        Log.d(TAG, "- Cuerpo: $body")
        Log.d(TAG, "- Remitente: $senderName ($senderId)")
        Log.d(TAG, "- Conversación: $conversationId")

        // Crear ID único para evitar duplicados
        val notificationId = "${senderId}_${conversationId}_${body.hashCode()}"

        Log.d(TAG, "🔍 DEBUG: ID de notificación generado: $notificationId")

        // Verificar si ya se mostró esta notificación
        if (displayedNotifications.contains(notificationId)) {
            Log.d(TAG, "⚠️ Notificación ya mostrada, ignorando: $notificationId")
            return
        }

        // Agregar a la lista de mostradas
        displayedNotifications.add(notificationId)
        Log.d(TAG, "🔍 DEBUG: Notificación agregada a lista de mostradas")

        // Mostrar la notificación
        showNotification(title, body, conversationId, senderId)

        // Limpiar lista si crece mucho (mantener solo las últimas 50)
        if (displayedNotifications.size > 50) {
            val iterator = displayedNotifications.iterator()
            repeat(10) { // Eliminar las 10 más antiguas
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }

        Log.d(TAG, "🔍 DEBUG: ===== FIN PROCESAMIENTO NOTIFICACIÓN =====")
    }

    /**
     * Se ejecuta cuando se genera un nuevo token FCM
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 Nuevo token FCM generado: ${token.take(20)}...")
        updateTokenInFirestore(token)
    }

    /**
     * Actualiza el token FCM en Firestore
     */
    private fun updateTokenInFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "⚠️ Usuario no autenticado, no se puede actualizar token")
            return
        }

        Log.d(TAG, "📝 Actualizando token para usuario: $userId")

        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        // Primero obtener los tokens existentes
        userRef.get()
            .addOnSuccessListener { document ->
                val existingTokens = document.get("fcmTokens") as? MutableList<String> ?: mutableListOf()
                val singleToken = document.getString("fcmToken")

                // Agregar token individual si existe y no está en la lista
                if (singleToken != null && !existingTokens.contains(singleToken)) {
                    existingTokens.add(singleToken)
                }

                // Agregar nuevo token si no existe
                if (!existingTokens.contains(token)) {
                    existingTokens.add(token)
                    Log.d(TAG, "➕ Nuevo token agregado a la lista")
                } else {
                    Log.d(TAG, "ℹ️ Token ya existe en la lista")
                }

                // Actualizar en Firestore
                val updates = hashMapOf<String, Any>(
                    "fcmToken" to token, // Token individual (compatibilidad)
                    "fcmTokens" to existingTokens, // Lista de tokens
                    "lastTokenUpdate" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                userRef.update(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Token actualizado en Firestore")
                        Log.d(TAG, "📱 Dispositivos registrados: ${existingTokens.size}")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error actualizando token", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error obteniendo datos del usuario", e)
            }
    }

    /**
     * Muestra la notificación en el dispositivo
     */
    private fun showNotification(title: String, body: String, conversationId: String?, senderId: String?) {
        Log.d(TAG, "📢 Mostrando notificación: $title")
        Log.d(TAG, "🔍 DEBUG: ===== CREANDO NOTIFICACIÓN LOCAL =====")
        Log.d(TAG, "🔍 DEBUG: Título: $title")
        Log.d(TAG, "🔍 DEBUG: Cuerpo: $body")
        Log.d(TAG, "🔍 DEBUG: Canal: $CHANNEL_ID")

        // Verificar NotificationManager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "🔍 DEBUG: NotificationManager obtenido: ${notificationManager != null}")

        // Verificar canal de notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            Log.d(TAG, "🔍 DEBUG: Canal existe: ${channel != null}")
            Log.d(TAG, "🔍 DEBUG: Importancia del canal: ${channel?.importance}")
        }

        // Intent para abrir la app al tocar la notificación
        val intent = Intent(this, MainChats::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // Agregar datos extras si están disponibles
            conversationId?.let { putExtra("conversationId", it) }
            senderId?.let { putExtra("senderId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // ID único
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d(TAG, "🔍 DEBUG: PendingIntent creado: ${pendingIntent != null}")

        // Configurar sonido
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        Log.d(TAG, "🔍 DEBUG: Sonido configurado: ${defaultSoundUri != null}")

        // Crear la notificación
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // Texto expandible
            .setAutoCancel(true) // Se elimina al tocarla
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta
            .setCategory(NotificationCompat.CATEGORY_MESSAGE) // Categoría mensaje
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible en pantalla de bloqueo
            .setShowWhen(true) // Mostrar timestamp
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)

        Log.d(TAG, "🔍 DEBUG: Notificación construida exitosamente")

        // Mostrar la notificación
        val uniqueNotificationId = System.currentTimeMillis().toInt()
        Log.d(TAG, "🔍 DEBUG: ID único generado: $uniqueNotificationId")

        try {
            notificationManager.notify(uniqueNotificationId, notificationBuilder.build())
            Log.d(TAG, "✅ Notificación mostrada con ID: $uniqueNotificationId")
            Log.d(TAG, "🔍 DEBUG: ===== NOTIFICACIÓN ENVIADA EXITOSAMENTE =====")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Error de permisos al mostrar notificación", e)
            Log.e(TAG, "🔍 DEBUG: Error de seguridad: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error general al mostrar notificación", e)
            Log.e(TAG, "🔍 DEBUG: Error general: ${e.message}")
        }
    }

    /**
     * Crea el canal de notificaciones para Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "📺 Creando canal de notificaciones")

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mensajes de Chat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de mensajes nuevos en el chat"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setShowBadge(true)

                // Configurar sonido
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(soundUri, android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build())
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "✅ Canal de notificaciones creado")
        }
    }
}
