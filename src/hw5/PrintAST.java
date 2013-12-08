
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import cs132.vapor.ast.*;


//Traverse tree, swapping variable references to the registers they
//are assigned to, or a position in memory if they've been spilled
public class PrintAST extends VInstr.VisitorPR<Integer, String, Throwable>  {
	VFunction func;
	int bytesInStackFrame;
	static String indentationSpacing;
	//Starting offset for spilled variables (everything after the backup storage)
	int numLocals;
	int numOut;
	int eqCounter;
    String [] arguments;
    String returnReg;
    String framePointer;
    String stackPointer;
    String returnAddress;
    String registerZero;
    String freeReg;
    ArrayList<String> listOfConstants;
    
	public PrintAST(VFunction func, ArrayList<String> listOfConstants) {
		int numParams = func.params.length;
		VFunction.Stack funcStack =  func.stack;
		//8 by default to save return address and frame pointer
		bytesInStackFrame = 8;
		bytesInStackFrame += funcStack.out*4;
		bytesInStackFrame += funcStack.local*4;
		eqCounter = 0;
		
		numLocals = funcStack.local;
		numOut = funcStack.out;
				  	  
		arguments = new String[4];

		String prefix = "$a";
		for(int i = 0; i < arguments.length; i++) {
			arguments[i] = prefix + String.valueOf(i);
		}
		
		returnReg = "$v0";
		framePointer = "$fp";
		stackPointer = "$sp";
		returnAddress = "$ra";
		freeReg = "$t9";
		registerZero = "$0";
		indentationSpacing = "  ";	

		this.listOfConstants = listOfConstants;
	}

	
	public String returnIntro(Integer indentation) {
		//Save $fp
		String preamble = storeWord(framePointer, stackPointer, -8, indentation);
		//make $fp equal to current $sp
		String s1 = move(framePointer, stackPointer, indentation);
		//move stack pointer to top of new stack
		String s2 = subtract(stackPointer, stackPointer, String.valueOf(bytesInStackFrame), indentation);
		//Save return address
		String s3 = storeWord(returnAddress, framePointer, -4, indentation);
		return concatentateInstructions(preamble,s1,s2,s3);
	}
	
	public String returnOutro(Integer indentation) {
		//Load return address
		String s1 = loadWord(returnAddress, framePointer, -4, indentation);
		//Load frame pointer
		String s2 = loadWord(framePointer, framePointer, -8, indentation);
		//move stack pointer back to frame pointer location
		String s3 = add(stackPointer, stackPointer, String.valueOf(bytesInStackFrame), indentation);
		//Jump back
		String s4 = jumpRegister(returnAddress, indentation);
		return concatentateInstructions(s1,s2,s3,s4);
	}
	
	public static String returnBuiltInDefinitions() {
		//Place declarations of some built in functions at the bottom of the
		//mips files so we can call them
		String print = "_print:\n  li $v0 1\n  syscall\n  la $a0 _newline\n  li $v0 4\n  syscall\n  jr $ra\n";
		String error = "_error:\n  li $v0 4\n  syscall\n  li  $v0 10\n  syscall\n";
		String heapAlloc = "_heapAlloc:\n  li $v0 9\n  syscall\n  jr $ra\n";
		return concatentateInstructions(print, error, heapAlloc); 
	}
	
	
	@Override
	public String visit(Integer indentation, VAssign arg1) throws Throwable {
		//NOTE:handle string as input to register? if so, look at other functions too
		String assignment = "";
		String destReg = arg1.dest.toString();
		//source can be literal, label, or register
		if(arg1.source instanceof VLitInt) {
			assignment = loadImmediate(destReg, arg1.source.toString(), indentation);
		} else if(arg1.source instanceof VLabelRef<?>) {
			
			assignment = loadAddress(destReg, arg1.source.toString().substring(1), indentation);
		} else if(arg1.source instanceof VVarRef){
			assignment = move(destReg, arg1.source.toString(), indentation);
		} else {
			assert(false);
		}
 		return assignment;
	}

	@Override
	public String visit(Integer indentation, VCall arg1) throws Throwable {
		// TODO Auto-generated method stub
		VAddr<VFunction> address = arg1.addr;
		String functionCall = "";
		if(address instanceof VAddr.Label<?>) {
			//Function label
			String label = address.toString().substring(1);
			functionCall = jumpAndLink(label, indentation);
		} else {
			functionCall = jumpAndLinkRegister(address.toString(), indentation);
		}
		return functionCall;
	}

