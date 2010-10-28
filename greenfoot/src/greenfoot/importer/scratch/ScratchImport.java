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


import greenfoot.core.GreenfootMain;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import bluej.pkgmgr.PackageFile;
import bluej.pkgmgr.PackageFileFactory;
import bluej.utility.Debug;

public class ScratchImport
{   
    /**
     * Reads a fixed number of bytes, treats them as ASCII, and returns them as a String
     */
    private static String readFixedASCII(FileInputStream input, int num) throws IOException
    {
        byte[] b = new byte[num];
        input.read(b);
        return new String(b, Charset.forName("US-ASCII"));
    }
    
    /**
     * Reads a fixed number of bytes, treats them as UTF8, and returns them as a String
     */
    private static String readUTF8(FileInputStream input, int num) throws IOException
    {
        byte[] b = new byte[num];
        input.read(b);
        return new String(b, Charset.forName("UTF-8"));
    }
    
    /**
     * Reads the version string from the file
     */
    private static void readVersion(FileInputStream input) throws IOException 
    {
        String ver = readFixedASCII(input, 10);
        if ("ScratchV01".equals(ver)) {
            //Version 1
        } else if ("ScratchV02".equals(ver)) {
            //Version 2
        } else {
            Debug.message("Unknown Scratch version: " + ver);
        }
    }
    
    /**
     * Reads a big-endian int using the given number of bytes
     * 
     * The Scratch format has all sorts of integer sizes, including 3 bytes.
     */
    private static long readInt(FileInputStream input, int bytes) throws IOException
    {
        long x = 0;
        for (int i = 0; i < bytes; i++)
        {
            x <<= 8;
            x |= input.read();
        }
        
        // Fix negative numbers when less than 8-bytes:
        if (x >> ((8 * bytes) - 1) != 0 && bytes < 8) {
            // When upper bit is one, perform sign extension:
            x |= 0xFFFFFFFFFFFFFFFFL << (8 * bytes);
        }
        
        return x;
    }

    /**
     * Reads the header from a Scratch file (the version, and the info block, which is skipped)
     */
    private static void readHeader(FileInputStream input) throws IOException
    {
        readVersion(input);
        int infoSize = (int)readInt(input, 4);
        input.skip(infoSize);
    }
    
    private static ScratchObject readObject(FileInputStream input) throws IOException
    {
        int id = input.read();
        if (id == -1)
            return null;
        
        if (id >= 100) {
            //User Object
            return readUserObject(id, input);
        } else {
            //Primitive (fixed-format) object, or reference:
            return readPrimitiveOrReferenceWithGivenId(id, input);
        }
    }
    
    // See Scratch Object IO.ObjStream.readObjectRecord
    private static ScratchUserObject readUserObject(int id, FileInputStream input) throws IOException
    {
        int version = input.read();
        int fieldAmount = input.read();
        
        List<ScratchObject> scratchObjects = Arrays.asList(readFields(input, fieldAmount));
        
        switch (id) {
        case ScratchUserObject.IMAGE_MEDIA:
            return new ImageMedia(version, scratchObjects);
        case ScratchUserObject.SOUND_MEDIA:
            return new SoundMedia(version, scratchObjects);
        case ScratchUserObject.SCRATCH_STAGE_MORPH:
            return new ScratchStageMorph(version, scratchObjects);
        case ScratchUserObject.SCRATCH_SPRITE_MORPH:
            return new ScratchSpriteMorph(version, scratchObjects);
        default: 
            return new ScratchUserObject(id, version, scratchObjects);
        }
    }
    
    private static ScratchObject readPrimitiveOrReference(FileInputStream input) throws IOException
    {
        int id = input.read();
        return readPrimitiveOrReferenceWithGivenId(id, input);
    }
    
