package antonioplesic.simpleabxtester.encoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

/**
 * For synchronizing tracks.
 */
public class TracksSynchronizer {

	int nChannels;
	int sampleSize;
	int endianes;

	char left1[];
	char left2[];
	
	int differenceSignal[];
	int windowSize;

	public TracksSynchronizer(int nChannels, int sampleSize) {

		this.nChannels = nChannels;
		this.sampleSize = sampleSize;
	}

	/**
	 * Loads samples into the synchronizer.
	 * @param path1 
	 * 				Path to file 1
	 * 
	 * @param path2
	 * 				Path to file 2
	 * 
	 * @param samplesToSkip
	 * 				How much samples to skip. Useful when tracks being synchronized are
	 * 				silent at the start (there is no point in synchronizing silence).
	 * 				Also, attempting synchronization at parts that contains actual music gives
	 * 				much better results.
	 * 
	 * @param windowSize
	 * 				How many samples are being taken into account during synchronization.
	 * 				Window size of for example 3000 means that the power of the  difference 
	 * 				signal calculation is based on 3000 samples.Implicitly, this also sets limit to
	 * 				how far can the two tracks be desynced for this procedure to work.
	 * 				Typical desyncronization that occurs during mp3 encoding/decoding
	 * 				is 2257 samples (due do priming (padding in front) with zeros). In order
	 * 				to sync original and such encoded/decoded file, you need sync window
	 * 				of at least 2257 samples.
	 * 
	 * @throws IOException
	 */
	public void loadSamples(String path1, String path2, long samplesToSkip, int windowSize)
			throws IOException {
		
		this.windowSize = windowSize;
		this.differenceSignal = new int[windowSize];

		if (this.nChannels == 2 && sampleSize == 16) {

			long bytesPerTrack = (sampleSize / 8) * nChannels * (windowSize*2); //why *2 ? See diagram.
			/*
			 * Read getSynchronizationOffset() documentation first for general idea behind
			 * this synchronization procedure.
			 * 
			 * Boxed area represents synchronization window. Here it is 4 samples long: what
			 * it means is that only 4 samples from each track will be used for calculation of a
			 * difference signal at any given moment. But, in order to calculate difference signal
			 * for different offsets, more than 4 samples per track must be actually loaded into
			 * the memory, but only 4 from each are used for calculation at any given time.
			 * 
			 * If offset limit is 4, that means we want to slide tracks against one another
			 * up to 4 samples in both directions.
			 * 
			 * Here only counterclockwise sliding is pictured. If we want to calculate difference
			 * when counterclockwise offset is 4 samples long, it is clear from the last picture
			 * that then samples numbered 5, 6, 7 and 8 from track 1 must be loaded into memory.
			 * 
			 * If counterclockwise offset is 1 sample long, then samples 2, 3, 4 and 5 from
			 * track 1 must be loaded, and so on.  
			 *           _______   
			 * Track 1  |1 1 1 1│1 1 1 1¦1 1 1 1 1 1 1 1      Offset is 0
			 * Track 2  |2 2 2 2│2 2 2 2¦2 2 2 2 2 2 2 2
			 *          ^‾‾‾‾‾‾‾^
			 *          |       |             windowSize
			 *          |‾‾‾‾‾‾‾|‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
			 *          |  _____|_
			 * Track 1  |1|1 1 1.1|1 1 1¦1 1 1 1 1 1 1 1      Offset is 1, counterclockwise
			 * Track 2  | |2 2 2.2|2 2 2 2¦2 2 2 2 2 2 2
			 *          |  ‾‾‾‾‾|‾
			 *          |       |   .   
			 *          |       |   .
			 *          |       |   .
			 *          |       |_______
			 * Track 1  |1 1 1 1|1 1 1 1|1 1 1 1 1 1 1 1      Offset is 4, counterclockwise
			 * Track 2  |       |2 2 2 2|2 2 2 2¦2 2 2 2      
			 *          |        ‾‾‾‾‾‾‾^                     This is valid as long as
			 *          |               |   windowSize*2      offsetLimit <= windowSize
			 *           ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾      (offsetLimit defined later)
			 */
			long samplesPerChannel = (bytesPerTrack / nChannels) / (sampleSize / 8);

			byte data1[] = new byte[(int) bytesPerTrack];
			byte data2[] = new byte[(int) bytesPerTrack];


			FileInputStream f1 = null;
			FileInputStream f2 = null;
			try {
				f1 = new FileInputStream(new File(path1));
				f2 = new FileInputStream(new File(path2));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			long bytesToSkip = (samplesToSkip * sampleSize * nChannels) / 8;

			f1.skip(bytesToSkip);
			f2.skip(bytesToSkip);

			int read1 = 0;
			int read2 = 0;

			while (read1 < data1.length) {
				read1 += f1.read(data1);
			}

			while (read2 < data2.length) {
				read2 += f2.read(data2);
			}

			// separate the left channel out of the first track
			char[] left1 = separateChannel(samplesPerChannel, data1);
			this.left1 = left1;

			// separate the left channel out of the second track
			char[] left2 = separateChannel(samplesPerChannel, data2);
			this.left2 = left2;
			/*
			 * separating right channels, probably not important, one
			 * channel is enough for calculating average power difference
			 * between tracks, or ultimately for synchronization.
			 */

			f1.close();
			f2.close();

			// dump(data1);
			// dump(data2);
		}
	}
	

	/**
	 * Extracts one channel (left one) from the track snippet. 
	 * One channel is enough for synchronization purposes.
	 * 
	 * @param samplesPerChannel		how much samples each channel contains
	 * @param snippet 				array holding stereo 16 bit pcm data
	 * @return 						array holding left channel data
	 */
	private char[] separateChannel(long samplesPerChannel, byte[] snippet) {
		char channel[] = new char[(int) samplesPerChannel];
		for (int sampleId = 0; sampleId < samplesPerChannel; sampleId++) {
			char first8Bits = (char) snippet[4 * sampleId];
			first8Bits = (char) (first8Bits << 8);
			// System.out.println(String.format("%02X", (int) first8Bits));

			char last8Bits = (char) snippet[4 * sampleId + 1];
			last8Bits = (char) (last8Bits & 0xFF);
			// System.out.println(String.format("%02X", (int) last8Bits));

			char mergedLeft = (char) (first8Bits | last8Bits);
			channel[sampleId] = mergedLeft;
			// System.out.println(String.format("%04X", (int) mergedLeft));
		}
		return channel;
	}

	
	/**
	 * Returns the number of samples track2 is trailing behind track1.<br><br>
	 *  
	 * How the synchronization works:
	 * Let's say we have two signals, a and b. They are identical, or nearly identical,
	 * but one of them, to be precise, b, is padded with 2 zeros at the start (for example,
	 * encoder added some silence at the start, as some encoders do that).
	 * <pre>
	 * Signals:
	 *      a)  -1 -1  0  1  2  2  3  3  2  2  1  1  0  0 -1 -1 -2 -2 -3 -3 ...
	 *      b)   0  0 -1 -1  0  1  2  2  3  3  2  2  1  1  0  0 -1 -1 -2 -2 ... </pre>
	 *      
	 * To synchronize the tracks, we "slide" them against each other multiple times,
	 * in both directions, each time offseting the slide a bit. For each position,
	 * we calculate the power of the difference signal. Tracks should be in sync when
	 * power of the difference signal is the lowest.
	 * <pre>
	 * Window size = 5
	 * offsetLimit = 3
	 * 
	 * Offset 0
	 *      a)  -1 -1  0  1  2
	 *      b)   0  0 -1 -1  0
	 *      ------------------
	 *   diff)  -1 -1  1  2  2   power = (-1)^2 + (-1)^2 + 1^2 + 2^2 + 2^2 
	 *                                 = 11
	 *   
	 * Offset 1, clockwise
	 *      a)     -1 -1  0  1  2  -->   
	 *      b)   0  0 -1 -1  0  1  <-- 
	 *      ---------------------
	 *   diff)     -1  0  1  1  1   power = (-1)^2 + 0^2 + 1^2 + 1^2 + 1^2 
	 *                                    = 4
	 *                                    
	 * Offset 2, clockwise
	 *      a)        -1 -1  0  1  2	
	 *      b)   0  0 -1 -1  0  1  2
	 *      ------------------------
	 *   diff)         0  0  0  0  0   power = 0
	 * 		
	 * Offset 3, clockwise
	 *      a)           -1 -1  0  1  2
	 *      b)  0  0  -1 -1  0  1  2  2
	 *      ---------------------------
	 *   diff)            0 -1 -1 -1  0   power = 3
	 *
	 * Offset 1, counterclockwise
	 *      a) -1 -1  0  1  2  2
	 *      b)     0  0 -1 -1  0
	 *      --------------------
	 *   diff)    -1  0  2  3  2	power = 18
	 *
	 * Offset 2, counterclockwise
	 *      a) -1 -1  0  1  2  2  3
	 *      b)        0  0 -1 -1  0
	 *      -----------------------
	 *   diff)        0  1  3  3  3   power = 28
	 *
	 * Offset 3, counterclockwise
	 *      a) -1 -1  0  1  2  2  3  3
	 *      b)           0  0 -1 -1  0
	 *      --------------------------
	 *   diff)           1  2  3  4  3   power = 39   </pre>
	 *   
	 * Clearly, power of the difference signal is the smallest in the case of clockwise
	 * offset by 2 samples, so to synchronize these two tracks, either track a should
	 * be padded with 2 zero samples in front, or track b should be clipped of its
	 * first two samples.<pre></pre> 
	 * 
	 * @param offsetLimit
	 *            how much should tracks be time shifted, slided around in each
	 *            direction when trying to achieve syncronization. Must be 
	 *            less or equal to windowSize set during sample loading. Make
	 *            sure this holds, as it is not currently checked against, but
	 *            will produce wrong results if it doesen't hold.
	 *            Recommended at least 3000 (this requires windowSize of
	 *            at least 3000 samples).
	 *            
	 * @param listener
	 *            progress update listener
	 *            	
	 * @param stopCallback
	 *            using this callback, method periodically checks whether it
	 *            should stop executing, for example because user canceled the job
	 *            
	 * @return
	 *            Number of samples track2 is trailing behind track1
	 *  
	 */
	public int getSynchronizationOffset(int offsetLimit, onSyncronizationProgressUpdateListener listener, StopExectutionCallback stopCallback) {
		
		Long startTime = System.currentTimeMillis();
		
		double differencePower = Double.POSITIVE_INFINITY;
		int bestOffset = 0;
		
		int requiredIterations = 2*offsetLimit; //Approximately
		int iteration = 0;

		// TODO: throw exception when offset is too long with respect to loaded samples

		for (int offset = 0; offset < offsetLimit; offset++) {
			
			calculateDifferenceSignal(this.left1, 0, this.left2, offset);
			double power = signalPower(differenceSignal, windowSize);
			
			iteration = offset;
			
			if(iteration%100==0){
				listener.progressUpdate( ((float) iteration)/requiredIterations );
			}
			
			if(iteration%100==0){
				if(stopCallback.stopExecution()){
					return 0;
				}
			}
			
			if (power <= differencePower) {
				// System.out.println(" " + power + " " + offset);
				differencePower = power;
				bestOffset = offset;
//				Log.i(this.getClass().getName(),"new best offset = " + bestOffset + ", power = " + differencePower);
			}
		}

		for (int offset = 0; offset < offsetLimit; offset++) {
			
			calculateDifferenceSignal(this.left1, offset,this.left2, 0);
			double power = signalPower(differenceSignal, windowSize);
			
			iteration+=1;
			if(iteration%100==0){
				listener.progressUpdate( ((float) iteration)/requiredIterations );
			}
			
			if(iteration%100==0){
				if(stopCallback.stopExecution()){
					return 0;
				}
			}
		
			if (power <= differencePower) {
				// System.out.println(" " + power + " -" + offset);
				differencePower = power;
				bestOffset = -offset;
//				Log.i(this.getClass().getName(),"new best offset = " + bestOffset + ", power = " + differencePower);
			}
		}
		
		Long endTime = System.currentTimeMillis();
//		Log.i(this.getClass().getName(),"best offset: " + bestOffset + ", found after " + ((float) (endTime-startTime))/1000);
		
		return bestOffset;
	}
	

	/**
	 * Calculates power of a discrete signal.<br>
	 * 
	 * P = sum( (s_1)^2 + (s_2)^2 + ... + (s_nsam)^2 ) / nsam , nsam=signal.length
	 *  
	 * @param signal 	Array containing integer samples of a signal.
	 * @return			Power of the discrete signal.
	 */
	public double signalPower(int signal[]) {
		return signalPower(signal, signal.length);
	}
	
	/**
	 * Calculates power of a discrete signal.<br>
	 * 
	 * P = sum( (s_1)^2 + (s_2)^2 + ... + (s_nsam)^2 ) / nsam , nsam=signalLength
	 *  
	 * @param signal 		Array containing integer samples of a signal.
	 * @param signalLength	Length of a signal
	 * @return				Power of the discrete signal.
	 */
	public double signalPower(int signal[], int signalLength){
		
		double sumOfPowers = 0;
		
		for(int i = 0; i<signalLength; i++){
			int sample = signal[i];
			sumOfPowers += (((float) sample) * sample);
		}
		
		double averagePower = sumOfPowers / signalLength;
		
		return averagePower;
	}
	
	/**
	 * Calculates difference signal of two input signals difference[] = signal1[] - signal2[].
	 * Signal lengths are equal to windowSize. Difference is stored in differenceSignal 
	 * field.
	 * 
	 * @param signal1		Signal being subtracted from
	 * @param offset1 		Samples to skip in signal1
	 * @param signal2		Signal that is being subtracted
	 * @param offset2		Samples to skip in signal2
	 */
	private void calculateDifferenceSignal(char signal1[], int offset1, char signal2[], int offset2){
		
		//TODO: check if offsetLimit <= windowSize
		
		for(int i = 0; i<windowSize; i++){
			this.differenceSignal[i] = sampleValue(signal1[i + offset1], true) - sampleValue(signal2[i + offset2], true);
		}
		
	}
	
	/**
	 * Swaps endianness of a 16 bit (char) sample. <br>
	 * 
	 * Sample of value 0x00EE is returned as 0xEE00
	 * 
	 * @param sample
	 * @return	Sample with reordered bytes.
	 */
	private int swapEndianess16(char sample) 
	{
		int first8Bits = (sample >> 8) & 0x000000ff;
		int second8Bits = sample & 0x000000ff;
		second8Bits = second8Bits << 8;

		return first8Bits | second8Bits;
	}

	/**
	 * Returns integer value of the sample.
	 * 
	 * @param sample 			16 bit sample
	 * @param isLittleEndian 	Whether the sample format is littleEndian, if 
	 * 							not then it is necessarily bigEndian
	 * @return					Integer value of the sample.
	 */
	private int sampleValue(char sample, boolean isLittleEndian) 
	{
		// but first switch bytes due to endianess
		if (isLittleEndian) {
			sample = (char) swapEndianess16(sample);
		}

		int sign;
		int kompl;

		if ((sample & 0x00008000) == 0) {
			sign = 1;
			kompl = ((sample) & 0x0000ffff);
		} else {
			sign = -1;
			kompl = ((~sample) & 0x0000ffff);
			kompl = kompl + 1;
		}

		return sign * kompl;
	}

	/**
	 * Trims certain amount of samples from the start of a raw pcm file.
	 * 
	 * @param fileToTrimmPath	Path to the raw pcm file being trimmed.
	 * @param samplesToTrim		Number of samples to remove from the start of file.
	 */
	public void trimmStart(String fileToTrimmPath, int samplesToTrim) {

		File original = new File(fileToTrimmPath);
		File tempFile = null;
		String justPath;

		String absolutePath = original.getAbsolutePath();
//		System.out.println(absolutePath);
		int lastIndex = absolutePath.lastIndexOf(File.separator);
		if (lastIndex != -1) {
			justPath = absolutePath.substring(0, lastIndex);
		} else {
			justPath = "";
		}

		tempFile = new File("" + justPath + File.separator + original.getName() + ".tmp");

//		System.out.println(original.getName());

		int bytesToTrim = (this.sampleSize / 8) * this.nChannels * samplesToTrim;

		BufferedInputStream in = null;
		BufferedOutputStream out = null;

		byte buffer[] = new byte[10000];
		int bytesRead = 0;

		try {
			in = new BufferedInputStream(new FileInputStream(original));
			out = new BufferedOutputStream(new FileOutputStream(tempFile));

			long skipped = in.skip(bytesToTrim);
//			System.out.println(skipped);

			while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
//				System.out.println(bytesRead);

				// out.write(buffer); //kretenu -.- ,pises i smece; zadnji
				// buffer nije pun, a ti ga svejedno ispises
				out.write(buffer, 0, bytesRead);
			}

			out.flush();
			out.close();
			in.close();
			
			boolean deleted = original.delete();
			//removedLog.w("Tracks segment", "Untrimmed file deleted:" + deleted);
			
			boolean renamed = tempFile.renameTo(original);
			//removedLog.w("Tracks segment", "Trimmed temp file renamed:" + renamed);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public interface onSyncronizationProgressUpdateListener{
		public void progressUpdate(float progress);
	}
	
	public interface StopExectutionCallback{
		public boolean stopExecution();
	}
	
	
	/**
	 * Dump data to stdout in hex format for debugging purposes.
	 * @param data
	 */
	public void dump(byte data[]) {

		int i = 0;
		for (byte b : data) {

			if (i != 15) {
				System.out.print(String.format("%02X ", b));
				i++;
			} else {
				System.out.println(String.format("%02X", b));
				i = 0;
			}
		}
		System.out.println();
	}
		
}
