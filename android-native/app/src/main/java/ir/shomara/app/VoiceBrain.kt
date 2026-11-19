package ir.shomara.app

/**
 * موتور زبان طبیعی فارسی هوشیار — قلب دستیار صوتی
 * ورودی: متن گفتار شناسایی‌شده یا تایپ‌شدهٔ فارسی
 * خروجی: یک دستور دقیق (ناوبری بین ماژول‌ها، ساخت/حذف کالا، صدور فاکتور،
 *         ثبت مشتری، دریافت/پرداخت، جستجو، یادآور، پشتیبان یا پاسخ گزارشی)
 * از روی همان داده‌های واقعی کاربر (Db.db) کار می‌کند — بدون هیچ وابستگی اندروید،
 * پس روی JVM هم قابل تست است (scripts/jvmtest).
 */
object VoiceBrain {

    /* ═══════════ نرمال‌سازی فارسی ═══════════ */

    /** یکدست‌سازی حروف عربی/نیم‌فاصله/علامت‌ها/اِعراب تا تطبیق دقیق شود */
    fun norm(s0: String): String {
        val sb = StringBuilder(s0.length)
        for (ch in s0) {
            val c = when (ch) {
                'ي' -> 'ی'; 'ك' -> 'ک'; 'ة', 'ۀ' -> 'ه'; 'ؤ' -> 'و'
                'أ', 'إ', 'آ' -> 'ا'
                '\u200c' -> ' '
                '،', '؟', '!', ':', ';', '.', '"', '\'', '«', '»', '(', ')', '[', ']', '{', '}',
                '-', '_', '*', '/', '\\', '+', '=' -> ' '
                // اِعراب و کشیده — حذف کامل تا «لطفاً» و «لطفا» یکی شوند
                '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652', '\u0670', '\u0640' -> ' '
                else -> ch
            }
            sb.append(c)
        }
        return sb.toString().replace(Regex("[\\s]+"), " ").trim().lowercase()
    }

    fun toks(s: String): List<String> = s.split(" ").filter { it.isNotEmpty() }

    /** وجود دنبالهٔ دقیق چندکلمه‌ای در توکن‌ها */
    fun seqIn(t: List<String>, vararg words: String): Boolean {
        if (words.isEmpty() || t.size < words.size) return false
        outer@ for (i in 0..t.size - words.size) {
            for (j in words.indices) if (t[i + j] != words[j]) continue@outer
            return true
        }
        return false
    }

    fun tHas(t: List<String>, vararg ws: String): Boolean = ws.any { t.contains(it) }

    /* ═══════════ عددنویسی فارسی ═══════════ */

    val NUM_WORDS = mapOf(
        "صفر" to 0L, "یک" to 1L, "یکی" to 1L, "دو" to 2L, "سه" to 3L, "چهار" to 4L, "چار" to 4L,
        "پنج" to 5L, "شش" to 6L, "شیش" to 6L, "هفت" to 7L, "هشت" to 8L, "نه" to 9L, "ده" to 10L,
        "یازده" to 11L, "دوازده" to 12L, "سیزده" to 13L, "چهارده" to 14L, "پونزده" to 15L,
        "پانزده" to 15L, "شانزده" to 16L, "هفده" to 17L, "هجده" to 18L, "نوزده" to 19L,
        "بیست" to 20L, "سی" to 30L, "چهل" to 40L, "پنجاه" to 50L, "شصت" to 60L,
        "هفتاد" to 70L, "هشتاد" to 80L, "نود" to 90L,
        "صد" to 100L, "یکصد" to 100L, "دویست" to 200L, "سیصد" to 300L, "چهارصد" to 400L,
        "پانصد" to 500L, "ششصد" to 600L, "هفتصد" to 700L, "هشتصد" to 800L, "نهصد" to 900L
    )
    val MULT_WORDS = mapOf("هزار" to 1000L, "میلیون" to 1000000L, "میلیارد" to 1000000000L)

    val UNIT_WORDS = setOf(
        "تا", "عدد", "دانه", "کیلو", "کیلوگرم", "گرم", "لیتر", "بسته", "کارتن", "متر",
        "جین", "حلقه", "شیشه", "نیم", "سری", "جعبه", "پاکت", "قوطی", "شانه"
    )

    private val DIGIT_RE = Regex("[0-9۰-۹٫.,٬]+")

    /**
     * تبدیل دنبالهٔ عدد فارسی (حروف یا رقم) به مقدار — مثل «دو میلیون و پانصد هزار» → 2500000
     * هر جا توکن غیرعددی رسید متوقف می‌شود.
     */
    fun wordsToNum(t: List<String>): Long {
        var acc = 0L; var total = 0L; var seen = false
        for (w in t) {
            if (NUM_WORDS.containsKey(w)) { acc += NUM_WORDS[w]!!; seen = true }
            else if (MULT_WORDS.containsKey(w)) { acc = (if (acc <= 0L) 1L else acc) * MULT_WORDS[w]!!; total += acc; acc = 0L; seen = true }
            else if (w == "و") { if (!seen) break }
            else if (DIGIT_RE.matches(w)) { val d = J.parseNum(w).toLong(); if (d <= 0 && seen) break; acc += d; seen = true }
            else break
        }
        return total + acc
    }

    /** «مبلغ» بعد از کلیدواژهٔ پولی — رقم + هزار/میلیون + تومان */
    fun parsePriceAfter(t: List<String>, keys: Set<String>): Long {
        for (i in t.indices) {
            if (t[i] in keys) {
                val v = wordsToNum(t.drop(i + 1).take(6))
                if (v > 0) return v
            }
        }
        return 0L
    }

