import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import cs132.vapor.ast.VAddr;
import cs132.vapor.ast.VAssign;
import cs132.vapor.ast.VBranch;
import cs132.vapor.ast.VBuiltIn;
import cs132.vapor.ast.VCall;
import cs132.vapor.ast.VCodeLabel;
import cs132.vapor.ast.VFunction;
import cs132.vapor.ast.VGoto;
import cs132.vapor.ast.VInstr;
import cs132.vapor.ast.VMemRead;
import cs132.vapor.ast.VMemRef;
import cs132.vapor.ast.VMemWrite;
import cs132.vapor.ast.VOperand;
import cs132.vapor.ast.VReturn;
import cs132.vapor.ast.VTarget;
import cs132.vapor.ast.VVarRef;
import cs132.vapor.ast.VaporProgram;


public class CalcLiveInLiveOutSets {

	//Map function names to CFGs
	HashMap <String,CFGNode> CFGs;
	HashMap <VInstr,CFGNode> instructionsToCFGNode;
	VaporProgram program;


	public CalcLiveInLiveOutSets(HashMap<String,CFGNode> CFGs, HashMap<VInstr, CFGNode> instructionToCFGNode, VaporProgram program) {
		this.CFGs = CFGs;
		this.instructionsToCFGNode = instructionToCFGNode;
		this.program = program;
		for(VFunction func:program.functions) {
			calcLiveInLiveOut(func);	
		}
	}

	public void calcLiveInLiveOut(VFunction func) {
		//Perform the fixed point alg to get live in + live out sets
		boolean changeInSets = true;
		while(changeInSets) {
			changeInSets = false;
			for(VInstr instr:func.body) {
				CFGNode n = instructionsToCFGNode.get(instr);
			
				//Solve data flow equations
				HashSet<String> liveOutWithoutDef = new HashSet<String>(n.liveOut);
				liveOutWithoutDef.removeAll(n.def);
				HashSet<String> newLiveIn = new HashSet<String>(n.use);
				newLiveIn.addAll(liveOutWithoutDef);
			
				//Compute new live out
				HashSet<String> newLiveOut = new HashSet<String>();
				for(CFGNode successor:n.successors) {
					newLiveOut.addAll(successor.liveIn);
				}
				
				//Have the sets changed?
				if(!newLiveOut.equals(n.liveOut) || !newLiveIn.equals(n.liveIn)) {
					changeInSets = true;
					//Update sets
					n.liveIn = newLiveIn;
					n.liveOut = newLiveOut;
				} 
			}
		}
	}

}
