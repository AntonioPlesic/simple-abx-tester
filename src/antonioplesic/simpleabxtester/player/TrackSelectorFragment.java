package antonioplesic.simpleabxtester.player;

import android.app.Fragment;

/**
 * Base class for various selector interface types.
 */
public abstract class TrackSelectorFragment extends Fragment {
	
	public abstract void notifiyPlaybacksStarted();
	
	public abstract void notifiyPlaybackStopped();
	
	public abstract void initialize();

}
