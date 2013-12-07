import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import cs132.vapor.ast.VFunction;
import cs132.vapor.ast.VInstr;
import cs132.vapor.ast.VaporProgram;


//Use the live in + live out sets to calculate live ranges
//for all of the variables so we can apply the linear
//scan algorithm. Number instructions based on
//their physical line number in the file
public class CalcLiveRanges {
	//Map function names to CFGs
	HashMap <String,CFGNode> CFGs;
	//Map instructions to CFGs
	HashMap <VInstr,CFGNode> instructionsToCFGNode;
	//Map function names to line ranges
	HashMap <String, HashMap<String,LiveRanges>> liveRanges;
	VaporProgram program;
	
	public CalcLiveRanges(HashMap <String,CFGNode> CFGs,HashMap <VInstr,CFGNode> instructionsToCFGNode, VaporProgram program ) {
		this.program = program;
		this.CFGs = CFGs;
		this.instructionsToCFGNode = instructionsToCFGNode;
		this.liveRanges = new HashMap <String, HashMap<String,LiveRanges>>();
		
		//Traverse each CFG
		for(VFunction f:program.functions) {
			HashMap<String,LiveRanges> ranges = new HashMap<String, LiveRanges>();
			HashSet<CFGNode> visited = new HashSet<CFGNode>();
			CFGNode root = CFGs.get(f.ident);
			CalcLiveRangesForCFG(root,ranges,visited);
			
			//Add first edge to the live range set, treating 0 as entrance to function
			for(String liveInVar:root.liveIn) {
				//Add an edge
				LiveRanges r = ranges.get(liveInVar);
				if(r == null) {
					r = new LiveRanges();
					ranges.put(liveInVar, r);
				}
				r.addEdge(0, root.lineNum);
			}
			liveRanges.put(f.ident, ranges);
		}
	}
	
	//Traverse CFG to calc live ranges.
	public void CalcLiveRangesForCFG(CFGNode node, HashMap<String,LiveRanges> ranges, HashSet<CFGNode> visited) {
		if(visited.contains(node)) {
			return;
		}
		
		for(String v:node.liveOut) {
			//Because this variable is live out, the var will be live in
			//in at least one of its successors. Find which successors, and
			//for each, add an edge to represent liveness
			boolean foundOneSuccesor = false;
			for(CFGNode successor:node.successors) {
				if(successor.liveIn.contains(v)) {
					//Add an edge
					LiveRanges r = ranges.get(v);
					if(r == null) {
						r = new LiveRanges();
						ranges.put(v, r);
					}
					
					r.addEdge(node.lineNum, successor.lineNum);
					foundOneSuccesor = true;
				}
			}
	
			assert(foundOneSuccesor);
		}
		
		
		
		//Now visit all of the successors
		for(CFGNode successor:node.successors) {
			//Keep track of visited nodes to prevent cycles
			visited.add(node);
			CalcLiveRangesForCFG(successor, ranges, visited);
		}
		
	}
	
	public void printLiveRanges() {
		//Traverse each CFG
		for(VFunction f:program.functions) {
			HashMap<String,LiveRanges> ranges = liveRanges.get(f.ident);
			//Print this set
			System.out.println("Function " + f.ident);
			for(Entry<String, LiveRanges> entry:ranges.entrySet()) {
				String var = entry.getKey();
				LiveRanges l = entry.getValue();
				printLiveRanges(var, l);
			}
		}
	}
	
	public static void printLiveRanges(String variable, LiveRanges l) {
		String set = variable + "= { ";
		LinkedHashSet<RangeTuple> tuples = l.ranges;
		for(RangeTuple tuple:tuples) {
			set += " " + String.valueOf(tuple.lineStart) + " -> " + String.valueOf(tuple.lineEnd) + ",";
		}
		set = set + " }";
		System.out.println(set);
	}
				
}
