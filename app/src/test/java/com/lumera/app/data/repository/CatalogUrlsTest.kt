package com.lumera.app.data.repository

import org.junit.Assert.*
import org.junit.Test

class CatalogUrlsTest {
    private val base = "https://example.invalid/addon"
    @Test fun oldRowsKeepTheirOriginalUrl() {
        assertEquals("$base/catalog/movie/top.json", CatalogUrls.catalog(base, "movie", "top"))
    }
    @Test fun genreNamesAreEncodedAsOneExtraValue() {
        assertEquals("$base/catalog/movie/top/genre=Sci-Fi+%26+Fantasy.json", CatalogUrls.catalog(base, "movie", "top", "Sci-Fi & Fantasy"))
    }
    @Test fun paginationPreservesTheGenre() {
        val first = CatalogUrls.catalog(base, "series", "top", "Crime")
        assertEquals("$base/catalog/series/top/genre=Crime&skip=50.json", CatalogUrls.page(first, 50))
    }
    @Test fun ordinaryPaginationStillWorks() {
        assertEquals("$base/catalog/movie/top/skip=50.json", CatalogUrls.page(CatalogUrls.catalog(base, "movie", "top"), 50))
    }
    @Test fun replacingPaginationNeverDuplicatesSkip() {
        val page = "$base/catalog/movie/top/genre=Action&skip=50.json"
        assertEquals("$base/catalog/movie/top/genre=Action&skip=100.json", CatalogUrls.page(page, 100))
    }
    @Test fun zeroOffsetPreservesTheOriginalFilter() {
        val first = CatalogUrls.catalog(base, "movie", "top", "Action")
        assertEquals(first, CatalogUrls.page(first, 0))
    }
    @Test fun unicodeAndTrailingTransportSlashesAreSupported() {
        val first = CatalogUrls.catalog("$base/", "movie", "top", "Comédie")
        assertEquals("$base/catalog/movie/top/genre=Com%C3%A9die.json", first)
        assertEquals("$base/catalog/movie/top/genre=Com%C3%A9die&skip=100.json", CatalogUrls.page(first, 100))
    }
}