	@Override
	public String visit(Integer indentation, VBuiltIn arg1) throws Throwable {
		String builtInOp = arg1.op.name;
		int numParams = arg1.op.numParams;
		String builtInStr = "";
		String destReg = "";
		if(arg1.dest != null) {
			destReg = arg1.dest.toString();
		} else {
			destReg = registerZero;
		}

		//Add
		if(builtInOp.equalsIgnoreCase("Add")) {
			assert(numParams == 2);
			//Two operands
			VOperand first =  arg1.args[0];
			VOperand second =  arg1.args[1];
			String firstOp = "";
			String secondOp ="";

			
			if(first instanceof VLitInt) {
				builtInStr = loadImmediate(freeReg, first.toString(), indentation);
				firstOp = freeReg;
			} else {
				firstOp = first.toString();
			}
			secondOp = second.toString();
			
			String s = add(destReg, firstOp, secondOp, indentation);
			builtInStr = concatentateInstructions(builtInStr, s);
		} else if(builtInOp.equalsIgnoreCase("Sub")) {
			assert(numParams == 2);
			//Two operands
			VOperand first =  arg1.args[0];
			VOperand second =  arg1.args[1];
			String firstOp = "";
			String secondOp ="";

			if(first instanceof VLitInt) {
				builtInStr = loadImmediate(freeReg, first.toString(), indentation);
				firstOp = freeReg;
			} else {
				firstOp = first.toString();
			}
			secondOp = second.toString();
			
			String s = subtract(destReg, firstOp, secondOp, indentation);
			builtInStr = concatentateInstructions(builtInStr, s);
		} else if(builtInOp.equalsIgnoreCase("MulS")) {
			assert(numParams == 2);
			//Two operands
			VOperand first =  arg1.args[0];
			VOperand second =  arg1.args[1];
			String firstOp = "";
			String secondOp ="";
			
			if(first instanceof VLitInt) {
				builtInStr = loadImmediate(freeReg, first.toString(), indentation);
				firstOp = freeReg;
			} else {
				firstOp = first.toString();
			}
			secondOp = second.toString();
			
			String s = multiply(destReg, firstOp, secondOp, indentation);
			builtInStr = concatentateInstructions(builtInStr, s);
		} else if(builtInOp.equalsIgnoreCase("LtS")) {
			assert(numParams == 2);
			//Two operands
			VOperand first =  arg1.args[0];
			VOperand second =  arg1.args[1];
			String firstOp = "";
			String secondOp= "";
			
			if(first instanceof VLitInt) {
				builtInStr = loadImmediate(freeReg, first.toString(), indentation);
				firstOp = freeReg;
			} else {
				firstOp = first.toString();
			}
			secondOp = second.toString();
			
			String s = "";
			if(second instanceof VLitInt) {
				s = setLessThanImmediate(destReg, firstOp, secondOp, indentation);
			} else {
				s = setLessThanReg(destReg, firstOp, secondOp, indentation);
			}
			
			builtInStr = concatentateInstructions(builtInStr, s);
		} else if(builtInOp.equalsIgnoreCase("Lt")) {
			assert(numParams == 2);
			//Two operands
			VOperand first =  arg1.args[0];
			VOperand second =  arg1.args[1];
			String firstOp = "";
			String secondOp= "";
			
			if(first instanceof VLitInt) {
				builtInStr = loadImmediate(freeReg, first.toString(), indentation);
				firstOp = freeReg;
			} else {
				firstOp = first.toString();
			}
			secondOp = second.toString();
			
			String s = "";
			if(second instanceof VLitInt) {
				s = setLessThanUnsignedImmediate(destReg, firstOp, secondOp, indentation);
			} else {
				s = setLessThanUnsignedReg(destReg, firstOp, secondOp, indentation);
			}
			
			builtInStr = concatentateInstructions(builtInStr, s);
		} else if(builtInOp.equalsIgnoreCase("Eq")) {
			assert(numParams == 2);
			//Two operands
			VOperand first =  arg1.args[0];
			VOperand second =  arg1.args[1];
			String firstOp = "";
			String secondOp= "";
			
			if(first instanceof VLitInt) {
				builtInStr = loadImmediate(freeReg, first.toString(), indentation);
				firstOp = freeReg;
			} else {
				firstOp = first.toString();
			}
			secondOp = second.toString();
			
			String branchLabel1 = "_equalNumsBegin" + String.valueOf(eqCounter);
			String branchLabel2 = "_equalNumsEnd" + String.valueOf(eqCounter);
			eqCounter++;
			
			String s1 = subtract(freeReg, firstOp, secondOp, indentation);
			String s2 = branchOnEqualZero(freeReg, branchLabel1, indentation);
			String s3 = move(destReg, "$0", indentation);
			String s4 = jump(branchLabel2, indentation);
			String s5 = branchLabel1+":";
			String s6 = loadImmediate(destReg, "1", indentation);
			String s7 = branchLabel2+":";
			builtInStr = concatentateInstructions(builtInStr,s1,s2,s3,s4,s5,s6,s7);
		} else if(builtInOp.equalsIgnoreCase("PrintIntS")) {
			assert(numParams == 1);
			//Two operands
			VOperand first =  arg1.args[0];
			
			if(first instanceof VLitInt) {
				builtInStr = loadImmediate(arguments[0], first.toString(), indentation);
			} else {
				builtInStr = move(arguments[0], first.toString(), indentation);
			}

			String s = jumpAndLink("_print", indentation);
			builtInStr = concatentateInstructions(builtInStr, s);
		} else if(builtInOp.equalsIgnoreCase("HeapAllocZ")) {
			assert(numParams == 1);
			//Two operands
			VOperand first =  arg1.args[0];
			
			if(first instanceof VLitInt) {
				builtInStr = loadImmediate(arguments[0], first.toString(), indentation);
			} else {
				builtInStr = move(arguments[0], first.toString(), indentation);
			}

			String s1 = jumpAndLink("_heapAlloc", indentation);
			//Assign result to dest reg
			String s2 = move(destReg, returnReg, indentation);
			builtInStr = concatentateInstructions(builtInStr, s1,s2);
		} else if(builtInOp.equalsIgnoreCase("Error")) {
			assert(numParams == 1);
			String arg = arg1.args[0].toString();
			int index = listOfConstants.indexOf(arg);
			assert(index != -1);
			String argLabel = "_str" + String.valueOf(index);
			String s1 = loadAddress(arguments[0], argLabel, indentation);
			String s2 = jump("_error", indentation);
			builtInStr = concatentateInstructions(builtInStr, s1, s2);
		}
		
		return builtInStr;
	}

