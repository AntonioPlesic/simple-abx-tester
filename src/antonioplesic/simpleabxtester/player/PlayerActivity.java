package antonioplesic.simpleabxtester.player;

import java.text.DecimalFormat;

import antonioplesic.simpleabxtester.BaseActivity;
import antonioplesic.simpleabxtester.R;
import antonioplesic.simpleabxtester.audiotrackplayer.AudioTrkPlyr;
import antonioplesic.simpleabxtester.lifted.RangeSeekBar;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class PlayerActivity extends BaseActivity implements AudioTrackFragment.PlayerFragmentCallbacks ,
		OnSeekBarChangeListener, IButtonPress {

	private static final int PICKFILE_RESULT_CODE = 1;
	
	public static final class SelectorType{
		public static final int DRAGGER_SELECTOR = 234523;
		public static final int ABX_SELECTOR = 524214;
	}

	private Handler GUIHandler;
//	private GUI_handler GUIHandler;

	//TODO:buggy, should switch to service
	private AudioTrackFragment playerFragment; //non GUI fragment used for persisting player instance
	private TrackSelectorFragment selectorFragment;
	
	private int selectorType;
	
	Arbiter arbiter;

	SeekBar seekBar;
	RangeSeekBar<Integer> rangeSeekBar;
	ImageButton btnPlayPause;
	CheckBox cbxLoop;
	
	int rangeStart = 0;
	
	TextView resultsTextView;
	
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//removedLog.w(this.getClass().getName(),"onCreate");
		
		setContentView(R.layout.activity_player_main);
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		getActionBar().setHomeButtonEnabled(true);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		//TODO: place in strings, don't hardcode
		//getActionBar().setTitle("");
		
		btnPlayPause = (ImageButton) findViewById(R.id.btnPlayPause);
		btnPlayPause.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				playPause();

			}
		});

		resultsTextView = (TextView) findViewById(R.id.dragger_textView_Y);

		
		GUIHandler = new Handler();
//		TODO: not used, delete
//		GUIHandler = new GUI_handler() {
//			public void handleMessage(Message msg) {
//				
//				//Cheking if this is ever used, don't think so, should remove if not used
//				//removedLog.w(this.getClass().getName(),"HANDLER USED AFTER ALL, DO NOT REMOVE *?=)=?*?=)=?*?=)=?*?=)=?*=)=?");
//				
//				switch (msg.what) {
//
//				case GUI_handler.POSITION:
//					seekBar.setProgress(msg.arg1);
//					break;
//
//				case GUI_handler.DURATION:
//					seekBar.setMax(msg.arg1);
//					break;
//
//				default:
//					break;
//				}
//
//			};
//		};
	
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
		
		FragmentManager fm = getFragmentManager();
		
		//FIXME: sometimes returns null even though fragment is retained and exists
		//possible fix
		//1) not working
