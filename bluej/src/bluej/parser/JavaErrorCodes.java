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
package bluej.parser;

/**
 * A wrapper for error message code strings.
 * 
 * <p>These codes are produced by the BlueJ parser, and correspond to parse errors occurring in
 * certain contexts.
 * 
 * @author Davin McCall
 */
public class JavaErrorCodes
{
    /** Method declaration not followed by one of ';', method body, or "throws" clause */
    public static final String BJ000 = "BJ000";
    
    /** Bracket expected (after "if" or "while" etc) - see also BJ02 */
    public static final String BJ001 = "BJ001";
    
    /** Condition expected (after "if" or "while" etc) - occurs when a brace is found but a bracket was expected */
    public static final String BJ002 = "BJ002";
    
    /** Expected semicolon (various contexts); no other token would be valid */
    public static final String BJ003 = "BJ003";
}
