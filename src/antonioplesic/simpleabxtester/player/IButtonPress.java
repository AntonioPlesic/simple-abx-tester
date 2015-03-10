package antonioplesic.simpleabxtester.player;

/**
 * Implementing this interface, activity that holds one of selector fragments
 * gets notified of button presses inside that fragment. Activity returns whether
 * playback is currently stopped or not.
 */
public interface IButtonPress {

	/**
	 * Notifies activity that button A is pressed; as a result, activity returns
	 * whether playback is stopped or not.
	 * 
	 * @return Playback state (playing, not playing).
	 */
	public boolean pressedA();

	public boolean pressedB();

	public boolean pressedX();

	public boolean pressedY();
	
	public boolean pressedOK(boolean AisX, boolean AisY, boolean BisX, boolean BisY);

//	/**
//	 * @see antonioplesic.simpleabxtester.player.PlayerActivity#isPlaying() 
//	 * @return supposed playback state
//	 */
//	public boolean isPlaying();
}