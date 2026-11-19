package ir.shomara.app

import java.io.File

/**
 * حافظهٔ یادگیری هوشیار — قلب «تطبیق با لحن کاربر»
 * ─────────────────────────────────────────────────────────
 * هر دستور صوتی/متنی که با موفقیت اجرا شد، همراه با جملهٔ اصلی کاربر
 * اینجا ذخیره می‌شود. از این به بعد:
 *   • همان جملهٔ دقیق  → بلافاصله همان دستور اجرا می‌شود (حتی اگر پایهٔ برنامه آن را نشناسد)
 *   • جملهٔ مشابه       → برای ناوبری/جستجو خودکار اجرا می‌شود؛ برای کارهای حساس
 *                         (ساخت/حذف/صدور) با یک تأیید امن اجرا می‌شود
 *   • جملهٔ نفهمیده‌شده → ثبت می‌شود تا کاربر بتواند با «وقتی گفتم … یعنی …» به دستیار یاد بدهد
 * ذخیره‌سازی: فایل متنی ساده در حافظهٔ خود اپ (بدون هیچ وابستگی — روی JVM هم تست می‌شود)
 */
object VoiceLearn {

    /* ═══════════ مدل ═══════════ */

    /** یک آموخته: جملهٔ کاربر + دستور کامل پارس‌شدهٔ معادل */
    data class Entry(
        val id: String,
        val phrase: String,              // جملهٔ اصلی کاربر (برای نمایش)
        val norm: String,                // شکل یکدست‌شده برای تطبیق
        val kind: String,
        val route: String,
        val subView: String,
        val itemName: String,
        val partnerName: String,
        val query: String,
        val title: String,
        val payKind: String,
        val method: String,
        val unit: String,
        val price: Long,
        val cost: Long,
        val amount: Long,
        val qty: Double,
        val dayOffset: Int,
        val service: Boolean,
        val hits: Int,
        val itemCode: String = "",               // کد کالای گفتاری — نسخهٔ جدید
        val lines: List<Triple<String, Double, Long>> = emptyList() // (اسم کالا، تعداد، قیمت)
    )

    private val entries = ArrayList<Entry>()
    private val unknowns = LinkedHashMap<String, Int>() // جمله → تعداد تکرار

    private var file: File? = null
    private var ufile: File? = null

    /** مقداردهی با مسیر حافظه — از MainActivity.onCreate (فایل‌ها داخل filesDir) */
    fun init(dir: File) {
        file = File(dir, "voice_learned.txt")
        ufile = File(dir, "voice_unknown.txt")
        load()
    }

    fun all(): List<Entry> = entries.toList()
    fun unknownList(): List<Pair<String, Int>> = unknowns.entries.map { it.key to it.value }.sortedByDescending { it.second }
    fun byId(id: String): Entry? = entries.firstOrNull { it.id == id }
    fun count(): Int = entries.size

    /* ═══════════ ثبت و یادگیری ═══════════ */

    /** ثبت خودکار هر دستور موفق — جملهٔ کاربر به دستور وصل می‌شود */
    fun record(raw: String, intent: VoiceBrain.VoiceIntent) {
        if (intent.kind !in setOf("nav", "item_new", "partner_new", "invoice_new", "payment_new", "reminder")) return
        val n = VoiceBrain.norm(raw)
        if (n.length < 3 || n.split(" ").size < 2) return // «فقط برو» و مشابه‌ها را حفظ نکن
        val db = Db.db
        val lines = intent.lines.map { l ->
            val nm = db.items.firstOrNull { it.id == l.itemId }?.name ?: ""
            Triple(nm, l.qty, l.price)
        }.filter { it.first.isNotEmpty() }
        val ex = entries.firstOrNull { it.norm == n }
        if (ex != null) {
            // به‌روزرسانی دستور همان جمله به آخرین تفسیر درست
            entries.removeAll { it.norm == n }
            entries.add(fromIntent(raw, n, intent, ex.hits + 1, lines))
        } else {
            entries.add(fromIntent(raw, n, intent, 0, lines))
            if (entries.size > 300) {
                // قدیمی‌ترینِ کم‌استفاده حذف می‌شود تا حافظه تمیز بماند
                entries.sortByDescending { it.hits }
                while (entries.size > 300) entries.removeAt(entries.size - 1)
                entries.sortBy { all().indexOf(it) }
            }
        }
        unknowns.remove(n)
        save()
    }

