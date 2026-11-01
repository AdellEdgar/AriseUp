"""Tests de build.py. Ejecutar: py -I tools/generic_foods/test_build.py"""
import csv
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build  # noqa: E402


def write_csv(path, header, rows):
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, quoting=csv.QUOTE_ALL)
        w.writerow(header)
        w.writerows(rows)


class BuildTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.usda = self.tmp / "usda"
        self.usda.mkdir()
        write_csv(self.usda / "food.csv", ["fdc_id", "data_type", "description", "food_category_id", "publication_date"], [
            ["1", "sr_legacy_food", "Chicken, breast, roasted", "5", "2019"],
            ["2", "sr_legacy_food", "Water, tap", "14", "2019"],
        ])
        nut = []
        for fid, nid, amount in [
            ("1", "1008", "165"), ("1", "1003", "31.02"), ("1", "1005", "0"), ("1", "2000", "0"),
            ("1", "1004", "3.57"), ("1", "1258", "1.01"), ("1", "1079", "0"), ("1", "1093", "74"),
            ("2", "1008", "0"), ("2", "1093", "4"),
        ]:
            nut.append([str(len(nut) + 1), fid, nid, amount, "", "", "", "", "", "", ""])
        write_csv(self.usda / "food_nutrient.csv",
                  ["id", "fdc_id", "nutrient_id", "amount", "data_points", "derivation_id", "min", "max", "median", "footnote", "min_year_acquired"], nut)
        self.mapping = self.tmp / "foods_es.csv"
        write_csv(self.mapping, ["fdc_id", "nombre_es", "categoria", "descripcion_en"], [
            ["2", "Agua", "Bebidas", "Water, tap"],
            ["1", "Pechuga de pollo asada", "Carnes", "Chicken, breast, roasted"],
        ])

    def run_build(self):
        out = self.tmp / "out.json"
        build.build(self.usda, self.mapping, out)
        return json.loads(out.read_text(encoding="utf-8"))

    def test_values_and_salt(self):
        foods = {f["name"]: f for f in self.run_build()}
        chicken = foods["Pechuga de pollo asada"]
        self.assertEqual(165.0, chicken["kcal"])
        self.assertEqual(31.02, chicken["protein"])
        self.assertEqual(0.19, chicken["salt"])  # 74 mg × 2,5 / 1000 = 0,185 → 0,19
        self.assertFalse(chicken["incomplete"])
        self.assertEqual("Carnes", chicken["category"])

    def test_missing_nutrient_is_zero_and_incomplete(self):
        water = {f["name"]: f for f in self.run_build()}["Agua"]
        self.assertEqual(0.0, water["protein"])
        self.assertTrue(water["incomplete"])

    def test_sorted_and_stable(self):
        a = self.run_build()
        b = self.run_build()
        self.assertEqual(a, b)
        self.assertEqual(["Agua", "Pechuga de pollo asada"], [f["name"] for f in a])

    def test_sugar_fallback_to_nlea(self):
        rows = list(csv.reader(open(self.usda / "food_nutrient.csv", encoding="utf-8")))
        rows = [r for r in rows if not (r[1] == "1" and r[2] == "2000")]
        rows.append(["99", "1", "1063", "2.5", "", "", "", "", "", "", ""])
        write_csv(self.usda / "food_nutrient.csv", rows[0], rows[1:])
        chicken = {f["name"]: f for f in self.run_build()}["Pechuga de pollo asada"]
        self.assertEqual(2.5, chicken["sugars"])
        self.assertFalse(chicken["incomplete"])


if __name__ == "__main__":
    unittest.main()
