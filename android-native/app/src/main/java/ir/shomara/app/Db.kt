package ir.shomara.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * موتور داده و حسابداری شمارا — مو‌به‌مو مطابق store.ts
 * همهٔ داده‌ها در یک فایل JSON روی حافظهٔ دستگاه ذخیره می‌شود و
 * اسناد حسابداری به‌صورت خودکار از فاکتورها، دریافت/پرداخت‌ها و چک‌ها تولید می‌شوند.
 */
object Db {

    var db: DB = seed()
    private var seq = 0
    private var lastBak = 0L

    fun init(c: Context) {
        val f = File(c.filesDir, "shomara-db.json")
        val bak = File(c.filesDir, "shomara-db.bak.json")
        var loaded: DB? = null
        var loadedFromPrimary = false
        try {
            if (f.exists()) {
                loaded = fromJson(JSONObject(f.readText()))
                loadedFromPrimary = loaded != null && loaded.settings.shopName.isNotEmpty()
            }
        } catch (ig: Exception) {}
        if (loaded == null || loaded.settings.shopName.isEmpty()) {
            try { if (bak.exists()) loaded = fromJson(JSONObject(bak.readText())) } catch (ig: Exception) {}
        }
        if (loaded != null && loaded.settings.shopName.isNotEmpty()) {
            db = normalize(loaded)
            lastBak = System.currentTimeMillis()
            if (loadedFromPrimary) {
                try { f.copyTo(bak, true) } catch (ig: Exception) {}
            } else {
                // Recover main storage from the verified backup without touching the backup itself.
                try {
                    val recoveryTmp = File(c.filesDir, "shomara-db.recovery.tmp")
                    recoveryTmp.writeText(toJson(db).toString())
                    if (f.exists()) f.delete()
                    if (!recoveryTmp.renameTo(f)) {
                        f.writeText(recoveryTmp.readText())
                        recoveryTmp.delete()
                    }
                } catch (ig: Exception) {}
            }
        } else {
            // فایل خراب را برای بررسی نگه می‌داریم و نسخهٔ پشتیوان را جایگزین می‌کنیم
            try {
                if (f.exists() && f.length() > 2L) f.copyTo(File(c.filesDir, "shomara-db-corrupt.json"), true)
            } catch (ig: Exception) {}
            db = seed()
        }
    }

    /** هر تاریخ خراب/کوتاه را به امروز برمی‌گرداند — ضربه‌گیر کرش برای کل برنامه */
    fun okDate(s: String): String {
        val p = J.jParse(s)
        return if (s.trim().length >= 7 && p[0] >= 1300 && p[0] <= 1500) J.jFmt(p) else J.jTodayStr()
    }

    fun normalize(d: DB): DB = d.copy(
        invoices = d.invoices.map { it.copy(date = okDate(it.date), lines = it.lines.map { l -> l.copy(qty = okD(l.qty), discount = okD(l.discount)) }) },
        payments = d.payments.map { it.copy(date = okDate(it.date)) },
        checks = d.checks.map { it.copy(date = okDate(it.date), dueDate = okDate(it.dueDate)) },
        stockDocs = d.stockDocs.map { it.copy(date = okDate(it.date), lines = it.lines.map { l -> l.copy(qty = okD(l.qty)) }) },
        vouchers = d.vouchers.map { it.copy(date = okDate(it.date)) },
        reminders = d.reminders.map { it.copy(date = okDate(it.date)) },
        recurring = d.recurring.map { it.copy(nextDate = okDate(it.nextDate), lines = it.lines.map { l -> l.copy(qty = okD(l.qty), discount = okD(l.discount)) }) },
        timeEntries = d.timeEntries.map { it.copy(date = okDate(it.date)) },
        bankTxns = d.bankTxns.map { it.copy(date = okDate(it.date)) }
    )

    private fun okD(v: Double): Double = if (v.isNaN() || v.isInfinite()) 0.0 else v

    fun save(): Boolean {
        return try {
            val c = App.ctx
            val f = File(c.filesDir, "shomara-db.json")
            val tmp = File(c.filesDir, "shomara-db.tmp")
            tmp.writeText(toJson(db).toString())
            // نسخهٔ پشتیبان چرخشی — قبل از جایگزینی فایل اصلی
            if (f.exists() && System.currentTimeMillis() - lastBak > 300_000L) {
                try { f.copyTo(File(c.filesDir, "shomara-db.bak.json"), true); lastBak = System.currentTimeMillis() } catch (ig: Exception) {}
            }
            if (!tmp.renameTo(f)) {
                f.writeText(tmp.readText()); tmp.delete()
            }
            f.exists() && f.length() > 0L
        } catch (e: Exception) {
            try { android.util.Log.e("Shomara", "save failed", e) } catch (ig: Exception) {}
            false
        }
    }


    /** جلوگیری از حذف رکوردهای master که اسناد تاریخی به شناسه‌شان وابسته است. */
    fun itemHasReferences(d: DB, itemId: String): Boolean =
        d.invoices.any { inv -> inv.lines.any { it.itemId == itemId } } ||
        d.recurring.any { r -> r.lines.any { it.itemId == itemId } } ||
        d.stockDocs.any { doc -> doc.lines.any { it.itemId == itemId } }

    fun partnerHasReferences(d: DB, partnerId: String): Boolean =
        d.invoices.any { it.partnerId == partnerId } || d.payments.any { it.partnerId == partnerId } ||
        d.checks.any { it.partnerId == partnerId } || d.recurring.any { it.partnerId == partnerId } ||
        d.projects.any { it.partnerId == partnerId }

    /** تغییر DB فقط وقتی پذیرفته می‌شود که نوشتن فایل نیز موفق باشد؛ در شکست، حافظه rollback می‌شود. */
    fun update(fn: (DB) -> DB): Boolean {
        val previous = db
        val candidate = fn(previous)
        db = candidate
        if (save()) return true
        db = previous
        try { UI.toast(App.ctx, "ذخیره‌سازی ناموفق بود؛ تغییر اعمال نشد. فضای دستگاه و مجوز را بررسی کنید.") } catch (ig: Exception) {}
        return false
    }

    fun reset() {
        db = seed()
        save()
    }

    fun newId(p: String): String {
        seq++
        return p + java.lang.Long.toString(System.currentTimeMillis(), 36) + java.lang.Long.toString(seq.toLong(), 36)
    }

    fun nextNo(key: String): Long = (db.counters[key] ?: 0L) + 1L

    fun nextPartnerCode(d: DB = db): String {
        var n = 501L + d.partners.size
        while (d.partners.any { it.code.trim() == n.toString() }) n++
        return n.toString()
    }

    // ══════════════════════════ محاسبات فاکتور ══════════════════════════

    fun invoiceTotals(inv: Invoice): InvoiceTot {
        // دقیقاً مثل نسخهٔ وب: جمع‌ها اعشاری، گرد کردن فقط در نمایش/نهایی
        var grossD = 0.0; var lineDisc = 0.0
        for (l in inv.lines) { grossD += l.qty * l.price; lineDisc += l.qty * l.discount }
        val netF = Math.max(0.0, grossD - lineDisc - inv.discount)
        val gross = Math.round(grossD)
        val net = Math.round(netF)
        val vat = Math.round((netF * inv.vatRate) / 100.0)
        val total = net + vat
        return InvoiceTot(gross, lineDisc, net, vat, total, total - inv.paid)
    }

    // ══════════════════════════ اسناد حسابداری خودکار ══════════════════════════

    fun bankAccOf(bankId: String): String {
        val b = db.banks.firstOrNull { it.id == bankId }
        return if (b != null && b.kind != "cash") "1102" else "1101"
    }
    fun bankNameOf(bankId: String): String = db.banks.firstOrNull { it.id == bankId }?.name ?: "صندوق"

    /** Cost of items on invoice lines, using the item's current master cost. Services are excluded.
     * This is a conservative interim valuation; v16 has no historical cost layers/FIFO ledger.
     */
    private fun invoiceCost(lines: List<InvoiceLine>, d: DB): Long =
        lines.sumOf { line ->
            val item = d.items.firstOrNull { it.id == line.itemId }
            if (item == null || item.service || item.cost <= 0L || line.qty <= 0.0) 0L else Math.round(line.qty * item.cost.toDouble())
        }

