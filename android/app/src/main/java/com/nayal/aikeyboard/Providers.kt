package com.nayal.aikeyboard

import android.content.Context

/**
 * An AI provider TypeShift can talk to. Every provider here speaks the OpenAI-compatible
 * `/chat/completions` schema, so a single network path serves all of them — only the
 * endpoint, model, and API key change.
 *
 * @param custom  true for user-defined endpoints (local LLM servers / custom OpenAI-compatible APIs)
 */
data class AiProvider(
    val id: String,
    val name: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val keyHint: String,
    val getKeyUrl: String,
    val needsKey: Boolean,
    val custom: Boolean = false
)

val AI_PROVIDERS: List<AiProvider> = listOf(
    AiProvider("groq",       "Groq",       "https://api.groq.com/openai/v1/chat/completions", "llama-3.3-70b-versatile",                       "gsk_…",  "console.groq.com",            true),
    AiProvider("openai",     "OpenAI",     "https://api.openai.com/v1/chat/completions",      "gpt-4o-mini",                                   "sk-…",   "platform.openai.com/api-keys", true),
    AiProvider("openrouter", "OpenRouter", "https://openrouter.ai/api/v1/chat/completions",   "openai/gpt-4o-mini",                            "sk-or-…","openrouter.ai/keys",          true),
    AiProvider("together",   "Together",   "https://api.together.xyz/v1/chat/completions",    "meta-llama/Llama-3.3-70B-Instruct-Turbo",       "…",      "api.together.xyz",            true),
    AiProvider("deepseek",   "DeepSeek",   "https://api.deepseek.com/v1/chat/completions",    "deepseek-chat",                                 "sk-…",   "platform.deepseek.com",       true),
    AiProvider("mistral",    "Mistral",    "https://api.mistral.ai/v1/chat/completions",      "mistral-small-latest",                          "…",      "console.mistral.ai",          true),
    AiProvider("local",      "Local",      "http://192.168.1.10:11434/v1/chat/completions",   "llama3.2",                                      "(none)", "ollama.com",                  false, custom = true),
    AiProvider("custom",     "Custom",     "",                                                "",                                              "(optional)", "",                        false, custom = true),
)

fun providerById(id: String): AiProvider =
    AI_PROVIDERS.firstOrNull { it.id == id } ?: AI_PROVIDERS.first()

private fun prefs(ctx: Context) =
    ctx.getSharedPreferences("ai_keyboard_prefs", Context.MODE_PRIVATE)

fun selectedProviderId(ctx: Context): String =
    prefs(ctx).getString("selected_provider", "groq") ?: "groq"

fun setSelectedProviderId(ctx: Context, id: String) {
    prefs(ctx).edit().putString("selected_provider", id).apply()
}

fun selectedProvider(ctx: Context): AiProvider = providerById(selectedProviderId(ctx))

// Groq's key keeps its legacy storage name so existing users never lose their key.
private fun keyPrefName(p: AiProvider) = if (p.id == "groq") "gemini_api_key" else "key_${p.id}"

fun apiKeyFor(ctx: Context, p: AiProvider): String =
    prefs(ctx).getString(keyPrefName(p), "") ?: ""

fun saveApiKey(ctx: Context, p: AiProvider, value: String) {
    prefs(ctx).edit().putString(keyPrefName(p), value.trim()).apply()
}

fun endpointFor(ctx: Context, p: AiProvider): String {
    val stored = prefs(ctx).getString("endpoint_${p.id}", "") ?: ""
    return stored.ifBlank { p.defaultEndpoint }
}

fun saveEndpoint(ctx: Context, p: AiProvider, value: String) {
    prefs(ctx).edit().putString("endpoint_${p.id}", value.trim()).apply()
}

fun modelFor(ctx: Context, p: AiProvider): String {
    val stored = prefs(ctx).getString("model_${p.id}", "") ?: ""
    return stored.ifBlank { p.defaultModel }
}

fun saveModel(ctx: Context, p: AiProvider, value: String) {
    prefs(ctx).edit().putString("model_${p.id}", value.trim()).apply()
}