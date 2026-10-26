package com.example.tasktracker.internship.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tasktracker.MainActivity
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against the real Activity, Hilt graph and Room database. */
@RunWith(AndroidJUnit4::class)
class ApplicationFlowTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createEditUndoDeleteRoundTrip() {
        // Persistent app data may already contain records from earlier runs.
        val company = "UI 测试公司 ${UUID.randomUUID().toString().take(8)}"

        composeRule.onNodeWithTag("application_add").performClick()
        composeRule.onNodeWithTag("form_company").performTextInput(company)
        composeRule.onNodeWithTag("form_job_title").performTextInput("Android 实习生")
        composeRule.onNodeWithTag("form_list")
            .performScrollToNode(hasTestTag("form_city"))
        composeRule.onNodeWithTag("form_city").performTextInput("上海")
        Espresso.closeSoftKeyboard()
        saveForm()

        awaitList()
        val card = applicationCardFor(company)
        scrollToCard(card)
        val cardTag = composeRule.onNode(card, useUnmergedTree = true)
            .assertExists()
            .fetchSemanticsNode()
            .config[SemanticsProperties.TestTag]
        val id = cardTag.removePrefix("application_card_")
        composeRule.onNodeWithTag("application_edit_$id").performClick()

        composeRule.onNodeWithTag("form_list")
            .performScrollToNode(hasTestTag("form_status_menu"))
        composeRule.onNodeWithTag("form_status_menu").performClick()
        composeRule.onNodeWithTag("form_status_INTERVIEW").performClick()
        saveForm()

        awaitList()
        scrollToCard(hasTestTag(cardTag))
        val updatedCard = card and hasAnyDescendant(hasText("面试"))
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(updatedCard, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(updatedCard, useUnmergedTree = true)
            .assertExists()

        // The undo prompt must be immediately actionable while its short timer is active.
        composeRule.onNodeWithTag("application_delete_$id").performClick()
        composeRule.waitUntil(timeoutMillis = 4_000) {
            composeRule.onAllNodesWithTag("undo_action").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("undo_snackbar").assertExists()
        composeRule.onNodeWithTag("undo_action").performClick()
        scrollToCard(hasTestTag(cardTag))
        composeRule.onNodeWithTag(cardTag).assertExists()

        // Leave the installed app's data as it was before this flow.
        composeRule.onNodeWithTag("application_delete_$id").performClick()
        composeRule.waitUntil(timeoutMillis = 4_000) {
            composeRule.onAllNodesWithTag(cardTag).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun saveForm() {
        composeRule.onNodeWithTag("form_list")
            .performScrollToNode(hasTestTag("form_save"))
        composeRule.onNodeWithTag("form_save").performClick()
    }

    private fun awaitList() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("application_list").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollToCard(card: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            try {
                composeRule.onNodeWithTag("application_list")
                    .performScrollToNode(card)
                true
            } catch (_: AssertionError) {
                false
            }
        }
    }

    private fun applicationCardFor(company: String): SemanticsMatcher =
        SemanticsMatcher("normal list card for $company") { node ->
            if (SemanticsProperties.TestTag !in node.config) false
            else node.config[SemanticsProperties.TestTag].let { tag ->
                tag.startsWith("application_card_") && !tag.startsWith("application_card_due_")
            }
        } and hasAnyDescendant(hasText(company))
}
