package antonioplesic.simpleabxtester.wavextractor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import antonioplesic.simpleabxtester.wavextractor.WavParser.OnWavToRawExtractionProgressUpdateListener;
import antonioplesic.simpleabxtester.wavextractor.WavParser.StopExectutionCallback;


/**
 * For parsing Wav files. Only formats expected to be supported in the App are
 * parsed in its entirety: this means only
 */
public class WavParserBetter {
	
	boolean formatFound = false;
	boolean dataFound = false;
	long dataStartChunkIdAt = -1;
	long dataLength;
	long dataStart;
	int paddingByte;
	int format = -1;
	int nChannels = -1;
	int sampleRate = -1;
	int bitsPerSample = -1;

	static final byte[] RIFF = "RIFF".getBytes(Charset.forName("US-ASCII"));
	static final byte[] WAVE = "WAVE".getBytes(Charset.forName("US-ASCII"));
	static final byte[] FMT  = "fmt ".getBytes(Charset.forName("US-ASCII"));
	static final byte[] DATA = "data".getBytes(Charset.forName("US-ASCII"));
	static final byte[]	FACT = "fact".getBytes(Charset.forName("US-ASCII"));
	
	FileInputStream in;
	byte[] buffer = new byte[4];
	
	
	public WavParserBetter(String path) throws FileNotFoundException{
		in = new FileInputStream(path);
	}
	
	public WavParserBetter(File file) throws FileNotFoundException{
		in = new FileInputStream(file);
	}

	public WavInfoBetter parse() throws IOException{
		
		formatFound = false;
		dataFound = false;
		dataStartChunkIdAt = -1;
		dataLength = -1;
		dataStart = -1;
		
		while(in.read(buffer) != -1 && !(formatFound && dataFound)){
			
			
			if(Arrays.equals(buffer, RIFF)){
				readRiffChunk();
			}
			else if(Arrays.equals(buffer, FMT)){
				readFormatChunk();
			}
			else if(Arrays.equals(buffer, DATA)){	
				readDataChunk();
			}
			else if(Arrays.equals(buffer, FACT)){
				//TODO: Fact chunk, when and if support for formats that need it is added
				readUnknownChunk();
			}
			//if unknown chunk, skip it
			else{
				readUnknownChunk();
			}
			
		}
		
		System.out.println("Done parsing");
		
		if(formatFound && dataFound){
			System.out.println("Format and data found");
			return new WavInfoBetter(dataStart, dataLength, format, nChannels, sampleRate, bitsPerSample);
		}
		else{
			System.out.println("Data or format not found");
			return new WavInfoBetter(dataStart, dataLength, format, nChannels, sampleRate, bitsPerSample);
		}
		
		
	}
	
	private void readRiffChunk() throws IOException{

		System.out.println("|RIFF");
		
		//master cksize
		in.read(buffer);
		long cksize = littleEndianToInt(buffer);
		System.out.println("|cksize: " + cksize);
		
		//master WAVEid, should be "WAVE"
		in.read(buffer);
		if( ! Arrays.equals(buffer, WAVE)) {
			System.out.println("WAVE not ok");
			return;
		}
		System.out.println("|  |WAVE");		
		System.out.println("|  -----------------------------------------------");
	}
	
	
	private void readUnknownChunk() throws IOException {
		
		System.out.println("|  |Unknown chunk: " + new String(buffer, Charset.forName("US-ASCII")));
		
		//cksize
		in.read(buffer);
		long cksize = littleEndianToInt(buffer);
		System.out.println("|  |cksize: " + cksize + "  (0x" + Long.toHexString(cksize) + ")");
		paddingByte = (cksize%2==0)?0:1;
		in.skip(cksize + paddingByte);
		System.out.println("|  -----------------------------------------------");
		
		
	}

	private void readDataChunk() throws IOException{
		
		dataStartChunkIdAt = in.getChannel().position() - 4;
		System.out.println("|  |data, chunkId ID at offset 0x" + Long.toHexString(dataStartChunkIdAt));
		
		//cksize and derived properties
		in.read(buffer);
		long cksize = littleEndianToInt(buffer);
		
		dataStart = in.getChannel().position();
		dataLength = cksize;
		
		System.out.println("|  |cksize: " + cksize + "  (0x" + Long.toHexString(cksize) + ")");
		System.out.println("|  |  |data, starting at 0x" + Long.toHexString(dataStart) +", with length of " + dataLength +" (0x" + Long.toHexString(dataLength)   + ") bytes");
		
		//padding byte
		paddingByte = (cksize%2==0)?0:1;
		System.out.println("|  |  |Padding byte: " + paddingByte);
		
		//skip rest of data chunk + padding if exists
		in.skip(cksize + paddingByte);
		
		dataFound = true;
		System.out.println("|  -----------------------------------------------");
		
	}
	
