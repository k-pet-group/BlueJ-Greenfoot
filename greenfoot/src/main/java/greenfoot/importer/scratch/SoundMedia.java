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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import bluej.utility.Debug;

/**
 * A sound clip, typically compressed as ADPCM.
 * 
 * 
 * @author neil
 *
 */
public class SoundMedia extends ScratchMedia
{
    //ADPCM step table, see Scratch/SmallTalk source code and
    //http://wiki.multimedia.cx/index.php?title=IMA_ADPCM
    private static final int[] STEP_TABLE = new int[] { 
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 
        19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 
        50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 
        130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
        337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
        876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 
        2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
        5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899, 
        15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767 
      };
    //Index table, see Scratch/SmallTalk source code
    private static final int[][] INDEX_TABLE = new int[][] {
        null, null, //0 and 1
        new int[] {-1, 2, -1, 2}, // 2
        new int[] {-1, -1, 2, 4, -1, -1, 2, 4}, // 3
        new int[] { // 4
        -1, -1, -1, -1, 2, 4, 6, 8,
        -1, -1, -1, -1, 2, 4, 6, 8},
        new int[] { // 8
        -1, -1, -1, -1, -1, -1, -1, -1, 1, 2, 4, 6, 8, 10, 13, 16,
        -1, -1, -1, -1, -1, -1, -1, -1, 1, 2, 4, 6, 8, 10, 13, 16}
    };
    private File destFile;
        
    public SoundMedia(int version, List<ScratchObject> scratchObjects)
    {
        super(SOUND_MEDIA, version, scratchObjects);
    }
    
    // Fields:
    //   originalSound, volume, balance, compressedSampleRate, compressedBitsPerSample, compressedData
    
    @Override
    public int fields()
    {
        return super.fields() + 6;
    }

    @Override
    public File saveInto(File destDir, Properties props, String prefix) throws IOException
    {
        if (destFile != null) return destFile;
        
        String name = getMediaName();
        
        // The code for this method is cobbled together from the Scratch/SmallTalk code
        // and this page: http://wiki.multimedia.cx/index.php?title=IMA_ADPCM
        
        float sampleRate = getSampleRate();
        //bits per sample can be 2, 3, 4, 5
        int bitsPerSample = getBitsPerSample();
        byte[] compressed = getCompressedSamples();
        
        if (compressed == null)
            return null; // TODO must be uncompressed?
        
        int uncompressedSamples = (compressed.length * 8) / bitsPerSample; // Length in samples
        byte[] uncompressed = new byte[uncompressedSamples * 2]; // * 2 because we use 16-bits (2 bytes) per sample
        
        short prev = 0;
        int stepIndex = 0;
        int step = 0;
        
        int byteIndex = 0;
        // Starts at 7, comes down to 0:
        int bitIndex = 8 - bitsPerSample;
        int destSample = 0;
        
        while (byteIndex < compressed.length) {
            int unsignedCompressedVal;
            if (bitIndex > 8 - bitsPerSample) {
                // We need bits from across two bytes -- this byte and the byte before!               
                unsignedCompressedVal = (compressed[byteIndex] >> bitIndex) & ((1 << (8 - bitIndex)) - 1);
                unsignedCompressedVal |= (compressed[byteIndex-1] & ((1 << (bitsPerSample - (8 - bitIndex))) - 1)) << (8 - bitIndex);
            } else {
                unsignedCompressedVal = (compressed[byteIndex] >> bitIndex) & ((1 << bitsPerSample) - 1);
            }
            if (bitIndex - bitsPerSample < 0) {
                byteIndex += 1;
                bitIndex = 8 + bitIndex - bitsPerSample;
            } else {
                bitIndex -= bitsPerSample;
            }                        
            stepIndex = Math.max(0,Math.min(88,stepIndex + INDEX_TABLE[bitsPerSample][unsignedCompressedVal]));
            // This code is copied from the Scratch algorithm
            // (see Sound-Synthesis.ADPCMCodec.privateDecodeMono),
            // hence all the bit-twiddling.
            int diff = 0;
            for (int bit = 1 << (bitsPerSample - 2); bit != 0; bit >>= 1) {
                if ((unsignedCompressedVal & bit) != 0) {
                    diff += step;
                }
                step >>= 1;
            }
            // The high bit appears to be a sign bit:
            if ((unsignedCompressedVal & (1 << (bitsPerSample - 1))) != 0)
                diff = -diff;
            prev = (short)Math.max(-32768, Math.min(32767, (int)prev + diff));
            uncompressed[destSample * 2] = (byte)(prev >> 8);
            uncompressed[(destSample * 2) + 1] = (byte)(prev & 255);
            destSample += 1;
            
            step = STEP_TABLE[stepIndex];
            
        }
        
        
        File soundsDir = new File(destDir, "sounds");
        soundsDir.mkdirs();
        destFile = new File(soundsDir, prefix + name + ".wav");
        
        ByteArrayInputStream baiStream = new ByteArrayInputStream(uncompressed);
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
        AudioInputStream aiStream = new AudioInputStream(baiStream,format,uncompressed.length);
        try {
            
            AudioSystem.write(aiStream,AudioFileFormat.Type.WAVE,destFile);
            aiStream.close();
            baiStream.close();
        }
        catch (IOException e) {
            Debug.reportError("Problem writing converted sound to WAV file", e);
        }
        
        return destFile;
    }

    private byte[] getCompressedSamples()
    {
        ScratchObject obj = scratchObjects.get(super.fields() + 5);
        if (obj == null) {
            return null;
        } else {
            return (byte[]) obj.getValue();
        }
    }

    private int getBitsPerSample()
    {
        ScratchObject obj = scratchObjects.get(super.fields() + 4);
        if (obj == null) {
            return 0;
        } else {
            return ((BigDecimal)obj.getValue()).intValue();
        }
    }

    private float getSampleRate()
    {
        ScratchObject obj = scratchObjects.get(super.fields() + 3);
        if (obj == null) {
            return 0;
        } else {
            return ((BigDecimal)obj.getValue()).floatValue();
        }
    }

    
    
}
