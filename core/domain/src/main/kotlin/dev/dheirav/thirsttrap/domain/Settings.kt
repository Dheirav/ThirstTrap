package dev.dheirav.thirsttrap.domain

import kotlinx.coroutines.flow.Flow

/**
 * User settings. Deliberately small - this grows with X1, not before.
 */
data class AppSettings(
    /**
     * When the diary was last exported, or null for never. D33 decided no
     * cloud backup, which quietly makes the user the backup system - so the
     * app owes them the one fact that system runs on. Displayed as quiet
     * text, never a badge: the anti-goals apply to guilt about backups too.
     */
    val lastExportAtMillis: Long? = null,
    /** Local hour the daily reminder sweep runs. */
    val reminderHour: Int = 9,
    /** Starting depletion trigger for newly added plants. */
    val defaultDepletionTrigger: Double = DEFAULT_DEPLETION_TRIGGER,
    /**
     * Off by default, and the only setting in the app that can cause a packet
     * to leave the phone. The requirements call offline-first a promise rather
     * than a default, so this asks first and stays asked.
     */
    val onlineSpeciesLookup: Boolean = false,
    /**
     * The can currently being measured into, in millilitres.
     */
    val wateringCanMl: Double = 1000.0,
    /**
     * The cans and bottles this person actually owns, in millilitres.
     *
     * Empty to begin with, and deliberately so. A shipped list of 250/500/1000
     * is a guess about someone else's cupboard, and every wrong guess is a chip
     * they have to read past to reach the one they use. These are typed once
     * and then they are the presets.
     */
    val wateringCanSizesMl: List<Double> = emptyList(),
    /**
     * Whether the first-run page has been read. False means show it.
     *
     * A flag with one reader and one writer, which is worth saying out loud in
     * this codebase: D30a found a setting that was written, shown back, and
     * never read by anything.
     */
    val introSeen: Boolean = false,
    /**
     * Reveals the specialist corners: pot stickers and the scanner, experiments,
     * and logging room conditions by hand.
     *
     * Off by default. None of them is useless, and each is noise for somebody
     * with four plants: stickers pay off at thirty pots and a printer,
     * experiments assume you want a controlled test on a houseplant, and room
     * conditions is a chore whose payoff is one explanatory sentence.
     * docs/SHARING.md section 3.
     */
    val advancedFeatures: Boolean = false,
    /**
     * Offer a plant's species care notes right after it is added.
     *
     * On by default, because the moment you have just typed a species name is
     * the moment the notes are worth reading, and nobody goes looking in a
     * menu for something they do not know is there. Switchable because it is
     * an interruption, and the second time you add a pothos you know.
     */
    val offerCareOnAdd: Boolean = true,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setReminderHour(hour: Int)
    suspend fun setDefaultDepletionTrigger(fraction: Double)
    suspend fun setOnlineSpeciesLookup(enabled: Boolean)
    suspend fun setWateringCanMl(ml: Double)
    suspend fun setWateringCanSizes(sizesMl: List<Double>)
    suspend fun setIntroSeen(seen: Boolean)
    suspend fun setAdvancedFeatures(enabled: Boolean)
    suspend fun setOfferCareOnAdd(enabled: Boolean)

    /** Called by the export flow on success; nothing else writes it. */
    suspend fun markExported(atMillis: Long)

    /** Dismissals are per drying cycle, so a new cycle can speak up again. */
    suspend fun dismissDiagnostic(key: String)
    suspend fun dismissedDiagnostics(): Set<String>
}
