package org.example;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.pdf.PdfLiteral;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ColorReplacingCanvasEditor extends PdfCanvasEditor {
  private final Map<Color, Color> colorMap;
  private Color currentColor = null;
  private static final List<String> TEXT_SHOWING_OPERATORS =
      Arrays.asList(
          "Tj", "'", "\"", "TJ", "Tf", "Td", "TD", "Tm", "T*", "Tw", "Tc", "Tz", "TL", "Ts", "BT",
          "ET", "Tr");

  public ColorReplacingCanvasEditor(Map<Color, Color> colorMap) {
    this.colorMap = colorMap;
  }

  @Override
  protected void write(
      PdfCanvasProcessor processor, PdfLiteral operator, List<PdfObject> operands) {
    String operatorString = operator.toString();

    if (TEXT_SHOWING_OPERATORS.contains(operatorString)) {
      Color currentFillColor = getGraphicsState().getFillColor();
      Optional<Color> targetColor = findMatchingColor(currentFillColor);

      if (targetColor.isPresent()) {
        // A color match was found. We write the new color setting operator before the original
        // operator.
        super.write(
            processor,
            new PdfLiteral("rg"),
            Colors.getColorEncoding(colorMap.get(targetColor.get())));
      }
    }
    // Always write the original operator and operands.
    super.write(processor, operator, operands);
  }

  private Optional<Color> findMatchingColor(Color pdfColor) {
    if (pdfColor == null || pdfColor.getColorValue() == null) {
      return Optional.empty();
    }
    for (Color targetColor : colorMap.keySet()) {
      if (areColorsClose(pdfColor, targetColor)) {
        return Optional.of(targetColor);
      }
    }

    return Optional.empty();
  }

  private boolean areColorsClose(Color c1, Color c2) {
    if (c1.getClass() != c2.getClass()) {
      return false;
    }

    float[] v1 = c1.getColorValue();
    float[] v2 = c2.getColorValue();
    if (v1.length != v2.length) {
      return false;
    }

    float tolerance = 0.005f;
    for (int i = 0; i < v1.length; i++) {
      if (Math.abs(v1[i] - v2[i]) > tolerance) {
        return false;
      }
    }
    return true;
  }
}
