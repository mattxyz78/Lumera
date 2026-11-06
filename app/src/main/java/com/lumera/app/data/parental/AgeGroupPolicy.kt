package com.lumera.app.data.parental

object AgeGroupPolicy {
    fun restricted(group: String?) = group != "adult"
    fun allows(group: String?, ukRatings: List<String>): Boolean {
        if (!restricted(group)) return true
        if (ukRatings.isEmpty()) return false
        val limit = when (group) { "under12" -> 8; "12to14" -> 12; "15to17" -> 15; else -> -1 }
        return ukRatings.all { rating ->
            val age = when (rating.trim().uppercase(java.util.Locale.ROOT)) { "U" -> 0; "PG" -> 8; "12", "12A" -> 12; "15" -> 15; "18", "R18" -> 18; else -> 100 }
            age <= limit
        }
    }
}
