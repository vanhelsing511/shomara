package ir.shomara.app

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * هوشیار — دستیار هوشمند مالی (الهام از Ask Zia در Zoho Books و Intuit Assist)
 * + تطبیق بانک (Xero) + پروژه و زمان (FreshBooks)
 */

/* ═════════════════════════ هوشیار ═════════════════════════ */

data class ChatMsg(val id: String, val role: String, val text: String, val actionLabel: String = "", val actionGo: String = "")

val SUGGESTIONS = listOf(
    "فروش این ماه چقدر بود؟", "سود دوره چقدر است؟", "برو روزنامه", "فاکتور فروش بزن",
    "کالا جدید بساز", "مطالبات من چقدر است؟", "موجودی کالاهای رو به اتمام", "بهترین مشتری من کیه؟",
    "چک‌های نزدیک سررسید", "مالیات دوره چقدر است؟", "برو انبار", "راهنما — چه کاری بلدی؟",
    "وقتی گفتم صندوق یعنی برو صندوق"
)

fun assistantAnswer(q: String): ChatMsg {
    fun has(vararg ws: String): Boolean = ws.any { q.contains(it) }
    fun mk(text: String, label: String = "", go: String = "") = ChatMsg(Db.newId("m"), "bot", text, label, go)
    val db = Db.db
    val today = J.jTodayStr()

    // قیمت یا موجودی کالا
    val itemQ = has("قیمت", "موجودی") && !has("کل", "مجموع")
    if (itemQ) {
        val namePart = q.replace("قیمت", "").replace("موجودی", "").replace("چقدر", "").replace("چنده", "")
            .replace("است", "").replace("داره", "").replace("کالا", "").replace("برای", "")
            .replace("؟", "").replace("?", "").trim()
        val item = db.items.firstOrNull { namePart.length > 1 && it.name.contains(namePart) }
            ?: db.items.firstOrNull { q.split(" ").any { w -> w.length > 2 && it.name.contains(w) } }
        if (item != null) {
            if (has("موجودی")) {
                return mk("قیمت فروش فعلی «" + item.name + "»: " + J.moneyFa(item.price) + " تومان — بهای تمام‌شده: " + J.moneyFa(item.cost) + " تومان. حاشیهٔ سود هر واحد: " + J.moneyFa(item.price - item.cost) + " تومان.", "رفتن به انبار", "warehouse")
            }
            return mk("«" + item.name + "» فعلاً " + J.moneyFa(item.price) + " تومان قیمت دارد. اگر " + J.faN(5) + " واحد بفروشید، " + J.moneyShort((item.price - item.cost) * 5) + " تومان سود نابی می‌ماند.", "صدور فاکتور فروش", "invoices")
        }
    }

    if (has("فروش", "درآمد")) {
        val sales = Db.monthlySales(db)
        val m = J.jMonthOf(today)
        val cur = sales.getOrNull(m - 1)?.total ?: 0L
        val prev = if (m > 1) sales.getOrNull(m - 2)?.total ?: 0L else 0L
        val diff = if (prev > 0) Math.round(((cur - prev).toDouble() / prev) * 100).toInt() else 0
        val trend = if (prev == 0L) "" else if (diff >= 0) " (" + J.faN(diff) + "٪ رشد نسبت به ماه قبل ↗)" else " (" + J.faN(Math.abs(diff)) + "٪ افت نسبت به ماه قبل ↘)"
        val todaySum = db.invoices.filter { it.kind == K_SALE && it.final && it.date == today }.sumOf { Db.invoiceTotals(it).total }
        return mk("فروش نهایی این ماه " + J.moneyFa(cur) + " تومان است" + trend + ". امروز هم " + J.moneyShort(todaySum) + " تومان فاکتور ثبت کرده‌اید.", "رفتن به فروش", "invoices")
    }

    if (has("سود")) {
        val pl = Db.profitLoss(db)
        val margin = if (pl.income > 0) Math.round((pl.net.toDouble() / pl.income) * 100).toInt() else 0
        return mk("سود خالص دوره تا امروز " + J.moneyFa(pl.net) + " تومان است — درآمد " + J.moneyShort(pl.income) + " در برابر بهای تمام‌شده و هزینه‌ها " + J.moneyShort(pl.income - pl.net) + ". حاشیهٔ سود: " + J.faN(margin) + "٪.", "گزارش سود و زیان", "reports")
    }

    if (has("طلب", "بدهی مشتری", "بدهکار", "مطالبات")) {
        val ar = Db.totalReceivable(db)
        val od = Db.overdueSales(db)
        return mk("مطالبات کل شما " + J.moneyFa(ar) + " تومان است که " + J.moneyShort(od.sumOf { it.amount }) + " تومان آن از سررسید گذشته" + (if (od.isNotEmpty()) " (" + J.faN(od.size) + " فاکتور)" else "") + ". پیشنهاد: برای فاکتورهای معوق پیامک یادآوری بفرستید.", "فاکتورهای فروش", "invoices")
    }

    if (has("بدهی من", "پرداختنی", "تامین")) {
        val ap = Db.totalPayable(db)
        return mk("بدهی شما به تامین‌کننده‌ها " + J.moneyFa(ap) + " تومان است. با توجه به خزانهٔ " + J.moneyShort(Db.totalCash(db)) + " تومانی، توان تسویهٔ فوری دارید.", "دریافت و پرداخت", "cash")
    }

    if (has("موجودی", "اتمام", "کم شد")) {
        val lows = Db.lowStockItems(db)
        return if (lows.isEmpty()) mk("همهٔ کالاها بالای نقطهٔ سفارش هستند. انبار سالم است")
        else mk(J.faN(lows.size) + " کالا به نقطهٔ سفارش رسیده: " + lows.take(4).joinToString("، ") { it.name } + ". سفارش خرید بدهید تا فروش از دست نرود.", "رفتن به انبار", "warehouse")
    }

    if (has("بهترین", "مشتری", "بیشترین")) {
        val top = Db.salesByPartner(db).firstOrNull()
        return if (top != null) mk("بهترین مشتری شما «" + top.partner.name + "» است با " + J.moneyShort(top.total) + " تومان خرید در " + J.faN(top.count) + " فاکتور. برای وفاداری‌اش تخفیف ویژهٔ پیشنهاد کنید!")
        else mk("هنوز فروشی ثبت نشده تا مشتری برتر را انتخاب کنم.")
    }

    if (has("چک")) {
        val near = db.checks
            .filter { (it.kind == "received" && (it.status == "drawer" || it.status == "inBank")) || (it.kind == "issued" && it.status == "drawer") }
            .filter { J.jDiff(it.dueDate, today) in 0..30 }
            .sortedBy { it.dueDate }
        return if (near.isEmpty()) mk("در ۳۰ روز آینده چکی سررسید نمی‌شود. خیال شما راحت است.")
        else mk(J.faN(near.size) + " چک تا ۳۰ روز آینده سررسید می‌شود؛ نزدیک‌ترین: " + J.moneyShort(near[0].amount) + " تومان در تاریخ " + J.jFa(near[0].dueDate) + ". مجموع: " + J.moneyShort(near.sumOf { it.amount }) + " تومان.", "چک و خزانه", "checks")
    }

    if (has("نقد", "جریان")) {
        val cf = Db.cashflow(db, 6)
        val cur = cf.lastOrNull() ?: CFRow(J.jMonthOf(today), 0, 0)
        val net = cur.income - cur.expense
        return mk("جریان نقدی این ماه: درآمد " + J.moneyShort(cur.income) + " در برابر هزینهٔ " + J.moneyShort(cur.expense) + " — خالص " + (if (net >= 0) "+" else "") + J.moneyShort(net) + " تومان. " + (if (net >= 0) "روند مثبت است." else "مراقب خروج نقد باشید."), "گزارش‌ها", "reports")
    }

    // سلام و احوالپرسی
    if (has("سلام", "درود", "خوبی", "هستی کی")) {
        return mk("سلام! من هوشیار هستم، حسابدار شخصی شما. از روی داده‌های واقعی خودتان تحلیل می‌دهم و می‌توانم مستقیم به بخش مربوطه ببرم. مثلا بپرسید «سود این دوره چقدر است؟» یا «چک‌های نزدیک سررسید را نشانم بده».")
    }

    // راهنما — کاربر نمی‌داند چه بپرسد
    if (has("راهنما", "کمک", "چه کاری", "چه سوال", "بلدی")) {
        val a = Db.smartAlerts(db)
        return mk("کارهایی که الان برایتان می‌توانم انجام بدهم:\n• فروش و سود این ماه\n• مطالبات و بدهی‌ها\n• موجودی و کالاهای رو به اتمام\n• چک‌های نزدیک سررسید\n• جریان نقدی ۶ ماه\n• متن پیامک یادآوری برای فاکتورهای معوق\n" + (if (a.isNotEmpty()) "فوری‌ترین کار امروز: " + a[0].title else "الان همه‌چیز مرتب است"))
    }

    // پروژه و ساعت کاری
    if (has("پروژه", "ساعت کار", "زمان")) {
        val pr = db.projects.filter { !it.done }
        val unbilled = db.timeEntries.filter { it.billed.isEmpty() }
        val v = unbilled.sumOf { (it.minutes / 60.0) * it.rate }
        return if (pr.isEmpty() && unbilled.isEmpty()) mk("فعلاً پروژه یا ساعت کاری ثبت‌شده‌ای ندارید. از ماژول «پروژه و زمان» اولین پروژه را بسازید.", "پروژه و زمان", "projects")
        else mk(J.faN(pr.size) + " پروژهٔ فعال دارید" + (if (unbilled.isNotEmpty()) " و " + J.faN(Math.round(unbilled.sumOf { it.minutes } / 60.0)) + " ساعت کار فاکتورنشده به ارزش " + J.moneyShort(v) + " تومان دارید — با یک لمس فاکتورش کنید!" else " و همهٔ ساعت‌ها فاکتور شده‌اند. آفرین!"), "پروژه و زمان", "projects")
    }

    // مالیات
    if (has("مالیات", "ارزش افزوده", "مودیان")) {
        val vatS = db.invoices.filter { it.kind == K_SALE && it.final }.sumOf { Db.invoiceTotals(it).vat }
        val vatB = db.invoices.filter { it.kind == K_PURCHASE && it.final }.sumOf { Db.invoiceTotals(it).vat }
        return mk("خلاصهٔ مالیاتی دوره: مالیات فروش " + J.moneyShort(vatS) + " تومان (بستانکار) در برابر اعتبار مالیاتی خرید " + J.moneyShort(vatB) + " تومان — مالیات پرداختنی: " + J.moneyShort(Math.max(0L, vatS - vatB)) + " تومان. نرخ پیش‌فرض " + J.faN(db.settings.vatRate) + "٪ است" + (if (db.settings.moudian) " و ارسال به سامانهٔ مودیان فعال است." else "."), "گزارش مالیات", "reports")
    }

    // انبار و ارزش موجودی
    if (has("انبار", "ارزش موجودی", "ارزش کل")) {
        val wv = db.items.sumOf { Db.stockOf(db, it.id) * it.cost }
        val lows = Db.lowStockItems(db)
        return mk("ارزش کل موجودی انبار " + J.moneyShort(wv) + " تومان (به بهای تمام‌شده) است در " + J.faN(db.items.count { !it.service }) + " قلم کالا" + (if (lows.isNotEmpty()) "؛ " + J.faN(lows.size) + " قلم هم زیر حد هشدار است — سفارش خرید را فراموش نکنید." else ". انبار سالم است."), "رفتن به انبار", "warehouse")
    }

    if (has("یادآور", "پیامک")) {
        val od = Db.overdueSales(db).firstOrNull()
        return if (od != null) mk("متن آمادهٔ یادآوری برای «" + od.partner.name + "»:\n\n«" + od.partner.name + " عزیز، فاکتور شمارهٔ " + J.faN(od.inv.no) + " به مبلغ " + J.moneyFa(od.amount) + " تومان از " + J.faN(od.days) + " روز پیش در انتظار تسویه است. ممنون از همکاری‌تان»\n\nاز بخش فاکتورها می‌توانید برای هر فاکتور معوق همین متن را کپی کنید.", "فاکتورهای معوق", "invoices")
        else mk("فعلاً فاکتور معوقی ندارید که یادآوری بخورد!")
    }

    if (has("سررسید", "قدیم", "معوق")) {
        val b = Db.agingBuckets(db)
        return mk("سنین مطالبات: ۰-۳۰ روز " + J.moneyShort(b[0].amount) + " • ۳۱-۶۰ " + J.moneyShort(b[1].amount) + " • ۶۱-۹۰ " + J.moneyShort(b[2].amount) + " • +۹۰ " + J.moneyShort(b[3].amount) + " تومان. هرچه به سمت سنین بالاتر بروید، احتمال وصول کمتر است — اول سراغ سنین بالا بروید.", "گزارش‌ها", "reports")
    }

    return mk("می‌توانم دربارهٔ این‌ها گزارش بدهم: فروش، سود، مطالبات، بدهی، موجودی، چک‌ها، جریان نقدی، بهترین مشتری یا متن پیامک یادآوری. یکی از پیشنهادهای زیر را بزنید.")
}

