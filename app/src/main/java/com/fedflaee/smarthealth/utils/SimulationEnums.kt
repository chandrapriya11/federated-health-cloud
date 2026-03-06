package com.fedflaee.smarthealth.utils

enum class ActivityState {
    REST,
    WALK,
    RUN
}

enum class SleepState {
    AWAKE,
    SLEEP
}

enum class SeverityLevel {
    NORMAL,
    ABNORMAL,
    SEVERE,
    CRITICAL,
    OUTLIER
}

enum class AgeGroup {
    CHILD,
    ADULT,
    ELDERLY
}

enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}