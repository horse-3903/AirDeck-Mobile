package io.github.horse3903.airdeck.data

import androidx.annotation.StringRes
import io.github.horse3903.airdeck.R

enum class GamepadStyle(@StringRes val nameRes: Int) {
    PLAYSTATION(R.string.style_playstation),
    XBOX(R.string.style_xbox);

    companion object {
        fun fromInt(i: Int): GamepadStyle = entries.getOrElse(i) { PLAYSTATION }
    }
}
