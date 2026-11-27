package com.harsha.myapplication

import com.harsha.myapplication.data.SecurityRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityRepositoryTest {
    @Test fun rejectsRepeatedAndSequentialPins() {
        listOf("0000", "1111", "1234", "4321", "123456", "9876").forEach { assertTrue(it, SecurityRepository.isTooSimple(it)) }
    }

    @Test fun acceptsOrdinaryPins() {
        listOf("1357", "2580", "482916", "1212").forEach { assertFalse(it, SecurityRepository.isTooSimple(it)) }
    }

    @Test fun validatesLengthAndDigits() {
        assertTrue(SecurityRepository.isValid("1357"))
        assertTrue(SecurityRepository.isValid("135790"))
        assertFalse(SecurityRepository.isValid("135"))
        assertFalse(SecurityRepository.isValid("1357902"))
        assertFalse(SecurityRepository.isValid("13a7"))
    }
}
