/*
 This file is part of the BlueJ program.
 Copyright (C) 2010,2011  Michael Kolling and John Rosenberg

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

package bluej.terminal;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.text.Segment;
import javax.swing.text.StyleContext;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;

import bluej.utility.Debug;
import bluej.utility.Utility;

/**
 * Handles the printing of the Terminal window, including pagination and line-wrapping.
 *
 * @author nccb
 *
 */
public class TerminalPrinter
{
    // This is done to match with the terminal text area which uses the
    // JTextArea default, which is that every tab is 8 spaces:
    private static final int tabSize = 8;
    
    private static final int PADDING = 5;
    
    public static class TerminalPage implements Printable
    {
        private List<String> pageText;
        private Font font;
        
        public TerminalPage(List<String> pageText, Font font)
        {
            this.pageText = pageText;
            this.font = font;
        }

        public int print(Graphics g, PageFormat pageFormat, int pageIndex)
                throws PrinterException
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
            
            // print main text area
            int textYPosition = yPosition; // + HEADER_SPACE;
            int textXPosition = xPosition + PADDING; // + (lineNumbers ? LINE_NUMBER_WIDTH : 0);
            g.drawRect(xPosition, textYPosition, width, height /* - (HEADER_SPACE + FOOTER_SPACE)*/);
            
            // print the text
            for(ListIterator<String> li = pageText.listIterator(); li.hasNext(); ) {
                position = textYPosition + (this.font.getSize() + 2) * (li.nextIndex() + 1);
                String line = li.next();
                
                int x = textXPosition;
                
                // workaround for strange problem on Mac:
                // trying to print empty lines throws exception
                if (line.length() == 0) {
                    char[] nonBlank = new char[] {' '};
                    Segment lineSeg = new Segment(nonBlank,0,1);
                    x = Utilities.drawTabbedText(lineSeg, x, position, g, null, 0);
                }
                else {
                    Segment lineSeg = new Segment(line.toCharArray(), 0, line.length());
                    TabExpander tab = Utility.makeTabExpander(line, tabSize, fontMetrics);
                    x = Utilities.drawTabbedText(lineSeg,x,position,g,tab,0);                    
                } 
            }
   
            return Printable.PAGE_EXISTS;   // print the page

        }

    }

    public static boolean printTerminal(PrinterJob job, TermTextArea textArea, PageFormat pageFormat, Font font)
    {
        try {
            Book pages = paginateText(textArea, pageFormat, font);        

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

    private static Book paginateText(TermTextArea textArea, PageFormat pageFormat, Font font)
    {
        // It is important to make a new linked list because we may
        // manipulate it later if lines need to be wrapped:
        List<String> text = new LinkedList<String>(Arrays.asList(textArea.getText().split("\r\n|\r|\n")));
        
        Book book = new Book();
        int pageNum = 1;        // page #

        // height of text area of a page
        int height = (int)pageFormat.getImageableHeight(); // - (HEADER_SPACE + FOOTER_SPACE);

        // number of lines on a page
        int linesPerPage = height / (font.getSize() + 2);   
        wrapLines(text, pageFormat, font);

        // set number of pages
        int numberOfPages = ((int)(text.size() / linesPerPage)) + 1;  

        List<String> pageText;      // one page of text

        ListIterator<String> li = text.listIterator();
        while ( pageNum <= numberOfPages) {
            pageText = new ArrayList<String>(); 

            for (int lineCount = 0; li.hasNext() && lineCount < linesPerPage; lineCount++) { 
                pageText.add(li.next());
            }
        
            // create a new page object with the text and add it to the book
            book.append(new TerminalPage(pageText, font), pageFormat);  
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
    private static void wrapLines(List<String> text, PageFormat format, Font font)
    {
        // code to wrap lines of text for printing
        // get a line, get its length, do some font metrics,
        StyleContext context = new StyleContext();
        FontMetrics fontMetrics = context.getFontMetrics(font);
        int maxWidth = (int)format.getImageableWidth() - (PADDING * 2);
        int fontWidth = fontMetrics.charWidth('m');           
        int chars = maxWidth / fontWidth;

        for(ListIterator<String> li = text.listIterator(); li.hasNext(); ) {
            String pl = li.next();
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
                    
                    String newSubString = pl.substring(begin, end);
                    if(newSubString.length() != 0)
                    {
                        li.add(newSubString);
                    }
                }
            }
        }
    }
}
