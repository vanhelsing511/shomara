package ir.shomara.app

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * ماژول فروش — فاکتور فروش، خرید، پیش‌فاکتور و برگشت‌ها با ویرایشگر کامل اقلام
 * + فاکتور تکرارشونده (مثل Zoho Books / FreshBooks)
 */

private val KIND_TABS = listOf(
    Triple("فروش", K_SALE, 0), Triple("خرید", K_PURCHASE, 1), Triple("پیش‌فاکتور", K_PROFORMA, 2),
    Triple("برگشت فروش", K_SALE_RET, 3), Triple("برگشت خرید", K_PURCHASE_RET, 4), Triple("تکرارشونده", "repeat", 5)
)

fun invoicesScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    val kind = act.invTab
    if (kind == "repeat") {
        root.addView(recurringPanel(act))
        return ScrollView(c).apply { isVerticalScrollBarEnabled = false; isFillViewport = true; addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)) }
    }
    val kindInvoices = db.invoices.filter { it.kind == kind }
    val kindTotal = kindInvoices.filter { it.final }.sumOf { Db.invoiceTotals(it).total }
    val q = act.invQuery

    // تب‌ها با تعداد
    val tabs = KIND_TABS.map { t ->
        Triple(t.first, if (t.second == "repeat") db.recurring.count { it.active } else db.invoices.count { it.kind == t.second }, t.second)
    }
    val selIdx = KIND_TABS.indexOfFirst { it.second == kind }
    root.addView(UI.segScroll(c, tabs, selIdx) { i ->
        act.invTab = KIND_TABS[i].second
        act.invQuery = ""
        act.refresh()
    })

    // خلاصه
    val summary = UI.card(c)
    summary.orientation = LinearLayout.HORIZONTAL
    val left = LinearLayout(c)
    left.orientation = LinearLayout.VERTICAL
    left.addView(UI.text(c, "جمع " + LB.invoiceKindShort(kind) + "های نهایی", 11, T.sub, true))
    val totalT = UI.text(c, J.moneyShort(kindTotal) + " تومان", 17, T.text, true)
    totalT.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 5) }
    left.addView(totalT)
    summary.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    val right = LinearLayout(c)
    right.orientation = LinearLayout.VERTICAL
    right.gravity = Gravity.START
    right.addView(UI.text(c, J.faN(kindInvoices.size) + " فاکتور", 12, T.text, true))
    right.addView(UI.text(c, J.faN(kindInvoices.count { it.final }) + " نهایی • " + J.faN(kindInvoices.count { !it.final }) + " پیش‌نویس", 10, T.sub))
    summary.addView(right)
    root.addView(summary)

    // جستجو — زنده، مثل نسخهٔ وب
    val (sbox, se) = UI.search(c, "جستجو با شماره یا نام طرف‌حساب…")
    se.setText(q)
    root.addView(sbox)

    // لیست فاکتورها — با هر بار تایپ دوباره ساخته می‌شود
    val list = LinearLayout(c)
    root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
    fun renderList() {
        val qq = act.invQuery
        list.removeAllViews()
        val filtered = kindInvoices
            .filter {
                if (qq.isEmpty()) true
                else it.no.toString().contains(qq) || (db.partners.firstOrNull { p -> p.id == it.partnerId }?.name ?: "").contains(qq)
            }
            .sortedByDescending { it.date }
        if (filtered.isEmpty()) {
            val em = if (qq.isEmpty()) UI.empty(c, LB.invoiceKindLabel(kind) + "ی ثبت نشده", "با دکمهٔ سبز اولین فاکتور را بزنید", R.drawable.ic_receipt)
                     else UI.empty(c, "چیزی پیدا نشد", "عبارت دیگری را امتحان کنید", R.drawable.ic_search)
            list.addView(em, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 20) })
        } else {
            for (inv in filtered) {
                val p = db.partners.firstOrNull { x -> x.id == inv.partnerId }
                val t = Db.invoiceTotals(inv)
                val ic = when (inv.kind) {
                    K_SALE -> R.drawable.ic_bag to T.primary
                    K_PURCHASE -> R.drawable.ic_doc to T.sky
                    K_PROFORMA -> R.drawable.ic_file_sign to T.violet
                    else -> R.drawable.ic_undo to T.amber
                }
                val badge = when {
                    inv.final -> UI.chip(c, "نهایی", "green")
                    inv.kind == K_PROFORMA -> UI.chip(c, "پیش‌فاکتور", "violet")
                    else -> UI.chip(c, "پیش‌نویس", "amber")
                }
                val endSub = if (inv.final) (if (t.due > 0) "مانده " + J.moneyShort(t.due) else "تسویه کامل") else "پیش‌نویس"
                val row = UI.listRow(c,
                    LB.invoiceKindShort(inv.kind) + " شمارهٔ " + J.faN(inv.no),
                    (p?.name ?: "—") + " • " + J.jFa(inv.date) + " • " + J.faN(inv.lines.size) + " قلم",
                    UI.endCol(c, J.moneyFa(t.total), endSub),
                    badge, icRes = ic.first, icTint = ic.second,
                    click = { invoiceViewSheet(act, inv) })
                list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
            }
        }
    }
    se.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, cc: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, cc: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { act.invQuery = s?.toString() ?: ""; renderList() }
    })
    renderList()

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

/** کمکی برای تغییر متن برچسب انتخاب‌گر */
class TextViewHelper(val parent: LinearLayout) {
    fun set(s: String) {
        for (i in parent.childCount - 1 downTo 0) {
            val v = parent.getChildAt(i)
            if (v is LinearLayout) {
                for (j in 0 until v.childCount) {
                    val w = v.getChildAt(j)
                    if (w is TextView) { w.text = s; return }
                }
            } else if (v is TextView && v.id == -1) { return }
        }
    }
}

/** پیدا کردن امن EditText در هر عمق — هرگز کرش نمی‌دهد */
private fun findEditorDeep(v: View): EditText? {
    if (v is EditText) return v
    if (v is ViewGroup) {
        for (i in 0 until v.childCount) findEditorDeep(v.getChildAt(i))?.let { return it }
    }
    return null
}

