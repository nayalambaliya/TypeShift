package com.nayal.aikeyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nayal.aikeyboard.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ─── Aurora Palette ──────────────────────────────────────────────────────────
val Bg        = Color(0xFF080812)
val Surface1  = Color(0x0AFFFFFF)
val Surface2  = Color(0x14FFFFFF)
val Surface3  = Color(0x22FFFFFF)
val Surface4  = Color(0x33FFFFFF)
val Accent    = Color(0xFF8B5CF6)
val AccentAlt = Color(0xFFA78BFA)
val AccentGlow= Color(0x608B5CF6)
val TextPrim  = Color(0xFFF1F5F9)
val TextSec   = Color(0xFF94A3B8)
val TextTert  = Color(0xFF475569)
val Success   = Color(0xFF34D399)
val Danger    = Color(0xFFF87171)
val Warning   = Color(0xFFFBBF24)

// Command category accent colors
val CatEdit     = Color(0xFF60A5FA)   // blue  — grammar / editing
val CatCreative = Color(0xFFFB923C)   // orange — creative / fun
val CatUtil     = Color(0xFF34D399)   // green  — structure / utility
val CatFun      = Color(0xFFF472B6)   // pink   — humor
val CatLang     = Color(0xFFA78BFA)   // lavender — language
val CatMeta     = Color(0xFF94A3B8)   // slate  — meta (undo, etc.)

fun commandColor(trigger: String): Color = when {
    trigger in setOf("?fix", "?improve", "?formal", "?casual", "?human", "?shorter", "?longer") -> CatEdit
    trigger in setOf("?emoji", "?roast", "?headline") -> CatCreative
    trigger in setOf("?joke", "?hinglish") -> CatFun
    trigger in setOf("?bullet", "?tldr", "?subject", "?tweet", "?eli5", "?reply") -> CatUtil
    trigger.startsWith("?translate") -> CatLang
    else -> CatMeta
}

// ─── Font ─────────────────────────────────────────────────────────────────────
private val gFonts = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)
val AppFont = FontFamily(
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = FontWeight.ExtraBold),
)

// ─── Models ───────────────────────────────────────────────────────────────────
data class CustomCommand(
    val id: String = UUID.randomUUID().toString(),
    val trigger: String,
    val name: String,
    val prompt: String
)

fun loadCustomCommands(context: Context): List<CustomCommand> {
    val prefs = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
    val json  = prefs.getString("custom_commands", "[]") ?: "[]"
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            CustomCommand(
                id      = obj.optString("id", UUID.randomUUID().toString()),
                trigger = obj.getString("trigger"),
                name    = obj.getString("name"),
                prompt  = obj.getString("prompt")
            )
        }
    } catch (_: Exception) { emptyList() }
}

fun saveCustomCommands(context: Context, commands: List<CustomCommand>) {
    val arr = JSONArray()
    commands.forEach { cc ->
        arr.put(JSONObject().apply {
            put("id",      cc.id)
            put("trigger", cc.trigger)
            put("name",    cc.name)
            put("prompt",  cc.prompt)
        })
    }
    context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
        .edit().putString("custom_commands", arr.toString()).apply()
}

// ─── Navigation ───────────────────────────────────────────────────────────────
enum class NavTab { Home, Commands, Explore, Settings }

data class NavItem(
    val tab: NavTab,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

val navItems = listOf(
    NavItem(NavTab.Home,     "Home",     Icons.Filled.Home,    Icons.Outlined.Home),
    NavItem(NavTab.Commands, "Commands", Icons.Filled.Bolt,    Icons.Outlined.Bolt),
    NavItem(NavTab.Explore,  "Explore",  Icons.Filled.Explore, Icons.Outlined.Explore),
    NavItem(NavTab.Settings, "Settings", Icons.Filled.Person,  Icons.Outlined.Person)
)

// ─── Entry ────────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background   = Bg,
                    surface      = Color(0xFF0D0D1F),
                    primary      = Accent,
                    onPrimary    = TextPrim,
                    onBackground = TextPrim,
                    onSurface    = TextPrim
                )
            ) { TypeShiftApp() }
        }
    }
}

