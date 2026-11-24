package io.github.horse3903.airdeck.data

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import io.github.horse3903.airdeck.R
import io.github.horse3903.airdeck.ui.theme.GlossyBlue
import io.github.horse3903.airdeck.ui.theme.GlossyGreen
import io.github.horse3903.airdeck.ui.theme.GlossyRed
import io.github.horse3903.airdeck.ui.theme.NeonBlue
import io.github.horse3903.airdeck.ui.theme.NeonGreen
import io.github.horse3903.airdeck.ui.theme.NeonRed

enum class BaseColor(@StringRes val nameRes: Int) {
    RED(R.string.color_red),
    GREEN(R.string.color_green),
    BLUE(R.string.color_blue);

    companion object {
        fun fromInt(i: Int): BaseColor {
            return when (i) {
                0 -> RED
                1 -> GREEN
                2 -> BLUE
                else -> throw IllegalArgumentException("Invalid BaseColor value")
            }
        }
    }
}

/**
 * Returns a color from a given base color name depending on the current color scheme.
 */
fun getColorFromBaseColor(baseColor: BaseColor, isDarkMode: Boolean): Color {
    return when (baseColor) {
        BaseColor.RED -> if (isDarkMode) NeonRed else GlossyRed
        BaseColor.GREEN -> if (isDarkMode) NeonGreen else GlossyGreen
        BaseColor.BLUE -> if (isDarkMode) NeonBlue else GlossyBlue
    }
}
