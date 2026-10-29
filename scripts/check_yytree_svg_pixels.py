from pathlib import Path
from PIL import Image, ImageChops, ImageStat

for i in range(1, 11):
    root = Path("temp/yytree-pdf-vector")
    with Image.open(root / f"{i}-svg.png") as a, Image.open(root / f"{i}.png") as b:
        a, b = a.convert("RGB"), b.convert("RGB")
        assert a.size == b.size
        error = sum(ImageStat.Stat(ImageChops.difference(a, b)).mean) / 765
        assert error < .03, (i, error)
        for x, y in ((.175,.875), (.325,.875), (.175,.925), (.325,.925), (.85,.85)):
            point = (int(x*a.width), int(y*a.height))
            assert max(abs(c-d) for c,d in zip(a.getpixel(point), b.getpixel(point))) < 8
        print(i, "SVG/PNG visual and heatmap samples passed", error)
