package greenfoot.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import bluej.utility.Debug;

public class Mp3AudioInputStream implements GreenfootAudioInputStream
{
	private static void printDebug(String s)
	{
		// Comment this line out if you don't want debug info.
		//System.out.println(s);
	}

	/** The MPEG audio bitstream. */
	private Bitstream bitstream;
	/** The MPEG audio decoder. */
	private Decoder decoder;
	/** URL of the resource */
	private URL url;
	private boolean readingHasStarted = false;
	private BufferedInputStream inputStream;
	private AudioFormat format;
	private SampleBuffer unreadSample;

	public Mp3AudioInputStream(URL url) throws IOException,
			UnsupportedAudioFileException
	{
		this.url = url;
		restart();

		// TODO: is this the correct way to get the format?
		Header header = null;
		try {
			header = bitstream.readFrame();

			bitstream.unreadFrame();

		} catch (BitstreamException e) {
			throw new IOException(e.toString());
		}
		if (header == null) {
			bitstream.closeFrame();
			format = new AudioFormat(decoder.getOutputFrequency(), 16, decoder
					.getOutputChannels(), true, false);
		} else {
			int mode = header.mode();
			int channels = mode == Header.SINGLE_CHANNEL ? 1 : 2;
			format = new AudioFormat(header.frequency(), 16, channels, true,
					false);
		}
		printDebug(" Created mp3 stream with audioFormat: " + format);
	}

	public String getSource()
	{
		return url.toString();
	}

	public void restart() throws IOException, UnsupportedAudioFileException
	{
		if (!readingHasStarted() && bitstream != null) {
			return;
		}
		
		readingHasStarted = false;
		unreadSample = null;

		if (bitstream != null) {
			try {
				bitstream.close();
			} catch (BitstreamException e) {
				// An exception here is probably not fatal, so we just log it
				// and continue.) {
				Debug.reportError(
						"Exception while closing mp3 audio input stream.", e);
			}
		}
		inputStream = new BufferedInputStream(url.openStream());
		bitstream = new Bitstream(inputStream);

		decoder = new Decoder();
	}

	/**
	 * Whether reading from this stream has begun.
	 * 
	 * @return True if it has been restarted and no reading has been done since.
	 *         False otherwise.
	 */
	private boolean readingHasStarted()
	{
		return readingHasStarted;
	}

	public int available() throws IOException
	{
		return inputStream.available();
	}

	public void close() throws IOException
	{
		try {
			bitstream.close();
		} catch (BitstreamException e) {
			throw new IOException(e.toString());
		}
	}

	public AudioFormat getFormat()
	{
		return format;

	}

	public void mark(int readlimit)
	{
		// not supported
	}

	public boolean markSupported()
	{
		return false;
	}

	public int read() throws IOException
	{
		byte [] b = new byte[1];
		
		int bytesRead = read(b, 0, 1);
		if(bytesRead < 0) {
			return -1;
		}
		else if(bytesRead == 0) {
			throw new IOException("cannot read a single byte if frame size > 1");
		} else {
			return b[0] & 0xFF;
		}
	}

	public int read(byte[] b) throws IOException
	{
		return read(b, 0, b.length);
	}

	public int read(final byte[] b, final int off, final int len) throws IOException
	{
		
		if(off < 0) {
			throw new IllegalArgumentException("The offset must be positive. It was: " + off);
		}
		if(len < 0) {
			throw new IllegalArgumentException("The length must be positive. It was: " + len);
		}
		if(off+len>b.length) {
			throw new IllegalArgumentException("Lenght + offset must not be bigger than the array length.");
		}
		readingHasStarted = true;

		printDebug("read() called with params: off:" + off + "  len:" + len);
		
		Header header = null;

		
		
		int read = 0;
		if(unreadSample != null) {
			
			toByteArray(unreadSample.getBuffer(), unreadSample.getBufferLength(), b, off);
			printDebug("UNREAD SAMPLE just read.");
			read += unreadSample.getBufferLength() * 2;
			unreadSample = null;
			bitstream.closeFrame();
		}
		try {
			header = bitstream.readFrame();
		} catch (BitstreamException e) {
			throw new IOException(e.toString());
		}
		while (header != null) {

			// decode the frame
			SampleBuffer sample = null;
			try {
				sample = (SampleBuffer) decoder.decodeFrame(header, bitstream);
			} catch (DecoderException e) {
				throw new IOException(e.toString());
			}

			printDebug("Read: " + read);
			int sampleLength = sample.getBufferLength();
			int sampleLengthInBytes = sampleLength * 2;
			printDebug("Buffer length: " + sampleLength);
			if(read + (sampleLengthInBytes) > len) {
				unreadSample = sample;
				printDebug(" saving unreadSample for later.");
				break;
			}
			toByteArray(sample.getBuffer(), sampleLength, b, off + read);
			printDebug("Just read bytes: " + sampleLengthInBytes);
			read += sampleLengthInBytes;
			bitstream.closeFrame();
			try {
				header = bitstream.readFrame();
			} catch (BitstreamException e) {
				throw new IOException(e.toString());
			}
		}
		
		return read;
	}
	
	final private void toByteArray(short[] samples, int len, byte[] b,  int off)
	{
		int shortIndex = 0;
		short s;
		while (len-- > 0) {
			s = samples[shortIndex++];
			b[off++] = (byte) s;
			b[off++] = (byte) (s >>> 8);
		}
	}

	public void reset() throws IOException
	{
		throw new UnsupportedOperationException();
	}

	public long skip(long n) throws IOException
	{
		throw new UnsupportedOperationException();
	}

}
