package com.lumera.app.data.parental

import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.model.tmdb.*
import com.lumera.app.data.profile.ProfileRuntimeCoordinator
import com.lumera.app.data.remote.TmdbApiService
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ParentalAccessTest {
    private val dao = mockk<AddonDao>()
    private val api = mockk<TmdbApiService>()
    private val pin = mockk<ParentPin>()
    private val coordinator = ProfileRuntimeCoordinator()
    private lateinit var access: ParentalAccess

    @Before fun setup() {
        every { pin.ratingApiKey } returns "test-key"
        every { pin.ratingKeyVersion } returns 0
        coEvery { dao.getProfileById(1) } returns ProfileEntity(id = 1, name = "Child", ageGroup = "12to14")
        access = ParentalAccess(dao, coordinator, api, pin)
    }
    private suspend fun activate() { coordinator.exclusive { coordinator.activate(1) } }
    private fun movieRatings(vararg ratings: Pair<String, String>) {
        coEvery { api.getMovieReleaseDates(10, any()) } returns Response.success(TmdbMovieReleaseDatesResponse(
            ratings.map { TmdbMovieReleaseDateCountry(it.first, listOf(TmdbMovieReleaseDateItem(it.second))) }))
    }
    @Test fun uk15IsBlockedEvenIfAnotherCountryIsLower() = runTest {
        activate(); movieRatings("US" to "PG", "GB" to "15")
        assertFalse(access.allowed("movie", "tmdb:10"))
    }
    @Test fun uk12IsAllowedWithoutUsingUsClassification() = runTest {
        activate(); movieRatings("US" to "R", "GB" to "12")
        assertTrue(access.allowed("movie", "tmdb:10"))
    }
    @Test fun missingUkRatingAndMissingKeyAreBlocked() = runTest {
        activate(); movieRatings("US" to "PG")
        assertFalse(access.allowed("movie", "tmdb:10"))
        every { pin.ratingApiKey } returns ""
        assertFalse(access.allowed("movie", "tmdb:11"))
    }
    @Test fun lateRatingCannotAuthorizeAnotherSession() = runTest {
        activate()
        val response = CompletableDeferred<Response<TmdbMovieReleaseDatesResponse>>()
        coEvery { api.getMovieReleaseDates(10, any()) } coAnswers { response.await() }
        val result = async { access.allowed("movie", "tmdb:10") }
        runCurrent()
        coordinator.exclusive { coordinator.deactivate(); coordinator.activate(1) }
        response.complete(Response.success(TmdbMovieReleaseDatesResponse(listOf(TmdbMovieReleaseDateCountry("GB", listOf(TmdbMovieReleaseDateItem("12")))))))
        assertFalse(result.await())
    }
    @Test fun cachedRatingStillUsesCurrentAgeGroup() = runTest {
        activate(); movieRatings("GB" to "12")
        assertTrue(access.allowed("movie", "tmdb:10"))
        coEvery { dao.getProfileById(1) } returns ProfileEntity(id = 1, name = "Child", ageGroup = "under12")
        assertFalse(access.allowed("movie", "tmdb:10"))
    }
    @Test fun noActiveSessionNeverGetsAccess() = runTest { assertFalse(access.allowed("movie", "tmdb:10")) }
    @Test fun ratingOutageFailsClosed() = runTest {
        activate(); coEvery { api.getMovieReleaseDates(10, any()) } throws java.io.IOException("offline")
        assertFalse(access.allowed("movie", "tmdb:10"))
    }
    @Test fun episodeUsesItsSeriesUkRating() = runTest {
        activate()
        coEvery { api.findByExternalId("tt12345", any(), any()) } returns Response.success(TmdbFindResponse(tvResults = listOf(TmdbFindResult(10))))
        coEvery { api.getTvContentRatings(10, any()) } returns Response.success(TmdbTvContentRatingsResponse(listOf(TmdbTvContentRatingItem("GB", "15"))))
        assertFalse(access.allowed("series", "tt12345:2:3"))
        coVerify { api.findByExternalId("tt12345", any(), "imdb_id") }
        coVerify { api.getTvContentRatings(10, any()) }
    }
    @Test fun ambiguousAddonNumericIdsCannotUseUnrelatedTmdbRatings() = runTest {
        activate(); movieRatings("GB" to "U")
        assertFalse(access.allowed("movie", "10"))
        coVerify(exactly = 0) { api.getMovieReleaseDates(any(), any()) }
    }
    @Test fun adultsKeepUnratedAndCustomAddonContent() = runTest {
        activate()
        coEvery { dao.getProfileById(1) } returns ProfileEntity(id = 1, name = "Adult", ageGroup = "adult")
        assertTrue(access.allowed("other", "custom-addon-id"))
        coVerify(exactly = 0) { api.getMovieReleaseDates(any(), any()) }
    }
}
