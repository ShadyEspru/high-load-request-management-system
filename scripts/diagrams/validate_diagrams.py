#!/usr/bin/env python3
"""Validate the generated HLRMS diagram delivery package."""

from __future__ import annotations

import argparse
from pathlib import Path
from xml.etree import ElementTree as ET

from PIL import Image
from pypdf import PdfReader

from generate_diagrams import HEIGHT, WIDTH, build_diagrams


PDF_WIDTH = WIDTH * 72 / 96
PDF_HEIGHT = HEIGHT * 72 / 96


def close_enough(actual: float, expected: float, tolerance: float = 0.5) -> bool:
    return abs(actual - expected) <= tolerance


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()

    diagram_root = args.root / "diagrams"
    expected_keys = {diagram.key for diagram in build_diagrams()}
    expected_files = {
        diagram_root / f"{key}.{extension}"
        for key in expected_keys
        for extension in ("drawio", "png", "pdf")
    }
    actual_files = {
        path
        for extension in ("drawio", "png", "pdf")
        for path in diagram_root.rglob(f"*.{extension}")
    }

    errors: list[str] = []
    for path in sorted(expected_files - actual_files):
        errors.append(f"missing: {path.relative_to(args.root)}")
    for path in sorted(actual_files - expected_files):
        errors.append(f"unexpected: {path.relative_to(args.root)}")

    for key in sorted(expected_keys):
        base = diagram_root / key
        drawio_path = base.with_suffix(".drawio")
        png_path = base.with_suffix(".png")
        pdf_path = base.with_suffix(".pdf")
        if not all(path.exists() for path in (drawio_path, png_path, pdf_path)):
            continue

        try:
            root = ET.parse(drawio_path).getroot()
            cells = root.findall(".//mxCell")
            if root.tag != "mxfile" or len(cells) < 3:
                errors.append(f"invalid drawio structure: {drawio_path.relative_to(args.root)}")
        except ET.ParseError as error:
            errors.append(f"invalid XML: {drawio_path.relative_to(args.root)} ({error})")

        try:
            with Image.open(png_path) as image:
                if image.size != (WIDTH, HEIGHT):
                    errors.append(
                        f"wrong PNG size: {png_path.relative_to(args.root)} "
                        f"is {image.size[0]}x{image.size[1]}"
                    )
        except OSError as error:
            errors.append(f"invalid PNG: {png_path.relative_to(args.root)} ({error})")

        try:
            reader = PdfReader(pdf_path)
            if len(reader.pages) != 1:
                errors.append(
                    f"wrong PDF page count: {pdf_path.relative_to(args.root)} "
                    f"has {len(reader.pages)} pages"
                )
            else:
                box = reader.pages[0].mediabox
                page_width = float(box.width)
                page_height = float(box.height)
                if not (
                    close_enough(page_width, PDF_WIDTH)
                    and close_enough(page_height, PDF_HEIGHT)
                ):
                    errors.append(
                        f"wrong PDF size: {pdf_path.relative_to(args.root)} "
                        f"is {page_width:.1f}x{page_height:.1f} pt"
                    )
        except Exception as error:  # pypdf raises several format-specific errors
            errors.append(f"invalid PDF: {pdf_path.relative_to(args.root)} ({error})")

    if errors:
        print("Diagram validation failed:")
        for error in errors:
            print(f"- {error}")
        raise SystemExit(1)

    print(
        f"OK: {len(expected_keys)} diagrams × 3 formats; "
        f"PNG {WIDTH}x{HEIGHT}; PDF {PDF_WIDTH:.0f}x{PDF_HEIGHT:.0f} pt; editable XML parsed."
    )


if __name__ == "__main__":
    main()
