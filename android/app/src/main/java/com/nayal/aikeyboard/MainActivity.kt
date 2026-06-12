package com.nayal.aikeyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.font.FontFamily
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

// ─────────────────────────────────────────────
//  Colors — AMOLED dark
// ─────────────────────────────────────────────
val Bg        = Color(0xFF000000)
val Surface1  = Color(0xFF0D0D0D)
val Surface2  = Color(0xFF1A1A1A)
val Surface3  = Color(0xFF242424)
val Surface4  = Color(0xFF2E2E2E)
val Accent    = Color(0xFF7B61FF)
val AccentAlt = Color(0xFF9E8FFF)
val AccentGlow= Color(0x407B61FF)
val TextPrim  = Color(0xFFFFFFFF)
val TextSec   = Color(0xFF8A8A8A)
val TextTert  = Color(0xFF444444)
val Success   = Color(0xFF34C759)
val Danger    = Color(0xFFFF3B30)
val Warning   = Color(0xFFFF9F0A)

// ─────────────────────────────────────────────
//  Font
// ─────────────────────────────────────────────
private val gFonts = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)
val AppFont = FontFamily(
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = androidx.compose.ui.text.font.FontWeight.Normal),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = androidx.compose.ui.text.font.FontWeight.Medium),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = androidx.compose.ui.text.font.FontWeight.Bold),
    Font(googleFont = GoogleFont("Outfit"), fontProvider = gFonts, weight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
)

// ─────────────────────────────────────────────
//  Custom command model
// ─────────────────────────────────────────────
data class CustomCommand(
    val id: String = UUID.randomUUID().toString(),
    val trigger: String,
    val name: String,
    val prompt: String
)

