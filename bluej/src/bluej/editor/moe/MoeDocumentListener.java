/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011  Michael Kolling and John Rosenberg 
 
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

/**
 * An interface for listening to parse events on a MoeSyntaxDocument.
 * 
 * @author Davin McCall
 */
public interface MoeDocumentListener
{
    /**
     * A parse error was encountered while parsing (part of) the document
     * 
     * @param position   The position of the error
     * @param size       The length of the error
     * @param message    The error message
     */
    public void parseError(int position, int size, String message);
    
    /**
     * A range of the document has been scheduled to be re-parsed. Any current errors within
     * the specified range should be considered invalid.
     * 
     * @param position  The beginning of the range
     * @param size      The size of the range
     */
    public void reparsingRange(int position, int size);
}
