package com.example.offlineplayer

import android.app.Application
import com.example.offlineplayer.di.AppGraph

/** Точка входа графа: живёт столько же, сколько процесс, поэтому состояние переживает поворот экрана. */
class OfflinePlayerApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
