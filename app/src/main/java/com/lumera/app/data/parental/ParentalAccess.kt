package com.lumera.app.data.parental

import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.stremio.MetaItem
import com.lumera.app.data.profile.ProfileRuntimeCoordinator
import com.lumera.app.data.remote.TmdbApiService
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentalAccess @Inject constructor(
    private val dao: AddonDao,
    private val coordinator: ProfileRuntimeCoordinator,
    private val api: TmdbApiService,
    private val parentPin: ParentPin
) {
    private data class Rating(val values: List<String>, val expires: Long)
    private val cache = ConcurrentHashMap<String, Rating>()
    private val permits = Semaphore(6)

    suspend fun isRestricted(): Boolean {
        val id = coordinator.session?.profileId ?: return true
        return AgeGroupPolicy.restricted(dao.getProfileById(id)?.ageGroup)
    }

    suspend fun allowed(type: String, contentId: String): Boolean {
        val owner = coordinator.session ?: return false
        val group = dao.getProfileById(owner.profileId)?.ageGroup
        if (!AgeGroupPolicy.restricted(group)) return coordinator.session == owner
        if (type !in listOf("movie", "series", "tv")) return false
        val id = contentId.removePrefix("trailer_")
        val ratings = ratings(type, id)
        // Re-read the policy so a changed profile/session can never inherit an earlier permission.
        return coordinator.session == owner && AgeGroupPolicy.allows(dao.getProfileById(owner.profileId)?.ageGroup, ratings)
    }

    private suspend fun ratings(type: String, id: String): List<String> {
        val key = "${parentPin.ratingKeyVersion}:$type:$id"
        val now = System.currentTimeMillis()
        cache[key]?.takeIf { it.expires > now }?.let { return it.values }
        if (parentPin.ratingApiKey.isBlank()) return emptyList()
        return permits.withPermit {
            try {
                val result = withTimeout(8_000L) {
                    val clean = id.removePrefix("tmdb:").removePrefix("movie:").removePrefix("series:").substringBefore(':').substringBefore('/')
                    val numeric = if (clean.matches(Regex("tt[0-9]+"))) {
                        val found = api.findByExternalId(clean, parentPin.ratingApiKey).body()
                        (if (type == "movie") found?.movieResults?.firstOrNull() else found?.tvResults?.firstOrNull())?.id
                    } else if (id.startsWith("tmdb:")) clean.toIntOrNull() else null
                    if (numeric == null) return@withTimeout emptyList<String>()
                    if (type == "movie") api.getMovieReleaseDates(numeric, parentPin.ratingApiKey).body()?.results.orEmpty()
                        .filter { it.iso31661.equals("GB", true) }.flatMap { it.releaseDates.orEmpty() }.mapNotNull { it.certification?.takeIf(String::isNotBlank) }
                    else api.getTvContentRatings(numeric, parentPin.ratingApiKey).body()?.results.orEmpty()
                        .filter { it.iso31661.equals("GB", true) }.mapNotNull { it.rating?.takeIf(String::isNotBlank) }
                }
                cache[key] = Rating(result, now + if (result.isEmpty()) 60_000L else 3_600_000L)
                result
            } catch (_: TimeoutCancellationException) { emptyList() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { emptyList() }
        }
    }

    suspend fun filter(items: List<MetaItem>): List<MetaItem> = coroutineScope {
        val owner = coordinator.session ?: return@coroutineScope emptyList()
        val results = items.map { item -> async { item.takeIf { allowed(it.type, it.id) } } }.awaitAll().filterNotNull()
        if (coordinator.session == owner) results else emptyList()
    }
}
