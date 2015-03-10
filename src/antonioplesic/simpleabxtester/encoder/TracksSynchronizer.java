package antonioplesic.simpleabxtester.encoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;

import android.util.Log;

public class TracksSynchronizer {

	int nChannels;
	int sampleSize;
	int endianes;

	char left1[];
	char left2[];
	
	int globalDifferenceSignal[];
	int globalLength;

	public TracksSynchronizer(int nChannels, int sampleSize) {

		this.nChannels = nChannels;
		this.sampleSize = sampleSize;

	}

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

	public void loadSamples(String path1, String path2, long offset, long length)
			throws IOException {

		if (this.nChannels == 2 && sampleSize == 16) {

			long bytesPerTrack = (sampleSize / 8) * nChannels * (length);
			long samplesPerChannel = (bytesPerTrack / nChannels)
					/ (sampleSize / 8);

			byte data1[] = new byte[(int) bytesPerTrack];
			byte data2[] = new byte[(int) bytesPerTrack];

			// char right1[] = new char[(int) samplesPerChannel];
			//
			// char right2[] = new char[(int) samplesPerChannel];

			FileInputStream f1 = null;
			FileInputStream f2 = null;
			try {
				f1 = new FileInputStream(new File(path1));
				f2 = new FileInputStream(new File(path2));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			long bytesToSkip = (offset * sampleSize * nChannels) / 8;
			// System.out.println("bitova za preskociti: " + bytesToSkip);

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
			 * TODO: separating right channels, proboably not important, one
			 * channel is enough for calculating average power difference
			 * beetwen tracks, or ultimatly for synchronization
			 */

			f1.close();
			f2.close();

			// dump(data1);
			// dump(data2);
		}

	}

	private char[] separateChannel(long samplesPerChannel, byte[] data1) {
		char channel[] = new char[(int) samplesPerChannel];
		for (int sampleId = 0; sampleId < samplesPerChannel; sampleId++) {
			char first8Bits = (char) data1[4 * sampleId];
			first8Bits = (char) (first8Bits << 8);
			// System.out.println(String.format("%02X", (int) first8Bits));

			char last8Bits = (char) data1[4 * sampleId + 1];
			last8Bits = (char) (last8Bits & 0xFF);
			// System.out.println(String.format("%02X", (int) last8Bits));

			char mergedLeft = (char) (first8Bits | last8Bits);
			channel[sampleId] = mergedLeft;
			// System.out.println(String.format("%04X", (int) mergedLeft));
		}
		return channel;
	}

	/**
	 * Calculates number of samples that needs to be clipped (or padded) from
	 * track2 in order to achieve synchronization
	 * <p>
	 * If returned number of samples <i>nsam</i> is positive, track2 is
	 * <i>nsam</i> samples behind the track1, and should be clipped of
	 * <i>nsam</i> beginning samples. If returned number is negative, then the
	 * opposite holds: track1 is trailing behind track2, so track1 should be
	 * clipped, or alternatively track2 should be padded with <i>nsam</i>
	 * samples at the beginning.
	 * 
	 * @param offsetLimit
	 *            how much samples in each direction should be searched,
	 *            recommended at least 3000
	 * 
	 * @return Number of samples track2 is trailing behind track1
	 */
	public int getSynchronizationOffset(int offsetLimit) {

		double differencePower = Double.POSITIVE_INFINITY;
		int bestOffset = 0;

		// TODO: baci exception kada je offset predug u odnosu na ucitane
		// sampleove

		for (int offset = 0; offset < offsetLimit; offset++) {
			double power = signalStrength(differenceSignal(this.left1, 0,
					this.left2, offset));
			if (power <= differencePower) {
				// System.out.println(" " + power + " " + offset);
				differencePower = power;
				bestOffset = offset;
			}
		}

		for (int offset = 0; offset < offsetLimit; offset++) {
			double power = signalStrength(differenceSignal(this.left1, offset,
					this.left2, 0));
			if (power <= differencePower) {
				// System.out.println(" " + power + " -" + offset);
				differencePower = power;
				bestOffset = -offset;
			}
		}

		return bestOffset;
	}
	
	
	public int getSynchronizationOffset2(int offsetLimit, onSyncronizationProgressUpdateListener listener, StopExectutionCallback stopCallback) {

		//TODO: stopCallback
		
		double differencePower = Double.POSITIVE_INFINITY;
		int bestOffset = 0;
		
		int requiredIterations = 2*offsetLimit; //Approximately
		int iteration = 0;

		// TODO: baci exception kada je offset predug u odnosu na ucitane
		// sampleove

		for (int offset = 0; offset < offsetLimit; offset++) {
			
			calculateDifferenceSignal(this.left1, 0, this.left2, offset);
			double power = signalStrength(globalDifferenceSignal, globalLength);
			
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
			}
		}

		
		
		for (int offset = 0; offset < offsetLimit; offset++) {
			
			calculateDifferenceSignal(this.left1, offset,this.left2, 0);
			double power = signalStrength(globalDifferenceSignal, globalLength);
			
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
			}
		}
		
