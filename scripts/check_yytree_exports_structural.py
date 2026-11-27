"""Cross-platform structural (and optional pixel) verification for PDF, SVG, and PPTX exports."""
import argparse
import json
import shutil
import subprocess
from pathlib import Path
from zipfile import ZipFile
from lxml import etree
from PIL import Image, ImageChops, ImageStat
from pypdf import PdfReader
from pypdf.generic import ContentStream


POSITIONS = ("left", "right", "top", "bottom", "topleft", "topright", "bottomleft", "bottomright")
PPTX_NS = {
    "a": "http://schemas.openxmlformats.org/drawingml/2006/main",
    "p": "http://schemas.openxmlformats.org/presentationml/2006/main",
}


def inspect_pdf_stream(stream, resources, reader, seen):
    counts = {"path": 0, "text": 0}
    for _operands, operator in ContentStream(stream, reader).operations:
        assert operator != b"INLINE IMAGE", "Inline raster image found"
        counts["path"] += operator in (b"m", b"l", b"c", b"re")
        counts["text"] += operator in (b"Tj", b"TJ")
    for reference in resources.get("/Font", {}).values():
        font = reference.get_object()
        assert "/ToUnicode" in font, "Missing /ToUnicode mapping in PDF font"
        for descendant in font.get("/DescendantFonts", [font]):
            descriptor = descendant.get_object().get("/FontDescriptor")
            assert descriptor is not None, "Missing /FontDescriptor"
            assert any(k in descriptor for k in ("/FontFile", "/FontFile2", "/FontFile3")), (
                "Non-embedded PDF font found"
            )
    for reference in resources.get("/XObject", {}).values():
        obj = reference.get_object()
        assert obj["/Subtype"] != "/Image", "Raster XObject image found in PDF"
        if obj["/Subtype"] == "/Form" and reference.idnum not in seen:
            seen.add(reference.idnum)
            nested = inspect_pdf_stream(obj, obj.get("/Resources", resources), reader, seen)
            counts["path"] += nested["path"]
            counts["text"] += nested["text"]
    return counts


def check_pdf(root: Path, index: int, pdftoppm_bin: str | None, require_pixels: bool):
    pdf_path = root / f"{index}.pdf"
    ref_png = root / f"{index}.png"
    reader = PdfReader(pdf_path)
    page = reader.pages[0]
    counts = inspect_pdf_stream(page.get_contents(), page["/Resources"], reader, set())
    assert counts["path"] > 0 and counts["text"] > 0, (pdf_path, counts)
    extracted = page.extract_text() or ""
    assert "test 中文" in extracted, (pdf_path, "missing 'test 中文'", extracted[:200])
    assert "中文" in extracted and "English" in extracted, (pdf_path, "missing leaf labels")

    pixel_error = None
    if pdftoppm_bin:
        rendered_prefix = root / f"{index}-verified"
        subprocess.run(
            [pdftoppm_bin, "-r", "72", "-png", "-singlefile", str(pdf_path), str(rendered_prefix)],
            check=True,
        )
        with Image.open(rendered_prefix.with_suffix(".png")) as actual, Image.open(ref_png) as reference:
            actual, reference = actual.convert("RGB"), reference.convert("RGB")
            assert actual.size == reference.size, (pdf_path, actual.size, reference.size)
            pixel_error = sum(ImageStat.Stat(ImageChops.difference(actual, reference)).mean) / (3 * 255)
            assert pixel_error < 0.03, (pdf_path, "PDF/PNG visual difference", pixel_error)
            for x, y in ((0.175, 0.875), (0.325, 0.875), (0.175, 0.925), (0.325, 0.925), (0.85, 0.85)):
                pt = (int(x * actual.width), int(y * actual.height))
                diff = max(abs(a - b) for a, b in zip(actual.getpixel(pt), reference.getpixel(pt)))
                assert diff < 8, (pdf_path, pt, diff)
    elif require_pixels:
        raise RuntimeError("pdftoppm not found but --require-pdf-pixels was requested.")
    return {"counts": counts, "pixel_error": pixel_error}


def check_svg(root: Path, index: int):
    svg_path = root / f"{index}.svg"
    tree = etree.parse(str(svg_path))
    doc = tree.getroot()
    assert doc.tag.endswith("svg"), (svg_path, doc.tag)
    forbidden = doc.xpath("//*[local-name()='image' or local-name()='foreignObject']")
    assert not forbidden, (svg_path, "Found raster/foreignObject in SVG")
    shapes = doc.xpath("//*[local-name()='path' or local-name()='rect' or local-name()='polygon']")
    assert len(shapes) > 10, (svg_path, len(shapes))
    texts = ["".join(el.itertext()).strip() for el in doc.xpath("//*[local-name()='text']")]
    assert "test 中文" in texts and "中文" in texts and "English" in texts, (svg_path, texts[:20])
    return {"shape_count": len(shapes), "text_count": len(texts)}


def check_pptx(root: Path, index: int):
    pptx_path = root / f"{index}.pptx"
    with ZipFile(pptx_path) as archive:
        assert not any(n.startswith("ppt/media/") for n in archive.namelist()), (pptx_path, "media found")
        slide = etree.fromstring(archive.read("ppt/slides/slide1.xml"))
        assert not slide.xpath("//p:pic", namespaces=PPTX_NS), (pptx_path, "p:pic found")
        texts = slide.xpath("//a:t/text()", namespaces=PPTX_NS)
        assert "test 中文" in texts and "中文" in texts and "English" in texts, (pptx_path, texts[:20])
        for pos in POSITIONS:
            assert f"Node-{pos}" in texts, (pptx_path, f"missing Node-{pos}")
    return {"text_count": len(texts)}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", default="temp/yytree-pdf-vector")
    parser.add_argument("--pdftoppm", default="")
    parser.add_argument("--require-pdf-pixels", action="store_true")
    parser.add_argument("--report", default="")
    args = parser.parse_args()

    root = Path(args.dir)
    pdftoppm_bin = args.pdftoppm or shutil.which("pdftoppm")
    summary = {"dir": str(root), "pdftoppm": pdftoppm_bin, "cases": {}}
    for i in range(1, 11):
        pdf_res = check_pdf(root, i, pdftoppm_bin, args.require_pdf_pixels)
        svg_res = check_svg(root, i)
        pptx_res = check_pptx(root, i)
        summary["cases"][str(i)] = {"pdf": pdf_res, "svg": svg_res, "pptx": pptx_res}
        print(
            f"Case {i:2d}: PDF(paths={pdf_res['counts']['path']}, text={pdf_res['counts']['text']}, "
            f"px_err={pdf_res['pixel_error']}), SVG(shapes={svg_res['shape_count']}), "
            f"PPTX(texts={pptx_res['text_count']}) PASS"
        )
    if args.report:
        report_path = Path(args.report)
        report_path.parent.mkdir(parents=True, exist_ok=True)
        report_path.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print("All 10 PDF/SVG/PPTX structural checks passed.")


if __name__ == "__main__":
    main()
