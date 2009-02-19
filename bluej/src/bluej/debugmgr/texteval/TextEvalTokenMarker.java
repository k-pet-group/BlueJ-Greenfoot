/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.debugmgr.texteval;

import org.syntax.jedit.KeywordMap;
import org.syntax.jedit.tokenmarker.CTokenMarker;

/**
 * Text Evaluator token marker.
 *
 * @author Michael Kolling
 * @version $Id: TextEvalTokenMarker.java 6163 2009-02-19 18:09:55Z polle $
 */
public class TextEvalTokenMarker extends CTokenMarker
{
    private static KeywordMap keywords;

    public TextEvalTokenMarker()
    {
        super(false, getKeywords());
    }

    public static KeywordMap getKeywords()
    {
        if(keywords == null) {
            keywords = new KeywordMap(false);
        }
        return keywords;
    }
}
