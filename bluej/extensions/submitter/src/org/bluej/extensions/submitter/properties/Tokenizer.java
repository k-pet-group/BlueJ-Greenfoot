package org.bluej.extensions.submitter.properties;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Turns an InputStream into readable information.
 * 
 * @author Clive Miller 
 * @version $Id: Tokenizer.java 1463 2002-10-23 12:40:32Z jckm $
 */
class Tokenizer
{
    static class Type {}
    static final Type END = new Type();
    static final Type BLOCK_START = new Type();
    static final Type BLOCK_END = new Type();
    static final Type CONFIG = new Type(); 
    static final Type EMPTY_SCHEME = new Type();

    private BufferedReader source;
    private String title, key, value;
    private char nextChar;
    private StringBuffer buf;
    private int lineNumber;
    private boolean endOfFile;
    private String currentLine;
    private int linePosition;
    
    Tokenizer (Reader source)
    {
        this.source = new BufferedReader (source);
        buf = new StringBuffer();
        lineNumber = 0;
        currentLine = "";
        linePosition = 1;
        endOfFile = false;
    }

    Type next() throws CompilationException
    {
        if (endOfFile) return END;
        Type token = null;
        readNext();
        skipWhiteSpace();
        if (endOfFile) token = END;
        else if (nextChar == '}') token = BLOCK_END;
        else if (parseConfig()) token = CONFIG;
        else if (parseString("\"{}=;/")) {
            if (nextChar == '{') {
                token = BLOCK_START;
                title = buf.toString();
            } else if (nextChar == ';') {
                token = EMPTY_SCHEME;
                title = buf.toString();
            } else {
                if (nextChar != '\"') pushBack();
                throw new CompilationException ("; or { expected", this);
            }
        }
        else throw new CompilationException ("Syntax Error", this);
        return token;
    }
    
    private boolean parseConfig() throws CompilationException
    {
        if (nextChar != '.') return false;
        parseString ("\"{}/*=!#\u00a3$%^&()+-[]@'~:;\\<>,"); // \u00a3 is a british pound symbol
        key = buf.toString().toLowerCase();
        if (nextChar != '=') throw new CompilationException ("= expected", this);
        readNext();
        parseString ("\";{}");
        if (nextChar != ';') {
            if (nextChar != '\"') pushBack();
            throw new CompilationException ("; expected", this);
        }
        value = buf.toString();
        return true;
    }
    
    private boolean parseString (String terminate) throws CompilationException
    {
        buf.setLength (0);
        skipWhiteSpace();
        if (nextChar == '"') {
            readNext();
            while (nextChar != '"') {
                buf.append (nextChar);
                readNext();
            }
            do {
                readNext();
            } while (nextChar == ' ');
            return true;
        }
        while (terminate.indexOf (nextChar) == -1) {
            if (nextChar != '\n') {
                buf.append (nextChar);
                }
            readNext();
        }
        return (buf.length() != 0);
    }
    
    private void skipWhiteSpace() throws CompilationException
    {
        boolean soak;
        do {
            soak = false;
            if (Character.isWhitespace (nextChar)) soak = true;
            else if (nextChar == '/') {
                if (lookAhead() == '/') {
                    do {
                        readNext();
                    } while (nextChar != '\n');
                    soak = true;
                }
                else if (lookAhead() == '*') {
                    do {
                        readNext();
                    } while (nextChar != '*' || lookAhead() != '/');
                    readNext();
                    soak = true;
                }
            }
            if (soak) readNext();
        } while (soak && !endOfFile);
    }
    
    private void readNext() throws CompilationException
    {
        char prevChar = nextChar;
        if (endOfFile) throw new CompilationException ("Unexpected End Of File", this);
        do {
            try {
                if (linePosition > currentLine.length()) {
                    lineNumber++;
                    currentLine = source.readLine();
                    linePosition = 0;
                    if (currentLine == null) {
                        endOfFile = true;
                        nextChar = 0;
                        return;
                    }
                }
                if (linePosition == currentLine.length()) {
                    linePosition++;
                    nextChar = '\n';
                } else {
                    nextChar = currentLine.charAt (linePosition++);
                    if (Character.isWhitespace (nextChar)) nextChar = 32;
                }
            } catch (IOException ex) {
                throw new CompilationException ("Trouble reading file: "+ex.toString(), this);
            }
        } while (Character.isWhitespace (prevChar) 
                && Character.isWhitespace (nextChar));
    }
    
    private char lookAhead()
    {
        if (linePosition < currentLine.length())
            return currentLine.charAt (linePosition);
        else
            return 0;
    }
    
    private void pushBack()
    {
        if (linePosition > 0) linePosition--;
    }
    
    String getCurrentLine()
    {
        return currentLine;
    }
    
    int getLinePosition()
    {
        return linePosition;
    }
    
    String getTitle()
    {
        String trim = title.trim();
        if (trim.charAt(0)=='#') trim = '#'+title.substring (1).trim();
        return trim;
    }
    
    String getKey()
    {
        return key.trim();
    }
    
    String getValue()
    {
        return value.trim();
    }
    
    int getLineNumber()
    {
        return lineNumber;
    }
}
