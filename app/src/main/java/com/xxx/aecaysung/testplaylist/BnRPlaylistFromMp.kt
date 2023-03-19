package com.xxx.aecaysung.testplaylist

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore.Audio
import android.util.Log
import androidx.core.database.getLongOrNull
import com.google.gson.Gson

object BnRPlaylistFromMp {

    private val PLAYLIST_PROJECTION by lazy {
        arrayOf(
            Audio.Playlists._ID,
            Audio.Playlists.NAME
        )
    }
    val DETAIL_PLAYLIST_PROJECTION by lazy { arrayOf(Audio.Playlists.Members.DATA) }
    val PLAYLIST_URI = Audio.Playlists.EXTERNAL_CONTENT_URI
    val gson by lazy { Gson() }

    // Read all playlist info and convert to json
    fun backup(context: Context): String {
        val results = arrayListOf<BackupPlaylist>()
        context.contentResolver.query(PLAYLIST_URI, PLAYLIST_PROJECTION, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                c.getString(1)?.takeIf { it.isNotEmpty() }?.let { name ->
                    val item = BackupPlaylist(name, getAudioPaths(context, id))
                    Log.d("tran.dc", "current: $item\n================================")
                    results += item
                }
                deletePlaylist(context, id)
            }
        }
        return gson.toJson(BackupData(results))
    }

    private fun deletePlaylist(context: Context, pId: Long) {
        context.contentResolver.delete(PLAYLIST_URI, "${Audio.Playlists._ID}=$pId", null)
    }

    fun restore(context: Context, dataJson: String) {
        gson.fromJson(dataJson, BackupData::class.java)?.playlists?.let { playlists ->
            playlists.forEach { item ->
                val pId = createPlaylist(context, item.name)
                addToPlaylist(context, pId, item.audios)
            }
        }


//        val projection = arrayOf(
//            Audio.Playlists._ID,
//            Audio.Playlists.NAME,
//            Audio.Playlists.DATA,
//        )
//        context.contentResolver.query(PLAYLIST_URI, projection, null, null, null)?.use { c ->
//            while (c.moveToNext()) {
//                val id = c.getLong(0)
//                val name = c.getString(1)
//                val path = c.getString(2)
//                Log.d("tran.dc", "=========================")
//                Log.d("tran.dc", "id: $id, name: $name => path: $path")
//                loadAudios(context, id)
//            }
//        }
    }

    fun loadAudios(context: Context, pId: Long) {
        val uri = Audio.Playlists.Members.getContentUri("external", pId)
        val projection = arrayOf(
            Audio.Playlists.Members.AUDIO_ID,
            Audio.Playlists.Members.TITLE,
            Audio.Playlists.Members.DATA
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val title = c.getString(1)
                val path = c.getString(2)
                Log.d("tran.dc", "current: $id, $title, $path")
            }
        }
        Log.d("tran.dc", "========================")
    }

    private fun addToPlaylist(context: Context, pId: Long, paths: List<String>) {
        val ids = paths.map { getAudioIdByPath(context, it) }
        val playListUri = Audio.Playlists.Members.getContentUri("external", pId)
        var count = getPlaylistCount(context, pId)
        ids.forEach {
            val contentValues = ContentValues(1).apply {
                put(Audio.Playlists.Members.AUDIO_ID, it)
                put(Audio.Playlists.Members.PLAY_ORDER, count + 1)
            }
            context.contentResolver.insert(playListUri, contentValues, null)
            ++count
        }
    }

    private fun getPlaylistCount(context: Context, pId: Long): Int {
        val playListUri = Audio.Playlists.Members.getContentUri("external", pId)
        context.contentResolver.query(playListUri, arrayOf(Audio.Playlists.Members.AUDIO_ID), null, null)?.use {
            return it.count
        }
        return 0
    }

    private fun createPlaylist(context: Context, name: String): Long {
        var pId = getPlaylistId(context, name)
        if (pId != -1L) return pId
        val content = ContentValues(1).apply {
            put(Audio.Playlists.NAME, name)
        }
        val uri = context.contentResolver.insert(PLAYLIST_URI, content)
        Log.d("tran.dc", "create playlist uri: $uri")
        pId = getPlaylistId(context, name)
        return pId
    }

    private fun getPlaylistId(context: Context, name: String): Long {
        context.contentResolver.query(
            PLAYLIST_URI,
            arrayOf(Audio.Media._ID, Audio.Playlists.NAME),
            "${Audio.Playlists.NAME}=?",
            arrayOf(name),
            null
        )?.use { c ->
            c.moveToFirst()
            return if (c.count > 0) c.getLongOrNull(0) ?: -1 else -1
        }
        return -1
    }

    private fun getAudioPaths(context: Context, pId: Long) = buildList {
        val pUri = Audio.Playlists.Members.getContentUri("external", pId)
        context.contentResolver.query(pUri, DETAIL_PLAYLIST_PROJECTION, null, null)?.use { c ->
            while (c.moveToNext()) {
                c.getString(0)?.takeIf { it.isNotEmpty() }?.let { this += it }
            }
        }
    }

    private fun getAudioIdByPath(context: Context, path: String): Long {
        val uri = Audio.Media.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            uri,
            arrayOf(Audio.Media._ID),
            "${Audio.Media.DATA}=?",
            arrayOf(path),
            null
        )?.use { c ->
            c.moveToFirst()
            return c.getLongOrNull(0) ?: -1L
        }
        return -1
    }

    data class BackupPlaylist(
        val name: String,
        val audios: List<String>
    )

    data class BackupData(val playlists: List<BackupPlaylist>? = null)
}