	@Override
	public String visit(Integer indentation, VMemWrite arg1) throws Throwable {
		//Refers to either global memory from the data seg or stack memory
		String memWrite = "";
		String writeDest = "";
		int offset = 0;
		
		if(arg1.dest instanceof VMemRef.Global) {
			//Global memory. of form [base_address+offset] where 
			//base address = label ,or is a register
			VMemRef.Global globalMemRef = (VMemRef.Global)arg1.dest; 
			offset = globalMemRef.byteOffset;
			if(globalMemRef.base instanceof VAddr.Label<?>) {
				//base address is a label
				String label = globalMemRef.base.toString().substring(1);
				//load the label into $ra. safe because $ra will be backed up at beginning of func.
				memWrite = loadAddress(returnAddress, label, indentation);
				writeDest = returnAddress;
			} else if(globalMemRef.base instanceof VAddr.Var<?>) {
				//base address is a register.
				writeDest = globalMemRef.base.toString();
			} else {
				assert(false);
			}
			
		} else {
			//writing to the stack.
			VMemRef.Stack stackMemRef = (VMemRef.Stack)arg1.dest;
			int stackOffset = stackMemRef.index;
			//Which stack is this?
			if(stackMemRef.region == VMemRef.Stack.Region.In) {
				int framePointerOffset = returnOffsetFromFramePointer(stackMemRef.region, stackOffset);
				writeDest = framePointer;
				offset = framePointerOffset;
			} else {
				int stackPointerOffset = returnOffsetFromStackPointer(stackMemRef.region, stackOffset);
				writeDest = stackPointer;
				offset = stackPointerOffset;
			}			
		}
		
		//source can be literal, label, or register. load all operands into $t9
		String loadString = "";
		if(arg1.source instanceof VLitInt) {
			loadString = loadImmediate(freeReg, arg1.source.toString(), indentation);
		} else if(arg1.source instanceof VLabelRef<?>) {
			loadString = loadAddress(freeReg, arg1.source.toString().substring(1), indentation);
		} else if(arg1.source instanceof VVarRef){
			loadString = move(freeReg, arg1.source.toString(), indentation);
		} else {
			assert(false);
		}
		memWrite = concatentateInstructions(memWrite, loadString);
		String s = storeWord(freeReg, writeDest, offset, indentation);
		memWrite = concatentateInstructions(memWrite,s);
		return memWrite;
	}
	
	

	
	
