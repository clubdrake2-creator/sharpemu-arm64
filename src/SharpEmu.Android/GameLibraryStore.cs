// Copyright (C) 2026 SharpEmu Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later

using System.Text.Json;
using SharpEmu.HLE.Host;

namespace SharpEmu.Android;

/// <summary>
/// Persists the Android game library (folders added via the Compose "add folder" picker) to a JSON
/// file under <see cref="AndroidHostPaths"/>. The Kotlin side (EmulatorRepository.kt) already resolves
/// the SAF tree URI the user picks down to a real absolute filesystem path and, for addGameFolder,
/// already verified the folder looks like a PS4/PS5 game (eboot.bin or sce_sys/param.sfo present)
/// before calling here — this only needs to persist it and read its sce_sys/param.json metadata, the
/// same way SharpEmu.GUI's MainWindow.axaml.cs does for the desktop library (TryReadParamJson/
/// FindCoverFor, ported here verbatim since that logic has no Avalonia dependency).
/// </summary>
internal static class GameLibraryStore
{
    private static readonly object Lock = new();
    private static List<GameRecord>? _cache;

    public static bool AddGameFolder(string displayName, string path)
    {
        var ebootPath = Path.Combine(path, "eboot.bin");
        if (!File.Exists(ebootPath))
        {
            return false;
        }

        lock (Lock)
        {
            var games = Load();
            if (games.Any(g => string.Equals(g.Path, ebootPath, StringComparison.OrdinalIgnoreCase)))
            {
                return true;
            }

            var (title, titleId, version) = TryReadParamJson(ebootPath);
            long sizeBytes;
            try
            {
                sizeBytes = new FileInfo(ebootPath).Length;
            }
            catch (IOException)
            {
                sizeBytes = 0;
            }

            // Path must be the eboot.bin file itself, not its containing folder — this matches
            // desktop's GameEntry.Path convention (SharpEmu.GUI's MainWindow.axaml.cs scan) and is
            // what SharpEmuRuntime.Run/LoadImage actually expects. Passing the folder here caused a
            // real on-device FileNotFoundException the moment a game was launched.
            games.Add(new GameRecord(
                Id: !string.IsNullOrEmpty(titleId) ? titleId : path,
                Title: !string.IsNullOrEmpty(title) ? title : displayName,
                Path: ebootPath,
                Source: path,
                Icon: FindCoverFor(ebootPath) ?? string.Empty,
                Version: version ?? string.Empty,
                SizeBytes: sizeBytes));

            Save(games);
            return true;
        }
    }

    public static string GetGames()
    {
        lock (Lock)
        {
            var games = Load();
            return JsonSerializer.Serialize(games.Select(g => new
            {
                id = g.Id,
                title = g.Title,
                path = g.Path,
                source = g.Source,
                icon = g.Icon,
                version = g.Version,
                sizeBytes = g.SizeBytes,
            }));
        }
    }

    public static bool DeleteGame(string gameId, bool forceDeleteFolder)
    {
        lock (Lock)
        {
            var games = Load();
            var match = games.FirstOrDefault(g => g.Id == gameId);
            if (match is null)
            {
                return false;
            }

            games.Remove(match);
            Save(games);

            if (forceDeleteFolder)
            {
                var folder = Path.GetDirectoryName(match.Path);
                if (folder is not null)
                {
                    try
                    {
                        Directory.Delete(folder, recursive: true);
                    }
                    catch (IOException)
                    {
                    }
                    catch (UnauthorizedAccessException)
                    {
                    }
                }
            }

            return true;
        }
    }

    private static string StorePath =>
        Path.Combine(AndroidHostPaths.InternalFilesRoot ?? AndroidHostPaths.ExternalFilesRoot ?? AppContext.BaseDirectory, "games.json");

