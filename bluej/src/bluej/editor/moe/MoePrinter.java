package bluej.editor.moe;

import java.awt.Color;
import java.awt.print.*;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import java.text.DateFormat;

import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.Config;


/**
 * Class to handle printing for the MoeEditor.
 * This borrows ideas and some source code from Andrew Weiland's Print example
 * at http://www.wam.umd.edu/~aweiland/Print.java.  
 * @author Bruce Quig
 * @version $ Id: $
 */
public class MoePrinter
{

    static final String CONTINUED_LABEL = Config.getString("editor.printer.continued");
    private final int HEADER_SPACE = 30;
    private final int FOOTER_SPACE = 20;
    private final int PADDING = 5;
    private final char TAB_CHAR = '\t';

    //private PageFormat pgfmt;         // The Pageformat
    private Book pages = new Book();  // This holds each page
    private static Font titleFont = new Font("SansSerif", Font.BOLD, 14);
    private static Font smallTitleFont = new Font("SansSerif", Font.BOLD, 10);
    private static Font footerFont = new Font("SansSerif", Font.ITALIC, 9);
     
    private String className;
    private int tabSize = 4; 

    /**
     * Default constructor
     */
    public MoePrinter()
    {
        // nothing yet
    }

    /**
     * Prints the document.  This method produces a copy of the document 
     * as a List of Strings and delegates the printing to a printText method.
     *
     * @returns   true if it was not cancelled.
     */
    public boolean printDocument(PlainDocument document, String className, 
                                 Font font, PageFormat format) 
    {
        List lines = new ArrayList();

        this.className = className;
        // extract tabsize attribute from document and assign to tabSize attribute
        Integer tabSizeAsInteger =  (Integer)document.getProperty(document.tabSizeAttribute);
        if(tabSizeAsInteger != null)
            tabSize = tabSizeAsInteger.intValue();

        try{
            // read lock the document while reading to avoid any subsequent edits
            // unlikely to happen due to the speed at which the document is read
            document.readLock();

            //  Get Root element of the document
            Element root = document.getDefaultRootElement();
            //get the number of lines (i.e. child elements)
            int count = root.getElementCount();
            Segment segment = new Segment();
            // Get each line element, get its text and put it in the string list
            for (int i = 0; i < count; i++) {
                Element lineElement = (Element)root.getElement(i);
                try {
                    document.getText(lineElement.getStartOffset(), 
                                     lineElement.getEndOffset() - lineElement.getStartOffset(),
                                     segment);
                    lines.add(removeNewLines(segment.toString()));
                } catch(BadLocationException ble) {
                    Debug.reportError("Exception thrown accessing document text: " + ble);
                }
            }
        }
        // make sure that read lock is removed
        finally {
            document.readUnlock();
        }

        return printText(lines, font, format);
    }


    /**
     * Remove double newline and carriage return characters from the end 
     * of this string. This is needed because of what appears to be a
     * bug in the document classes: sometimes the lines appear to include
     * double NL or CR characters, and those get printed.
     */
    private String removeNewLines(String line)
    {
        int length = line.length();
        char secondLastChar = (length > 1 ? line.charAt(line.length()-2) : ' ');
        while((secondLastChar == '\n') || (secondLastChar == '\r')) {
            line = line.substring(0, line.length()-2) + '\n';
            length = line.length();
            secondLastChar = (length > 1 ? line.charAt(line.length()-2) : ' ');
        }
        return line;
    }