    /** هر عبارت پولی «X تومان» در متن */
    fun parseMoneyAny(t: List<String>): Long {
        for (i in t.indices) {
            // «تومانی/تومنش» هم مثل «تومان» — جملهٔ صوتی کاربر صرف می‌شود
            if (t[i] == "تومان" || t[i] == "تومن" || t[i] == "ریال" || t[i] == "تومانی" || t[i] == "تومنش" || t[i] == "تومنت" || t[i] == "تومانش") {
                // به عقب برو تا شروع عبارت عددی را پیدا کنی
                var j = i - 1
                var start = i
                var steps = 0
                while (j >= 0 && steps < 6 && (DIGIT_RE.matches(t[j]) || t[j] in NUM_WORDS || t[j] in MULT_WORDS || t[j] == "و")) {
                    if (t[j] != "و") start = j
                    j--; steps++
                }
                val v = wordsToNum(t.drop(start).take(i - start))
                if (v > 0) return v
            }
        }
        return 0L
    }

    /** تعداد ساده (بدون هزار/میلیون) بعد از کلیدواژه — برای موجودی */
    fun parseQtyAfter(t: List<String>, keys: Set<String>): Double {
        for (i in t.indices) {
            if (t[i] in keys && i + 1 < t.size) {
                val w = t[i + 1]
                if (DIGIT_RE.matches(w)) return J.parseNum(w)
                if (NUM_WORDS.containsKey(w)) return NUM_WORDS[w]!!.toDouble()
            }
        }
        return 0.0
    }

    /* ═══════════ تطبیق فازی ═══════════ */

    fun scoreMatch(name: String, q: String): Int {
        val n = norm(name)
        val query = norm(q)
        if (query.length < 2 || n.isEmpty()) return 0
        if (n == query) return 100
        if (n.contains(query)) return 80
        val nt = toks(n)
        val qt = toks(query).filter { it.length > 1 }
        if (qt.isEmpty()) return 0
        val hit = qt.count { q0 -> nt.any { it == q0 || it.contains(q0) || q0.contains(it) } }
        return if (hit == qt.size) 60 + hit * 3 else if (hit > 0) 25 + hit * 6 else 0
    }

    fun <T> bestMatch(list: List<T>, nameOf: (T) -> String, q: String, minScore: Int = 30): T? =
        list.map { it to scoreMatch(nameOf(it), q) }
            .filter { it.second >= minScore }
            .maxByOrNull { it.second }?.first

    /* ═══════════ جدول ناوبری ماژول‌ها ═══════════ */

    data class RouteDef(val variants: List<List<String>>, val route: String, val label: String, val sub: String = "")

    val ROUTES = listOf(
        RouteDef(listOf(listOf("دفتر", "روزنامه"), listOf("روزنامه")), "ledger", "دفتر روزنامه", "journal"),
        RouteDef(listOf(listOf("دفتر", "کل")), "ledger", "دفتر کل", "general"),
        RouteDef(listOf(listOf("دفتر", "معین")), "ledger", "دفتر معین", "subsidiary"),
        RouteDef(listOf(listOf("حسابداری"), listOf("دفترها"), listOf("دفتر"), listOf("اسناد")), "ledger", "حسابداری"),
        RouteDef(listOf(listOf("سند", "دستی"), listOf("سند", "حسابداری")), "ledger", "سند دستی", "manual"),
        RouteDef(listOf(listOf("سود", "و", "زیان")), "reports", "سود و زیان", "pnl"),
        RouteDef(listOf(listOf("تراز", "ازمایشی"), listOf("تراز", "آزمایشی")), "reports", "تراز آزمایشی", "trial"),
        RouteDef(listOf(listOf("ترازنامه")), "reports", "ترازنامه", "sheet"),
        RouteDef(listOf(listOf("گردش", "کالا"), listOf("گردش", "اجناس")), "reports", "گردش کالا", "items"),
        RouteDef(listOf(listOf("گردش", "طرف", "حساب")), "reports", "گردش طرف حساب", "partners"),
        RouteDef(listOf(listOf("مالیات"), listOf("ارزش", "افزوده"), listOf("مودیان")), "reports", "گزارش مالیات", "vat"),
        RouteDef(listOf(listOf("گزارشها"), listOf("گزارش", "ها"), listOf("گزارشات"), listOf("گزارش")), "reports", "گزارش‌ها"),
        RouteDef(listOf(listOf("گزارش", "انبار")), "reports", "گزارش انبار", "stock"),
        RouteDef(listOf(listOf("انبار"), listOf("موجودی", "انبار"), listOf("استوک"), listOf("انبارها")), "warehouse", "انبار"),
        RouteDef(listOf(listOf("برگشت", "از", "فروش")), "invoices", "برگشت از فروش", "saleReturn"),
        RouteDef(listOf(listOf("برگشت", "از", "خرید")), "invoices", "برگشت از خرید", "purchaseReturn"),
        RouteDef(listOf(listOf("پیش", "فاکتور"), listOf("پیشفاکتور")), "invoices", "پیش‌فاکتور", "proforma"),
        RouteDef(listOf(listOf("فاکتور", "خرید"), listOf("فاکتورهای", "خرید"), listOf("خریدها"), listOf("خرید")), "invoices", "فاکتور خرید", "purchase"),
        RouteDef(listOf(listOf("فاکتور", "فروش"), listOf("فاکتورها"), listOf("فاکتور"), listOf("فروشها"), listOf("فروش")), "invoices", "فروش", "sale"),
        RouteDef(listOf(listOf("کالاها"), listOf("کالا", "ها"), listOf("کالا"), listOf("کالای"), listOf("کالاهای"), listOf("اجناس"), listOf("جنس"), listOf("محصولات")), "items", "کالاها"),
        RouteDef(listOf(listOf("مشتریان"), listOf("مشتریها"), listOf("مشتری", "ها"), listOf("مشتری"), listOf("خریدار")), "partners", "مشتریان", "customer"),
        RouteDef(listOf(listOf("تامین", "کننده"), listOf("تامین", "کننده", "ها"), listOf("تامین", "کنندگان"), listOf("تامینکننده")), "partners", "تامین‌کننده‌ها", "supplier"),
        RouteDef(listOf(listOf("طرف", "حساب"), listOf("طرفحساب")), "partners", "طرف‌حساب‌ها", "all"),
        RouteDef(listOf(listOf("صندوق"), listOf("دریافت", "و", "پرداخت"), listOf("خزانه"), listOf("پولها")), "cash", "دریافت و پرداخت"),
        RouteDef(listOf(listOf("چک", "صادره"), listOf("چک", "پرداختی"), listOf("چکهای", "صادر")), "checks", "چک‌های صادرشده", "issued"),
        RouteDef(listOf(listOf("چک"), listOf("چکها"), listOf("چکهای"), listOf("چک", "ها")), "checks", "چک‌ها", "received"),
        RouteDef(listOf(listOf("تطبیق", "بانک"), listOf("بانک")), "reconcile", "تطبیق بانک"),
        RouteDef(listOf(listOf("پروژهها"), listOf("پروژه", "ها"), listOf("پروژه"), listOf("ساعت", "کاری"), listOf("زمان")), "projects", "پروژه و زمان"),
        RouteDef(listOf(listOf("ابزار"), listOf("ابزارها"), listOf("فروشگاه"), listOf("مشخصات", "فروشگاه"), listOf("اطلاعات", "فروشگاه")), "tools", "ابزارها"),
        RouteDef(listOf(listOf("راهنما")), "guide", "راهنما"),
        RouteDef(listOf(listOf("تنظیمات"), listOf("بیشتر")), "more", "بیشتر"),
        RouteDef(listOf(listOf("دستیار"), listOf("هلو", "فهم")), "assistant", "هوشیار"),
        RouteDef(listOf(listOf("خانه"), listOf("داشبورد"), listOf("صفحه", "اصلی")), "dashboard", "خانه")
    )

