package io.github.adelledgar.ariseup.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.domain.BodyShape
import io.github.adelledgar.ariseup.domain.model.Rank
import io.github.adelledgar.ariseup.domain.model.Sex
import io.github.adelledgar.ariseup.ui.components.AvatarMode
import io.github.adelledgar.ariseup.ui.components.ShadowAvatar
import io.github.adelledgar.ariseup.ui.theme.SystemColors
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.test.assertTrue

/** Genera una imagen de varias siluetas en app/build/snapshots para revisarlas a ojo. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AvatarSnapshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun renderShapes() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            androidx.compose.foundation.layout.Column(Modifier.testTag("avatars").background(SystemColors.background)) {
                Row {
                ShadowAvatar(BodyShape(0.05, 0.1, 1.0, Sex.MALE), AvatarMode.NORMAL, Rank.E, Modifier.size(150.dp, 250.dp))
                ShadowAvatar(BodyShape(0.9, 0.1, 1.0, Sex.MALE), AvatarMode.NORMAL, Rank.D, Modifier.size(150.dp, 250.dp))
                }
                Row {
                ShadowAvatar(BodyShape(0.2, 1.0, 1.05, Sex.MALE), AvatarMode.NORMAL, Rank.S, Modifier.size(150.dp, 250.dp))
                ShadowAvatar(BodyShape(0.4, 0.2, 0.95, Sex.FEMALE), AvatarMode.PENALTY, Rank.C, Modifier.size(150.dp, 250.dp))
                }
            }
        }
        compose.mainClock.advanceTimeBy(2000)
        val bmp = compose.onNodeWithTag("avatars").captureToImage().asAndroidBitmap()
        val out = File("build/snapshots/avatars.png").apply { parentFile!!.mkdirs() }
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(out.length() > 0)
    }
}
