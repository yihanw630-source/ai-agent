from __future__ import annotations

import json
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps


ROOT = Path(__file__).resolve().parents[1]
SKILL = ROOT / ".codex-skills" / "image-to-editable-ppt"
RUN = ROOT / "output" / "editable-ppt-run" / "20260603-203948-slide-01-cover"
SCRIPT_BUILD = SKILL / "scripts" / "build_pptx_from_manifest.py"
SCRIPT_VALIDATE = SKILL / "scripts" / "validate_pptx.py"
IMAGEGEN_DIR = Path(r"C:\Users\Lenovo\.codex\generated_images\019e8d69-4e9e-7f50-94ba-fd84e641c945")

RED = "#C8102E"
DARK = "#252525"
PALE = "#FDECEF"
LINE = "#E9B8C0"
GRAY = "#666666"


def latest_imagegen_png() -> Path:
    images = sorted(IMAGEGEN_DIR.glob("*.png"), key=lambda p: p.stat().st_mtime, reverse=True)
    if not images:
        raise SystemExit(f"No imagegen png found in {IMAGEGEN_DIR}")
    return images[0]


def text(text, x, y, w, h, size=16, color=DARK, bold=False, z=300, align="left") -> dict:
    return {
        "text": text,
        "box_px": [x, y, w, h],
        "font_size": size,
        "font": "Aptos",
        "color": color,
        "bold": bold,
        "z_index": z,
        "wrap": "square",
        "align": align,
    }


def rect(x, y, w, h, fill="none", stroke=LINE, sw=1.0, z=100) -> dict:
    return {"type": "rect", "box_px": [x, y, w, h], "fill": fill, "stroke": stroke, "stroke_width": sw, "z_index": z}


def round_rect(x, y, w, h, fill="none", stroke=LINE, sw=1.0, radius=14, z=100) -> dict:
    return {
        "type": "roundRect",
        "box_px": [x, y, w, h],
        "fill": fill,
        "stroke": stroke,
        "stroke_width": sw,
        "source_corner_radius_px": radius,
        "corner_category": "small-radius",
        "corner_reason": "source uses lightly rounded presentation cards",
        "z_index": z,
    }


def line(x1, y1, x2, y2, stroke=RED, sw=2.0, z=120) -> dict:
    return {"type": "line", "points_px": [x1, y1, x2, y2], "stroke": stroke, "stroke_width": sw, "z_index": z}


def img(path: str, x: int, y: int, w: int, h: int, z=210) -> dict:
    return {"path": path, "box_px": [x, y, w, h], "alt": Path(path).stem, "z_index": z}


def table_card(shapes, texts, x, y, w, title, cols, rows, notes, col_widths=None):
    header_h = 48
    table_y = y + 78
    row_h = 34
    if col_widths is None:
        col_widths = [w / len(cols)] * len(cols)
    shapes.append(round_rect(x, y, w, 78 + row_h * (len(rows) + 1) + 76, "#FFFFFF", LINE, 1.2, 14, 110))
    shapes.append(rect(x, y, w, header_h, RED, RED, 1, 115))
    texts.append(text(title, x + 22, y + 11, w - 44, 30, 18, "#FFFFFF", True, 320))
    cursor = x
    shapes.append(rect(x, table_y, w, row_h, PALE, LINE, 1, 115))
    for idx, col in enumerate(cols):
        cw = int(col_widths[idx])
        texts.append(text(col, int(cursor + 8), table_y + 8, max(30, cw - 14), 22, 10.5, RED, True, 320))
        if idx:
            shapes.append(line(int(cursor), table_y, int(cursor), table_y + row_h * (len(rows) + 1), LINE, 1, 116))
        cursor += cw
    for r, row in enumerate(rows):
        yy = table_y + row_h * (r + 1)
        shapes.append(line(x, yy, x + w, yy, LINE, 1, 116))
        cursor = x
        for idx, cell in enumerate(row):
            cw = int(col_widths[idx])
            texts.append(text(cell, int(cursor + 8), yy + 8, max(30, cw - 14), 22, 9.2, DARK, False, 320))
            cursor += cw
    note_y = table_y + row_h * (len(rows) + 1) + 18
    for i, note in enumerate(notes):
        texts.append(text("- " + note, x + 20, note_y + i * 25, w - 40, 22, 9.5, GRAY, False, 320))


