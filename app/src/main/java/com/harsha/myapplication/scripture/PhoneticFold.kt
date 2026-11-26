package com.harsha.myapplication.scripture

/**
 * Sound folding for romanized Telugu (and English) book names.
 *
 * Two spellings that sound alike fold to the same string:
 *   ee/i  oo/u  aa/a  th/t  dh/d  ph/f  kh/k  gh/g  bh/b  sh/s  ck/k
 *   k/q/c -> k     j/z -> j     w -> v     x -> ks
 *   m/n before a consonant -> n   (prasamgi == prasangi)
 *   doubled letters collapse      (aadhikaandamu == adikandamu)
 *
 * [skeleton] additionally drops the vowels, so "krtika" and "keerthanalu" both start with "krt".
 *
 * Letters of other scripts (Telugu, Hindi, ...) are kept as they are (lower-cased), so imported
 * Bible names still take part in typo-tolerant matching.
 */
internal object PhoneticFold {

    private val DIGRAPHS = arrayOf(
        "aa" to "a", "ee" to "i", "ii" to "i", "oo" to "u", "uu" to "u",
        "th" to "t", "dh" to "d", "ph" to "f", "kh" to "k", "gh" to "g",
        "bh" to "b", "sh" to "s", "ck" to "k"
    )
    private const val CONSONANTS = "bcdfghjklmnpqrstvwxyz"
    private const val VOWELS = "aeiou"

    /** Letters and combining marks (Telugu vowel signs are marks, not letters). */
    private fun isWordChar(c: Char): Boolean {
        if (c.isLetter()) return true
        return when (Character.getType(c).toByte()) {
            Character.NON_SPACING_MARK, Character.COMBINING_SPACING_MARK, Character.ENCLOSING_MARK -> true
            else -> false
        }
    }

    fun fold(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) if (isWordChar(ch)) sb.append(ch.lowercaseChar())
        var s = sb.toString()
        for ((from, to) in DIGRAPHS) s = s.replace(from, to)

        sb.setLength(0)
        for (ch in s) {
            when (ch) {
                'q', 'c' -> sb.append('k')
                'z' -> sb.append('j')
                'w' -> sb.append('v')
                'x' -> sb.append("ks")
                else -> sb.append(ch)
            }
        }
        s = sb.toString()

        sb.setLength(0)
        for (i in s.indices) {
            var c = s[i]
            if ((c == 'm' || c == 'n') && i + 1 < s.length && s[i + 1] in CONSONANTS) c = 'n'
            if (sb.isNotEmpty() && sb[sb.length - 1] == c) continue
            sb.append(c)
        }
        return sb.toString()
    }

    /** Consonants only, taken from an already folded string. */
    fun skeletonOfFolded(folded: String): String {
        val sb = StringBuilder(folded.length)
        for (c in folded) if (c !in VOWELS) sb.append(c)
        return sb.toString()
    }

    fun skeleton(raw: String): String = skeletonOfFolded(fold(raw))

    /**
     * Even looser key for heavily garbled spellings: consonant skeleton where the glides v / y
     * (after the first letter) are dropped and a leading j counts as y.
     * "yuvhan", "yohaanu", "johan" all give "yhn".
     */
    fun looseOfFolded(folded: String): String {
        val s = skeletonOfFolded(folded)
        if (s.isEmpty()) return s
        val sb = StringBuilder(s.length)
        sb.append(if (s[0] == 'j') 'y' else s[0])
        for (i in 1 until s.length) if (s[i] != 'v' && s[i] != 'y') sb.append(s[i])
        return sb.toString()
    }
}