package ir.shomara.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * سیستم طراحی شمارا — مو‌به‌مو مطابق bits.tsx نسخهٔ وب
 * با پشتیبانی پوستهٔ روشن و تیره
 */
object T {
    var dark = false

    val bg get() = if (dark) 0xFF0B0F14.toInt() else 0xFFF6F7F9.toInt()
    val card get() = if (dark) 0xFF141A21.toInt() else 0xFFFFFFFF.toInt()
    val cardSoft get() = if (dark) 0xFF1A2027.toInt() else 0xFFF0F2F5.toInt() // accent
    val border get() = if (dark) 0xFF252C35.toInt() else 0xFFE9ECEF.toInt()
    val text get() = if (dark) 0xFFE8EDF3.toInt() else 0xFF141A22.toInt()
    val sub get() = if (dark) 0xFF98A4B1.toInt() else 0xFF697482.toInt()
    val input get() = if (dark) 0xFF1E252E.toInt() else 0xFFF2F4F7.toInt()
    val scrim get() = 0x66000000

    val primary get() = 0xFF059669.toInt()          // emerald-600
    val primaryD get() = 0xFF0F766E.toInt()         // teal-700
    val primarySoft get() = if (dark) 0xFF103B2F.toInt() else 0xFFE7F5EF.toInt()

    val amber get() = 0xFFF59E0B.toInt(); val amberSoft = if (dark) 0xFF3D2E10.toInt() else 0xFFFEF3E0.toInt()
    val rose get() = 0xFFE11D48.toInt();  val roseSoft = if (dark) 0xFF3B1220.toInt() else 0xFFFDECEF.toInt()
    val sky get() = 0xFF0284C7.toInt();   val skySoft = if (dark) 0xFF0E2A3D.toInt() else 0xFFE8F4FC.toInt()
    val violet get() = 0xFF7C3AED.toInt(); val violetSoft = if (dark) 0xFF251A40.toInt() else 0xFFF1EBFF.toInt()
    val lime get() = 0xFF65A30D.toInt();  val limeSoft = if (dark) 0xFF1F2E10.toInt() else 0xFFF0F7E4.toInt()
    val slate get() = if (dark) 0xFF64748B.toInt() else 0xFF64748B.toInt()

    fun tone(t: String): Int = when (t) {
        "amber" -> amber; "rose" -> rose; "sky" -> sky; "violet" -> violet
        "lime" -> lime; "slate" -> slate; else -> primary
    }
    fun toneSoft(t: String): Int = when (t) {
        "amber" -> amberSoft; "rose" -> roseSoft; "sky" -> skySoft; "violet" -> violetSoft
        "lime" -> limeSoft; "slate" -> if (dark) 0xFF232A33.toInt() else 0xFFF2F4F7.toInt(); else -> primarySoft
    }
}

object U {
    var fontR: Typeface? = null
    var fontB: Typeface? = null

    fun initFonts(c: Context) {
        try { fontR = Typeface.createFromAsset(c.assets, "fonts/Vazirmatn-Regular.ttf") } catch (ig: Exception) {}
        try { fontB = Typeface.createFromAsset(c.assets, "fonts/Vazirmatn-Bold.ttf") } catch (ig: Exception) {}
    }
}

object UI {

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    // ---------- shapes ----------

    fun rounded(c: Context, color: Int, rDp: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(c, rDp).toFloat()
        return g
    }

    fun stroke(c: Context, color: Int, rDp: Int, strokeColor: Int, sDp: Int = 1): GradientDrawable {
        val g = rounded(c, color, rDp)
        g.setStroke(dp(c, sDp), strokeColor)
        return g
    }

