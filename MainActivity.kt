package uz.gamelock

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val pubgPackages = listOf(
        "com.tencent.ig",
        "com.pubg.krmobile",
        "com.vng.pubgmobile",
        "com.rekoo.pubgm",
        "com.pubg.imobile"
    )

    private lateinit var accBtn: Button
    private lateinit var dndBtn: Button
    private lateinit var edgesBtn: Button
    private lateinit var gameText: TextView
    private lateinit var lockBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        root.addView(text("GAME LOCK", 28f, "#F5B301", true).apply { gravity = Gravity.CENTER })
        root.addView(text("O'yin paytida tasodifan chiqib ketishdan himoya", 13f, "#9AA4B2", false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(28))
        })

        accBtn = button { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        root.addView(accBtn)
        root.addView(text(
            "Tugma ishlamasa: Sozlamalar → Ilovalar → Game Lock → ⋮ → \"Cheklangan sozlamalarga ruxsat berish\", keyin qayta urining.",
            12f, "#9AA4B2", false
        ).apply { setPadding(0, 0, 0, dp(16)) })

        dndBtn = button { startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
        root.addView(dndBtn)

        edgesBtn = button {
            val sp = getSharedPreferences("gl", MODE_PRIVATE)
            sp.edit().putBoolean("edges", !sp.getBoolean("edges", true)).apply()
            refresh()
        }
        root.addView(edgesBtn)

        gameText = text("", 14f, "#FFFFFF", false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(20))
        }
        root.addView(gameText)

        lockBtn = button { onLockClick() }
        root.addView(lockBtn)

        root.addView(text(
            "Chiqish: ovoz + va − tugmalarini birga 3 soniya bosib turing.\n\n" +
                "Chetni qulflash: ekranning eng yuqori va eng pastki qismi teginishni qabul qilmaydi (Home/Recents/xabarlar paneli tasodifan bosilmasligi uchun). " +
                "Agar pastdan surib Home'ga chiqib ketsa, Sozlamalar → Tizim navigatsiyasi'dan 3 tugmali navigatsiyani tanlang.\n\n" +
                "Qo'ng'iroq va xabarlar o'yin paytida jim bo'ladi, o'yindan keyin bildirishnomalar panelida va qo'ng'iroqlar tarixida ko'rasiz.",
            13f, "#9AA4B2", false
        ).apply { gravity = Gravity.CENTER; setPadding(0, dp(20), 0, 0) })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0E1116"))
            isFillViewport = true
            addView(root)
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun accessibilityOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains("$packageName/") && s.contains("LockService")
    }

    private fun dndOn(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun findPubg(): String? =
        pubgPackages.firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }

    private fun refresh() {
        val acc = accessibilityOn()
        val dnd = dndOn()
        val pkg = findPubg()

        accBtn.text = if (acc) "1. Maxsus imkoniyat: YOQILGAN ✓" else "1. Maxsus imkoniyatni yoqish"
        paint(accBtn, if (acc) "#2E7D32" else "#1F2630", Color.WHITE)
        dndBtn.text = if (dnd) "2. Bezovta qilmang ruxsati: BERILGAN ✓" else "2. Bezovta qilmang ruxsatini berish"
        paint(dndBtn, if (dnd) "#2E7D32" else "#1F2630", Color.WHITE)
        val edges = getSharedPreferences("gl", MODE_PRIVATE).getBoolean("edges", true)
        edgesBtn.text = if (edges) "3. Yuqori va pastki chetni qulflash: YOQILGAN ✓" else "3. Yuqori va pastki chetni qulflash: o'chiq"
        paint(edgesBtn, if (edges) "#2E7D32" else "#1F2630", Color.WHITE)
        gameText.text = if (pkg != null) "PUBG topildi ($pkg)" else "PUBG topilmadi"

        if (LockState.locked) {
            lockBtn.text = "QULFNI OCHISH"
            paint(lockBtn, "#C62828", Color.WHITE)
            lockBtn.isEnabled = true
            lockBtn.alpha = 1f
        } else {
            val ready = acc && dnd && pkg != null
            lockBtn.text = "QULFLASH VA O'YINNI OCHISH"
            paint(lockBtn, "#F5B301", Color.BLACK)
            lockBtn.isEnabled = ready
            lockBtn.alpha = if (ready) 1f else 0.4f
        }
    }

    private fun onLockClick() {
        if (LockState.locked) {
            LockState.unlock(this)
            refresh()
            return
        }
        val pkg = findPubg() ?: return
        GameDnd.enable(this)
        LockState.target = pkg
        LockState.locked = true
        LockService.instance?.showEdges()
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    private fun text(t: String, size: Float, color: String, bold: Boolean) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun paint(b: Button, bg: String, fg: Int) {
        b.setTextColor(fg)
        b.background = GradientDrawable().apply {
            setColor(Color.parseColor(bg)); cornerRadius = dp(14).toFloat()
        }
    }

    private fun button(onClick: () -> Unit): Button = Button(this).apply {
        setTypeface(null, Typeface.BOLD)
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
        ).apply { bottomMargin = dp(10) }
        setOnClickListener { onClick() }
        paint(this, "#1F2630", Color.WHITE)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
