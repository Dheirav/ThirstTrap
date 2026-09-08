package dev.dheirav.thirsttrap.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.dheirav.thirsttrap.domain.LookupFailure
import dev.dheirav.thirsttrap.domain.LookupResult
import dev.dheirav.thirsttrap.domain.SettingsRepository
import dev.dheirav.thirsttrap.domain.SpeciesLookup
import dev.dheirav.thirsttrap.domain.SpeciesLookupService
import dev.dheirav.thirsttrap.domain.isUsableMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GBIF for the name, Wikipedia for the article. Nothing for the care advice.
 *
 * Deliberately built on [HttpURLConnection] rather than an HTTP library. Two
 * calls to two keyless public APIs do not justify pulling OkHttp and its
 * interceptor stack into an app whose whole argument is that it does not need
 * a network - and a dependency that ships its own connection pool and disk
 * cache is a larger surface to reason about than the thing it would replace.
 *
 * What leaves the phone is the species name the user typed, and nothing else:
 * no plant id, no photo, no log, no device identifier, no analytics.
 */
@Singleton
class GbifSpeciesLookupService @Inject constructor(
    private val context: Context,
    private val settings: SettingsRepository,
) : SpeciesLookupService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun lookUp(query: String): LookupResult = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 3) return@withContext LookupResult.Failed(LookupFailure.NO_MATCH)

        // Consent is checked here, not at the call site, so there is exactly one
        // place that can get it wrong.
        if (!settings.settings.first().onlineSpeciesLookup) {
            return@withContext LookupResult.Failed(LookupFailure.NOT_ENABLED)
        }
        if (!isOnline()) return@withContext LookupResult.Failed(LookupFailure.OFFLINE)

        val match = try {
            get("https://api.gbif.org/v1/species/match" +
                "?kingdom=Plantae&name=${URLEncoder.encode(q, "UTF-8")}")
        } catch (e: IOException) {
            TTLog.w(TTLog.DATA, { "species lookup: GBIF unreachable" }, e)
            return@withContext LookupResult.Failed(LookupFailure.SERVICE_ERROR)
        } ?: return@withContext LookupResult.Failed(LookupFailure.SERVICE_ERROR)

        val obj = runCatching { json.parseToJsonElement(match).jsonObject }.getOrNull()
            ?: return@withContext LookupResult.Failed(LookupFailure.SERVICE_ERROR)

        // Not just "did it match" - "did it match something specific enough to
        // be worth showing". GBIF answers "Flax seeds" with the kingdom.
        val usable = isUsableMatch(
            matchType = obj.str("matchType"),
            rank = obj.str("rank"),
            confidence = obj["confidence"]?.jsonPrimitive?.content?.toIntOrNull(),
        )
        if (!usable) return@withContext LookupResult.Failed(LookupFailure.NO_MATCH)

        // `species` carries the ACCEPTED name when the query was a synonym;
        // `canonicalName` is what was matched. Preferring `species` is the
        // entire reason this call is worth making.
        val canonical = obj.str("canonicalName")
        val accepted = obj.str("species") ?: canonical
            ?: return@withContext LookupResult.Failed(LookupFailure.NO_MATCH)

        val wiki = accepted.let { runCatching { wikipedia(it) }.getOrNull() }

        LookupResult.Found(
            SpeciesLookup(
                query = q,
                acceptedName = accepted,
                family = obj.str("family"),
                synonymOf = canonical?.takeIf {
                    obj.str("status") == "SYNONYM" && !it.equals(accepted, ignoreCase = true)
                },
                wikipediaTitle = wiki?.str("title"),
                wikipediaUrl = wiki?.get("content_urls")?.jsonObject
                    ?.get("desktop")?.jsonObject?.str("page"),
                wikipediaExtract = wiki?.str("extract"),
            ),
        )
    }

    /** Null rather than an exception when the article simply does not exist. */
    private fun wikipedia(title: String): JsonObject? {
        val body = get(
            "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                URLEncoder.encode(title.replace(' ', '_'), "UTF-8"),
        ) ?: return null
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        // Disambiguation pages are worse than nothing - they promise an answer
        // and deliver a list.
        return obj.takeIf { it.str("type") != "disambiguation" }
    }

    private fun get(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            // Wikipedia asks for a descriptive agent; GBIF does not mind one.
            setRequestProperty("User-Agent", "ThirstTrap/1.0 (offline plant diary)")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) null
            else conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun JsonObject.str(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()?.takeIf { it.isNotBlank() }
}
