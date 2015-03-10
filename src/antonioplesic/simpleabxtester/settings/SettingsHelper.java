package antonioplesic.simpleabxtester.settings;

import antonioplesic.simpleabxtester.DataManager;
import antonioplesic.simpleabxtester.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Advanceable;

public class SettingsHelper {
	
	public static final int RETURN_DEFAULT_IF_DISABLED = 1;
	public static final int RETURN_SAVED_IF_DISABLED = 2;
	
	Context context;
	SharedPreferences sharedPref;
	
	public SettingsHelper(Context context){
		this.context = context;
		sharedPref =  PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public int getBitrate(int mode){
		return Integer.parseInt(sharedPref.getString("encoderBitrate", ""));
	}

	public int getQuality(int mode){
		switch (mode){
		case RETURN_SAVED_IF_DISABLED:
			return Integer.parseInt(sharedPref.getString("encoderQuality", ""));
		case RETURN_DEFAULT_IF_DISABLED:
			boolean enabled = sharedPref.getBoolean("advancedEncoder",false);
			if(enabled){
				return Integer.parseInt(sharedPref.getString("encoderQuality", ""));
			}
			else{
				return Integer.parseInt(context.getString(R.string.encoder_quality_default));
			}
		default:
			return -1; //unreachable actually
		}
	}
	
	/**
	 * Returns true if "use custom directory" setting is checked, so app uses that instead of internal
	 * working directory.
	 * @return True if "use custom directory" setting is checked.
	 */
	public boolean usesCustomDirectory(){
		
		return sharedPref.getBoolean("usesCustomDirectory", false);
	}
	
	/**Returns custom directory path that the user selected to be used as the data directory for the app.<p>
	 * 
	 * Note however, that <b>this is not the actual directory that will be used</b> as data directory,
	 * as another directory that will serve as data directory will be nested inside the user 
	 * selected one, in order to protect user from polluting her storage space.<br>
	 * To get the actual directory used for app's data storage, call <b>getActualCustomDirectory()</b>.
	 * 
	 * @return Custom directory path that the user selected to be used as the data directory for the app.
	 */
	public String getCustomDirectory(){
		return sharedPref.getString("customDirectory",Environment.getExternalStorageDirectory().getAbsolutePath());
	}
	
	/**
	 * Sets directory to use as custom working directory, that is public (accessible via file explorers).
	 * @param directory Directory to use as custom working directory.
	 */
	public void setCustomDirectory(String directory){
		sharedPref.edit().putString("customDirectory", directory).commit();
	}
	
	public String getActualCustomDirectory(){
		return getCustomDirectory() + "/" + DataManager.mandatoryDataDirectoryName();
	}
	
	/**
	 * Returns the number of samples (stereo samples) synchronization routine should take into account
	 * for its calculations.
	 * @return the number of samples (stereo samples) synchronization routine should take into account
	 * for its calculations.
	 */
	public int getSyncWindowLength(){
		return sharedPref.getInt("syncWindowLength", 5000);
	}
}
