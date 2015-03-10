package antonioplesic.simpleabxtester;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;



import android.content.Context;
import android.os.Environment;
import antonioplesic.simpleabxtester.encoder.mp3EncoderDecoder;
import antonioplesic.simpleabxtester.settings.SettingsHelper;

public class DataManager {
	
	private Context context;
	
	public DataManager(Context context){
		this.context = context;
	}
	
	final static String TAG = "DataManager";
	private final static String defaultEncodedDecodedDataDirectory = "ABX test data";
	
	private static final boolean makeMp3InnaccessableToUser = true;
	
	/**
	 * Return path to <b>"default to user exposed"</b> data directory. <br>
	 * <br>
	 * Normally, app saves it's required files in user-unaccessible storage,
	 * to enable clearing that data manually by using Android's own Application Manager,
	 * or automatically during uninstall.<br><br>
	 * 
	 * If user chooses so in settings, some other directory can be used as data directory (working directory), 
	 * presumably one that can be accessed by user via file explorer of her choosing, so 
	 * the user can inspect the files generated/used by this app.
	 * However, Android's Application Manager can't clear that directory, as it is public (may be used by
	 * other applications).<br><br>
	 * 
	 * If user enables mentioned setting, this method returns path to default such directory,
	 * which looks something like:
	 * <pre>/storage/emulated/0/ABX test data</pre>
	 * where "/storage/emulated/0" is path typically returned by getExternalStorageDirectory(),
	 * that represent root of user visible directory tree. "ABX test data" is specific directory
	 * where app data is placed.
	 * 
	 * @return Path to <b>default exposed to user</b> data directory
	 */
	public static String getDefaultDirectoryPathCustom(){
		return Environment.getExternalStorageDirectory() + "/" + defaultEncodedDecodedDataDirectory;
	}
	
	/**
	 * Returns path to internal, system provided, user inaccessible directory where app stores
	 * audio data, i.e. working directory. This is the default such directory, which should be
	 * used always, unless user requested use of custom directory by checking the appropriate 
	 * setting.
	 *  
	 * @return Returns path to internal, system provided, user inaccessible working directory.
	 */
	public String getDefaultDirectoryPath(){
//		return Environment.getExternalStorageDirectory() + "/" + defaultEncodedDecodedDataDirectory;
		return context.getFilesDir().getAbsolutePath() + "/" + defaultEncodedDecodedDataDirectory;
	}
	
	/** Final part of the path that will be used to store encoded/decoded files the app operates on.<p>
	 * 
	 * For example, if user selects some directory "../someDirectory" as data folder, actual data folder
	 * used by the app should be "../someDirectory/myAppNameData". This is to prevent pollution of existing
	 * public directories. This way, app can delete the data directory without worrying that a user pointed 
	 * it to "Music", "Android" or similar top level shared directories (has this extra precaution not been 
	 * made, clearing data folder via app would clear those folders too, and that would be very bad).
	 * 
	 * @return Final part of the data directory path.
	 */
	public static String mandatoryDataDirectoryName(){
		return defaultEncodedDecodedDataDirectory;
	}

