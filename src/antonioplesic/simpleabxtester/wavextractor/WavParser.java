package antonioplesic.simpleabxtester.wavextractor;
//dijelovi preuzeti sa: http://stackoverflow.com/questions/19991405/how-can-i-detect-whether-a-wav-file-has-a-44-or-46-byte-header/19991594#19991594

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class WavParser {

	static boolean debug = false;

	static WavInfo parseWave(File file) throws IOException {

		int position = 0;
		int dataStart = -1;
		int dataLength = -1;

		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			byte[] bytes = new byte[4];

			// read first 4 bytes
			// should be RIFF descriptor
			if (in.read(bytes) < 0) {
				return null;
			}
			position += 4;

			if (debug) { printDescriptor(bytes); }

			// first subchunk will always be at byte 12
			// there is no other dependable constant
			in.skip(8);
			position += 4;

			for (;;) {
				// read each chunk descriptor
				if (in.read(bytes) < 0) {
					break;
				}
				position += 4;

				if (debug) { printDescriptor(bytes); }
				String descriptor = new String(bytes, "US-ASCII");

				if (descriptor.equals("data")) {
					if (debug) { System.out.println("pronadjen data, start position (descriptor) " + Integer.toString(position)); }
					
					dataStart = position;
				}

				if (descriptor.equals("fmt ")) {
					if (debug) { System.out.println("pronadjen fmt, start position (descriptor) " + Integer.toString(position)); }
				}

				// read chunk length
				if (in.read(bytes) < 0) {
					break;
				}
				position += 4;

				int chunkLength = (bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8
						| (bytes[2] & 0xFF) << 16 | (bytes[3] & 0xFF) << 24;

				if (descriptor.equals("data")) {
					dataLength = chunkLength;
				}

				position += chunkLength;

				// skip the length of this chunk
				// next bytes should be another descriptor or EOF
				if (debug) {
					System.out.println(chunkLength);
				}
				in.skip(chunkLength);
			}

			if (debug) {
				System.out.println("end of file");
				System.out.println(dataStart);
				System.out.println(dataLength);
			}

		} finally {
			if (in != null) {
				in.close();
			}
		}

		return new WavInfo(dataStart, dataLength);
	}

	static void printDescriptor(byte[] bytes, int chunkSize) throws IOException {
		System.out.print("found '" + new String(bytes, "US-ASCII")
				+ "' descriptor");
		if (chunkSize == 0) {
			System.out.println();
		} else {
			System.out.println(chunkSize);
		}
	}

	static void printDescriptor(byte[] bytes) throws IOException {
		printDescriptor(bytes, 0);

	}

	public static void main(String[] args) {

		String inputFilePath;
		String outputFilePath;
		inputFilePath = "C:\\Users\\Author\\Desktop\\za konverziju\\You Were Mine WAV PCM.wav";
		outputFilePath = "C:\\Users\\Author\\Desktop\\za konverziju\\You Were Mine.pcm";

		try {
			extractRawDataFromWav(inputFilePath, outputFilePath, null, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void extractRawDataFromWav(String inputFilePath, String outputFilePath, OnWavToRawExtractionProgressUpdateListener listener, StopExectutionCallback stopCallback) throws IOException{
		
		WavInfo wavInfo = parseWave(new File(inputFilePath));
		copyFilePart(inputFilePath, outputFilePath, wavInfo.dataStart,
				wavInfo.dataLength,listener,stopCallback);
		
	}

	public static void copyFilePart(String inputFilePath,
			String outputFilePath, int dataStart, int dataLength,
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

}