//		fm.executePendingTransactions();
		playerFragment = (AudioTrackFragment) fm.findFragmentByTag("player");
		

		// If the Fragment is non-null, then it is currently being
		// retained across a configuration change.
		if (playerFragment == null) {

			//removedLog.i(this.getClass().getName(),"STAVARAM NOVI FRAGMENT");
			// //kad se radi s samo jednim playerom

			// playerFragment = new PlayerFragment();
			// fm.beginTransaction().add(playerFragment, "player").commit();
			// playerFragment.loadAudio("/sdcard/music/Stay Stay Stay.flac");

			// kad se radi s multiPlayerom

			//playerFragment = new MultiplePlayerFragment();
			playerFragment = new AudioTrackFragment();
			fm.beginTransaction().add(playerFragment, "player").commit();
			
			//related to above FIXME:
			//1) not working
//			fm.executePendingTransactions();
						
			// TODO: krsenje nekog principa
			// multiplePlayerFragment nema metodu za loadanje samo jednog
			// izvora,
			// no zato jer naivno nasljeduje playerFragment misli da ima;
			// playerFragment.loadAudio(path);
			// Ovo castanje je grozno
						
			if( getIntent().getExtras() != null){
				
				String fajl1 = getIntent().getExtras().getString("fajl1", "");
				String fajl2 = getIntent().getExtras().getString("fajl2", "");
				
				if(! fajl1.equals("")){
					//removedLog.w(this.getClass().getName(),"loadam " + fajl1);
					//removedLog.w(this.getClass().getName(),"loadam " + fajl2);
					playerFragment.loadAudio(fajl1, fajl2);
				}	
			}
			else{
				//removedLog.w(this.getClass().getName(),"nisu zadani fajlovi, loadam hardkodirane");
				playerFragment.loadAudio("/sdcard/AAA input fajlovi/Radetzkijev marš.raw", "/sdcard/AAA input fajlovi/Cowboy Take Me Away.raw");
			}
		}else{
			//removedLog.i(this.getClass().getName(),"NIJE STVOREN NOVI FRAGMENT");
		}
		
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
//||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||	

		
		//=====================================================================
		// Initialize or get the "retained" DraggerFragment
		//=====================================================================
		
		if (savedInstanceState != null) {
			//TODO: does nothing, no assignment is made? Actually handled in onCreate and/or restoreInstanceState?
			getFragmentManager().getFragment(savedInstanceState,"selectorFragment");
		} else {
			//removedLog.w("ovo se dogodi samo prilikom paljenja appa", "samo jednom");
			if(getIntent().getExtras()!= null){
				selectorType = getIntent().getExtras().getInt("selectorType");
			}
			
			switch (selectorType) {
			
			case SelectorType.DRAGGER_SELECTOR:
				arbiter = new Arbiter(Arbiter.TYPE_NORMAL);
				selectorFragment = new DraggerFragment();
				break;
				
			case SelectorType.ABX_SELECTOR:
//				arbiter = new Arbiter(Arbiter.TYPE_SPECIAL);
				arbiter = new Arbiter(Arbiter.TYPE_ONE_REFERENCE_TWO_CHOICES);
				selectorFragment = new ABXselector();
				break;
				
			default:
				arbiter = new Arbiter(Arbiter.TYPE_NORMAL);
				selectorFragment = new DraggerFragment();
				break;
			}
			
			FragmentTransaction transaction = fm.beginTransaction();
			transaction.add(R.id.frameLayout, selectorFragment);
			transaction.commit();

//			selectorFragment.initialize();  //FIXME: why is this commented? where is fragment initialized if not here?
		}
		
		cbxLoop = (CheckBox) findViewById(R.id.cbxLoop);
		cbxLoop.setChecked(playerFragment.isLooping());
		cbxLoop.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				playerFragment.setLooping(arg1);

			}
		});
		
		seekBar = (SeekBar) findViewById(R.id.seekBar);
		seekBar.setOnSeekBarChangeListener(this);
		
		rangeSeekBar = (RangeSeekBar<Integer>) findViewById(R.id.rangeSeekBar);
		rangeSeekBar.setRangeValues(0, playerFragment.getDuration());
		rangeSeekBar.setNotifyWhileDragging(true);
		rangeSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {

			@Override
			public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar,
					Integer minValue, Integer maxValue) {
				
				/*RangeSeekBar.OnRangeSeekBarChangeListener doesn't provide a way to tell
				 * whether start or end of range was changed. Therefore, rangeStart stores
				 * the last start position, which is then compared with new start position
				 * (minValue). 
				 */
				if(rangeStart == minValue){
					//removedLog.i(this.getClass().getName(),"Promijenen end");
				}
				else{
					//removedLog.i(this.getClass().getName(),"Promijenen start");
					rangeStart = minValue;
					playerFragment.seekTo(minValue);
					seekBar.setProgress(minValue);
				}
				
				playerFragment.setLoopingStart((int) minValue);
				playerFragment.setLoopingEnd((int) maxValue);
				
			}
			
		});

		setViewTexts();

		seekUpdation();

	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void setViewTexts() {
		seekBar.setMax(playerFragment.getDuration());
//		btnPlayPause.setText((playerFragment.isSetToPlay()) ? "Pause" : "Play");
//		btnPlayPause.setBackgroundResource((playerFragment.isSetToPlay()) ? R.drawable.ic_pause_base : R.drawable.ic_play_base);
		btnPlayPause.setBackgroundResource((playerFragment.isSetToPlay()) ? R.drawable.ic_pause : R.drawable.ic_play);
		
	}

	@Override
	protected void onDestroy() {	
		super.onDestroy();
		
		//removedLog.i(this.getClass().getName(),"onDestroy");

		/*
		 * ne zelim da se u pozadini, na odbacenom activitiju nastavi vrtiti
		 * update loopuvjeri se da se to zaista dogaða ako makneš ovaj redak
		 */
		stopUpdation();

		if (isFinishing()) {
			
			//removedLog.i(this.getClass().getName(),"onDestroy, isFinishing");
			
			playerFragment.stop();
			
//			playerFragment.setRetainInstance(false);
			
//			FragmentManager fm = getFragmentManager();
//			fm.beginTransaction().remove(playerFragment).commit();
			
			playerFragment = null;
		}

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		//removedLog.i(this.getClass().getName(),"onSaveInstanceState");
		//removedLog.i(this.getClass().getName(),"ACTIVITY: " + this);

		getFragmentManager().putFragment(outState, "selectorFragment", selectorFragment);
		outState.putSerializable("arbiter", arbiter);
		outState.putString("results", resultsTextView.getText().toString());
		
		outState.putInt("rangeStart", rangeStart);
		
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		//removedLog.i(this.getClass().getName(),"onRestoreInstanceState");
		
		selectorFragment = (TrackSelectorFragment) getFragmentManager().getFragment(savedInstanceState, "selectorFragment");
		arbiter = (Arbiter) savedInstanceState.getSerializable("arbiter");
		resultsTextView.setText(savedInstanceState.getString("results"));
		
		rangeStart = savedInstanceState.getInt("rangeStart");
	}
	
	
	/**Called by play/pause button <b>in order to toggle<b> between paused and playing states.
	 * Does the following:
	 * 1) sets appropriate icons (play/pause)
	 * 2) notifies DraggerFragment of playing/paused state so it can update its own icons
	 * 3) stops/resumes progress polling loop
	 */
	private void playPause() {

		if (playerFragment.isPlaying()) {
						
			playerFragment.pause();
//			btnPlayPause.setText("Play");
			btnPlayPause.setBackgroundResource(R.drawable.ic_play);
			selectorFragment.notifiyPlaybackStopped();

			stopUpdation();
			seekBar.setProgress(playerFragment.getCurrentPosition());

		} else {
			
			playerFragment.play();
//			btnPlayPause.setText("Pause");
			btnPlayPause.setBackgroundResource(R.drawable.ic_pause);
			selectorFragment.notifiyPlaybacksStarted();
			seekUpdation();
		}
	}

	//=========================================================================
	//Implementation of AudioTrackFragment.PlayerFragmentCallbacks interface
	//=========================================================================
	
	//TODO: not used, delete
