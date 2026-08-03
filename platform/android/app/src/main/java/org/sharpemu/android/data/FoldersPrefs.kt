// SPDX-FileCopyrightText: Copyright 2026 shadPS4 Emulator Project
// SPDX-License-Identifier: GPL-2.0-or-later
// Adapted for the SharpEmu Emulator Project, Copyright (C) 2026.

package org.sharpemu.android.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// A home-screen folder grouping several games together (like an iOS/PS app folder). Pure Android-side
// UI state, so it lives in SharedPreferences rather than the native emulator config. Games are
// referenced by their grid key (the game's path, which the library list uses as a stable unique id).
data class GameFolder(
    val id: String,
    val name: String,
    val gamePaths: List<String>,
    val favorite: Boolean,
)

object FoldersPrefs {
    private const val PREFS = "game_folders"
    private const val KEY = "folders"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getFolders(context: Context): List<GameFolder> {
        val raw = prefs(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val pathsArr = o.optJSONArray("paths") ?: JSONArray()
                    val paths = buildList {
                        for (j in 0 until pathsArr.length()) add(pathsArr.getString(j))
                    }
                    add(
                        GameFolder(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            gamePaths = paths,
                            favorite = o.optBoolean("favorite"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, folders: List<GameFolder>) {
        val arr = JSONArray()
        for (f in folders) {
            // Drop folders that ended up empty so they don't linger as ghosts on the home screen.
            if (f.gamePaths.isEmpty()) continue
            val o = JSONObject()
            o.put("id", f.id)
            o.put("name", f.name)
            o.put("favorite", f.favorite)
            o.put("paths", JSONArray(f.gamePaths))
            arr.put(o)
        }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    // Returns the folder that contains the given game path, or null if it is ungrouped.
    fun folderOf(context: Context, gamePath: String): GameFolder? =
        getFolders(context).firstOrNull { it.gamePaths.contains(gamePath) }

    // Creates a new (unnamed) folder from two games and returns it. A game can live in only one
    // folder, so both are first removed from any folder they were already in.
    fun createFolder(context: Context, firstPath: String, secondPath: String): GameFolder {
        val folders = getFolders(context).map {
            it.copy(gamePaths = it.gamePaths.filterNot { p -> p == firstPath || p == secondPath })
        }.toMutableList()
        val folder = GameFolder(
            id = "folder_" + System.currentTimeMillis(),
            name = "",
            gamePaths = listOf(firstPath, secondPath),
            favorite = false,
        )
        folders.add(folder)
        save(context, folders)
        return folder
    }

    // Adds a game to an existing folder (removing it from any other folder first).
    fun addGameToFolder(context: Context, folderId: String, gamePath: String) {
        val folders = getFolders(context).map {
            it.copy(gamePaths = it.gamePaths.filterNot { p -> p == gamePath })
        }.map {
            if (it.id == folderId && !it.gamePaths.contains(gamePath)) {
                it.copy(gamePaths = it.gamePaths + gamePath)
            } else {
                it
            }
        }
        save(context, folders)
    }

    fun addGamesToFolder(context: Context, folderId: String, gamePaths: Collection<String>) {
        if (gamePaths.isEmpty()) return
        val paths = gamePaths.toSet()
        val folders = getFolders(context).map {
            it.copy(gamePaths = it.gamePaths.filterNot { p -> paths.contains(p) })
        }.map {
            if (it.id == folderId) {
                it.copy(gamePaths = (it.gamePaths + paths).distinct())
            } else {
                it
            }
        }
        save(context, folders)
    }

    fun rename(context: Context, folderId: String, name: String) {
        save(context, getFolders(context).map {
            if (it.id == folderId) it.copy(name = name) else it
        })
    }

    fun toggleFavorite(context: Context, folderId: String): Boolean {
        var nowFav = false
        save(context, getFolders(context).map {
            if (it.id == folderId) {
                nowFav = !it.favorite
                it.copy(favorite = nowFav)
            } else {
                it
            }
        })
        return nowFav
    }

    // Removes a game from its folder (the game returns to the home screen). If the folder is left with
    // a single game, it is dissolved too so a "folder of one" never appears.
    fun removeGameFromFolder(context: Context, gamePath: String) {
        val folders = getFolders(context).map {
            it.copy(gamePaths = it.gamePaths.filterNot { p -> p == gamePath })
        }.filter { it.gamePaths.size >= 2 }
        save(context, folders)
    }

    // Deletes a folder entirely; all of its games return to the home screen.
    fun removeFolder(context: Context, folderId: String) {
        save(context, getFolders(context).filterNot { it.id == folderId })
    }
}
