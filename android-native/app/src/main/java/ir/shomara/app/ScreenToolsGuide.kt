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
 * ابزار و تنظیمات — اطلاعات فروشگاه، پشتیبان‌گیری/بازیابی، بانک‌ها، یادآورها و بازنشانی
 */

fun toolsScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    // اطلاعات فروشگاه
    val shop = UI.card(c)
    shop.addView(UI.sectionTitle(c, "اطلاعات فروشگاه", "روی سربرگ و فاکتورها نمایش داده می‌شود", R.drawable.ic_store, topMargin = 0))
    val sName = UI.field(act, "نام فروشگاه", db.settings.shopName)
    val sOwner = UI.field(act, "نام مدیر", db.settings.ownerName)
    val sPhone = UI.field(act, "تلفن", db.settings.phone, ltr = true, center = true)
    val sYear = UI.field(act, "سال مالی", db.settings.fiscalYear, center = true)
    val sVat = UI.numField(act, "نرخ مالیات پیش‌فرض", db.settings.vatRate, "٪")
    val sAddr = UI.field(act, "آدرس", db.settings.address)
    val r1 = LinearLayout(c)
    r1.orientation = LinearLayout.HORIZONTAL
    r1.addView(sOwner.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    r1.addView(View(c), LinearLayout.LayoutParams(UI.dp(c, 10), ViewGroup.LayoutParams.WRAP_CONTENT))
    r1.addView(sPhone.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    val r2 = LinearLayout(c)
    r2.orientation = LinearLayout.HORIZONTAL
    r2.addView(sYear.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    r2.addView(View(c), LinearLayout.LayoutParams(UI.dp(c, 10), ViewGroup.LayoutParams.WRAP_CONTENT))
    r2.addView(sVat, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    shop.addView(sName.first, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
    shop.addView(r1)
    shop.addView(r2)
    shop.addView(sAddr.first)
    shop.addView(UI.switchRow(c, "مؤدی مالیاتی", "ارسال صورتحساب به سامانهٔ مودیان", db.settings.moudian) { v -> act.tmpMoudian = v })
    act.tmpMoudian = db.settings.moudian // از وضعیت فعلی شروع شود — نه اسنپ‌شات قدیمی
    val save = UI.primaryBtn(c, "ذخیرهٔ تنظیمات", icRes = R.drawable.ic_gear) {
        Db.update { d -> d.copy(settings = d.settings.copy(
            shopName = sName.second.text.toString(), ownerName = sOwner.second.text.toString(),
            phone = sPhone.second.text.toString(), address = sAddr.second.text.toString(),
            vatRate = J.parseLong(findEditor(sVat).text.toString()),
            fiscalYear = sYear.second.text.toString(), moudian = act.tmpMoudian
        )) }
        act.refresh()
        UI.toast(c, "تنظیمات ذخیره شد — " + sName.second.text.toString())
    }
    shop.addView(save, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
    root.addView(shop)

    // پشتیبان‌گیری
    val backup = UI.card(c)
    backup.addView(UI.sectionTitle(c, "پشتیبان‌گیری و بازیابی", "کل داده‌ها در یک فایل JSON", R.drawable.ic_backup, topMargin = 0))
    backup.addView(UI.btnRow(c,
        UI.outlineBtn(c, "دانلود پشتیبان", icRes = R.drawable.ic_download) { act.backupJson() },
        UI.primaryBtn(c, "بازیابی از فایل", icRes = R.drawable.ic_upload) { act.restoreJson() }))
    val note = UI.text(c, "پیشنهاد: هر هفته یک نسخهٔ پشتیبان بگیرید. بازیابی، همهٔ دادهٔ فعلی را با فایل پشتیبان جایگزین می‌کند.", 10.5f, T.sub)
    note.setLineSpacing(0f, 1.2f)
    backup.addView(note, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
    root.addView(backup)

    // گزارش خطاها — دیده‌شدن و ارسال لاگ، مثل Sentry در اپ‌های حرفه‌ای
    val errCard = UI.card(c)
    errCard.addView(UI.sectionTitle(c, "گزارش خطاها", "اگر جایی چیزی درست کار نکرد، همین‌جا ببینید و بفرستید", R.drawable.ic_alert, topMargin = 0))
    errCard.addView(UI.btnRow(c,
        UI.outlineBtn(c, "مشاهدهٔ گزارش", icRes = R.drawable.ic_search) { crashLogSheet(act) },
        UI.primaryBtn(c, "ارسال گزارش", icRes = R.drawable.ic_send) { ErrorReport.share(c, "from:tools") }))
    errCard.addView(UI.text(c, "اگر خطایی رخ داده باشد، فهرست کامل با جزئیات فنی اینجا ذخیره می‌شود و با یک لمس قابل ارسال است.", 10.5f, T.sub),
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
    root.addView(errCard)

    // بانک‌ها و صندوق‌ها
    val banks = UI.card(c)
    val addBankBtn = UI.text(c, "+ جدید", 11, Color.WHITE, true)
    addBankBtn.background = UI.rounded(c, T.primary, 12)
    addBankBtn.setPadding(UI.dp(c, 10), UI.dp(c, 6), UI.dp(c, 10), UI.dp(c, 7))
    addBankBtn.setOnClickListener { bankEditorSheet(act, null) }
    banks.addView(UI.sectionTitle(c, "بانک‌ها و صندوق‌ها", J.faN(db.banks.size) + " حساب", R.drawable.ic_landmark, addBankBtn, topMargin = 0))
    for (b in db.banks) {
        val row = UI.listRow(c, b.name,
            LB.bankKindLabel(b.kind) + (if (b.accountNo.isNotEmpty()) " • حساب " + J.fa(b.accountNo) else "") + (if (b.branch.isNotEmpty()) " • " + b.branch else "") + (if (b.cardNo.isNotEmpty()) " • کارت " + J.fa(b.cardNo.takeLast(4)) else ""),
            UI.endCol(c, J.moneyShort(Db.bankBalance(db, b.id)) + " ت"),
            null, icRes = R.drawable.ic_wallet,
            click = { bankEditorSheet(act, b) })
        banks.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
    }
    root.addView(banks)

    // یادآورها — با جستجو و حالت خالی مثل نسخهٔ وب
    val rem = UI.card(c)
    val addRemBtn = UI.text(c, "+ جدید", 11, Color.WHITE, true)
    addRemBtn.background = UI.rounded(c, T.primary, 12)
    addRemBtn.setPadding(UI.dp(c, 10), UI.dp(c, 6), UI.dp(c, 10), UI.dp(c, 7))
    addRemBtn.setOnClickListener { reminderEditorSheet(act, Reminder("", J.jTodayStr(), "", "", false)) }
    rem.addView(UI.sectionTitle(c, "یادآورها", J.faN(db.reminders.count { !it.done }) + " کار باز", R.drawable.ic_bell, addRemBtn, topMargin = 0))
    val (remBox, remE) = UI.search(c, "جستجو در یادآورها…")
    rem.addView(remBox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
    val remWrap = LinearLayout(c)
    rem.addView(remWrap)
    fun renderRems() {
        val q = remE.text.toString().trim()
        remWrap.removeAllViews()
        val sorted = db.reminders.sortedBy { it.date }
        val visible = if (q.isEmpty()) sorted else sorted.filter { it.title.contains(q, true) || it.desc.contains(q, true) || it.category.contains(q, true) }
        if (visible.isEmpty()) {
            val em = UI.empty(c, if (q.isEmpty()) "یادآوری نیست" else "چیزی پیدا نشد", "یادآورها مثل سررسید چک و تماس با مشتری", R.drawable.ic_bell)
            remWrap.addView(em, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
            return
        }
        for (r0 in visible) {
        val row = UI.innerCard(c, T.cardSoft, 16, 12)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val check = UI.icon(c, if (r0.done) R.drawable.ic_check_c else R.drawable.ic_circle, 20, if (r0.done) T.primary else T.sub)
        check.setOnClickListener {
            Db.update { d -> d.copy(reminders = d.reminders.map { if (it.id == r0.id) it.copy(done = !it.done) else it }) }
            act.refresh()
        }
        row.addView(check)
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        cl.marginStart = UI.dp(c, 10)
        val title = UI.text(c, r0.title, 12.5f, if (r0.done) T.sub else T.text, true)
        if (r0.done) title.paintFlags = title.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        col.addView(title)
        col.addView(UI.text(c, J.jFa(r0.date) + " • " + (when (r0.priority) { "urgent" -> "فوری"; "high" -> "زیاد"; "low" -> "کم"; else -> "معمولی" }) + (if (r0.desc.isNotEmpty()) " • " + r0.desc else ""), 10, T.sub))
        row.addView(col, cl)
        val edit = UI.icon(c, R.drawable.ic_pencil, 16, T.sub)
        edit.setOnClickListener { reminderEditorSheet(act, r0) }
        row.addView(edit)
        val del = UI.icon(c, R.drawable.ic_trash, 16, T.rose)
        del.setOnClickListener {
            Db.update { d -> d.copy(reminders = d.reminders.filter { it.id != r0.id }) }
            act.refresh()
            UI.toast(c, "یادآور حذف شد")
        }
        val dl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dl.marginStart = UI.dp(c, 8)
        row.addView(del, dl)
        remWrap.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }
    remE.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b2: Int, c2: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b2: Int, c2: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { renderRems() }
    })
    renderRems()
    root.addView(rem)

    // بازنشانی
    val reset = UI.card(c)
    reset.addView(UI.sectionTitle(c, "بازنشانی داده‌ها", "برگشت به دادهٔ نمونهٔ اولیه", R.drawable.ic_shield, topMargin = 0))
    val warn = UI.innerCard(c, T.roseSoft, 16, 12)
    warn.orientation = LinearLayout.HORIZONTAL
    warn.addView(UI.icon(c, R.drawable.ic_shield, 16, T.rose))
    val wt = UI.text(c, "همهٔ کالاها، فاکتورها، اسناد و تنظیمات با دادهٔ نمونه جایگزین می‌شود. قبل از بازنشانی، پشتیبان بگیرید.", 11, T.rose)
    wt.setLineSpacing(0f, 1.2f)
    val wl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    wl.marginStart = UI.dp(c, 8)
    warn.addView(wt, wl)
    reset.addView(warn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
    val resetBtn = UI.outlineBtn(c, "بازنشانی کامل", danger = true, icRes = R.drawable.ic_rotate) {
        confirmSheet(c, "بازنشانی کامل داده‌ها؟", "همهٔ اطلاعات فعلی پاک شده و دادهٔ نمونهٔ اولیه برمی‌گردد. این کار قابل بازگشت نیست.", "بازنشانی") {
            Db.reset()
            act.refresh()
            UI.toast(c, "داده‌ها بازنشانی شد")
        }
    }
    reset.addView(resetBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
    root.addView(reset)

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

fun bankEditorSheet(act: MainActivity, b0: Bank?) {
    val bs = BottomSheet(act)
    var b = b0 ?: Bank("", "", "cash", "", 0)
    val isNew = b.id.isEmpty()
    bs.show(if (isNew) "صندوق / بانک جدید" else "ویرایش حساب", "صندوق، حساب بانکی یا کارتخوان را ثبت کنید") { bd ->
        val name = UI.field(act, "نام", b.name)
        hookText(name.first) { v -> b = b.copy(name = v) }
        bd.addView(name.first)

        val kLabel = ValueLabel()
        val kSel = UI.selectField(act, "نوع", kLabel.current) {
            optionSheet(act, "نوع حساب", listOf(Opt("cash", "صندوق نقدی"), Opt("bank", "حساب بانکی"), Opt("pos", "کارتخوان")), b.kind) { o ->
                b = b.copy(kind = o.id)
                kLabel.set(o.label)
            }
        }
        kLabel.bind(kSel, LB.bankKindLabel(b.kind))
        bd.addView(kSel)

        val accNo = UI.field(act, "شمارهٔ حساب (اختیاری)", b.accountNo, ltr = true, center = true)
        val opening = UI.numField(act, "ماندهٔ اولیه", b.opening)
        val branch = UI.field(act, "نام شعبه (اختیاری)", b.branch)
        val holder = UI.field(act, "نام صاحب حساب (اختیاری)", b.accountHolder)
        val iban = UI.field(act, "شماره شبا (اختیاری)", b.iban, hint = "IR…", ltr = true, center = true)
        val cardNo = UI.field(act, "شماره کارت (اختیاری)", b.cardNo, hint = "۱۶ رقم", ltr = true, center = true)
        val bDesc = UI.fieldMulti(act, "شرح / توضیحات", b.desc)
        hookText(accNo.first) { v -> b = b.copy(accountNo = v.trim()) }
        hookNum(opening) { v -> b = b.copy(opening = v) }
        hookText(branch.first) { v -> b = b.copy(branch = v.trim()) }
        hookText(holder.first) { v -> b = b.copy(accountHolder = v.trim()) }
        hookText(iban.first) { v -> b = b.copy(iban = v.trim().uppercase().replace(" ", "")) }
        hookText(cardNo.first) { v -> b = b.copy(cardNo = v.filter { it.isDigit() }) }
        hookText(bDesc.first) { v -> b = b.copy(desc = v.trim()) }
        bd.addView(branch.first)
        bd.addView(holder.first)
        bd.addView(iban.first)
        bd.addView(cardNo.first)
        bd.addView(bDesc.first)
        val r = LinearLayout(act)
        r.orientation = LinearLayout.HORIZONTAL
        r.addView(accNo.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r.addView(opening, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        bd.addView(r)

        bd.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ذخیره") {
                if (b.name.trim().isEmpty()) { UI.toast(act, "نام صندوق/بانک را بنویسید"); return@primaryBtn }
                if (b.accountNo.isNotBlank() && b.accountNo.filter { it.isDigit() }.length !in 6..30) { UI.toast(act, "شماره حساب نامعتبر است"); return@primaryBtn }
                if (b.iban.isNotBlank() && !Regex("IR[0-9]{24}").matches(b.iban.uppercase())) { UI.toast(act, "شبا باید با IR و ۲۴ رقم وارد شود"); return@primaryBtn }
                if (b.cardNo.isNotBlank() && b.cardNo.filter { it.isDigit() }.length != 16) { UI.toast(act, "شماره کارت باید ۱۶ رقم باشد"); return@primaryBtn }
                if (b.opening < 0 && b.kind != "cash") { UI.toast(act, "ماندهٔ اولیه حساب بانکی نمی‌تواند منفی باشد"); return@primaryBtn }
                val saved = if (isNew) {
                    b = b.copy(id = Db.newId("bk"))
                    Db.update { d -> d.copy(banks = d.banks + b) }
                } else {
                    Db.update { d -> d.copy(banks = d.banks.map { if (it.id == b.id) b else it }) }
                }
                if (!saved) { UI.toast(act, "ذخیرهٔ حساب انجام نشد؛ اطلاعات را نگه دارید و دوباره تلاش کنید"); return@primaryBtn }
                bs.close(); act.refresh()
                UI.toast(act, "ذخیره شد — " + b.name)
            }))
        if (!isNew && Db.db.banks.size > 1) {
            val del = UI.outlineBtn(act, "حذف حساب", danger = true, icRes = R.drawable.ic_trash) {
                bs.close()
                confirmSheet(act, "حذف حساب؟", "«" + b.name + "» حذف می‌شود؛ اسناد قبلی حفظ می‌شوند.") {
                    Db.update { d -> d.copy(banks = d.banks.filter { it.id != b.id }) }
                    act.refresh()
                    UI.toast(act, "حساب حذف شد")
                }
            }
            bd.addView(del, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        }
    }
}

fun reminderEditorSheet(act: MainActivity, r0: Reminder) {
    val bs = BottomSheet(act)
    var r = r0
    val isNew = r.id.isEmpty()
    bs.show(if (isNew) "یادآور جدید" else "ویرایش یادآور", "مثلاً سررسید چک، تماس با مشتری، سفارش بار") { b ->
        val title = UI.field(act, "عنوان", r.title)
        hookText(title.first) { v -> r = r.copy(title = v) }
        b.addView(title.first)
        val dLabel = ValueLabel()
        val dField = UI.dateField(act, "تاریخ", r.date) {
            dateSheet(act, r.date) { v -> r = r.copy(date = v); dLabel.set(J.jFa(v)) }
        }
        dLabel.bind(dField, J.jFa(r.date))
        b.addView(dField)
        val desc = UI.fieldMulti(act, "توضیحات / اقدام بعدی", r.desc)
        hookText(desc.first) { v -> r = r.copy(desc = v) }
        b.addView(desc.first)
        val catLabel = ValueLabel()
        val catSel = UI.selectField(act, "دستهٔ یادآور", catLabel.current) {
            val options = listOf(Opt("general", "عمومی"), Opt("finance", "مالی / تسویه"), Opt("sales", "فروش / مشتری"), Opt("purchase", "خرید / تأمین"), Opt("inventory", "انبار"), Opt("project", "پروژه"), Opt("tax", "مالیات"))
            optionSheet(act, "دستهٔ یادآور", options, r.category) { o -> r = r.copy(category = o.id); catLabel.set(o.label) }
        }
        catLabel.bind(catSel, listOf(Opt("general", "عمومی"), Opt("finance", "مالی / تسویه"), Opt("sales", "فروش / مشتری"), Opt("purchase", "خرید / تأمین"), Opt("inventory", "انبار"), Opt("project", "پروژه"), Opt("tax", "مالیات")).firstOrNull { it.id == r.category }?.label ?: "عمومی")
        b.addView(catSel)
        val prLabel = ValueLabel()
        val prSel = UI.selectField(act, "اولویت", prLabel.current) {
            optionSheet(act, "اولویت یادآور", listOf(Opt("low", "کم"), Opt("normal", "معمولی"), Opt("high", "زیاد"), Opt("urgent", "فوری")), r.priority) { o -> r = r.copy(priority = o.id); prLabel.set(o.label) }
        }
        prLabel.bind(prSel, listOf(Opt("low", "کم"), Opt("normal", "معمولی"), Opt("high", "زیاد"), Opt("urgent", "فوری")).firstOrNull { it.id == r.priority }?.label ?: "معمولی")
        b.addView(prSel)
        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ذخیره") {
                if (r.title.trim().isEmpty()) { UI.toast(act, "عنوان یادآور را بنویسید"); return@primaryBtn }
                val saved = if (isNew) {
                    r = r.copy(id = Db.newId("rm"))
                    Db.update { d -> d.copy(reminders = d.reminders + r) }
                } else {
                    Db.update { d -> d.copy(reminders = d.reminders.map { if (it.id == r.id) r else it }) }
                }
                if (!saved) { UI.toast(act, "یادآور ذخیره نشد؛ دوباره تلاش کنید"); return@primaryBtn }
                bs.close(); act.refresh()
                UI.toast(act, "یادآور ذخیره شد — " + r.title)
            }))
    }
}

/* ═════════════════════════ راهنمای شمارا ═════════════════════════ */

data class GVersion(val code: String, val name: String, val price: Long?, val priceNote: String, val audience: String, val features: List<String>, val highlight: Boolean)

val RETAIL_VERSIONS = listOf(
    GVersion("۱۱", "شمارا APEX فروشگاهی ساده", 4_700_000, "", "فروشگاه‌های خرد و تک‌صندوقه که تازه وارد دنیای حسابداری می‌شوند",
        listOf("انبار و خرید/فروش", "مالی کاملاً خودکار", "گردش چک، بانک و صندوق", "۱۰ تیپ قیمت برای هر کالا"), false),
    GVersion("۱۲", "شمارا APEX فروشگاهی متوسط", 9_700_000, "۹٫۷ میلیون تومان به بالا", "مغازه‌هایی که گردش چک و بانک پرحجم دارند و مدیریت هزینه می‌خواهند",
        listOf("همه امکانات کد ۱۱", "مدیریت کامل اسناد دریافت/پرداخت", "کنترل هزینه به تفکیک مرکز هزینه", "گزارش‌های خزانه پیشرفته"), false),
    GVersion("۱۳", "شمارا APEX فروشگاهی پیشرفته", 15_700_000, "", "بنگاه‌های دارای چند انبار یا چند فروشگاه با کالاهای پرتعداد",
        listOf("همه امکانات کد ۱۲", "انبار چندگانه و انتقال بین انبارها", "سفارش خرید و پیش‌فاکتور", "گزارش‌ساز قدرتمند"), true),
    GVersion("۱۴", "شمارا APEX فروشگاهی ویژه", null, "قیمت بر اساس پیکربندی", "فروشگاه‌های بزرگ با نیازهای خاص مثل باشگاه مشتریان و اتصال فروشگاه اینترنتی",
        listOf("همه امکانات کد ۱۳", "امکانات ویژه صنفی", "اتصال به سامانه‌های جانبی"), false),
    GVersion("۱۵", "شمارا APEX فروشگاهی ۲ کاربره", 34_700_000, "", "تیم‌های فروش چندنفره که هم‌زمان روی یک پایگاه داده کار می‌کنند",
        listOf("نسخه شبکه‌ای چندکاربره", "نقش‌ها و سطح دسترسی", "مناسب شعبه‌دارها"), false)
)

val OTHER_FAMILIES = listOf(
    Triple("سری شرکتی", 6_700_000L, "برای شرکت‌های بازرگانی با حجم اسناد بالا (کدهای ۲۱ به بالا)"),
    Triple("سری تولیدی", 12_700_000L, "مدیریت تولید، BOM و مراکز هزینه تولید (کدهای ۳۱ به بالا)"),
    Triple("سری جامع", 35_700_000L, "پوشش کامل مالی، تجاری و تولیدی سازمان‌های بزرگ (کدهای ۴۱ به بالا)")
)

val G_MODULES = listOf(
    Triple(R.drawable.ic_calc, "حسابداری مالی", "صدور خودکار اسناد، دفاتر قانونی، ترازنامه و صورت سود و زیان بدون نیاز به دانش حسابداری"),
    Triple(R.drawable.ic_boxes, "انبار و کالا", "کاردکس کالا، شمارش فیزیکی، اعلان اتمام موجودی و قیمت‌گذاری چندلایه"),
    Triple(R.drawable.ic_receipt, "خرید و فروش", "فاکتور، پیش‌فاکتور، مرجوعی و تخفیف با محاسبه خودکار ارزش افزوده"),
    Triple(R.drawable.ic_wallet, "خزانه، چک و بانک", "مدیریت چک‌های دریافتی و پرداختی، وصول، خرج و برگشت با یادآور سررسید"),
    Triple(R.drawable.ic_users, "مدیریت مشتریان", "پروفایل مشتری، سقف بدهی، مانده حساب و تاریخچه کامل خریدها"),
    Triple(R.drawable.ic_chart, "گزارش‌ساز", "بیش از صدها گزارش آماده به‌همراه طراحی گزارش دلخواه و خروجی Excel"),
    Triple(R.drawable.ic_landmark, "سامانه مودیان", "ارسال صورتحساب‌ها به سامانه مودیان مالیاتی مطابق قانون جدید"),
    Triple(R.drawable.ic_phone, "اپلیکیشن و ابر", "مشاهده گزارش‌ها و ثبت فاکتور از موبایل و نسخه تحت وب برای کار از راه دور")
)

val PROS = listOf(
    "صدور خودکار اسناد حسابداری؛ نیازی به ثبت دستی نیست",
    "رابط کاملاً فارسی و آشنا برای کسب‌وکارهای ایرانی",
    "پشتیبانی گسترده در سراسر کشور و جامعه کاربری بزرگ",
    "به‌روزرسانی منظم و سازگاری با قوانین جدید مالیاتی",
    "امکان ارتقا از نسخه فروشگاهی به شرکتی و تولیدی"
)

val CONS = listOf(
    "هزینه خرید اولیه و به‌روزرسانی سالانه نسبتاً بالا برای مشاغل خرد",
    "ظاهر نسخه دسکتاپ نسبت به ابزارهای مدرن‌تر قدیمی‌تر است",
    "آموزش اولیه برای کاربر تازه‌کار زمان‌بر است",
    "قیمت‌گذاری کد به کد است و پیدا کردن نسخه مناسب کمی گیج‌کننده می‌شود"
)

val BUY_TIPS = listOf(
    Pair("اول دموی رایگان را تست کنید", "قبل از خرید، نسخهٔ نمایشی نرم‌افزار حسابداری را نصب کنید و یک فاکتور واقعی خودتان را در آن بزنید."),
    Pair("هزینه سال بعد را هم حساب کنید", "به‌روزرسانی و پشتیبانی سالانه هزینه جداگانه دارد؛ آن را در بودجه بگنجانید."),
    Pair("نسخه را از روی نیاز بخرید نه از روی قیمت", "اگر چک زیاد دارید حداقل کد ۱۲ و اگر چند انبار دارید حداقل کد ۱۳ را بگیرید."),
    Pair("آموزش اولیه را جدی بگیرید", "دو روز آموزش شروع، ماه‌ها خطای ثبت را کم می‌کند؛ فیلم‌های آموزشی رسمی را ببینید.")
)

val FAQ = listOf(
    Pair("نرم‌افزار حسابداری کامل برای فروشگاه کوچک من لازم است یا نرم‌افزارهای ساده‌تر کافی است؟", "اگر فقط فروش نقدی و ثبت ساده دارید، نرم‌افزارهای سبک‌تر ارزان‌تر تمام می‌شوند. اما به‌محض اینکه چک، قسط، چند انبار یا گزارش مالیاتی به کارتان اضافه شود، صدور خودکار اسناد زمان و خطا را به‌شدت کم می‌کند."),
    Pair("تفاوت سری فروشگاهی با سری شرکتی چیست؟", "سری فروشگاهی (کدهای ۱۱ تا ۱۵) برای مغازه و خرده‌فروشی طراحی شده و ساده‌تر و ارزان‌تر است. سری شرکتی (کد ۲۱ به بالا) اسناد پرحجم، چند کاربر شبکه‌ای و مدیریت هزینه‌های سازمانی را پوشش می‌دهد."),
    Pair("قیمت‌ها ثابت است یا مذاکره‌پذیر؟", "قیمت پایه از سمت شرکت سازنده اعلام می‌شود اما نمایندگی‌ها تخفیف‌های متفاوتی می‌دهند و معمولاً هزینه نصب و آموزش را رایگان انجام می‌دهند. از چند نمایندگی استعلام بگیرید."),
    Pair("ارتباط با سامانه مودیان چطور انجام می‌شود؟", "نسخه‌های به‌روز نرم‌افزار به‌صورت مستقیم صورتحساب را به سامانه مودیان سازمان امور مالیاتی ارسال می‌کنند؛ مطمئن شوید نسخه نصب‌شده شما آخرین به‌روزرسانی را دارد.")
)

val PRICE_WARNING = "قیمت‌ها بر اساس لیست‌های عمومی منتشرشده از نمایندگی‌های رسمی است و ممکن است با تغییر سالانه یا تخفیف نمایندگی متفاوت باشد؛ برای رقم دقیق حتماً استعلام بگیرید."
val APEX_NOTE = "سری جدید این نرم‌افزار با نام تجاری «شمارا APEX» عرضه می‌شود؛ کدهای ۱۱ تا ۱۵ همان نسخه‌های فروشگاهی هستند و امکانات مالی همه نسخه‌ها یکسان و خودکار است."

fun guideScreen(act: MainActivity): View {
    val c = act
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    val tabs = listOf(Triple("امکانات شمارا", -1, "features"), Triple("نسخه و قیمت", -1, "versions"), Triple("آزمون انتخاب", -1, "quiz"), Triple("نکات خرید", -1, "tips"))
    val selIdx = tabs.indexOfFirst { it.third == act.guideTab }.coerceAtLeast(0)
    root.addView(UI.segScroll(c, tabs, selIdx) { i ->
        act.guideTab = tabs[i].third
        act.refresh()
    })

    val body = LinearLayout(c)
    root.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
    val tab = act.guideTab

    if (tab == "features") {
        val grid = LinearLayout(c)
        grid.orientation = LinearLayout.VERTICAL
        for (chunk in G_MODULES.chunked(2)) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            for ((i, m) in chunk.withIndex()) {
                val card = LinearLayout(c)
                card.orientation = LinearLayout.VERTICAL
                card.background = UI.stroke(c, T.card, 18, T.border, 1)
                card.setPadding(UI.dp(c, 14), UI.dp(c, 14), UI.dp(c, 14), UI.dp(c, 14))
                card.addView(UI.iconBox(c, m.first, T.primary, T.primarySoft, 44, 22, 14))
                val t = UI.text(c, m.second, 13.5f, T.text, true)
                t.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) }
                card.addView(t)
                val d = UI.text(c, m.third, 11.5f, T.sub)
                d.setLineSpacing(0f, 1.15f)
                card.addView(d, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) })
                row.addView(card, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
                if (i == 0) row.addView(View(c), LinearLayout.LayoutParams(UI.dp(c, 10), ViewGroup.LayoutParams.MATCH_PARENT))
            }
            grid.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
        body.addView(grid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })

        val moudian = UI.card(c)
        moudian.background = UI.gradient(c, 0xFF047857.toInt(), 0xFF134E4A.toInt(), 20)
        moudian.addView(UI.text(c, "مطابقت با قانون مودیان", 14, Color.WHITE, true))
        val mt = UI.text(c, "از نسخه‌های به‌روز، صورتحساب‌ها مستقیماً به سامانه مودیان سازمان امور مالیاتی ارسال می‌شود؛ یعنی یک کار جداگانه برای مالیات کمتر. هنگام خرید حتماً نسخه پشتیبان مودیان را بخواهید.", 12.5f, 0xD9ECFDF5.toInt())
        mt.setLineSpacing(0f, 1.3f)
        moudian.addView(mt, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        body.addView(moudian, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
    }

    if (tab == "versions") {
        body.addView(UI.sectionTitle(c, "نسخه‌های فروشگاهی شمارا APEX", "کدهای ۱۱ تا ۱۵ — از ساده تا چندکاربره", R.drawable.ic_store))
        for (v in RETAIL_VERSIONS) {
            val card = UI.card(c)
            if (v.highlight) card.background = UI.stroke(c, T.card, 18, T.primary, 2)
            if (v.highlight) {
                card.addView(UI.chip(c, "پیشنهاد اکثر فروشگاه‌ها", "green", R.drawable.ic_sparkles), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.bottomMargin = UI.dp(c, 8) })
            }
            val head = LinearLayout(c)
            head.orientation = LinearLayout.HORIZONTAL
            head.gravity = Gravity.CENTER_VERTICAL
            val code = UI.text(c, v.code, 17, Color.WHITE, true)
            code.gravity = Gravity.CENTER
            code.background = UI.gradient(c, T.primary, 0xFF0D9488.toInt(), 14)
            code.layoutParams = LinearLayout.LayoutParams(UI.dp(c, 48), UI.dp(c, 48))
            head.addView(code)
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            cl.marginStart = UI.dp(c, 12)
            col.addView(UI.text(c, v.name, 14, T.text, true))
            col.addView(UI.text(c, if (v.price != null) J.moneyFa(v.price) + " تومان" else v.priceNote, 14, T.amber, true), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 3) })
            head.addView(col, cl)
            card.addView(head)
            if (v.price != null) {
                val track = View(c)
                track.background = UI.rounded(c, if (T.dark) 0xFF232A33.toInt() else 0xFFE4E8EC.toInt(), 8)
                val pct = Math.max(9, ((v.price * 100) / 34_700_000L)).toInt()
                val wrap = android.widget.FrameLayout(c)
                wrap.addView(track, android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 8)))
                val fill = View(c)
                fill.background = UI.gradient(c, 0xFF10B981.toInt(), 0xFF0D9488.toInt(), 8)
                wrap.addView(fill, android.widget.FrameLayout.LayoutParams(UI.dp(c, pct), UI.dp(c, 8)))
                card.addView(wrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 8)).also { it.topMargin = UI.dp(c, 12) })
            }
            val aud = UI.text(c, "", 12, T.sub)
            aud.setLineSpacing(0f, 1.25f)
            aud.text = "مناسب برای: " + v.audience
            card.addView(aud, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
            val chips = LinearLayout(c)
            chips.orientation = LinearLayout.HORIZONTAL
            for (f in v.features) {
                chips.addView(UI.chip(c, f, "slate"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = UI.dp(c, 6); it.topMargin = UI.dp(c, 6) })
            }
            card.addView(chips, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            body.addView(card)
        }
        body.addView(UI.sectionTitle(c, "سایر سری‌ها", "اگر فروشگاهی نیستید", R.drawable.ic_building, topMargin = 16))
        val familyIcons = listOf(R.drawable.ic_building, R.drawable.ic_factory, R.drawable.ic_globe)
        for ((fi, f) in OTHER_FAMILIES.withIndex()) {
            val card = UI.card(c)
            card.orientation = LinearLayout.HORIZONTAL
            card.gravity = Gravity.CENTER_VERTICAL
            card.addView(UI.iconBox(c, familyIcons[fi % familyIcons.size], 0xFF0D9488.toInt(), 0x1F0D9488.toInt(), 44, 20))
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            cl.marginStart = UI.dp(c, 12)
            val nameRow = LinearLayout(c)
            nameRow.orientation = LinearLayout.HORIZONTAL
            nameRow.addView(UI.text(c, f.first + "  ", 13, T.text, true))
            nameRow.addView(UI.text(c, "از " + J.moneyFa(f.second) + " تومان", 12, T.amber, true))
            col.addView(nameRow)
            val d = UI.text(c, f.third, 11.5f, T.sub)
            d.setLineSpacing(0f, 1.15f)
            col.addView(d)
            card.addView(col, cl)
            body.addView(card)
        }
        val warn = UI.innerCard(c, T.amberSoft, 16, 13)
        warn.orientation = LinearLayout.HORIZONTAL
        warn.addView(UI.icon(c, R.drawable.ic_alert, 17, T.amber))
        val wt = UI.text(c, PRICE_WARNING, 11.5f, T.amber)
        wt.setLineSpacing(0f, 1.2f)
        val wl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        wl.marginStart = UI.dp(c, 10)
        warn.addView(wt, wl)
        body.addView(warn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
    }

    if (tab == "quiz") {
        body.addView(guideQuiz(act))
    }

    if (tab == "tips") {
        body.addView(UI.sectionTitle(c, "مزایا و معایب", "روی ترازو بگذارید و تصمیم بگیرید", R.drawable.ic_scale))
        val prosCard = UI.card(c)
        prosCard.addView(UI.text(c, "نقاط قوت", 13, T.primary, true))
        for (p in PROS) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            val dot = View(c)
            dot.background = UI.oval(c, T.primary)
            row.addView(dot, LinearLayout.LayoutParams(UI.dp(c, 6), UI.dp(c, 6)).also { it.topMargin = UI.dp(c, 8) })
            val t = UI.text(c, p, 12.5f, T.text)
            t.setLineSpacing(0f, 1.25f)
            row.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(c, 10) })
            prosCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        }
        body.addView(prosCard)
        val consCard = UI.card(c)
        consCard.addView(UI.text(c, "نقاط ضعف", 13, T.rose, true))
        for (p in CONS) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            val dot = View(c)
            dot.background = UI.oval(c, 0xFFF43F5E.toInt())
            row.addView(dot, LinearLayout.LayoutParams(UI.dp(c, 6), UI.dp(c, 6)).also { it.topMargin = UI.dp(c, 8) })
            val t = UI.text(c, p, 12.5f, T.text)
            t.setLineSpacing(0f, 1.25f)
            row.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(c, 10) })
            consCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        }
        body.addView(consCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })

        body.addView(UI.sectionTitle(c, "۴ نکته طلایی قبل از خرید", "", R.drawable.ic_bulb, topMargin = 16))
        for ((i, tip) in BUY_TIPS.withIndex()) {
            val card = UI.card(c)
            card.orientation = LinearLayout.HORIZONTAL
            val num = UI.text(c, J.faN(i + 1), 14, Color.WHITE, true)
            num.gravity = Gravity.CENTER
            num.background = UI.gradient(c, 0xFFFBBF24.toInt(), 0xFFF59E0B.toInt(), 12)
            num.layoutParams = LinearLayout.LayoutParams(UI.dp(c, 36), UI.dp(c, 36))
            card.addView(num)
            val col = LinearLayout(c)
            col.orientation = LinearLayout.VERTICAL
            val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            cl.marginStart = UI.dp(c, 12)
            col.addView(UI.text(c, tip.first, 13, T.text, true))
            val d = UI.text(c, tip.second, 11.5f, T.sub)
            d.setLineSpacing(0f, 1.2f)
            col.addView(d, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
            card.addView(col, cl)
            body.addView(card)
        }

        body.addView(UI.sectionTitle(c, "سوال‌های پرتکرار", "روی هر سوال بزنید تا پاسخ باز شود", R.drawable.ic_info, topMargin = 16))
        for (f in FAQ) {
            val card = UI.card(c)
            val qRow = LinearLayout(c)
            qRow.orientation = LinearLayout.HORIZONTAL
            qRow.gravity = Gravity.CENTER_VERTICAL
            qRow.addView(UI.text(c, f.first, 12.5f, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val chev = UI.icon(c, R.drawable.ic_chev_d, 16, T.sub)
            qRow.addView(chev)
            card.addView(qRow)
            // آکاردئون — پاسخ با لمس باز و بسته می‌شود (مثل نسخهٔ وب)
            val a = UI.text(c, f.second, 12, T.sub)
            a.setLineSpacing(0f, 1.25f)
            a.visibility = View.GONE
            card.addView(a, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
            var open = false
            qRow.setOnClickListener {
                open = !open
                a.visibility = if (open) View.VISIBLE else View.GONE
                chev.animate().rotation(if (open) 180f else 0f).setDuration(180).start()
            }
            body.addView(card)
        }
    }

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

/** آزمون انتخاب نسخه — سه سوال + نتیجه */
fun guideQuiz(act: MainActivity): View {
    val c = act
    val saved = act.quizState  // "step|users|warehouse|cheque"
    val parts = saved.split("|")
    var step = parts.getOrNull(0)?.toIntOrNull() ?: 0
    var users = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: ""
    var warehouse = parts.getOrNull(2)?.takeIf { it.isNotEmpty() } ?: ""
    var cheque = parts.getOrNull(3)?.takeIf { it.isNotEmpty() } ?: ""

    fun saveQuiz() { act.quizState = listOf(step.toString(), users, warehouse, cheque).joinToString("|"); act.refresh() }

    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL

    fun quizResultView(): View {
        val v = GVersion(when {
            users == "many" -> "۱۵"
            warehouse == "multi" -> "۱۳"
            cheque == "yes" -> "۱۲"
            else -> "۱۱"
        }, "", null, "", "", emptyList(), false)
        val res = RETAIL_VERSIONS.first { it.code == v.code }
        val reason = when (res.code) {
            "۱۵" -> "چون بیش از دو نفر قرار است هم‌زمان کار کنند، به نسخه شبکه‌ای چندکاربره نیاز دارید."
            "۱۳" -> "چون چند انبار یا فروشگاه دارید، مدیریت انبار چندگانه کد ۱۳ برایتان ضروری است."
            "۱۲" -> "با حجم چک و بانک، مدیریت کامل خزانه و یادآور سررسید کد ۱۲ خیالتان را راحت می‌کند."
            else -> "برای شروع ساده و کم‌هزینه، کد ۱۱ همه نیازهای یک فروشگاه تک‌صندوقه را پوشش می‌دهد."
        }
        val root = LinearLayout(c)
        root.orientation = LinearLayout.VERTICAL
        val hero = LinearLayout(c)
        hero.orientation = LinearLayout.VERTICAL
        hero.gravity = Gravity.CENTER
        hero.background = UI.gradient(c, 0xFF059669.toInt(), 0xFF047857.toInt(), 24)
        hero.setPadding(UI.dp(c, 20), UI.dp(c, 22), UI.dp(c, 20), UI.dp(c, 22))
        val chip = UI.chip(c, "پیشنهاد نهایی برای کسب‌وکار شما", "green")
        hero.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val code = UI.text(c, res.code, 26, Color.WHITE, true)
        code.gravity = Gravity.CENTER
        code.background = UI.rounded(c, 0x26FFFFFF.toInt(), 22)
        val codeLp = LinearLayout.LayoutParams(UI.dp(c, 64), UI.dp(c, 64))
        codeLp.topMargin = UI.dp(c, 16)
        hero.addView(code, codeLp)
        val nm = UI.text(c, res.name, 19, Color.WHITE, true)
        nm.gravity = Gravity.CENTER
        hero.addView(nm, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        val pr = UI.text(c, if (res.price != null) J.moneyFa(res.price) + " تومان" else res.priceNote, 16, 0xFFFFC53D.toInt(), true)
        pr.gravity = Gravity.CENTER
        hero.addView(pr, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
        val rs = UI.text(c, reason, 13, 0xD9ECFDF5.toInt())
        rs.gravity = Gravity.CENTER
        rs.setLineSpacing(0f, 1.3f)
        hero.addView(rs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
        root.addView(hero, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val feats = UI.card(c)
        feats.addView(UI.text(c, "امکاناتی که با این نسخه می‌گیرید", 13, T.text, true))
        for (f in res.features) {
            val row = LinearLayout(c)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.addView(UI.icon(c, R.drawable.ic_check_c, 16, T.primary))
            row.addView(UI.text(c, f, 12.5f, T.text), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(c, 10) })
            feats.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
        }
        val apexNote = UI.text(c, APEX_NOTE, 11.5f, T.sub)
        apexNote.setLineSpacing(0f, 1.25f)
        apexNote.background = UI.rounded(c, T.cardSoft, 14)
        apexNote.setPadding(UI.dp(c, 12), UI.dp(c, 10), UI.dp(c, 12), UI.dp(c, 10))
        feats.addView(apexNote, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })
        root.addView(feats)

        val warn = UI.innerCard(c, T.amberSoft, 16, 13)
        warn.orientation = LinearLayout.HORIZONTAL
        warn.addView(UI.icon(c, R.drawable.ic_alert, 17, T.amber))
        val wt = UI.text(c, PRICE_WARNING, 11.5f, T.amber)
        wt.setLineSpacing(0f, 1.2f)
        warn.addView(wt, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(c, 10) })
        root.addView(warn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 12) })

        val restart = UI.outlineBtn(c, "از اول", icRes = R.drawable.ic_rotate) { step = 0; users = ""; warehouse = ""; cheque = ""; saveQuiz() }
        val compare = UI.primaryBtn(c, "مقایسه همه نسخه‌ها") { act.guideTab = "versions"; act.refresh() }
        root.addView(UI.btnRow(c, restart, compare))
        return root
    }

    if (step >= 3) return quizResultView()

    data class Q(val key: String, val title: String, val ic: Int, val opts: List<Triple<String, String, String>>)
    val questions = listOf(
        Q("users", "چند نفر قرار است هم‌زمان با نرم‌افزار کار کنند؟", R.drawable.ic_usercheck, listOf(
            Triple("small", "۱ تا ۲ نفر", "خودم یا با یک همکار"),
            Triple("many", "۳ نفر یا بیشتر", "تیم فروش یا چند صندوق"))),
        Q("warehouse", "انبار شما چطور است؟", R.drawable.ic_wh, listOf(
            Triple("one", "یک انبار دارم", "همه کالاها یک‌جا"),
            Triple("multi", "چند انبار یا چند فروشگاه", "نیاز به انتقال بین انبارها"))),
        Q("cheque", "با مشتری‌ها و تامین‌کننده‌ها چک کار می‌کنید؟", R.drawable.ic_banknote, listOf(
            Triple("yes", "بله، چک زیاد دارم", "دریافت و پرداخت چک"),
            Triple("no", "معمولاً نقد یا کارت‌خوان", "چک جزو کار روزانه نیست")))
    )
    val q = questions[step]
    root.addView(UI.sectionTitle(c, "آزمون انتخاب نسخه شمارا", "سوال " + J.faN(step + 1) + " از " + J.faN(3) + " — کوتاه و دقیق", q.ic))
    // نوار پیشرفت
    val progress = LinearLayout(c)
    progress.orientation = LinearLayout.HORIZONTAL
    for (i in 0 until 3) {
        val seg = View(c)
        seg.background = UI.rounded(c, if (i <= step) T.primary else (if (T.dark) 0xFF232A33.toInt() else 0xFFE4E8EC.toInt()), 6)
        progress.addView(seg, LinearLayout.LayoutParams(0, UI.dp(c, 6), 1f).also { if (i > 0) it.marginStart = UI.dp(c, 4) })
    }
    root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })

    val card = UI.card(c)
    card.addView(UI.text(c, q.title, 15, T.text, true))
    for (o in q.opts) {
        val btn = LinearLayout(c)
        btn.orientation = LinearLayout.HORIZONTAL
        btn.gravity = Gravity.CENTER_VERTICAL
        btn.background = UI.stroke(c, T.card, 18, T.border, 2)
        btn.setPadding(UI.dp(c, 14), UI.dp(c, 13), UI.dp(c, 14), UI.dp(c, 13))
        btn.addView(UI.iconBox(c, R.drawable.ic_badge, T.primary, T.primarySoft, 44, 22))
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        val cl = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        cl.marginStart = UI.dp(c, 12)
        col.addView(UI.text(c, o.second, 13.5f, T.text, true))
        col.addView(UI.text(c, o.third, 11.5f, T.sub))
        btn.addView(col, cl)
        btn.addView(UI.icon(c, R.drawable.ic_chev_l, 15, T.sub))
        btn.setOnClickListener {
            when (q.key) {
                "users" -> users = o.first
                "warehouse" -> warehouse = o.first
                "cheque" -> cheque = o.first
            }
            step++
            saveQuiz()
        }
        card.addView(btn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })
    }
    root.addView(card)

    if (step > 0) {
        val back = UI.text(c, "سوال قبلی", 12.5f, T.sub, true)
        back.setPadding(UI.dp(c, 12), UI.dp(c, 10), UI.dp(c, 12), UI.dp(c, 10))
        back.setOnClickListener { step--; saveQuiz() }
        root.addView(back, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    return root
}

