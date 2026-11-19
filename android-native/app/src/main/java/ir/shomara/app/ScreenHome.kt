package ir.shomara.app

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * داشبورد + صفحهٔ بیشتر — قلب برنامه: خلاصهٔ کل کسب‌وکار و شبکهٔ ماژول‌ها
 */

data class ModuleDef(val id: String, val label: String, val ic: Int, val c1: Int, val c2: Int, val desc: String, val short: String = "")

val MODULES = listOf(
    ModuleDef("invoices", "فروش و فاکتور", R.drawable.ic_receipt, 0xFF10B981.toInt(), 0xFF0D9488.toInt(), "فروش، خرید، پیش‌فاکتور، برگشت", "فروش"),
    ModuleDef("items", "کالا و خدمات", R.drawable.ic_boxes, 0xFF0EA5E9.toInt(), 0xFF2563EB.toInt(), "تعریف کالا، قیمت و بارکد", "کالاها"),
    ModuleDef("warehouse", "انبار", R.drawable.ic_pkg_check, 0xFF8B5CF6.toInt(), 0xFF9333EA.toInt(), "موجودی، انتقال، اصلاح", "انبار"),
    ModuleDef("partners", "طرف‌حساب‌ها", R.drawable.ic_users, 0xFFF59E0B.toInt(), 0xFFEA580C.toInt(), "مشتری، تامین‌کننده، کارت حساب", "طرف‌حساب‌ها"),
    ModuleDef("cash", "دریافت و پرداخت", R.drawable.ic_banknote, 0xFF84CC16.toInt(), 0xFF16A34A.toInt(), "نقد، کارتخوان، حواله", "خزانه"),
    ModuleDef("checks", "چک و خزانه", R.drawable.ic_landmark, 0xFFF43F5E.toInt(), 0xFFDC2626.toInt(), "سررسید، وصول، برگشت", "چک‌ها"),
    ModuleDef("ledger", "حسابداری", R.drawable.ic_calc, 0xFF14B8A6.toInt(), 0xFF0891B2.toInt(), "سند، دفاتر کل و معین", "حسابداری"),
    ModuleDef("reports", "گزارش‌ها", R.drawable.ic_chart, 0xFFD946EF.toInt(), 0xFFEC4899.toInt(), "تراز، سود و زیان، گردش", "گزارش‌ها"),
    ModuleDef("assistant", "هوشیار — دستیار هوشمند", R.drawable.ic_sparkles, 0xFF8B5CF6.toInt(), 0xFF6366F1.toInt(), "سوال بپرس، پاسخ بگیر، عمل کن", "هوشیار"),
    ModuleDef("reconcile", "تطبیق بانک", R.drawable.ic_bank, 0xFF0EA5E9.toInt(), 0xFF2563EB.toInt(), "مثل Xero — تطبیق با یک لمس", "تطبیق بانک"),
    ModuleDef("projects", "پروژه و زمان", R.drawable.ic_timer, 0xFFF59E0B.toInt(), 0xFFEA580C.toInt(), "تایمر، ساعت کار، فاکتور خودکار", "پروژه‌ها"),
    ModuleDef("tools", "ابزار و تنظیمات", R.drawable.ic_gear, 0xFF64748B.toInt(), 0xFF475569.toInt(), "پشتیبان، اطلاعات فروشگاه", "ابزارها"),
    ModuleDef("guide", "راهنمای شمارا", R.drawable.ic_book, 0xFF047857.toInt(), 0xFF166534.toInt(), "نسخه‌ها، قیمت، آزمون خرید", "راهنما")
)

