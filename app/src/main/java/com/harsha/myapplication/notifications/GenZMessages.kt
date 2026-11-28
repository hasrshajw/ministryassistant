package com.harsha.myapplication.notifications

import android.content.Context
import com.harsha.myapplication.data.Gender
import com.harsha.myapplication.data.GenZPrefsRepository

/**
 * Gen-Z notification copy pools (Telugu + English slang + emojis).
 * Used by [ReminderNotifier] when Gen-Z mode is enabled.
 *
 * Messages rotate via a shuffled pool stored in SharedPreferences so the same
 * line is not repeated until the pool is exhausted.
 */
object GenZMessages {

    private const val POOL_PREFS = "GenZMessagePools"

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Builds title + body for a visit / study reminder.
     * @param isExact true when the notification fires at the exact visit time
     *                (vs. an "upcoming in X minutes" style reminder).
     */
    fun visitReminder(
        context: Context,
        contactName: String,
        isStudy: Boolean,
        isExact: Boolean = true,
    ): Pair<String, String> {
        GenZPrefsRepository.init(context)
        val prefs = GenZPrefsRepository.prefs
        if (!prefs.enabled) {
            // Caller should not reach here, but keep a safe fallback.
            val type = if (isStudy) "study" else "return visit"
            return if (isExact) {
                (if (isStudy) "Study" else "Return Visit") to "Your $type with $contactName is now."
            } else {
                "Upcoming ${if (isStudy) "Study" else "Return Visit"}" to
                    "Your $type with $contactName is coming up."
            }
        }

        val nickname = prefs.name.ifBlank {
            if (prefs.gender == Gender.Female) "Heyy" else "Bro"
        }
        val genderKey = when (prefs.gender) {
            Gender.Female -> "Female"
            else -> "Male" // Male + Other use the male pool
        }

        val title = when {
            isExact && isStudy -> "It's time to Study"
            isExact -> "It's time to Return Visit"
            isStudy -> "Upcoming Study"
            else -> "Upcoming Return Visit"
        }

        val pool = if (isExact) {
            if (genderKey == "Female") femaleExact else maleExact
        } else {
            if (genderKey == "Female") femaleBefore else maleBefore
        }
        val poolId = "${genderKey}_${if (isExact) "exact" else "before"}"
        var body = pick(context, poolId, pool)
            .replace("{name}", contactName)
            .replace("John", contactName)
            .replace("{nickname/name}", nickname)
            .replace("{nickname}", nickname)

        if (isStudy) {
            body = body
                .replace("return visit", "study", ignoreCase = true)
                .replace("visit", "study", ignoreCase = true)
        }
        return title to body
    }

    // ── Pool rotation ───────────────────────────────────────────────────────

