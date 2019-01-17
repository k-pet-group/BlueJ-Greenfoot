/*
 This file is part of the BlueJ program. 
 Copyright (C) 2019  Michael Kolling and John Rosenberg

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
package bluej.flow;

import bluej.editor.flow.Document;
import bluej.editor.flow.HoleDocument;
import bluej.editor.flow.SlowDocument;
import bluej.flow.gen.GenRandom;
import bluej.flow.gen.GenString;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.util.Random;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitQuickcheck.class)
public class TestDocument
{
    @Property
    public void propDocumentStringReplace(@From(GenRandom.class) Random r)
    {
        try
        {
            Document[] documents = new Document[]{new SlowDocument()};
            String curContent = "";
            GenString stringMaker = new GenString();
            // Perform 100 replacements:
            int lastInsert = 0;
            for (int i = 0; i < 100; i++)
            {
                int length = curContent.length();
                int start, end;
                // 20% chance of inserting at same position:
                if (r.nextInt(5) == 1)
                {
                    start = lastInsert;
                }
                else
                {
                    // Insert at very end of document is possible:
                    start = r.nextInt(length + 1);
                }
                // 50% chance of making an insert (replacing no content):
                if (r.nextInt(2) == 1 || start == length)
                {
                    end = start;
                }
                else
                {
                    end = start + r.nextInt(length - start);
                }
                String newContent = stringMaker.generate(new SourceOfRandomness(r), null);

                // Calculate desired content and check the document matches:
                curContent = curContent.substring(0, start) + newContent + curContent.substring(end);
                for (Document document : documents)
                {
                    document.replaceText(start, end, newContent);
                    assertEquals(curContent, document.getFullContent());
                }
            }
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