    fun autoVouchers(d: DB = db): List<Voucher> {
        val out = ArrayList<Voucher>()
        for (inv in d.invoices) {
            if (!inv.final || inv.kind == K_PROFORMA) continue
            val t = invoiceTotals(inv)
            val partner = d.partners.firstOrNull { it.id == inv.partnerId }
            val lines = ArrayList<VoucherLine>()
            val payAcc = if (inv.payBankId.isNotEmpty()) bankAccOf(inv.payBankId) else "1101"
            when (inv.kind) {
                K_SALE -> {
                    lines.add(VoucherLine("1103", inv.partnerId, t.total - inv.paid, 0, partner?.name ?: ""))
                    if (inv.paid > 0) lines.add(VoucherLine(payAcc, inv.payBankId, inv.paid, 0, "دریافت نقدی"))
                    lines.add(VoucherLine("4101", "", 0, t.net, "فروش"))
                    if (t.vat > 0) lines.add(VoucherLine("2103", "", 0, t.vat, "مالیات بر ارزش افزوده"))
                    val cogs = invoiceCost(inv.lines, d)
                    if (cogs > 0) {
                        lines.add(VoucherLine("5101", "", cogs, 0, "بهای تمام‌شده کالای فروش‌رفته"))
                        lines.add(VoucherLine("1201", "", 0, cogs, "کاهش موجودی بابت فروش"))
                    }
                }
                K_PURCHASE -> {
                    lines.add(VoucherLine("1201", "", t.net, 0, "خرید کالا"))
                    lines.add(VoucherLine("1105", "", t.vat, 0, "مالیات خرید"))
                    lines.add(VoucherLine("2101", inv.partnerId, 0, t.total, partner?.name ?: ""))
                    if (inv.paid > 0) {
                        lines.add(VoucherLine("2101", inv.partnerId, inv.paid, 0, "پرداخت نقدی"))
                        lines.add(VoucherLine(payAcc, inv.payBankId, 0, inv.paid, "پرداخت نقدی"))
                    }
                }
                K_SALE_RET -> {
                    lines.add(VoucherLine("5103", "", t.net, 0, "برگشت از فروش"))
                    lines.add(VoucherLine("2103", "", t.vat, 0, "مالیات برگشتی"))
                    lines.add(VoucherLine("1103", inv.partnerId, 0, t.total, partner?.name ?: ""))
                    val returnedCost = invoiceCost(inv.lines, d)
                    if (returnedCost > 0) {
                        lines.add(VoucherLine("1201", "", returnedCost, 0, "افزایش موجودی بابت برگشت فروش"))
                        lines.add(VoucherLine("5101", "", 0, returnedCost, "برگشت بهای تمام‌شده"))
                    }
                }
                K_PURCHASE_RET -> {
                    lines.add(VoucherLine("2101", inv.partnerId, t.total, 0, partner?.name ?: ""))
                    lines.add(VoucherLine("1105", "", 0, t.vat, "مالیات خرید برگشتی"))
                    lines.add(VoucherLine("1201", "", 0, t.net, "برگشت کالا"))
                }
            }
            out.add(Voucher(
                id = "sys-inv-${inv.id}", no = 0, date = inv.date,
                desc = "سند خودکار ${LB.invoiceKindLabel(inv.kind)} شمارهٔ ${inv.no}${if (inv.moudian) " (مودیان)" else ""}",
                lines = lines.filter { it.debit != 0L || it.credit != 0L },
                source = "inv:${inv.id}"
            ))
        }

        // Inventory adjustments are posted to stock and adjustment accounts at item master cost.
        for (doc in d.stockDocs.filter { it.kind == "adjust" }) {
            val lines = ArrayList<VoucherLine>()
            for (line in doc.lines) {
                val item = d.items.firstOrNull { it.id == line.itemId } ?: continue
                if (item.service || item.cost <= 0L || line.qty == 0.0) continue
                val value = Math.round(Math.abs(line.qty) * item.cost)
                if (value <= 0L) continue
                if (line.qty > 0.0) {
                    lines.add(VoucherLine("1201", "", value, 0, "افزایش موجودی شمارش: ${item.name}"))
                    lines.add(VoucherLine("4102", "", 0, value, "سود اصلاح موجودی: ${item.name}"))
                } else {
                    lines.add(VoucherLine("5102", "", value, 0, "کسری موجودی شمارش: ${item.name}"))
                    lines.add(VoucherLine("1201", "", 0, value, "کاهش موجودی شمارش: ${item.name}"))
                }
            }
            if (lines.isNotEmpty()) out.add(Voucher(
                id = "sys-stock-${doc.id}", no = 0, date = doc.date,
                desc = "سند خودکار اصلاح موجودی شمارهٔ ${doc.no}", lines = lines,
                source = "stock:${doc.id}"
            ))
        }

        for (p in d.payments) {
            val acc = bankAccOf(p.bankId)
            val partner = d.partners.firstOrNull { it.id == p.partnerId }
            if (p.kind == "receipt") {
                out.add(Voucher(
                    id = "sys-pay-${p.id}", no = 0, date = p.date,
                    desc = "سند خودکار دریافت ${LB.payMethodLabel(p.method)} شمارهٔ ${p.no}",
                    lines = if (p.method == "check") listOf(
                        VoucherLine("1104", "", p.amount, 0, "چک در جریان وصول"),
                        VoucherLine("1103", p.partnerId, 0, p.amount, partner?.name ?: "")
                    ) else listOf(
                        VoucherLine(acc, p.bankId, p.amount, 0, bankNameOf(p.bankId)),
                        VoucherLine("1103", p.partnerId, 0, p.amount, partner?.name ?: "")
                    ),
                    source = "pay:${p.id}"
                ))
            } else {
                out.add(Voucher(
                    id = "sys-pay-${p.id}", no = 0, date = p.date,
                    desc = "سند خودکار پرداخت ${LB.payMethodLabel(p.method)} شمارهٔ ${p.no}",
                    lines = listOf(
                        VoucherLine("2101", p.partnerId, p.amount, 0, partner?.name ?: ""),
                        VoucherLine(acc, p.bankId, 0, p.amount, bankNameOf(p.bankId))
                    ),
                    source = "pay:${p.id}"
                ))
            }
        }

        for (c in d.checks) {
            val partner = d.partners.firstOrNull { it.id == c.partnerId }
            if (c.kind == "received") {
                out.add(Voucher(
                    id = "sys-ck-${c.id}-rec", no = 0, date = c.date,
                    desc = "سند خودکار دریافت چک شمارهٔ ${c.serial}",
                    lines = listOf(
                        VoucherLine("1104", "", c.amount, 0, "چک ${c.bankName}"),
                        VoucherLine("1103", c.partnerId, 0, c.amount, partner?.name ?: "")
                    ),
                    source = "ck:${c.id}"
                ))
                when (c.status) {
                    "cleared" -> out.add(Voucher(
                        id = "sys-ck-${c.id}-clr", no = 0, date = c.dueDate,
                        desc = "سند خودکار وصول چک شمارهٔ ${c.serial}",
                        lines = listOf(
                            VoucherLine("1102", "", c.amount, 0, "وصول چک"),
                            VoucherLine("1104", "", 0, c.amount, "چک ${c.bankName}")
                        ),
                        source = "ck:${c.id}"
                    ))
                    "bounced" -> out.add(Voucher(
                        id = "sys-ck-${c.id}-bnc", no = 0, date = c.dueDate,
                        desc = "سند خودکار برگشت چک شمارهٔ ${c.serial}",
                        lines = listOf(
                            VoucherLine("1103", c.partnerId, c.amount, 0, partner?.name ?: ""),
                            VoucherLine("1104", "", 0, c.amount, "چک ${c.bankName}")
                        ),
                        source = "ck:${c.id}"
                    ))
                    "spent" -> out.add(Voucher(
                        id = "sys-ck-${c.id}-spn", no = 0, date = c.dueDate,
                        desc = "سند خودکار خرج چک شمارهٔ ${c.serial}",
                        lines = listOf(
                            VoucherLine("2101", c.partnerId, c.amount, 0, partner?.name ?: ""),
                            VoucherLine("1104", "", 0, c.amount, "چک ${c.bankName}")
                        ),
                        source = "ck:${c.id}"
                    ))
                }
            } else {
                out.add(Voucher(
                    id = "sys-ck-out-${c.id}", no = 0, date = c.date,
                    desc = "سند خودکار صدور چک شمارهٔ ${c.serial}",
                    lines = listOf(
                        VoucherLine("2101", c.partnerId, c.amount, 0, partner?.name ?: ""),
                        VoucherLine("2102", "", 0, c.amount, "چک ${c.bankName}")
                    ),
                    source = "ck:${c.id}"
                ))
                if (c.status == "cleared") out.add(Voucher(
                    id = "sys-ck-out2-${c.id}", no = 0, date = c.dueDate,
                    desc = "سند خودکار پاس شدن چک شمارهٔ ${c.serial}",
                    lines = listOf(
                        VoucherLine("2102", "", c.amount, 0, "چک ${c.bankName}"),
                        VoucherLine("1102", "", 0, c.amount, "پاس شدن چک")
                    ),
                    source = "ck:${c.id}-c"
                ))
                else if (c.status == "bounced") out.add(Voucher(
                    id = "sys-ck-out3-${c.id}", no = 0, date = c.dueDate,
                    desc = "سند خودکار برگشت چک پرداختی شمارهٔ ${c.serial}",
                    lines = listOf(
                        VoucherLine("2102", "", c.amount, 0, "چک ${c.bankName}"),
                        VoucherLine("2101", c.partnerId, 0, c.amount, partner?.name ?: "")
                    ),
                    source = "ck:${c.id}-b"
                ))
            }
        }
        return out
    }

