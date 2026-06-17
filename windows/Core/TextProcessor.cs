using System.Runtime.InteropServices;
using System.Text;
using System.Text.RegularExpressions;
using System.Windows;
using TypeShift.Models;
// WinForms is enabled for the tray icon, so its `Application` clashes with WPF's. Pin to WPF.
using Application = System.Windows.Application;

namespace TypeShift.Core;

public sealed class TextProcessor : IDisposable
{
    [DllImport("user32.dll")] static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, nuint dwExtraInfo);
    private const byte VK_CONTROL = 0x11;
    private const byte VK_A       = 0x41;
    private const byte VK_C       = 0x43;
    private const byte VK_V       = 0x56;
    private const uint KEYEVENTF_KEYUP = 0x0002;

    private static readonly Dictionary<string, string> Triggers = new(StringComparer.OrdinalIgnoreCase)
    {
        ["?fix"]      = "Fix all grammar and spelling mistakes. Return only the corrected text, nothing else.",
        ["?improve"]  = "Improve this text to be clearer and more engaging. Return only the improved text, nothing else.",
        ["?formal"]   = "Rewrite this text in a formal, professional tone. Return only the rewritten text, nothing else.",
        ["?casual"]   = "Rewrite this text in a casual, friendly tone. Return only the rewritten text, nothing else.",
        ["?shorter"]  = "Make this text shorter and more concise. Return only the shortened text, nothing else.",
        ["?longer"]   = "Expand this text with more detail. Return only the expanded text, nothing else.",
        ["?emoji"]    = "Add relevant and fun emojis throughout this text. Return only the text with emojis, nothing else.",
        ["?reply"]    = "Write a natural, friendly reply to this message. Return only the reply, nothing else.",
        ["?human"]    = "Rewrite this text to sound more natural and human. Return only the rewritten text, nothing else.",
        ["?hinglish"] = "Rewrite this text in Hinglish — a natural mix of Hindi and English. Return only the Hinglish text, nothing else.",
        ["?roast"]    = "Rewrite this as a funny, light-hearted roast. Return only the roast, nothing else.",
        ["?tweet"]    = "Rewrite this as a punchy tweet under 280 characters. Return only the tweet, nothing else.",
        ["?bullet"]   = "Convert this text into clear bullet points. Return only the bullet points, nothing else.",
        ["?subject"]  = "Generate a short email subject line based on this text. Return only the subject line, nothing else.",
        ["?eli5"]     = "Explain this text like I'm 5 years old. Return only the explanation, nothing else.",
        ["?tldr"]     = "Summarize this text in one sentence. Return only the summary, nothing else.",
        ["?headline"] = "Rewrite this as a short, catchy headline. Return only the headline, nothing else.",
    };

    private readonly KeyboardHook _hook = new();
    private readonly StringBuilder _buf = new(256);
    private volatile bool _isProcessing;
    private string? _lastOriginal;

    public event Action<string>? StatusChanged;

    public void Start()
    {
        _hook.KeyDown += OnKey;
        _hook.Install();
    }

    private void OnKey(int vk)
    {
        if (_isProcessing) return;

        // Reset buffer on navigation/control keys
        if (vk is 0x0D or 0x1B or 0x09 or 0x25 or 0x26 or 0x27 or 0x28 or 0x24 or 0x23)
        { _buf.Clear(); return; }

        if (vk == 0x08) // Backspace
        { if (_buf.Length > 0) _buf.Length--; return; }

        if (vk == 0x20) // Space
        {
            _ = CheckAndProcessAsync();
            _buf.Append(' ');
            if (_buf.Length > 200) _buf.Remove(0, _buf.Length - 200);
            return;
        }

        // Append printable character (lowercase for trigger matching)
        if (vk is >= 0x41 and <= 0x5A)         // A–Z
            _buf.Append((char)(vk + 32));
        else if (vk is >= 0x30 and <= 0x39)     // 0–9
            _buf.Append((char)vk);
        else if (vk == 0xBF)                    // ? key
            _buf.Append('?');
        else if (vk == 0xBE)                    // .
            _buf.Append('.');
        else if (vk == 0xBC)                    // ,
            _buf.Append(',');
        else if (vk == 0xBA)                    // :
            _buf.Append(':');

        if (_buf.Length > 200) _buf.Remove(0, _buf.Length - 200);
    }

    private async Task CheckAndProcessAsync()
    {
        var text = _buf.ToString().TrimEnd();

        // ?undo
        if (text.EndsWith("?undo", StringComparison.OrdinalIgnoreCase))
        {
            if (_lastOriginal != null) await PasteTextAsync(_lastOriginal);
            else StatusChanged?.Invoke("Nothing to undo");
            _buf.Clear();
            return;
        }

        // ?translate:XX
        var transMatch = Regex.Match(text, @"\?translate:([a-zA-Z]{2,10})$", RegexOptions.IgnoreCase);
        if (transMatch.Success)
        {
            var lang = transMatch.Groups[1].Value;
            var instr = $"Translate the following text to {lang}. Return only the translated text, nothing else.";
            await ProcessAsync(instr);
            return;
        }

        // Built-in triggers
        foreach (var (trigger, instruction) in Triggers)
        {
            if (text.EndsWith(trigger, StringComparison.OrdinalIgnoreCase))
            { await ProcessAsync(instruction); return; }
        }

        // Custom commands
        foreach (var cmd in CustomCommandStore.Load())
        {
            if (text.EndsWith(cmd.Trigger, StringComparison.OrdinalIgnoreCase))
            { await ProcessAsync(cmd.Prompt); return; }
        }
    }

    private async Task ProcessAsync(string instruction)
    {
        _isProcessing = true;
        StatusChanged?.Invoke("Reading…");

        string? savedClip = null;
        try
        {
            // Save clipboard on UI thread
            await Application.Current.Dispatcher.InvokeAsync(() =>
            {
                savedClip = System.Windows.Clipboard.ContainsText() ? System.Windows.Clipboard.GetText() : null;
                System.Windows.Clipboard.Clear();
            });

            // Ctrl+A, Ctrl+C
            await Task.Delay(80);
            SendCtrl(VK_A); await Task.Delay(120);
            SendCtrl(VK_C); await Task.Delay(300);

            string fullText = "";
            await Application.Current.Dispatcher.InvokeAsync(() =>
            {
                fullText = System.Windows.Clipboard.ContainsText() ? System.Windows.Clipboard.GetText() : "";
            });

            if (string.IsNullOrWhiteSpace(fullText))
            { StatusChanged?.Invoke("Click in a text field first"); return; }

            // Strip trigger from the end
            var trimmed = fullText.TrimEnd();
            var cleanText = trimmed;
            foreach (var trigger in Triggers.Keys)
            {
                if (trimmed.EndsWith(trigger, StringComparison.OrdinalIgnoreCase))
                { cleanText = trimmed[..^trigger.Length].TrimEnd(); break; }
            }
            foreach (var cmd in CustomCommandStore.Load())
            {
                if (trimmed.EndsWith(cmd.Trigger, StringComparison.OrdinalIgnoreCase))
                { cleanText = trimmed[..^cmd.Trigger.Length].TrimEnd(); break; }
            }
            // Strip translate trigger
            var tm = Regex.Match(trimmed, @"\?translate:[a-zA-Z]{2,10}$", RegexOptions.IgnoreCase);
            if (tm.Success) cleanText = trimmed[..tm.Index].TrimEnd();

            if (string.IsNullOrWhiteSpace(cleanText))
            { StatusChanged?.Invoke("No text before trigger"); return; }

            _lastOriginal = cleanText;

            // Show spinner
            await Application.Current.Dispatcher.InvokeAsync(() => System.Windows.Clipboard.SetText("⟳ Thinking…"));
            SendCtrl(VK_A); await Task.Delay(60);
            SendCtrl(VK_V); await Task.Delay(80);

            StatusChanged?.Invoke("Thinking…");

            var apiKey     = Settings.ApiKey;
            var temperature = Settings.Temperature;

            if (string.IsNullOrWhiteSpace(apiKey))
            { StatusChanged?.Invoke("Set your API key in Settings"); return; }

            var result = await GroqApi.CallAsync(cleanText, instruction, apiKey, temperature);

            // Paste result
            await Application.Current.Dispatcher.InvokeAsync(() => System.Windows.Clipboard.SetText(result));
            SendCtrl(VK_A); await Task.Delay(60);
            SendCtrl(VK_V);

            StatusChanged?.Invoke("Done ✓");
            _buf.Clear();

            await Task.Delay(600);
            await Application.Current.Dispatcher.InvokeAsync(() =>
            {
                if (savedClip != null) System.Windows.Clipboard.SetText(savedClip);
                else System.Windows.Clipboard.Clear();
            });
        }
        catch (Exception ex)
        {
            StatusChanged?.Invoke($"Error: {ex.Message}");
        }
        finally
        {
            _isProcessing = false;
        }
    }

    // Run a command from the tray menu (select all → process)
    public async Task ProcessSelectionAsync(string instruction)
    {
        if (_isProcessing) return;
        await ProcessAsync(instruction);
    }

    private static void SendCtrl(byte key)
    {
        keybd_event(VK_CONTROL, 0, 0, 0);
        keybd_event(key, 0, 0, 0);
        keybd_event(key, 0, KEYEVENTF_KEYUP, 0);
        keybd_event(VK_CONTROL, 0, KEYEVENTF_KEYUP, 0);
    }

    private async Task PasteTextAsync(string text)
    {
        await Application.Current.Dispatcher.InvokeAsync(() => System.Windows.Clipboard.SetText(text));
        SendCtrl(VK_A); await Task.Delay(60);
        SendCtrl(VK_V);
    }

    public void Stop() => _hook.Dispose();
    public void Dispose() => Stop();
}