    fun gradient(c: Context, start: Int, end: Int, rDp: Int): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end))
        g.cornerRadius = dp(c, rDp).toFloat()
        return g
    }

    /** افکت لمس (موج) روی هر پس‌زمینه — بازخورد لمسی مثل نسخهٔ وب */
    fun ripple(c: Context, bg: android.graphics.drawable.Drawable): android.graphics.drawable.RippleDrawable {
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x339AA6B1), bg, null)
    }

    fun oval(c: Context, color: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.shape = GradientDrawable.OVAL
        return g
    }

    // ---------- basic views ----------

    fun text(c: Context, s: CharSequence, sp: Int, color: Int, bold: Boolean = false): TextView =
        textImpl(c, s, sp.toFloat(), color, bold)

    fun text(c: Context, s: CharSequence, sp: Float, color: Int, bold: Boolean = false): TextView =
        textImpl(c, s, sp, color, bold)

    fun textImpl(c: Context, s: CharSequence, sp: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(c)
        t.text = s
        t.textSize = sp
        t.setTextColor(color)
        t.typeface = if (bold) (U.fontB ?: Typeface.DEFAULT_BOLD) else (U.fontR ?: Typeface.DEFAULT)
        return t
    }

    fun icon(c: Context, res: Int, sizeDp: Int, tint: Int): ImageView {
        val iv = ImageView(c)
        try { iv.setImageResource(res); iv.setColorFilter(tint) } catch (ig: Exception) {}
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        iv.layoutParams = LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp))
        return iv
    }

    /** مربع گرد با آیکون وسط — مثل span با bg-primary/10 */
    fun iconBox(c: Context, res: Int, tint: Int, soft: Int, box: Int, icSize: Int, radius: Int = 12): FrameLayout {
        val f = FrameLayout(c)
        f.background = rounded(c, soft, radius)
        val iv = icon(c, res, icSize, tint)
        val lp = FrameLayout.LayoutParams(dp(c, icSize), dp(c, icSize))
        lp.gravity = Gravity.CENTER
        f.addView(iv, lp)
        f.layoutParams = LinearLayout.LayoutParams(dp(c, box), dp(c, box))
        return f
    }

    fun sep(c: Context): View {
        val v = View(c)
        v.setBackgroundColor(T.border)
        return v
    }

    fun toast(c: Context, msg: String) = Toast.makeText(c, msg, Toast.LENGTH_SHORT).show()

    fun copyText(c: Context, s: String, msg: String = "کپی شد") {
        try {
            val cm = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("shm", s))
            toast(c, msg)
        } catch (ig: Exception) { toast(c, "کپی نشد") }
    }

    fun shareText(c: Context, s: String) {
        try {
            val i = android.content.Intent(android.content.Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(android.content.Intent.EXTRA_TEXT, s)
            c.startActivity(android.content.Intent.createChooser(i, "اشتراک‌گذاری"))
        } catch (ig: Exception) { toast(c, "امکان اشتراک‌گذاری نیست") }
    }

    // ---------- کارت شیشه‌ای (glass-card) ----------

    fun card(c: Context, topMargin: Int = 10): LinearLayout {
        val v = LinearLayout(c)
        v.orientation = LinearLayout.VERTICAL
        v.background = ripple(c, stroke(c, T.card, 18, T.border, 1))
        // عمق ملایم — مثل سایهٔ کارت‌های نسخهٔ وب (در پوستهٔ تیره بی‌اثر است)
        v.elevation = dp(c, 2).toFloat()
        v.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        v.setPadding(dp(c, 15), dp(c, 14), dp(c, 15), dp(c, 14))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(c, topMargin)
        v.layoutParams = lp
        return v
    }

    fun innerCard(c: Context, bg: Int = T.cardSoft, radius: Int = 14, pad: Int = 10): LinearLayout {
        val v = LinearLayout(c)
        v.orientation = LinearLayout.VERTICAL
        v.background = rounded(c, bg, radius)
        v.setPadding(dp(c, pad), dp(c, pad), dp(c, pad), dp(c, pad))
        return v
    }

    fun sectionTitle(c: Context, title: String, sub: String = "", icRes: Int = 0, action: View? = null, topMargin: Int = 16): LinearLayout {
        val v = LinearLayout(c)
        v.orientation = LinearLayout.HORIZONTAL
        v.gravity = Gravity.CENTER_VERTICAL
        if (icRes != 0) {
            v.addView(iconBox(c, icRes, T.primary, T.primarySoft, 40, 20))
            val lp0 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp0.marginStart = dp(c, 12)
            // spacing handled below
        }
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.addView(text(c, title, 15, T.text, true))
        if (sub.isNotEmpty()) {
            val s = text(c, sub, 11, T.sub)
            val slp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            slp.topMargin = dp(c, 2)
            col.addView(s, slp)
        }
        val clp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        clp.marginStart = dp(c, 12)
        v.addView(col, clp)
        if (action != null) v.addView(action)
        val wl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        wl.topMargin = dp(c, topMargin)
        wl.bottomMargin = dp(c, 4)
        v.layoutParams = wl
        return v
    }

    // ---------- چیپ ----------

    fun chip(c: Context, txt: String, tone: String = "green", withIcon: Int = 0): LinearLayout {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = rounded(c, T.toneSoft(tone), 20)
        row.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4))
        if (withIcon != 0) {
            row.addView(icon(c, withIcon, 11, T.tone(tone)))
            val il = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            il.marginEnd = dp(c, 4)
            (withIcon)
        }
        val t = text(c, txt, 10, T.tone(tone), true)
        row.addView(t)
        return row
    }

    // ---------- کارت آماری (StatCard) ----------

    fun statCard(c: Context, label: String, value: String, hint: String = "", tone: String = "green", icRes: Int = 0, click: (() -> Unit)? = null): LinearLayout {
        val v = LinearLayout(c)
        v.orientation = LinearLayout.VERTICAL
        v.background = ripple(c, stroke(c, T.card, 18, T.border, 1))
        v.elevation = dp(c, 2).toFloat()
        v.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        v.setPadding(dp(c, 14), dp(c, 13), dp(c, 14), dp(c, 13))
        val head = LinearLayout(c)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(UI.text(c, label, 11, T.sub, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (icRes != 0) head.addView(iconBox(c, icRes, T.tone(tone), T.toneSoft(tone), 32, 16, 10))
        v.addView(head)
        val valT = text(c, value, 19, T.text, true)
        val vlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        vlp.topMargin = dp(c, 8)
        v.addView(valT, vlp)
        if (hint.isNotEmpty()) {
            val h = text(c, hint, 10, T.sub)
            val hlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            hlp.topMargin = dp(c, 2)
            v.addView(h, hlp)
        }
        if (click != null) v.setOnClickListener { click() }
        return v
    }

    fun grid2(c: Context, a: View, b: View, gap: Int = 10): LinearLayout {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val alp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        a.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        row.addView(a, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        val gl = LinearLayout.LayoutParams(dp(c, gap), ViewGroup.LayoutParams.MATCH_PARENT)
        row.addView(View(c), gl)
        b.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        row.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        val wrap = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        wrap.topMargin = dp(c, 10)
        row.layoutParams = wrap
        return row
    }

    /**
     * ردیف آمار داخل بنرهای گرادیانی — تراز کامل عمودی/افقی + فاصلهٔ یکنواخت بین خانه‌ها.
     * قبلاً خانه‌ها بدون فاصله اضافه می‌شدند و قاب‌ها به هم می‌چسبیدند.
     */
    fun heroStats(c: Context, cells: List<Pair<String, String>>, labelColor: Int = 0xD9ECFDF5.toInt(), gap: Int = 6): LinearLayout {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        for ((i, sv) in cells.withIndex()) {
            val cell = LinearLayout(c)
            cell.orientation = LinearLayout.VERTICAL
            cell.gravity = Gravity.CENTER
            cell.background = stroke(c, 0x1FFFFFFF.toInt(), 14, 0x33FFFFFF.toInt(), 1)
            cell.setPadding(dp(c, 6), dp(c, 9), dp(c, 6), dp(c, 9))
            val v = text(c, sv.first, 14, Color.WHITE, true)
            v.gravity = Gravity.CENTER
            cell.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            val lb = text(c, sv.second, 9, labelColor)
            lb.gravity = Gravity.CENTER
            cell.addView(lb, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(c, 3) })
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            if (i > 0) lp.marginStart = dp(c, gap)
            row.addView(cell, lp)
        }
        return row
    }

    // ---------- ردیف لیست (ListRow) ----------

    fun listRow(
        c: Context,
        title: String,
        sub: String = "",
        end: View? = null,
        badge: LinearLayout? = null,
        icRes: Int = 0,
        icTint: Int = -1,
        click: (() -> Unit)? = null,
        chev: Boolean = true,
        extra: View? = null
    ): LinearLayout {
        val v = LinearLayout(c)
        v.orientation = LinearLayout.VERTICAL
        v.background = ripple(c, stroke(c, T.card, 18, T.border, 1))
        v.setPadding(dp(c, 13), dp(c, 13), dp(c, 13), dp(c, 13))
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        if (icRes != 0) {
            row.addView(iconBox(c, icRes, if (icTint == -1) T.sub else icTint, T.cardSoft, 40, 20))
            val il = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            il.marginStart = dp(c, 12)
        }
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        val titleRow = LinearLayout(c)
        titleRow.orientation = LinearLayout.HORIZONTAL
        titleRow.gravity = Gravity.CENTER_VERTICAL
        val t = text(c, title, 13, T.text, true)
        titleRow.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (badge != null) titleRow.addView(badge)
        col.addView(titleRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        if (sub.isNotEmpty()) {
            val s = text(c, sub, 11, T.sub)
            val slp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            slp.topMargin = dp(c, 2)
            col.addView(s, slp)
        }
        val clp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        clp.marginStart = dp(c, 12)
        row.addView(col, clp)
        if (end != null) row.addView(end)
        if (click != null && chev) row.addView(icon(c, R.drawable.ic_chev_l, 15, 0x889AA6B1.toInt()))
        v.addView(row)
        if (extra != null) {
            val elp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            elp.topMargin = dp(c, 8)
            v.addView(extra, elp)
        }
        if (click != null) v.setOnClickListener { click() }
        return v
    }

    /** ستون «انتهای ردیف» — عدد راست‌چین */
    fun endCol(c: Context, main: String, sub: String = "", mainColor: Int = T.text, mainSize: Int = 13): LinearLayout =
        endColF(c, main, sub, mainColor, mainSize.toFloat())


    fun endColF(c: Context, main: String, sub: String = "", mainColor: Int = T.text, mainSize: Float = 13f): LinearLayout {
        val v = LinearLayout(c)
        v.orientation = LinearLayout.VERTICAL
        v.gravity = Gravity.START
        v.addView(text(c, main, mainSize, mainColor, true))
        if (sub.isNotEmpty()) {
            val s = text(c, sub, 9, T.sub)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(c, 1)
            v.addView(s, lp)
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = dp(c, 4)
        v.layoutParams = lp
        return v
    }

    // ---------- حالت خالی ----------

    fun empty(c: Context, msg: String, sub: String = "", icRes: Int = R.drawable.ic_doc): View {
        val v = LinearLayout(c)
        v.orientation = LinearLayout.VERTICAL
        v.gravity = Gravity.CENTER
        v.setPadding(dp(c, 20), dp(c, 36), dp(c, 20), dp(c, 20))
        v.addView(iconBox(c, icRes, 0x88697482.toInt(), T.cardSoft, 56, 24, 20))
        val t = text(c, msg, 13, T.sub, true)
        t.gravity = Gravity.CENTER
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(c, 12)
        v.addView(t, lp)
        if (sub.isNotEmpty()) {
            val s = text(c, sub, 11, T.sub)
            s.gravity = Gravity.CENTER
            val slp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            slp.topMargin = dp(c, 4)
            v.addView(s, slp)
        }
        return v
    }

    // ---------- جستجو ----------

    fun search(c: Context, hint: String = "جستجو…"): Pair<LinearLayout, EditText> {
        val box = LinearLayout(c)
        box.orientation = LinearLayout.HORIZONTAL
        box.gravity = Gravity.CENTER_VERTICAL
        box.background = ripple(c, stroke(c, T.card, 16, T.border, 1))
        box.setPadding(dp(c, 12), dp(c, 9), dp(c, 12), dp(c, 9))
        box.addView(icon(c, R.drawable.ic_search, 17, T.sub))
        val e = EditText(c)
        e.background = null
        e.hint = hint
        e.textSize = 13f
        e.setTextColor(T.text)
        e.setHintTextColor(if (T.dark) 0xFF5C6875.toInt() else 0xFFAAB2BD.toInt())
        e.typeface = U.fontR ?: Typeface.DEFAULT
        e.isSingleLine = true
        val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp.marginStart = dp(c, 8)
        box.addView(e, lp)
        // دکمهٔ پاک‌کردن ✕ — مثل نسخهٔ وب
        val x = icon(c, R.drawable.ic_close, 15, T.sub)
        x.visibility = View.GONE
        x.setOnClickListener {
            e.setText("")
            e.requestFocus()
        }
        box.addView(x, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = dp(c, 6) })
        e.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { x.visibility = if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE }
        })
        val wlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        wlp.topMargin = dp(c, 10)
        box.layoutParams = wlp
        return box to e
    }

    // ---------- تب‌های قرصی (Segmented) ----------

    fun seg(
        c: Context,
        options: List<Triple<String, Int, String>>, // label, count(-1=بدون), value
        selected: Int,
        horizontal: Boolean = true,
        onPick: (Int) -> Unit = {}
    ): LinearLayout {
        var cur = selected
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        val buttons = ArrayList<LinearLayout>()
        fun restyle() {
            for (i in buttons.indices) {
                val t = buttons[i]
                val lbl = t.findViewWithTag("sl") as? TextView
                val cnt = t.findViewWithTag("sc") as? TextView
                if (i == cur) {
                    t.background = gradient(c, T.primary, T.primaryD, 16)
                    lbl?.setTextColor(Color.WHITE)
                    cnt?.setBackgroundColor(0x40FFFFFF)
                    cnt?.setTextColor(Color.WHITE)
                } else {
                    t.background = rounded(c, if (T.dark) 0xFF1E252E.toInt() else 0xFFF0F2F5.toInt(), 16)
                    lbl?.setTextColor(T.sub)
                    cnt?.setBackgroundColor(if (T.dark) 0xFF2A323C.toInt() else 0xFFE2E6EB.toInt())
                    cnt?.setTextColor(T.sub)
                }
            }
        }
        for ((i, o) in options.withIndex()) {
            val t = LinearLayout(c)
            t.orientation = LinearLayout.HORIZONTAL
            t.gravity = Gravity.CENTER_VERTICAL
            val lbl = text(c, o.first, 12, T.sub, true)
            lbl.tag = "sl"
            t.addView(lbl)
            if (o.second >= 0) {
                val cnt = text(c, J.faN(o.second), 9, T.sub, true)
                cnt.setPadding(dp(c, 6), dp(c, 1), dp(c, 6), dp(c, 1))
                cnt.background = rounded(c, 0xFFE2E6EB.toInt(), 20)
                cnt.tag = "sc"
                t.addView(cnt, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginStart = dp(c, 5) })
            }
            t.setPadding(dp(c, 13), dp(c, 8), dp(c, 13), dp(c, 8))
            t.setOnClickListener { cur = i; restyle(); onPick(i) }
            buttons.add(t)
            row.addView(t, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { if (i > 0) it.marginStart = dp(c, 6) })
        }
        restyle()
        return row
    }

    fun segScroll(c: Context, options: List<Triple<String, Int, String>>, selected: Int, onPick: (Int) -> Unit): HorizontalScroll {
        // اسکرول افقی ضدکرش — همهٔ HSVهای برنامه از کلاس امن HorizontalScroll می‌روند
        val hs = HorizontalScroll(c)
        hs.addView(seg(c, options, selected, onPick = onPick),
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(c, 10)
        hs.layoutParams = lp
        return hs
    }

    // ---------- دکمه‌ها ----------

    fun primaryBtn(c: Context, txt: String, icRes: Int = 0, click: () -> Unit): TextView {
        val t = text(c, txt, 13, Color.WHITE, true)
        t.gravity = Gravity.CENTER
        t.background = ripple(c, gradient(c, T.primary, T.primaryD, 16))
        t.setPadding(dp(c, 14), dp(c, 13), dp(c, 14), dp(c, 13))
        if (icRes != 0) {
            t.setCompoundDrawablesWithIntrinsicBounds(icRes, 0, 0, 0)
            try { t.compoundDrawables[0]?.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN) } catch (ig: Exception) {}
            t.compoundDrawablePadding = dp(c, 6)
        }
        t.setOnClickListener { click() }
        return t
    }

    fun outlineBtn(c: Context, txt: String, danger: Boolean = false, icRes: Int = 0, click: () -> Unit): TextView {
        val color = if (danger) T.rose else T.primary
        val soft = if (danger) T.roseSoft else T.primarySoft
        val t = text(c, txt, 13, color, true)
        t.gravity = Gravity.CENTER
        t.background = ripple(c, rounded(c, soft, 16))
        t.setPadding(dp(c, 14), dp(c, 13), dp(c, 14), dp(c, 13))
        if (icRes != 0) {
            t.setCompoundDrawablesWithIntrinsicBounds(icRes, 0, 0, 0)
            try { t.compoundDrawables[0]?.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN) } catch (ig: Exception) {}
            t.compoundDrawablePadding = dp(c, 6)
        }
        t.setOnClickListener { click() }
        return t
    }

    fun btnRow(c: Context, a: View, b: View, gap: Int = 8): LinearLayout {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        val wlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        wlp.topMargin = dp(c, 12)
        row.layoutParams = wlp
        row.addView(a, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val gl = LinearLayout.LayoutParams(dp(c, gap), ViewGroup.LayoutParams.MATCH_PARENT)
        row.addView(View(c), gl)
        row.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        return row
    }

    // ---------- فیلدهای فرم ----------

    fun field(c: Context, label: String, value: String, hint: String = "", num: Boolean = false, ltr: Boolean = false, center: Boolean = false): Pair<LinearLayout, EditText> {
        val box = LinearLayout(c)
        box.orientation = LinearLayout.VERTICAL
        box.background = rounded(c, T.input, 16)
        box.setPadding(dp(c, 13), dp(c, 9), dp(c, 13), dp(c, 10))
        box.addView(text(c, label, 11, T.sub, true))
        val e = EditText(c)
        e.background = null
        e.textSize = 13.5f
        e.setTextColor(T.text)
        e.setHintTextColor(if (T.dark) 0xFF5C6875.toInt() else 0xFFAAB2BD.toInt())
        e.typeface = U.fontR ?: Typeface.DEFAULT
        if (num) e.inputType = InputType.TYPE_CLASS_NUMBER
        if (ltr) e.textDirection = View.TEXT_DIRECTION_LTR
        if (center) e.gravity = Gravity.CENTER
        e.setText(value)
        if (hint.isNotEmpty()) e.hint = hint
        box.addView(e)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(c, 10)
        box.layoutParams = lp
        return box to e
    }

    /** فیلد چندخطی (Textarea) — برای توضیحات و آدرس */
    fun fieldMulti(c: Context, label: String, value: String, hint: String = ""): Pair<LinearLayout, EditText> {
        val pair = field(c, label, value, hint)
        val e = pair.second
        e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        e.isSingleLine = false
        e.minLines = 2
        e.gravity = Gravity.TOP or Gravity.START
        return pair
    }

    /** فیلد عددی با جداکنندهٔ هزارگان و پسوند */
    fun numField(c: Context, label: String, value: Long, suffix: String = "", hint: String = "", allowNeg: Boolean = false): LinearLayout {
        val pair = field(c, label, if (value != 0L) J.moneyFa(value) else "", hint, num = true, ltr = true, center = true)
        if (allowNeg) pair.second.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        if (suffix.isNotEmpty()) {
            val sfx = text(c, suffix, 9, T.sub)
            sfx.gravity = Gravity.START
            pair.first.addView(sfx, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return pair.first
    }

    /** فیلد انتخاب با باتن‌شیت (درایورها همه باتن‌شیت هستند) */
    fun selectField(c: Context, label: String, value: String, open: () -> Unit): LinearLayout {
        val box = LinearLayout(c)
        box.orientation = LinearLayout.VERTICAL
        box.background = rounded(c, T.input, 16)
        box.setPadding(dp(c, 13), dp(c, 9), dp(c, 13), dp(c, 10))
        box.addView(text(c, label, 11, T.sub, true))
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val v = text(c, value, 13.5f, T.text, true)
        v.tag = "vv"
        row.addView(v, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(icon(c, R.drawable.ic_chev_d, 14, T.sub))
        box.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(c, 2) })
        box.setOnClickListener { open() }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(c, 10)
        box.layoutParams = lp
        return box
    }

    /** فیلد تاریخ جلالی — با لمس باتن‌شیت تقویم باز می‌شود */
    fun dateField(c: Context, label: String, value: String, open: () -> Unit): LinearLayout {
        val box = LinearLayout(c)
        box.orientation = LinearLayout.VERTICAL
        box.background = rounded(c, T.input, 16)
        box.setPadding(dp(c, 13), dp(c, 9), dp(c, 13), dp(c, 10))
        box.addView(text(c, label, 11, T.sub, true))
        val v = text(c, J.jFa(value), 13.5f, T.text, true)
        v.tag = "dv"
        box.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = dp(c, 2) })
        box.setOnClickListener { open() }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(c, 10)
        box.layoutParams = lp
        return box
    }

    /** کنترل تعداد (QtyStepper) — فیلد میانی تایپ‌پذیر، مثل نسخهٔ وب */
    fun qtyStepper(c: Context, value: Double, onChange: (Double) -> Unit): LinearLayout {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        row.background = rounded(c, T.cardSoft, 14)
        row.setPadding(dp(c, 3), dp(c, 3), dp(c, 3), dp(c, 3))
        var cur = value
        fun mk(txt: String, bg: Int, fg: Int, click: () -> Unit): TextView {
            val t = text(c, txt, 14, fg, true)
            t.gravity = Gravity.CENTER
            t.background = ripple(c, rounded(c, bg, 12))
            t.setPadding(0, 0, 0, dp(c, 2))
            t.setOnClickListener { click() }
            return t
        }
        val num = EditText(c)
        num.background = null
        num.textSize = 13f
        num.setTextColor(T.text)
        num.typeface = U.fontB ?: Typeface.DEFAULT_BOLD
        num.gravity = Gravity.CENTER
        num.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        num.minWidth = dp(c, 40)
        num.tag = "qv"
        num.isSingleLine = true
        num.setText(J.qty(value))
        // هر تغییر (دکمه یا تایپ) یک‌بار onChange می‌فرستد — مقدارِ لحظه‌ای
        num.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c2: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val p = J.parseNum(s?.toString() ?: "")
                if (p != cur) { cur = p; onChange(p) }
            }
        })
        val minus = mk("−", T.card, T.text) { num.setText(J.qty(Math.max(0.0, cur - 1))) }
        row.addView(minus, LinearLayout.LayoutParams(dp(c, 30), dp(c, 30)))
        row.addView(num, LinearLayout.LayoutParams(dp(c, 42), dp(c, 30)))
        val plus = mk("+", T.primary, Color.WHITE) { num.setText(J.qty(cur + 1)) }
        row.addView(plus, LinearLayout.LayoutParams(dp(c, 30), dp(c, 30)))
        return row
    }

    fun switchRow(c: Context, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val box = LinearLayout(c)
        box.orientation = LinearLayout.HORIZONTAL
        box.gravity = Gravity.CENTER_VERTICAL
        box.background = rounded(c, T.cardSoft, 16)
        box.setPadding(dp(c, 13), dp(c, 12), dp(c, 13), dp(c, 12))
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.addView(text(c, title, 12, T.text, true))
        if (sub.isNotEmpty()) col.addView(text(c, sub, 10, T.sub))
        box.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(makeSwitch(c, checked, onChange))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(c, 10)
        box.layoutParams = lp
        return box
    }

    /** سوییچ سادهٔ بومی */
    fun makeSwitch(c: Context, checked: Boolean, onChange: (Boolean) -> Unit): FrameLayout {
        val track = FrameLayout(c)
        fun bgOf(on: Boolean) = rounded(c, if (on) T.primary else (if (T.dark) 0xFF3A4450.toInt() else 0xFFD8DDE3.toInt()), 20)
        track.background = bgOf(checked)
        val w = dp(c, 46); val h = dp(c, 26)
        val knob = View(c)
        knob.background = oval(c, Color.WHITE)
        knob.translationX = if (checked) dp(c, 20).toFloat() else 0f
        val klp = FrameLayout.LayoutParams(dp(c, 20), dp(c, 20))
        klp.gravity = Gravity.CENTER_VERTICAL
        klp.marginStart = dp(c, 3)
        track.addView(knob, klp)
        var on = checked
        track.setOnClickListener {
            on = !on
            track.background = bgOf(on)
            knob.animate().translationX(if (on) dp(c, 20).toFloat() else 0f).setDuration(150).start()
            onChange(on)
        }
        track.layoutParams = LinearLayout.LayoutParams(w, h)
        return track
    }

    fun kv(c: Context, k: String, v: String, vBold: Boolean = true, vColor: Int = T.text): LinearLayout {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val t1 = text(c, k, 12, T.sub)
        row.addView(t1, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(text(c, v, 13, vColor, vBold))
        return row
    }

    fun spacer(c: Context, h: Int): View {
        val v = View(c)
        v.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, h))
        return v
    }
}

