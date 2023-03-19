package com.xxx.aecaysung.testplaylist

import android.os.Bundle
import android.provider.MediaStore.Audio
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {

    private var jsonData: String = ""
    private val backupFile by lazy { File(dataDir, "backup.text") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnTest1).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                jsonData = BnRPlaylistFromMp.backup(applicationContext)
                backupFile.writeText(jsonData)
                Log.d("tran.dc", "json method 1: $jsonData")
            }
        }

        findViewById<Button>(R.id.btnTest11).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                jsonData = backupFile.readText()
                BnRPlaylistFromMp.restore(applicationContext, jsonData)
            }
        }

        findViewById<Button>(R.id.btnTest2).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                M3uHelper.backup(applicationContext, backupFile)
            }
        }

        findViewById<Button>(R.id.btnTest22).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                M3uHelper.restore(applicationContext, backupFile)
            }
        }
    }

    private fun loadAudios(pId: Long) {
        val uri = Audio.Playlists.Members.getContentUri("external", pId)
        val projection = arrayOf(
            Audio.Playlists.Members.AUDIO_ID,
            Audio.Playlists.Members.TITLE,
            Audio.Playlists.Members.DATA
        )

        contentResolver.query(uri, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val title = c.getString(1)
                val path = c.getString(2)
                Log.d("tran.dc", "current: $id, $title, $path")
            }
        }
    }
}