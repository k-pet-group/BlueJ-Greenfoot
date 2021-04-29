import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * This class can be used to read animated gif image files and extract the individual
 * images of the animation sequence.
 * 
 * @author Michael Berry
 * @author Neil Brown
 * 
 * Copyright (c) 2011,2013,2014,2018,2021
 */
public class GifImage
{
    /** The images used in the animation. */
    private GreenfootImage[] images;
    /** The delay between each frame. */
    private int[] delay;
    /** The index of the current frame in the GIF file. */
    private int currentIndex;
    /** The time passed since the last frame in ms. */
    private long time;
    /** Whether the animation is paused or not. */
    private boolean pause;

    /**
     * Set the image of the actor. If the image is a normal picture, it will be displayed as normal.
     * If it's an animated GIF file then it will be displayed as an animated actor.
     */
    public GifImage(String file)
    {
        pause = false;
        if(file.toLowerCase().endsWith(".gif")) {
            loadImages(file);
        }
        else {
            images = new GreenfootImage[] {new GreenfootImage(file)};
            delay = new int[] {1000}; // Doesn't matter, as long as it's not zero
            currentIndex = 0;
            time = System.currentTimeMillis();
        }
    }

    /**
     * Copy the given GifImage.  This is faster, and uses less memory, than loading the same
     * GIF multiple times.  The current play state (position in the GIF, paused state) is copied
     * from the given GifImage, but after that they can be independently played/paused.
     * 
     * The images making up the GIF are shared between the two images, so any modifications to
     * the images will be shared in both GIFs.  You can call this constructor on the same source
     * GIF multiple times.
     * @param copyFrom The GifImage to copy from.
     */
    public GifImage(GifImage copyFrom)
    {
        pause = copyFrom.pause;
        images = copyFrom.images.clone();
        delay = copyFrom.delay.clone();
        currentIndex = copyFrom.currentIndex;
        time = copyFrom.time;
    }

    /**
     * Get all the images used in the animation
     * @return a list of GreenfootImages, corresponding to each frame.
     */
    public List<GreenfootImage> getImages()
    {
        ArrayList<GreenfootImage> images = new ArrayList<GreenfootImage>(this.images.length);
        for(GreenfootImage image : this.images) {
            images.add(image);
        }
        return images;
    }

    /**
     * Pause the animation.
     */
    public void pause()
    {
        pause = true;
    }

    /**
     * Resume the animation.
     */
    public void resume()
    {
        pause = false;
        time = System.currentTimeMillis();
    }

    /**
     * Determines whether the animation is running
     * @return true if the animation is running, false otherwise
     */
    public boolean isRunning()
    {
        return !pause;
    }

    public GreenfootImage getCurrentImage()
    {
        long delta = System.currentTimeMillis() - time;

        while (delta >= delay[currentIndex] && !pause) {
            delta -= delay[currentIndex];
            time += delay[currentIndex];
            currentIndex = (currentIndex+1) % images.length;
        }
        return images[currentIndex];
    }

    /**
     * Load the images
     */
    private void loadImages(String file)
    {
        GifDecoder decode = new GifDecoder();
        decode.read(file);
        int numFrames = decode.getFrameCount();
        if(numFrames>0) {
            images = new GreenfootImage[numFrames];
            delay = new int[numFrames];
        }
        else {
            images = new GreenfootImage[1];
            images[0] = new GreenfootImage(1, 1);
        }

        for (int i=0 ; i<numFrames ; i++) {
            GreenfootImage image = new GreenfootImage(decode.getFrame(i).getWidth(), decode.getFrame(i).getHeight());
            image.drawImage(decode.getFrame(i), 0, 0);
            delay[i] = decode.getDelay(i);
            images[i] = image;
        }
        time = System.currentTimeMillis();
    }

    /**
     * The Rectangle class represents rectangles. This is essentially a re-implementation
     * of the java.awt.Rectangle class, created in order to avoid any dependency on AWT.
     */
    private static class Rectangle
    {
        public int x;
        public int y;
        public int width;
        public int height;
        
