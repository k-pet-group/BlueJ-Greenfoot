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
        Dimension pageSize = new Dimension((int)pageFormat.getImageableWidth(),
                                           (int)pageFormat.getImageableHeight());
        Dimension graphSize = pkg.getMinimumSize();
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

        FontMetrics tfm = g.getFontMetrics(printTitleFont);
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
        Utility.drawRightText(g, dateFormat.format(new Date()) + ", Page " + pageNum,
                              printArea.x, printArea.y + printArea.height - PRINT_VMARGIN,
                              printArea.width, ifm.getHeight());
    }
}
