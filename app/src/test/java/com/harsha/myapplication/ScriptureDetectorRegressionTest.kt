package com.harsha.myapplication

import com.harsha.myapplication.scripture.ScriptureDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the scripture detector (pure Kotlin, runs on the host JVM).
 */
class ScriptureDetectorRegressionTest {

    /**
     * Used to take minutes: a long run of digits after "and" / "&" could be split into digit
     * groups in exponentially many ways, which froze the note editor (ANR) on phones.
     * The timeout makes the test fail instead of hanging if that ever comes back.
     */
    @Test(timeout = 5_000)
    fun longDigitRunAfterAnd_doesNotFreeze() {
        val digits = "1234567891".repeat(5)
        val refs = ScriptureDetector.extractReferences("John 3:16 and ${digits}x")
        assertTrue(refs.isNotEmpty())
        assertEquals("John", refs.first().book)
        assertEquals(16, refs.first().verse)
    }

    @Test(timeout = 5_000)
    fun longDigitRunAfterAmpersand_doesNotFreeze() {
        val digits = "9876543210".repeat(4)
        val refs = ScriptureDetector.extractReferences("Romans 5:8 & $digits and more text")
        assertTrue(refs.isNotEmpty())
        assertEquals("Romans", refs.first().book)
    }

    @Test
    fun andFollowedByAWord_isNotReadAsAnotherVerse() {
        val ref = ScriptureDetector.extractReferences("John 3:16 and 17 people came").first()
        assertEquals(16, ref.verse)
        assertTrue(ref.more.isEmpty())
    }

    @Test
    fun andFollowedByANumber_isStillAnotherVerse() {
        val ref = ScriptureDetector.extractReferences("John 3:16 and 17").first()
        assertEquals(1, ref.more.size)
        assertEquals(17, ref.more.first().verse)
    }

    @Test
    fun listOfVerses_isStillRecognised() {
        val ref = ScriptureDetector.extractReferences("Read John 3:16, 17, 18 today").first()
        assertEquals(2, ref.more.size)
    }
}
