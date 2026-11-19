package ir.shomara.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * پوستهٔ اصلی شمارا — سربرگ + ناوبری پایین + همهٔ ماژول‌ها (مثل نسخهٔ وب)
 */
class MainActivity : Activity() {

    lateinit var container: FrameLayout
    lateinit var titleView: TextView
    lateinit var subView: TextView
    lateinit var backBtn: FrameLayout
    lateinit var logoBox: FrameLayout
    lateinit var themeIcon: android.widget.ImageView
    lateinit var addBtn: FrameLayout
    // میکروفن نیم‌دایرهٔ فوتر — نگه‌داشتن = شنیدن، رهاکردن = اجرای دستور
    lateinit var micBtn: FrameLayout
    lateinit var micGlow: View
    var micIdleBg: android.graphics.drawable.Drawable? = null
    lateinit var recPill: LinearLayout
    lateinit var recTitle: TextView
    lateinit var recLive: TextView
    lateinit var holdTalker: HoldTalker
    val navItems = HashMap<String, LinearLayout>()

    var tab = "dashboard"

    // وضعیت هر ماژول
    var invTab = K_SALE
    var invQuery = ""
    var itemsQuery = ""
    var itemsGroup = "all"
    var whFilter = "all"
    var partnersQuery = ""
    var partnersType = "all"
    var cashTab = "receipt"
    var checksTab = "received"
    var ledgerView = "journal"
    var ledgerQuery = ""
    var ledgerAcc = ""
    var reportTab = "pnl"
    var guideTab = "features"
    var quizState = "0|||"
    var reconBank = ""
    var reconExpanded = ""
    var projectsTab = "active"
    var timerFor = ""
    var timerStartAt = 0L
    var timerElapsed = 0L
    var chatMsgs = listOf<ChatMsg>()
    // جملهٔ اصلیِ فرمانی که منتظر تایید ثبت فاکتور/پرداخت است
    var pendingVoicePhrase: String = ""
    var tmpMoudian = Db.db.settings.moudian

    val MAIN_TABS = setOf("dashboard", "invoices", "items", "ledger")

    private var timerRunnable: Runnable? = null
    private var timerHandler: android.os.Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root.setBackgroundColor(T.bg)

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setBackgroundColor(T.bg)
        root.addView(col, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // ---- سربرگ ----
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(UI.dp(this, 12), UI.dp(this, 12), UI.dp(this, 12), UI.dp(this, 12))
        bar.setBackgroundColor(T.card)

        backBtn = FrameLayout(this)
        backBtn.background = UI.rounded(this, T.cardSoft, 14)
        backBtn.setPadding(UI.dp(this, 6), UI.dp(this, 6), UI.dp(this, 6), UI.dp(this, 6))
        val bicon = UI.icon(this, R.drawable.ic_chev_r, 20, T.text)
        val blp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        blp.gravity = Gravity.CENTER
        backBtn.addView(bicon, blp)
        backBtn.setOnClickListener { go("dashboard") }
        backBtn.visibility = View.GONE
        bar.addView(backBtn, LinearLayout.LayoutParams(UI.dp(this, 42), UI.dp(this, 42)))

        logoBox = FrameLayout(this)
        logoBox.background = UI.gradient(this, 0xFF10B981.toInt(), 0xFF047857.toInt(), 14)
        val logo = TextView(this)
        logo.text = "ش"
        logo.textSize = 16f
        logo.setTextColor(Color.WHITE)
        logo.typeface = U.fontB ?: Typeface.DEFAULT_BOLD
        logo.gravity = Gravity.CENTER
        val llp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        logoBox.addView(logo, llp)
        bar.addView(logoBox, LinearLayout.LayoutParams(UI.dp(this, 42), UI.dp(this, 42)))

        val tc = LinearLayout(this)
        tc.orientation = LinearLayout.VERTICAL
        val tlp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        tlp.marginStart = UI.dp(this, 12)
        titleView = UI.text(this, "شمارا ۵", 15, T.text, true)
        tc.addView(titleView)
        subView = UI.text(this, J.jFa(J.jTodayStr()), 10.5f, T.sub)
        tc.addView(subView)
        bar.addView(tc, tlp)

        val searchBtn = UI.iconBox(this, R.drawable.ic_search, T.sub, T.cardSoft, 40, 18)
        searchBtn.setOnClickListener { globalSearchSheet(this) }
        bar.addView(searchBtn)

        // دکمهٔ افزودن سریع ماژول — چون میکروفن جای «+» شناور را در فوتر گرفته،
        // دکمهٔ افزودن هر ماژول به سربرگ منتقل شد و هیچ امکان کم نشده
        addBtn = UI.iconBox(this, R.drawable.ic_plus, T.primary, T.primarySoft, 40, 18)
        addBtn.setOnClickListener { onFab() }
        bar.addView(addBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.marginStart = UI.dp(this, 8) })

