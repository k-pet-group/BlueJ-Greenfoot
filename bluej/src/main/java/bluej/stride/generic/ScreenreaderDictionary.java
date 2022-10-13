/*
 This file is part of the BlueJ program.
 Copyright (C) 2021 Michael Kölling and John Rosenberg

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
package bluej.stride.generic;

import java.util.HashMap;

/**
 * @author Cherry Lim
 */
public class ScreenreaderDictionary {

    // ascii pronunciation guide: http://ascii-table.com/pronunciation-guide.php
    // java operators : https://www.w3schools.com/java/java_operators.asp
    private static HashMap<String, String> syntaxToTextMap = new HashMap<>() {{
        // braces
        put("("," left bracket ");
        put(")"," right bracket ");
        put("{"," left curly bracket ");
        put("}"," right curly bracket ");
        put("["," left square bracket ");
        put("]"," right square bracket ");
        // punctuation
        put("#"," hash ");
        put("_"," underscore ");
        put("@"," at sign ");
        put(":", " colon ");
        put(";"," semicolon ");
        put("."," dot ");
        put("\\"," back slash ");
        // arithmetic operators
        put("-"," minus "); // or negative or dash
        put("+"," plus ");
        put("*"," times ");
        put("/"," divided by ");
        put("^"," caret "); // POWER OR XOR
        put("%"," modulo ");
        put("++", " increment ");
        put("--", " decrement ");
        // assignment operators
        put("="," equal ");
        put("+=", " increment by ");
        put("-=", " decrement by ");
        put("*=", " multiplied itself by ");
        put("/=", " divided itself by "); // there's more
        // comparison operators
        put("==", " is equal to ");
        put("!=", " is not equal to ");
        put("<"," is less than "); // or part of List<> thingy
        put(">"," is greater than "); // List<> thingy
        put(">=", " is greater than or equal to ");
        put("<=", " is less than or equal to ");
        //logical operators
        put("&&", " logical and ");
        put("||", " logical or ");
        put("!", " logical not ");
        //bitwise
        put("|", " bitwise or ");
        put("&", " bitwise and ");
        put("~", " bitwise not ");
        put("<<", " zero-fill left shift ");
        put(">>", " signed right shift ");
        put(">>>", " zero-fill right shift");
        // idk
        put("..", " dot dot ");
        put("<:", " left pointy colon ");
        put("::", " double colon ");
        put("->", " right arrow ");
    }};

    public static String transcribeForScreenreader(String inText) {

        String outText;

        // operator
        if (syntaxToTextMap.containsKey(inText)) {
            outText = syntaxToTextMap.get(inText);
        } else{
            try {
                // float
                Float.parseFloat(inText); // throws exception if text isn't a float
                StringBuilder builder = new StringBuilder();
                for (char c : inText.toCharArray()) {
                    switch (c) {
                        case '-':
                            builder.append(" negative ");
                            break;
                        case '.':
                            builder.append(" point ");
                            break;
                        default:
                            builder.append(c);
                    }
                }
                outText = builder.toString();
            } catch (Exception e) {
                // just text probably
                StringBuilder builder = new StringBuilder();
                for (char c : inText.toCharArray()) {
                    if (Character.isUpperCase(c)) {
                        builder.append(" " + c); // separate by uppercase letters
                    } else {
                        builder.append(c);
                    }
                }
                outText = builder.toString();
            }
        }

        return outText;
    }


}
