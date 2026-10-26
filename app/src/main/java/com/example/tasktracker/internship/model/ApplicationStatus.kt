package com.example.tasktracker.internship.model

/** Stable codes are persisted in Room. Never persist an enum ordinal. */
enum class ApplicationStatus(
    val code: String,
    val displayName: String,
    val isTerminal: Boolean = false,
) {
    PENDING("PENDING", "待投递"),
    APPLIED("APPLIED", "已投递"),
    ASSESSMENT("ASSESSMENT", "笔试"),
    INTERVIEW("INTERVIEW", "面试"),
    OFFER("OFFER", "Offer", true),
    REJECTED("REJECTED", "未通过", true);

    companion object {
        fun fromCode(code: String): ApplicationStatus =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("未知的投递阶段：$code")
    }
}
