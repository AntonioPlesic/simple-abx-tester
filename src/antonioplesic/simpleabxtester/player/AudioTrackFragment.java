package antonioplesic.simpleabxtester.player;



import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import antonioplesic.simpleabxtester.audiotrackplayer.AudioTrkPlyr;
import antonioplesic.simpleabxtester.audiotrackplayer.SelectablePlayer;

public class AudioTrackFragment extends Fragment{
	
	public static final int PLAYER1 = 1;
	public static final int PLAYER2 = 2;
	
//	MediaPlayer player1;
//	MediaPlayer player2;
	
	AudioTrkPlyr player;
	
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
		//TODO: not used, remove
		//void sendMessage(Message msg);
		void onCompletion(AudioTrkPlyr player);
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
				//removedLog.w(this.getClass().getName(),"Activity " + myCallbacks.toString() + " attached");
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
//		myCallbacks = null; //we want to disable sending info to nonexistant activity (the destroyed one)
		//removedLog.i(this.getClass().getName(), "fragment detached from activity");
	}
	
	public void stop(){
		setRetainInstance(false);
		player.stop();
	}
	
	public void loadAudio(String path1,String path2){
		
		player = new SelectablePlayer();
		//removedLog.w("PlayerFragment", "loadan audio file");
		
		try {
			player.setDataSources(path1, path2);
			player.prepare();
			player.setOnCompletionListener(new AudioTrkPlyr.OnCompletionListener() {
				
				@Override
				public void onCompletion(AudioTrkPlyr player) {
					
					//removedLog.i(this.getClass().getName(),"PlayerFragment received onCompletion (of playback) callback");
					isPlaying = false;
					
					//Notify playerActivity of completion
					myCallbacks.onCompletion(player);
					
				}
			});
			
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
		}
		
	}
	
	public void play(){
		player.play();
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
		return player.isPlaying() ;  //pogledati gore zake ovo "nije dobro"
	}
	
	public boolean isSetToPlay(){
		return isPlaying;
	}
//##############################################
	
	public int getDuration(){
		//kod normalnog koristenja trajanja svih traka su ista, dovoljno vratiti jedno
		return player.getDuration();
	}
	
	public int getCurrentPosition(){
		//pozicije oba playera iste, dovoljno vratiti jednu
		return player.getCurrentPosition();
	}
	
	public void seekTo(int milis){
		player.seekTo(milis);
	}
	
	public void setLooping(boolean arg0){
		player.setLooping(arg0);
	}
	
	public void setLoopingStart(int milis){
		player.setLoopingSectionStart(milis);
	}
	
	public void setLoopingEnd(int milis){
		player.setLoopingSectionEnd(milis);
	}
	
	public boolean isLooping(){
		return player.isLooping();
	}
	
	public void solo1(){
		
//		//ako vec je mutan, treba simulirati delay
//		if(isPlayer1Muted){
//			
//		}
//		
//		player2.setVolume(0f, 0f);
//		player1.setVolume(1f, 1f);
//		
//		isPlayer1Muted = false;
//		isPlayer2Muted = true;
//		
		
		player.selectTrack(1);
		
		Aselected = true;
		Bselected = false;
		Xselected = false;
		Yselected = false;
	}
	
	public void solo2(){
		
//		//ako vec je mutan, treba simulirati delay
//		if(isPlayer2Muted){
//			
//		}
//		
//		player1.setVolume(0f, 0f);
//		player2.setVolume(1f,1f);
//		
//		isPlayer2Muted = false;
//		isPlayer1Muted = true;
//		
		player.selectTrack(2);
		
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

}