    private fun pick(context: Context, poolId: String, messages: Array<String>): String {
        if (messages.isEmpty()) return ""
        val sp = context.applicationContext.getSharedPreferences(POOL_PREFS, Context.MODE_PRIVATE)
        val key = "pool_$poolId"
        var pool = sp.getString(key, null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toMutableList()
            ?: mutableListOf()

        if (pool.isEmpty()) {
            pool = (messages.indices).toMutableList()
            pool.shuffle()
        }
        val index = pool.removeAt(0)
        sp.edit().putString(key, pool.joinToString(",")).apply()
        return messages.getOrElse(index) { messages.first() }
    }

    // ── Message pools (ported from the Flutter NotificationHelper) ──────────

    private val maleBefore = arrayOf(
        "Anna 👀 {name} tho return visit undhi 30 mins lo!",
        "Bro 😂 {name} tho return visit undhi, gurthundha?",
        "Guru ⏰ {name} tho visit time daggarlo undhi!",
        "Bossu 👀 Ready ah? 30 mins lo {name} ni kalavali!",
        "Anna garu 😭 {name} ni kalavalsina time vachesthundi!",
        "Bro 👀 {name} tho return visit miss avvakunda chusko!",
        "Guru 🔔 30 mins lo {name} tho return visit undhi!",
        "Anna 👀 {name} tho visit undhi… marchipoledu kada?",
        "Brooo 😭 {name} wait chesthunnadu… return visit undhi!",
        "Bossu 👀 {name} tho return visit undhi, ready avvu!",
        "Anna garu ⏰ {name} tho meeting undhi… time ki vellali mari!",
        "Guru 😌 {name} tho return visit… clean ga ready avvu!",
        "Bro 😭 {name} tho return visit miss cheyyaku!",
        "Bossu 👀 30 mins only… {name} tho return visit gurthundha?",
        "Anna ⏰ {name} tho return visit countdown start!",
        "Guru 😏 {name} tho return visit ki time ayyipothundi!",
        "Bro 👀 {name} tho return visit pending undhi!",
        "Anna garu ❤️ {name} tho return visit undhi… time ki reach avvu!",
        "{nickname} 👀 {name} tho return visit undhi 30 mins lo!",
        "{nickname} 😂 {name} tho visit undhi… gurthundha?",
        "{nickname} ⏰ Ready avvu! {name} tho visit time daggarlo undhi.",
        "{nickname} 👀 30 mins lo {name} ni kalavali guru!",
        "{nickname} 😭 {name} wait chesthunnadu… late avvaku!",
        "{nickname} 🔔 Return visit time almost! {name} tho undhi.",
        "{nickname} 😌 {name} tho return visit… ready ah?",
        "{nickname} 👀 Bossu, {name} tho return visit marchipoku!",
        "{nickname} ⏰ Countdown start! {name} tho return visit undhi.",
        "{nickname} ❤️ {name} tho return visit undhi… time ki reach avvu!",
    )

    private val maleExact = arrayOf(
        "Bro 👀 return visit time ayyindiga… start chesava? 😂",
        "Brooo ⏰ {name} daggare unnavuga? 👀",
        "Bro 😂 time ayyindi… {name} tho start chesava?",
        "Bossu 👀 return visit time! {name} ni kalisesava?",
        "Brooo 😭 inka ekkada unnav? {name} waiting emo 😂",
        "Guru ⏰ {name} tho return visit start ayyinda?",
        "Bro 👀 {name} daggara check-in ayyava?",
        "Bossu 😂 time ayyindi… {name} tho start cheyyi!",
        "Brooo 🔔 return visit time ayyindi… ready aa?",
        "Anna 👀 {name} tho conversation start chesava?",
        "Bro 😎 mission time! {name} tho return visit start cheddama?",
        "Anna garu ⏰ {name} tho appointment time ayyindi!",
        "Bro 👀 {name} tho return visit undi kada… start chesava?",
        "Brooo 😭 return visit time ayyindi… {name} daggare unnavuga?",
        "Bossu 😂 {name} tho meeting time ayyindi!",
        "Bro 🔥 return visit mode ON chesava? {name} ready!",
        "Guru 👀 {name} ni kalavalsina time vachchesindi!",
        "Bro ⏰ time ayyindi… {name} tho return visit ki vellava?",
        "Bossu 😂 Mission {name} starts now! Return visit time ayyindi!",
        "{nickname} 👀 return visit time ayyindi… start chesava?",
        "{nickname} ⏰ time ayyindi bro! Return visit ki ready aa?",
        "{nickname} 👀 {name} waiting emo… return visit time ayyindi!",
        "{nickname} 😂 Time ayyindi guru… {name} tho start chesava?",
        "{nickname} 🔔 Return visit time! {name} ni kalisesava?",
        "{nickname} 😎 Mission time! {name} tho start cheddama?",
        "{nickname} 👀 {name} daggara check-in ayyava?",
        "{nickname} 😭 Inka ekkada unnav? {name} waiting!",
        "{nickname} 🔥 Return visit mode ON chesava?",
        "{nickname} ⏰ {name} tho appointment time ayyindi!",
        "{nickname} 👀 {name} tho conversation start chesava?",
    )

    private val femaleBefore = arrayOf(
        "Bangaram 🥹 {name} tho return visit undhi, gurthundha?",
        "Heyy 👀 {name} tho return visit undhi 30 mins lo!",
        "Ammayi 👀 {name} tho visit time daggarlo undhi!",
        "Heyy 🫶 30 mins lo return visit undhi, marchipoku sarena?",
        "Bestie 👀 {name} tho return visit in 30 mins!",
        "Chinnari 😌 Return visit time aipothundi… ready?",
        "Heyy 👀 {name} tho return visit undhi, miss avvakunda chusko!",
        "Bangaram 😭 {name} tho return visit undhi… inka ready avvaleda?",
        "Heyy cutie 👀 {name} tho return visit undhi!",
        "Chinnari 🫶 {name} tho visit undhi… gurthundhi kada?",
        "Heyy 😌 {name} tho return visit ki ready avvu!",
        "Bangaram 👀 30 mins only… {name} tho return visit undhi!",
        "Bestie 😂 {name} tho return visit marchipoku please!",
        "Heyy 🥹 {name} tho return visit waiting undhi!",
        "Ammayi 👀 Countdown start… 30 mins lo {name} tho visit!",
        "Bangaram ❤️ {name} tho return visit undhi… time ki vellali!",
        "Heyy 🫶 {name} tho return visit undhi… ready kada?",
        "Chinnitalli 🥹 {name} tho return visit undhi… ready ayyava?",
        "{nickname} 👀 {name} tho return visit undhi 30 mins lo!",
        "{nickname} 🥹 {name} tho visit undhi… gurthundha?",
        "{nickname} 🫶 Ready ah? {name} tho return visit undhi!",
        "{nickname} 😌 30 mins lo {name} ni kalavali!",
        "{nickname} 👀 {name} wait chesthundi… late avvaku!",
        "{nickname} 😭 Inka ready avvaleda? {name} tho return visit undhi!",
        "{nickname} ❤️ {name} tho return visit undhi… time ki vellali!",
        "{nickname} 😂 {name} tho return visit marchipoku please!",
        "{nickname} ⏰ Countdown start! {name} tho return visit undhi!",
        "{nickname} 🥹 {name} tho return visit time daggarlo undhi!",
        "{nickname} 👀 30 mins only… ready avvu! {name} tho visit undhi.",
        "{nickname} 🫶 {name} tho return visit… ready kada?",
    )

    private val femaleExact = arrayOf(
        "Bangaram 👀 return visit time ayyindiga… start chesava?",
        "Heyy 🥹 {name} daggare unnavuga? Return visit time ayyindi!",
        "Chinnari ⏰ {name} tho return visit start chesava?",
        "Ammayi 👀 time ayyindi… {name} ni kalisesava?",
        "Heyy bestie 😂 return visit time! Ready aa?",
        "Bangaram 🫶 {name} tho visit time ayyindi… start cheyyi!",
        "Bestie 👀 {name} waiting emo… return visit time ayyindi!",
        "Chinnitalli 😭 inka ekkada unnav? {name} daggara undalsina time idi!",
        "Heyy ⏰ {name} tho return visit time ayyindi!",
        "Bangaram 👀 Mission time! {name} tho start chesava?",
        "Chinnari 🥹 {name} ni kalavalsina time vachchesindi!",
        "Heyy bestie 👀 {name} daggara check-in ayyava?",
        "Bangaram 😌 Return visit mode ON chesava?",
        "Chinnatalli 🫶 {name} tho conversation start ayyinda?",
        "Heyy 😭 return visit clock kottindi… {name} daggare unnavuga?",
        "Chinnitalli 👀 time ayyindi… {name} tho visit start cheddama?",
        "Bangaram ⏰ {name} tho appointment time ayyindi!",
        "Ammayi 😂 return visit time vachchesindi… late avvaku!",
        "Heyy 👀 {name} tho return visit starts now! Ready aa?",
        "{nickname} 👀 return visit time ayyindi… start chesava?",
        "{nickname} 🥹 Time ayyindi! {name} tho return visit undhi!",
        "{nickname} 🫶 {name} waiting emo… return visit time ayyindi!",
        "{nickname} 👀 Time ayyindi… {name} ni kalisesava?",
        "{nickname} ⏰ {name} tho return visit start cheyyi!",
        "{nickname} 😌 Return visit mode ON chesava?",
        "{nickname} 😂 Bestie, {name} tho visit time ayyindi!",
        "{nickname} 😭 Inka ekkada unnav? {name} waiting!",
        "{nickname} 👀 {name} daggara check-in ayyava?",
        "{nickname} 🥹 {name} ni kalavalsina time vachchesindi!",
        "{nickname} 🔔 Return visit starts now! {name} tho ready aa?",
    )
}
