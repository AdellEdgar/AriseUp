package io.github.adelledgar.ariseup.ui

import io.github.adelledgar.ariseup.domain.BodyShape
import io.github.adelledgar.ariseup.domain.model.Sex
import io.github.adelledgar.ariseup.ui.components.AvatarGeometry
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvatarGeometryTest {
    private fun shape(fat: Double = 0.3, muscle: Double = 0.2, height: Double = 1.0, sex: Sex = Sex.MALE) = BodyShape(fat, muscle, height, sex)

    @Test
    fun outlineIsSymmetric() {
        val pts = AvatarGeometry.outline(shape())
        assertEquals(48, pts.size)
        for (i in pts.indices) {
            val mirror = pts[pts.size - 1 - i]
            assertEquals(pts[i].x, 1f - mirror.x, 1e-5f)
            assertEquals(pts[i].y, mirror.y, 1e-5f)
        }
    }

    @Test
    fun moreFatWiderWaist() {
        val lean = AvatarGeometry.outline(shape(fat = 0.0))[AvatarGeometry.WAIST].x
        val fat = AvatarGeometry.outline(shape(fat = 1.0))[AvatarGeometry.WAIST].x
        assertTrue(fat > lean + 0.03f)
    }

    @Test
    fun moreMuscleWiderShoulders() {
        val a = AvatarGeometry.outline(shape(muscle = 0.0))[AvatarGeometry.SHOULDER].x
        val b = AvatarGeometry.outline(shape(muscle = 1.0))[AvatarGeometry.SHOULDER].x
        assertTrue(b > a + 0.02f)
    }

    @Test
    fun femaleWiderHips() {
        val m = AvatarGeometry.outline(shape(sex = Sex.MALE))[AvatarGeometry.HIP].x
        val f = AvatarGeometry.outline(shape(sex = Sex.FEMALE))[AvatarGeometry.HIP].x
        assertTrue(f > m)
    }

    @Test
    fun heightScaleStretches() {
        fun span(h: Double) = AvatarGeometry.outline(shape(height = h)).let { p -> p.maxOf { it.y } - p.minOf { it.y } }
        assertEquals(1.08f, span(1.08) / span(1.0), 1e-4f)
    }

    @Test
    fun hunchedLowersShoulders() {
        val normal = AvatarGeometry.outline(shape())[AvatarGeometry.SHOULDER]
        val hunched = AvatarGeometry.outline(shape(), hunched = true)[AvatarGeometry.SHOULDER]
        assertTrue(hunched.y > normal.y)
    }
}
