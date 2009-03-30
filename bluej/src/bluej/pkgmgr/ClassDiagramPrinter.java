/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.awt.print.*;
import java.awt.*;
import java.util.Date;
import java.text.DateFormat;

import bluej.utility.Utility;
import bluej.Config;

/**
 * Provides the ability to print a package class diagram
 *
 * @author Bruce Quig
 *
 */
public class ClassDiagramPrinter implements Printable
{

    private PrinterJob printerJob;
    private Package pkg;
    private PageFormat pageFormat;
    
    private int pageColumns = 0;
    private int pageRows = 0;
    private int pages;
    private int currentPage;
    private int currentColumn = 0;
    private int currentRow = 0;
    private double scaleFactor = Config.getPropInteger("bluej.print.scale", 50) / 100.0;
   
   // Add a title to printouts
    static final int TITLEBOX_HEIGHT = 20;
    static final int PRINT_HMARGIN = 1;
    static final int PRINT_VMARGIN = TITLEBOX_HEIGHT + 1;
    static final Font printTitleFont = new Font("SansSerif", Font.PLAIN, 12);
                                           //Config.printTitleFontsize);
    static final Font printInfoFont = new Font("SansSerif", Font.ITALIC, 9);
                                           //Config.printInfoFontsize);
    
    public ClassDiagramPrinter(PrinterJob printer, Package pkg, PageFormat pageFormat)
    {
        this.printerJob = printer;
        this.pkg = pkg;
        this.pageFormat = pageFormat;
    }

    /**
     * Print the package class diagram for the package that is held by this printer.
     */
    public void printPackage()
    {
        printerJob.setPrintable(this, pageFormat);

        calculatePages();
        try {
            // call the Printable interface to do the actual printing
            printerJob.print();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        pkg.setStatus(Config.getString("pkgmgr.info.printed"));
    }


    /**
     * Calculate how many pages, rows and columns are needed to print this diagram.
     * This method initialises the instance fields with this information.
     */
    private void calculatePages()
    {
        Dimension graphSize = pkg.getMinimumSize();
        graphSize.setSize(graphSize.getWidth() * scaleFactor, 
                          graphSize.getHeight() * scaleFactor);
        Rectangle printArea = getPrintArea(pageFormat);
        
        pageColumns = (graphSize.width + printArea.width - 1) / printArea.width;
        pageRows = (graphSize.height + printArea.height - 1) / printArea.height;
        pages = pageColumns * pageRows;
        currentColumn = currentRow = 0;
    }


    /**
     * Method that implements Printable interface and does that actual printing of
     * class diagram.
     */
    public int print(Graphics g, PageFormat pageFormat, int pageIndex)
    {
        if(pageIndex >= pages) {
            return Printable.NO_SUCH_PAGE;
        }
        
        Rectangle printArea = getPrintArea(pageFormat);
        
        if(currentColumn < pageColumns) {
            if(currentPage < pageIndex)
                currentColumn++;
        }
        else if(currentRow < pageRows) {
            //if(currentPage < pageIndex)
            currentRow++;
            currentColumn = 0;
        }

        printTitle(g, pageFormat, pageIndex + 1);

        g.translate(printArea.x - currentColumn * printArea.width,
                    printArea.y - currentRow * printArea.height);
        g.setClip(currentColumn * printArea.width, currentRow * printArea.height,
                  printArea.width, printArea.height);
        ((Graphics2D)g).scale(scaleFactor, scaleFactor);

        pkg.getEditor().paint(g);

        currentPage = pageIndex;
        return Printable.PAGE_EXISTS;
    }

 

    /**
     * Return the rectangle on the page in which to draw the class diagram.
     * The rectangle is the page minus margins minus space for header and
     * footer text.
     */
    private Rectangle getPrintArea(PageFormat pageFormat)
    {
        return new Rectangle((int)pageFormat.getImageableX() + PRINT_HMARGIN,
                             (int)pageFormat.getImageableY() + PRINT_VMARGIN,
                             (int)pageFormat.getImageableWidth() - (2 * PRINT_HMARGIN),
                             (int)pageFormat.getImageableHeight() - (2 * PRINT_VMARGIN));
    }

    /**
     * Print the page title and other page decorations (frame, footer).
     */
    private void printTitle(Graphics g, PageFormat pageFormat, int pageNum)
    {
        FontMetrics ifm = g.getFontMetrics(printInfoFont);
        Rectangle printArea = new Rectangle((int)pageFormat.getImageableX(),
                                            (int)pageFormat.getImageableY(),
                                            (int)pageFormat.getImageableWidth(),
                                            (int)pageFormat.getImageableHeight());

        // shade and frame header area
        g.setColor(Color.lightGray);
        g.fillRect(printArea.x, printArea.y, printArea.width-1, TITLEBOX_HEIGHT);
        g.setColor(Color.black);
        g.drawRect(printArea.x, printArea.y, printArea.width-1, TITLEBOX_HEIGHT);

        // frame print area
        g.drawRect(printArea.x, printArea.y, printArea.width-1,
                   printArea.height - PRINT_VMARGIN);

        // write header
        String title = "Project " + pkg.getProject().getProjectName();
        String packageName = pkg.getQualifiedName();
        if(packageName.length() > 0)
            title += " [package " + packageName + "]";
        
        g.setFont(printTitleFont);
        Utility.drawCentredText(g, title, printArea.x, printArea.y,
                                printArea.width, TITLEBOX_HEIGHT);
        // write footer
        g.setFont(printInfoFont);
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        Utility.drawRightText(g, dateFormat.format(new Date()) + 
                              "     Page " + pageNum + " of " + pages,
                              printArea.x, printArea.y + printArea.height - PRINT_VMARGIN,
                              printArea.width, ifm.getHeight());
    }
}