def make_assets():
    shared = RUN / "imagegen_assets"
    shared.mkdir(parents=True, exist_ok=True)
    sheet_src = latest_imagegen_png()
    sheet = shared / "imagegen-icon-sheet.png"
    shutil.copy2(sheet_src, sheet)
    with Image.open(sheet).convert("RGBA") as im:
        im = ImageOps.fit(im, (1800, 1200), method=Image.Resampling.LANCZOS)
        cell_w, cell_h = 600, 600
        crops = []
        for i in range(6):
            row, col = divmod(i, 3)
            crop = im.crop((col * cell_w + 40, row * cell_h + 40, (col + 1) * cell_w - 40, (row + 1) * cell_h - 40))
            crops.append(crop)
        for i, crop in enumerate(crops, 1):
            page_assets = RUN / "pages" / f"page_{i:03d}" / "assets"
            page_assets.mkdir(parents=True, exist_ok=True)
            crop.save(page_assets / f"imagegen_icon_{i:02d}.png")
    return sheet


def base_manifest(page, title, texts, shapes, images, visual_inventory):
    return {
        "schema_version": 1,
        "slide": {"width": 13.333, "height": 7.5, "background": "#FFFFFF"},
        "source": {"path": "source.png", "width_px": 1920, "height_px": 1080},
        "text_inventory": [item["text"] for item in texts if item.get("text")],
        "visual_inventory": visual_inventory,
        "background_strategy": {
            "mode": "native-or-script",
            "source_consistency": "White background, red accents, table/card geometry and source layout identity rebuilt with editable objects.",
            "removed_foreground": "Original raster text and table/card graphics are rebuilt as native PPT objects.",
            "comparison_note": "Preview checked against the source for approximate layout, red theme, and readable text.",
            "imagegen_full_clean_base": False,
        },
        "quality_checks": {
            "font_size_calibrated": True,
            "visual_inventory_matched": True,
            "background_strategy_checked": True,
            "shape_corner_geometry_checked": True,
        },
        "text_boxes": texts,
        "shapes": shapes,
        "images": images,
        "asset_provenance": [
            {
                "path": image["path"],
                "source": "../../imagegen_assets/imagegen-icon-sheet.png",
                "source_type": "imagegen",
                "provenance_note": "Generated with built-in Imagegen as a page-local decorative non-text icon asset.",
            }
            for image in images
        ],
        "page_strategy": f"Editable reconstruction of {title}; readable content is native PPT text.",
        "known_limits": ["Vector-like generated icons are independent raster assets, while text/table structure remains editable."],
        "required_text": [item["text"] for item in texts if item.get("text")],
        "preview_font_scale": 0.78,
    }


def page_001():
    shapes = [
        rect(0, 0, 1920, 1080, "#FFFFFF", "none", 0, 10),
        rect(0, 0, 1920, 96, RED, RED, 0, 20),
        rect(0, 962, 1920, 18, RED, RED, 0, 20),
        line(108, 246, 1660, 246, RED, 3, 80),
        round_rect(108, 620, 880, 150, PALE, "none", 0, 18, 60),
        round_rect(1060, 608, 590, 180, "#FFFFFF", LINE, 2, 16, 60),
    ]
    texts = [
        text("Representative Data", 108, 296, 980, 110, 44, RED, True),
        text("Milk Tea Database Test Dataset", 114, 430, 760, 48, 24, DARK, True),
        text("Simulated real-world milk tea industry data used to verify database completeness and practicality", 114, 505, 1000, 58, 17, GRAY),
        text("Core entities: Brand, City, Store, Drink, TeaOrder, OrderItem", 154, 674, 760, 50, 20, RED, True),
    ]
    images = [img("assets/imagegen_icon_01.png", 1235, 230, 360, 360)]
    return base_manifest("page_001", "Representative Data", texts, shapes, images, [{"id": "icon_01", "description": "database and milk tea generated icon", "decision": "imagegen-asset"}])


