package com.lumera.app.data.repository

import java.net.URLEncoder

/** Stremio catalog extras share one path component, including when paginating. */
object CatalogUrls {
    fun catalog(transport: String, type: String, id: String, genre: String? = null): String {
        val base = "${transport.trimEnd('/')}/catalog/$type/$id"
        return if (genre.isNullOrBlank()) "$base.json"
        else "$base/genre=${URLEncoder.encode(genre, "UTF-8")}.json"
    }

    fun page(baseUrl: String, skip: Int): String {
        if (skip <= 0) return baseUrl
        require(baseUrl.endsWith(".json"))
        val stem = baseUrl.removeSuffix(".json")
        val path = stem.substringAfterLast("/catalog/")
        if (path.split('/').size < 3) return "$stem/skip=$skip.json"
        val parent = stem.substringBeforeLast('/')
        val extras = stem.substringAfterLast('/').split('&').filterNot { it.startsWith("skip=") }
        return "$parent/${(extras + "skip=$skip").joinToString("&")}.json"
    }
}