    /** آموزش صریح کاربر: «وقتی گفتم X یعنی Y» — نتیجهٔ موفق/ناموفق + پیام فارسی */
    fun teach(whenPhrase: String, meaning: String): Pair<Boolean, String> {
        val wp = whenPhrase.trim()
        if (wp.length < 2) return Pair(false, "جملهٔ اول («وقتی گفتم …») را کامل نگفتید.")
        val intent = VoiceBrain.parseVoice(meaning)
        if (intent.kind == "unknown" || intent.kind == "query" || intent.kind == "teach")
            return Pair(false, "معنی «" + meaning.trim() + "» را هنوز بلد نیستم — اول معنی را با یک دستور پایه بگویید، مثلا «برو روزنامه» یا «فاکتور فروش دو تا شیر برای علی».")
        val n = VoiceBrain.norm(wp)
        val old = entries.firstOrNull { it.norm == n }
        entries.removeAll { it.norm == n }
        val db = Db.db
        val lines = intent.lines.map { l ->
            Triple(db.items.firstOrNull { it.id == l.itemId }?.name ?: "", l.qty, l.price)
        }.filter { it.first.isNotEmpty() }
        entries.add(fromIntent(wp, n, intent, old?.hits ?: 0, lines))
        unknowns.remove(n)
        save()
        val what = when (intent.kind) {
            "nav" -> "رفتن به «" + intent.routeLabel.ifEmpty { intent.route } + "»"
            "item_new" -> "ساخت کالا" + (if (intent.itemName.isNotEmpty()) " «" + intent.itemName + "»" else "")
            "item_del" -> "حذف کالا" + (if (intent.itemName.isNotEmpty()) " «" + intent.itemName + "»" else "")
            "partner_new" -> "ثبت طرف‌حساب" + (if (intent.partnerName.isNotEmpty()) " «" + intent.partnerName + "»" else "")
            "invoice_new" -> "صدور فاکتور"
            "payment_new" -> if (intent.payKind == "receipt") "ثبت دریافت" else "ثبت پرداخت"
            "search" -> "جستجوی «" + intent.query + "»"
            "reminder" -> "ثبت یادآور"
            else -> intent.kind
        }
        return Pair(true, "یاد گرفتم! از این به بعد وقتی بگویید «" + wp + "» یعنی " + what + ". خودتان امتحان کنید.")
    }

    /** فراموشی یک آموخته */
    fun forget(id: String) {
        entries.removeAll { it.id == id }
        save()
    }

    /* ═══════════ تطبیق ═══════════ */

    /** تطبیق دقیق — جملهٔ یکسان */
    fun matchExact(raw: String): Entry? {
        val n = VoiceBrain.norm(raw)
        if (n.length < 3) return null
        return entries.firstOrNull { it.norm == n }
    }

    /** تطبیق فازی با آستانهٔ ایمن — فقط وقتی پایهٔ برنامه جمله را نفهمیده استفاده می‌شود */
    fun matchFuzzy(raw: String, minScore: Double = 0.66): Entry? {
        val n = VoiceBrain.norm(raw)
        if (n.length < 4 || entries.isEmpty()) return null
        var best: Entry? = null
        var bs = 0.0
        for (e in entries) {
            val s = sim(n, e.norm)
            if (s > bs) { bs = s; best = e }
        }
        return if (best != null && bs >= minScore) best else null
    }

    /** ثبت جملهٔ نفهمیده‌شده — کاربر می‌تواند بعداً یادش بدهد */
    fun noteUnknown(raw: String) {
        val n = VoiceBrain.norm(raw)
        if (n.length < 3) return
        unknowns[n] = (unknowns[n] ?: 0) + 1
        if (unknowns.size > 60) {
            val keep = unknowns.entries.sortedByDescending { it.value }.take(60)
            unknowns.clear()
            for (k in keep) unknowns[k.key] = k.value
        }
        saveUnknown()
    }