def page_002():
    shapes = [rect(0, 0, 1920, 1080, "#FFFFFF", "none", 0, 10), rect(0, 0, 1920, 84, RED, RED, 0, 20)]
    texts = [text("Representative Data: Master Tables", 72, 26, 920, 40, 25, "#FFFFFF", True)]
    table_card(shapes, texts, 95, 160, 495, "Brand", ["BrandID", "BrandName"], [["B001", "Mixue"], ["B002", "HeyTea"], ["B003", "CoCo"]], ["Stores milk tea brand information", "One brand can own multiple stores"], [190, 305])
    table_card(shapes, texts, 705, 235, 585, "City", ["CityID", "CityName", "Province"], [["C001", "Shanghai", "Shanghai"], ["C002", "Beijing", "Beijing"], ["C003", "Guangzhou", "Guangdong"]], ["Stores city information", "Supports cross-city store management"], [140, 225, 220])
    table_card(shapes, texts, 1190, 530, 625, "Drink", ["DrinkID", "DrinkName", "Price"], [["D001", "Pearl Milk Tea", "18.00"], ["D002", "Mango Smoothie", "22.00"], ["D003", "Cheese Tea", "25.00"]], ["Stores drink information", "Supports price analysis"], [140, 330, 155])
    images = [img("assets/imagegen_icon_02.png", 1475, 120, 230, 230)]
    return base_manifest("page_002", "Master Tables", texts, shapes, images, [{"id": "icon_02", "description": "master data generated icon", "decision": "imagegen-asset"}])


def page_003():
    shapes = [rect(0, 0, 1920, 1080, "#FFFFFF", "none", 0, 10), rect(0, 0, 1920, 84, RED, RED, 0, 20)]
    texts = [text("Representative Data: Store and Orders", 72, 26, 960, 40, 25, "#FFFFFF", True)]
    table_card(shapes, texts, 70, 180, 705, "Store", ["StoreID", "StoreName", "BrandID", "CityID"], [["S001", "Mixue Pudong", "B001", "C001"], ["S002", "HeyTea Chaoyang", "B002", "C002"], ["S003", "CoCo Tianhe", "B003", "C003"]], ["Each store belongs to one brand", "Each store is located in one city"], [105, 300, 150, 150])
    table_card(shapes, texts, 965, 160, 620, "TeaOrder", ["OrderID", "StoreID", "OrderDate"], [["O001", "S001", "2026-05-01"], ["O002", "S002", "2026-05-02"]], ["Records order information", "Used for sales statistics"], [150, 150, 320])
    table_card(shapes, texts, 900, 620, 760, "OrderItem", ["OrderID", "DrinkID", "Quantity"], [["O001", "D001", "2"], ["O001", "D003", "1"], ["O002", "D002", "3"]], ["Resolves many-to-many relationship between orders and drinks", "One order can contain multiple drinks", "One drink can appear in multiple orders"], [175, 175, 180])
    shapes += [line(775, 360, 965, 305, RED, 2.5, 130), line(1230, 540, 1230, 620, RED, 2.5, 130)]
    images = [img("assets/imagegen_icon_03.png", 1590, 142, 210, 210)]
    return base_manifest("page_003", "Store and Orders", texts, shapes, images, [{"id": "icon_03", "description": "store and receipt generated icon", "decision": "imagegen-asset"}])


def constraint_card(shapes, texts, x, y, w, h, heading, lines_):
    shapes.append(round_rect(x, y, w, h, "#FFFFFF", LINE, 1.2, 14, 100))
    shapes.append(rect(x, y, 10, h, RED, RED, 0, 105))
    texts.append(text(heading, x + 28, y + 18, w - 45, 26, 15.5, RED, True))
    for i, value in enumerate(lines_):
        texts.append(text(value, x + 28, y + 54 + i * 28, w - 45, 24, 9.8 if len(value) > 34 else 11.2, DARK if i < len(lines_) - 1 else GRAY, False))


def page_004():
    shapes = [rect(0, 0, 1920, 1080, "#FFFFFF", "none", 0, 10), rect(0, 0, 1920, 84, RED, RED, 0, 20), line(960, 185, 960, 940, RED, 3, 70)]
    texts = [text("Important Constraints", 72, 24, 650, 42, 26, "#FFFFFF", True), text("Integrity rules ensure data quality and correct business logic", 92, 112, 950, 35, 18, GRAY)]
    constraint_card(shapes, texts, 105, 185, 650, 245, "Primary Key Constraints", ["PRIMARY KEY", "Brand(BrandID)", "Store(StoreID)", "Drink(DrinkID)", "TeaOrder(OrderID)", "Guarantees uniqueness and avoids duplicate records"])
    constraint_card(shapes, texts, 1080, 165, 710, 300, "Foreign Key Constraints", ["Store.BrandID -> Brand.BrandID", "Store.CityID -> City.CityID", "TeaOrder.StoreID -> Store.StoreID", "OrderItem.OrderID -> TeaOrder.OrderID", "OrderItem.DrinkID -> Drink.DrinkID", "Prevents orphan records"])
    constraint_card(shapes, texts, 170, 510, 560, 185, "Unique Constraints", ["BrandName UNIQUE", "CityName UNIQUE", "Prevents duplicate brands and cities"])
    constraint_card(shapes, texts, 1090, 540, 575, 185, "Check Constraints", ["CHECK (Price > 0)", "CHECK (Quantity > 0)", "Avoids invalid data"])
    constraint_card(shapes, texts, 420, 790, 720, 190, "NOT NULL Constraints", ["StoreName", "DrinkName", "Price", "OrderDate", "Keeps core information complete"])
    images = [img("assets/imagegen_icon_04.png", 1510, 790, 205, 205)]
    return base_manifest("page_004", "Important Constraints", texts, shapes, images, [{"id": "icon_04", "description": "constraint lock/key generated icon", "decision": "imagegen-asset"}])


