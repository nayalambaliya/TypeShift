package com.nayal.aikeyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// ─────────────────────────────────────────────
//  Colors — pure AMOLED dark like OxygenOS
// ─────────────────────────────────────────────
val Bg        = Color(0xFF000000)
val Surface1  = Color(0xFF141414)
val Surface2  = Color(0xFF1E1E1E)
val Surface3  = Color(0xFF282828)
val Accent    = Color(0xFF7B61FF)   // soft electric violet
val AccentAlt = Color(0xFF9E8FFF)
val TextPrim  = Color(0xFFFFFFFF)
val TextSec   = Color(0xFF8A8A8A)
val TextTert  = Color(0xFF555555)
val Success   = Color(0xFF34C759)
val Danger    = Color(0xFFFF3B30)

// ─────────────────────────────────────────────
//  Font — Outfit (similar to OnePlus Sans)
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
                Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                    HomeScreen()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Home screen
// ─────────────────────────────────────────────
@Composable
fun HomeScreen() {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var serviceOn by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            serviceOn = isAccessibilityServiceEnabled(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(52.dp))

        // ── App title (big, OxygenOS style) ──────────────
        Text(
            "TypeShift",
            fontFamily = AppFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize   = 40.sp,
            color      = TextPrim,
            letterSpacing = (-0.5).sp
        )
        Text(
            "AI text assistant — works in every app",
            fontFamily = AppFont,
            fontWeight = FontWeight.Normal,
            fontSize   = 15.sp,
            color      = TextSec
        )

        Spacer(Modifier.height(32.dp))

        // ── Service status card ───────────────────────────
        ServiceCard(serviceOn) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        Spacer(Modifier.height(16.dp))

        // ── API key card ──────────────────────────────────
        ApiCard(context)

        Spacer(Modifier.height(16.dp))

        // ── Commands card ─────────────────────────────────
        CommandsCard()

        Spacer(Modifier.height(40.dp))
    }
}

// ─────────────────────────────────────────────
//  Service card
// ─────────────────────────────────────────────
@Composable
fun ServiceCard(isOn: Boolean, onEnable: () -> Unit) {
    val statusColor = if (isOn) Success else Danger

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Surface1,
                        Surface2
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    if (isOn) "Service active" else "Service disabled",
                    fontFamily = AppFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 17.sp,
                    color      = TextPrim
                )
            }

            Text(
                if (isOn)
                    "TypeShift is running in the background. Type any trigger command in any app."
                else
                    "Enable TypeShift in Accessibility Settings so it can detect your trigger commands.",
                fontFamily = AppFont,
                fontSize   = 14.sp,
                color      = TextSec,
                lineHeight = 22.sp
            )

            if (!isOn) {
                Button(
                    onClick = onEnable,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text(
                        "Enable in Accessibility Settings",
                        fontFamily = AppFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        color      = TextPrim
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
//  API Key card
// ─────────────────────────────────────────────
@Composable
fun ApiCard(context: Context) {
    val prefs   = context.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
    var key     by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }
    var saved   by remember { mutableStateOf(false) }
    var showKey by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Surface1)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "API Key",
                fontFamily = AppFont,
                fontWeight = FontWeight.Bold,
                fontSize   = 22.sp,
                color      = TextPrim
            )
            Text(
                "Powered by Groq — free, fast, no credit card required",
                fontFamily = AppFont,
                fontSize   = 14.sp,
                color      = TextSec
            )

            // Input field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface2)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value                = key,
                    onValueChange        = { key = it; saved = false },
                    placeholder          = { Text("gsk_...", fontFamily = AppFont, fontSize = 14.sp, color = TextTert) },
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier             = Modifier.fillMaxWidth(),
                    singleLine           = true,
                    colors               = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor     = TextPrim,
                        unfocusedTextColor   = TextPrim,
                        cursorColor          = Accent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = AppFont,
                        fontSize   = 14.sp,
                        color      = TextPrim
                    ),
                    trailingIcon = {
                        Text(
                            if (showKey) "Hide" else "Show",
                            modifier       = Modifier.clickable { showKey = !showKey }.padding(8.dp),
                            fontFamily     = AppFont,
                            fontWeight     = FontWeight.Medium,
                            fontSize       = 13.sp,
                            color          = Accent
                        )
                    }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Button(
                    onClick  = { prefs.edit().putString("gemini_api_key", key.trim()).apply(); saved = true },
                    modifier = Modifier.height(48.dp),
                    shape    = RoundedCornerShape(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("Save", fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                if (saved) {
                    Text(
                        "✓  Saved",
                        fontFamily = AppFont,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 14.sp,
                        color      = Success
                    )
                }
            }

            Text(
                "Get your free key at console.groq.com",
                fontFamily = AppFont,
                fontSize   = 13.sp,
                color      = TextTert
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Commands card
// ─────────────────────────────────────────────
@Composable
fun CommandsCard() {
    var expanded by remember { mutableStateOf(false) }

    val commands = listOf(
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
        "?bullet"       to "Convert to bullet points",
        "?subject"      to "Email subject line",
        "?eli5"         to "Explain like I'm 5",
        "?tldr"         to "One sentence summary",
        "?headline"     to "Catchy headline",
        "?undo"         to "Restore original text",
        "?translate:XX" to "Translate any language"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Surface1)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Commands",
                    fontFamily = AppFont,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 22.sp,
                    color      = TextPrim
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        if (expanded) "Show less" else "See all ${commands.size}",
                        fontFamily = AppFont,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 14.sp,
                        color      = Accent
                    )
                }
            }

            Text(
                "Type any command at the end of your text",
                fontFamily = AppFont,
                fontSize   = 14.sp,
                color      = TextSec
            )

            Spacer(Modifier.height(8.dp))

            val visible = if (expanded) commands else commands.take(6)
            visible.forEach { (cmd, desc) ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Pill chip for command
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Accent.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            cmd,
                            fontFamily = AppFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp,
                            color      = AccentAlt,
                            maxLines   = 1,
                            overflow   = TextOverflow.Clip
                        )
                    }
                    Text(
                        desc,
                        fontFamily = AppFont,
                        fontSize   = 14.sp,
                        color      = TextSec
                    )
                }
                if (cmd != visible.last().first) {
                    HorizontalDivider(color = Surface3, thickness = 0.8.dp)
                }
            }

            if (!expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "+ ${commands.size - 6} more commands",
                    fontFamily = AppFont,
                    fontSize   = 13.sp,
                    color      = TextTert
                )
            }
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${AiAccessibilityService::class.java.name}"
    val enabled  = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
}