fun assistantScreen(act: MainActivity): View {
    fun safeAnswer(q: String): ChatMsg = try {
        assistantAnswer(q)
    } catch (e: Throwable) {
        // مهار کرش پاسخ هوشمند — کاربر پیام خطای شفاف می‌بیند، اپ باز می‌ماند
        ErrorReport.report(act, "assistant:" + q, e)
        ChatMsg(Db.newId("m"), "bot", "تحلیل این سوال با خطا مواجه شد — لطفا دوباره بپرسید یا از پیشنهادها استفاده کنید.")
    }

    val c = act
    val db = Db.db
    val alerts = Db.smartAlerts(db)
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 16))

    val hero = LinearLayout(c)
    hero.orientation = LinearLayout.VERTICAL
    hero.background = UI.gradient(c, 0xFF7C3AED.toInt(), 0xFF4F46E5.toInt(), 22)
    hero.setPadding(UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18))
    hero.addView(UI.text(c, "هوشیار — دستیار مالی هوشمند", 17, Color.WHITE, true))
    val sub = UI.text(c, "مثل Zia در Zoho Books: از روی داده‌های خودتان سوال بپرسید، پاسخ دقیق بگیرید و مستقیم عمل کنید.", 11, 0xD9E7FF.toInt())
    sub.setLineSpacing(0f, 1.2f)
    hero.addView(sub, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })

    // دکمهٔ بزرگ صحبت — دستیار صوتی، مثل Siri/Copilot: بگو و انجام شود
    val speak = UI.text(c, "صحبت کن — دستور صوتی بدهید", 13.5f, Color.WHITE, true)
    speak.background = UI.rounded(c, 0x40FFFFFF, 18)
    speak.setPadding(UI.dp(c, 16), UI.dp(c, 11), UI.dp(c, 16), UI.dp(c, 12))
    speak.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic, 0, 0, 0)
    try { speak.compoundDrawables[0]?.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN) } catch (ig: Exception) {}
    speak.compoundDrawablePadding = UI.dp(c, 8)
    speak.setOnClickListener { act.openVoice() }
    val slp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    slp.topMargin = UI.dp(c, 12)
    hero.addView(speak, slp)
    val hint = UI.text(c, "مثلا بگویید: برو روزنامه — فاکتور فروش دو تا شیر برای علی رضایی", 10.5f, 0xC9D9E7FF.toInt())
    hint.gravity = Gravity.CENTER
    hero.addView(hint, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 7) })
    root.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

    // هشدارهای هوشمند
    root.addView(UI.sectionTitle(c, "هشدارهای هوشمند", "تشخیص ناهنجاری از روی داده‌های واقعی", R.drawable.ic_alert))
    if (alerts.isEmpty()) {
        val ok = UI.card(c)
        ok.orientation = LinearLayout.HORIZONTAL
        ok.gravity = Gravity.CENTER_VERTICAL
        ok.addView(UI.iconBox(c, R.drawable.ic_sparkles, T.primary, T.primarySoft, 40, 20))
        ok.addView(UI.text(c, "همه‌چیز مرتب است — هشدار فعالی ندارید", 12.5f, T.primary, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(c, 12) })
        root.addView(ok)
    } else {
        for (a in alerts) {
            val row = UI.listRow(c, a.title, a.desc, icRes = alertIcon(a.icon), icTint = T.tone(a.tone), click = { act.go(a.go) })
            root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }

    // گفتگو
    val chatTitle = UI.sectionTitle(c, "بپرس از هوشیار", "پاسخ از روی اسناد واقعی خودتان", R.drawable.ic_bot)
    if (act.chatMsgs.isNotEmpty()) {
        val trow = LinearLayout(c)
        trow.orientation = LinearLayout.HORIZONTAL
        trow.gravity = Gravity.CENTER_VERTICAL
        val copy = UI.text(c, "کپی پاسخ", 11, T.sub, true)
        copy.background = UI.rounded(c, T.cardSoft, 12)
        copy.setPadding(UI.dp(c, 10), UI.dp(c, 6), UI.dp(c, 10), UI.dp(c, 6))
        copy.setOnClickListener {
            val last = act.chatMsgs.lastOrNull { it.role == "bot" }
            if (last != null) UI.copyText(c, last.text)
        }
        trow.addView(copy)
        val clear = UI.text(c, "پاک‌کردن گفتگو", 11, T.rose, true)
        clear.background = UI.rounded(c, T.roseSoft, 12)
        clear.setPadding(UI.dp(c, 10), UI.dp(c, 6), UI.dp(c, 10), UI.dp(c, 6))
        val clp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        clp.marginStart = UI.dp(c, 6)
        trow.addView(clear, clp)
        clear.setOnClickListener {
            act.chatMsgs = listOf<ChatMsg>()
            act.saveChat()
            UI.toast(c, "گفتگو پاک شد — شروع تازه!")
            act.refreshAssistant()
        }
        (chatTitle.layoutParams as? LinearLayout.LayoutParams)?.topMargin = UI.dp(c, 16)
        chatTitle.addView(trow)
    }
    root.addView(chatTitle)

    val chat = UI.card(c)
    if (act.chatMsgs.isEmpty()) {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(UI.iconBox(c, R.drawable.ic_bot, Color.WHITE, 0xFF7C3AED.toInt(), 36, 18, 12))
        val bubble = UI.innerCard(c, T.cardSoft, 14, 12)
        bubble.addView(UI.text(c, "سلام! من هوشیار هستم، حسابدار شخصی شما. مثلا بنویسید «فروش این ماه چقدر بود؟» یا از دکمه‌های پیشنهادی استفاده کنید.", 12, T.text))
        bubble.setPadding(UI.dp(c, 12), UI.dp(c, 10), UI.dp(c, 12), UI.dp(c, 10))
        val bl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        bl.marginStart = UI.dp(c, 10)
        row.addView(bubble, bl)
        chat.addView(row)
    } else {
        for (m in act.chatMsgs) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            if (m.role == "user") row.gravity = Gravity.END
            if (m.role == "bot") {
                row.addView(UI.iconBox(c, R.drawable.ic_bot, Color.WHITE, 0xFF7C3AED.toInt(), 36, 18, 12))
            } else {
                row.addView(UI.iconBox(c, R.drawable.ic_person, Color.WHITE, 0xFF0D9488.toInt(), 36, 18, 12))
            }
            val bubble = LinearLayout(c)
            bubble.orientation = LinearLayout.VERTICAL
            bubble.background = if (m.role == "bot") UI.rounded(c, T.cardSoft, 14) else UI.rounded(c, 0x1A059669.toInt(), 14)
            bubble.setPadding(UI.dp(c, 12), UI.dp(c, 10), UI.dp(c, 12), UI.dp(c, 10))
            val t = UI.text(c, m.text, 12, if (m.role == "bot") T.text else T.primary)
            t.setLineSpacing(0f, 1.25f)
            bubble.addView(t, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            if (m.actionGo.isNotEmpty()) {
                val abtn = UI.text(c, m.actionLabel + " ←", 11, Color.WHITE, true)
                abtn.background = UI.gradient(c, T.primary, T.primaryD, 12)
                abtn.setPadding(UI.dp(c, 12), UI.dp(c, 7), UI.dp(c, 12), UI.dp(c, 8))
                abtn.setOnClickListener { act.go(m.actionGo) }
                bubble.addView(abtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
            }
            val bl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            bl.marginStart = UI.dp(c, 10)
            bl.weight = 0f
            row.addView(bubble, bl)
            chat.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        }
    }
    root.addView(chat, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })

    // پیشنهادها
    val sug = HorizontalScroll(c)
    for (s in SUGGESTIONS) {
        val chip = UI.text(c, s, 11.5f, T.violet, true)
        chip.background = UI.stroke(c, T.violetSoft, 16, 0x447C3AED.toInt(), 1)
        chip.setPadding(UI.dp(c, 12), UI.dp(c, 8), UI.dp(c, 12), UI.dp(c, 9))
        chip.setOnClickListener {
            // همهٔ پیشنهادها با موتور یکسان اجرا می‌شوند — حتی جملهٔ آموزشی
            val intent = VoiceBrain.parseVoice(s)
            val bot = try {
                act.voiceExec(intent)
            } catch (err: Throwable) {
                ErrorReport.report(act, "suggestion:" + s, err)
                ChatMsg(Db.newId("m"), "bot", "اجرای این مورد با خطا مواجه شد — دوباره تلاش کنید.")
            }
            act.chatMsgs = act.chatMsgs + ChatMsg(Db.newId("m"), "user", s) + bot
            act.saveChat()
            act.refreshAssistant()
        }
        sug.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = UI.dp(c, 8) })
    }
    root.addView(sug, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 44)).also { it.topMargin = UI.dp(c, 12) })

    // آموخته‌های هوشیار — جمله‌های خود کاربر که یاد گرفته است (تطبیق با لحن کاربر)
    val learned = VoiceLearn.all()
    if (learned.isNotEmpty()) {
        root.addView(UI.sectionTitle(c, "آموخته‌های من", "جمله‌های شما که یاد گرفته‌ام — لمس: اجرا • نگه‌داشتن: فراموشی", R.drawable.ic_zap))
        val lrow = HorizontalScroll(c)
        for (e0 in learned.sortedByDescending { it.hits }.take(12)) {
            val chip = UI.text(c, e0.phrase, 11, T.primary, true)
            chip.background = UI.stroke(c, T.primarySoft, 16, 0x4410B981.toInt(), 1)
            chip.setPadding(UI.dp(c, 12), UI.dp(c, 8), UI.dp(c, 12), UI.dp(c, 9))
            chip.setOnClickListener {
                val bot = try {
                    act.voiceExec(VoiceLearn.expand(e0))
                } catch (err: Throwable) {
                    ErrorReport.report(act, "learned-run", err)
                    ChatMsg(Db.newId("m"), "bot", "اجرای این آموخته با خطا مواجه شد.")
                }
                act.chatMsgs = act.chatMsgs + ChatMsg(Db.newId("m"), "user", e0.phrase) + bot
                act.saveChat()
                act.refreshAssistant()
            }
            chip.setOnLongClickListener {
                confirmSheet(act, "فراموشی «" + e0.phrase + "»؟", "این جمله از حافظهٔ آموخته‌های هوشیار پاک می‌شود.", "فراموش کن") {
                    VoiceLearn.forget(e0.id)
                    UI.toast(act, "فراموش شد")
                    act.refreshAssistant()
                }
                true
            }
            lrow.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = UI.dp(c, 8) })
        }
        root.addView(lrow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 44)).also { it.topMargin = UI.dp(c, 8) })
    }

    // جمله‌های نفهمیده‌شده — کاربر می‌تواند با «وقتی گفتم … یعنی …» یاد بدهد
    val unk = VoiceLearn.unknownList()
    if (unk.isNotEmpty()) {
        val uCard = UI.innerCard(c, T.cardSoft, 16, 12)
        uCard.addView(UI.text(c, "جمله‌هایی که هنوز یاد نگرفته‌ام (" + J.faN(unk.size) + ")", 12, T.text, true))
        for (u in unk.take(3)) {
            uCard.addView(UI.text(c, "«" + u.first + "»" + (if (u.second > 1) " — " + J.faN(u.second) + " بار گفته‌اید" else ""), 11, T.sub),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 5) })
        }
        uCard.addView(UI.text(c, "برای یاد دادن بنویسید: وقتی گفتم " + unk[0].first + " یعنی برو روزنامه", 11, T.primary, true),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 7) })
        root.addView(uCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
    }

    // ورودی — با Enter هم ارسال می‌شود (مثل نسخهٔ وب)
    val inputRow = LinearLayout(c)
    inputRow.orientation = LinearLayout.HORIZONTAL
    inputRow.gravity = Gravity.CENTER_VERTICAL
    val (box, e) = UI.search(c, "سوال خود را بنویسید…")
    (box.layoutParams as? LinearLayout.LayoutParams)?.topMargin = 0
    inputRow.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    val send = UI.iconBox(c, R.drawable.ic_send, Color.WHITE, 0xFF7C3AED.toInt(), 44, 20, 14)
    fun doSend() {
        val txt = e.text.toString().trim()
        if (txt.isEmpty()) return
        // ورودی تایپی هم با همان موتور صوتی اجرا و یاد گرفته می‌شود
        val intent = VoiceBrain.parseVoice(txt)
        val bot = try {
            act.voiceExec(intent)
        } catch (err: Throwable) {
            ErrorReport.report(act, "assistant-send", err)
            ChatMsg(Db.newId("m"), "bot", "اجرای این دستور با خطا مواجه شد — دوباره تلاش کنید.")
        }
        if (intent.kind in setOf("nav", "item_new", "partner_new", "invoice_new", "payment_new", "reminder"))
            VoiceLearn.record(txt, intent)
        else if (intent.kind == "unknown" || intent.reply.startsWith(VoiceBrain.GENERIC_FALLBACK))
            VoiceLearn.noteUnknown(txt)
        act.chatMsgs = act.chatMsgs + ChatMsg(Db.newId("m"), "user", txt) + bot
        act.saveChat()
        act.refreshAssistant()
    }
    send.setOnClickListener { doSend() }
    e.setOnEditorActionListener { _, _, _ -> doSend(); true }
    val sl = LinearLayout.LayoutParams(UI.dp(c, 44), UI.dp(c, 44))
    sl.marginStart = UI.dp(c, 8)
    send.layoutParams = sl
    inputRow.addView(send)
    root.addView(inputRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })

    val sc = ScrollView(c)
    sc.isVerticalScrollBarEnabled = false
    sc.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    // اسکرول خودکار به آخرین پیام — مثل نسخهٔ وب
    sc.post { try { sc.fullScroll(View.FOCUS_DOWN) } catch (ig: Exception) {} }
    return sc
}

