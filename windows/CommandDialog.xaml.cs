using System.Windows;
using TypeShift.Models;

namespace TypeShift;

public partial class CommandDialog : Window
{
    public CustomCommand? Result { get; private set; }

    private readonly string? _existingId;

    public CommandDialog(CustomCommand? existing = null)
    {
        InitializeComponent();
        _existingId = existing?.Id;

        if (existing != null)
        {
            TitleLabel.Text    = "Edit Command";
            NameBox.Text       = existing.Name;
            TriggerBox.Text    = existing.Trigger;
            PromptBox.Text     = existing.Prompt;
        }
    }

    private void Save_Click(object sender, RoutedEventArgs e)
    {
        var name    = NameBox.Text.Trim();
        var trigger = TriggerBox.Text.Trim();
        var prompt  = PromptBox.Text.Trim();

        if (string.IsNullOrEmpty(name))    { ShowError("Please enter a name."); return; }
        if (string.IsNullOrEmpty(trigger)) { ShowError("Please enter a trigger."); return; }
        if (!trigger.StartsWith('?'))      { ShowError("Trigger must start with ?."); return; }
        if (trigger.Contains(' '))         { ShowError("Trigger cannot contain spaces."); return; }
        if (string.IsNullOrEmpty(prompt))  { ShowError("Please enter an AI prompt."); return; }

        Result = new CustomCommand
        {
            Id      = _existingId ?? Guid.NewGuid().ToString(),
            Name    = name,
            Trigger = trigger.ToLower(),
            Prompt  = prompt,
        };

        DialogResult = true;
    }

    private void Cancel_Click(object sender, RoutedEventArgs e) => DialogResult = false;

    private void ShowError(string msg)
    {
        ErrorLabel.Text       = msg;
        ErrorLabel.Visibility = Visibility.Visible;
    }
}
