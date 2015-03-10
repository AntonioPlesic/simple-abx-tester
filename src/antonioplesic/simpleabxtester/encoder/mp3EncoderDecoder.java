package antonioplesic.simpleabxtester.encoder;

import java.io.File;



import android.app.Application.OnProvideAssistDataListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import antonioplesic.simpleabxtester.settings.SettingsHelper;

public class mp3EncoderDecoder {
	
	static{
		System.loadLibrary("mp3lame");
	}
	
	/**
	 * 
	 * 
	 * 
	 *
	 */
	public final static class Presets{
		/** Drek */
		public final static int V9 = 410; /*Vx to match Lame and VBR_xx to match FhG*/
		public final static int VBR_10 = 410;
		public final static int V8 = 420;
		public final static int VBR_20 = 420;
		public final static int V7 = 430;
		public final static int VBR_30 = 430;
		public final static int V6 = 440;
		public final static int VBR_40 = 440;
		public final static int V5 = 450;
		public final static int VBR_50 = 450;
		public final static int V4 = 460;
		public final static int VBR_60 = 460;
		public final static int V3 = 470;
		public final static int VBR_70 = 470;
		public final static int V2 = 480;
		public final static int VBR_80 = 480;
		public final static int V1 = 490;
		public final static int VBR_90 = 490;
		public final static int V0 = 500;
		public final static int VBR_100 = 500;
		
		
		public final static int CBR_64 = 64 ;
		public final static int CBR_96 = 96;
		public final static int CBR_128 = 128;
		public final static int CBR_192 = 192;
		public final static int CBR_320 = 320;
	}
	
	/** Returns string representation of selected bitrate
	 * 
	 * For example: 
	 * <t>getStr(320) returns "320",
	 * <t>getStr(500) returns "V0",
	 *
	 * @param bitrateOrPresetValue - value defined in {@link mp3EncoderDecoder.Presets}
	 * @return
	 */
	public static String getStr(int bitrateOrPresetValue){
		
		String stringRepresentation = "";
		
		switch (bitrateOrPresetValue) {
		case Presets.V9: stringRepresentation = "V9"; break;
		case Presets.V8: stringRepresentation = "V8"; break;
		case Presets.V7: stringRepresentation = "V7"; break;
		case Presets.V6: stringRepresentation = "V6"; break;
		case Presets.V5: stringRepresentation = "V5"; break;
		case Presets.V4: stringRepresentation = "V4"; break;
		case Presets.V3: stringRepresentation = "V3"; break;
		case Presets.V2: stringRepresentation = "V2"; break;
		case Presets.V1: stringRepresentation = "V1"; break;
		case Presets.V0: stringRepresentation = "V0"; break;
		default:
			stringRepresentation = "" + bitrateOrPresetValue;
		}
		return stringRepresentation;
	}
	
	public static String getBitrateDescription(int presetCode){
		if(presetCode>320){
			return "LAME preset " + getStr(presetCode) + ", variable bit rate";
		}
		else{
			return getStr(presetCode) + " kbps, constant bit rate";
		}
	}
	
	private native void initEncoderCBR(int numChannels, int sampleRate, int bitRate,
			int mode, int quality, int outSampleRate);
	
	private native void initEncoderPreset(int numChannels, int sampleRate,
			int mode, int preset);
	
	public native Mp3Info getInfo(String sourcePath);
	
	private native void initDecoder();
	
	private native void destroyEncoder();
	
	private native void destroyDecoder();
	
	private native void encodeFile(String sourcePath, String targetPath, OnEncoderProgressUpdateListener listener, StopExectutionCallback stopCallback);
	
	private native void decodeFile(String sourcePath, String targetPath, OnDecodeProgressUpdateListener listener, StopExectutionCallback stopCallback);
	