    private val NAV_VERBS = setOf("برو", "بریم", "برویم", "میروم", "میریم")
    private val LEAD_NAV_NOISE = setOf("به", "بخش", "ماژول", "صفحه", "قسمت", "سراغ", "روی")

    private fun navByTable(t: List<String>): RouteDef? {
        var best: RouteDef? = null; var bestLen = 0
        for (r in ROUTES) for (v in r.variants) {
            if (seqIn(t, *v.toTypedArray()) && v.size > bestLen) { best = r; bestLen = v.size }
        }
        return best
    }

    private fun navStandalone(t: List<String>): RouteDef? {
        val stripped = t.dropWhile { it in LEAD_NAV_NOISE }
        if (stripped.isEmpty()) return null
        for (r in ROUTES) for (v in r.variants) {
            if (stripped == v || (stripped.size == v.size + 1 && stripped.last() == "رو" && stripped.dropLast(1) == v)) return r
        }
        return null
    }

    /* ═══════════ استخراج اسم ═══════════ */

    private val JUNK = setOf(
        "لطفا", "لطفا", "میخوام", "می", "خواهم", "خوام", "بده", "بده", "کن", "بکن", "کنید", "بزن",
        "جدید", "اضافه", "بساز", "بسازید", "ثبت", "ایجاد", "تعریف", "یک", "یه", "را", "رو",
        "بخوام", "میشه", "ممکن", "است", "هست", "داره", "دارم", "برام", "برای", "از", "با", "به",
        "اسم", "نام", "اسمش", "نامش", "در", "که", "و", "تا", "هم", "خیلی", "خب", "اوکی", "باشه",
        // لحن محاوره‌ای — «برا من یه کالای یخچال» اسمش باید «یخچال» بشه نه «برا من یخچال»
        "برا", "من", "تو", "اون", "کنم", "بکنم", "بگیر", "بگیرم", "میخواستم", "میخوایم"
    )

    private val NAME_STOP = setOf(
        "قیمت", "قیمتش", "تومان", "تومن", "موجودی", "موجودیاش", "تعداد", "خرید", "خریدش", "بهای",
        "تمام", "شده", "کد", "بارکد", "گروه", "واحد", "توضیح", "توضیحات", "با", "به", "شماره",
        "تلفن", "همراه", "موبایل", "واحدش", "استوک", "حداقل", "برای", "از", "تا", "و", "هزار", "میلیون"
    )