/* ═════════════════════════ نمودارها ═════════════════════════ */

/** نمودار میله‌ای عمودی — MiniBars */
class MiniBarsView(c: Context, private val data: List<Triple<String, Long, Boolean>>, private val heightDp: Int = 120) : LinearLayout(c) {
    init {
        orientation = VERTICAL
        val max = Math.max(data.maxOfOrNull { it.second } ?: 1L, 1L)
        val inner = LinearLayout(c)
        inner.orientation = HORIZONTAL
        for (d in data) {
            val col = LinearLayout(c)
            col.orientation = VERTICAL
            col.gravity = Gravity.CENTER or Gravity.BOTTOM
            val hFull = UI.dp(c, heightDp - 26)
            val barH = if (d.second > 0) Math.max(UI.dp(c, 8), (d.second * hFull / max).toInt()) else UI.dp(c, 2)
            val valT = TextView(c)
            valT.text = if (d.second > 0) J.moneyShort(d.second) else ""
            valT.textSize = 8.5f
            valT.setTextColor(T.sub)
            valT.typeface = U.fontB ?: Typeface.DEFAULT_BOLD
            valT.gravity = Gravity.CENTER
            valT.layoutParams = LinearLayout.LayoutParams(UI.dp(c, 30), ViewGroup.LayoutParams.WRAP_CONTENT)
            col.addView(valT)
            val bar = View(c)
            bar.background = if (d.third) UI.gradient(c, 0xFFFBBF24.toInt(), 0xFFF59E0B.toInt(), 6) else UI.gradient(c, T.primary, T.primaryD, 6)
            col.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barH).also { it.topMargin = UI.dp(c, 2) })
            val lbl = UI.text(c, d.first, 8.5f, T.sub)
            lbl.gravity = Gravity.CENTER
            lbl.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 3) }
            col.addView(lbl)
            inner.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(inner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}