fun alertIcon(name: String): Int = when (name) {
    "alert" -> R.drawable.ic_alert
    "clock", "time" -> R.drawable.ic_clock
    "box" -> R.drawable.ic_boxes
    "bank" -> R.drawable.ic_bank
    "repeat" -> R.drawable.ic_repeat
    "trend" -> R.drawable.ic_trend_down
    else -> R.drawable.ic_sparkles
}

/**
 * اسکرول افقی ضدکرش — HorizontalScrollView فقط یک فرزند مستقیم می‌پذیرد.
 * این کلاس همهٔ فرزندان را خودکار داخل یک LinearLayout افقی واحد می‌گذارد،
 * پس حتی اگر کد فرزندان را مستقیم اضافه کند (مثل چیپ‌های هوشیار) هرگز
 * «IllegalStateException: can host only one direct child» رخ نمی‌دهد.
 */
class HorizontalScroll(c: Context) : android.widget.HorizontalScrollView(c) {
    private val ctx: Context = c
    private val row: LinearLayout = LinearLayout(c)
    private var ready = false

    init {
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false
        // تنها فرزند مستقیمِ اسکرول همان ردیف است — قبل از فعال‌شدن مسیرِ override
        super.addView(row, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        ready = true
    }

    // ⚠ ضدکرش کامل: همهٔ شکل‌های addView به ردیفِ داخلی هدایت می‌شوند تا هیچ‌وقت
    // «HorizontalScrollView can host only one direct child» رخ ندهد — روی هر رام و هر مسیری
    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (!ready || child == row) { super.addView(child, index, params); return }
        try { row.addView(child, index, params) } catch (e: Throwable) {
            ErrorReport.report(ctx, "HorizontalScroll:addView", e)
        }
    }

