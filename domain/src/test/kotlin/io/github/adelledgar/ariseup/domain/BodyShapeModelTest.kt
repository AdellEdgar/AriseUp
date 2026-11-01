package io.github.adelledgar.ariseup.domain

import io.github.adelledgar.ariseup.domain.model.Sex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BodyShapeModelTest {
    @Test
    fun bmi() {
        assertEquals(24.69, BodyShapeModel.bmi(80.0, 180.0), 0.01)
    }

    @Test
    fun deurenberg() {
        assertEquals(20.33, BodyShapeModel.estimatedBodyFat(24.69, 30, Sex.MALE), 0.01)
        assertEquals(31.13, BodyShapeModel.estimatedBodyFat(24.69, 30, Sex.FEMALE), 0.01)
    }

    @Test
    fun registeredFatWins() {
        assertEquals(0.5, BodyShapeModel.shape(Sex.MALE, 30, 180.0, 80.0, 24.0, 10.0).fat, 1e-9)
    }

    @Test
    fun estimatedFatUsedWhenMissing() {
        val s = BodyShapeModel.shape(Sex.MALE, 30, 180.0, 80.0, null, 10.0)
        assertEquals((20.328 - 8) / 32, s.fat, 0.001)
    }

    @Test
    fun clamped() {
        assertEquals(1.0, BodyShapeModel.shape(Sex.FEMALE, 40, 160.0, 140.0, null, 10.0).fat)
        assertEquals(0.0, BodyShapeModel.shape(Sex.MALE, 20, 190.0, 55.0, 5.0, 3.0).fat)
        assertEquals(0.0, BodyShapeModel.shape(Sex.MALE, 20, 190.0, 55.0, 5.0, 3.0).muscle)
        assertEquals(1.0, BodyShapeModel.shape(Sex.MALE, 20, 190.0, 80.0, null, 150.0).muscle)
    }

    @Test
    fun muscleFromFue() {
        assertEquals(0.5, BodyShapeModel.shape(Sex.MALE, 30, 175.0, 70.0, null, 52.5).muscle, 1e-9)
    }

    @Test
    fun heightScale() {
        assertEquals(1.0, BodyShapeModel.shape(Sex.MALE, 30, 175.0, 70.0, null, 10.0).heightScale, 1e-9)
        assertEquals(0.92, BodyShapeModel.shape(Sex.MALE, 30, 140.0, 50.0, null, 10.0).heightScale, 1e-9)
        assertEquals(1.08, BodyShapeModel.shape(Sex.MALE, 30, 210.0, 90.0, null, 10.0).heightScale, 1e-9)
    }

    @Test
    fun moreWeightMoreFat() {
        val a = BodyShapeModel.shape(Sex.MALE, 30, 180.0, 75.0, null, 10.0)
        val b = BodyShapeModel.shape(Sex.MALE, 30, 180.0, 95.0, null, 10.0)
        assertTrue(b.fat > a.fat)
    }
}
