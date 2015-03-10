package antonioplesic.simpleabxtester.player;

import java.util.Random;

import android.util.Log;
import android.util.SparseArray;

/**
 * @deprecated Replaced by Arbiter class.
 */
@Deprecated
public class BtnTrackBinder {

	private static BtnTrackBinder binderInstance;

	public static BtnTrackBinder getInstance() {
		synchronized (BtnTrackBinder.class) {
			if (binderInstance == null) {
				//removedLog.w("binder ne postoji", "binder ne postoji, stvaram ga");
				binderInstance = new BtnTrackBinder();
				return binderInstance;
			} else {
				//removedLog.w("binder vec postoji", "binder vec postoji, vracam postojecu referencu");
				return binderInstance;
			}

		}
	}

	public static BtnTrackBinder getInstance(int buttonAid, int buttonAtrack,
			int buttonBid, int buttonBtrack, int buttonXid, int buttonXtrack,
			int buttonYid, int buttonYtrack) {

		synchronized (BtnTrackBinder.class) {
			if (binderInstance == null) {
				//removedLog.w("binder ne postoji", "binder ne postoji, stvaram ga");
				binderInstance = new BtnTrackBinder();
				binderInstance.bind(buttonAid, buttonAtrack, buttonBid, buttonBtrack, buttonXid, buttonXtrack, buttonYid, buttonYtrack);
				return binderInstance;
			} else {
				//removedLog.w("binder vec postoji", "binder vec postoji, vracam postojecu referencu");
				return binderInstance;
			}
		}

	}

	private SparseArray<Integer> map;

	/**
	 * Rearanges tracks of the target MultiplePlayerFragment
	 * 
	 * 
	 * @param playerFragment
	 */
	public void rearangeTracks(MultiplePlayerFragment playerFragment) {

		String path1 = "";
		String path2 = "";

		playerFragment.loadAudio(path1, path2);
	}

	public void bind(int buttonAid, int buttonAtrack, int buttonBid,
			int buttonBtrack, int buttonXid, int buttonXtrack, int buttonYid,
			int buttonYtrack) {

		map = new SparseArray<Integer>();
		map.put(buttonAid, buttonAtrack);
		map.put(buttonBid, buttonBtrack);
		map.put(buttonXid, buttonXtrack);
		map.put(buttonYid, buttonYtrack);
	}

	public int getTrack(int buttonId) {
		return map.get(buttonId);
	}
	
	/**Randomizes binding while ensuring that:
	 * 1) {buttonA,buttonB} -> {track1,track2} is bijective mapping
	 * 2) {buttonX,buttonY} -> {track1,track2} is bijective mapping
	 * 
	 * @param buttonA
	 * @param buttonB
	 * @param buttonX
	 * @param buttonY
	 * @param track1
	 * @param track2
	 */
	public void randomizeBinding(int buttonA, int buttonB, int buttonX, int buttonY, int track1, int track2){
		Random r = new Random();
		
		map = new SparseArray<Integer>();
		
		//bijectivly map {buttonA,buttonB} -> {track1,track2}
		if(r.nextBoolean()){
			map.put(buttonA, track1);
			map.put(buttonB, track2);
		}
		else{
			map.put(buttonA, track2);
			map.put(buttonB, track1);
		}
		
		//bijectivly map {buttonX,buttonY} -> {track1,track2}
		if(r.nextBoolean()){
			map.put(buttonX, track1);
			map.put(buttonY, track2);
		}
		else{
			map.put(buttonX, track2);
			map.put(buttonY, track1);
		}
		
	}

}
