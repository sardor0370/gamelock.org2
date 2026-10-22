package uz.gamelock

import android.app.Activity
import android.app.AlertDialog
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

    private val BG = Color.parseColor("#0B0E13")
    private val CARD = Color.parseColor("#151B24")
    private val LINE = Color.parseColor("#232C38")
    private val GOLD = Color.parseColor("#F5B301")
    private val GOLD_BG = Color.parseColor("#2A2310")
    private val GREEN = Color.parseColor("#22C55E")
    private val GREEN_BG = Color.parseColor("#102A1B")
    private val RED = Color.parseColor("#EF4444")
    private val RED_BG = Color.parseColor("#2A1213")
    private val MUTED = Color.parseColor("#8A97A8")

    private class Row(val box: LinearLayout, val sub: TextView, val status: TextView)

    private lateinit var banner: TextView
    private lateinit var accRow: Row
    private lateinit var dndRow: Row
    private lateinit var edgeRow: Row
    private lateinit var gameName: TextView
    private lateinit var lockBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(24))
        }

        root.addView(label("Game Lock", 26f, Color.WHITE, true))
        root.addView(label("O'yin paytida tasodifan chiqib ketishdan himoya", 13f, MUTED, false).apply {
            setPadding(0, dp(2), 0, dp(20))
        })

        banner = label("", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(20) }
        }
        root.addView(banner)

        root.addView(section("SOZLASH"))
        accRow = row("Maxsus imkoniyat", "Qulf ishlashi uchun kerak") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        dndRow = row("Bezovta qilmang", "Qo'ng'iroq va xabarlarni jim qiladi") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
        edgeRow = row("Chetni qulflash", "Yuqori va pastki chetga teginish yopiladi") {
            val sp = getSharedPreferences("gl", MODE_PRIVATE)
            sp.edit().putBoolean("edges", !sp.getBoolean("edges", true)).apply()
            refresh()
        }
        root.addView(accRow.box)
        root.addView(dndRow.box)
        root.addView(edgeRow.box)
        root.addView(label(
            "Maxsus imkoniyat tugmasi ishlamasa: Sozlamalar → Ilovalar → Game Lock → ⋮ → \"Cheklangan sozlamalarga ruxsat berish\".",
            11f, MUTED, false
        ).apply { setPadding(dp(4), 0, dp(4), dp(8)) })

        root.addView(section("O'YIN").apply { setPadding(0, dp(12), 0, dp(8)) })
        val gameBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = box(CARD, LINE, 14, 1)
            setPadding(dp(16), dp(12), dp(8), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(20) }
        }
        gameName = label("", 15f, Color.WHITE, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        gameBox.addView(gameName)
        gameBox.addView(Button(this).apply {
            text = "Tanlash"
            isAllCaps = false
            setTextColor(GOLD)
            setTypeface(null, Typeface.BOLD)
            background = box(GOLD_BG, GOLD, 10, 1)
            setOnClickListener { pickApp() }
        })
        root.addView(gameBox)

        lockBtn = Button(this).apply {
            isAllCaps = false
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(60)
            ).apply { bottomMargin = dp(16) }
            setOnClickListener { onLockClick() }
        }
        root.addView(lockBtn)

        root.addView(label(
            "Chiqish: ovoz + va − tugmalarini birga 3 soniya bosib turing.\n\n" +
                "Pastdan surib Home'ga chiqib ketsa, Sozlamalar → Tizim navigatsiyasi'dan 3 tugmali navigatsiyani tanlang.\n\n" +
                "Qo'ng'iroq va xabarlar o'yin paytida jim bo'ladi, o'yindan keyin bildirishnomalar panelida va qo'ng'iroqlar tarixida ko'rasiz.",
            12f, MUTED, false
        ).apply { gravity = Gravity.CENTER })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(BG)
            isFillViewport = true
            addView(root)
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    // ---------- holat ----------

    private fun accessibilityOn(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains("$packageName/") && s.contains("LockService")
    }

    private fun dndOn(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    private fun targetPkg(): String? {
        val saved = getSharedPreferences("gl", MODE_PRIVATE).getString("target", null)
        if (saved != null && packageManager.getLaunchIntentForPackage(saved) != null) return saved
        return pubgPackages.firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }
    }

    private fun labelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) { pkg }

    private fun refresh() {
        val acc = accessibilityOn()
        val dnd = dndOn()
        val pkg = targetPkg()
        val edges = getSharedPreferences("gl", MODE_PRIVATE).getBoolean("edges", true)

        setStatus(accRow, acc, "✓ Tayyor", "Yoqish ›")
        setStatus(dndRow, dnd, "✓ Tayyor", "Ruxsat ›")
        // chetni qulflash: yoqiq/o'chiq tugma
        edgeRow.status.text = if (edges) "YOQILGAN" else "O'CHIQ"
        edgeRow.status.setTextColor(if (edges) GREEN else MUTED)
        edgeRow.status.background = box(if (edges) GREEN_BG else CARD, if (edges) GREEN else LINE, 10, 1)

        gameName.text = if (pkg != null) labelOf(pkg) else "O'yin topilmadi, tanlang"

        if (LockState.locked) {
            banner.text = "QULF YOQILGAN\nChiqish: ovoz + va − tugmalarini 3 soniya ushlang"
            banner.setTextColor(RED)
            banner.background = box(RED_BG, RED, 14, 1)
            lockBtn.text = "Qulfni ochish"
            lockBtn.setTextColor(Color.WHITE)
            lockBtn.background = box(RED, RED, 14, 1)
            lockBtn.isEnabled = true
            lockBtn.alpha = 1f
        } else {
            val ready = acc && dnd && pkg != null
            banner.text = if (ready) "Hammasi tayyor" else "Avval sozlashni tugating"
            banner.setTextColor(if (ready) GREEN else MUTED)
            banner.background = box(if (ready) GREEN_BG else CARD, if (ready) GREEN else LINE, 14, 1)
            lockBtn.text = "Qulflash va o'yinni ochish"
            lockBtn.setTextColor(Color.BLACK)
            lockBtn.background = box(GOLD, GOLD, 14, 1)
            lockBtn.isEnabled = ready
            lockBtn.alpha = if (ready) 1f else 0.4f
        }
    }

    private fun setStatus(r: Row, ok: Boolean, okText: String, offText: String) {
        r.status.text = if (ok) okText else offText
        r.status.setTextColor(if (ok) GREEN else GOLD)
        r.status.background = if (ok) box(GREEN_BG, GREEN, 10, 1) else box(GOLD_BG, GOLD, 10, 1)
    }

    // ---------- amallar ----------

    private fun pickApp() {
        val pm = packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(q, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != packageName }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }
        val names = apps.map { it.second }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Ilovani tanlang")
            .setItems(names) { _, i ->
                getSharedPreferences("gl", MODE_PRIVATE).edit().putString("target", apps[i].first).apply()
                refresh()
            }
            .show()
    }

    private fun onLockClick() {
        if (LockState.locked) {
            LockState.unlock(this)
            refresh()
            return
        }
        val pkg = targetPkg() ?: return
        GameDnd.enable(this)
        LockState.target = pkg
        LockState.locked = true
        LockService.instance?.showEdges()
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    // ---------- ko'rinish yordamchilari ----------

    private fun row(title: String, sub: String, onClick: () -> Unit): Row {
        val b = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = box(CARD, LINE, 14, 1)
            setPadding(dp(16), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            setOnClickListener { onClick() }
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(label(title, 15f, Color.WHITE, true))
        val s = label(sub, 12f, MUTED, false)
        col.addView(s)
        val st = label("", 13f, GOLD, true).apply { setPadding(dp(12), dp(6), dp(12), dp(6)) }
        b.addView(col)
        b.addView(st)
        return Row(b, s, st)
    }

    private fun box(fill: Int, stroke: Int, radiusDp: Int, strokeDp: Int) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        setStroke(dp(strokeDp), stroke)
    }

    private fun label(t: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    private fun section(t: String) = label(t, 11f, MUTED, true).apply {
        letterSpacing = 0.12f
        setPadding(0, dp(4), 0, dp(8))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
