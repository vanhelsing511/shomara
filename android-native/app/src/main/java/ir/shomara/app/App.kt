package ir.shomara.app

import android.content.Context

/** کلاس اپلیکیشن — فونت‌ها، پایگاه داده و گزارشگر خطا */
class App : android.app.Application() {
    companion object { lateinit var ctx: Context }

    override fun onCreate() {
        super.onCreate()
        ctx = this
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                // گزارش کرش — همیشه اضافه می‌شود، آخرین ۳ کرش حفظ می‌شود
                val f = java.io.File(filesDir, "crash.txt")
                val sep = "\n\n════════ کرش جدید ${System.currentTimeMillis()} ════════\n"
                val txt = (if (f.exists()) f.readText().take(60_000) else "") + sep + android.util.Log.getStackTraceString(e)
                f.writeText(txt)
            } catch (ig: Exception) {}
            def?.uncaughtException(t, e)
        }
        try { U.initFonts(this) } catch (ig: Exception) {}
        try { Db.init(this) } catch (ig: Exception) {}
    }
}
