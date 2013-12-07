import java.util.ArrayList;
import java.util.LinkedHashSet;

import cs132.vapor.ast.VFunction;


//Represent live ranges for some variable
public class LiveRanges {
	String varName;
	VFunction enclosingFunction;
	//Represent ranges as a set of range tuples
	//so a live range of 1-->2-->3--->4-->5-->2 will
	//be represented like {(1,2),(2,3),(3,4),(4,5),(5,2)}
	LinkedHashSet<RangeTuple> ranges;
	
	public LiveRanges() {
		ranges = new LinkedHashSet<RangeTuple>();
	}
	
	public void addEdge(int lineStart, int lineEnd) {
		RangeTuple edge = new RangeTuple(lineStart, lineEnd);
		this.ranges.add(edge);
	}
	
	//If Yes, then return true so indicate that the variable for which
	//we're checking whether a conflict exists cannot simultaneously be
	//in the same register as this variable
	public boolean rangeConflict(LiveRanges other) {
		for(RangeTuple edge:other.ranges) {
			if(ranges.contains(edge)) {
				return true;
			}
		}
		return false;
	}
}
