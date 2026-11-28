package com.harsha.myapplication.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Holds a person id from a notification tap until the UI opens their profile.
 * Set from [MainActivity] when a notification is clicked; consumed by [PeopleScreen].
 */
object NotificationNav {
    /** Non-null when a visit/before/after/overdue notification was tapped. */
    var pendingPersonId by mutableStateOf<String?>(null)
        private set

    /** Switch to People tab when this becomes true (driven by pendingPersonId). */
    var requestPeopleTab by mutableStateOf(false)
        private set

    fun openPerson(recordId: String) {
        if (recordId.isBlank()) return
        pendingPersonId = recordId
        requestPeopleTab = true
    }

    fun consumePersonId(): String? {
        val id = pendingPersonId
        pendingPersonId = null
        return id
    }

    fun peopleTabHandled() {
        requestPeopleTab = false
    }
}
