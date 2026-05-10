package com.bounswe2026group8.emergencyhub.ui

/**
 * Pure helpers for staff-role checks on the mobile client.
 *
 * The backend remains the source of truth for authorization; these helpers
 * only gate the UI so we don't show admin/moderation cards to standard users.
 */
object StaffRoleHelper {

    const val NONE = "NONE"
    const val MODERATOR = "MODERATOR"
    const val VERIFICATION_COORDINATOR = "VERIFICATION_COORDINATOR"
    const val ADMIN = "ADMIN"

    fun isAdmin(staffRole: String?): Boolean = staffRole == ADMIN

    fun isModerator(staffRole: String?): Boolean = staffRole == MODERATOR

    fun isVerificationCoordinator(staffRole: String?): Boolean =
        staffRole == VERIFICATION_COORDINATOR

    fun canModerate(staffRole: String?): Boolean =
        isAdmin(staffRole) || isModerator(staffRole)

    fun canVerifyExpertise(staffRole: String?): Boolean =
        isAdmin(staffRole) || isVerificationCoordinator(staffRole)

    fun hasAnyStaffRole(staffRole: String?): Boolean =
        !staffRole.isNullOrBlank() && staffRole != NONE

    fun label(staffRole: String?): String = when (staffRole) {
        ADMIN -> "Admin"
        MODERATOR -> "Moderator"
        VERIFICATION_COORDINATOR -> "Verification Coordinator"
        else -> "No staff role"
    }
}
