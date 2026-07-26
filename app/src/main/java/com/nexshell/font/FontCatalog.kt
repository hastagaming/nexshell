package com.nexshell.font

import android.content.Context
import android.graphics.Typeface
import java.io.File

enum class FontSource { NERD_FONT, MONOSPACE, SYSTEM, CUSTOM }
enum class FontStyle { REGULAR, BOLD, ITALIC, BOLD_ITALIC }

/**
 * A font family with up to 4 style variants. Nerd Fonts release zips
 * ship all four for nearly every family; asset paths follow the
 * convention "<Base>NerdFont-<Style>.ttf" as extracted from the official
 * archives. If a family only ships some styles, the missing ones fall
 * back to Regular at resolve time rather than crashing.
 */
data class FontFamilyOption(
    val displayName: String,
    val source: FontSource,
    val assetBase: String? = null,       // e.g. "fonts/JetBrainsMonoNerdFont"
    val customFilePaths: Map<FontStyle, String>? = null // for CUSTOM imports
) {
    fun assetPathFor(style: FontStyle): String? {
        if (assetBase == null) return null
        val suffix = when (style) {
            FontStyle.REGULAR -> "Regular"
            FontStyle.BOLD -> "Bold"
            FontStyle.ITALIC -> "Italic"
            FontStyle.BOLD_ITALIC -> "BoldItalic"
        }
        return "$assetBase-$suffix.ttf"
    }
}

object FontCatalog {

    // assetBase points at the shared filename prefix; actual per-style
    // files are "<assetBase>-Regular.ttf", "-Bold.ttf", "-Italic.ttf",
    // "-BoldItalic.ttf" as extracted from each official nerd-fonts zip.
    val nerdFonts = listOf(
        "0xProto" to "fonts/0xProtoNerdFont",
        "3270" to "fonts/3270NerdFont",
        "Adwaita Mono" to "fonts/AdwaitaMonoNerdFont",
        "Agave" to "fonts/AgaveNerdFont",
        "Anonymous Pro" to "fonts/AnonymicePro",
        "Arimo" to "fonts/ArimoNerdFont",
        "Atkinson Hyperlegible Mono" to "fonts/AtkynsonMonoNerdFont",
        "Aurulent Sans Mono" to "fonts/AurulentSansMNerdFont",
        "BigBlue Terminal" to "fonts/BigBlueTermNerdFont",
        "Bitstream Vera Sans Mono" to "fonts/BitstromWeraNerdFont",
        "IBM Plex Mono" to "fonts/BlexMonoNerdFont",
        "Cascadia Code" to "fonts/CaskaydiaCoveNerdFont",
        "Cascadia Mono" to "fonts/CaskaydiaMonoNerdFont",
        "Code New Roman" to "fonts/CodeNewRomanNerdFont",
        "ComicShanns Mono" to "fonts/ComicShannsMonoNerdFont",
        "Commit Mono" to "fonts/CommitMonoNerdFont",
        "Cousine" to "fonts/CousineNerdFont",
        "D2Coding" to "fonts/D2CodingLigatureNerdFont",
        "DaddyTime Mono" to "fonts/DaddyTimeMonoNerdFont",
        "DejaVu Sans Mono" to "fonts/DejaVuSansMNerdFont",
        "Departure Mono" to "fonts/DepartureMonoNerdFont",
        "Droid Sans Mono" to "fonts/DroidSansMNerdFont",
        "Envy Code R" to "fonts/EnvyCodeRNerdFont",
        "Fantasque Sans Mono" to "fonts/FantasqueSansMNerdFont",
        "FiraCode Nerd Font" to "fonts/FiraCodeNerdFont",
        "Fira Mono" to "fonts/FiraMonoNerdFont",
        "Geist Mono" to "fonts/GeistMonoNerdFont",
        "Go Mono" to "fonts/GoMonoNerdFont",
        "Gohu" to "fonts/GohuFontNerdFont",
        "Hack Nerd Font" to "fonts/HackNerdFont",
        "Hasklig" to "fonts/HasklugNerdFont",
        "Heavy Data" to "fonts/HeavyDataNerdFont",
        "Hermit" to "fonts/HurmitNerdFont",
        "iA Writer" to "fonts/iMWritingNerdFont",
        "Inconsolata" to "fonts/InconsolataNerdFont",
        "Inconsolata Go" to "fonts/InconsolataGoNerdFont",
        "Inconsolata LGC" to "fonts/InconsolataLGCNerdFont",
        "Intel One Mono" to "fonts/IntoneMonoNerdFont",
        "Iosevka Nerd Font" to "fonts/IosevkaNerdFont",
        "Iosevka Term" to "fonts/IosevkaTermNerdFont",
        "Iosevka Term Slab" to "fonts/IosevkaTermSlabNerdFont",
        "JetBrainsMono Nerd Font" to "fonts/JetBrainsMonoNerdFont",
        "Lekton" to "fonts/LektonNerdFont",
        "Liberation Mono" to "fonts/LiterationMonoNerdFont",
        "Lilex" to "fonts/LilexNerdFont",
        "Martian Mono" to "fonts/MartianMonoNerdFont",
        "Meslo Nerd Font" to "fonts/MesloLGSNerdFont",
        "Monaspace" to "fonts/MonaspiceNerdFont",
        "Monofur" to "fonts/MonofurNerdFont",
        "Monoid" to "fonts/MonoidNerdFont",
        "Mononoki" to "fonts/MononokiNerdFont",
        "M+" to "fonts/MPlusNerdFont",
        "Noto" to "fonts/NotoNerdFont",
        "OpenDyslexic" to "fonts/OpenDyslexicNerdFont",
        "Overpass" to "fonts/OverpassNerdFont",
        "ProFont" to "fonts/ProFontNerdFont",
        "ProggyClean" to "fonts/ProggyCleanNerdFont",
        "Recursive Mono" to "fonts/RecMonoNerdFont",
        "Roboto Mono" to "fonts/RobotoMonoNerdFont",
        "Share Tech Mono" to "fonts/ShureTechMonoNerdFont",
        "Source Code Pro" to "fonts/SauceCodeProNerdFont",
        "Space Mono" to "fonts/SpaceMonoNerdFont",
        "Symbols Only" to "fonts/SymbolsNerdFont",
        "Terminus" to "fonts/TerminessNerdFont",
        "Tinos" to "fonts/TinosNerdFont",
        "Ubuntu" to "fonts/UbuntuNerdFont",
        "Ubuntu Mono" to "fonts/UbuntuMonoNerdFont",
        "Ubuntu Sans" to "fonts/UbuntuSansNerdFont",
        "Victor Mono" to "fonts/VictorMonoNerdFont",
        "Zed Mono" to "fonts/ZedMonoNerdFont"
    ).map { (name, base) -> FontFamilyOption(name, FontSource.NERD_FONT, base) }

