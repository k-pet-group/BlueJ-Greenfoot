/*
 This file is part of the BlueJ program.
 Copyright (C) 2011,2013  Michael Kolling and John Rosenberg

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.utility.JavaNames;
import bluej.utility.PersistentMarkDocument;

/**
 * Document implementation for the terminal editor pane.
 * 
 * <p>This is mainly necessary to override PlainDocument's slightly brain-damaged
 * implementation of the insertUpdate() method, which can clear line attributes
 * unexpectedly (insertUpdate method).
 * 
 * <p>It also allows highlighting of exception stack traces
 * 
 * @author Davin McCall
 */
public class TerminalDocument extends PersistentMarkDocument
{
    private boolean highlightSourceLinks;
    private Project project;
    
    public TerminalDocument(Project project, boolean highlightSourceLinks)
    {
        this.project = project;
        this.highlightSourceLinks = highlightSourceLinks;
    }
    
    /**
     * Mark a line as displaying method output.
     * 
     * @param line  The line number (0..N)
     */
    public void markLineAsMethodOutput(int line)
    {
        writeLock();
        
        Element el = root.getElement(line);
        MutableAttributeSet attr = (MutableAttributeSet) el.getAttributes();
        attr.addAttribute(TerminalView.METHOD_RECORD, Boolean.valueOf(true));
        
        writeUnlock();
    }
    
    @Override
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr)
    {
        super.insertUpdate(chng, attr);
        
        if (highlightSourceLinks) {
            scanForStackTrace();
        }
    }

    /**
     * Looks through the contents of the terminal for lines
     * that look like they are part of a stack trace.
     */
    private void scanForStackTrace()
    {
        try {
            String content = getText(0, getLength());
            
            Pattern p = java.util.regex.Pattern.compile("at (\\S+)\\((\\S+)\\.java:(\\d+)\\)");
            // Matches things like:
            // at greenfoot.localdebugger.LocalDebugger$QueuedExecution.run(LocalDebugger.java:267)
            //    ^--------------------group 1----------------------------^ ^--group 2--^      ^3^
            Matcher m = p.matcher(content);
            while (m.find())
            {
                int elementIndex = getDefaultRootElement().getElementIndex(m.start());
                Element el = getDefaultRootElement().getElement(elementIndex);
                MutableAttributeSet attr = (MutableAttributeSet) el.getAttributes();
                
                String fullyQualifiedMethodName = m.group(1);
                String javaFile = m.group(2);
                int lineNumber = Integer.parseInt(m.group(3));
                
                // The fully qualified method name will end in ".method", so we can
                // definitely remove that:
                
                String fullyQualifiedClassName = JavaNames.getPrefix(fullyQualifiedMethodName);
                // The class name may be an inner class, so we want to take the package:
                String packageName = JavaNames.getPrefix(fullyQualifiedClassName);
                
                //Find out if that file is available, and only link if it is:                
                Package pkg = project.getPackage(packageName);
                
                if (pkg != null && pkg.getAllClassnames().contains(javaFile))
                {
                    attr.addAttribute(TerminalView.SOURCE_LOCATION, new ExceptionSourceLocation(m.start(1), m.end(), pkg, javaFile, lineNumber));
                }
                else
                {
                    attr.addAttribute(TerminalView.FOREIGN_STACK_TRACE, Boolean.valueOf(true));
                }
            }
            
            //Also mark up native method lines in stack traces with a marker for font colour:
            
            p = java.util.regex.Pattern.compile("at \\S+\\(Native Method|Unknown Source\\)");
            // Matches things like:
            //  at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
            m = p.matcher(content);
            while (m.find())
            {
                int elementIndex = getDefaultRootElement().getElementIndex(m.start());
                Element el = getDefaultRootElement().getElement(elementIndex);
                MutableAttributeSet attr = (MutableAttributeSet) el.getAttributes();
                
                attr.addAttribute(TerminalView.FOREIGN_STACK_TRACE, Boolean.valueOf(true));
            }
        }
        catch (BadLocationException e) {
            e.printStackTrace();
        }
        catch (NumberFormatException e ) {
            //In case it looks like an exception but has a large line number:
            e.printStackTrace();
        }
    }
    
}
