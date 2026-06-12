package com.nayal.aikeyboard

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class AiAccessibilityService : AccessibilityService() {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isProcessing = AtomicBoolean(false)

    private val spinnerFrames = arrayOf("◐", "◓", "◑", "◒")
    private var spinnerFrame = 0
    private var spinnerRunnable: Runnable? = null

    @Volatile private var lastOriginalText: String? = null

    private val triggers = mapOf(
        "?fix"      to "Fix all grammar and spelling mistakes. Return only the corrected text, nothing else.",
        "?improve"  to "Improve this text to be clearer and more engaging. Return only the improved text, nothing else.",
        "?formal"   to "Rewrite this text in a formal, professional tone. Return only the rewritten text, nothing else.",
        "?casual"   to "Rewrite this text in a casual, friendly tone. Return only the rewritten text, nothing else.",
        "?simplify" to "Simplify this text so it is easy to understand. Return only the simplified text, nothing else.",
        "?shorter"  to "Make this text shorter and more concise. Return only the shortened text, nothing else.",
        "?longer"   to "Expand this text with more detail. Return only the expanded text, nothing else.",
        "?emoji"    to "Add relevant and fun emojis throughout this text to make it more expressive. Return only the text with emojis added, nothing else.",
        "?reply"    to "Write a natural, friendly reply to this message. Return only the reply text, nothing else.",
        "?human"    to "Rewrite this text to sound more natural and human, less like AI-generated content. Return only the rewritten text, nothing else.",
        "?hinglish" to "Rewrite this text in Hinglish — a natural mix of Hindi and English words as spoken by young Indians (e.g. 'bhai yeh toh bahut cool hai'). Return only the Hinglish text, nothing else.",
        "?roast"    to "Rewrite this text as a funny, light-hearted roast or joke about what was said. Keep it humorous and playful. Return only the roast, nothing else.",
        "?tweet"    to "Rewrite this text as a punchy tweet under 280 characters. Make it engaging and shareable. Return only the tweet text, nothing else.",
        "?bullet"   to "Convert this text into clear, concise bullet points. Return only the bullet points, nothing else.",
        "?subject"  to "Generate a short, compelling email subject line based on this text. Return only the subject line, nothing else.",
        "?eli5"     to "Explain this text like I'm 5 years old, using simple words and examples. Return only the explanation, nothing else.",
        "?tldr"     to "Summarize this text in one sentence. Return only the summary, nothing else.",
        "?headline" to "Rewrite this as a short, catchy, attention-grabbing headline. Return only the headline, nothing else."
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return
        if (isProcessing.get()) return

        val source = event.source ?: return
        if (source.isPassword) { source.recycle(); return }

        val text = source.text?.toString() ?: run { source.recycle(); return }
        if (text.isEmpty()) { source.recycle(); return }

        // Check ?undo trigger
        if (text.trimEnd().endsWith("?undo", ignoreCase = true)) {
            val previous = lastOriginalText
            if (previous == null) {
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show()
            } else {
                if (!isProcessing.compareAndSet(false, true)) { source.recycle(); return }
                replaceText(source, previous)
                lastOriginalText = null
                isProcessing.set(false)
            }
            source.recycle()
            return
        }

        // Check ?translate:language trigger (e.g. "hello ?translate:hindi")
        val translateRegex = Regex(""".*\?translate:(\w+)$""", RegexOption.IGNORE_CASE)
        val translateMatch = translateRegex.find(text.trimEnd())
        if (translateMatch != null) {
            val language = translateMatch.groupValues[1]
            val trigger = "?translate:$language"
            val cleanText = text.trimEnd().dropLast(trigger.length).trim()
            if (cleanText.isNotEmpty()) {
                if (!isProcessing.compareAndSet(false, true)) { source.recycle(); return }
                val instruction = "Translate the following text to $language. Return only the translated text, nothing else."
                processText(source, cleanText, instruction)
                return
            }
        }

        for ((trigger, instruction) in triggers) {
            if (text.trimEnd().endsWith(trigger, ignoreCase = true)) {
                val cleanText = text.trimEnd().dropLast(trigger.length).trim()
                if (cleanText.isEmpty()) { source.recycle(); return }
                if (!isProcessing.compareAndSet(false, true)) { source.recycle(); return }
                processText(source, cleanText, instruction)
                return
            }
        }

        // Check custom user-defined commands
        val customCommands = loadCustomCommands(this)
        for (cc in customCommands) {
            if (text.trimEnd().endsWith(cc.trigger, ignoreCase = true)) {
                val cleanText = text.trimEnd().dropLast(cc.trigger.length).trim()
                if (cleanText.isEmpty()) { source.recycle(); return }
                if (!isProcessing.compareAndSet(false, true)) { source.recycle(); return }
                processText(source, cleanText, cc.prompt)
                return
            }
        }

        source.recycle()
    }

    private fun processText(source: AccessibilityNodeInfo, text: String, instruction: String) {
        val prefs = getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "Open AI Text Assistant app and save your Gemini API key", Toast.LENGTH_LONG).show()
            isProcessing.set(false)
            source.recycle()
            return
        }

        lastOriginalText = text
        startSpinner(source, text)

        executor.execute {
            try {
                Log.d("AIService", "Calling AI for: $text")
                val result = callAI(text, instruction, apiKey)
                Log.d("AIService", "Got result: $result")
                mainHandler.post {
                    stopSpinner()
                    replaceText(source, result)
                    isProcessing.set(false)
                    source.recycle()
                }
            } catch (e: Exception) {
                Log.e("AIService", "Error: ${e.message}", e)
                mainHandler.post {
                    stopSpinner()
                    replaceText(source, text)
                    Toast.makeText(this, "AI Error: ${e.message}", Toast.LENGTH_LONG).show()
                    isProcessing.set(false)
                    source.recycle()
                }
            }
        }
    }

    private fun startSpinner(source: AccessibilityNodeInfo, baseText: String) {
        spinnerFrame = 0
        val runnable = object : Runnable {
            override fun run() {
                if (!source.refresh()) return
                setNodeText(source, "$baseText ${spinnerFrames[spinnerFrame]}")
                spinnerFrame = (spinnerFrame + 1) % spinnerFrames.size
                mainHandler.postDelayed(this, 200)
                spinnerRunnable = this
            }
        }
        spinnerRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopSpinner() {
        spinnerRunnable?.let { mainHandler.removeCallbacks(it) }
        spinnerRunnable = null
    }

    private fun replaceText(source: AccessibilityNodeInfo, newText: String) {
        if (!source.refresh()) {
            Log.e("AIService", "Source node refresh failed")
            return
        }

        val success = setNodeText(source, newText)
        Log.d("AIService", "ACTION_SET_TEXT result: $success")

        if (success) return

        // Clipboard fallback for apps that don't support ACTION_SET_TEXT
        Log.d("AIService", "Falling back to clipboard paste")
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val oldClip = clipboard.primaryClip
        clipboard.setPrimaryClip(ClipData.newPlainText("AI Result", newText))

        source.refresh()
        val len = source.text?.length ?: 0
        val selectArgs = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, len)
        }
        source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectArgs)
        source.performAction(AccessibilityNodeInfo.ACTION_PASTE)

        // Restore old clipboard after a short delay
        mainHandler.postDelayed({
            try {
                if (oldClip != null) clipboard.setPrimaryClip(oldClip)
            } catch (_: Exception) {}
        }, 500)
    }

    private fun setNodeText(source: AccessibilityNodeInfo, text: String): Boolean {
        val bundle = Bundle()
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    private fun callAI(text: String, instruction: String, apiKey: String): String {
        val url = URL("https://api.groq.com/openai/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 30000

        val prefs2 = getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)
        val temperature = prefs2.getFloat("ai_temperature", 0.7f)
        val safeText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val safeInstruction = instruction.replace("\"", "\\\"")
        val body = """{"model":"llama-3.3-70b-versatile","temperature":$temperature,"messages":[{"role":"user","content":"$safeInstruction\n\nText:\n$safeText"}]}"""

        OutputStreamWriter(conn.outputStream).use { it.write(body) }

        if (conn.responseCode != 200) {
            val error = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("API error ${conn.responseCode}: $error")
        }

        val response = conn.inputStream.bufferedReader().readText()
        return JSONObject(response)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }

    override fun onInterrupt() {}
}
