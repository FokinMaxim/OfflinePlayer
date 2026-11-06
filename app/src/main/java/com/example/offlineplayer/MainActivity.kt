package com.example.offlineplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.offlineplayer.navigation.OfflinePlayerNavHost
import com.example.offlineplayer.ui.theme.OfflinePlayerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as OfflinePlayerApp).graph
        setContent {
            OfflinePlayerTheme {
                OfflinePlayerNavHost(store = graph.libraryStore, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
