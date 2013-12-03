/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.editor.moe;

import java.awt.Color;
import java.awt.print.*;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.print.PrinterJob;
import javax.swing.text.*;

import java.util.*;
import java.text.DateFormat;

import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.Config;


/**
 * Class to handle printing for the MoeEditor.
 * This borrows ideas and some source code from Andrew Weiland's Print example
 * at http://www.wam.umd.edu/~aweiland/Print.java.  Which no longer exists..
 *
 * @author Bruce Quig
 */
public class MoePrinter
{
    static final String CONTINUED_LABEL = Config.getString("editor.printer.continued");
    private final int HEADER_SPACE = 30;
    private final int FOOTER_SPACE = 20;
    private final int LINE_NUMBER_WIDTH = 20;
    private final int PADDING = 5;
    
    private Book pages = new Book();  // This holds each page
    
    private static int titleFontSize = Config.getPropInteger("bluej.fontsize.printTitle", 14);
    private static Font titleFont = new Font("SansSerif", Font.BOLD, titleFontSize);
    private static Font smallTitleFont = new Font("SansSerif", Font.BOLD, 10);
    private static Font footerFont = new Font("SansSerif", Font.ITALIC, 9);
    private static Font lineNumberFont = new Font("SansSerif", Font.PLAIN, 6);
     
    private String className;
    private int tabSize = Config.getPropInteger("bluej.editor.tabsize", 4); 

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
    public boolean printDocument(PrinterJob printJob, MoeSyntaxDocument document, boolean lineNumbers, boolean syntaxHighlighting,
                                 String className, Font font, PageFormat format) 
    {
        List<PrintLine> lines = new ArrayList<PrintLine>();

        this.className = className;
        // extract tabsize attribute from document and assign to tabSize attribute
        Integer tabSizeAsInteger =  (Integer)document.getProperty(PlainDocument.tabSizeAttribute);
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
            // Get each line element, get its text and put it in the string list
            for (int i = 0; i < count; i++) {
                Element lineElement = (Element)root.getElement(i);
                lines.add(removeNewLines(new PrintLine(document, lineElement, i+1)));
            }
        }
        // make sure that read lock is removed
        finally {
            document.readUnlock();
        }