/** نمودار جریان نقدی دوگانه — DualBars */
class DualBarsView(c: Context, private val data: List<Triple<String, Long, Long>>, private val heightDp: Int = 150) : LinearLayout(c) {
    init {
        orientation = VERTICAL
        val max = Math.max(data.maxOfOrNull { Math.max(it.second, it.third) } ?: 1L, 1L)
        val inner = LinearLayout(c)
        inner.orientation = HORIZONTAL
        for (d in data) {
            val col = LinearLayout(c)
            col.orientation = VERTICAL
            col.gravity = Gravity.CENTER or Gravity.BOTTOM
            val hFull = UI.dp(c, heightDp - 30)
            val pair = LinearLayout(c)
            pair.orientation = HORIZONTAL
            pair.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            fun bar(v: Long, grad: IntArray): View {
                val b = View(c)
                val g = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, grad)
                g.cornerRadius = UI.dp(c, 4).toFloat()
                b.background = g
                val h = if (v > 0) Math.max(UI.dp(c, 4), (v * hFull / max).toInt()) else UI.dp(c, 3)
                pair.addView(b, LinearLayout.LayoutParams(UI.dp(c, 8), h).also { it.marginEnd = UI.dp(c, 2) })
                return b
            }
            bar(d.second, intArrayOf(T.primary, 0xFF2DD4BF.toInt()))
            bar(d.third, intArrayOf(0xFFF43F5E.toInt(), 0xFFFB923C.toInt()))
            col.addView(pair, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hFull))
            val lbl = UI.text(c, d.first, 8.5f, T.sub)
            lbl.gravity = Gravity.CENTER
            lbl.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 3) }
            col.addView(lbl)
            inner.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(inner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        // legend
        val leg = LinearLayout(c)
        leg.orientation = HORIZONTAL
        leg.gravity = Gravity.CENTER
        fun legendItem(color: Int, label: String) {
            val row = LinearLayout(c)
            row.orientation = HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val dot = View(c)
            val g = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(color, color))
            g.shape = GradientDrawable.OVAL
            dot.background = g
            row.addView(dot, LinearLayout.LayoutParams(UI.dp(c, 10), UI.dp(c, 10)).also { it.marginEnd = UI.dp(c, 5) })
            row.addView(UI.text(c, label, 10, T.sub, true))
            leg.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.marginEnd = UI.dp(c, 18) })
        }
        legendItem(T.primary, "درآمد")
        legendItem(0xFFF43F5E.toInt(), "هزینه")
        addView(leg, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).also { it.topMargin = UI.dp(c, 10); it.gravity = Gravity.CENTER })
    }
}

