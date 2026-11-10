package ir.moamelo.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

private const val PW = 1080
private const val PH = 1350
private const val PHOTO_H = 700
private const val FOOTER_H = 200

private val P_BG = 0xFFF4F8F5.toInt()
private val P_INK = 0xFF12241B.toInt()
private val P_MUTED = 0xFF5F7569.toInt()
private val P_GREEN = 0xFF0F7B54.toInt()
private val P_GREEN_DARK = 0xFF0B5A3E.toInt()
private val P_GOLD = 0xFFF0C87A.toInt()
private val P_WHITE = 0xFFFFFFFF.toInt()

private fun tp(size: Float, color: Int, bold: Boolean = false): TextPaint {
    val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
    p.textSize = size
    p.color = color
    p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    return p
}

private fun makeLayout(text: String, paint: TextPaint, width: Int, maxLines: Int): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setTextDirection(TextDirectionHeuristics.RTL)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()

private fun Canvas.drawLayoutAt(l: StaticLayout, x: Float, y: Float) {
    save()
    translate(x, y)
    l.draw(this)
    restore()
}

private fun decodeScaled(bytes: ByteArray, maxSide: Int): Bitmap? {
    val b = BitmapFactory.Options()
    b.inJustDecodeBounds = true
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, b)
    var sample = 1
    while (maxOf(b.outWidth, b.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val o = BitmapFactory.Options()
    o.inSampleSize = sample
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
}

fun propertyCaption(p: JSONObject, phone: String?): String = buildString {
    append(p.str("title") ?: "")
    append("\n").append(locationText(p))
    append("\n").append(priceText(p))
    val d = detailsText(p)
    if (d.isNotEmpty()) append("\n").append(d)
    if (!phone.isNullOrBlank()) append("\n\nتماس: ").append(phone.toFaDigits())
    append("\nارسال شده از معاملو")
}

fun shareText(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "text/plain"
    i.putExtra(Intent.EXTRA_TEXT, text)
    ctx.startActivity(Intent.createChooser(i, "اشتراک‌گذاری ملک"))
}

fun sharePosterFile(ctx: Context, file: File, caption: String) {
    val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
    val i = Intent(Intent.ACTION_SEND)
    i.type = "image/jpeg"
    i.putExtra(Intent.EXTRA_STREAM, uri)
    i.putExtra(Intent.EXTRA_TEXT, caption)
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(i, "اشتراک‌گذاری ملک"))
}

