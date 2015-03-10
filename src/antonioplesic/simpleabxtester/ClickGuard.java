package antonioplesic.simpleabxtester;

/* XXX: there must be some platform provided way of doing this, or some more
 * elegant way (handling all clicks via the same listener, that in turn checks
 * for multiple/double clicks). Something about declaring launch mode in the 
 * manifest for activities is suggested on the Internet. */

/**
 * For preventing quick successive, or multiple button presses.
 * 
 * General idea: before doing certain actions, button should check
 * if another incompatible action is already being executed/started 
 * asynchronously (such as starting an activity). Since I prefer anonymous
 * button onClick listeners, using this guard should look something like this:
 * 
 * <pre><code>
 * someButton.setOnClickListener(new OnClickListener() {
 * {@literal @}Override
 *    public void onClick(View v) {
 *        if(passesClickGuard()){
 *            someAction();
 *        }
 *    }
 * });
 * </code></pre>
 * 
 * ClickGuard should be reset in onResume(), or at any other time when button 
 * clicks that check this guard should be enabled again.
 */
public class ClickGuard {

	private boolean passable;
	
	/**
	 * Creates a new ClickGuard object. Initial state is passable.
	 */
	public ClickGuard(){
		this.passable = true;
	}
	
	/**
	 * Returns true if guard is passable (if button click should be performed). If so,
	 * guard is made unpassable for subsequent checks, until the reset() is called.
	 * Else if unpassable, false is returned.
	 * @return True if passable, false if not.
	 */
	boolean passed(){
		if(passable==true){
			passable=false;
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Resets ClickGuard to passable state.
	 */
	public void reset(){
		this.passable = true;
	}
	
}