	private void readFormatChunk() throws IOException{
		System.out.println("|  |fmt ");
		
		//cksize
		in.read(buffer);
		long cksize = littleEndianToInt(buffer);
		System.out.println("|  |cksize: " + cksize);
		
		//wFormatTag & nChannels, read together
		in.read(buffer);
		
		//wFormatTag
		int wFormatTag = firstTwoLittleEndianToInt(buffer);
		this.format = wFormatTag;
		switch (wFormatTag) {
		case WavInfoBetter.WAVE_FORMAT_PCM:
			System.out.println("|  |  |wFormatTag: WAVE_FORMAT_PCM");
			break;
		default:
			System.out.println("|  |  |wFormatTag: OTHER_FORMAT");
			break;
		}
		
		//nChannels
		int nChannels = lastTwoLittleEndianToInt(buffer);
		this.nChannels = nChannels;
		System.out.println("|  |  |nChannels: " + nChannels);
		
		//nSamplesPerSec
		in.read(buffer);
		long nSamplesPerSecond = littleEndianToInt(buffer);
		this.sampleRate = (int) nSamplesPerSecond; //it's ok, samplesRate surely fits into int
		System.out.println("|  |  |nSamplesPerSecond: " + nSamplesPerSecond);
		
		//nAvgBytesPerSec
		in.read(buffer);
		long nAvgBytesPerSec =  littleEndianToInt(buffer);
		System.out.println("|  |  |nAvgBytesPerSec: " + nAvgBytesPerSec);
		
		//nBlockAlign & wBitsPerSample, read together
		in.read(buffer);
		
		//nBlockAlign
		int nBlockAlign = firstTwoLittleEndianToInt(buffer);
		System.out.println("|  |  |nBlockAlign: " + nBlockAlign);
		
		//wBitsPerSample
		int wBitsPerSample = lastTwoLittleEndianToInt(buffer);
		this.bitsPerSample = wBitsPerSample;
		System.out.println("|  |  |wBitsPerSample: " + wBitsPerSample);
		
		
		//if cksize is 18 or 40, read cbSize, else return
		
		//if cbSize is 22, read extension, else return
		
		formatFound = true;
		
		System.out.println("|  ---------------------------------------------");
	}
	
	private static long littleEndianToInt(byte[] bytes){
		
		return (bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8 | (bytes[2] & 0xFF) << 16 | (bytes[3] & 0xFF) << 24;
	}
	
	private static int firstTwoLittleEndianToInt(byte[] bytes){	
		
		return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
	}
	
	private static int lastTwoLittleEndianToInt(byte[] bytes){
		
		return (bytes[2] & 0xFF) |  ((bytes[3] & 0xFF) << 8);
	}
	
public static void extractRawDataFromWav(String inputFilePath, String outputFilePath, OnWavToRawExtractionProgressUpdateListener listener, StopExectutionCallback stopCallback) throws IOException{
		
		WavInfoBetter wavInfo =  new WavParserBetter(new File(inputFilePath)).parse();
		copyFilePart(inputFilePath, outputFilePath, wavInfo.dataStart,
				wavInfo.dataLength,listener,stopCallback);
		
	}

	public static void copyFilePart(String inputFilePath,
			String outputFilePath, long dataStart, long dataLength,
			OnWavToRawExtractionProgressUpdateListener listener,
			StopExectutionCallback stopCallback)
			throws IOException {

		File outputFile = new File(outputFilePath);
		File inputFile = new File(inputFilePath);
		FileInputStream in = null;
		FileOutputStream out = null;

		byte[] buffer = new byte[10000];

		try {
			out = new FileOutputStream(outputFile);
			in = new FileInputStream(inputFile);

			long bytesSkipped = in.skip(dataStart);
			if (bytesSkipped != dataStart) {
				throw new IOException("unable to seek to data");
			}

			int bytesReadSoFar = 0;
			int bytesRead;
			int iterationCounter = 0;
			
			while ((bytesRead = in.read(buffer)) != -1
					&& bytesReadSoFar <= dataLength) {
				
				bytesReadSoFar += bytesRead;
				out.write(buffer, 0, bytesRead);
				
				//do not send update after each write, cause it will trigger too much updates
				if(listener!=null && iterationCounter%100 == 0){
					listener.updateProgress(((float) bytesReadSoFar ) / dataLength);
				}
				
				//stop executing?
				if(stopCallback != null && iterationCounter%100 == 0){
					if(stopCallback.stopExecution()){
						break;
					}
				}

				iterationCounter++;

			}
			out.flush();
			
			if(listener!=null){
				listener.updateProgress(((float) bytesReadSoFar ) / dataLength);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			out.close();
			in.close();
		}

	}
	
	public interface OnWavToRawExtractionProgressUpdateListener{
		public void updateProgress(float progress);
	}
	
	/**When parsing, parser from time to time checks whether it should stop executing,
	 * so hosting thread could end immediately if canceled by user.
	 *
	 */
	public interface StopExectutionCallback{
		/**When implemented this method should return whether execution should stop.
		 * 
		 * @return
		 */
		public boolean stopExecution();
	}



	
	
	
	public static void main(String[] args) throws IOException {
//		WavParserBetter parser = new WavParserBetter("C:/Users/Antonio/Desktop/01 - Ours.wav");
//		WavParserBetter parser = new WavParserBetter("C:/Users/Antonio/Desktop/05 - Another Brick in the Wall, Part 2.wav");
		WavParserBetter parser = new WavParserBetter("C:/Users/Antonio/Desktop/pokvareni ours.wav");
		parser.parse();
	}
	
	
}
