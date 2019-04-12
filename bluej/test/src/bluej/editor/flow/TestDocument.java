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
package bluej.editor.flow;

import bluej.editor.flow.Document.Bias;
import bluej.editor.flow.gen.GenRandom;
import bluej.editor.flow.gen.GenString;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.When;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(JUnitQuickcheck.class)
public class TestDocument
{
    @Property(trials = 20, shrink = false)
    public void propDocumentStringReplace(@From(GenRandom.class) Random r)
    {
        class Pos
        {
            // One entry per document, all tracking the same position:
            final List<TrackedPosition> onePosPerDoc;
            final Bias bias;
            int lastPosition;
            
            public Pos(Document[] documents, int initialPos, Bias bias)
            {
                this.bias = bias;
                onePosPerDoc = Arrays.stream(documents).map(d -> d.trackPosition(initialPos, bias)).collect(Collectors.toList());
                this.lastPosition = initialPos;
            }
        }
        
        // Documents with identical content to test alongside each other:
        Document[] documents = new Document[] { new SlowDocument(), new HoleDocument() };
        String curContent = "";
        GenString stringMaker = new GenString();
        
        // One entry per tracked position;
        List<Pos> trackedPositions = new ArrayList<>();
        
        // Perform 100 replacements:
        int lastInsert = 0;
        for (int i = 0; i < 100; i++)
        {
            int length = curContent.length();

            if (r.nextInt(3) == 1)
            {
                // Track a random new position:
                int newTrackPos = r.nextInt(length + 1);
                Bias bias = Bias.values()[r.nextInt(Bias.values().length)];
                trackedPositions.add(new Pos(documents, newTrackPos, bias));
            }
            
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
            // What is the position if we kept on typing?
            lastInsert = start + newContent.length();
            
            // Update all the positions and check them:
            for (Pos pos : trackedPositions)
            {
                int was = pos.lastPosition;
                
                if (pos.lastPosition < start)
                {
                    // Position is before our change, nothing to alter
                }
                else if (pos.lastPosition < end || (pos.lastPosition == end && pos.bias != Bias.FORWARD))
                {
                    // Position is in the deleted region, set to start:
                    pos.lastPosition = start;
                }
                else
                {                    
                    // Position after change, and not in deleted region:
                    pos.lastPosition = pos.lastPosition - (end - start) + newContent.length();
                }

                // Check line and column info.  This is O(N^3) as we add around 30 positions, then
                // check them on each add, then scan whole document for each in case
                // of SlowDocument.  But ~100*100*2000 isn't that bad...
                int linesBefore = (int)curContent.substring(0, pos.lastPosition).codePoints().filter(n -> n == '\n').count();
                int column = pos.lastPosition - (1 + curContent.lastIndexOf('\n', pos.lastPosition - 1));

                for (TrackedPosition trackedPosition : pos.onePosPerDoc)
                {
                    assertEquals("Position was " + was + "," + pos.bias + " but then replaced " + start + "-" + end + " with " + newContent.length(), pos.lastPosition, trackedPosition.getPosition());
                    assertEquals(linesBefore, trackedPosition.getLine());
                    assertEquals(column, trackedPosition.getColumn());
                }
            }
            
            // Check a few line start and end positions:
            for (int j = 0; j < 10; j++)
            {
                int pos = r.nextInt(curContent.length() + 1);
                int line = documents[0].getLineFromPosition(pos);
                int column = documents[0].getColumnFromPosition(pos);

                for (Document document : documents)
                {
                    assertEquals("" + pos, line, document.getLineFromPosition(pos));
                    assertEquals("" + pos, column, document.getColumnFromPosition(pos));
                    assertEquals(pos, document.getLineStart(line) + column);
                    int nextNewLine = curContent.indexOf('\n', pos);
                    assertEquals(nextNewLine == -1 ? curContent.length() : nextNewLine, document.getLineEnd(line));
                    assertEquals(document.getLineLength(line), document.getLineEnd(line) - document.getLineStart(line) + (nextNewLine == -1 ? 0 : 1));
                }
            }
            
            // Check that no lines have \n, and re-assembling them makes full content:
            for (Document document : documents)
            {
                List<CharSequence> lines = document.getLines();
                for (CharSequence line : lines)
                {
                    assertFalse(line.codePoints().anyMatch(c -> c == '\n'));
                }
                assertEquals(curContent, lines.stream().collect(Collectors.joining("\n")));
                assertEquals(lines.size(), document.getLineCount());
            }
            
            // Check that document reader on a random sub-part does the right thing:
            int startRead = curContent.length() <= 1 ? 0 : r.nextInt(curContent.length() - 1);
            int endRead = curContent.length() == 0 ? 0 : r.nextInt(curContent.length() - startRead) + startRead;
            String expectedRead = curContent.substring(startRead, endRead);
            for (Document document : documents)
            {
                // Check getContent while we're at it:
                assertEquals(expectedRead, document.getContent(startRead, endRead));
                
                Reader reader = document.makeReader(startRead, endRead);
                try
                {
                    StringBuilder actual = new StringBuilder();
                    // Either use bulk version or single char version or a combination.
                    // We divide the read into sub-chunks and randomly decide for each chunk.
                    // This tests that the reader position is maintained correctly.
                    int numChunks = 1 + r.nextInt(5);
                    int[] chunkEnds = new int[numChunks];
                    chunkEnds[numChunks - 1] = endRead;
                    for (int j = 0; j < chunkEnds.length - 1; j++)
                    {
                        chunkEnds[j] = (endRead == startRead ? 0 : r.nextInt(endRead - startRead)) + startRead;
                    }
                    Arrays.sort(chunkEnds);
                    int curStart = startRead;

                    for (int j = 0; j < numChunks; j++)
                    {
                        if (r.nextBoolean())
                        {
                            // 50% chance: single char version
                            StringBuilder b = new StringBuilder();
                            for (; curStart < chunkEnds[j]; curStart++)
                            {
                                int c = reader.read();
                                assertNotEquals(-1, c);
                                actual.append((char) c);
                            }
                        }
                        else
                        {
                            // 50% chance: bulk read:
                            char[] large = new char[chunkEnds[j] - curStart + 20];
                            reader.read(large, 10, chunkEnds[j] - curStart);
                            actual.append(large, 10, chunkEnds[j] - curStart);
                            curStart = chunkEnds[j];
                        }
                    }
                    // Check our own logic:
                    assertEquals(endRead, curStart);
                    // Check output of reader:
                    assertEquals(document.getClass().toString(), expectedRead, actual.toString());
                }
                catch (IOException e)
                {
                    fail("IOException: " + e.getLocalizedMessage());
                }
            }
        }
    }
}
