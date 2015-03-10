package antonioplesic.simpleabxtester.player;

import java.io.Serializable;
import java.util.Random;

public class Arbiter implements Serializable {
	
//	public static final int A = 10;
//	public static final int B = 11;
//	public static final int X = 12;
//	public static final int Y = 13;
	
	public static final int TYPE_NORMAL = 34987;
	public static final int TYPE_SPECIAL = 43824;
	public static final int TYPE_ONE_REFERENCE_TWO_CHOICES = 45241;
	
	private int type;
	
	public Arbiter(){
		type = TYPE_NORMAL;
		randomizeTracks();
	}
	
	public Arbiter(int type){
		this.type = type;
		randomizeTracks();
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int A;
	private int B;
	private int X;
	private int Y;
	
	private int testCount = 0;
	private int correctCount = 0;
	
	public static final int track1 = 14;
	public static final int track2 = 4544;
	
	public int get_A_track(){
		return A;
	}
	
	public int get_B_track(){
		return B;
	}
	
	public int get_X_track(){
		return X;
	}
	
	public int get_Y_track(){
		return Y;
	}
	
	public boolean check(boolean claim_A_is_X, boolean claim_A_is_Y){
		
//		if(claim_A_is_X && claim_A_is_Y){
//			throw new IllegalArgumentException("Only one claim can be claimed");
//		}
//		
//		if( (claim_A_is_X || claim_A_is_Y) == false ){
//			throw new IllegalArgumentException("At least one situation must be claimed");
//		}
		
		if(claim_A_is_X){
			
			if(get_A_track() == get_X_track()){
				onCorrect();
				return true;
			}
			else{
				onIncorrect();
				return false;
			}	
		}
		
		else if(claim_A_is_Y){
			
			if(get_A_track() == get_Y_track()){
				onCorrect();
				return true;
			}
			else{
				onIncorrect();
				return false;
			}
		}
		
		//implicit claim A_is_B, X_is_Y
		else{
			
			//same as get_X_track() == get_Y_track()
			if(get_A_track() == get_B_track()){
				onCorrect();
				return true;
			}
			else{
				onIncorrect();
				return false;
			}
			
		}
	}
	
	public void setTracks(boolean AisX, boolean AisY, boolean BisX, boolean BisY){
		
	}
	
	public void randomizeTracks(){
		
		if(type == TYPE_NORMAL){

			int situation = randInt(0, 3);

			switch (situation) {
			case 0:
				X=track1;
				Y=track2;
				A=X;
				B=Y;
				break;
			case 1:
				X=track1;
				Y=track2;
				A=Y;
				B=X;
				break;
			case 2:
				X=track2;
				Y=track1;
				A=X;
				B=Y;
				break;
			case 3:
				X=track2;
				Y=track1;
				A=Y;
				B=X;
				break;

			default:
				//should never happen
				break;
			}
		}
		
		if(type == TYPE_SPECIAL){
			
			int situation = randInt(0, 5);
			
			/*posible situations: 
			 *     A     B     X(C)  Y
			 * 0:  t1    t1    t2    t?(2)
			 * 1:  t1    t2    t1    t?(2)
			 * 2:  t2    t1    t1    t?(2)
			 * 3:  t2    t2    t1    t?(1)
			 * 4:  t2    t1    t2    t?(1)
			 * 5:  t1    t2    t2    t?(1)
			 * Only tracks behind A,B,X(C) actually matter, Y is same as the least used in A,B,X
			 */
			int t1 = track1;
			int t2 = track2;
			
			switch (situation) {
			case 0:  A = t1; B = t1; X = t2;   Y = t2; break;
			case 1:  A = t1; B = t2; X = t1;   Y = t2; break;
			case 2:  A = t2; B = t1; X = t1;   Y = t2; break;
			case 3:  A = t2; B = t2; X = t1;   Y = t1; break;
			case 4:  A = t2; B = t1; X = t2;   Y = t1; break;
			case 5:  A = t1; B = t2; X = t2;   Y = t1; break;
			default:
				break;
			}
			
		}
		
		if(type == TYPE_ONE_REFERENCE_TWO_CHOICES){
			
			int situation = randInt(0, 3);

			/*possible situations:
			 *		reference(A)	choice1(B)		choice2(X)	|	dummy(Y)
			 *0:		t1				t1				t2		|	  t2
			 *1:		t1 				t2				t1		|	  t2
			 *2:		t2				t1				t2		|	  t1
			 *3:		t2				t2				t1		|	  t1
			 */
			
			int t1 = track1;
			int t2 = track2;
			
			switch (situation) {
			case 0: A = t1; B = t1; X = t2; Y = t2; break;
			case 1: A = t1; B = t2; X = t1; Y = t2; break;
			case 2: A = t2; B = t1; X = t2; Y = t1; break;
			case 3: A = t2; B = t2; X = t1; Y = t1; break;
			default:
				break;
			}
			
		}
		
	}
	
	//http://stackoverflow.com/a/363692
	public static int randInt(int min, int max) {

	    Random rand = new Random();
	    int randomNum = rand.nextInt((max - min) + 1) + min;
	    return randomNum;
	}
	
	private void onCorrect(){
		testCount++;
		correctCount++;
	}
	
	private void onIncorrect(){
		testCount++;
	}
	
	public int testCount(){
		return testCount;
	}
	
	public int correctCount(){
		return correctCount;
	}
	
	public double probabilityOfGuessing(){
		
//		return prob(this.testCount, this.correctCount);
		return probBetter(this.testCount, this.correctCount);
	}
	
	//#########################################################################
	//######## old way of calculating probability of guessing #################
	//#########################################################################
	//overflow at 170! -> calculation basically limited to 170 trials
	
//	//lol, faktorijeli
//	private double prob(int total, int correct){
//		
//		double result = 0;
//		
//		for(int i = correct;i<total+1;i++){
//			result = result + (factorial(total)/(factorial(i)*factorial(total-i))) * Math.pow(0.5, total);	
//		}
//		return result;
//	}
//	
//	//lol, ubij se
//	private double factorial(int x){
//		
//		if(x==0){
//			return 1;
//		}
//		else{
//			return x*factorial(x-1);
//		}
//		
//	}
	
	//#########################################################################
	//######## better way of calculating probability of guessing ##############
	//#########################################################################
	
	//tested up to 600 trials
	
	private double probBetter(int total, int correct){
		
		double probability = 0;
		
		for(int i = correct;i<total+1;i++){
			probability += binomial(total, i)* Math.pow(0.5, total);
		}
		
		return probability;
	}
	
	private double binomial(int n, int k){
		
		if(k>n){
			//should throw exception or something
			throw new IllegalArgumentException("k must be <= n");
		}
		if(k==0){
			return 1;
		}
		if(k>n/2){
			return binomial(n, n-k);
		}
		return n*(binomial(n-1, k-1)/k);
	}

}