    /**
     * Prints the text.  It sets paper size (at present) and paginates text
     * into a pageable Book for printing.
     *
     * @returns   true if it was not cancelled.
     */
    private boolean printText(List text, Font font, PageFormat format) 
    {
        try {
            // create a printjob
            PrinterJob job = PrinterJob.getPrinterJob(); 

            // alter page to Moe default settings
     //        Paper moePaper = format.getPaper();
//             double width = moePaper.getWidth();
//             double height = moePaper.getHeight();
//             moePaper = format.getPaper();

//             // make it A4 roughly (temp)
//             moePaper.setSize(595, 840);
            
//             double leftSideMargin = 36;
//             double rightSideMargin = 72;
//             double topMargin = 54;
//             double bottomMargin = 36;
//             moePaper.setImageableArea(leftSideMargin, 
//                                       topMargin, 
//                                       width - (leftSideMargin + rightSideMargin), 
//                                       height - (topMargin + bottomMargin));
//             format.setPaper(moePaper);

            // make sure the pageformat is ok
            format = job.validatePage(format);           

            pages = paginateText(text, format, font);        

            // set the book pageable so the printjob knows 
            // we are printing more than one page (maybe)
            job.setPageable(pages);

            if (job.printDialog()) {
                // print.  This calls each MoePage object's print method
                job.print();
                return true;
            }
            else
                return false;
        }
        catch (Exception e) {
            // should it be an error dialog?
            Debug.reportError("Exception thrown during printing: " + e);
            return false;
        }
    }


    /**
     * The pagination method.  Paginate the text onto Printable page objects.
     * This includes wrapping long lines of text.
     */   
    private Book paginateText(List text, PageFormat pageFormat, Font font) 
    {
        Book book = new Book();
        int currentLine = 0;       // line I am  currently reading
        int pageNum = 1;        // page #

        // height of text area of a page
        int height = (int)pageFormat.getImageableHeight() - (HEADER_SPACE + FOOTER_SPACE);

        // number of lines on a page
        int linesPerPage = height / (font.getSize() + 2);   
        wrapLines(text, pageFormat, font);

        // set number of pages
        int numberOfPages = ((int)(text.size() / linesPerPage)) + 1;  

        List pageText;      // one page of text

        ListIterator li = text.listIterator();
        while ( pageNum <= numberOfPages) {
            pageText = new ArrayList(); 

            for (int lineCount = 0; li.hasNext() && lineCount < linesPerPage; lineCount++) { 
                pageText.add(li.next());
                currentLine++; //bq needed?
            }
	    
            // create a new page object with the text and add it to the book
            book.append(new MoePage(pageText, font), pageFormat);  
            pageNum++;   // increase the page number I am on
        }
        return book;  // return the completed book
    }


    /**
     * Wraps lines so that long lines of text outside of print page dimensions for a 
     * given page format and font are printed as a new line.  This method iterates 
     * through each line of text, calculates if there is an overlap and inserts 
     * overlapping text on the next line.
     */
    private void wrapLines(List text, PageFormat format, Font font)
    {
        // code to wrap lines of text for printing
        // get a line, get its length, do some font metrics,
        StyleContext context = new StyleContext();
        FontMetrics fontMetrics = context.getFontMetrics(font);
        int maxWidth = (int)format.getImageableWidth() - (PADDING * 2);
        int fontWidth = fontMetrics.charWidth('m');           
        int chars = maxWidth / fontWidth;

        for(ListIterator li = text.listIterator(); li.hasNext(); ) {
            String currentLine = Utility.convertTabsToSpaces((String)li.next(), tabSize);
            li.set(currentLine);
            int currentLineLength = currentLine.length();
            int width = fontMetrics.stringWidth(currentLine);
            
            // if line needs to be wrapped
            if(width > maxWidth) {
                int indexOfLine = li.previousIndex();
                // remove original
                li.remove();
                double iterations = (currentLineLength / chars) + 1;
                for(int begin = 0, end = 0; iterations > 0; iterations--, begin += chars) {
                    if(begin + chars < currentLineLength)
                        end = begin + chars;
                    else
                        end = currentLineLength;
                    String newSubString = currentLine.substring(begin, end);
                    if(newSubString.length() != 0)
                        li.add(newSubString);
                }
            }
        }
    }



    /* An inner class that defines one page of text based
     * on data about the PageFormat etc. from the book defined
     * in the parent class
     */                         
    class MoePage implements Printable 
    { 
        private List text;  // the text for the page
        private Font font;

