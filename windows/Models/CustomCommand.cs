using System.IO;
using System.Text.Json;

namespace TypeShift.Models;

public class CustomCommand
{
    public string Id      { get; set; } = Guid.NewGuid().ToString();
    public string Trigger { get; set; } = "";
    public string Name    { get; set; } = "";
    public string Prompt  { get; set; } = "";
}

public static class CustomCommandStore
{
    private static readonly string FilePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "TypeShift", "custom_commands.json"
    );

    public static List<CustomCommand> Load()
    {
        try
        {
            if (!File.Exists(FilePath)) return [];
            var json = File.ReadAllText(FilePath);
            return JsonSerializer.Deserialize<List<CustomCommand>>(json) ?? [];
        }
        catch { return []; }
    }

    public static void Save(List<CustomCommand> commands)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(FilePath)!);
        File.WriteAllText(FilePath, JsonSerializer.Serialize(commands, new JsonSerializerOptions { WriteIndented = true }));
    }
}
