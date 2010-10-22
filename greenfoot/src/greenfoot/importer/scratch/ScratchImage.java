/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.importer.scratch;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;

/**
 * A Scratch image resource, loaded from the Scratch Form class.
 * 
 * @author neil
 *
 */
public class ScratchImage extends ScratchObject
{
    // The ScratchObject representing the bits.  Almost certainly an object reference
    // When it is resolved, the actual image will be stored in the "img" field
    private ScratchObject bitsRef;
    private int w;
    private int h;
    private int d;
    private int offset;
    // The image, only valid after resolve is called
    private BufferedImage img;
    // The ScratchObject representing the palette.  Almost certainly an object reference
    // When it is resolved, the actual image will be stored in the "palette" field
    private ScratchObject paletteRef;
    private Color[] palette;
    
    private boolean isResolved = false;
    
    /**
     * Constructs a ScratchImage using the data read from the file
     * @param w Width of image
     * @param h Height of image
     * @param d Depth (number of *bits* (not bytes) per pixel)
     * @param offset Offset (TODO work out how this is used)
     * @param bitsRef Reference to byte array with raw compressed data
     * @param paletteRef Reference to palette (array of colours)
     */
    public ScratchImage(int w, int h, int d, int offset, ScratchObject bitsRef, ScratchObject paletteRef)
    {
        this.w = w;
        this.h = h;
        this.d = d;
        this.offset = offset;
        this.bitsRef = bitsRef;
        this.paletteRef = paletteRef;            
    }

    /**
     * Resolves the references for bits and palette, and decodes the image
     */
    public ScratchObject resolve(ArrayList<ScratchObject> objects) {
        if (isResolved) return this;
        
        if (paletteRef != null) {
            ScratchObject resolvedPalette = paletteRef.resolve(objects);
            ScratchObject[] pal = (ScratchObject[])resolvedPalette.getValue();
            
            palette = new Color[pal.length];
            for (int i = 0;i < pal.length;i++) {
                palette[i] = (Color)pal[i].resolve(objects).getValue();
            }
        }
        
        // The compression scheme is documented in the 
        // Graphics-Primitives.Bitmap.compress:toByteArray: method
        
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
               
        ScratchObject resolved = bitsRef.resolve(objects);
        
        if (resolved.getValue() instanceof int[]) {
            // Uncompressed:
            int[] values = (int[])resolved.getValue();
            for (int pos = 0; pos < values.length;pos++) {
                setBitmapEntry(pos, values[pos]);
            }
            
        } else if (resolved.getValue() instanceof byte[]) {
            //Compressed, need to decompress:
            ByteArrayInputStream bitsInput = new ByteArrayInputStream((byte[]) resolved.getValue());
            
            
    
            int bitmapPos = 0;
            // Skip the length at the very beginning of the image
            // It tells us final size but we already know that:
            decodeLen(bitsInput);
            for (int rawN = decodeLen(bitsInput);rawN != -1;rawN = decodeLen(bitsInput)) {
                // Lowest two bits contain a 0-3 code
                // Rest of bits contain a count of (4-byte) words:
                int wordCount = rawN >> 2;
                switch (rawN & 0x3) {
                case 0: // Skip that many words
                    bitmapPos += wordCount;
                break;
                case 1: { // Replicate next byte to all 4 bytes to wordCount words:
                    int b = bitsInput.read();
                    int x = (b << 24) | (b << 16) | (b << 8) | b;
                    int end = bitmapPos + wordCount;
                    while (bitmapPos < end) {
                        setBitmapEntry(bitmapPos++, x);
                    }
                }
                break;
                case 2: { //Replicate next 4 bytes to wordCount words: 
                    int x = 0;
                    for (int i = 0; i < 4; i++) {
                        x <<= 8;
                        x |= bitsInput.read();
                    }
                    int end = bitmapPos + wordCount;
                    while (bitmapPos < end) {
                        setBitmapEntry(bitmapPos++, x);
                    }
                }
                break;
                case 3: { //Exact wordCount words follows (no repetition) 
                    int end = bitmapPos + wordCount;
                    while (bitmapPos < end) {
                        int x = 0;
                        for (int i = 0; i < 4; i++) {
                            x <<= 8;
                            x |= bitsInput.read();
                        }
                        
                        setBitmapEntry(bitmapPos++, x);
                    }
                }
                break;
                }
            }
        }
        
        isResolved = true;

        return this;
    }
    
    private void setBitmapEntry(int pos, int val)
    {
        final int pixelsPerWord = 32 / d;
        for (int i = 0; i < pixelsPerWord;i++) {
            int index = d == 32 ? val : val & ((1 << d) - 1);
            
            // Number of pixels per row divided by pixels-per-word gives number of words per row:
            int realWidth = (w + (pixelsPerWord - 1)) / pixelsPerWord;
            
            // Take remainder from word-position by words per row to get
            // index of words into row, then times by pixels-per-word to get number of pixels,
            // then add i (number of pixels into word)
            int x = ((pos % realWidth) * pixelsPerWord) + (pixelsPerWord - i);
            // Divide word-position by words per row to get row 
            int y = pos / realWidth;
            // Some bits can be beyond the image due to aligning the images to nice 2^n sizes:
            if (x < w && y < h) {
                if (palette != null) {
                    img.setRGB(x, y, palette[index].getRGB());
                } else {
                    // If the alpha is zero but the other channels are not,
                    // set alpha to 255.  I can't find any part of the Scratch code
                    // that does this, but it's the only rule I can find
                    // that seems to make the images work correctly.
                    if (index >> 24 == 0 && (index & 0xFFFFFF) != 0) {
                        index |= 0xFF000000;
                    }
                    img.setRGB(x, y, index);
                }
            }
            
            val >>= d;
        }
    }


    /**
     * Decodes a count field.
     * Anything above 0xE0 has its low bits (& 0x1F) merged with the next number
     */
    private int decodeLen(ByteArrayInputStream bitsInput)
    {
        int x = 0;
        int first = bitsInput.read();
        
        if (first == -1) //EOF
            return -1;
        
        if (first == 0xFF) {
            for (int i = 0; i < 4; i++) {
                x <<= 8;
                x |= bitsInput.read();
            }
            return x;
        } else if (first >= 0xE0) {
            x = (first & 0x1F) << 8;
            x |= bitsInput.read();
        } else {
            x = first;
        }
        return x;
    }

    public int getWidth()
    {
        return w;
    }

    public int getHeight()
    {
        return h;
    }

    public BufferedImage getBufferedImage()
    {
        return img;
    }
    
}