        MoePage(List text, Font font) 
        {
            this.text = text;  // set the page's text
            this.font = font;  // set the page's font
        }

        /** 
         * Method that implements Printable interface.   
         * 
         */       
        public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException 
        { 
            // the printing part
            int position;
            g.setFont(this.font);     // Set the font
            g.setColor(Color.black);  // set color

            // get co-ordinates for frame
            int xPosition = (int)pageFormat.getImageableX(); 
            int yPosition = (int)pageFormat.getImageableY();
            int width = (int)pageFormat.getImageableWidth();
            int height = (int)pageFormat.getImageableHeight();

            // print a header
            printHeader(g, pageIndex, xPosition, yPosition, width, HEADER_SPACE);

            // print main text area
            int textYPosition = yPosition + HEADER_SPACE;
            int textXPosition = xPosition + PADDING;
            g.drawRect(xPosition, textYPosition, width, height - (HEADER_SPACE + FOOTER_SPACE));             
            // print the text
            for(ListIterator li = text.listIterator(); li.hasNext(); ) {
                position = textYPosition + (this.font.getSize() + 2) * (li.nextIndex() + 1);
                g.drawString((String)li.next(), textXPosition, position); 
            }

            // print footer
            int footerYPosition = yPosition + height - FOOTER_SPACE;
            printFooter(g, xPosition, footerYPosition, width, FOOTER_SPACE); 
    
            return Printable.PAGE_EXISTS;   // print the page
        } 

        /**
         * Prints a header box on a page, including a title and page number.
         */
        private void printHeader(Graphics g, int pageIndex, int xPos, int yPos, int width, int height) 
        {
            // draw title box
            g.setColor(Color.lightGray);
            g.fillRect(xPos, yPos, width, height);
            g.setColor(Color.black);  // set color
            g.drawRect(xPos, yPos, width, height);
            int titleYPos = yPos + HEADER_SPACE - this.font.getSize() + 2;
            Font currentFont = null;
            String title = "Class " + className;

            // print class name on left
            // if first page make title bigger
            if(pageIndex == 0) 
                g.setFont(titleFont);
            else {
                // don't add (continued) if there is no definition
                if(!"".equals(CONTINUED_LABEL) && !"editor.printer.continued".equals(CONTINUED_LABEL))
                    title = title + " (" + CONTINUED_LABEL + ")";
                g.setFont(smallTitleFont);
            }
            // print class name
            g.drawString(title, xPos + PADDING, titleYPos); 

            // set to smaller title font for page number
            g.setFont(smallTitleFont);
            FontMetrics pfm = g.getFontMetrics(smallTitleFont);
            // print page number on right
            String pageInfo = (pageIndex + 1) + "/" + pages.getNumberOfPages();
            int pageInfoX = xPos + width - PADDING - pfm.stringWidth(pageInfo);
            g.drawString(pageInfo, pageInfoX, titleYPos);
            g.setFont(font);

        }

        /**
         * Prints a footer box on a page that shows the print date.
         */
	    private void printFooter(Graphics g, int xPos, int yPos, int width, int height) 
        {
            // draw footer box
            //g.drawRect(xPos, yPos, width, height);

            // set up font and text position
            g.setFont(footerFont);
            FontMetrics pfm = g.getFontMetrics(footerFont);
            int footerTextYPos = yPos + FOOTER_SPACE - this.font.getSize() + 2;
        
            // package name not shown at present
            //g.drawString("Package: " + className, xPos + PADDING, footerTextYPos);  
         
            // print date on right
            Date today = new Date();
            DateFormat dateFormat = DateFormat.getDateTimeInstance();
            String date = dateFormat.format(today);
            int pageInfoX = xPos + width - PADDING - pfm.stringWidth(date);
            g.drawString(date, pageInfoX, footerTextYPos);
            //set font back to original
            g.setFont(font);

        }
	
    }
}



