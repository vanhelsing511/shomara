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
 * ماژول طرف‌حساب‌ها + دریافت و پرداخت + چک و خزانه
 */

fun partnersScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    val totalAr = db.partners.sumOf { Math.max(0L, Db.partnerBalance(db, it.id).net) }
    val totalAp = db.partners.sumOf { Math.max(0L, -Db.partnerBalance(db, it.id).net) }
    root.addView(UI.grid2(c,
        UI.statCard(c, "کل مطالبات", J.moneyShort(totalAr), "بدهی مشتریان به ما — تومان", "amber", R.drawable.ic_users),
        UI.statCard(c, "کل بدهی ما", J.moneyShort(totalAp), "بدهی ما به تامین‌کنندگان — تومان", "rose", R.drawable.ic_building)
    ))

    val (sbox, se) = UI.search(c, "جستجو با نام، کد یا تلفن…")
    se.setText(act.partnersQuery)
    root.addView(sbox)

    val typeOptions = listOf(
        Triple("همه", db.partners.size, "all"),
        Triple("مشتریان", db.partners.count { it.type != "supplier" }, "customer"),
        Triple("تامین‌کنندگان", db.partners.count { it.type != "customer" }, "supplier")
    )
    if (typeOptions.indexOfFirst { it.third == act.partnersType } < 0) act.partnersType = "all"
    val selIdx = typeOptions.indexOfFirst { it.third == act.partnersType }.coerceAtLeast(0)
    root.addView(UI.segScroll(c, typeOptions, selIdx) { i ->
        act.partnersType = typeOptions[i].third
        act.refresh()
    })

    val list = LinearLayout(c)
    root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
    fun renderList() {
        val q = act.partnersQuery
        val tp = act.partnersType
        list.removeAllViews()
        val filtered = db.partners.filter { p ->
            if (tp == "customer" && p.type == "supplier") return@filter false
            if (tp == "supplier" && p.type == "customer") return@filter false
            if (q.isEmpty()) true else p.name.contains(q) || p.code.contains(q) || p.phone.contains(q)
        }
        if (filtered.isEmpty()) {
            val em = if (q.isEmpty()) UI.empty(c, "طرف‌حسابی پیدا نشد", "با دکمهٔ سبز یکی اضافه کنید", R.drawable.ic_users)
                     else UI.empty(c, "چیزی پیدا نشد", "عبارت دیگری را امتحان کنید", R.drawable.ic_search)
            list.addView(em, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 16) })
        } else {
            for (p in filtered) {
                val bal = Db.partnerBalance(db, p.id)
                val row = UI.listRow(c,
                    p.name,
                    LB.partnerTypeLabel(p.type) + " • کد " + J.fa(p.code) + (if (p.phone.isNotEmpty()) " • " + J.fa(p.phone) else ""),
                    UI.endCol(c,
                        if (bal.net == 0L) "تسویه" else J.moneyFa(Math.abs(bal.net)),
                        if (bal.net > 0) "بدهکار (می‌دهد)" else if (bal.net < 0) "بستانکار (می‌گیریم)" else "—",
                        if (bal.net > 0) T.amber else if (bal.net < 0) T.rose else T.sub, 13),
                    null,
                    icRes = if (p.type == "supplier") R.drawable.ic_building else R.drawable.ic_users,
                    icTint = if (p.type == "supplier") T.rose else T.amber,
                    click = { accountCardSheet(act, p) })
                list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
            }
        }
    }
    se.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { act.partnersQuery = s?.toString() ?: ""; renderList() }
    })
    renderList()

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

