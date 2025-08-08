package org.example;

import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfLiteral;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfOutputStream;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfResources;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.IContentOperator;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class PdfCanvasEditor extends PdfCanvasProcessor {
  /**
   * This method edits the immediate contents of a page, i.e. its content stream. It explicitly does
   * not descent into form xobjects, patterns, or annotations.
   */
  public void editPage(PdfDocument pdfDocument, int pageNumber) throws IOException {
    if ((pdfDocument.getReader() == null) || (pdfDocument.getWriter() == null)) {
      throw new PdfException("PdfDocument must be opened in stamping mode.");
    }

    // get the current page and resources associated with it
    PdfPage page = pdfDocument.getPage(pageNumber);
    PdfResources pdfResources = page.getResources();
    // make a new canvas to make changes on it
    PdfCanvas pdfCanvas = new PdfCanvas(new PdfStream(), pdfResources, pdfDocument);
    // make the changes
    editContent(page.getContentBytes(), pdfResources, pdfCanvas);
    // overwrite the old page with the new contents
    page.put(PdfName.Contents, pdfCanvas.getContentStream());
  }

  /**
   * This method processes the content bytes and outputs to the given canvas. It explicitly does not
   * descent into form xobjects, patterns, or annotations.
   */
  public void editContent(byte[] contentBytes, PdfResources resources, PdfCanvas canvas) {
    // since a canvas is used throughout, this is just a convenient way to avoid passing
    // it all around
    this.canvas = canvas;
    processContent(contentBytes, resources);
    this.canvas = null;
  }

  /**
   * This method writes content stream operations to the target canvas. The default implementation
   * writes them as they come, so it essentially generates identical copies of the original
   * instructions the {@link ContentOperatorWrapper} instances forward to it.
   *
   * <p>Override this method to achieve some fancy editing effect.
   */
  protected void write(
      PdfCanvasProcessor processor, PdfLiteral operator, List<PdfObject> operands) {
    PdfOutputStream pdfOutputStream = canvas.getContentStream().getOutputStream();
    int index = 1;
    for (PdfObject object : operands) {
      pdfOutputStream.write(object);
      if (index++ < operands.size()) pdfOutputStream.writeSpace();
      else pdfOutputStream.writeNewLine();
    }
  }

  //
  // constructors
  //
  /** giving the parent a dummy listener to talk to */
  public PdfCanvasEditor() {
    this(new DummyEventListener());
  }

  /** giving the parent a custom listener to talk to */
  public PdfCanvasEditor(IEventListener listener) {
    super(listener);
  }

  //
  // Overrides of PdfContentStreamProcessor methods
  //
  @Override
  public IContentOperator registerContentOperator(
      String operatorString, IContentOperator operator) {
    ContentOperatorWrapper wrapper = new ContentOperatorWrapper();
    wrapper.setOriginalOperator(operator);
    IContentOperator formerOperator = super.registerContentOperator(operatorString, wrapper);
    return formerOperator instanceof ContentOperatorWrapper
        ? ((ContentOperatorWrapper) formerOperator).getOriginalOperator()
        : formerOperator;
  }

  //
  // members holding the output canvas and the resources
  //
  protected PdfCanvas canvas = null;

  //
  // A content operator class to wrap all content operators to forward the invocation to the editor
  //
  class ContentOperatorWrapper implements IContentOperator {
    public IContentOperator getOriginalOperator() {
      return originalOperator;
    }

    public void setOriginalOperator(IContentOperator originalOperator) {
      this.originalOperator = originalOperator;
    }

    @Override
    public void invoke(
        PdfCanvasProcessor processor, PdfLiteral operator, List<PdfObject> operands) {
      if (this.originalOperator != null && !"Do".equals(operator.toString())) {
        this.originalOperator.invoke(processor, operator, operands);
      }
      write(processor, operator, operands);
    }

    private IContentOperator originalOperator = null;
  }

  //
  // A dummy event listener to give to the underlying canvas processor to feed events to
  //
  static class DummyEventListener implements IEventListener {
    @Override
    public void eventOccurred(IEventData data, EventType type) {}

    @Override
    public Set<EventType> getSupportedEvents() {
      return null;
    }
  }
}