// ─── Animated Aurora background ───────────────────────────────────────────────
@Composable
fun AuroraBg(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val tr = rememberInfiniteTransition(label = "aurora")
    val c1 by tr.animateColor(
        initialValue  = Color(0xFF0D0A20),
        targetValue   = Color(0xFF080D1E),
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "c1"
    )
    val c2 by tr.animateColor(
        initialValue  = Color(0xFF06080F),
        targetValue   = Color(0xFF0C0A1C),
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "c2"
    )
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(c1, Color(0xFF080812), c2),
                start = Offset(0f, 0f),
                end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        ),
        content = content
    )
}

// ─── Root shell ───────────────────────────────────────────────────────────────
@Composable
fun TypeShiftApp() {
    var selectedTab by remember { mutableStateOf(NavTab.Home) }

    AuroraBg(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                TypeShiftNavBar(selected = selectedTab, onSelect = { selectedTab = it })
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    NavTab.Home     -> HomeTab()
                    NavTab.Commands -> CommandsTab()
                    NavTab.Explore  -> ExploreTab()
                    NavTab.Settings -> SettingsTab()
                }
            }
        }
    }
}

// ─── Bottom nav ───────────────────────────────────────────────────────────────
@Composable
fun TypeShiftNavBar(selected: NavTab, onSelect: (NavTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEA080812))))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.6.dp)
                .background(Color.White.copy(alpha = 0.08f))
        )
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier       = Modifier.padding(top = 0.6.dp)
        ) {
            navItems.forEach { item ->
                val isSelected = selected == item.tab
                val iconScale by animateFloatAsState(
                    targetValue   = if (isSelected) 1.12f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label         = "scale"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) AccentAlt else TextSec,
                    label       = "tint"
                )
                NavigationBarItem(
                    selected = isSelected,
                    onClick  = { onSelect(item.tab) },
                    icon = {
                        Icon(
                            imageVector        = if (isSelected) item.filledIcon else item.outlinedIcon,
                            contentDescription = item.label,
                            tint               = iconTint,
                            modifier           = Modifier.scale(iconScale)
                        )
                    },
                    label = {
                        Text(
                            item.label,
                            fontFamily = AppFont,
                            fontSize   = 10.sp,
                            color      = iconTint,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor      = Accent.copy(alpha = 0.20f),
                        selectedIconColor   = AccentAlt,
                        unselectedIconColor = TextSec,
                        selectedTextColor   = AccentAlt,
                        unselectedTextColor = TextSec
                    )
                )
            }
        }
    }
}

// ─── HOME TAB ─────────────────────────────────────────────────────────────────
@Composable
fun HomeTab() {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var serviceOn by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            serviceOn = isAccessibilityServiceEnabled(context)
        }
    }

    val quickCommands = listOf(
        "?fix"     to "Fix Grammar",
        "?improve" to "Improve",
        "?formal"  to "Formal",
        "?casual"  to "Casual",
        "?shorter" to "Shorter",
        "?joke"    to "Get a Joke"
    )

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            HomeHeader(serviceOn) {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            StatsRow(context)
        }
        item {
            Spacer(Modifier.height(24.dp))
            SectionLabel("Quick Commands")
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(quickCommands) { (trigger, label) ->
                    QuickCommandChip(trigger, label, commandColor(trigger))
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            SectionLabel("Pro Tip")
            Spacer(Modifier.height(12.dp))
            TipCard()
        }
    }
}