    /** سند افتتاحیهٔ خودکار از ماندهٔ اولیهٔ صندوق‌ها و بانک‌ها */
    fun openingVouchers(d: DB = db): List<Voucher> {
        val out = ArrayList<Voucher>()
        for (b in d.banks) {
            if (b.opening == 0L) continue
            out.add(Voucher(
                id = "sys-open-${b.id}", no = 0, date = "${d.settings.fiscalYear}/01/01",
                desc = "سند افتتاحیهٔ ${LB.bankKindLabel(b.kind)} — ${b.name}",
                lines = listOf(
                    VoucherLine(if (b.kind == "cash") "1101" else "1102", b.id, b.opening, 0, b.name),
                    VoucherLine("3101", "", 0, b.opening, "سرمایه")
                ),
                source = "open:${b.id}"
            ))
        }
        return out
    }

    fun allVouchers(d: DB = db): List<Voucher> =
        (d.vouchers + autoVouchers(d) + openingVouchers(d)).sortedBy { it.date }

    // ══════════════════════════ موجودی انبار ══════════════════════════

    fun itemMoves(d: DB, itemId: String): List<StockMove> {
        val moves = ArrayList<StockMove>()
        for (inv in d.invoices) {
            if (!inv.final || inv.kind == K_PROFORMA) continue
            for (l in inv.lines) {
                if (l.itemId != itemId) continue
                val sign = when (inv.kind) {
                    K_SALE -> -1.0
                    K_PURCHASE -> 1.0
                    K_SALE_RET -> 1.0
                    K_PURCHASE_RET -> -1.0
                    else -> 0.0
                }
                if (sign != 0.0) moves.add(StockMove(sign * l.qty, inv.warehouseId, inv.kind, inv.date, "${inv.kind}-${inv.id}"))
            }
        }
        for (doc in d.stockDocs) {
            for (l in doc.lines) {
                if (l.itemId != itemId) continue
                if (doc.kind == "adjust") {
                    moves.add(StockMove(l.qty, doc.warehouseId, "adjust", doc.date, doc.id))
                } else {
                    moves.add(StockMove(-l.qty, doc.warehouseId, "transferOut", doc.date, doc.id))
                    if (doc.toWarehouseId.isNotEmpty()) moves.add(StockMove(l.qty, doc.toWarehouseId, "transferIn", doc.date, doc.id))
                }
            }
        }
        return moves
    }

    fun stockOf(d: DB, itemId: String, warehouseId: String = ""): Double {
        var s = 0.0
        for (m in itemMoves(d, itemId)) if (warehouseId.isEmpty() || m.warehouseId == warehouseId) s += m.qty
        return s
    }

    fun lowStockItems(d: DB = db): List<Item> =
        d.items.filter { !it.service && it.minStock > 0 && stockOf(d, it.id) < it.minStock }

    // ══════════════════════════ مانده‌ها ══════════════════════════

    /** ماندهٔ خالص طرف‌حساب — مثبت: طلبکار ما */
    fun partnerBalance(d: DB, partnerId: String): PartnerBal {
        var ar = 0L; var ap = 0L
        for (v in allVouchers(d)) for (l in v.lines) {
            if (l.detailId != partnerId) continue
            if (l.accountCode == "1103") ar += l.debit - l.credit
            if (l.accountCode == "2101") ap += l.credit - l.debit
        }
        return PartnerBal(ar, ap, ar - ap)
    }

    fun bankBalance(d: DB, bankId: String): Long {
        var bal = 0L
        for (v in allVouchers(d)) for (l in v.lines)
            if (l.detailId == bankId && (l.accountCode == "1101" || l.accountCode == "1102")) bal += l.debit - l.credit
        return bal
    }

    fun totalCash(d: DB = db): Long = d.banks.sumOf { bankBalance(d, it.id) }
    fun totalReceivable(d: DB = db): Long = d.partners.sumOf { Math.max(0L, partnerBalance(d, it.id).net) }
    fun totalPayable(d: DB = db): Long = d.partners.sumOf { Math.max(0L, -partnerBalance(d, it.id).net) }

    // ══════════════════════════ گزارش‌های مالی ══════════════════════════

    fun trialBalance(d: DB = db): List<AccRow> {
        val rows = LinkedHashMap<String, AccRow>()
        for (a in ACCOUNTS) rows[a.code] = AccRow(a.code, a.name, a.kind, a.grp, 0, 0, 0)
        for (v in allVouchers(d)) for (l in v.lines) {
            val r = rows[l.accountCode] ?: continue
            rows[l.accountCode] = r.copy(debit = r.debit + l.debit, credit = r.credit + l.credit)
        }
        return rows.values.map { r ->
            val bal = if (r.kind == "asset" || r.kind == "expense") r.debit - r.credit else r.credit - r.debit
            r.copy(balance = bal)
        }
    }

    fun profitLoss(d: DB = db): PL {
        val rows = trialBalance(d)
        val income = rows.filter { it.kind == "income" }.sumOf { it.balance }
        val returns = rows.filter { it.code == "5103" }.sumOf { it.balance }
        val expenses = rows.filter { it.kind == "expense" && it.code != "5103" }.sumOf { it.balance }
        val cogs = rows.firstOrNull { it.code == "5101" }?.balance ?: 0
        return PL(income, returns, expenses, cogs, income - returns - expenses, rows)
    }

    fun balanceSheet(d: DB = db): BSDat {
        val rows = trialBalance(d)
        val assets = rows.filter { it.kind == "asset" }
        val liab = rows.filter { it.kind == "liability" }
        val equity = rows.filter { it.kind == "equity" }
        val assetsSum = assets.sumOf { it.balance }
        val liabSum = liab.sumOf { it.balance }
        val pl = profitLoss(d)
        val equitySum = equity.sumOf { it.balance } + Math.max(0L, pl.net)
        return BSDat(assets, liab, equity, assetsSum, liabSum, equitySum, pl)
    }

    /** فروش ماهانهٔ سال جاری برای نمودار */
    fun monthlySales(d: DB = db, kind: String = K_SALE): List<MonthVal> {
        val year = J.jYearOfToday()
        val sums = (1..12).map { MonthVal(it, 0L) }.toMutableList()
        for (inv in d.invoices) {
            if (!inv.final || inv.kind != kind) continue
            val y = J.jYearOf(inv.date); val m = J.jMonthOf(inv.date)
            if (y != year || m < 1 || m > 12) continue
            sums[m - 1] = sums[m - 1].copy(total = sums[m - 1].total + invoiceTotals(inv).net)
        }
        return sums
    }

    fun salesByItem(d: DB = db, kind: String = K_SALE): List<ByItemRow> {
        val map = LinkedHashMap<String, DoubleArray>()
        for (inv in d.invoices) {
            if (!inv.final || inv.kind != kind) continue
            for (l in inv.lines) {
                val cur = map[l.itemId] ?: doubleArrayOf(0.0, 0.0)
                cur[0] += l.qty
                cur[1] += l.qty * l.price - l.qty * l.discount
                map[l.itemId] = cur
            }
        }
        // Allocate invoice-wide discount proportionally across item rows so item reports
        // reconcile to invoice net sales (line discounts already applied above).
        for (inv in d.invoices) {
            if (!inv.final || inv.kind != kind || inv.discount <= 0L) continue
            val base = inv.lines.sumOf { it.qty * it.price - it.qty * it.discount }.coerceAtLeast(0.0)
            if (base <= 0.0) continue
            for (l in inv.lines) {
                val cur = map[l.itemId] ?: doubleArrayOf(0.0, 0.0)
                val lineBase = (l.qty * l.price - l.qty * l.discount).coerceAtLeast(0.0)
                cur[1] -= inv.discount * (lineBase / base)
                map[l.itemId] = cur
            }
        }
        return map.entries.mapNotNull { (itemId, v) ->
            val item = d.items.firstOrNull { it.id == itemId } ?: return@mapNotNull null
            ByItemRow(item, v[0], Math.round(v[1]))
        }.sortedByDescending { it.total }
    }

