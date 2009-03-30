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
package bluej.compiler;

import java.io.IOException;
import java.io.Writer;

import bluej.utility.DialogManager;

public class JavacErrorWriter extends Writer
{
    private boolean haserror = false, hasfollowup = false, hasWarnings = false;
    private int ignoreCount = 0;    // when > 0, indicates number of lines to ignore

    private String filename, message;
    private String warning = "";
    private int lineno;
    
    private boolean internal;
    
    private String lineBuf;
    private String newLineSequence = System.getProperty("line.separator");
    
    public JavacErrorWriter(boolean internal)
    {
        this.internal = internal;
        lineBuf = "";
    }
    
    public void reset()
    {
        haserror = false;
        hasfollowup = false;
        hasWarnings = false;
        ignoreCount = 0;
        warning = "";
    }

    public boolean hasError()
    {
        return haserror;
    }
    
    public boolean hasWarnings()
    {
        return hasWarnings;
    }

    public String getFilename()
    {
        return filename;
    }

    public int getLineNo()
    {
        return lineno;
    }

    public String getMessage()
    {
        return message;
    }
    
    public String getWarning()
    {
        return warning;
    }
    
    /* (non-Javadoc)
     * @see java.io.Writer#write(char[], int, int)
     */
    public void write(char[] cbuf, int off, int len)
        throws IOException
    {
        int lineBufLen = lineBuf.length();
        lineBuf += new String(cbuf, off, len);
        
        int startSearch = lineBufLen - newLineSequence.length() + 1;
        if (startSearch < 0) {
            startSearch = 0;
        }
        
        int eolIndex = lineBuf.indexOf(newLineSequence, startSearch);
        if (eolIndex != -1) {
            String line = lineBuf.substring(0, eolIndex);
            lineBuf = lineBuf.substring(eolIndex + newLineSequence.length());
            processLine(line);
        }
    }

    public void flush()
        throws IOException
    {

    }

    public void close()
        throws IOException
    {
        if (lineBuf.length() != 0) {
            processLine(lineBuf);
            lineBuf = "";
        }
    }

    private void processLine(String msg)
    {
        if (haserror)
            return;
            
        if (ignoreCount > 0) {
            ignoreCount--;
            return;
        }

        // there are some error messages that give important information in the
        // following lines. Try to munge it into a better message by utilising the
        // second/third line of the error
        if (hasfollowup) {
            int colonPoint = 9;
            String label = msg.substring(0, colonPoint);
            String info = msg.substring(colonPoint).trim();
            
            if(label.equals("found   :")) {             // incompatible types
                message += " - found " + info;
            } else if (label.equals("required:")) {
                message += " but expected " + info;
                haserror = true;  
            } else if (label.equals("symbol  :")) {     // unresolved symbol
                message += " - " + info;                             
                haserror = true;  
            }
            else {
                // if not what we were expecting, bail out
                haserror = true;  
            }
            
            return;          
        }

        int first_colon = msg.indexOf(':', 0);
        if(first_colon == -1) {
            // no colon may mean we are processing the end of compile msgs
            // of the form
            // x warning(s)

            if (msg.trim().endsWith("warnings") || msg.trim().endsWith("warning")) {
                warning += msg.trim() + newLineSequence;
                hasWarnings = true;
                return;
            }
            
            // otherwise, cannot read format of error message
            DialogManager.showErrorWithText(null, "compiler-error", msg);
            return;
        }

        // "unchecked" warnings for generics begin with "Note: "
        // and the filename is everything from there to ".java uses"
        if(msg.startsWith("Note: ")) {
            if(internal) // set from compiler.showunchecked in the PrefMgr
                return;
            int uses = msg.indexOf(".java uses");
            if(uses != -1) {
                filename = msg.substring(5, uses) + ".java";
            }
            warning += msg.trim() + newLineSequence;
            hasWarnings = true;
            return;
        }
        
        filename = msg.substring(0, first_colon);

        // Windows might have a colon after drive name. If so, ignore it
        if(! filename.endsWith(".java")) {
            first_colon = msg.indexOf(':', first_colon + 1);
            if(first_colon == -1) {
                // cannot read format of error message
                DialogManager.showErrorWithText(null, "compiler-error", msg);
                return;
            }
            filename = msg.substring(0, first_colon);
        }
        int second_colon = msg.indexOf(':', first_colon + 1);
        if(second_colon == -1) {
            // cannot read format of error message
            DialogManager.showErrorWithText(null, "compiler-error", msg);
            return;
        }

        lineno = 0;
        try {
            lineno = Integer.parseInt(msg.substring(first_colon + 1, second_colon));
        } catch(NumberFormatException e) {
            // ignore it
        }

        message = msg.substring(second_colon + 1).trim();

        if (message.startsWith("warning:")) {
            // Record the warnings and display them to users.
            // This may end up multi-line, so ensure that the
            // message is broken into (single-spaced) lines
            warning += msg.trim() + newLineSequence;
            ignoreCount = 2;
            // This type of warning generates an additional two lines:
            // one is a duplicate of the source line, the next is empty
            // other than a single caret (^) indicating the position in the line.
            if (message.startsWith("warning: [unchecked] unchecked cast")) {
                ignoreCount = 4;
            }
            else if (message.startsWith("warning: non-varargs call of varargs method with inexact argument type for last parameter")) {
                ignoreCount = 4;
            }
            
            if(!hasWarnings)
                hasWarnings = true;           
            return;
        }

        if (message.equals("cannot resolve symbol")
                || message.equals("cannot find symbol")
                || message.equals("incompatible types")) {
            hasfollowup = true;
        }
        else {
            haserror = true;
        }
    }
}
