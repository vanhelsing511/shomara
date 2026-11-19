package ir.shomara.app

import android.content.Context
import android.content.Intent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ثبت، نمایش و اشتراک‌گذاری گزارش خطا — هیچ کرشی گم نمی‌شود.
 * همهٔ خطاهای مهارشدهٔ رابط + کرش‌های غیرمنتظره (از App) در crash.txt ثبت می‌شوند
 * و کاربر می‌تواند از بخش ابزارها یا کارت خطا، گزارش را مستقیم بفرستد.
 */
object ErrorReport {

    private fun file(c: Context): File = File(c.filesDir, "crash.txt")

    fun version(c: Context): String = try {
        c.packageManager.getPackageInfo(c.packageName, 0).versionName ?: "?"
    } catch (ig: Throwable) { "?" }

    /** ثبت خطای مهارشده — اپ بسته نمی‌شود ولی ردپای کامل می‌ماند */
    fun report(c: Context, where: String, e: Throwable) {
        try {
            android.util.Log.e("Shomara", where, e)
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val entry = "\n═══ خطا $ts [$where] نسخهٔ " + version(c) + " ═══\n" +
                android.util.Log.getStackTraceString(e) + "\n"
            val f = file(c)
            val prev = if (f.exists()) f.readText().take(120_000) else ""
            f.writeText(prev + entry)
        } catch (ig: Throwable) {}
    }

    /** یادداشت تشخیصی — بدون استثنا؛ برای ردیابی ابعاد چیدمان و مسیرهای حساس */
    fun note(c: Context, where: String, msg: String) {
        try {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val entry = "\n═══ نکته $ts [$where] نسخهٔ " + version(c) + " ═══\n" + msg + "\n"
            val f = file(c)
            val prev = if (f.exists()) f.readText().take(120_000) else ""
            f.writeText(prev + entry)
        } catch (ig: Throwable) {}
    }

    fun read(c: Context): String = try { file(c).readText() } catch (ig: Throwable) { "" }

    fun clear(c: Context) { try { file(c).delete() } catch (ig: Throwable) {} }

    /** اشتراک‌گذاری کل گزارش — برای ارسال به پشتیبانی */
    fun share(c: Context, extra: String = "") {
        try {
            val log = read(c).take(80_000)
            if (log.isEmpty()) { UI.toast(c, "هیچ خطایی ثبت نشده — همه‌چیز سالم است ✔"); return }
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, "گزارش خطای شمارا")
            i.putExtra(Intent.EXTRA_TEXT, extra + "\n" + log)
            c.startActivity(Intent.createChooser(i, "ارسال گزارش خطا"))
        } catch (ig: Throwable) { UI.toast(c, "امکان ارسال گزارش نیست") }
    }
}
