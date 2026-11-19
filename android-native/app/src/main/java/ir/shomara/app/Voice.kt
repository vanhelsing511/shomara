package ir.shomara.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/**
 * دستیار صوتی شمارا — میکروفون + گفتار فارسی + اجرای واقعی دستورها روی داده‌های کاربر
 * همه‌چیز بومی و بدون وابستگی: SpeechRecognizer خود اندروید (fa-IR)، TextToSpeech بومی،
 * رابط همان باتن‌شیت مو‌به‌مو با بقیهٔ اپ، ورودی تایپی به‌عنوان جایگزین همیشه هست.
 */

/* ═══════════ گویندهٔ پاسخ (TTS بومی) ═══════════ */

object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    var muted = false

    /** آیا الان صدای دستیار در حال پخش است؟ میکروفن تا تمام‌شدنش باز نمی‌شود تا صدای خودش را نشنود */
    @Volatile var speaking = false

    fun ensure(c: Context) {
        if (tts != null) return
        try {
            tts = TextToSpeech(c.applicationContext) { st ->
                ready = st == TextToSpeech.SUCCESS
                if (ready) pickLang()
            }
        } catch (ig: Throwable) { ready = false }
    }

    private fun pickLang() {
        val t = tts ?: return
        // رصد پایان گفتار — میکروفن فقط بعد از سکوت کامل دستیار دوباره باز می‌شود
        try {
            t.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(u: String?) { speaking = true }
                override fun onDone(u: String?) { speaking = false }
                override fun onError(u: String?) { speaking = false }
            })
        } catch (ig: Throwable) {}
        val opts = listOf(Locale("fa", "IR"), Locale("fa"), Locale("en", "US"), Locale.US)
        for (l in opts) {
            try {
                val r = t.setLanguage(l)
                if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) return
            } catch (ig: Exception) {}
        }
    }

    /** پخش پاسخ — متن بلند برای گفتار کوتاه می‌شود تا شنیدن خسته‌کننده نشود */
    fun say(raw: String) {
        if (muted || !ready) return
        try {
            val t = tts ?: return
            val clean = raw.replace(Regex("«|»|\\*"), "").replace(Regex("[\\n]+"), "؛ ").trim()
            t.setSpeechRate(1.02f)
            speaking = true
            t.speak(clean.take(320), TextToSpeech.QUEUE_FLUSH, null, "shomara-voice")
        } catch (ig: Throwable) { speaking = false }
    }

    fun stop() {
        speaking = false
        try { tts?.stop() } catch (ig: Throwable) {}
    }

    fun shutdown() {
        try { tts?.stop(); tts?.shutdown() } catch (ig: Throwable) {}
        tts = null; ready = false
    }
}

/* ═══════════ اجرای دستور صوتی روی داده‌های واقعی ═══════════ */

/**
 * هر دستور پارس‌شده را روی دیتابیس واقعی کاربر اجرا می‌کند و پیام دستیار را برمی‌گرداند.
 * ناوبری: ماژول را باز می‌کند؛ ساخت/حذف/صدور: سند را واقعاً ثبت می‌کند.
 */
