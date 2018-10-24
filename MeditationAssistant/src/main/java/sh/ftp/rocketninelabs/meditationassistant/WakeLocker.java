package sh.ftp.rocketninelabs.meditationassistant;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class WakeLocker {
    private List<String> wakeLocks = new CopyOnWriteArrayList<String>();

    private PowerManager.WakeLock wakeLockScreenOff;
    private PowerManager.WakeLock wakeLockScreenOn;

    @SuppressLint({"WakelockTimeout"})
    String acquire(Context ctx, Boolean turnScreenOn) {
        String wakeLockID = String.valueOf(System.currentTimeMillis());

        Log.d("MeditationAssistant", "WAKELOCKER: Acquiring wakelock " + wakeLockID);
        wakeLocks.add(wakeLockID);

        if (wakeLockScreenOff == null || wakeLockScreenOn == null) {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            if (wakeLockScreenOff == null) {
                wakeLockScreenOff = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "meditationassistant:screenoff");
            }
            if (wakeLockScreenOn == null) {
                wakeLockScreenOn = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "meditationassistant:screenon");
            }
        }

        if (!turnScreenOn) {
            if (!wakeLockScreenOff.isHeld()) {
                wakeLockScreenOff.acquire();
            }
        } else {
            if (!wakeLockScreenOn.isHeld()) {
                wakeLockScreenOn.acquire();
            }
        }

        return wakeLockID;
    }

    void release(String wakeLockID) {
        Log.d("MeditationAssistant", "WAKELOCKER: Releasing wakelock " + wakeLockID);
        wakeLocks.remove(wakeLockID);

        if (wakeLocks.isEmpty()) {
            if (wakeLockScreenOff != null && wakeLockScreenOff.isHeld()) {
                wakeLockScreenOff.release();
            }
            if (wakeLockScreenOn != null && wakeLockScreenOn.isHeld()) {
                wakeLockScreenOn.release();
            }
        }
    }

    void releaseAll() {
        Log.d("MeditationAssistant", "WAKELOCKER: Releasing all wakelocks");

        for (String wakeLockID : wakeLocks) {
            release(wakeLockID);
        }
    }
}