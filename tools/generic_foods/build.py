"""Genera app/src/main/assets/generic_foods.json desde USDA FoodData Central (SR Legacy).

Uso:
  py -I tools/generic_foods/build.py --usda <carpeta CSV> --search "chicken breast"
  py -I tools/generic_foods/build.py --usda <carpeta CSV> --select tools/generic_foods/foods_query.csv --out tools/generic_foods/foods_es.csv
  py -I tools/generic_foods/build.py --usda <carpeta CSV> --mapping tools/generic_foods/foods_es.csv --out app/src/main/assets/generic_foods.json
"""
import argparse
import csv
import json
from pathlib import Path

# id de nutriente USDA → campo
NUTRIENTS = {
    "1008": "kcal",
    "1003": "protein",
    "1005": "carbs",
    "1004": "fat",
    "1258": "saturated",
    "1079": "fiber",
}
SUGARS = "2000"
SUGARS_NLEA = "1063"
SODIUM_MG = "1093"
FIELDS = ["kcal", "protein", "carbs", "sugars", "fat", "saturated", "fiber", "salt"]


def read_foods(usda: Path) -> dict:
    with open(usda / "food.csv", encoding="utf-8") as f:
        return {r["fdc_id"]: r["description"] for r in csv.DictReader(f)}


def read_nutrients(usda: Path, wanted: set) -> dict:
    ids = set(NUTRIENTS) | {SUGARS, SUGARS_NLEA, SODIUM_MG}
    out: dict = {}
    with open(usda / "food_nutrient.csv", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            if r["fdc_id"] in wanted and r["nutrient_id"] in ids and r["amount"] != "":
                out.setdefault(r["fdc_id"], {})[r["nutrient_id"]] = float(r["amount"])
    return out


def to_entry(name: str, category: str, n: dict) -> dict:
    e = {"name": name, "category": category}
    missing = False
    for nid, field in NUTRIENTS.items():
        if nid in n:
            e[field] = round(n[nid], 2)
        else:
            e[field] = 0.0
            missing = True
    sugars = n.get(SUGARS, n.get(SUGARS_NLEA))
    e["sugars"] = round(sugars, 2) if sugars is not None else 0.0
    missing |= sugars is None
    if SODIUM_MG in n:
        e["salt"] = round(n[SODIUM_MG] * 2.5 / 1000 + 1e-9, 2)
    else:
        e["salt"] = 0.0
        missing = True
    e["incomplete"] = missing
    return {k: e[k] for k in ["name", "category", *FIELDS, "incomplete"]}


def build(usda: Path, mapping: Path, out: Path) -> None:
    with open(mapping, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    nutrients = read_nutrients(usda, {r["fdc_id"] for r in rows})
    entries = [to_entry(r["nombre_es"], r["categoria"], nutrients.get(r["fdc_id"], {})) for r in rows]
    entries.sort(key=lambda e: e["name"].lower())
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")


def search(usda: Path, text: str) -> None:
    words = text.lower().split()
    for fid, desc in read_foods(usda).items():
        if all(w in desc.lower() for w in words):
            print(fid, desc)


def select(usda: Path, query: Path, out: Path) -> None:
    """Resuelve cada consulta (palabras que deben aparecer) a la descripción más corta que las contiene."""
    foods = read_foods(usda)
    resolved, failed = [], []
    with open(query, encoding="utf-8") as f:
        for r in csv.DictReader(f):
            must = [w.strip().lower() for w in r["incluye"].split(";") if w.strip()]
            avoid = [w.strip().lower() for w in r.get("excluye", "").split(";") if w.strip()]
            hits = [(fid, d) for fid, d in foods.items()
                    if all(w in d.lower() for w in must) and not any(w in d.lower() for w in avoid)]
            if not hits:
                failed.append(r["nombre_es"])
                continue
            fid, desc = min(hits, key=lambda h: (len(h[1]), h[1]))
            resolved.append({"fdc_id": fid, "nombre_es": r["nombre_es"], "categoria": r["categoria"], "descripcion_en": desc})
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["fdc_id", "nombre_es", "categoria", "descripcion_en"])
        w.writeheader()
        w.writerows(resolved)
    print(f"{len(resolved)} resueltos, {len(failed)} sin resultado")
    for name in failed:
        print("  SIN RESULTADO:", name)


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--usda", type=Path, required=True)
    p.add_argument("--search")
    p.add_argument("--select", type=Path)
    p.add_argument("--mapping", type=Path)
    p.add_argument("--out", type=Path)
    a = p.parse_args()
    if a.search:
        search(a.usda, a.search)
    elif a.select:
        select(a.usda, a.select, a.out)
    else:
        build(a.usda, a.mapping, a.out)


if __name__ == "__main__":
    main()
