package org.example;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceCmyk;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfLiteral;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

// credit for this implementation to:
// https://stackoverflow.com/questions/40401800/traverse-whole-pdf-and-change-some-attribute-with-some-object-in-it-using-itext/40709845#40709845
public class App {
  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Usage: ./gradlew run <input-filepath> <output-filepath>");
      System.exit(1);
    }
    String inputFilePath = args[0];
    String outputFilePath = args[1];

    File input = new File(inputFilePath);
    if (!input.exists()) {
      System.out.println("Input file does not exist!");
      System.exit(1);
    }
    File output = new File(outputFilePath);

    App app = new App();
    try {
      app.run(input, output);
    } catch (IOException e) {
      System.out.println("I/O exception caught: " + e.getMessage());
    }
  }

  public void run(File input, File output) throws IOException {
    try (InputStream resource = new FileInputStream(input);
        PdfReader pdfReader = new PdfReader(resource);
        OutputStream result = new FileOutputStream(output);
        PdfWriter pdfWriter = new PdfWriter(result);
        PdfDocument pdfDocument = new PdfDocument(pdfReader, pdfWriter)) {

      // Color targetColor = new DeviceRgb(0, 255, 0); // unreadable pure green
      Color targetColor = new DeviceRgb(255, 0, 0); // unreadable pure red
      // Color replacementColor = new DeviceRgb(0, 0, 255); // navy blue
      Color replacementColor = new DeviceRgb(128, 32, 32); // crimson red

      PdfCanvasEditor editor =
          new PdfCanvasEditor() {
            Color currentColor = null;
            final List<String> TEXT_SHOWING_OPERATORS =
                Arrays.asList(
                    "Tj", "'", "\"", "TJ", "Tf", "Td", "TD", "Tm", "T*", "Tw", "Tc", "Tz", "TL",
                    "Ts", "BT", "ET", "Tr");

            @Override
            protected void write(
                PdfCanvasProcessor processor, PdfLiteral operator, List<PdfObject> operands) {
              String operatorString = operator.toString();

              // currentColor is non-null only when we're processing text matching the target color
              if (TEXT_SHOWING_OPERATORS.contains(operatorString)) {
                Color currentFillColor = getGraphicsState().getFillColor();
                if (currentColor == null && targetColor.equals(currentFillColor)) {
                  currentColor = currentFillColor;
                  super.write(
                      processor, new PdfLiteral("rg"), Colors.getColorEncoding(replacementColor));
                }
              } else if (currentColor != null) {
                if (currentColor instanceof DeviceCmyk) {
                  super.write(processor, new PdfLiteral("k"), Colors.blackAsCMYK());
                } else if (currentColor instanceof DeviceGray) {
                  super.write(processor, new PdfLiteral("g"), Colors.blackAsGreyscale());
                } else {
                  super.write(processor, new PdfLiteral("rg"), Colors.blackAsRGB());
                }
                currentColor = null;
              }
              super.write(processor, operator, operands);
            }
          };

      for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
        editor.editPage(pdfDocument, i);
      }
    }
  }
}
