package sh.ftp.rocketninelabs.meditationassistant;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public abstract class WakeLocker {
    private static PowerManager.WakeLock wakeLock;

    public static void acquire(Context ctx, Boolean turnScreenOn) {
        Log.d("MeditationAssistant", "WAKELOCKER: Acquiring wakelock");
        if (wakeLock != null) {
            Log.d("MeditationAssistant", "WAKELOCKER: Releasing old wakelock first...");
            wakeLock.release();
        }

        PowerManager pm = (PowerManager) ctx
                .getSystemService(Context.POWER_SERVICE);

        if (turnScreenOn) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE, "MeditationAssistant");
        } else {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MeditationAssistant");
        }
        wakeLock.acquire();
    }

    public static void release() {
        if (wakeLock != null) {
            Log.d("MeditationAssistant", "WAKELOCKER: Releasing wakelock");
            wakeLock.release();
        }
        wakeLock = null;
    }
}