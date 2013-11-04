package com.smartechz.core.stream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamReader {
    private String line = null;

    private BufferedReader buffRead = null;
    private InputStream inpStr = null;

    public StreamReader(InputStream inputStream) throws IOException
    {
        inpStr = inputStream;
        buffRead = new BufferedReader(new InputStreamReader(inputStream));
        line = buffRead.readLine();
    }

    public boolean hasNextLine()
    {
        return line != null;
    }

    public String nextLine() throws IOException
    {
        String ret = line;
        if (line != null) {
            line = buffRead.readLine();
        }
        return ret;
    }

    public void close() throws IOException
    {
        inpStr.close();
        buffRead.close();
    }
}
