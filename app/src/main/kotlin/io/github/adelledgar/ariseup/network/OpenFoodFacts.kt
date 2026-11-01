package io.github.adelledgar.ariseup.network

import io.github.adelledgar.ariseup.data.db.FoodEntity
import io.github.adelledgar.ariseup.data.db.FoodSource
import io.github.adelledgar.ariseup.domain.Nutrition
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException

@Serializable
data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_es") val productNameEs: String? = null,
    val brands: String? = null,
    @SerialName("serving_quantity") val servingQuantity: JsonElement? = null,
    val nutriments: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class OffProductResponse(val status: Int = 0, val product: OffProduct? = null)

@Serializable
data class OffSearchResponse(val products: List<OffProduct> = emptyList())

sealed interface OffResult {
    data class Found(val foods: List<FoodEntity>) : OffResult
    data object NotFound : OffResult
    data object Offline : OffResult
    data class Error(val msg: String) : OffResult
}

/** Convierte un producto de Open Food Facts a nuestro alimento por 100 g. */
object OffMapper {
    private fun JsonElement?.number(): Double? =
        (this as? JsonPrimitive)?.contentOrNull?.trim()?.replace(',', '.')?.toDoubleOrNull()

    fun toFood(p: OffProduct): FoodEntity {
        val n = p.nutriments
        val serving = p.servingQuantity.number()?.takeIf { it > 0 }
        var incomplete = false

        fun per100(key: String): Double? =
            n["${key}_100g"].number() ?: serving?.let { s -> n["${key}_serving"].number()?.let { it * 100 / s } }

        fun value(key: String): Double = per100(key) ?: run { incomplete = true; 0.0 }

        val kcal = per100("energy-kcal")
            ?: per100("energy-kj")?.let(Nutrition::kjToKcal)
            ?: per100("energy")?.let(Nutrition::kjToKcal)
            ?: run { incomplete = true; 0.0 }
        val name = listOf(p.productNameEs, p.productName).firstOrNull { !it.isNullOrBlank() }?.trim() ?: "Producto ${p.code ?: ""}".trim()
        return FoodEntity.create(
            name = name,
            brand = p.brands?.split(',')?.firstOrNull()?.trim()?.ifBlank { null },
            barcode = p.code,
            source = FoodSource.OFF,
            kcal = kcal,
            protein = value("proteins"),
            carbs = value("carbohydrates"),
            sugars = value("sugars"),
            fat = value("fat"),
            saturated = value("saturated-fat"),
            fiber = value("fiber"),
            salt = value("salt"),
            servingG = serving,
            incomplete = incomplete,
        )
    }
}

class OpenFoodFactsClient(private val http: HttpClient, private val versionName: String) {
    private val userAgent get() = "AriseUp/$versionName (https://github.com/AdellEdgar/AriseUp)"

    suspend fun product(code: String): OffResult = call {
        val res = http.get("$BASE/api/v2/product/${code.trim()}") {
            parameter("fields", FIELDS)
            header(HttpHeaders.UserAgent, userAgent)
        }
        if (res.status == HttpStatusCode.NotFound) return@call OffResult.NotFound
        if (res.status != HttpStatusCode.OK) return@call OffResult.Error("[Error del servidor: ${res.status.value}]")
        val body = res.body<OffProductResponse>()
        val p = body.product
        if (body.status != 1 || p == null) OffResult.NotFound else OffResult.Found(listOf(OffMapper.toFood(p.copy(code = p.code ?: code))))
    }

    suspend fun search(q: String, page: Int = 1): OffResult = call {
        val res = http.get("$BASE/cgi/search.pl") {
            parameter("search_terms", q)
            parameter("search_simple", 1)
            parameter("action", "process")
            parameter("json", 1)
            parameter("page_size", 20)
            parameter("page", page)
            parameter("cc", "es")
            parameter("lc", "es")
            parameter("fields", FIELDS)
            header(HttpHeaders.UserAgent, userAgent)
        }
        if (res.status != HttpStatusCode.OK) return@call OffResult.Error("[Error del servidor: ${res.status.value}]")
        val foods = res.body<OffSearchResponse>().products
            .filter { !it.productName.isNullOrBlank() || !it.productNameEs.isNullOrBlank() }
            .map(OffMapper::toFood)
        if (foods.isEmpty()) OffResult.NotFound else OffResult.Found(foods)
    }

    private suspend fun call(block: suspend () -> OffResult): OffResult = try {
        block()
    } catch (_: IOException) {
        OffResult.Offline
    } catch (e: Exception) {
        if (e.cause is IOException) OffResult.Offline else OffResult.Error("[Respuesta no válida del servidor]")
    }

    companion object {
        private const val BASE = "https://world.openfoodfacts.org"
        private const val FIELDS = "code,product_name,product_name_es,brands,nutriments,serving_quantity"

        fun httpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
            install(HttpTimeout) { requestTimeoutMillis = 15_000 }
            expectSuccess = false
        }
    }
}