	@Override
	public String visit(Integer indentation, VMemRead arg1) throws Throwable {
		//Refers to either global memory from the data seg or stack memory
		String memRead = "";
		String readDest = "";
		int offset = 0;
				
		if(arg1.source instanceof VMemRef.Global) {
			//Global memory. of form [base_address+offset] where 
			//base address = label ,or is a register
			VMemRef.Global globalMemRef = (VMemRef.Global)arg1.source; 
			offset = globalMemRef.byteOffset;
			if(globalMemRef.base instanceof VAddr.Label<?>) {
				//base address is a label
				String label = globalMemRef.base.toString().substring(1);
				//load the label into $ra. safe because $ra will be backed up at beginning of func.
				memRead = loadAddress(returnAddress, label, indentation);
				readDest = returnAddress;
			} else if(globalMemRef.base instanceof VAddr.Var<?>) {
				//base address is a register.
				readDest = globalMemRef.base.toString();
			} else {
				assert(false);
			}
					
		} else {
			//read from the stack.
			VMemRef.Stack stackMemRef = (VMemRef.Stack)arg1.source;
			int stackOffset = stackMemRef.index;
			//Which stack is this?
			if(stackMemRef.region == VMemRef.Stack.Region.In) {
				int framePointerOffset = returnOffsetFromFramePointer(stackMemRef.region, stackOffset);
				readDest = framePointer;
				offset = framePointerOffset;
			} else {
				int stackPointerOffset = returnOffsetFromStackPointer(stackMemRef.region, stackOffset);
				readDest = stackPointer;
				offset = stackPointerOffset;
			}			
		}
				
		//source can be literal, label, or register. load all operands into $t9
		String s = loadWord(arg1.dest.toString(), readDest, offset, indentation);
		memRead = concatentateInstructions(memRead,s);
	
		return memRead;
	}

	@Override
	public String visit(Integer indentation, VBranch arg1) throws Throwable {
		String branchString = "";
		//NOTE: make sure all labels dont have colon
		String label = arg1.target.toString().substring(1);
		String branchVal = "";
	
				
		if(arg1.value instanceof VLitInt) {
			//load the literal into $t9
			branchString = loadImmediate(freeReg, arg1.value.toString(), indentation);				//branch on val of $t9
			branchVal = freeReg;
		}  else if(arg1.value instanceof VVarRef){
			branchVal = arg1.value.toString();
		} else {
			assert(false);
		}
			
		if(arg1.positive) {
			//bneqz
			String s = branchOnNotEqualZero(branchVal, label, indentation);
			branchString = concatentateInstructions(branchString, s);
		} else {
			String s = branchOnEqualZero(branchVal, label, indentation);
			branchString = concatentateInstructions(branchString, s);
		}
		
		return branchString;
	}

	@Override
	public String visit(Integer indentation, VGoto arg1) throws Throwable {
		String label = arg1.target.toString().substring(1);
		return jump(label, indentation);
	}

	@Override
	public String visit(Integer indentation, VReturn arg1) throws Throwable {
		//Just return
		return "";
	}
	
	
    //Commonly used strings    
    public static String storeWord(String sourceReg,String destReg, int offset, Integer indentation) {
    	return getIndentation(indentation) + "sw " + sourceReg + " " + String.valueOf(offset) + "(" + destReg + ")";
    }
    
    public static String loadWord(String destReg,String sourceReg, int offset, Integer indentation) {
    	return getIndentation(indentation) + "lw " + destReg + " " + String.valueOf(offset) + "(" + sourceReg + ")";
    }
    