        return printText(printJob, lines, font, document, lineNumbers, syntaxHighlighting, format);
    }


    /**
     * Remove newline and carriage return characters from the end 
     * of this string. This is needed to fix a printing bug with 
     * the handling of newline characters on some printers
     * 
     * Having trailing spaces in the comments also seems to screw up
     * the spacing when printing with syntax highlighting, so we remove
     * them too.
     */
    private PrintLine removeNewLines(PrintLine line)
    {
        int length = line.length();
        char lastChar = (length > 0 ? line.charAt(line.length()-1) : 'a');

        while((lastChar == '\n') || (lastChar == '\r') || (lastChar == ' ') || (lastChar == '\t')) {
            
            line.chopLast();
            length = line.length();
            lastChar = (length > 0 ? line.charAt(line.length()-1) : 'a');
        }
        return line;
    }


    /**
     * Prints the text.  It sets paper size (at present) and paginates text
     * into a pageable Book for printing.
     *
     * @returns   true if it was not cancelled.
     */
    private synchronized boolean printText(PrinterJob job, List<PrintLine> text, Font font, MoeSyntaxDocument document, boolean lineNumbers, boolean syntaxHighlighting, PageFormat format) 
    {
        try {
            pages = paginateText(text, format, document, lineNumbers, syntaxHighlighting, font);        

            // set the book pageable so the printjob knows 
            // we are printing more than one page (maybe)
            job.setPageable(pages);
            job.print();
                
                return true;
        }
        catch (Exception e) {
            // should it be an error dialog?
            Debug.reportError("Exception thrown during printing: " + e);
            e.printStackTrace();
            return false;
        }
    }


    /**
     * The pagination method.  Paginate the text onto Printable page objects.
     * This includes wrapping long lines of text.
     */   
    private Book paginateText(List<PrintLine> text, PageFormat pageFormat, MoeSyntaxDocument document, boolean lineNumbers, boolean syntaxHighlighting, Font font) 
    {
        Book book = new Book();
        int pageNum = 1;        // page #

        // height of text area of a page
        int height = (int)pageFormat.getImageableHeight() - (HEADER_SPACE + FOOTER_SPACE);

        // number of lines on a page
        int linesPerPage = height / (font.getSize() + 2);   
        wrapLines(text, pageFormat, lineNumbers, font);

        // set number of pages
        int numberOfPages = ((int)(text.size() / linesPerPage)) + 1;  

        List<PrintLine> pageText;      // one page of text

        ListIterator<PrintLine> li = text.listIterator();
        while ( pageNum <= numberOfPages) {
            pageText = new ArrayList<PrintLine>(); 

            for (int lineCount = 0; li.hasNext() && lineCount < linesPerPage; lineCount++) { 
                pageText.add(li.next());
            }
        
            // create a new page object with the text and add it to the book
            book.append(new MoePage(pageText, document, lineNumbers, syntaxHighlighting, font), pageFormat);  
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
    private void wrapLines(List<PrintLine> text, PageFormat format, boolean lineNumbers, Font font)
    {
        // code to wrap lines of text for printing
        // get a line, get its length, do some font metrics,
        StyleContext context = new StyleContext();
        FontMetrics fontMetrics = context.getFontMetrics(font);
        int maxWidth = (int)format.getImageableWidth() - (PADDING * 2);
        if (lineNumbers)
        {
            maxWidth -= LINE_NUMBER_WIDTH;
        }
        int fontWidth = fontMetrics.charWidth('m');           
        int chars = maxWidth / fontWidth;

        for(ListIterator<PrintLine> li = text.listIterator(); li.hasNext(); ) {
            PrintLine pl = li.next();
            String currentLine = Utility.convertTabsToSpaces(pl.toString(), tabSize);
            int currentLineLength = currentLine.length();
            int width = fontMetrics.stringWidth(currentLine);
            
            // if line needs to be wrapped
            if(width > maxWidth) {
                int[] tabSpaces = Utility.calculateTabSpaces(pl.toString(), tabSize);
                // remove original
                li.remove();
                double iterations = (currentLineLength / chars) + 1;
                for(int begin = 0, end = 0; iterations > 0; iterations--, begin = Utility.advanceChars(pl.toString(),tabSpaces,begin,chars)) {
                    end = Utility.advanceChars(pl.toString(),tabSpaces,begin,chars);
                    
                    PrintLine newSubString = pl.substring(begin, end);
                    if(newSubString.length() != 0)
                    {
                        li.add(newSubString);
                    }
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
        private List<PrintLine> text;  // the text for the page
        private MoeSyntaxDocument document;
        private boolean lineNumbers;
        private boolean syntaxHighlighting;
        private Font font;

        MoePage(List<PrintLine> text, MoeSyntaxDocument document, boolean lineNumbers, boolean syntaxHighlighting, Font font) 
        {
            this.text = text;  // set the page's text
            this.font = font;  // set the page's font
            this.document = document;
            this.lineNumbers = lineNumbers; // whether to print line numbers
            this.syntaxHighlighting = syntaxHighlighting; // whether to print with syntax highlighting
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
            
            // Get some style information:
            StyleContext context = new StyleContext();
            final FontMetrics fontMetrics = context.getFontMetrics(font);
            Color[] colors = MoeSyntaxDocument.getColors();
            Color def = Color.black;

            // print a header
            printHeader(g, pageIndex, xPosition, yPosition, width, HEADER_SPACE);

            // print main text area
            int textYPosition = yPosition + HEADER_SPACE;
            int textXPosition = xPosition + PADDING + (lineNumbers ? LINE_NUMBER_WIDTH : 0);
            g.drawRect(xPosition, textYPosition, width, height - (HEADER_SPACE + FOOTER_SPACE));
            
            int lineNumberXPosition = xPosition + PADDING;
            int lastLineNumber = 0;
            if (lineNumbers)
            {
                //The vertical line dividing the line numbers from the code:
                g.drawLine(xPosition + LINE_NUMBER_WIDTH, textYPosition
                          ,xPosition + LINE_NUMBER_WIDTH, yPosition + height - FOOTER_SPACE);
            }
            
            
            // print the text
            for(ListIterator<PrintLine> li = text.listIterator(); li.hasNext(); ) {
                position = textYPosition + (this.font.getSize() + 2) * (li.nextIndex() + 1);
                PrintLine line = li.next();
                
                if (lineNumbers && line.getLineNumber() != lastLineNumber)
                {
                    g.setColor(Color.black);
                    g.setFont(lineNumberFont);
                    g.drawString(Integer.toString(line.getLineNumber()), lineNumberXPosition, position);
                    lastLineNumber = line.getLineNumber();
                    g.setFont(font);
                }
                
                int x = textXPosition;
                
                Token tokens = document.getParser().getMarkTokensFor(line.getStartOffset(), Math.min(line.getEndOffset(),document.getLength()) - line.getStartOffset(), 0, document);
                int offset = 0;
                while (tokens.id != Token.END) {
                    byte id = tokens.id;
                    
                    int length = tokens.length;
                    Color color;
                    if(id == Token.NULL)
                        color = def;
                    else {
                        // check we are within the array bounds
                        // safeguard for updated syntax package
                        if(id < colors.length)
                            color = colors[id];
                        else color = def;
                    }
                    if (syntaxHighlighting)
                    {
                        g.setColor(color == null ? def : color);
                    }

                    Segment lineSeg = line.getSegment();
                    lineSeg.count = length;
                    lineSeg.offset += offset;
                    
                    // workaround for strange problem on Mac:
                    // trying to print empty lines throws exception
                    if (lineSeg.count == 0) {
                        char[] chars = new char[]{' '};
                        Segment nonBlank = new Segment(chars,0,1);
                        x = Utilities.drawTabbedText(nonBlank, x, position, g, null, 0);
                    }
                    else {
                        TabExpander tab = Utility.makeTabExpander(lineSeg.toString(), tabSize, fontMetrics);
                        x = Utilities.drawTabbedText(lineSeg,x,position,g,tab,offset);
                    }
                    offset += length;
                    tokens = tokens.next;
                } 
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
            String title = "Class " + className;

            // print class name on left
            // if first page make title bigger
            if(pageIndex == 0) 
                g.setFont(titleFont);
            else {
                // don't add (continued) if there is no definition
                if(!"".equals(CONTINUED_LABEL) 
                   && !"editor.printer.continued".equals(CONTINUED_LABEL))
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
    
    /**
     * A class that is something like a Segment, but references a MoeSyntaxDocument
     * rather than simply a char[] like Segment does.
     *  
     * @author Neil Brown
     *
     */
    private class PrintLine implements CharSequence
    {
        private MoeSyntaxDocument document;
        private int startOffset;
        private int endOffset;
        private int lineNumber;

        public PrintLine(MoeSyntaxDocument document, int startOffset,
                int endOffset, int lineNumber) {
            this.document = document;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.lineNumber = lineNumber;
        }
        
        public PrintLine(MoeSyntaxDocument document, Element e, int lineNumber) {
            this.document = document;
            this.startOffset = e.getStartOffset();
            this.endOffset = e.getEndOffset();
            this.lineNumber = lineNumber;
        }

        public int getStartOffset() {
            return startOffset;
        }
        
        public int getEndOffset() {
            return endOffset;
        }

        public int getLineNumber() {
            return lineNumber;
        }
        
        public int length() {
            return endOffset - startOffset;
        }
        
        @Override
        public String toString() {
            try
            {
                if (length() == 0)
                {
                    return "";
                }
                else
                {
                    return document.getText(startOffset, length());
                }
            }
            catch (BadLocationException e)
            {
                Debug.reportError("PrintLine.toString(), offsets: " + startOffset + " and " + endOffset, e);
                throw new RuntimeException(e);
            }
        }

        public char charAt(int n) {
            return toString().charAt(n);
        }

        public CharSequence subSequence(int start, int end) {
            return toString().subSequence(start, end);
        }
        
        public void chopLast()
        {
            if (endOffset > startOffset)
                endOffset -= 1;
        }
        
        public PrintLine substring(int begin, int end)
        {
            return new PrintLine(document, begin + startOffset, end + startOffset, lineNumber);
        }
        
        public Segment getSegment()
        {
            try
            {
                Segment seg = new Segment();
                document.getText(getStartOffset(), getEndOffset() - getStartOffset(), seg);
                return seg;
            }
            catch (BadLocationException e)
            {
                Debug.reportError("PrintLine.getSegment(), offsets: " + startOffset + " and " + endOffset, e);
                return null;
            }
        }
    }
}



