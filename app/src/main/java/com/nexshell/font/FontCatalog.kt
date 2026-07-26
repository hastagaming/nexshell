package com.nexshell.font

import android.content.Context
import android.graphics.Typeface
import java.io.File

enum class FontSource { NERD_FONT, SYSTEM, CUSTOM }

data class FontOption(
    val displayName: String,
    val source: FontSource,
    val assetPath: String? = null,   // for NERD_FONT, path under assets/fonts/
    val customFilePath: String? = null // for CUSTOM, absolute path after import
)

object FontCatalog {
    val nerdFonts = listOf(
        FontOption("JetBrainsMono Nerd Font", FontSource.NERD_FONT, "fonts/JetBrainsMonoNerdFont-Regular.ttf"),
        FontOption("FiraCode Nerd Font", FontSource.NERD_FONT, "fonts/FiraCodeNerdFont-Regular.ttf"),
        FontOption("Hack Nerd Font", FontSource.NERD_FONT, "fonts/HackNerdFont-Regular.ttf"),
        FontOption("Iosevka Nerd Font", FontSource.NERD_FONT, "fonts/IosevkaNerdFont-Regular.ttf"),
        FontOption("Meslo Nerd Font", FontSource.NERD_FONT, "fonts/MesloLGSNerdFont-Regular.ttf")
    )

    val systemDefault = FontOption("System Default", FontSource.SYSTEM)

    fun resolveTypeface(context: Context, option: FontOption): Typeface {
        return when (option.source) {
            FontSource.NERD_FONT -> Typeface.createFromAsset(context.assets, option.assetPath!!)
            FontSource.SYSTEM -> Typeface.MONOSPACE
            FontSource.CUSTOM -> Typeface.createFromFile(File(option.customFilePath!!))
        }
    }
}