	/**
	 * Returns path to currently used working directory (data directory, where audio is stored).
	 * What path is returned depends on current settings. It is one of the following:<br>
	 * &nbsp;&nbsp; 1):	Default internal, user inaccessible directory, deleted on uninstall or manually via Application Manager<br>
	 * &nbsp;&nbsp; 2):	Default custom, user accessible directory, not deleted on uninstall, not deletable via Application Manager<br>
	 * &nbsp;&nbsp; 3): Fully custom, user accessible directory, not deleted on uninstall, not deletable via Application Manager<br>
	 * @return Path to currently used working directory
	 * @throws StorageUnaccessibleException
	 */
	public String getStorageDirectoryPath() throws StorageUnaccessibleException{
		
		File dir = null;
		SettingsHelper h = new SettingsHelper(context);

		/*app will curently save all its encoded/decoded files in the following directory,
		 * however, this is only temporary. Not sure whether this is the best way to store such
		 * files, because it can be argued they should be invisible to user so user wouldn't use this
		 * app as an encoder/decoder (due to patent/royalty issues with mp3 encoding algorithms). However, 
		 * target audience for this app is expected to be highly skeptical about comparison results
		 * achieved with this app, and should be able to pull files and manually examine them,
		 * either to provide suggestions, see for themselves that encoding is correct, and similar
		 * causes. And since this app is strictly non-profit (at least for now), this can be considered
		 * fair use, but I'm sure mp3 patent holders would disagree :)  .
		 */

		//removedLog.w(this.getClass().getName(),"Uses custom directory: " + h.usesCustomDirectory());
		
		boolean usesDefaultDirectory = !h.usesCustomDirectory();
		
		if(usesDefaultDirectory){
			dir = new File(getDefaultDirectoryPath());
			//removedLog.d(this.getClass().getName(),"Uses default directory: " + dir.getAbsolutePath());
		}
		else{
			dir = new File( h.getCustomDirectory() + "/" + defaultEncodedDecodedDataDirectory);
			//removedLog.d(this.getClass().getName(),"Uses custom directory: " + dir.getAbsolutePath());
		}
		
		//check if directory exist, if not, create it
		if(dir.exists() && dir.isDirectory()) {
			//removedLog.w(this.getClass().getName(),"Working directory already exists");
		}
		else{
			//removedLog.w(this.getClass().getName(),"Working direcory doesen't exist, creating");
			
			//if unable to create directory, it probably points to the unmounted sdcard
			//TODO: if unrooted KitKat, it can't point to ACTUAL sd card, due to some weird Google philosophy. Supposedly fixed in 5.0+.
			boolean directoryCreated = dir.mkdir();
			if(!directoryCreated){
				//removedLog.w(this.getClass().getName(),"Unable to create working directory");
				throw new StorageUnaccessibleException();
			}
		}

		//TODO: Look into using Environment.getExternalStoragePublicDirectory(type);

		return dir.getAbsolutePath();
	}
	
	public boolean isProcessedForCurrentSettings(File originalFile) throws StorageUnaccessibleException{
		
		//TODO:Arhitekturno pogresan pristup, metoda, iako je posve druge klase
		//TODO:U settings dio dodaj klasu koja služi kao setting
				
		File reqiredSourceFile       = new File(getSourcePCMPath(originalFile));
		File requiredDecodedMp3File  = new File(getMp3DecodedPath(originalFile)) ;
		
		//---------------------------------------------------------------------
		// Requirement 1) Files with right filenames must exist
		//---------------------------------------------------------------------
		boolean exists1 = exists(reqiredSourceFile.getName());
		boolean exists2 = exists(requiredDecodedMp3File.getName());
		
		//removedLog.w(this.getClass().getName(),"checking for existance of file \"" + reqiredSourceFile + "\": " + Boolean.toString(exists1));
		//removedLog.w(this.getClass().getName(),"checking for existance of file \"" + requiredDecodedMp3File + "\": " + Boolean.toString(exists2));
		
		boolean result = exists1 && exists2;
		//removedLog.w(this.getClass().getName(),"All required files exist?" + Boolean.toString(result));
		
		/*TODO: other requirements. Req. 1 is not really enough, two different files could have same names, app should behave correctly even then. 
		 * This situation could easily arise if user ripped several CD's in order to ABX some codec, but didn't bother to name individual ripped files. 
		 * Instead, they were put into different directories, which were named correctly. So after ripping two cd's, user has something like this:
		 * ╔�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?╦�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?╗
		 * ║ The Cosmos Rocks ║ Chinese Democracy ║
		 * ╠�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?╬�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?╣
		 * ║ Track 1.wav      ║ Track 1.wav       ║
		 * ║ Track 2.wav      ║ Track 2.wav       ║
		 * ║ Track 3.wav      ║ Track 3.wav       ║
		 * ║ Track 4.wav ...  ║ Track 4.wav ...   ║
		 * ╚�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?╩�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?�?╝
		 * First, user starts some ABXY test on "The Cosmos Rocks" "Track 1.wav". Since there are no files named "Track 1.wav.encoded" in the working directory,
		 * encoding is started using current settings. After user finishes testing that track, she proceeds with testing "Track 1.wav" from the "Chinese
		 * Democracy". Now, the app sees there already is a track named "Track 1.wav.encoded" in the folder, and prompts user if she wants to re-encode, or use the
		 * existing files, which is clearly wrong, as those already encoded files resulted from unrelated file that happens to have the same name as this one.
		 * 
		 * After writing this comment, I see that the whole mechanism of tracking what is or is not encoded should maybe be revisited or implemented completely
		 * differently than by this simple naming scheme, some day one day. 
		 */
				
		return result;
	}
	
