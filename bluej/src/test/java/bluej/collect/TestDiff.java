/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014  Michael Kolling and John Rosenberg 
 
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;
import difflib.DiffUtils;
import difflib.Patch;

public class TestDiff extends TestCase
{
    private String[] readFile( File file ) throws IOException {
        BufferedReader reader = new BufferedReader( new FileReader (file));
        String         line = null;
        ArrayList<String> lines = new ArrayList<String>();
        
        while( ( line = reader.readLine() ) != null ) {
            lines.add(line);
        }
        
        reader.close();
        return lines.toArray(new String[0]);
    }
    
    private void assertDiffRoundTrip(String[] orig, String[] mod) throws IOException, InterruptedException
    {
        // Get the diff using our library:
        Patch patch = DiffUtils.diff(Arrays.asList(orig), Arrays.asList(mod));
        String diff = DataCollectorImpl.makeDiff(patch);
        // Now send it on a round trip with the system diff.
        
        //   Make temp file and fill it with original:
        File tempFile = File.createTempFile("SRC", ".java");
        FileWriter fileWriter = new FileWriter(tempFile);
        BufferedWriter bufferedWriter = new BufferedWriter( fileWriter);
        for (String line : orig)
        {
            bufferedWriter.write(line + "\n");
        }
        bufferedWriter.close();
        fileWriter.close();
        
        //   Run patch, and feed it the diff:
        Process p = Runtime.getRuntime().exec("patch --force " + tempFile.getAbsolutePath());
        p.getOutputStream().write(diff.getBytes(Charset.forName("UTF-8")));
        p.getOutputStream().close();

        int returnCode = p.waitFor();        
        assertEquals("Patch exit code", 0, returnCode);
        
        //   Read back the original and check:
        String[] patched = readFile(tempFile);
        
        assertEqualStringArray(orig, diff, mod, patched);
    }
    
    
    private void assertEqualStringArray(String[] orig, String diff, String[] a, String[] b)
    {
        assertEquals("Array length: " + printInfo(orig, diff, a, b), a.length, b.length);
        for (int i = 0; i < a.length; i++)
            assertEquals("Line " + i + " for\n"  + printInfo(orig, diff, a, b), a[i], b[i]);
        
    }

    private String printInfo(String[] orig, String diff, String[] a, String[] b)
    {
        StringBuilder s = new StringBuilder();
        s.append("### Original:\n");
        for (String line : orig) s.append(line + "\n");
        s.append("### Diff:\n").append(diff).append("### Expected:\n");
        for (String line : a) s.append(line + "\n");
        s.append("### Actual:\n");
        for (String line : b) s.append(line + "\n");
        s.append("###\n");
        return s.toString();
    }

    public void testInsert() throws IOException, InterruptedException
    {
        assertDiffRoundTrip(new String[] {
"class Foo",
"{",
"}"}, new String[] {
"class Foo",
"{",
"  public int x;",
"}"});
    }
    
    // This test can take a little while -- 75 seconds on my machine
    /*
    public void testBruteForceDiffs() throws IOException, InterruptedException
    {
        String[] choices = new String[] {"aaaa", "bbbb", ""};
        //Forms all files of length 0 to 4 with all possible combinations of those three lines:
        final int LONGEST_FILE = 4;
        Collection<String[]>[] allFiles = new Collection[LONGEST_FILE + 1];
        
        allFiles[0] = new ArrayList<String[]>();
        allFiles[0].add(new String[0]);
        
        for (int length = 1; length <= LONGEST_FILE; length++)
        {
            allFiles[length] = new ArrayList<String[]>();
            // For that length, create all possible files by building on all files of previous length:
            for (int chosenNew = 0; chosenNew < choices.length; chosenNew++)
            {
                for (String[] shorterFile : allFiles[length - 1])
                {
                    String[] newFile = Arrays.copyOf(shorterFile, length);
                    newFile[length - 1] = choices[chosenNew];
                    allFiles[length].add(newFile);
                }
            }
        }
        
        // Now flatten:
        ArrayList<String[]> flattened = new ArrayList<String[]>();
        for (Collection<String[]> coll : allFiles)
        {
            for (String[] f : coll)
            {
                flattened.add(f);
            }
        }
        allFiles = null;
        
        for (int i = 0; i < flattened.size(); i++)
        {
            for (int j = 0; j < flattened.size(); j++)
            {
                assertDiffRoundTrip(flattened.get(i), flattened.get(j));
            }
        }
    }
    */
}
