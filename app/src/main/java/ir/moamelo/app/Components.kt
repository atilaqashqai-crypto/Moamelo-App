package ir.moamelo.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Brand)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Brand else Soft
    val fg = if (selected) OnBrand else Ink
    Box(
        Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = fg, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        content = content
    )
}

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    var m = modifier.fillMaxWidth().clip(shape).background(CardBg).border(1.dp, Border, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(16.dp), content = content)
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Brand)
    }
}

@Composable
fun ErrorBox(msg: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(msg, color = Danger, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("تلاش دوباره") }
    }
}

@Composable
fun EmptyBox(text: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = OnBrand)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AppField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    number: Boolean = false,
    phone: Boolean = false,
    singleLine: Boolean = true,
    hint: String? = null
) {
    val kt = if (number) KeyboardType.Number else if (phone) KeyboardType.Phone else KeyboardType.Text
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = kt),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        supportingText = { if (hint != null) Text(hint, fontSize = 12.sp) }
    )
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.width(100.dp))
        Text(value, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun Badge2(text: String, color: Color = Brand) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = Brand, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
fun ScoreBadge(score: Int) {
    val c = if (score >= 80) Green else if (score >= 60) Brand else Muted
    Box(
        Modifier.size(54.dp).clip(CircleShape).background(c.copy(alpha = 0.16f)).border(1.5.dp, c, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("${faNum(score)}٪", color = c, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun PricePreview(value: Long, minRequired: Long?) {
    Column(Modifier.padding(bottom = 6.dp)) {
        Text("${formatMoney(value)}  (${moneyWords(value)})", color = Brand, fontSize = 13.sp)
        if (minRequired != null && value < minRequired) {
            Text(
                "حداقل قیمت فروش ${formatMoney(minRequired)} است. صفرها را کامل وارد کنید.",
                color = Danger, fontSize = 12.sp
            )
        }
    }
}

@Composable
fun RemoteImage(path: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = Api.imageUrl(path),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
fun PhotoTile(onRemove: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.size(92.dp).clip(RoundedCornerShape(14.dp)).background(Soft)) {
        content()
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0x99000000))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "حذف", tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun AddPhotoTile(onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier.size(92.dp).clip(shape).background(Soft).border(1.dp, Border, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = Brand)
            Text("افزودن", color = Muted, fontSize = 11.sp)
        }
    }
}