fun MainActivity.voiceExec(v: VoiceBrain.VoiceIntent, confirmed: Boolean = false): ChatMsg {
    fun mk(text: String, label: String = "", go: String = "") =
        ChatMsg(Db.newId("m"), "bot", text, label, go)
    val db = Db.db
    return try {
        when (v.kind) {

            "nav" -> {
                when (v.route) {
                    "ledger" -> if (v.subView.isNotEmpty()) ledgerView = v.subView
                    "invoices" -> if (v.subView.isNotEmpty()) invTab = v.subView
                    "checks" -> if (v.subView.isNotEmpty()) checksTab = v.subView
                    "partners" -> if (v.subView.isNotEmpty()) partnersType = v.subView
                    "reports" -> if (v.subView.isNotEmpty()) reportTab = v.subView
                    "cash" -> if (v.subView.isNotEmpty()) cashTab = v.subView
                }
                go(v.route)
                mk(v.reply.ifEmpty { "رفتم به «" + (TITLES[v.route]?.first ?: v.route) + "»." })
            }

            "item_new" -> {
                val name = v.itemName.trim()
                // ⚡ کد گفتاری — اگر کاربر کد گفت، همان ثبت می‌شود؛ اگر تکراری بود خودکار کد آزاد می‌گیریم
                var code = v.itemCode.trim()
                if (code.isNotEmpty() && db.items.any { it.code == code }) code = nextItemCode()
                if (code.isEmpty()) code = nextItemCode()
                val codeNote = if (v.itemCode.trim().isNotEmpty() && code != v.itemCode.trim())
                    " کد «" + J.fa(v.itemCode.trim()) + "» تکراری بود — کد آزاد " + J.fa(code) + " گذاشتم."
                else ""
                if (name.isNotEmpty()) {
                    if (db.items.any { it.name.trim().equals(name, true) }) return mk("کالای «" + name + "» از قبل وجود دارد؛ از جستجو استفاده کنید تا همان رکورد را باز کنید.", "جستجوی کالا", "items")
                    if (v.itemBarcode.isNotBlank() && db.items.any { it.barcode.trim() == v.itemBarcode.trim() }) return mk("این بارکد قبلاً به کالا دیگری متصل شده است؛ کالا ثبت نشد.")
                    // بدون قیمت هم ثبت ممکن است؛ در پاسخ، خالی بودن قیمت شفاف گفته می‌شود.
                    val it0 = Item(
                        Db.newId("it"), code, name,
                        db.groups.firstOrNull()?.id ?: "", v.unit,
                        v.price, v.cost, v.service, 0.0, v.itemBarcode.trim(), "ثبت با دستیار صوتی هوشیار"
                    )
                    // کالا و سند موجودی اولیه در یک تراکنش ذخیره می‌شوند؛ پیام موفقیت فقط پس از ذخیرهٔ پایدار.
                    val wh = db.warehouses.firstOrNull()
                    if (v.qty > 0 && !v.service && wh == null) return mk("کالا ثبت نشد: برای افزودن موجودی اولیه، ابتدا یک انبار بسازید.")
                    val stockDoc = if (v.qty > 0 && !v.service && wh != null) {
                        val no = (db.stockDocs.maxOfOrNull { it.no } ?: 300L) + 1
                        StockDoc(Db.newId("sd"), no, "adjust", J.jTodayStr(), wh.id,
                            "", listOf(StockLine(it0.id, v.qty)), "موجودی اولیه با دستیار صوتی")
                    } else null
                    val saved = Db.update { d -> d.copy(items = d.items + it0,
                        stockDocs = if (stockDoc != null) listOf(stockDoc) + d.stockDocs else d.stockDocs) }
                    if (!saved) return mk("ثبت کالا ناموفق بود؛ داده به حافظه یا فایل پایدار ذخیره نشد. دوباره تلاش کنید.")
                    go("items")
                    UI.toast(this, "کالا ثبت شد: " + name)
                    mk("کالای «" + name + "» ثبت شد" +
                        (if (v.itemCode.trim().isNotEmpty()) " با کد " + J.fa(code) else "") +
                        (if (v.itemBarcode.trim().isNotEmpty()) " و بارکد " + J.fa(v.itemBarcode.trim()) else "") +
                        (if (v.price > 0) " — قیمت فروش " + J.moneyFa(v.price) + " تومان" else " — فعلاً بدون قیمت؛ هر وقت خواستید از فرم کالا قیمتش را بزنید") +
                        codeNote +
                        (if (v.cost > 0) "، بهای خرید " + J.moneyFa(v.cost) + " تومان" else "") +
                        (if (v.qty > 0) "، " + J.qty(v.qty) + " " + v.unit + " هم به موجودی انبار اضافه شد" else "") +
                        ".", "رفتن به کالاها", "items")
                } else {
                    val it0 = Item(
                        Db.newId("it"), code, name,
                        db.groups.firstOrNull()?.id ?: "", v.unit,
                        v.price, 0, v.service, 0.0, v.itemBarcode.trim(), ""
                    )
                    itemEditorSheet(this, it0)
                    mk("فرم کالای جدید باز شد" +
                        (if (v.itemCode.trim().isNotEmpty()) " با کد " + J.fa(code) else "") +
                        " — اسم و قیمت را کامل کنید یا بگویید مثلا «کالای جدید یخچال قیمت پنجاه هزار تومان».", "همین حالا دستی ثبت می‌کنم", "items")
                }
            }

            "item_del" -> {
                val q = v.itemName
                if (q.isEmpty()) mk("اسم کالا را نگفتید — مثلا بگویید «حذف کالای شیر».")
                else {
                    val hits = db.items.filter { VoiceBrain.scoreMatch(it.name, q) >= 30 }
                        .sortedByDescending { VoiceBrain.scoreMatch(it.name, q) }
                    when {
                        hits.isEmpty() -> mk("کالایی شبیه «" + q + "» پیدا نکردم. می‌توانید بگویید «سرچ " + q + "» تا خودتان ببینید.")
                        hits.size == 1 -> {
                            val it0 = hits[0]
                            if (Db.itemHasReferences(db, it0.id)) return mk("حذف «" + it0.name + "» مسدود شد؛ در فاکتور/قالب/سند انبار استفاده شده و برای حفظ سابقه نمی‌توان حذفش کرد.")
                            confirmSheet(this, "حذف کالای «" + it0.name + "»؟", "این حذف فقط برای کالای بدون گردش مجاز است.") {
                                val saved = Db.update { d -> d.copy(items = d.items.filter { it.id != it0.id }) }
                                if (saved) { refresh(); UI.toast(this, "کالای «" + it0.name + "» حذف شد") }
                            }
                            mk("«" + it0.name + "» را پیدا کردم — تایید حذف باز شد. چون حذف مهم است، خودتان نهایی کنید.")
                        }
                        else -> {
                            optionSheet(this, "کدام کالا؟", hits.take(8).map { Opt(it.id, it.name, J.moneyFa(it.price) + " تومان") }, "") { o ->
                                val del = Db.db.items.firstOrNull { it.id == o.id } ?: return@optionSheet
                                if (Db.itemHasReferences(Db.db, del.id)) { UI.toast(this, "این کالا در اسناد ثبت شده؛ حذف آن مسدود است."); return@optionSheet }
                                confirmSheet(this, "حذف کالای «" + del.name + "»؟", "این حذف فقط برای کالای بدون گردش مجاز است.") {
                                    val saved = Db.update { d -> d.copy(items = d.items.filter { it.id != del.id }) }
                                    if (saved) { refresh(); UI.toast(this, "کالای «" + del.name + "» حذف شد") }
                                }
                            }
                            mk(J.faN(hits.size) + " کالا شبیه «" + q + "» پیدا کردم — از لیست انتخاب کنید.")
                        }
                    }
                }
            }

            "partner_new" -> {
                val name = v.partnerName.trim()
                val type = if (v.service) "supplier" else "customer"
                if (name.isNotEmpty()) {
                    if (db.partners.any { it.name.trim().equals(name, true) }) return mk("طرف‌حساب «" + name + "» از قبل ثبت شده؛ برای مشاهده و استفاده از همان رکورد، جستجو کنید.", "جستجوی طرف‌حساب", "partners")
                    val p = Partner(Db.newId("pt"), Db.nextPartnerCode(db), name, type, "", "", "", "")
                    if (!Db.update { d -> d.copy(partners = d.partners + p) }) return mk("ثبت طرف‌حساب ناموفق بود؛ ذخیره‌سازی انجام نشد. دوباره تلاش کنید.")
                    partnersType = type
                    go("partners")
                    UI.toast(this, "طرف‌حساب ثبت شد: " + name)
                    mk((if (type == "supplier") "تامین‌کنندهٔ" else "مشتری") + " «" + name + "» ثبت شد. از این به بعد می‌توانید برایش فاکتور بزنید.", "طرف‌حساب‌ها", "partners")
                } else {
                    partnerEditorSheet(this, Partner(Db.newId("pt"), Db.nextPartnerCode(db), "", type))
                    mk("فرم طرف‌حساب جدید باز شد — اسم و شماره را کامل کنید.")
                }
            }

            "partner_del" -> {
                val q = v.partnerName
                val hits = if (q.isEmpty()) emptyList() else db.partners.filter { VoiceBrain.scoreMatch(it.name, q) >= 30 }
                when {
                    q.isEmpty() -> mk("اسم طرف‌حساب را نگفتید.")
                    hits.isEmpty() -> mk("طرف‌حسابی شبیه «" + q + "» پیدا نکردم.")
                    hits.size == 1 -> {
                        val p0 = hits[0]
                        if (Db.partnerHasReferences(db, p0.id)) return mk("حذف «" + p0.name + "» مسدود شد؛ در فاکتور، پرداخت، چک، قالب یا پروژه استفاده شده است.")
                        confirmSheet(this, "حذف «" + p0.name + "»؟", "این حذف فقط برای طرف‌حساب بدون گردش مجاز است.") {
                            val saved = Db.update { d -> d.copy(partners = d.partners.filter { it.id != p0.id }) }
                            if (saved) { refresh(); UI.toast(this, "«" + p0.name + "» حذف شد") }
                        }
                        mk("«" + p0.name + "» پیدا شد — تایید حذف باز شد.")
                    }
                    else -> {
                        optionSheet(this, "کدام طرف‌حساب؟", hits.take(8).map { Opt(it.id, it.name, LB.partnerTypeLabel(it.type)) }, "") { o ->
                            val del = Db.db.partners.firstOrNull { it.id == o.id } ?: return@optionSheet
                            if (Db.partnerHasReferences(Db.db, del.id)) { UI.toast(this, "این طرف‌حساب گردش دارد؛ حذف آن مسدود است."); return@optionSheet }
                            confirmSheet(this, "حذف «" + del.name + "»؟", "این حذف فقط برای طرف‌حساب بدون گردش مجاز است.") {
                                val saved = Db.update { d -> d.copy(partners = d.partners.filter { it.id != del.id }) }
                                if (saved) { refresh(); UI.toast(this, "«" + del.name + "» حذف شد") }
                            }
                        }
                        mk(J.faN(hits.size) + " طرف‌حساب مشابه پیدا کردم؛ برای جلوگیری از حذف اشتباه، انتخاب کنید.")
                    }
                }
            }

            "invoice_new" -> {
                val kind = if (v.subView.isNotEmpty()) v.subView else K_SALE
                if (!confirmed) {
                    val itemSummary = v.lines.joinToString("\n") { line ->
                        val itemName = db.items.firstOrNull { it.id == line.itemId }?.name ?: "کالای نامشخص"
                        itemName + " × " + J.faN(line.qty) + " | واحد: " + J.moneyFa(line.price) +
                            " | تخفیف واحد: " + J.moneyFa(Math.round(line.discount))
                    }.ifEmpty { "اقلام نیازمند تکمیل" }
                    val previewInvoice = Invoice(
                        id = "voice-preview", no = 0, kind = kind, partnerId = "",
                        warehouseId = db.warehouses.firstOrNull()?.id ?: "", date = J.jTodayStr(),
                        lines = v.lines, discount = 0, vatRate = db.settings.vatRate, paid = 0,
                        payBankId = db.banks.firstOrNull()?.id ?: "", final = false
                    )
                    val previewTotals = Db.invoiceTotals(previewInvoice)
                    val supplierType = kind == K_PURCHASE || kind == K_PURCHASE_RET
                    val previewPartner = if (v.partnerName.isNotEmpty()) {
                        val eligible = db.partners.filter { it.type == (if (supplierType) "supplier" else "customer") || it.type == "both" }
                        VoiceBrain.bestMatch(db.partners, { it.name }, v.partnerName, 30)
                            ?: VoiceBrain.bestMatch(eligible, { it.name }, v.partnerName, 15)
                    } else null
                    val partnerPreview = when {
                        v.partnerName.isEmpty() -> "بدون طرف‌حساب"
                        previewPartner != null -> "${previewPartner.name} (تطبیق‌شده)"
                        else -> "طرف‌حساب جدید: ${v.partnerName}"
                    }
                    val totalSummary = "\nجمع قبل تخفیف: ${J.moneyFa(previewTotals.gross)} تومان" +
                        "\nجمع خالص: ${J.moneyFa(previewTotals.net)} تومان" +
                        "\nمالیات: ${J.moneyFa(previewTotals.vat)} تومان" +
                        "\nمبلغ کل: ${J.moneyFa(previewTotals.total)} تومان" +
                        (if (v.missed.isNotEmpty()) "\nکالاهای پیدا نشده: ${v.missed}" else "")
                    confirmSheet(this, "تأیید ${LB.invoiceKindLabel(kind)} صوتی",
                        "قبل از ثبت، اطلاعات فرمان را بررسی کنید:\n$itemSummary\nطرف‌حساب: $partnerPreview" +
                            totalSummary + "\nثبت فقط پس از تأیید شما انجام می‌شود.", "ثبت فاکتور") {
                        val beforeCount = Db.db.invoices.size
                        val result = voiceExec(v, confirmed = true)
                        if (Db.db.invoices.size > beforeCount && pendingVoicePhrase.isNotBlank()) {
                            VoiceLearn.record(pendingVoicePhrase, v)
                            pendingVoicePhrase = ""
                        }
                        chatMsgs = chatMsgs + result
                        saveChat()
                        if (tab == "assistant") refreshAssistant()
                    }
                    return mk("پیش‌نمایش فاکتور آماده است. برای ثبت نهایی، اطلاعات را در پنجره تأیید بررسی کنید.")
                }
                val supplierish = kind == K_PURCHASE || kind == K_PURCHASE_RET
                var partnerId = ""
                var newPartner: Partner? = null
                if (v.partnerName.isNotEmpty()) {
                    val pool = db.partners.filter { it.type == (if (supplierish) "supplier" else "customer") || it.type == "both" }
                    val m = VoiceBrain.bestMatch(db.partners, { it.name }, v.partnerName, 30)
                        ?: VoiceBrain.bestMatch(pool, { it.name }, v.partnerName, 15)
                    if (m != null) partnerId = m.id
                    else {
                        newPartner = Partner(Db.newId("pt"), Db.nextPartnerCode(db), v.partnerName,
                            if (supplierish) "supplier" else "customer")
                        partnerId = newPartner!!.id
                    }
                }
                if (v.lines.isNotEmpty()) {
                    val base = when (kind) { K_SALE -> 100L; K_PURCHASE -> 200L; K_PROFORMA -> 300L; else -> 400L }
                    val no = (db.invoices.filter { it.kind == kind }.maxOfOrNull { it.no } ?: base) + 1
                    val inv = Invoice(
                        Db.newId("inv"), no, kind, partnerId,
                        db.warehouses.firstOrNull()?.id ?: "", J.jTodayStr(),
                        v.lines, 0, db.settings.vatRate, 0,
                        payBankId = db.banks.firstOrNull()?.id ?: "",
                        final = kind != K_PROFORMA,
                        moudian = kind == K_SALE && db.settings.moudian,
                        desc = "صدور با دستیار صوتی هوشیار"
                    )
                    if (!Db.update { d -> d.copy(invoices = listOf(inv) + d.invoices,
                        partners = if (newPartner != null) d.partners + newPartner!! else d.partners) })
                        return mk("صدور فاکتور انجام نشد؛ ذخیره‌سازی ناموفق بود و طرف‌حساب جدید نیز ثبت نشد.")
                    invTab = kind
                    go("invoices")
                    val tot = Db.invoiceTotals(inv)
                    val pn = db.partners.firstOrNull { it.id == partnerId }?.name ?: (if (partnerId.isEmpty()) "بدون مشتری" else v.partnerName)
                    UI.toast(this, LB.invoiceKindLabel(kind) + " شمارهٔ " + J.faN(no) + " صادر شد")
                    mk(LB.invoiceKindLabel(kind) + " شمارهٔ " + J.faN(no) + " برای «" + pn + "» صادر شد — " +
                        J.faN(v.lines.size) + " قلم، مبلغ کل " + J.moneyFa(tot.total) + " تومان با مالیات. " +
                        "سند حسابداری و گردش انبار هم خودکار ثبت شد." +
                        (if (kind == K_PROFORMA) " چون پیش‌فاکتور است، نهایی نشده — در اپ تاییدش کنید." else "") +
                        (if (v.missed.isNotEmpty()) "\nمواردی که در کالاها پیدا نکردم: " + v.missed + " — بگویید «کالا جدید …» تا ثبتشان کنم." else ""),
                        "مشاهدهٔ فاکتورها", "invoices")
                } else {
                    val inv0 = newInvoiceFor(kind).copy(partnerId = partnerId)
                    invoiceEditorSheet(this, inv0)
                    mk("فرم " + LB.invoiceKindLabel(kind) + " باز شد" +
                        (if (v.partnerName.isNotEmpty()) " برای «" + v.partnerName + "»" else "") +
                        " — اقلام را اضافه و ثبت کنید.")
                }
            }

            "payment_new" -> {
                if (!confirmed) {
                    val direction = if (v.payKind == "receipt") "دریافت" else "پرداخت"
                    val matchedPartner = if (v.partnerName.isNotEmpty())
                        VoiceBrain.bestMatch(db.partners, { it.name }, v.partnerName, 30) else null
                    val person = when {
                        v.partnerName.isEmpty() -> "طرف‌حساب مشخص نشده"
                        matchedPartner != null -> "${matchedPartner.name} (تطبیق‌شده)"
                        else -> "طرف‌حساب جدید: ${v.partnerName}"
                    }
                    confirmSheet(this, "تأیید $direction صوتی",
                        "$direction ${J.moneyFa(v.amount)} تومان\nطرف‌حساب: $person\nروش: ${LB.payMethodLabel(v.method)}\nاین عملیات پس از تأیید ثبت می‌شود.", "ثبت $direction") {
                        val beforeCount = Db.db.payments.size
                        val result = voiceExec(v, confirmed = true)
                        if (Db.db.payments.size > beforeCount && pendingVoicePhrase.isNotBlank()) {
                            VoiceLearn.record(pendingVoicePhrase, v)
                            pendingVoicePhrase = ""
                        }
                        chatMsgs = chatMsgs + result
                        saveChat()
                        if (tab == "assistant") refreshAssistant()
                    }
                    return mk("پیش‌نمایش $direction آماده است. مبلغ، طرف‌حساب و روش را در پنجره تأیید بررسی کنید.")
                }
                val supplierish = v.payKind == "payment"
                var partnerId = ""
                var newPartner: Partner? = null
                if (v.partnerName.isNotEmpty()) {
                    val m = VoiceBrain.bestMatch(db.partners, { it.name }, v.partnerName, 30)
                    if (m != null) partnerId = m.id
                    else {
                        newPartner = Partner(Db.newId("pt"), Db.nextPartnerCode(db), v.partnerName,
                            if (supplierish) "supplier" else "customer")
                        partnerId = newPartner!!.id
                    }
                }
                val p = Payment(
                    Db.newId("pay"), 400L + db.payments.size + 1, v.payKind, v.method,
                    partnerId, db.banks.firstOrNull()?.id ?: "", v.amount, J.jTodayStr(),
                    "ثبت با دستیار صوتی هوشیار"
                )
                if (!Db.update { d -> d.copy(payments = listOf(p) + d.payments,
                    partners = if (newPartner != null) d.partners + newPartner!! else d.partners) })
                    return mk("ثبت دریافت/پرداخت ناموفق بود؛ ذخیره‌سازی انجام نشد و طرف‌حساب جدید نیز ثبت نشد.")
                cashTab = v.payKind
                go("cash")
                val pn = db.partners.firstOrNull { it.id == partnerId }?.name ?: ""
                UI.toast(this, (if (v.payKind == "receipt") "دریافت" else "پرداخت") + " ثبت شد")
                mk((if (v.payKind == "receipt") "دریافت " else "پرداخت ") + J.moneyFa(v.amount) + " تومانی" +
                    (if (pn.isNotEmpty()) " برای «" + pn + "»" else "") +
                    " ثبت شد — سند حسابداری خودکار ساخته شد و خزانه به‌روز شد.", "دریافت و پرداخت", "cash")
            }

            "reminder" -> {
                if (v.title.isEmpty()) mk("موضوع یادآور را نگفتید — مثلا «یادآور تماس با علی تا فردا».")
                else {
                    val date = J.jAddDays(J.jTodayStr(), v.dayOffset.toLong())
                    val r = Reminder(Db.newId("rem"), date, v.title, "", false)
                    if (!Db.update { d -> d.copy(reminders = d.reminders + r) }) return mk("ثبت یادآور ناموفق بود؛ ذخیره‌سازی انجام نشد.")
                    go("tools")
                    mk("یادآور «" + v.title + "» برای " + J.jFa(date) + " ثبت شد — در بخش ابزارها می‌بینیدش.", "ابزارها", "tools")
                }
            }

            // ثبت چک با صدا — فرم پیش‌پرشده باز می‌شود و تایید نهایی با کاربر است
            "check_new" -> {
                val kind = if (v.payKind == "issued") "issued" else "received"
                var partnerId = ""
                if (v.partnerName.isNotEmpty()) {
                    val m = VoiceBrain.bestMatch(db.partners, { it.name }, v.partnerName, 30)
                    if (m != null) partnerId = m.id
                    else {
                        val p = Partner(Db.newId("pt"), Db.nextPartnerCode(db), v.partnerName,
                            if (kind == "issued") "supplier" else "customer")
                        Db.update { d -> d.copy(partners = d.partners + p) }
                        partnerId = p.id
                    }
                } else partnerId = db.partners.firstOrNull()?.id ?: ""
                val due = J.jAddDays(J.jTodayStr(), v.dayOffset.toLong())
                val ch = Check(Db.newId("ck"), 500L + db.checks.size + 1, kind, partnerId, "", "", v.amount, J.jTodayStr(), due, "drawer")
                checksTab = kind
                checkEditorSheet(this, ch)
                mk((if (kind == "issued") "چک پرداختی" else "چک دریافتی") +
                    (if (v.amount > 0) " به مبلغ " + J.moneyFa(v.amount) + " تومان" else "") +
                    (if (v.partnerName.isNotEmpty()) " برای «" + v.partnerName + "»" else "") +
                    (if (v.dayOffset > 0) " با سررسید " + J.jFa(due) else "") +
                    " آماده شد — فرم ثبت چک باز شد؛ بانک و تایید نهایی با شما.", "چک‌ها", "checks")
            }

            // پوستهٔ روشن/تیره با صدا
            "theme" -> {
                val want = v.title
                if (want.isNotEmpty() && ((want == "dark") == T.dark)) {
                    mk(if (want == "dark") "پوسته همین حالا تیره است." else "پوسته همین حالا روشن است.")
                } else {
                    toggleTheme()
                    mk(v.reply.ifEmpty { "پوسته عوض شد." })
                }
            }

            "backup" -> {
                backupJson()
                mk("پشتیبان‌گیری کامل باز شد — فایل JSON را در جای امن نگه دارید.")
            }

            // یاد دادن به دستیار: «وقتی گفتم X یعنی Y»
            "teach" -> {
                val (ok, msg) = VoiceLearn.teach(v.title, v.query)
                if (ok) UI.toast(this, "هوشیار یاد گرفت — با لحن خودتان حرف بزنید")
                mk(msg)
            }

            // اجرای آموختهٔ فازی بعد از تایید کاربر
            "learn_confirm" -> {
                val e0 = VoiceLearn.byId(v.learnId)
                if (e0 == null) mk("این آموخته پیدا نشد — دوباره یادش بدهید.")
                else {
                    confirmSheet(this, "«" + e0.phrase + "» اجرا شود؟",
                        "این دستور را قبلاً از شما یاد گرفته‌ام — با تایید شما اجرا می‌شود.", "اجرا کن") {
                        val res = try {
                            voiceExec(VoiceLearn.expand(e0))
                        } catch (err: Throwable) {
                            ErrorReport.report(this, "learn-confirm", err)
                            ChatMsg(Db.newId("m"), "bot", "اجرای آموخته با خطا مواجه شد — اپ سالم است.")
                        }
                        chatMsgs = chatMsgs + res
                        saveChat()
                        if (tab == "assistant") refreshAssistant()
                        UI.toast(this, "فرمان آموخته ارسال شد؛ عملیات مالی در صورت نیاز تایید جداگانه دارد.")
                    }
                    mk("«" + e0.phrase + "» را از شما یاد گرفته‌ام — پنجرهٔ تایید باز شد تا اجرا شود.")
                }
            }

            "search" -> {
                globalSearchSheet(this, v.query)
                mk(if (v.query.isNotEmpty()) "جستجوی «" + v.query + "» باز شد؛ نتایج واقعی کالاها، اشخاص و اسناد را نشان می‌دهد." else "جستجوی همه‌جا باز شد؛ عبارت را وارد کنید تا نتایج واقعی نمایش داده شوند.")
            }
            // پرسش‌های گزارش/حسابداری توسط assistantAnswer از دادهٔ فعلی DB محاسبه می‌شوند؛ action link نیز حفظ می‌شود.
            "query" -> mk(v.reply.ifEmpty { "برای این پرسش هنوز پاسخ محاسباتی ندارم." }, v.actionLabel, v.actionGo)
            "help" -> mk(v.reply)
            "hello" -> mk(v.reply)

            else -> mk(v.reply.ifEmpty { "این را نفهمیدم — بگویید «چیکار میتونی بکنی» تا لیست دستورها را ببینید." })
        }
    } catch (e: Throwable) {
        ErrorReport.report(this, "voiceExec:" + v.kind, e)
        UI.toast(this, "اجرای دستور صوتی با خطا مواجه شد — گزارش ثبت شد")
        mk("اجرای این دستور با خطا مواجه شد ولی اپ سالم است. دوباره تلاش کنید یا از فرم خود ماژول استفاده کنید.")
    }
}

