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

import java.awt.Dimension;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.editor.Editor;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.ProjectPrintDialog;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.ReadmeTarget;


/**
 * Manages the printing of package assets eg. Class Diagram, Readme and Source
 * files.  All source files are printed as one large batch.  Individual
 * printing occurs through the source code editor
 * 
 * @author Bruce Quig
 */
public class PackagePrintManager extends Thread
{
    private PageFormat pageFormat;
    private Package pkg;
    private ProjectPrintDialog dialog;
    private List<Editor> editorsToPrint = new ArrayList<>();
    private Dimension pkgSize;

    /**
     * Constructor for PackagePrinter.
     * 
     * @param pkg package to be printed
     * @param pageFormat pageformat of printer job
     * @param dialog the project print dialog used by user to select which
     *        assets to print.
     */
    @OnThread(Tag.Swing)
    public PackagePrintManager(Package pkg, PageFormat pageFormat, 
                               ProjectPrintDialog dialog)
    {
        this.pkg = pkg;
        this.pkgSize = pkg.getMinimumSize();
        this.pageFormat = pageFormat;
        this.dialog = dialog;
        
        // We need to pull out the editors in this method, because getEditor may
        // need to create the editor, and so it must be on the Swing thread,
        // unlike the print job:
        if (dialog.printSource())
        {
            editorsToPrint.addAll(pkg.getAllClassnamesWithSource().stream().map(className -> ((ClassTarget) pkg.getTarget(className)).getEditor()).collect(Collectors.toList()));
        }
        if (dialog.printReadme())
        {
            ReadmeTarget readmeTgt = pkg.getReadmeTarget();
            if (readmeTgt != null)
                editorsToPrint.add(readmeTgt.getEditor());
        }
    }

    /**
     * Overridden run method called as part of the usage of this class as a
     * background operation via a thread with lower priority.
     */
    public void run()
    {
        PrinterJob printer = PrinterJob.getPrinterJob();

        if (printer.printDialog()) {
            if (dialog.printDiagram()) {
                printClassDiagram(printer);
            }

            for (Editor e : editorsToPrint)
                e.printTo(printer, dialog.printLineNumbers(), dialog.printHighlighting());
        }
    }

    /**
     * Prints the graphical representation of classes in the package.
     * 
     * @param printJob the printer job to print the diagram to.
     */
    public void printClassDiagram(PrinterJob printJob)
    {
        ClassDiagramPrinter diagramPrinter = new ClassDiagramPrinter(printJob, 
                                                                     pkg,
                                                                     pkgSize,
                                                                     pageFormat);
        SwingUtilities.invokeLater(() -> pkg.setStatus(Config.getString("pkgmgr.info.printing")));
        diagramPrinter.printPackage();
    }

}
