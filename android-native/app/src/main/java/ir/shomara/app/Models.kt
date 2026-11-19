package ir.shomara.app

/**
 * مدل دادهٔ شمارا — مو‌به‌مو مطابق نسخهٔ وب (store.ts)
 * همهٔ تاریخ‌ها به‌صورت رشتهٔ جلالی «1405/08/28» ذخیره می‌شوند.
 */

data class ItemGroup(val id: String, val name: String)

data class Item(
    val id: String,
    val code: String,
    val name: String,
    val groupId: String,
    val unit: String,
    val price: Long,          // قیمت فروش
    val cost: Long,           // بهای خرید / تمام‌شده
    val service: Boolean = false, // کالا یا خدمت
    val minStock: Double = 0.0,
    val barcode: String = "",
    val desc: String = ""
)

data class Warehouse(val id: String, val name: String, val loc: String = "")

data class Partner(
    val id: String,
    val code: String,
    val name: String,
    val type: String,         // customer | supplier | both
    val phone: String = "",
    val address: String = "",
    val nationalId: String = "",
    val desc: String = "",
    val email: String = "",
    val postalCode: String = "",
    val economicCode: String = "",
    val creditLimit: Long = 0,
    val paymentTermDays: Long = 0
)

data class Bank(
    val id: String,
    val name: String,
    val kind: String,         // cash | bank | pos
    val accountNo: String = "",
    val opening: Long = 0
)

data class InvoiceLine(val id: String, val itemId: String, val qty: Double, val price: Long, val discount: Double)

const val K_SALE = "sale"
const val K_PURCHASE = "purchase"
const val K_PROFORMA = "proforma"
const val K_SALE_RET = "saleReturn"
const val K_PURCHASE_RET = "purchaseReturn"

data class Invoice(
    val id: String,
    val no: Long,
    val kind: String,         // InvoiceKind
    val partnerId: String,
    val warehouseId: String,
    val date: String,
    val lines: List<InvoiceLine>,
    val discount: Long = 0,   // تخفیف کلی
    val vatRate: Long = 10,
    val paid: Long = 0,
    val payBankId: String = "",
    val final: Boolean = false,
    val moudian: Boolean = false,
    val desc: String = "",
    val supplierInvoiceNo: String = ""
)

data class Payment(
    val id: String,
    val no: Long,
    val kind: String,         // receipt | payment
    val method: String,       // cash | card | transfer | check
    val partnerId: String = "",
    val bankId: String,
    val amount: Long,
    val date: String,
    val desc: String = ""
)

data class Check(
    val id: String,
    val no: Long,
    val kind: String,         // received | issued
    val partnerId: String,
    val bankName: String,
    val serial: String,
    val amount: Long,
    val date: String,
    val dueDate: String,
    val status: String,       // drawer | inBank | cleared | bounced | spent
    val desc: String = ""
)

data class Account(val code: String, val name: String, val kind: String, val grp: String)

data class VoucherLine(
    val accountCode: String,
    val detailId: String = "",
    val debit: Long = 0,
    val credit: Long = 0,
    val note: String = ""
)

data class Voucher(
    val id: String,
    val no: Long,
    val date: String,
    val desc: String,
    val lines: List<VoucherLine>,
    val source: String = ""   // شناسهٔ سند سیستمی
)

data class StockLine(val itemId: String, val qty: Double)

data class StockDoc(
    val id: String,
    val no: Long,
    val kind: String,         // adjust | transfer
    val date: String,
    val warehouseId: String,
    val toWarehouseId: String = "",
    val lines: List<StockLine>,
    val desc: String = ""
)

data class Reminder(val id: String, val date: String, val title: String, val desc: String = "", val done: Boolean = false)

data class Recurring(
    val id: String,
    val title: String,
    val active: Boolean,
    val partnerId: String,
    val warehouseId: String,
    val lines: List<InvoiceLine>,
    val discount: Long = 0,
    val vatRate: Long = 10,
    val everyMonths: Long = 1,
    val nextDate: String,
    val lastGenerated: String = ""
)

data class Project(
    val id: String,
    val name: String,
    val partnerId: String = "",
    val rate: Long = 0,
    val color: String,
    val done: Boolean = false
)

data class TimeEntry(
    val id: String,
    val projectId: String,
    val date: String,
    val desc: String,
    val minutes: Long,
    val rate: Long,
    val billed: String = ""
)

data class BankMatch(val kind: String, val label: String) // receipt | payment | expense | skip

data class BankTxn(
    val id: String,
    val bankId: String,
    val date: String,
    val amount: Long,
    val desc: String,
    val matched: BankMatch? = null
)

data class BankRule(val id: String, val keyword: String, val target: String, val note: String) // receipt | payment

data class Settings(
    val shopName: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val vatRate: Long,
    val fiscalYear: String,
    val moudian: Boolean
)

data class DB(
    val version: Long,
    val settings: Settings,
    val groups: List<ItemGroup>,
    val items: List<Item>,
    val warehouses: List<Warehouse>,
    val partners: List<Partner>,
    val banks: List<Bank>,
    val invoices: List<Invoice>,
    val payments: List<Payment>,
    val checks: List<Check>,
    val stockDocs: List<StockDoc>,
    val vouchers: List<Voucher>, // فقط اسناد دستی
    val reminders: List<Reminder>,
    val recurring: List<Recurring>,
    val projects: List<Project>,
    val timeEntries: List<TimeEntry>,
    val bankTxns: List<BankTxn>,
    val bankRules: List<BankRule>,
    val accounts: List<Account>,
    val counters: Map<String, Long>
)

