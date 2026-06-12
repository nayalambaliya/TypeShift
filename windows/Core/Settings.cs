using System.IO;
using System.Text.Json;

namespace TypeShift.Core;

// Simple settings persisted as JSON in %APPDATA%\TypeShift\settings.json
public static class Settings
{
    private static readonly string FilePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "TypeShift", "settings.json"
    );

    private static SettingsData _data = Load();

    public static string ApiKey
    {
        get => _data.ApiKey;
        set { _data.ApiKey = value; Save(); }
    }

    public static double Temperature
    {
        get => _data.Temperature;
        set { _data.Temperature = value; Save(); }
    }

    private static SettingsData Load()
    {
        try
        {
            if (!File.Exists(FilePath)) return new();
            var json = File.ReadAllText(FilePath);
            return JsonSerializer.Deserialize<SettingsData>(json) ?? new();
        }
        catch { return new(); }
    }

    private static void Save()
    {
        Directory.CreateDirectory(Path.GetDirectoryName(FilePath)!);
        File.WriteAllText(FilePath, JsonSerializer.Serialize(_data, new JsonSerializerOptions { WriteIndented = true }));
    }

    private class SettingsData
    {
        public string ApiKey     { get; set; } = "";
        public double Temperature { get; set; } = 0.7;
    }

    // Reload from disk (in case another window updated it)
    public static void Reload() => _data = Load();
}
