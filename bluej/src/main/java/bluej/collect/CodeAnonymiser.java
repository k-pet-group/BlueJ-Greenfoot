/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2016  Michael Kolling and John Rosenberg
 
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
package bluej.collect;

import java.io.IOException;
import java.io.StringReader;

import bluej.parser.lexer.JavaLexer;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.utility.Debug;
import bluej.utility.Utility;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParsingException;
import nu.xom.Text;
import threadchecker.OnThread;
import threadchecker.Tag;

//package-visible
@OnThread(Tag.FXPlatform)
class CodeAnonymiser
{
    public static String anonymise(String sourceCode)
    {
        if (sourceCode.startsWith("<?xml"))
            return anonymiseStride(sourceCode);
        else
            return anonymiseJava(sourceCode);
    }

    private static String anonymiseStride(String sourceCode)
    {
        try
        {
            Element topLevel = new Element(new Builder().build(new StringReader(sourceCode)).getRootElement());

            for (int i = 0; i < topLevel.getChildElements().size(); i++)
            {
                Element sub = topLevel.getChildElements().get(i);
                if (sub.getLocalName().equals("javadoc"))
                {
                    // Should only really be one child node, a Text node, but we do all:
                    for (int j = 0; j < sub.getChildCount(); j++)
                    {
                        Node childofJavadoc = sub.getChild(j);
                        if (childofJavadoc instanceof Text)
                        {
                            Text text = (Text)childofJavadoc;
                            text.setValue(replaceWords(text.getValue()));
                        }
                    }
                }
            }

            return Utility.serialiseCodeToString(topLevel);
        }
        catch (ParsingException | IOException e)
        {
            Debug.reportError(e);
            // If we run into problems, we'll have to leave as-is:
            return sourceCode;
        }
    }


    private static String anonymiseJava(String sourceCode)
    {
        StringBuilder result = new StringBuilder();
        
        int importOrPackageLine;
        
        JavaLexer lexer = new JavaLexer(new StringReader(sourceCode));
        lexer.setGenerateWhitespaceTokens(true);
        
        importOrPackageLine = -1;
        for (LocatableToken token = lexer.nextToken(); token.getType() != JavaTokenTypes.EOF; token = lexer.nextToken())
        {   
            switch (token.getType())
            {
            case JavaTokenTypes.ML_COMMENT:
            case JavaTokenTypes.SL_COMMENT:
                result.append(replaceWords(token.getText()));
                break;
            case JavaTokenTypes.WHITESPACE:
                result.append(token.getText());
                break;
            case JavaTokenTypes.LITERAL_import:
            case JavaTokenTypes.LITERAL_package:
                // Count all tokens on rest of line as part of declaration, regardless
                // of syntax:
                importOrPackageLine = token.getLine();
                result.append(token.getText());
                break;
            default:
                if (token.getLine() == importOrPackageLine)
                {
                    result.append(token.getText());
                }
                else
                {
                    // Start of real program; copy everything else from hereon in:
                    result.append(sourceCode.substring(token.getPosition()));
                    // Done:
                    return result.toString();
                }
                break;
            }
        }
        
        return result.toString();
    }

    private static String replaceWords(String substring)
    {
        StringBuilder s = new StringBuilder(substring.length());
        int i = 0;
        while (i < substring.length())
        {
            int codePoint = substring.codePointAt(i);
            
            if (Character.isLetterOrDigit(codePoint))
            {
                s.append("#");
            }
            else
            {
                s.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }
        return s.toString();
    }
}