fun loadCustomCommands(context: Context): List<CustomCommand> {
    val prefs = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("custom_commands", "[]") ?: "[]"
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

// ─────────────────────────────────────────────
//  Navigation
// ─────────────────────────────────────────────
enum class NavTab { Home, Commands, Explore, Settings }

data class NavItem(
    val tab: NavTab,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
)

val navItems = listOf(
    NavItem(NavTab.Home,     "Home",     Icons.Filled.Home,     Icons.Outlined.Home),
    NavItem(NavTab.Commands, "Commands", Icons.Filled.Bolt,     Icons.Outlined.Bolt),
    NavItem(NavTab.Explore,  "Explore",  Icons.Filled.Explore,  Icons.Outlined.Explore),
    NavItem(NavTab.Settings, "Settings", Icons.Filled.Person,   Icons.Outlined.Person)
)

// ─────────────────────────────────────────────
//  Entry
// ─────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background   = Bg,
                    surface      = Surface1,
                    primary      = Accent,
                    onPrimary    = TextPrim,
                    onBackground = TextPrim,
                    onSurface    = TextPrim
                )
            ) {
                TypeShiftApp()
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Root app shell with bottom nav
// ─────────────────────────────────────────────
@Composable
fun TypeShiftApp() {
    var selectedTab by remember { mutableStateOf(NavTab.Home) }

    Scaffold(
        containerColor = Bg,
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

// ─────────────────────────────────────────────
//  Bottom navigation bar (Instagram-style)
// ─────────────────────────────────────────────
@Composable
fun TypeShiftNavBar(selected: NavTab, onSelect: (NavTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1)
    ) {
        // Thin top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Surface3)
        )
        NavigationBar(
            containerColor  = Color.Transparent,
            tonalElevation  = 0.dp,
            modifier        = Modifier.padding(top = 0.5.dp)
        ) {
            navItems.forEach { item ->
                val isSelected = selected == item.tab
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.1f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "iconScale"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Accent else TextSec,
                    label = "iconTint"
                )

                NavigationBarItem(
                    selected = isSelected,
                    onClick  = { onSelect(item.tab) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.filledIcon else item.outlinedIcon,
                            contentDescription = item.label,
                            tint     = iconTint,
                            modifier = Modifier.scale(iconScale)
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
                        indicatorColor         = AccentGlow,
                        selectedIconColor      = Accent,
                        unselectedIconColor    = TextSec,
                        selectedTextColor      = Accent,
                        unselectedTextColor    = TextSec
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  HOME TAB
// ─────────────────────────────────────────────
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
        "?fix" to "Fix grammar",
        "?improve" to "Improve",
        "?formal" to "Formal",
        "?casual" to "Casual",
        "?shorter" to "Shorter",
        "?emoji" to "Add emojis"
    )

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(Bg),
        contentPadding      = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header
        item {
            HomeHeader(serviceOn) {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        // Stats row
        item {
            Spacer(Modifier.height(20.dp))
            StatsRow(context)
        }

        // Quick commands
        item {
            Spacer(Modifier.height(24.dp))
            SectionLabel("Quick Commands")
            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding      = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(quickCommands) { (trigger, label) ->
                    QuickCommandChip(trigger, label)
                }
            }
        }

        // Tip card
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
    val statusColor = if (serviceOn) Success else Danger
    val statusText  = if (serviceOn) "Active" else "Disabled"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0D0B1A), Bg))
            )
            .padding(horizontal = 20.dp)
            .padding(top = 52.dp, bottom = 24.dp)
    ) {
        Column {
            // Logo row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Logo pill with black border
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(Color(0xFF2A2A2A), Color(0xFF111111))),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF3D2BFF), Color(0xFF7B61FF)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "T›",
                        fontFamily = AppFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 18.sp,
                        color      = TextPrim
                    )
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
                    Row(
                        verticalAlignment  = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            "Service $statusText",
                            fontFamily = AppFont,
                            fontSize   = 12.sp,
                            color      = statusColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Status card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = if (serviceOn) Success.copy(alpha = 0.25f) else Danger.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(Surface2)
                    .padding(20.dp)
            ) {
                if (serviceOn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Success.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Success, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("Running in background", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                            Text("Type any trigger in any app", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Danger.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Warning, null, tint = Danger, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text("Service disabled", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                                Text("Enable to start detecting triggers", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)
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
    val hasKey      = (prefs.getString("gemini_api_key", "") ?: "").isNotEmpty()
    val customCount = loadCustomCommands(context).size

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(
            modifier = Modifier.weight(1f),
            icon     = Icons.Filled.Bolt,
            value    = "19",
            label    = "Commands"
        )
        StatChip(
            modifier = Modifier.weight(1f),
            icon     = Icons.Filled.Star,
            value    = "$customCount",
            label    = "Custom"
        )
        StatChip(
            modifier = Modifier.weight(1f),
            icon     = Icons.Filled.Key,
            value    = if (hasKey) "Set" else "None",
            label    = "API Key",
            valueColor = if (hasKey) Success else Danger
        )
    }
}

@Composable
fun StatChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    valueColor: Color = TextPrim
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
            Text(value, fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = valueColor)
            Text(label, fontFamily = AppFont, fontSize = 11.sp, color = TextSec)
        }
    }
}

