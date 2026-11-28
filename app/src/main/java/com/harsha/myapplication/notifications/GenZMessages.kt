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


    /** Morning daily summary when there are no visits scheduled today. */
    fun morningEmpty(context: Context): Pair<String, String> {
        GenZPrefsRepository.init(context)
        val prefs = GenZPrefsRepository.prefs
        if (!prefs.enabled) {
            return pickPair(context, "Normal_morning", normalMorning)
        }
        val genderKey = if (prefs.gender == Gender.Female) "Female" else "Male"
        val pool = if (genderKey == "Female") femaleMorning else maleMorning
        return pickPair(context, "${genderKey}_morning", pool)
    }

    fun afterVisit(context: Context, contactName: String, isStudy: Boolean, delayMinutes: Int): Pair<String, String> {
        GenZPrefsRepository.init(context)
        val prefs = GenZPrefsRepository.prefs
        val title = if (isStudy) "Update Study Details" else "Update Return Visit Details"
        if (!prefs.enabled) {
            return title to "Add the details from your ${if (isStudy) "study" else "return visit"}."
        }
        val nickname = prefs.name.ifBlank { if (prefs.gender == Gender.Female) "Heyy" else "Bro" }
        val genderKey = if (prefs.gender == Gender.Female) "Female" else "Male"
        val pool = if (genderKey == "Female") femaleAfter else maleAfter
        val timeText = if (delayMinutes % 60 == 0) {
            val h = delayMinutes / 60
            if (h == 1) "1 hour" else "$h hours"
        } else "$delayMinutes mins"
        var body = pick(context, "${genderKey}_after_visit", pool)
            .replace("{name}", contactName)
            .replace("John", contactName)
            .replace("{nickname}", nickname)
            .replace("{nickname/name}", nickname)
            .replace("1 hours", timeText)
            .replace("2 hours", timeText)
            .replace("1 hour", timeText)
        if (isStudy) {
            body = body.replace("return visit", "study", ignoreCase = true)
                .replace("visit", "study", ignoreCase = true)
        }
        return title to body
    }

    fun overdue(context: Context, contactName: String, isStudy: Boolean, overdueMinutes: Int): Pair<String, String> {
        GenZPrefsRepository.init(context)
        val prefs = GenZPrefsRepository.prefs
        val title = "Follow-Up Pending"
        if (!prefs.enabled || contactName.isBlank()) {
            val type = if (isStudy) "study" else "return visit"
            return title to if (contactName.isNotBlank()) "Follow up with $contactName for your $type."
            else "You have a return visit that still needs an update."
        }
        val nickname = prefs.name.ifBlank { if (prefs.gender == Gender.Female) "Heyy" else "Bro" }
        val genderKey = if (prefs.gender == Gender.Female) "Female" else "Male"
        val pool = if (genderKey == "Female") femaleOverdue else maleOverdue
        val timeText = if (overdueMinutes % 60 == 0) {
            val h = overdueMinutes / 60
            if (h == 1) "1 hour" else "$h hours"
        } else "$overdueMinutes mins"
        var body = pick(context, "${genderKey}_overdue", pool)
            .replace("{name}", contactName)
            .replace("John", contactName)
            .replace("{nickname}", nickname)
            .replace("{nickname/name}", nickname)
            .replace("1 hours", timeText)
            .replace("2 hours", timeText)
            .replace("1 hour", timeText)
        if (isStudy) {
            body = body.replace("return visit", "study", ignoreCase = true)
                .replace("visit", "study", ignoreCase = true)
        }
        return title to body
    }

    private fun pickPair(context: Context, poolId: String, messages: Array<Pair<String, String>>): Pair<String, String> {
        if (messages.isEmpty()) return "Good Morning" to "Have a meaningful day in the ministry."
        val idx = pickIndex(context, poolId, messages.size)
        return messages.getOrElse(idx) { messages.first() }
    }

    private fun pickIndex(context: Context, poolId: String, total: Int): Int {
        val sp = context.applicationContext.getSharedPreferences(POOL_PREFS, Context.MODE_PRIVATE)
        val key = "pool_$poolId"
        var pool = sp.getString(key, null)?.split(",")?.mapNotNull { it.toIntOrNull() }?.toMutableList()
            ?: mutableListOf()
        if (pool.isEmpty()) {
            pool = (0 until total).toMutableList()
            pool.shuffle()
        }
        val index = pool.removeAt(0)
        sp.edit().putString(key, pool.joinToString(",")).apply()
        return index
    }

    private val normalMorning = arrayOf(
        "Good Morning ☀️" to "Have a meaningful day in the ministry.",
        "Good Morning 💚" to "Ready for another day of ministry?",
        "Good Morning ☀️" to "Take a moment to plan your ministry for today.",
    )

    private val maleMorning = arrayOf(
        "Good Morning Bro ☀️" to "Eroju full ga enjoy cheyyi 😌 Opportunity dorikithe evaritho aina preaching cheyadaniki try cheyyi!",
        "Morninggg Orey 👀" to "Eroju emaina special plans unnaya? First Day Text chadhivesava?",
        "Rise & Shine ☀️" to "Eroju chill ga undu bro 😎 Kudirthe oka manchi conversation start cheyyi.",
        "Good Morning Boss 😌" to "Day start cheyyadam mundu Bible open cheyyi… 5 mins chaalu 👀",
        "Orey Morning 😂" to "Eroju ministry plan lekapoyina parledu… opportunity vaste miss avvaku!",
        "Morning Bro ☀️" to "Day Text chadhivi, coffee/tea pattuko… tarvatha full day enjoy cheyyi 😂",
        "Good Morning 👊" to "Eroju konchem Bible time ki kuda slot ivvu bro 📖",
        "Today’s Vibe 😎" to "No plans? Perfect. Eroju enjoy cheyyi… chance dorikithe preaching kuda cheyyi!",
        "Good Morning Bro 💚" to "Eroju peaceful ga enjoy cheyyi. Kudirthe evarikaina oka encouraging thought share cheyyi.",
        "Have a Good Day Bro ☀️" to "Eroju nee day ni enjoy cheyyi! 📖 Bible time ki time dorikithe super… preaching chance vaste inka super!",
    )

    private val femaleMorning = arrayOf(
        "Good Morning Bangaram ☀️" to "Eroju full ga enjoy cheyyi 🫶 Kudirthe evaritho aina preaching cheyadaniki try cheyyi!",
        "Morninggg Bestie 👀" to "Eroju plans em levu… Day Text chadhivi day ni enjoy cheyyi!",
        "Good Morning Chinnari 🌸" to "First Day Text 📖 Tarvatha nee day ni full enjoy cheyyi 😌",
        "Heyy Good Morning 🫶" to "Eroju konchem Bible time ki kuda place ivvu… migatha time enjoy cheyyi!",
        "Morning Ammayi ☀️" to "Eroju ministry schedule empty undhi 👀 Opportunity vaste matram miss avvaku!",
        "Good Morning Bestie 💕" to "Day Text chadhivesi, breakfast chesi, life enjoy cheyyi 😂",
        "Rise & Shine 🫶" to "Eroju Day Text tho start chesthe nice ga untundi 📖",
        "Good Morning 🌸" to "Eroju full enjoy cheyyi! Kudirthe evarikaina oka encouraging thought share cheyyi.",
        "Good Morning Bestie 🫶" to "Eroju nee day ni enjoy cheyyi. Chance dorikithe preaching ki oka small try ivvu!",
        "Have a Beautiful Day 🌷" to "Eroju full ga enjoy cheyyi! 📖 Day Text chadhavu… chance dorikithe preaching kuda try cheyyi!",
    )

    private val maleAfter = arrayOf(
        "Bro 👀 visit aipoyi 1 hours ayyindi… update chesava?",
        "Guru 😂 {name} tho visit ayipoyindi… details ekkada?",
        "Bro 😭 return visit complete chesav… note cheyyadam marchipoyava?",
        "Bossu 👀 {name} tho visit ela ayyindi? Details add cheyyi!",
        "Brooo ⏰ 1 hours ayyindi… visit update pending undhi!",
        "Guru 😂 Mission complete… report ekkada boss?",
        "Bro 👀 return visit ayipoyindi kada… details save chesava?",
        "Anna 😭 visit chesav, ippudu app lo kuda cheppu!",
        "Bro 😎 {name} tho visit done aa? Update cheyyadam marchipoku!",
        "{nickname} 👀 visit ayipoyindi… update chesava?",
        "{nickname} 😂 {name} tho visit complete! Details add cheyyi bro!",
        "{nickname} ⏰ 1 hours ayyindi… visit update pending undhi!",
        "{nickname} 😎 Mission complete… notes lo kuda add cheddama?",
        "{nickname} 😭 visit details inka pending unnayi!",
        "{nickname} ❤️ visit done aithe details kuda done cheddama?",
    )

    private val femaleAfter = arrayOf(
        "Bangaram 👀 visit aipoyi 1 hours ayyindi… update chesava?",
        "Heyy 🥹 {name} tho visit ayipoyindi… details add chesava?",
        "Chinnari 👀 return visit complete ayyinda? Update cheyyadam marchipoku!",
        "Bangaram 😭 visit ayipoyindi… naku kuda details cheppava?",
        "Heyy bestie 😂 {name} tho visit done… update ekkada?",
        "Chinnitalli 🫶 1 hours ayyindi… visit details pending unnayi!",
        "Bestie 👀 return visit ayipoyindi kada… oka small update cheyyi!",
        "Bangaram 😌 visit complete… ippudu details save cheddama?",
        "Heyy 🥹 {name} tho visit ela ayyindi? Konchem note cheyyi!",
        "{nickname} 👀 visit ayipoyindi… update chesava?",
        "{nickname} 🥹 {name} tho visit complete ayyindi… details add cheyyi!",
        "{nickname} ⏰ 1 hours ayyindi… visit update pending undhi!",
        "{nickname} 🫶 Mission complete… app lo kuda complete cheddama?",
        "{nickname} 😭 visit details inka pending unnayi!",
        "{nickname} ❤️ visit done aithe update kuda done cheddama?",
    )

    private val maleOverdue = arrayOf(
        "Bro 👀 {name} tho follow-up overdue! Inka update cheyyaledha? 😭",
        "Guru 😂 {name} tho visit aipoyi hours ayyindi… still pending aa?",
        "Bro 😭 {name} waiting for update… report eppudu isthav?",
        "Bossu 👀 {name} tho return visit overdue aipoyindi… update cheyyi!",
        "Brooo ⏰ {name} tho follow-up miss avvakunda update kottu!",
        "Guru 😂 {name} ni marchipoyava? Overdue alert active!",
        "Bro 👀 {name} tho status inka pending enti? Update cheyyi anna!",
        "Anna 😭 {name} tho return visit records pending unnayi… complete cheyyi!",
        "{nickname} 👀 {name} tho follow-up overdue! Update chesava?",
        "{nickname} 😂 {name} tho visit aipoyi hours ayyindi… report ekkada?",
        "{nickname} ⏰ {name} waiting for update… late avvaku!",
        "{nickname} 😭 return visit overdue aipoyindi… update kottu!",
        "{nickname} 😎 {name} tho follow-up pending undhi… complete cheyyi!",
    )

    private val femaleOverdue = arrayOf(
        "Bangaram 👀 {name} tho follow-up overdue! Inka update cheyyaledha? 😭",
        "Heyy 🥹 {name} tho visit aipoyi hours ayyindi… still pending aa?",
        "Chinnari 😭 {name} waiting for update… report eppudu isthave?",
        "Bangaram 👀 {name} tho return visit overdue aipoyindi… update cheyyi!",
        "Heyy bestie ⏰ {name} tho follow-up miss avvakunda update kottu!",
        "Bestie 😂 {name} ni marchipoyava? Overdue alert active!",
        "Bangaram 👀 {name} tho status inka pending enti? Update cheyyi bestie!",
        "Chinnitalli 😭 {name} tho return visit records pending unnayi… complete cheyyi!",
        "{nickname} 👀 {name} tho follow-up overdue! Update chesava?",
        "{nickname} 🥹 {name} tho visit aipoyi hours ayyindi… report ekkada?",
        "{nickname} 🫶 {name} waiting for update… late avvaku!",
        "{nickname} 😭 return visit overdue aipoyindi… update kottu!",
        "{nickname} 👀 {name} tho follow-up pending undhi… complete cheyyi!",
    )

}
