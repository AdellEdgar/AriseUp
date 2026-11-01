# Tabla genérica de alimentos

Genera `app/src/main/assets/generic_foods.json`: unos 390 alimentos básicos con nombre en español y valores por 100 g, sacados de **USDA FoodData Central, SR Legacy** (dominio público).

## Archivos

- `foods_query.csv`: la lista editable. Cada fila es un nombre en español, una categoría, las palabras que deben aparecer en la descripción del USDA (separadas por `;`) y, opcionalmente, palabras a excluir.
- `foods_es.csv`: la lista resuelta (`fdc_id`, nombre, categoría y descripción original), para revisarla a mano.
- `build.py`: busca, resuelve y genera el JSON.
- `data/`: los CSV del USDA. Está ignorada por git.

## Regenerar

1. Descarga y descomprime SR Legacy en `data/`: <https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip>.
2. Ejecuta:

```bash
U=tools/generic_foods/data/FoodData_Central_sr_legacy_food_csv_2018-04
py -I tools/generic_foods/build.py --usda $U --search "chicken breast roasted"
py -I tools/generic_foods/build.py --usda $U --select tools/generic_foods/foods_query.csv --out tools/generic_foods/foods_es.csv
py -I tools/generic_foods/build.py --usda $U --mapping tools/generic_foods/foods_es.csv --out app/src/main/assets/generic_foods.json
py -I tools/generic_foods/test_build.py
```

## Valores

- **Sal** = sodio (mg) × 2,5 / 1000.
- **Azúcares**: "Sugars, Total" y, si falta, "Sugars, Total NLEA".
- Un nutriente ausente cuenta como 0 y marca el alimento como `incomplete`.