/* ═══════════ پردازش مشترک دستور — هم میکروفن فوتر هم صفحهٔ هوشیار ═══════════ */

/**
 * هر جملهٔ صوتی/متنی از کاربر از همین مسیر پردازش می‌شود: پارس → اجرای واقعی →
 * ثبت در تاریخچهٔ گفتگو → یادگیری لحن کاربر → گفتن پاسخ. همهٔ ورودی‌ها یک رفتار دارند.
 */
fun MainActivity.processVoiceCommand(text: String): Pair<ChatMsg, VoiceBrain.VoiceIntent> {
    Speaker.stop()
    val intent = VoiceBrain.parseVoice(text)
    if (intent.kind in setOf("invoice_new", "payment_new")) pendingVoicePhrase = text
    val beforeItems = Db.db.items.size
    val beforePartners = Db.db.partners.size
    val beforeInvoices = Db.db.invoices.size
    val beforePayments = Db.db.payments.size
    val beforeReminders = Db.db.reminders.size
    val bot = try {
        voiceExec(intent)
    } catch (err: Throwable) {
        ErrorReport.report(this, "voice-handle", err)
        ChatMsg(Db.newId("m"), "bot", "یک خطا رخ داد ولی اپ سالم است — دوباره بگویید.")
    }
    // یادگیری: هر دستور موفق همراه با جملهٔ اصلی کاربر حفظ می‌شود؛
    // جملهٔ نفهمیده هم ثبت می‌شود تا کاربر بتواند یادش بدهد
    // یادگیری فقط بعد از اثبات اجرا؛ فرمان‌هایی که صرفاً پیش‌نمایش/فرم باز می‌کنند ذخیره نمی‌شوند.
    val after = Db.db
    val executed = when (intent.kind) {
        "nav" -> tab == intent.route
        "search" -> true // پنجرهٔ جستجو واقعاً باز می‌شود
        "item_new" -> intent.itemName.isNotBlank() && after.items.size > beforeItems && after.items.any { it.name.equals(intent.itemName.trim(), true) }
        "partner_new" -> intent.partnerName.isNotBlank() && after.partners.size > beforePartners && after.partners.any { it.name.equals(intent.partnerName.trim(), true) }
        "reminder" -> intent.title.isNotBlank() && after.reminders.size > beforeReminders && after.reminders.any { it.title == intent.title }
        "invoice_new" -> after.invoices.size > beforeInvoices
        "payment_new" -> after.payments.size > beforePayments
        "theme" -> true
        else -> false
    }
    if (executed && intent.kind !in setOf("invoice_new", "payment_new")) VoiceLearn.record(text, intent)
    else if (intent.kind == "unknown" || intent.reply.startsWith(VoiceBrain.GENERIC_FALLBACK)) VoiceLearn.noteUnknown(text)
    chatMsgs = chatMsgs + ChatMsg(Db.newId("m"), "user", text) + bot
    saveChat()
    Speaker.say(bot.text)
    return bot to intent
}

