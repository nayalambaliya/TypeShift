using System.Net.Http;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;

namespace TypeShift.Core;

public static class GroqApi
{
    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(30) };

    public static async Task<string> CallAsync(string text, string instruction, string apiKey, double temperature = 0.7)
    {
        Http.DefaultRequestHeaders.Clear();
        Http.DefaultRequestHeaders.Add("Authorization", $"Bearer {apiKey}");

        var body = JsonSerializer.Serialize(new
        {
            model       = "llama-3.3-70b-versatile",
            temperature,
            messages    = new[]
            {
                new { role = "system", content = instruction },
                new { role = "user",   content = text }
            }
        });

        var response = await Http.PostAsync(
            "https://api.groq.com/openai/v1/chat/completions",
            new StringContent(body, Encoding.UTF8, "application/json")
        );

        response.EnsureSuccessStatusCode();
        using var doc = await JsonDocument.ParseAsync(await response.Content.ReadAsStreamAsync());
        return doc.RootElement
            .GetProperty("choices")[0]
            .GetProperty("message")
            .GetProperty("content")
            .GetString()?.Trim() ?? "";
    }
}
