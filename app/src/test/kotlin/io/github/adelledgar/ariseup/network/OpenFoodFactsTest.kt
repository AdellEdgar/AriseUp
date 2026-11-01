package io.github.adelledgar.ariseup.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenFoodFactsTest {
    private fun dto(
        name: String? = "Producto",
        nameEs: String? = null,
        kcal100: Double? = null,
        kj100: Double? = null,
        kcalServing: Double? = null,
        servingQty: Double? = null,
        protein100: Double? = null,
    ) = OffProduct(
        code = "123",
        productName = name,
        productNameEs = nameEs,
        brands = "Marca",
        servingQuantity = servingQty?.let { JsonPrimitive(it) },
        nutriments = buildMap {
            kcal100?.let { put("energy-kcal_100g", JsonPrimitive(it)) }
            kj100?.let { put("energy-kj_100g", JsonPrimitive(it)) }
            kcalServing?.let { put("energy-kcal_serving", JsonPrimitive(it)) }
            protein100?.let { put("proteins_100g", JsonPrimitive(it)) }
        },
    )

    @Test
    fun kjOnly() {
        assertEquals(100.0, OffMapper.toFood(dto(kj100 = 418.4)).kcal, 1e-6)
    }

    @Test
    fun missingNutrimentsMarksIncomplete() {
        val f = OffMapper.toFood(dto(kcal100 = 250.0))
        assertEquals(0.0, f.protein)
        assertTrue(f.incomplete)
    }

    @Test
    fun servingOnlyNormalized() {
        val f = OffMapper.toFood(dto(kcalServing = 60.0, servingQty = 30.0))
        assertEquals(200.0, f.kcal, 1e-6)
        assertEquals(30.0, f.servingG)
    }

    @Test
    fun prefersSpanishName() {
        assertEquals("Galletas", OffMapper.toFood(dto(name = "Biscuits", nameEs = "Galletas")).name)
    }

    @Test
    fun stringNumbersAccepted() {
        val p = dto().copy(nutriments = mapOf("energy-kcal_100g" to JsonPrimitive("120,5"), "proteins_100g" to JsonPrimitive("3")))
        val f = OffMapper.toFood(p)
        assertEquals(120.5, f.kcal, 1e-9)
        assertEquals(3.0, f.protein, 1e-9)
    }

    private fun client(handler: io.ktor.client.engine.mock.MockRequestHandler) = OpenFoodFactsClient(OpenFoodFactsClient.httpClient(MockEngine(handler)), "1.0.0")

    @Test
    fun foundByCode() = runTest {
        val c = client {
            respond(
                """{"status":1,"product":{"code":"8410","product_name":"Leche","nutriments":{"energy-kcal_100g":64,"proteins_100g":3.1,"carbohydrates_100g":4.7,"sugars_100g":4.7,"fat_100g":3.6,"saturated-fat_100g":2.4,"fiber_100g":0,"salt_100g":0.13}}}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val r = assertIs<OffResult.Found>(c.product("8410"))
        assertEquals("Leche", r.foods.single().name)
        assertEquals("8410", r.foods.single().barcode)
        assertEquals(false, r.foods.single().incomplete)
    }

    @Test
    fun notFound() = runTest {
        assertEquals(OffResult.NotFound, client { respond("""{"status":0}""", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "application/json")) }.product("000"))
        assertEquals(OffResult.NotFound, client { respond("""{"status":0}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }.product("000"))
    }

    @Test
    fun offline() = runTest {
        assertEquals(OffResult.Offline, client { throw IOException("sin red") }.product("1"))
    }

    @Test
    fun userAgentSent() = runTest {
        var ua: String? = null
        client {
            ua = it.headers[HttpHeaders.UserAgent]
            respond("""{"products":[]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }.search("leche")
        assertTrue(ua!!.startsWith("AriseUp/1.0.0"), ua)
    }

    @Test
    fun searchSkipsProductsWithoutName() = runTest {
        val c = client {
            respond(
                """{"products":[{"code":"1","product_name":"Yogur","nutriments":{"energy-kcal_100g":60}},{"code":"2","nutriments":{}}]}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        assertEquals(listOf("Yogur"), assertIs<OffResult.Found>(c.search("yogur")).foods.map { it.name })
    }
}
