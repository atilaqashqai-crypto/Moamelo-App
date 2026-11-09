package ir.moamelo.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun LoginScreen() {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Image(painter = painterResource(R.drawable.logo), contentDescription = "معاملو", modifier = Modifier.size(130.dp))
        Spacer(Modifier.height(8.dp))
        Text("معاملو", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("معامله هوشمند، آینده مطمئن", color = Brand, fontSize = 14.sp)
        Spacer(Modifier.height(40.dp))

        if (step == 1) {
            AppField(phone, { phone = it.toEnDigits() }, "شماره موبایل", phone = true, hint = "مثال: ۰۹۱۲۱۲۳۴۵۶۷")
            Spacer(Modifier.height(12.dp))
            GoldButton("دریافت کد ورود", enabled = !busy) {
                error = null
                busy = true
                scope.launch {
                    val r = Api.call("POST", "/auth/request-otp", JSONObject().put("phone", phone))
                    busy = false
                    if (r.ok) step = 2 else error = r.error
                }
            }
        } else {
            Text("کد ورود برای $phone ارسال شد", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            AppField(code, { code = it.toEnDigits() }, "کد ورود", number = true, hint = "نسخه آزمایشی: کد ورود ۱۲۳۴۵ است")
            Spacer(Modifier.height(12.dp))
            GoldButton("ورود", enabled = !busy) {
                error = null
                busy = true
                scope.launch {
                    val r = Api.call("POST", "/auth/verify", JSONObject().put("phone", phone).put("code", code))
                    busy = false
                    if (r.ok) Session.save(r.json.getString("token")) else error = r.error
                }
            }
            TextButton(onClick = { step = 1; code = "" }) { Text("تغییر شماره", color = Brand) }
        }
        if (busy) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(color = Brand)
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Danger, textAlign = TextAlign.Center)
        }
    }
}
