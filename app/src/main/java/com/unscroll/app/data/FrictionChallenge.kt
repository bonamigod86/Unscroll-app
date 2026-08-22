package com.unscroll.app.data

enum class FrictionChallenge(val id: Int) {
    PHRASE(0),
    STOIC(1),
    BREATH(2);

    companion object {
        fun fromId(id: Int): FrictionChallenge =
            entries.firstOrNull { it.id == id } ?: PHRASE
    }
}
