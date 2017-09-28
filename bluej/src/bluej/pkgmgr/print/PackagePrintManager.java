/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2016  Michael Kolling and John Rosenberg 
 
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

import bluej.Config;
import bluej.editor.Editor;
import bluej.editor.moe.PrintDialog.PrintChoices;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.ReadmeTarget;
import bluej.utility.Debug;
import bluej.utility.javafx.FXRunnable;
import javafx.print.PrinterJob;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Manages the printing of package assets eg. Class Diagram, Readme and Source
 * files.  All source files are printed as one large batch.  Individual
 * printing occurs through the source code editor
 * 
 * @author Bruce Quig
 */
public class PackagePrintManager extends Thread
{
    private final List<FXRunnable> printActions = new ArrayList<>();
    private final PrinterJob job;

    /**
     * Constructor for PackagePrinter.
     * 
     * @param pkg package to be printed
     * @param printChoices the print options chosen by the user
     */
    @OnThread(Tag.FXPlatform)
    public PackagePrintManager(PrinterJob job, PkgMgrFrame pkgMgrFrame, PrintChoices printChoices)
    {
        this.job = job;

        if (printChoices.printDiagram) {
            printActions.add(() -> printClassDiagram(job, pkgMgrFrame));
        }

        Package pkg = pkgMgrFrame.getPackage();
        
        // We need to pull out the editors in this method, because printTo needs
        // to run on the FXPlatform thread:
        if (printChoices.printSource)
        {
            printActions.addAll(pkg.getAllClassnamesWithSource().stream()
                    .map(className -> ((ClassTarget) pkg.getTarget(className)).getEditor())
                    .map(ed -> ed.printTo(job, printChoices.printSize, printChoices.printLineNumbers, printChoices.printHighlighting))
                    .collect(Collectors.toList()));
        }
        if (printChoices.printReadme)
        {
            ReadmeTarget readmeTgt = pkg.getReadmeTarget();
            if (readmeTgt != null)
                printActions.add(readmeTgt.getEditor().printTo(job, printChoices.printSize, printChoices.printLineNumbers, printChoices.printHighlighting));
        }
    }

    /**
     * Overridden run method called as part of the usage of this class as a
     * background operation via a thread with lower priority.
     */
    @OnThread(value = Tag.FX, ignoreParent = true)
    public void run()
    {
        try
        {
            for (FXRunnable printAction : printActions)
            {
                printAction.run();
            }
        }
        catch (Throwable t)
        {
            Debug.reportError(t);
        }
        job.endJob();
    }

    /**
     * Prints the graphical representation of classes in the package.
     * 
     * @param printJob the printer job to print the diagram to.
     */
    @OnThread(Tag.FX)
    public void printClassDiagram(PrinterJob printJob, PkgMgrFrame pkgMgrFrame)
    {
        pkgMgrFrame.printDiagram(printJob);
    }

}