/* ═══════════ میکروفن نگه‌داشتنی فوتر ═══════════ */

/**
 * ⚡ درمان قطعی «قطع‌وصل شدن و حساسیت زیاد»: میکروفن فقط وقتی گوش می‌دهد که
 * انگشت کاربر روی دکمه است. تا وقت که نگه داشته شده، هر بار شناساگر نتیجه یا
 * خطا بدهد بلافاصله دوباره شروع می‌شود (حلقهٔ پیوسته) — پس با هیچ مکثی جلسه
 * بسته نمی‌شود. با رهاکردن دکمه، آخرین جملهٔ شنیده‌شده پردازش می‌شود.
 */
class HoldTalker(private val act: MainActivity) {
    private var sr: SpeechRecognizer? = null
    private var handler = Handler(Looper.getMainLooper())
    private var held = false
    private var finishing = false
    private var busyErrs = 0
    private var best = ""

    /** متن زندهٔ شنیده‌شده — برای نمایش روی پیام «در حال شنیدن» */
    var onUpdate: ((String) -> Unit)? = null
    /** شروع/پایان شنیدن — برای رنگ میکروفن و انیمیشن */
    var onActive: ((Boolean) -> Unit)? = null
    /** جملهٔ نهایی پس از رهاکردن دکمه */
    var onFinal: ((String) -> Unit)? = null

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, act.packageName)
            // آستانه‌های سکوتِ بلند — حتی اگر سرویس گفتار خودش جلسه را ببندد،
            // حلقهٔ نگه‌داشتن بلافاصله دوباره شروع می‌کند و کاربر هیچ قطعی نمی‌بیند
            try {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 8000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 6000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
            } catch (ig: Throwable) {}
        }

    private fun ensure(): Boolean {
        if (sr != null) return true
        sr = try { SpeechRecognizer.createSpeechRecognizer(act) } catch (ig: Throwable) { null }
        if (sr == null && Build.VERSION.SDK_INT >= 31) {
            sr = try { SpeechRecognizer.createOnDeviceSpeechRecognizer(act) } catch (ig: Throwable) { null }
        }
        sr?.setRecognitionListener(listener)
        return sr != null
    }

    /** نگه‌داشتن دکمه — شنیدن پیوسته تا رهاکردن */
    fun start() {
        held = true; finishing = false; best = ""; busyErrs = 0
        Speaker.stop()
        onActive?.invoke(true)
        if (!ensure()) {
            held = false
            onActive?.invoke(false)
            onFinal?.invoke("")
            return
        }
        restart(0)
    }

    /** رهاکردن دکمه — مهلت کوتاه برای رسیدن نتیجهٔ نهایی، سپس پردازش */
    fun release() {
        if (!held) return
        held = false
        try { sr?.stopListening() } catch (ig: Throwable) {}
        // Recognition callbacks often arrive late after stopListening() on OEM devices.
        // Wait for onResults before finalizing so a valid spoken word is not lost.
        handler.postDelayed({ if (!held && !finishing) finish() }, 1400)
    }

    fun shutdown() {
        held = false
        handler.removeCallbacksAndMessages(null)
        try { sr?.destroy() } catch (ig: Throwable) {}
        sr = null
    }

    private fun restart(delay: Long) {
        handler.postDelayed({
            if (!held) { if (!finishing) finish(); return@postDelayed }
            try { sr?.cancel() } catch (ig: Throwable) {}
            try { sr?.startListening(buildIntent()) } catch (ig: Throwable) {
                handler.postDelayed({ if (held) restart(150) }, 150)
            }
        }, delay)
    }

    private fun finish() {
        if (finishing) return
        finishing = true
        try { sr?.cancel() } catch (ig: Throwable) {}
        onActive?.invoke(false)
        val text = best
        handler.post { onFinal?.invoke(text) }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(p0: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(p0: Float) {}
        override fun onBufferReceived(p0: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(p0: android.os.Bundle?) {
            val parts = try { p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) } catch (ig: Exception) { null }
            val p = parts?.firstOrNull { it.isNotBlank() } ?: return
            best = p
            onUpdate?.invoke(p)
        }
        override fun onResults(p0: android.os.Bundle?) {
            val res = try { p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) } catch (ig: Exception) { null }
            val scores = try { p0?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES) } catch (ig: Exception) { null }
            val candidates = res?.filter { it.isNotBlank() }.orEmpty()
            val picked = if (candidates.isNotEmpty() && scores != null && scores.size >= candidates.size) {
                candidates.indices.maxByOrNull { scores[it] }?.let { candidates[it] }
            } else candidates.firstOrNull()
            picked?.let { best = it; onUpdate?.invoke(it) }
            // If released, finalize using the best available final or partial transcript.
            if (held) restart(120) else finish()
        }
        override fun onError(err: Int) {
            // Some speech services report NO_MATCH/TIMEOUT after a useful partial result.
            if (!held) { finish(); return }
            when {
                err == SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> { if (++busyErrs < 30) restart(400) else finish() }
                err == SpeechRecognizer.ERROR_NO_MATCH || err == SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> restart(150)
                err == SpeechRecognizer.ERROR_CLIENT -> {
                    try { sr?.destroy() } catch (ig: Throwable) {}
                    sr = null
                    if (ensure()) restart(150) else finish()
                }
                err == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    held = false; finish()
                    UI.toast(act, "اجازهٔ میکروفن داده نشده است")
                }
                else -> restart(200)
            }
        }
        override fun onEvent(p0: Int, p1: android.os.Bundle?) {}
    }
}

