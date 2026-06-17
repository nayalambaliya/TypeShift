using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using TypeShift.Core;
using TypeShift.Models;
// WinForms is enabled (tray icon) so these type names exist in two namespaces — pin to WPF.
using Color = System.Windows.Media.Color;
using FontFamily = System.Windows.Media.FontFamily;
using Brushes = System.Windows.Media.Brushes;
using Orientation = System.Windows.Controls.Orientation;
using Button = System.Windows.Controls.Button;
using HorizontalAlignment = System.Windows.HorizontalAlignment;
using VerticalAlignment = System.Windows.VerticalAlignment;

namespace TypeShift;

public partial class MainWindow : Window
{
    private static readonly (string Trigger, string Desc)[] BuiltinCommands =
    [
        ("?fix",          "Fix grammar & spelling"),
        ("?improve",      "Improve clarity"),
        ("?formal",       "Professional tone"),
        ("?casual",       "Friendly tone"),
        ("?shorter",      "Make it concise"),
        ("?longer",       "Expand with detail"),
        ("?emoji",        "Add relevant emojis"),
        ("?reply",        "Generate a reply"),
        ("?human",        "Sound more human"),
        ("?hinglish",     "Convert to Hinglish"),
        ("?roast",        "Funny roast"),
        ("?tweet",        "Shrink to a tweet"),
        ("?bullet",       "Bullet points"),
        ("?subject",      "Email subject line"),
        ("?eli5",         "Explain like I'm 5"),
        ("?tldr",         "One-line summary"),
        ("?headline",     "Catchy headline"),
        ("?undo",         "Restore original"),
        ("?translate:XX", "Translate any language"),
    ];

    private static readonly (string Emoji, string Title, string Desc)[] UseCases =
    [
        ("✍️", "Writing",      "Use ?improve and ?formal to polish emails, essays, and reports in seconds."),
        ("💬", "Messaging",    "Add ?casual or ?emoji to make your texts more fun and expressive."),
        ("🐦", "Social Media", "Turn any long thought into a viral tweet with ?tweet."),
        ("📧", "Email",        "Generate the perfect subject line with ?subject — never blank again."),
        ("🌍", "Translate",    "Type ?translate:french to instantly translate to any language."),
        ("🎤", "Content",      "?headline turns plain text into attention-grabbing titles."),
    ];

    public MainWindow()
    {
        InitializeComponent();
        Loaded += OnLoaded;
    }

    private void OnLoaded(object sender, RoutedEventArgs e)
    {
        PopulateHome();
        PopulateBuiltinCommands();
        PopulateCustomCommands();
        PopulateExplore();
        LoadSettings();
    }

    // ── Home ──────────────────────────────────────────────────
    private void PopulateHome()
    {
        // Quick command chips
        QuickCommandsList.Items.Clear();
        foreach (var (trigger, label) in BuiltinCommands.Take(6))
        {
            var chip = new Border
            {
                CornerRadius = new CornerRadius(20),
                Padding      = new Thickness(14, 8, 14, 8),
                Margin       = new Thickness(0, 0, 8, 8),
                Background   = new SolidColorBrush(Color.FromArgb(20, 123, 97, 255)),
                BorderBrush  = new SolidColorBrush(Color.FromArgb(64, 123, 97, 255)),
                BorderThickness = new Thickness(1),
                Child = new StackPanel
                {
                    Children =
                    {
                        new TextBlock { Text = trigger, FontSize = 12, FontWeight = FontWeights.SemiBold, Foreground = new SolidColorBrush(Color.FromRgb(158, 143, 255)), FontFamily = new FontFamily("Consolas") },
                        new TextBlock { Text = label,   FontSize = 11, Foreground = new SolidColorBrush(Color.FromRgb(138, 138, 138)) }
                    }
                }
            };
            QuickCommandsList.Items.Add(chip);
        }

        // Stats
        var customs = CustomCommandStore.Load();
        CustomCount.Text = customs.Count.ToString();
        var key = Settings.ApiKey;
        if (!string.IsNullOrWhiteSpace(key))
        {
            ApiKeyStat.Text       = "Set";
            ApiKeyStat.Foreground = new SolidColorBrush(Color.FromRgb(52, 199, 89));
        }
    }

