package greenfoot.export.mygame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.httpclient.methods.multipart.FilePart;

/**
 * A FilePart which tracks upload progress.
 * 
 * @author Davin McCall
 */
public class ProgressTrackingPart extends FilePart
{
    private MyGameClient listener;
    
    public ProgressTrackingPart(String partName, File file, MyGameClient listener)
        throws FileNotFoundException
    {
        super(partName, file);
        this.listener = listener;
    }
    
    @Override
    protected void sendData(OutputStream output) throws IOException
    {
        if (lengthOfData() == 0) {
            return;
        }
        
        byte [] buf = new byte[4096];
        InputStream istream = getSource().createInputStream();
        try {
            int len = istream.read(buf);
            while (len != -1) {
                output.write(buf, 0, len);
                listener.progress(len);
                len = istream.read(buf);
            }
        }
        finally {
            istream.close();
        }
    }
}
