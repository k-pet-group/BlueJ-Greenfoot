import java.util.*;

import static java.lang.Character.*;

/**
 * Translate a String into Morse code.
 * 
 * Maps lower case letters to upper case.
 * Returned string contains the Morse equivalent of the parameter, 
 * with codons sperated by slashes, and spaces represented by spaces..
 * 
 * @author Ian Utting
 * @version 1.0
 */

public class StringToMorse
{
    private static final Map<Character, String> code = new HashMap<>();
        static {
            // Upper case letters
            /* A */ code.put('A', ".-");
            /* B */ code.put('B', "-...");
            /* C */ code.put('C', "-.-.");
            /* D */ code.put('D', "-..");
            /* E */ code.put('E', ".");
            /* F */ code.put('F', "..-.");
            /* G */ code.put('G', "--.");
            /* H */ code.put('H', "....");
            /* I */ code.put('I', "..");
            /* J */ code.put('J', ".---");
            /* K */ code.put('K', "-.-");
            /* L */ code.put('L', ".-..");
            /* M */ code.put('M', "--");
            /* N */ code.put('N', "-.");
            /* O */ code.put('O', "---");
            /* P */ code.put('P', ".--.");
            /* Q */ code.put('Q', "--.-");
            /* R */ code.put('R', ".-.");
            /* S */ code.put('S', "...");
            /* T */ code.put('T', "-");
            /* U */ code.put('U', "..-");
            /* V */ code.put('V', "...-");
            /* W */ code.put('W', ".--");
            /* X */ code.put('X', "-..-");
            /* Y */ code.put('Y', "-.--");
            /* Z */ code.put('Z', "--..");

            // Lower case letters map to the same values as upper case
            /* A */ code.put('a', ".-");
            /* B */ code.put('b', "-...");
            /* C */ code.put('c', "-.-.");
            /* D */ code.put('d', "-..");
            /* E */ code.put('e', ".");
            /* F */ code.put('f', "..-.");
            /* G */ code.put('g', "--.");
            /* H */ code.put('h', "....");
            /* I */ code.put('i', "..");
            /* J */ code.put('j', ".---");
            /* K */ code.put('k', "-.-");
            /* L */ code.put('l', ".-..");
            /* M */ code.put('m', "--");
            /* N */ code.put('n', "-.");
            /* O */ code.put('o', "---");
            /* P */ code.put('p', ".--.");
            /* Q */ code.put('q', "--.-");
            /* R */ code.put('r', ".-.");
            /* S */ code.put('s', "...");
            /* T */ code.put('t', "-");
            /* U */ code.put('u', "..-");
            /* V */ code.put('v', "...-");
            /* W */ code.put('w', ".--");
            /* X */ code.put('x', "-..-");
            /* Y */ code.put('y', "-.--");
            /* Z */ code.put('z', "--..");
            
            // Digits
            /* 0 */ code.put('0', "-----");
            /* 1 */ code.put('1', ".----");
            /* 2 */ code.put('2', "..---");
            /* 3 */ code.put('3', "...--");
            /* 4 */ code.put('4', "....-");
            /* 5 */ code.put('5', ".....");
            /* 6 */ code.put('6', "-....");
            /* 7 */ code.put('7', "--...");
            /* 8 */ code.put('8', "---..");
            /* 9 */ code.put('9', "----.");            
            
            // punctuation
            /* . */ code.put('.', ".-.-.-");
            /* , */ code.put(',', "--..--");
            /* : */ code.put(':', "---...");
            /* ? */ code.put('?', "..--..");
            /* ' */ code.put('\'', ".----.");
            /* - */ code.put('-', "-....-");
            /* / */ code.put('/', "-..-.");
            /* ( */ code.put('(', "-.--.-");
            /* ) */ code.put(')', "-.--.-");
            /* " */ code.put('"', ".-..-.");
            /* @ */ code.put('@', ".--.-.");
            /* = */ code.put('=', "-...-");
            
            // Special code for a space character.
                    code.put(' ', " ");
        }
        
    /**
     * Translate a string into its Morse code equivalent
     * 
     * Spaces in message are encoded as spaces in the returned string. Codons are separated by slashes
     * 
     * @param message   The string to be translated
     * @return The encoded string.
     * @throws IllegalArgumentException  if there is a character in message which is not codeable in Morse
     */    
    public static String translate(String message) {
        String result = new String();
        
        for(char c : message.toCharArray()) {
            if (code.containsKey(c)) result = result + code.get(c) + "/";
            else throw new IllegalArgumentException("Character " + c + "is not in Morse's code");
        }
        // trim the trailing slash
        result = result.substring(0, result.length() - 1);
        return result;
    }
}