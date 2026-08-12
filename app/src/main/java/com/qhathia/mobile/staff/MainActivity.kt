package com.qhathia.mobile.staff

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.common.navigation.EXTRA_DESTINATION
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.navigation.QhathiaNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Same instance hiltViewModel() resolves inside setContent — both use this Activity as the
    // ViewModelStoreOwner — so the destination recorded here reaches the composables below.
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh start: on a configuration change the Activity is recreated with the same
        // launching intent, and re-reading it would jump to the section a second time.
        if (savedInstanceState == null) {
            viewModel.onDeepLink(intent.getStringExtra(EXTRA_DESTINATION))
        }
        setContent {
            QhathiaTheme {
                QhathiaRoot()
            }
        }
    }

    /** A notification tapped while the app is already running (launchMode is `singleTop`). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.onDeepLink(intent.getStringExtra(EXTRA_DESTINATION))
    }
}

/** Resolves the session state, then renders the navigation graph with the right start destination. */
@Composable
private fun QhathiaRoot(viewModel: MainViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val pendingDestination by viewModel.pendingDestination.collectAsStateWithLifecycle()

    when (authState) {
        AuthState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AuthState.Authenticated -> QhathiaNavHost(
            startAuthenticated = true,
            pendingDestination = pendingDestination,
            onPendingDestinationConsumed = viewModel::onPendingDestinationConsumed,
        )
        AuthState.Unauthenticated -> QhathiaNavHost(
            startAuthenticated = false,
            pendingDestination = pendingDestination,
            onPendingDestinationConsumed = viewModel::onPendingDestinationConsumed,
        )
    }
}
