package dev.dheirav.thirsttrap.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val DUE = "due"
    const val HELP_REMINDERS = "help/reminders"
    const val DEBUG = "debug"
    const val LOG_EVENT = "plant/log"
    const val COMPARE = "plant/compare"
    const val PLANT_EDIT = "plant/edit"
    const val PLANT_DETAIL = "plant/detail"
    /** Empty id means "new plant". */
    fun plantDetail(plantId: String) = "$PLANT_DETAIL/$plantId"

    fun logEvent(plantId: String) = "$LOG_EVENT/$plantId"

    fun compare(plantId: String) = "$COMPARE/$plantId"

    fun plantEdit(plantId: String? = null) =
        if (plantId == null) "$PLANT_EDIT?id=" else "$PLANT_EDIT?id=$plantId"
}