/* ═══════════ باتن‌شیت صوتی ═══════════ */

fun MainActivity.openVoice() {
    val needPerm = Build.VERSION.SDK_INT >= 23 &&
        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
    if (needPerm) {
        try {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 7001)
            UI.toast(this, "برای گفتار صوتی، اجازهٔ میکروفون را بدهید")
        } catch (e: Throwable) { voiceSheet(this) }
    } else voiceSheet(this)
}

private fun voiceBubble(c: Context, m: ChatMsg): View {
    val row = LinearLayout(c)
    row.orientation = LinearLayout.HORIZONTAL
    if (m.role == "user") row.gravity = Gravity.END
    if (m.role == "bot") row.addView(UI.iconBox(c, R.drawable.ic_mic, Color.WHITE, 0xFF7C3AED.toInt(), 34, 17, 12))
    else row.addView(UI.iconBox(c, R.drawable.ic_person, Color.WHITE, 0xFF0D9488.toInt(), 34, 17, 12))
    val bubble = LinearLayout(c)
    bubble.orientation = LinearLayout.VERTICAL
    bubble.background = UI.rounded(c, if (m.role == "bot") T.cardSoft else 0x1A059669.toInt(), 14)
    bubble.setPadding(UI.dp(c, 11), UI.dp(c, 9), UI.dp(c, 11), UI.dp(c, 9))
    val t = UI.text(c, m.text, 12, if (m.role == "bot") T.text else T.primary)
    t.setLineSpacing(0f, 1.22f)
    bubble.addView(t, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    if (m.actionGo.isNotEmpty()) {
        val abtn = UI.text(c, m.actionLabel.ifEmpty { "نمایش" } + " ←", 11, Color.WHITE, true)
        abtn.background = UI.gradient(c, T.primary, T.primaryD, 12)
        abtn.setPadding(UI.dp(c, 12), UI.dp(c, 7), UI.dp(c, 12), UI.dp(c, 8))
        bubble.addView(abtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 7) })
    }
    val bl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    bl.marginStart = UI.dp(c, 9)
    row.addView(bubble, bl)
    return row
}