    /** بازسازی کامل دستور — اسم کالاهای ذخیره‌شده دوباره با کالاهای فعلی تطبیق می‌دهد */
    fun expand(e: Entry): VoiceBrain.VoiceIntent {
        val intent = VoiceBrain.VoiceIntent(
            kind = e.kind, route = e.route, routeLabel = "", subView = e.subView,
            itemName = e.itemName, itemCode = e.itemCode, price = e.price, cost = e.cost, qty = e.qty, unit = e.unit,
            service = e.service, partnerName = e.partnerName, amount = e.amount,
            payKind = e.payKind, method = e.method, query = e.query, dayOffset = e.dayOffset,
            title = e.title
        )
        if (e.lines.isEmpty() || e.kind != "invoice_new") return intent
        val db = Db.db
        val lines = ArrayList<InvoiceLine>()
        val missed = ArrayList<String>()
        for (l in e.lines) {
            val it0 = VoiceBrain.bestMatch(db.items, { it.name }, l.first, 25)
                ?: db.items.firstOrNull { it.name.contains(VoiceBrain.norm(l.first)) }
            if (it0 != null) lines.add(InvoiceLine(Db.newId("l"), it0.id, l.second, if (l.third > 0) l.third else it0.price, 0.0))
            else missed.add(l.first)
        }
        return intent.copy(lines = lines, missed = missed.joinToString("، "))
    }

    /** تشخیص جملهٔ آموزش: «وقتی گفتم/گفتی/بگم X یعنی Y» */
    fun parseTeach(raw: String): Pair<String, String>? {
        val t = VoiceBrain.toks(VoiceBrain.norm(raw))
        val wi = t.indexOfFirst { it == "وقتی" }
        val yi = t.indexOfFirst { it == "یعنی" || it == "ینی" }
        if (wi >= 0 && yi > wi + 1) {
            val whenT = t.drop(wi + 1).takeWhile { it != "یعنی" && it != "ینی" }
                .dropWhile { it in setOf("گفتم", "گفتی", "بگم", "بگم", "بگویم", "میگم", "بگیم", "میگیم", "بگید", "میگید", "هر") }
            val meanT = t.drop(yi + 1)
            if (whenT.isNotEmpty() && meanT.isNotEmpty())
                return Pair(whenT.joinToString(" "), meanT.joinToString(" "))
        }
        return null
    }

    private fun fromIntent(raw: String, n: String, intent: VoiceBrain.VoiceIntent, hits: Int, lines: List<Triple<String, Double, Long>>) =
        Entry(
            id = Db.newId("lr"), phrase = raw.trim(), norm = n,
            kind = intent.kind, route = intent.route, subView = intent.subView,
            itemName = intent.itemName, partnerName = intent.partnerName, query = intent.query,
            title = intent.title, payKind = intent.payKind, method = intent.method, unit = intent.unit,
            price = intent.price, cost = intent.cost, amount = intent.amount, qty = intent.qty,
            dayOffset = intent.dayOffset, service = intent.service, hits = hits,
            itemCode = intent.itemCode, lines = lines
        )

    /* ═══════════ شباهت (لونشتاین + هم‌پوشانی توکن + کلمات پُرکننده) ═══════════ */

    /** کلمات تعارف و پرکننده — در مقایسهٔ لحن نادیده گرفته می‌شوند */
    private val NOISE = setOf(
        "لطفا", "خب", "اوکی", "باشه", "میخوام", "میخواستم", "میشه", "برام", "التماس",
        "ممنون", "مرسی", "سلام", "رو", "را", "دیگه", "دیگر", "هم", "میکنی", "میکنم", "بزنم"
    )

    private fun stripNoise(s: String): String =
        VoiceBrain.toks(s).filter { it !in NOISE }.joinToString(" ").trim()

