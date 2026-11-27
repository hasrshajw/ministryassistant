package com.harsha.myapplication

import com.harsha.myapplication.data.SecurityRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityRepositoryTest {
    @Test fun rejectsRepeatedAndSequentialPins() {
        listOf("0000", "1111", "1234", "4321", "9876").forEach { assertTrue(it, SecurityRepository.isTooSimple(it)) }
    }

    @Test fun acceptsOrdinaryPins() {
        listOf("1357", "2580", "4829", "1212").forEach { assertFalse(it, SecurityRepository.isTooSimple(it)) }
    }

    @Test fun validatesLengthAndDigits() {
        assertTrue(SecurityRepository.isValid("1357"))
        assertFalse(SecurityRepository.isValid("13579"))
        assertFalse(SecurityRepository.isValid("135"))
        assertFalse(SecurityRepository.isValid("135790"))
        assertFalse(SecurityRepository.isValid("13a7"))
    }

    @Test fun normalizesAnswers() {
        assertTrue(SecurityRepository.normalizeAnswer("  Fluffy   The Cat ") == "fluffy the cat")
        assertTrue(SecurityRepository.isValidAnswer("Rex"))
        assertFalse(SecurityRepository.isValidAnswer("  a "))
    }
}