@Composable
fun HomeHeader(serviceOn: Boolean, onEnable: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0x601A0A40), Color.Transparent)))
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 24.dp)
    ) {
        Column {
            // Logo row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF4C1D95), Accent)))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(listOf(Color.White.copy(0.30f), Color.White.copy(0.05f))),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("T›", fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = TextPrim)
                }
                Column {
                    Text(
                        "TypeShift",
                        fontFamily    = AppFont,
                        fontWeight    = FontWeight.ExtraBold,
                        fontSize      = 26.sp,
                        color         = TextPrim,
                        letterSpacing = (-0.5).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(if (serviceOn) Success else Danger))
                        Text(
                            if (serviceOn) "Service Active" else "Service Disabled",
                            fontFamily = AppFont, fontSize = 12.sp,
                            color      = if (serviceOn) Success else Danger
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Status glass card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Surface2)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(
                            if (serviceOn) Success.copy(0.35f) else Danger.copy(0.30f),
                            Color.White.copy(0.05f)
                        )),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                if (serviceOn) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Success.copy(0.12f)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Success, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("Running in background", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                            Text("Type any trigger in any app", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Danger.copy(0.12f)),
                                contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Warning, null, tint = Danger, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text("Service disabled", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                                Text("Tap below to enable", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)
                            }
                        }
                        Button(
                            onClick  = onEnable,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape    = RoundedCornerShape(50.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) {
                            Text("Enable Accessibility Service", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(context: Context) {
    val prefs       = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
    val hasKey      = selectedProvider(context).let { !it.needsKey || apiKeyFor(context, it).isNotEmpty() }
    val customCount = loadCustomCommands(context).size

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatChip(Modifier.weight(1f), Icons.Filled.Bolt, "20",           "Commands")
        StatChip(Modifier.weight(1f), Icons.Filled.Star, "$customCount", "Custom")
        StatChip(Modifier.weight(1f), Icons.Filled.Key,  if (hasKey) "Set" else "None", "API Key",
            valueColor = if (hasKey) Success else Danger)
    }
}

@Composable
fun StatChip(modifier: Modifier = Modifier, icon: ImageVector, value: String, label: String, valueColor: Color = TextPrim) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.13f), Color.White.copy(0.02f))), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = AccentAlt, modifier = Modifier.size(18.dp))
            Text(value, fontFamily = AppFont, fontWeight = FontWeight.Bold,   fontSize = 18.sp, color = valueColor)
            Text(label, fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = 11.sp, color = TextSec)
        }
    }
}

@Composable
fun QuickCommandChip(trigger: String, label: String, color: Color = Accent) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(trigger, fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = color)
            Text(label,   fontFamily = AppFont, fontSize = 11.sp, color = TextSec)
        }
    }
}

@Composable
fun TipCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0x2A1A0A5A), Color(0x180A1428))))
            .border(1.dp, Brush.linearGradient(listOf(Accent.copy(0.30f), Color(0xFF6366F1).copy(0.08f))), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Accent.copy(0.15f)), contentAlignment = Alignment.Center) {
                Text("💡", fontSize = 20.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tip of the day", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccentAlt)
                Text(
                    "Type your text, add ?formal at the end and press space — TypeShift rewrites it instantly in any app.",
                    fontFamily = AppFont, fontSize = 13.sp, color = TextSec, lineHeight = 20.sp
                )
            }
        }
    }
}

