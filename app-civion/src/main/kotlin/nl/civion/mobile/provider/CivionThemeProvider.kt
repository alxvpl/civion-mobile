package nl.civion.mobile.provider

import net.thunderbird.core.ui.theme.api.ThemeProvider
import nl.civion.mobile.R

internal class CivionThemeProvider : ThemeProvider {
    override val appThemeResourceId = R.style.Theme_Civion_DayNight
    override val appLightThemeResourceId = R.style.Theme_Civion_Light
    override val appDarkThemeResourceId = R.style.Theme_Civion_Dark
    override val dialogThemeResourceId = R.style.Theme_Civion_DayNight_Dialog
    override val translucentDialogThemeResourceId = R.style.Theme_Civion_DayNight_Dialog_Translucent
}