    // ── Commands ──────────────────────────────────────────────
    private void PopulateBuiltinCommands()
    {
        BuiltinCommandsList.Items.Clear();
        for (int i = 0; i < BuiltinCommands.Length; i++)
        {
            var (trigger, desc) = BuiltinCommands[i];
            var row = CommandRow(trigger, desc);
            if (i < BuiltinCommands.Length - 1)
            {
                var wrap = new StackPanel { Children = { row, new Separator { Background = new SolidColorBrush(Color.FromRgb(36, 36, 36)), Margin = new Thickness(16, 0, 16, 0) } } };
                BuiltinCommandsList.Items.Add(wrap);
            }
            else
            {
                BuiltinCommandsList.Items.Add(row);
            }
        }
    }

    private static StackPanel CommandRow(string trigger, string desc)
    {
        var pill = new Border
        {
            CornerRadius = new CornerRadius(10),
            Background   = new SolidColorBrush(Color.FromArgb(30, 123, 97, 255)),
            Padding      = new Thickness(10, 4, 10, 4),
            Child        = new TextBlock { Text = trigger, FontSize = 12, FontWeight = FontWeights.SemiBold, Foreground = new SolidColorBrush(Color.FromRgb(158, 143, 255)), FontFamily = new FontFamily("Consolas") }
        };
        pill.Margin = new Thickness(0, 0, 12, 0);
        var label = new TextBlock { Text = desc, FontSize = 13, Foreground = new SolidColorBrush(Color.FromRgb(138, 138, 138)), VerticalAlignment = VerticalAlignment.Center };
        return new StackPanel
        {
            Orientation = Orientation.Horizontal,
            Margin      = new Thickness(16, 11, 16, 11),
            Children    = { pill, label }
        };
    }

    private void PopulateCustomCommands()
    {
        var customs = CustomCommandStore.Load();
        CommandsCountLabel.Text = $"{BuiltinCommands.Length} built-in  •  {customs.Count} custom";

        CustomCommandsList.Items.Clear();
        if (customs.Count == 0)
        {
            CustomListContainer.Visibility    = Visibility.Collapsed;
            EmptyCustomPlaceholder.Visibility = Visibility.Visible;
        }
        else
        {
            CustomListContainer.Visibility    = Visibility.Visible;
            EmptyCustomPlaceholder.Visibility = Visibility.Collapsed;
            foreach (var cmd in customs)
                CustomCommandsList.Items.Add(CustomCommandItem(cmd));
        }
    }

    private UIElement CustomCommandItem(CustomCommand cmd)
    {
        var editBtn = new Button { Content = "Edit", Width = 60, Height = 30, Margin = new Thickness(0, 0, 6, 0), Style = (Style)FindResource("GhostButton") };
        var delBtn  = new Button { Content = "Delete", Width = 64, Height = 30, Background = new SolidColorBrush(Color.FromArgb(30, 255, 59, 48)), BorderThickness = new Thickness(0), Style = (Style)FindResource("GhostButton") };
        delBtn.Foreground = new SolidColorBrush(Color.FromRgb(255, 59, 48));

        editBtn.Click += (_, _) => OpenCommandDialog(cmd);
        delBtn.Click  += (_, _) =>
        {
            var list = CustomCommandStore.Load();
            list.RemoveAll(c => c.Id == cmd.Id);
            CustomCommandStore.Save(list);
            PopulateCustomCommands();
            PopulateHome();
        };

        var icon = new Border
        {
            Width = 38, Height = 38, CornerRadius = new CornerRadius(10),
            Background = new SolidColorBrush(Color.FromArgb(30, 123, 97, 255)),
            Margin = new Thickness(0, 0, 12, 0),
            Child = new TextBlock { Text = "✦", FontSize = 16, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center }
        };

        var info = new StackPanel
        {
            Children =
            {
                new TextBlock { Text = cmd.Name, FontSize = 14, FontWeight = FontWeights.SemiBold, Foreground = Brushes.White },
                new TextBlock { Text = cmd.Trigger, FontSize = 12, Foreground = new SolidColorBrush(Color.FromRgb(158, 143, 255)), FontFamily = new FontFamily("Consolas") },
                new TextBlock { Text = cmd.Prompt, FontSize = 12, Foreground = new SolidColorBrush(Color.FromRgb(138, 138, 138)), TextTrimming = TextTrimming.CharacterEllipsis }
            }
        };

        var grid = new Grid();
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        grid.ColumnDefinitions.Add(new ColumnDefinition());
        grid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        Grid.SetColumn(icon,  0); grid.Children.Add(icon);
        Grid.SetColumn(info,  1); grid.Children.Add(info);
        var btnRow = new StackPanel { Orientation = Orientation.Horizontal, VerticalAlignment = VerticalAlignment.Center, Children = { editBtn, delBtn } };
        Grid.SetColumn(btnRow, 2); grid.Children.Add(btnRow);

        return new Border { Padding = new Thickness(14), Margin = new Thickness(4), Child = grid };
    }