    val monospaceFonts = listOf(
        "JetBrains Mono" to "fonts/JetBrainsMono",
        "Fira Code" to "fonts/FiraCode",
        "Cascadia Code" to "fonts/CascadiaCode",
        "Source Code Pro" to "fonts/SourceCodePro",
        "IBM Plex Mono" to "fonts/IBMPlexMono",
        "Ubuntu Mono" to "fonts/UbuntuMono",
        "Roboto Mono" to "fonts/RobotoMono",
        "Inconsolata" to "fonts/Inconsolata"
    ).map { (name, base) -> FontFamilyOption(name, FontSource.MONOSPACE, base) }

    val systemDefault = FontFamilyOption("System Default", FontSource.SYSTEM)

    fun allGrouped(): Map<String, List<FontFamilyOption>> = linkedMapOf(
        "Nerd Font" to nerdFonts,
        "Monospace" to monospaceFonts,
        "System" to listOf(systemDefault)
    )

    /** Resolves a specific style. Falls back to Regular, then to the
     *  system monospace typeface, if the requested style's asset is
     *  missing (e.g. a family that only ships Regular + Bold). */
    fun resolveTypeface(context: Context, option: FontFamilyOption, style: FontStyle): Typeface {
        return when (option.source) {
            FontSource.SYSTEM -> systemStyleFlags(style)
            FontSource.CUSTOM -> {
                val path = option.customFilePaths?.get(style) ?: option.customFilePaths?.get(FontStyle.REGULAR)
                if (path != null) Typeface.createFromFile(File(path)) else Typeface.MONOSPACE
            }
            FontSource.NERD_FONT, FontSource.MONOSPACE -> {
                val primary = option.assetPathFor(style)
                val fallback = option.assetPathFor(FontStyle.REGULAR)
                loadAsset(context, primary) ?: loadAsset(context, fallback) ?: Typeface.MONOSPACE
            }
        }
    }

    private fun loadAsset(context: Context, path: String?): Typeface? {
        if (path == null) return null
        return runCatching { Typeface.createFromAsset(context.assets, path) }.getOrNull()
    }

    private fun systemStyleFlags(style: FontStyle): Typeface = when (style) {
        FontStyle.REGULAR -> Typeface.MONOSPACE
        FontStyle.BOLD -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        FontStyle.ITALIC -> Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC)
        FontStyle.BOLD_ITALIC -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)
    }
}