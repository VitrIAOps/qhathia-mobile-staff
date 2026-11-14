package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Brings the list back to its first item each time [token] changes (F-80). A keyed lazy list keeps the
 * first visible item's key when its data changes, so items inserted above it stay out of view; this
 * forgets that anchor. Runs as a [SideEffect], i.e. after composition and before this frame is
 * measured, so the list cannot first re-anchor on the old key (a `LaunchedEffect` runs after layout and
 * jumps a frame late). The handled token is saved, so coming back to a tab does not reset again.
 */
@Composable
fun LazyListState.ResetScrollOn(token: Int) {
    var handled by rememberSaveable { mutableIntStateOf(token) }
    SideEffect {
        if (token != handled) {
            handled = token
            requestScrollToItem(0)
        }
    }
}