/** فرم طرف‌حساب */
fun partnerEditorSheet(act: MainActivity, p0: Partner) {
    val bs = BottomSheet(act)
    var p = p0
    val isNew = !Db.db.partners.any { it.id == p.id }
    bs.show(if (isNew) "طرف‌حساب جدید" else "ویرایش طرف‌حساب", "مشتری یا تامین‌کننده را ثبت کنید") { b ->
        val name = UI.field(act, "نام شخص یا شرکت", p.name, hint = "مثلاً آقای رضایی")
        hookText(name.first) { v -> p = p.copy(name = v) }
        b.addView(name.first)

        val code = UI.field(act, "کد", p.code, center = true)
        hookText(code.first) { v -> p = p.copy(code = v) }
        val tLabel = ValueLabel()
        val typeSel = UI.selectField(act, "نوع", tLabel.current) {
            optionSheet(act, "نوع طرف‌حساب", listOf(
                Opt("customer", "مشتری"), Opt("supplier", "تامین‌کننده"), Opt("both", "مشتری / تامین‌کننده")
            ), p.type) { o ->
                p = p.copy(type = o.id)
                tLabel.set(o.label)
            }
        }
        tLabel.bind(typeSel, LB.partnerTypeLabel(p.type))
        val r1 = LinearLayout(act)
        r1.orientation = LinearLayout.HORIZONTAL
        r1.addView(code.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r1.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r1.addView(typeSel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r1)

        val phone = UI.field(act, "تلفن همراه", p.phone, ltr = true, center = true)
        val nid = UI.field(act, "شناسه/کد ملی", p.nationalId, ltr = true, center = true)
        hookText(phone.first) { v -> p = p.copy(phone = v) }
        hookText(nid.first) { v -> p = p.copy(nationalId = v) }
        val r2 = LinearLayout(act)
        r2.orientation = LinearLayout.HORIZONTAL
        r2.addView(phone.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r2.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r2.addView(nid.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r2)

        val addr = UI.fieldMulti(act, "آدرس", p.address)
        hookText(addr.first) { v -> p = p.copy(address = v) }
        b.addView(addr.first)

        val email = UI.field(act, "ایمیل (اختیاری)", p.email, hint = "name@example.com", ltr = true)
        val postal = UI.field(act, "کد پستی (اختیاری)", p.postalCode, hint = "۱۰ رقم", ltr = true, center = true)
        val economic = UI.field(act, "شناسه اقتصادی (اختیاری)", p.economicCode, ltr = true, center = true)
        hookText(email.first) { v -> p = p.copy(email = v.trim()) }
        hookText(postal.first) { v -> p = p.copy(postalCode = v.trim()) }
        hookText(economic.first) { v -> p = p.copy(economicCode = v.trim()) }
        val contactRow = LinearLayout(act); contactRow.orientation = LinearLayout.HORIZONTAL
        contactRow.addView(email.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        contactRow.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 8), ViewGroup.LayoutParams.WRAP_CONTENT))
        contactRow.addView(postal.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(contactRow)
        b.addView(economic.first)

        val credit = UI.numField(act, "سقف اعتبار مشتری (تومان، اختیاری)", p.creditLimit)
        val term = UI.numField(act, "مهلت تسویه (روز)", p.paymentTermDays)
        hookNum(credit) { v -> p = p.copy(creditLimit = v) }
        hookNum(term) { v -> p = p.copy(paymentTermDays = v) }
        val creditRow = LinearLayout(act); creditRow.orientation = LinearLayout.HORIZONTAL
        creditRow.addView(credit, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        creditRow.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 8), ViewGroup.LayoutParams.WRAP_CONTENT))
        creditRow.addView(term, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(creditRow)

        val desc = UI.fieldMulti(act, "یادداشت / توضیحات", p.desc)
        hookText(desc.first) { v -> p = p.copy(desc = v) }
        b.addView(desc.first)

        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ذخیره") {
                val current = Db.db
                if (p.name.trim().isEmpty()) { UI.toast(act, "نام طرف‌حساب را بنویسید"); return@primaryBtn }
                if (p.code.trim().isEmpty()) { UI.toast(act, "کد طرف‌حساب الزامی است"); return@primaryBtn }
                if (current.partners.any { it.id != p.id && it.code.trim().equals(p.code.trim(), true) }) { UI.toast(act, "کد طرف‌حساب تکراری است"); return@primaryBtn }
                if (current.partners.any { it.id != p.id && it.name.trim().equals(p.name.trim(), true) }) { UI.toast(act, "طرف‌حساب با این نام قبلاً ثبت شده است"); return@primaryBtn }
                val phoneDigits = p.phone.filter { it.isDigit() }
                if (p.phone.isNotBlank() && phoneDigits.length !in 7..15) { UI.toast(act, "شماره تلفن نامعتبر است"); return@primaryBtn }
                if (p.email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(p.email.trim()).matches()) { UI.toast(act, "ایمیل نامعتبر است"); return@primaryBtn }
                val postalDigits = p.postalCode.filter { it.isDigit() }
                if (p.postalCode.isNotBlank() && postalDigits.length != 10) { UI.toast(act, "کد پستی باید ۱۰ رقم باشد"); return@primaryBtn }
                val economicDigits = p.economicCode.map { ch -> when (ch) { in '۰'..'۹' -> ('0' + (ch - '۰')); in '٠'..'٩' -> ('0' + (ch - '٠')); else -> ch } }.filter { it.isDigit() }.joinToString("")
                if (p.economicCode.isNotBlank() && economicDigits.length !in 11..14) { UI.toast(act, "شناسه اقتصادی باید ۱۱ تا ۱۴ رقم باشد"); return@primaryBtn }
                if (p.creditLimit < 0 || p.paymentTermDays < 0) { UI.toast(act, "سقف اعتبار و مهلت تسویه نمی‌توانند منفی باشند"); return@primaryBtn }
                val nidDigits = p.nationalId.map { ch -> when (ch) { in '۰'..'۹' -> ('0' + (ch - '۰')); in '٠'..'٩' -> ('0' + (ch - '٠')); else -> ch } }.filter { it.isDigit() }.joinToString("")
                if (p.nationalId.isNotBlank() && nidDigits.length !in 10..12) { UI.toast(act, "کد ملی/شناسه ملی باید ۱۰ تا ۱۲ رقم باشد"); return@primaryBtn }
                if (nidDigits.length == 10 && p.nationalId.isNotBlank()) {
                    val digits = nidDigits.map { it - '0' }
                    val check = digits[9]
                    val sum = (0..8).sumOf { digits[it] * (10 - it) } % 11
                    val valid = if (sum < 2) check == sum else check == 11 - sum
                    if (!valid) { UI.toast(act, "کد ملی واردشده معتبر نیست"); return@primaryBtn }
                }
                fun normalizeId(raw: String): String = raw.map { ch -> when (ch) { in '۰'..'۹' -> ('0' + (ch - '۰')); in '٠'..'٩' -> ('0' + (ch - '٠')); else -> ch } }.filter { it.isDigit() }.joinToString("")
                if (nidDigits.isNotEmpty() && current.partners.any { it.id != p.id && normalizeId(it.nationalId) == nidDigits }) { UI.toast(act, "این شناسه قبلاً برای طرف‌حساب دیگری ثبت شده است"); return@primaryBtn }
                val exists = current.partners.any { it.id == p.id }
                val saved = Db.update { d -> d.copy(partners = if (exists) d.partners.map { if (it.id == p.id) p else it } else d.partners + p) }
                if (!saved) return@primaryBtn
                bs.close(); act.refresh()
                UI.toast(act, (if (exists) "طرف‌حساب ویرایش شد" else "طرف‌حساب جدید ثبت شد") + " — " + p.name)
            }))
        if (!isNew) {
            val del = UI.outlineBtn(act, "حذف طرف‌حساب", danger = true, icRes = R.drawable.ic_trash) {
                if (Db.partnerHasReferences(Db.db, p.id)) {
                    UI.toast(act, "این طرف‌حساب در فاکتور، پرداخت، چک، قالب یا پروژه استفاده شده و حذف آن مسدود است.")
                    return@outlineBtn
                }
                bs.close()
                confirmSheet(act, "حذف طرف‌حساب؟", "«" + p.name + "» حذف می‌شود.") {
                    val saved = Db.update { d -> d.copy(partners = d.partners.filter { it.id != p.id }) }
                    if (saved) { act.refresh(); UI.toast(act, "طرف‌حساب حذف شد — " + p.name) }
                }
            }
            b.addView(del, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        }
    }
}

