package antonioplesic.simpleabxtester.player;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

/** 
 * Same as the regular Button, but can rapidly enter drag mode.<p>
 * Might break some methods like on onLongClick, however not a problem currently
 * since only normal click and drag actions are used. Most of the code taken from 
 * <a href="http://stackoverflow.com/a/16485989">http://stackoverflow.com/a/16485989</a>
 */
class MyButton extends Button{
	
	public MyButton(Context context) {
		super(context);
	}
	
	public MyButton(Context context, AttributeSet attrs){
		super(context,attrs);
	}
	
	public MyButton(Context context, AttributeSet attrs, int defStyle){
		super(context,attrs,defStyle);
	}
	
	private float mDownX;
	private float mDownY;
	private final float SCROLL_THRESHOLD = 10;
	private boolean isOnClick;

	
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
	    switch (ev.getAction() & MotionEvent.ACTION_MASK) {
	        case MotionEvent.ACTION_DOWN:
	            mDownX = ev.getX();
	            mDownY = ev.getY();
	            isOnClick = true;
	            break;
	        case MotionEvent.ACTION_CANCEL:
	        case MotionEvent.ACTION_UP:
	            if (isOnClick) {
	                //removedLog.i(this.getClass().getName(), "onClick ");
	                //TODO onClick code
	                performClick();
	            }
	            break;
	        case MotionEvent.ACTION_MOVE:
	            if (isOnClick && (Math.abs(mDownX - ev.getX()) > SCROLL_THRESHOLD || Math.abs(mDownY - ev.getY()) > SCROLL_THRESHOLD)) {
	                //removedLog.i(this.getClass().getName(), "movement detected");
	                isOnClick = false;
	                
	                //enter drag, moj dodatak
	                DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(this);
	    			
	                this.startDrag(null, shadowBuilder, this, 0);
//	                this.setVisibility(View.INVISIBLE); //nesmije biti tu
	                //removedLog.i(this.getClass().getName(),"this.setVisibility(View.INVISIBLE);");
	                
	    			return true;
	            }
	            break;
	        default:
	            break;
	    }
	    return true;
	}
	
}