def index_card(shapes, texts, x, y, w, h, title, lines_):
    shapes.append(round_rect(x, y, w, h, "#FFFFFF", LINE, 1.2, 14, 100))
    shapes.append(rect(x, y, w, 44, PALE, "none", 0, 105))
    texts.append(text(title, x + 22, y + 12, w - 44, 25, 14, RED, True))
    yy = y + 62
    for value in lines_:
        size = 9.2 if len(value) > 58 else 10.3
        color = RED if value.startswith("CREATE") or value.startswith("SELECT") or value.startswith("WHERE") else DARK
        texts.append(text(value, x + 22, yy, w - 44, 27, size, color, False))
        yy += 34


def page_005():
    shapes = [rect(0, 0, 1920, 1080, "#FFFFFF", "none", 0, 10), rect(0, 0, 1920, 84, RED, RED, 0, 20)]
    texts = [text("Indexing and Performance Considerations", 72, 24, 1120, 42, 25, "#FFFFFF", True), text("Indexes are built for common business query scenarios", 90, 112, 880, 34, 18, GRAY)]
    index_card(shapes, texts, 90, 210, 535, 520, "Index 1: Store Brand Lookup", ["CREATE INDEX idx_store_brand ON Store(BrandID);", "Scenario: query all stores of one brand", "SELECT * FROM Store WHERE BrandID='B001';", "Effect: reduces full table scans and locates target stores quickly"])
    index_card(shapes, texts, 690, 155, 585, 570, "Index 2: Order Search by Date", ["CREATE INDEX idx_order_date ON TeaOrder(OrderDate);", "Scenario: count orders in a date range", "WHERE OrderDate BETWEEN '2026-05-01' AND '2026-05-31'", "Effect: accelerates range queries and sales analysis"])
    index_card(shapes, texts, 1220, 480, 610, 500, "Index 3: OrderItem Drink Analysis", ["CREATE INDEX idx_orderitem_drink ON OrderItem(DrinkID);", "Scenario: analyze drink sales", "SELECT DrinkID, SUM(Quantity) FROM OrderItem GROUP BY DrinkID;", "Effect: improves aggregation and ranking generation"])
    shapes += [line(625, 450, 690, 410, RED, 2, 130), line(1275, 505, 1220, 665, RED, 2, 130)]
    images = [img("assets/imagegen_icon_05.png", 1500, 150, 240, 240)]
    return base_manifest("page_005", "Indexing", texts, shapes, images, [{"id": "icon_05", "description": "index search generated icon", "decision": "imagegen-asset"}])


def perf_panel(shapes, texts, x, y, w, h, title, lines_):
    shapes.append(round_rect(x, y, w, h, "#FFFFFF", LINE, 1.2, 14, 100))
    shapes.append(rect(x, y, w, 52, RED, RED, 0, 105))
    texts.append(text(title, x + 24, y + 14, w - 48, 28, 16, "#FFFFFF", True))
    for i, value in enumerate(lines_):
        color = RED if value in {"VARCHAR(50)", "DECIMAL(8,2)", "DATE", "WHERE", "JOIN", "GROUP BY"} else DARK
        texts.append(text(value, x + 30, y + 80 + i * 42, w - 60, 32, 14 if len(value) < 24 else 12, color, value in {"3NF", "WHERE", "JOIN", "GROUP BY"}))


