/*
 * Blackbox Code Viewer sample
 * Copyright (c) 2012, Neil Brown
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bluej.utility.Utility;
import difflib.DiffUtils;
import difflib.Patch;


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
    
    public String getStateAfter(String stateBefore) throws Exception
    {
        if (type.equals("complete"))
        {
            return content;
        }
        else if (type.equals("diff") && stateBefore != null)
        {
            // DiffUtils library expects the three-minus, three-plus header -- we don't record
            // it to save space, but we trivially add it back on here to please the library:
            Patch diff = DiffUtils.parseUnifiedDiff(toLines("---\n+++\n" + content));
            if (diff != null)
            {
                List<String> result = (List<String>) DiffUtils.patch(toLines(stateBefore), diff);
            
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
        }
        //Currently doesn't support renames and deletes
        
        //Whinge if something goes wrong:
        throw new Exception("Could not do diff: " + stateBefore + " type: " + type + " content: \"" + content + "\"");
    }

    // Gets a version by applying all versions since the beginning (could be expensive):
    static String getVersion(ArrayList<SourceHistory> history, int version) throws Exception
    {
        String s = null;
        for (int i = 0; i <= version; i++)
        {
            s = history.get(i).getStateAfter(s);
        }
        return s;
    }
}