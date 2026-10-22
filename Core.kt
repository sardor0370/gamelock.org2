package uz.gamelock

import android.app.NotificationManager
import android.content.Context

object LockState {
    @Volatile var locked = false
    @Volatile var target = ""

    fun unlock(ctx: Context) {
        locked = false
        LockService.instance?.hideEdges()
        GameDnd.restore(ctx)
    }
}

/**
 * "Bezovta qilmang" (Priority) rejimi: qo'ng'iroq va xabarlar jim bo'ladi,
 * bildirishnoma oynachalari chiqmaydi, lekin o'yin tovushi (media) va budilnik ishlayveradi.
 * Bildirishnomalar paneldа saqlanib qoladi, o'yindan keyin ko'rasiz.
 */
object GameDnd {
    private const val PREFS = "dnd"

    fun enable(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) return
        val p = nm.notificationPolicy
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("saved", true)
            .putInt("filter", nm.currentInterruptionFilter)
            .putInt("cat", p.priorityCategories)
            .putInt("calls", p.priorityCallSenders)
            .putInt("msgs", p.priorityMessageSenders)
            .putInt("vis", p.suppressedVisualEffects)
            .apply()

        val vis = NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
            NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
            NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS or
            NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT or
            NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON
        val cats = NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
            NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS
        nm.setNotificationPolicy(
            NotificationManager.Policy(
                cats,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                vis
            )
        )
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun restore(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!sp.getBoolean("saved", false) || !nm.isNotificationPolicyAccessGranted) return
        nm.setNotificationPolicy(
            NotificationManager.Policy(
                sp.getInt("cat", 0),
                sp.getInt("calls", NotificationManager.Policy.PRIORITY_SENDERS_ANY),
                sp.getInt("msgs", NotificationManager.Policy.PRIORITY_SENDERS_ANY),
                sp.getInt("vis", 0)
            )
        )
        nm.setInterruptionFilter(
            sp.getInt("filter", NotificationManager.INTERRUPTION_FILTER_ALL)
        )
        sp.edit().putBoolean("saved", false).apply()
    }
}