//	@Override
//	public void sendMessage(Message msg) {
//		GUIHandler.sendMessage(msg);
//	}
	
	@Override
	public void onCompletion(AudioTrkPlyr player) {
		
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				btnPlayPause.setBackgroundResource(R.drawable.ic_play);
				selectorFragment.notifiyPlaybackStopped();
				stopUpdation();
				seekBar.setProgress(playerFragment.getCurrentPosition());
				//removedLog.i(this.getClass().getName(),"onCompletion executed in GUI thread");
			}
		});
	}
	//--------------------------------------------------------------------------

	Runnable seekBarUpdater = new Runnable() {
		@Override
		public void run() {
			seekUpdation();
		}
	};

	private void stopUpdation() {
		GUIHandler.removeCallbacks(seekBarUpdater);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//removedLog.i(this.getClass().getName(),"onResume");
		
		if (playerFragment.isSetToPlay()) { //this is where the difference between isSeStoPlay and isPlaying comes to play
			btnPlayPause.setBackgroundResource(R.drawable.ic_pause);
			selectorFragment.notifiyPlaybacksStarted();
			seekBar.setProgress(playerFragment.getCurrentPosition());
			seekUpdation();

		} else {
			btnPlayPause.setBackgroundResource(R.drawable.ic_play);
			selectorFragment.notifiyPlaybackStopped();
			seekBar.setProgress(playerFragment.getCurrentPosition());
		}
		
//		seekUpdation();
//		
//		if(playerFragment.isPlaying()){
//			seekUpdation();
//		}else{
//			stopUpdation();
//		}
	}
	
	
	@Override
	protected void onStop() {
		super.onStop();
		
		//removedLog.i(this.getClass().getName(),"onStop");
		
//		stopUpdation();
	}

	public void seekUpdation() {
		
		//removedLog.w(this.getClass().getName(), "Progress poll (seekUpdation). Seekbar instance: " + seekBar.toString());
		seekBar.setProgress(playerFragment.getCurrentPosition());
		// sam se zaustavlja ako je pauziran
		if (playerFragment.isSetToPlay()) {
			stopUpdation();
			GUIHandler.postDelayed(seekBarUpdater, 300);
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// Nastavi automatski updajte seekbara
		seekUpdation();

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// korisnik pomice seekbar, nemoj ga programski updejtat
		stopUpdation();

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (fromUser) {
			playerFragment.seekTo(progress);
		}
	}

	@Override
	public boolean pressedA() {
		//removedLog.w("pritisnut A", "vracam A");
		
		//TODO: ne direktno solirati, vec pogledati koji je trenutni binding
//		int boundPlayer = binder.getTrack(R.id.btnMicanje1);
//		switch (boundPlayer) {
//		case MultiplePlayerFragment.PLAYER1:
//			//TODO: saznaj koja trenutno odgovara gumbu A i soliraj ju
//			playerFragment.solo1();
//			break;
//		case MultiplePlayerFragment.PLAYER2:
//			playerFragment.solo2();
//			break;
//		default:
//			break;
//		}
		
		int boundPlayer = arbiter.get_A_track();
		switch (boundPlayer) {
		case Arbiter.track1:
			playerFragment.solo1();
			break;
		case Arbiter.track2:
			playerFragment.solo2();
			break;
		default:
			break;
		}

		return playerFragment.isSetToPlay();
	}

	@Override
	public boolean pressedB() {
		//removedLog.w("pritisnut B", "vracam B");
		
//		//TODO: ne direktno solirati, vec pogledati koji je trenutni binding
//		int boundPlayer = binder.getTrack(R.id.btnMicanje2);
//		switch (boundPlayer) {
//		case MultiplePlayerFragment.PLAYER1:
//			playerFragment.solo1();
//			break;
//		case MultiplePlayerFragment.PLAYER2:
//			playerFragment.solo2();
//			break;
//		default:
//			break;
//		}
		
		int boundPlayer = arbiter.get_B_track();
		switch (boundPlayer) {
		case Arbiter.track1:
			playerFragment.solo1();
			break;
		case Arbiter.track2:
			playerFragment.solo2();
			break;
		default:
			break;
		}

		return playerFragment.isSetToPlay();
	}

	@Override
	public boolean pressedX() {
		//removedLog.w("pritisnut X", "vracam X");

		//TODO: ne direktno solirati, vec pogledati koji je trenutni binding
//		int boundPlayer = binder.getTrack(R.id.button22);
//		switch (boundPlayer) {
//		case MultiplePlayerFragment.PLAYER1:
//			playerFragment.solo1();
//			break;
//		case MultiplePlayerFragment.PLAYER2:
//			playerFragment.solo2();
//			break;
//		default:
//			break;
//		}
		
		int boundPlayer = arbiter.get_X_track();
		switch (boundPlayer) {
		case Arbiter.track1:
			playerFragment.solo1();
			break;
		case Arbiter.track2:
			playerFragment.solo2();
			break;
		default:
			break;
		}
		
		return playerFragment.isSetToPlay();
	}

	@Override
	public boolean pressedY() {
		//removedLog.w("pritisnut Y", "vracam Y");

		//TODO: ne direktno solirati, vec pogledati koji je trenutni binding
//		int boundPlayer = binder.getTrack(R.id.button33);
//		switch (boundPlayer) {
//		case MultiplePlayerFragment.PLAYER1:
//			playerFragment.solo1();
//			break;
//		case MultiplePlayerFragment.PLAYER2:
//			playerFragment.solo2();
//			break;
//		default:
//			break;
//		}
		
		int boundPlayer = arbiter.get_Y_track();
		switch (boundPlayer) {
		case Arbiter.track1:
			playerFragment.solo1();
			break;
		case Arbiter.track2:
			playerFragment.solo2();
			break;
		default:
			break;
		}

		return playerFragment.isSetToPlay();
	}

//	/**Confusing, isSetToPlay is better used directly -> WRONG, has nothing to do with those is playings
//	 * this here is implementation of IButtonPress interface, that dragger fragment uses to inquire about the supposed playback state.
//	 * however, that is never used so this whole thing is commented to be deleted
//	 * @see antonioplesic.simpleabxtester.player.IButtonPress 
//	 */
//	@Override
//	public boolean isPlaying() {
//		return playerFragment.isSetToPlay();
//	}
	
	@Override
	public boolean pressedOK(boolean AisX, boolean AisY, boolean BisX, boolean BisY) {
		
		//check with arbiter if claims are true
		if(arbiter.check(AisX, AisY)){
			Toast.makeText(this, "Correct :)", Toast.LENGTH_SHORT).show();
		}
		else{
			Toast.makeText(this, "Incorrect!", Toast.LENGTH_SHORT).show();
		}
		
		DecimalFormat decimalFormat = new DecimalFormat();
		decimalFormat.setMaximumFractionDigits(2);
		
		resultsTextView.setText("Correct " + arbiter.correctCount() + "/" + arbiter.testCount() + " trials\nProbability of guessing: " + decimalFormat.format(arbiter.probabilityOfGuessing()*100) + "%");
		
		
		//randomize
		arbiter.randomizeTracks();
		selectorFragment.initialize();
		
		return false;
	}
	
	


}
