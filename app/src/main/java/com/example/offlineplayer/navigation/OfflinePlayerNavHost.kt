package com.example.offlineplayer.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.offlineplayer.state.LibraryAction
import com.example.offlineplayer.state.LibraryStore
import com.example.offlineplayer.ui.edit.ColorPickerScreen
import com.example.offlineplayer.ui.edit.MediaEditScreen
import com.example.offlineplayer.ui.list.MediaListScreen

private const val PICKER_ENTER_MS = 400
private const val PICKER_EXIT_MS = 300

/**
 * Карта экранов. Навигацию решает только она; бизнес-изменения не делает, а отправляет в [store] действиями.
 *
 * Компонент:
 * - Вход: [store] (состояние для экранов и точка приёма действий).
 * - Выход: переходы между экранами, действия в [store].
 * - Конфигурация: длительности анимаций.
 * - Зависимости: `NavController` (создаётся здесь же), [LibraryStore].
 */
@Composable
fun OfflinePlayerNavHost(store: LibraryStore, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val state by store.state.collectAsState()

    NavHost(navController = navController, startDestination = LibraryRoute, modifier = modifier) {
        composable<LibraryRoute> {
            MediaListScreen(
                state = state,
                onAction = store::dispatch,
                onAdd = { navController.navigate(EditRoute()) { launchSingleTop = true } },
                onEdit = { media ->
                    navController.navigate(EditRoute(mediaId = media.id.toString())) { launchSingleTop = true }
                }
            )
        }

        composable<EditRoute>(
            // Выбор цвета открывается поверх редактора, поэтому сам редактор при этом не анимируем.
            exitTransition = {
                if (targetState.destination.hasRoute<ColorPickerRoute>()) ExitTransition.None else null
            },
            popEnterTransition = {
                if (initialState.destination.hasRoute<ColorPickerRoute>()) EnterTransition.None else null
            }
        ) { entry ->
            val mediaId = entry.toRoute<EditRoute>().mediaUuid
            val existing = mediaId?.let { id -> state.items.firstOrNull { it.id == id } }
            val pickedArgb by entry.savedStateHandle
                .getStateFlow<Int?>(PICKED_COLOR_KEY, null)
                .collectAsState()

            val waitingForRecord = mediaId != null && existing == null
            if (waitingForRecord && !state.isLoading) {
                // Запись, которую просили открыть, исчезла: возвращаемся, а не открываем пустую форму.
                LaunchedEffect(Unit) { navController.popBackStackFrom<EditRoute>() }
            }

            Scaffold { innerPadding ->
                if (waitingForRecord) {
                    // Список ещё читается с диска (например, после восстановления процесса).
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    MediaEditScreen(
                        initial = existing,
                        onSave = { media ->
                            store.dispatch(LibraryAction.MediaSaved(media))
                            navController.popBackStackFrom<EditRoute>()
                        },
                        onPickColor = { current ->
                            navController.navigate(ColorPickerRoute(color = current.toArgb())) {
                                launchSingleTop = true
                            }
                        },
                        pickedColor = pickedArgb?.let { Color(it) },
                        onPickedColorConsumed = { entry.savedStateHandle.remove<Int>(PICKED_COLOR_KEY) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        composable<ColorPickerRoute>(
            enterTransition = {
                slideIntoContainer(SlideDirection.Up, tween(PICKER_ENTER_MS)) + fadeIn(tween(PICKER_ENTER_MS))
            },
            exitTransition = { fadeOut(tween(PICKER_EXIT_MS)) },
            popExitTransition = {
                slideOutOfContainer(SlideDirection.Down, tween(PICKER_EXIT_MS)) + fadeOut(tween(PICKER_EXIT_MS))
            }
        ) { entry ->
            val initialColor = Color(entry.toRoute<ColorPickerRoute>().color)

            Scaffold { innerPadding ->
                ColorPickerScreen(
                    initialColor = initialColor,
                    onDone = { picked ->
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(PICKED_COLOR_KEY, picked.toArgb())
                        navController.popBackStackFrom<ColorPickerRoute>()
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

/** Закрывает экран, только если он ещё текущий: двойной тап по «Сохранить» не должен вынести из списка. */
private inline fun <reified T : Any> NavController.popBackStackFrom() {
    if (currentDestination?.hasRoute<T>() == true) popBackStack()
}