    /** تبدیل رقم‌های فارسی/عربی به لاتین + حذف جداکننده — برای کد کالا و بارکد گفتاری */
    fun digitsLatin(s0: String): String {
        val fa = "۰۱۲۳۴۵۶۷۸۹"
        val ar = "٠١٢٣٤٥٦٧٨٩"
        val sb = StringBuilder(s0.length)
        for (ch in s0) {
            val i = fa.indexOf(ch); val j = ar.indexOf(ch)
            when {
                i >= 0 -> sb.append(('0' + i))
                j >= 0 -> sb.append(('0' + j))
                ch == '٫' || ch == ',' || ch == '٬' || ch == '.' || ch == '-' || ch == ' ' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun nameFrom(t: List<String>, skips: Set<String>): String {
        val out = ArrayList<String>()
        for (w in t) {
            if (w in skips || w in JUNK) continue
            if (w in NAME_STOP) break
            out.add(w)
        }
        return out.joinToString(" ").trim()
    }

    /* ═══════════ صورت دستور ═══════════ */

    data class VoiceIntent(
        val kind: String,          // nav | item_new | item_del | partner_new | partner_del | invoice_new | payment_new | search | reminder | backup | help | hello | query | unknown
        val route: String = "",
        val routeLabel: String = "",
        val subView: String = "",
        val itemName: String = "",
        val itemCode: String = "",
        val itemBarcode: String = "",
        val price: Long = 0L,
        val cost: Long = 0L,
        val qty: Double = 1.0,
        val unit: String = "عدد",
        val service: Boolean = false,
        val partnerName: String = "",
        val amount: Long = 0L,
        val payKind: String = "",
        val method: String = "cash",
        val query: String = "",
        val dayOffset: Int = 0,
        val title: String = "",
        val lines: List<InvoiceLine> = emptyList(),
        val missed: String = "",
        val actionLabel: String = "",
        val actionGo: String = "",
        val learnId: String = "",
        val reply: String = ""
    )

    /** پاسخ عمومی وقتی پایهٔ برنامه جمله را نفهمید — دستیار صوتی با همین علامت جمله را «نفهمیده» ثبت می‌کند */
    const val GENERIC_FALLBACK = "می‌توانم دربارهٔ این‌ها گزارش بدهم"

    val HELP_TEXT =
        "هر چه بگویید انجام می‌دهم! نمونه‌های صوتی:\n" +
        "• ناوبری: «برو روزنامه»، «باز کن انبار»، «برو گزارش سود و زیان»\n" +
        "• کالا: «کالای جدید یخچال قیمت پنجاه هزار تومان» یا «کالای جدید شیر کد ۱۰۲ بارکد ۶۲۵۰۱۲ قیمت دو هزار تومان» یا «کالای شیر رو حذف کن»\n" +
        "• فاکتور: «فاکتور فروش دو تا شیر و سه بسته ماست برای علی رضایی»، «فاکتور خرید بزن»\n" +
        "• مشتری: «مشتری جدید سارا شماره ۰۹۱۲۳۴۵۶۷۸۹»\n" +
        "• خزانه: «دریافت پنجاه هزار تومان از علی»، «پرداخت سی هزار تومان به پگاه»\n" +
        "• چک: «ثبت چک پنجاه هزار تومانی از آقای رضایی سررسید فردا»\n" +
        "• ظاهر: «پوسته تیره»، «پوسته روشن»\n" +
        "• جستجو: «سرچ شیر»، «پیدا کن فاکتور صد و بیست»\n" +
        "• یادآور: «یادآور تماس با علی تا فردا»\n" +
        "• گزارش: «فروش این ماه چقدر بود؟»، «سود دوره؟»، «مطالبات؟»، «چک‌های نزدیک سررسید»\n" +
        "• یاد دادن به من: «وقتی گفتم صندوق یعنی برو صندوق» — از این به بعد با لحن خودتان حرف می‌زنم\n" +
        "من جمله‌های تازهٔ شما را حفظ می‌کنم و کم‌کم خودم را با لحن شما تطبیق می‌دهم."

    /* ═══════════ پارسر اصلی ═══════════ */

    /**
     * پارسر با حافظهٔ یادگیری:
     * ۱) جملهٔ آموزشی «وقتی گفتم X یعنی Y»
     * ۲) پارس پایه — اگر فهمید، جمله همراه دستور در حافظه ثبت می‌شود (وقتی اجرا شد)
     * ۳) اگر نفهمید، اول آموخته‌های دقیق و بعد فازی امتحان می‌شود
     */
    fun parseVoice(raw: String): VoiceIntent {
        // ---- آموزش صریح ----
        val teach = VoiceLearn.parseTeach(raw)
        if (teach != null)
            return VoiceIntent("teach", title = teach.first, query = teach.second,
                reply = "دارم یاد می‌گیرم: وقتی بگویید «" + teach.first + "» یعنی «" + teach.second + "»…")

        val t = toks(norm(raw))
        if (t.isEmpty()) return VoiceIntent("unknown", reply = "شنیدم ولی چیزی نگفتید — دوباره بفرمایید.")

        val core = parseCore(t)
        if (core.kind !in setOf("query", "unknown")) return core

        // ---- آموختهٔ دقیق ----
        val ex = VoiceLearn.matchExact(raw)
        if (ex != null) {
            val ie = VoiceLearn.expand(ex)
            return ie.copy(reply = ie.reply.ifEmpty { "انجام شد." } + "\n(طبق آموخته‌هایم)")
        }
        // ---- آموختهٔ فازی ----
        val fz = VoiceLearn.matchFuzzy(raw)
        if (fz != null) {
            val ie = VoiceLearn.expand(fz)
            if (ie.kind in setOf("nav", "search"))
                return ie.copy(reply = ie.reply.ifEmpty { "انجام شد." } + "\n(طبق آموخته‌هایم)")
            return VoiceIntent("learn_confirm", learnId = fz.id,
                reply = "«" + fz.phrase + "» را از شما یاد گرفته‌ام و یعنی «" + fz.norm + "». همان را انجام بدهم؟ تایید کنید.")
        }
        return core
    }

    /** پارس پایه — بدون حافظه؛ ورودی توکن‌های یکدست‌شده */
    private fun parseCore(t: List<String>): VoiceIntent {

        // سلام و احوالپرسی کوتاه
        val greetOnly = t.all { it in setOf("سلام", "درود", "هی", "هلو", "یار", "فهم", "خوبی", "چطوری", "عزیز") }
        if (greetOnly) return VoiceIntent("hello", reply = "سلام! در خدمت شما هستم — بگویید مثلا «برو روزنامه» یا «فاکتور فروش دو تا شیر برای علی».")

        if (tHas(t, "پشتیبان", "بکاپ", "بک آپ")) return VoiceIntent("backup", reply = "پشتیبان‌گیری کامل داده‌ها باز شد — فایل JSON را در جای امن ذخیره کنید.")

        /* ---- پوستهٔ روشن/تیره با صدا ---- */
        if (tHas(t, "پوسته", "تم", "ظاهر", "شکل") && tHas(t, "تیره", "روشن", "شب", "روز", "عوض", "تغییر")) {
            val target = when {
                tHas(t, "تیره", "شب") -> "dark"
                tHas(t, "روشن", "روز") -> "light"
                else -> ""
            }
            return VoiceIntent("theme", title = target,
                reply = when (target) {
                    "dark" -> "پوستهٔ تیره فعال می‌شود…"
                    "light" -> "پوستهٔ روشن فعال می‌شود…"
                    else -> "پوسته را عوض می‌کنم…"
                })
        }

        if (t.all { it in setOf("ممنون", "ممنونم", "متشکرم", "مرسی", "تشکر", "عالی", "خوبه", "دستت", "درد", "نکنه", "خدا", "فدات") })
            return VoiceIntent("hello", reply = "خواهش می‌کنم! کاری بود در خدمتتم — بگویید مثلا «برو فروش» یا «سود این ماه؟»")

        if ((tHas(t, "چیکار", "چی", "چه") && tHas(t, "بلدی", "میتونی", "میتوانی", "بکنی", "میکنی", "کارهایی", "کارهایی", "دستور")) ||
            tHas(t, "راهنما", "کمک", "کمکم", "دستورها", "نمونه"))
            return VoiceIntent("help", reply = HELP_TEXT)

        /* ---- جستجو ---- */
        if (tHas(t, "سرچ", "جستجو", "جستجوکن", "بگرد", "پیدا")) {
            val idx = t.indexOfFirst { it in setOf("سرچ", "جستجو", "جستجوکن", "بگرد", "پیدا") }
            val q = t.drop(idx + 1).dropWhile { it in setOf("کن", "کنید", "دنبال", "رو", "را", "برای", "میکنم", "میکن") }.joinToString(" ")
            return VoiceIntent("search", query = q, reply = if (q.isEmpty()) "جستجوی همه‌جا باز شد" else "نتایج جستجوی «" + q + "» را می‌آورم…")
        }

        /* ---- حذف ---- */
        val delVerb = tHas(t, "حذف", "پاک", "بنداز", "پاکش")
        if (delVerb) {
            if (tHas(t, "کالا", "کالای", "جنس", "اجناس", "خدمت", "محصول")) {
                val nm = nameFrom(t.filterIndexed { i, w -> !(i == 0 && w in setOf("کالا", "کالای", "جنس", "اجناس", "خدمت", "محصول")) }, setOf("حذف", "پاک", "بنداز", "کالا", "کالای", "جنس", "اجناس", "خدمت", "محصول", "رو", "را"))
                return VoiceIntent("item_del", itemName = nm, reply = if (nm.isEmpty()) "اسم کالا را نگفتید — مثلا بگویید «حذف کالای شیر»" else "بگذار «" + nm + "» را از کالاها پیدا کنم…")
            }
            if (tHas(t, "مشتری", "خریدار", "تامین", "کننده", "طرف")) {
                val nm = nameFrom(t, setOf("حذف", "پاک", "بنداز", "مشتری", "خریدار", "تامین", "کننده", "طرف", "حساب", "رو", "را"))
                return VoiceIntent("partner_del", partnerName = nm, reply = if (nm.isEmpty()) "اسم طرف‌حساب را نگفتید" else "بگذار «" + nm + "» را در طرف‌حساب‌ها پیدا کنم…")
            }
        }

        /* ---- ساخت کالا / خدمت ---- */
        if (!tHas(t, "فاکتور") && tHas(t, "کالا", "کالای", "کالاهای", "جنس", "اجناس", "خدمت", "محصول", "خدمت جدید") &&
            tHas(t, "جدید", "بساز", "اضافه", "ثبت", "ایجاد", "تعریف", "کن", "بده")) {
            val service = tHas(t, "خدمت")
            // ⚡ کد و بارکد گفتاری — «کالا جدید شیر کد ۱۰۲ بارکد ۶۲۵۰۱۲ قیمت پنجاه هزار تومان»
            // قطعهٔ «کد X» و «بارکد X» از جمله حذف می‌شود تا اسم کالا را نخورد
            var skipNum = false
            val cleanT = ArrayList<String>()
            for (w in t) {
                if (skipNum) {
                    // کل دنبالهٔ عددی بعد از «کد/بارکد» حذف می‌شود — «صد و پنج» و «۱۰۲»
                    if (w in NUM_WORDS || w in MULT_WORDS || w == "و" || DIGIT_RE.matches(w)) continue
                    skipNum = false
                }
                if (w in setOf("کد", "کدش", "کدت", "بارکد", "بارکدش")) { skipNum = true; continue }
                cleanT.add(w)
            }
            fun codeAfter(keys: Set<String>): String {
                for (i in t.indices) {
                    if (t[i] in keys && i + 1 < t.size) {
                        val w = t[i + 1]
                        if (DIGIT_RE.matches(w)) return digitsLatin(w)
                        val n = wordsToNum(t.drop(i + 1).take(4))
                        if (n > 0) return n.toString()
                    }
                }
                return ""
            }
            val vCode = codeAfter(setOf("کد", "کدش", "کدت"))
            val vBar = codeAfter(setOf("بارکد", "بارکدش"))
            val skips = setOf("کالا", "کالای", "کالاهای", "جنس", "اجناس", "خدمت", "محصول", "رو", "را")
            val nm = nameFrom(cleanT, skips)
            val price = Math.max(parsePriceAfter(t, setOf("قیمت", "قیمتش", "فروش")), parseMoneyAny(t))
            val cost = parsePriceAfter(t, setOf("خرید", "خریدش", "بهای", "تمام"))
            var qty = parseQtyAfter(t, setOf("موجودی", "تعداد", "استوک"))
            if (qty <= 0 && (seqIn(t, "موجودی", "دارد") || t.lastOrNull() == "موجودی")) qty = 0.0
            val unit = UNIT_WORDS.firstOrNull { t.contains(it) && it != "تا" && it != "عدد" && it != "نیم" } ?: "عدد"
            return VoiceIntent(
                "item_new", itemName = nm, itemCode = vCode, itemBarcode = vBar, price = price, cost = cost,
                qty = if (qty > 0) qty else 0.0, unit = unit, service = service,
                reply = if (nm.isNotEmpty() && price > 0)
                    "بگذار کالای «" + nm + "» را با قیمت " + J.moneyFa(price) + " تومان ثبت کنم…"
                else "فرم کالای جدید را برایت باز می‌کنم" + (if (nm.isNotEmpty()) " با اسم «" + nm + "»" else "") + " — بقیه را کامل کن."
            )
        }

        /* ---- ساخت مشتری / تامین‌کننده ---- */
        if (!tHas(t, "فاکتور") && !tHas(t, "دریافت") && !tHas(t, "پرداخت") &&
            (seqIn(t, "طرف", "حساب") || tHas(t, "مشتری", "مشتریها", "خریدار", "تامین", "تامینکننده", "تامینکنندهها")) &&
            tHas(t, "جدید", "اضافه", "بساز", "ثبت", "ایجاد", "تعریف", "کن", "بده")) {
            val supplier = tHas(t, "تامین", "تامینکننده")
            val nm = nameFrom(t, setOf("مشتری", "مشتریها", "خریدار", "تامین", "کننده", "تامینکننده", "طرف", "حساب", "رو", "را", "جدید"))
            val phIdx = t.indexOfFirst { it in setOf("شماره", "تلفن", "همراه", "موبایل") }
            val phone = if (phIdx >= 0 && phIdx + 1 < t.size && DIGIT_RE.matches(t[phIdx + 1])) t[phIdx + 1].filter { it.isDigit() || it in '۰'..'۹' } else ""
            return VoiceIntent(
                "partner_new", partnerName = nm, service = supplier,
                reply = if (nm.isNotEmpty()) "ثبت " + (if (supplier) "تامین‌کننده" else "مشتری") + " «" + nm + "»…" else "فرم طرف‌حساب جدید باز شد — اسم و مشخصات را کامل کنید."
            )
        }

        /* ---- فاکتور ---- */
        if (tHas(t, "فاکتور", "فاکتوری") && !tHas(t, "برو", "بریم", "برویم", "میروم")) {
            val kind = when {
                seqIn(t, "برگشت", "از", "فروش") -> K_SALE_RET
                seqIn(t, "برگشت", "از", "خرید") -> K_PURCHASE_RET
                seqIn(t, "پیش", "فاکتور") || tHas(t, "پیشفاکتور") -> K_PROFORMA
                tHas(t, "خرید") -> K_PURCHASE
                else -> K_SALE
            }
            // مشتری: بعد از «برای» یا «از» — بلندترین پیشوندی که با طرف‌حساب‌های واقعی تطبیق دارد
            val forKey = listOf("برای", "از").firstOrNull { t.contains(it) }
            val partnerName = if (forKey != null) partnerPhraseFrom(t, forKey) else ""
            // اقلام: بعد از «فاکتور» و قبل از کلیدواژهٔ طرف‌حساب
            val fx = t.indexOfFirst { it.startsWith("فاکتور") }.coerceAtLeast(0)
            val kindWords = setOf("فاکتور", "فاکتوری", "فروش", "خرید", "برگشت", "از", "پیش", "پیشفاکتور", "بزن", "بزنیم", "صادر", "صادرش", "ثبت", "کن", "کنم", "کنید", "جدید", "میخوام", "لطفا")
            val cutIdx = forKey?.let { t.indexOf(it) } ?: t.size
            val front = ArrayList<String>()
            var i0 = fx + 1
            while (i0 < t.size && i0 < cutIdx) { val w = t[i0]; if (w !in kindWords) front.add(w); i0++ }
            var parsed = parseInvoiceSegments(front, Db.db)
            // اقلام بعد از اسم طرف‌حساب هم ممکن است بیایند: «فاکتور خرید از پگاه دو تا یخچال»
            if (parsed.first.isEmpty() && forKey != null && partnerName.isNotEmpty()) {
                val after = t.drop(t.indexOf(forKey) + 1 + toks(norm(partnerName)).size)
                val back = after.filter { it !in kindWords }
                if (back.isNotEmpty()) {
                    val p2 = parseInvoiceSegments(back, Db.db)
                    if (p2.first.isNotEmpty()) parsed = Triple(p2.first, parsed.second, p2.third)
                }
            }
            val pName = if (parsed.second.isNotEmpty()) parsed.second else partnerName
            val label = LB.invoiceKindLabel(kind)
            return when {
                parsed.first.isNotEmpty() ->
                    VoiceIntent("invoice_new", route = "invoices", subView = kind, partnerName = pName, lines = parsed.first,
                        missed = parsed.third,
                        reply = "صدور " + label + "…")
                else ->
                    VoiceIntent("invoice_new", route = "invoices", subView = kind, partnerName = partnerName,
                        reply = "فرم " + label + " را باز می‌کنم" + (if (partnerName.isNotEmpty()) " برای «" + partnerName + "»" else "") + " — اقلام را اضافه کنید.")
            }
        }

        /* ---- ثبت چک صوتی — «ثبت چک پنجاه هزار تومانی از آقای رضایی سررسید فردا» ---- */
        val chkAmt = Math.max(parseMoneyAny(t), parsePriceAfter(t, setOf("مبلغ", "مبلغش")))
        if (tHas(t, "چک") && !tHas(t, "برو", "بریم", "برویم", "میروم", "میریم", "سرچ", "جستجو", "پیدا", "حذف", "باز", "چقدر", "چند") &&
            !seqIn(t, "نزدیک", "سررسید") && (chkAmt > 0 || tHas(t, "ثبت", "بزن", "بساز", "جدید", "بگیر", "بده", "کن"))) {
            val kind = when {
                tHas(t, "پرداختی", "صادره", "صادرم", "صادر", "دادم", "دادیم", "خریدم") -> "issued"
                tHas(t, "دریافتی", "گرفتم", "گرفتیم", "وصول") -> "received"
                t.contains("به") && !t.contains("از") -> "issued"
                else -> "received"
            }
            val off = when {
                seqIn(t, "پس", "فردا") -> 2
                tHas(t, "فردا") -> 1
                seqIn(t, "هفته", "بعد") || seqIn(t, "هفته", "دیگه") -> 7
                seqIn(t, "ماه", "بعد") || seqIn(t, "ماه", "دیگه") -> 30
                else -> 0
            }
            val pKey = if (kind == "issued") "به" else "از"
            val pname = if (t.contains(pKey)) partnerPhraseFrom(t, pKey) else ""
            return VoiceIntent("check_new", payKind = kind, amount = chkAmt, partnerName = pname, dayOffset = off,
                reply = if (chkAmt > 0)
                    (if (kind == "issued") "ثبت چک پرداختی " else "ثبت چک دریافتی ") + J.moneyFa(chkAmt) + " تومان" +
                    (if (pname.isNotEmpty()) " برای «" + pname + "»" else "") + "…"
                else
                    "فرم " + (if (kind == "issued") "چک پرداختی" else "چک دریافتی") + " را باز می‌کنم — مبلغ و بانک را کامل کنید.")
        }

        /* ---- دریافت / پرداخت ---- */
        val recvVerb = tHas(t, "دریافت", "گرفتیم", "گرفتم", "وصول")
        val payVerb = tHas(t, "پرداخت", "دادیم", "دادم", "پرداختش")
        if ((recvVerb || payVerb) && !tHas(t, "برو", "بریم", "برویم")) {
            val amt = Math.max(parseMoneyAny(t), parsePriceAfter(t, setOf("مبلغ", "مبلغش")))
            if (tHas(t, "چک")) return VoiceIntent("nav", route = "checks", routeLabel = "چک‌ها", subView = if (recvVerb) "received" else "issued",
                reply = "چک‌ها را باز کردم — ثبت چک از دکمهٔ پایین.")
            if (amt <= 0) return VoiceIntent("nav", route = "cash", routeLabel = "دریافت و پرداخت", subView = if (recvVerb) "receipt" else "payment",
                reply = "مبلغی نشنیدم — بخش دریافت و پرداخت را باز کردم تا دستی ثبت کنید.")
            val pk = if (recvVerb) "receipt" else "payment"
            val method = when {
                tHas(t, "کارت", "کارتخوان", "پوز", "کارتخوانش") -> "card"
                tHas(t, "حواله", "انتقال", "انتقالی", "کارتی") -> "transfer"
                else -> "cash"
            }
            val pKey = if (recvVerb) "از" else "به"
            val pname = if (t.contains(pKey)) partnerPhraseFrom(t, pKey) else ""
            return VoiceIntent("payment_new", payKind = pk, amount = amt, method = method, partnerName = pname,
                reply = (if (recvVerb) "ثبت دریافت " else "ثبت پرداخت ") + J.moneyFa(amt) + " تومان" + (if (pname.isNotEmpty()) " برای «" + pname + "»" else "") + "…")
        }

        /* ---- یادآور ---- */
        if (tHas(t, "یاداور", "یاداوری", "یادم", "یادداشت", "یاداوری کن", "یاداور کن") || seqIn(t, "یادم", "بنداز") || seqIn(t, "یادم", "بیا")) {
            val off = when {
                seqIn(t, "پس", "فردا") -> 2
                tHas(t, "فردا") -> 1
                seqIn(t, "هفته", "بعد") || seqIn(t, "هفته", "دیگه") -> 7
                else -> 0
            }
            val title = nameFrom(t.filter { it !in setOf("امروز", "فردا", "پس", "بعد", "دیگه", "هفته") }, setOf("یاداور", "یاداوری", "یادم", "یادداشت", "بنداز", "کن", "بگذار", "بذار", "تا", "رو", "را"))
            return VoiceIntent("reminder", dayOffset = off, title = title,
                reply = if (title.isEmpty()) "موضوع یادآور را نگفتید — مثلا «یادآور تماس با علی تا فردا»"
                else "یادآور «" + title + "» برای " + (if (off == 0) "امروز" else if (off == 1) "فردا" else if (off == 2) "پس‌فردا" else "هفتهٔ بعد") + " ثبت می‌شود…")
        }

        /* ---- ناوبری صریح یا اسم سادهٔ ماژول ---- */
        if (tHas(t, "برگرد", "عقب") && t.size <= 3 && !tHas(t, "از"))
            return VoiceIntent("nav", route = "dashboard", routeLabel = "خانه", reply = "برگشتم به خانه.")
        val explicitNav = tHas(t, "برو", "بریم", "برویم", "میروم", "میریم", "ببر", "وارد") || seqIn(t, "باز", "کن") || (t.firstOrNull() == "باز")
        if (explicitNav) {
            val r = navByTable(t)
            if (r != null) return VoiceIntent("nav", route = r.route, routeLabel = r.label, subView = r.sub, reply = "رفتم به «" + r.label + "».")
            // «برو» بدون مقصد مشخص
            if (tHas(t, "برو", "بریم", "برویم", "میروم", "میریم", "ببر", "وارد") || seqIn(t, "باز", "کن"))
                return VoiceIntent("help", reply = "مقصد را دقیق‌تر بگویید؛ می‌توانم خانه، فروش، خرید، کالاها، اشخاص، صندوق، چک‌ها، انبار، دفترها، گزارش‌ها، پروژه‌ها، تطبیق بانک، ابزارها، راهنما، تنظیمات یا دستیار را باز کنم.")
        }
        val st = navStandalone(t)
        if (st != null) return VoiceIntent("nav", route = st.route, routeLabel = st.label, subView = st.sub, reply = "رفتم به «" + st.label + "».")

        /* ---- بقیه: پرسش گزارشی از داده‌های واقعی ---- */
        val ans = assistantAnswer(t.joinToString(" "))
        return VoiceIntent("query", query = t.joinToString(" "), actionLabel = ans.actionLabel, actionGo = ans.actionGo,
            reply = ans.text)
    }

    private fun partnerPhraseFrom(t: List<String>, key: String): String {
        val idx = t.indexOf(key)
        if (idx < 0 || idx + 1 >= t.size) return ""
        val rest = t.drop(idx + 1).takeWhile { it !in setOf("تا", "با", "قیمت", "تومان", "تومن", "امروز", "فردا", "الان", "لطفا", "مبلغ") && it !in UNIT_WORDS }
        val clean = rest.filter { !(DIGIT_RE.matches(it) || it in NUM_WORDS || it in MULT_WORDS) }
        for (k in Math.min(3, clean.size) downTo 1) {
            val cand = clean.take(k).joinToString(" ")
            if (bestMatch(Db.db.partners, { it.name }, cand, 60) != null) return cand
        }
        return clean.take(3).joinToString(" ").trim()
    }

    /** تقسیم بخش اقلام روی «و» و تطبیق با کالاهای واقعی کاربر */
    fun parseInvoiceSegments(seg: List<String>, db: DB): Triple<List<InvoiceLine>, String, String> {
        val lines = ArrayList<InvoiceLine>()
        val groups = ArrayList<List<String>>()
        var cur = ArrayList<String>()
        for (w in seg) {
            if (w == "و" && cur.isNotEmpty()) { groups.add(cur); cur = ArrayList() } else cur.add(w)
        }
        if (cur.isNotEmpty()) groups.add(cur)
        var partner = ""
        val missed = ArrayList<String>()
        for (g in groups) {
            // «برای علی» داخل گروه اقلام هم ممکن است بیاید
            val bi = g.indexOf("برای")
            if (bi >= 0 && bi + 1 < g.size) {
                partner = g.drop(bi + 1).take(2).joinToString(" ")
                val gg = g.subList(0, bi).toMutableList()
                addLine(gg, db, lines, missed)
                continue
            }
            addLine(g, db, lines, missed)
        }
        return Triple(lines, partner, missed.joinToString("، "))
    }

    private fun addLine(g: List<String>, db: DB, lines: MutableList<InvoiceLine>, missed: MutableList<String>) {
        if (g.isEmpty()) return
        var qi = 0
        var qty = 1.0
        if (DIGIT_RE.matches(g[0])) {
            qty = Math.max(J.parseNum(g[0]), 0.5)
            qi = 1
            if (qi < g.size && g[qi] in UNIT_WORDS) qi++
        } else if (NUM_WORDS.containsKey(g[0]) || g[0] == "نیم") {
            qty = if (g[0] == "نیم") 0.5 else NUM_WORDS[g[0]]!!.toDouble()
            qi = 1
            if (qi < g.size && g[qi] in UNIT_WORDS) qi++
        }
        // قیمت اختیاری داخل گروه: «به قیمت X تومان»
        var price = 0L
        val ki = g.indexOfFirst { it == "قیمت" || it == "قیمتش" }
        if (ki > qi) {
            price = wordsToNum(g.drop(ki + 1).take(4))
            if (price == 0L) price = parseMoneyAny(g)
        }
        val nameToks = g.subList(qi, if (ki > qi) ki else g.size).filter {
            it !in UNIT_WORDS && it !in setOf("به", "تومان", "تومن", "با", "برای", "برا", "من", "فروش", "خرید", "برگشت", "از", "سند", "صادر", "صادرش", "بزن", "بزنیم", "ثبت", "کن", "کنید", "کنم", "جدید", "فاکتور", "میخوام")
        }
        val name = nameToks.joinToString(" ").trim()
        if (name.isEmpty()) return
        val item = bestMatch(db.items, { it.name }, name, 30) ?: if (name.length >= 2) db.items.firstOrNull { it.name.contains(name) } else null
        if (item == null) { missed.add(name); return }
        lines.add(InvoiceLine(Db.newId("l"), item.id, qty, if (price > 0) price else item.price, 0.0))
    }
}
