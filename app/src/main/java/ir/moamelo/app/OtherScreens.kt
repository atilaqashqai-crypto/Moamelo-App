package ir.moamelo.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SupportScreen() {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("پشتیبانی")
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = Brand, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("بزودی...", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "ثبت تیکت، گفتگو با پشتیبانی، سوالات متداول و گزارش مشکل بزودی به معاملو اضافه می‌شود.",
                color = Muted, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AccountScreen() {
    var name by remember { mutableStateOf("") }
    var agency by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var created by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val r = Api.call("GET", "/auth/me")
        if (r.ok) {
            val u = r.json.optJSONObject("user")
            if (u != null) {
                name = u.str("full_name") ?: ""
                agency = u.str("agency_name") ?: ""
                city = u.str("city") ?: ""
                phone = u.str("phone") ?: ""
            }
        } else error = r.error
        loading = false
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("حساب و اشتراک")
        Column(Modifier.padding(16.dp)) {
            if (loading) LoadingBox()
            AppCard {
                SectionTitle("پروفایل")
                InfoRow("موبایل", phone.toFaDigits())
                Spacer(Modifier.height(6.dp))
                AppField(name, { name = it }, "نام و نام خانوادگی")
                AppField(agency, { agency = it }, "نام آژانس / دفتر املاک")
                AppField(city, { city = it }, "شهر")
                Spacer(Modifier.height(6.dp))
                GoldButton("ذخیره اطلاعات", enabled = !saving) {
                    saving = true
                    msg = null
                    error = null
                    scope.launch {
                        val body = JSONObject().put("full_name", name).put("agency_name", agency).put("city", city)
                        val r = Api.call("PUT", "/auth/me", body)
                        saving = false
                        if (r.ok) msg = "اطلاعات ذخیره شد ✓" else error = r.error
                    }
                }
                msg?.let { Text(it, color = Green, modifier = Modifier.padding(top = 8.dp)) }
                error?.let { Text(it, color = Danger, modifier = Modifier.padding(top = 8.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            AppCard {
                SectionTitle("ظاهر برنامه")
                ChipRow {
                    listOf("system" to "خودکار", "light" to "روشن", "dark" to "شب").forEach { m ->
                        SelectChip(m.second, Session.themeMode == m.first) { Session.setTheme(m.first) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            AppCard {
                SectionTitle("اشتراک")
                Text("طرح فعلی: رایگان", color = Ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("ارتقا و تمدید اشتراک، تاریخچه پرداخت و فاکتورها بزودی اضافه می‌شود.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text("ارتقای اشتراک — بزودی")
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = { Session.clear() }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Danger)
                Spacer(Modifier.width(8.dp))
                Text("خروج از حساب", color = Danger)
            }
            Spacer(Modifier.height(16.dp))
            Text("معاملو • نسخه ۱٫۰ (آزمایشی)", color = Muted, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun NotificationsScreen(nav: NavController) {
    var list by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val r = Api.call("GET", "/notifications")
        if (r.ok) {
            list = r.json.optJSONArray("notifications")?.objects() ?: emptyList()
            Api.call("POST", "/notifications/read-all")
        } else error = r.error
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("اعلان‌ها", onBack = { nav.popBackStack() })
        if (loading) LoadingBox()
        error?.let { ErrorBox(it) { nav.popBackStack() } }
        if (!loading && error == null && list.isEmpty()) EmptyBox("اعلانی وجود ندارد.")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { n ->
                AppCard {
                    Text(n.str("title") ?: "", color = Ink, fontWeight = FontWeight.Bold)
                    Text(n.str("body") ?: "", color = Muted, fontSize = 13.sp)
                    Text(jalali(n.str("created_at")), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
