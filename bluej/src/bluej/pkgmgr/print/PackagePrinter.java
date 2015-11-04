/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr.print;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.SwingUtilities;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.utility.Utility;

/**
 * Provides the ability to print a package as a separate thread 
 * (typically as a low priority background task)
 *
 * @author Bruce Quig
 */
public class PackagePrinter extends Thread implements Printable
{

    private PageFormat pageFormat;
    private final Package pkg;
    private final String pkgQualName;

    private int pageColumns = 0;
    private int pageRows = 0;
    private int pages;
    private int currentPage;
    private int currentColumn = 0;
    private int currentRow = 0;

    final static int a4Width = 595;
    final static int a4Height = 840;

   // Add a title to printouts
    static final int PRINT_HMARGIN = 6;
    static final int PRINT_VMARGIN = 24;
    static final Font printTitleFont = new Font("SansSerif", Font.PLAIN,
                                                12); //Config.printTitleFontsize);
    static final Font printInfoFont = new Font("SansSerif", Font.ITALIC,
                                               10); //Config.printInfoFontsize);
    private final Dimension graphSize;

    @OnThread(Tag.Swing)
    public PackagePrinter(Package pkg, PageFormat pageFormat)
    {
        this.pkg = pkg;
        this.pkgQualName = pkg.getQualifiedName();
        this.pageFormat = pageFormat;
        graphSize = pkg.getMinimumSize();
    }

    public void run()
    {
        printPackage();
    }

    private void printPackage()
    {
        PrinterJob printerJob = PrinterJob.getPrinterJob();

        printerJob.setPrintable(this, pageFormat);

        if (printerJob.printDialog()) {
            SwingUtilities.invokeLater(() -> pkg.setStatus(Config.getString("pkgmgr.info.printing")));
            calculatePages();
            try {
                // call the Printable interface to do the actual printing
                printerJob.print();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            SwingUtilities.invokeLater(() -> pkg.setStatus(Config.getString("pkgmgr.info.printed")));
        }
    }

    private void calculatePages()
    {
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
        if(pageIndex >= pages)
            return Printable.NO_SUCH_PAGE;

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

        FontMetrics tfm = g.getFontMetrics(printTitleFont);
        FontMetrics ifm = g.getFontMetrics(printInfoFont);
        Rectangle printArea = new Rectangle((int)pageFormat.getImageableX(),
                                            (int)pageFormat.getImageableY(),
                                            (int)pageFormat.getImageableWidth(),
                                            (int)pageFormat.getImageableHeight());

        // frame header area
        g.setColor(Color.lightGray);
        g.fillRect(printArea.x, printArea.y, printArea.width, PRINT_VMARGIN);

        //    Rectangle titleRectangle = new Rectangle(printArea.x, 
        //                                                  printArea.y, 
        //                                                  printArea.width, 
        //                                                  PRINT_VMARGIN);
        
        //         g.fill(titleRectangle);

        // g.setColor(titleCol);
        g.setColor(Color.black);

        g.drawRect(printArea.x, printArea.y, printArea.width, PRINT_VMARGIN);
        // g.draw(titleRectangle);

        // frame print area
        g.drawRect(printArea.x, printArea.y, printArea.width,
                   printArea.height - PRINT_VMARGIN);

        // write header
        //String title = (packageName == noPackage) ? dirname : packageName;
        String title = pkgQualName;
        g.setFont(printTitleFont);
        Utility.drawCentredText(g, "BlueJ package - " + title,
                                printArea.x, printArea.y,
                                printArea.width, tfm.getHeight());
        // write footer
        g.setFont(printInfoFont);
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        Utility.drawRightText(g, dateFormat.format(new Date()) + ", Page " + pageNum,
                              printArea.x, printArea.y + printArea.height - PRINT_VMARGIN,
                              printArea.width, ifm.getHeight());
    }


}
