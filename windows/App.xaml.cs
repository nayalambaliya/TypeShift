using System.Windows;
using System.Windows.Forms;
using TypeShift.Core;
using Application = System.Windows.Application;

namespace TypeShift;

public partial class App : Application
{
    private TextProcessor? _processor;
    private NotifyIcon?    _trayIcon;
    private MainWindow?    _mainWindow;

    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        _processor = new TextProcessor();
        _processor.Start();

        BuildTray();
    }

    private void BuildTray()
    {
        _trayIcon = new NotifyIcon
        {
            Text    = "TypeShift",
            Visible = true,
            Icon    = SystemIcons.Application,
        };

        var menu = new ContextMenuStrip();
        menu.BackColor = System.Drawing.Color.FromArgb(13, 13, 13);
        menu.ForeColor = System.Drawing.Color.White;
        menu.RenderMode = ToolStripRenderMode.System;

        // Status label
        var statusItem = new ToolStripLabel("● TypeShift Active") { ForeColor = System.Drawing.Color.FromArgb(52, 199, 89) };
        menu.Items.Add(statusItem);
        menu.Items.Add(new ToolStripSeparator());

        // Built-in commands header
        menu.Items.Add(new ToolStripLabel("BUILT-IN COMMANDS") { ForeColor = System.Drawing.Color.Gray });

        void AddCmd(string label, string trigger, string instr)
        {
            var item = new ToolStripMenuItem(label);
            item.Click += async (_, _) => await _processor!.ProcessSelectionAsync(instr);
            menu.Items.Add(item);
        }

        AddCmd("Fix Grammar",    "?fix",     "Fix all grammar and spelling mistakes. Return only the corrected text, nothing else.");
        AddCmd("Improve",        "?improve", "Improve this text to be clearer and more engaging. Return only the improved text, nothing else.");
        AddCmd("Make Formal",    "?formal",  "Rewrite this text in a formal, professional tone. Return only the rewritten text, nothing else.");
        AddCmd("Make Casual",    "?casual",  "Rewrite this text in a casual, friendly tone. Return only the rewritten text, nothing else.");
        AddCmd("Shorten",        "?shorter", "Make this text shorter and more concise. Return only the shortened text, nothing else.");
        AddCmd("Expand",         "?longer",  "Expand this text with more detail. Return only the expanded text, nothing else.");
        AddCmd("Write Reply",    "?reply",   "Write a natural, friendly reply to this message. Return only the reply, nothing else.");
        AddCmd("Add Emojis",     "?emoji",   "Add relevant and fun emojis throughout this text. Return only the text with emojis, nothing else.");
        AddCmd("Summarize",      "?tldr",    "Summarize this text in one sentence. Return only the summary, nothing else.");
        AddCmd("Make Human",     "?human",   "Rewrite this text to sound more natural and human. Return only the rewritten text, nothing else.");

        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add(new ToolStripLabel("MY COMMANDS") { ForeColor = System.Drawing.Color.Gray, Tag = "myCommandsHeader" });

        // Placeholder — refreshed on open
        menu.Opening += (_, _) => RefreshCustomCommands(menu);

        menu.Items.Add(new ToolStripSeparator());

        var settingsItem = new ToolStripMenuItem("Settings…");
        settingsItem.Click += (_, _) => ShowMainWindow();
        menu.Items.Add(settingsItem);

        menu.Items.Add(new ToolStripSeparator());

        var quitItem = new ToolStripMenuItem("Quit TypeShift");
        quitItem.Click += (_, _) => { _trayIcon?.Dispose(); Shutdown(); };
        menu.Items.Add(quitItem);

        _trayIcon.ContextMenuStrip = menu;
        _trayIcon.DoubleClick     += (_, _) => ShowMainWindow();
    }

    private void RefreshCustomCommands(ContextMenuStrip menu)
    {
        // Remove old custom command items (between MY COMMANDS header and the separator after it)
        var headerIdx = -1;
        for (int i = 0; i < menu.Items.Count; i++)
            if (menu.Items[i].Tag?.ToString() == "myCommandsHeader") { headerIdx = i; break; }

        if (headerIdx < 0) return;

        // Remove items after header until next separator
        while (headerIdx + 1 < menu.Items.Count && menu.Items[headerIdx + 1] is not ToolStripSeparator)
            menu.Items.RemoveAt(headerIdx + 1);

        var customs = Models.CustomCommandStore.Load();
        if (customs.Count == 0)
        {
            var none = new ToolStripMenuItem("No custom commands") { Enabled = false };
            menu.Items.Insert(headerIdx + 1, none);
        }
        else
        {
            for (int i = customs.Count - 1; i >= 0; i--)
            {
                var cmd = customs[i];
                var item = new ToolStripMenuItem(cmd.Name);
                item.Click += async (_, _) => await _processor!.ProcessSelectionAsync(cmd.Prompt);
                menu.Items.Insert(headerIdx + 1, item);
            }
        }
    }

    private void ShowMainWindow()
    {
        if (_mainWindow == null || !_mainWindow.IsLoaded)
        {
            _mainWindow = new MainWindow();
            _mainWindow.Show();
        }
        else
        {
            _mainWindow.Activate();
            _mainWindow.WindowState = WindowState.Normal;
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        _trayIcon?.Dispose();
        _processor?.Stop();
        base.OnExit(e);
    }
}
