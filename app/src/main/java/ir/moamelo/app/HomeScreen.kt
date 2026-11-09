package ir.moamelo.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun CategoryCard(title: String, subtitle: String, imageRes: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1376f / 768f)
            .clip(shape)
            .border(1.dp, Border, shape)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xCC062E1E))))
        )
        Column(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, color = Color.White.copy(alpha = 0.88f), fontSize = 13.sp)
        }
    }
}

@Composable
fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Brand)
        Spacer(Modifier.height(6.dp))
        Text(label, color = Ink, fontSize = 12.sp)
    }
}

@Composable
fun HomeScreen(nav: NavController) {
    var dash by remember { mutableStateOf<JSONObject?>(null) }
    var notes by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var me by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            val r = Api.call("GET", "/dashboard")
            val n = Api.call("GET", "/notifications")
            val m = Api.call("GET", "/auth/me")
            if (r.ok) { dash = r.json; error = null } else error = r.error
            if (n.ok) notes = (n.json.optJSONArray("notifications")?.objects() ?: emptyList())
            if (m.ok) me = m.json.optJSONObject("user")
            loading = false
        }
    }
    LaunchedEffect(Unit) { load() }

    val stats = dash?.optJSONObject("stats")
    val unread = stats?.optInt("unread_notifications", 0) ?: 0
    val name = me?.str("full_name") ?: "مشاور محترم"
    val agency = me?.str("agency_name")

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(46.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("خوش آمدید 👋", color = Muted, fontSize = 12.sp)
                    Text(name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (!agency.isNullOrBlank()) Text(agency, color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = { nav.navigate("notifications") }) {
                    if (unread > 0) {
                        BadgedBox(badge = { Badge { Text(faNum(unread)) } }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "اعلان‌ها", tint = Brand)
                        }
                    } else {
                        Icon(Icons.Filled.Notifications, contentDescription = "اعلان‌ها", tint = Brand)
                    }
                }
            }
        }
        item {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Brand.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("اشتراک فعال: رایگان", color = Brand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (loading && dash == null) {
            item { LoadingBox() }
        }
        error?.let { msg ->
            item { ErrorBox(msg) { load() } }
        }
        item {
            CategoryCard("فروش", "${faNum(stats?.optInt("sale_properties", 0) ?: 0)} ملک فعال", R.drawable.cat_sale) {
                nav.navigate("properties/sale")
            }
        }
        item {
            CategoryCard("رهن و اجاره", "${faNum(stats?.optInt("rent_properties", 0) ?: 0)} ملک فعال", R.drawable.cat_rent) {
                nav.navigate("properties/rent")
            }
        }
        item {
            CategoryCard("خریداران و مشتریان", "${faNum(stats?.optInt("customers", 0) ?: 0)} مشتری فعال", R.drawable.cat_customer) {
                nav.navigate("customers")
            }
        }
        item {
            SectionTitle("دسترسی سریع")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("ملک جدید", Icons.Filled.Add, Modifier.weight(1f)) { nav.navigate("property_form/sale/0") }
                QuickAction("مشتری جدید", Icons.Filled.PersonAdd, Modifier.weight(1f)) { nav.navigate("customer_form/0") }
                QuickAction("نتایج تطبیق", Icons.Filled.AutoAwesome, Modifier.weight(1f)) { nav.navigate("matches") }
            }
        }
        if (notes.isNotEmpty()) {
            item { SectionTitle("اعلان‌های مهم") }
            items(notes.take(3)) { n ->
                AppCard(onClick = { nav.navigate("notifications") }) {
                    Text(n.str("title") ?: "", color = Ink, fontWeight = FontWeight.Bold)
                    Text(n.str("body") ?: "", color = Muted, fontSize = 13.sp)
                }
            }
        }
        val rp = dash?.optJSONArray("recent_properties")?.objects() ?: emptyList()
        val rc = dash?.optJSONArray("recent_customers")?.objects() ?: emptyList()
        if (rp.isNotEmpty() || rc.isNotEmpty()) {
            item { SectionTitle("آخرین فعالیت‌ها") }
        }
        items(rp) { p ->
            AppCard(onClick = { nav.navigate("property/${p.optInt("id")}") }) {
                Text(p.str("title") ?: "", color = Ink, fontWeight = FontWeight.Bold)
                Text(
                    "${if (p.str("listing_type") == "sale") "فروش" else "رهن و اجاره"} • ${locationText(p)} • ${jalali(p.str("created_at"))}",
                    color = Muted, fontSize = 12.sp
                )
            }
        }
        items(rc) { c ->
            AppCard(onClick = { nav.navigate("customer/${c.optInt("id")}") }) {
                Text(c.str("full_name") ?: "", color = Ink, fontWeight = FontWeight.Bold)
                Text(
                    "${labelOf(CUSTOMER_TYPES, c.str("customer_type"))} • ${jalali(c.str("created_at"))}",
                    color = Muted, fontSize = 12.sp
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