    fun salesByPartner(d: DB = db, kind: String = K_SALE): List<ByPartnerRow> {
        val map = LinkedHashMap<String, LongArray>()
        for (inv in d.invoices) {
            if (!inv.final || inv.kind != kind) continue
            val cur = map[inv.partnerId] ?: longArrayOf(0L, 0L)
            cur[0] += invoiceTotals(inv).total
            cur[1] += 1
            map[inv.partnerId] = cur
        }
        return map.entries.mapNotNull { (pid, v) ->
            val p = d.partners.firstOrNull { it.id == pid } ?: return@mapNotNull null
            ByPartnerRow(p, v[0], v[1].toInt())
        }.sortedByDescending { it.total }
    }

    // ══════════════════════════ موتور هوشمند ══════════════════════════

    fun overdueSales(d: DB = db): List<OverdueRow> {
        val today = J.jTodayStr()
        val out = ArrayList<OverdueRow>()
        for (inv in d.invoices) {
            if (inv.kind != K_SALE || !inv.final) continue
            val due = invoiceTotals(inv).due
            if (due <= 0) continue
            val age = J.jDiff(today, inv.date)
            if (age <= 30) continue
            val partner = d.partners.firstOrNull { it.id == inv.partnerId } ?: continue
            val net = partnerBalance(d, partner.id).net
            if (net <= 0) continue
            out.add(OverdueRow(inv, partner, Math.min(due, net), age))
        }
        return out.sortedByDescending { it.days }
    }

    fun agingBuckets(d: DB = db): List<AgingRow> {
        val today = J.jTodayStr()
        val buckets = mutableListOf(
            AgingRow("۰ تا ۳۰ روز", 0, "green"),
            AgingRow("۳۱ تا ۶۰ روز", 0, "amber"),
            AgingRow("۶۱ تا ۹۰ روز", 0, "rose"),
            AgingRow("بیش از ۹۰ روز", 0, "slate")
        )
        for (inv in d.invoices) {
            if (inv.kind != K_SALE || !inv.final) continue
            val due = invoiceTotals(inv).due
            if (due <= 0) continue
            val age = J.jDiff(today, inv.date)
            val b = if (age <= 30) 0 else if (age <= 60) 1 else if (age <= 90) 2 else 3
            buckets[b] = buckets[b].copy(amount = buckets[b].amount + due)
        }
        return buckets
    }

    /** Cash-basis monthly flows: only cash actually paid/received, not invoice totals.
     * Invoice paid amounts are counted here; separate Payments are counted as their own cash events.
     * Because v16 Payment records are not linked to invoices, users must avoid recording the same
     * settlement both in invoice.paid and as a separate Payment until invoice allocation is added.
     */
    fun cashflow(d: DB = db, months: Int = 6): List<CFRow> {
        val today = J.jTodayStr()
        val p = J.jParse(today)
        val list = ArrayList<Triple<String, Int, LongArray>>() // ym, month, [cash-in, cash-out]
        for (i in months - 1 downTo 0) {
            var mm = p[1] - i
            var yy = p[0]
            while (mm <= 0) { mm += 12; yy-- }
            list.add(Triple(String.format(java.util.Locale.US, "%d/%02d", yy, mm), mm, longArrayOf(0, 0)))
        }
        for (inv in d.invoices) {
            if (!inv.final || inv.paid <= 0) continue
            val i = list.indexOfFirst { it.first == J.ymKey(inv.date) }
            if (i < 0) continue
            val row = list[i]
            when (inv.kind) {
                K_SALE -> row.third[0] += inv.paid
                K_PURCHASE -> row.third[1] += inv.paid
                K_SALE_RET -> row.third[1] += inv.paid
                K_PURCHASE_RET -> row.third[0] += inv.paid
            }
        }
        for (pay in d.payments) {
            val i = list.indexOfFirst { it.first == J.ymKey(pay.date) }
            if (i < 0 || pay.method == "check") continue
            val row = list[i]
            if (pay.kind == "payment") row.third[1] += pay.amount
            if (pay.kind == "receipt") row.third[0] += pay.amount
        }
        // Cleared/used checks are actual cash movements on their clearing/due date.
        // Check-method Payments are excluded above to avoid counting their issue date twice.
        for (check in d.checks) {
            val movementDate = check.dueDate
            val i = list.indexOfFirst { it.first == J.ymKey(movementDate) }
            if (i < 0) continue
            if (check.kind == "received" && check.status == "cleared") list[i].third[0] += check.amount
            if (check.kind == "issued" && (check.status == "cleared" || check.status == "spent")) list[i].third[1] += check.amount
        }
        for (v in d.vouchers) {
            val i = list.indexOfFirst { it.first == J.ymKey(v.date) }
            if (i < 0) continue
            val row = list[i]
            row.third[1] += v.lines.filter { LB.accKindOf(it.accountCode) == "expense" }.sumOf { it.debit }
        }
        return list.map { CFRow(it.second, it.third[0], it.third[1]) }
    }

    fun smartAlerts(d: DB = db): List<SmartAlert> {
        val out = ArrayList<SmartAlert>()
        val today = J.jTodayStr()

        val od = overdueSales(d)
        if (od.isNotEmpty()) {
            val sum = od.sumOf { it.amount }
            out.add(SmartAlert("overdue", "rose", "alert",
                "${J.faN(od.size)} فاکتور از سررسید گذشته",
                "${J.moneyShort(sum)} تومان مطالبات معوق — پیگیری با پیامک یادآوری", "invoices"))
        }

        val nearChecks = d.checks.filter {
            (it.kind == "received" && (it.status == "drawer" || it.status == "inBank")) || (it.kind == "issued" && it.status == "drawer")
        }.filter { J.jDiff(it.dueDate, today) >= 0 && J.jDiff(it.dueDate, today) <= 7 }
        if (nearChecks.isNotEmpty()) {
            val sum = nearChecks.sumOf { it.amount }
            out.add(SmartAlert("checks", "amber", "clock",
                "${J.faN(nearChecks.size)} چک تا ۷ روز آینده سررسید می‌شود",
                "مجموع ${J.moneyShort(sum)} تومان — برای وصول آماده کنید", "checks"))
        }

        val lows = lowStockItems(d)
        if (lows.isNotEmpty()) {
            out.add(SmartAlert("stock", "sky", "box",
                "${J.faN(lows.size)} کالا به نقطهٔ سفارش رسیده",
                "کمترین: " + lows.take(2).joinToString(" و ") { it.name }, "warehouse"))
        }

        val unmatched = d.bankTxns.filter { it.matched == null }
        if (unmatched.isNotEmpty()) {
            out.add(SmartAlert("bank", "violet", "bank",
                "${J.faN(unmatched.size)} تراکنش بانکی تطبیق نشده",
                "با یک لمس با فاکتورها و دریافت‌ها تطبیق دهید", "reconcile"))
        }

        val unbilled = d.timeEntries.filter { it.billed.isEmpty() && it.billable }
        if (unbilled.isNotEmpty()) {
            val h = unbilled.sumOf { it.minutes } / 60.0
            val v = unbilled.sumOf { (it.minutes / 60.0) * it.rate }
            out.add(SmartAlert("time", "green", "time",
                "${J.faN(Math.round(h))} ساعت کار ثبت‌شده هنوز فاکتور نشده",
                "${J.moneyShort(v)} تومان درآمد در انتظار صدور فاکتور", "projects"))
        }

        val dueRecurring = d.recurring.filter { it.active && J.jDiff(today, it.nextDate) >= 0 }
        if (dueRecurring.isNotEmpty()) {
            out.add(SmartAlert("repeat", "sky", "repeat",
                "${J.faN(dueRecurring.size)} فاکتور تکرارشونده آمادهٔ صدور است",
                dueRecurring.joinToString(" • ") { it.title }, "invoices"))
        }

        val cf = cashflow(d, 6)
        if (cf.size >= 4) {
            val cur = cf.last()
            val prev3 = cf.subList(cf.size - 4, cf.size - 1)
            val avg = prev3.sumOf { it.expense } / 3.0
            if (avg > 0 && cur.expense > avg * 1.4) {
                val pct = Math.round(((cur.expense - avg) / avg) * 100)
                out.add(SmartAlert("anomaly-exp", "amber", "trend",
                    "هزینه‌های این ماه ${J.faN(pct)}٪ بیشتر از میانگین است",
                    "مثل تشخیص ناهنجاری Zia — بررسی کنید که هزینهٔ غیرعادی ثبت نشده باشد", "reports"))
            }
            val avgInc = prev3.sumOf { it.income } / 3.0
            if (cur.income > 0 && avgInc > 0 && cur.income < avgInc * 0.55) {
                out.add(SmartAlert("anomaly-inc", "rose", "trend",
                    "فروش این ماه نسبت به میانگین افت کرده",
                    "کمپین تخفیف یا پیگیری مشتریان پیشنهاد می‌شود", "reports"))
            }
        }
        return out
    }