@Composable
fun QuickCommandChip(trigger: String, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .border(1.dp, AccentGlow, RoundedCornerShape(50.dp))
            .background(Accent.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(trigger, fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AccentAlt)
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
            .background(
                Brush.linearGradient(listOf(Color(0xFF1A1040), Color(0xFF0D0D1A)))
            )
            .border(1.dp, Accent.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("💡", fontSize = 20.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tip of the day", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccentAlt)
                Text(
                    "Type your text, then add ?formal at the end and press space — TypeShift rewrites it instantly in any app.",
                    fontFamily = AppFont,
                    fontSize   = 13.sp,
                    color      = TextSec,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  COMMANDS TAB
// ─────────────────────────────────────────────
@Composable
fun CommandsTab() {
    val context = LocalContext.current
    var customCommands by remember { mutableStateOf(loadCustomCommands(context)) }
    var showAddSheet   by remember { mutableStateOf(false) }
    var editTarget     by remember { mutableStateOf<CustomCommand?>(null) }
    var builtinExpanded by remember { mutableStateOf(true) }

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
        "?tweet"        to "Shrink to a tweet",
        "?bullet"       to "Bullet points",
        "?subject"      to "Email subject line",
        "?eli5"         to "Explain like I'm 5",
        "?tldr"         to "One-line summary",
        "?headline"     to "Catchy headline",
        "?undo"         to "Restore original",
        "?translate:XX" to "Translate any language"
    )

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Spacer(Modifier.height(52.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Commands", fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TextPrim)
                    Text("${builtinCommands.size + customCommands.size} total", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)
                }
                Spacer(Modifier.height(20.dp))
            }

            // Built-in section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .clickable { builtinExpanded = !builtinExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    SectionLabel("Built-in  •  ${builtinCommands.size}")
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
                    ) {
                        Column {
                            builtinCommands.forEachIndexed { index, (trigger, desc) ->
                                CommandRow(trigger, desc)
                                if (index < builtinCommands.lastIndex) {
                                    HorizontalDivider(color = Surface3, thickness = 0.6.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // My Commands section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    SectionLabel("My Commands  •  ${customCommands.size}")
                }
                Spacer(Modifier.height(8.dp))
            }

            if (customCommands.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Surface3, RoundedCornerShape(20.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("✦", fontSize = 28.sp)
                            Text("No custom commands yet", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                            Text("Tap + to create your first one", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)
                        }
                    }
                }
            } else {
                items(customCommands, key = { it.id }) { cmd ->
                    CustomCommandItem(
                        cmd = cmd,
                        onEdit = { editTarget = it; showAddSheet = true },
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
            onClick            = { editTarget = null; showAddSheet = true },
            modifier           = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor     = Accent,
            contentColor       = TextPrim,
            shape              = CircleShape
        ) {
            Icon(Icons.Filled.Add, "Add command", modifier = Modifier.size(26.dp))
        }
    }

    if (showAddSheet) {
        AddCommandSheet(
            existing = editTarget,
            onDismiss = { showAddSheet = false; editTarget = null },
            onSave = { cmd ->
                customCommands = if (editTarget != null) {
                    customCommands.map { if (it.id == cmd.id) cmd else it }
                } else {
                    customCommands + cmd
                }
                saveCustomCommands(context, customCommands)
                showAddSheet = false
                editTarget = null
            }
        )
    }
}

@Composable
fun CommandRow(trigger: String, desc: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(Accent.copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(trigger, fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AccentAlt, maxLines = 1)
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
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(cmd.name, fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                Text(cmd.trigger, fontFamily = AppFont, fontSize = 12.sp, color = AccentAlt)
                Text(cmd.prompt, fontFamily = AppFont, fontSize = 12.sp, color = TextSec, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                Icon(
                    Icons.Filled.MoreVert, null, tint = TextSec,
                    modifier = Modifier.clickable { showMenu = true }.padding(4.dp)
                )
                DropdownMenu(
                    expanded         = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor   = Surface3
                ) {
                    DropdownMenuItem(
                        text    = { Text("Edit", fontFamily = AppFont, color = TextPrim) },
                        onClick = { showMenu = false; onEdit(cmd) },
                        leadingIcon = { Icon(Icons.Filled.Edit, null, tint = Accent) }
                    )
                    DropdownMenuItem(
                        text    = { Text("Delete", fontFamily = AppFont, color = Danger) },
                        onClick = { showMenu = false; onDelete() },
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
        containerColor   = Surface2,
        dragHandle       = {
            Box(
                modifier = Modifier.padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Surface4)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                if (existing != null) "Edit Command" else "New Command",
                fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrim
            )

            SheetField(label = "Name", value = name, placeholder = "e.g. Formal Email") { name = it }
            SheetField(label = "Trigger", value = trigger, placeholder = "e.g. ?email") {
                trigger = if (it.startsWith("?")) it else "?$it"
            }
            SheetField(label = "Prompt", value = prompt, placeholder = "e.g. Rewrite as a professional email…", singleLine = false) { prompt = it }

            if (error != null) {
                Text(error!!, fontFamily = AppFont, fontSize = 13.sp, color = Danger)
            }

            Button(
                onClick = {
                    when {
                        name.isBlank()    -> error = "Name is required"
                        trigger.length < 2 || !trigger.startsWith("?") -> error = "Trigger must start with ? and have a keyword"
                        prompt.isBlank()  -> error = "Prompt is required"
                        else -> {
                            val cmd = CustomCommand(
                                id      = existing?.id ?: UUID.randomUUID().toString(),
                                trigger = trigger.trim().lowercase(),
                                name    = name.trim(),
                                prompt  = prompt.trim()
                            )
                            onSave(cmd)
                        }
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
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface3)
        ) {
            OutlinedTextField(
                value            = value,
                onValueChange    = onValueChange,
                placeholder      = { Text(placeholder, fontFamily = AppFont, fontSize = 14.sp, color = TextTert) },
                modifier         = Modifier.fillMaxWidth(),
                singleLine       = singleLine,
                maxLines         = if (singleLine) 1 else 4,
                colors           = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Accent.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor     = TextPrim,
                    unfocusedTextColor   = TextPrim,
                    cursorColor          = Accent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = AppFont, fontSize = 14.sp, color = TextPrim),
                shape     = RoundedCornerShape(14.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
//  EXPLORE TAB
// ─────────────────────────────────────────────
@Composable
fun ExploreTab() {
    val useCases = listOf(
        "✍️" to "Writing" to "Use ?improve and ?formal to polish emails, essays, and reports in seconds.",
        "💬" to "Messaging" to "Add ?casual or ?emoji to make your texts more fun and expressive.",
        "🐦" to "Social Media" to "Turn any long thought into a viral tweet with ?tweet.",
        "📧" to "Email" to "Generate the perfect subject line with ?subject — never blank again.",
        "🌍" to "Translate" to "Type ?translate:french to instantly translate to any language.",
        "🎤" to "Content" to "?headline turns plain text into attention-grabbing titles."
    )

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Spacer(Modifier.height(52.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Explore", fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TextPrim)
                Text("Discover what TypeShift can do", fontFamily = AppFont, fontSize = 14.sp, color = TextSec)
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            // Hero flow diagram
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A1040), Color(0xFF0D1A20))))
                    .border(1.dp, Accent.copy(0.15f), RoundedCornerShape(20.dp))
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

        item {
            SectionLabel("Use Cases")
            Spacer(Modifier.height(12.dp))
        }

        items(useCases) { (emojiTitle, desc) ->
            val (emoji, title) = emojiTitle
            ExploreCard(emoji = emoji, title = title, desc = desc)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun FlowStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Accent.copy(0.2f)),
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
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Surface3),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                Text(desc,  fontFamily = AppFont, fontSize = 13.sp, color = TextSec, lineHeight = 19.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────
//  SETTINGS TAB
// ─────────────────────────────────────────────
@Composable
fun SettingsTab() {
    val context = LocalContext.current
    val prefs   = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(Bg),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Spacer(Modifier.height(52.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Settings", fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = TextPrim)
                Text("Configure TypeShift", fontFamily = AppFont, fontSize = 14.sp, color = TextSec)
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            ApiKeyCard(context)
            Spacer(Modifier.height(16.dp))
        }

        item {
            TemperatureCard(context)
            Spacer(Modifier.height(16.dp))
        }

        item {
            ModelInfoCard()
            Spacer(Modifier.height(16.dp))
        }

        item {
            AboutCard()
        }
    }
}

@Composable
fun ApiKeyCard(context: Context) {
    val prefs   = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
    var key     by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }
    var saved   by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        .clip(RoundedCornerShape(20.dp)).background(Surface2).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Key, null, tint = Accent, modifier = Modifier.size(20.dp))
                Text("Groq API Key", fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrim)
            }
            Text("Free, fast, no credit card required", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)

            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface3)) {
                OutlinedTextField(
                    value                = key,
                    onValueChange        = { key = it; saved = false },
                    placeholder          = { Text("gsk_...", fontFamily = AppFont, fontSize = 14.sp, color = TextTert) },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier             = Modifier.fillMaxWidth(),
                    singleLine           = true,
                    colors               = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Accent.copy(0.5f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor     = TextPrim, unfocusedTextColor = TextPrim, cursorColor = Accent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = AppFont, fontSize = 14.sp, color = TextPrim),
                    shape     = RoundedCornerShape(14.dp),
                    trailingIcon = {
                        Text(
                            if (showKey) "Hide" else "Show",
                            modifier   = Modifier.clickable { showKey = !showKey }.padding(8.dp),
                            fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Accent
                        )
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick  = { prefs.edit().putString("gemini_api_key", key.trim()).apply(); saved = true },
                    modifier = Modifier.height(46.dp),
                    shape    = RoundedCornerShape(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("Save", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                if (saved) {
                    Text("✓  Saved", fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Success)
                }
            }

            Text("Get your free key at console.groq.com", fontFamily = AppFont, fontSize = 12.sp, color = TextTert)
        }
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
        temperature < 0.4f -> Color(0xFF4FC3F7)
        temperature < 0.8f -> Accent
        temperature < 1.1f -> Warning
        else               -> Danger
    }

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        .clip(RoundedCornerShape(20.dp)).background(Surface2).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🌡️", fontSize = 18.sp)
                Text("AI Temperature", fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrim)
            }
            Text("Controls how creative or deterministic the AI output is.", fontFamily = AppFont, fontSize = 13.sp, color = TextSec)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    tempLabel,
                    fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = tempColor
                )
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(tempColor.copy(0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("%.1f".format(temperature), fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = tempColor)
                }
            }

            Slider(
                value         = temperature,
                onValueChange = { temperature = it },
                onValueChangeFinished = {
                    prefs.edit().putFloat("ai_temperature", temperature).apply()
                },
                valueRange    = 0f..1.5f,
                steps         = 14,
                colors        = SliderDefaults.colors(
                    thumbColor            = tempColor,
                    activeTrackColor      = tempColor,
                    inactiveTrackColor    = Surface4
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0.0 Precise", fontFamily = AppFont, fontSize = 11.sp, color = TextTert)
                Text("1.5 Wild", fontFamily = AppFont, fontSize = 11.sp, color = TextTert)
            }
        }
    }
}

@Composable
fun ModelInfoCard() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        .clip(RoundedCornerShape(20.dp)).background(Surface2).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF3D2BFF), Accent))),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 20.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("AI Model", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
                Text("llama-3.3-70b-versatile", fontFamily = AppFont, fontSize = 13.sp, color = AccentAlt)
                Text("via Groq — ~300ms response time", fontFamily = AppFont, fontSize = 12.sp, color = TextSec)
            }
        }
    }
}

@Composable
fun AboutCard() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        .clip(RoundedCornerShape(20.dp)).background(Surface2).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("About", fontFamily = AppFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version", fontFamily = AppFont, fontSize = 14.sp, color = TextSec)
                Text("1.0", fontFamily = AppFont, fontSize = 14.sp, color = TextPrim)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Platform", fontFamily = AppFont, fontSize = 14.sp, color = TextSec)
                Text("Android", fontFamily = AppFont, fontSize = 14.sp, color = TextPrim)
            }
            HorizontalDivider(color = Surface3, thickness = 0.6.dp)
            Text(
                "TypeShift works in every app — no copy-paste, no switching. AI rewrites your text in place.",
                fontFamily = AppFont, fontSize = 13.sp, color = TextSec, lineHeight = 20.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Shared helpers
// ─────────────────────────────────────────────
@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        modifier      = Modifier.padding(horizontal = 20.dp),
        fontFamily    = AppFont,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 13.sp,
        color         = TextSec,
        letterSpacing = 0.5.sp
    )
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${AiAccessibilityService::class.java.name}"
    val enabled  = Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
}
