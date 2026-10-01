package dev.dheirav.thirsttrap.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val DUE = "due"
    const val HELP = "help"
    const val INTRO = "intro"
    const val WAYFINDING = "help/wayfinding"
    const val HELP_REMINDERS = "help/reminders"
    const val DEBUG = "debug"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
    const val LOG_EVENT = "plant/log"
    const val WEIGHT = "plant/weight"
    const val SCALE_HELP = "help/scale"
    const val LIGHT = "plant/light"
    const val PLACE_LIGHT = "place/light"
    const val PROPAGATION = "propagation"
    const val POST_MORTEM = "plant/postmortem"
    const val STICKER = "plant/sticker"
    const val DIAGNOSE = "plant/diagnose"
    const val AMBIENT = "ambient"
    const val STATS = "stats"
    const val PLACES = "places"
    const val FERTILIZER = "fertilizer"
    const val EXPERIMENTS = "experiments"
    const val SCAN = "scan"
    const val EXPERIMENT = "experiment"
    const val WEIGHING = "weighing"
    const val CARE = "plant/care"
    const val PLANT_EDIT = "plant/edit"
    const val PLANT_DETAIL = "plant/detail"
    /** Empty id means "new plant". */
    fun plantDetail(plantId: String) = "$PLANT_DETAIL/$plantId"

    fun logEvent(plantId: String) = "$LOG_EVENT/$plantId"


    fun weight(plantId: String) = "$WEIGHT/$plantId"


    fun light(plantId: String) = "$LIGHT/$plantId"

    /** Place names are free text, so they have to survive being a path segment. */
    fun placeLight(place: String) = "$PLACE_LIGHT/${android.net.Uri.encode(place)}"

    fun postMortem(plantId: String) = "$POST_MORTEM/$plantId"

    fun sticker(plantId: String) = "$STICKER/$plantId"

    fun care(plantId: String) = "$CARE/$plantId"
    fun diagnose(plantId: String) = "$DIAGNOSE/$plantId"
    fun experiment(id: String) = "$EXPERIMENT/$id"

    fun plantEdit(plantId: String? = null) =
        if (plantId == null) "$PLANT_EDIT?id=" else "$PLANT_EDIT?id=$plantId"
}
