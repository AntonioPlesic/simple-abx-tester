package antonioplesic.simpleabxtester.player;

import java.io.IOException;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

/**
 * @deprecated Naive approach, using MediaPlayer class for playback, unworkable
 * due to many reasons. Not deleted because of sentimental value.
 */
@Deprecated
public class MultiplePlayerFragment extends Fragment {
	
	public static final int PLAYER1 = 1;
	public static final int PLAYER2 = 2;
	
	MediaPlayer player1;
	MediaPlayer player2;
	
	boolean isPlayer1Muted;
	boolean isPlayer2Muted;
	
	private boolean Aselected;
	private boolean Bselected;
	private boolean Xselected;
	private boolean Yselected;
	
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
	
	private PlayerFragmentCallbacks myCallbacks;
	
	public interface PlayerFragmentCallbacks{
		void sendMessage(Message msg);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState); //OBAVEZNO
		setRetainInstance(true);
		
	}
	
	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		
		if(! (activity instanceof PlayerFragmentCallbacks)){
			throw new IllegalStateException("Activity must implement the PlayerFragmentCallbacks interface.");
		}
		
		//Referenca na roditeljski activity preko koje javljam rezultate
				myCallbacks = (PlayerFragmentCallbacks) activity;
//				attached.set(true); // moj dodatak
				//removedLog.w("spojen activity ",myCallbacks.toString());
		
		
	}
	
	public void loadAudio(String path1,String path2){
		
		//TODO: obrati paznju na ovo
//		if(player != null){
//			player.release(); //releasaj stari player
//			player = new MediaPlayer(); //stvori novi
//		}
		
		player1 = new MediaPlayer();
		//removedLog.w("PlayerFragment", "loadan audio file");
		
		player2 = new MediaPlayer();
		
		try {
			player1.setDataSource(path1);
			player1.prepare();
			
			player2.setDataSource(path2);
			player2.prepare();
			
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
		player1.start();
		player2.start();
		isPlaying = true;
	}
	
	public void pause(){
		player1.pause();
		player2.pause();
		isPlaying = false;
	}

//##############################################
//### KAK NAZVATI OVO DVOJE A DA IMA SMISLA ####
//##############################################
	public boolean isPlaying(){				  	
		return (player1.isPlaying() || player2.isPlaying()) ;  //pogledati gore zake ovo "nije dobro"
	}
	
	public boolean isSetToPlay(){
		return isPlaying;
	}
//##############################################
	
	public int getDuration(){
		//kod normalnog koristenja trajanja svih traka su ista, dovoljno vratiti jedno
		return player1.getDuration();
	}
	
	public int getCurrentPosition(){
		//pozicije oba playera iste, dovoljno vratiti jednu
		return player1.getCurrentPosition();
	}
	
	public void seekTo(int milis){
		player1.seekTo(milis);
		player2.seekTo(milis);
	}
	
	public void setLooping(boolean arg0){
		player1.setLooping(arg0);
		player2.setLooping(arg0);
	}
	
	public void solo1(){
		
		//ako vec je mutan, treba simulirati delay
		if(isPlayer1Muted){
			
		}
		
		player2.setVolume(0f, 0f);
		player1.setVolume(1f, 1f);
		
		isPlayer1Muted = false;
		isPlayer2Muted = true;
		
		Aselected = true;
		Bselected = false;
		Xselected = false;
		Yselected = false;
	}
	
	public void solo2(){
		
		//ako vec je mutan, treba simulirati delay
		if(isPlayer2Muted){
			
		}
		
		player1.setVolume(0f, 0f);
		player2.setVolume(1f,1f);
		
		isPlayer2Muted = false;
		isPlayer1Muted = true;
		
		Bselected = true;
		Aselected = false;
		Xselected = false;
		Yselected = false;
	}
	
	public void soloX(){
		
	}
	
	public void soloY(){
		
	}

	public boolean isAselected() {
		return Aselected;
	}

	public boolean isBselected() {
		return Bselected;
	}

	public boolean isXselected() {
		return Xselected;
	}

	public boolean isYselected() {
		return Yselected;
	}
	
	

	
	
	//jel bolje pitati za progress preko javne metode ili slati,
	//tj. ko treba voditi racuna o tome, mainActicity ili fragment?
	//u ovom slucaju vjerojatno je bolje da to radi activity, tj. view

	

	
	

}
