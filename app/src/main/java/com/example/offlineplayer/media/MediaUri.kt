package com.example.offlineplayer.media

import java.net.URI

/** Строка → [URI], только если это абсолютная ссылка со схемой и хостом; иначе null. */
fun String.toMediaUriOrNull(): URI? {
    val uri = runCatching { URI(trim()) }.getOrNull() ?: return null
    if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) return null
    return uri
}