    // See Scratch Object IO.ObjStream.readField and Scratch Object IO.ObjStream.<class>  
    private static ScratchObject readPrimitiveOrReferenceWithGivenId(int id, FileInputStream input) throws IOException
    {
        switch (id)
        {
        case 99: //Object Reference (3-byte one-based index into object table):
            return new ScratchObjectReference((int)readInt(input, 3));
        
        case 1: return null; //Undefined -- is null okay?
        case 2: // True 
        case 3: // False
            return new ScratchPrimitive(new Boolean(id == 2));
        case 4: //4-byte integer
            return new ScratchPrimitive(new BigDecimal(readInt(input, 4)));
        case 5: //2-byte integer 
            return new ScratchPrimitive(new BigDecimal(readInt(input, 2)));
        case 8: { /* Float */
            long bits = readInt(input, 8);
            return new ScratchPrimitive(new BigDecimal(Double.longBitsToDouble(bits)));
        } case 9:  // String
        case 10: { // Symbol
            int size = (int)readInt(input, 4);
            return new ScratchPrimitive(readFixedASCII(input, size));
        } case 11: { // ByteArray
            int size = (int)readInt(input, 4);
            byte[] b = new byte[size];
            input.read(b);
            return new ScratchPrimitive(b);
        } case 12: { // SoundBuf -- TODO read this properly as int16s
            int size = (int)readInt(input, 4);
            byte[] b = new byte[size * 2];
            input.read(b);
            return new ScratchPrimitive(b);
        } case 13: { //Bitmap, oddly this is effectively int[] and nothing more
            int size = (int)readInt(input, 4);
            int[] arr = new int[size];
            for (int i = 0; i < size; i++) {
                arr[i] = (int)readInt(input, 4);
            }
            return new ScratchPrimitive(arr);
        } case 14: { // UTF8
            int size = (int)readInt(input, 4);
            return new ScratchPrimitive(readUTF8(input, size));
        } case 20: // Array
          case 21: { // OrderedCollection
            int size = (int)readInt(input, 4);
            
            ScratchObject[] scratchObjects = readFields(input, size);
            
            return new ScratchObjectArray(scratchObjects);
        } case 24: { // Dictionary
            int size = (int)readInt(input, 4);
            ScratchObject[] keyValues = readFields(input, size*2);
            HashMap<ScratchObject, ScratchObject> map = new HashMap<ScratchObject, ScratchObject>();
            for (int i = 0; i < size; i++) {
                map.put(keyValues[i*2], keyValues[i*2+1]);
            }
            return new ScratchPrimitive(map);
        } case 30: // Color
          case 31: { // TranslucentColor
            int colour =(int)readInt(input, 4);
            int alpha = id == 31 ? (int)readInt(input, 1) : 255;
            //Smalltalk uses 10-bit colour channels, with the top 2 bits unused.
            //So we just take the high 8 bits out of each 10-bit channel:
            Color c = new Color((colour >> 22) & 255, (colour >> 12) & 255, (colour >> 2) & 255, alpha);
            return new ScratchPrimitive(c);
        } case 32: {  //Point
            ScratchObject[] fields = readFields(input, 2);
            BigDecimal x = (BigDecimal)fields[0].getValue();
            BigDecimal y = (BigDecimal)fields[1].getValue();
            return new ScratchPoint(x, y);
        } case 33: { // Rectangle
            ScratchObject[] fields = readFields(input, 4);
            BigDecimal x = (BigDecimal)fields[0].getValue();
            BigDecimal y = (BigDecimal)fields[1].getValue();
            BigDecimal x2 = (BigDecimal)fields[2].getValue();
            BigDecimal y2 = (BigDecimal)fields[3].getValue();
            return new ScratchRectangle(x, y, x2, y2);
        } case 34: // Form (an image)
          case 35: { // ColorForm (colour image)
              ScratchObject[] fields = readFields(input, id == 35 ? 6 : 5);
              int w = ((BigDecimal)fields[0].getValue()).intValue();
              int h = ((BigDecimal)fields[1].getValue()).intValue();
              int d = ((BigDecimal)fields[2].getValue()).intValue();
              int offset = fields[3] == null ? 0 : (Integer)fields[3].getValue();
              ScratchObject bits = fields[4];
              ScratchObject palette = id == 35 ? fields[5] : null;
              return new ScratchImage(w,h,d,offset,bits, palette);
          }
          default:
              Debug.message("*** UNKNOWN SCRATCH FIELD: " + id + " ***");
              return null;
        }
    }

