package com.bounswe2026group8.emergencyhub.mesh.ui

import android.content.Context
import com.bounswe2026group8.emergencyhub.R

/**
 * Locale-aware "fix Xs/m/h/d ago" formatter shared by every mesh UI surface.
 * Splitting this out lets us localize the strings via per-locale strings.xml
 * (values-xx/strings.xml) instead of hardcoding English in each adapter/activity.
 */
internal fun formatFixAge(ctx: Context, ageSec: Long): String {
    return when {
        ageSec < 60 -> ctx.getString(R.string.mesh_fix_seconds_format, ageSec.toInt())
        ageSec < 3600 -> ctx.getString(R.string.mesh_fix_minutes_format, (ageSec / 60).toInt())
        ageSec < 86400 -> ctx.getString(R.string.mesh_fix_hours_format, (ageSec / 3600).toInt())
        else -> ctx.getString(R.string.mesh_fix_days_format, (ageSec / 86400).toInt())
    }
}
