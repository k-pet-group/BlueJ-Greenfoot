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

        return lines.toArray(new String[0]);
    }
    
    private void assertDiffRoundTrip(String[] orig, String[] mod) throws IOException, InterruptedException
    {
        // Get the diff using our library:
        Patch patch = DiffUtils.diff(Arrays.asList(orig), Arrays.asList(mod));
        String diff = DataCollector.makeDiff(patch);
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
        
        assertEquals("Patch exit code", 0, p.waitFor());
        
        //   Read back the original and check:
        String[] patched = readFile(tempFile);
        
        assertEqualStringArray(mod, patched);
    }
    
    
    private void assertEqualStringArray(String[] a, String[] b)
    {
        assertEquals("Array length: " + printArrays(a, b), a.length, b.length);
        for (int i = 0; i < a.length; i++)
            assertEquals("Line " + i + " for "  + printArrays(a, b), a[i], b[i]);
        
    }

    private String printArrays(String[] a, String[] b)
    {
        StringBuilder s = new StringBuilder();
        for (String line : a) s.append(line + "\n");
        s.append("###\n");
        for (String line : b) s.append(line + "\n");
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
}
