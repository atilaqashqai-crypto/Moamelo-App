package ir.moamelo.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private const val FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

fun String.toFaDigits(): String = map { if (it in '0'..'9') FA_DIGITS[it - '0'] else it }.joinToString("")

fun String.toEnDigits(): String = map {
    when (it) {
        in '۰'..'۹' -> '0' + (it - '۰')
        in '٠'..'٩' -> '0' + (it - '٠')
        else -> it
    }
}.joinToString("")

fun faNum(n: Number?): String = (n?.toString() ?: "").toFaDigits()

fun formatMoney(v: Long?): String {
    if (v == null) return "—"
    return (String.format(Locale.US, "%,d", v) + " تومان").toFaDigits()
}

fun moneyWords(v: Long): String {
    if (v <= 0L) return ""
    val b = v / 1_000_000_000L
    val m = (v % 1_000_000_000L) / 1_000_000L
    val t = (v % 1_000_000L) / 1_000L
    val parts = mutableListOf<String>()
    if (b > 0) parts.add("$b میلیارد")
    if (m > 0) parts.add("$m میلیون")
    if (t > 0) parts.add("$t هزار")
    if (parts.isEmpty()) parts.add("$v")
    return (parts.joinToString(" و ") + " تومان").toFaDigits()
}

// ---------- Jalali date ----------
fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
    var jy = -1595 + (33 * (days / 12053))
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + days / 31
        jd = 1 + days % 31
    } else {
        jm = 7 + (days - 186) / 30
        jd = 1 + (days - 186) % 30
    }
    return Triple(jy, jm, jd)
}

// "2026-11-09T10:00:00.000Z" -> "۱۴۰۵/۰۸/۱۸"
fun jalali(iso: String?): String {
    if (iso == null || iso.length < 10) return "—"
    return try {
        val y = iso.substring(0, 4).toInt()
        val m = iso.substring(5, 7).toInt()
        val d = iso.substring(8, 10).toInt()
        val (jy, jm, jd) = gregorianToJalali(y, m, d)
        String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd).toFaDigits()
    } catch (e: Exception) {
        "—"
    }
}

// ---------- JSON helpers ----------
fun JSONObject.str(k: String): String? = if (!has(k) || isNull(k)) null else optString(k)
fun JSONObject.longOrNull(k: String): Long? = if (!has(k) || isNull(k)) null else optLong(k)
fun JSONObject.intOrNull(k: String): Int? = if (!has(k) || isNull(k)) null else optInt(k)
fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

// ---------- Labels ----------
val PROPERTY_TYPES = listOf(
    "apartment" to "آپارتمان", "villa" to "ویلا", "land" to "زمین",
    "commercial" to "تجاری", "office" to "اداری", "other" to "سایر"
)
val CUSTOMER_TYPES = listOf("buyer" to "خریدار", "tenant" to "مستأجر", "seller" to "فروشنده", "landlord" to "موجر")
val INTERACTION_KINDS = listOf("note" to "یادداشت", "call" to "تماس", "visit" to "بازدید")

fun propertyStatuses(listingType: String): List<Pair<String, String>> =
    if (listingType == "sale") listOf(
        "active" to "فعال", "negotiating" to "در حال مذاکره", "sold" to "فروخته شد", "expired" to "منقضی", "archived" to "بایگانی"
    ) else listOf(
        "active" to "فعال", "negotiating" to "در حال مذاکره", "rented" to "اجاره رفت", "expired" to "منقضی", "archived" to "بایگانی"
    )

fun labelOf(list: List<Pair<String, String>>, key: String?): String = list.firstOrNull { it.first == key }?.second ?: (key ?: "—")

fun priceText(p: JSONObject): String =
    if (p.str("listing_type") == "sale") formatMoney(p.longOrNull("price"))
    else "ودیعه ${formatMoney(p.longOrNull("deposit"))} | اجاره ${formatMoney(p.longOrNull("monthly_rent"))}"

fun locationText(p: JSONObject): String {
    val parts = listOfNotNull(p.str("city"), p.str("neighborhood")).filter { it.isNotBlank() }
    return if (parts.isEmpty()) "—" else parts.joinToString("، ")
}

fun detailsText(p: JSONObject): String {
    val parts = mutableListOf<String>()
    p.intOrNull("area")?.let { parts.add("${faNum(it)} متر") }
    p.intOrNull("bedrooms")?.let { parts.add("${faNum(it)} خوابه") }
    return parts.joinToString(" • ")
}

const val MIN_SALE_PRICE = 1_000_000_000L
const val SALE_PRICE_MSG =
    "قیمت ملک فروش باید حداقل ۱,۰۰۰,۰۰۰,۰۰۰ تومان (یک میلیارد) باشد. قیمت ملک را به تومان و با تمام صفرها وارد کنید. مثال: ۱۰,۰۰۰,۰۰۰,۰۰۰"