val TITLES: Map<String, Pair<String, String>> = mapOf(
    "dashboard" to Pair("شمارا ۵", "مجموعهٔ کامل مدیریت کسب‌وکار"),
    "invoices" to Pair("فروش و فاکتور", "فروش، خرید، پیش‌فاکتور و تکرارشونده"),
    "items" to Pair("کالا و خدمات", "تعریف کالا، قیمت و بارکد"),
    "warehouse" to Pair("انبار", "موجودی، انتقال و اصلاح"),
    "partners" to Pair("طرف‌حساب‌ها", "مشتریان و تامین‌کنندگان"),
    "cash" to Pair("دریافت و پرداخت", "خزانهٔ نقد، کارتخوان و حواله"),
    "checks" to Pair("چک و خزانه", "سررسید، وصول و برگشت چک"),
    "ledger" to Pair("حسابداری", "سند، دفاتر کل و معین"),
    "reports" to Pair("گزارش‌ها", "تراز، سود و زیان و گردش"),
    "tools" to Pair("ابزار و تنظیمات", "پشتیبان، فروشگاه و یادآور"),
    "guide" to Pair("راهنمای شمارا", "نسخه‌ها، قیمت و آزمون خرید"),
    "assistant" to Pair("هوشیار", "دستیار مالی هوشمند"),
    "reconcile" to Pair("تطبیق بانک", "تطبیق تراکنش‌ها با یک لمس"),
    "projects" to Pair("پروژه و زمان", "ثبت ساعت کار و صدور فاکتور"),
    "more" to Pair("بیشتر", "همهٔ ماژول‌ها و تنظیمات")
)

fun moduleCard(c: Context, m: ModuleDef, go: (String) -> Unit): LinearLayout {
    val v = LinearLayout(c)
    v.orientation = LinearLayout.VERTICAL
    v.background = UI.stroke(c, T.card, 18, T.border, 1)
    v.setPadding(UI.dp(c, 14), UI.dp(c, 14), UI.dp(c, 14), UI.dp(c, 14))
    val icon = View(c)
    icon.background = UI.gradient(c, m.c1, m.c2, 14)
    val f = FrameLayout(c)
    val iv = UI.icon(c, m.ic, 22, Color.WHITE)
    f.addView(iv, FrameLayout.LayoutParams(UI.dp(c, 22), UI.dp(c, 22), Gravity.CENTER))
    f.background = UI.gradient(c, m.c1, m.c2, 14)
    f.layoutParams = LinearLayout.LayoutParams(UI.dp(c, 44), UI.dp(c, 44))
    v.addView(f)
    val t = UI.text(c, m.label, 13, T.text, true)
    val tl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    tl.topMargin = UI.dp(c, 10)
    v.addView(t, tl)
    val d = UI.text(c, m.desc, 10, T.sub)
    d.setLineSpacing(0f, 1.1f)
    val dl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    dl.topMargin = UI.dp(c, 4)
    v.addView(d, dl)
    v.setOnClickListener { go(m.id) }
    return v
}

/** کاشی کوچک ماژول برای شبکهٔ فشردهٔ داشبورد — اندازهٔ ثابت برای هم‌ترازی کامل افقی/عمودی */
fun moduleTile(c: Context, m: ModuleDef, onClick: () -> Unit): LinearLayout {
    val v = LinearLayout(c)
    v.orientation = LinearLayout.VERTICAL
    v.gravity = Gravity.CENTER
    v.background = UI.stroke(c, T.card, 16, T.border, 1)
    v.setPadding(UI.dp(c, 4), UI.dp(c, 10), UI.dp(c, 4), UI.dp(c, 8))
    val f = FrameLayout(c)
    f.background = UI.gradient(c, m.c1, m.c2, 12)
    val iv = UI.icon(c, m.ic, 18, Color.WHITE)
    f.addView(iv, FrameLayout.LayoutParams(UI.dp(c, 18), UI.dp(c, 18), Gravity.CENTER))
    f.layoutParams = LinearLayout.LayoutParams(UI.dp(c, 36), UI.dp(c, 36))
    v.addView(f)
    val t = UI.text(c, m.short.ifEmpty { m.label }, 10, T.text, true)
    t.gravity = Gravity.CENTER
    t.maxLines = 2
    // ارتفاع ثابت ناحیهٔ برچسب ⇒ همهٔ کاشی‌ها دقیقاً هم‌قد و هم‌تراز (کارت‌ها به هم نمی‌چسبند)
    val tl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 28))
    tl.topMargin = UI.dp(c, 6)
    v.addView(t, tl)
    v.setOnClickListener { onClick() }
    return v
}

/* ═════════════════════════ داشبورد ═════════════════════════ */

