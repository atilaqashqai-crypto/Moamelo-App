package ir.moamelo.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun PropertyCard(p: JSONObject, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val cover = p.str("cover_image")
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Soft),
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) {
                    RemoteImage(cover, Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.Apartment, contentDescription = null, tint = Brand)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.str("title") ?: "", color = Ink, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(locationText(p), color = Muted, fontSize = 12.sp, maxLines = 1)
                Text(priceText(p), color = Brand, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                val d = detailsText(p)
                if (d.isNotEmpty()) Text(d, color = Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (p.optBoolean("is_favorite")) Icon(Icons.Filled.Favorite, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                Spacer(Modifier.height(4.dp))
                val st = p.str("status")
                Badge2(labelOf(propertyStatuses(p.str("listing_type") ?: "sale"), st), if (st == "active") Green else Muted)
            }
        }
    }
}

@Composable
fun CustomerMatchRow(m: JSONObject, onClick: () -> Unit) {
    val c = m.getJSONObject("customer")
    val reasons = m.optJSONArray("reasons")?.strings() ?: emptyList()
    val mismatches = m.optJSONArray("mismatches")?.strings() ?: emptyList()
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.str("full_name") ?: "", color = Ink, fontWeight = FontWeight.Bold)
                Text((c.str("phone") ?: "").toFaDigits(), color = Muted, fontSize = 12.sp)
                val budget = if (c.str("customer_type") == "buyer") {
                    c.longOrNull("max_budget")?.let { "بودجه تا ${formatMoney(it)}" }
                } else {
                    val d = c.longOrNull("max_deposit")?.let { "ودیعه تا ${formatMoney(it)}" }
                    val r = c.longOrNull("max_rent")?.let { "اجاره تا ${formatMoney(it)}" }
                    listOfNotNull(d, r).joinToString(" | ").ifEmpty { null }
                }
                if (budget != null) Text(budget, color = Brand, fontSize = 12.sp)
            }
            ScoreBadge(m.optInt("score"))
        }
        reasons.take(3).forEach { Text("✓ $it", color = Green, fontSize = 12.sp) }
        mismatches.take(2).forEach { Text("! $it", color = Warn, fontSize = 12.sp) }
    }
}

@Composable
fun PropertyListScreen(nav: NavController, type: String) {
    val isSale = type == "sale"
    var q by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("date") }
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(q, status, sort, reload) {
        loading = true
        var path = "/properties?listing_type=$type&sort=${if (sort == "price" && !isSale) "rent" else sort}"
        if (status.isNotEmpty()) path += "&status=$status"
        if (q.isNotBlank()) path += "&q=${Api.enc(q.trim())}"
        val r = Api.call("GET", path)
        if (r.ok) {
            list = r.json.optJSONArray("properties")?.objects() ?: emptyList()
            error = null
        } else error = r.error
        loading = false
    }

    Scaffold(
        containerColor = Bg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("property_form/$type/0") },
                containerColor = Brand,
                contentColor = OnBrand
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("ملک جدید", fontWeight = FontWeight.Bold)
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            ScreenHeader(if (isSale) "املاک فروش" else "رهن و اجاره", onBack = { nav.popBackStack() })
            Column(Modifier.padding(horizontal = 16.dp)) {
                AppField(q, { q = it }, "جستجو (عنوان، آدرس، محله، مالک)")
                ChipRow {
                    SelectChip("همه", status == "") { status = "" }
                    propertyStatuses(type).forEach { s -> SelectChip(s.second, status == s.first) { status = s.first } }
                }
                ChipRow {
                    SelectChip("جدیدترین", sort == "date") { sort = "date" }
                    SelectChip(if (isSale) "قیمت" else "اجاره", sort == "price") { sort = "price" }
                    SelectChip("متراژ", sort == "area") { sort = "area" }
                }
            }
            if (loading) LoadingBox()
            error?.let { ErrorBox(it) { reload++ } }
            if (!loading && error == null && list.isEmpty()) EmptyBox("هنوز ملکی ثبت نشده است.\nبا دکمه «ملک جدید» شروع کنید.")
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list) { p -> PropertyCard(p) { nav.navigate("property/${p.optInt("id")}") } }
            }
        }
    }
}

