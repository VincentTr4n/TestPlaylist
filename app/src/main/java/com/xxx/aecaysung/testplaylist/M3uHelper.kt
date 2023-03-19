package com.xxx.aecaysung.testplaylist

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.xxx.aecaysung.testplaylist.BnRPlaylistFromMp.PLAYLIST_URI
import java.io.BufferedWriter
import java.io.File

object M3uHelper {

    val DEFAULT_MUSIC_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath
    val DEFAULT_ROOT_PATH = Environment.getExternalStorageDirectory().absolutePath
    private val END_TEXT = "#END"
    private val BEGIN_TEXT = "#BEGIN"

    fun backup(context: Context, dstFile: File) {
        val files = getPlaylistFiles(context)

        dstFile.bufferedWriter().use { writer ->
            files.forEach { file ->
                writer.appendLine(BEGIN_TEXT)
                writer.appendLine(file.name)
                file.readLines().forEach { line -> writer.appendLine(line) }
                writer.appendLine(END_TEXT)
                file.delete()
            }
        }
    }

    fun getPlaylistFiles(context: Context): List<File> = buildList {
        val projection = arrayOf(
            MediaStore.Audio.Playlists.DATA,
        )
        context.contentResolver.query(PLAYLIST_URI, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val path = c.getString(0)
                Log.d("tran.dc", "path: $path")
                this += File(path)
            }
        }
    }

    fun restore(context: Context, srcFile: File) {

        var currentName = ""
        var bufferedWriter: BufferedWriter? = null
        val paths = arrayListOf<String>()
        for (line in srcFile.readLines()) {
            Log.d("tran.dc", "current line: $line}")
            if (line.startsWith(BEGIN_TEXT)) {
                currentName = ""
                continue
            }
            if (line.startsWith(END_TEXT)) {
                bufferedWriter?.close()
                paths += File(DEFAULT_MUSIC_PATH, currentName).absolutePath
                continue
            }
            if (currentName.isEmpty()) {
                currentName = line
                bufferedWriter = File(DEFAULT_MUSIC_PATH, currentName).bufferedWriter()
                continue
            }
            bufferedWriter?.appendLine(line)
        }

        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { path, uri ->
            val projection = arrayOf(
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATA,
            )
            context.contentResolver.query(PLAYLIST_URI, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1)
                    val path = c.getString(2)
                    Log.d("tran.dc", "=========================")
                    Log.d("tran.dc", "id: $id, name: $name => path: $path")
                    BnRPlaylistFromMp.loadAudios(context, id)
                }
            }
        }
    }
}