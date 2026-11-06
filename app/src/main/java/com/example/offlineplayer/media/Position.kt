package com.example.offlineplayer.media

/** Место остановки и полная длительность — связанная пара, поэтому живут и проверяются вместе. */
data class Position(
    val progressSeconds: Int = 0,
    val durationSeconds: Int? = null
) {
    init {
        require(progressSeconds >= 0) { "progressSeconds must not be negative" }
        require(durationSeconds == null || durationSeconds >= 0) {
            "durationSeconds must not be negative"
        }
    }
}