fun dashboardScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val go = { s: String -> act.go(s) }
    val today = J.jTodayStr()
    val sales = Db.monthlySales(db)
    val currentMonth = J.jMonthOf(today)
    val todaySales = db.invoices.filter { it.kind == K_SALE && it.final && it.date == today }.sumOf { Db.invoiceTotals(it).total }
    val monthSales = sales.getOrNull(currentMonth - 1)?.total ?: 0
    val pl = Db.profitLoss(db)
    val cash = Db.totalCash(db)
    val receivable = Db.totalReceivable(db)
    val payable = Db.totalPayable(db)
    val lows = Db.lowStockItems(db)
    val dueChecks = db.checks.filter { (it.kind == "received" && (it.status == "drawer" || it.status == "inBank")) || (it.kind == "issued" && it.status == "drawer") }
    val nearChecks = dueChecks.filter { J.jDiff(it.dueDate, today) in 0..30 }.sortedBy { it.dueDate }
    val undoneReminders = db.reminders.filter { !it.done }.sortedBy { it.date }
    val recentInvoices = db.invoices.sortedByDescending { it.date }.take(4)
    val alerts = Db.smartAlerts(db)
    val cf = Db.cashflow(db, 6)
    val aging = Db.agingBuckets(db)
    val od = Db.overdueSales(db)
    val unbilledHours = db.timeEntries.filter { it.billed.isEmpty() }.sumOf { it.minutes } / 60.0
    val dueRecurring = db.recurring.filter { it.active && J.jDiff(today, it.nextDate) >= 0 }
    val unmatchedTxns = db.bankTxns.count { it.matched == null }

    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    // ---------- هدر خوش‌آمد ----------
    // ⚠ اصلاح به‌هم‌ریختگی: همهٔ عناصر داخل heroCol (یک ستون عمودی) هستند؛
    // قبلاً نام فروشگاه، زیرعنوان و آمار مستقیم به FrameLayout اضافه می‌شد و روی هم می‌افتاد.
    val hero = android.widget.FrameLayout(c)
    hero.background = UI.gradient(c, 0xFF059669.toInt(), 0xFF047857.toInt(), 22)
    val heroCol = LinearLayout(c)
    heroCol.orientation = LinearLayout.VERTICAL
    heroCol.setPadding(UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18))
    // دایره‌های تزئینی محو — مثل blur-circles نسخهٔ وب
    try {
        val orb1 = View(c)
        orb1.background = UI.oval(c, 0x2EFFFFFF)
        hero.addView(orb1, android.widget.FrameLayout.LayoutParams(UI.dp(c, 110), UI.dp(c, 110), Gravity.TOP or Gravity.START).also { it.marginStart = UI.dp(c, -40); it.topMargin = UI.dp(c, -34) })
        val orb2 = View(c)
        orb2.background = UI.oval(c, 0x22FFFFFF)
        hero.addView(orb2, android.widget.FrameLayout.LayoutParams(UI.dp(c, 80), UI.dp(c, 80), Gravity.BOTTOM or Gravity.END).also { it.marginEnd = UI.dp(c, -24); it.bottomMargin = UI.dp(c, -20) })
    } catch (ig: Exception) {}
    heroCol.addView(UI.text(c, J.jFa(today) + " — سال مالی " + J.fa(db.settings.fiscalYear), 12, 0xB3ECFDF5.toInt(), true))
    val h1 = UI.text(c, db.settings.shopName, 19, Color.WHITE, true)
    val h1l = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    h1l.topMargin = UI.dp(c, 5)
    heroCol.addView(h1, h1l)
    val hsub = UI.text(c, "مجموعهٔ کامل مدیریت کسب‌وکار — همهٔ ماژول‌های حسابداری حرفه‌ای", 11, 0xD1ECFDF5.toInt())
    val hsl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    hsl.topMargin = UI.dp(c, 3)
    heroCol.addView(hsub, hsl)
    // سه آمار — با فاصلهٔ یکنواخت بین خانه‌ها (heroStats)
    val statsLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    statsLp.topMargin = UI.dp(c, 14)
    heroCol.addView(UI.heroStats(c, listOf(
        Pair(J.moneyShort(monthSales), "فروش این ماه"),
        Pair(J.moneyShort(cash), "خزانه"),
        Pair(J.moneyShort(pl.net), "سود دوره")
    )), statsLp)
    hero.addView(heroCol, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    root.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

    // ---------- شبکهٔ همهٔ ماژول‌ها — درست زیر سربرگ تا همهٔ ماژول‌ها در همان صفحه دیده شوند ----------
    root.addView(UI.sectionTitle(c, "همهٔ ماژول‌ها", "همهٔ بخش‌های حسابداری حرفه‌ای + دستیار صوتی — همه در همین صفحه", R.drawable.ic_wallet))
    val tileGrid = LinearLayout(c)
    tileGrid.orientation = LinearLayout.VERTICAL
    // ۱۳ ماژول + دستیار صوتی + بیشتر = ۱۵ کاشی = ۵ ردیف کامل ۳ستونه
    val tiles = MODULES + ModuleDef("voice", "دستیار صوتی", R.drawable.ic_mic, 0xFF7C3AED.toInt(), 0xFF4F46E5.toInt(), "با صدا دستور بدهید", "دستیار صوتی") +
        ModuleDef("more", "همهٔ ماژول‌ها", R.drawable.ic_menu, 0xFF64748B.toInt(), 0xFF334155.toInt(), "همهٔ بخش‌ها یکجا", "همه‌چیز")
    for (chunk in tiles.chunked(3)) {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        for (i in 0 until 3) {
            if (i > 0) row.addView(View(c), LinearLayout.LayoutParams(UI.dp(c, 8), UI.dp(c, 1)))
            if (i < chunk.size) {
                val m = chunk[i]
                val tile = moduleTile(c, m) { if (m.id == "voice") act.openVoice() else go(m.id) }
                tile.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(tile)
            } else {
                row.addView(View(c), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
        }
        val rl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rl.topMargin = UI.dp(c, 8)
        tileGrid.addView(row, rl)
    }
    root.addView(tileGrid)

    // ---------- بنر دستیار هوشمند ----------
    if (alerts.isNotEmpty()) {
        val banner = LinearLayout(c)
        banner.orientation = LinearLayout.HORIZONTAL
        banner.gravity = Gravity.CENTER_VERTICAL
        banner.background = UI.gradient(c, 0x2E7C3AED.toInt(), 0x1A6366F1.toInt(), 20)
        banner.setPadding(UI.dp(c, 14), UI.dp(c, 13), UI.dp(c, 14), UI.dp(c, 13))
        banner.addView(UI.iconBox(c, R.drawable.ic_sparkles, Color.WHITE, 0xFF8B5CF6.toInt(), 42, 20, 14))
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        val cl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        cl.marginStart = UI.dp(c, 12)
        col.addView(UI.text(c, J.faN(alerts.size) + " هشدار هوشمند برای شما", 12.5f, 0xFF7C3AED.toInt(), true))
        col.addView(UI.text(c, alerts[0].title + " — بپرسید از هوشیار", 10, T.sub))
        banner.addView(col, cl)
        banner.addView(UI.icon(c, R.drawable.ic_chev_l, 15, 0xFF8B5CF6.toInt()))
        banner.setOnClickListener { go("assistant") }
        val bl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        bl.topMargin = UI.dp(c, 10)
        root.addView(banner, bl)
    }

    // ---------- کارت‌های کلیدی ----------
    val g1 = UI.grid2(c,
        UI.statCard(c, "فروش امروز", J.moneyShort(todaySales) + " ت", J.faN(db.invoices.count { it.kind == K_SALE && it.final && it.date == today }) + " فاکتور", "green", R.drawable.ic_trend_up) { go("invoices") },
        UI.statCard(c, "مطالبات", J.moneyShort(receivable) + " ت", "از مشتریان", "amber", R.drawable.ic_users) { go("partners") }
    )
    val g2 = UI.grid2(c,
        UI.statCard(c, "بدهی‌ها", J.moneyShort(payable) + " ت", "به تامین‌کنندگان", "rose", R.drawable.ic_wallet) { go("partners") },
        UI.statCard(c, "کالای کم‌موجود", J.faN(lows.size), "نیاز به سفارش", "sky", R.drawable.ic_boxes) { go("reports") }
    )
    root.addView(g1)
    root.addView(g2)

    // ---------- کارهای امروز ----------
    val tasks = UI.card(c)
    val hdr = LinearLayout(c)
    hdr.orientation = LinearLayout.HORIZONTAL
    hdr.gravity = Gravity.CENTER_VERTICAL
    hdr.addView(UI.iconBox(c, R.drawable.ic_listcheck, T.primary, T.primarySoft, 40, 20))
    val hcol = LinearLayout(c)
    hcol.orientation = LinearLayout.VERTICAL
    val hcl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    hcl.marginStart = UI.dp(c, 12)
    hcol.addView(UI.text(c, "کارهای امروز", 15, T.text, true))
    hcol.addView(UI.text(c, "چیزی که منتظر شماست — مثل QuickBooks", 11, T.sub))
    hdr.addView(hcol, hcl)
    // دکمهٔ «هوشیار» در سربرگ کارهای امروز — مثل نسخهٔ وب
    if (alerts.isNotEmpty()) {
        val hf = UI.text(c, "هوشیار", 11, Color.WHITE, true)
        hf.gravity = Gravity.CENTER
        hf.background = UI.gradient(c, 0xFF7C3AED.toInt(), 0xFF4F46E5.toInt(), 12)
        hf.setPadding(UI.dp(c, 12), UI.dp(c, 7), UI.dp(c, 12), UI.dp(c, 8))
        hf.setOnClickListener { go("assistant") }
        hdr.addView(hf)
    }
    tasks.addView(hdr)
    if (alerts.isEmpty()) {
        val ok = UI.innerCard(c, 0x14059669.toInt(), 16)
        ok.orientation = LinearLayout.HORIZONTAL
        ok.gravity = Gravity.CENTER_VERTICAL
        ok.setPadding(UI.dp(c, 12), UI.dp(c, 11), UI.dp(c, 12), UI.dp(c, 11))
        ok.addView(UI.icon(c, R.drawable.ic_sparkles, 18, T.primary))
        val t = UI.text(c, "همه‌چیز مرتب است — کاری منتظر نیست", 12, T.primary, true)
        val tl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tl.marginStart = UI.dp(c, 10)
        ok.addView(t, tl)
        val okl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        okl.topMargin = UI.dp(c, 10)
        tasks.addView(ok, okl)
    } else {
        data class Task(val ic: Int, val tone: String, val label: String, val go: String)
        val taskList = ArrayList<Task>()
        if (od.isNotEmpty()) taskList.add(Task(R.drawable.ic_receipt, "rose", J.faN(od.size) + " فاکتور معوق — " + J.moneyShort(od.sumOf { it.amount }) + " تومان", "invoices"))
        if (dueRecurring.isNotEmpty()) taskList.add(Task(R.drawable.ic_repeat, "sky", J.faN(dueRecurring.size) + " فاکتور تکرارشونده آمادهٔ صدور", "invoices"))
        if (unmatchedTxns > 0) taskList.add(Task(R.drawable.ic_bank, "violet", J.faN(unmatchedTxns) + " تراکنش بانکی در انتظار تطبیق", "reconcile"))
        if (unbilledHours >= 0.5) taskList.add(Task(R.drawable.ic_timer, "amber", J.faN(Math.round(unbilledHours)) + " ساعت کار فاکتورنشده", "projects"))
        if (nearChecks.isNotEmpty()) taskList.add(Task(R.drawable.ic_clock, "amber", J.faN(nearChecks.size) + " چک در ۳۰ روز آینده سررسید می‌شود", "checks"))
        if (lows.isNotEmpty()) taskList.add(Task(R.drawable.ic_boxes, "sky", J.faN(lows.size) + " کالا به نقطهٔ سفارش رسیده", "warehouse"))
        for (tk in taskList) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.background = UI.stroke(c, T.toneSoft(tk.tone), 16, (0x33 shl 24) or (T.tone(tk.tone) and 0xFFFFFF), 1)
            row.setPadding(UI.dp(c, 10), UI.dp(c, 10), UI.dp(c, 10), UI.dp(c, 10))
            row.addView(UI.iconBox(c, tk.ic, T.tone(tk.tone), (0x26 shl 24) or (T.tone(tk.tone) and 0xFFFFFF), 36, 18, 12))
            val lbl = UI.text(c, tk.label, 12, T.text, true)
            val ll = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            ll.marginStart = UI.dp(c, 10)
            row.addView(lbl, ll)
            row.addView(UI.icon(c, R.drawable.ic_chev_l, 14, T.sub))
            row.setOnClickListener { go(tk.go) }
            tasks.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }
    root.addView(tasks)

    // ---------- نمودار فروش سال ----------
    val salesCard = UI.card(c)
    salesCard.addView(UI.sectionTitle(c, "روند فروش سال", "خالص فروش نهایی به تفکیک ماه", R.drawable.ic_chart, topMargin = 0))
    val monthsLbl = arrayOf("فرو", "ارد", "خرد", "تیر", "مرد", "شهر", "مهر", "آبا", "آذر", "دی", "بهم", "اسف")
    val salesData = sales.take(currentMonth).map { Triple(monthsLbl[it.month - 1], it.total, it.month == currentMonth) }
    salesCard.addView(MiniBarsView(c, salesData, 120), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
    root.addView(salesCard)

    // ---------- جریان نقدی ----------
    val cfCard = UI.card(c)
    cfCard.addView(UI.sectionTitle(c, "جریان نقدی ۶ ماه", "درآمد در برابر هزینه — بر پایهٔ اسناد واقعی", R.drawable.ic_banknote, topMargin = 0))
    cfCard.addView(DualBarsView(c, cf.map { Triple(monthsLbl[it.month - 1], it.income, it.expense) }, 150), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
    root.addView(cfCard)

    // ---------- سنین مطالبات ----------
    val agingCard = UI.card(c)
    agingCard.addView(UI.sectionTitle(c, "سنین مطالبات", "هرچه قدیمی‌تر، وصول سخت‌تر — اول این‌ها را پیگیری کنید", R.drawable.ic_clock, topMargin = 0))
    val maxAging = Math.max(aging.maxOfOrNull { it.amount } ?: 1L, 1L)
    for (b in aging) {
        val bar = agingBar(c, b.label, b.amount, if (b.tone == "slate") "rose" else b.tone, Math.max(2, ((b.amount * 100) / maxAging).toInt()))
        val bl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        bl.topMargin = UI.dp(c, 10)
        agingCard.addView(bar, bl)
    }
    root.addView(agingCard)

    // ---------- یادآور و سررسید ----------
    val remCard = UI.card(c)
    val manageBtn = UI.text(c, "مدیریت", 11, T.primary, true)
    manageBtn.background = UI.rounded(c, T.primarySoft, 12)
    manageBtn.setPadding(UI.dp(c, 10), UI.dp(c, 6), UI.dp(c, 10), UI.dp(c, 6))
    manageBtn.setOnClickListener { go("tools") }
    remCard.addView(UI.sectionTitle(c, "یادآور و سررسید", J.faN(undoneReminders.size) + " یادآور و " + J.faN(nearChecks.size) + " چک در ۳۰ روز آینده", R.drawable.ic_bell, manageBtn, topMargin = 0))
    if (undoneReminders.isEmpty() && nearChecks.isEmpty()) {
        remCard.addView(UI.empty(c, "یادآوری ندارید", "همه چیز مرتب است", R.drawable.ic_bell))
    } else {
        for (r in undoneReminders.take(2)) {
            val row = UI.innerCard(c, T.cardSoft, 16)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.addView(UI.icon(c, R.drawable.ic_clip, 18, T.primary))
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            cl.marginStart = UI.dp(c, 10)
            col.addView(UI.text(c, r.title, 12, T.text, true))
            col.addView(UI.text(c, J.jFa(r.date), 10, T.sub))
            row.addView(col, cl)
            if (J.jDiff(r.date, today) <= 0) row.addView(UI.chip(c, "امروز", "rose"))
            remCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
        for (chk in nearChecks.take(2)) {
            val p = db.partners.firstOrNull { it.id == chk.partnerId }
            val row = UI.innerCard(c, T.amberSoft, 16)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.addView(UI.icon(c, R.drawable.ic_landmark, 18, T.amber))
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            cl.marginStart = UI.dp(c, 10)
            col.addView(UI.text(c, "چک " + (if (chk.kind == "received") "دریافتی" else "پرداختی") + " — " + (p?.name ?: ""), 12, T.text, true))
            col.addView(UI.text(c, "سررسید " + J.jFa(chk.dueDate) + " • " + J.moneyFa(chk.amount) + " تومان", 10, T.sub))
            row.addView(col, cl)
            row.addView(UI.chip(c, J.faN(J.jDiff(chk.dueDate, today)) + " روز", "amber"))
            remCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }
    root.addView(remCard)

    // ---------- آخرین اسناد ----------
    val recent = UI.card(c)
    recent.addView(UI.sectionTitle(c, "آخرین اسناد", "جدیدترین فاکتورهای ثبت‌شده", R.drawable.ic_doc, topMargin = 0))
    if (recentInvoices.isEmpty()) {
        recent.addView(UI.empty(c, "هنوز سندی ثبت نشده", "", R.drawable.ic_receipt))
    } else {
        val list = LinearLayout(c)
        list.orientation = LinearLayout.VERTICAL
        recent.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        for (inv in recentInvoices) {
            val p = db.partners.firstOrNull { it.id == inv.partnerId }
            val badge = UI.chip(c, if (inv.final) "نهایی" else "پیش‌نویس", if (inv.final) "green" else "amber")
            val row = UI.listRow(c,
                LB.invoiceKindShort(inv.kind) + " شمارهٔ " + J.faN(inv.no),
                (p?.name ?: "—") + " • " + J.jFa(inv.date),
                UI.endCol(c, J.moneyFa(Db.invoiceTotals(inv).total), "تومان"),
                badge, icRes = R.drawable.ic_receipt, click = { go("invoices") })
            val rl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            rl.topMargin = UI.dp(c, 8)
            list.addView(row, rl)
        }
    }
    root.addView(recent)

    // ---------- توضیح اسناد سیستمی ----------
    val note = UI.innerCard(c, T.cardSoft, 18, 13)
    note.orientation = LinearLayout.HORIZONTAL
    note.setPadding(UI.dp(c, 14), UI.dp(c, 13), UI.dp(c, 14), UI.dp(c, 13))
    note.addView(UI.iconBox(c, R.drawable.ic_swap, 0xFF0D9488.toInt(), 0x1F0D9488.toInt(), 40, 20))
    val nt = UI.text(c, "در شمارا، هر فاکتور و چکی که ثبت می‌کنید به‌صورت خودکار سند حسابداری می‌سازد؛ دفاتر و تراز همیشه با آخرین تغییرات هم‌گام است.", 11.5f, T.sub)
    nt.setLineSpacing(0f, 1.2f)
    val nl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    nl.marginStart = UI.dp(c, 12)
    note.addView(nt, nl)
    val notel = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    notel.topMargin = UI.dp(c, 12)
    root.addView(note, notel)

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

/* ═════════════════════════ صفحهٔ بیشتر ═════════════════════════ */

fun moreScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val go = { s: String -> act.go(s) }
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    // بنر خلاصه
    val hero = LinearLayout(c)
    hero.orientation = LinearLayout.VERTICAL
    hero.background = UI.gradient(c, 0xFF059669.toInt(), 0xFF065F46.toInt(), 22)
    hero.setPadding(UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18))
    hero.addView(UI.text(c, "همهٔ ماژول‌ها", 17, Color.WHITE, true))
    val hsub = UI.text(c, J.faN(db.items.size) + " کالا • " + J.faN(db.partners.size) + " طرف‌حساب • " + J.faN(db.invoices.size) + " فاکتور • " + J.faN(db.checks.size) + " چک", 11, 0xCCECFDF5.toInt())
    hero.addView(hsub, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
    val quick = LinearLayout(c)
    quick.orientation = LinearLayout.HORIZONTAL
    quick.gravity = Gravity.CENTER_VERTICAL
    quick.background = UI.rounded(c, 0x26FFFFFF.toInt(), 14)
    quick.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))
    quick.addView(UI.icon(c, R.drawable.ic_sparkles, 16, Color.WHITE))
    quick.addView(UI.text(c, "دسترسی سریع", 12, Color.WHITE, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = UI.dp(c, 8) })
    quick.setOnClickListener { quickSheet(act) }
    hero.addView(quick, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
    root.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

    // ⭐ همهٔ ماژول‌ها در همین صفحه — کاشی هم‌قد ۳ستونه (۱۳ ماژول + میکروفن = ۱۴ کاشی)
    // ارتفاع ثابت کاشی‌ها ⇒ تراز کامل افقی/عمودی و بدون به‌هم‌ریختگی روی هر دستگاهی
    root.addView(UI.sectionTitle(c, "دسترسی به همهٔ ماژول‌ها", "همهٔ بخش‌های حسابداری حرفه‌ای + دستیار صوتی — همه در همین صفحه", R.drawable.ic_grid))
    val tileGrid = LinearLayout(c)
    tileGrid.orientation = LinearLayout.VERTICAL
    val tiles = MODULES + ModuleDef("voice", "دستیار صوتی", R.drawable.ic_mic, 0xFF7C3AED.toInt(), 0xFF4F46E5.toInt(), "با صدا همه‌کار می‌کند", "دستیار صوتی")
    for (chunk in tiles.chunked(3)) {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        for (i in 0 until 3) {
            if (i > 0) row.addView(View(c), LinearLayout.LayoutParams(UI.dp(c, 8), UI.dp(c, 1)))
            if (i < chunk.size) {
                val m = chunk[i]
                val tile = moduleTile(c, m) { if (m.id == "voice") act.openVoice() else go(m.id) }
                tile.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                row.addView(tile)
            } else {
                row.addView(View(c), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
        }
        val rl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rl.topMargin = UI.dp(c, 8)
        tileGrid.addView(row, rl)
    }
    root.addView(tileGrid)

    // تنظیمات و ابزارهای سریع — همهٔ دسترسی‌های عمومی یکجا
    val actCard = UI.card(c)
    actCard.addView(UI.sectionTitle(c, "تنظیمات و ابزارها", "هرچه برای مدیریت اپ لازم دارید", R.drawable.ic_gear, topMargin = 0))
    val acts = listOf(
        Quadruple(if (T.dark) R.drawable.ic_sun else R.drawable.ic_moon, "پوستهٔ روشن / تیره", "تغییر ظاهر برنامه", "theme"),
        Quadruple(R.drawable.ic_backup, "پشتیبان‌گیری", "کل داده‌ها در یک فایل JSON", "backup"),
        Quadruple(R.drawable.ic_upload, "بازیابی از فایل", "برگرداندن پشتیبان", "restore"),
        Quadruple(R.drawable.ic_alert, "گزارش خطاها", "اگر جایی چیزی درست کار نکرد", "errors"),
        Quadruple(R.drawable.ic_share, "اشتراک‌گذاری برنامه", "معرفی شمارا به دیگران", "share"),
        Quadruple(R.drawable.ic_book, "راهنمای شمارا", "قابلیت‌ها، قیمت و آموزش", "guide")
    )
    for (a in acts) {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(UI.dp(c, 6), UI.dp(c, 10), UI.dp(c, 6), UI.dp(c, 10))
        row.addView(UI.iconBox(c, a.first, T.sub, T.cardSoft, 40, 20))
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        cl.marginStart = UI.dp(c, 12)
        col.addView(UI.text(c, a.second, 13, T.text, true))
        col.addView(UI.text(c, a.third, 10, T.sub))
        row.addView(col, cl)
        row.addView(UI.icon(c, R.drawable.ic_chev_l, 14, T.sub))
        row.setOnClickListener {
            when (a.fourth) {
                "theme" -> act.toggleTheme()
                "backup" -> act.backupJson()
                "restore" -> act.restoreJson()
                "errors" -> crashLogSheet(act)
                "share" -> UI.shareText(c, "شمارا — مجموعهٔ کامل مدیریت کسب‌وکار و حسابداری حرفه‌ای")
                "guide" -> go("guide")
            }
        }
        actCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    root.addView(actCard)

    // درباره
    val about = UI.sectionTitle(c, "دربارهٔ شمارا ۵", "نسخهٔ " + ErrorReport.version(c), R.drawable.ic_sparkles)
    root.addView(about)
    val aboutT = UI.text(c, "شمارا ۵ همهٔ ماژول‌های حسابداری حرفه‌ای را دارد و حالا بهترین قابلیت‌های اپ‌های حسابداری دنیا هم اضافه شده است: تطبیق بانک مثل Xero، دستیار هوشمند مثل Zia در Zoho Books، ثبت زمان و فاکتور پروژه مثل FreshBooks، فاکتور تکرارشونده، یادآوری، سنین مطالبات و جریان نقدی مثل QuickBooks — همه آفلاین و روی حافظهٔ خودتان.", 11.5f, T.sub)
    aboutT.setLineSpacing(0f, 1.3f)
    root.addView(aboutT, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}