		return bestOffset;
	}

	public double signalStrength(int signal[]) {

		double sumOfPowers = 0;

		for (int sample : signal) {
			sumOfPowers += (((float) sample) * sample);
		}

		double averagePower = sumOfPowers / signal.length;

		return averagePower;
	}
	
	public double signalStrength(int signal[], int signalLength){
		
		double sumOfPowers = 0;
		
		for(int i = 0; i<signalLength; i++){
			int sample = signal[i];
			sumOfPowers += (((float) sample) * sample);
		}
		
		double averagePower = sumOfPowers / signalLength;
		
		return averagePower;
	}

	//TODO: bezveze je svaki put alocirati novi signal. Alocirati memoriju jednom, a potom ju iznova puniti
	private int[] differenceSignal(char signal1[], int offset1, char signal2[],
			int offset2) {

		int len1 = signal1.length - offset1;
		int len2 = signal2.length - offset2;

		int shorterLength = (len1 <= len2) ? len1 : len2;

		int differenceSignal[] = new int[shorterLength];

		for (int i = 0; i < shorterLength; i++) {
			differenceSignal[i] = sampleValue(signal1[i + offset1], true) - sampleValue(signal2[i + offset2], true);
		}

		return differenceSignal;
	}
	
	private void calculateDifferenceSignal(char signal1[], int offset1, char signal2[], int offset2){
		
		//initialize differenceSignal array for the first time
		if(this.globalDifferenceSignal == null){
			this.globalDifferenceSignal = new int[signal1.length];
		}
		
		int len1 = signal1.length - offset1;
		int len2 = signal2.length - offset2;
		
		int shorterLength = (len1 <= len2) ? len1 : len2;
		
		for(int i = 0; i<shorterLength; i++){
			this.globalDifferenceSignal[i] = sampleValue(signal1[i + offset1], true) - sampleValue(signal2[i + offset2], true);
		}
		
		this.globalLength = shorterLength;
	}
	
	
	

	private int swapEndianess16(char sample) {

		int first8Bits = (sample >> 8) & 0x000000ff;
		int second8Bits = sample & 0x000000ff;

		second8Bits = second8Bits << 8;

		return first8Bits | second8Bits;
	}

	public int sampleValue(char sample, boolean flipped) {

		// but first switch bytes due to endianess
		if (flipped) {
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

		// try {
		// tempFile = File.createTempFile(original.getName(), ".tmp", new
		// File(justPath));
		// } catch (IOException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }

		tempFile = new File("" + justPath + File.separator + original.getName()
				+ ".tmp");

//		System.out.println(original.getName());

		int bytesToTrim = (this.sampleSize / 8) * this.nChannels
				* samplesToTrim;

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

}