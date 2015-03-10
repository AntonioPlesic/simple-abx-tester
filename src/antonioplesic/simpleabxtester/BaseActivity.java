package antonioplesic.simpleabxtester;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import antonioplesic.simpleabxtester.R;
import antonioplesic.simpleabxtester.encoder.FlacInfo;
import antonioplesic.simpleabxtester.encoder.Mp3Info;
import antonioplesic.simpleabxtester.encoder.flacDecoder;
import antonioplesic.simpleabxtester.encoder.mp3EncoderDecoder;
import antonioplesic.simpleabxtester.settings.SettingsActivity;
import antonioplesic.simpleabxtester.wavextractor.WavInfoBetter;
import antonioplesic.simpleabxtester.wavextractor.WavParserBetter;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.Toast;

/**
 * Most activities that have generic menu that consists of items like Settings, 
 * Help, About should extend this activity in order to provide common menu
 * functionality.
 */
public abstract class BaseActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*"hack" to force action overflow regardless of physical menu key
		 * (i consider it a hack since it is not part of the api)
		 * Sorce: http://stackoverflow.com/a/11438245 
		 */
		try {
	        ViewConfiguration config = ViewConfiguration.get(this);
	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception ex) {
	        // Ignore
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		/// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.common_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		switch (id) {
		case R.id.action_settings: 
			launchSettingsActivity();
			return true;
//		case R.id.start_menu_About:
//			launchAboutActivity();
//			return true;
		case R.id.start_menu_Help:
			launchHelpActivity();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void launchSettingsActivity() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}
	
	public void launchHelpActivity() {
		Intent intent = new Intent(this, HelpActivity.class);
		startActivity(intent);
	}
	
	public void launchAboutActivity() {
		//should this be the same activity as Help, just started with different first page? Probably.
		Intent intent = new Intent(this, HelpActivity.class);
		//put some extra to differentiate Help, About and other similiar (webView based) activities
		startActivity(intent);
	}
	
	/**
	 * Returns whether this particular Wav file is supported by this application.
	 * @param fajl File in question.
	 * @return true if supported, false if not.
	 */
	public boolean checkWav(File fajl) {
		WavInfoBetter info = null;
		
		try {
			WavParserBetter vawParser = new WavParserBetter(fajl);
			info =  vawParser.parse();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(	info==null ){
			Toast.makeText(this, "Unable to open file", Toast.LENGTH_LONG).show();
			return false;
		}
		if( info.getFormat()!=WavInfoBetter.WAVE_FORMAT_PCM
		 || info.getnChannels() != 2
		 || info.getBitsPerSample() != 16
		 || info.getSampleRate() != 44100)
		{	
			StringBuilder sb = new StringBuilder("Unsupported WAV format: ");
			if(info.getFormat() != WavInfoBetter.WAVE_FORMAT_PCM){
				sb.append("\nFormat is ").append(WavInfoBetter.getFormatString(info.getFormat()));
				sb.append("; only ").append(WavInfoBetter.getFormatString(WavInfoBetter.WAVE_FORMAT_PCM));
				sb.append(" supported.");
			}
			if(info.getnChannels() != 2){
				sb.append("\nNo. of channels: ").append(info.getnChannels()).append(". Only stereo supported.");
			}
			if(info.getBitsPerSample()!=16){
				sb.append("\nBits per sample: ").append(info.getBitsPerSample()).append(". Only 16 bps supported.");
			}
			if(info.getSampleRate() != 44100){
				sb.append("\nSample rate: ").append(info.getSampleRate()).append(". Only 44100 supported.");
			}
			
			Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
			
			return false;
			
		}
		
		return true;
	}
	
	/**
	 * Returns whether this particular Flac file is supported by this application.
	 * @param fajl File in question.
	 * @return true if supported, false if not.
	 */
	public boolean checkFlac(File fajl){
		
		FlacInfo info = new flacDecoder().getMetadata(fajl.getAbsolutePath());
		
		if(info.getChannels()!=2 || info.getBps()!= 16 || info.getSampleRate() != 44100){
		
			StringBuilder sb = new StringBuilder("Unsupported FLAC format:");
			
			if(info.getChannels()!=2){
				sb.append("\nNo. of channels: ").append(info.getChannels()).append(". Only stereo supported.");
			}
			if(info.getSampleRate()!=44100){
				sb.append("\nSample rate: ").append(info.getSampleRate()).append(". Only 44100 supported.");
			}
			if(info.getBps()!=16){
				sb.append("\nBits per sample: ").append(info.getBps()).append(". Only 16 bps supported.");
			}
			
			Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns whether this particular Mp3 file is supported by this application.
	 * @param fajl File in question.
	 * @return true if supported, false if not.
	 */
	public boolean checkMp3(File fajl){
		
		Mp3Info info = new mp3EncoderDecoder().getInfo(fajl.getAbsolutePath());
		
		if(info.getSampleRate() != 44100){
			Toast.makeText(this, "Unsoported sample rate: " + info.getSampleRate() + ".",Toast.LENGTH_LONG).show();
			return false;
		}
		
		return true;
	}

}
