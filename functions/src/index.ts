import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();

/**
 * Cloud Function que se ejecuta cuando se crea un nuevo mensaje
 * Envía notificaciones push automáticamente a todos los dispositivos del destinatario
 */
export const sendMessageNotification = functions.firestore
    .document('conversations/{conversationId}/messages/{messageId}')
    .onCreate(async (snapshot, context) => {
        try {
            const messageData = snapshot.data();
            const { conversationId } = context.params;

            console.log('📨 Nuevo mensaje detectado:', {
                conversationId,
                senderId: messageData.senderId,
                receiverId: messageData.receiverId,
                senderName: messageData.senderName,
                text: messageData.text
            });

            // Obtener información del destinatario
            const receiverDoc = await admin.firestore()
                .collection('users')
                .doc(messageData.receiverId)
                .get();

            if (!receiverDoc.exists) {
                console.error('❌ Usuario destinatario no encontrado:', messageData.receiverId);
                return;
            }

            const receiverData = receiverDoc.data();
            let fcmTokens: string[] = [];

            // Obtener todos los tokens FCM del destinatario
            if (receiverData?.fcmTokens && Array.isArray(receiverData.fcmTokens)) {
                fcmTokens = receiverData.fcmTokens;
            }

            // También incluir el token individual (compatibilidad)
            if (receiverData?.fcmToken && !fcmTokens.includes(receiverData.fcmToken)) {
                fcmTokens.push(receiverData.fcmToken);
            }

            if (fcmTokens.length === 0) {
                console.warn('⚠️ No se encontraron tokens FCM para el usuario:', messageData.receiverId);
                return;
            }

            console.log(`📤 Enviando notificación a ${fcmTokens.length} dispositivos`);

            // Debug: Mostrar los tokens que se van a usar
            console.log('🔍 DEBUG: Tokens FCM encontrados:', fcmTokens.map(token => token.substring(0, 20) + '...'));

            // Crear el payload de la notificación con la estructura correcta de FCM
            const notificationPayload = {
                notification: {
                    title: `${messageData.senderName} te envió un mensaje`,
                    body: messageData.text || 'Te envió una imagen'
                    // Removidos: icon, sound, click_action (no son válidos en FCM v1)
                },
                data: {
                    senderId: messageData.senderId,
                    senderName: messageData.senderName,
                    conversationId: conversationId,
                    messageType: messageData.messageType || 'text',
                    timestamp: Date.now().toString()
                },
                // Configurar para Android
                android: {
                    notification: {
                        icon: 'ic_notification',
                        sound: 'default',
                        channel_id: 'chat_message_channel',
                        priority: 'high' as const // Fix: Use 'as const' to make it a literal type
                    }
                }
            };

            console.log('🔍 DEBUG: Payload de notificación:', JSON.stringify(notificationPayload, null, 2));

            // Enviar a todos los tokens
            const sendPromises = fcmTokens.map(async (token) => {
                try {
                    const response = await admin.messaging().send({
                        token: token,
                        ...notificationPayload
                    });
                    console.log('✅ Notificación enviada al token:', token.substring(0, 20) + '...');
                    return response;
                } catch (error: any) {
                    console.error('❌ Error enviando a token:', token.substring(0, 20) + '...', error.message);

                    // Si el token es inválido, eliminarlo de la base de datos
                    if (error.code === 'messaging/registration-token-not-registered' ||
                        error.code === 'messaging/invalid-registration-token') {
                        await removeInvalidToken(messageData.receiverId, token);
                    }
                    return null;
                }
            });

            const results = await Promise.allSettled(sendPromises);
            const successful = results.filter(result => result.status === 'fulfilled').length;

            console.log(`📊 Notificaciones enviadas: ${successful}/${fcmTokens.length}`);

        } catch (error) {
            console.error('❌ Error en sendMessageNotification:', error);
        }
    });

/**
 * Función para eliminar tokens inválidos de la base de datos
 */
async function removeInvalidToken(userId: string, invalidToken: string) {
    try {
        const userRef = admin.firestore().collection('users').doc(userId);
        const userDoc = await userRef.get();

        if (userDoc.exists) {
            const userData = userDoc.data();
            let fcmTokens: string[] = userData?.fcmTokens || [];

            // Eliminar el token inválido
            fcmTokens = fcmTokens.filter(token => token !== invalidToken);

            await userRef.update({
                fcmTokens: fcmTokens,
                // También limpiar el token individual si coincide
                ...(userData?.fcmToken === invalidToken && { fcmToken: admin.firestore.FieldValue.delete() })
            });

            console.log('🧹 Token inválido eliminado:', invalidToken.substring(0, 20) + '...');
        }
    } catch (error) {
        console.error('❌ Error eliminando token inválido:', error);
    }
}

/**
 * Cloud Function para limpiar tokens FCM inválidos periódicamente
 */
export const cleanupInvalidTokens = functions.pubsub
    .schedule('every 24 hours')
    .onRun(async (context) => {
        console.log('🧹 Iniciando limpieza de tokens inválidos...');

        try {
            const usersSnapshot = await admin.firestore().collection('users').get();
            let totalCleaned = 0;

            for (const userDoc of usersSnapshot.docs) {
                const userData = userDoc.data();
                const fcmTokens: string[] = userData.fcmTokens || [];

                if (fcmTokens.length === 0) continue;

                // Verificar cada token
                const validTokens: string[] = [];

                for (const token of fcmTokens) {
                    try {
                        await admin.messaging().send({
                            token: token,
                            data: { test: 'connectivity' }
                        }, true); // dry run

                        validTokens.push(token);
                    } catch (error: any) {
                        if (error.code === 'messaging/registration-token-not-registered' ||
                            error.code === 'messaging/invalid-registration-token') {
                            console.log('🗑️ Token inválido encontrado:', token.substring(0, 20) + '...');
                            totalCleaned++;
                        } else {
                            // Si es otro tipo de error, mantener el token
                            validTokens.push(token);
                        }
                    }
                }

                // Actualizar solo si hay cambios
                if (validTokens.length !== fcmTokens.length) {
                    await userDoc.ref.update({ fcmTokens: validTokens });
                }
            }

            console.log(`✅ Limpieza completada. Tokens eliminados: ${totalCleaned}`);
        } catch (error) {
            console.error('❌ Error en limpieza de tokens:', error);
        }
    });
