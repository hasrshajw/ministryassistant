package com.harsha.myapplication.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object SetupRepository {
    private const val PREFS_NAME = "setup_prefs"
    private const val KEY_SETUP_COMPLETED = "setup_completed"
    private const val KEY_MONTHLY_GOAL = "monthly_goal_hours"

    const val DEFAULT_MONTHLY_GOAL_HOURS = 30

    var isSetupCompleted by mutableStateOf(true)
        private set

    /** Monthly field-service goal (hours) picked in the wizard. Read this from Home. */
    var monthlyGoalHours by mutableIntStateOf(DEFAULT_MONTHLY_GOAL_HOURS)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isSetupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        monthlyGoalHours = prefs.getInt(KEY_MONTHLY_GOAL, DEFAULT_MONTHLY_GOAL_HOURS)
    }

    fun setSetupCompleted(context: Context, completed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
        isSetupCompleted = completed
    }

    fun setMonthlyGoal(context: Context, hours: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_MONTHLY_GOAL, hours).apply()
        monthlyGoalHours = hours
    }
}
