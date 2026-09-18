package com.qhathia.mobile.staff.feature.profile.common

/** The three states of a section that loads on its own. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    data class Loaded<T>(val value: T) : LoadState<T>

    data object Error : LoadState<Nothing>
}

/** The loaded value, or `null` while loading or after a failure. */
val <T> LoadState<T>.valueOrNull: T?
    get() = (this as? LoadState.Loaded<T>)?.value

/**
 * The state after a failed reload: data already on screen stays (stale beats blank), otherwise the
 * section shows its error.
 */
fun <T> LoadState<T>.afterFailure(): LoadState<T> = if (this is LoadState.Loaded) this else LoadState.Error