// ─── COMMANDS TAB ─────────────────────────────────────────────────────────────
@Composable
fun CommandsTab() {
    val context = LocalContext.current
    var customCommands  by remember { mutableStateOf(loadCustomCommands(context)) }
    var showAddSheet    by remember { mutableStateOf(false) }
    var editTarget      by remember { mutableStateOf<CustomCommand?>(null) }
    var builtinExpanded by remember { mutableStateOf(true) }
    var searchQuery     by remember { mutableStateOf("") }

    val builtinCommands = listOf(
        "?fix"          to "Fix grammar & spelling",
        "?improve"      to "Improve clarity",
        "?formal"       to "Professional tone",
        "?casual"       to "Friendly tone",
        "?shorter"      to "Make it concise",
        "?longer"       to "Expand with detail",
        "?emoji"        to "Add relevant emojis",
        "?reply"        to "Generate a reply",
        "?human"        to "Sound more human",
        "?hinglish"     to "Convert to Hinglish",
        "?roast"        to "Funny roast",
        "?joke"         to "Random joke",
        "?tweet"        to "Shrink to a tweet",
        "?bullet"       to "Bullet points",
        "?subject"      to "Email subject line",
        "?eli5"         to "Explain like I'm 5",
        "?tldr"         to "One-line summary",
        "?headline"     to "Catchy headline",
        "?undo"         to "Restore original",
        "?translate:XX" to "Translate any language"
    )

    val filteredBuiltin = builtinCommands.filter { (trigger, desc) ->
        searchQuery.isBlank() ||
            trigger.contains(searchQuery, ignoreCase = true) ||
            desc.contains(searchQuery, ignoreCase = true)
    }
    val filteredCustom = customCommands.filter { cmd ->
        searchQuery.isBlank() ||
            cmd.name.contains(searchQuery, ignoreCase = true) ||
            cmd.trigger.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {

            // Header
            item {
                Spacer(Modifier.height(52.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Commands", fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TextPrim)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(Surface3)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${builtinCommands.size + customCommands.size}",
                            fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentAlt
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Raycast-style search bar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface2)
                        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.12f), Accent.copy(0.06f))), RoundedCornerShape(16.dp))
                ) {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder   = { Text("Search commands…", fontFamily = AppFont, fontSize = 14.sp, color = TextTert) },
                        leadingIcon   = { Icon(Icons.Outlined.Search, null, tint = TextSec, modifier = Modifier.size(20.dp)) },
                        trailingIcon  = if (searchQuery.isNotEmpty()) {{
                            Icon(Icons.Filled.Clear, null, tint = TextSec,
                                modifier = Modifier.size(18.dp).clickable { searchQuery = "" })
                        }} else null,
                        modifier    = Modifier.fillMaxWidth(),
                        singleLine  = true,
                        colors      = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor     = TextPrim,
                            unfocusedTextColor   = TextPrim,
                            cursorColor          = AccentAlt
                        ),
                        textStyle = TextStyle(fontFamily = AppFont, fontSize = 14.sp, color = TextPrim),
                        shape     = RoundedCornerShape(16.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // Built-in section
            if (filteredBuiltin.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            .clickable { builtinExpanded = !builtinExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        SectionLabel("Built-in  •  ${filteredBuiltin.size}")
                        Icon(
                            if (builtinExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            null, tint = TextSec, modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (builtinExpanded) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Surface2)
                                .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.02f))), RoundedCornerShape(20.dp))
                        ) {
                            Column {
                                filteredBuiltin.forEachIndexed { i, (trigger, desc) ->
                                    CommandRow(trigger, desc)
                                    if (i < filteredBuiltin.lastIndex)
                                        HorizontalDivider(color = Color.White.copy(0.04f), thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            // My Commands section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    SectionLabel("My Commands  •  ${filteredCustom.size}")
                }
                Spacer(Modifier.height(8.dp))
            }

            if (filteredCustom.isEmpty() && customCommands.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Surface2)
                            .border(1.dp, Color.White.copy(0.06f), RoundedCornerShape(20.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✦", fontSize = 28.sp)
                            Text("No custom commands yet", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                            Text("Tap + to create your first one", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)
                        }
                    }
                }
            } else {
                items(filteredCustom, key = { it.id }) { cmd ->
                    CustomCommandItem(
                        cmd      = cmd,
                        onEdit   = { editTarget = it; showAddSheet = true },
                        onDelete = {
                            customCommands = customCommands.filter { it.id != cmd.id }
                            saveCustomCommands(context, customCommands)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick        = { editTarget = null; showAddSheet = true },
            modifier       = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = Accent,
            contentColor   = TextPrim,
            shape          = CircleShape
        ) {
            Icon(Icons.Filled.Add, "Add command", modifier = Modifier.size(26.dp))
        }
    }

    if (showAddSheet) {
        AddCommandSheet(
            existing  = editTarget,
            onDismiss = { showAddSheet = false; editTarget = null },
            onSave    = { cmd ->
                customCommands = if (editTarget != null)
                    customCommands.map { if (it.id == cmd.id) cmd else it }
                else
                    customCommands + cmd
                saveCustomCommands(context, customCommands)
                showAddSheet = false
                editTarget   = null
            }
        )
    }
}

@Composable
fun CommandRow(trigger: String, desc: String) {
    val cat = commandColor(trigger)
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(cat.copy(alpha = 0.12f))
                .border(1.dp, cat.copy(0.32f), RoundedCornerShape(50.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(trigger, fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = cat, maxLines = 1)
        }
        Text(desc, fontFamily = AppFont, fontSize = 14.sp, color = TextSec, modifier = Modifier.weight(1f))
    }
}

@Composable
fun CustomCommandItem(cmd: CustomCommand, onEdit: (CustomCommand) -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.02f))), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(Accent.copy(0.15f))
                    .border(1.dp, Accent.copy(0.22f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text("✦", fontSize = 18.sp) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(cmd.name,    fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                Text(cmd.trigger, fontFamily = AppFont, fontSize = 12.sp, color = AccentAlt)
                Text(cmd.prompt,  fontFamily = AppFont, fontSize = 12.sp, color = TextSec, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                Icon(Icons.Filled.MoreVert, null, tint = TextSec, modifier = Modifier.clickable { showMenu = true }.padding(4.dp))
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = Color(0xFF14122A)) {
                    DropdownMenuItem(
                        text        = { Text("Edit",   fontFamily = AppFont, color = TextPrim) },
                        onClick     = { showMenu = false; onEdit(cmd) },
                        leadingIcon = { Icon(Icons.Filled.Edit,   null, tint = Accent) }
                    )
                    DropdownMenuItem(
                        text        = { Text("Delete", fontFamily = AppFont, color = Danger) },
                        onClick     = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Danger) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCommandSheet(existing: CustomCommand?, onDismiss: () -> Unit, onSave: (CustomCommand) -> Unit) {
    var name    by remember { mutableStateOf(existing?.name    ?: "") }
    var trigger by remember { mutableStateOf(existing?.trigger ?: "?") }
    var prompt  by remember { mutableStateOf(existing?.prompt  ?: "") }
    var error   by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF0E0C1E),
        dragHandle = {
            Box(
                modifier = Modifier.padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Surface4)
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (existing != null) "Edit Command" else "New Command",
                fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrim
            )
            SheetField("Name",    name,    "e.g. Formal Email")                          { name    = it }
            SheetField("Trigger", trigger, "e.g. ?email") { trigger = if (it.startsWith("?")) it else "?$it" }
            SheetField("Prompt",  prompt,  "e.g. Rewrite as a professional email…", singleLine = false) { prompt = it }
            if (error != null) Text(error!!, fontFamily = AppFont, fontSize = 13.sp, color = Danger)
            Button(
                onClick = {
                    when {
                        name.isBlank()    -> error = "Name is required"
                        trigger.length < 2 || !trigger.startsWith("?") -> error = "Trigger must start with ?"
                        prompt.isBlank()  -> error = "Prompt is required"
                        else -> onSave(CustomCommand(
                            id      = existing?.id ?: UUID.randomUUID().toString(),
                            trigger = trigger.trim().lowercase(),
                            name    = name.trim(),
                            prompt  = prompt.trim()
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Text("Save Command", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun SheetField(label: String, value: String, placeholder: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextSec)
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface3)
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
        ) {
            OutlinedTextField(
                value         = value,
                onValueChange = onValueChange,
                placeholder   = { Text(placeholder, fontFamily = AppFont, fontSize = 14.sp, color = TextTert) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = singleLine,
                maxLines      = if (singleLine) 1 else 4,
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Accent.copy(0.4f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor     = TextPrim, unfocusedTextColor = TextPrim, cursorColor = AccentAlt
                ),
                textStyle = TextStyle(fontFamily = AppFont, fontSize = 14.sp, color = TextPrim),
                shape     = RoundedCornerShape(14.dp)
            )
        }
    }
}

// ─── EXPLORE TAB ──────────────────────────────────────────────────────────────
@Composable
fun ExploreTab() {
    val useCases = listOf(
        Triple("✍️", "Writing",      "Use ?improve and ?formal to polish emails, essays, and reports in seconds."),
        Triple("💬", "Messaging",    "Add ?casual or ?emoji to make your texts more fun and expressive."),
        Triple("🐦", "Social Media", "Turn any long thought into a viral tweet with ?tweet."),
        Triple("📧", "Email",        "Generate the perfect subject line with ?subject — never blank again."),
        Triple("🌍", "Translate",    "Type ?translate:french to instantly translate to any language."),
        Triple("😂", "Just for Fun", "Type ?joke anywhere to get an instant AI-generated joke.")
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Spacer(Modifier.height(52.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Explore",  fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TextPrim)
                Text("Discover what TypeShift can do", fontFamily = AppFont, fontSize = 14.sp, color = TextSec)
            }
            Spacer(Modifier.height(24.dp))
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0x28221060), Color(0x180A1428))))
                    .border(1.dp, Brush.linearGradient(listOf(Accent.copy(0.30f), Color(0xFF6366F1).copy(0.08f))), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("How it works", fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrim)
                    FlowStep("1", "Type your text in any app")
                    FlowArrow()
                    FlowStep("2", "Append a command like ?fix or ?formal")
                    FlowArrow()
                    FlowStep("3", "Press space — text is rewritten instantly")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        item { SectionLabel("Use Cases"); Spacer(Modifier.height(12.dp)) }
        items(useCases) { (emoji, title, desc) ->
            ExploreCard(emoji = emoji, title = title, desc = desc)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun FlowStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(Accent.copy(0.18f))
                .border(1.dp, Accent.copy(0.32f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentAlt)
        }
        Text(text, fontFamily = AppFont, fontSize = 14.sp, color = TextSec)
    }
}

@Composable
fun FlowArrow() {
    Row(modifier = Modifier.padding(start = 12.dp)) {
        Text("│", fontSize = 14.sp, color = TextTert)
    }
}

@Composable
fun ExploreCard(emoji: String, title: String, desc: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.10f), Color.White.copy(0.02f))), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Surface3)
                    .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 22.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                Text(desc,  fontFamily = AppFont, fontSize = 13.sp, color = TextSec, lineHeight = 19.sp)
            }
        }
    }
}