    private fun sim(a0: String, b0: String): Double {
        val a = stripNoise(a0)
        val b = stripNoise(b0)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        val ta = VoiceBrain.toks(a).toSet()
        val tb = VoiceBrain.toks(b).toSet()
        val uni = (ta + tb).size
        val j = if (uni == 0) 1.0 else (ta intersect tb).size * 1.0 / uni
        val d = 1.0 - lev(a, b) / Math.max(1, Math.max(a.length, b.length)).toDouble()
        val base = 0.45 * d + 0.55 * j
        // اگر یکی داخل دیگری بود (جملهٔ کاربر با کلمات کمتر/بیشتر) — مطابقت قوی
        val contains = a.contains(b) || b.contains(a)
        return if (contains) Math.max(base, Math.min(0.78 + 0.03 * Math.min(ta.size, tb.size), 0.9)) else base
    }

    private fun lev(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        val cur = IntArray(b.length + 1)
        for (i in 1..a.length) {
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            System.arraycopy(cur, 0, prev, 0, cur.size)
        }
        return prev[b.length]
    }

    /* ═══════════ ذخیره‌سازی (فرمت متنی ساده، بدون وابستگی) ═══════════ */

    private fun esc(s: String) = s.replace("|", "«V»").replace("~", "«T»").replace("^", "«C»").replace("\n", "«N»")
    private fun un(s: String) = s.replace("«V»", "|").replace("«T»", "~").replace("«C»", "^").replace("«N»", "\n")

    private fun save() {
        val f = file ?: return
        try {
            val sb = StringBuilder()
            for (e in entries) {
                sb.append(esc(e.phrase)).append("|")
                    .append(esc(e.kind)).append("|").append(esc(e.route)).append("|").append(esc(e.subView))
                    .append("|").append(esc(e.itemName)).append("|").append(esc(e.partnerName))
                    .append("|").append(esc(e.query)).append("|").append(esc(e.title))
                    .append("|").append(esc(e.payKind)).append("|").append(esc(e.method)).append("|").append(esc(e.unit))
                    .append("|").append(e.price).append("|").append(e.cost).append("|").append(e.amount)
                    .append("|").append(e.qty).append("|").append(e.dayOffset)
                    .append("|").append(if (e.service) "1" else "0").append("|").append(e.hits)
                    .append("|")
                sb.append(e.lines.joinToString("^") { esc(it.first) + "~" + it.second.toString() + "~" + it.third.toString() })
                sb.append("|").append(esc(e.itemCode))
                sb.append("\n")
            }
            f.parentFile?.mkdirs()
            f.writeText(sb.toString(), Charsets.UTF_8)
        } catch (ig: Throwable) {}
    }

    private fun load() {
        val f = file ?: return
        try {
            if (!f.exists()) return
            entries.clear()
            for (line0 in f.readText(Charsets.UTF_8).split("\n")) {
                val line = line0.trim()
                if (line.isEmpty()) continue
                val p = line.split("|")
                if (p.size < 19) continue
                val lines = if (p[18].isEmpty()) emptyList() else p[18].split("^").mapNotNull { g ->
                    val gp = g.split("~")
                    if (gp.size < 3) null else Triple(un(gp[0]), gp[1].toDoubleOrNull() ?: 1.0, gp[2].toLongOrNull() ?: 0L)
                }
                entries.add(Entry(
                    id = Db.newId("lr"), phrase = un(p[0]), norm = VoiceBrain.norm(un(p[0])),
                    kind = un(p[1]), route = un(p[2]), subView = un(p[3]),
                    itemName = un(p[4]), partnerName = un(p[5]), query = un(p[6]), title = un(p[7]),
                    payKind = un(p[8]), method = un(p[9]), unit = un(p[10]),
                    price = p[11].toLongOrNull() ?: 0L, cost = p[12].toLongOrNull() ?: 0L,
                    amount = p[13].toLongOrNull() ?: 0L, qty = p[14].toDoubleOrNull() ?: 1.0,
                    dayOffset = p[15].toIntOrNull() ?: 0, service = p[16] == "1", hits = p[17].toIntOrNull() ?: 0,
                    itemCode = if (p.size > 19) un(p[19]) else "",
                    lines = lines
                ))
            }
        } catch (ig: Throwable) {}
    }

    private fun saveUnknown() {
        val f = ufile ?: return
        try {
            f.parentFile?.mkdirs()
            f.writeText(unknowns.entries.joinToString("\n") { esc(it.key) + "|" + it.value }, Charsets.UTF_8)
        } catch (ig: Throwable) {}
    }
}