	/**
	 * Encodes raw PCM file to mp3, using encoding parameters specified in sharedPreferences
	 * @param context 		Used to get sharedPrefs from which encoding parameters will be read (so any context that provides access to this app's sharedPrefs is OK I guess).
	 * @param pcmFilePath	Path to raw PCM file which should be encoded to mp3. 
	 * @param mp3OutputPath	Path to file where resulting mp3 should be saved.
	 * @param listener		Listener whom to report progress to. May be <code>null</code>.
	 * @param stopCallback	Used to periodically check whether encoding job should stop. May be <code>null</code>.
	 */
	public void selfContainedEncodeFile(Context context, String pcmFilePath, String mp3OutputPath, OnEncoderProgressUpdateListener listener, StopExectutionCallback stopCallback){
		
		//TODO: loadaj iz settinga ako postoje, inace default
		int NUM_CHANNELS = 2;
		
		//TODO: for now 
		int SAMPLE_RATE = 44100;
		int OUT_SAMPLE_RATE = 44100;
	
		int MODE = 1;
		
		int BITRATE;
		int QUALITY;
		int PRESET;
		
		boolean isPreset = false;
		boolean isCBR = false;
		
//		SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
	
		SettingsHelper settingsHelper = new SettingsHelper(context);

//		BITRATE = Integer.parseInt(sharedPref.getString("encoderBitrate", ""));
		BITRATE = settingsHelper.getBitrate(SettingsHelper.RETURN_DEFAULT_IF_DISABLED);
		PRESET = BITRATE;
//		QUALITY = Integer.parseInt(sharedPref.getString("encoderQuality", ""));
		QUALITY = settingsHelper.getQuality(SettingsHelper.RETURN_DEFAULT_IF_DISABLED);
		
		if(BITRATE>320){
			isPreset = true;
		}
		else{
			isCBR = true;
		}
		
		//removedLog.w(this.getClass().getName(),"Starting encoding:\nBitrate = " + BITRATE + "\nQuality = " + QUALITY);
		
		
		//TODO: bufferSize odrediti prema num_samples -> modificirati initEncoder
		
		File inputFile = new File(pcmFilePath);
		File outputFile = new File(mp3OutputPath);
		//removedLog.e(this.getClass().getName(),"Can write to " + outputFile.getAbsolutePath() +": " +  outputFile.canWrite());
		
		if(isCBR){
			initEncoderCBR(NUM_CHANNELS, SAMPLE_RATE, BITRATE, MODE, QUALITY, OUT_SAMPLE_RATE);
		}
		else if(isPreset){
			initEncoderPreset(NUM_CHANNELS, SAMPLE_RATE, MODE, PRESET);
		}
		
		encodeFile(inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), listener, stopCallback);
		
		destroyEncoder();
	}
	
	/**
	 * Decodes mp3 file to raw PCM file.
	 * @param srcPath		Path to mp3 file to decode.
	 * @param destPath		Path to pcm file into which mp3 should be decoded.
	 * @param listener		Listener whom to report progress to. May be <code>null</code>.
	 * @param stopCallback	Used to periodically check whether decoding job should stop. May be <code>null</code>.
	 */
	public void selfContainedDecodeFile(String srcPath, String destPath, OnDecodeProgressUpdateListener listener, StopExectutionCallback stopCallback) {
		initDecoder();
		decodeFile(srcPath,destPath,listener,stopCallback);
		destroyDecoder();
	}
	
	/**
	 * Implement to receive progress updates from encoder.
	 */
	public interface OnEncoderProgressUpdateListener{
		/**
		 * @param progress	Current progress in range [0, 1] (multiply by 100 to get percentage).
		 */
		void updateProgress(float progress);
	}
	
	/**
	 * Implement to receive progress updates from decoder.
	 */
	public interface OnDecodeProgressUpdateListener{
		/**
		 * @param progress	Current progress in range [0, 1] (multiply by 100 to get percentage).
		 */
		void updateProgress(float progress);
	}
	
	/**
	 * Encoding (or decoding, albeit not nearly as much) is a long running process, with durations in order
	 * of minutes. Implementing this callback enables almost immediate stopping of thread that hosts 
	 * encoding/decoding job, by virtue of stopping the job's own execution loop. In other words, method 
	 * that represents encoding/decoding job simply returns, so thread doesen't have to wait for it to 
	 * naturally finish.
	 */
	public interface StopExectutionCallback{
		/**
		 * When implemented this method should return whether execution should stop.
		 * @return Whether method should stop executing
		 */
		public boolean stopExecution();
	}

}
