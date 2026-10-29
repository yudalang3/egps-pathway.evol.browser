"""Check PDF vector content recursively, including nested Form resources."""
from pathlib import Path
import subprocess
from PIL import Image, ImageChops, ImageStat
from pypdf import PdfReader
from pypdf.generic import ContentStream


def inspect(stream, resources, reader, seen):
    counts = {"path": 0, "text": 0}
    for operands, operator in ContentStream(stream, reader).operations:
        assert operator != b"INLINE IMAGE", "Inline raster image found"
        counts["path"] += operator in (b"m", b"l", b"c", b"re")
        counts["text"] += operator in (b"Tj", b"TJ")
    for reference in resources.get("/Font", {}).values():
        font = reference.get_object()
        assert "/ToUnicode" in font, "Missing Unicode mapping"
        for descendant in font.get("/DescendantFonts", [font]):
            descriptor = descendant.get_object().get("/FontDescriptor")
            assert descriptor is not None
            assert any(key in descriptor for key in ("/FontFile", "/FontFile2", "/FontFile3"))
    for reference in resources.get("/XObject", {}).values():
        obj = reference.get_object()
        assert obj["/Subtype"] != "/Image", "Raster image found"
        if obj["/Subtype"] == "/Form" and reference.idnum not in seen:
            seen.add(reference.idnum)
            nested = inspect(obj, obj.get("/Resources", resources), reader, seen)
            for key in counts:
                counts[key] += nested[key]
    return counts


files = sorted(Path("temp/yytree-pdf-vector").glob("*.pdf"))
assert len(files) == 10
for path in files:
    reader = PdfReader(path)
    page = reader.pages[0]
    counts = inspect(page.get_contents(), page["/Resources"], reader, set())
    assert counts["path"] > 0 and counts["text"] > 0
    assert "test 中文" in page.extract_text()
    rendered = path.with_name(path.stem + "-verified")
    subprocess.run(["pdftoppm", "-r", "72", "-png", "-singlefile", str(path), str(rendered)], check=True)
    with Image.open(rendered.with_suffix(".png")) as actual, Image.open(path.with_suffix(".png")) as reference:
        actual, reference = actual.convert("RGB"), reference.convert("RGB")
        assert actual.size == reference.size
        error = sum(ImageStat.Stat(ImageChops.difference(actual, reference)).mean) / (3 * 255)
        assert error < .03, (path, "PDF/PNG visual difference", error)
        for x, y in ((.175,.875), (.325,.875), (.175,.925), (.325,.925), (.85,.85)):
            point = (int(x * actual.width), int(y * actual.height))
            assert max(abs(a-b) for a,b in zip(actual.getpixel(point), reference.getpixel(point))) < 8
    print(path.name, counts, "embedded Unicode fonts; zero raster images")
