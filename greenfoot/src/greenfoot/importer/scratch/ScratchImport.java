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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class ScratchImport
{
    /**
     * Used at the moment for debug output
     */
    private static void print(String s)
    {
        System.out.println(s);
    }
    
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
            print("Version 01");
        } else if ("ScratchV02".equals(ver)) {
            print("Version 02");
        } else {
            print("Unknown version:" + ver);
        }
    }
    
    /**
     * Reads a big-endian int using the given number of bytes
     * 
     * The Scratch format has all sorts of integer sizes, including 3 bytes.
     * 
     * TODO: there may be signed/unsigned issues with 4-byte integers
     */
    private static int readInt(FileInputStream input, int bytes) throws IOException
    {
        int x = 0;
        for (int i = 0; i < bytes; i++)
        {
            x <<= 8;
            x |= input.read();
        }
        return x;
    }

    /**
     * Reads the header from a Scratch file (the version, and the info block, which is skipped)
     */
    private static void readHeader(FileInputStream input) throws IOException
    {
        readVersion(input);
        int infoSize = readInt(input, 4);
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
        int classVersion = input.read();
        int fieldAmount = input.read();
        
        ScratchObject[] scratchObjects = readFields(input, fieldAmount);
        
        return new ScratchUserObject(id, classVersion, Arrays.asList(scratchObjects));
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
            return new ScratchObjectReference(readInt(input, 3));
        
        case 1: return null; //Undefined -- is null okay?
        case 2: // True 
        case 3: // False
            return new ScratchPrimitive(new Boolean(id == 2));
        case 4: //4-byte integer
            return new ScratchPrimitive(new Integer(readInt(input, 4)));
        case 5: //2-byte integer 
            return new ScratchPrimitive(new Integer(readInt(input, 2)));
        case 8: /* Float, TODO */
            readInt(input, 8);
            return new ScratchPrimitive(new Float(0));
        case 9:  // String
        case 10: { // Symbol
            int size = readInt(input, 4);
            return new ScratchPrimitive(readFixedASCII(input, size));
        } case 11: { // ByteArray
            int size = readInt(input, 4);
            byte[] b = new byte[size];
            input.read(b);
            return new ScratchPrimitive(b);
        } case 12: { // SoundBuf -- TODO read this properly as int16s
            int size = readInt(input, 4);
            byte[] b = new byte[size * 2];
            input.read(b);
            return new ScratchPrimitive(b);
        } case 14: { // UTF8
            int size = readInt(input, 4);
            return new ScratchPrimitive(readUTF8(input, size));
        } case 20: // Array
          case 21: { // OrderedCollection
            int size = readInt(input, 4);
            
            ScratchObject[] scratchObjects = readFields(input, size);
            
            return new ScratchObjectArray(scratchObjects);
        } case 24: { // Dictionary
            int size = readInt(input, 4);
            ScratchObject[] keyValues = readFields(input, size*2);
            HashMap<ScratchObject, ScratchObject> map = new HashMap<ScratchObject, ScratchObject>();
            for (int i = 0; i < size; i++) {
                map.put(keyValues[i*2], keyValues[i*2+1]);
            }
            return new ScratchPrimitive(map);
        } case 30: // Color
          case 31: { // TranslucentColor
            int colour = readInt(input, 4);
            int alpha = id == 31 ? readInt(input, 1) : 255;
            //Smalltalk uses 10-bit colour channels, with the top 2 bits unused.
            //So we just take the high 8 bits out of each 10-bit channel:
            Color c = new Color((colour >> 22) & 255, (colour >> 12) & 255, (colour >> 2) & 255, alpha);
            return new ScratchPrimitive(c);
        } case 32: {  //Point
            ScratchObject[] fields = readFields(input, 2);
            Number x = (Number)fields[0].getValue();
            Number y = (Number)fields[1].getValue();
            return new ScratchPrimitive(new Point(x.intValue(), y.intValue()));
        } case 33: { // Rectangle
            ScratchObject[] fields = readFields(input, 4);
            int x = (Integer)fields[0].getValue();
            int y = (Integer)fields[1].getValue();
            int width = (Integer)fields[2].getValue() - x;
            int height = (Integer)fields[3].getValue() - y;
            return new ScratchPrimitive(new Rectangle(x, y, width, height));
        } case 34: // Form (an image)
          case 35: { // ColorForm (colour image)
              ScratchObject[] fields = readFields(input, id == 35 ? 6 : 5);
              int w = (Integer)fields[0].getValue();
              int h = (Integer)fields[1].getValue();
              int d = (Integer)fields[2].getValue();
              int offset = fields[3] == null ? 0 : (Integer)fields[3].getValue();
              ScratchObject bits = fields[4];
              ScratchObject palette = id == 35 ? fields[5] : null;
              return new ScratchImage(w,h,d,offset,bits, palette);
          }
          default:
              print("*** UNKNOWN FIELD ***");
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
            print("Unknown object store header: " + header);
            return null;
        }
        
        int numObjects = readInt(input, 4);
        
        ArrayList<ScratchObject> objects = new ArrayList<ScratchObject>(numObjects);
        for (int i = 0; i < numObjects; i++) {
            objects.add(readObject(input));
        }
        
        
        // For debug; Print before resolving, otherwise we have circular references and print forever:
        int c = 1;
        for (ScratchObject m : objects) {
            print(c + ": " + m);
            c++;
        }
        
        // Resolve all objects:
        for (int i = 0; i < objects.size(); i++) {
            if (objects.get(i) != null) {
                objects.set(i, objects.get(i).resolve(objects));
            }
        }
        
        return objects;
    }
    
    public static ScratchUserObject findStage(List<ScratchObject> scratchObjects)
    {
        for (ScratchObject m : scratchObjects) {
            ScratchUserObject stage = m.getStage();
            if (stage != null)
                return stage;
        }
        return null;
    }
    
    public static List<ScratchUserObject> findSprites(List<ScratchObject> scratchObjects)
    {
        List<ScratchUserObject> sprites = new ArrayList<ScratchUserObject>();
        for (ScratchObject m : scratchObjects) {
            if (m != null) {
                ScratchUserObject sprite = m.getSprite();
                if (sprite != null)
                    sprites.add(sprite);
            }
        }
        return sprites;
    }
    
    
    public static void main(String[] args)
    {
        try {
            FileInputStream input = new FileInputStream("/home/neil/work/scratch/Projects/Simulations/1 Spinner.sb");
        
            readHeader(input);
            List<ScratchObject> objects = readObjectStore(input);
            
            for (ScratchObject m : objects) {
                if (m instanceof ScratchImage) {
                    BufferedImage bimg = ((ScratchImage)m).getBufferedImage();
                    if (bimg != null) {                       
                        ImageIcon icon = new ImageIcon();   
                        icon.setImage(bimg);   
                        JOptionPane.showMessageDialog(null, icon); 
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
