package com.example.offlineplayer.media

import org.json.JSONArray
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Хранилище скачанных [SavedMedia] с сохранением в JSON-файл.
 * Коллекция наружу отдаётся только для чтения — менять её можно только через методы этого класса.
 *
 * [storageFile] должен указывать на файл внутри Context.filesDir приложения:
 * список скачанного — данные пользователя, их нельзя терять при очистке кэша.
 */
class MediaStorage(private val storageFile: File) {

    private val log = LoggerFactory.getLogger(MediaStorage::class.java)

    private val _items: MutableList<SavedMedia> = mutableListOf()

    val items: List<SavedMedia>
        get() = _items.toList()

    /** Добавляет медиа; если запись с таким же id уже есть — заменяет её (upsert). */
    fun add(media: SavedMedia) {
        val replaced = _items.removeAll { it.id == media.id }
        _items.add(media)
        log.debug("add: {} (replaced={}), {} items total", media.id, replaced, _items.size)
    }

    /** Удаляет медиа по id. Возвращает false, если такого id не было — это не ошибка. */
    fun remove(id: UUID): Boolean {
        val removed = _items.removeAll { it.id == id }
        if (removed) {
            log.debug("remove: {} removed, {} items left", id, _items.size)
        } else {
            log.warn("remove: {} not found, nothing to remove", id)
        }
        return removed
    }

    /** Обновляет таймкод просмотра, заменяя элемент через copy() вместо мутации. */
    fun updateProgress(id: UUID, progressSeconds: Int): Boolean {
        val index = _items.indexOfFirst { it.id == id }
        if (index == -1) {
            log.warn("updateProgress: {} not found", id)
            return false
        }
        _items[index] = _items[index].copy(progressSeconds = progressSeconds.coerceAtLeast(0))
        log.debug("updateProgress: {} -> {}s", id, progressSeconds)
        return true
    }

    fun save() {
        log.info("save: start, {} items", _items.size)
        try {
            storageFile.writeText(_items.toJsonArray().toString())
            log.info("save: done, {} bytes", storageFile.length())
        } catch (e: IOException) {
            log.error("save: failed", e)
        }
    }

    /** Загружает список из файла. Отсутствующий файл или битые записи — не ошибка. */
    fun load() {
        log.info("load: start")
        _items.clear()
        if (!storageFile.exists()) {
            log.warn("load: file does not exist, returning empty list")
            return
        }
        val text = storageFile.readText()
        if (text.isBlank()) {
            log.warn("load: file is empty")
            return
        }
        val array = runCatching { JSONArray(text) }.getOrNull()
        if (array == null) {
            log.error("load: file content is not valid JSON")
            return
        }
        _items.addAll(array.parseSavedMediaList())
        log.debug("load: {} items restored", _items.size)
    }
}
