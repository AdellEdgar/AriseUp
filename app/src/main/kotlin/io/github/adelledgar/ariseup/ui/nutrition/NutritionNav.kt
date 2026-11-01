package io.github.adelledgar.ariseup.ui.nutrition

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.adelledgar.ariseup.data.db.Meal
import io.github.adelledgar.ariseup.di.AppContainer
import io.github.adelledgar.ariseup.ui.nav.Routes
import java.time.LocalDate

private object N {
    const val SEARCH = "nutrition/search/{date}/{meal}"
    const val SCAN = "nutrition/scan/{date}/{meal}"
    const val FOOD = "nutrition/food/{id}/{date}/{meal}"
    const val NEW_FOOD = "nutrition/new/{date}/{meal}?barcode={barcode}&id={id}"
    const val RECIPE_LOG = "nutrition/recipe-log/{id}/{date}/{meal}"
    const val RECIPES = "nutrition/recipes"
    const val RECIPE_EDIT = "nutrition/recipe/{id}"
    const val RULES = "nutrition/rules"

    fun search(d: LocalDate, m: Meal) = "nutrition/search/$d/${m.name}"
    fun scan(d: LocalDate, m: Meal) = "nutrition/scan/$d/${m.name}"
    fun food(id: Long, d: LocalDate, m: Meal) = "nutrition/food/$id/$d/${m.name}"
    fun newFood(d: LocalDate, m: Meal, barcode: String? = null, id: Long? = null) =
        "nutrition/new/$d/${m.name}?barcode=${barcode.orEmpty()}&id=${id ?: ""}"
    fun recipeLog(id: Long, d: LocalDate, m: Meal) = "nutrition/recipe-log/$id/$d/${m.name}"
    fun recipeEdit(id: Long) = "nutrition/recipe/$id"
}

private fun androidx.navigation.NavBackStackEntry.date() = LocalDate.parse(arguments!!.getString("date"))
private fun androidx.navigation.NavBackStackEntry.meal() = Meal.valueOf(arguments!!.getString("meal")!!)
private fun androidx.navigation.NavBackStackEntry.id() = arguments!!.getString("id")?.toLongOrNull()

fun NutritionRoutes(builder: NavGraphBuilder, nav: NavHostController, container: AppContainer) = with(builder) {
    composable(Routes.NUTRITION) {
        NutritionScreen(
            container,
            onSearch = { d, m -> nav.navigate(N.search(d, m)) },
            onScan = { d, m -> nav.navigate(N.scan(d, m)) },
            onRecipes = { nav.navigate(N.RECIPES) },
            onRules = { nav.navigate(N.RULES) },
        )
    }
    composable(N.SEARCH) { e ->
        val d = e.date()
        val m = e.meal()
        FoodSearchScreen(
            container, d, m,
            onPickFood = { nav.navigate(N.food(it, d, m)) },
            onPickRecipe = { nav.navigate(N.recipeLog(it, d, m)) },
            onNewFood = { nav.navigate(N.newFood(d, m)) },
            onBack = { nav.popBackStack() },
        )
    }
    composable(N.SCAN) { e ->
        val d = e.date()
        val m = e.meal()
        ScanScreen(
            container,
            onFound = { nav.navigate(N.food(it, d, m)) { popUpTo(N.SCAN) { inclusive = true } } },
            onNotFound = { nav.navigate(N.newFood(d, m, barcode = it)) { popUpTo(N.SCAN) { inclusive = true } } },
            onBack = { nav.popBackStack() },
        )
    }
    composable(N.FOOD) { e ->
        val d = e.date()
        val m = e.meal()
        FoodDetailScreen(
            container, e.id()!!, d, m,
            onDone = { nav.popBackStack(Routes.NUTRITION, inclusive = false) },
            onEdit = { nav.navigate(N.newFood(d, m, id = it)) },
        )
    }
    composable(N.NEW_FOOD) { e ->
        val d = e.date()
        val m = e.meal()
        FoodEditorScreen(
            container,
            foodId = e.id(),
            barcode = e.arguments?.getString("barcode")?.ifBlank { null },
            onSaved = { nav.navigate(N.food(it, d, m)) { popUpTo(N.NEW_FOOD) { inclusive = true } } },
            onBack = { nav.popBackStack() },
        )
    }
    composable(N.RECIPE_LOG) { e ->
        RecipeLogScreen(container, e.id()!!, e.date(), e.meal(), onDone = { nav.popBackStack(Routes.NUTRITION, inclusive = false) })
    }
    composable(N.RECIPES) { RecipeListScreen(container, onEdit = { nav.navigate(N.recipeEdit(it)) }, onBack = { nav.popBackStack() }) }
    composable(N.RECIPE_EDIT) { e -> RecipeEditorScreen(container, e.id() ?: 0, onDone = { nav.popBackStack() }) }
    composable(N.RULES) { DietRulesScreen(container, onBack = { nav.popBackStack() }) }
}
