package com.lumera.app.data.parental

import org.junit.Assert.*
import org.junit.Test

class AgeGroupPolicyTest {
    @Test fun childAndYoungTeenCannotAccess15Or18() {
        for (group in listOf("under12", "12to14")) for (rating in listOf("15", "18", "R18")) assertFalse(AgeGroupPolicy.allows(group, listOf(rating)))
    }
    @Test fun groupsHaveDifferentLimits() {
        assertTrue(AgeGroupPolicy.allows("under12", listOf("U", "PG")))
        assertFalse(AgeGroupPolicy.allows("under12", listOf("12")))
        assertTrue(AgeGroupPolicy.allows("12to14", listOf("12", "12A")))
        assertTrue(AgeGroupPolicy.allows("15to17", listOf("15")))
        assertFalse(AgeGroupPolicy.allows("15to17", listOf("18")))
    }
    @Test fun unknownPoliciesAndUnratedContentFailClosed() {
        for (group in listOf(null, "", "unexpected", "under12", "12to14", "15to17")) {
            assertFalse(AgeGroupPolicy.allows(group, emptyList()))
            assertFalse(AgeGroupPolicy.allows(group, listOf("NR")))
            assertFalse(AgeGroupPolicy.allows(group, listOf("TV-MA")))
            assertFalse(AgeGroupPolicy.allows(group, listOf("8.5")))
        }
    }
    @Test fun mostRestrictiveUkCertificationWins() {
        assertFalse(AgeGroupPolicy.allows("12to14", listOf("12", "15")))
        assertFalse(AgeGroupPolicy.allows("12to14", listOf("PG", "unknown")))
    }
    @Test fun adultRetainsAccessToUnratedAndAllAges() {
        assertTrue(AgeGroupPolicy.allows("adult", emptyList()))
        assertTrue(AgeGroupPolicy.allows("adult", listOf("18")))
    }
}