    private static ScratchObject[] readFields(FileInputStream input,
            int size) throws IOException
    {
        List<ScratchObject> scratchObjects = new ArrayList<ScratchObject>();
        for (int i = 0; i < size; i++) {
            scratchObjects.add(readPrimitiveOrReference(input));
        }
        return scratchObjects.toArray(new ScratchObject[0]);
    }



    private static List<ScratchObject> readObjectStore(FileInputStream input) throws IOException
    {
        String header = readFixedASCII(input, 10);
        if (!"ObjS\001Stch\001".equals(header)) {
            Debug.message("Unknown Scratch object store header: " + header);
            return null;
        }
        
        int numObjects = (int)readInt(input, 4);
        
        ArrayList<ScratchObject> objects = new ArrayList<ScratchObject>(numObjects);
        for (int i = 0; i < numObjects; i++) {
            objects.add(readObject(input));
        }
        
        
        // For debug; Print before resolving, otherwise we have circular references and print forever:
        //int c = 1;
        //for (ScratchObject m : objects) {
        //    Debug.message(c + ": " + m);
        //    c++;
        //}
        
        // Resolve all objects:
        for (int i = 0; i < objects.size(); i++) {
            if (objects.get(i) != null) {
                objects.set(i, objects.get(i).resolve(objects));
            }
        }
        
        return objects;
    }

    private static void importScratch(File src, File dest)
    {
        try {
            FileInputStream input = new FileInputStream(src);
        
            readHeader(input);
            List<ScratchObject> objects = readObjectStore(input);
            
            Properties props = new Properties();
            props.setProperty("version", GreenfootMain.getAPIVersion().toString());
            for (ScratchObject o : objects) {
                o.saveInto(dest, props, null);
            }
            
            File javaFile = new File(dest, "Bubble.java");
            FileWriter javaFileWriter = new FileWriter(javaFile);
            javaFileWriter.write("import greenfoot.*;\nimport java.awt.Color;\npublic class Bubble extends Actor \n{\n");
            javaFileWriter.write("public Bubble(String s)\n{\nsetImage(new GreenfootImage(s, 15, Color.BLACK, Color.WHITE));\n}\n");
            javaFileWriter.write("public void act()\n{\n}\n");
            javaFileWriter.write("}\n");
            javaFileWriter.close();
            
            
            PackageFile packageFile = PackageFileFactory.getPackageFile(dest);
            packageFile.create();
            packageFile.save(props);
            
        } catch (IOException e) {
            Debug.reportError("Problem during Scratch import", e);
        }
    }

    public static File convert(File scratchFile)
    {
        String archiveName = scratchFile.getName();
        int dotIndex = archiveName.lastIndexOf('.');
        String strippedName = null;
        if(dotIndex != -1) {
            strippedName = archiveName.substring(0, dotIndex);
        } else {
            strippedName = archiveName;
        }
        File dest = new File(scratchFile.getParentFile(), strippedName);
        
        int i = 0;
        while (dest.exists()) {
            dest = new File(scratchFile.getParentFile(), strippedName + i);
            i++;
        }
        
        existingNames = new HashSet<String>();
        existingNames.add("World");
        existingNames.add("Actor");
        
        importScratch(scratchFile, dest);
        
        return dest;
    }
    
    private static Set<String> existingNames;
    
    // Munges a Scratch name into a valid Java class name, also
    // avoiding naming anything World or Actor, and taking care to avoid duplicates
    // Therefore you should only call this method once per name you want to convert.
    static String mungeUnique(String orig)
    {
        StringBuilder r = new StringBuilder();
        if (orig.length() > 0) {
            if (Character.isJavaIdentifierStart(orig.charAt(0))) {
                r.append(orig.charAt(0));
            } else {
                r.append("C");
            }
            for (char c : Arrays.copyOfRange(orig.toCharArray(),1, orig.toCharArray().length)) {
                if (Character.isJavaIdentifierPart(c)) {
                    r.append(c);
                }
            }
        }
        String initial = r.toString();
        String result;
        if (initial.length() == 0 || existingNames.contains(initial)) {
            int i = 0;
            while (existingNames.contains(initial + i)) {
                i += 1;
            }
            result = initial + i;
        } else {
            result = initial;
        }
        existingNames.add(result);
        return result;
    }
}
