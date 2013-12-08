import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import cs132.util.ProblemException;
import cs132.vapor.parser.VaporParser;
import cs132.vapor.ast.VBuiltIn;
import cs132.vapor.ast.VCodeLabel;
import cs132.vapor.ast.VDataSegment;
import cs132.vapor.ast.VFunction;
import cs132.vapor.ast.VInstr;
import cs132.vapor.ast.VOperand;
import cs132.vapor.ast.VReturn;
import cs132.vapor.ast.VaporProgram;
import cs132.vapor.ast.VBuiltIn.Op;

public class VM2M {
	public static void main(String[] args) {
		//V2VM.parseVapor(in, err)
		InputStream stream = null;
		if(args.length == 1) {
		   try {
			   stream = new FileInputStream(args[0]);
		   } catch (FileNotFoundException e) {
			   // TODO Auto-generated catch block
			   e.printStackTrace();
		   } 
		   
		} else {
			stream = System.in;
		}
		
		try {
			parseVapor(stream,System.err);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static VaporProgram parseVapor(InputStream in, PrintStream err)
			  throws IOException
			{
			  Op[] ops = {
			    Op.Add, Op.Sub, Op.MulS, Op.Eq, Op.Lt, Op.LtS,
			    Op.PrintIntS, Op.HeapAllocZ, Op.Error,
			  };
			  boolean allowLocals = false;
			  String[] registers = {
					    "v0", "v1",
					    "a0", "a1", "a2", "a3",
					    "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
					    "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
					    "t8",
					  };
			  boolean allowStack = true;
			  

			  VaporProgram program;
			  try {
			    program = VaporParser.run(new InputStreamReader(in), 1, 1,
			                              java.util.Arrays.asList(ops),
			                              allowLocals, registers, allowStack);
			  }
			  catch (ProblemException ex) {
			    err.println(ex.getMessage());
			    return null;
			  }
			  
			  String dataSection = ".data\n";
			  //Just print out exact data section from input vapor code

			  for(VDataSegment seg:program.dataSegments) {
				  String currentSection = seg.ident;
				  for(VOperand.Static data: seg.values) {
					  String label = data.toString();
					  if(label.startsWith(":"))
						  label = label.substring(1);
					  String dataString = getIndentation(1)+label;
					  currentSection = concatentateInstructions(currentSection, dataString);
				  }
				  dataSection = concatentateInstructions(dataSection, currentSection,"");
			  }
			  			  
			  String beginText = "\n.text\n  jal Main\n  li $v0 10\n  syscall\n";
			  ArrayList<String> listOfConstants = new ArrayList<String>();
			  String stringConstants = returnStringConstantsForProgram(program,listOfConstants);
			   //Go through functions now.
			  String functions = "";
			  for(VFunction func:program.functions) {
				  PrintAST printASTVisitor = new PrintAST(func,listOfConstants);
				  
				  //Function declaration
				  String funcDec = func.ident + ":\n";
	
				  //Create the start of the function
				  String functionStart = funcDec+printASTVisitor.returnIntro(1);
				  VCodeLabel []labels =  func.labels;

				  String body = "";
				  //Step through body
				  for(int i = 0; i < func.body.length; i++) {
					  VInstr instruction = func.body[i];
					  try {
						String mipsCode = instruction.accept(1,printASTVisitor);
						for(VCodeLabel l:labels) {
							if(l.instrIndex == i) {
								//Label refers to this instruction. Put it before the corresponding MIPs code
								body = concatentateInstructions(body, l.ident+":");
							}
						}		
						body = concatentateInstructions(body, mipsCode);
					} catch (Throwable e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					  
				  }
				  String functionEnd = printASTVisitor.returnOutro(1);
				  String currentFunction = concatentateInstructions(functionStart, body,functionEnd);
				  functions = concatentateInstructions(functions, currentFunction);

			  }
			  System.out.println(dataSection);
			  System.out.println(beginText);
			  System.out.println(functions);
			  System.out.println("\n"+PrintAST.returnBuiltInDefinitions());
			  System.out.println(stringConstants);
			  return program;
			}
	
	
	  
	
	public static String getIndentation(Integer indentation) {
		String ret = "";
		String indentationSpacing = "  ";
		for(int i = 0; i<indentation;i++) {
			ret += indentationSpacing;
		}
		return ret;
	}
	
	public static String concatentateInstructions(String v1, String v2, String...strings) {
		String concatentedString = v1 + "\n" + v2;
		for(String s:strings) {
		  concatentedString += "\n" + s;
	   }
		return concatentedString;
	}

	public static String returnStringConstantsForProgram(VaporProgram program, ArrayList<String> listOfConstants) {
		//Find all string constants used
		String constLabel = "_str";
		String stringConstants = ".data\n.align 0\n_newline: .asciiz \"\\n\"";
		LinkedHashSet<String> constants = new LinkedHashSet<String>();
		for(VFunction func:program.functions) {
			//If error, then check the string
			for(VInstr instr:func.body) {
				if(instr instanceof VBuiltIn) {
					VBuiltIn builtIn = (VBuiltIn)instr;
					String builtInOp = builtIn.op.name;
					if(builtInOp.equalsIgnoreCase("Error")) {
						String err = builtIn.args[0].toString();
						constants.add(err);
					}
				}
			}
		}
		
		for(String constant:constants) {
			listOfConstants.add(constant);
		}
		
		for(int i = 0; i < listOfConstants.size(); i++) {
			String label = constLabel + String.valueOf(i) + ":	.asciiz ";
			String s = listOfConstants.get(i);
			label += s.subSequence(0, s.length()-1) + "\\n\"";
			stringConstants = concatentateInstructions(stringConstants, label);
		}
		
		return stringConstants;
	}
	
}
