package ir.moamelo.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.json.JSONObject

fun limitText(v: Int?, unit: String): String = if (v == null) "$unit نامحدود" else "${faNum(v)} $unit"

fun usageText(used: Int, max: Int?): String = if (max == null) "${faNum(used)} (نامحدود)" else "${faNum(used)} از ${faNum(max)}"

fun planPrice(p: JSONObject, period: String): Long? = when (period) {
    "quarterly" -> p.longOrNull("price_quarterly")
    "yearly" -> p.longOrNull("price_yearly")
    else -> p.longOrNull("price_monthly")
}

@Composable
fun PlanCard(plan: JSONObject, period: String, isCurrent: Boolean, extraUserPrice: Long, onBuy: () -> Unit) {
    val monthly = plan.longOrNull("price_monthly") ?: 0L
    val price = planPrice(plan, period) ?: if (period == "monthly") monthly else null
    val months = when (period) { "quarterly" -> 3L; "yearly" -> 12L; else -> 1L }
    val isFree = monthly == 0L
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(plan.str("name") ?: "", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (isCurrent) Badge2("پلن فعلی", Green)
        }
        Spacer(Modifier.height(6.dp))
        if (isFree) {
            Text("رایگان", color = Brand, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        } else if (price != null) {
            Text(formatMoney(price), color = Brand, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (months > 1L) Text("معادل ${formatMoney(price / months)} در ماه", color = Muted, fontSize = 12.sp)
            else Text("در ماه", color = Muted, fontSize = 12.sp)
        } else {
            Text("برای این مدت موجود نیست", color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        val feats = mutableListOf<String>()
        feats.add(limitText(plan.intOrNull("max_properties"), "ملک"))
        feats.add(limitText(plan.intOrNull("max_customers"), "مشتری"))
        feats.add("${faNum(plan.intOrNull("max_images"))} عکس برای هر ملک")
        val users = plan.intOrNull("max_users") ?: 1
        feats.add(if (users > 1) "${faNum(users)} کاربر با دیتای مشترک" else "تک‌کاربره")
        feats.add("تطبیق هوشمند ملک و مشتری")
        feats.add(if (plan.optBoolean("watermark")) "پوستر با نشان معاملو" else "پوستر بدون نشان با لوگو و شماره شما")
        feats.forEach { Text("✓ $it", color = Ink, fontSize = 13.sp, modifier = Modifier.padding(vertical = 1.dp)) }
        if (users > 1 && extraUserPrice > 0) {
            Text("کاربر اضافه: ${formatMoney(extraUserPrice)} در ماه برای هر نفر", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        if (!isFree && !isCurrent && price != null) {
            Spacer(Modifier.height(12.dp))
            GoldButton("درخواست خرید") { onBuy() }
        }
    }
}

@Composable
fun PlansScreen(nav: NavController) {
    var plans by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var sub by remember { mutableStateOf<JSONObject?>(null) }
    var kind by remember { mutableStateOf("single") }
    var period by remember { mutableStateOf("monthly") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var buyPlan by remember { mutableStateOf<JSONObject?>(null) }
    var reload by remember { mutableStateOf(0) }
    val ctx = LocalContext.current

    LaunchedEffect(reload) {
        val r1 = Api.call("GET", "/plans")
        val r2 = Api.call("GET", "/subscription")
        if (r1.ok) {
            plans = r1.json.optJSONArray("plans")?.objects() ?: emptyList()
            error = null
        } else error = r1.error
        if (r2.ok) sub = r2.json
        loading = false
    }

    val currentCode = sub?.optJSONObject("plan")?.str("code")
    val extraPrice = sub?.longOrNull("extra_user_price") ?: 0L
    val supportPhone = sub?.str("support_phone") ?: ""

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("پلن‌های اشتراک", onBack = { nav.popBackStack() })
        if (loading) LoadingBox()
        error?.let { ErrorBox(it) { reload++ } }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val s = sub
            if (s != null) {
                item {
                    val plan = s.optJSONObject("plan")
                    val usage = s.optJSONObject("usage")
                    AppCard {
                        Text("پلن فعلی شما: ${plan?.str("name") ?: "—"}", color = Ink, fontWeight = FontWeight.Bold)
                        val sb = s.optJSONObject("subscription")
                        if (sb != null) {
                            val trial = if (s.optBoolean("is_trial")) " (دوره آزمایشی)" else ""
                            Text("تا ${jalali(sb.str("ends_at"))}$trial", color = Muted, fontSize = 12.sp)
                        }
                        if (plan != null && usage != null) {
                            Spacer(Modifier.height(6.dp))
                            Text("ملک: ${usageText(usage.optInt("properties"), plan.intOrNull("max_properties"))}", color = Ink, fontSize = 13.sp)
                            Text("مشتری: ${usageText(usage.optInt("customers"), plan.intOrNull("max_customers"))}", color = Ink, fontSize = 13.sp)
                            Text("کاربران: ${usageText(usage.optInt("users"), s.intOrNull("max_users"))}", color = Ink, fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                ChipRow {
                    SelectChip("مشاور (تک‌کاربره)", kind == "single") { kind = "single" }
                    SelectChip("آژانس (چند کاربره)", kind == "agency") { kind = "agency" }
                }
                ChipRow {
                    SelectChip("ماهانه", period == "monthly") { period = "monthly" }
                    SelectChip("۳ ماهه", period == "quarterly") { period = "quarterly" }
                    SelectChip("سالانه", period == "yearly") { period = "yearly" }
                }
            }
            items(plans.filter { it.str("kind") == kind }) { p ->
                PlanCard(p, period, p.str("code") == currentCode, extraPrice) { buyPlan = p }
            }
            item {
                Text(
                    "قیمت‌ها به تومان است. پرداخت آنلاین بزودی فعال می‌شود و فعلاً خرید از طریق پشتیبانی انجام می‌شود.",
                    color = Muted, fontSize = 12.sp
                )
            }
        }
    }

    buyPlan?.let { p ->
        AlertDialog(
            onDismissRequest = { buyPlan = null },
            title = { Text("خرید ${p.str("name") ?: ""}") },
            text = {
                Text(
                    if (supportPhone.isNotBlank()) "برای فعال‌سازی پلن با پشتیبانی تماس بگیرید:\n${supportPhone.toFaDigits()}"
                    else "پرداخت آنلاین بزودی فعال می‌شود. شماره پشتیبانی بزودی اعلام خواهد شد."
                )
            },
            confirmButton = {
                if (supportPhone.isNotBlank()) {
                    TextButton(onClick = {
                        buyPlan = null
                        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone")))
                    }) { Text("تماس", color = Brand) }
                }
            },
            dismissButton = { TextButton(onClick = { buyPlan = null }) { Text("بستن") } }
        )
    }
}

@Composable
fun TeamScreen(nav: NavController) {
    var members by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var maxUsers by remember { mutableStateOf(1) }
    var isOwner by remember { mutableStateOf(false) }
    var planName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var limitHit by remember { mutableStateOf(false) }
    var removeId by remember { mutableStateOf<Int?>(null) }
    var reload by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        val r = Api.call("GET", "/team")
        if (r.ok) {
            members = r.json.optJSONArray("members")?.objects() ?: emptyList()
            maxUsers = r.json.optInt("max_users", 1)
            isOwner = r.json.optBoolean("is_owner")
            planName = r.json.str("plan_name") ?: ""
        } else error = r.error
        loading = false
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("تیم آژانس", onBack = { nav.popBackStack() })
        if (loading) LoadingBox()
        Column(Modifier.padding(16.dp)) {
            AppCard {
                Text("پلن: $planName", color = Ink, fontWeight = FontWeight.Bold)
                Text("ظرفیت کاربران: ${faNum(members.size)} از ${faNum(maxUsers)}", color = Muted, fontSize = 13.sp)
                Text("ملک‌ها و مشتری‌های همه اعضا بین اعضای آژانس مشترک است.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
            SectionTitle("اعضا")
            members.forEach { m ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.str("full_name") ?: "بدون نام", color = Ink, fontWeight = FontWeight.Bold)
                            Text((m.str("phone") ?: "").toFaDigits(), color = Muted, fontSize = 12.sp)
                        }
                        if (m.str("role") == "owner") Badge2("مدیر", Brand)
                        else if (isOwner) {
                            IconButton(onClick = { removeId = m.optInt("id") }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف عضو", tint = Danger)
                            }
                        } else Badge2("عضو", Muted)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (isOwner) {
                SectionTitle("افزودن عضو")
                AppField(name, { name = it }, "نام عضو")
                AppField(phone, { phone = it.toEnDigits() }, "شماره موبایل عضو", phone = true, hint = "عضو با همین شماره وارد اپ می‌شود")
                GoldButton(if (busy) "در حال افزودن..." else "افزودن عضو", enabled = !busy && phone.isNotBlank()) {
                    busy = true
                    error = null
                    limitHit = false
                    scope.launch {
                        val r = Api.call("POST", "/team/members", JSONObject().put("phone", phone).put("full_name", name))
                        busy = false
                        if (r.ok) {
                            phone = ""
                            name = ""
                            reload++
                        } else {
                            limitHit = r.json.optString("code") == "LIMIT_USERS"
                            error = r.error
                        }
                    }
                }
                error?.let { Text(it, color = Danger, modifier = Modifier.padding(top = 8.dp)) }
                if (limitHit) {
                    OutlinedButton(onClick = { nav.navigate("plans") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("مشاهده پلن‌ها و ارتقا", color = Brand)
                    }
                }
            } else {
                Text("فقط مدیر آژانس می‌تواند عضو اضافه یا حذف کند.", color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    removeId?.let { rid ->
        AlertDialog(
            onDismissRequest = { removeId = null },
            title = { Text("حذف عضو") },
            text = { Text("ملک‌ها و مشتری‌های این عضو در آژانس باقی می‌مانند و به مدیر منتقل می‌شوند. ادامه می‌دهید؟") },
            confirmButton = {
                TextButton(onClick = {
                    removeId = null
                    scope.launch {
                        val r = Api.call("DELETE", "/team/members/$rid")
                        if (r.ok) reload++ else error = r.error
                    }
                }) { Text("حذف", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { removeId = null }) { Text("انصراف") } }
        )
    }
}
