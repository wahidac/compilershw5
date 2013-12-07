import java.util.HashMap;

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

//Init use + def sets
public class InitCFG extends VInstr.VisitorP<CFGNode,Throwable>  {

	//Map function names to CFGs
	HashMap <String,CFGNode> CFGs;
	//Map instructions to CFGs
	HashMap <VInstr,CFGNode> instructionsToCFGNode;
	VaporProgram program;
	
	public InitCFG(VaporProgram program) throws Throwable {
		CFGs = new HashMap<String,CFGNode>();
		instructionsToCFGNode = new HashMap<VInstr,CFGNode>();
		this.program = program;
		for(VFunction func:program.functions) {
			CFGNode root;
			CFGNode prev = null;
			for(int i = 0; i < func.body.length; i++) {
				CFGNode n = new CFGNode(func,func.body[i]);
				instructionsToCFGNode.put(func.body[i], n);
				if(i == 0) {
					root = n;
					CFGs.put(func.ident, root);
				}
				//Calc use + def set
				func.body[i].accept(n, this);
				if(prev != null) {
					prev.successors.add(n);
				} 
				prev = n;
			}			
			
			//Now add in extra edges by looking at goto statements. 
			for(int i = 0; i < func.body.length; i++) {
				//Goto's appear in goto statements and branches. A goto 
				VInstr instr = func.body[i];
				CFGNode n = instructionsToCFGNode.get(instr);
				if(instr instanceof VBranch) {
					VBranch branch = (VBranch)instr;
					int targetInstructionIndex = branch.target.getTarget().instrIndex;
					VInstr targetInstruction = func.body[targetInstructionIndex];
					//Get the CFG Node associated with this instruction
					CFGNode targetNode = instructionsToCFGNode.get(targetInstruction);
					assert(targetNode != null);
					//Add an edge from this node to the target node
					n.successors.add(targetNode);
				} else if(instr instanceof VGoto) {
					VGoto gotoInstr = (VGoto)instr;
					VAddr<VCodeLabel> address = gotoInstr.target;
					if(address instanceof VAddr.Label<?>) {
						//A static label, so we know where this goto will lead to
						VAddr.Label<VCodeLabel> label = (VAddr.Label<VCodeLabel>) address;
						int targetInstructionIndex = label.label.getTarget().instrIndex;
						VInstr targetInstruction = func.body[targetInstructionIndex];
						//Get the CFG Node associated with this instruction
						CFGNode targetNode = instructionsToCFGNode.get(targetInstruction);
						assert(targetNode != null);
						//Add an edge from this node to the target node
						n.successors.add(targetNode);
					} else if(address instanceof VAddr.Var<?>) {
						//Address is stored inside a variable
						//Can't at compile time know all labels this goto may go to so 
						//add an edge between this node and all labels in this function
						//NOTE: if considering scope of whole program, need to look at all gotos
						
						//Iterate through all labels in this function
						for(VCodeLabel l:func.labels) {
							int targetInstructionIndex = l.instrIndex;
							VInstr targetInstruction = func.body[targetInstructionIndex];
							//Get the CFG Node associated with this instruction
							CFGNode targetNode = instructionsToCFGNode.get(targetInstruction);
							assert(targetNode != null);
							//Add an edge from this node to the target node
							n.successors.add(targetNode);
						}
					}
				}
			}
		
		}	
	}
	
	public static boolean isOperandVariable(VOperand operand) {
		return (operand instanceof VVarRef.Local);
	}

	public static String variableFromMemAddress(VAddr varAddr) {
		if(varAddr instanceof VAddr.Var<?>) {
			VAddr.Var v = (VAddr.Var)varAddr;
			String var = v.var.toString();
			return var;
		}
		return null;
	}
	
	
	@Override
	public void visit(CFGNode n, VAssign arg1) throws Throwable {
		//RHS variables are use set
		if(isOperandVariable(arg1.source)) {
			n.use.add(arg1.source.toString());
		}
		//LHS in def
		n.def.add(arg1.dest.toString());
	}


