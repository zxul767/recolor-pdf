#!/usr/bin/env python3
import argparse
import json
import re
from pathlib import Path

from tqdm import tqdm
import pymupdf

RGBColor = tuple[float, float, float]


def hex_to_rgb_normalized(hex_color: str) -> RGBColor:
    """
    Converts a hex color string (e.g., '#RRGGBB') to a normalized RGB tuple (0.0-1.0).
    """
    hex_color = hex_color.lstrip("#")
    if len(hex_color) != 6:
        raise ValueError("Invalid hex color format. Expected #RRGGBB.")

    r = int(hex_color[0:2], 16) / 255.0
    g = int(hex_color[2:4], 16) / 255.0
    b = int(hex_color[4:6], 16) / 255.0
    return (round(r, 4), round(g, 4), round(b, 4))


# <number> <number> <number> (rg|RG)
COLOR_COMMAND_PATTERN = re.compile(
    r"(-?\d+\.?\d*)\s+(-?\d+\.?\d*)\s+(-?\d+\.?\d*)\s+(rg|RG)",
    re.IGNORECASE,
)


def change_text_color(
    input_pdf_path: Path,
    output_pdf_path: Path,
    color_change_requests: dict[RGBColor, RGBColor],
) -> None:
    """
    Changes the color of text based on a provided color map by modifying the page's
    content stream directly. This method avoids potential layout issues from re-writing text.

    This version uses a more flexible regex and a custom replacement function to handle
    both integer (0-255) and floating-point (0.0-1.0) color specifications.
    """

    # to be used by re.sub to check and replace colors
    def recolor_text(match):
        """
        This function is the heart of the color replacement. It checks if the matched
        color values (as floats) are close enough to one of the keys in the color_map.
        """
        r_matched, g_matched, b_matched = (
            float(match.group(1)),
            float(match.group(2)),
            float(match.group(3)),
        )

        # The PDF spec allows colors to be specified as 0-255 or 0.0-1.0
        # We normalize the matched values to the 0.0-1.0 range for a consistent comparison
        if r_matched > 1.0 or g_matched > 1.0 or b_matched > 1.0:
            r_normalized = r_matched / 255.0
            g_normalized = g_matched / 255.0
            b_normalized = b_matched / 255.0
        else:
            r_normalized = r_matched
            g_normalized = g_matched
            b_normalized = b_matched

        # Use a small tolerance for floating-point comparisons
        tolerance = 0.005

        # Check if the matched color is one of the keys in our color map
        for old_color, new_color in color_change_requests.items():
            if (
                abs(r_normalized - old_color[0]) < tolerance
                and abs(g_normalized - old_color[1]) < tolerance
                and abs(b_normalized - old_color[2]) < tolerance
            ):
                # If it's a match, return the new color string
                new_color_string = (
                    f"{new_color[0]:.4f} {new_color[1]:.4f} {new_color[2]:.4f} rg"
                )
                return new_color_string

        # If there is no match, leave the original string untouched
        return match.group(0)

    try:
        with pymupdf.open(input_pdf_path) as doc:
            for page in tqdm(doc):
                # PyMuPDF stores the content streams in xrefs, one or more per page.
                for xref in page.get_contents():
                    # Get the content stream as bytes
                    content_bytes = doc.xref_stream(xref)
                    content_str = content_bytes.decode("latin1")
                    # Use the recolor_text function to perform the replacement
                    new_content_str = COLOR_COMMAND_PATTERN.sub(
                        recolor_text, content_str
                    )
                    # If a change was made, update this specific content stream using its xref
                    if new_content_str != content_str:
                        doc.update_stream(xref, new_content_str.encode("latin1"))

            doc.save(output_pdf_path)
            print(f"Successfully saved the modified PDF to: {output_pdf_path}")

    except FileNotFoundError:
        print(f"Error: The file at '{input_pdf_path}' was not found.")


def existing_pdf(path_str: str) -> Path:
    path = Path(path_str)
    if not path.is_file():
        raise argparse.ArgumentTypeError(f"Input file does not exist: {path}")
    return path


def writable_pdf(path_str: str) -> Path:
    path = Path(path_str)
    if not path.parent.exists():
        raise argparse.ArgumentTypeError(
            f"Output directory does not exist: {path.parent}"
        )
    return path


def load_color_change_requests(colors_file_path: Path) -> dict[RGBColor, RGBColor]:
    color_change_requests: dict[RGBColor, RGBColor] = {}
    try:
        with open(colors_file_path, "r") as f:
            color_data = json.load(f)
            for item in color_data:
                target_hex = item["target"]
                replacement_hex = item["replacement"]
                color_change_requests[hex_to_rgb_normalized(target_hex)] = (
                    hex_to_rgb_normalized(replacement_hex)
                )
    except FileNotFoundError:
        print(f"Error: Colors file '{colors_file_path}' not found.")
        exit(1)
    except json.JSONDecodeError:
        print(f"Error: Invalid JSON format in '{colors_file_path}'.")
        exit(1)
    except ValueError as e:
        print(f"Error in color conversion: {e}")
        exit(1)
    return color_change_requests


def main() -> None:
    parser = argparse.ArgumentParser(description="Change text color in a PDF.")
    parser.add_argument(
        "input_pdf", type=existing_pdf, help="Path to the input PDF file"
    )
    parser.add_argument(
        "output_pdf", type=writable_pdf, help="Path to the output PDF file"
    )
    parser.add_argument(
        "--colors_file",
        type=existing_pdf,
        default="colors.json",
        help="Path to the JSON file containing color mappings (default: colors.json)",
    )
    args = parser.parse_args()

    color_change_requests = load_color_change_requests(args.colors_file)

    change_text_color(
        args.input_pdf, args.output_pdf, color_change_requests=color_change_requests
    )


def normalize_color(rgb_color: tuple[int, int, int]) -> tuple[float, float, float]:
    def _normalize(value):
        return round(value / 255.0, 4)

    return (
        _normalize(rgb_color[0]),
        _normalize(rgb_color[1]),
        _normalize(rgb_color[2]),
    )


if __name__ == "__main__":
    main()
