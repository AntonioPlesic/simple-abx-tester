package antonioplesic.simpleabxtester.encoder;

/** Provides interface for decoding flac files using libFLAC library.
 */
public class flacDecoder {

	static{
		System.loadLibrary("FLAC");
	}
	
	/**
	 * Returns FlacInfo object containing properties of a FLAC file given by
	 * sourcePath: sample rate, bits per sample and channel number.
	 * @return FlacInfo object containing properties of a FLAC file given by
	 * sourcePath: sample rate, bits per sample and channel number.
	 * @param sourcePath Path to the FLAC file.
	 */
	public native FlacInfo getMetadata(String sourcePath);
	
	
	/** 
	 * Decodes FLAC file to raw PCM file
	 * 
	 * @param sourcePath 	Path to the FLAC file being decoded
	 * @param targetPath 	Path to raw pcm file sourcePath file is being decoded to
	 * @param listener 		Listener whom to report progress to. May be <code>null</code>.
	 * @param stopCallback	Used to periodically check whether decoding job should stop. May be <code>null</code>.
	 */
	private native void decodeFile(String sourcePath, String targetPath, OnDecoderProgressUpdateListener listener, StopExectutionCallback stopCallback); 
	
	/**
	 * Decodes FLAC file to raw PCM file
	 * 
	 * @param sourcePath 	Path to the FLAC file being decoded
	 * @param targetPath 	Path to raw pcm file sourcePath file is being decoded to
	 * @param listener 		Listener whom to report progress to. May be <code>null</code>.
	 * @param stopCallback	Used to periodically check whether decoding job should stop. May be <code>null</code>.
	 */
	public void decodeFLAC(String sourcePath, String targetPath, OnDecoderProgressUpdateListener listener, StopExectutionCallback stopCallback){
		decodeFile(sourcePath, targetPath, listener, stopCallback);
	}
	
	/**
	 * Implement this interface in object that has authority do decide whether flac decoding job should stop.
	 * Flac decoder then periodically calls stopExecution method to see whether it should stop decoding, for
	 * example because user requested it to stop.
	 */
	public interface StopExectutionCallback{
		
		/**
		 * @return Should return true if execution should stop, false otherwise.
		 */
		public boolean stopExecution();
	}
	
	/**
	 * Implement to receive progress updates.
	 */
	public interface OnDecoderProgressUpdateListener{
		void updateProgress(float progress);
	}
	
}