fun findEditor(box: LinearLayout): EditText = findEditorDeep(box) ?: EditText(box.context)

fun hookNum(box: LinearLayout, onChange: (Long) -> Unit) {
    val e = findEditor(box)
    e.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) {
            val digits = s?.filter { it in '۰'..'۹' || it in '0'..'9' }?.map { ch -> if (ch in '۰'..'۹') ('0' + (ch - '۰')) else ch }?.joinToString("") ?: ""
            onChange(if (digits.isEmpty()) 0L else digits.toLongOrNull() ?: 0L)
        }
    })
}

fun hookText(box: LinearLayout, onChange: (String) -> Unit) {
    val e = findEditor(box)
    e.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { onChange(s?.toString() ?: "") }
    })
}

/* ═════════════════════════ ویرایشگر فاکتور ═════════════════════════ */

fun invoiceEditorSheet(act: MainActivity, inv0: Invoice) {
    val bs = BottomSheet(act)
    var inv = inv0
    val isNew = !Db.db.invoices.any { it.id == inv.id }
    val isReturn = inv.kind == K_SALE_RET || inv.kind == K_PURCHASE_RET
    val isPurchase = inv.kind == K_PURCHASE || inv.kind == K_PURCHASE_RET

    bs.show(if (isNew) LB.invoiceKindLabel(inv.kind) + " جدید" else "ویرایش " + LB.invoiceKindLabel(inv.kind),
        "شمارهٔ " + J.faN(inv.no) + " — اطلاعات را کامل و نهایی کنید") { b ->

        val partnerOptions = Db.db.partners.filter { p ->
            if (isPurchase) p.type != "customer" else p.type != "supplier"
        }

        // ---------- کارت جمع کل (تعریف اول، استفاده بعد) ----------
        val totalsCard = UI.innerCard(act, T.primary, 18, 14)
        totalsCard.background = UI.gradient(act, T.primary, T.primaryD, 18)
        fun totalRow(label: String): Pair<TextView, TextView> {
            val row = LinearLayout(act)
            row.orientation = LinearLayout.HORIZONTAL
            val l = UI.text(act, label, 12, 0xCCECFDF5.toInt())
            val v = UI.text(act, "", 12, Color.WHITE, true)
            row.addView(l, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(v)
            totalsCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
            return Pair(l, v)
        }
        val rGross = totalRow("جمع کل اقلام")
        val rLineDisc = totalRow("تخفیف اقلام")
        val rDisc = totalRow("تخفیف کلی")
        val rVat = totalRow("مالیات ارزش افزوده")
        val rNet = totalRow("مبلغ نهایی")
        val rDue = totalRow("ماندهٔ بدهی")
        fun renderTotals() {
            val t = Db.invoiceTotals(inv)
            rVat.first.text = "مالیات ارزش افزوده (" + J.faN(inv.vatRate) + "٪)"
            rGross.second.text = J.moneyFa(t.gross)
            rLineDisc.second.text = "−" + J.moneyFa(Math.round(t.lineDisc))
            rLineDisc.first.visibility = if (t.lineDisc > 0) View.VISIBLE else View.GONE
            rLineDisc.second.visibility = if (t.lineDisc > 0) View.VISIBLE else View.GONE
            rDisc.second.text = "−" + J.moneyFa(inv.discount)
            rDisc.first.visibility = if (inv.discount > 0) View.VISIBLE else View.GONE
            rDisc.second.visibility = if (inv.discount > 0) View.VISIBLE else View.GONE
            rVat.second.text = J.moneyFa(t.vat) + " (" + J.fa(inv.vatRate.toString()) + "٪)"
            rNet.second.text = J.moneyFa(t.total) + " تومان"
            rNet.first.textSize = 14f; rNet.second.textSize = 14f
            rDue.second.text = J.moneyFa(t.due)
            val showDue = !isReturn && inv.paid > 0
            rDue.first.visibility = if (showDue) View.VISIBLE else View.GONE
            rDue.second.visibility = if (showDue) View.VISIBLE else View.GONE
        }

        // ---------- طرف‌حساب + انبار ----------
        val pRow = LinearLayout(act)
        pRow.orientation = LinearLayout.HORIZONTAL
        val ph1 = ValueLabel()
        val pSel = UI.selectField(act, "طرف‌حساب", ph1.current) {
            optionSheet(act, "طرف‌حساب", partnerOptions.map { Opt(it.id, it.name) }, inv.partnerId) { o ->
                inv = inv.copy(partnerId = o.id)
                ph1.set(o.label)
                renderTotals()
            }
        }
        ph1.bind(pSel, Db.db.partners.firstOrNull { it.id == inv.partnerId }?.name ?: "انتخاب کنید")
        val wLabel = ValueLabel()
        val wSel = UI.selectField(act, "انبار", wLabel.current) {
            optionSheet(act, "انبار", Db.db.warehouses.map { Opt(it.id, it.name, it.loc) }, inv.warehouseId) { o ->
                inv = inv.copy(warehouseId = o.id)
                wLabel.set(o.label)
            }
        }
        wLabel.bind(wSel, Db.db.warehouses.firstOrNull { it.id == inv.warehouseId }?.name ?: "انتخاب کنید")
        pRow.addView(pSel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        pRow.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        pRow.addView(wSel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(pRow)
        if (isPurchase) {
            val supplierNo = UI.field(act, "شماره فاکتور فروشنده (اختیاری)", inv.supplierInvoiceNo, hint = "شماره روی فاکتور فروشنده", center = true)
            hookText(supplierNo.first) { value -> inv = inv.copy(supplierInvoiceNo = value.trim()) }
            b.addView(supplierNo.first)
        }

        // ---------- تاریخ + مالیات ----------
        val dLabel = ValueLabel()
        val dField = UI.dateField(act, "تاریخ", inv.date) {
            dateSheet(act, inv.date) { v -> inv = inv.copy(date = v); dLabel.set(J.jFa(v)) }
        }
        dLabel.bind(dField, J.jFa(inv.date))
        val vatBox = UI.field(act, "درصد مالیات ارزش افزوده", J.fa(inv.vatRate.toString()), num = true, ltr = true, center = true)
        hookNum(vatBox.first) { v -> inv = inv.copy(vatRate = v); renderTotals() }
        val dRow = LinearLayout(act)
        dRow.orientation = LinearLayout.HORIZONTAL
        dRow.addView(dField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        dRow.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        dRow.addView(vatBox.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(dRow)

        // ---------- اقلام ----------
        val linesCard = UI.innerCard(act, T.cardSoft, 16, 12)
        val addBtn = UI.text(act, "+ افزودن کالا", 11, Color.WHITE, true)
        addBtn.background = UI.rounded(act, T.primary, 12)
        addBtn.setPadding(UI.dp(act, 12), UI.dp(act, 6), UI.dp(act, 12), UI.dp(act, 7))
        val linesHead = LinearLayout(act)
        linesHead.orientation = LinearLayout.HORIZONTAL
        linesHead.addView(UI.text(act, "اقلام " + LB.invoiceKindLabel(inv.kind), 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        linesHead.addView(addBtn)
        linesCard.addView(linesHead)
        val linesWrap = LinearLayout(act)
        linesWrap.orientation = LinearLayout.VERTICAL
        linesCard.addView(linesWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        b.addView(linesCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })

        fun renderLines() {
            linesWrap.removeAllViews()
            if (inv.lines.isEmpty()) {
                val t = UI.text(act, "هنوز قلمی اضافه نشده — دکمهٔ «افزودن کالا» را بزنید", 11, T.sub)
                t.gravity = Gravity.CENTER
                t.setPadding(0, UI.dp(act, 12), 0, UI.dp(act, 12))
                linesWrap.addView(t)
            } else {
                for ((idx, l) in inv.lines.withIndex()) {
                    val it0 = Db.db.items.firstOrNull { i -> i.id == l.itemId }
                    val card = UI.innerCard(act, T.card, 14, 11)
                    val headL = LinearLayout(act)
                    headL.orientation = LinearLayout.HORIZONTAL
                    headL.addView(UI.text(act, it0?.name ?: "—", 12.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    val x = UI.icon(act, R.drawable.ic_close, 15, T.rose)
                    x.setOnClickListener {
                        inv = inv.copy(lines = inv.lines.filterIndexed { i, _ -> i != idx })
                        renderLines(); renderTotals()
                    }
                    headL.addView(x)
                    card.addView(headL)
                    val sumT = UI.text(act, "جمع قلم: " + J.moneyFa(Math.round(l.qty * (l.price - l.discount))), 11, T.primary, true)
                    sumT.gravity = Gravity.START
                    fun updLine(mut: (MutableList<InvoiceLine>) -> Unit) {
                        val lines = inv.lines.toMutableList()
                        if (idx >= lines.size) return
                        mut(lines)
                        inv = inv.copy(lines = lines)
                        sumT.text = "جمع قلم: " + J.moneyFa(Math.round(lines[idx].qty * (lines[idx].price - lines[idx].discount)))
                        renderTotals()
                    }
                    val qty = UI.qtyStepper(act, l.qty) { v -> updLine { ls -> ls[idx] = ls[idx].copy(qty = Math.max(0.0, v)) } }
                    val priceBox = UI.field(act, "قیمت", J.moneyFa(l.price), num = true, ltr = true, center = true)
                    val discBox = UI.field(act, "تخفیف", if (l.discount != 0.0) J.moneyFa(l.discount.toLong()) else "", num = true, ltr = true, center = true)
                    hookNum(priceBox.first) { v -> updLine { ls -> ls[idx] = ls[idx].copy(price = v) } }
                    hookNum(discBox.first) { v -> updLine { ls -> ls[idx] = ls[idx].copy(discount = v.toDouble()) } }
                    val ctl1 = LinearLayout(act)
                    ctl1.orientation = LinearLayout.HORIZONTAL
                    ctl1.gravity = Gravity.CENTER_VERTICAL
                    ctl1.addView(qty)
                    val pc = LinearLayout(act)
                    pc.orientation = LinearLayout.VERTICAL
                    pc.addView(priceBox.first)
                    pc.addView(discBox.first, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
                    ctl1.addView(pc, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(act, 10) })
                    card.addView(ctl1, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
                    card.addView(sumT, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
                    linesWrap.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
                }
            }
        }
        addBtn.setOnClickListener {
            optionSheet(act, "کالا یا خدمت را انتخاب کنید",
                Db.db.items.map { it0 ->
                    val stock = Db.stockOf(Db.db, it0.id, inv.warehouseId)
                    val added = inv.lines.any { it.itemId == it0.id }
                    Opt(it0.id,
                        if (added) it0.name + " ✓" else it0.name,
                        if (added) "از قبل در فاکتور هست"
                        else if (it0.service) "خدمت • " + J.moneyShort(it0.price)
                        else "موجودی " + J.qty(stock) + " • " + J.moneyShort(if (isPurchase) it0.cost else it0.price))
                }, "") { o ->
                    if (inv.lines.any { it.itemId == o.id }) return@optionSheet
                    val it0 = Db.db.items.firstOrNull { i -> i.id == o.id } ?: return@optionSheet
                    inv = inv.copy(lines = inv.lines + InvoiceLine(Db.newId("l"), o.id, 1.0, if (isPurchase) it0.cost else it0.price, 0.0))
                    renderLines(); renderTotals()
                }
        }
        renderLines()

        // ---------- تخفیف کلی + پرداخت نقدی ----------
        val discBox = UI.numField(act, "تخفیف کلی (تومان)", inv.discount)
        hookNum(discBox) { v -> inv = inv.copy(discount = v); renderTotals() }
        val bankLabel = ValueLabel()
        val bankWrap = LinearLayout(act)
        bankWrap.orientation = LinearLayout.VERTICAL
        fun renderBank() {
            bankWrap.removeAllViews()
            if (!isReturn && inv.paid > 0) {
                val sel = UI.selectField(act, "صندوق / بانک پرداخت نقدی", bankLabel.current) {
                    optionSheet(act, "صندوق / بانک", Db.db.banks.map { Opt(it.id, it.name, LB.bankKindLabel(it.kind)) }, inv.payBankId) { o ->
                        inv = inv.copy(payBankId = o.id)
                        bankLabel.set(o.label)
                    }
                }
                bankLabel.bind(sel, Db.db.banks.firstOrNull { it.id == inv.payBankId }?.name ?: "انتخاب کنید")
                bankWrap.addView(sel)
            }
        }
        val paidBox = UI.numField(act, "نقدی پرداخت‌شده", inv.paid)
        hookNum(paidBox) { v -> inv = inv.copy(paid = v); renderTotals(); renderBank() }
        val dRow2 = LinearLayout(act)
        dRow2.orientation = LinearLayout.HORIZONTAL
        if (isReturn) {
            dRow2.addView(discBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        } else {
            dRow2.addView(discBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            dRow2.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
            dRow2.addView(paidBox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }
        b.addView(dRow2)

        // صندوق پرداخت
        renderBank()
        b.addView(bankWrap)

        // مودیان
        if (inv.kind == K_SALE) {
            b.addView(UI.switchRow(act, "ارسال به سامانهٔ مودیان", "صورتحساب مالیاتی الکترونیکی", inv.moudian) { v -> inv = inv.copy(moudian = v) })
        }

        // توضیحات — چندخطی مثل نسخهٔ وب
        val desc = UI.fieldMulti(act, "توضیحات فاکتور", inv.desc)
        hookText(desc.first) { v -> inv = inv.copy(desc = v) }
        b.addView(desc.first)

        b.addView(totalsCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
        renderTotals()

        // دکمه‌ها
        val saveDraft = UI.outlineBtn(act, "ذخیرهٔ پیش‌نویس") { bs.close(); saveInvoice(act, inv, false) }
        val finalize = UI.primaryBtn(act, "نهایی و صدور سند", icRes = R.drawable.ic_checkcheck) { bs.close(); saveInvoice(act, inv, true) }
        b.addView(UI.btnRow(act, saveDraft, finalize))
        if (!isNew) {
            val del = UI.outlineBtn(act, "حذف فاکتور", danger = true, icRes = R.drawable.ic_trash) { bs.close(); confirmDeleteInvoice(act, inv) }
            b.addView(del, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        }
    }
}

fun saveInvoice(act: MainActivity, inv: Invoice, finalize: Boolean) {
    if (inv.partnerId.isEmpty()) { UI.toast(act, "طرف‌حساب را انتخاب کنید"); return }
    if (inv.lines.isEmpty()) { UI.toast(act, "حداقل یک قلم کالا اضافه کنید"); return }
    val existing = Db.db.invoices.firstOrNull { it.id == inv.id }
    if (existing?.final == true) { UI.toast(act, "فاکتور نهایی قفل است؛ برای اصلاح، ابتدا مسیر ابطال رسمی لازم است"); return }
    if (inv.lines.any { it.qty <= 0.0 || it.qty.isNaN() || it.qty.isInfinite() || it.price < 0L || it.discount < 0.0 || it.discount > it.price.toDouble() }) {
        UI.toast(act, "مقدار، قیمت یا تخفیف قلم نامعتبر است؛ تخفیف هر واحد نمی‌تواند از قیمت واحد بیشتر باشد"); return
    }
    if (inv.lines.any { line -> Db.db.items.none { item -> item.id == line.itemId } }) { UI.toast(act, "یکی از کالاهای فاکتور دیگر در فهرست کالاها وجود ندارد؛ قلم را اصلاح کنید"); return }
    if (inv.vatRate !in 0L..100L) { UI.toast(act, "درصد مالیات باید بین صفر تا ۱۰۰ باشد"); return }
    val subtotalAfterLineDiscount = Math.round(inv.lines.sumOf { line -> line.qty * (line.price.toDouble() - line.discount) }.coerceAtLeast(0.0))
    if (inv.discount < 0L || inv.discount > subtotalAfterLineDiscount) { UI.toast(act, "تخفیف کلی باید بین صفر و جمع پس از تخفیف اقلام باشد"); return }
    if (inv.paid < 0L || inv.paid > Db.invoiceTotals(inv).total) { UI.toast(act, "مبلغ پرداختی خارج از محدوده فاکتور است؛ پیش‌دریافت را جدا ثبت کنید"); return }
    val partner = Db.db.partners.firstOrNull { it.id == inv.partnerId }
    if (partner == null) { UI.toast(act, "طرف‌حساب انتخاب‌شده وجود ندارد؛ دوباره انتخاب کنید"); return }
    if (isPurchase && partner.type == "customer") { UI.toast(act, "در فاکتور خرید باید فروشنده انتخاب شود"); return }
    if (!isPurchase && partner.type == "supplier") { UI.toast(act, "در فاکتور فروش باید مشتری انتخاب شود"); return }
    if (finalize && inv.lines.any { line -> Db.db.items.firstOrNull { it.id == line.itemId }?.service != true } && Db.db.warehouses.none { it.id == inv.warehouseId }) { UI.toast(act, "برای نهایی‌کردن فاکتور کالا، انبار معتبر انتخاب کنید"); return }
    if (finalize && inv.kind == K_SALE && partner.creditLimit > 0L) {
        val currentReceivable = Math.max(0L, Db.partnerBalance(Db.db, partner.id).net)
        val newReceivable = Math.max(0L, currentReceivable + Db.invoiceTotals(inv).total - inv.paid)
        if (newReceivable > partner.creditLimit) { UI.toast(act, "سقف اعتبار مشتری پر می‌شود — مانده پس از فاکتور " + J.moneyFa(newReceivable) + " از سقف " + J.moneyFa(partner.creditLimit) + " بیشتر است"); return }
    }
    if (finalize && inv.kind == K_SALE) {
        for (line in inv.lines) {
            val item = Db.db.items.firstOrNull { it.id == line.itemId } ?: continue
            if (!item.service) {
                val available = Db.stockOf(Db.db, line.itemId, inv.warehouseId)
                if (line.qty > available) { UI.toast(act, "موجودی کافی نیست: ${item.name} — موجودی ${J.qty(available)}"); return }
            }
        }
    }
    val fixed = inv.copy(final = if (finalize) true else inv.final)
    val exists = Db.db.invoices.any { it.id == fixed.id }
    val saved = Db.update { d ->
        d.copy(invoices = if (exists) d.invoices.map { if (it.id == fixed.id) fixed else it } else listOf(fixed) + d.invoices)
    }
    if (!saved) return
    act.refresh()
    val t = Db.invoiceTotals(fixed)
    UI.toast(act, if (finalize) LB.invoiceKindLabel(fixed.kind) + " نهایی شد — سند خودکار " + J.moneyFa(t.total) + " تومان"
    else (if (exists) "فاکتور ذخیره شد" else LB.invoiceKindLabel(fixed.kind) + " ثبت شد") + " — شمارهٔ " + J.faN(fixed.no))
}

fun confirmDeleteInvoice(act: MainActivity, inv: Invoice) {
    if (inv.final) { UI.toast(act, "فاکتور نهایی قابل حذف نیست؛ از ابطال با سند معکوس استفاده کنید"); return }
    confirmSheet(act, "حذف فاکتور؟", "«" + LB.invoiceKindLabel(inv.kind) + " شمارهٔ " + J.faN(inv.no) + "» حذف می‌شود. اسناد حسابداری مرتبط هم دیگر دیده نمی‌شوند.") {
        Db.update { d -> d.copy(invoices = d.invoices.filter { it.id != inv.id }) }
        act.refresh()
        UI.toast(act, "فاکتور حذف شد")
    }
}

fun toggleFinal(act: MainActivity, inv: Invoice) {
    if (inv.final) { UI.toast(act, "فاکتور نهایی قابل برگشت به پیش‌نویس نیست؛ ابطال رسمی لازم است"); return }
    if (inv.kind == K_SALE) {
        for (line in inv.lines) {
            val item = Db.db.items.firstOrNull { it.id == line.itemId } ?: continue
            if (!item.service && line.qty > Db.stockOf(Db.db, line.itemId, inv.warehouseId)) {
                UI.toast(act, "موجودی کافی برای نهایی‌سازی: ${item.name}"); return
            }
        }
    }
    val next = inv.copy(final = true)
    Db.update { d -> d.copy(invoices = d.invoices.map { if (it.id == inv.id) next else it }) }
    act.refresh()
    UI.toast(act, if (next.final) "فاکتور نهایی شد — سند حسابداری صادر شد" else "فاکتور به پیش‌نویس برگشت")
}

fun convertProforma(act: MainActivity, inv: Invoice) {
    val maxSale = Db.db.invoices.filter { it.kind == K_SALE }.maxOfOrNull { it.no } ?: 100L
    val sale = inv.copy(
        id = Db.newId("inv"), no = maxSale + 1, kind = K_SALE, date = J.jTodayStr(),
        paid = 0, final = false, moudian = Db.db.settings.moudian,
        desc = (if (inv.desc.isNotEmpty()) inv.desc + " — " else "") + "تبدیل از پیش‌فاکتور " + J.faN(inv.no)
    )
    Db.update { d -> d.copy(invoices = listOf(sale) + d.invoices) }
    act.refresh()
    UI.toast(act, "پیش‌فاکتور به فاکتور فروش تبدیل شد — شمارهٔ " + J.faN(sale.no))
}

/* ═════════════════════════ نمایش فاکتور ═════════════════════════ */

fun invoiceViewSheet(act: MainActivity, inv: Invoice) {
    val bs = BottomSheet(act)
    val db = Db.db
    val t = Db.invoiceTotals(inv)
    val p = db.partners.firstOrNull { it.id == inv.partnerId }
    val w = db.warehouses.firstOrNull { it.id == inv.warehouseId }
    bs.show(LB.invoiceKindLabel(inv.kind) + " شمارهٔ " + J.faN(inv.no), J.jFa(inv.date) + " • " + (w?.name ?: "") + " • " + (p?.name ?: "")) { b ->

        val doc = UI.innerCard(act, T.card, 20, 14)
        doc.background = UI.stroke(act, T.card, 20, T.border, 1)
        val head = LinearLayout(act)
        head.orientation = LinearLayout.HORIZONTAL
        val hcol = LinearLayout(act)
        hcol.orientation = LinearLayout.VERTICAL
        hcol.addView(UI.text(act, db.settings.shopName, 13, T.text, true))
        hcol.addView(UI.text(act, J.fa(db.settings.phone), 10, T.sub))
        head.addView(hcol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (inv.moudian && inv.kind == K_SALE) head.addView(UI.chip(act, "مودیان", "green", R.drawable.ic_send))
        else head.addView(UI.chip(act, if (inv.final) "نهایی" else "پیش‌نویس", if (inv.final) "green" else "amber"))
        doc.addView(head)
        doc.addView(UI.sep(act), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(act, 1)).also { it.topMargin = UI.dp(act, 10); it.bottomMargin = UI.dp(act, 10) })

        for (l in inv.lines) {
            val it0 = db.items.firstOrNull { i -> i.id == l.itemId }
            val row = UI.innerCard(act, T.cardSoft, 14, 10)
            row.orientation = LinearLayout.HORIZONTAL
            val col = LinearLayout(act)
            col.orientation = LinearLayout.VERTICAL
            col.addView(UI.text(act, it0?.name ?: "—", 12, T.text, true))
            col.addView(UI.text(act, J.qty(l.qty) + " " + (it0?.unit ?: "") + " × " + J.moneyFa(l.price) + (if (l.discount > 0) " − " + J.moneyFa(Math.round(l.discount)) + " تخفیف" else ""), 10, T.sub))
            row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(UI.text(act, J.moneyFa(Math.round(l.qty * (l.price - l.discount))), 12, T.text, true))
            doc.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 5) })
        }
        doc.addView(UI.sep(act), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(act, 1)).also { it.topMargin = UI.dp(act, 10); it.bottomMargin = UI.dp(act, 10) })
        doc.addView(UI.kv(act, "جمع اقلام", J.moneyFa(t.gross), vColor = T.sub, vBold = false))
        if (inv.discount > 0) doc.addView(UI.kv(act, "تخفیف کلی", "−" + J.moneyFa(inv.discount), vColor = T.sub, vBold = false))
        doc.addView(UI.kv(act, "مالیات (" + J.fa(inv.vatRate.toString()) + "٪)", J.moneyFa(t.vat), vColor = T.sub, vBold = false))
        doc.addView(UI.kv(act, "مبلغ کل", J.moneyFa(t.total) + " تومان", vColor = T.text))
        b.addView(doc)

        if (inv.desc.isNotEmpty()) {
            val d = UI.innerCard(act, T.cardSoft, 16, 12)
            d.addView(UI.text(act, inv.desc, 11.5f, T.sub))
            b.addView(d, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
        }

        if (inv.kind == K_PROFORMA && !inv.final) {
            val conv = UI.primaryBtn(act, "تبدیل به فاکتور فروش", icRes = R.drawable.ic_swap) { bs.close(); convertProforma(act, inv) }
            conv.background = UI.gradient(act, 0xFF7C3AED.toInt(), 0xFF4F46E5.toInt(), 16)
            b.addView(conv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
        }

        if (inv.kind == K_SALE && inv.final && t.due > 0) {
            val rem = UI.text(act, "یادآوری پیامکی تسویه — کپی متن آماده", 12.5f, T.amber, true)
            rem.gravity = Gravity.CENTER
            rem.background = UI.rounded(act, T.amberSoft, 16)
            rem.setPadding(UI.dp(act, 14), UI.dp(act, 12), UI.dp(act, 14), UI.dp(act, 12))
            // آیکون زنگ — مثل نسخهٔ وب
            rem.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bell, 0, 0, 0)
            try { rem.compoundDrawables[0]?.setColorFilter(T.amber, android.graphics.PorterDuff.Mode.SRC_IN) } catch (ig: Exception) {}
            rem.compoundDrawablePadding = UI.dp(act, 8)
            rem.setOnClickListener {
                val text = (p?.name ?: "مشتری") + " عزیز، فاکتور شمارهٔ " + J.faN(inv.no) + " به مبلغ " + J.moneyFa(t.due) + " تومان در انتظار تسویه است. ممنون از همکاری‌تان — " + db.settings.shopName
                UI.copyText(act, text, "متن یادآوری کپی شد — مخاطب: " + (p?.name ?: "") + " " + (p?.phone ?: ""))
            }
            b.addView(rem, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
        }

        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "بستن") { bs.close() },
            UI.outlineBtn(act, "ویرایش", icRes = R.drawable.ic_pencil) { bs.close(); invoiceEditorSheet(act, inv) }))
        val fin = UI.primaryBtn(act, if (inv.final) "به پیش‌نویس برگرد" else "نهایی کن", icRes = if (inv.final) R.drawable.ic_undo else R.drawable.ic_badge) { bs.close(); toggleFinal(act, inv) }
        b.addView(fin, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
    }
}

/* ═════════════════════════ فاکتور تکرارشونده ═════════════════════════ */

fun recurringPanel(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    val today = J.jTodayStr()

    val hero = android.widget.FrameLayout(c)
    hero.background = UI.gradient(c, 0xFF0284C7.toInt(), 0xFF0F766E.toInt(), 20)
    val heroCol = LinearLayout(c)
    heroCol.orientation = LinearLayout.VERTICAL
    heroCol.setPadding(UI.dp(c, 16), UI.dp(c, 16), UI.dp(c, 16), UI.dp(c, 16))
    heroCol.addView(UI.text(c, "فاکتور تکرارشونده", 15, Color.WHITE, true))
    val sub = UI.text(c, "قالب بسازید؛ در سررسید هر دوره فاکتور نهایی خودکار صادر می‌شود — مثل Zoho Books. قالب‌های فعال این‌جا دیده می‌شوند.", 11, 0xCCECFDF5.toInt())
    sub.setLineSpacing(0f, 1.2f)
    heroCol.addView(sub, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
    hero.addView(heroCol, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    // واترمارک Repeat — مثل نسخهٔ وب
    try {
        val wm = UI.icon(c, R.drawable.ic_repeat, 110, 0x26FFFFFF)
        val wml = android.widget.FrameLayout.LayoutParams(UI.dp(c, 110), UI.dp(c, 110), Gravity.CENTER or Gravity.END)
        wml.marginEnd = UI.dp(c, 12)
        hero.addView(wm, wml)
    } catch (ig: Exception) {}
    root.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

    if (db.recurring.isEmpty()) {
        val em = UI.empty(c, "قالب تکرارشونده‌ای ندارید", "برای مشتریان دائمی مثل اجاره یا سرویس دوره‌ای عالی است", R.drawable.ic_repeat)
        root.addView(em, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 16) })
    } else {
        for (r in db.recurring) {
            val p = db.partners.firstOrNull { it.id == r.partnerId }
            val tot = Db.invoiceTotals(Invoice("tmp", 0, K_SALE, r.partnerId, r.warehouseId, today, r.lines, r.discount, r.vatRate, 0))
            val due = J.jDiff(today, r.nextDate) >= 0
            val card = UI.card(c)
            val head = LinearLayout(c)
            head.orientation = LinearLayout.HORIZONTAL
            head.gravity = Gravity.CENTER_VERTICAL
            head.addView(UI.iconBox(c, R.drawable.ic_repeat, T.sky, T.skySoft, 40, 20))
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            cl.marginStart = UI.dp(c, 12)
            val titleRow = LinearLayout(c)
            titleRow.orientation = LinearLayout.HORIZONTAL
            titleRow.gravity = Gravity.CENTER_VERTICAL
            titleRow.addView(UI.text(c, r.title, 13, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            titleRow.addView(UI.chip(c, if (!r.active) "موقفا" else if (due) "سررسید شده" else "فعال", if (!r.active) "slate" else if (due) "amber" else "green"))
            col.addView(titleRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            col.addView(UI.text(c, (p?.name ?: "") + " • هر " + J.faN(r.everyMonths) + " ماه • صدور بعدی: " + J.jFa(r.nextDate), 10, T.sub))
            head.addView(col, cl)
            val endC = LinearLayout(c)
            endC.orientation = LinearLayout.VERTICAL
            endC.gravity = Gravity.START
            endC.addView(UI.text(c, J.moneyShort(tot.total), 12.5f, T.text, true))
            endC.addView(UI.text(c, "تومان هر دوره", 9, T.sub))
            head.addView(endC)
            card.addView(head)

            val btns = LinearLayout(c)
            btns.orientation = LinearLayout.HORIZONTAL
            btns.gravity = Gravity.CENTER_VERTICAL
            val issue = UI.text(c, "صدور فوری", 11.5f, Color.WHITE, true)
            issue.gravity = Gravity.CENTER
            issue.background = UI.gradient(c, T.primary, T.primaryD, 14)
            issue.setPadding(0, UI.dp(c, 9), 0, UI.dp(c, 9))
            issue.setOnClickListener { issueRecurringNow(act, r) }
            btns.addView(issue, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val edit = UI.text(c, "ویرایش", 11.5f, T.text, true)
            edit.gravity = Gravity.CENTER
            edit.background = UI.rounded(c, T.cardSoft, 14)
            edit.setPadding(0, UI.dp(c, 9), 0, UI.dp(c, 9))
            edit.setOnClickListener { recurringEditorSheet(act, r) }
            btns.addView(edit, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(c, 8) })
            val sw = UI.makeSwitch(c, r.active) { v ->
                Db.update { d -> d.copy(recurring = d.recurring.map { if (it.id == r.id) it.copy(active = v) else it }) }
                act.refresh()
            }
            btns.addView(sw, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = UI.dp(c, 8) })
            card.addView(btns, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
            root.addView(card)
        }
    }

    val newBtn = UI.text(c, "+ قالب تکرارشوندهٔ جدید", 13, T.primary, true)
    newBtn.gravity = Gravity.CENTER
    newBtn.background = UI.stroke(c, T.card, 18, T.primary, 1)
    newBtn.setPadding(UI.dp(c, 14), UI.dp(c, 20), UI.dp(c, 14), UI.dp(c, 20))
    newBtn.setOnClickListener {
        recurringEditorSheet(act, Recurring(Db.newId("rc"), "", true,
            db.partners.firstOrNull { it.type != "supplier" }?.id ?: "",
            db.warehouses.firstOrNull()?.id ?: "", emptyList(), 0, db.settings.vatRate, 1, today))
    }
    root.addView(newBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 14) })

    return ScrollView(c).apply { isVerticalScrollBarEnabled = false; isFillViewport = true; addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)) }
}

fun issueRecurringNow(act: MainActivity, r: Recurring) {
    val today = J.jTodayStr()
    val maxSale = Db.db.invoices.filter { it.kind == K_SALE }.maxOfOrNull { it.no } ?: 100L
    val inv = Invoice(
        Db.newId("inv"), maxSale + 1, K_SALE, r.partnerId, r.warehouseId, today,
        r.lines.map { it.copy(id = Db.newId("l")) }, r.discount, r.vatRate, 0,
        final = true, moudian = Db.db.settings.moudian,
        desc = "صدور از قالب تکرارشونده — " + r.title
    )
    Db.update { d -> d.copy(
        invoices = listOf(inv) + d.invoices,
        recurring = d.recurring.map { if (it.id == r.id) it.copy(lastGenerated = today, nextDate = J.jAddMonths(if (J.jDiff(it.nextDate, today) > 0) it.nextDate else today, Math.max(1L, it.everyMonths))) else it }
    ) }
    act.refresh()
    UI.toast(act, "فاکتور شمارهٔ " + J.faN(inv.no) + " صادر شد — سند حسابداری خودکار ساخته شد")
}

fun recurringEditorSheet(act: MainActivity, r0: Recurring) {
    val bs = BottomSheet(act)
    var r = r0
    val isNew = !Db.db.recurring.any { it.id == r.id }
    bs.show("قالب تکرارشونده", "هر دوره به‌طور خودکار فاکتور نهایی صادر می‌شود") { b ->
        val titleLabel = ValueLabel()
        val title = UI.field(act, "عنوان قالب", r.title, hint = "مثلا اجارهٔ ماهانهٔ قفسه")
        hookText(title.first) { v -> r = r.copy(title = v) }
        b.addView(title.first)

        val ph1 = ValueLabel()
        val pSel = UI.selectField(act, "مشتری", ph1.current) {
            optionSheet(act, "مشتری", Db.db.partners.filter { it.type != "supplier" }.map { Opt(it.id, it.name) }, r.partnerId) { o ->
                r = r.copy(partnerId = o.id)
                ph1.set(o.label)
            }
        }
        ph1.bind(pSel, Db.db.partners.firstOrNull { it.id == r.partnerId }?.name ?: "انتخاب کنید")
        val ph2 = ValueLabel()
        val wSel = UI.selectField(act, "انبار", ph2.current) {
            optionSheet(act, "انبار", Db.db.warehouses.map { Opt(it.id, it.name) }, r.warehouseId) { o ->
                r = r.copy(warehouseId = o.id)
                ph2.set(o.label)
            }
        }
        ph2.bind(wSel, Db.db.warehouses.firstOrNull { it.id == r.warehouseId }?.name ?: "انتخاب کنید")
        val row = LinearLayout(act)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(pSel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        row.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        row.addView(wSel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(row)

        val ph3 = ValueLabel()
        val every = UI.numField(act, "هر چند ماه", r.everyMonths, "ماه")
        hookNum(every) { v -> r = r.copy(everyMonths = Math.max(1L, v)) }
        val dField = UI.dateField(act, "صدور بعدی", r.nextDate) {
            dateSheet(act, r.nextDate) { v -> r = r.copy(nextDate = v); ph3.set(J.jFa(v)) }
        }
        ph3.bind(dField, J.jFa(r.nextDate))
        val row2 = LinearLayout(act)
        row2.orientation = LinearLayout.HORIZONTAL
        row2.addView(every, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        row2.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        row2.addView(dField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(row2)

        // اقلام
        val linesCard = UI.innerCard(act, T.cardSoft, 16, 12)
        val addBtn = UI.text(act, "+ افزودن", 11, Color.WHITE, true)
        addBtn.background = UI.rounded(act, T.primary, 12)
        addBtn.setPadding(UI.dp(act, 12), UI.dp(act, 6), UI.dp(act, 12), UI.dp(act, 7))
        val head = LinearLayout(act)
        head.orientation = LinearLayout.HORIZONTAL
        head.addView(UI.text(act, "اقلام هر دوره", 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(addBtn)
        linesCard.addView(head)
        val linesWrap = LinearLayout(act)
        linesWrap.orientation = LinearLayout.VERTICAL
        linesCard.addView(linesWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        b.addView(linesCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })

        // مبلغ هر دوره — زنده، با هر تغییر به‌روز می‌شود
        val totalVal = UI.text(act, "", 13.5f, Color.WHITE, true)
        fun updTotal() {
            val total = r.lines.sumOf { Math.round(it.qty * (it.price - it.discount)) } - r.discount
            totalVal.text = J.moneyFa(Math.max(0L, total)) + " تومان"
        }

        fun renderLines() {
            linesWrap.removeAllViews()
            if (r.lines.isEmpty()) {
                val t = UI.text(act, "اقلام دوره را اضافه کنید", 11, T.sub)
                t.gravity = Gravity.CENTER
                t.setPadding(0, UI.dp(act, 10), 0, UI.dp(act, 10))
                linesWrap.addView(t)
                return
            }
            for ((idx, l) in r.lines.withIndex()) {
                val it0 = Db.db.items.firstOrNull { i -> i.id == l.itemId }
                val card = UI.innerCard(act, T.card, 14, 11)
                val headL = LinearLayout(act)
                headL.orientation = LinearLayout.HORIZONTAL
                headL.addView(UI.text(act, it0?.name ?: "—", 12.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val x = UI.icon(act, R.drawable.ic_close, 15, T.rose)
                x.setOnClickListener {
                    r = r.copy(lines = r.lines.filterIndexed { i, _ -> i != idx })
                    renderLines(); updTotal()
                }
                headL.addView(x)
                card.addView(headL)
                val qty = UI.qtyStepper(act, l.qty) { v ->
                    val lines = r.lines.toMutableList()
                    if (idx >= lines.size) return@qtyStepper
                    lines[idx] = lines[idx].copy(qty = Math.max(0.0, v))
                    r = r.copy(lines = lines)
                    updTotal()
                }
                val price = UI.numField(act, "قیمت", l.price)
                hookNum(price) { v ->
                    val lines = r.lines.toMutableList()
                    if (idx >= lines.size) return@hookNum
                    lines[idx] = lines[idx].copy(price = v)
                    r = r.copy(lines = lines)
                    updTotal()
                }
                val ctl = LinearLayout(act)
                ctl.orientation = LinearLayout.HORIZONTAL
                ctl.gravity = Gravity.CENTER_VERTICAL
                ctl.addView(qty)
                ctl.addView(price, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(act, 8) })
                card.addView(ctl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
                linesWrap.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
            }
        }
        addBtn.setOnClickListener {
            optionSheet(act, "کالا یا خدمت", Db.db.items.map { Opt(it.id, it.name, J.moneyShort(it.price)) }, "") { o ->
                if (r.lines.any { it.itemId == o.id }) return@optionSheet
                val it0 = Db.db.items.firstOrNull { i -> i.id == o.id } ?: return@optionSheet
                r = r.copy(lines = r.lines + InvoiceLine(Db.newId("l"), o.id, 1.0, it0.price, 0.0))
                renderLines(); updTotal()
            }
        }
        renderLines()

        val totalCard = UI.innerCard(act, 0xFF0284C7.toInt(), 18, 14)
        totalCard.background = UI.gradient(act, 0xFF0284C7.toInt(), 0xFF0F766E.toInt(), 18)
        totalCard.orientation = LinearLayout.HORIZONTAL
        totalCard.addView(UI.text(act, "مبلغ هر دوره", 13.5f, Color.WHITE, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        totalCard.addView(totalVal)
        b.addView(totalCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
        updTotal()

        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "حذف قالب", danger = true) { bs.close(); confirmDeleteRecurring(act, r) },
            UI.primaryBtn(act, "ذخیرهٔ قالب") {
                if (r.title.trim().isEmpty() || r.partnerId.isEmpty() || r.warehouseId.isEmpty() || r.lines.isEmpty()) { UI.toast(act, "عنوان، مشتری، انبار و حداقل یک قلم لازم است"); return@primaryBtn }
                val exists = Db.db.recurring.any { it.id == r.id }
                val saved = Db.update { d -> d.copy(recurring = if (exists) d.recurring.map { if (it.id == r.id) r else it } else d.recurring + r) }
                if (!saved) return@primaryBtn
                bs.close(); act.refresh()
                UI.toast(act, if (exists) "قالب تکرارشونده ویرایش شد" else "قالب تکرارشونده ساخته شد")
            }))
    }
}

fun confirmDeleteRecurring(act: MainActivity, r: Recurring) {
    confirmSheet(act, "حذف قالب؟", "«" + r.title + "» حذف می‌شود و دیگر فاکتور خودکار صادر نمی‌کند.") {
        Db.update { d -> d.copy(recurring = d.recurring.filter { it.id != r.id }) }
        act.refresh()
        UI.toast(act, "قالب حذف شد")
    }
}
