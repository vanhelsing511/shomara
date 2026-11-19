package ir.shomara.app

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pure utilities: Persian digits, money formatting, Jalali calendar.
 * No Android dependencies (testable on JVM).
 */
object J {

    private val FA = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    val J_MONTHS = arrayOf("فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند")

    // ---------- digits / numbers ----------

    fun fa(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            if (ch in '0'..'9') sb.append(FA[ch - '0'])
            else if (ch == ',') sb.append('٬')
            else if (ch == '.') sb.append('٫')
            else sb.append(ch)
        }
        return sb.toString()
    }

    /** 12500 -> "12,500" */
    fun group(v: Long): String {
        val neg = v < 0
        var s = java.lang.Math.abs(v).toString()
        val sb = StringBuilder()
        var c = 0
        for (i in s.length - 1 downTo 0) {
            sb.append(s[i])
            c++
            if (c % 3 == 0 && i > 0) sb.append(',')
        }
        s = sb.reverse().toString()
        return (if (neg) "-" else "") + s
    }

    fun money(v: Long): String = group(v)

    fun moneyFa(v: Long): String = fa(money(v))

    fun qty(v: Double): String {
        return if (v == v.toLong().toDouble()) v.toLong().toString()
        else String.format(Locale.US, "%.1f", v)
    }

    /** Accepts Persian/Latin digits, ٬ , and ٫ . */
    fun parseNum(raw: String): Double {
        val sb = StringBuilder()
        for (ch in raw) {
            if (ch in '۰'..'۹') sb.append(('0' + (ch - '۰')))
            else if (ch == '٫') sb.append('.')
            else if (ch == ',' || ch == '٬' || ch == ' ') {}
            else sb.append(ch)
        }
        return sb.toString().toDoubleOrNull() ?: 0.0
    }

    fun parseLong(raw: String): Long = java.lang.Math.round(parseNum(raw))

    // ---------- Jalali calendar (jdf algorithm) ----------

    fun g2d(gy0: Int, gm0: Int, gd0: Int): Int {
        // الگوریتم jdf: آرایه تجمعی «سال عادی» + جبران سال کبیسه از طریق gy2
        // محدودسازی ورودی — هرگز ArrayIndexOutOfBoundsException نمی‌دهد
        val gy = gy0.coerceIn(1, 4000)
        val gm = gm0.coerceIn(1, 12)
        val gd = gd0.coerceIn(1, 31)
        val cum = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        return 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + cum[gm - 1]
    }

    fun d2j(d00: Int): IntArray {
        // پیدا کردن سال با جستجوی سریع روی nowruzDay (مونوتونیک) — با محدودیت ایمن
        val base = g2d(2025, 3, 21)
        val d0 = d00.coerceIn(base - 400_000, base + 400_000)
        var jy = 1404 + Math.round((d0 - base) / 365.2422).toInt()
        var guard = 0
        while (nowruzDay(jy) > d0 && guard++ < 1500) jy--
        while (nowruzDay(jy + 1) <= d0 && guard++ < 3000) jy++
        jy = jy.coerceIn(1, 3000)
        val doy = d0 - nowruzDay(jy)
        val jm: Int
        val jd: Int
        if (doy < 186) {
            jm = 1 + doy / 31
            jd = 1 + doy % 31
        } else {
            jm = 7 + (doy - 186) / 30
            jd = 1 + (doy - 186) % 30
        }
        return intArrayOf(jy, jm, jd)
    }

    fun j2d(jy0: Int, jm0: Int, jd0: Int): Int {
        val jy = jy0.coerceIn(1, 3000)
        val jm = jm0.coerceIn(1, 12)
        val jd = jd0.coerceIn(1, 31)
        return nowruzDay(jy) + (if (jm <= 6) (jm - 1) * 31 + (jd - 1) else 186 + (jm - 7) * 30 + (jd - 1))
    }

    /**
     * سال‌های کبیسه رسمی تقویم ایران (رصدی). برای بازه ۱۳۰۰ تا ۱۵۰۰ دقیق؛
     * بیرون این بازه از چرخه ۳۳ ساله استفاده می‌شود (تقریب).
     */
    private val LEAPS = intArrayOf(
        1304, 1308, 1312, 1316, 1320, 1324, 1329, 1333, 1337, 1342,
        1346, 1350, 1354, 1358, 1362, 1366, 1371, 1375, 1379, 1383,
        1387, 1391, 1395, 1399, 1403, 1408, 1412, 1416, 1420, 1424,
        1428, 1432, 1437, 1441, 1445, 1449, 1453, 1457, 1461, 1465,
        1470, 1474, 1478, 1482, 1486, 1490, 1494, 1498
    )

    fun isJalaliLeap(jy: Int): Boolean {
        if (jy >= 1300 && jy <= 1500) {
            for (l in LEAPS) if (l == jy) return true
            return false
        }
        val r = ((jy % 33) + 33) % 33
        return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    }

    private fun leapsBetween(a: Int, b: Int): Int {
        // تعداد سال‌های کبیسه در بازه بسته [a, b]
        if (b < a) return 0
        var n = 0
        for (y in a..b) if (isJalaliLeap(y)) n++
        return n
    }

    /** روز شماره ۱ فروردین jy روی مقیاس g2d (مبنای تأییدشده: ۱۴۰۴/۰۱/۰۱ = 2025-03-21) */
    fun nowruzDay(jy: Int): Int {
        val base = g2d(2025, 3, 21)
        var delta = 365 * (jy - 1404)
        delta += if (jy > 1404) leapsBetween(1404, jy - 1) else if (jy < 1404) -leapsBetween(jy, 1403) else 0
        return base + delta
    }

    fun d2g(d0: Int): IntArray {
        // civil-from-days (Hinnant), shifted so numbers stay positive:
        // g2d(1970,1,1) == 1075195 ; shift = 719468 + 4*146097 - 1075195 = 228661
        val z = d0 + 228661
        val era = z / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        var y = era * 400 + yoe - 1600
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        if (m <= 2) y += 1
        return intArrayOf(y, m, d)
    }

    fun todayIso(): String {
        val c = Calendar.getInstance()
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun jDate(iso: String): IntArray {
        return try {
            val p = iso.trim().split("-")
            if (p.size < 3) return jDate(todayIso())
            d2j(g2d(p[0].trim().toIntOrNull() ?: return jDate(todayIso()), p[1].trim().toIntOrNull() ?: 1, p[2].trim().toIntOrNull() ?: 1))
        } catch (ig: Exception) { jDate(todayIso()) }
    }

    /** "1405/08/28" in Persian digits */
    fun jStr(iso: String): String {
        val j = jDate(iso)
        return fa(String.format(Locale.US, "%d/%02d/%02d", j[0], j[1], j[2]))
    }

    /** "۲۸ آبان ۱۴۰۵" */
    fun jStrLong(iso: String): String {
        val j = jDate(iso)
        return fa(j[2].toString()) + " " + jMonth(j[1]) + " " + fa(j[0].toString())
    }

    fun jMonth(m: Int): String = if (m in 1..12) J_MONTHS[m - 1] else ""

    /** 1=شنبه..7=جمعه */
    fun weekdayOfIso(iso: String): Int {
        val c = calFor(iso)
        val dow = c.get(Calendar.DAY_OF_WEEK) // 1=Sun..7=Sat
        return dow % 7 + 1
    }

    fun calFor(iso: String): Calendar {
        val c = Calendar.getInstance()
        try {
            val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            f.isLenient = false
            c.time = f.parse(iso.trim()) ?: Calendar.getInstance().time
        } catch (ig: Exception) { }
        return c
    }

    fun jMonthLen(jy0: Int, jm0: Int): Int {
        val jy = jy0.coerceIn(1, 2999)
        val jm = jm0.coerceIn(1, 12)
        if (jm <= 6) return 31
        if (jm <= 11) return 30
        return nowruzDay(jy + 1) - nowruzDay(jy) - 336
    }

    fun isoFromJal(jy: Int, jm: Int, jd: Int): String {
        val g = d2g(j2d(jy, jm, jd))
        return String.format(Locale.US, "%04d-%02d-%02d", g[0], g[1], g[2])
    }

    fun addDaysIso(iso: String, n: Int): String {
        val c = calFor(iso)
        c.add(Calendar.DAY_OF_MONTH, n)
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return f.format(c.time)
    }

    /** days between two iso dates (b - a) */
    fun daysBetween(a: String, b: String): Int {
        return j2dOf(b) - j2dOf(a)
    }

    fun j2dOf(iso: String): Int {
        val p = iso.split("-")
        if (p.size < 3) return 0
        return g2d(p[0].toInt(), p[1].toInt(), p[2].toInt())
    }

    // ---------- جلالی به‌صورت رشتهٔ «1405/08/28» (مثل نسخهٔ وب) ----------

    /** امروز به‌صورت رشتهٔ جلالی */
    fun jTodayStr(): String {
        val j = jDate(todayIso())
        return String.format(Locale.US, "%d/%02d/%02d", j[0], j[1], j[2])
    }

    fun jParse(s: String): IntArray {
        val p = s.trim().split("/")
        if (p.size < 3) return intArrayOf(1405, 1, 1)
        val y = (p[0].trim().toIntOrNull() ?: 1405).coerceIn(1, 3000)
        val m = (p[1].trim().toIntOrNull() ?: 1).coerceIn(1, 12)
        val d = (p[2].trim().toIntOrNull() ?: 1).coerceIn(1, 31)
        return intArrayOf(y, m, d)
    }

    fun jFmt(p: IntArray): String = String.format(Locale.US, "%d/%02d/%02d", p[0], p[1], p[2])

    /** نمایش فارسی تاریخ: ۱۴۰۵/۰۸/۲۸ */
    fun jFa(s: String): String = fa(s)

    /** شمارهٔ روز مطلق از تاریخ جلالی */
    fun jDaysOf(s: String): Int {
        val p = jParse(s)
        return j2d(p[0], p[1], p[2])
    }

    fun jAddMonths(s: String, n: Long): String {
        val p = jParse(s)
        var mm = p[1] + (n % 600).toInt()
        var yy = p[0]
        while (mm > 12) { mm -= 12; yy++ }
        while (mm <= 0) { mm += 12; yy-- }
        yy = yy.coerceIn(1, 2999)
        val dl = jMonthLen(yy, mm)
        return jFmt(intArrayOf(yy, mm, Math.min(p[2], dl)))
    }

    fun jAddDays(s: String, n: Long): String {
        val d = jDaysOf(s) + n.toInt()
        val g = d2g(d)
        val dd = d2j(g2d(g[0], g[1], g[2]))
        return String.format(Locale.US, "%d/%02d/%02d", dd[0], dd[1], dd[2])
    }

    /** کلید «سال/ماه» جلالی برای گروه‌بندی — امن برای هر رشتهٔ تاریخ */
    fun ymKey(jalali: String): String {
        val p = jParse(jalali)
        return String.format(Locale.US, "%d/%02d", p[0], p[1])
    }

    fun jDiff(a: String, b: String): Int = jDaysOf(a) - jDaysOf(b)

    fun jMonthOf(s: String): Int = jParse(s)[1]
    fun jYearOf(s: String): Int = jParse(s)[0]

    fun jYearOfToday(): Int = jDate(todayIso())[0]

    // ---------- عدد کوتاه فارسی ----------

    private fun trimNum(v: Double, dec: Int): String {
        val f = if (dec == 0) "%.0f" else "%." + dec + "f"
        var s = String.format(Locale.US, f, v)
        if (dec > 0 && s.endsWith(".0")) s = s.dropLast(2)
        return s
    }

    /** 12500000 -> «۱۲٫۵ م» / «۸۳۰ هزار» / «۱٫۵ میلیارد» */
    fun moneyShort(v: Long): String {
        val neg = v < 0
        val a = Math.abs(v.toDouble())
        val s: String = when {
            a >= 1_000_000_000.0 -> trimNum(a / 1_000_000_000.0, 1) + " میلیارد"
            a >= 1_000_000.0 -> trimNum(a / 1_000_000.0, if (a >= 10_000_000.0) 0 else 1) + " م"
            a >= 1_000.0 -> trimNum(Math.round(a / 1000.0).toDouble(), 0) + " هزار"
            else -> trimNum(Math.round(a).toDouble(), 0)
        }
        return (if (neg) "−" else "") + fa(s)
    }

    fun moneyShort(v: Double): String = moneyShort(Math.round(v))

    /** عدد فارسی از رشته/عدد */
    fun faN(v: Any): String = fa(v.toString())
}
