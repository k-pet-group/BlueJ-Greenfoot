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
package bluej.parser;

/**
 * Describes a possible code completion.
 * 
 * @author Marion Zalk
 */
public abstract class AssistContent
{
    /** Get just the name of the method, to display in the box */
    public abstract String getDisplayMethodName();
    /** Get the parameters of the method (including brackets), to display in the box */
    public abstract String getDisplayMethodParams();
    
    /** Get the text to display in the code completion box for this completion */
    public abstract String getDisplayName();
    
    /** Get the completion text (to appear in front of the cursor/selection) */
    public abstract String getCompletionText();
    
    /** Get the completion text to appear selected */
    public abstract String getCompletionTextSel();
    
    /** Get the completion text (portion to appear behind the cursor/selection) */
    public abstract String getCompletionTextPost();

    /** Get the return type for this completion (as a string) */
    public abstract String getReturnType();

    /** Get the declaring class of this completion (as a string) */
    public abstract String getDeclaringClass();

    /** Return true if this completion has parameters */
    public abstract boolean hasParameters();
    
    /**
     * Get the javadoc comment for this completion. The comment has been stripped of the
     * delimiters (slash-star at the start and star-slash at the end) and intermediate
     * star characters.
     */
    public abstract String getJavadoc();
}
