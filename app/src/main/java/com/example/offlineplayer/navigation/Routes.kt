package com.example.offlineplayer.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Типизированные маршруты: каждый экран — класс, аргументы — его поля. Компилятор проверяет
 * и имена, и типы аргументов, а IDE умеет переименовывать маршрут вместе со всеми переходами.
 */

/** Список. Аргументов нет, поэтому объект. */
@Serializable
data object LibraryRoute

/** Редактор. [mediaId] == null — создание новой записи, иначе правка существующей. */
@Serializable
data class EditRoute(val mediaId: String? = null) {
    /** [mediaId] как UUID; null, если это создание или строка не является корректным UUID. */
    val mediaUuid: UUID?
        get() = mediaId?.let { raw -> runCatching { UUID.fromString(raw) }.getOrNull() }
}

/** Выбор цвета. [color] — начальный цвет в ARGB: в аргументы навигации годятся только простые типы. */
@Serializable
data class ColorPickerRoute(val color: Int = Color.White.toArgb())

/** Ключ в savedStateHandle редактора, через который выбранный цвет возвращается с экрана выбора. */
const val PICKED_COLOR_KEY = "pickedColor"
