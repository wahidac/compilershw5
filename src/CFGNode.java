import java.util.HashSet;
import java.util.LinkedHashSet;

import cs132.vapor.ast.VFunction;
import cs132.vapor.ast.VInstr;


//A node in the control flow graph. Will have an edge to any node
//the instruction can reach. Each instruction in the program will
//have its own CFGNode. There will be one control flow graph per
//function
public class CFGNode {
	//Variables in use set
	HashSet<String> use;
	HashSet<String> def;
	HashSet<String> liveIn;
	HashSet<String> liveOut;
	VInstr instruction;
	int lineNum;
	VFunction function;
	//Instructions that can be directly reached from this node
	HashSet<CFGNode> successors;
	
	public CFGNode(VFunction func, VInstr instruction) {
		use = new HashSet<String>();
		def = new HashSet<String>();
		liveIn = new HashSet<String>();
		liveOut = new HashSet<String>();
		successors = new LinkedHashSet<CFGNode>();
		this.instruction = instruction;
		this.lineNum = instruction.sourcePos.line;
		this.function = func;
	}

}