// ─── SETTINGS TAB ─────────────────────────────────────────────────────────────
@Composable
fun SettingsTab() {
    val context = LocalContext.current
    var providerId by remember { mutableStateOf(selectedProviderId(context)) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            Spacer(Modifier.height(52.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Settings", fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TextPrim)
                Text("Configure TypeShift", fontFamily = AppFont, fontSize = 14.sp, color = TextSec)
            }
            Spacer(Modifier.height(24.dp))
        }
        item {
            ProviderCard(context, providerId) { providerId = it }
            Spacer(Modifier.height(16.dp))
        }
        item { TemperatureCard(context);          Spacer(Modifier.height(16.dp)) }
        item { ModelInfoCard(context, providerId); Spacer(Modifier.height(16.dp)) }
        item { AboutCard() }
    }
}

@Composable
fun ProviderCard(context: Context, selectedId: String, onSelect: (String) -> Unit) {
    val provider = providerById(selectedId)

    // Field state resets whenever the selected provider changes.
    var key      by remember(selectedId) { mutableStateOf(apiKeyFor(context, provider)) }
    var endpoint by remember(selectedId) { mutableStateOf(endpointFor(context, provider)) }
    var model    by remember(selectedId) { mutableStateOf(modelFor(context, provider)) }
    var saved    by remember(selectedId) { mutableStateOf(false) }
    var showKey  by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.13f), Accent.copy(0.06f))), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Key, null, tint = AccentAlt, modifier = Modifier.size(20.dp))
                Text("AI Provider", fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrim)
            }
            Text("Choose which AI powers TypeShift.", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AI_PROVIDERS) { p ->
                    val selected = p.id == selectedId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (selected) Accent else Surface3)
                            .border(1.dp, if (selected) Accent else Color.White.copy(0.08f), RoundedCornerShape(50.dp))
                            .clickable { setSelectedProviderId(context, p.id); onSelect(p.id) }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            p.name,
                            fontFamily = AppFont,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize   = 13.sp,
                            color      = if (selected) Color.White else TextSec
                        )
                    }
                }
            }

            if (provider.needsKey || provider.custom) {
                Text(
                    if (provider.needsKey) "${provider.name} API Key" else "API Key (optional)",
                    fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrim
                )
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Surface3).border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
                ) {
                    OutlinedTextField(
                        value                = key,
                        onValueChange        = { key = it; saved = false },
                        placeholder          = { Text(provider.keyHint, fontFamily = AppFont, fontSize = 14.sp, color = TextTert) },
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier             = Modifier.fillMaxWidth(),
                        singleLine           = true,
                        colors               = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Accent.copy(0.4f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor     = TextPrim, unfocusedTextColor = TextPrim, cursorColor = AccentAlt
                        ),
                        textStyle    = TextStyle(fontFamily = AppFont, fontSize = 14.sp, color = TextPrim),
                        shape        = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            Text(
                                if (showKey) "Hide" else "Show",
                                modifier = Modifier.clickable { showKey = !showKey }.padding(8.dp),
                                fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = AccentAlt
                            )
                        }
                    )
                }
            }

            if (provider.custom) {
                Text("Server URL", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrim)
                ProviderTextField(endpoint, "https://…/v1/chat/completions") { endpoint = it; saved = false }
            }

            Text("Model", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrim)
            ProviderTextField(model, provider.defaultModel.ifBlank { "model name" }) { model = it; saved = false }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick  = {
                        saveApiKey(context, provider, key)
                        saveModel(context, provider, model)
                        if (provider.custom) saveEndpoint(context, provider, endpoint)
                        saved = true
                    },
                    modifier = Modifier.height(46.dp),
                    shape    = RoundedCornerShape(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("Save", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                if (saved) Text("✓  Saved", fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Success)
            }

            if (provider.getKeyUrl.isNotEmpty()) {
                Text(
                    if (provider.needsKey) "Get a key at ${provider.getKeyUrl}" else "Setup guide: ${provider.getKeyUrl}",
                    fontFamily = AppFont, fontSize = 12.sp, color = TextTert
                )
            }
        }
    }
}