    private static List<GameRecord> Load()
    {
        if (_cache is not null)
        {
            return _cache;
        }

        try
        {
            if (File.Exists(StorePath))
            {
                var json = File.ReadAllText(StorePath);
                _cache = JsonSerializer.Deserialize<List<GameRecord>>(json) ?? [];
                return _cache;
            }
        }
        catch (Exception exception) when (exception is IOException or JsonException)
        {
        }

        _cache = [];
        return _cache;
    }

    private static void Save(List<GameRecord> games)
    {
        _cache = games;
        try
        {
            File.WriteAllText(StorePath, JsonSerializer.Serialize(games));
        }
        catch (IOException)
        {
        }
    }

    /// <summary>
    /// Reads the game title, title id and content version from sce_sys/param.json next to the
    /// executable, when present. Ported from SharpEmu.GUI's MainWindow.axaml.cs TryReadParamJson.
    /// </summary>
    private static (string? Title, string? TitleId, string? Version) TryReadParamJson(string ebootPath)
    {
        try
        {
            var directory = Path.GetDirectoryName(ebootPath);
            if (directory is null)
            {
                return (null, null, null);
            }

            var paramPath = Path.Combine(directory, "sce_sys", "param.json");
            if (!File.Exists(paramPath))
            {
                return (null, null, null);
            }

            using var document = JsonDocument.Parse(File.ReadAllText(paramPath));
            var root = document.RootElement;

            string? titleId = null;
            if (root.TryGetProperty("titleId", out var idElement) && idElement.ValueKind == JsonValueKind.String)
            {
                titleId = idElement.GetString();
            }

            string? version = null;
            if (root.TryGetProperty("contentVersion", out var versionElement) &&
                versionElement.ValueKind == JsonValueKind.String)
            {
                version = versionElement.GetString();
            }
            else if (root.TryGetProperty("masterVersion", out var masterElement) &&
                     masterElement.ValueKind == JsonValueKind.String)
            {
                version = masterElement.GetString();
            }

            string? title = null;
            if (root.TryGetProperty("localizedParameters", out var localized) &&
                localized.ValueKind == JsonValueKind.Object)
            {
                if (localized.TryGetProperty("defaultLanguage", out var language) &&
                    language.ValueKind == JsonValueKind.String &&
                    localized.TryGetProperty(language.GetString()!, out var defaultBlock) &&
                    defaultBlock.ValueKind == JsonValueKind.Object &&
                    defaultBlock.TryGetProperty("titleName", out var titleName) &&
                    titleName.ValueKind == JsonValueKind.String)
                {
                    title = titleName.GetString();
                }
                else
                {
                    foreach (var property in localized.EnumerateObject())
                    {
                        if (property.Value.ValueKind == JsonValueKind.Object &&
                            property.Value.TryGetProperty("titleName", out var anyTitleName) &&
                            anyTitleName.ValueKind == JsonValueKind.String)
                        {
                            title = anyTitleName.GetString();
                            break;
                        }
                    }
                }
            }

            return (
                string.IsNullOrWhiteSpace(title) ? null : title,
                string.IsNullOrWhiteSpace(titleId) ? null : titleId,
                string.IsNullOrWhiteSpace(version) ? null : version.Trim());
        }
        catch (Exception exception) when (exception is IOException or JsonException)
        {
            return (null, null, null);
        }
    }

    /// <summary>
    /// Finds the cover art shipped with the game: sce_sys/icon0.png next to the executable, falling
    /// back to pic0.png. Ported from SharpEmu.GUI's MainWindow.axaml.cs FindCoverFor.
    /// </summary>
    private static string? FindCoverFor(string ebootPath)
    {
        var directory = Path.GetDirectoryName(ebootPath);
        if (directory is null)
        {
            return null;
        }

        var sceSys = Path.Combine(directory, "sce_sys");
        foreach (var candidate in new[] { "icon0.png", "pic0.png" })
        {
            var coverPath = Path.Combine(sceSys, candidate);
            if (File.Exists(coverPath))
            {
                return coverPath;
            }
        }

        return null;
    }

    private sealed record GameRecord(
        string Id, string Title, string Path, string Source, string Icon, string Version, long SizeBytes);
}
