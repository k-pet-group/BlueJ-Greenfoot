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
import java.util.ArrayList;

/**
 * A Scratch image resource.
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
    
    // The byte array with the raw (compressed) data, and current pointer into it
    // This is shared between methods, so is a member.
    // TODO refactor this to a ByteArrayInputStream
    private byte[] bits;
    private int bitsPos;
    
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
        ScratchObject resolved = bitsRef.resolve(objects);
        bits = (byte[]) resolved.getValue();
        
        if (paletteRef != null) {
            resolved = paletteRef.resolve(objects);
            ScratchObject[] pal = (ScratchObject[])resolved.getValue();
            
            palette = new Color[pal.length];
            for (int i = 0;i < pal.length;i++) {
                palette[i] = (Color)pal[i].resolve(objects).getValue();
            }
        }
        
        // The compression scheme is documented in the 
        // Graphics-Primitives.Bitmap.compress:toByteArray: method
        
        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        bitsPos = 0;

        int bitmapPos = 0;
        decodeLen();
        while (bitsPos < bits.length) {
            final int len = decodeLen();
            // Lowest two bits contain a 0-3 code
            // Rest of bits contain a count of (4-byte) words:
            int wordCount = (len & (~0x3)) >> 2;
            switch (len & 0x3) {
            case 0: // Skip that many words
                bitmapPos += wordCount * 4;
            break;
            case 1: { // Replicate next byte to all 4 bytes to wordCount words:
                int b = bits[bitsPos++]&0xFF;
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
                    x |= bits[bitsPos++]&0xFF;
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
                        x |= bits[bitsPos++]&0xFF;
                    }
                    
                    setBitmapEntry(bitmapPos++, x);
                }
            }
            break;
            }
        }

        return this;
    }
    
    private void setBitmapEntry(int pos, int val)
    {
        final int pixelsPerWord = 32 / d;
        for (int i = 0; i < pixelsPerWord;i++) {
            int index = val & ((1 << d) - 1);
            
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
                img.setRGB(x, y, palette[index].getRGB());
            }
            
            val >>= d;
        }
    }


    /**
     * Decodes a count field.
     * Anything above 0xE0 has its low bits (& 0x1F) merged with the next number
     */
    private int decodeLen()
    {
        // The b & 0xFF trick (where b is a byte) turns a byte into
        // an int by treating the byte as *unsigned* which is crucial here.
        int x = 0;
        if ((bits[bitsPos]&0xFF) >= 0xE0) {
            x = ((bits[bitsPos++]&0xFF) & 0x1F) << 8;
            x |= bits[bitsPos++]&0xFF;
        } else {
            x = bits[bitsPos++]&0xFF;
        }
        return x;
    }

    public BufferedImage getBufferedImage()
    {
        return img;
    }   
    
}