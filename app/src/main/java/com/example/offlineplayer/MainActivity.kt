package com.example.offlineplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.offlineplayer.media.MediaStorage
import com.example.offlineplayer.media.MediaType
import com.example.offlineplayer.media.SavedMedia
import com.example.offlineplayer.ui.edit.MediaEditScreen
import com.example.offlineplayer.ui.theme.OfflinePlayerTheme
import org.slf4j.LoggerFactory
import java.io.File

class MainActivity : ComponentActivity() {

    private val log = LoggerFactory.getLogger(MainActivity::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val storage = MediaStorage(File(filesDir, "saved_media.json"))
        storage.load()
        log.info("onCreate: loaded {} items", storage.items.size)

        val media = SavedMedia(url = "https://youtu.be/test", type = MediaType.Video, title = "Test video")
        storage.add(media)
        storage.save()
        log.info("onCreate: after add+save, {} items", storage.items.size)

        val removed = storage.remove(media.id)
        storage.save()
        log.info("onCreate: removed={}, after remove+save, {} items", removed, storage.items.size)

        enableEdgeToEdge()
        setContent {
            OfflinePlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MediaEditScreen(
                        initial = media,
                        onSave = { edited -> log.info("edit screen: saved {}", edited.title) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}