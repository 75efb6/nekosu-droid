package ru.nsu.ccfit.zuev.osu

import androidx.annotation.StringRes
import ru.nsu.ccfit.zuev.osuplus.R

enum class RankedStatus(@StringRes val stringId: Int) {
    ranked(R.string.ranked_status_ranked),
    approved(R.string.ranked_status_approved),
    qualified(R.string.ranked_status_qualified),
    loved(R.string.ranked_status_loved),
    pending(R.string.ranked_status_pending),
    workInProgress(R.string.ranked_status_wip),
    graveyard(R.string.ranked_status_graveyard);

    companion object {
        @JvmStatic
        fun valueOf(value: Int): RankedStatus {
            return when (value) {
                1 -> ranked
                2 -> approved
                3 -> qualified
                4 -> loved
                0 -> pending
                -1 -> workInProgress
                -2 -> graveyard
                else -> throw IllegalArgumentException("Invalid value: $value")
            }
        }
    }
}