    private void AddCommand_Click(object sender, RoutedEventArgs e) => OpenCommandDialog(null);

    private void OpenCommandDialog(CustomCommand? existing)
    {
        var dlg = new CommandDialog(existing) { Owner = this };
        if (dlg.ShowDialog() == true)
        {
            var list = CustomCommandStore.Load();
            if (existing != null)
                list.RemoveAll(c => c.Id == existing.Id);
            list.Add(dlg.Result!);
            CustomCommandStore.Save(list);
            PopulateCustomCommands();
            PopulateHome();
        }
    }

    // ── Explore ───────────────────────────────────────────────
    private void PopulateExplore()
    {
        UseCasesList.Items.Clear();
        foreach (var (emoji, title, desc) in UseCases)
        {
            var card = new Border
            {
                CornerRadius = new CornerRadius(14),
                Background   = new SolidColorBrush(Color.FromRgb(26, 26, 26)),
                Padding      = new Thickness(14),
                Margin       = new Thickness(0, 0, 0, 10),
                Child        = new StackPanel
                {
                    Orientation = Orientation.Horizontal,
                    Children    =
                    {
                        new Border { Width = 44, Height = 44, CornerRadius = new CornerRadius(10), Background = new SolidColorBrush(Color.FromRgb(36, 36, 36)), Margin = new Thickness(0, 0, 14, 0), Child = new TextBlock { Text = emoji, FontSize = 22, HorizontalAlignment = HorizontalAlignment.Center, VerticalAlignment = VerticalAlignment.Center } },
                        new StackPanel
                        {
                            VerticalAlignment = VerticalAlignment.Center,
                            Children =
                            {
                                new TextBlock { Text = title, FontSize = 14, FontWeight = FontWeights.SemiBold, Foreground = Brushes.White },
                                new TextBlock { Text = desc,  FontSize = 13, Foreground = new SolidColorBrush(Color.FromRgb(138, 138, 138)), TextWrapping = TextWrapping.Wrap, LineHeight = 19, Margin = new Thickness(0, 3, 0, 0) }
                            }
                        }
                    }
                }
            };
            UseCasesList.Items.Add(card);
        }
    }

    // ── Settings ──────────────────────────────────────────────
    private void LoadSettings()
    {
        Settings.Reload();
        ApiKeyBox.Password   = Settings.ApiKey;
        TempSlider.Value     = Settings.Temperature;
        UpdateTempLabel(Settings.Temperature);
    }

    private void SaveApiKey_Click(object sender, RoutedEventArgs e)
    {
        Settings.ApiKey = ApiKeyBox.Password.Trim();
        ApiKeySavedLabel.Visibility = Visibility.Visible;
        PopulateHome();
    }

    private void ApiKeyBox_PasswordChanged(object sender, RoutedEventArgs e)
        => ApiKeySavedLabel.Visibility = Visibility.Collapsed;

    private void TempSlider_Changed(object sender, RoutedPropertyChangedEventArgs<double> e)
    {
        Settings.Temperature = e.NewValue;
        UpdateTempLabel(e.NewValue);
    }

    private void UpdateTempLabel(double val)
    {
        if (TempLabel == null) return;
        (TempLabel.Text, var color) = val switch
        {
            < 0.4  => ("Precise",  Color.FromRgb(79,  195, 247)),
            < 0.8  => ("Balanced", Color.FromRgb(123, 97,  255)),
            < 1.1  => ("Creative", Color.FromRgb(255, 159, 10)),
            _      => ("Wild",     Color.FromRgb(255, 59,  48)),
        };
        var brush = new SolidColorBrush(color);
        TempLabel.Foreground = brush;
        if (TempValue != null) { TempValue.Text = $"{val:F1}"; TempValue.Foreground = brush; }
    }
}
