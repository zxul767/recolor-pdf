package org.example;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceCmyk;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfLiteral;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.pdf.PdfObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Colors {
  static boolean isBlack(Color color) {
    float[] values = color.getColorValue(); // RGB/Grayscale/CMYK values in range [0, 1]
    if (color instanceof DeviceRgb) {
      float tolerance = 0.1f; // Allow up to 10% brightness deviation
      return values[0] <= tolerance && values[1] <= tolerance && values[2] <= tolerance;
    } else if (color instanceof DeviceGray) {
      float tolerance = 0.1f; // Allow up to 10% brightness deviation
      return values[0] <= tolerance; // Single component for grayscale
    } else if (color instanceof DeviceCmyk) {
      float tolerance = 0.1f; // Allow small deviations for pure black in CMYK
      return values[0] >= 0.9 && values[1] >= 0.9 && values[2] >= 0.9 && values[3] <= tolerance;
    }
    return false; // Unknown color space, assume not black
  }

  public static List<PdfObject> getColorEncoding(Color color) {
    List<PdfObject> list = new ArrayList<>();
    float[] values = color.getColorValue();
    if (color instanceof DeviceCmyk) {
      list.add(new PdfNumber(values[0]));
      list.add(new PdfNumber(values[1]));
      list.add(new PdfNumber(values[2]));
      list.add(new PdfNumber(values[3]));
      list.add(new PdfLiteral("k"));
    } else if (color instanceof DeviceGray) {
      list.add(new PdfNumber(values[0]));
      list.add(new PdfLiteral("g"));
    } else if (color instanceof DeviceRgb) {
      list.add(new PdfNumber(values[0]));
      list.add(new PdfNumber(values[1]));
      list.add(new PdfNumber(values[2]));
      list.add(new PdfLiteral("rg"));
    } else {
      throw new IllegalArgumentException("Unexpected kind of color: " + Arrays.toString(values));
    }
    return list;
  }

  public static List<PdfObject> blackAsCMYK() {
    return Arrays.asList(
        new PdfNumber(0),
        new PdfNumber(0),
        new PdfNumber(0),
        new PdfNumber(1),
        new PdfLiteral("k"));
  }

  public static List<PdfObject> blackAsGreyscale() {
    return Arrays.asList(new PdfNumber(0), new PdfLiteral("g"));
  }

  public static List<PdfObject> blackAsRGB() {
    return Arrays.asList(
        new PdfNumber(0), new PdfNumber(0), new PdfNumber(0), new PdfLiteral("rg"));
  }
}
