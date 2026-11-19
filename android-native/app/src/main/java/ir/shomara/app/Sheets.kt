package ir.shomara.app

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * باتن‌شیت بومی — همهٔ درایورها، منوها، فرم‌ها و انتخابگرها پایین صفحه باز می‌شوند
 * (بدون هیچ کتابخانهٔ خارجی)
 */
class BottomSheet(private val act: Activity) {
    // دیالوگ فقط داخل show() و داخل بلوک محافظت‌شده ساخته می‌شود —
    // هیچ لمسی که شیت باز می‌کند دیگر نمی‌تواند کرش کند
    private var dlg: Dialog? = null
    private lateinit var sheet: LinearLayout
    private var dismissed = false

    /** پس از بسته‌شدن شیت (هر مسیری: ضربدر، کشیدن، بک) — برای پاکسازی منابع مثل میکروفون */
    var onDismissed: (() -> Unit)? = null

    fun show(title: String, sub: String = "", build: (LinearLayout) -> Unit) {
        val dm = act.resources.displayMetrics
        val d = try { Dialog(act, R.style.BSD) } catch (e: Throwable) {
            ErrorReport.report(act, "sheet-ctor:" + title, e)
            return
        }
        dlg = d
        val root = FrameLayout(act)
        root.layoutDirection = View.LAYOUT_DIRECTION_RTL
        root.setBackgroundColor(T.scrim)
        root.setOnClickListener { close() }

        sheet = LinearLayout(act)
        sheet.orientation = LinearLayout.VERTICAL
        sheet.background = SheetBg.rounded(act)
        sheet.setOnClickListener { }
        val slp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        slp.gravity = Gravity.BOTTOM
        root.addView(sheet, slp)

        // دستگیرهٔ کشیدن
        val hw = View(act)
        hw.background = UI.rounded(act, if (T.dark) 0xFF3A4450.toInt() else 0xFFD9DEE5.toInt(), 4)
        val hlp = LinearLayout.LayoutParams(UI.dp(act, 44), UI.dp(act, 5))
        hlp.gravity = Gravity.CENTER_HORIZONTAL
        hlp.topMargin = UI.dp(act, 10)
        hlp.bottomMargin = UI.dp(act, 4)
        sheet.addView(hw, hlp)
        var sy = 0f
        hw.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { sy = ev.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    val d = ev.rawY - sy
                    if (d > 0) sheet.translationY = d
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (sheet.translationY > UI.dp(act, 130)) close()
                    else sheet.animate().translationY(0f).setDuration(180).start()
                    true
                }
                else -> false
            }
        }

        // عنوان
        val tr = LinearLayout(act)
        tr.orientation = LinearLayout.HORIZONTAL
        tr.gravity = Gravity.CENTER_VERTICAL
        tr.setPadding(UI.dp(act, 18), UI.dp(act, 6), UI.dp(act, 18), UI.dp(act, 10))
        val tcol = LinearLayout(act)
        tcol.orientation = LinearLayout.VERTICAL
        tcol.addView(UI.text(act, title, 16, T.text, true))
        if (sub.isNotEmpty()) {
            val s = UI.text(act, sub, 11, T.sub)
            val sl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            sl.topMargin = UI.dp(act, 2)
            tcol.addView(s, sl)
        }
        tr.addView(tcol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val x = UI.icon(act, R.drawable.ic_close, 22, T.sub)
        x.setOnClickListener { close() }
        tr.addView(x)
        sheet.addView(tr)
        sheet.addView(UI.sep(act), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(act, 1)))

        // بدنه با اسکرول
        // ⚡ درمان «چسبیدن افقی/عمودی در همهٔ فرم‌ها»: بدنهٔ شیت فاصلهٔ عمودی بین
        // ردیف‌های بی‌مارجین را خودکار تامین می‌کند — هر فرم و هر دیالوگ اپ یک‌جا درست می‌شود
        // ⚡ ردیف‌هایی که بدون LayoutParams اضافه می‌شوند هم عرض کامل + فاصلهٔ استاندارد می‌گیرند
        //   (قبلاً مارجین خودِ فیلدها هنگام addView بدون پارامتر گم می‌شد و ردیف‌ها به هم می‌چسبیدند)
        val body = object : LinearLayout(act) {
            override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
                if (child != null && params == null) {
                    val lp = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    if (childCount > 0) lp.topMargin = UI.dp(act, 12)
                    super.addView(child, index, lp)
                    return
                }
                if (child != null && params is LinearLayout.LayoutParams &&
                    params.topMargin == 0 && params.bottomMargin == 0) {
                    val pos = if (index >= 0) index else childCount
                    if (pos > 0) params.topMargin = UI.dp(act, 12) // فاصلهٔ استاندارد بین ردیف‌های فرم
                }
                super.addView(child, index, params)
            }
        }
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(UI.dp(act, 16), UI.dp(act, 14), UI.dp(act, 16), UI.dp(act, 24))
        val scroll = ScrollView(act)
        scroll.isVerticalScrollBarEnabled = false
        scroll.addView(body, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        sheet.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        try {
            build(body)
        } catch (e: Throwable) {
            // مهار کرش باتن‌شیت — شیت باز می‌ماند و پیام خطا نشان داده می‌شود
            ErrorReport.report(act, "sheet:" + title, e)
            val et = UI.text(act, "باز شدن این بخش با خطا مواجه شد — گزارش ثبت شد", 12.5f, T.rose, true)
            body.addView(et, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            val eb = UI.outlineBtn(act, "ارسال گزارش") { ErrorReport.share(act, "from:sheet " + title) }
            body.addView(eb, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
        }

        try {
            d.setContentView(root)
            try { d.setOnDismissListener { onDismissed?.invoke() } } catch (ig: Throwable) {}
            d.window?.let { w ->
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            d.show()
        } catch (e: Throwable) {
            // اگر اکتیویتی در حال بسته‌شدن باشد یا پنجره آماده نباشد — اپ نباید کرش کند
            ErrorReport.report(act, "sheet-show:" + title, e)
            try { d.dismiss() } catch (ig: Throwable) {}
            return
        }

        sheet.post {
            try {
                val maxH = (dm.heightPixels * 0.88).toInt()
                val chrome = sheet.height - scroll.height
                if (sheet.height > maxH && chrome < maxH) {
                    scroll.layoutParams.height = maxH - chrome
                    scroll.requestLayout()
                }
                sheet.translationY = sheet.height.toFloat()
                sheet.animate().translationY(0f).setDuration(230).start()
                // نگهبان تضمینی نمایش — مثل اصلاح صفحهٔ خالی، اگر انیمیشن گم شد شیت محو نمی‌ماند
                sheet.postDelayed({
                    try { if (sheet.translationY > sheet.height * 0.5f) sheet.translationY = 0f } catch (ig: Throwable) {}
                }, 500L)
            } catch (ig: Throwable) {
                try { sheet.translationY = 0f } catch (ig2: Throwable) {}
            }
        }
    }

    fun close() {
        if (dismissed) return
        dismissed = true
        val d = dlg ?: return
        try {
            sheet.animate().translationY(sheet.height.toFloat()).setDuration(190)
                .withEndAction { try { d.dismiss() } catch (ig: Throwable) {} }.start()
        } catch (ig: Exception) { try { d.dismiss() } catch (ig2: Throwable) {} }
    }
}

/** پس‌زمینهٔ گرد بالای باتن‌شیت */
object SheetBg {
    fun rounded(c: android.content.Context): android.graphics.drawable.GradientDrawable {
        val g = android.graphics.drawable.GradientDrawable()
        g.setColor(T.card)
        val r = UI.dp(c, 26).toFloat()
        g.cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
        return g
    }
}

/* ═════════════════════════ انتخابگرها (همه باتن‌شیت) ═════════════════════════ */

/** انتخاب از لیست گزینه‌ها — جایگزین <select> نسخهٔ وب */
data class Opt(val id: String, val label: String, val sub: String = "", val ic: Int = 0)

fun optionSheet(act: Activity, title: String, items: List<Opt>, selectedId: String, onPick: (Opt) -> Unit) {
    val bs = BottomSheet(act)
    bs.show(title, "یک گزینه انتخاب کنید") { b ->
        if (items.isEmpty()) {
            b.addView(UI.empty(act, "گزینه‌ای وجود ندارد"))
            return@show
        }
        for (it0 in items) {
            val row = LinearLayout(act)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.background = UI.rounded(act, if (it0.id == selectedId) T.primarySoft else T.cardSoft, 16)
            row.setPadding(UI.dp(act, 12), UI.dp(act, 11), UI.dp(act, 12), UI.dp(act, 11))
            if (it0.ic != 0) {
                row.addView(UI.icon(act, it0.ic, 20, if (it0.id == selectedId) T.primary else T.sub))
            } else {
                row.addView(UI.iconBox(act, R.drawable.ic_dots, T.sub, T.card, 30, 14, 10))
            }
            val col = LinearLayout(act)
            col.orientation = LinearLayout.VERTICAL
            val il = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            il.marginStart = UI.dp(act, 10)
            col.addView(UI.text(act, it0.label, 13, if (it0.id == selectedId) T.primary else T.text, true))
            if (it0.sub.isNotEmpty()) {
                val s = UI.text(act, it0.sub, 10, T.sub)
                val sl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                sl.topMargin = UI.dp(act, 1)
                col.addView(s, sl)
            }
            row.addView(col, il)
            val w = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            row.layoutParams = w
            row.setOnClickListener {
                bs.close()
                onPick(it0)
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = UI.dp(act, 6)
            b.addView(row, lp)
        }
    }
}

/** انتخابگر تاریخ جلالی — تقویم ماهانهٔ واقعی با باتن‌شیت */
fun dateSheet(act: Activity, iso: String, onPick: (String) -> Unit) {
    val bs = BottomSheet(act)
    bs.show("انتخاب تاریخ", "تقویم جلالی") { b ->
        val j0 = J.jParse(if (iso.isEmpty()) J.jTodayStr() else iso)
        var jy = j0[0]
        var jm = j0[1]
        var jd = j0[2]

        val head = LinearLayout(act)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val prev = UI.icon(act, R.drawable.ic_chev_r, 22, T.text)
        val next = UI.icon(act, R.drawable.ic_chev_l, 22, T.text)
        val label = UI.text(act, J.jMonth(jm) + " " + J.fa(jy.toString()), 14, T.text, true)
        label.gravity = Gravity.CENTER
        head.addView(prev)
        head.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(next)
        b.addView(head)

        val gridWrap = LinearLayout(act)
        gridWrap.orientation = LinearLayout.VERTICAL
        b.addView(gridWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })

        val weekdays = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val wkRow = LinearLayout(act)
        wkRow.orientation = LinearLayout.HORIZONTAL
        for (w in weekdays) {
            val t = UI.text(act, w, 10, T.sub, true)
            t.gravity = Gravity.CENTER
            wkRow.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        gridWrap.addView(wkRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        fun buildGrid() {
            label.text = J.jMonth(jm) + " " + J.fa(jy.toString())
            gridWrap.removeViews(1, gridWrap.childCount - 1)
            val firstDow = ((J.jDaysOf(J.jFmt(intArrayOf(jy, jm, 1))) % 7) + 3) % 7 // 0=شنبه .. 6=جمعه
            val len = J.jMonthLen(jy, jm)
            var row: LinearLayout? = null
            for (d in 1..(firstDow + len)) {
                if ((d - 1) % 7 == 0) {
                    row = LinearLayout(act)
                    row.orientation = LinearLayout.HORIZONTAL
                    gridWrap.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 4) })
                }
                val day = d - firstDow
                if (day < 1) {
                    row?.addView(View(act), LinearLayout.LayoutParams(0, UI.dp(act, 40), 1f))
                    continue
                }
                val isSel = day == jd
                val t = UI.text(act, J.fa(day.toString()), 12, if (isSel) Color.WHITE else T.text, isSel)
                t.gravity = Gravity.CENTER
                t.background = if (isSel) UI.gradient(act, T.primary, T.primaryD, 12) else null
                val lp = LinearLayout.LayoutParams(0, UI.dp(act, 40), 1f)
                t.setOnClickListener {
                    jd = day
                    bs.close()
                    onPick(J.jFmt(intArrayOf(jy, jm, day)))
                }
                row?.addView(t, lp)
            }
        }
        prev.setOnClickListener { if (jm == 1) { jm = 12; jy-- } else jm--; buildGrid() }
        next.setOnClickListener { if (jm == 12) { jm = 1; jy++ } else jm++; buildGrid() }
        buildGrid()

        val todayBtn = UI.outlineBtn(act, "برو به امروز") {
            val t = J.jTodayStr()
            val p = J.jParse(t)
            jy = p[0]; jm = p[1]; jd = p[2]
            bs.close()
            onPick(t)
        }
        b.addView(todayBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
    }
}

/** تأیید حذف/عملیات */
fun confirmSheet(act: Activity, title: String, msg: String, yesLabel: String = "حذف", onYes: () -> Unit) {
    val bs = BottomSheet(act)
    bs.show(title) { b ->
        val t = UI.text(act, msg, 12.5f, T.sub)
        t.setLineSpacing(0f, 1.25f)
        b.addView(t)
        val row = LinearLayout(act)
        row.orientation = LinearLayout.HORIZONTAL
        val rl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rl.topMargin = UI.dp(act, 14)
        val yes = UI.primaryBtn(act, yesLabel) { bs.close(); onYes() }
        yes.background = UI.rounded(act, T.rose, 16)
        row.addView(yes, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val no = UI.outlineBtn(act, "انصراف") { bs.close() }
        val nl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        nl.marginStart = UI.dp(act, 8)
        row.addView(no, nl)
        b.addView(row, rl)
    }
}

/** منوی دسترسی سریع — مثل Sheet نسخهٔ وب */
fun quickSheet(act: MainActivity) {
    val bs = BottomSheet(act)
    bs.show("دسترسی سریع", "پرکاربردترین بخش‌ها، یک لمس دورتر") { b ->
        val items = listOf(
            Quadruple(R.drawable.ic_sparkles, "هوشیار — دستیار هوشمند", "از داده‌هایتان بپرسید و عمل کنید", "assistant"),
            Quadruple(R.drawable.ic_receipt, "فاکتور فروش جدید", "صدور سریع با سند خودکار", "invoices"),
            Quadruple(R.drawable.ic_compass, "ثبت دریافت از مشتری", "نقدی، کارتخوان یا حواله", "cash"),
            Quadruple(R.drawable.ic_search, "تطبیق بانک", "تراکنش‌ها را با یک لمس تطبیق دهید", "reconcile"),
            Quadruple(R.drawable.ic_grid, "همهٔ ماژول‌ها", "منوی کامل ماژول‌ها", "more")
        )
        for (a in items) {
            val row = LinearLayout(act)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.background = UI.stroke(act, T.card, 18, T.border, 1)
            row.setPadding(UI.dp(act, 12), UI.dp(act, 11), UI.dp(act, 12), UI.dp(act, 11))
            row.addView(UI.iconBox(act, a.first, T.primary, T.primarySoft, 40, 20))
            val col = LinearLayout(act)
            col.orientation = LinearLayout.VERTICAL
            val cl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            cl.marginStart = UI.dp(act, 12)
            col.addView(UI.text(act, a.second, 13, T.text, true))
            col.addView(UI.text(act, a.third, 10, T.sub))
            row.addView(col, cl)
            row.setOnClickListener { bs.close(); act.go(a.fourth) }
            b.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        }
    }
}

/** جستجوی سراسری — Command Palette */
fun globalSearchSheet(act: MainActivity, initial: String = "") {
    val bs = BottomSheet(act)
    bs.show("جستجو در همه‌جا", "کالا، طرف‌حساب، فاکتور، چک، پروژه یا تراکنش بانکی") { b ->
        val (box, e) = UI.search(act, "بنویسید… مثلا «یخچال» یا «سپهر»")
        b.addView(box)
        val results = LinearLayout(act)
        results.orientation = LinearLayout.VERTICAL
        b.addView(results, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })

        fun render() {
            results.removeAllViews()
            val hits = Db.globalSearch(Db.db, e.text.toString())
            if (e.text.toString().trim().isNotEmpty() && hits.isEmpty()) {
                val t = UI.text(act, "چیزی پیدا نشد", 12, T.sub)
                t.gravity = Gravity.CENTER
                t.setPadding(0, UI.dp(act, 24), 0, UI.dp(act, 12))
                results.addView(t)
                return
            }
            for (h in hits) {
                val row = LinearLayout(act)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.background = UI.stroke(act, T.card, 18, T.border, 1)
                row.setPadding(UI.dp(act, 12), UI.dp(act, 10), UI.dp(act, 12), UI.dp(act, 10))
                val kb = UI.iconBox(act, R.drawable.ic_search, T.primary, T.primarySoft, 36, 16, 12)
                val kt = UI.text(act, h.kind.take(2), 10, T.primary, true)
                row.addView(kb)
                val col = LinearLayout(act)
                col.orientation = LinearLayout.VERTICAL
                val cl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                cl.marginStart = UI.dp(act, 12)
                col.addView(UI.text(act, h.title, 12.5f, T.text, true))
                if (h.sub.isNotEmpty()) col.addView(UI.text(act, h.sub, 10, T.sub))
                row.addView(col, cl)
                row.setOnClickListener {
                    bs.close()
                    when (h.kind) {
                        "کالا" -> act.itemsQuery = h.title
                        "طرف‌حساب" -> act.partnersQuery = h.title
                        "فاکتور" -> act.invQuery = initial
                    }
                    act.go(h.go)
                }
                results.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
            }
        }
        e.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b2: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { render() }
        })
        if (initial.isNotEmpty()) e.setText(initial) // از دستیار صوتی می‌آید
        render()
        e.requestFocus()
        e.postDelayed({ try { (act.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showSoftInput(e, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) } catch (ig: Exception) {} }, 250)
    }
}

data class Quadruple(val first: Int, val second: String, val third: String, val fourth: String)

/** انتخابگر بخش حسابداری (دفتر روزنامه/کل/معین/دستی) و تب‌های عمومی */
fun tabSheet(act: Activity, title: String, options: List<Opt>, selectedId: String, onPick: (String) -> Unit) {
    optionSheet(act, title, options, selectedId) { o ->
        onPick(o.id)
    }
}

/** نمایش و ارسال گزارش خطاها — مثل Sentry درون اپ */
fun crashLogSheet(act: MainActivity) {
    val bs = BottomSheet(act)
    bs.show("گزارش خطاها", "آخرین خطاهای ثبت‌شده در برنامه") { b ->
        val log = ErrorReport.read(act)
        if (log.isEmpty()) {
            b.addView(UI.empty(act, "هیچ خطایی ثبت نشده", "همه‌چیز سالم است — نیازی به ارسال گزارش نیست", R.drawable.ic_checkcheck))
        } else {
            val card = UI.card(c = act)
            val t = UI.text(act, log.take(12000), 9.5f, T.sub)
            t.setLineSpacing(0f, 1.15f)
            card.addView(t, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            b.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            val row = LinearLayout(act)
            row.orientation = LinearLayout.HORIZONTAL
            val rl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            rl.topMargin = UI.dp(act, 10)
            row.addView(UI.primaryBtn(act, "ارسال برای پشتیبانی") { ErrorReport.share(act, "from:crashLogSheet") },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val sl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            sl.marginStart = UI.dp(act, 8)
            row.addView(UI.outlineBtn(act, "پاک‌کردن گزارش") {
                ErrorReport.clear(act)
                UI.toast(act, "گزارش خطاها پاک شد")
                bs.close()
            }, sl)
            b.addView(row, rl)
        }
    }
}
