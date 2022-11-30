/*
 This file is part of the BlueJ program. 
 Copyright (C) 2011,2019  Michael Kolling and John Rosenberg 
 
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
package bluej.editor.flow;

import java.io.File;

import bluej.Config;
import bluej.utility.BlueJFileReader;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Wrapper for functionality around translating parser error codes to human-readable
 * error messages.
 * 
 * @author Davin McCall
 */
public class ParserMessageHandler
{
    /**
     * Translate an error code or message from the parser into a human-readable error message
     * in the appropriate language.
     */
    @OnThread(Tag.Any)
    public static String getMessageForCode(String code)
    {
        if (code.startsWith("BJ")) {
            // This looks like a BlueJ parser error code.
            File fileName = Config.getLanguageFile("bluejparser.help");
            String helpText = BlueJFileReader.readHelpText(fileName, code, true);
            if (helpText != null) {
                return helpText;
            }
        }
        return code;
    }
}
