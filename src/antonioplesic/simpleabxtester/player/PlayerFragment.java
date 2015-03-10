package antonioplesic.simpleabxtester.player;

import java.io.IOException;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class PlayerFragment extends Fragment {
		
	MediaPlayer player;
	boolean isPlaying = false; /*Zasto koristim ovo, 
	 * umjesto da jednostavno pozivam mediaPlayer.isPlaying()? 
	 * Odgovor:
	 * Playback can be paused and stopped, and the current playback position can be adjusted.
	 * Playback can be paused via pause(). When the call to pause() returns, the MediaPlayer
	 * object enters the Paused state. 
	 * Note that the transition from the Started state to the Paused state and vice versa 
	 * happens asynchronously in the player engine. It may take some time before the state 
	 * is updated in calls to isPlaying(), and it can be a number of seconds in the case of 
	 * streamed content.
	 */
	
	//TODO: not used, delete
//	private PlayerFragmentCallbacks myCallbacks;
//	
//	public interface PlayerFragmentCallbacks{
//		void sendMessage(Message msg);
//	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState); //OBAVEZNO
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
	}
	
	@Override
	public void onDetach() {
		// TODO Auto-generated method stub
		super.onDetach();
		
		
	}
	
	//TODO: not used, delete
//	@Override
//	public void onAttach(Activity activity) {
//		// TODO Auto-generated method stub
//		super.onAttach(activity);
//		
//		if(! (activity instanceof PlayerFragmentCallbacks)){
//			throw new IllegalStateException("Activity must implement the PlayerFragmentCallbacks interface.");
//		}
//		
//		//Referenca na roditeljski activity preko koje javljam rezultate
//				myCallbacks = (PlayerFragmentCallbacks) activity;
////				attached.set(true); // moj dodatak
//				//removedLog.w("spojen activity ",myCallbacks.toString());
//		
//		
//	}
	
	public void loadAudio(String path){
				
		player = new MediaPlayer();
		//removedLog.w("PlayerFragment", "loadan audio file");
		
		try {
			player.setDataSource(path);
			player.prepare();
			//player.ge
			//TODO: javi da je loadan, tj. enablaj play/pause/seek/whatever
			
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void play(){
		player.start();
		isPlaying = true;
	}
	
	public void pause(){
		player.pause();
		isPlaying = false;
	}

//##############################################
//### KAK NAZVATI OVO DVOJE A DA IMA SMISLA ####
//##############################################
	public boolean isPlaying(){				  	
		return player.isPlaying();  //pogledati gore zake ovo "nije dobro"
	}
	
	public boolean isSetToPlay(){
		return isPlaying;
	}
//##############################################
	
	public int getDuration(){
		return player.getDuration();
	}
	
	public int getCurrentPosition(){
		return player.getCurrentPosition();
	}
	
	public void seekTo(int milis){
		player.seekTo(milis);
	}
	
	public void setLooping(boolean arg0){
		player.setLooping(arg0);
	}

}