/** کارت حساب — گردش مالی کامل هر طرف از روی اسناد حسابداری */
fun accountCardSheet(act: MainActivity, p: Partner) {
    val bs = BottomSheet(act)
    val db = Db.db
    val bal = Db.partnerBalance(db, p.id)
    bs.show("کارت حساب — " + p.name, "گردش مالی کامل از روی اسناد حسابداری") { b ->
        val r1 = UI.innerCard(act, T.amberSoft, 16, 12)
        r1.addView(UI.text(act, "طلب از این طرف", 10.5f, T.amber, true))
        r1.addView(UI.text(act, J.moneyFa(bal.ar), 15, T.amber, true))
        val r2 = UI.innerCard(act, T.roseSoft, 16, 12)
        r2.addView(UI.text(act, "بدهی ما به این طرف", 10.5f, T.rose, true))
        r2.addView(UI.text(act, J.moneyFa(bal.ap), 15, T.rose, true))
        b.addView(UI.grid2(act, r1, r2))

        // اطلاعات تماس — در اسکرول افقی تا آدرس بلند چیزی را نپوشاند
        val chips = LinearLayout(act)
        chips.orientation = LinearLayout.HORIZONTAL
        if (p.phone.isNotEmpty()) chips.addView(UI.chip(act, J.fa(p.phone), "slate", R.drawable.ic_phone), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = UI.dp(act, 6) })
        if (p.address.isNotEmpty()) chips.addView(UI.chip(act, p.address, "slate", R.drawable.ic_pin), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = UI.dp(act, 6) })
        chips.addView(UI.chip(act, LB.partnerTypeLabel(p.type), "slate", R.drawable.ic_badge))
        val chipsScroll = HorizontalScroll(act)
        chipsScroll.addView(chips, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        b.addView(chipsScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })

        // گردش
        data class CardRow(val date: String, val desc: String, val debit: Long, val credit: Long)
        val rows = ArrayList<CardRow>()
        for (v in Db.allVouchers(db)) for (l in v.lines) {
            if (l.detailId != p.id) continue
            if (l.accountCode == "1103") rows.add(CardRow(v.date, v.desc, l.debit, l.credit))
            if (l.accountCode == "2101") rows.add(CardRow(v.date, v.desc, l.credit, l.debit))
        }
        rows.sortBy { it.date }
        val headRow = LinearLayout(act)
        headRow.orientation = LinearLayout.HORIZONTAL
        headRow.gravity = Gravity.CENTER_VERTICAL
        headRow.addView(UI.icon(act, R.drawable.ic_arrow_r, 15, T.primary))
        headRow.addView(UI.text(act, "گردش حساب (" + J.faN(rows.size) + " ردیف)", 12, T.text, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = UI.dp(act, 6) })
        b.addView(headRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
        if (rows.isEmpty()) {
            b.addView(UI.empty(act, "گردشی ثبت نشده", "با فاکتور و دریافت/پرداخت، گردش ساخته می‌شود", R.drawable.ic_card))
        } else {
            var running = 0L
            for (r in rows) {
                running += r.debit - r.credit
                val cell = UI.innerCard(act, T.cardSoft, 14, 11)
                val head = LinearLayout(act)
                head.orientation = LinearLayout.HORIZONTAL
                head.addView(UI.text(act, J.jFa(r.date), 10, T.sub, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                head.addView(UI.text(act, if (r.debit > 0) "+" + J.moneyFa(r.debit) else "−" + J.moneyFa(r.credit), 12, if (r.debit > 0) T.amber else T.primary, true))
                cell.addView(head)
                cell.addView(UI.text(act, r.desc, 11.5f, T.text, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 3) })
                cell.addView(UI.text(act, "مانده: " + J.moneyFa(Math.abs(running)) + " " + (if (running >= 0) "(بدهکار)" else "(بستانکار)"), 10, T.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 3) })
                b.addView(cell, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
            }
        }

        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "ویرایش", icRes = R.drawable.ic_pencil) { bs.close(); partnerEditorSheet(act, p) },
            UI.outlineBtn(act, "ثبت دریافت", icRes = R.drawable.ic_arrow_down_c) { bs.close(); paymentEditorSheet(act, "receipt", null, p.id) }))
        val close = UI.outlineBtn(act, "بستن کارت حساب") { bs.close() }
        b.addView(close, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
    }
}

