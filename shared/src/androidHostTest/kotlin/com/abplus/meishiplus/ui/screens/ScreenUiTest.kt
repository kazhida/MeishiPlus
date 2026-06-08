package com.abplus.meishiplus.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.abplus.meishiplus.data.entities.CardEntity
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cardExchangeScreen_showsCardAndQrTabs() {
        composeRule.setContent {
            MaterialTheme {
                CardExchangeScreen(
                    cardEntity = testCard,
                )
            }
        }

        composeRule.onNodeWithText("名刺交換").assertIsDisplayed()
        composeRule.onNodeWithText("QRコード").assertIsDisplayed()
        composeRule.onNodeWithText("QR読み取り").assertIsDisplayed()
        composeRule.onNodeWithText(testCard.organization.value).assertIsDisplayed()
        composeRule.onNodeWithText(testCard.name.value).assertIsDisplayed()
        composeRule.onNodeWithText(testCard.email.value).assertIsDisplayed()
    }

    @Test
    fun cardPrintScreen_initiallyShowsPostcardWithoutMarginFields() {
        composeRule.setContent {
            MaterialTheme {
                CardPrintScreen(
                    cardEntity = testCard,
                )
            }
        }

        composeRule.onAllNodesWithText("PDF出力").assertCountEquals(2)
        composeRule.onNodeWithTag("postcard-page-size-radio").assertIsSelected()
        composeRule.onNodeWithTag("a4-page-size-radio").assertIsNotSelected()
        composeRule.onNodeWithText("上 mm").assertDoesNotExist()
        composeRule.onNodeWithText("下 mm").assertDoesNotExist()
        composeRule.onNodeWithText("左 mm").assertDoesNotExist()
        composeRule.onNodeWithText("右 mm").assertDoesNotExist()
    }

    @Test
    fun cardPrintScreen_showsMarginFieldsWhenA4Selected() {
        composeRule.setContent {
            MaterialTheme {
                CardPrintScreen(
                    cardEntity = testCard,
                )
            }
        }

        composeRule.onNodeWithTag("a4-page-size-radio").performClick()

        composeRule.onNodeWithTag("postcard-page-size-radio").assertIsNotSelected()
        composeRule.onNodeWithTag("a4-page-size-radio").assertIsSelected()
        composeRule.onNodeWithText("余白").assertIsDisplayed()
        composeRule.onNodeWithText("上 mm").assertIsDisplayed()
        composeRule.onNodeWithText("下 mm").assertIsDisplayed()
        composeRule.onNodeWithText("左 mm").assertIsDisplayed()
        composeRule.onNodeWithText("右 mm").assertIsDisplayed()
    }

    private companion object {
        val testCard = CardEntity(
            id = "card-ui-test",
            organization = CardEntity.CardElement(
                value = "UIテスト株式会社",
                x = 0.07f,
                y = 0.12f,
                fontSize = 14f,
            ),
            name = CardEntity.CardElement(
                value = "名刺 太郎",
                x = 0.07f,
                y = 0.33f,
                fontSize = 24f,
            ),
            email = CardEntity.CardElement(
                value = "ui-test@example.com",
                x = 0.20f,
                y = 0.66f,
                fontSize = 12f,
            ),
        )
    }
}