        themeIcon = UI.icon(this, R.drawable.ic_moon, 18, T.sub)
        val themeBtn = FrameLayout(this)
        themeBtn.background = UI.rounded(this, T.cardSoft, 14)
        val t2lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        t2lp.gravity = Gravity.CENTER
        themeBtn.addView(themeIcon, t2lp)
        themeBtn.setOnClickListener { toggleTheme() }
        val tbl = LinearLayout.LayoutParams(UI.dp(this, 40), UI.dp(this, 40))
        tbl.marginStart = UI.dp(this, 8)
        themeBtn.layoutParams = tbl
        bar.addView(themeBtn)

        col.addView(bar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(View(this).also { it.setBackgroundColor(T.border) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 1)))

        // ---- محتوا ----
        container = FrameLayout(this)
        col.addView(container, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // ---- ناوبری پایین + میکروفن نیم‌دایره (مثل تصویر مرجع) ----
        // نوار قرصی سفید با ۴ مقصد متوازن و میکروفنی که از نیم‌دایرهٔ وسط بالا می‌زند؛
        // میکروفن جای «+» را گرفته و فقط با نگه‌داشتن کار می‌کند
        val navWrap = FrameLayout(this)
        navWrap.clipChildren = false
        navWrap.clipToPadding = false
        col.clipChildren = false
        col.clipToPadding = false

        val navBar = LinearLayout(this)
        navBar.orientation = LinearLayout.HORIZONTAL
        navBar.gravity = Gravity.CENTER_VERTICAL
        navBar.background = UI.stroke(this, T.card, 28, T.border, 1)
        navBar.elevation = UI.dp(this, 12).toFloat()
        navBar.setPadding(UI.dp(this, 6), UI.dp(this, 6), UI.dp(this, 6), UI.dp(this, 6))
        val barLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 66), Gravity.BOTTOM)
        barLp.setMargins(UI.dp(this, 14), 0, UI.dp(this, 14), UI.dp(this, 9))
        navWrap.addView(navBar, barLp)

        // چهار مقصد متوازن: دو گزینه در هر سمت میکروفن مرکزی
        val navDefs = arrayOf(
            arrayOf("dashboard", "خانه", R.drawable.ic_home),
            arrayOf("invoices", "فروش", R.drawable.ic_receipt),
            arrayOf("items", "کالا", R.drawable.ic_boxes),
            arrayOf("ledger", "مالی", R.drawable.ic_calc)
        )
        for ((idx, d) in navDefs.withIndex()) {
            if (idx == 2) {
                // گود وسط نوار — جای میکروفن
                navBar.addView(View(this), LinearLayout.LayoutParams(UI.dp(this, 70), UI.dp(this, 1)))
            }
            val item = makeNavItem(d[0] as String, d[1] as String, d[2] as Int)
            navItems[d[0] as String] = item
            navBar.addView(item, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        // نیم‌دایرهٔ گود (رنگ پس‌زمینه → توهم بریدگی) + هالهٔ نئونی + دکمهٔ میکروفن
        val notch = View(this)
        notch.background = UI.oval(this, T.bg)
        val notchLp = FrameLayout.LayoutParams(
            UI.dp(this, 82), UI.dp(this, 82), Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
        notchLp.bottomMargin = UI.dp(this, 40)
        navWrap.addView(notch, notchLp)

        micGlow = View(this)
        micGlow.background = UI.oval(this, 0x337C3AED)
        val glowLp = FrameLayout.LayoutParams(
            UI.dp(this, 92), UI.dp(this, 92), Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
        glowLp.bottomMargin = UI.dp(this, 31)
        navWrap.addView(micGlow, glowLp)

        micBtn = FrameLayout(this)
        micBtn.clipChildren = false
        micIdleBg = UI.gradient(this, 0xFF7C3AED.toInt(), 0xFF4F46E5.toInt(), 30)
        micBtn.background = micIdleBg
        micBtn.elevation = UI.dp(this, 10).toFloat()
        val micIv = UI.icon(this, R.drawable.ic_mic, 26, Color.WHITE)
        micBtn.addView(micIv, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        val micLp = FrameLayout.LayoutParams(
            UI.dp(this, 62), UI.dp(this, 62), Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
        micLp.bottomMargin = UI.dp(this, 43)
        navWrap.addView(micBtn, micLp)
        setupHoldTalk(micBtn)

        col.addView(navWrap, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(this, 104)))

        // پیام «در حال شنیدن» — شناور بالای فوتر، متن زندهٔ کاربر را نشان می‌دهد
        recPill = LinearLayout(this)
        recPill.orientation = LinearLayout.HORIZONTAL
        recPill.gravity = Gravity.CENTER_VERTICAL
        recPill.background = UI.rounded(this, 0xEE312E81.toInt(), 24)
        recPill.setPadding(UI.dp(this, 16), UI.dp(this, 10), UI.dp(this, 16), UI.dp(this, 10))
        recPill.elevation = UI.dp(this, 12).toFloat()
        val dot = View(this)
        dot.background = UI.oval(this, 0xFFFF5470.toInt())
        try {
            dot.startAnimation(AlphaAnimation(1f, 0.25f).apply {
                duration = 550; repeatMode = Animation.REVERSE; repeatCount = Animation.INFINITE
            })
        } catch (ig: Throwable) {}
        recPill.addView(dot, LinearLayout.LayoutParams(UI.dp(this, 10), UI.dp(this, 10)))
        recTitle = UI.text(this, "در حال شنیدن…", 12.5f, Color.WHITE, true)
        recPill.addView(recTitle, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.marginStart = UI.dp(this, 8) })
        recLive = UI.text(this, "…", 12.5f, 0xFFC7D2FE.toInt(), true)
        recPill.addView(recLive, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.marginStart = UI.dp(this, 8) })
        recPill.visibility = View.GONE
        root.clipChildren = false
        root.addView(recPill, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).also { it.bottomMargin = UI.dp(this, 118) })

        setContentView(root)

        // بازیابی وضعیت
        try {
            val prefs = getSharedPreferences("shomara", MODE_PRIVATE)
            T.dark = prefs.getBoolean("dark", false)
            tab = prefs.getString("screen", "dashboard") ?: "dashboard"
            quizState = prefs.getString("quiz", "0|||") ?: "0|||"
        } catch (ig: Exception) {}
        timerHandler = android.os.Handler(mainLooper)
        loadChat()
        // حافظهٔ یادگیری هوشیار — جملات کاربر بین اجراها حفظ می‌شود
        try { VoiceLearn.init(filesDir) } catch (ig: Throwable) {}
        startTicker()

        // صدور خودکار فاکتورهای تکرارشونده — مثل نسخهٔ وب
        try {
            val (next, created) = Db.runRecurring(Db.db)
            if (created.isNotEmpty()) {
                Db.db = next
                Db.save()
                UI.toast(this, J.faN(created.size) + " فاکتور تکرارشونده خودکار صادر شد — سند حسابداری هم ساخته شد")
            }
        } catch (e: Throwable) { ErrorReport.report(this, "onCreate:recurring", e) }
        try { go(tab) } catch (e: Throwable) {
            ErrorReport.report(this, "onCreate:go(" + tab + ")", e)
            UI.toast(this, "باز شدن آخرین صفحه با خطا مواجه شد")
            try { go("dashboard") } catch (ig: Throwable) {}
        }
    }

    private fun startTicker() {
        timerRunnable?.let { timerHandler?.removeCallbacks(it) }
        val r = object : Runnable {
            override fun run() {
                val active = timerFor.isNotEmpty() && timerStartAt > 0
                if (active) {
                    timerElapsed = (System.currentTimeMillis() - timerStartAt) / 1000
                    if (tab == "projects") {
                        // فقط متن تایمر به‌روز شود
                        try {
                            val tv = container.findViewWithTag<TextView>("timer_text")
                            tv?.text = fmtElapsed(timerElapsed)
                        } catch (ig: Exception) {}
                    }
                }
                // وقتی تایمری فعال نیست هر ۵ ثانیه یک‌بار چک می‌کنیم (صرفه‌جویی در باتری)
                timerHandler?.postDelayed(this, if (active) 1000 else 5000)
            }
        }
        timerRunnable = r
        timerHandler?.postDelayed(r, 1000)
    }

    private fun makeNavItem(key: String, label: String, ic: Int): LinearLayout {
        val v = LinearLayout(this)
        v.orientation = LinearLayout.VERTICAL
        v.gravity = Gravity.CENTER
        v.setPadding(0, UI.dp(this, 6), 0, UI.dp(this, 4))
        // قرصِ ملایم دور آیکون — تب فعال را مثل اپ‌های مدرن واضح می‌کند
        val pill = FrameLayout(this)
        pill.tag = "pill"
        val iv = UI.icon(this, ic, 21, T.sub)
        iv.tag = "ic"
        val ilp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        ilp.gravity = Gravity.CENTER
        pill.addView(iv, ilp)
        pill.setPadding(UI.dp(this, 14), UI.dp(this, 4), UI.dp(this, 14), UI.dp(this, 4))
        v.addView(pill)
        val t = UI.text(this, label, 9.5f, T.sub, true)
        t.tag = "lb"
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = UI.dp(this, 3)
        v.addView(t, lp)
        v.setOnClickListener { go(key) }
        return v
    }

    private fun styleNav() {
        for ((k, v) in navItems) {
            val active = k == tab
            val pill = v.findViewWithTag("pill") as? FrameLayout
            val ic = v.findViewWithTag("ic") as? android.widget.ImageView
            val lb = v.findViewWithTag("lb") as? TextView
            pill?.background = if (active) UI.rounded(this, T.primarySoft, 14) else null
            ic?.setColorFilter(if (active) T.primary else T.sub)
            lb?.setTextColor(if (active) T.primary else T.sub)
        }
    }

    fun go(t: String) {
        tab = t
        try {
            getSharedPreferences("shomara", MODE_PRIVATE).edit().putString("screen", t).apply()
        } catch (ig: Exception) {}
        container.removeAllViews()
        // مهار کرش صفحه — اگر ساخت هر بخشی خطا بدهد، اپ بسته نمی‌شود؛ کارت خطا با دکمهٔ تلاش دوباره و ارسال گزارش می‌آید
        val v: View = try {
            when (t) {
                "dashboard" -> dashboardScreen(this)
                "invoices" -> invoicesScreen(this)
                "items" -> itemsScreen(this)
                "warehouse" -> warehouseScreen(this)
                "partners" -> partnersScreen(this)
                "cash" -> cashScreen(this)
                "checks" -> checksScreen(this)
                "ledger" -> ledgerScreen(this)
                "reports" -> reportsScreen(this)
                "tools" -> toolsScreen(this)
                "guide" -> guideScreen(this)
                "assistant" -> assistantScreen(this)
                "reconcile" -> reconcileScreen(this)
                "projects" -> projectsScreen(this)
                "more" -> moreScreen(this)
                else -> dashboardScreen(this)
            }
        } catch (e: Throwable) {
            ErrorReport.report(this, "screen:" + t, e)
            UI.toast(this, "نمایش این بخش با خطا مواجه شد — گزارش ثبت شد")
            errorCard(this, t, e)
        }
        container.addView(v, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        // تشخیص چیدمان — اگر صفحه از دید کوتاه‌تر بود یا اندازه‌گیری غیرعادی داشت، در گزارش ثبت می‌شود
        v.post {
            try {
                val vh = container.height
                val ch = v.height
                val scroll = if (v is android.widget.ScrollView) v.getChildAt(0) else null
                val sh = scroll?.height ?: -1
                if (vh > 0 && (ch < vh || (sh >= 0 && sh < vh / 2)))
                    ErrorReport.note(this, "layout:" + t,
                        "viewport=" + vh + "px screen=" + ch + "px content=" + sh + "px children=" + (scroll?.let { (it as? android.view.ViewGroup)?.childCount } ?: -1))
            } catch (ig: Throwable) {}
        }
        // انیمیشن ورود صفحه — مثل anim-tab نسخهٔ وب
        // ⚠ اصلاح کرش/صفحهٔ خالی: اگر انیمیشن به هر دلیلی (مثلاً روی برخی رام‌ها مثل MIUI)
        // اجرا نشود، صفحه تا همیشه آلفا=۰ می‌ماند و «همه جا خالی» دیده می‌شد.
        // حالا: انیمیشن فقط بعد از چسبیدن ویو به پنجره اجرا می‌شود و یک نگهبان تضمینی
        // بعد از ۴۵۰ میلی‌ثانیه، نمایش را اجباراً کامل می‌کند — دیگر هیچ صفحه‌ای خالی نمی‌ماند.
        try {
            v.alpha = 0f
            v.translationY = UI.dp(this, 12).toFloat()
            v.post {
                try { v.animate().alpha(1f).translationY(0f).setDuration(220).start() } catch (ig: Exception) {
                    try { v.alpha = 1f; v.translationY = 0f } catch (ig2: Exception) {}
                }
            }
            // نگهبان تضمینی — حتی اگر انیمیشن گم شد، صفحه حتماً دیده می‌شود
            v.postDelayed({
                try { if (v.alpha < 1f) { v.alpha = 1f; v.translationY = 0f } } catch (ig: Exception) {}
            }, 450L)
        } catch (ig: Exception) {
            try { v.alpha = 1f; v.translationY = 0f } catch (ig2: Exception) {}
        }
        // نگهبان محتوا — اگر به هر دلیل هیچ ویویی در ظرف نبود، داشبورد ساخته می‌شود
        container.postDelayed({
            try {
                if (tab == t && container.childCount == 0) {
                    val fb = try { dashboardScreen(this@MainActivity) } catch (ig: Throwable) { null }
                    if (fb != null) {
                        fb.alpha = 1f
                        container.addView(fb, FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    }
                }
            } catch (ig: Throwable) {}
        }, 700L)
        val meta = TITLES[t] ?: Pair("شمارا", "")
        titleView.text = meta.first
        subView.text = J.jFa(J.jTodayStr()) + " • " + meta.second
        val isMain = t in MAIN_TABS
        backBtn.visibility = if (isMain) View.GONE else View.VISIBLE
        logoBox.visibility = if (isMain) View.VISIBLE else View.GONE
        styleNav()
        // دکمهٔ + سربرگ — افزودن سریع همان ماژول (جایگزین FAB شناور)
        val showAdd = when (t) {
            "invoices" -> invTab != "repeat"
            "items", "partners", "checks", "reconcile", "projects", "warehouse" -> true
            else -> false
        }
        addBtn.visibility = if (showAdd) View.VISIBLE else View.GONE
    }

    /** میکروفن فوتر: نگه‌داشتن = شنیدن پیوسته، رهاکردن = پردازش دستور */
    private fun setupHoldTalk(btn: FrameLayout) {
        holdTalker = HoldTalker(this)
        val micIdle = UI.gradient(this, 0xFF7C3AED.toInt(), 0xFF4F46E5.toInt(), 30)
        micIdleBg = micIdle
        val pulse = ScaleAnimation(1f, 1.18f, 1f, 1.18f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 650; repeatMode = Animation.REVERSE; repeatCount = Animation.INFINITE
        }
        holdTalker.onUpdate = { p ->
            recLive.text = p
            recPill.visibility = View.VISIBLE
        }
        holdTalker.onActive = { active ->
            if (active) {
                btn.animate().scaleX(0.94f).scaleY(0.94f).setDuration(110).start()
                btn.background = UI.gradient(this, 0xFFF43F5E.toInt(), 0xFFBE123C.toInt(), 30)
                micGlow.background = UI.oval(this, 0x55F43F5E)
                try { micGlow.startAnimation(pulse) } catch (ig: Throwable) {}
                recTitle.text = "در حال شنیدن…"
                recLive.text = "…"
                recPill.visibility = View.VISIBLE
                vibrate()
            } else {
                btn.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                btn.background = micIdle
                micGlow.clearAnimation()
                micGlow.background = UI.oval(this, 0x337C3AED)
                recPill.visibility = View.GONE
            }
        }
        holdTalker.onFinal = { text ->
            if (text.isBlank()) {
                UI.toast(this, "چیزی نشنیدم — دکمهٔ میکروفن را نگه دارید و بگویید")
            } else {
                val res = try { processVoiceCommand(text) } catch (e: Throwable) {
                    ErrorReport.report(this, "hold-exec", e)
                    null
                }
                if (res != null) UI.toast(this, res.first.text.replace("\n", " ").take(120))
            }
        }
        btn.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { startHold(); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { endHold(); true }
                else -> true
            }
        }
    }

    private fun startHold() {
        val needPerm = Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        if (needPerm) {
            try { requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 7001) } catch (e: Throwable) {}
            UI.toast(this, "برای دستیار صوتی، اجازهٔ میکروفن را بدهید و دوباره نگه دارید")
            return
        }
        holdTalker.start()
    }

    private fun endHold() { holdTalker.release() }

    private fun vibrate(ms: Long = 30) {
        try {
            @Suppress("DEPRECATION")
            val v = getSystemService(VIBRATOR_SERVICE) as? android.os.Vibrator
            if (Build.VERSION.SDK_INT >= 26) {
                v?.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v?.vibrate(ms)
            }
        } catch (ig: Throwable) {}
    }

    fun refresh() { go(tab) }
    fun refreshAssistant() { if (tab == "assistant") refresh() }

    private fun onFab() {
        // مهار کرش باتن‌شیت‌ها — مثل صفحات
        try {
            when (tab) {
                "invoices" -> invoiceEditorSheet(this, newInvoiceFor(invTab))
                "items" -> itemEditorSheet(this, Item(Db.newId("it"), nextItemCode(), "", Db.db.groups.firstOrNull()?.id ?: "", "عدد", 0, 0, false, 0.0))
                "warehouse" -> stockDocSheet(this, "transfer", null)
                "partners" -> partnerEditorSheet(this, Partner(Db.newId("pt"), Db.nextPartnerCode(Db.db), "", "customer"))
                "checks" -> checkEditorSheet(this, Check(Db.newId("ck"), 500L + Db.db.checks.size + 1, checksTab, Db.db.partners.firstOrNull()?.id ?: "", "", "", 0, J.jTodayStr(), J.jTodayStr(), "drawer"))
                "reconcile" -> bankTxnSheet(this)
                "projects" -> projectSheet(this)
            }
        } catch (e: Throwable) {
            ErrorReport.report(this, "add:" + tab, e)
            UI.toast(this, "باز شدن این بخش با خطا مواجه شد — گزارش ثبت شد")
        }
    }

    /** کارت خطای درون‌صفحه — اپ هرگز به‌خاطر یک بخش بسته نمی‌شود */
    fun errorCard(act: MainActivity, screen: String, e: Throwable): View {
        val c = act
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(UI.dp(c, 20), UI.dp(c, 40), UI.dp(c, 20), UI.dp(c, 20))
        val card = UI.card(c)
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(UI.dp(c, 18), UI.dp(c, 24), UI.dp(c, 18), UI.dp(c, 18))
        card.addView(UI.iconBox(c, R.drawable.ic_alert, T.rose, T.roseSoft, 56, 28, 16))
        val t1 = UI.text(c, "نمایش این بخش با خطا مواجه شد", 15, T.text, true)
        t1.gravity = Gravity.CENTER
        val l1 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        l1.topMargin = UI.dp(c, 12)
        card.addView(t1, l1)
        val t2 = UI.text(c, "ردپای خطا ثبت شد و اپ بسته نشد. با دکمهٔ زیر می‌توانید گزارش را ببینید یا برای رفع سریع بفرستید.", 11.5f, T.sub)
        t2.gravity = Gravity.CENTER
        t2.setLineSpacing(0f, 1.25f)
        val l2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        l2.topMargin = UI.dp(c, 6)
        card.addView(t2, l2)
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        val rl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rl.topMargin = UI.dp(c, 14)
        row.addView(UI.primaryBtn(c, "تلاش دوباره") { act.go(screen) }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val s1 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        s1.marginStart = UI.dp(c, 8)
        row.addView(UI.outlineBtn(c, "گزارش خطا") { ErrorReport.share(c, "from:screen " + screen) }, s1)
        card.addView(row, rl)
        root.addView(card)
        return root
    }

    fun newInvoiceFor(kind: String): Invoice {
        val db = Db.db
        val partnerPool = if (kind == K_PURCHASE || kind == K_PURCHASE_RET) "supplier" else "customer"
        val defaultPartner = db.partners.firstOrNull { it.type == partnerPool || it.type == "both" } ?: db.partners.firstOrNull()
        val kindInvoices = db.invoices.filter { it.kind == kind }
        val base = when (kind) { K_SALE -> 100L; K_PURCHASE -> 200L; K_PROFORMA -> 300L; else -> 400L }
        return Invoice(
            Db.newId("inv"), base + kindInvoices.size + 1, kind,
            defaultPartner?.id ?: "", db.warehouses.firstOrNull()?.id ?: "",
            J.jTodayStr(), emptyList(), 0, db.settings.vatRate, 0,
            payBankId = db.banks.firstOrNull()?.id ?: "",
            final = false, moudian = kind == K_SALE && db.settings.moudian, desc = ""
        )
    }

    fun nextItemCode(): String {
        val nums = Db.db.items.map { it.code.toIntOrNull() ?: 0 }
        return (Math.max(Math.max(nums.maxOrNull() ?: 100, 100), 100) + 1).toString()
    }

    fun toggleTheme() {
        T.dark = !T.dark
        try {
            getSharedPreferences("shomara", MODE_PRIVATE).edit().putBoolean("dark", T.dark).apply()
        } catch (ig: Exception) {}
        UI.toast(this, if (T.dark) "پوسته تیره فعال شد" else "پوسته روشن فعال شد")
        // بازسازی کامل رابط
        recreateSoft()
    }

    /** بازسازی ریشه بدون از دست رفتن فونت‌ها — با recreate رسمی اندروید */
    private fun recreateSoft() {
        recreate()
    }

    // ---- مکالمهٔ دستیار: ذخیرهٔ ماندگار مثل نسخهٔ وب ----
    fun saveChat() {
        try {
            val arr = org.json.JSONArray()
            for (m in chatMsgs) {
                val o = org.json.JSONObject()
                o.put("role", m.role); o.put("text", m.text)
                o.put("al", m.actionLabel); o.put("ag", m.actionGo)
                arr.put(o)
            }
            getSharedPreferences("shomara", MODE_PRIVATE).edit().putString("chat", arr.toString()).apply()
        } catch (ig: Exception) {}
    }

    fun loadChat() {
        try {
            val s = getSharedPreferences("shomara", MODE_PRIVATE).getString("chat", "") ?: ""
            if (s.isEmpty()) return
            val arr = org.json.JSONArray(s)
            val list = ArrayList<ChatMsg>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(ChatMsg(o.optString("id", Db.newId("m")), o.optString("role", "bot"), o.optString("text"), o.optString("al"), o.optString("ag")))
            }
            chatMsgs = list
        } catch (ig: Exception) {}
    }

    fun backupJson() {
        try {
            val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
            i.type = "application/json"
            i.putExtra(Intent.EXTRA_TITLE, "shomara-backup-" + J.jTodayStr().replace("/", "-") + ".json")
            startActivityForResult(i, 21)
        } catch (ig: Exception) { UI.toast(this, "امکان ذخیره نیست") }
    }

    fun restoreJson() {
        try {
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.type = "application/json"
            startActivityForResult(Intent.createChooser(i, "انتخاب فایل پشتیبان"), 22)
        } catch (ig: Exception) { UI.toast(this, "امکان بازیابی نیست") }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res != RESULT_OK || data == null || data.data == null) return
        if (req == 21) {
            try {
                val os = contentResolver.openOutputStream(data?.data ?: return)
                os?.use { it.write(Db.toJson(Db.db).toString().toByteArray(Charsets.UTF_8)) }
                UI.toast(this, "پشتیبان کامل ذخیره شد — فایل JSON را در جای امن نگه دارید")
            } catch (e: Exception) { UI.toast(this, "خطا در ذخیره فایل") }
        }
        if (req == 22) {
            try {
                val inp = contentResolver.openInputStream(data?.data ?: return)
                val s = inp?.readBytes()?.toString(Charsets.UTF_8) ?: ""
                inp?.close()
                if (s.startsWith("{")) {
                    val restored = Db.fromJson(org.json.JSONObject(s))
                    // اعتبارسنجی قوی‌تر — مثل نسخهٔ وب: بخش‌های اصلی باید باشند
                    val ok = restored != null && restored.settings.shopName.isNotEmpty() &&
                        org.json.JSONObject(s).has("items") && org.json.JSONObject(s).has("partners") && org.json.JSONObject(s).has("settings")
                    if (ok && restored != null) {
                        Db.db = Db.normalize(restored)
                        Db.save()
                        UI.toast(this, "بازیابی کامل انجام شد — " + J.faN(restored.items.size) + " کالا و " + J.faN(restored.partners.size) + " طرف‌حساب")
                        refresh()
                    } else UI.toast(this, "فایل پشتیبان معتبر نیست")
                } else UI.toast(this, "فایل پشتیبان معتبر نیست")
            } catch (e: Exception) { UI.toast(this, "فایل معتبر نیست") }
        }
    }

    override fun onBackPressed() {
        if (tab !in MAIN_TABS) go("dashboard") else super.onBackPressed()
    }

    /** پاسخ مجوز میکروفون — بعد از اجازه، دستیار صوتی باز می‌شود */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7001) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                UI.toast(this, "حالا دکمهٔ میکروفن فوتر را نگه دارید و حرف بزنید")
            } else {
                UI.toast(this, "بدون میکروفون هم می‌توانید از صفحهٔ هوشیار تایپی استفاده کنید")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunnable?.let { timerHandler?.removeCallbacks(it) }
        try { holdTalker.shutdown() } catch (ig: Throwable) {}
    }
}