	@Override
	public void visit(CFGNode n, VCall arg1) throws Throwable {
		//Function call
		//All variable arguments in use 
		for(VOperand op:arg1.args) {
			if(isOperandVariable(op)) {
				//NOTE:'this' show up as local var right?
				n.use.add(op.toString());
			}
		}
		
		if(arg1.dest != null) {
			n.def.add(arg1.dest.toString());
		}
		
		//May be using a variable to call the actual function
		String fun = variableFromMemAddress(arg1.addr);
		if(fun != null) {
			n.use.add(fun);
		}
	}


	@Override
	public void visit(CFGNode n, VBuiltIn arg1) throws Throwable {
		//All variable arguments in use 
		for(VOperand op:arg1.args) {
			if(isOperandVariable(op)) {
				//NOTE:'this' show up as local var right?
				n.use.add(op.toString());
			}
		}
		if(arg1.dest != null) {		
			n.def.add(arg1.dest.toString());
		}
	}


	@Override
	public void visit(CFGNode n, VMemWrite arg1) throws Throwable {
		if(isOperandVariable(arg1.source)) {
			n.use.add(arg1.source.toString());
		}
		
		//Handle case in which base address is in a variable.
		if(arg1.dest instanceof VMemRef.Global) {
			VMemRef.Global ref = (VMemRef.Global)arg1.dest;
			String baseAddr = variableFromMemAddress(ref.base);
			if(baseAddr != null) {
				n.use.add(baseAddr);
			}
		}
	}

	@Override
	public void visit(CFGNode n, VMemRead arg1) throws Throwable {
		//Handle case in which base address is in a variable
		
		if(arg1.source instanceof VMemRef.Global) {
			VMemRef.Global ref = (VMemRef.Global)arg1.source;
			String baseAddr = variableFromMemAddress(ref.base);
			if(baseAddr != null) {
				n.use.add(baseAddr);
			}
		}
		
		n.def.add(arg1.dest.toString());
		
	}


	@Override
	public void visit(CFGNode n, VBranch arg1) throws Throwable {
		if(isOperandVariable(arg1.value)) {
			n.use.add(arg1.value.toString());
		}
	}


	@Override
	public void visit(CFGNode n, VGoto arg1) throws Throwable {
		String var = variableFromMemAddress(arg1.target);
		if(var != null) {
			n.use.add(var);
		}
	}


	@Override
	public void visit(CFGNode n, VReturn arg1) throws Throwable {
		if(isOperandVariable(arg1.value)) {
			n.use.add(arg1.value.toString());
		}
		
	}
	
	//For debugging
	public static void printCFGNode(CFGNode currentNode, int startLineNum) {
		System.out.println("-------------------------------------------------------------------------------------");
		System.out.println("Node: " + currentNode.instruction.toString() + " at line: " + (currentNode.instruction.sourcePos.line - startLineNum));
		String useSet = "{ ";
		for(String var:currentNode.use) {
			useSet += " " + var;
		}
		useSet += " }";
		System.out.println("Use Set: " + useSet);
		
		String defSet = "{ ";
		for(String var:currentNode.def) {
			defSet += " " + var;
		}
		defSet += " }";
		System.out.println("Def Set: " + defSet);
		
		
		String liveIn = "{ ";
		for(String var:currentNode.liveIn) {
			liveIn += " " + var;
		}
		liveIn += " }";
		System.out.println("Live-In: " + liveIn);

		String liveOut = "{ ";
		for(String var:currentNode.liveOut) {
			liveOut += " " + var;
		}
		liveOut += " }";
		System.out.println("Live-Out: " + liveOut);
		
		
		String successors = "{ ";
		for(CFGNode n:currentNode.successors) {
			successors += " " + n.instruction.toString() + " at line: " + (n.instruction.sourcePos.line - startLineNum) + ',';
		}
		successors += " }";
		System.out.println("Successors: " + successors);
		System.out.println("-------------------------------------------------------------------------------------\n");
		
	}
	
	//For debugging 
	public void printCFG() {
		for(VFunction func:program.functions) {
			System.out.println(func.ident + ":\n");
			for(int i = 0; i < func.body.length; i++) {
				CFGNode n = instructionsToCFGNode.get(func.body[i]);
				printCFGNode(n,0);
			}

		}
	}

}
