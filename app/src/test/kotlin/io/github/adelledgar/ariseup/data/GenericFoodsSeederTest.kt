package io.github.adelledgar.ariseup.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.adelledgar.ariseup.TestEnv
import io.github.adelledgar.ariseup.data.db.FoodSource
import io.github.adelledgar.ariseup.data.seed.GenericFoodsSeeder
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class GenericFoodsSeederTest {
    private val env = TestEnv()

    @Test
    fun seedsOnceFromAssets() = runTest {
        val seeder = GenericFoodsSeeder(env.context, env.db)
        val n = seeder.seedIfEmpty()
        assertTrue(n >= 300, "solo $n alimentos")
        assertEquals(n, env.db.foodDao().countBySource(FoodSource.GENERIC))
        assertEquals(0, seeder.seedIfEmpty())
        assertEquals(n, env.db.foodDao().countBySource(FoodSource.GENERIC))
    }

    @Test
    fun genericFoodsAreSearchable() = runTest {
        GenericFoodsSeeder(env.context, env.db).seedIfEmpty()
        val chicken = env.foods.searchLocal("pechuga de pollo a la plancha").first()
        assertEquals(165.0, chicken.kcal, 1e-9)
        assertEquals(31.02, chicken.protein, 1e-9)
        assertTrue(env.foods.searchLocal("platano").isNotEmpty())
    }
}