data class AccRow(val code: String, val name: String, val kind: String, val grp: String, val debit: Long, val credit: Long, val balance: Long)

data class PL(
    val income: Long, val returns: Long, val expenses: Long,
    val cogs: Long, val net: Long, val rows: List<AccRow>
)

data class BSDat(
    val assets: List<AccRow>, val liab: List<AccRow>, val equity: List<AccRow>,
    val assetsSum: Long, val liabSum: Long, val equitySum: Long, val pl: PL
)

data class SmartAlert(
    val id: String,
    val tone: String,   // green | amber | rose | sky | violet
    val icon: String,   // alert | clock | box | bank | time | repeat | trend | spark
    val title: String,
    val desc: String,
    val go: String
)

data class MonthVal(val month: Int, val total: Long)
data class MonthIE(val month: Int, val income: Long, val expense: Long)
data class CFRow(val month: Int, val income: Long, val expense: Long)
data class ByItemRow(val item: Item, val qty: Double, val total: Long)
data class ByPartnerRow(val partner: Partner, val total: Long, val count: Int)
data class OverdueRow(val inv: Invoice, val partner: Partner, val amount: Long, val days: Int)
data class AgingRow(val label: String, val amount: Long, val tone: String)
data class StockMove(val qty: Double, val warehouseId: String, val kind: String, val date: String, val ref: String)
data class PartnerBal(val ar: Long, val ap: Long, val net: Long)
data class InvoiceTot(val gross: Long, val lineDisc: Double, val net: Long, val vat: Long, val total: Long, val due: Long)
data class ProjHours(val entries: List<TimeEntry>, val totalMin: Long, val unbilled: List<TimeEntry>, val unbilledMin: Long, val unbilledValue: Double)
data class SearchHit(val kind: String, val title: String, val sub: String, val go: String)

/** برچسب‌های فارسی */
object LB {
    fun invoiceKindLabel(k: String) = when (k) {
        K_SALE -> "فاکتور فروش"; K_PURCHASE -> "فاکتور خرید"; K_PROFORMA -> "پیش‌فاکتور"
        K_SALE_RET -> "برگشت از فروش"; K_PURCHASE_RET -> "برگشت از خرید"; else -> "فاکتور"
    }
    fun invoiceKindShort(k: String) = when (k) {
        K_SALE -> "فروش"; K_PURCHASE -> "خرید"; K_PROFORMA -> "پیش‌فاکتور"
        K_SALE_RET -> "برگشت فروش"; K_PURCHASE_RET -> "برگشت خرید"; else -> "فاکتور"
    }
    fun payMethodLabel(m: String) = when (m) {
        "cash" -> "نقدی"; "card" -> "کارتخوان"; "transfer" -> "حواله/انتقالی"; else -> "چک"
    }
    fun bankKindLabel(k: String) = when (k) {
        "cash" -> "صندوق"; "pos" -> "کارتخوان"; else -> "بانک"
    }
    fun checkStatusLabel(s: String) = when (s) {
        "drawer" -> "در صندوق"; "inBank" -> "سپرده در بانک"; "cleared" -> "وصول/پاس شده"
        "bounced" -> "برگشتی"; else -> "خرج شده"
    }
    fun partnerTypeLabel(t: String) = when (t) {
        "customer" -> "مشتری"; "supplier" -> "تامین‌کننده"; else -> "مشتری/تامین‌کننده"
    }
    fun accKindOf(code: String): String = ACCOUNTS.firstOrNull { it.code == code }?.kind ?: "asset"
}

/** سرفصل‌های حساب — دقیقاً مثل نسخهٔ وب */
val ACCOUNTS = listOf(
    Account("1101", "صندوق", "asset", "دارایی جاری"),
    Account("1102", "بانک و کارتخوان", "asset", "دارایی جاری"),
    Account("1103", "حساب‌های دریافتنی (مطالبات)", "asset", "دارایی جاری"),
    Account("1104", "چک‌های دریافتی در جریان وصول", "asset", "دارایی جاری"),
    Account("1105", "مالیات خرید (اعتبار مالیاتی)", "asset", "دارایی جاری"),
    Account("1201", "موجودی کالا", "asset", "دارایی جاری"),
    Account("2101", "حساب‌های پرداختنی (بدهی)", "liability", "بدهی جاری"),
    Account("2102", "چک‌های پرداختی در جریان", "liability", "بدهی جاری"),
    Account("2103", "مالیات بر ارزش افزوده فروش", "liability", "بدهی جاری"),
    Account("3101", "سرمایه اول دوره", "equity", "حقوق صاحبان سرمایه"),
    Account("4101", "درآمد فروش", "income", "درآمدها"),
    Account("4102", "سایر درآمدها", "income", "درآمدها"),
    Account("5101", "بهای تمام‌شده کالای فروش‌رفته", "expense", "بهای فروش"),
    Account("5102", "هزینه‌های عملیاتی", "expense", "هزینه‌ها"),
    Account("5103", "برگشت از فروش", "expense", "بهای فروش")
)

fun accName(code: String) = ACCOUNTS.firstOrNull { it.code == code }?.name ?: code