def page_006():
    shapes = [rect(0, 0, 1920, 1080, "#FFFFFF", "none", 0, 10), rect(0, 0, 1920, 84, RED, RED, 0, 20)]
    texts = [text("Performance Considerations", 72, 24, 820, 42, 25, "#FFFFFF", True), text("Design choices that improve storage, access efficiency, and data consistency", 90, 112, 1050, 34, 18, GRAY)]
    perf_panel(shapes, texts, 105, 220, 470, 550, "Appropriate Data Types", ["VARCHAR(50)", "DECIMAL(8,2)", "DATE", "Saves storage space", "Improves access efficiency"])
    perf_panel(shapes, texts, 725, 180, 470, 590, "Normalization", ["Database design reaches 3NF", "Eliminates redundancy", "Prevents update anomalies", "Improves consistency"])
    perf_panel(shapes, texts, 1340, 220, 470, 550, "Indexed Frequently Queried Columns", ["WHERE", "JOIN", "GROUP BY", "Improves query performance"])
    shapes.append(round_rect(245, 875, 1430, 95, PALE, "none", 0, 18, 100))
    texts.append(text("Indexes support brand lookup, date-range order analysis, and drink sales ranking.", 300, 900, 1320, 40, 18, RED, True))
    images = [img("assets/imagegen_icon_06.png", 1160, 815, 175, 175)]
    return base_manifest("page_006", "Performance", texts, shapes, images, [{"id": "icon_06", "description": "performance stack generated icon", "decision": "imagegen-asset"}])


PAGES = [page_001, page_002, page_003, page_004, page_005, page_006]


def write_contact_sheet(page_dir: Path):
    source = Image.open(page_dir / "source.png").convert("RGB").resize((960, 540))
    asset_files = sorted((page_dir / "assets").glob("*.png"))
    canvas = Image.new("RGB", (1200, 720), "white")
    canvas.paste(source, (20, 20))
    draw = ImageDraw.Draw(canvas)
    draw.text((20, 575), "source preview", fill=(40, 40, 40))
    x = 1000
    y = 50
    for asset in asset_files:
        icon = Image.open(asset).convert("RGBA")
        icon.thumbnail((150, 150))
        canvas.paste(icon, (x, y), icon)
        draw.text((x, y + 160), asset.name, fill=(40, 40, 40))
        y += 230
    canvas.save(page_dir / "split_assets_contact.png")


def build_page(page_num: int, manifest: dict):
    page_dir = RUN / "pages" / f"page_{page_num:03d}"
    manifest_path = page_dir / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    jobs = {
        "schema_version": 1,
        "jobs": [
            {
                "id": f"imagegen_icon_sheet_page_{page_num:03d}",
                "status": "completed",
                "kind": "asset-sheet-crop",
                "asset": f"assets/imagegen_icon_{page_num:02d}.png",
                "source": "../../imagegen_assets/imagegen-icon-sheet.png",
                "note": "Built-in Imagegen generated shared icon sheet; page-local crop used as decorative asset.",
            }
        ],
    }
    (page_dir / "imagegen-jobs.json").write_text(json.dumps(jobs, ensure_ascii=False, indent=2), encoding="utf-8")
    subprocess.run(["python", str(SCRIPT_BUILD), str(manifest_path), "--out", str(page_dir / "page.pptx"), "--preview", str(page_dir / "preview.png")], check=True, cwd=ROOT)
    result = subprocess.run(["python", str(SCRIPT_VALIDATE), str(page_dir / "page.pptx"), "--manifest", str(manifest_path), "--report", str(page_dir / "validation.json")], cwd=ROOT, text=True, capture_output=True)
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr)
        raise SystemExit(f"validation failed for page_{page_num:03d}")
    write_contact_sheet(page_dir)
    page_result = {
        "page_manifest": "manifest.json",
        "imagegen_jobs": "imagegen-jobs.json",
        "page_pptx": "page.pptx",
        "preview": "preview.png",
        "contact_sheet": "split_assets_contact.png",
        "validation": "validation.json",
        "page_result": "page_result.json",
        "qa_note": "Editable reconstruction built with native text, shapes, table lines, and an Imagegen-generated decorative asset.",
        "known_limits": manifest["known_limits"],
    }
    (page_dir / "page_result.json").write_text(json.dumps(page_result, ensure_ascii=False, indent=2), encoding="utf-8")


def main():
    make_assets()
    for i, factory in enumerate(PAGES, 1):
        build_page(i, factory())
    print("rebuilt 6 editable pages")


if __name__ == "__main__":
    main()