/** نوار پیشرفت افقی — سنین مطالبات */
fun agingBar(c: Context, label: String, amount: Long, tone: String, pct: Int): LinearLayout {
    val v = LinearLayout(c)
    v.orientation = LinearLayout.VERTICAL
    val head = LinearLayout(c)
    head.orientation = LinearLayout.HORIZONTAL
    head.addView(UI.text(c, label, 11, T.sub, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    head.addView(UI.text(c, J.moneyShort(amount) + " تومان", 11, T.text, true))
    v.addView(head)
    val track = View(c)
    track.background = UI.rounded(c, if (T.dark) 0xFF232A33.toInt() else 0xFFE4E8EC.toInt(), 8)
    val fill = View(c)
    fill.background = UI.rounded(c, T.tone(tone), 8)
    val wrap = FrameLayout(c)
    wrap.addView(track, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 8)))
    wrap.addView(fill, FrameLayout.LayoutParams(UI.dp(c, Math.max(2, pct)), UI.dp(c, 8)))
    v.addView(wrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, UI.dp(c, 8)).also { it.topMargin = UI.dp(c, 6) })
    return v
}

/** خط روند (Spark) */
class SparkView(c: Context, private val points: List<Long>) : View(c) {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF10B981.toInt()
        strokeWidth = UI.dp(c, 2).toFloat()
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return
        val max = Math.max(points.max(), 1L)
        val min = Math.min(points.min(), 0L)
        val span = Math.max(max - min, 1L)
        val w = width.toFloat()
        val h = height.toFloat()
        val path = android.graphics.Path()
        for ((i, p) in points.withIndex()) {
            val x = i * w / (points.size - 1)
            val y = h - ((p - min) / span.toFloat()) * (h - UI.dp(context, 6)) - UI.dp(context, 3)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }
}

