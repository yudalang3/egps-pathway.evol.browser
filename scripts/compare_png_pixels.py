"""Compare two PNG images pixel-by-pixel and report deterministic metrics as JSON."""
import hashlib
import json
import sys
from pathlib import Path
from PIL import Image, ImageChops, ImageStat


def md5(path: Path) -> str:
    return hashlib.md5(path.read_bytes()).hexdigest()


def main() -> None:
    if len(sys.argv) < 3:
        sys.exit("Usage: compare_png_pixels.py <actual.png> <reference.png> [probe_x probe_y]")
    actual_path = Path(sys.argv[1])
    ref_path = Path(sys.argv[2])
    probe = None
    if len(sys.argv) >= 5:
        probe = (int(sys.argv[3]), int(sys.argv[4]))

    with Image.open(actual_path) as a_img, Image.open(ref_path) as b_img:
        a = a_img.convert("RGB")
        b = b_img.convert("RGB")
        if a.size != b.size:
            raise AssertionError(f"Dimension mismatch: actual={a.size} vs reference={b.size}")
        diff = ImageChops.difference(a, b)
        stat = ImageStat.Stat(diff)
        mean_abs_diff = sum(stat.mean) / (3.0 * 255.0)
        max_channel_diff = max(hi for _, hi in diff.getextrema())
        result = {
            "actual": str(actual_path),
            "reference": str(ref_path),
            "width": a.size[0],
            "height": a.size[1],
            "md5_equal": md5(actual_path) == md5(ref_path),
            "mean_abs_diff": mean_abs_diff,
            "max_channel_diff": int(max_channel_diff),
        }
        if probe is not None:
            result["probe"] = {
                "x": probe[0],
                "y": probe[1],
                "actual_rgb": list(a.getpixel(probe)),
                "reference_rgb": list(b.getpixel(probe)),
            }
    print(json.dumps(result))


if __name__ == "__main__":
    main()