    override fun addView(child: View?) {
        if (!ready || child == row) { super.addView(child); return }
        try { row.addView(child) } catch (e: Throwable) {
            ErrorReport.report(ctx, "HorizontalScroll:addView", e)
        }
    }

    override fun addView(child: View?, index: Int) {
        if (!ready || child == row) { super.addView(child, index); return }
        try { row.addView(child, index) } catch (e: Throwable) {
            ErrorReport.report(ctx, "HorizontalScroll:addView", e)
        }
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (!ready || child == row) { super.addView(child, params); return }
        try { row.addView(child, params) } catch (e: Throwable) {
            ErrorReport.report(ctx, "HorizontalScroll:addView", e)
        }
    }

    override fun addView(child: View?, width: Int, height: Int) {
        if (!ready || child == row) { super.addView(child, width, height); return }
        try { row.addView(child, width, height) } catch (e: Throwable) {
            ErrorReport.report(ctx, "HorizontalScroll:addView", e)
        }
    }

    override fun removeAllViews() { row.removeAllViews() }
    override fun removeAllViewsInLayout() { row.removeAllViewsInLayout() }
    override fun removeView(view: View?) { row.removeView(view) }
    override fun removeViewAt(index: Int) { row.removeViewAt(index) }
    override fun removeViews(start: Int, count: Int) { row.removeViews(start, count) }
    override fun removeViewInLayout(view: View?) { row.removeViewInLayout(view) }
}

/* ═════════════════════════ تطبیق بانک ═════════════════════════ */