@Composable
private fun ProviderTextField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Surface3).border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(14.dp))
    ) {
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, fontFamily = AppFont, fontSize = 14.sp, color = TextTert) },
            modifier      = Modifier.fillMaxWidth(),
            singleLine    = true,
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Accent.copy(0.4f),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor     = TextPrim, unfocusedTextColor = TextPrim, cursorColor = AccentAlt
            ),
            textStyle = TextStyle(fontFamily = AppFont, fontSize = 14.sp, color = TextPrim),
            shape     = RoundedCornerShape(14.dp)
        )
    }
}

@Composable
fun TemperatureCard(context: Context) {
    val prefs = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
    var temperature by remember { mutableStateOf(prefs.getFloat("ai_temperature", 0.7f)) }

    val tempLabel = when {
        temperature < 0.4f -> "Precise"
        temperature < 0.8f -> "Balanced"
        temperature < 1.1f -> "Creative"
        else               -> "Wild"
    }
    val tempColor = when {
        temperature < 0.4f -> CatEdit
        temperature < 0.8f -> Accent
        temperature < 1.1f -> Warning
        else               -> Danger
    }

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.13f), tempColor.copy(0.08f))), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🌡️", fontSize = 18.sp)
                Text("AI Temperature", fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrim)
            }
            Text("Controls how creative or deterministic the AI output is.", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tempLabel, fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = tempColor)
                Box(modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(tempColor.copy(0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("%.1f".format(temperature), fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = tempColor)
                }
            }
            Slider(
                value                 = temperature,
                onValueChange         = { temperature = it },
                onValueChangeFinished = { prefs.edit().putFloat("ai_temperature", temperature).apply() },
                valueRange            = 0f..1.5f,
                steps                 = 14,
                colors                = SliderDefaults.colors(thumbColor = tempColor, activeTrackColor = tempColor, inactiveTrackColor = Surface4)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0.0 Precise", fontFamily = AppFont, fontSize = 11.sp, color = TextTert)
                Text("1.5 Wild",    fontFamily = AppFont, fontSize = 11.sp, color = TextTert)
            }
        }
    }
}

