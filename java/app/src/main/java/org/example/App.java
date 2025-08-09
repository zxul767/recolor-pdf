package org.example;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// credit for this implementation to:
// https://stackoverflow.com/questions/40401800/traverse-whole-pdf-and-change-some-attribute-with-some-object-in-it-using-itext/40709845#40709845
public class App {
  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println(
          "Usage: ./gradlew run <input-filepath> <output-filepath> <colors-filepath>");
      System.exit(1);
    }
    String inputFilePath = args[0];
    String outputFilePath = args[1];
    String colorsFilePath = args[2];

    File colorsFile = new File(colorsFilePath);
    if (!colorsFile.exists()) {
      System.out.println("Colors file does not exist!");
      System.exit(1);
    }

    File input = new File(inputFilePath);
    if (!input.exists()) {
      System.out.println("Input file does not exist!");
      System.exit(1);
    }
    File output = new File(outputFilePath);

    App app = new App();
    try {
      Map<Color, Color> colorMap = app.loadColorMap(colorsFile);
      app.run(input, output, colorMap);
    } catch (IOException e) {
      System.out.println("I/O exception caught: " + e.getMessage());
    }
  }

  public void run(File input, File output, Map<Color, Color> colorMap) throws IOException {
    try (InputStream resource = new FileInputStream(input);
        PdfReader pdfReader = new PdfReader(resource);
        OutputStream result = new FileOutputStream(output);
        PdfWriter pdfWriter = new PdfWriter(result);
        PdfDocument pdfDocument = new PdfDocument(pdfReader, pdfWriter)) {
      PdfCanvasEditor editor = new ColorReplacingCanvasEditor(colorMap);

      for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
        editor.editPage(pdfDocument, i);
      }
    }
  }

  private Map<Color, Color> loadColorMap(File colorsFile) throws IOException {
    Properties properties = new Properties();
    try (FileInputStream fis = new FileInputStream(colorsFile)) {
      properties.load(fis);
    }

    Map<Color, Color> colorMap = new HashMap<>();
    for (String targetColorHex : properties.stringPropertyNames()) {
      String replacementColorHex = properties.getProperty(targetColorHex);
      colorMap.put(hexToColor(targetColorHex), hexToColor(replacementColorHex));
    }
    return colorMap;
  }

  private Color hexToColor(String hex) {
    java.awt.Color awtColor = java.awt.Color.decode("#" + hex);
    return new DeviceRgb(
        awtColor.getRed() / 255f, awtColor.getGreen() / 255f, awtColor.getBlue() / 255f);
  }
}