fun reconcileScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    val banks = db.banks.filter { it.kind != "cash" }
    val allBanks = banks + db.banks.filter { it.kind == "cash" }
    val bankId = if (allBanks.any { it.id == act.reconBank }) act.reconBank else allBanks.firstOrNull()?.id ?: ""

    val hero = android.widget.FrameLayout(c)
    hero.background = UI.gradient(c, 0xFF0284C7.toInt(), 0xFF4F46E5.toInt(), 22)
    val heroCol = LinearLayout(c)
    heroCol.orientation = LinearLayout.VERTICAL
    heroCol.setPadding(UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18))
    heroCol.addView(UI.text(c, "تطبیق بانک", 17, Color.WHITE, true))
    val sub = UI.text(c, "مثل Xero: صورت‌حساب بانک را وارد کنید و هر تراکنش را با یک لمس به فاکتور، دریافت یا هزینه تبدیل کنید.", 11, 0xD9E7FF.toInt())
    sub.setLineSpacing(0f, 1.2f)
    heroCol.addView(sub, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
    hero.addView(heroCol, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    // واترمارک Landmark — مثل نسخهٔ وب
    try {
        val wm = UI.icon(c, R.drawable.ic_landmark, 110, 0x26FFFFFF)
        val wml = android.widget.FrameLayout.LayoutParams(UI.dp(c, 110), UI.dp(c, 110), Gravity.CENTER or Gravity.END)
        wml.marginEnd = UI.dp(c, 12)
        hero.addView(wm, wml)
    } catch (ig: Exception) {}
    val txns = db.bankTxns.filter { it.bankId == bankId }.sortedByDescending { it.date }
    val unmatched = txns.filter { it.matched == null }
    val matched = txns.filter { it.matched != null }
    // آمار — با فاصلهٔ یکنواخت بین خانه‌ها (heroStats)
    val statsLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    statsLp.topMargin = UI.dp(c, 14)
    heroCol.addView(UI.heroStats(c, listOf(
        Pair(J.faN(unmatched.size), "تطبیق‌نشده"),
        Pair(J.faN(matched.size), "تطبیق‌شده"),
        Pair(J.moneyShort(Db.bankBalance(db, bankId)), "ماندهٔ دفاتر")
    ), 0xD9E7FF.toInt()), statsLp)
    root.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

    // انتخاب بانک
    root.addView(UI.segScroll(c, allBanks.map { Triple(it.name, -1, it.id) }, allBanks.indexOfFirst { it.id == bankId }) { i ->
        act.reconBank = allBanks[i].id
        act.refresh()
    }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })

    // جستجو + قوانین
    val sr = LinearLayout(c)
    sr.orientation = LinearLayout.HORIZONTAL
    val (sbox, se) = UI.search(c, "جستجو در توضیح تراکنش‌ها…")
    (sbox.layoutParams as? LinearLayout.LayoutParams)?.topMargin = 0
    sr.addView(sbox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    val rulesBtn = UI.text(c, "قوانین", 12, T.sky, true)
    rulesBtn.background = UI.stroke(c, T.skySoft, 16, 0x440284C7.toInt(), 1)
    rulesBtn.setPadding(UI.dp(c, 12), UI.dp(c, 10), UI.dp(c, 12), UI.dp(c, 10))
    rulesBtn.setOnClickListener { rulesSheet(act) }
    val rl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    rl.marginStart = UI.dp(c, 8)
    rulesBtn.layoutParams = rl
    sr.addView(rulesBtn)
    root.addView(sr, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })

    // بخش تراکنش‌ها — با جستجوی زنده
    val txnWrap = LinearLayout(c)
    root.addView(txnWrap)
    fun renderTxns() {
        val q = se.text.toString()
        txnWrap.removeAllViews()
        txnWrap.addView(UI.sectionTitle(c, "در انتظار تطبیق", "پیشنهادهای هوشمند را بررسی و تایید کنید", R.drawable.ic_sparkles))
        val filtered = unmatched.filter { q.isEmpty() || it.desc.contains(q) }
        if (filtered.isEmpty()) {
            txnWrap.addView(UI.empty(c, "همهٔ تراکنش‌ها تطبیق داده شده‌اند", "تراکنش جدید از دکمهٔ پایین اضافه کنید", R.drawable.ic_checkcheck), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        } else {
            for (t in filtered) {
            val card = UI.card(c)
            val head = LinearLayout(c)
            head.orientation = LinearLayout.HORIZONTAL
            head.gravity = Gravity.CENTER_VERTICAL
            head.addView(UI.iconBox(c, if (t.amount > 0) R.drawable.ic_arrow_dl else R.drawable.ic_arrow_ur, if (t.amount > 0) T.primary else T.rose, if (t.amount > 0) T.primarySoft else T.roseSoft, 40, 20))
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            cl.marginStart = UI.dp(c, 12)
            col.addView(UI.text(c, t.desc, 13, T.text, true))
            col.addView(UI.text(act, J.jFa(t.date), 10.5f, T.sub))
            head.addView(col, cl)
            head.addView(UI.text(c, (if (t.amount > 0) "+" else "−") + J.moneyFa(Math.abs(t.amount)), 13, if (t.amount > 0) T.primary else T.rose, true))
            val exp = UI.iconBox(c, R.drawable.ic_chev_d, T.sub, T.cardSoft, 36, 16, 12)
            exp.setOnClickListener {
                t.id.let { tid -> act.reconExpanded = if (act.reconExpanded == tid) "" else tid }
                act.refresh()
            }
            val el = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            el.marginStart = UI.dp(c, 6)
            exp.layoutParams = el
            head.addView(exp)
            card.addView(head)

            if (act.reconExpanded == t.id) {
                val sugg = LinearLayout(c)
                sugg.orientation = LinearLayout.VERTICAL
                for (s0 in reconcileSuggestions(db, t)) {
                    val srow = LinearLayout(c)
                    srow.orientation = LinearLayout.HORIZONTAL
                    srow.gravity = Gravity.CENTER_VERTICAL
                    srow.background = UI.stroke(c, if (s0.first == "invoice") 0x33F59E0B.toInt() else T.cardSoft, 16, 0x332563EB.toInt(), 1)
                    srow.setPadding(UI.dp(c, 12), UI.dp(c, 10), UI.dp(c, 12), UI.dp(c, 10))
                    srow.addView(UI.iconBox(c, if (s0.first == "rule") R.drawable.ic_zap else if (s0.first == "invoice") R.drawable.ic_receipt else R.drawable.ic_banknote, if (s0.first == "rule") T.sky else if (s0.first == "invoice") T.primary else T.sub, T.card, 36, 18, 12))
                    val scol = LinearLayout(c)
                    scol.orientation = LinearLayout.VERTICAL
                    val scl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    scl.marginStart = UI.dp(c, 10)
                    scol.addView(UI.text(c, s0.second, 12, T.text, true))
                    scol.addView(UI.text(c, s0.third, 10, T.sub))
                    srow.addView(scol, scl)
                    srow.addView(UI.icon(c, R.drawable.ic_checkcheck, 18, T.primary))
                    srow.setOnClickListener { reconcileConfirm(act, t, s0); return@setOnClickListener }
                    sugg.addView(srow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
                }
                card.addView(sugg, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
                card.addView(UI.btnRow(c,
                    UI.outlineBtn(c, "نادیده گرفتن") {
                        Db.update { d -> d.copy(bankTxns = d.bankTxns.map { if (it.id == t.id) it.copy(matched = BankMatch("skip", "دست‌نخورده — خارج از دفاتر")) else it }) }
                        act.refresh()
                        UI.toast(c, "تراکنش نادیده گرفته شد")
                    },
                    UI.outlineBtn(c, "حذف", danger = true, icRes = R.drawable.ic_trash) {
                        Db.update { d -> d.copy(bankTxns = d.bankTxns.filter { it.id != t.id }) }
                        act.refresh()
                        UI.toast(c, "تراکنش حذف شد")
                    }))
            }
            txnWrap.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }

    // تطبیق‌شده‌ها
    if (matched.isNotEmpty()) {
        txnWrap.addView(UI.sectionTitle(c, "تطبیق‌شده‌ها", "روی هر ردیف بزنید تا تطبیق بازگشت داده شود", R.drawable.ic_checkcheck))
        for (t in matched) {
            val row = UI.listRow(c, t.desc,
                J.jFa(t.date) + " • " + (t.matched?.label ?: ""),
                UI.endCol(c, (if (t.amount > 0) "+" else "−") + J.moneyShort(Math.abs(t.amount)), "", if (t.amount > 0) T.primary else T.rose, 13),
                null, icRes = R.drawable.ic_checkcheck, icTint = T.primary,
                click = {
                    Db.update { d -> d.copy(bankTxns = d.bankTxns.map { if (it.id == t.id) it.copy(matched = null) else it }) }
                    act.refresh()
                    UI.toast(c, "تطبیق بازگشت داده شد — تراکنش دوباره در لیست تطبیق است")
                })
            txnWrap.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }
    }
    se.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, cc: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, cc: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { renderTxns() }
    })
    renderTxns()

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

/** موتور پیشنهادهای تطبیق — مثل Match در Xero */
fun reconcileSuggestions(db: DB, t: BankTxn): List<Triple<String, String, String>> {
    val out = ArrayList<Triple<String, String, String>>()
    // ۱) قانون بانکی منطبق؟
    val rule = db.bankRules.firstOrNull { t.desc.contains(it.keyword) }
    if (rule != null) {
        out.add(Triple("rule", if (rule.target == "receipt") "ثبت دریافت (طبق قانون)" else "ثبت پرداخت/هزینه (طبق قانون)", "قانون «" + rule.keyword + "» — " + rule.note))
    }
    if (t.amount > 0) {
        // ۲) فاکتور فروش نهایی با ماندهٔ مساوی
        for (inv in db.invoices) {
            if (inv.kind != K_SALE || !inv.final) continue
            val due = Db.invoiceTotals(inv).due
            if (due > 0 && Math.abs(due - t.amount) < Math.max(1.0, due * 0.001)) {
                val p = db.partners.firstOrNull { it.id == inv.partnerId }
                out.add(Triple("invoice", "دریافت از " + (p?.name ?: "مشتری") + " — فاکتور " + J.faN(inv.no), "ماندهٔ دقیق فاکتور: " + J.moneyFa(due) + " تومان"))
                break
            }
        }
        out.add(Triple("quick", "ثبت دریافت از مشتری", J.moneyFa(t.amount) + " تومان واریز به " + (db.banks.firstOrNull { it.id == t.bankId }?.name ?: "")))
    } else if (t.amount < 0) {
        val abs = Math.abs(t.amount)
        val inv = db.invoices.firstOrNull { i ->
            i.kind == K_PURCHASE && i.final && Db.invoiceTotals(i).due > 0 && Math.abs(Db.invoiceTotals(i).due - abs) < Math.max(1.0, Db.invoiceTotals(i).due * 0.001)
        }
        val sup = inv?.let { db.partners.firstOrNull { x -> x.id == it.partnerId } }
        out.add(Triple(if (inv != null) "invoice" else "quick",
            if (inv != null) "پرداخت به " + (sup?.name ?: "تامین‌کننده") + " — فاکتور " + J.faN(inv.no) else "ثبت پرداخت/هزینه",
            if (inv != null) "ماندهٔ دقیق فاکتور: " + J.moneyFa(Db.invoiceTotals(inv).due) + " تومان" else J.moneyFa(abs) + " تومان برداشت از " + (db.banks.firstOrNull { it.id == t.bankId }?.name ?: "")))
    }
    return out
}

fun reconcileConfirm(act: MainActivity, t: BankTxn, s0: Triple<String, String, String>) {
    val db = Db.db
    val rule = db.bankRules.firstOrNull { t.desc.contains(it.keyword) }
    // واریز ⇒ دریافت، برداشت ⇒ پرداخت (مثل نسخهٔ وب)
    val kind: String = if (t.amount > 0) "receipt" else "payment"
    val amount = Math.abs(t.amount)
    val note: String = when {
        rule != null && s0.first == "rule" -> rule.note
        s0.first == "invoice" -> "تسویهٔ " + s0.second
        else -> t.desc.ifEmpty { s0.second }
    }
    val partnerId = if (s0.first == "invoice") {
        if (t.amount > 0) db.invoices.filter { it.kind == K_SALE && it.final }.firstOrNull { Math.abs(Db.invoiceTotals(it).due - amount) < Math.max(1.0, Db.invoiceTotals(it).due * 0.001) }?.partnerId ?: ""
        else db.invoices.filter { it.kind == K_PURCHASE && it.final }.firstOrNull { Math.abs(Db.invoiceTotals(it).due - amount) < Math.max(1.0, Db.invoiceTotals(it).due * 0.001) }?.partnerId ?: ""
    } else ""
    val payment = Payment(
        Db.newId("pay"), 400L + Db.nextNo("payment"), kind, "transfer", partnerId, t.bankId, amount, t.date, note
    )
    Db.update { d -> d.copy(
        payments = listOf(payment) + d.payments,
        counters = d.counters + ("payment" to (d.counters["payment"] ?: 0L) + 1L),
        bankTxns = d.bankTxns.map { if (it.id == t.id) it.copy(matched = BankMatch(kind, note)) else it }
    ) }
    act.refresh()
    UI.toast(act, (if (kind == "receipt") "دریافت ثبت و تراکنش تطبیق شد" else "پرداخت ثبت و تراکنش تطبیق شد") + " — " + J.moneyFa(amount) + " تومان، سند خودکار ساخته شد")
}

/** افزودن تراکنش بانکی */
fun bankTxnSheet(act: MainActivity) {
    val bs = BottomSheet(act)
    var date = J.jTodayStr()
    var amount = 0L
    // اگر بانکی انتخاب نشده، اولین حساب بانکی/کارتخوان — نه رشتهٔ خالی
    val db0 = Db.db
    val bankId0 = if (db0.banks.any { it.id == act.reconBank }) act.reconBank
                  else (db0.banks.firstOrNull { it.kind != "cash" } ?: db0.banks.firstOrNull())?.id ?: ""
    bs.show("تراکنش صورت‌حساب بانکی", "یک خط از صورت‌حساب بانک را وارد کنید تا در دفاتر تطبیق شود") { b ->
        val dLabel = ValueLabel()
        val dField = UI.dateField(act, "تاریخ", date) {
            dateSheet(act, date) { v -> date = v; dLabel.set(J.jFa(v)) }
        }
        dLabel.bind(dField, J.jFa(date))
        b.addView(dField)
        val amt = UI.numField(act, "مبلغ (تومان) — واریز مثبت، برداشت منفی", amount, hint = "مثلا ۱۵۵۰۰۰۰۰ یا −۴۲۰۰۰۰۰", allowNeg = true)
        hookNum(amt) { v -> amount = v }
        b.addView(amt)
        val desc = UI.field(act, "توضیح (طبق صورت‌حساب بانک)", "", hint = "مثلا واریز کارت به کارت آقای رضایی")
        var descV = ""
        hookText(desc.first) { v -> descV = v }
        b.addView(desc.first)
        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "افزودن به لیست تطبیق") {
                if (amount == 0L) { UI.toast(act, "مبلغ را وارد کنید"); return@primaryBtn }
                val saved = Db.update { d -> d.copy(bankTxns = listOf(BankTxn(Db.newId("bt"), bankId0, date, amount, descV.ifEmpty { "تراکنش بانکی" })) + d.bankTxns) }
                if (!saved) { UI.toast(act, "تراکنش ذخیره نشد؛ دوباره تلاش کنید"); return@primaryBtn }
                bs.close(); act.refresh()
                UI.toast(act, "تراکنش اضافه شد — حالا با یک لمس تطبیق دهید")
            }))
    }
}

/** قوانین دسته‌بندی بانک */
fun rulesSheet(act: MainActivity) {
    val bs = BottomSheet(act)
    val db = Db.db
    bs.show("قوانین دسته‌بندی بانک", "مثل Bank Rules در Xero — تراکنش‌های مشابه خودکار دسته‌بندی می‌شوند") { b ->
        for (r in db.bankRules) {
            val row = UI.listRow(act, "اگر توضیح شامل «" + r.keyword + "» بود",
                (if (r.target == "receipt") "پیشنهاد ثبت دریافت" else "پیشنهاد ثبت پرداخت/هزینه") + " • " + r.note,
                icRes = R.drawable.ic_zap, icTint = T.sky, chev = false)
            val del = UI.icon(act, R.drawable.ic_trash, 18, T.rose)
            del.setPadding(UI.dp(act, 8), 0, 0, 0)
            del.setOnClickListener {
                Db.update { d -> d.copy(bankRules = d.bankRules.filter { it.id != r.id }) }
                act.refresh()
                UI.toast(act, "قانون حذف شد")
                // بعد از پایان انیمیشن بسته‌شدن باز می‌کنیم — نه همزمان
                bs.close()
                android.os.Handler(act.mainLooper).postDelayed({ rulesSheet(act) }, 230)
            }
            row.addView(del)
            b.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
        }
        if (db.bankRules.isEmpty()) b.addView(UI.empty(act, "قانونی ندارید", "با کلمهٔ کلیدی، دسته‌بندی خودکار بسازید", R.drawable.ic_zap))

        val newCard = UI.innerCard(act, T.cardSoft, 16, 14)
        newCard.addView(UI.text(act, "قانون جدید", 12.5f, T.text, true))
        val kw = UI.field(act, "کلمهٔ کلیدی در توضیح تراکنش", "", hint = "مثلا اجاره")
        b.addView(newCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
        newCard.addView(kw.first, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
        val note = UI.field(act, "یادداشت دسته", "", hint = "مثلا هزینهٔ اجارهٔ ماهانه")
        var target = "payment"
        var rTarget = UI.makeSwitch(act, true) { v -> target = if (v) "payment" else "receipt" }
        val swRow = LinearLayout(act)
        swRow.orientation = LinearLayout.HORIZONTAL
        swRow.gravity = Gravity.CENTER_VERTICAL
        swRow.background = UI.rounded(act, T.card, 14)
        swRow.setPadding(UI.dp(act, 12), UI.dp(act, 10), UI.dp(act, 12), UI.dp(act, 10))
        val lbl1 = UI.text(act, "دریافت", 11, T.primary, true)
        swRow.addView(lbl1, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        swRow.addView(rTarget)
        val lbl2 = UI.text(act, "پرداخت", 11, T.rose, true)
        val l2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        l2.marginStart = UI.dp(act, 10)
        swRow.addView(lbl2, l2)
        newCard.addView(swRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
        newCard.addView(note.first, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
        val add = UI.primaryBtn(act, "ثبت قانون") {
            val keyword = findEditor(kw.first).text.toString().trim()
            if (keyword.isEmpty()) { UI.toast(act, "کلمهٔ کلیدی را وارد کنید"); return@primaryBtn }
            if (Db.db.bankRules.any { it.keyword.equals(keyword, true) && it.target == target }) { UI.toast(act, "قانون مشابه با همین کلمه و نوع قبلاً ثبت شده است"); return@primaryBtn }
            val noteV = findEditor(note.first).text.toString().trim()
            val saved = Db.update { d -> d.copy(bankRules = d.bankRules + BankRule(Db.newId("br"), keyword, target, noteV.ifEmpty { "دسته‌بندی خودکار" })) }
            if (!saved) { UI.toast(act, "قانون ذخیره نشد؛ دوباره تلاش کنید"); return@primaryBtn }
            bs.close(); act.refresh()
            UI.toast(act, "قانون بانکی ثبت شد — از این به بعد تراکنش‌های مشابه خودکار پیشنهاد می‌شوند")
        }
        newCard.addView(add, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
    }
}

/* ═════════════════════════ پروژه و زمان ═════════════════════════ */

val PROJ_COLORS = listOf("#0d9488", "#7c3aed", "#e11d48", "#ea580c", "#0284c7", "#16a34a")

fun projectsScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    val activeProjects = db.projects.filter { !it.done }
    val doneProjects = db.projects.filter { it.done }
    val totalUnbilled = db.timeEntries.filter { it.billed.isEmpty() }.sumOf { (it.minutes / 60.0) * it.rate }
    val totalHours = db.timeEntries.sumOf { it.minutes } / 60.0

    val hero = LinearLayout(c)
    hero.orientation = LinearLayout.VERTICAL
    hero.background = UI.gradient(c, 0xFFF59E0B.toInt(), 0xFFE11D48.toInt(), 22)
    hero.setPadding(UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18), UI.dp(c, 18))
    hero.addView(UI.text(c, "پروژه و زمان", 17, Color.WHITE, true))
    val sub = UI.text(c, "قابلیت برند FreshBooks: ساعت کار را ثبت کنید و با یک لمس فاکتور بزنید.", 11, 0xFFFDE8D9.toInt())
    sub.setLineSpacing(0f, 1.2f)
    hero.addView(sub, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
    // آمار — با فاصلهٔ یکنواخت بین خانه‌ها (heroStats)
    val statsLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    statsLp.topMargin = UI.dp(c, 14)
    hero.addView(UI.heroStats(c, listOf(
        Pair(J.faN(Math.round(totalHours)), "ساعت ثبت‌شده"),
        Pair(J.moneyShort(totalUnbilled), "فاکتورنشده"),
        Pair(J.faN(activeProjects.size), "پروژهٔ فعال")
    ), 0xFFFDE8D9.toInt()), statsLp)
    root.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

    // تایمر زنده — با فیلد «شرح کار» مثل نسخهٔ وب
    if (act.timerFor.isNotEmpty()) {
        val proj = db.projects.firstOrNull { it.id == act.timerFor }
        val timer = UI.card(c)
        timer.background = UI.stroke(c, T.card, 18, 0xFFFF8A3D.toInt(), 2)
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val icon = UI.iconBox(c, R.drawable.ic_timer, 0xFFEA580C.toInt(), 0x1FEA580C.toInt(), 44, 22)
        row.addView(icon)
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        cl.marginStart = UI.dp(c, 12)
        col.addView(UI.text(c, proj?.name ?: "", 13, T.text, true))
        val timerT = UI.text(c, fmtElapsed(act.timerElapsed), 20, 0xFFEA580C.toInt(), true)
        timerT.tag = "timer_text"
        col.addView(timerT, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 2) })
        row.addView(col, cl)
        val stop = UI.text(c, "توقف و ثبت", 12, Color.WHITE, true)
        stop.gravity = Gravity.CENTER
        stop.background = UI.gradient(c, 0xFFDC2626.toInt(), 0xFF9F1239.toInt(), 14)
        stop.setPadding(UI.dp(c, 14), UI.dp(c, 12), UI.dp(c, 14), UI.dp(c, 12))
        stop.setOnClickListener { stopTimer(act) }
        row.addView(stop)
        timer.addView(row)
        // شرح کار در حال انجام — هنگام توقف ثبت می‌شود
        val dIn = UI.field(act, "شرح کار در حال انجام…", timerDesc, hint = "مثلا نصب و راه‌اندازی")
        dIn.second.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b2: Int, c2: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b2: Int, c2: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { timerDesc = s?.toString() ?: "" }
        })
        timer.addView(dIn.first, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        root.addView(timer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
    }

    val tabs = listOf(Triple("در جریان", activeProjects.size, "active"), Triple("تمام‌شده", doneProjects.size, "done"))
    val selIdx = tabs.indexOfFirst { it.third == act.projectsTab }.coerceAtLeast(0)
    root.addView(UI.segScroll(c, tabs, selIdx) { i ->
        act.projectsTab = tabs[i].third
        act.refresh()
    })

    val visible = if (act.projectsTab == "active") activeProjects else doneProjects
    if (visible.isEmpty()) {
        root.addView(UI.empty(c, "پروژه‌ای ندارید", "با دکمهٔ پایین اولین پروژه را بسازید و ساعت کار ثبت کنید", R.drawable.ic_brief), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 16) })
    } else {
        for (p in visible) {
            val h = Db.projectHours(db, p.id)
            val partner = db.partners.firstOrNull { it.id == p.partnerId }
            val card = UI.card(c)
            val head = LinearLayout(c)
            head.orientation = LinearLayout.HORIZONTAL
            head.gravity = Gravity.CENTER_VERTICAL
            val colorBox = UI.iconBox(c, R.drawable.ic_brief, Color.WHITE, 0xFFEA580C.toInt(), 44, 20)
            try {
                (colorBox.background as? android.graphics.drawable.GradientDrawable)?.setColor(Color.parseColor(p.color))
            } catch (ig: Exception) {}
            head.addView(colorBox)
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            cl.marginStart = UI.dp(c, 12)
            val titleRow = LinearLayout(c)
            titleRow.orientation = LinearLayout.HORIZONTAL
            titleRow.gravity = Gravity.CENTER_VERTICAL
            titleRow.addView(UI.text(c, p.name, 13.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            titleRow.addView(UI.chip(c, if (p.done) "تمام‌شده" else if (h.unbilled.isNotEmpty()) J.faN(h.unbilled.size) + " قلم زمان فاکتورنشده" else "همه فاکتور شده", if (p.done) "slate" else if (h.unbilled.isNotEmpty()) "amber" else "green"))
            col.addView(titleRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            col.addView(UI.text(c, (partner?.name?.plus(" • ") ?: "") + (if (p.code.isNotBlank()) "کد " + p.code + " • " else "") + "نرخ " + J.moneyFa(p.rate) + " تومان/ساعت • " + J.faN(Math.round(h.totalMin / 60.0)) + " ساعت کار" + (if (p.dueDate.isNotBlank()) " • تحویل " + J.jFa(p.dueDate) else ""), 10.5f, T.sub))
            head.addView(col, cl)
            card.addView(head)

            val btns = LinearLayout(c)
            btns.orientation = LinearLayout.HORIZONTAL
            btns.gravity = Gravity.CENTER_VERTICAL
            if (!p.done) {
                val running = act.timerFor == p.id
                if (running) {
                    val runLbl = UI.text(c, "تایمر در جریان…", 11.5f, 0xFFEA580C.toInt(), true)
                    runLbl.background = UI.rounded(c, 0x1FEA580C.toInt(), 14)
                    runLbl.setPadding(UI.dp(c, 12), UI.dp(c, 9), UI.dp(c, 12), UI.dp(c, 9))
                    btns.addView(runLbl, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                } else {
                    val start = UI.text(c, "شروع تایمر", 11.5f, Color.WHITE, true)
                    start.gravity = Gravity.CENTER
                    start.background = UI.gradient(c, 0xFFF97316.toInt(), 0xFFE11D48.toInt(), 14)
                    start.setPadding(0, UI.dp(c, 9), 0, UI.dp(c, 9))
                    start.setOnClickListener {
                        act.timerFor = p.id
                        act.timerStartAt = System.currentTimeMillis()
                        act.refresh()
                        UI.toast(c, "تایمر شروع شد — کار را انجام دهید؛ با توقف، زمان ثبت می‌شود")
                    }
                    btns.addView(start, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                }
                val manual = UI.text(c, "ثبت دستی ساعت", 11.5f, T.text, true)
                manual.gravity = Gravity.CENTER
                manual.background = UI.rounded(c, T.cardSoft, 14)
                manual.setPadding(0, UI.dp(c, 9), 0, UI.dp(c, 9))
                manual.setOnClickListener { timeEntrySheet(act, p.id) }
                btns.addView(manual, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(c, 8) })
            }
            if (h.unbilled.isNotEmpty()) {
                val bill = UI.text(c, "فاکتور " + J.moneyShort(h.unbilledValue) + " تومان", 11.5f, Color.WHITE, true)
                bill.gravity = Gravity.CENTER
                bill.background = UI.gradient(c, T.primary, T.primaryD, 14)
                bill.setPadding(0, UI.dp(c, 9), 0, UI.dp(c, 9))
                bill.setOnClickListener { billProject(act, p.id) }
                btns.addView(bill, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(c, 8) })
            }
            card.addView(btns, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })

            for (t in h.entries.take(5)) {
                val row = LinearLayout(c)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.addView(UI.icon(c, if (t.billed.isNotEmpty()) R.drawable.ic_badge else R.drawable.ic_clock, 15, if (t.billed.isNotEmpty()) T.primary else T.amber))
                val lbl = UI.text(c, t.desc, 11, T.text, true)
                val ll = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                ll.marginStart = UI.dp(c, 6)
                row.addView(lbl, ll)
                row.addView(UI.text(c, J.jFa(t.date), 10, T.sub))
                row.addView(UI.text(c, J.faN(Math.round((t.minutes / 60.0) * 10) / 10.0) + " ساعت", 10, T.sub, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = UI.dp(c, 6) })
                row.addView(UI.text(c, J.moneyShort((t.minutes / 60.0) * t.rate), 10, T.primary, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = UI.dp(c, 6) })
                card.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
            }
            if (h.entries.size > 5) {
                card.addView(UI.text(c, "و " + J.faN(h.entries.size - 5) + " قلم دیگر…", 10, T.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
            }
            root.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

fun fmtElapsed(s: Long): String {
    val h = String.format("%02d", s / 3600)
    val m = String.format("%02d", (s % 3600) / 60)
    val sec = String.format("%02d", s % 60)
    return J.fa(h + ":" + m + ":" + sec)
}

fun stopTimer(act: MainActivity) {
    val elapsed = Math.max(1L, (System.currentTimeMillis() - act.timerStartAt) / 1000)
    val minutes = Math.max(1L, elapsed / 60)
    val proj = Db.db.projects.firstOrNull { it.id == act.timerFor }
    Db.update { d -> d.copy(timeEntries = listOf(
        TimeEntry(Db.newId("te"), act.timerFor, J.jTodayStr(), timerDesc.trim().ifEmpty { "کار با تایمر" }, minutes, proj?.rate ?: 0)
    ) + d.timeEntries) }
    UI.toast(act, "زمان ثبت شد — " + J.faN(minutes / 60) + " ساعت و " + J.faN(minutes % 60) + " دقیقه روی «" + (proj?.name ?: "") + "»")
    act.timerFor = ""
    timerDesc = ""
    act.refresh()
}

private var timerDesc = ""

fun timeEntrySheet(act: MainActivity, projectId: String) {
    val bs = BottomSheet(act)
    val db = Db.db
    var date = J.jTodayStr()
    val proj = db.projects.firstOrNull { it.id == projectId }
    bs.show("ثبت زمان — " + (proj?.name ?: ""), "زمان ثبت‌شده در صدور فاکتور پروژه لحاظ می‌شود") { b ->
        val dLabel = ValueLabel()
        val dField = UI.dateField(act, "تاریخ", date) {
            dateSheet(act, date) { v -> date = v; dLabel.set(J.jFa(v)) }
        }
        dLabel.bind(dField, J.jFa(date))
        b.addView(dField)
        val hours = UI.field(act, "مدت (ساعت) — مثلا ۱٫۵", "", hint = "مثلا ۲ یا ۱٫۵", num = true)
        var hoursV = 0.0
        hours.second.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b2: Int, c2: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b2: Int, c2: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { hoursV = J.parseNum(s?.toString() ?: "") }
        })
        b.addView(hours.first)
        val rate = UI.numField(act, "نرخ ساعتی (تومان)", proj?.rate ?: 0)
        var rateV = proj?.rate ?: 0L
        hookNum(rate) { v -> rateV = v }
        b.addView(rate)
        val desc = UI.field(act, "شرح کار", "", hint = "مثلا نصب و راه‌اندازی")
        var descV = ""
        hookText(desc.first) { v -> descV = v }
        b.addView(desc.first)
        var billable = true
        b.addView(UI.switchRow(act, "قابل صورتحساب", "اگر خاموش باشد در فاکتور پروژه وارد نمی‌شود", true) { v -> billable = v })
        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ثبت زمان") {
                if (hoursV <= 0) { UI.toast(act, "ساعت را وارد کنید"); return@primaryBtn }
                val saved = Db.update { d -> d.copy(timeEntries = listOf(
                    TimeEntry(Db.newId("te"), projectId, date, descV.ifEmpty { "کار انجام‌شده" }, Math.round(hoursV * 60), rateV, "", billable)
                ) + d.timeEntries) }
                if (!saved) { UI.toast(act, "زمان کار ذخیره نشد؛ دوباره تلاش کنید"); return@primaryBtn }
                bs.close(); act.refresh()
                UI.toast(act, "زمان ثبت شد — " + (proj?.name ?: ""))
            }))
    }
}

fun projectSheet(act: MainActivity) {
    val bs = BottomSheet(act)
    val db = Db.db
    projPartner = "" // انتخاب قبلی را حمل نکند — انتخابگر همیشه از «بدون مشتری» شروع می‌شود
    bs.show("پروژهٔ جدید", "مشتری و نرخ ساعتی را تعیین کنید") { b ->
        val name = UI.field(act, "نام پروژه *", "", hint = "مثلا تجهیز دفتر مرکزی")
        var nameV = ""
        hookText(name.first) { v -> nameV = v.trim() }
        b.addView(name.first)
        val code = UI.field(act, "کد پروژه (اختیاری)", "", hint = "مثلا PR-1405-01", center = true)
        var codeV = ""
        hookText(code.first) { v -> codeV = v.trim() }
        b.addView(code.first)
        val startDate = UI.field(act, "تاریخ شروع (اختیاری)", "", hint = "۱۴۰۵/۰۱/۰۱", center = true)
        val dueDate = UI.field(act, "تاریخ پایان / تحویل (اختیاری)", "", hint = "۱۴۰۵/۱۲/۲۹", center = true)
        var startV = ""; var dueV = ""
        hookText(startDate.first) { v -> startV = v.trim() }
        hookText(dueDate.first) { v -> dueV = v.trim() }
        val datesRow = LinearLayout(act); datesRow.orientation = LinearLayout.HORIZONTAL
        datesRow.addView(startDate.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        datesRow.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 8), ViewGroup.LayoutParams.WRAP_CONTENT))
        datesRow.addView(dueDate.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(datesRow)
        val budget = UI.numField(act, "بودجهٔ پروژه (تومان، اختیاری)", 0)
        var budgetV = 0L
        hookNum(budget) { v -> budgetV = v }
        b.addView(budget)
        val pDesc = UI.fieldMulti(act, "شرح / محدودهٔ پروژه (اختیاری)", "")
        var descV = ""
        hookText(pDesc.first) { v -> descV = v.trim() }
        b.addView(pDesc.first)
        val pLabel = ValueLabel()
        val pSel = UI.selectField(act, "مشتری", pLabel.current) {
            optionSheet(act, "مشتری", listOf(Opt("", "— بدون مشتری —")) + db.partners.filter { it.type != "supplier" }.map { Opt(it.id, it.name) }, "") { o ->
                pLabel.set(o.label)
                projPartner = o.id
            }
        }
        pLabel.bind(pSel, "— بدون مشتری —")
        b.addView(pSel)
        val rate = UI.numField(act, "نرخ ساعتی پیش‌فرض (تومان)", 0)
        var rateV = 0L
        hookNum(rate) { v -> rateV = v }
        b.addView(rate)
        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ساخت پروژه") {
                if (nameV.isEmpty()) { UI.toast(act, "نام پروژه را وارد کنید"); return@primaryBtn }
                if (rateV < 0 || budgetV < 0) { UI.toast(act, "نرخ و بودجه نمی‌توانند منفی باشند"); return@primaryBtn }
                val datePattern = Regex("[0-9۰-۹٠-٩]{4}/[0-9۰-۹٠-٩]{1,2}/[0-9۰-۹٠-٩]{1,2}")
                if (startV.isNotBlank() && !datePattern.matches(startV)) { UI.toast(act, "تاریخ شروع را با قالب سال/ماه/روز وارد کنید"); return@primaryBtn }
                if (dueV.isNotBlank() && !datePattern.matches(dueV)) { UI.toast(act, "تاریخ پایان را با قالب سال/ماه/روز وارد کنید"); return@primaryBtn }
                if (codeV.isNotBlank() && Db.db.projects.any { it.code.equals(codeV, true) }) { UI.toast(act, "کد پروژه تکراری است"); return@primaryBtn }
                val normalizedDate = { raw: String ->
                    val parts = raw.map { ch -> when (ch) { in '۰'..'۹' -> ('0' + (ch - '۰')); in '٠'..'٩' -> ('0' + (ch - '٠')); else -> ch } }.joinToString("").split('/')
                    if (parts.size != 3) "" else {
                        val y = parts[0].toIntOrNull() ?: 0; val m = parts[1].toIntOrNull() ?: 0; val d0 = parts[2].toIntOrNull() ?: 0
                        if (y < 1000 || m !in 1..12 || d0 !in 1..31) "" else String.format(java.util.Locale.ROOT, "%04d/%02d/%02d", y, m, d0)
                    }
                }
                val startNorm = if (startV.isNotBlank()) normalizedDate(startV) else ""
                val dueNorm = if (dueV.isNotBlank()) normalizedDate(dueV) else ""
                if (startV.isNotBlank() && startNorm.isEmpty()) { UI.toast(act, "تاریخ شروع معتبر نیست"); return@primaryBtn }
                if (dueV.isNotBlank() && dueNorm.isEmpty()) { UI.toast(act, "تاریخ پایان معتبر نیست"); return@primaryBtn }
                if (startNorm.isNotBlank() && dueNorm.isNotBlank() && startNorm > dueNorm) { UI.toast(act, "تاریخ پایان قبل از شروع است"); return@primaryBtn }
                val saved = Db.update { d -> d.copy(projects = d.projects + Project(
                    Db.newId("pr"), nameV, projPartner, rateV,
                    PROJ_COLORS[d.projects.size % PROJ_COLORS.size], false, codeV, startNorm, dueNorm, budgetV, descV
                )) }
                if (!saved) { UI.toast(act, "پروژه ذخیره نشد؛ اطلاعات را نگه دارید و دوباره تلاش کنید"); return@primaryBtn }
                projPartner = ""
                bs.close(); act.refresh()
                UI.toast(act, "پروژه ساخته شد — حالا ساعت کارش را ثبت کنید")
            }))
    }
}