/** دونات (Donut) */
class DonutView(c: Context, private val data: List<Pair<Int, Long>>, private val thicknessDp: Int = 16) : View(c) {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        val total = data.sumOf { it.second }.coerceAtLeast(1L)
        val size = Math.min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val r = (size - UI.dp(context, thicknessDp)) / 2f
        val stroke = UI.dp(context, thicknessDp).toFloat()
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = stroke
        // پس‌زمینهٔ حلقه
        paint.color = if (T.dark) 0xFF232A33.toInt() else 0xFFE9EDF1.toInt()
        canvas.drawCircle(cx, cy, r, paint)
        var start = -90f
        for ((color, v) in data) {
            if (v <= 0) continue
            val sweep = (v * 360f / total)
            paint.color = color
            canvas.drawArc(cx - r, cy - r, cx + r, cy + r, start, sweep, false, paint)
            start += sweep
        }
    }
}

/** نگهدارندهٔ مقدار نمایشی فیلدهای انتخاب‌گر */
class ValueLabel {
    var current: String = ""
    private var tv: TextView? = null
    fun set(s: String) {
        current = s
        tv?.text = s
    }
    fun bind(parent: LinearLayout, initial: String) {
        current = initial
        tv = parent.findViewWithTag("vv") ?: parent.findViewWithTag("dv")
        tv?.text = initial
    }
}
