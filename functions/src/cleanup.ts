import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();

/**
 * Función manual para limpiar tokens duplicados
 * Ejecutar una sola vez para limpiar la base de datos
 */
export const cleanupDuplicateTokens = functions.https.onRequest(async (req, res) => {
    console.log('🧹 Iniciando limpieza de tokens duplicados...');

    try {
        const usersSnapshot = await admin.firestore().collection('users').get();
        let totalCleaned = 0;
        let usersProcessed = 0;

        for (const userDoc of usersSnapshot.docs) {
            const userData = userDoc.data();
            const fcmTokens: string[] = userData.fcmTokens || [];
            const originalCount = fcmTokens.length;

            if (fcmTokens.length === 0) continue;

            // Eliminar duplicados usando Set
            const uniqueTokens = [...new Set(fcmTokens)];
            const duplicatesRemoved = originalCount - uniqueTokens.length;

            // Solo actualizar si hay duplicados
            if (duplicatesRemoved > 0) {
                await userDoc.ref.update({ fcmTokens: uniqueTokens });
                console.log(`🔧 Usuario ${userDoc.id}: ${originalCount} → ${uniqueTokens.length} tokens (${duplicatesRemoved} duplicados eliminados)`);
                totalCleaned += duplicatesRemoved;
            }

            usersProcessed++;
        }

        const result = {
            message: 'Limpieza completada',
            usersProcessed,
            duplicatesRemoved: totalCleaned
        };

        console.log('✅ Limpieza completada:', result);
        res.json(result);

    } catch (error) {
        console.error('❌ Error en limpieza:', error);
        // Cast error to any to access .message
        res.status(500).json({ error: (error as any).message });
    }
});


