package bluej.debugger;

import java.io.*;
import java.util.*;

class InspectorClassLoader extends ClassLoader
{

    File dir;

    public InspectorClassLoader(File dir)
    {
        this.dir = dir;
    }

    private InspectorClassLoader()
    {
    }

    public Class findClass(String name)
    {
        byte[] b = loadClassData(name);
        return defineClass(name, b, 0, b.length);
    }

    private byte[] loadClassData(String name)
    {
        // load the class data from the connection
        try
        {
            InputStream is = new FileInputStream(new File(dir, name + ".class"));
            List bytes = new ArrayList();
            byte[] bary = new byte[1024];
            int bread = is.read(bary);
            int btotal = 0;
            int blast = 0;
            while (bread > 0)
            {
                btotal += bread;
                blast = bread;
                bytes.add(bary);
                bary = new byte[1024];
                bread = is.read(bary);
            }
            Iterator e = bytes.iterator();
            bary = ((byte[]) e.next());
            int target = 0;
            byte[] output = new byte[btotal];
            while (e.hasNext())
            {
                System.arraycopy(bary, 0, output, target, bary.length);
                target += bary.length;
                bary = ((byte[]) e.next());
            }
            System.arraycopy(bary, 0, output, target, blast);
            return output;
        }
        catch (IOException e)
        {
            System.out.println("IOException in class loader");
        }
        return new byte[0];
    }
}