@Composable
fun PropertyDetailScreen(nav: NavController, id: Int) {
    var p by remember { mutableStateOf<JSONObject?>(null) }
    var images by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var custMatches by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var viewer by remember { mutableStateOf<String?>(null) }
    var shareDialog by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    LaunchedEffect(reload) {
        val r = Api.call("GET", "/properties/$id")
        if (r.ok) {
            p = r.json.optJSONObject("property")
            images = r.json.optJSONArray("images")?.objects() ?: emptyList()
            error = null
            val mr = Api.call("GET", "/properties/$id/matches")
            if (mr.ok) custMatches = mr.json.optJSONArray("matches")?.objects() ?: emptyList()
        } else error = r.error
        loading = false
    }

    val prop = p
    val type = prop?.str("listing_type") ?: "sale"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("جزئیات ملک", onBack = { nav.popBackStack() }) {
            if (prop != null) {
                IconButton(onClick = {
                    scope.launch { Api.call("PATCH", "/properties/$id/favorite"); reload++ }
                }) {
                    Icon(
                        if (prop.optBoolean("is_favorite")) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "علاقه‌مندی", tint = Brand
                    )
                }
                IconButton(onClick = { shareDialog = true }) { Icon(Icons.Filled.Share, contentDescription = "اشتراک‌گذاری", tint = Brand) }
                IconButton(onClick = { nav.navigate("property_form/$type/$id") }) {
                    Icon(Icons.Filled.Edit, contentDescription = "ویرایش", tint = Brand)
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = Danger)
                }
            }
        }
        if (loading) LoadingBox()
        error?.let { ErrorBox(it) { reload++ } }
        if (prop != null && images.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(images) { img ->
                    val u = img.str("url") ?: ""
                    RemoteImage(
                        u,
                        Modifier.width(300.dp).height(210.dp).clip(RoundedCornerShape(18.dp)).clickable { viewer = u }
                    )
                }
            }
        }
        if (prop != null) {
            Column(Modifier.padding(16.dp)) {
                AppCard {
                    Text(prop.str("title") ?: "", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(locationText(prop), color = Muted)
                    Spacer(Modifier.height(8.dp))
                    if (type == "sale") {
                        Text(formatMoney(prop.longOrNull("price")), color = Brand, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        prop.longOrNull("price")?.let { Text(moneyWords(it), color = Muted, fontSize = 12.sp) }
                    } else {
                        Text("ودیعه: ${formatMoney(prop.longOrNull("deposit"))}", color = Brand, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("اجاره ماهانه: ${formatMoney(prop.longOrNull("monthly_rent"))}", color = Brand, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (prop.optBoolean("negotiable")) Text("ودیعه و اجاره قابل مذاکره است", color = Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                SectionTitle("وضعیت")
                ChipRow {
                    propertyStatuses(type).forEach { s ->
                        SelectChip(s.second, prop.str("status") == s.first) {
                            scope.launch {
                                Api.call("PUT", "/properties/$id", JSONObject().put("status", s.first))
                                reload++
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                AppCard {
                    InfoRow("نوع ملک", labelOf(PROPERTY_TYPES, prop.str("property_type")))
                    InfoRow("آدرس", prop.str("address"))
                    InfoRow("متراژ", prop.intOrNull("area")?.let { "${faNum(it)} متر" })
                    InfoRow("خواب", prop.intOrNull("bedrooms")?.let { faNum(it) })
                    val fl = prop.intOrNull("floor")
                    val tf = prop.intOrNull("total_floors")
                    InfoRow("طبقه", if (fl != null) "${faNum(fl)}${if (tf != null) " از ${faNum(tf)}" else ""}" else null)
                    InfoRow("سال ساخت", prop.intOrNull("build_year")?.let { faNum(it) })
                    val am = listOfNotNull(
                        if (prop.optBoolean("has_elevator")) "آسانسور" else null,
                        if (prop.optBoolean("has_parking")) "پارکینگ" else null,
                        if (prop.optBoolean("has_storage")) "انباری" else null,
                        if (prop.optBoolean("has_balcony")) "بالکن" else null
                    )
                    InfoRow("امکانات", if (am.isEmpty()) null else am.joinToString("، "))
                    InfoRow("وضعیت سند", prop.str("document_status"))
                    InfoRow("تاریخ تحویل", prop.str("delivery_date"))
                    InfoRow("مدت قرارداد", prop.intOrNull("contract_months")?.let { "${faNum(it)} ماه" })
                    InfoRow("توضیحات", prop.str("description"))
                    InfoRow("یادداشت", prop.str("notes"))
                    InfoRow("تاریخ ثبت", jalali(prop.str("created_at")))
                    InfoRow("آخرین ویرایش", jalali(prop.str("updated_at")))
                }
                SectionTitle(
                    (if (type == "sale") "خریداران مناسب این ملک" else "مستأجران مناسب این ملک") + " (${faNum(custMatches.size)})"
                )
                if (custMatches.isEmpty()) {
                    Text(
                        if (type == "sale") "فعلاً خریداری با مشخصات این ملک پیدا نشد. با ثبت مشتری جدید، سیستم خودکار بررسی می‌کند."
                        else "فعلاً مستأجری با مشخصات این ملک پیدا نشد. با ثبت مشتری جدید، سیستم خودکار بررسی می‌کند.",
                        color = Muted, fontSize = 13.sp
                    )
                }
                custMatches.forEach { m ->
                    CustomerMatchRow(m) { nav.navigate("customer/${m.getJSONObject("customer").optInt("id")}") }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(10.dp))
                val ownerName = prop.str("owner_name")
                val ownerPhone = prop.str("owner_phone")
                if (!ownerName.isNullOrBlank() || !ownerPhone.isNullOrBlank()) {
                    AppCard {
                        SectionTitle("مالک")
                        InfoRow("نام", ownerName)
                        InfoRow("موبایل", ownerPhone?.toFaDigits())
                        if (!ownerPhone.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$ownerPhone"))) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, tint = Brand)
                                Spacer(Modifier.width(8.dp))
                                Text("تماس با مالک", color = Brand)
                            }
                        }
                    }
                }
            }
        }
    }

    if (shareDialog && prop != null) {
        AlertDialog(
            onDismissRequest = { shareDialog = false },
            title = { Text("اشتراک‌گذاری ملک") },
            text = { Text("پوستر آماده با عکس، قیمت، لوگو و شماره تماس شما برای واتساپ، تلگرام و اینستاگرام ساخته می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    shareDialog = false
                    busy = true
                    scope.launch {
                        val me = Api.call("GET", "/auth/me").json.optJSONObject("user")
                        val wm = Api.call("GET", "/subscription").json.optJSONObject("plan")?.optBoolean("watermark", false) ?: false
                        val file = buildPoster(
                            ctx, prop, images.firstOrNull()?.str("url"),
                            me?.str("full_name"), me?.str("agency_name"), me?.str("phone"), wm
                        )
                        busy = false
                        if (file != null) sharePosterFile(ctx, file, propertyCaption(prop, me?.str("phone")))
                        else error = "ساخت پوستر ناموفق بود."
                    }
                }) { Text("ارسال پوستر", color = Brand) }
            },
            dismissButton = {
                TextButton(onClick = {
                    shareDialog = false
                    scope.launch {
                        val me = Api.call("GET", "/auth/me").json.optJSONObject("user")
                        shareText(ctx, propertyCaption(prop, me?.str("phone")))
                    }
                }) { Text("فقط متن") }
            }
        )
    }
    if (busy) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("در حال ساخت پوستر...") },
            text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) } }
        )
    }

    viewer?.let { u ->
        Dialog(onDismissRequest = { viewer = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                Modifier.fillMaxSize().background(Color.Black).clickable { viewer = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = Api.imageUrl(u),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف ملک") },
            text = { Text("این ملک برای همیشه حذف می‌شود. مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        val r = Api.call("DELETE", "/properties/$id")
                        if (r.ok) nav.popBackStack() else error = r.error
                    }
                }) { Text("حذف", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } }
        )
    }
}

private val TEXT_KEYS = listOf(
    "title", "city", "neighborhood", "address", "document_status", "delivery_date",
    "owner_name", "owner_phone", "description", "notes"
)
private val NUM_KEYS = listOf(
    "price", "deposit", "monthly_rent", "area", "bedrooms", "floor", "total_floors", "build_year", "contract_months"
)
private val FLAG_KEYS = listOf("negotiable", "has_elevator", "has_parking", "has_storage", "has_balcony")

@Composable
fun PropertyFormScreen(nav: NavController, type: String, id: Int) {
    val isRent = type == "rent"
    val text = remember { mutableStateMapOf<String, String>() }
    val flags = remember { mutableStateMapOf<String, Boolean>() }
    var propType by remember { mutableStateOf("apartment") }
    var status by remember { mutableStateOf("active") }
    var loading by remember { mutableStateOf(id != 0) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dupMsg by remember { mutableStateOf<String?>(null) }
    var matchedMsg by remember { mutableStateOf<String?>(null) }
    var infoMsg by remember { mutableStateOf<String?>(null) }
    var limitHit by remember { mutableStateOf(false) }
    var maxImgs by remember { mutableStateOf(12) }
    var existing by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    val picked = remember { mutableStateListOf<Uri>() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val room = maxOf(0, maxImgs - existing.size - picked.size)
        picked.addAll(uris.take(room))
    }

    fun t(k: String): String = text[k] ?: ""

    LaunchedEffect(Unit) {
        val sr = Api.call("GET", "/subscription")
        if (sr.ok) maxImgs = sr.json.optJSONObject("plan")?.optInt("max_images", 12) ?: 12
    }

    LaunchedEffect(Unit) {
        if (id != 0) {
            val r = Api.call("GET", "/properties/$id")
            val p = r.json.optJSONObject("property")
            if (r.ok && p != null) {
                for (k in TEXT_KEYS) p.str(k)?.let { text[k] = it }
                for (k in NUM_KEYS) p.longOrNull(k)?.let { text[k] = it.toString() }
                for (k in FLAG_KEYS) flags[k] = p.optBoolean(k)
                propType = p.str("property_type") ?: "apartment"
                status = p.str("status") ?: "active"
                existing = r.json.optJSONArray("images")?.objects() ?: emptyList()
            } else error = r.error
            loading = false
        }
    }

    fun buildBody(force: Boolean): JSONObject {
        val o = JSONObject()
        o.put("listing_type", type)
        o.put("property_type", propType)
        o.put("status", status)
        for (k in TEXT_KEYS) o.put(k, t(k).trim())
        for (k in NUM_KEYS) {
            val v = t(k).filter { it.isDigit() }
            if (v.isNotEmpty()) o.put(k, v.toLong()) else o.put(k, JSONObject.NULL)
        }
        for (k in FLAG_KEYS) o.put(k, flags[k] ?: false)
        if (force) o.put("force", true)
        return o
    }

    fun save(force: Boolean) {
        error = null
        limitHit = false
        if (t("title").isBlank()) { error = "عنوان ملک را وارد کنید."; return }
        if (t("city").isBlank()) { error = "شهر را وارد کنید."; return }
        if (!isRent) {
            val pv = t("price").toLongOrNull()
            if (pv == null || pv < MIN_SALE_PRICE) { error = SALE_PRICE_MSG; return }
        } else {
            if (t("deposit").isBlank()) { error = "مبلغ ودیعه (رهن) را به تومان وارد کنید."; return }
            if (t("monthly_rent").isBlank()) { error = "مبلغ اجاره ماهانه را به تومان وارد کنید."; return }
        }
        saving = true
        scope.launch {
            val body = buildBody(force)
            val r = if (id == 0) Api.call("POST", "/properties", body) else Api.call("PUT", "/properties/$id", body)
            if (r.ok) {
                val pid = if (id == 0) (r.json.optJSONObject("property")?.optInt("id") ?: 0) else id
                var upErr: String? = null
                if (picked.isNotEmpty() && pid != 0) {
                    val bytes = picked.toList().mapNotNull { uriToJpeg(ctx, it) }
                    if (bytes.isEmpty()) {
                        upErr = "عکس‌ها خوانده نشدند."
                    } else {
                        val ur = Api.uploadImages(pid, bytes)
                        if (!ur.ok) upErr = ur.error
                    }
                }
                saving = false
                val m = r.json.optInt("matched_customers", 0)
                if (upErr != null) {
                    infoMsg = "ملک ذخیره شد ولی ارسال عکس‌ها ناموفق بود: $upErr\nاز بخش ویرایش ملک دوباره عکس‌ها را اضافه کنید."
                } else if (id == 0 && m > 0) {
                    matchedMsg = "این ملک با مشخصات ${faNum(m)} مشتری شما مطابقت دارد."
                } else {
                    nav.popBackStack()
                }
            } else {
                saving = false
                limitHit = r.code == 403 && r.json.optString("code").startsWith("LIMIT")
                if (r.code == 409) dupMsg = r.error else error = r.error
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            (if (id == 0) "ثبت ملک جدید" else "ویرایش ملک") + (if (isRent) " (رهن و اجاره)" else " (فروش)"),
            onBack = { nav.popBackStack() }
        )
        if (loading) LoadingBox()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            AppField(t("title"), { text["title"] = it }, "عنوان ملک (مثال: آپارتمان ۱۱۰ متری)")
            SectionTitle("نوع ملک")
            ChipRow { PROPERTY_TYPES.forEach { pt -> SelectChip(pt.second, propType == pt.first) { propType = pt.first } } }
            AppField(t("city"), { text["city"] = it }, "شهر")
            AppField(t("neighborhood"), { text["neighborhood"] = it }, "محله")
            AppField(t("address"), { text["address"] = it }, "آدرس")

            SectionTitle(if (isRent) "مبلغ ودیعه و اجاره" else "قیمت")
            if (!isRent) {
                AppField(
                    t("price"),
                    { text["price"] = it.toEnDigits().filter { c -> c.isDigit() } },
                    "قیمت کل (تومان)",
                    number = true,
                    hint = "قیمت ملک را به تومان و با تمام صفرها وارد کنید. مثال: ۱۰,۰۰۰,۰۰۰,۰۰۰"
                )
                t("price").toLongOrNull()?.let { PricePreview(it, MIN_SALE_PRICE) }
            } else {
                AppField(
                    t("deposit"),
                    { text["deposit"] = it.toEnDigits().filter { c -> c.isDigit() } },
                    "مبلغ ودیعه / رهن (تومان)", number = true
                )
                t("deposit").toLongOrNull()?.let { PricePreview(it, null) }
                AppField(
                    t("monthly_rent"),
                    { text["monthly_rent"] = it.toEnDigits().filter { c -> c.isDigit() } },
                    "اجاره ماهانه (تومان)", number = true
                )
                t("monthly_rent").toLongOrNull()?.let { PricePreview(it, null) }
                SwitchRow("ودیعه و اجاره قابل مذاکره است", flags["negotiable"] ?: false) { flags["negotiable"] = it }
            }

            SectionTitle("مشخصات")
            AppField(t("area"), { text["area"] = it.toEnDigits().filter { c -> c.isDigit() } }, "متراژ (متر مربع)", number = true)
            AppField(t("bedrooms"), { text["bedrooms"] = it.toEnDigits().filter { c -> c.isDigit() } }, "تعداد خواب", number = true)
            AppField(t("floor"), { text["floor"] = it.toEnDigits().filter { c -> c.isDigit() } }, "طبقه", number = true)
            AppField(t("total_floors"), { text["total_floors"] = it.toEnDigits().filter { c -> c.isDigit() } }, "تعداد کل طبقات", number = true)
            AppField(t("build_year"), { text["build_year"] = it.toEnDigits().filter { c -> c.isDigit() } }, "سال ساخت", number = true)
            if (isRent) {
                AppField(t("delivery_date"), { text["delivery_date"] = it }, "تاریخ تحویل")
                AppField(t("contract_months"), { text["contract_months"] = it.toEnDigits().filter { c -> c.isDigit() } }, "مدت قرارداد (ماه)", number = true)
            } else {
                AppField(t("document_status"), { text["document_status"] = it }, "وضعیت سند (مثال: تک‌برگ)")
            }

            SectionTitle("امکانات")
            SwitchRow("آسانسور", flags["has_elevator"] ?: false) { flags["has_elevator"] = it }
            SwitchRow("پارکینگ", flags["has_parking"] ?: false) { flags["has_parking"] = it }
            SwitchRow("انباری", flags["has_storage"] ?: false) { flags["has_storage"] = it }
            SwitchRow("بالکن", flags["has_balcony"] ?: false) { flags["has_balcony"] = it }

            SectionTitle("مالک")
            AppField(t("owner_name"), { text["owner_name"] = it }, "نام مالک")
            AppField(t("owner_phone"), { text["owner_phone"] = it.toEnDigits() }, "موبایل مالک", phone = true)

            SectionTitle("توضیحات")
            AppField(t("description"), { text["description"] = it }, "توضیحات", singleLine = false)
            AppField(t("notes"), { text["notes"] = it }, "یادداشت‌های اضافی", singleLine = false)

            SectionTitle("عکس‌ها (حداکثر ${faNum(maxImgs)})")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item { AddPhotoTile { picker.launch("image/*") } }
                items(existing) { img ->
                    PhotoTile(onRemove = {
                        scope.launch {
                            val rr = Api.call("DELETE", "/properties/$id/images/${img.optInt("id")}")
                            if (rr.ok) existing = existing.filter { e -> e.optInt("id") != img.optInt("id") }
                        }
                    }) {
                        RemoteImage(img.str("url") ?: "", Modifier.fillMaxSize())
                    }
                }
                items(picked.toList()) { uri ->
                    PhotoTile(onRemove = { picked.remove(uri) }) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (id != 0) {
                SectionTitle("وضعیت ملک")
                ChipRow { propertyStatuses(type).forEach { s -> SelectChip(s.second, status == s.first) { status = s.first } } }
            }

            error?.let { Text(it, color = Danger, modifier = Modifier.padding(vertical = 8.dp)) }
            if (limitHit) {
                OutlinedButton(onClick = { nav.navigate("plans") }, modifier = Modifier.fillMaxWidth()) {
                    Text("مشاهده پلن‌ها و ارتقا", color = Brand)
                }
            }
            Spacer(Modifier.height(8.dp))
            GoldButton(if (saving) "در حال ذخیره و ارسال..." else "ذخیره", enabled = !saving && !loading) { save(false) }
            Spacer(Modifier.height(30.dp))
        }
    }

    infoMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { infoMsg = null; nav.popBackStack() },
            title = { Text("توجه") },
            text = { Text(m) },
            confirmButton = { TextButton(onClick = { infoMsg = null; nav.popBackStack() }) { Text("باشه") } }
        )
    }
    dupMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { dupMsg = null },
            title = { Text("ملک تکراری؟") },
            text = { Text("$m\nآیا باز هم ثبت شود؟") },
            confirmButton = { TextButton(onClick = { dupMsg = null; save(true) }) { Text("بله، ثبت شود", color = Brand) } },
            dismissButton = { TextButton(onClick = { dupMsg = null }) { Text("انصراف") } }
        )
    }
    matchedMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { matchedMsg = null; nav.popBackStack() },
            title = { Text("مشتری مناسب پیدا شد 🎯") },
            text = { Text(m) },
            confirmButton = {
                TextButton(onClick = {
                    matchedMsg = null
                    nav.popBackStack()
                    nav.navigate("matches")
                }) { Text("دیدن نتایج", color = Brand) }
            },
            dismissButton = { TextButton(onClick = { matchedMsg = null; nav.popBackStack() }) { Text("بعداً") } }
        )
    }
}
