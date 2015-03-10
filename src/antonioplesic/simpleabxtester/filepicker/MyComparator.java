package antonioplesic.simpleabxtester.filepicker;

import java.io.File;
import java.util.Comparator;

import antonioplesic.simpleabxtester.filepicker.FilePickerActivity;



//TODO: mozda bolje staviti kao nested klasu?
public class MyComparator implements Comparator<File> {
	
	public static final int SORT_NONE = 0;
	public static final int SORT_AtoZ = 1;
	public static final int SORT_ZtoA = 2;
	public static final int SORT_CREATED_ASC = 3; //oldest first
	public static final int SORT_CREATED_DESC = 4; //newest first
	
	private int mode = SORT_NONE;
	
	public MyComparator(int mode){
		this.mode = mode;
	}
	
	public MyComparator(){
		this.mode = SORT_NONE;
	}

	@Override
	public int compare(File lhs, File rhs) {
	
		switch (mode) {
		case SORT_AtoZ:
			return lhs.getName().compareTo(rhs.getName()); 
		case SORT_ZtoA:
			return rhs.getName().compareTo(lhs.getName());
		case SORT_CREATED_ASC:
			return Long.valueOf(lhs.lastModified()).compareTo(rhs.lastModified());
		case SORT_CREATED_DESC:
			return Long.valueOf(rhs.lastModified()).compareTo(lhs.lastModified());
		default:
			return 0;
		}
		
		
	}

	
	
}
