package com.ivy.creditcards

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.creditcards.preview.CreditCardDetailsUiTest
import com.ivy.creditcards.preview.CreditCardsContentUiTest
import com.ivy.creditcards.preview.EditCreditCardUiTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class CreditCardsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    private val dark get() = theme == PaparazziTheme.Dark

    @Test
    fun `snapshot cards collapsed`() {
        snapshot(theme) { CreditCardsContentUiTest(dark = dark, expanded = false) }
    }

    @Test
    fun `snapshot cards expanded`() {
        snapshot(theme) { CreditCardsContentUiTest(dark = dark, expanded = true) }
    }

    @Test
    fun `snapshot cards empty`() {
        snapshot(theme) { CreditCardsContentUiTest(dark = dark, expanded = false, empty = true) }
    }

    @Test
    fun `snapshot edit add`() {
        snapshot(theme) { EditCreditCardUiTest(dark = dark, isEdit = false) }
    }

    @Test
    fun `snapshot edit errors`() {
        snapshot(theme) { EditCreditCardUiTest(dark = dark, isEdit = false, withErrors = true) }
    }

    @Test
    fun `snapshot edit existing`() {
        snapshot(theme) { EditCreditCardUiTest(dark = dark, isEdit = true) }
    }

    @Test
    fun `snapshot details hidden`() {
        snapshot(theme) { CreditCardDetailsUiTest(dark = dark, revealed = false) }
    }

    @Test
    fun `snapshot details revealed`() {
        snapshot(theme) { CreditCardDetailsUiTest(dark = dark, revealed = true) }
    }
}
