package com.ivy.data.skin

import io.kotest.matchers.shouldBe
import org.junit.Test

class CardSkinImageStoreTest {
    @Test
    fun `sample size halves until the width fits`() {
        computeSampleSize(width = 800, maxWidth = 1200) shouldBe 1
        computeSampleSize(width = 1200, maxWidth = 1200) shouldBe 1
        computeSampleSize(width = 2400, maxWidth = 1200) shouldBe 2
        computeSampleSize(width = 4000, maxWidth = 1200) shouldBe 2
        computeSampleSize(width = 10_000, maxWidth = 1200) shouldBe 8
    }

    @Test
    fun `default maximum is 1200 px`() {
        computeSampleSize(width = 4800) shouldBe 4
    }
}