private val VOICE_SUGGESTIONS = listOf(
    "برو روزنامه", "فاکتور فروش دو تا یخچال برای آقای رضایی", "کالا جدید ماست به قیمت پنجاه هزار تومان",
    "دریافت پنجاه هزار تومان از آقای رضایی", "برو انبار", "فروش این ماه چقدر بود؟",
    "مشتری جدید سارا", "سرچ یخچال", "یادآور تماس با علی تا فردا",
    "وقتی گفتم صندوق یعنی برو صندوق", "حذف کالای شیر", "برو گزارش سود و زیان"
)

fun MainActivity.voiceSheet(act: MainActivity) {
    Speaker.ensure(act)
    val bs = BottomSheet(act)
    val handler = Handler(Looper.getMainLooper())
    var destroyed = false
    var listening = false
    var autoListen = true
    var runsNoSpeech = 0
    // Keep the last partial transcript; some Android speech engines return an empty
    // final result or NO_MATCH even after showing a useful live transcript.
    var partialBest = ""

    // ارجاع‌های رو به جلو — بعداً مقدار می‌گیرند
    var handleText: ((String) -> Unit)? = null
    var startListen: (() -> Unit)? = null
    var stopListen: (() -> Unit)? = null
    var micBtn: FrameLayout? = null
    var micIdleBg: android.graphics.drawable.Drawable? = null
    // شناساگر گفتار — بیرون از بلوک شیت تعریف می‌شود تا در onDismissed هم آزاد شود
    var sr: SpeechRecognizer? = null

    bs.show("دستیار صوتی شمارا", "دستور بدهید — روی داده‌های خودتان اجرا می‌شود") { b ->

        /* ---- قهرمان: میکروفون بزرگ ---- */
        val hero = LinearLayout(act)
        hero.orientation = LinearLayout.VERTICAL
        hero.gravity = Gravity.CENTER_HORIZONTAL
        hero.background = UI.gradient(act, 0xFF7C3AED.toInt(), 0xFF4F46E5.toInt(), 22)
        hero.setPadding(UI.dp(act, 16), UI.dp(act, 16), UI.dp(act, 16), UI.dp(act, 14))

        val micWrap = FrameLayout(act)
        val glow = View(act)
        glow.background = UI.oval(act, 0x55FFFFFF)
        val glp = FrameLayout.LayoutParams(UI.dp(act, 92), UI.dp(act, 92))
        glp.gravity = Gravity.CENTER
        micWrap.addView(glow, glp)

        val mb = FrameLayout(act)
        val idleBg = UI.gradient(act, 0xFFFFFFFF.toInt(), 0xFFE8E2FF.toInt(), 32)
        micIdleBg = idleBg
        mb.background = idleBg
        val mic = UI.icon(act, R.drawable.ic_mic, 30, 0xFF7C3AED.toInt())
        val mlp = FrameLayout.LayoutParams(UI.dp(act, 30), UI.dp(act, 30))
        mlp.gravity = Gravity.CENTER
        mb.addView(mic, mlp)
        val mbp = FrameLayout.LayoutParams(UI.dp(act, 72), UI.dp(act, 72))
        mbp.gravity = Gravity.CENTER
        micWrap.addView(mb, mbp)
        micBtn = mb
        hero.addView(micWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val status = UI.text(act, "برای شروع، میکروفون را بزنید", 13, Color.WHITE, true)
        status.gravity = Gravity.CENTER
        hero.addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
        val live = UI.text(act, "…", 15, 0xFFEDE9FE.toInt(), true)
        live.gravity = Gravity.CENTER
        hero.addView(live, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 4) })
        live.visibility = View.GONE

        // کلیدهای حالت — فقط وضعیت را عوض می‌کنند، ارجاعی جلو ندارند
        val toggles = LinearLayout(act)
        toggles.orientation = LinearLayout.HORIZONTAL
        toggles.gravity = Gravity.CENTER
        fun toggleChip(label: String, on0: Boolean, bgOn: Int, onClick: (Boolean) -> Unit): TextView {
            val t = UI.text(act, label, 11, Color.WHITE, true)
            var on = on0
            fun bg() { t.background = UI.rounded(act, if (on) bgOn else 0x33FFFFFF, 16) }
            bg()
            t.setPadding(UI.dp(act, 12), UI.dp(act, 7), UI.dp(act, 12), UI.dp(act, 8))
            t.setOnClickListener { on = !on; bg(); onClick(on) }
            toggles.addView(t, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = UI.dp(act, 8) })
            return t
        }
        toggleChip("صدای پاسخ: روشن", true, 0x66FFFFFF) { on -> Speaker.muted = !on; if (!on) Speaker.stop() }
        toggleChip("گفتگوی پیوسته: روشن", true, 0x66FFFFFF) { on -> autoListen = on }
        hero.addView(toggles, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 12) })
        b.addView(hero)

        /* ---- گزارش گفتگو ---- */
        val log = LinearLayout(act)
        log.orientation = LinearLayout.VERTICAL
        b.addView(log, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
        fun append(m: ChatMsg) {
            log.addView(voiceBubble(act, m), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 8) })
        }
        // پیوستگی صوت و متن — آخرین تاریخچهٔ هوشیار هم دیده می‌شود
        for (m in act.chatMsgs.takeLast(4)) append(m)

        /* ---- پیشنهادهای صوتی ---- */
        val sug = HorizontalScroll(act)
        for (s in VOICE_SUGGESTIONS) {
            val chip = UI.text(act, s, 11, T.violet, true)
            chip.background = UI.stroke(act, T.violetSoft, 16, 0x447C3AED.toInt(), 1)
            chip.setPadding(UI.dp(act, 11), UI.dp(act, 7), UI.dp(act, 11), UI.dp(act, 8))
            chip.setOnClickListener { handleText?.invoke(s) }
            sug.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = UI.dp(act, 7) })
        }
        b.addView(sug, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(act, 42)).also { it.topMargin = UI.dp(act, 10) })

        /* ---- ورودی تایپی جایگزین ---- */
        val inputRow = LinearLayout(act)
        inputRow.orientation = LinearLayout.HORIZONTAL
        inputRow.gravity = Gravity.CENTER_VERTICAL
        val (box, e) = UI.search(act, "یا بنویسید و اینتر بزنید…")
        (box.layoutParams as? LinearLayout.LayoutParams)?.topMargin = 0
        inputRow.addView(box, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val send = UI.iconBox(act, R.drawable.ic_send, Color.WHITE, 0xFF7C3AED.toInt(), 44, 20, 14)
        send.setOnClickListener { val s = e.text.toString().trim(); if (s.isNotEmpty()) { e.setText(""); handleText?.invoke(s) } }
        e.setOnEditorActionListener { _, _, _ ->
            val s = e.text.toString().trim()
            if (s.isNotEmpty()) { e.setText(""); handleText?.invoke(s) }
            true
        }
        val sl = LinearLayout.LayoutParams(UI.dp(act, 44), UI.dp(act, 44))
        sl.marginStart = UI.dp(act, 8)
        send.layoutParams = sl
        inputRow.addView(send)
        b.addView(inputRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })

        /* ---- شنیدن (SpeechRecognizer بومی fa-IR) ---- */
        val ri = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, act.packageName)
            // ⚡ درمان «حساسیت زیاد و قطع‌وصل»: آستانهٔ سکوت را بلند می‌کنیم تا سرویس گفتار
            // وسط حرف کاربر و با هر توقف کوتاه، جلسه را قطع نکند
            try {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            } catch (ig: Throwable) {}
        }
        // ⚠ ریشهٔ «گفتار در دسترس نیست»: isRecognitionAvailable روی برخی رام‌ها (مثل MIUI)
        // حتی با وجود سرویس گفتار، false برمی‌گرداند. پس مستقیم می‌سازیم؛ اگر واقعاً سرویس نبود null می‌شود.
        // اگر نشد، شناساگر روی‌دستگاه اندروید ۱۲+ را هم امتحان می‌کنیم.
        sr = try { SpeechRecognizer.createSpeechRecognizer(act) } catch (ig: Throwable) { null }
        if (sr != null) ErrorReport.note(act, "voice:stt", "standard recognizer created")
        if (sr == null && Build.VERSION.SDK_INT >= 31) {
            try {
                sr = SpeechRecognizer.createOnDeviceSpeechRecognizer(act)
                ErrorReport.note(act, "voice:stt", "on-device recognizer created")
            } catch (ig: Throwable) { sr = null }
        }
        if (sr == null) ErrorReport.note(act, "voice:stt", "NO recognition service on this device")
        val speechReady = sr != null
        var clientErrs = 0

        // کارت ترمیم گفتار — وقتی سرویس گفتار گوشی خاموش/حذف است، راه فعال‌سازی را یک‌لمسه باز می‌کنیم
        if (!speechReady) {
            val fix = LinearLayout(act)
            fix.orientation = LinearLayout.VERTICAL
            fix.background = UI.rounded(act, T.amberSoft, 16)
            fix.setPadding(UI.dp(act, 12), UI.dp(act, 10), UI.dp(act, 12), UI.dp(act, 12))
            val ft = UI.text(act, "سرویس گفتار این گوشی خاموش یا حذف شده است. با دکمهٔ زیر آن را روشن کنید؛ تا آن زمان می‌توانید دستورها را تایپ کنید.", 11.5f, T.amber, true)
            ft.setLineSpacing(0f, 1.2f)
            fix.addView(ft)
            val frow = LinearLayout(act)
            frow.orientation = LinearLayout.HORIZONTAL
            frow.gravity = Gravity.CENTER_VERTICAL
            val flp0 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            flp0.topMargin = UI.dp(act, 8)
            frow.addView(UI.outlineBtn(act, "تنظیمات ورودی صوتی", icRes = R.drawable.ic_gear) {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS))
                } catch (ig: Throwable) {
                    try {
                        startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                    } catch (ig2: Throwable) { UI.toast(act, "تنظیمات این گوشی باز نشد") }
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val s0 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            s0.marginStart = UI.dp(act, 8)
            frow.addView(UI.primaryBtn(act, "بررسی دوباره", icRes = R.drawable.ic_rotate) {
                try { bs.close() } catch (ig: Throwable) {}
                handler.postDelayed({ openVoice() }, 350)
            }, s0)
            fix.addView(frow, flp0)
            b.addView(fix, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(act, 10) })
        }

        fun idleState(msg: String) {
            listening = false
            status.text = msg
            live.visibility = View.GONE
            glow.clearAnimation()
            try { micBtn?.background = micIdleBg } catch (ig: Throwable) {}
        }

        // منطق یک دستور — پردازش مشترک با میکروفن فوتر (پارس، اجرا، یادگیری، صدا)
        handleText = { text ->
            if (!destroyed && text.isNotBlank()) {
                stopListen?.invoke()
                val (bot, intent) = act.processVoiceCommand(text)
                val um = ChatMsg(Db.newId("m"), "user", text)
                append(um); append(bot)

                val formOpened = bot.text.startsWith("فرم")
                if (intent.kind in setOf("nav", "search", "backup", "item_del", "partner_del", "check_new") || formOpened) {
                    handler.postDelayed({
                        if (!destroyed) {
                            destroyed = true
                            try { bs.close() } catch (ig: Throwable) {}
                        }
                    }, 700)
                    if (intent.kind == "search") {
                        handler.postDelayed({
                            try { globalSearchSheet(act, intent.query) } catch (ig: Throwable) {}
                        }, 800)
                    }
                } else if (autoListen) {
                    // ⚡ درمان «قطع‌وصل»: میکروفن تا تمام‌شدن صدای پاسخ دستیار باز نمی‌شود —
                    // وگرنه دستیار صدای خودش را می‌شنود و بی‌دلیل حساس می‌شود
                    runsNoSpeech = 0
                    fun wake(tries: Int) {
                        if (destroyed || !autoListen) return
                        if (Speaker.speaking && tries < 50) { handler.postDelayed({ wake(tries + 1) }, 500); return }
                        handler.postDelayed({ if (!destroyed && !listening) startListen?.invoke() }, 400)
                    }
                    wake(0)
                }
            }
        }

        startListen = {
            if (!destroyed && !listening) {
                val rec = sr
                if (rec == null) {
                    status.text = "گفتار صوتی روی این گوشی در دسترس نیست — می‌توانید تایپ کنید"
                } else {
                    try {
                        listening = true
                        partialBest = ""
                        // ⚠ شمارندهٔ سکوت اینجا صفر نمی‌شود — فقط با شنیدن دستور واقعی صفر می‌شود
                        status.text = "گوش می‌دهم… بفرمایید"
                        live.visibility = View.VISIBLE
                        live.text = "…"
                        val pulse = ScaleAnimation(1f, 1.22f, 1f, 1.22f,
                            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
                            duration = 700; repeatMode = Animation.REVERSE; repeatCount = Animation.INFINITE
                        }
                        glow.startAnimation(pulse)
                        try { micBtn?.background = UI.gradient(act, 0xFF10B981.toInt(), 0xFF047857.toInt(), 32) } catch (ig: Throwable) {}
                        rec.startListening(ri)
                    } catch (err: Throwable) {
                        idleState("شروع گفتار ممکن نشد — دوباره بزنید یا تایپ کنید")
                    }
                }
            }
        }

        stopListen = {
            try { sr?.stopListening() } catch (ig: Throwable) {}
            try { sr?.cancel() } catch (ig: Throwable) {}
            listening = false
            glow.clearAnimation()
        }

        mb.setOnClickListener {
            Speaker.stop()
            if (listening) stopListen?.invoke() else startListen?.invoke()
        }

        sr?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: android.os.Bundle?) { status.text = "گوش می‌دهم… بفرمایید" }
            override fun onBeginningOfSpeech() { status.text = "دارید می‌گویید…" }
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "در حال پردازش…" }
            override fun onError(err: Int) {
                if ((err == SpeechRecognizer.ERROR_NO_MATCH || err == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) && partialBest.isNotBlank()) {
                    val fallbackText = partialBest.trim()
                    partialBest = ""
                    idleState("متن دریافت شد؛ در حال اجرا…")
                    handleText?.invoke(fallbackText)
                    return
                }
                // ⚡ گفتگوی پیوسته: سکوت یعنی ادامهٔ گوش‌دادن، نه قطع — رابط نمی‌پرد و
                // چراغ میکروفن خاموش نمی‌شود؛ کاربر دیگر «قطع‌وصل» نمی‌بیند
                if ((err == SpeechRecognizer.ERROR_NO_MATCH || err == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) &&
                    autoListen && runsNoSpeech < 100) {
                    runsNoSpeech++
                    status.text = "گوش می‌دهم… بفرمایید"
                    handler.postDelayed({ if (!destroyed && !listening && autoListen) startListen?.invoke() }, 650)
                    return
                }
                val msg = when (err) {
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_NETWORK -> "شبکهٔ گوشی قطع است — گفتار به اینترنت نیاز دارد. می‌توانید تایپ کنید."
                    SpeechRecognizer.ERROR_NO_MATCH -> "چیزی نشنیدم — دوباره بفرمایید"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "گفتگوی پیوسته متوقف شد — برای ادامه میکروفن را بزنید"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "اجازهٔ میکروفن داده نشده است"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "یک لحظه…"
                    SpeechRecognizer.ERROR_CLIENT -> "سرویس گفتار خطا داد — خودکار تازه‌سازی می‌کنم…"
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "زبان فارسی برای گفتار روی این گوشی نصب نیست — از تنظیمات ورودی صوتی فارسی را اضافه کنید"
                    else -> "گفتار در دسترس نیست — می‌توانید تایپ کنید"
                }
                idleState(msg)
                // شلوغی سرویس — بعد از یک ثانیه خودکار ادامه می‌دهیم
                if (err == SpeechRecognizer.ERROR_RECOGNIZER_BUSY && autoListen) {
                    handler.postDelayed({
                        if (!destroyed && !listening) {
                            try { sr?.cancel() } catch (ig: Throwable) {}
                            startListen?.invoke()
                        }
                    }, 1000)
                }
                // خطای سرویس (ERROR_CLIENT) — شناساگر را تازه‌سازی و دوباره وصل می‌کنیم
                if (err == SpeechRecognizer.ERROR_CLIENT && clientErrs < 2) {
                    clientErrs++
                    try { sr?.destroy() } catch (ig: Throwable) {}
                    sr = try { SpeechRecognizer.createSpeechRecognizer(act) } catch (ig: Throwable) { null }
                    sr?.setRecognitionListener(this)
                    handler.postDelayed({ if (!destroyed && !listening) startListen?.invoke() }, 500)
                }
            }
            override fun onPartialResults(p0: android.os.Bundle?) {
                try {
                    val parts = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val candidate = parts?.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                    if (candidate.isNotEmpty()) { partialBest = candidate; live.visibility = View.VISIBLE; live.text = candidate }
                } catch (ig: Exception) {}
            }
            override fun onResults(p0: android.os.Bundle?) {
                try {
                    val res = p0?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = res?.firstOrNull { it.isNotBlank() }?.trim().orEmpty().ifEmpty { partialBest.trim() }
                    partialBest = ""
                    idleState(if (best.isEmpty()) "چیزی نشنیدم — دوباره بفرمایید" else "شنیدم!")
                    if (best.isNotEmpty()) handleText?.invoke(best)
                    else if (autoListen && runsNoSpeech < 100) {
                        runsNoSpeech++
                        handler.postDelayed({ if (!destroyed && !listening) startListen?.invoke() }, 700)
                    }
                } catch (err: Throwable) { idleState("پردازش گفتار با خطا مواجه شد") }
            }
            override fun onEvent(p0: Int, p1: android.os.Bundle?) {}
        })

        // شروع خودکار — شیت که باز شد خودش گوش می‌دهد
        handler.postDelayed({ startListen?.invoke() }, 450)
    }

    // پاکسازی کامل هنگام بستن شیت — هیچ منبعی بی‌صاحب نمی‌ماند
    // ⚠ نشت واقعی: در نسخه‌های قبل شناساگر گفتار هرگز destroy نمی‌شد و بعد از چند بار
    // باز کردن، سرویس مشغول/بی‌پاسخ می‌شد. حالا شنیدن، انیمیشن و شناساگر همه آزاد می‌شوند.
    bs.onDismissed = {
        destroyed = true
        handler.removeCallbacksAndMessages(null)
        try { stopListen?.invoke() } catch (ig: Throwable) {}
        try { sr?.destroy() } catch (ig: Throwable) {}
        sr = null
        Speaker.stop()
    }
}