/* ═════════════════════════ دریافت و پرداخت ═════════════════════════ */

fun cashScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    // خزانه
    val treasury = UI.card(c)
    treasury.addView(UI.sectionTitle(c, "خزانه", "ماندهٔ صندوق، بانک و کارتخوان از روی اسناد", R.drawable.ic_wallet, topMargin = 0))
    for (b in db.banks) {
        val bal = Db.bankBalance(db, b.id)
        val ic = when (b.kind) { "cash" -> R.drawable.ic_banknote; "pos" -> R.drawable.ic_card; else -> R.drawable.ic_landmark }
        val tint = when (b.kind) { "cash" -> T.lime; "pos" -> T.violet; else -> T.sky }
        val row = UI.listRow(c, b.name,
            LB.bankKindLabel(b.kind) + (if (b.accountNo.isNotEmpty()) " • " + J.fa(b.accountNo) else ""),
            UI.endCol(c, J.moneyShort(bal), "تومان", if (bal < 0) T.rose else T.primary, 13),
            null, icRes = ic, icTint = tint, chev = false)
        treasury.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
    }
    root.addView(treasury)

    val kind = act.cashTab
    // کارت‌های دریافت/پرداخت
    val recSum = db.payments.filter { it.kind == "receipt" }.sumOf { it.amount }
    val paySum = db.payments.filter { it.kind == "payment" }.sumOf { it.amount }
    val rec = UI.listRow(c, "دریافت‌ها", J.moneyShort(recSum) + " تومان", icRes = R.drawable.ic_arrow_down_c, icTint = T.primary, chev = false)
    if (kind == "receipt") rec.background = UI.stroke(c, T.card, 18, T.primary, 2)
    rec.setOnClickListener { act.cashTab = "receipt"; act.refresh() }
    val pay = UI.listRow(c, "پرداخت‌ها", J.moneyShort(paySum) + " تومان", icRes = R.drawable.ic_arrow_up_c, icTint = T.rose, chev = false)
    if (kind == "payment") pay.background = UI.stroke(c, T.card, 18, T.rose, 2)
    pay.setOnClickListener { act.cashTab = "payment"; act.refresh() }
    root.addView(UI.grid2(c, rec, pay))

    // جمع + دکمهٔ جدید
    val head = LinearLayout(c)
    head.orientation = LinearLayout.HORIZONTAL
    head.gravity = Gravity.CENTER_VERTICAL
    val list = db.payments.filter { it.kind == kind }.sortedByDescending { it.date }
    val kindTotal = list.sumOf { it.amount }
    head.addView(UI.text(c, (if (kind == "receipt") "جمع دریافت‌ها" else "جمع پرداخت‌ها") + ": " + J.moneyFa(kindTotal) + " تومان", 12, T.sub, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    val newBtn = UI.text(c, "+ " + if (kind == "receipt") "دریافت جدید" else "پرداخت جدید", 11.5f, Color.WHITE, true)
    newBtn.background = UI.rounded(c, T.primary, 14)
    newBtn.setPadding(UI.dp(c, 12), UI.dp(c, 8), UI.dp(c, 12), UI.dp(c, 8))
    newBtn.setOnClickListener { paymentEditorSheet(act, kind, null, "") }
    head.addView(newBtn)
    root.addView(head, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })

    // لیست
    val listWrap = LinearLayout(c)
    root.addView(listWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
    if (list.isEmpty()) {
        listWrap.addView(UI.empty(c, "سندی ثبت نشده", "اولین دریافت یا پرداخت را ثبت کنید", R.drawable.ic_banknote), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 16) })
    } else {
        for (p in list) {
            val partner = db.partners.firstOrNull { it.id == p.partnerId }
            val bank = db.banks.firstOrNull { it.id == p.bankId }
            val ic = when (p.method) { "cash" -> R.drawable.ic_banknote; "card" -> R.drawable.ic_card; "transfer" -> R.drawable.ic_send; else -> R.drawable.ic_landmark }
            val row = UI.listRow(c,
                LB.payMethodLabel(p.method) + " — " + (partner?.name ?: "عمومی"),
                "شمارهٔ " + J.faN(p.no) + " • " + J.jFa(p.date) + " • " + (bank?.name ?: "—") + (if (p.desc.isNotEmpty()) " • " + p.desc else ""),
                UI.endCol(c, (if (p.kind == "receipt") "+" else "−") + J.moneyFa(p.amount), "تومان", if (p.kind == "receipt") T.primary else T.rose, 13),
                null, icRes = ic, icTint = if (p.kind == "receipt") T.primary else T.rose,
                click = { paymentEditorSheet(act, p.kind, p, "") })
            listWrap.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

fun paymentEditorSheet(act: MainActivity, kind: String, p0: Payment?, presetPartner: String) {
    val bs = BottomSheet(act)
    val db = Db.db
    var p = p0 ?: Payment(
        Db.newId("pay"), 400L + db.payments.size + 1, kind, "cash",
        if (presetPartner.isNotEmpty()) presetPartner else db.partners.firstOrNull()?.id ?: "",
        db.banks.firstOrNull()?.id ?: "", 0, J.jTodayStr(), ""
    )
    val isNew = !db.payments.any { it.id == p.id }
    bs.show(if (isNew) (if (p.kind == "receipt") "دریافت جدید" else "پرداخت جدید") else "ویرایش سند",
        "سند حسابداری به‌صورت خودکار ثبت می‌شود") { b ->

        val mSel = UI.seg(act, listOf(Triple("نقدی", -1, "cash"), Triple("کارتخوان", -1, "card"), Triple("حواله", -1, "transfer")),
            listOf("cash", "card", "transfer").indexOf(p.method)) { i -> p = p.copy(method = listOf("cash", "card", "transfer")[i]) }
        b.addView(mSel)

        val pLabel = ValueLabel()
        val pOptions = db.partners.filter { if (p.kind == "receipt") it.type != "supplier" else it.type != "customer" }
        val pSel = UI.selectField(act, "طرف‌حساب", pLabel.current) {
            optionSheet(act, "طرف‌حساب", pOptions.map { Opt(it.id, it.name, LB.partnerTypeLabel(it.type)) }, p.partnerId) { o ->
                p = p.copy(partnerId = o.id)
                pLabel.set(o.label)
            }
        }
        pLabel.bind(pSel, db.partners.firstOrNull { it.id == p.partnerId }?.name ?: "انتخاب کنید")
        b.addView(pSel)

        val bLabel = ValueLabel()
        val bSel = UI.selectField(act, "صندوق / بانک", bLabel.current) {
            optionSheet(act, "صندوق / بانک", db.banks.map { Opt(it.id, it.name, LB.bankKindLabel(it.kind)) }, p.bankId) { o ->
                p = p.copy(bankId = o.id)
                bLabel.set(o.label)
            }
        }
        bLabel.bind(bSel, db.banks.firstOrNull { it.id == p.bankId }?.name ?: "انتخاب کنید")
        val dLabel = ValueLabel()
        val dField = UI.dateField(act, "تاریخ", p.date) {
            dateSheet(act, p.date) { v -> p = p.copy(date = v); dLabel.set(J.jFa(v)) }
        }
        dLabel.bind(dField, J.jFa(p.date))
        val r1 = LinearLayout(act)
        r1.orientation = LinearLayout.HORIZONTAL
        r1.addView(bSel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r1.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r1.addView(dField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r1)

        val amount = UI.numField(act, "مبلغ (تومان)", p.amount)
        hookNum(amount) { v -> p = p.copy(amount = v) }
        b.addView(amount)

        val desc = UI.field(act, "توضیحات", p.desc)
        hookText(desc.first) { v -> p = p.copy(desc = v) }
        b.addView(desc.first)

        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ثبت و صدور سند") {
                if (p.amount <= 0) { UI.toast(act, "مبلغ را وارد کنید"); return@primaryBtn }
                if (p.partnerId.isBlank() || db.partners.none { it.id == p.partnerId }) { UI.toast(act, "طرف‌حساب را انتخاب کنید"); return@primaryBtn }
                val partner = db.partners.firstOrNull { it.id == p.partnerId }
                if (partner != null && p.kind == "receipt" && partner.type == "supplier") { UI.toast(act, "برای دریافت، مشتری یا طرف‌حساب مشترک انتخاب کنید"); return@primaryBtn }
                if (partner != null && p.kind != "receipt" && partner.type == "customer") { UI.toast(act, "برای پرداخت، فروشنده یا طرف‌حساب مشترک انتخاب کنید"); return@primaryBtn }
                if (p.bankId.isBlank() || db.banks.none { it.id == p.bankId }) { UI.toast(act, "صندوق یا بانک را انتخاب کنید"); return@primaryBtn }
                if (p.method !in listOf("cash", "card", "transfer", "check")) { UI.toast(act, "روش دریافت/پرداخت معتبر نیست"); return@primaryBtn }
                val exists = db.payments.any { it.id == p.id }
                val saved = Db.update { d -> d.copy(payments = if (exists) d.payments.map { if (it.id == p.id) p else it } else listOf(p) + d.payments) }
                if (!saved) return@primaryBtn
                bs.close(); act.refresh()
                UI.toast(act, (if (exists) "سند ویرایش شد" else if (p.kind == "receipt") "دریافت ثبت شد" else "پرداخت ثبت شد") + " — " + J.moneyFa(p.amount) + " تومان — سند حسابداری خودکار ساخته شد")
            }))
        if (!isNew) {
            val del = UI.outlineBtn(act, "حذف سند", danger = true, icRes = R.drawable.ic_trash) {
                bs.close()
                confirmSheet(act, "حذف سند؟", "این دریافت/پرداخت حذف می‌شود.") {
                    Db.update { d -> d.copy(payments = d.payments.filter { it.id != p.id }) }
                    act.refresh()
                    UI.toast(act, "سند حذف شد")
                }
            }
            b.addView(del, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        }
    }
}

/* ═════════════════════════ چک و خزانه ═════════════════════════ */

private val NEXT_STATUSES: Map<String, List<Triple<String, String, String>>> = mapOf(
    "drawer" to listOf(
        Triple("inBank", "سپرده در بانک", "sky"), Triple("cleared", "وصول شد", "green"),
        Triple("bounced", "برگشت خورد", "rose"), Triple("spent", "خرج شد", "violet")),
    "inBank" to listOf(Triple("cleared", "وصول شد", "green"), Triple("bounced", "برگشت خورد", "rose")),
    "cleared" to emptyList(),
    "bounced" to listOf(Triple("drawer", "برگشت به صندوق", "sky")),
    "spent" to emptyList()
)

fun checksScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))
    val today = J.jTodayStr()

    val openChecks = db.checks.filter { it.status == "drawer" || it.status == "inBank" }
    val receivedOpen = openChecks.filter { it.kind == "received" }.sumOf { it.amount }
    val issuedOpen = openChecks.filter { it.kind == "issued" }.sumOf { it.amount }
    root.addView(UI.grid2(c,
        UI.statCard(c, "چک‌های دریافتی (در جریان)", J.moneyShort(receivedOpen), "تومان", "green", R.drawable.ic_landmark),
        UI.statCard(c, "چک‌های پرداختی (در جریان)", J.moneyShort(issuedOpen), "تومان", "rose", R.drawable.ic_landmark)
    ))

    val soonDue = openChecks.filter { J.jDiff(it.dueDate, today) in 0..10 }
    val overdue = openChecks.filter { J.jDiff(it.dueDate, today) < 0 }
    if (overdue.size + soonDue.size > 0) {
        val alert = UI.innerCard(act, T.amberSoft, 16, 13)
        alert.orientation = LinearLayout.HORIZONTAL
        alert.gravity = Gravity.CENTER_VERTICAL
        alert.addView(UI.icon(act, R.drawable.ic_alert, 18, T.amber))
        alert.addView(UI.text(act, J.faN(soonDue.size) + " چک در ۱۰ روز آینده و " + J.faN(overdue.size) + " چک گذشته از سررسید", 12, T.amber, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(act, 10) })
        root.addView(alert, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
    }

    val kind = act.checksTab
    val kindOptions = listOf(Triple("دریافتی", db.checks.count { it.kind == "received" }, "received"), Triple("پرداختی", db.checks.count { it.kind == "issued" }, "issued"))
    val selIdx = kindOptions.indexOfFirst { it.third == kind }.coerceAtLeast(0)
    root.addView(UI.segScroll(c, kindOptions, selIdx) { i ->
        act.checksTab = kindOptions[i].third
        act.refresh()
    })

    val list = db.checks.filter { it.kind == kind }.sortedBy { it.dueDate }
    val listWrap = LinearLayout(c)
    root.addView(listWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
    if (list.isEmpty()) {
        listWrap.addView(UI.empty(c, "چکی ثبت نشده", "با دکمهٔ سبز اولین چک را ثبت کنید", R.drawable.ic_landmark), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 16) })
    } else {
        for (chk in list) {
            val p = db.partners.firstOrNull { it.id == chk.partnerId }
            val days = J.jDiff(chk.dueDate, today)
            val overdueC = (chk.status == "drawer" || chk.status == "inBank") && days < 0
            val statusTone = when (chk.status) {
                "cleared" -> "green"; "bounced" -> "rose"; "spent" -> "violet"; "inBank" -> "sky"; else -> "amber"
            }
            val card = UI.card(c)
            card.addView(UI.listRow(c,
                chk.bankName + " — " + J.fa(chk.serial),
                (p?.name ?: "—") + " • " + J.jFa(chk.date),
                UI.endCol(c, J.moneyFa(chk.amount), "تومان"),
                UI.chip(c, LB.checkStatusLabel(chk.status), statusTone),
                icRes = R.drawable.ic_landmark, icTint = T.rose, chev = false,
                click = { checkEditorSheet(act, chk) }))
            // نوار سررسید + دکمه‌های وضعیت
            val strip = UI.innerCard(act, T.cardSoft, 14, 12)
            strip.orientation = LinearLayout.VERTICAL
            val dueRow = LinearLayout(act)
            dueRow.orientation = LinearLayout.HORIZONTAL
            dueRow.gravity = Gravity.CENTER_VERTICAL
            dueRow.addView(UI.icon(act, R.drawable.ic_cal_clock, 14, if (overdueC) T.rose else T.sub))
            val dueTxt = UI.text(act, "سررسید " + J.jFa(chk.dueDate) + (if (chk.status == "drawer" || chk.status == "inBank") " (" + (if (days >= 0) J.faN(days) + " روز مانده" else J.faN(-days) + " روز گذشته") + ")" else ""), 11, if (overdueC) T.rose else T.sub, true)
            val dl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            dl.marginStart = UI.dp(act, 6)
            dueRow.addView(dueTxt, dl)
            strip.addView(dueRow)
            val actions = NEXT_STATUSES[chk.status] ?: emptyList()
            if (actions.isNotEmpty()) {
                val btnRow = LinearLayout(act)
                btnRow.orientation = LinearLayout.HORIZONTAL
                for (ns in actions) {
                    val btn = UI.text(act, ns.second, 10.5f, T.tone(ns.third), true)
                    btn.gravity = Gravity.CENTER
                    btn.background = UI.stroke(act, T.toneSoft(ns.third), 12, (0x44 shl 24) or (T.tone(ns.third) and 0xFFFFFF), 1)
                    btn.setPadding(UI.dp(act, 8), UI.dp(act, 6), UI.dp(act, 8), UI.dp(act, 7))
                    btn.setOnClickListener {
                        val saved = Db.update { d -> d.copy(checks = d.checks.map { if (it.id == chk.id) it.copy(status = ns.first) else it }) }
                        if (saved) {
                            act.refresh()
                            UI.toast(act, "وضعیت چک شمارهٔ " + J.fa(chk.serial) + " تغییر کرد — " + LB.checkStatusLabel(ns.first) + (if (ns.first == "cleared") " — سند خودکار ثبت شد" else ""))
                        }
                    }
                    btnRow.addView(btn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { if (btnRow.childCount > 0) it.marginStart = UI.dp(act, 6) })
                }
                strip.addView(btnRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
            }
            card.addView(strip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
            listWrap.addView(card, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

fun checkEditorSheet(act: MainActivity, c0: Check) {
    val bs = BottomSheet(act)
    var chk = c0
    val isNew = !Db.db.checks.any { it.id == chk.id }
    bs.show(if (isNew) (if (chk.kind == "received") "چک دریافتی جدید" else "چک پرداختی جدید") else "ویرایش چک",
        "چرخهٔ کامل چک: صندوق، بانک، وصول، برگشت، خرج") { b ->

        val kindSel = UI.seg(act, listOf(Triple("دریافتی", -1, "received"), Triple("پرداختی", -1, "issued")),
            if (chk.kind == "received") 0 else 1) { i -> chk = chk.copy(kind = if (i == 0) "received" else "issued") }
        b.addView(kindSel)

        val pLabel = ValueLabel()
        val pSel = UI.selectField(act, "طرف‌حساب", pLabel.current) {
            optionSheet(act, "طرف‌حساب", Db.db.partners.map { Opt(it.id, it.name) }, chk.partnerId) { o ->
                chk = chk.copy(partnerId = o.id)
                pLabel.set(o.label)
            }
        }
        pLabel.bind(pSel, Db.db.partners.firstOrNull { it.id == chk.partnerId }?.name ?: "انتخاب کنید")
        b.addView(pSel)

        val bank = UI.field(act, "بانک صادرکننده", chk.bankName, hint = "بانک ملت")
        val serial = UI.field(act, "شمارهٔ چک", chk.serial, ltr = true, center = true)
        hookText(bank.first) { v -> chk = chk.copy(bankName = v) }
        hookText(serial.first) { v -> chk = chk.copy(serial = v) }
        val r1 = LinearLayout(act)
        r1.orientation = LinearLayout.HORIZONTAL
        r1.addView(bank.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r1.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r1.addView(serial.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r1)

        val amount = UI.numField(act, "مبلغ (تومان)", chk.amount)
        hookNum(amount) { v -> chk = chk.copy(amount = v) }
        b.addView(amount)

        val d1 = ValueLabel()
        val d2 = ValueLabel()
        val dateF = UI.dateField(act, "تاریخ دریافت/صدور", chk.date) {
            dateSheet(act, chk.date) { v -> chk = chk.copy(date = v); d1.set(J.jFa(v)) }
        }
        d1.bind(dateF, J.jFa(chk.date))
        val dueF = UI.dateField(act, "تاریخ سررسید", chk.dueDate) {
            dateSheet(act, chk.dueDate) { v -> chk = chk.copy(dueDate = v); d2.set(J.jFa(v)) }
        }
        d2.bind(dueF, J.jFa(chk.dueDate))
        val r2 = LinearLayout(act)
        r2.orientation = LinearLayout.HORIZONTAL
        r2.addView(dateF, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r2.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r2.addView(dueF, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r2)

        val desc = UI.field(act, "توضیحات", chk.desc)
        hookText(desc.first) { v -> chk = chk.copy(desc = v) }
        b.addView(desc.first)

        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ثبت چک") {
                val current = Db.db
                if (chk.amount <= 0 || chk.serial.trim().isEmpty()) { UI.toast(act, "مبلغ و شمارهٔ چک را کامل کنید"); return@primaryBtn }
                if (chk.bankName.trim().isEmpty()) { UI.toast(act, "نام بانک صادرکننده را وارد کنید"); return@primaryBtn }
                if (chk.partnerId.isBlank() || current.partners.none { it.id == chk.partnerId }) { UI.toast(act, "طرف‌حساب چک را انتخاب کنید"); return@primaryBtn }
                if (chk.date.isBlank() || chk.dueDate.isBlank()) { UI.toast(act, "تاریخ صدور/دریافت و سررسید الزامی است"); return@primaryBtn }
                if (J.jDiff(chk.dueDate, chk.date) < 0) { UI.toast(act, "سررسید چک نمی‌تواند قبل از تاریخ صدور/دریافت باشد"); return@primaryBtn }
                if (current.checks.any { it.id != chk.id && it.bankName.trim().equals(chk.bankName.trim(), true) && it.serial.trim() == chk.serial.trim() }) { UI.toast(act, "این شماره چک در همین بانک قبلاً ثبت شده است"); return@primaryBtn }
                val exists = current.checks.any { it.id == chk.id }
                val saved = Db.update { d -> d.copy(checks = if (exists) d.checks.map { if (it.id == chk.id) chk else it } else listOf(chk) + d.checks) }
                if (!saved) return@primaryBtn
                bs.close(); act.refresh()
                UI.toast(act, (if (exists) "چک ویرایش شد" else if (chk.kind == "received") "چک دریافتی ثبت شد" else "چک پرداختی ثبت شد") + " — " + J.moneyFa(chk.amount) + " تومان، سررسید " + J.jFa(chk.dueDate))
            }))
        if (!isNew) {
            val del = UI.outlineBtn(act, "حذف چک", danger = true, icRes = R.drawable.ic_trash) {
                bs.close()
                confirmSheet(act, "حذف چک؟", "چک شمارهٔ " + J.fa(chk.serial) + " حذف می‌شود.") {
                    Db.update { d -> d.copy(checks = d.checks.filter { it.id != chk.id }) }
                    act.refresh()
                    UI.toast(act, "چک حذف شد")
                }
            }
            b.addView(del, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        }
    }
}
