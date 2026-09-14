package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import org.junit.Rule
import org.junit.Test

/**
 * The badge on a bottom-bar item: absent unless there is something to report, capped past 99, and
 * announced as part of the destination rather than as a loose number.
 */
class QhathiaBottomBarItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setBadge(count: Int?, description: String? = "Solicitudes, $count pendientes") {
        composeTestRule.setContent {
            QhathiaTheme {
                QhathiaBottomBar {
                    QhathiaBottomBarItem(
                        selected = false,
                        onClick = {},
                        iconRes = R.drawable.ic_nav_requests,
                        label = "Solicitudes",
                        badgeCount = count,
                        badgeContentDescription = description,
                    )
                }
            }
        }
    }

    @Test
    fun noBadge_whenCountIsNull() {
        setBadge(count = null, description = null)

        composeTestRule.onNodeWithText("Solicitudes").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun noBadge_whenCountIsZero() {
        setBadge(count = 0, description = null)

        composeTestRule.onNodeWithText("Solicitudes").assertIsDisplayed()
        composeTestRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun badgeShowsCount_whenSomethingIsPending() {
        setBadge(count = 2)

        composeTestRule
            .onNodeWithContentDescription("Solicitudes, 2 pendientes")
            .assertIsDisplayed()
    }

    @Test
    fun badgeIsCapped_aboveNinetyNine() {
        composeTestRule.setContent {
            QhathiaTheme {
                QhathiaBottomBar {
                    QhathiaBottomBarItem(
                        selected = false,
                        onClick = {},
                        iconRes = R.drawable.ic_nav_requests,
                        label = "Solicitudes",
                        badgeCount = 128,
                        // No merged description here, so the digits stay individually assertable.
                        badgeContentDescription = null,
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("99+").assertIsDisplayed()
    }
}