private var projPartner = ""

/** صدور فاکتور از ساعت‌های فاکتورنشدهٔ پروژه — جادوی FreshBooks */
fun billProject(act: MainActivity, projectId: String) {
    val db = Db.db
    val proj = db.projects.firstOrNull { it.id == projectId } ?: return
    val h = Db.projectHours(db, projectId)
    if (h.unbilled.isEmpty()) { UI.toast(act, "ساعت فاکتورنشده‌ای وجود ندارد"); return }
    // گروه‌بندی بر اساس شرح
    val byDesc = LinkedHashMap<String, DoubleArray>() // minutes, rate
    for (t in h.unbilled) {
        val key = t.desc.trim().ifEmpty { "خدمات پروژه" }
        val cur = byDesc[key] ?: doubleArrayOf(0.0, 0.0)
        cur[0] += t.minutes
        if (cur[1] == 0.0) cur[1] = t.rate.toDouble()
        byDesc[key] = cur
    }
    Db.update { d ->
        var items = d.items
        var svcId = d.items.firstOrNull { it.service && it.name == "خدمات پروژه" }?.id
        if (svcId == null) {
            val id = Db.newId("it")
            svcId = id
            items = items + Item(id, (900 + d.items.count { it.service } + 1).toString(), "خدمات پروژه",
                d.groups.firstOrNull { it.name.contains("خدمات") }?.id ?: d.groups.firstOrNull()?.id ?: "", "ساعت",
                proj.rate, 0, true)
        }
        val maxSale = d.invoices.filter { it.kind == K_SALE }.maxOfOrNull { it.no } ?: 100L
        val no = maxSale + 1
        val inv = Invoice(
            Db.newId("inv"), no, K_SALE, proj.partnerId.ifEmpty { d.partners.firstOrNull()?.id ?: "" },
            d.warehouses.firstOrNull()?.id ?: "", J.jTodayStr(),
            byDesc.entries.map { InvoiceLine(Db.newId("l"), svcId ?: "", Math.round((it.value[0] / 60.0) * 100) / 100.0, it.value[1].toLong(), 0.0) },
            0, d.settings.vatRate, 0, final = true, moudian = d.settings.moudian,
            desc = "خدمات پروژه «" + proj.name + "» — " + byDesc.keys.joinToString(" ، ")
        )
        d.copy(
            items = items, invoices = listOf(inv) + d.invoices,
            counters = d.counters + ("invoice" to (d.counters["invoice"] ?: 0L) + 1L),
            timeEntries = d.timeEntries.map { if (it.projectId == projectId && it.billed.isEmpty()) it.copy(billed = J.faN(no)) else it }
        )
    }
    act.refresh()
    UI.toast(act, "فاکتور صادر شد — سند حسابداری هم ساخته شد؛ " + J.faN(h.unbilled.size) + " قلم زمان به ارزش " + J.moneyShort(h.unbilledValue) + " تومان")
}