    fun projectHours(d: DB, projectId: String): ProjHours {
        val entries = d.timeEntries.filter { it.projectId == projectId }.sortedByDescending { it.date }
        val totalMin = entries.sumOf { it.minutes }
        val unbilled = entries.filter { it.billed.isEmpty() && it.billable }
        val unbilledMin = unbilled.sumOf { it.minutes }
        val unbilledValue = unbilled.sumOf { (it.minutes / 60.0) * it.rate }
        return ProjHours(entries, totalMin, unbilled, unbilledMin, unbilledValue)
    }

    /** صدور خودکار فاکتورهای تکرارشوندهٔ سررسیدشده */
    fun runRecurring(d0: DB): Pair<DB, List<Invoice>> {
        val today = J.jTodayStr()
        val invoices = d0.invoices.toMutableList()
        val created = ArrayList<Invoice>()
        val recurring = ArrayList<Recurring>()
        for (r in d0.recurring) {
            if (!r.active) { recurring.add(r); continue }
            var next = if (J.jParse(r.nextDate)[0] >= 1300) r.nextDate else today
            var last = r.lastGenerated
            var guard = 0
            while (J.jDiff(today, next) >= 0 && guard < 12) {
                val maxSale = invoices.filter { it.kind == K_SALE }.maxOfOrNull { it.no } ?: 100L
                val inv = Invoice(
                    id = newId("inv"), no = maxSale + 1, kind = K_SALE,
                    partnerId = r.partnerId, warehouseId = r.warehouseId, date = next,
                    lines = r.lines.map { it.copy(id = newId("l")) },
                    discount = r.discount, vatRate = r.vatRate, paid = 0,
                    final = true, moudian = d0.settings.moudian,
                    desc = "صدور خودکار — ${r.title}"
                )
                invoices.add(0, inv)
                created.add(inv)
                last = next
                next = J.jAddMonths(next, Math.max(1L, r.everyMonths))
                guard++
            }
            recurring.add(r.copy(nextDate = next, lastGenerated = last))
        }
        return Pair(d0.copy(invoices = invoices, recurring = recurring), created)
    }

    /** جستجوی سراسری (Command Palette) */
    fun globalSearch(d: DB, q: String): List<SearchHit> {
        val qq = q.trim()
        if (qq.isEmpty()) return emptyList()
        val hits = ArrayList<SearchHit>()
        for (i in d.items) if (i.name.contains(qq, true) || i.code.contains(qq, true) || i.barcode.contains(qq, true))
            hits.add(SearchHit("کالا", i.name, "کد ${J.fa(i.code)} • ${J.moneyFa(i.price)} تومان", "items"))
        for (p in d.partners) if (p.name.contains(qq, true) || p.phone.contains(qq, true))
            hits.add(SearchHit("طرف‌حساب", p.name, if (p.phone.isNotEmpty()) "تلفن ${J.fa(p.phone)}" else "", "partners"))
        for (i in d.invoices) {
            val p = d.partners.firstOrNull { it.id == i.partnerId }
            if (i.no.toString().contains(qq) || (p?.name ?: "").contains(qq, true))
                hits.add(SearchHit("فاکتور", "${LB.invoiceKindShort(i.kind)} ${J.faN(i.no)} — ${p?.name ?: ""}", "${J.jFa(i.date)} • ${J.moneyFa(invoiceTotals(i).total)} تومان", "invoices"))
        }
        for (c in d.checks) if (c.serial.contains(qq, true) || c.no.toString().contains(qq))
            hits.add(SearchHit("چک", "چک ${J.fa(c.serial)}", "${J.jFa(c.dueDate)} • ${J.moneyFa(c.amount)} تومان", "checks"))
        for (pr in d.projects) if (pr.name.contains(qq, true))
            hits.add(SearchHit("پروژه", pr.name, "نرخ ${J.moneyFa(pr.rate)} تومان/ساعت", "projects"))
        for (t in d.bankTxns) if (t.desc.contains(qq, true))
            hits.add(SearchHit("بانک", t.desc, "${J.jFa(t.date)} • ${J.moneyFa(Math.abs(t.amount))} تومان", "reconcile"))
        return hits.take(12)
    }

    // ══════════════════════════ JSON ══════════════════════════

    private fun s(o: JSONObject, k: String, def: String = ""): String = try { o.optString(k, def) } catch (ig: Exception) { def }
    private fun b(o: JSONObject, k: String, def: Boolean = false): Boolean = try { o.optBoolean(k, def) } catch (ig: Exception) { def }

