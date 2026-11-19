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
 * ماژول کالا و خدمات — تعریف کالا، گروه، واحد، قیمت، بارکد و موجودی
 */

fun itemsScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    // آمار
    val totalStockValue = db.items.sumOf { Db.stockOf(db, it.id) * it.cost }
    val a = LinearLayout(c)
    a.orientation = LinearLayout.VERTICAL
    a.background = UI.stroke(c, T.card, 18, T.border, 1)
    a.setPadding(UI.dp(c, 14), UI.dp(c, 13), UI.dp(c, 14), UI.dp(c, 13))
    a.addView(UI.text(c, "تعداد کالا و خدمات", 11, T.sub, true))
    val av = UI.text(c, J.faN(db.items.size), 17, T.text, true)
    av.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) }
    a.addView(av)
    val chipRow = LinearLayout(c)
    chipRow.orientation = LinearLayout.HORIZONTAL
    chipRow.addView(UI.chip(c, J.faN(db.items.count { !it.service }) + " کالا", "sky"))
    chipRow.addView(UI.chip(c, J.faN(db.items.count { it.service }) + " خدمت", "violet"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = UI.dp(c, 6) })
    chipRow.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) }
    a.addView(chipRow)
    val b0 = LinearLayout(c)
    b0.orientation = LinearLayout.VERTICAL
    b0.background = UI.stroke(c, T.card, 18, T.border, 1)
    b0.setPadding(UI.dp(c, 14), UI.dp(c, 13), UI.dp(c, 14), UI.dp(c, 13))
    b0.addView(UI.text(c, "ارزش کل موجودی", 11, T.sub, true))
    val bv = UI.text(c, J.moneyShort(totalStockValue), 17, T.text, true)
    bv.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 6) }
    b0.addView(bv)
    b0.addView(UI.text(c, "به بهای تمام‌شده — تومان", 10, T.sub), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
    root.addView(UI.grid2(c, a, b0))

    // جستجو + گروه‌ها
    val searchRow = LinearLayout(c)
    searchRow.orientation = LinearLayout.HORIZONTAL
    val (sbox, se) = UI.search(c, "جستجو با نام، کد یا بارکد…")
    (sbox.layoutParams as? LinearLayout.LayoutParams)?.topMargin = 0
    searchRow.addView(sbox, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    val groupsBtn = UI.iconBox(c, R.drawable.ic_layers, T.sub, T.cardSoft, 44, 20)
    groupsBtn.setOnClickListener { groupsSheet(act) }
    val gl = LinearLayout.LayoutParams(UI.dp(c, 44), UI.dp(c, 44))
    gl.marginStart = UI.dp(c, 8)
    groupsBtn.layoutParams = gl
    searchRow.addView(groupsBtn)
    val srl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    srl.topMargin = UI.dp(c, 10)
    searchRow.layoutParams = srl
    root.addView(searchRow)

    // فیلتر گروه — اگر گروه ذخیره‌شده دیگر وجود ندارد، به «همه» برگرد
    var q = act.itemsQuery
    se.setText(q)
    val groupOptions = listOf(Triple("همه", db.items.size, "all")) + db.groups.map { g -> Triple(g.name, db.items.count { it.groupId == g.id }, g.id) }
    var groupFilter = act.itemsGroup
    if (groupOptions.indexOfFirst { it.third == groupFilter } < 0) { groupFilter = "all"; act.itemsGroup = "all" }
    val selIdx = groupOptions.indexOfFirst { it.third == groupFilter }.coerceAtLeast(0)
    root.addView(UI.segScroll(c, groupOptions, selIdx) { i ->
        act.itemsGroup = groupOptions[i].third
        act.refresh()
    })

    // لیست کالاها — زنده با تایپ
    val list = LinearLayout(c)
    root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 4) })
    fun renderList() {
        val qq = act.itemsQuery
        val gf = act.itemsGroup
        list.removeAllViews()
        val filtered = db.items.filter {
            if (gf != "all" && it.groupId != gf) return@filter false
            if (qq.isEmpty()) true else it.name.contains(qq) || it.code.contains(qq) || it.barcode.contains(qq)
        }
        if (filtered.isEmpty()) {
            val em = if (qq.isEmpty()) UI.empty(c, "کالایی پیدا نشد", "با دکمهٔ سبز یکی اضافه کنید", R.drawable.ic_boxes)
                     else UI.empty(c, "چیزی پیدا نشد", "عبارت دیگری را امتحان کنید", R.drawable.ic_search)
            list.addView(em, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 16) })
        } else {
            for (it0 in filtered) {
                val stock = Db.stockOf(db, it0.id)
                val low = !it0.service && it0.minStock > 0 && stock < it0.minStock
                val gname = db.groups.firstOrNull { g -> g.id == it0.groupId }?.name ?: "—"
                // قیمت همیشه سبز، وضعیت موجودی هنگام کمبود قرمز — مثل نسخهٔ وب
                val end = LinearLayout(c)
                end.orientation = LinearLayout.VERTICAL
                end.gravity = Gravity.START
                end.addView(UI.text(c, J.moneyFa(it0.price), 13, T.primary, true))
                val stT = UI.text(c, if (it0.service) "خدمت" else "موجودی: " + J.qty(stock) + " " + it0.unit + (if (low) " • کم!" else ""), 9, if (low) T.rose else T.sub)
                val slp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                slp.topMargin = UI.dp(c, 1)
                end.addView(stT, slp)
                val elp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                elp.marginEnd = UI.dp(c, 4)
                end.layoutParams = elp
                val row = UI.listRow(c,
                    it0.name,
                    "کد " + J.fa(it0.code) + " • " + gname,
                    end,
                    null,
                    icRes = if (it0.service) R.drawable.ic_wrench else R.drawable.ic_pkg,
                    icTint = if (it0.service) T.violet else T.sky,
                    click = { itemEditorSheet(act, it0) })
                list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
            }
        }
    }
    se.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { act.itemsQuery = s?.toString() ?: ""; renderList() }
    })
    renderList()

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

