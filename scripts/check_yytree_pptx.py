from pathlib import Path
from zipfile import ZipFile
from lxml import etree
from PIL import Image, ImageChops, ImageStat

ns = {"a":"http://schemas.openxmlformats.org/drawingml/2006/main", "p":"http://schemas.openxmlformats.org/presentationml/2006/main"}
for i in range(1,11):
    root = Path("temp/yytree-pdf-vector")
    with ZipFile(root / f"{i}.pptx") as archive:
        assert not any(n.startswith("ppt/media/") for n in archive.namelist())
        slide = etree.fromstring(archive.read("ppt/slides/slide1.xml"))
        texts = slide.xpath("//a:t/text()",namespaces=ns)
        assert "test 中文" in texts and "中文" in texts and "English" in texts
        for position in ("left","right","top","bottom","topleft","topright","bottomleft","bottomright"):
            assert "Node-"+position in texts
        assert not slide.xpath("//p:pic",namespaces=ns)
    with Image.open(root/f"{i}-pptx.png") as a, Image.open(root/f"{i}.png") as b:
        a,b=a.convert("RGB"),b.convert("RGB")
        assert a.size == b.size
        error=sum(ImageStat.Stat(ImageChops.difference(a,b)).mean)/765
        print(i,error)
        assert error < .03,(i,error)
        for x,y in ((.175,.875),(.325,.875),(.175,.925),(.325,.925),(.85,.85)):
            p=int(x*a.width),int(y*a.height)
            assert max(abs(c-d) for c,d in zip(a.getpixel(p),b.getpixel(p)))<8,(i,p)
