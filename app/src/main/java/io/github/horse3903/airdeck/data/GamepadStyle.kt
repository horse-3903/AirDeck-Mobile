package io.github.horse3903.airdeck.data

import androidx.annotation.StringRes
import io.github.horse3903.airdeck.R

enum class GamepadStyle(@StringRes val nameRes: Int) {
    PLAYSTATION(R.string.style_playstation),
    XBOX(R.string.style_xbox),
    NINTENDO(R.string.style_nintendo);

    companion object {
        fun fromInt(i: Int): GamepadStyle = entries.getOrElse(i) { PLAYSTATION }
    }
}