fun itemEditorSheet(act: MainActivity, it0: Item, initialQty: Long = 0L) {
    val bs = BottomSheet(act)
    var item = it0
    val isNew = !Db.db.items.any { i -> i.id == item.id }
    var initialStock = initialQty.coerceAtLeast(0L)
    bs.show(if (isNew) "کالای جدید" else "ویرایش کالا", "اطلاعات کامل کالا، قیمت‌ها و کنترل موجودی را ثبت کنید") { b ->
        val name = UI.field(act, "نام کالا / خدمت", item.name, hint = "مثلاً یخچال سامسونگ")
        hookText(name.first) { v -> item = item.copy(name = v) }
        b.addView(name.first)

        val code = UI.field(act, "کد کالا", item.code, center = true)
        val barcode = UI.field(act, "بارکد (اختیاری)", item.barcode, ltr = true, center = true)
        hookText(code.first) { v -> item = item.copy(code = v) }
        hookText(barcode.first) { v -> item = item.copy(barcode = v) }
        val r1 = LinearLayout(act)
        r1.orientation = LinearLayout.HORIZONTAL
        r1.addView(code.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r1.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r1.addView(barcode.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r1)

        // گروه + واحد
        val gLabel = ValueLabel()
        val gSel = UI.selectField(act, "گروه", Db.db.groups.firstOrNull { g -> g.id == item.groupId }?.name ?: "انتخاب کنید") {
            optionSheet(act, "گروه کالا", Db.db.groups.map { Opt(it.id, it.name) }, item.groupId) { o ->
                item = item.copy(groupId = o.id)
                gLabel.set(o.label)
            }
        }
        gLabel.bind(gSel, Db.db.groups.firstOrNull { g -> g.id == item.groupId }?.name ?: "انتخاب کنید")
        val unit = UI.field(act, "واحد شمارش", item.unit, hint = "عدد / دستگاه / ساعت")
        hookText(unit.first) { v -> item = item.copy(unit = v) }
        val r2 = LinearLayout(act)
        r2.orientation = LinearLayout.HORIZONTAL
        r2.addView(gSel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r2.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r2.addView(unit.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r2)

        val price = UI.numField(act, "قیمت فروش (تومان)", item.price)
        val cost = UI.numField(act, "بهای خرید (تومان)", item.cost)
        hookNum(price) { v -> item = item.copy(price = v) }
        hookNum(cost) { v -> item = item.copy(cost = v) }
        val r3 = LinearLayout(act)
        r3.orientation = LinearLayout.HORIZONTAL
        r3.addView(price, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r3.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r3.addView(cost, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r3)

        val minStock = UI.numField(act, "حداقل موجودی هشدار (کمتر از این اطلاع بده)", Math.round(item.minStock))
        hookNum(minStock) { v -> item = item.copy(minStock = v.toDouble()) }
        val initialQtyField = if (isNew) UI.numField(act, "موجودی اولیه (شروع ثبت)", initialStock, hint = "اگر موجودی اولیه دارید وارد کنید") else null
        if (initialQtyField != null) hookNum(initialQtyField) { v -> initialStock = v }
        val sw = UI.switchRow(act, "خدمت (بدون انبار)", "خدمات موجودی و انبار ندارند", item.service) { v -> item = item.copy(service = v); if (v) initialStock = 0L }
        val r4 = LinearLayout(act)
        r4.orientation = LinearLayout.VERTICAL
        r4.addView(minStock, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        if (initialQtyField != null) r4.addView(initialQtyField, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        r4.addView(sw, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.topMargin = UI.dp(act, 10) })
        b.addView(r4)

        val desc = UI.fieldMulti(act, "توضیحات", item.desc)
        hookText(desc.first) { v -> item = item.copy(desc = v) }
        b.addView(desc.first)

        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ذخیره") {
                val current = Db.db
                if (item.name.trim().isEmpty()) { UI.toast(act, "نام کالا را بنویسید"); return@primaryBtn }
                if (item.code.trim().isEmpty()) { UI.toast(act, "کد کالا الزامی است"); return@primaryBtn }
                if (item.unit.trim().isEmpty()) { UI.toast(act, "واحد شمارش الزامی است"); return@primaryBtn }
                if (item.price < 0 || item.cost < 0 || item.minStock < 0 || initialStock < 0) { UI.toast(act, "قیمت، بهای خرید، موجودی هشدار و موجودی اولیه نمی‌توانند منفی باشند"); return@primaryBtn }
                if (isNew && item.price <= 0) { UI.toast(act, "برای رکورد جدید قیمت فروش باید بیشتر از صفر باشد"); return@primaryBtn }
                if (isNew && !item.service && item.cost <= 0) { UI.toast(act, "برای کالای جدید بهای خرید را وارد کنید تا سود و بهای تمام‌شده درست محاسبه شود"); return@primaryBtn }
                if (initialStock > 0 && item.cost <= 0) { UI.toast(act, "موجودی اولیه بدون بهای خرید ثبت نمی‌شود؛ ابتدا بهای خرید را وارد کنید"); return@primaryBtn }
                val duplicateCode = current.items.any { it.id != item.id && it.code.trim().equals(item.code.trim(), true) }
                if (duplicateCode) { UI.toast(act, "کد کالا تکراری است"); return@primaryBtn }
                if (current.items.any { it.id != item.id && it.name.trim().equals(item.name.trim(), true) }) { UI.toast(act, "کالایی با همین نام قبلاً ثبت شده؛ برای مدل/برند متفاوت نام دقیق‌تری وارد کنید"); return@primaryBtn }
                val duplicateBarcode = item.barcode.trim().isNotEmpty() && current.items.any { it.id != item.id && it.barcode.trim() == item.barcode.trim() }
                if (duplicateBarcode) { UI.toast(act, "بارکد برای کالای دیگری ثبت شده است"); return@primaryBtn }
                if (item.service && initialStock > 0) { UI.toast(act, "برای خدمت نمی‌توان موجودی اولیه ثبت کرد"); return@primaryBtn }
                if (initialStock > 0 && current.warehouses.isEmpty()) { UI.toast(act, "برای ثبت موجودی اولیه ابتدا یک انبار تعریف کنید"); return@primaryBtn }
                val exists = current.items.any { i -> i.id == item.id }
                val saved = Db.update { d ->
                    val updatedItems = if (exists) d.items.map { if (it.id == item.id) item else it } else d.items + item
                    val docs = if (!exists && initialStock > 0 && d.warehouses.isNotEmpty()) {
                        val docNo = (d.stockDocs.maxOfOrNull { it.no } ?: 300L) + 1L
                        d.stockDocs + StockDoc(Db.newId("sd"), docNo, "adjust", J.jTodayStr(), d.warehouses.first().id,
                            "", listOf(StockLine(item.id, initialStock.toDouble())), "موجودی اولیه هنگام ثبت کالا")
                    } else d.stockDocs
                    d.copy(items = updatedItems, stockDocs = docs)
                }
                if (!saved) return@primaryBtn
                bs.close(); act.refresh()
                UI.toast(act, (if (exists) "کالا ویرایش شد" else "کالای جدید ثبت شد") + " — " + item.name +
                    (if (!exists && initialStock > 0) " • موجودی اولیه " + J.faN(initialStock) else ""))
            }))
        if (!isNew) {
            val del = UI.outlineBtn(act, "حذف کالا", danger = true, icRes = R.drawable.ic_trash) {
                if (Db.itemHasReferences(Db.db, item.id)) {
                    UI.toast(act, "این کالا در فاکتور، قالب یا سند انبار استفاده شده و حذف آن مسدود است. برای حفظ تاریخچه، کالا را غیرفعال/خدمت کنید.")
                    return@outlineBtn
                }
                bs.close()
                confirmSheet(act, "حذف کالا؟", "«" + item.name + "» حذف می‌شود.") {
                    val saved = Db.update { d -> d.copy(items = d.items.filter { i -> i.id != item.id }) }
                    if (saved) { act.refresh(); UI.toast(act, "کالا حذف شد — " + item.name) }
                }
            }
            b.addView(del, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        }
    }
}

fun groupsSheet(act: MainActivity) {
    val bs = BottomSheet(act)
    val db = Db.db
    bs.show("گروه‌های کالا", "برای منظم شدن انبار، کالاها را گروه‌بندی کنید") { b ->
        for (g in db.groups) {
            val row = UI.listRow(act, g.name, J.faN(db.items.count { it.groupId == g.id }) + " کالا", icRes = R.drawable.ic_layers, chev = false)
            b.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
        }
        val (box, e) = UI.field(act, "گروه جدید", "", hint = "نام گروه جدید")
        b.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
        val add = UI.primaryBtn(act, "افزودن گروه") {
            if (e.text.toString().trim().isEmpty()) { UI.toast(act, "نام گروه را بنویسید"); return@primaryBtn }
            val g = ItemGroup(Db.newId("grp"), e.text.toString().trim())
            Db.update { d -> d.copy(groups = d.groups + g) }
            bs.close()
            act.refresh()
            UI.toast(act, "گروه جدید ساخته شد — " + g.name)
        }
        b.addView(add, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
    }
}

/* ═════════════════════════ انبار ═════════════════════════ */

fun warehouseScreen(act: MainActivity): View {
    val c = act
    val db = Db.db
    val root = LinearLayout(c)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10))

    // آمار
    val rows = db.items.filter { !it.service }.map {
        Triple(it, Db.stockOf(db, it.id), db.warehouses.map { w -> Pair(w, Db.stockOf(db, it.id, w.id)) })
    }
    val lowCount = rows.count { it.first.minStock > 0 && it.second < it.first.minStock }
    val totalValue = rows.sumOf { it.second * it.first.cost }

    val a = UI.statCard(c, "ارزش موجودی کل", J.moneyShort(totalValue), "تومان — به بهای خرید", "green", R.drawable.ic_wallet)
    val bl = UI.statCard(c, "هشدار کم‌موجودی", J.faN(lowCount), "کالا زیر حد هشدار", if (lowCount > 0) "amber" else "green", R.drawable.ic_alert)
    root.addView(UI.grid2(c, a, bl))

    // دکمه‌های انتقال و اصلاح
    val btnRow = LinearLayout(c)
    btnRow.orientation = LinearLayout.HORIZONTAL
    val transfer = UI.text(c, "انتقال بین انبار", 12.5f, T.text, true)
    transfer.gravity = Gravity.CENTER
    transfer.background = UI.stroke(c, T.card, 18, T.border, 1)
    transfer.setPadding(UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14))
    transfer.setOnClickListener { stockDocSheet(act, "transfer", null) }
    btnRow.addView(transfer, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    val adjust = UI.text(c, "اصلاح / شمارش", 12.5f, T.text, true)
    adjust.gravity = Gravity.CENTER
    adjust.background = UI.stroke(c, T.card, 18, T.border, 1)
    adjust.setPadding(UI.dp(c, 10), UI.dp(c, 14), UI.dp(c, 10), UI.dp(c, 14))
    adjust.setOnClickListener { stockDocSheet(act, "adjust", null) }
    btnRow.addView(adjust, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginStart = UI.dp(c, 10) })
    root.addView(btnRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10) })

    // فیلتر انبار — اگر انبار ذخیره‌شده دیگر وجود ندارد، به «همه» برگرد
    val whOptions = listOf(Triple("کل انبارها", -1, "all")) + db.warehouses.map { w -> Triple(w.name, -1, w.id) }
    var whFilter = act.whFilter
    if (whOptions.indexOfFirst { it.third == whFilter } < 0) { whFilter = "all"; act.whFilter = "all" }
    val selIdx = whOptions.indexOfFirst { it.third == whFilter }.coerceAtLeast(0)
    root.addView(UI.segScroll(c, whOptions, selIdx) { i ->
        act.whFilter = whOptions[i].third
        act.refresh()
    })

    // موجودی کالاها
    val stockCard = UI.card(c)
    stockCard.addView(UI.sectionTitle(c, "موجودی کالاها", J.faN(rows.size) + " قلم کالا در انبار", R.drawable.ic_boxes, topMargin = 0))
    val visible = rows.filter { r ->
        if (whFilter == "all") r.second != 0.0 else r.third.firstOrNull { x -> x.first.id == whFilter }?.second != 0.0
    }
    if (visible.isEmpty()) {
        stockCard.addView(UI.empty(c, "موجودی ثبت‌شده ندارید", "با فاکتور خرید یا اصلاح موجودی، کالا وارد انبار کنید", R.drawable.ic_pkg))
    } else {
        for (r in visible) {
            val low = r.first.minStock > 0 && r.second < r.first.minStock
            val shown = if (whFilter == "all") r.second else (r.third.firstOrNull { x -> x.first.id == whFilter }?.second ?: 0.0)
            val cell = UI.innerCard(act, T.cardSoft, 16, 12)
            val head = LinearLayout(act)
            head.orientation = LinearLayout.HORIZONTAL
            val col = LinearLayout(act)
            col.orientation = LinearLayout.VERTICAL
            col.addView(UI.text(act, r.first.name, 12.5f, T.text, true))
            col.addView(UI.text(act, "کد " + J.fa(r.first.code) + " • حد هشدار " + J.qty(r.first.minStock), 10, T.sub))
            head.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val end = LinearLayout(act)
            end.orientation = LinearLayout.VERTICAL
            end.gravity = Gravity.START
            end.addView(UI.text(act, J.qty(shown), 14, if (low) T.rose else T.primary, true))
            end.addView(UI.text(act, r.first.unit, 9, T.sub))
            head.addView(end)
            cell.addView(head)
            if (whFilter == "all") {
                val chips = LinearLayout(act)
                chips.orientation = LinearLayout.HORIZONTAL
                for (x in r.third) {
                    chips.addView(UI.chip(act, x.first.name + ": " + J.qty(x.second), if (x.second == 0.0) "slate" else "sky"), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = UI.dp(act, 6); it.topMargin = UI.dp(act, 8) })
                }
                cell.addView(chips, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            stockCard.addView(cell, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        }
    }
    root.addView(stockCard)

    // اسناد انبار
    val docsCard = UI.card(c)
    docsCard.addView(UI.sectionTitle(c, "اسناد انبار", "انتقال‌ها و اصلاح‌های اخیر", R.drawable.ic_wh, topMargin = 0))
    if (db.stockDocs.isEmpty()) {
        docsCard.addView(UI.empty(c, "سند انباری ثبت نشده", "", R.drawable.ic_wh))
    } else {
        for (d in db.stockDocs) {
            val row = UI.listRow(c,
                if (d.kind == "transfer") "انتقال انبار" else "اصلاح موجودی",
                "شمارهٔ " + J.faN(d.no) + " • " + J.jFa(d.date) + (if (d.desc.isNotEmpty()) " • " + d.desc else ""),
                UI.endCol(c, J.faN(d.lines.size) + " قلم"),
                null,
                icRes = if (d.kind == "transfer") R.drawable.ic_swap else R.drawable.ic_clip,
                icTint = if (d.kind == "transfer") T.violet else T.sky,
                chev = false)
            docsCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
        }
    }
    root.addView(docsCard)

    // انبارهای من
    val whCard = UI.card(c)
    whCard.addView(UI.sectionTitle(c, "انبارهای من", J.faN(db.warehouses.size) + " انبار", R.drawable.ic_pin, topMargin = 0))
    for (w in db.warehouses) {
        val valSum = db.items.sumOf { Db.stockOf(db, it.id, w.id) * it.cost }
        val row = UI.listRow(c, w.name, w.loc.ifEmpty { "—" }, UI.endCol(c, J.moneyShort(valSum) + " ت"), null, icRes = R.drawable.ic_wh, chev = false)
        whCard.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 8) })
    }
    root.addView(whCard)

    return ScrollView(c).apply {
        isVerticalScrollBarEnabled = false
        isFillViewport = true
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

fun stockDocSheet(act: MainActivity, kind: String, doc0: StockDoc?) {
    val bs = BottomSheet(act)
    val db = Db.db
    var doc = doc0 ?: StockDoc(
        Db.newId("st"), 600L + db.stockDocs.size + 1, kind, J.jTodayStr(),
        db.warehouses.firstOrNull()?.id ?: "", db.warehouses.getOrNull(1)?.id ?: "", emptyList(), ""
    )
    val wh1 = ValueLabel()
    val wh2 = ValueLabel()
    val linesWrap = LinearLayout(act)
    val dLabel = ValueLabel()

    fun renderLines() {
        linesWrap.removeAllViews()
        if (doc.lines.isEmpty()) {
            val t = UI.text(act, "قلمی اضافه نشده", 11, T.sub)
            t.gravity = Gravity.CENTER
            t.setPadding(0, UI.dp(act, 10), 0, UI.dp(act, 10))
            linesWrap.addView(t)
            return
        }
        for ((idx, l) in doc.lines.withIndex()) {
            val it0 = db.items.firstOrNull { i -> i.id == l.itemId }
            val row = UI.innerCard(act, T.card, 14, 10)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val col = LinearLayout(act)
            col.orientation = LinearLayout.VERTICAL
            col.addView(UI.text(act, it0?.name ?: "—", 12, T.text, true))
            if (doc.kind == "adjust") col.addView(UI.text(act, "موجودی فعلی: " + J.qty(Db.stockOf(db, l.itemId, doc.warehouseId)), 10, T.sub))
            row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val qty = UI.qtyStepper(act, l.qty) { v ->
                val lines = doc.lines.toMutableList()
                if (idx >= lines.size) return@qtyStepper
                lines[idx] = lines[idx].copy(qty = Math.max(0.0, v))
                doc = doc.copy(lines = lines)
            }
            row.addView(qty)
            val x = UI.icon(act, R.drawable.ic_close, 14, T.rose)
            x.setPadding(UI.dp(act, 8), 0, UI.dp(act, 2), 0)
            x.setOnClickListener {
                doc = doc.copy(lines = doc.lines.filterIndexed { i, _ -> i != idx })
                renderLines()
            }
            row.addView(x)
            linesWrap.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 6) })
        }
    }

    bs.show(if (doc.kind == "transfer") "انتقال بین انبارها" else "اصلاح و شمارش موجودی",
        if (doc.kind == "transfer") "کالاها را از یک انبار به انبار دیگر بفرستید" else "مغایرت شمارش را با مثبت یا منفی ثبت کنید") { b ->

        val dField = UI.dateField(act, "تاریخ سند", doc.date) {
            dateSheet(act, doc.date) { v -> doc = doc.copy(date = v); dLabel.set(J.jFa(v)) }
        }
        dLabel.bind(dField, J.jFa(doc.date))
        val wSel = UI.selectField(act, if (doc.kind == "transfer") "انبار مبدأ" else "انبار", wh1.current) {
            optionSheet(act, "انبار مبدأ", db.warehouses.map { Opt(it.id, it.name) }, doc.warehouseId) { o ->
                doc = doc.copy(warehouseId = o.id)
                wh1.set(o.label)
                // «موجودی فعلی» اقلام با تغییر انبار به‌روز شود — مثل نسخهٔ وب
                renderLines()
            }
        }
        wh1.bind(wSel, db.warehouses.firstOrNull { it.id == doc.warehouseId }?.name ?: "انتخاب کنید")
        val r1 = LinearLayout(act)
        r1.orientation = LinearLayout.HORIZONTAL
        r1.addView(dField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        r1.addView(View(act), LinearLayout.LayoutParams(UI.dp(act, 12), ViewGroup.LayoutParams.WRAP_CONTENT))
        r1.addView(wSel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        b.addView(r1)

        if (doc.kind == "transfer") {
            val wSel2 = UI.selectField(act, "انبار مقصد", wh2.current) {
                optionSheet(act, "انبار مقصد", db.warehouses.filter { it.id != doc.warehouseId }.map { Opt(it.id, it.name) }, doc.toWarehouseId) { o ->
                    doc = doc.copy(toWarehouseId = o.id)
                    wh2.set(o.label)
                }
            }
            wh2.bind(wSel2, db.warehouses.firstOrNull { it.id == doc.toWarehouseId }?.name ?: "انتخاب کنید")
            b.addView(wSel2)
        }

        val addBtn = UI.text(act, "+ افزودن قلم", 11, Color.WHITE, true)
        addBtn.background = UI.rounded(act, T.primary, 12)
        addBtn.setPadding(UI.dp(act, 12), UI.dp(act, 6), UI.dp(act, 12), UI.dp(act, 7))
        val linesHead = LinearLayout(act)
        linesHead.orientation = LinearLayout.HORIZONTAL
        linesHead.addView(UI.text(act, "اقلام سند", 12, T.text, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        linesHead.addView(addBtn)
        val linesCard = UI.innerCard(act, T.cardSoft, 16, 12)
        linesCard.addView(linesHead)
        linesCard.addView(linesWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        b.addView(linesCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
        addBtn.setOnClickListener {
            optionSheet(act, "کالا را انتخاب کنید",
                db.items.filter { !it.service }.map { it0 -> Opt(it0.id, it0.name, "موجودی " + J.qty(Db.stockOf(db, it0.id, doc.warehouseId))) }, "") { o ->
                if (doc.lines.any { it.itemId == o.id }) return@optionSheet
                doc = doc.copy(lines = doc.lines + StockLine(o.id, 1.0))
                renderLines()
            }
        }
        renderLines()

        val desc = UI.field(act, "توضیحات", doc.desc)
        hookText(desc.first) { v -> doc = doc.copy(desc = v) }
        b.addView(desc.first)

        b.addView(UI.btnRow(act,
            UI.outlineBtn(act, "انصراف") { bs.close() },
            UI.primaryBtn(act, "ثبت سند") {
                if (doc.lines.isEmpty()) { UI.toast(act, "حداقل یک قلم اضافه کنید"); return@primaryBtn }
                if (doc.lines.any { it.qty <= 0.0 || it.qty.isNaN() || it.qty.isInfinite() }) { UI.toast(act, "مقدار اقلام باید مثبت و معتبر باشد"); return@primaryBtn }
                if (doc.lines.map { it.itemId }.distinct().size != doc.lines.size) { UI.toast(act, "یک کالا دوبار در سند آمده است؛ ردیف‌های تکراری را حذف کنید"); return@primaryBtn }
                if (doc.lines.any { line -> db.items.none { it.id == line.itemId } }) { UI.toast(act, "یکی از کالاهای سند دیگر موجود نیست؛ سند را اصلاح کنید"); return@primaryBtn }
                if (db.warehouses.none { it.id == doc.warehouseId }) { UI.toast(act, "انبار معتبر انتخاب کنید"); return@primaryBtn }
                if (doc.kind == "adjust" && doc.lines.any { line ->
                    val item = db.items.firstOrNull { it.id == line.itemId }
                    item == null || item.service
                }) { UI.toast(act, "اصلاح موجودی فقط برای کالاهای موجود معتبر است"); return@primaryBtn }
                if (doc.kind == "transfer" && doc.lines.any { line -> line.qty > Db.stockOf(db, line.itemId, doc.warehouseId) }) {
                    UI.toast(act, "موجودی انبار مبدأ برای انتقال کافی نیست"); return@primaryBtn
                }
                if (doc.kind == "transfer" && (doc.toWarehouseId.isEmpty() || doc.toWarehouseId == doc.warehouseId)) {
                    UI.toast(act, "انبار مقصد را انتخاب کنید — مقصد باید با مبدأ متفاوت باشد")
                    return@primaryBtn
                }
                Db.update { d -> d.copy(stockDocs = listOf(doc) + d.stockDocs) }
                bs.close(); act.refresh()
                UI.toast(act, (if (doc.kind == "transfer") "انتقال انبار ثبت شد" else "اصلاح موجودی ثبت شد") + " — سند شمارهٔ " + J.faN(doc.no))
            }))
    }
}
