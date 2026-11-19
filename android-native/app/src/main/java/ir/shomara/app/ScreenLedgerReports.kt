package ir.shomara.app

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * ماژول حسابداری — سند دستی، دفتر روزنامه، دفتر کل و دفتر معین
 * اسناد سیستمی از فاکتورها/چک‌ها خودکار اینجا هم دیده می‌شوند (مثل نرم‌افزارهای حرفه‌ای)
 */

fun ledgerScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    val all = Db.allVouchers(db)
    val summary = UI.card(c)
    summary.orientation = LinearLayout.HORIZONTAL
    fun sumCell(label: String, value: String): LinearLayout {
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.addView(UI.text(c, label, 11, T.sub, true))
        val t = UI.text(c, value, 17, T.text, true)
        t.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 5) }
        col.addView(t)
        return col
    }
    summary.addView(sumCell("اسناد ثبت‌شده", J.faN(all.size)), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    summary.addView(sumCell("اسناد دستی", J.faN(db.vouchers.size)))
    summary.addView(sumCell("اسناد سیستمی", J.faN(all.size - db.vouchers.size)))
    root.addView(summary)

    val views = listOf(
        Triple("دفتر روزنامه", -1, "journal"), Triple("دفتر کل", -1, "general"),
        Triple("دفتر معین", -1, "subsidiary"), Triple("سند دستی", db.vouchers.size, "manual")
    )
    val selIdx = views.indexOfFirst { it.third == act.ledgerView }.coerceAtLeast(0)
    root.addView(UI.segScroll(c, views, selIdx) { i ->
        act.ledgerView = views[i].third
        act.refresh()
    })

    val view = act.ledgerView
    val body = LinearLayout(c)
    root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })

    if (view == "journal" || view == "manual") {
        if (view == "manual") {
            val newBtn = UI.text(c, "+ سند حسابداری دستی جدید", 12.5f, T.primary, true)
            newBtn.gravity = Gravity.CENTER
            newBtn.background = UI.stroke(c, T.card, 18, T.border, 1)
            newBtn.setPadding(UI.dp(c, 12), UI.dp(c, 14), UI.dp(c, 12), UI.dp(c, 14))
            newBtn.setOnClickListener { voucherEditorSheet(act, null) }
            body.addView(newBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
        // ⚡ رفع «روزنامه خالی»: در تب سند دستی هم لیست کامل اسناد رندر می‌شود
        // (قبلاً فقط دکمهٔ ثبت نمایش داده می‌شد و لیست خالی به نظر می‌رسید)
        if (view == "journal" || view == "manual") {
            val (sbox, se) = UI.search(c, "جستجو در شرح اسناد…")
            se.setText(act.ledgerQuery)
            body.addView(sbox)
            // لیست اسناد — زنده با تایپ
            val vList = LinearLayout(c)
            body.addView(vList, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
            fun renderList() {
                val q = act.ledgerQuery
                vList.removeAllViews()
                val filtered = all
                    .filter { if (view == "manual") it.source.isEmpty() else true }
                    .filter { if (q.isEmpty()) true else it.desc.contains(q) || it.no.toString().contains(q) }
                    .sortedByDescending { it.date }
                if (filtered.isEmpty()) {
                    val em = if (q.isEmpty())
                        UI.empty(c,
                            if (view == "manual") "هنوز سند دستی ثبت نکرده‌اید" else "سندی یافت نشد",
                            "با ثبت فاکتور یا دریافت، سند خودکار ساخته می‌شود؛ یا اولین سند را خودتان ثبت کنید",
                            R.drawable.ic_book)
                    else UI.empty(c, "چیزی پیدا نشد", "عبارت دیگری را امتحان کنید", R.drawable.ic_search)
                    vList.addView(em, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 16) })
                    // هیچ حالت خالی بی‌عمل نمی‌ماند — ثبت اولین سند یک‌لمسه
                    if (q.isEmpty()) {
                        val b0 = UI.primaryBtn(c, "ثبت اولین سند حسابداری", icRes = R.drawable.ic_pen_note) { voucherEditorSheet(act, null) }
                        val bl0 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        bl0.setMargins(UI.dp(c, 48), UI.dp(c, 12), UI.dp(c, 48), 0)
                        vList.addView(b0, bl0)
                    }
                } else {
                    val partnerMap = db.partners.associate { it.id to it.name }
                    val bankMap = db.banks.associate { it.id to it.name }
                    fun detailName(id: String) = partnerMap[id] ?: bankMap[id] ?: ""
                    for (v in filtered.take(40)) {
                        val sumD = v.lines.sumOf { it.debit }
                        val card = UI.card(c)
                        val head = LinearLayout(c)
                        head.orientation = LinearLayout.HORIZONTAL
                        val col = LinearLayout(c)
                        col.orientation = LinearLayout.VERTICAL
                        val titleRow = LinearLayout(c)
                        titleRow.orientation = LinearLayout.HORIZONTAL
                        titleRow.gravity = Gravity.CENTER_VERTICAL
                        titleRow.addView(UI.text(c, v.desc, 12.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        titleRow.addView(UI.chip(c, if (v.source.isNotEmpty()) "خودکار" else "دستی", if (v.source.isNotEmpty()) "sky" else "violet"))
                        col.addView(titleRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                        col.addView(UI.text(c, J.jFa(v.date) + " • سند " + (if (v.no == 0L) "سیستمی" else J.faN(v.no)), 10, T.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
                        head.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        head.addView(UI.text(c, J.moneyShort(sumD) + " ت", 13, T.text, true))
                        card.addView(head)
                        for (l in v.lines) {
                            val row = UI.innerCard(c, T.cardSoft, 12, 10)
                            row.orientation = LinearLayout.HORIZONTAL
                            val txt = J.fa(l.accountCode) + " " + accName(l.accountCode) + (if (l.detailId.isNotEmpty()) " — " + detailName(l.detailId) else "") + (if (l.note.isNotEmpty()) " • " + l.note else "")
                            row.addView(UI.text(c, txt, 11, T.sub), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                            row.addView(UI.text(c, if (l.debit != 0L) "بدهکار " + J.moneyFa(l.debit) else "بستانکار " + J.moneyFa(l.credit), 11, if (l.debit != 0L) T.amber else T.primary, true))
                            card.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 5) })
                        }
                        if (v.source.isEmpty()) {
                            card.addView(UI.btnRow(c,
                                UI.outlineBtn(c, "ویرایش", icRes = R.drawable.ic_pen_note) { voucherEditorSheet(act, v) },
                                UI.outlineBtn(c, "حذف", danger = true, icRes = R.drawable.ic_trash) {
                                    confirmSheet(c, "حذف سند؟", "سند «" + v.desc + "» حذف می‌شود.") {
                                        Db.update { d -> d.copy(vouchers = d.vouchers.filter { it.id != v.id }) }
                                        act.refresh()
                                        UI.toast(act, "سند حذف شد")
                                    }
                                }))
                        }
                        vList.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
                    }
                }
            }
            se.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) { act.ledgerQuery = s?.toString() ?: ""; renderList() }
            })
            renderList()
        }
    }

    if (view == "general") {
        body.addView(UI.sectionTitle(c, "دفتر کل", "جمع بدهکار و بستانکار هر سرفصل", R.drawable.ic_bookmark))
        val tb = Db.trialBalance(db)
        val card = UI.card(c)
        for (a in ACCOUNTS) {
            val r = tb.firstOrNull { it.code == a.code } ?: continue
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(UI.dp(c, 4), UI.dp(c, 10), UI.dp(c, 4), UI.dp(c, 10))
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            col.addView(UI.text(c, J.fa(a.code) + " — " + a.name, 12, T.text, true))
            col.addView(UI.text(c, a.grp, 10, T.sub))
            row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val end = LinearLayout(c)
            end.orientation = LinearLayout.VERTICAL
            end.gravity = Gravity.START
            end.addView(UI.text(c, J.moneyShort(r.balance), 12, T.text, true))
            end.addView(UI.text(c, if (r.kind == "asset" || r.kind == "expense") "بدهکار" else "بستانکار", 9, T.sub))
            row.addView(end)
            card.addView(row)
            if (ACCOUNTS.indexOf(a) < ACCOUNTS.size - 1) card.addView(UI.sep(c), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 1)))
        }
        body.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
    }

    if (view == "subsidiary") {
        body.addView(UI.sectionTitle(c, "دفتر معین", "گردش یک حساب را با انتخاب سرفصل ببینید", R.drawable.ic_booktext))
        val accLabel = ValueLabel()
        val accSel = UI.selectField(c, "سرفصل حساب", accLabel.current) {
            optionSheet(c, "انتخاب سرفصل", ACCOUNTS.map { Opt(it.code, J.fa(it.code) + " — " + it.name) }, act.ledgerAcc) { o ->
                act.ledgerAcc = o.id
                act.refresh()
            }
        }
        accLabel.bind(accSel, if (act.ledgerAcc.isEmpty()) "— سرفصل را انتخاب کنید —" else J.fa(act.ledgerAcc) + " — " + accName(act.ledgerAcc))
        body.addView(accSel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })

        if (act.ledgerAcc.isEmpty()) {
            body.addView(UI.empty(c, "یک سرفصل انتخاب کنید", "مثلاً «حساب‌های دریافتنی» برای دیدن مطالبات", R.drawable.ic_layers), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        } else {
            data class Row(val date: String, val desc: String, val debit: Long, val credit: Long, val detail: String)
            val rows = ArrayList<Row>()
            val partnerMap = db.partners.associate { it.id to it.name }
            val bankMap = db.banks.associate { it.id to it.name }
            for (v in Db.allVouchers(db)) for (l in v.lines)
                if (l.accountCode == act.ledgerAcc)
                    rows.add(Row(v.date, v.desc, l.debit, l.credit, partnerMap[l.detailId] ?: bankMap[l.detailId] ?: ""))
            rows.sortBy { it.date }
            if (rows.isEmpty()) {
                body.addView(UI.empty(c, "گردشی ثبت نشده", "", R.drawable.ic_booktext), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
            } else {
                var run = 0L
                val card = UI.card(c)
                for (r in rows) {
                    run += r.debit - r.credit
                    val cell = UI.innerCard(c, T.cardSoft, 14, 10)
                    val head = LinearLayout(c)
                    head.orientation = LinearLayout.HORIZONTAL
                    head.addView(UI.text(c, J.jFa(r.date), 10, T.sub, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    head.addView(UI.text(c, if (r.debit != 0L) "+" + J.moneyFa(r.debit) else "−" + J.moneyFa(r.credit), 11.5f, if (r.debit != 0L) T.amber else T.primary, true))
                    cell.addView(head)
                    cell.addView(UI.text(c, r.desc, 11, T.text, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 3) })
                    cell.addView(UI.text(c, (if (r.detail.isNotEmpty()) r.detail + " • " else "") + "مانده: " + J.moneyShort(Math.abs(run)) + " " + (if (run >= 0) "(بدهکار)" else "(بستانکار)"), 9.5f, T.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 3) })
                    card.addView(cell, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
                }
                body.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
                val fin = UI.card(c)
                fin.orientation = LinearLayout.HORIZONTAL
                fin.gravity = Gravity.CENTER_VERTICAL
                fin.addView(UI.text(c, "ماندهٔ نهایی حساب", 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                fin.addView(UI.text(c, J.moneyFa(Math.abs(run)) + " " + (if (run >= 0) "بدهکار" else "بستانکار"), 15, if (run >= 0) T.amber else T.primary, true))
                body.addView(fin, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
            }
        }
    }

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

/* ═════════════════════════ سند دستی ═════════════════════════ */

fun voucherEditorSheet(act: MainActivity, v0: Voucher?) {
    val bs = BottomSheet(act)
    val db = Db.db
    var v = v0 ?: Voucher(Db.newId("vc"), 700L + db.vouchers.size + 1, J.jTodayStr(), "", listOf(
        VoucherLine("1101", "", 0, 0), VoucherLine("5102", "", 0, 0)))
    val isNew = !db.vouchers.any { it.id == v.id }
    val linesWrap = LinearLayout(act)
    val balanceT = UI.text(act, "", 12, T.text, true)
    val dLabel = ValueLabel()

    fun renderBalance() {
        val sumD = v.lines.sumOf { it.debit }
        val sumC = v.lines.sumOf { it.credit }
        balanceT.text = "بدهکار: " + J.moneyFa(sumD) + "   •   بستانکار: " + J.moneyFa(sumC)
        balanceT.setTextColor(if (sumD == sumC && sumD != 0L) T.primary else T.rose)
    }

    fun renderLines() {
        linesWrap.removeAllViews()
        for ((idx, l) in v.lines.withIndex()) {
            val isBankAcc = l.accountCode == "1101" || l.accountCode == "1102"
            val isDetailAcc = l.accountCode == "1103" || l.accountCode == "2101"
            val card = UI.innerCard(act, T.card, 14, 10)
            val accLabel = ValueLabel()
            val accSel = UI.selectField(act, "سرفصل", accLabel.current) {
                optionSheet(act, "انتخاب سرفصل", ACCOUNTS.map { Opt(it.code, J.fa(it.code) + " — " + it.name) }, l.accountCode) { o ->
                    val lines = v.lines.toMutableList()
                    lines[idx] = lines[idx].copy(accountCode = o.id)
                    v = v.copy(lines = lines)
                    accLabel.set(o.label)
                    renderLines()
                }
            }
            accSel.background = UI.rounded(act, T.cardSoft, 12)
            accLabel.bind(accSel, J.fa(l.accountCode) + " — " + accName(l.accountCode))
            val head = LinearLayout(act)
            head.orientation = LinearLayout.HORIZONTAL
            head.addView(accSel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val x = UI.text(act, "✕", 12, T.rose, true)
            x.setPadding(UI.dp(act, 8), 0, 0, 0)
            x.setOnClickListener {
                v = v.copy(lines = v.lines.filterIndexed { i, _ -> i != idx })
                renderLines(); renderBalance()
            }
            head.addView(x)
            card.addView(head)

            if (isBankAcc) {
                val bl = ValueLabel()
                val bSel = UI.selectField(act, "کدام صندوق/بانک؟", bl.current) {
                    optionSheet(act, "صندوق / بانک", db.banks.map { Opt(it.id, it.name) }, l.detailId) { o ->
                        val lines = v.lines.toMutableList()
                        lines[idx] = lines[idx].copy(detailId = o.id)
                        v = v.copy(lines = lines)
                        bl.set(o.label)
                    }
                }
                bl.bind(bSel, db.banks.firstOrNull { it.id == l.detailId }?.name ?: "")
                card.addView(bSel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
            }
            if (isDetailAcc) {
                val pl = ValueLabel()
                val pSel = UI.selectField(act, "کدام طرف‌حساب؟", pl.current) {
                    optionSheet(act, "طرف‌حساب", db.partners.map { Opt(it.id, it.name) }, l.detailId) { o ->
                        val lines = v.lines.toMutableList()
                        lines[idx] = lines[idx].copy(detailId = o.id)
                        v = v.copy(lines = lines)
                        pl.set(o.label)
                    }
                }
                pl.bind(pSel, db.partners.firstOrNull { it.id == l.detailId }?.name ?: "")
                card.addView(pSel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
            }

            val debit = UI.field(act, "بدهکار", if (l.debit != 0L) J.moneyFa(l.debit) else "", num = true, ltr = true, center = true)
            val credit = UI.field(act, "بستانکار", if (l.credit != 0L) J.moneyFa(l.credit) else "", num = true, ltr = true, center = true)
            hookNum(debit.first) { nv ->
                val lines = v.lines.toMutableList()
                lines[idx] = lines[idx].copy(debit = nv, credit = if (nv != 0L) 0L else lines[idx].credit)
                v = v.copy(lines = lines)
                renderBalance()
            }
            hookNum(credit.first) { nv ->
                val lines = v.lines.toMutableList()
                lines[idx] = lines[idx].copy(credit = nv, debit = if (nv != 0L) 0L else lines[idx].debit)
                v = v.copy(lines = lines)
                renderBalance()
            }
            val r = LinearLayout(act)
            r.orientation = LinearLayout.HORIZONTAL
            r.addView(debit.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            r.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 8), ViewGroup.LayoutParams.WRAP_CONTENT))
            r.addView(credit.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
            linesWrap.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
        }
    }

    bs.show(if (isNew) "سند حسابداری دستی" else "ویرایش سند دستی", "سند باید بدهکار = بستانکار باشد (اتاز)") { b ->
        val dField = UI.dateField(act, "تاریخ سند", v.date) {
            dateSheet(act, v.date) { vv -> v = v.copy(date = vv); dLabel.set(J.jFa(vv)) }
        }
        dLabel.bind(dField, J.jFa(v.date))
        val noBox = UI.field(act, "شمارهٔ سند", J.faN(v.no), center = true)
        noBox.second.isEnabled = false
        val r1 = LinearLayout(act)
        r1.orientation = LinearLayout.HORIZONTAL
        r1.addView(dField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r1.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r1.addView(noBox.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r1)

        val desc = UI.field(act, "شرح سند", v.desc, hint = "مثلاً هزینهٔ برق آبان")
        hookText(desc.first) { v2 -> v = v.copy(desc = v2) }
        b.addView(desc.first)

        val linesCard = UI.innerCard(act, T.cardSoft, 16, 12)
        val addBtn = UI.text(act, "+ آرتیکل", 11, Color.WHITE, true)
        addBtn.background = UI.rounded(act, T.primary, 12)
        addBtn.setPadding(UI.dp(act, 12), UI.dp(act, 6), UI.dp(act, 12), UI.dp(act, 7))
        val head = LinearLayout(act)
        head.orientation = LinearLayout.HORIZONTAL
        head.addView(UI.text(act, "آرتیکل‌های سند", 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(addBtn)
        linesCard.addView(head)
        linesCard.addView(linesWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        linesCard.addView(balanceT, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10); it.gravity = Gravity.CENTER })
        b.addView(linesCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
        addBtn.setOnClickListener {
            v = v.copy(lines = v.lines + VoucherLine("1101", "", 0, 0))
            renderLines()
        }
        renderLines(); renderBalance()

        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ثبت سند") {
                if (v.desc.trim().isEmpty()) { UI.toast(act, "شرح سند را بنویسید"); return@primaryBtn }
                if (v.lines.size < 2) { UI.toast(act, "سند حسابداری حداقل باید دو ردیف داشته باشد"); return@primaryBtn }
                if (v.lines.any { it.accountCode !in ACCOUNTS.map { a -> a.code } }) { UI.toast(act, "سرفصل نامعتبر در سند وجود دارد"); return@primaryBtn }
                if (v.lines.any { it.debit < 0L || it.credit < 0L || (it.debit > 0L && it.credit > 0L) || (it.debit == 0L && it.credit == 0L) }) { UI.toast(act, "هر ردیف باید فقط یک مبلغ مثبت در ستون بدهکار یا بستانکار داشته باشد"); return@primaryBtn }
                if (v.lines.any { (it.accountCode == "1101" || it.accountCode == "1102") && it.detailId.isBlank() }) { UI.toast(act, "برای ردیف صندوق/بانک، صندوق یا حساب بانکی مشخص کنید"); return@primaryBtn }
                if (v.lines.any { (it.accountCode == "1103" || it.accountCode == "2101") && it.detailId.isBlank() }) { UI.toast(act, "برای ردیف دریافتنی/پرداختنی، طرف‌حساب مشخص کنید"); return@primaryBtn }
                val sumD = v.lines.sumOf { it.debit }
                val sumC = v.lines.sumOf { it.credit }
                if (sumD != sumC || sumD == 0L) { UI.toast(act, "سند تراز نیست — بدهکار " + J.moneyFa(sumD) + " ≠ بستانکار " + J.moneyFa(sumC)); return@primaryBtn }
                val exists = Db.db.vouchers.any { it.id == v.id }
                Db.update { d -> d.copy(vouchers = if (exists) d.vouchers.map { if (it.id == v.id) v else it } else listOf(v) + d.vouchers) }
                bs.close(); act.refresh()
                UI.toast(act, (if (exists) "سند ویرایش شد" else "سند حسابداری ثبت شد") + " — " + J.moneyFa(sumD) + " تومان، تراز است")
            }))
    }
}

/* ═════════════════════════ گزارش‌ها ═════════════════════════ */

fun reportsScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    val views = listOf(
        Triple("سود و زیان", -1, "pnl"), Triple("تراز آزمایشی", -1, "trial"), Triple("ترازنامه", -1, "sheet"),
        Triple("گردش کالا", -1, "items"), Triple("گردش طرف‌حساب", -1, "partners"), Triple("مالیات", -1, "vat"), Triple("انبار", -1, "stock")
    )
    val selIdx = views.indexOfFirst { it.third == act.reportTab }.coerceAtLeast(0)
    root.addView(UI.segScroll(c, views, selIdx) { i ->
        act.reportTab = views[i].third
        act.refresh()
    })

    val body = LinearLayout(c)
    root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
    val view = act.reportTab

    if (view == "pnl") {
        val pl = Db.profitLoss(db)
        body.addView(UI.grid2(c,
            UI.statCard(c, "درآمد فروش", J.moneyShort(pl.income), "تومان", "green", R.drawable.ic_trend_up),
            UI.statCard(c, "هزینه‌ها", J.moneyShort(pl.expenses), "تومان", "rose", R.drawable.ic_trend_down)))
        val card = UI.card(c)
        card.addView(UI.sectionTitle(c, "صورت سود و زیان", "از روی اسناد حسابداری واقعی", R.drawable.ic_chart, topMargin = 0))
        val incomeRow = UI.innerCard(c, 0x14059669.toInt(), 14, 12)
        incomeRow.orientation = LinearLayout.HORIZONTAL
        incomeRow.addView(UI.text(c, "درآمد فروش", 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        incomeRow.addView(UI.text(c, J.moneyFa(pl.income), 12, T.primary, true))
        card.addView(incomeRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        if (pl.returns > 0) {
            val rr = UI.innerCard(c, T.amberSoft, 14, 12)
            rr.orientation = LinearLayout.HORIZONTAL
            rr.addView(UI.text(c, "برگشت از فروش", 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            rr.addView(UI.text(c, "−" + J.moneyFa(pl.returns), 12, T.amber, true))
            card.addView(rr, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
        }
        for (r in pl.rows.filter { it.kind == "expense" && it.balance > 0 }) {
            val rr = UI.innerCard(c, T.cardSoft, 14, 10)
            rr.orientation = LinearLayout.HORIZONTAL
            rr.addView(UI.text(c, r.name, 11.5f, T.sub), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            rr.addView(UI.text(c, "−" + J.moneyFa(r.balance), 11.5f, T.rose, true))
            card.addView(rr, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 5) })
        }
        val net = UI.innerCard(c, T.primary, 16, 13)
        net.background = UI.gradient(c, T.primary, T.primaryD, 16)
        net.orientation = LinearLayout.HORIZONTAL
        net.gravity = Gravity.CENTER_VERTICAL
        net.addView(UI.text(c, "سود خالص دوره", 13, Color.WHITE, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        net.addView(UI.text(c, J.moneyFa(pl.net) + " تومان", 15, Color.WHITE, true))
        card.addView(net, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
        body.addView(card)
    }

    if (view == "trial") {
        val tb = Db.trialBalance(db)
        val debits = tb.sumOf { it.debit }
        val credits = tb.sumOf { it.credit }
        val head = UI.card(c)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.addView(UI.text(c, "جمع بدهکار", 11, T.sub, true))
        l.addView(UI.text(c, J.moneyFa(debits), 14, T.text, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
        head.addView(l, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(UI.icon(c, R.drawable.ic_scale, 24, if (Math.abs(debits - credits) < 2) T.primary else T.rose))
        val r = LinearLayout(c)
        r.orientation = LinearLayout.VERTICAL
        r.gravity = Gravity.START
        r.addView(UI.text(c, "جمع بستانکار", 11, T.sub, true))
        r.addView(UI.text(c, J.moneyFa(credits), 14, T.text, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
        head.addView(r, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        body.addView(head)
        val chip = UI.chip(c, if (Math.abs(debits - credits) < 2) "سند و دفاتر کاملاً تراز است" else "اختلاف تراز — بررسی کنید", if (Math.abs(debits - credits) < 2) "green" else "rose")
        val chipWrap = LinearLayout(c)
        chipWrap.addView(chip)
        body.addView(chipWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        val card = UI.card(c)
        val header = LinearLayout(c)
        header.orientation = LinearLayout.HORIZONTAL
        header.background = UI.rounded(c, T.cardSoft, 10)
        header.setPadding(UI.dp(c, 10), UI.dp(c, 8), UI.dp(c, 10), UI.dp(c, 8))
        for (lbl in listOf(Pair("سرفصل", 1.4f), Pair("بدهکار", 1f), Pair("بستانکار", 1f), Pair("مانده", 1f))) {
            val t = UI.text(c, lbl.first, 10, T.sub, true)
            t.gravity = Gravity.CENTER
            header.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, lbl.second))
        }
        card.addView(header)
        for (r0 in tb.filter { it.debit != 0L || it.credit != 0L }) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(UI.dp(c, 10), UI.dp(c, 9), UI.dp(c, 10), UI.dp(c, 9))
            val cells = listOf(
                Pair(r0.name, false), Pair(if (r0.debit != 0L) J.moneyShort(r0.debit) else "—", true),
                Pair(if (r0.credit != 0L) J.moneyShort(r0.credit) else "—", true), Pair(J.moneyShort(r0.balance), true))
            for ((i, cell) in cells.withIndex()) {
                val t = UI.text(c, cell.first, 10.5f, if (i == 0) T.text else (if (i == 3) T.text else T.sub), i == 0 || i == 3)
                t.gravity = if (i == 0) Gravity.START else Gravity.CENTER
                t.isSingleLine = false
                row.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, if (i == 0) 1.4f else 1f))
            }
            card.addView(row)
        }
        body.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
    }

    if (view == "sheet") {
        val bs = Db.balanceSheet(db)
        val assetsCard = UI.card(c)
        assetsCard.addView(UI.sectionTitle(c, "دارایی‌ها", "جمع: " + J.moneyShort(bs.assetsSum) + " تومان", R.drawable.ic_scale, topMargin = 0))
        for (a in bs.assets.filter { it.balance != 0L }) {
            val row = UI.innerCard(c, T.cardSoft, 14, 10)
            row.orientation = LinearLayout.HORIZONTAL
            row.addView(UI.text(c, a.name, 11.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(UI.text(c, J.moneyFa(a.balance), 11.5f, T.sky, true))
            assetsCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
        }
        if (bs.assets.none { it.balance != 0L }) {
            assetsCard.addView(UI.text(c, "دارایی ثبت نشده", 11, T.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
        body.addView(assetsCard)
        val liabCard = UI.card(c)
        liabCard.addView(UI.sectionTitle(c, "بدهی‌ها", "جمع: " + J.moneyShort(bs.liabSum) + " تومان", R.drawable.ic_percent, topMargin = 0))
        for (l in bs.liab.filter { it.balance != 0L }) {
            val row = UI.innerCard(c, T.cardSoft, 14, 10)
            row.orientation = LinearLayout.HORIZONTAL
            row.addView(UI.text(c, l.name, 11.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(UI.text(c, J.moneyFa(l.balance), 11.5f, T.rose, true))
            liabCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
        }
        if (bs.liab.none { it.balance != 0L }) {
            liabCard.addView(UI.text(c, "بدهی ثبت نشده", 11, T.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
        body.addView(liabCard)
        val eqCard = UI.card(c)
        eqCard.addView(UI.sectionTitle(c, "حقوق صاحبان سرمایه", "سرمایه + سود دوره", R.drawable.ic_sheet, topMargin = 0))
        for (e in bs.equity.filter { it.balance != 0L }) {
            val row = UI.innerCard(c, T.cardSoft, 14, 10)
            row.orientation = LinearLayout.HORIZONTAL
            row.addView(UI.text(c, e.name, 11.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(UI.text(c, J.moneyFa(e.balance), 11.5f, T.text, true))
            eqCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
        }
        if (bs.pl.net > 0) {
            val row = UI.innerCard(c, 0x14059669.toInt(), 14, 10)
            row.orientation = LinearLayout.HORIZONTAL
            row.addView(UI.text(c, "سود خالص دوره", 11.5f, T.primary, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(UI.text(c, J.moneyFa(bs.pl.net), 11.5f, T.primary, true))
            eqCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
        }
        val sum = UI.innerCard(c, T.sky, 16, 13)
        sum.background = UI.gradient(c, 0xFF0284C7.toInt(), 0xFF1D4ED8.toInt(), 16)
        sum.orientation = LinearLayout.HORIZONTAL
        sum.gravity = Gravity.CENTER_VERTICAL
        sum.addView(UI.text(c, "جمع بدهی‌ها + سرمایه", 12.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        sum.addView(UI.text(c, J.moneyFa(bs.liabSum + bs.equitySum), 13.5f, Color.WHITE, true))
        eqCard.addView(sum, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        body.addView(eqCard)
    }

    if (view == "items") {
        val byItem = Db.salesByItem(db)
        val byPurchase = Db.salesByItem(db, K_PURCHASE)
        val bestCard = UI.card(c)
        bestCard.addView(UI.sectionTitle(c, "پرفروش‌ترین کالاها", "بر اساس مبلغ فروش خالص", R.drawable.ic_bag, topMargin = 0))
        if (byItem.isEmpty()) {
            bestCard.addView(UI.empty(c, "فروشی ثبت نشده", "", R.drawable.ic_bag))
        } else {
            bestCard.addView(MiniBarsView(c, byItem.take(6).map { Triple((it.item.name.split(" ").firstOrNull() ?: it.item.name), it.total, false) }, 110), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
            for ((i, x) in byItem.take(8).withIndex()) {
                val row = UI.innerCard(c, T.cardSoft, 14, 10)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.addView(UI.text(c, J.faN(i + 1) + ". " + x.item.name, 11.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val end = LinearLayout(c)
                end.orientation = LinearLayout.VERTICAL
                end.gravity = Gravity.START
                end.addView(UI.text(c, J.moneyShort(x.total), 11.5f, T.text, true))
                end.addView(UI.text(c, J.qty(x.qty) + " " + x.item.unit, 9, T.sub))
                row.addView(end)
                bestCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
            }
        }
        body.addView(bestCard)
        val buyCard = UI.card(c)
        buyCard.addView(UI.sectionTitle(c, "بیشترین خریدها", "از تامین‌کنندگان", R.drawable.ic_pkg, topMargin = 0))
        if (byPurchase.isEmpty()) {
            buyCard.addView(UI.empty(c, "خریدی ثبت نشده", "", R.drawable.ic_pkg))
        } else {
            for ((i, x) in byPurchase.take(6).withIndex()) {
                val row = UI.innerCard(c, T.cardSoft, 14, 10)
                row.orientation = LinearLayout.HORIZONTAL
                row.addView(UI.text(c, J.faN(i + 1) + ". " + x.item.name, 11.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(UI.text(c, J.moneyShort(x.total), 11.5f, T.text, true))
                buyCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
            }
        }
        body.addView(buyCard)
    }

    if (view == "partners") {
        body.addView(UI.sectionTitle(c, "گردش طرف‌حساب‌ها", "ماندهٔ هر مشتری و تامین‌کننده از روی اسناد", R.drawable.ic_users))
        val card = UI.card(c)
        if (db.partners.isEmpty()) {
            card.addView(UI.empty(c, "طرف‌حسابی نیست", "", R.drawable.ic_users))
        } else {
            for (x in db.partners.map { Pair(it, Db.partnerBalance(db, it.id)) }.sortedByDescending { Math.abs(it.second.net) }) {
                val row = UI.listRow(c, x.first.name,
                    if (x.first.type == "supplier") "تامین‌کننده" else "مشتری",
                    UI.endCol(c, if (x.second.net == 0L) "تسویه" else J.moneyFa(Math.abs(x.second.net)),
                        if (x.second.net > 0) "بدهکار" else if (x.second.net < 0) "بستانکار" else "",
                        if (x.second.net > 0) T.amber else if (x.second.net < 0) T.rose else T.sub, 13),
                    null, icRes = R.drawable.ic_users, chev = false)
                card.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
            }
        }
        body.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
    }

    if (view == "vat") {
        val vatSales = db.invoices.filter { it.kind == K_SALE && it.final }.sumOf { Db.invoiceTotals(it).vat }
        val vatBuy = db.invoices.filter { it.kind == K_PURCHASE && it.final }.sumOf { Db.invoiceTotals(it).vat }
        body.addView(UI.grid2(c,
            UI.statCard(c, "مالیات فروش (بستانکار)", J.moneyShort(vatSales), "تومان — قابل پرداخت", "green", R.drawable.ic_percent),
            UI.statCard(c, "مالیات خرید (بدهکار)", J.moneyShort(vatBuy), "تومان — اعتبار مالیاتی", "sky", R.drawable.ic_percent)))
        val card = UI.card(c)
        card.addView(UI.sectionTitle(c, "مالیات بر ارزش افزوده", "خلاصه برای اظهارنامهٔ دوره", R.drawable.ic_percent, topMargin = 0))
        val r1 = UI.innerCard(c, T.cardSoft, 14, 12)
        r1.orientation = LinearLayout.HORIZONTAL
        r1.addView(UI.text(c, "مالیات فروش‌های نهایی", 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r1.addView(UI.text(c, J.moneyFa(vatSales), 12, T.text, true))
        card.addView(r1, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        val r2 = UI.innerCard(c, T.cardSoft, 14, 12)
        r2.orientation = LinearLayout.HORIZONTAL
        r2.addView(UI.text(c, "مالیات خریدها (قابل کسر)", 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r2.addView(UI.text(c, "−" + J.moneyFa(vatBuy), 12, T.text, true))
        card.addView(r2, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
        val r3 = UI.innerCard(c, T.primary, 16, 13)
        r3.background = UI.gradient(c, T.primary, T.primaryD, 16)
        r3.orientation = LinearLayout.HORIZONTAL
        r3.gravity = Gravity.CENTER_VERTICAL
        r3.addView(UI.text(c, "مالیات پرداختنی دوره", 12.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        r3.addView(UI.text(c, J.moneyFa(Math.max(0L, vatSales - vatBuy)) + " تومان", 14, Color.WHITE, true))
        card.addView(r3, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        body.addView(card)
        val note = UI.card(c)
        note.orientation = LinearLayout.HORIZONTAL
        note.addView(UI.icon(c, R.drawable.ic_percent, 18, T.primary))
        val t = UI.text(c, "نرخ مالیات پیش‌فرض در تنظیمات " + J.fa(db.settings.vatRate.toString()) + "٪ است و هنگام ثبت فاکتور می‌توانید برای هر فاکتور تغییر دهید." + (if (db.settings.moudian) " ارسال صورتحساب به سامانهٔ مودیان هم فعال است." else ""), 11.5f, T.sub)
        t.setLineSpacing(0f, 1.2f)
        val tl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        tl.marginStart = UI.dp(c, 10)
        note.addView(t, tl)
        body.addView(note, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
    }

    if (view == "stock") {
        val lows = Db.lowStockItems(db)
        val lowCard = UI.card(c)
        lowCard.addView(UI.sectionTitle(c, "کالاهای کم‌موجود", "زیر حد هشدار — وقت سفارش است", R.drawable.ic_boxes, topMargin = 0))
        if (lows.isEmpty()) {
            lowCard.addView(UI.empty(c, "همه چیز کافی است", "هیچ کالایی زیر حد هشدار نیست", R.drawable.ic_pkg))
        } else {
            for (it0 in lows) {
                val row = UI.innerCard(c, T.amberSoft, 14, 12)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                row.addView(UI.text(c, it0.name, 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(UI.text(c, J.qty(Db.stockOf(db, it0.id)) + " / " + J.qty(it0.minStock), 11.5f, T.amber, true))
                lowCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
            }
        }
        body.addView(lowCard)
        val whCard = UI.card(c)
        whCard.addView(UI.sectionTitle(c, "موجودی به تفکیک انبار", "ارزش ریالی موجودی هر انبار", R.drawable.ic_boxes, topMargin = 0))
        for (w in db.warehouses) {
            val row = UI.innerCard(c, T.cardSoft, 14, 12)
            row.orientation = LinearLayout.HORIZONTAL
            row.addView(UI.text(c, w.name, 11.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(UI.text(c, J.moneyShort(db.items.sumOf { Db.stockOf(db, it.id, w.id) * it.cost }) + " ت", 11.5f, T.text, true))
            whCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
        }
        val total = UI.innerCard(c, T.violet, 16, 13)
        total.background = UI.gradient(c, 0xFF7C3AED.toInt(), 0xFF9333EA.toInt(), 16)
        total.orientation = LinearLayout.HORIZONTAL
        total.gravity = Gravity.CENTER_VERTICAL
        total.addView(UI.text(c, "جمع ارزش انبارها", 12.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        total.addView(UI.text(c, J.moneyShort(db.items.sumOf { Db.stockOf(db, it.id) * it.cost }) + " تومان", 13.5f, Color.WHITE, true))
        whCard.addView(total, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        body.addView(whCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
    }

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}