@Composable
fun ModelInfoCard(context: Context, selectedId: String) {
    val provider = providerById(selectedId)
    val model    = modelFor(context, provider)

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.13f), Color.White.copy(0.02f))), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4C1D95), Accent))),
                contentAlignment = Alignment.Center
            ) { Text("⚡", fontSize = 20.sp) }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Active Model", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                Text(model.ifBlank { "not set" }, fontFamily = AppFont, fontSize = 13.sp, color = AccentAlt)
                Text("via ${provider.name}", fontFamily = AppFont, fontSize = 12.sp, color = TextSec)
            }
        }
    }
}

@Composable
fun AboutCard() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface2)
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.13f), Color.White.copy(0.02f))), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("About", fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version",  fontFamily = AppFont, fontSize = 14.sp, color = TextSec)
                Text("1.3",      fontFamily = AppFont, fontSize = 14.sp, color = TextPrim)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Platform", fontFamily = AppFont, fontSize = 14.sp, color = TextSec)
                Text("Android",  fontFamily = AppFont, fontSize = 14.sp, color = TextPrim)
            }
            HorizontalDivider(color = Color.White.copy(0.06f), thickness = 0.6.dp)
            Text(
                "TypeShift works in every app — no copy-paste, no switching. AI rewrites your text in place.",
                fontFamily = AppFont, fontSize = 13.sp, color = TextSec, lineHeight = 20.sp
            )
        }
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        modifier      = Modifier.padding(horizontal = 20.dp),
        fontFamily    = AppFont,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        color         = TextSec,
        letterSpacing = 0.8.sp
    )
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${AiAccessibilityService::class.java.name}"
    val enabled  = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
}
