package ir.moamelo.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun MatchCard(m: JSONObject, onClick: () -> Unit) {
    val p = m.getJSONObject("property")
    val reasons = m.optJSONArray("reasons")?.strings() ?: emptyList()
    val mismatches = m.optJSONArray("mismatches")?.strings() ?: emptyList()
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
            ScoreBadge(m.optInt("score"))
        }
        if (reasons.isNotEmpty() || mismatches.isNotEmpty()) Spacer(Modifier.height(8.dp))
        reasons.forEach { Text("✓ $it", color = Green, fontSize = 12.sp) }
        mismatches.forEach { Text("! $it", color = Warn, fontSize = 12.sp) }
    }
}

@Composable
fun CustomerListScreen(nav: NavController) {
    var q by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(q, type, reload) {
        loading = true
        var path = "/customers?x=1"
        if (type.isNotEmpty()) path += "&customer_type=$type"
        if (q.isNotBlank()) path += "&q=${Api.enc(q.trim())}"
        val r = Api.call("GET", path)
        if (r.ok) {
            list = r.json.optJSONArray("customers")?.objects() ?: emptyList()
            error = null
        } else error = r.error
        loading = false
    }

    Scaffold(
        containerColor = Bg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("customer_form/0") },
                containerColor = Brand,
                contentColor = OnBrand
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("مشتری جدید", fontWeight = FontWeight.Bold)
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            ScreenHeader("خریداران و مشتریان", onBack = { nav.popBackStack() })
            Column(Modifier.padding(horizontal = 16.dp)) {
                AppField(q, { q = it }, "جستجو (نام یا شماره)")
                ChipRow {
                    SelectChip("همه", type == "") { type = "" }
                    CUSTOMER_TYPES.forEach { c -> SelectChip(c.second, type == c.first) { type = c.first } }
                }
            }
            if (loading) LoadingBox()
            error?.let { ErrorBox(it) { reload++ } }
            if (!loading && error == null && list.isEmpty()) EmptyBox("هنوز مشتری ثبت نشده است.")
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(list) { c ->
                    AppCard(onClick = { nav.navigate("customer/${c.optInt("id")}") }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(c.str("full_name") ?: "", color = Ink, fontWeight = FontWeight.Bold)
                                Text((c.str("phone") ?: "").toFaDigits(), color = Muted, fontSize = 12.sp)
                                val budget = if (c.str("customer_type") == "buyer") {
                                    c.longOrNull("max_budget")?.let { "تا ${formatMoney(it)}" }
                                } else if (c.str("customer_type") == "tenant") {
                                    c.longOrNull("max_rent")?.let { "اجاره تا ${formatMoney(it)}" }
                                } else null
                                if (budget != null) Text(budget, color = Brand, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Badge2(labelOf(CUSTOMER_TYPES, c.str("customer_type")))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerDetailScreen(nav: NavController, id: Int) {
    var customer by remember { mutableStateOf<JSONObject?>(null) }
    var interactions by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var matches by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf("note") }
    var noteText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    LaunchedEffect(reload) {
        val r = Api.call("GET", "/customers/$id")
        if (r.ok) {
            customer = r.json.optJSONObject("customer")
            interactions = r.json.optJSONArray("interactions")?.objects() ?: emptyList()
            matches = r.json.optJSONArray("matches")?.objects() ?: emptyList()
            error = null
        } else error = r.error
        loading = false
    }

    val c = customer
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("پروفایل مشتری", onBack = { nav.popBackStack() }) {
            if (c != null) {
                IconButton(onClick = { nav.navigate("customer_form/$id") }) {
                    Icon(Icons.Filled.Edit, contentDescription = "ویرایش", tint = Brand)
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = Danger)
                }
            }
        }
        if (loading) LoadingBox()
        error?.let { ErrorBox(it) { reload++ } }
        if (c != null) {
            val type = c.str("customer_type")
            Column(Modifier.padding(16.dp)) {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.str("full_name") ?: "", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Badge2(labelOf(CUSTOMER_TYPES, type))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text((c.str("phone") ?: "").toFaDigits(), color = Muted)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.str("phone")}"))) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, tint = Brand)
                        Spacer(Modifier.width(8.dp))
                        Text("تماس", color = Brand)
                    }
                }
                Spacer(Modifier.height(6.dp))
                AppCard {
                    SectionTitle("نیازمندی‌ها")
                    InfoRow("شهر", c.str("city"))
                    val nb = c.optJSONArray("neighborhoods")?.strings() ?: emptyList()
                    InfoRow("محله‌ها", if (nb.isEmpty()) null else nb.joinToString("، "))
                    InfoRow("نوع ملک", c.str("property_type")?.let { labelOf(PROPERTY_TYPES, it) })
                    if (type == "buyer") {
                        InfoRow("حداقل بودجه", c.longOrNull("min_budget")?.let { formatMoney(it) })
                        InfoRow("حداکثر بودجه", c.longOrNull("max_budget")?.let { formatMoney(it) })
                    }
                    if (type == "tenant") {
                        InfoRow("حداکثر ودیعه", c.longOrNull("max_deposit")?.let { formatMoney(it) })
                        InfoRow("حداکثر اجاره", c.longOrNull("max_rent")?.let { formatMoney(it) })
                    }
                    InfoRow("حداقل متراژ", c.intOrNull("min_area")?.let { "${faNum(it)} متر" })
                    InfoRow("حداکثر متراژ", c.intOrNull("max_area")?.let { "${faNum(it)} متر" })
                    InfoRow("حداقل خواب", c.intOrNull("bedrooms")?.let { faNum(it) })
                    val am = listOfNotNull(
                        if (c.optBoolean("needs_elevator")) "آسانسور" else null,
                        if (c.optBoolean("needs_parking")) "پارکینگ" else null,
                        if (c.optBoolean("needs_storage")) "انباری" else null,
                        if (c.optBoolean("needs_balcony")) "بالکن" else null
                    )
                    InfoRow("امکانات", if (am.isEmpty()) null else am.joinToString("، "))
                    InfoRow("سایر خواسته‌ها", c.str("preferences"))
                    InfoRow("یادداشت", c.str("notes"))
                    InfoRow("تاریخ ثبت", jalali(c.str("created_at")))
                }

                if (type == "buyer" || type == "tenant") {
                    SectionTitle("ملک‌های مناسب (${faNum(matches.size)})")
                    if (matches.isEmpty()) {
                        Text("فعلاً ملکی با مشخصات این مشتری پیدا نشد. با ثبت ملک جدید، سیستم خودکار اطلاع می‌دهد.", color = Muted, fontSize = 13.sp)
                    }
                    matches.forEach { m ->
                        MatchCard(m) { nav.navigate("property/${m.getJSONObject("property").optInt("id")}") }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                SectionTitle("ثبت تماس / یادداشت")
                ChipRow { INTERACTION_KINDS.forEach { k -> SelectChip(k.second, kind == k.first) { kind = k.first } } }
                AppField(noteText, { noteText = it }, "توضیحات", singleLine = false)
                GoldButton("ثبت", enabled = noteText.isNotBlank()) {
                    scope.launch {
                        Api.call("POST", "/customers/$id/interactions", JSONObject().put("kind", kind).put("note", noteText))
                        noteText = ""
                        reload++
                    }
                }
                SectionTitle("تاریخچه ارتباط‌ها")
                if (interactions.isEmpty()) Text("هنوز سابقه‌ای ثبت نشده است.", color = Muted, fontSize = 13.sp)
                interactions.forEach { it2 ->
                    AppCard {
                        Row {
                            Badge2(labelOf(INTERACTION_KINDS, it2.str("kind")))
                            Spacer(Modifier.weight(1f))
                            Text(jalali(it2.str("created_at")), color = Muted, fontSize = 11.sp)
                        }
                        Text(it2.str("note") ?: "", color = Ink, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف مشتری") },
            text = { Text("این مشتری و تمام سابقه‌اش حذف می‌شود. مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        val r = Api.call("DELETE", "/customers/$id")
                        if (r.ok) nav.popBackStack() else error = r.error
                    }
                }) { Text("حذف", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } }
        )
    }
}

private val C_TEXT_KEYS = listOf("full_name", "phone", "city", "neighborhoods", "preferences", "notes")
private val C_NUM_KEYS = listOf("min_budget", "max_budget", "max_deposit", "max_rent", "min_area", "max_area", "bedrooms")
private val C_FLAG_KEYS = listOf("needs_elevator", "needs_parking", "needs_storage", "needs_balcony")

@Composable
fun CustomerFormScreen(nav: NavController, id: Int) {
    val text = remember { mutableStateMapOf<String, String>() }
    val flags = remember { mutableStateMapOf<String, Boolean>() }
    var type by remember { mutableStateOf("buyer") }
    var propType by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(id != 0) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun t(k: String): String = text[k] ?: ""

    LaunchedEffect(Unit) {
        if (id != 0) {
            val r = Api.call("GET", "/customers/$id")
            val c = r.json.optJSONObject("customer")
            if (r.ok && c != null) {
                for (k in C_TEXT_KEYS) if (k != "neighborhoods") c.str(k)?.let { text[k] = it }
                text["neighborhoods"] = (c.optJSONArray("neighborhoods") ?: JSONArray()).strings().joinToString("، ")
                for (k in C_NUM_KEYS) c.longOrNull(k)?.let { text[k] = it.toString() }
                for (k in C_FLAG_KEYS) flags[k] = c.optBoolean(k)
                type = c.str("customer_type") ?: "buyer"
                propType = c.str("property_type") ?: ""
            } else error = r.error
            loading = false
        }
    }

    fun save() {
        error = null
        val body = JSONObject()
        for (k in C_TEXT_KEYS) body.put(k, t(k).trim())
        for (k in C_NUM_KEYS) {
            val v = t(k).filter { it.isDigit() }
            if (v.isNotEmpty()) body.put(k, v.toLong()) else body.put(k, JSONObject.NULL)
        }
        for (k in C_FLAG_KEYS) body.put(k, flags[k] ?: false)
        body.put("customer_type", type)
        body.put("property_type", if (propType.isEmpty()) JSONObject.NULL else propType)
        saving = true
        scope.launch {
            val r = if (id == 0) Api.call("POST", "/customers", body) else Api.call("PUT", "/customers/$id", body)
            saving = false
            if (r.ok) {
                val newId = r.json.optJSONObject("customer")?.optInt("id") ?: 0
                nav.popBackStack()
                if (id == 0 && newId != 0) nav.navigate("customer/$newId")
            } else error = r.error
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (id == 0) "مشتری جدید" else "ویرایش مشتری", onBack = { nav.popBackStack() })
        if (loading) LoadingBox()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            AppField(t("full_name"), { text["full_name"] = it }, "نام و نام خانوادگی")
            AppField(t("phone"), { text["phone"] = it.toEnDigits() }, "شماره موبایل", phone = true, hint = "مثال: ۰۹۱۲۱۲۳۴۵۶۷")
            SectionTitle("نوع مشتری")
            ChipRow { CUSTOMER_TYPES.forEach { c -> SelectChip(c.second, type == c.first) { type = c.first } } }

            if (type == "buyer" || type == "tenant") {
                SectionTitle("نیازمندی‌ها")
                AppField(t("city"), { text["city"] = it }, "شهر")
                AppField(t("neighborhoods"), { text["neighborhoods"] = it }, "محله‌های مورد نظر", hint = "با ویرگول جدا کنید. مثال: سعادت آباد، ونک")
                Text("نوع ملک", color = Muted, fontSize = 13.sp)
                ChipRow {
                    SelectChip("هر نوع", propType == "") { propType = "" }
                    PROPERTY_TYPES.forEach { pt -> SelectChip(pt.second, propType == pt.first) { propType = pt.first } }
                }
                if (type == "buyer") {
                    AppField(t("min_budget"), { text["min_budget"] = it.toEnDigits().filter { c -> c.isDigit() } }, "حداقل بودجه (تومان)", number = true)
                    t("min_budget").toLongOrNull()?.let { PricePreview(it, null) }
                    AppField(t("max_budget"), { text["max_budget"] = it.toEnDigits().filter { c -> c.isDigit() } }, "حداکثر بودجه (تومان)", number = true, hint = "با تمام صفرها وارد کنید. مثال: ۲۰,۰۰۰,۰۰۰,۰۰۰")
                    t("max_budget").toLongOrNull()?.let { PricePreview(it, null) }
                } else {
                    AppField(t("max_deposit"), { text["max_deposit"] = it.toEnDigits().filter { c -> c.isDigit() } }, "حداکثر ودیعه (تومان)", number = true)
                    t("max_deposit").toLongOrNull()?.let { PricePreview(it, null) }
                    AppField(t("max_rent"), { text["max_rent"] = it.toEnDigits().filter { c -> c.isDigit() } }, "حداکثر اجاره ماهانه (تومان)", number = true)
                    t("max_rent").toLongOrNull()?.let { PricePreview(it, null) }
                }
                AppField(t("min_area"), { text["min_area"] = it.toEnDigits().filter { c -> c.isDigit() } }, "حداقل متراژ", number = true)
                AppField(t("max_area"), { text["max_area"] = it.toEnDigits().filter { c -> c.isDigit() } }, "حداکثر متراژ", number = true)
                AppField(t("bedrooms"), { text["bedrooms"] = it.toEnDigits().filter { c -> c.isDigit() } }, "حداقل تعداد خواب", number = true)
                SectionTitle("امکانات مورد نیاز")
                SwitchRow("آسانسور", flags["needs_elevator"] ?: false) { flags["needs_elevator"] = it }
                SwitchRow("پارکینگ", flags["needs_parking"] ?: false) { flags["needs_parking"] = it }
                SwitchRow("انباری", flags["needs_storage"] ?: false) { flags["needs_storage"] = it }
                SwitchRow("بالکن", flags["needs_balcony"] ?: false) { flags["needs_balcony"] = it }
                AppField(t("preferences"), { text["preferences"] = it }, "سایر خواسته‌ها", singleLine = false)
            }
            AppField(t("notes"), { text["notes"] = it }, "یادداشت", singleLine = false)

            error?.let { Text(it, color = Danger, modifier = Modifier.padding(vertical = 8.dp)) }
            Spacer(Modifier.height(8.dp))
            GoldButton(if (saving) "در حال ذخیره..." else "ذخیره", enabled = !saving && !loading) { save() }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun MatchesScreen(nav: NavController) {
    var results by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        val r = Api.call("GET", "/matches")
        if (r.ok) {
            results = r.json.optJSONArray("results")?.objects() ?: emptyList()
            error = null
        } else error = r.error
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("نتایج تطبیق هوشمند", onBack = { nav.popBackStack() })
        if (loading) LoadingBox()
        error?.let { ErrorBox(it) { reload++ } }
        if (!loading && error == null && results.isEmpty()) {
            EmptyBox("هنوز تطبیقی پیدا نشد.\nبا ثبت ملک و مشتری، سیستم خودکار بهترین گزینه‌ها را پیشنهاد می‌دهد.")
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(results) { res ->
                val c = res.getJSONObject("customer")
                val ms = res.optJSONArray("matches")?.objects() ?: emptyList()
                AppCard {
                    Row(
                        Modifier.clickable { nav.navigate("customer/${c.optInt("id")}") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.str("full_name") ?: "", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "${labelOf(CUSTOMER_TYPES, c.str("customer_type"))} • ${faNum(res.optInt("count"))} ملک مناسب",
                                color = Muted, fontSize = 12.sp
                            )
                        }
                        ScoreBadge(res.optInt("best_score"))
                    }
                    Spacer(Modifier.height(10.dp))
                    ms.take(3).forEach { m ->
                        val p = m.getJSONObject("property")
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { nav.navigate("property/${p.optInt("id")}") }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.str("title") ?: "", color = Ink, fontSize = 13.sp, maxLines = 1)
                                Text(priceText(p), color = Brand, fontSize = 12.sp)
                            }
                            Text("${faNum(m.optInt("score"))}٪", color = Green, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
