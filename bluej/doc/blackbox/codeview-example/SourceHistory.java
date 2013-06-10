/*
 * Blackbox Code Viewer sample
 * Copyright (c) 2012, Neil Brown
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.io.*;
import java.nio.charset.Charset;

import bluej.utility.Utility;


/**
 * Mirrors an entry in the source_histories table in the database
 */
public class SourceHistory
{
    public String type;
    public String content;
    
    // Convert String to lines (without line-termination characters)
    private static List<String> toLines(String s)
    {
        return Arrays.asList(Utility.splitLines(s));
    }

    private static String[] readFile( File file ) throws IOException
    {
        BufferedReader reader = new BufferedReader( new FileReader (file));
        String         line = null;
        ArrayList<String> lines = new ArrayList<String>();
        
        while( ( line = reader.readLine() ) != null ) {
            lines.add(line);
        }

        return lines.toArray(new String[0]);
    }

    
    private String getStateAfter(String stateBefore) throws Exception
    {
        if (type.equals("complete"))
        {
            return content;
        }
        else if (type.equals("diff") && stateBefore != null)
        {
        //   Make temp file and fill it with original:
        File tempFile = File.createTempFile("SRC", ".java");
        FileWriter fileWriter = new FileWriter(tempFile);
        BufferedWriter bufferedWriter = new BufferedWriter( fileWriter);
        bufferedWriter.write(stateBefore);
        bufferedWriter.close();
        fileWriter.close();
        
        //   Run patch, and feed it the diff:
        Process p = Runtime.getRuntime().exec("patch --force " + tempFile.getAbsolutePath());
        p.getOutputStream().write(content.getBytes(Charset.forName("UTF-8")));
        p.getOutputStream().close();

        p.waitFor();
        
        //   Read back the original and check:
        String[] result = readFile(tempFile);
            
                if (result != null)
                {
                    // Re-assemble the patched lines into a single String:
                    StringBuilder sb = new StringBuilder();
                    for (String s : result)
                    {
                        sb.append(s).append("\n");
                    }
                    return sb.toString();
                }
            
        }       
        //Currently doesn't support renames and deletes
        
        //Whinge if something goes wrong:
        throw new Exception("Could not do diff: " + stateBefore + " type: " + type + " content: \"" + content + "\"");
    }

    // Gets all versions of a file:
    static ArrayList<String> getAllVersions(ArrayList<SourceHistory> history) throws Exception
    {
        ArrayList<String> r = new ArrayList<String>();
        String s = null;
        for (SourceHistory h : history)
        {
            s = h.getStateAfter(s);
            r.add(s);
        }
        return r;
    }
}