    fun toJson(d: DB): JSONObject {
        val o = JSONObject()
        o.put("version", d.version)
        val st = JSONObject()
        st.put("shopName", d.settings.shopName); st.put("ownerName", d.settings.ownerName)
        st.put("phone", d.settings.phone); st.put("address", d.settings.address)
        st.put("vatRate", d.settings.vatRate); st.put("fiscalYear", d.settings.fiscalYear)
        st.put("moudian", d.settings.moudian)
        o.put("settings", st)

        fun linesJson(lines: List<InvoiceLine>): JSONArray = JSONArray().apply {
            for (l in lines) put(JSONObject().apply {
                put("id", l.id); put("itemId", l.itemId); put("qty", okD(l.qty))
                put("price", l.price); put("discount", okD(l.discount))
            })
        }

        o.put("groups", JSONArray().apply { for (g in d.groups) put(JSONObject().apply { put("id", g.id); put("name", g.name) }) })
        o.put("items", JSONArray().apply { for (i in d.items) put(JSONObject().apply {
            put("id", i.id); put("code", i.code); put("name", i.name); put("groupId", i.groupId)
            put("unit", i.unit); put("price", i.price); put("cost", i.cost); put("service", i.service)
            put("minStock", okD(i.minStock)); put("barcode", i.barcode); put("desc", i.desc)
        }) })
        o.put("warehouses", JSONArray().apply { for (w in d.warehouses) put(JSONObject().apply { put("id", w.id); put("name", w.name); put("loc", w.loc) }) })
        o.put("partners", JSONArray().apply { for (p in d.partners) put(JSONObject().apply {
            put("id", p.id); put("code", p.code); put("name", p.name); put("type", p.type)
            put("phone", p.phone); put("address", p.address); put("nationalId", p.nationalId); put("desc", p.desc)
            put("email", p.email); put("postalCode", p.postalCode); put("economicCode", p.economicCode)
            put("creditLimit", p.creditLimit); put("paymentTermDays", p.paymentTermDays)
        }) })
        o.put("banks", JSONArray().apply { for (b in d.banks) put(JSONObject().apply {
            put("id", b.id); put("name", b.name); put("kind", b.kind); put("accountNo", b.accountNo); put("opening", b.opening); put("branch", b.branch); put("iban", b.iban); put("cardNo", b.cardNo); put("accountHolder", b.accountHolder); put("desc", b.desc)
        }) })
        o.put("invoices", JSONArray().apply { for (i in d.invoices) put(JSONObject().apply {
            put("id", i.id); put("no", i.no); put("kind", i.kind); put("partnerId", i.partnerId)
            put("warehouseId", i.warehouseId); put("date", i.date); put("lines", linesJson(i.lines))
            put("discount", i.discount); put("vatRate", i.vatRate); put("paid", i.paid)
            put("payBankId", i.payBankId); put("final", i.final); put("moudian", i.moudian); put("desc", i.desc)
            put("supplierInvoiceNo", i.supplierInvoiceNo)
        }) })
        o.put("payments", JSONArray().apply { for (p in d.payments) put(JSONObject().apply {
            put("id", p.id); put("no", p.no); put("kind", p.kind); put("method", p.method)
            put("partnerId", p.partnerId); put("bankId", p.bankId); put("amount", p.amount)
            put("date", p.date); put("desc", p.desc)
        }) })
        o.put("checks", JSONArray().apply { for (c in d.checks) put(JSONObject().apply {
            put("id", c.id); put("no", c.no); put("kind", c.kind); put("partnerId", c.partnerId)
            put("bankName", c.bankName); put("serial", c.serial); put("amount", c.amount)
            put("date", c.date); put("dueDate", c.dueDate); put("status", c.status); put("desc", c.desc)
        }) })
        o.put("stockDocs", JSONArray().apply { for (d0 in d.stockDocs) put(JSONObject().apply {
            put("id", d0.id); put("no", d0.no); put("kind", d0.kind); put("date", d0.date)
            put("warehouseId", d0.warehouseId); put("toWarehouseId", d0.toWarehouseId)
            put("lines", JSONArray().apply { for (l in d0.lines) put(JSONObject().apply { put("itemId", l.itemId); put("qty", okD(l.qty)) }) })
            put("desc", d0.desc)
        }) })
        o.put("vouchers", JSONArray().apply { for (v in d.vouchers) put(JSONObject().apply {
            put("id", v.id); put("no", v.no); put("date", v.date); put("desc", v.desc); put("source", v.source)
            put("lines", JSONArray().apply { for (l in v.lines) put(JSONObject().apply {
                put("accountCode", l.accountCode); put("detailId", l.detailId)
                put("debit", l.debit); put("credit", l.credit); put("note", l.note)
            }) })
        }) })
        o.put("reminders", JSONArray().apply { for (r in d.reminders) put(JSONObject().apply {
            put("id", r.id); put("date", r.date); put("title", r.title); put("desc", r.desc); put("done", r.done); put("priority", r.priority); put("category", r.category)
        }) })
        o.put("recurring", JSONArray().apply { for (r in d.recurring) put(JSONObject().apply {
            put("id", r.id); put("title", r.title); put("active", r.active); put("partnerId", r.partnerId)
            put("warehouseId", r.warehouseId); put("lines", linesJson(r.lines))
            put("discount", r.discount); put("vatRate", r.vatRate); put("everyMonths", r.everyMonths)
            put("nextDate", r.nextDate); put("lastGenerated", r.lastGenerated)
        }) })
        o.put("projects", JSONArray().apply { for (p in d.projects) put(JSONObject().apply {
            put("id", p.id); put("name", p.name); put("partnerId", p.partnerId)
            put("rate", p.rate); put("color", p.color); put("done", p.done); put("code", p.code); put("startDate", p.startDate); put("dueDate", p.dueDate); put("budget", p.budget); put("desc", p.desc)
        }) })
        o.put("timeEntries", JSONArray().apply { for (t in d.timeEntries) put(JSONObject().apply {
            put("id", t.id); put("projectId", t.projectId); put("date", t.date); put("desc", t.desc)
            put("minutes", t.minutes); put("rate", t.rate); put("billed", t.billed); put("billable", t.billable)
        }) })
        o.put("bankTxns", JSONArray().apply { for (t in d.bankTxns) put(JSONObject().apply {
            put("id", t.id); put("bankId", t.bankId); put("date", t.date); put("amount", t.amount); put("desc", t.desc)
            if (t.matched != null) { put("matchedKind", t.matched.kind); put("matchedLabel", t.matched.label) }
        }) })
        o.put("bankRules", JSONArray().apply { for (r in d.bankRules) put(JSONObject().apply {
            put("id", r.id); put("keyword", r.keyword); put("target", r.target); put("note", r.note)
        }) })
        o.put("accounts", JSONArray().apply { for (a in d.accounts) put(JSONObject().apply {
            put("code", a.code); put("name", a.name); put("kind", a.kind); put("group", a.grp)
        }) })
        val cnt = JSONObject()
        for ((k, v) in d.counters) cnt.put(k, v)
        o.put("counters", cnt)
        return o
    }

    fun fromJson(root: JSONObject): DB? = try {
        fun linesJson(a: JSONArray): List<InvoiceLine> = (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            InvoiceLine(s(o, "id"), s(o, "itemId"), o.optDouble("qty", 1.0), o.optLong("price"), o.optDouble("discount"))
        }
        val st = root.optJSONObject("settings") ?: JSONObject()
        val settings = Settings(
            s(st, "shopName", "فروشگاه من"), s(st, "ownerName", "مدیر"), s(st, "phone"),
            s(st, "address"), st.optLong("vatRate", 10), s(st, "fiscalYear", J.jYearOfToday().toString()),
            b(st, "moudian", true)
        )
        fun arr(k: String) = root.optJSONArray(k) ?: JSONArray()
        val groups = (0 until arr("groups").length()).mapNotNull { i ->
            val o = arr("groups").optJSONObject(i) ?: return@mapNotNull null
            ItemGroup(s(o, "id"), s(o, "name"))
        }
        val items = (0 until arr("items").length()).mapNotNull { i ->
            val o = arr("items").optJSONObject(i) ?: return@mapNotNull null
            Item(s(o, "id"), s(o, "code"), s(o, "name"), s(o, "groupId"), s(o, "unit", "عدد"),
                o.optLong("price"), o.optLong("cost"), b(o, "service"), o.optDouble("minStock"), s(o, "barcode"), s(o, "desc"))
        }
        val warehouses = (0 until arr("warehouses").length()).mapNotNull { i ->
            val o = arr("warehouses").optJSONObject(i) ?: return@mapNotNull null
            Warehouse(s(o, "id"), s(o, "name"), s(o, "loc"))
        }
        val partners = (0 until arr("partners").length()).mapNotNull { i ->
            val o = arr("partners").optJSONObject(i) ?: return@mapNotNull null
            Partner(s(o, "id"), s(o, "code"), s(o, "name"), s(o, "type", "customer"), s(o, "phone"), s(o, "address"), s(o, "nationalId"), s(o, "desc"), s(o, "email"), s(o, "postalCode"), s(o, "economicCode"), o.optLong("creditLimit", 0), o.optLong("paymentTermDays", 0))
        }
        val banks = (0 until arr("banks").length()).mapNotNull { i ->
            val o = arr("banks").optJSONObject(i) ?: return@mapNotNull null
            Bank(s(o, "id"), s(o, "name"), s(o, "kind", "cash"), s(o, "accountNo"), o.optLong("opening"), s(o, "branch"), s(o, "iban"), s(o, "cardNo"), s(o, "accountHolder"), s(o, "desc"))
        }
        val invoices = (0 until arr("invoices").length()).mapNotNull { i ->
            val o = arr("invoices").optJSONObject(i) ?: return@mapNotNull null
            Invoice(s(o, "id"), o.optLong("no"), s(o, "kind", K_SALE), s(o, "partnerId"), s(o, "warehouseId"), s(o, "date"),
                linesJson(o.optJSONArray("lines") ?: JSONArray()), o.optLong("discount"), o.optLong("vatRate", 10),
                o.optLong("paid"), s(o, "payBankId"), b(o, "final"), b(o, "moudian"), s(o, "desc"), s(o, "supplierInvoiceNo"))
        }
        val payments = (0 until arr("payments").length()).mapNotNull { i ->
            val o = arr("payments").optJSONObject(i) ?: return@mapNotNull null
            Payment(s(o, "id"), o.optLong("no"), s(o, "kind", "receipt"), s(o, "method", "cash"), s(o, "partnerId"),
                s(o, "bankId"), o.optLong("amount"), s(o, "date"), s(o, "desc"))
        }
        val checks = (0 until arr("checks").length()).mapNotNull { i ->
            val o = arr("checks").optJSONObject(i) ?: return@mapNotNull null
            Check(s(o, "id"), o.optLong("no"), s(o, "kind", "received"), s(o, "partnerId"), s(o, "bankName"),
                s(o, "serial"), o.optLong("amount"), s(o, "date"), s(o, "dueDate"), s(o, "status", "drawer"), s(o, "desc"))
        }
        val stockDocs = (0 until arr("stockDocs").length()).mapNotNull { i ->
            val o = arr("stockDocs").optJSONObject(i) ?: return@mapNotNull null
            StockDoc(s(o, "id"), o.optLong("no"), s(o, "kind", "adjust"), s(o, "date"), s(o, "warehouseId"), s(o, "toWarehouseId"),
                (0 until (o.optJSONArray("lines") ?: JSONArray()).length()).mapNotNull { j ->
                    val l = (o.optJSONArray("lines") ?: JSONArray()).optJSONObject(j) ?: return@mapNotNull null
                    StockLine(s(l, "itemId"), l.optDouble("qty"))
                }, s(o, "desc"))
        }
        val vouchers = (0 until arr("vouchers").length()).mapNotNull { i ->
            val o = arr("vouchers").optJSONObject(i) ?: return@mapNotNull null
            Voucher(s(o, "id"), o.optLong("no"), s(o, "date"), s(o, "desc"),
                (0 until (o.optJSONArray("lines") ?: JSONArray()).length()).mapNotNull { j ->
                    val l = (o.optJSONArray("lines") ?: JSONArray()).optJSONObject(j) ?: return@mapNotNull null
                    VoucherLine(s(l, "accountCode"), s(l, "detailId"), l.optLong("debit"), l.optLong("credit"), s(l, "note"))
                }, s(o, "source"))
        }
        val reminders = (0 until arr("reminders").length()).mapNotNull { i ->
            val o = arr("reminders").optJSONObject(i) ?: return@mapNotNull null
            Reminder(s(o, "id"), s(o, "date"), s(o, "title"), s(o, "desc"), b(o, "done"), s(o, "priority", "normal"), s(o, "category", "general"))
        }
        val recurring = (0 until arr("recurring").length()).mapNotNull { i ->
            val o = arr("recurring").optJSONObject(i) ?: return@mapNotNull null
            Recurring(s(o, "id"), s(o, "title"), b(o, "active", true), s(o, "partnerId"), s(o, "warehouseId"),
                linesJson(o.optJSONArray("lines") ?: JSONArray()), o.optLong("discount"), o.optLong("vatRate", 10),
                o.optLong("everyMonths", 1), s(o, "nextDate"), s(o, "lastGenerated"))
        }
        val projects = (0 until arr("projects").length()).mapNotNull { i ->
            val o = arr("projects").optJSONObject(i) ?: return@mapNotNull null
            Project(s(o, "id"), s(o, "name"), s(o, "partnerId"), o.optLong("rate"), s(o, "color", "#0d9488"), b(o, "done"), s(o, "code"), s(o, "startDate"), s(o, "dueDate"), o.optLong("budget"), s(o, "desc"))
        }
        val timeEntries = (0 until arr("timeEntries").length()).mapNotNull { i ->
            val o = arr("timeEntries").optJSONObject(i) ?: return@mapNotNull null
            TimeEntry(s(o, "id"), s(o, "projectId"), s(o, "date"), s(o, "desc"), o.optLong("minutes"), o.optLong("rate"), s(o, "billed"), b(o, "billable", true))
        }
        val bankTxns = (0 until arr("bankTxns").length()).mapNotNull { i ->
            val o = arr("bankTxns").optJSONObject(i) ?: return@mapNotNull null
            val mk = s(o, "matchedKind")
            BankTxn(s(o, "id"), s(o, "bankId"), s(o, "date"), o.optLong("amount"), s(o, "desc"),
                if (mk.isNotEmpty()) BankMatch(mk, s(o, "matchedLabel")) else null)
        }
        val bankRules = (0 until arr("bankRules").length()).mapNotNull { i ->
            val o = arr("bankRules").optJSONObject(i) ?: return@mapNotNull null
            BankRule(s(o, "id"), s(o, "keyword"), s(o, "target", "payment"), s(o, "note"))
        }
        val accounts = (0 until arr("accounts").length()).mapNotNull { i ->
            val o = arr("accounts").optJSONObject(i) ?: return@mapNotNull null
            Account(s(o, "code"), s(o, "name"), s(o, "kind", "asset"), s(o, "group"))
        }.ifEmpty { ACCOUNTS }
        val counters = HashMap<String, Long>()
        val cnt = root.optJSONObject("counters") ?: JSONObject()
        for (k in cnt.keys()) counters[k] = cnt.optLong(k)
        DB(root.optLong("version", 5), settings, groups, items, warehouses, partners, banks, invoices, payments,
            checks, stockDocs, vouchers, reminders, recurring, projects, timeEntries, bankTxns, bankRules, accounts, counters)
    } catch (e: Exception) { null }