	/**
	 * Returns true if file exists in app's working directory.
	 * 
	 * @param filename
	 *            Name of the file
	 * @return True if filename exists in app's working directory.
	 * @throws StorageUnaccessibleException
	 */
	private boolean exists(String filename) throws StorageUnaccessibleException {
		File file = new File(getStorageDirectoryPath() + "/" + filename);
		return (file.exists() && file.isFile());
	}
	
	
	//TODO: Javadoc is outdated. The method was split to multiple methods.
	/**
	 * Creates a PCM (.raw) file in application data folder, given FLAC, WAV or
	 * PCM source file selected by user <p>
	 * Cases: <p>
	 * 
	 * 1) source is PCM: Assumes the PCM is the most common one (16 bit that
	 * corresponds to red book audio) and copies it to the application folder <p>
	 * 
	 * 2) source is WAV: Extracts PCM from WAV and saves it to the application
	 * data directory. In most common case this means just the WAV header is
	 * removed, as the rest of data should correspond to red book audio.
	 * However, there are various WAV formats, and in the future all (or several
	 * most common) should be covered. <p>
	 * 
	 * 3) source is FLAC: <s> This should actually not be part of this method. In
	 * case of FLAC, method should return that is the case, so caller could
	 * apply FLAC decoding. </s> It actually should be here, as it will then
	 * simply call <b>statit decodeFlacToPcm</b> of FlacDecoder object, similar
	 * to how it is achieved in mp3 encoder. <s>Since FLAC decoding is fast
	 * compared to mp3 encoding, there is no need to do this in a service,
	 * asynctask will do.</s>
	 * 
	 * @param originalFile
	 * @throws StorageUnaccessibleException
	 */
	public void toRaw(File originalFile, OnToRawProgressUpdateListener listener) throws StorageUnaccessibleException{
				
		/*if raw 16 bit PCM
		 *  - just copy to the work folder
		 */
		if(originalFile.getName().endsWith(".raw")){
			
			//TODO: use getSourcePCM
			File originalPCM = new File(getStorageDirectoryPath() + "/" + originalFile.getName());		
			try {
//				copyFromFileIntoFile(originalFile, originalPCM);
				copyWithProgress(originalFile, originalPCM, listener);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
	
	public void toRaw(File originalFile, String copyDestinationPath, OnToRawProgressUpdateListener listener) throws StorageUnaccessibleException{
		
		File dest = new File(copyDestinationPath);
		try{
			copyWithProgress(originalFile, dest, listener);
		} catch (IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
		
	private static void copyFromFileIntoFile(File originalFile, File originalPCM) throws IOException {
		FileChannel source = null;
		FileChannel destination = null;
		try{
			source = new FileInputStream(originalFile).getChannel();
			destination = new FileOutputStream(originalPCM).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally {
			if(source != null){
				source.close();
			}
			if(destination!= null){
				destination.close();
			}
		}
	}
	
	public File copyFromStream(InputStream inputStream, String name) throws IOException, StorageUnaccessibleException{
		
		File originalPCM = new File(getStorageDirectoryPath() + "/" + name);
		return copyFromStream(inputStream, originalPCM);	
	}
	
	
	private static File copyFromStream(InputStream inputStream, File originalPCM) throws IOException{
		
		long bytesCopied = 0;
		
		byte[] buffer = new byte[8192];
		
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		
		File returnFile = null;
		
		try{
			in = new BufferedInputStream(inputStream);
			out = new BufferedOutputStream(new FileOutputStream(originalPCM));
			
			int bytesRead = 0;
			int iterationCounter = 0;
			float progress = 0;
			
			while(  (bytesRead = in.read(buffer, 0, buffer.length)) != -1   ){
				out.write(buffer, 0, bytesRead);
				bytesCopied += bytesRead;	
				iterationCounter ++;
				//removedLog.w(TAG, "copying from stream, bytesCopied:" + bytesCopied);
			}
			out.flush();
			//removedLog.w(TAG, "finished copying from stream, bytesCopied:" + bytesCopied);
			returnFile = originalPCM;
			
		} finally {
			if(in != null){
				in.close();
			}
			if(out != null){
				out.close();
			}
		}
		
		return returnFile;
		
		
	}
	
	private static void copyWithProgress(File originalFile, File originalPCM, OnToRawProgressUpdateListener listener) throws IOException{
		
		long lengthInBytes = originalFile.length();
		long bytesCopied = 0;
		
		byte[] buffer = new byte[8192];
		
		BufferedInputStream in = null;
		BufferedOutputStream out = null;
		
		try {
			in = new BufferedInputStream(new FileInputStream(originalFile));
			out = new BufferedOutputStream(new FileOutputStream(originalPCM));
			
			int bytesRead = 0;
			int iterationCounter = 0;
			float progress = 0;
			
			while(  (bytesRead = in.read(buffer, 0, buffer.length)) != -1   ){
				out.write(buffer, 0, bytesRead);
				bytesCopied += bytesRead;	
				iterationCounter ++;
				
				//how often should progress be updated
				if(iterationCounter % 100 == 0){
					progress = ((float) bytesCopied)/lengthInBytes;
					if(listener != null){
						listener.updateProgress(progress);
					}
					//removedLog.w(TAG, "copy progress listener invoked: " + progress);
				}
				//removedLog.w(TAG, "copy progress: " + progress);
			}
			out.flush();
			progress = ((float) bytesCopied)/lengthInBytes;
			if(listener != null){
				listener.updateProgress(progress);
			}
			//removedLog.w(TAG, "copy progress (finished): " + progress);
			//removedLog.w(TAG, "copied " + bytesCopied + "/" + lengthInBytes);
			
		} finally {
			if(in != null){
				in.close();
			}
			if(out != null){
				out.close();
			}
		}
	}
	
	/**Returns path where raw pcm should be saved
	 * 
	 * While most of the path is the same for each type of input lossless file (app's dedicated data directory),
	 * rules concerning extensions are different depending on input type as follows:
	 * 
	 * if input = "someFile.raw"
	 * then: output = "appDataDirectory/someFile.raw"
	 * 
	 * if input = "someFile.wav"
	 * then: output = "appDataDirectory/someFile.wav.raw
	 * 
	 * if input = "someFile.flac"
	 * then: output = "appDataDirectory/someFile.flac.raw
	 * 
	 * By using this naming convention it is easy to see how each file was obtained during "encoding" processes
	 * (in cases of raw and wav files there is no actual encoding, just plain copying for the former and extraction of data for the latter)
	 * 
	 * 
	 * @param file - original file that is submitted to the testing, i.e. an lossless song that is to be encoded/decoded to 128kbit cbr mp3 and then blind tested
	 * @return path (including name and extensions) where processed file should be saved
	 * @throws StorageUnaccessibleException 
	 */
	public String getSourcePCMPath(File file) throws StorageUnaccessibleException{

		if(file.getName().endsWith(".raw")){

			return new File(getStorageDirectoryPath() + "/" + file.getName()).getAbsolutePath();
		}
		else if(file.getName().endsWith(".wav")){

			return new File(getStorageDirectoryPath() + "/" + file.getName() + ".raw").getAbsolutePath();
		}
		else if(file.getName().endsWith(".flac")){

			return new File(getStorageDirectoryPath() + "/" + file.getName() + ".raw").getAbsolutePath();
		}
		else{
			return "Ne vracaj nista nego baci exception";
		}
	}
	
	/**
	 * Returns sufix to append to encoded mp3 file, which holds information about the way it
	 * was encoded (CBR/VBR, bitrate, quality etc.) Depends on currently selected settings:
	 * explain how.
	 * @return
	 */
	private String getMp3Suffix(){
		
		SettingsHelper helper = new SettingsHelper(context);
		
		int requiredBitrate =  helper.getBitrate(SettingsHelper.RETURN_DEFAULT_IF_DISABLED);
		int requiredQuality =  helper.getQuality(SettingsHelper.RETURN_DEFAULT_IF_DISABLED);
		
		/*if VBR preset mode, apply VBR preset naming format
		 * i.e. someFile.raw.VBR_V0.mp3 */
		if(requiredBitrate>320){
			return ".VBR_" + mp3EncoderDecoder.getStr(requiredBitrate) + ".mp3";
		}
		/*if CBR mode, apply CBR naming format
		 * i.e. someFile.raw.CBR_320.Q2.mp3 */
		else{
			return ".CBR_" + requiredBitrate + ".Q" + requiredQuality + ".mp3";
		}
		
	}
	
	/**
	 * Returns the path to the mp3 file where the result of encoding the <b>sourceFile</b> should be saved.<p>
	 * 
	 * For example, if a file <i>original.flac</i> is being encoded to a 320kbps cbr mp3, the result of encoding is
	 * saved to a location that goes something like {@code "...\working directory\original.flac.cbr.320.mp3"}. <p>
	 * 
	 * Actual path depends on currently decided naming format, that is likely to change, and upon the location of working folder.
	 * 
	 * @param sourceFile File being encoded
	 * @return Path to mp3 file where data that is the result of encoding {@code file} is saved.
	 * @throws StorageUnaccessibleException
	 */
	public String getMp3Path(File sourceFile) throws StorageUnaccessibleException{
				
		
		if(!makeMp3InnaccessableToUser){
			return getSourcePCMPath(sourceFile) + getMp3Suffix();
		}
		else{
			//generate internal directory if does not exist already
			File internalDirectory = new File(getDefaultDirectoryPath());
			if( ! internalDirectory.exists()){
				internalDirectory.mkdir();
			}
			
//			return getDefaultDirectoryPath() + "/" + sourceFile.getName() + getMp3Suffix();
			return getDefaultDirectoryPath() + "/" + new File(getSourcePCMPath(sourceFile)).getName() + getMp3Suffix();
		}
		
	}
	
	/**
	 * Returns path to the {@code .raw} file where data decoded from  <b>file</b> should be saved.
	 * @param file File being decoded.
	 * @return Path to the {@code .raw} file where data decoded from  <b>file</b> should be saved
	 * @throws StorageUnaccessibleException
	 */
	public String getMp3DecodedPath(File file) throws StorageUnaccessibleException{
								
		//no matter where mp3 is saved, it is decoded to the currently set working directory
		return getStorageDirectoryPath() + "/" + new File(getSourcePCMPath(file)).getName() + getMp3Suffix() + ".raw";
		
//		return getMp3Path(file) + ".raw";
	}
	
	
	/**
	 * Deletes generated mp3 file if required by current policy.
	 * @param mp3FilePath path to mp3 file to be deleted if necessary
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteMp3IfRequired(String mp3FilePath){
		
		if(makeMp3InnaccessableToUser){
		
			/* simple check if it is really the generated mp3 file getting deleted, I don't
			 * want to enable deleting other files (public ones) by mistake
			 */
			File generatedMp3File = new File(mp3FilePath);
			if(mp3FilePath.endsWith(getMp3Suffix())){
				return generatedMp3File.delete();
			}
		}
		
		return false;
	}
	
	/**
	 * Returns path to te {@code .raw} file where data decoded from <b>file</b> should be saved, where <b>file</b> is the first of the two files being compared in self prepared mode.
	 * @param file First of the two files being compared in self prepared mode.
	 * @return Path to te {@code .raw} file where data decoded from <b>file</b> should be saved, where <b>file</b> is the first of the two files being compared in self prepared mode.
	 * @throws StorageUnaccessibleException
	 */
	public String getSelfPreparedPath1(File file) throws StorageUnaccessibleException{
		
		return new File(getStorageDirectoryPath() + "/" + file.getName() + ".selfPrepared1").getAbsolutePath();
	}
	
	/**
	 * Returns path to te {@code .raw} file where data decoded from <b>file</b> should be saved, where <b>file</b> is the second of the two files being compared in self prepared mode.
	 * @param file Second of the two files being compared in self prepared mode.
	 * @return Path to te {@code .raw} file where data decoded from <b>file</b> should be saved, where <b>file</b> is the second of the two files being compared in self prepared mode.
	 * @throws StorageUnaccessibleException
	 */
	public String getSelfPreparedPath2(File file) throws StorageUnaccessibleException{
		
		return new File(getStorageDirectoryPath() + "/" + file.getName() + ".selfPrepared2").getAbsolutePath();
	}
	
	/**
	 * Object that wants to receive toRaw progress should implement this interface.
	 */
	public interface OnToRawProgressUpdateListener{
		void updateProgress(float progress);
	}
	
	public static class StorageUnaccessibleException extends Exception{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public StorageUnaccessibleException(){
			super("sd card not mounted Storage uncaccessible, several possible reasons, most likely: " +
					"1)unmounted sd card while working directory set to sd card; " +
					"2) OS version is KitKat, which has weird rules about accessing the sd card");
		}
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