    public static String move(String destReg, String sourceReg, Integer indentation) {
    	return getIndentation(indentation) + "move " + destReg + " " + sourceReg; 
    }
    
    public static String subtract(String destReg, String sourceReg, String regToSubtractFromSource, Integer indentation) {
    	return getIndentation(indentation) + "subu " + destReg + " " + sourceReg + " " + regToSubtractFromSource;
    }
    
    public static String add(String destReg, String sourceReg, String regToAddToSource, Integer indentation) {
    	return getIndentation(indentation) + "addu " + destReg + " " + sourceReg + " " + regToAddToSource;
    }
    
    public static String multiply(String destReg, String opReg1, String opReg2, Integer indentation) {
    	return getIndentation(indentation) + "mul " + destReg + " " + opReg1 + " " + opReg2;
    }
    
    public static String loadImmediate(String destReg, String immediateVal, Integer indentation) {
    	return getIndentation(indentation) + "li " + destReg + " " + immediateVal; 
    }
    public static String jumpAndLink(String label, Integer indentation) {
    	return getIndentation(indentation) + "jal " + label;
    }
    public static String jumpAndLinkRegister(String register, Integer indentation) {
    	return getIndentation(indentation) + "jalr " + register;
    }
    public static String loadAddress(String destReg, String label, Integer indentation) {
    	return getIndentation(indentation) + "la " + destReg + " " + label; 
    }
    public static String branchOnNotEqualZero(String reg, String label, Integer indentation) {
    	return getIndentation(indentation) + "bnez " + reg + " " + label; 
    }
    public static String branchOnEqualZero(String reg, String label, Integer indentation) {
    	return getIndentation(indentation) + "beqz " + reg + " " + label; 
    }
    
    public static String jumpRegister(String destReg,Integer indentation) {
    	return getIndentation(indentation) + "jr " + destReg;
    }
    public static String jump(String label,Integer indentation) {
    	return getIndentation(indentation) + "j " + label;
    }
    
    public static String setLessThanReg(String destReg, String opReg1, String opReg2, Integer indentation) {
    	return getIndentation(indentation) + "slt " + destReg + " " + opReg1 + " " + opReg2;
    }
    public static String setLessThanImmediate(String destReg, String opReg1, String immediate, Integer indentation) {
    	return getIndentation(indentation) + "slti " + destReg + " " + opReg1 + " " + immediate;
    }
    
    public static String setLessThanUnsignedReg(String destReg, String opReg1, String opReg2, Integer indentation) {
    	return getIndentation(indentation) + "sltu " + destReg + " " + opReg1 + " " + opReg2;
    }
    public static String setLessThanUnsignedImmediate(String destReg, String opReg1, String immediate, Integer indentation) {
    	return getIndentation(indentation) + "sltiu " + destReg + " " + opReg1 + " " + immediate;
    }

	//Helper functions
    public static String getIndentation(Integer indentation) {
		String ret = "";
		for(int i = 0; i<indentation;i++) {
			ret += indentationSpacing;
		}
		return ret;
	}
	
	private static String concatentateInstructions(String v1, String v2, String...strings) {
		String concatentedString = v1 + "\n" + v2;
		for(String s:strings) {
		  concatentedString += "\n" + s;
	   }
		return concatentedString;
	}

	
	int returnOffsetFromStackPointer(VMemRef.Stack.Region regionType,int offset) {
		//Return the offset relative to the current stack pointer to get the offset
		int adjustedOffset = 0;
		if(regionType == VMemRef.Stack.Region.Local) {
			//local stack	
			adjustedOffset = numOut*4 + offset*4;
		} else if(regionType == VMemRef.Stack.Region.Out) {
			//out stack
			adjustedOffset = offset*4;
 		} else {
 			System.err.println("Can't handle in-stack here!");
 			System.exit(0);
 		}
		
		assert(adjustedOffset < bytesInStackFrame);
		return adjustedOffset;
	}
 
	
	int returnOffsetFromFramePointer(VMemRef.Stack.Region regionType,int offset) {
		int adjustedOffset = 0;
		if(regionType == VMemRef.Stack.Region.In) {
			//local stack	
			adjustedOffset = offset * 4;
 		} else {
 			System.err.println("Can't handle out-stack or local stack here!");
 			System.exit(0);
 		}
		return adjustedOffset;	
	}
    
  
}