    // ══════════════════════════ دادهٔ نمونه ══════════════════════════

    fun seed(): DB {
        val g1 = "grp-main"; val g2 = "grp-av"; val g3 = "grp-kitchen"; val g4 = "grp-service"
        val w1 = "wh-main"; val w2 = "wh-shop"; val w3 = "wh-branch"
        val today = J.jTodayStr()
        val p0 = J.jParse(today)
        fun monthsAgo(n: Int, day: Int): String {
            var mm = p0[1] - n
            var yy = p0[0]
            while (mm <= 0) { mm += 12; yy-- }
            val dl = J.jMonthLen(yy, mm)
            return String.format("%d/%02d/%02d", yy, mm, Math.min(day, dl))
        }

        val settings = Settings(
            "فروشگاه لوازم خانگی آرین", "مدیر فروشگاه", "۰۹۱۲۳۴۵۶۷۸۹",
            "تهران، خیابان ولیعصر، پلاک ۱۲۰", 10, J.jYearOfToday().toString(), true
        )
        val groups = listOf(
            ItemGroup(g1, "لوازم خانگی بزرگ"), ItemGroup(g2, "صوتی و تصویری"),
            ItemGroup(g3, "آشپزخانه"), ItemGroup(g4, "خدمات و تعمیرات")
        )
        val items = listOf(
            Item("it-ref", "101", "یخچال فریزر سامسونگ ۲۰ فوت", g1, "دستگاه", 45_800_000, 41_200_000, false, 2.0, "8801234567890"),
            Item("it-wash", "102", "ماشین لباسشویی اسنوا ۸ کیلویی", g1, "دستگاه", 28_500_000, 25_300_000, false, 2.0),
            Item("it-stove", "103", "اجاق گاز پاتریس ۵ شعله", g1, "دستگاه", 18_900_000, 16_100_000, false, 3.0),
            Item("it-tv", "201", "تلویزیون ایکس‌ویژن ۵۵ اینچ", g2, "دستگاه", 32_400_000, 28_700_000, false, 2.0),
            Item("it-speaker", "202", "ست بلوتوث ایمی ۱۵۰ وات", g2, "دستگاه", 9_800_000, 7_600_000),
            Item("it-micro", "301", "ماکروویو دوو ۲۸ لیتری", g3, "دستگاه", 12_700_000, 10_900_000, false, 2.0),
            Item("it-kettle", "302", "کتری و قوری برقی فلر", g3, "سرویس", 3_450_000, 2_650_000, false, 5.0),
            Item("it-vacuum", "303", "جاروبرقی کنا ستاره", g3, "دستگاه", 7_900_000, 6_400_000),
            Item("it-rice", "304", "پلوپز پارس خزر ۵ نفره", g3, "دستگاه", 2_980_000, 2_300_000, false, 6.0),
            Item("it-install", "401", "نصب و راه‌اندازی", g4, "ساعت", 850_000, 0, true),
            Item("it-repair", "402", "خدمات تعمیرات", g4, "ساعت", 1_200_000, 0, true)
        )
        val warehouses = listOf(
            Warehouse(w1, "انبار اصلی", "جیران"), Warehouse(w2, "فروشگاه", "ولیعصر"), Warehouse(w3, "شعبهٔ ۲", "صادقیه")
        )
        val partners = listOf(
            Partner("pt-c1", "501", "آقای رضایی", "customer", "۰۹۱۲۱۱۱۲۲۳۳", "تهران، نارمک"),
            Partner("pt-c2", "502", "خانم موسوی", "customer", "۰۹۳۵۴۴۴۵۵۶۶"),
            Partner("pt-c3", "503", "شرکت ساختمانی سپهر", "both", "۰۲۱۸۸۸۸۷۷۶۶", "", "14001234567"),
            Partner("pt-c4", "504", "آقای کریمی", "customer", "۰۹۹۰۱۲۳۴۵۶۷"),
            Partner("pt-s1", "601", "پخش لوازم خانگی کاوه", "supplier", "۰۲۱۶۶۵۵۴۴۳۳"),
            Partner("pt-s2", "602", "تولیدی صوتی تصویر ایران", "supplier", "۰۲۱۷۷۸۸۹۹۰۰")
        )
        val banks = listOf(
            Bank("bk-cash", "صندوق فروشگاه", "cash", "", 15_000_000),
            Bank("bk-mlt", "بانک ملت", "bank", "1234567890", 200_000_000),
            Bank("bk-pos", "کارتخوان سامان", "pos")
        )
        val invoices = listOf(
            // inv-1: پرداخت نقدی هنگام صدور ۸م + دریافت بعدی ۱۲م (pay-1) + چک ۸م در جریان (ck-2)
            // ⇒ ماندهٔ درست مشتری: ۳۲٫۶۷م − ۸م − ۱۲م − ۸م = ۴٫۶۷م بدهکار — بدون دوبار‌شماری
            Invoice("inv-1", 101, K_SALE, "pt-c1", w2, monthsAgo(3, 12),
                listOf(InvoiceLine("l1", "it-wash", 1.0, 28_500_000, 500_000.0), InvoiceLine("l2", "it-install", 2.0, 850_000, 0.0)),
                0, 10, 8_000_000, "bk-pos", true, true, "خرید حضوری"),
            Invoice("inv-2", 102, K_SALE, "pt-c2", w2, monthsAgo(2, 8),
                listOf(InvoiceLine("l1", "it-kettle", 3.0, 3_450_000, 150_000.0)),
                0, 10, 9_700_000, "bk-cash", true, true),
            Invoice("inv-3", 103, K_SALE, "pt-c3", w1, monthsAgo(1, 20),
                listOf(InvoiceLine("l1", "it-tv", 4.0, 32_400_000, 400_000.0), InvoiceLine("l2", "it-speaker", 4.0, 9_800_000, 0.0)),
                1_000_000, 10, 0, "", true, true, "فروش به پروژهٔ ساختمانی"),
            Invoice("inv-4", 104, K_SALE, "pt-c4", w2, monthsAgo(0, 3),
                listOf(InvoiceLine("l1", "it-micro", 1.0, 12_700_000, 0.0), InvoiceLine("l2", "it-rice", 2.0, 2_980_000, 100_000.0)),
                0, 10, 10_000_000, "bk-pos", true, true),
            Invoice("inv-5", 201, K_PURCHASE, "pt-s1", w1, monthsAgo(2, 5),
                listOf(InvoiceLine("l1", "it-ref", 5.0, 41_200_000, 0.0), InvoiceLine("l2", "it-wash", 4.0, 25_300_000, 0.0)),
                0, 10, 100_000_000, "bk-mlt", true, false, "بار اول مهرماه"),
            Invoice("inv-6", 202, K_PURCHASE, "pt-s2", w1, monthsAgo(1, 10),
                listOf(InvoiceLine("l1", "it-tv", 6.0, 28_700_000, 0.0), InvoiceLine("l2", "it-speaker", 8.0, 7_600_000, 0.0)),
                0, 10, 0, "", true),
            Invoice("inv-7", 301, K_PROFORMA, "pt-c3", w1, monthsAgo(0, 6),
                listOf(InvoiceLine("l1", "it-stove", 6.0, 18_900_000, 300_000.0)),
                0, 10, 0, "", false, false, "پیش‌فاکتور پروژهٔ برج نگین")
        )
        val payments = listOf(
            Payment("pay-1", 401, "receipt", "cash", "pt-c1", "bk-cash", 12_000_000, monthsAgo(3, 12), "باقیماندهٔ فاکتور ۱۰۱"),
            Payment("pay-2", 402, "receipt", "transfer", "pt-c3", "bk-mlt", 80_000_000, monthsAgo(1, 22), "پیش‌پرداخت پروژه"),
            Payment("pay-3", 403, "payment", "transfer", "pt-s1", "bk-mlt", 60_000_000, monthsAgo(1, 1), "تسویهٔ بخشی از خرید"),
            Payment("pay-4", 404, "payment", "cash", "", "bk-cash", 3_500_000, monthsAgo(0, 4), "هزینهٔ حمل و نقلی")
        )
        val checks = listOf(
            Check("ck-1", 501, "received", "pt-c3", "بانک ملی", "789123", 30_000_000, monthsAgo(1, 20), J.jAddDays(today, 12), "drawer", "چک پروژهٔ سپهر"),
            Check("ck-2", 502, "received", "pt-c1", "بانک صادرات", "456321", 8_000_000, monthsAgo(2, 8), J.jAddDays(today, 3), "drawer"),
            Check("ck-3", 503, "issued", "pt-s2", "بانک ملت", "112233", 45_000_000, monthsAgo(1, 10), J.jAddDays(today, 20), "drawer"),
            Check("ck-4", 504, "received", "pt-c2", "بانک تجارت", "998877", 5_000_000, monthsAgo(3, 1), monthsAgo(2, 1), "cleared", "وصول شد")
        )
        val stockDocs = listOf(
            StockDoc("st-1", 601, "transfer", monthsAgo(0, 10), w1, w3,
                listOf(StockLine("it-kettle", 2.0), StockLine("it-rice", 3.0)), "تجهیز شعبهٔ ۲"),
            StockDoc("st-2", 602, "adjust", monthsAgo(0, 15), w1, "",
                listOf(StockLine("it-vacuum", -1.0)), "مغایرت انبارش")
        )
        val vouchers = listOf(
            Voucher("vc-1", 701, "${p0[0]}/01/01", "سرمایهٔ اول دورهٔ مالی",
                listOf(VoucherLine("1201", "", 120_000_000, 0, "موجودی اول دورهٔ انبار"), VoucherLine("3101", "", 0, 120_000_000, "سرمایه"))),
            Voucher("vc-2", 702, monthsAgo(1, 8), "هزینهٔ اجارهٔ فروشگاه — مهر",
                listOf(VoucherLine("5102", "", 25_000_000, 0, "اجاره"), VoucherLine("1102", "bk-mlt", 0, 25_000_000, "انتقالی بانک ملت")))
        )
        val reminders = listOf(
            Reminder("rm-1", J.jAddDays(today, 3), "سررسید چک ۸ میلیونی آقای رضایی", "چک شمارهٔ ۴۵۶۳۲۱ بانک صادرات", false),
            Reminder("rm-2", J.jAddDays(today, 20), "سررسید چک پرداختی به تامین‌کننده", "چک ۴۵ میلیون بانک ملت", false),
            Reminder("rm-3", today, "پیگیری پیش‌فاکتور برج نگین", "", false)
        )
        val recurring = listOf(
            Recurring("rc-1", "اجارهٔ ماهانهٔ قفسهٔ تبلیغاتی", true, "pt-c3", w1,
                listOf(InvoiceLine("l1", "it-install", 10.0, 850_000, 0.0)), 0, 10, 1, J.jAddDays(today, 5)),
            Recurring("rc-2", "قرارداد سرویس و نگهداری فصلی", true, "pt-c1", w2,
                listOf(InvoiceLine("l1", "it-repair", 4.0, 1_200_000, 0.0)), 0, 10, 3, J.jAddDays(today, 22))
        )
        val projects = listOf(
            Project("pr-1", "تجهیز کافی‌شاپ رستوران تهران", "pt-c3", 950_000, "#0d9488"),
            Project("pr-2", "پروژهٔ برج نگین — واحد نمونه", "pt-c4", 1_100_000, "#7c3aed")
        )
        val timeEntries = listOf(
            TimeEntry("te-1", "pr-1", J.jAddDays(today, -1), "بازدید و برآورد اولیه", 120, 950_000),
            TimeEntry("te-2", "pr-1", today, "نصب و راه‌اندازی یخچال صنعتی", 210, 950_000),
            TimeEntry("te-3", "pr-2", J.jAddDays(today, -2), "مشاورهٔ انتخاب کالا", 90, 1_100_000),
            TimeEntry("te-4", "pr-2", J.jAddDays(today, -6), "حضور در پروژه و آموزش", 240, 1_100_000, "۱۰۵")
        )
        val bankTxns = listOf(
            BankTxn("bt-1", "bk-mlt", today, 15_500_000, "واریز کارت به کاربر — نامشخص"),
            BankTxn("bt-2", "bk-mlt", J.jAddDays(today, -1), -4_200_000, "خرید اینترنتی — درگاه پرداخت"),
            BankTxn("bt-3", "bk-pos", J.jAddDays(today, -1), 10_000_000, "تسویهٔ کارتخوان سامان"),
            BankTxn("bt-4", "bk-mlt", J.jAddDays(today, -3), 2_400_000, "سود سپردهٔ بانکی"),
            BankTxn("bt-5", "bk-mlt", J.jAddDays(today, -4), -25_000_000, "اجارهٔ فروشگاه", BankMatch("expense", "هزینهٔ اجاره — مهر"))
        )
        val bankRules = listOf(
            BankRule("br-1", "اجاره", "payment", "هزینهٔ اجارهٔ ماهانه"),
            BankRule("br-2", "سود سپرده", "receipt", "درآمد سود بانکی")
        )
        return DB(5, settings, groups, items, warehouses, partners, banks, invoices, payments, checks,
            stockDocs, vouchers, reminders, recurring, projects, timeEntries, bankTxns, bankRules, ACCOUNTS, emptyMap())
    }
}
