package com.nayal.aikeyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

@Suppress("DEPRECATION")
class AiKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var keyboard: Keyboard
    private lateinit var progressBar: ProgressBar
    private var isCapsOn = false

    // Background executor for API calls (never block the main thread for network)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // What we tell Gemini for each command
    private val instructions = mapOf(
        "fix"      to "Fix all grammar and spelling mistakes. Return only the corrected text, nothing else.",
        "improve"  to "Improve this text to be clearer and more engaging. Return only the improved text, nothing else.",
        "formal"   to "Rewrite this text in a formal, professional tone. Return only the rewritten text, nothing else.",
        "casual"   to "Rewrite this text in a casual, friendly tone. Return only the rewritten text, nothing else.",
        "simplify" to "Simplify this text so it is easy to understand. Return only the simplified text, nothing else."
    )

    override fun onCreateInputView(): View {
        // Inflate our keyboard layout (AI toolbar + keys)
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)

        progressBar = view.findViewById(R.id.progress_bar)

        // Set up the QWERTY keyboard
        keyboardView = view.findViewById(R.id.keyboard_view)
        keyboard = Keyboard(this, R.xml.keyboard_qwerty)
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)

        // Wire up AI toolbar buttons
        view.findViewById<Button>(R.id.btn_fix).setOnClickListener { runCommand("fix") }
        view.findViewById<Button>(R.id.btn_improve).setOnClickListener { runCommand("improve") }
        view.findViewById<Button>(R.id.btn_formal).setOnClickListener { runCommand("formal") }
        view.findViewById<Button>(R.id.btn_casual).setOnClickListener { runCommand("casual") }
        view.findViewById<Button>(R.id.btn_simplify).setOnClickListener { runCommand("simplify") }

        return view
    }

    private fun runCommand(command: String) {
        val ic = currentInputConnection ?: return

        // Get all text currently in the input field
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        val fullText = extracted?.text?.toString()?.trim() ?: ""

        if (fullText.isEmpty()) {
            Toast.makeText(this, "Type something first", Toast.LENGTH_SHORT).show()
            return
        }

        // Get API key from SharedPreferences
        val prefs = getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Set your Gemini API key in Settings", Toast.LENGTH_LONG).show()
            return
        }

        val instruction = instructions[command] ?: return

        // Show loading bar
        mainHandler.post { progressBar.visibility = View.VISIBLE }

        // Run API call on background thread
        executor.execute {
            try {
                val result = callGemini(fullText, instruction, apiKey)

                mainHandler.post {
                    progressBar.visibility = View.GONE
                    // Replace all text in the field with the AI result
                    ic.beginBatchEdit()
                    ic.deleteSurroundingText(fullText.length, 0)
                    ic.commitText(result, 1)
                    ic.endBatchEdit()
                }
            } catch (e: Exception) {
                mainHandler.post {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun callGemini(text: String, instruction: String, apiKey: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        // Escape the text so it's safe to embed in JSON
        val safeText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val body = """{"contents":[{"parts":[{"text":"$instruction\n\nText:\n$safeText"}]}]}"""

        OutputStreamWriter(conn.outputStream).use { it.write(body) }

        if (conn.responseCode != 200) {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("API error ${conn.responseCode}: $error")
        }

        val response = conn.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        return json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
    }

    // Called when user taps a key
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> ic.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_SHIFT  -> toggleCaps()
            10                      -> ic.commitText("\n", 1) // Enter key
            else -> {
                var char = primaryCode.toChar()
                if (isCapsOn) char = char.uppercaseChar()
                ic.commitText(char.toString(), 1)
                // Auto-turn off caps after one letter
                if (isCapsOn) toggleCaps()
            }
        }
    }

    private fun toggleCaps() {
        isCapsOn = !isCapsOn
        keyboard.isShifted = isCapsOn
        keyboardView.invalidateAllKeys()
    }

    // These are required by the interface but we don't need them
    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
