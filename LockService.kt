package uz.gamelock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class LockService : AccessibilityService() {

    companion object {
        @Volatile var instance: LockService? = null
    }

    private val edges = mutableListOf<View>()

    override fun onServiceConnected() {
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        hideEdges()
        instance = null
        return super.onUnbind(intent)
    }

    /** Ekranning yuqori va pastki chetiga ko'rinmas "to'siq" qo'yadi: teginishlarni yutib yuboradi. */
    fun showEdges() {
        hideEdges()
        if (!getSharedPreferences("gl", MODE_PRIVATE).getBoolean("edges", true)) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density

        fun add(gravity: Int, heightDp: Int) {
            val v = View(this)
            v.setOnTouchListener { _, _ -> true }
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                (heightDp * density).toInt(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
            lp.gravity = gravity
            try {
                wm.addView(v, lp)
                edges.add(v)
            } catch (_: Exception) { }
        }
        add(Gravity.BOTTOM, 48)
        add(Gravity.TOP, 28)
    }

    fun hideEdges() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        for (v in edges) {
            try { wm.removeView(v) } catch (_: Exception) { }
        }
        edges.clear()
    }

    private val handler = Handler(Looper.getMainLooper())
    private var upDown = false
    private var downDown = false
    private var lastLaunch = 0L

    // Shu ilovalar oynasi ochilsa, o'yinga qaytarmaymiz
    private val keep = setOf(
        "com.android.systemui",
        "android",
        "com.google.android.gms",
        "com.samsung.android.honeyboard",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.game.gametools"
    )

    private val unlockRunnable = Runnable {
        if (LockState.locked && upDown && downDown) unlockNow()
    }

    private fun unlockNow() {
        LockState.unlock(this)
        try {
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) { }
        Toast.makeText(this, "Qulf ochildi", Toast.LENGTH_LONG).show()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!LockState.locked) {
            upDown = false
            downDown = false
            return false
        }
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) return false

        // tugma ushlab turilganda takrorlanuvchi hodisalarni e'tiborsiz qoldiramiz
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount > 0) return false

        val down = event.action == KeyEvent.ACTION_DOWN
        if (code == KeyEvent.KEYCODE_VOLUME_UP) upDown = down else downDown = down

        handler.removeCallbacks(unlockRunnable)
        if (upDown && downDown) handler.postDelayed(unlockRunnable, 3000)
        return false // ovoz balandligi odatdagidek o'zgaradi
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!LockState.locked) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == LockState.target || pkg == packageName || pkg in keep) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunch < 500) return
        lastLaunch = now

        val intent = packageManager.getLaunchIntentForPackage(LockState.target)
        if (intent == null) {
            unlockNow() // o'yin topilmasa, qulfda qolib ketmaslik uchun
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
