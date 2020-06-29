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
        put("~"," tilde ");
        put("_"," underscore ");
        put("@"," at sign ");
        put(";"," semicolon ");
        put("."," dot "); // or period, point
        put("\\"," back slash ");
        // arithmetic operators
        put("-"," minus "); // or negative or dash
        put("+"," plus ");
        put("*"," times "); // or asterisk
        put("/"," divided by "); // or forward slash
        put("%"," modulo "); // or percent
        put("++", " increment ");
        put("--", " decrement ");
        // assignment operators
        put("="," equal ");
        put("+=", " increment by ");
        put("-=", " decrement by ");
        put("*=", " multiplied by ");
        put("/=", " divided by "); // there's more
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
        put("!", " logical not "); // or exclam

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