// Draws a 1080x1350 poster (photo, price, details, logo, agent contact) and saves it as JPEG
suspend fun buildPoster(
    ctx: Context,
    p: JSONObject,
    imageUrl: String?,
    agentName: String?,
    agency: String?,
    phone: String?,
    watermark: Boolean = false
): File? = withContext(Dispatchers.IO) {
    try {
        val bmp = Bitmap.createBitmap(PW, PH, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(P_BG)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val logo = BitmapFactory.decodeResource(ctx.resources, R.drawable.logo)
        val isSale = p.str("listing_type") == "sale"

        // ---- photo area with rounded bottom corners ----
        val photoBytes = if (imageUrl != null) Api.download(Api.imageUrl(imageUrl)) else null
        val photo = if (photoBytes != null) decodeScaled(photoBytes, 1400) else null
        val clip = Path()
        clip.addRoundRect(
            RectF(0f, 0f, PW.toFloat(), PHOTO_H.toFloat()),
            floatArrayOf(0f, 0f, 0f, 0f, 56f, 56f, 56f, 56f),
            Path.Direction.CW
        )
        c.save()
        c.clipPath(clip)
        if (photo != null) {
            val target = PW.toFloat() / PHOTO_H
            val ratio = photo.width.toFloat() / photo.height
            val src = if (ratio > target) {
                val nw = (photo.height * target).toInt()
                Rect((photo.width - nw) / 2, 0, (photo.width - nw) / 2 + nw, photo.height)
            } else {
                val nh = (photo.width / target).toInt()
                Rect(0, (photo.height - nh) / 2, photo.width, (photo.height - nh) / 2 + nh)
            }
            c.drawBitmap(photo, src, RectF(0f, 0f, PW.toFloat(), PHOTO_H.toFloat()), fill)
        } else {
            val g = Paint(Paint.ANTI_ALIAS_FLAG)
            g.shader = LinearGradient(0f, 0f, PW.toFloat(), PHOTO_H.toFloat(), P_GREEN, P_GREEN_DARK, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, PW.toFloat(), PHOTO_H.toFloat(), g)
            val s = 380
            c.drawBitmap(logo, null, RectF((PW - s) / 2f, (PHOTO_H - s) / 2f, (PW + s) / 2f, (PHOTO_H + s) / 2f), fill)
        }
        c.restore()

        // ---- watermark for the free plan ----
        if (watermark) {
            val wp = tp(130f, 0x66FFFFFF, true)
            val ww = wp.measureText("معاملو")
            c.save()
            c.rotate(-25f, PW / 2f, PHOTO_H / 2f)
            c.drawText("معاملو", (PW - ww) / 2f, PHOTO_H / 2f + 45f, wp)
            c.restore()
        }

        // ---- logo badge (top-left) ----
        val badge = Paint(Paint.ANTI_ALIAS_FLAG)
        badge.color = P_WHITE
        c.drawCircle(124f, 124f, 70f, badge)
        c.drawBitmap(logo, null, RectF(84f, 84f, 164f, 164f), fill)

        // ---- type pill (top-right) ----
        val pillText = tp(40f, P_WHITE, true)
        val label = if (isSale) "فروش" else "رهن و اجاره"
        val tw = pillText.measureText(label)
        val pill = RectF(PW - 56f - tw - 64f, 64f, PW - 56f, 148f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        pillPaint.color = P_GREEN
        c.drawRoundRect(pill, 42f, 42f, pillPaint)
        c.drawText(label, pill.left + 32f, pill.top + 57f, pillText)

        // ---- info block ----
        val x = 56f
        val w = PW - 112
        var y = PHOTO_H + 48f

        val title = p.str("title") ?: ""
        val tl = makeLayout(title, tp(52f, P_INK, true), w, 2)
        c.drawLayoutAt(tl, x, y)
        y += tl.height + 12f

        val typeLabel = labelOf(PROPERTY_TYPES, p.str("property_type"))
        val loc = typeLabel + " • " + locationText(p)
        val ll = makeLayout(loc, tp(36f, P_MUTED), w, 1)
        c.drawLayoutAt(ll, x, y)
        y += ll.height + 12f

        if (isSale) {
            val pl = makeLayout(formatMoney(p.longOrNull("price")), tp(62f, P_GREEN, true), w, 1)
            c.drawLayoutAt(pl, x, y)
            y += pl.height + 4f
            val words = p.longOrNull("price")?.let { moneyWords(it) } ?: ""
            if (words.isNotEmpty()) {
                val wl = makeLayout(words, tp(32f, P_MUTED), w, 1)
                c.drawLayoutAt(wl, x, y)
                y += wl.height + 10f
            }
        } else {
            val d1 = makeLayout("ودیعه: " + formatMoney(p.longOrNull("deposit")), tp(46f, P_GREEN, true), w, 1)
            c.drawLayoutAt(d1, x, y)
            y += d1.height + 6f
            val d2 = makeLayout("اجاره ماهانه: " + formatMoney(p.longOrNull("monthly_rent")), tp(46f, P_GREEN, true), w, 1)
            c.drawLayoutAt(d2, x, y)
            y += d2.height + 10f
        }

        val parts = mutableListOf<String>()
        val d = detailsText(p)
        if (d.isNotEmpty()) parts.add(d)
        if (p.optBoolean("has_elevator")) parts.add("آسانسور")
        if (p.optBoolean("has_parking")) parts.add("پارکینگ")
        if (p.optBoolean("has_storage")) parts.add("انباری")
        if (p.optBoolean("has_balcony")) parts.add("بالکن")
        if (parts.isNotEmpty()) {
            val dl = makeLayout(parts.joinToString(" • "), tp(34f, P_INK), w, 1)
            c.drawLayoutAt(dl, x, y)
        }

        // ---- footer: agent contact + brand ----
        val fy = PH - FOOTER_H.toFloat()
        val bar = Paint(Paint.ANTI_ALIAS_FLAG)
        bar.color = P_GREEN
        c.drawRect(0f, fy, PW.toFloat(), PH.toFloat(), bar)

        val tx = PW - 56f - 640f
        var ty = fy + 30f
        if (!agentName.isNullOrBlank()) {
            val al = makeLayout(agentName, tp(46f, P_WHITE, true), 640, 1)
            c.drawLayoutAt(al, tx, ty)
            ty += al.height + 2f
        }
        if (!agency.isNullOrBlank()) {
            val gl = makeLayout(agency, tp(32f, 0xE6FFFFFF.toInt()), 640, 1)
            c.drawLayoutAt(gl, tx, ty)
            ty += gl.height + 6f
        }
        if (!phone.isNullOrBlank()) {
            val ph = makeLayout(phone.toFaDigits(), tp(50f, P_GOLD, true), 640, 1)
            c.drawLayoutAt(ph, tx, ty)
        }

        c.drawCircle(150f, fy + 82f, 56f, badge)
        c.drawBitmap(logo, null, RectF(112f, fy + 44f, 188f, fy + 120f), fill)
        val brand = tp(30f, P_WHITE, true)
        val bw = brand.measureText("معاملو")
        c.drawText("معاملو", 150f - bw / 2f, fy + 172f, brand)

        val dir = File(ctx.cacheDir, "shared")
        dir.mkdirs()
        val file = File(dir, "poster_" + p.optInt("id") + ".jpg")
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        file
    } catch (e: Exception) {
        null
    }
}
