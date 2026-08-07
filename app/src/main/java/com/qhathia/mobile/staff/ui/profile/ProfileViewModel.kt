package com.qhathia.mobile.staff.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Profile screen's sign-out action. Clearing the session (inside [LogoutUseCase]) flips
 * the observed auth state to unauthenticated, which returns the app to login — no manual navigation.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