        public Rectangle(int x, int y, int width, int height)
        {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Class GifDecoder - Decodes a GIF file into one or more frames. <br><br>
     * 
     * <i>I (Michael) edited this slightly on 10/09/08 to bring class up to date with generics and therefore remove warnings.
     * Also edited so that resources are grabbed from the jar file and not externally, so no security exceptions.</i>
     * <br><br>
     * <pre>
     *  Example:
     *     GifDecoder d = new GifDecoder();
     *     d.read(&quot;sample.gif&quot;);
     *     int n = d.getFrameCount();
     *     for (int i = 0; i &lt; n; i++) {
     *        BufferedImage frame = d.getFrame(i);  // frame i
     *        int t = d.getDelay(i);  // display duration of frame in milliseconds
     *        // do something with frame
     *     }
     * </pre>
     * 
     * No copyright asserted on the source code of this class. May be used for any
     * purpose, however, refer to the Unisys LZW patent for any additional
     * restrictions. Please forward any corrections to kweiner@fmsware.com.
     * 
     * @author Kevin Weiner, FM Software; LZW decoder adapted from John Cristy's
     *         ImageMagick.
     * @version 1.03 November 2003
     * 
     */
    private class GifDecoder
    {
        /**
         * File read status: No errors.
         */
        public static final int STATUS_OK = 0;

        /**
         * File read status: Error decoding file (may be partially decoded)
         */
        public static final int STATUS_FORMAT_ERROR = 1;

        /**
         * File read status: Unable to open source.
         */
        public static final int STATUS_OPEN_ERROR = 2;

        private BufferedInputStream in;

        private int status;

        private int width; // full image width

        private int height; // full image height

        private boolean gctFlag; // global color table used

        private int gctSize; // size of global color table

        private int loopCount = 1; // iterations; 0 = repeat forever

        private int[] gct; // global color table

        private int[] lct; // local color table

        private int[] act; // active color table

        private int bgIndex; // background color index

        private Color bgColor; // background color

        private Color lastBgColor; // previous bg color

        private int pixelAspect; // pixel aspect ratio

        private boolean lctFlag; // local color table flag

        private boolean interlace; // interlace flag

        private int lctSize; // local color table size

        private int ix, iy, iw, ih; // current image rectangle

        private Rectangle lastRect; // last image rect

        private GreenfootImage image; // current frame

        private GreenfootImage lastImage; // previous frame

        private byte[] block = new byte[256]; // current data block

        private int blockSize = 0; // block size

        // last graphic control extension info
        private int dispose = 0;

        // 0=no action; 1=leave in place; 2=restore to bg; 3=restore to prev
        private int lastDispose = 0;

        private boolean transparency = false; // use transparent color

        private int delay = 0; // delay in milliseconds

        private int transIndex; // transparent color index

        private static final int MaxStackSize = 4096;

        // max decoder pixel stack size

        // LZW decoder working arrays
        private short[] prefix;

        private byte[] suffix;

        private byte[] pixelStack;

        private byte[] pixels;

        private ArrayList<GifFrame> frames; // frames read from current file

        private int frameCount;

        /**
         * A single frame
         */
        private class GifFrame {
            public GifFrame(GreenfootImage im, int del) {
                image = im;
                delay = del;
            }

            private GreenfootImage image;

            private int delay;
        }

        /**
         * Convert an RGB integer value to a Color.
         */
        private Color colorFromInt(int rgb)
        {
            int r = (rgb & 0xFF0000) >> 16;
            int g = (rgb & 0xFF00) >> 8;
            int b = (rgb & 0xFF);
            return new Color(r,g,b);
        }
        
        /**
         * Gets display duration for specified frame.
         * 
         * @param n
         *          int index of frame
         * @return delay in milliseconds
         */
        public int getDelay(int n) {
            //
            delay = -1;
            if ((n >= 0) && (n < frameCount)) {
                delay = (frames.get(n)).delay;
            }
            return delay;
        }

        /**
         * Gets the number of frames read from file.
         * 
         * @return frame count
         */
        public int getFrameCount() {
            return frameCount;
        }

        /**
         * Gets the first (or only) image read.
         * 
         * @return BufferedImage containing first frame, or null if none.
         */
        public GreenfootImage getImage() {
            return getFrame(0);
        }

        /**
         * Gets the "Netscape" iteration count, if any. A count of 0 means repeat
         * indefinitiely.
         * 
         * @return iteration count if one was specified, else 1.
         */
        public int getLoopCount() {
            return loopCount;
        }

        /**
         * Creates new frame image from current data (and previous frames as specified
         * by their disposition codes).
         */
        protected void setPixels() {
            // fill in starting image contents based on last image's dispose code
            if (lastDispose > 0) {
                if (lastDispose == 3) {
                    // use image before last
                    int n = frameCount - 2;
                    if (n > 0) {
                        lastImage = getFrame(n - 1);
                    } else {
                        lastImage = null;
                    }
                }

                if (lastImage != null) {
                    image.clear();
                    image.drawImage(lastImage, 0, 0);
                    
                    // copy pixels

                    if (lastDispose == 2) {
                        // fill last image rect area with background color
                        Color c = null;
                        if (transparency) {
                            c = new Color(0, 0, 0, 0); // assume background is transparent
                        } else {
                            c = lastBgColor; // use given background color
                        }
                        for (int x = 0; x < lastRect.width; x++)
                        {
                            for (int y = 0; y < lastRect.height; y++)
                            {
                                image.setColorAt(lastRect.x + x, lastRect.y + y, c);
                            }
                        }
                    }
                }
            }

            // copy each source line to the appropriate place in the destination
            int pass = 1;
            int inc = 8;
            int iline = 0;
            for (int i = 0; i < ih; i++) {
                int line = i;
                if (interlace) {
                    if (iline >= ih) {
                        pass++;
                        switch (pass) {
                        case 2:
                            iline = 4;
                            break;
                        case 3:
                            iline = 2;
                            inc = 4;
                            break;
                        case 4:
                            iline = 1;
                            inc = 2;
                        }
                    }
                    line = iline;
                    iline += inc;
                }
                line += iy;
                if (line < height) {
                    int k = line * width;
                    int dlim = Math.min(ix + iw, width);
                    int sx = i * iw;
                    
                    for (int dx = ix; dx < dlim; dx++) {
                        int index = ((int) pixels[sx++]) & 0xff;
                        int c = act[index];
                        if (c != 0) {
                            image.setColorAt(dx, line, colorFromInt(c));
                        }
                    }
                }
            }
        }

        /**
         * Gets the image contents of frame n.
         * 
         * @return BufferedImage representation of frame, or null if n is invalid.
         */
        public GreenfootImage getFrame(int n) {
            GreenfootImage im = null;
            if ((n >= 0) && (n < frameCount)) {
                im = ((GifFrame) frames.get(n)).image;
            }
            return im;
        }

        /**
         * Gets image size.
         * 
         * @return GIF image dimensions as an array - [0] = width, [1] = height
         */
        public int[] getFrameSize() {
            return new int[]{width, height};
        }

        /**
         * Reads GIF image from stream
         * 
         * @param BufferedInputStream
         *          containing GIF file.
         * @return read status code (0 = no errors)
         */
        public int read(BufferedInputStream is) {
            init();
            if (is != null) {
                in = is;
                readHeader();
                if (!err()) {
                    readContents();
                    if (frameCount < 0) {
                        status = STATUS_FORMAT_ERROR;
                    }
                }
            } else {
                status = STATUS_OPEN_ERROR;
            }
            try {
                is.close();
            } catch (IOException e) {
            }
            return status;
        }

        /**
         * Reads GIF image from stream
         * 
         * @param InputStream
         *          containing GIF file.
         * @return read status code (0 = no errors)
         */
        public int read(InputStream is) {
            init();
            if (is != null) {
                if (!(is instanceof BufferedInputStream))
                    is = new BufferedInputStream(is);
                in = (BufferedInputStream) is;
                readHeader();
                if (!err()) {
                    readContents();
                    if (frameCount < 0) {
                        status = STATUS_FORMAT_ERROR;
                    }
                }
            } else {
                status = STATUS_OPEN_ERROR;
            }
            try {
                is.close();
            } catch (IOException e) {
            }
            return status;
        }

        /**
         * Reads GIF file from specified file/URL source (URL assumed if name contains
         * ":/" or "file:")
         * 
         * @param name
         *          String containing source
         * @return read status code (0 = no errors)
         */
        public int read(String name) {
            status = STATUS_OK;
            InputStream resource = this.getClass().getResourceAsStream(name);
            if (resource == null) {
                name = "images/" + name;
                resource = this.getClass().getResourceAsStream(name);
                if (resource == null) {
                    throw new RuntimeException("The gif file \"" + name + "\" doesn't exist.");
                }
            }
            in = new BufferedInputStream(resource);
            status = read(in);

            return status;
        }

        /**
         * Decodes LZW image data into pixel array. Adapted from John Cristy's
         * ImageMagick.
         */
        protected void decodeImageData() {
            int NullCode = -1;
            int npix = iw * ih;
            int available, clear, code_mask, code_size, end_of_information, in_code, old_code, bits, code, count, i, datum, data_size, first, top, bi, pi;

            if ((pixels == null) || (pixels.length < npix)) {
                pixels = new byte[npix]; // allocate new pixel array
            }
            if (prefix == null)
                prefix = new short[MaxStackSize];
            if (suffix == null)
                suffix = new byte[MaxStackSize];
            if (pixelStack == null)
                pixelStack = new byte[MaxStackSize + 1];

            // Initialize GIF data stream decoder.

            data_size = read();
            clear = 1 << data_size;
            end_of_information = clear + 1;
            available = clear + 2;
            old_code = NullCode;
            code_size = data_size + 1;
            code_mask = (1 << code_size) - 1;
            for (code = 0; code < clear; code++) {
                prefix[code] = 0;
                suffix[code] = (byte) code;
            }

            // Decode GIF pixel stream.

            datum = bits = count = first = top = pi = bi = 0;

            for (i = 0; i < npix;) {
                if (top == 0) {
                    if (bits < code_size) {
                        // Load bytes until there are enough bits for a code.
                        if (count == 0) {
                            // Read a new data block.
                            count = readBlock();
                            if (count <= 0)
                                break;
                            bi = 0;
                        }
                        datum += (((int) block[bi]) & 0xff) << bits;
                        bits += 8;
                        bi++;
                        count--;
                        continue;
                    }

                    // Get the next code.

                    code = datum & code_mask;
                    datum >>= code_size;
                        bits -= code_size;

                        // Interpret the code

                        if ((code > available) || (code == end_of_information))
                            break;
                        if (code == clear) {
                            // Reset decoder.
                            code_size = data_size + 1;
                            code_mask = (1 << code_size) - 1;
                            available = clear + 2;
                            old_code = NullCode;
                            continue;
                        }
                        if (old_code == NullCode) {
                            pixelStack[top++] = suffix[code];
                            old_code = code;
                            first = code;
                            continue;
                        }
                        in_code = code;
                        if (code == available) {
                            pixelStack[top++] = (byte) first;
                            code = old_code;
                        }
                        while (code > clear) {
                            pixelStack[top++] = suffix[code];
                            code = prefix[code];
                        }
                        first = ((int) suffix[code]) & 0xff;

                        // Add a new string to the string table,

                        if (available >= MaxStackSize)
                            break;
                        pixelStack[top++] = (byte) first;
                        prefix[available] = (short) old_code;
                        suffix[available] = (byte) first;
                        available++;
                        if (((available & code_mask) == 0) && (available < MaxStackSize)) {
                            code_size++;
                            code_mask += available;
                        }
                        old_code = in_code;
                }

                // Pop a pixel off the pixel stack.

                top--;
                pixels[pi++] = pixelStack[top];
                i++;
            }

            for (i = pi; i < npix; i++) {
                pixels[i] = 0; // clear missing pixels
            }

        }

        /**
         * Returns true if an error was encountered during reading/decoding
         */
        protected boolean err() {
            return status != STATUS_OK;
        }

        /**
         * Initializes or re-initializes reader
         */
        protected void init() {
            status = STATUS_OK;
            frameCount = 0;
            frames = new ArrayList<GifFrame>();
            gct = null;
            lct = null;
        }

        /**
         * Reads a single byte from the input stream.
         */
        protected int read() {
            int curByte = 0;
            try {
                curByte = in.read();
            } catch (IOException e) {
                status = STATUS_FORMAT_ERROR;
            }
            return curByte;
        }

        /**
         * Reads next variable length block from input.
         * 
         * @return number of bytes stored in "buffer"
         */
        protected int readBlock() {
            blockSize = read();
            int n = 0;
            if (blockSize > 0) {
                try {
                    int count = 0;
                    while (n < blockSize) {
                        count = in.read(block, n, blockSize - n);
                        if (count == -1)
                            break;
                        n += count;
                    }
                } catch (IOException e) {
                }

                if (n < blockSize) {
                    status = STATUS_FORMAT_ERROR;
                }
            }
            return n;
        }

        /**
         * Reads color table as 256 RGB integer values
         * 
         * @param ncolors
         *          int number of colors to read
         * @return int array containing 256 colors (packed ARGB with full alpha)
         */
        protected int[] readColorTable(int ncolors) {
            int nbytes = 3 * ncolors;
            int[] tab = null;
            byte[] c = new byte[nbytes];
            int n = 0;
            try {
                n = in.read(c);
            } catch (IOException e) {
            }
            if (n < nbytes) {
                status = STATUS_FORMAT_ERROR;
            } else {
                tab = new int[256]; // max size to avoid bounds checks
                int i = 0;
                int j = 0;
                while (i < ncolors) {
                    int r = ((int) c[j++]) & 0xff;
                    int g = ((int) c[j++]) & 0xff;
                    int b = ((int) c[j++]) & 0xff;
                    tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
                }
            }
            return tab;
        }

        /**
         * Main file parser. Reads GIF content blocks.
         */
        protected void readContents() {
            // read GIF file content blocks
            boolean done = false;
            while (!(done || err())) {
                int code = read();
                switch (code) {

                case 0x2C: // image separator
                    readImage();
                    break;

                case 0x21: // extension
                    code = read();
                    switch (code) {
                    case 0xf9: // graphics control extension
                        readGraphicControlExt();
                        break;

                    case 0xff: // application extension
                        readBlock();
                        String app = "";
                        for (int i = 0; i < 11; i++) {
                            app += (char) block[i];
                        }
                        if (app.equals("NETSCAPE2.0")) {
                            readNetscapeExt();
                        } else
                            skip(); // don't care
                        break;

                    default: // uninteresting extension
                        skip();
                    }
                    break;

                case 0x3b: // terminator
                    done = true;
                    break;

                case 0x00: // bad byte, but keep going and see what happens
                    break;

                default:
                    status = STATUS_FORMAT_ERROR;
                }
            }
        }

        /**
         * Reads Graphics Control Extension values
         */
        protected void readGraphicControlExt() {
            read(); // block size
            int packed = read(); // packed fields
            dispose = (packed & 0x1c) >> 2; // disposal method
            if (dispose == 0) {
                dispose = 1; // elect to keep old image if discretionary
            }
            transparency = (packed & 1) != 0;
            delay = readShort() * 10; // delay in milliseconds
            transIndex = read(); // transparent color index
            read(); // block terminator
        }

        /**
         * Reads GIF file header information.
         */
        protected void readHeader() {
            String id = "";
            for (int i = 0; i < 6; i++) {
                id += (char) read();
            }
            if (!id.startsWith("GIF")) {
                status = STATUS_FORMAT_ERROR;
                return;
            }

            readLSD();
            if (gctFlag && !err()) {
                gct = readColorTable(gctSize);
                bgColor = colorFromInt(gct[bgIndex]);
            }
        }

        /**
         * Reads next frame image
         */
        protected void readImage() {
            ix = readShort(); // (sub)image position & size
            iy = readShort();
            iw = readShort();
            ih = readShort();

            int packed = read();
            lctFlag = (packed & 0x80) != 0; // 1 - local color table flag
            interlace = (packed & 0x40) != 0; // 2 - interlace flag
            // 3 - sort flag
            // 4-5 - reserved
            lctSize = 2 << (packed & 7); // 6-8 - local color table size

            if (lctFlag) {
                lct = readColorTable(lctSize); // read table
                act = lct; // make local table active
            } else {
                act = gct; // make global table active
                if (bgIndex == transIndex)
                    bgColor = colorFromInt(0);
            }
            int save = 0;
            if (transparency) {
                save = act[transIndex];
                act[transIndex] = 0; // set transparent color if specified
            }

            if (act == null) {
                status = STATUS_FORMAT_ERROR; // no color table defined
            }

            if (err())
                return;

            decodeImageData(); // decode pixel data
            skip();

            if (err())
                return;

            frameCount++;

            // create new image to receive frame data
            image = new GreenfootImage(width, height);

            setPixels(); // transfer pixel data to image

            frames.add(new GifFrame(image, delay)); // add image to frame list

            if (transparency) {
                act[transIndex] = save;
            }
            resetFrame();

        }

        /**
         * Reads Logical Screen Descriptor
         */
        protected void readLSD() {

            // logical screen size
            width = readShort();
            height = readShort();

            // packed fields
            int packed = read();
            gctFlag = (packed & 0x80) != 0; // 1 : global color table flag
            // 2-4 : color resolution
            // 5 : gct sort flag
            gctSize = 2 << (packed & 7); // 6-8 : gct size

            bgIndex = read(); // background color index
            pixelAspect = read(); // pixel aspect ratio
        }

        /**
         * Reads Netscape extenstion to obtain iteration count
         */
        protected void readNetscapeExt() {
            do {
                readBlock();
                if (block[0] == 1) {
                    // loop count sub-block
                    int b1 = ((int) block[1]) & 0xff;
                    int b2 = ((int) block[2]) & 0xff;
                    loopCount = (b2 << 8) | b1;
                }
            } while ((blockSize > 0) && !err());
        }

        /**
         * Reads next 16-bit value, LSB first
         */
        protected int readShort() {
            // read 16-bit value, LSB first
            return read() | (read() << 8);
        }

        /**
         * Resets frame state for reading next image.
         */
        protected void resetFrame() {
            lastDispose = dispose;
            lastRect = new Rectangle(ix, iy, iw, ih);
            lastImage = image;
            lastBgColor = bgColor;
            int dispose = 0;
            boolean transparency = false;
            int delay = 0;
            lct = null;
        }

        /**
         * Skips variable length blocks up to and including next zero length block.
         */
        protected void skip() {
            do {
                readBlock();
            } while ((blockSize > 0) && !err());
        }
    }

}
