package Processor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import Utilities.CircularQueue;
import Utilities.FileIO;

import static Utilities.Constants.*;
import static Utilities.Utility.*;

public class APEX {
	private static Integer PC = 20000;
	private static Integer UPDATED_PC = 0;
	private static LinkedList<String> printQueue = new LinkedList<>();
	private static Map<String, PhysicalRegister> UNIFIED_REGISTER_FILE = new TreeMap<>();
	//private static Queue<Instruction> fetchFetchLatch = new LinkedList<>();
	private static Queue<Instruction> fetchDecodeLatch = new LinkedList<>();
	private static Queue<Instruction> decodeRenameLatch = new LinkedList<>();

	private static Queue<Instruction> issueQueueIntegerFuLatch = new LinkedList<>();
	private static Queue<Instruction> issueQueueMultiplyFuLatch = new LinkedList<>();
	private static Queue<Instruction> issueQueueMemoryFuLatch = new LinkedList<>();
	private static Queue<Instruction> issueQueueBranchFuLatch = new LinkedList<>();
	private static Queue<Instruction> memory1memory2Latch = new LinkedList<>();
	private static Queue<Instruction> integer1integer2Latch = new LinkedList<>();

	private static Queue<Instruction> memoryForwardingLatch = new LinkedList<>();
	private static Queue<Instruction> integerForwardingLatch = new LinkedList<>();
	private static Queue<Instruction> multiplyForwardingLatch = new LinkedList<>();
	private static Queue<Instruction> branchForwardingLatch = new LinkedList<>();
	private static List<String> instructionList = new ArrayList<>(PC);
	private static Integer[] MEMORY_ARRAY = new Integer[10000];
	private static boolean isFetchDone,isMultiplyFuFree, isLoadStoreFuFree, isBranchFUFree;
	private static boolean JUMP_DETECTED = false;
	private static boolean INVALID_PC = false;
	private static boolean HALT_ALERT;
	private static List<Instruction> LSQ = new ArrayList<>();
	private static List<Instruction> ISSUE_QUEUE = new ArrayList<>();
	public static Map<String, String> RENAME_TABLE = new HashMap<>();
	public static Map<String, String> RETIREMENT_RENAME_TABLE = new HashMap<>();

	private static CircularQueue ROB = new CircularQueue();
	private static int lastRobSlotID;
	private static boolean BRANCH_PREDICTED;

	private static String fetchStageValue;
	private static String decodeStage1Value;
	private static String decodeStage2Value;
	private static String executeStage1Value;
	private static String executeStage2Value;
	private static String multiplyStageValue;
	private static String memoryStage1Value;
	private static String memoryStage2Value;
	private static String branchingStageValue;
	private static String commitStageValue;

	public static List<String> printList = new ArrayList<String>();


	public static void init(File file) {
		echo("\nSet PC to 4000");
		PC = 4000;

		instructionList = FileIO.loadFile(file, PC);

		echo("Initialize Memory...");
		for(int i = 0; i < MEMORY_ARRAY.length; i++)
			MEMORY_ARRAY[i] = 0;

		echo("Initialize Register File ...");
		//R0 to R7
		for(int i=0; i<40; i++) {
			String regName = "P" + String.format("%02d",i);
		//	System.out.println(regName);
			PhysicalRegister pr= new PhysicalRegister();
			pr.setAllocated(0);
			pr.setValue(-1);
			UNIFIED_REGISTER_FILE.put(regName, pr);
		}



	/*	for(int i=0; i<15; i++){
			String regName="R"+i;
			RenameTableEntry rte = new RenameTableEntry();
			rte.setSrcBit(0);
			rte.setRegisterSrc(null);
			RENAME_TABLE.put(regName, rte);
			RETIREMENT_RENAME_TABLE.put(regName, rte);
		}*/

		//Add X register to Register file
		//UNIFIED_REGISTER_FILE.put("X", 0);
		RenameTableEntry rte = new RenameTableEntry();
		rte.setSrcBit(0);
		rte.setRegisterSrc(null);
		RENAME_TABLE.put("X", null);
		//RETIREMENT_RENAME_TABLE.put("X", 0);

		echo("Reset flags...");
		isMultiplyFuFree = true;
		isFetchDone = false;
		HALT_ALERT = false;
		JUMP_DETECTED = false;
		echo("\nSimulator state intialized successfully");
	}


	public static void simulate(int cycleCount) {
		int cycle = 0;
		LinkedList<String> tempList = new LinkedList<>();
		while(cycle != cycleCount) {
			if(INVALID_PC) {
				break;
			}

			doCommit();
			doForwarding();
			doExecution();
			decode2();
			doDecode1();
			doFetch();

			Integer k = cycle+1;
			StringBuilder sb = new StringBuilder();
//			sb.append("'" + inOrderPrint(k.toString()) + "'");
			sb.append("\n");
			sb.append("'"+ inOrderPrint(fetchStageValue)+"'");
			sb.append(inOrderPrint(decodeStage1Value) + "'");
			sb.append(inOrderPrint(decodeStage2Value) + "'");
			sb.append(inOrderPrint(executeStage1Value) + "'");
			sb.append(inOrderPrint(executeStage2Value) + "'");
			sb.append(inOrderPrint(multiplyStageValue) + "'");
			sb.append(inOrderPrint(memoryStage1Value) + "'");
			sb.append(inOrderPrint(memoryStage2Value) + "'");
			sb.append(inOrderPrint(branchingStageValue)+ "'");
			sb.append(inOrderPrint(commitStageValue) + "'");

//			System.out.println(sb);
//			System.out.println(DisplayStages.repeat("-", 170));
			printList.add(sb.toString()+"\n\n");
//			printList.add(Utility.repeat("-",170));



			while(!printQueue.isEmpty())
				tempList.add(printQueue.removeLast());

			cycle++;
		}

		printQueue.addAll(tempList);

		if(INVALID_PC) {
			//		displaySimulationResult();
	
			echo("\nSimulation ended due to bad PC value..." + PC);
			System.exit(0);
		}
	}



	private static void doFetch() {
		if(!fetchDecodeLatch.isEmpty() || HALT_ALERT) {
			printQueue.add("--");
			fetchStageValue = "--";
			return;
		}

		if(BRANCH_PREDICTED || JUMP_DETECTED) {
			if(PC < instructionList.size()) {
				String temp = instructionList.get(PC);
				Instruction instruction = getInstructionObject(temp);
				instruction.setPc(PC);
				instruction.setToBeSqaushed(true);
				instruction.setStringRepresentation(temp);
				fetchDecodeLatch.add(instruction);
				PC++;
				printQueue.add(temp);
				fetchStageValue = temp;
			} else {
				printQueue.add("--");
				fetchStageValue = "--";
			}

			if(BRANCH_PREDICTED) {
				PC = UPDATED_PC;
				BRANCH_PREDICTED = false;
			}

			if(JUMP_DETECTED)
				JUMP_DETECTED = false;

			return;
		}

		if(PC == instructionList.size()) {
			printQueue.add("--");
			fetchStageValue = "--";
			return;
		}


		if(PC > instructionList.size()) {//PC is updated by BZ/BNZ/JUMP instructions
			echo("Invalid PC value detected: " + PC);
			INVALID_PC = true;
			return;
		}

		String temp = instructionList.get(PC);
		Instruction instruction = getInstructionObject(temp);
		instruction.setPc(PC);
		fetchDecodeLatch.add(instruction);
		PC++;

		if(HALT_ALERT) {
			instruction.setToBeSqaushed(true);
		}

		instruction.setStringRepresentation(temp);
	//	System.out.println("FETCH"+instruction.getStringRepresentation());
		printQueue.add(temp);
		fetchStageValue = temp;
	}


	public static Instruction getInstructionObject(String instruction) {
		String[] parts = instruction.split(" ");
		InstructionType type = InstructionType.getInstructionType(instruction);

		Instruction instructObj = new Instruction();
		instructObj.setOpCode(type);
		instructObj.setFuType(FunctionalUnitType.getFunctionalUnitType(type.getValue()));



		if(parts.length > 1) {
			instructObj.setDestRegName(parts[1]);
			instructObj.setNoOfSources(0);
		}

		if(parts.length > 2) {
			instructObj.setSrc1RegName(parts[2]);
			instructObj.setNoOfSources(1);
		}

		if(parts.length > 3) {
			//	System.out.println("sssssssssssssssssssssssssssssssssssssssss");
			instructObj.setSrc2RegName(parts[3]);
			instructObj.setNoOfSources(2);
		}
		if(parts[0].equalsIgnoreCase("STORE"))
		{
			//		System.out.println("getInstructionObject() wwwwwwwwwwwwwwwwwwwwwwwwwwwwww");
			instructObj.setNoOfSources(3);
		}

		return instructObj;
	}

	private static void doDecode1()
	{
		if(fetchDecodeLatch.isEmpty()) {
			printQueue.add("--");
			decodeStage1Value = "--";
			return;
		}

		Instruction instruction = fetchDecodeLatch.poll();
		InstructionType instructionType = instruction.getOpCode();

		if(instruction.isToBeSqaushed()) {
			printQueue.add("--");
			decodeStage1Value = "--";
			return;
		}

		if(HALT_INSTRUCTION.equalsIgnoreCase(instructionType.getValue())) {
			HALT_ALERT = true;
			printQueue.add("HALT");
			decodeStage1Value = "HALT";
			return;
		}

		if(isIssueQueueFull()) {
			printQueue.add("IQ Full");
			decodeStage1Value = "IQ Full";
			fetchDecodeLatch.add(instruction);
			return;
		}


		switch (instruction.getOpCode()) {
			case MOVC:
				instruction.setSrc1Value(Integer.parseInt(instruction.getSrc1RegName()));
				instruction.setSrc1Ready(true);

				//	dispatch(instruction);
				break;
			case MOV:
			//	System.out.println("decode1 "+ instruction.getSrc1RegName());
				decodeSources(instruction, true, false,false);
				break;
			case ADD:
			case SUB:
			case AND:
			case OR:
			case MUL:
				//	System.out.println(instruction.getStringRepresentation());
				//	System.out.println("CASE MuL");
				decodeSources(instruction, true, true,false);

				//	dispatch(instruction);

				break;

			case BZ:
			case BNZ:
				break;

			case LOAD:
				decodeSources(instruction, true, true, false);
				break;
			case STORE:
				decodeSources(instruction, true, true, true);
				break;


		}
		decodeRenameLatch.add(instruction);
		decodeStage1Value = instruction.getStringRepresentation();

	}

	public static void decodeSources(Instruction instruction,boolean decodeSrc1,boolean decodeSrc2,boolean decodeDest)
	{
		if(decodeSrc1) {
			String src1 = instruction.getSrc1RegName();
		//	System.out.println("Check renmae " + src1);
			if(isLiteral(src1)) {
				instruction.setSrc1Value(Integer.parseInt(src1));
				instruction.setSrc1Ready(true);
			} else
			{
//				System.out.println("REGISTER "+instruction.getSrc1RegName());
//				System.out.println(instruction.getStringRepresentation());
				String pr = RENAME_TABLE.get(instruction.getSrc1RegName());
//				System.out.println("pgfgwerwe"+pr);
				instruction.setSrc1RegName(pr);
				int srcValue = 0;
				PhysicalRegister p = UNIFIED_REGISTER_FILE.get(pr);
				srcValue= p.getValue();
				if(srcValue!=-1)
				{instruction.setSrc1Value(srcValue);
					instruction.setSrc1Ready(true);}
			}
		}
		if(decodeSrc2) {
			String src2 = instruction.getSrc2RegName();
			if(isLiteral(src2)) {
				instruction.setSrc2Value(Integer.parseInt(src2));
				instruction.setSrc2Ready(true);
			} else {
				String pr = RENAME_TABLE.get(instruction.getSrc2RegName());
				instruction.setSrc2RegName(pr);
				int srcValue = 0;
				PhysicalRegister p = UNIFIED_REGISTER_FILE.get(pr);
				srcValue= p.getValue();
				if(srcValue!=-1)
				{instruction.setSrc2Value(srcValue);
					instruction.setSrc2Ready(true);}
			}
		}
		if(decodeDest) {
			//RenameTableEntry regDestRte = RENAME_TABLE.get(instruction.getDestRegName());

			String dest = instruction.getDestRegName();
//			System.out.println("decodesrc3 " + dest);
			if(isLiteral(dest)) {
				instruction.setDestinationValue(Integer.parseInt(dest));
			} else {
				String pr = RENAME_TABLE.get(instruction.getDestRegName());
				instruction.setDestRegName(pr);
				int srcValue = 0;
				PhysicalRegister p = UNIFIED_REGISTER_FILE.get(pr);
				srcValue= p.getValue();
				if(srcValue!=-1)
				{	instruction.setDestinationValue(srcValue);
					instruction.setDestReady(true);
				}
//					System.out.println("Store else"+instruction.getDestRegName());
			}
		}
	}

	private static boolean isLiteral(String value) {
		return (value.charAt(0) != 'R' && value.charAt(0) != 'X');
	}

	private static boolean isIssueQueueFull() {
		return ISSUE_QUEUE.size() == MAX_ISSUE_QUEUE_SIZE;
	}


	public static void echo(String data) {
		System.out.println(data);
	}

	public static void dispatch(Instruction instruction)
	{
		int lastRobSlotID = ROB.getNextSlotIndex();
		instruction.setRobSlotId(lastRobSlotID);
		ROB.add(instruction);
		ISSUE_QUEUE.add(instruction);


	}

	public static void decode2()
	{
		if(decodeRenameLatch.isEmpty()) {
			printQueue.add("--");
			decodeStage2Value = "--";
			return;
		}

		Instruction instruction = decodeRenameLatch.poll();
		InstructionType instructionType = instruction.getOpCode();

		if(instruction.isToBeSqaushed()) {
			printQueue.add("--");
			decodeStage2Value = "--";
			return;
		}

		if(HALT_INSTRUCTION.equalsIgnoreCase(instructionType.getValue())) {
			HALT_ALERT = true;
			printQueue.add("HALT");
			decodeStage2Value = "HALT";
			return;
		}

		if(isIssueQueueFull()) {
			printQueue.add("IQ Full");
			decodeStage2Value = "IQ Full";
			decodeRenameLatch.add(instruction);
			return;
		}


		switch (instruction.getOpCode()) {
			case MOVC:
				updateRenameTable(instruction, RENAME_TABLE);
				String dest=instruction.getDestRegName();
				String pr=getMapping(dest);
				instruction.setDestRegName(pr);
				//System.out.println(instruction.getDestRegName());
				//System.out.println(instruction.getSrc1Value());
				if(instruction.isSrc1Ready()==true)
				{
					instruction.setValid(true);
				}
				printQueue.add(instruction.getStringRepresentation());
				decodeStage2Value = instruction.getStringRepresentation();
				dispatch(instruction);
				break;

			case MOV:
				updateRenameTable(instruction, RENAME_TABLE);
				String dest2=instruction.getDestRegName();
				String pr2=getMapping(dest2);
				instruction.setDestRegName(pr2);
				//System.out.println(instruction.getDestRegName());
				//System.out.println(instruction.getSrc1Value());
				instruction.setSrc2Ready(true);
				if(instruction.isSrc1Ready()==true)
				{
					instruction.setValid(true);
				}
				printQueue.add(instruction.getStringRepresentation());
				decodeStage2Value = instruction.getStringRepresentation();
				dispatch(instruction);
				break;
			case ADD:
			case SUB:
			case AND:
			case OR:
			case MUL:
				updateRenameTable(instruction, RENAME_TABLE);
				String dest1=instruction.getDestRegName();
				String pr1=getMapping(dest1);
				instruction.setDestRegName(pr1);
			//	System.out.println(instruction.getStringRepresentation()+"decode2");
				//System.out.println("REname add"+instruction.getDestRegName());
				//System.out.println(instruction.getSrc1Value());
				if(instruction.isSrc1Ready() && instruction.isSrc2Ready()) {
					instruction.setValid(true);
				}
				printQueue.add(instruction.getStringRepresentation());
				decodeStage2Value = instruction.getStringRepresentation();
				dispatch(instruction);

				break;

			case BZ:
			case BNZ:
			//	System.out.println("BRANCH------------------------>"+instruction.getStringRepresentation());
				instruction.setSrc1Value(lastRobSlotID);
				instruction.setSrc1Ready(false);
				instruction.setValid(false);
				printQueue.add(instruction.getStringRepresentation());
				decodeStage2Value = instruction.getStringRepresentation();
				dispatch(instruction);
				break;

			case LOAD:
				updateRenameTable(instruction, RENAME_TABLE);
				String des=instruction.getDestRegName();
				String ph=getMapping(des);
				instruction.setDestRegName(ph);
				//		System.out.println(instruction.getStringRepresentation()+"decode2");
				if(instruction.isSrc1Ready() && instruction.isSrc2Ready()) {
					instruction.setValid(true);
				}
				printQueue.add(instruction.getStringRepresentation());
				decodeStage2Value = instruction.getStringRepresentation();
				dispatch(instruction);
				break;
			case STORE:
		//		System.out.println("decode2() STORE");
				if(instruction.isDestReady()&&instruction.isSrc1Ready() && instruction.isSrc2Ready()) {
		//			System.out.println("set all true");
					instruction.setValid(true);
				}
				printQueue.add(instruction.getStringRepresentation());
				decodeStage2Value = instruction.getStringRepresentation();
				dispatch(instruction);

				break;

		}




	}


	public static void updateRenameTable(Instruction instruction, Map<String, String> RENAME_TABLE )
	{
		String dest=instruction.getDestRegName();
		String physicalRegister=nextAvailablePhysicalRegister(RENAME_TABLE);
//		System.out.println(physicalRegister);

		RENAME_TABLE.put(dest,physicalRegister);
//		System.out.println("Rename R to pr-->"+ RENAME_TABLE.get(dest));


	}

	public static void updateRetirementRenameTable(Map<String, String> RENAME_TABLE,Map<String, String> RETIREMENT_RENAME_TABLE,Instruction instruction )
	{
		String dest=instruction.getStringRepresentation().split(SPACE)[1];
		String pr=RENAME_TABLE.get(dest);
		String register=RETIREMENT_RENAME_TABLE.get(dest);
		if(register==null)
			RETIREMENT_RENAME_TABLE.put(dest, pr);
		else
		{
//			System.out.println("NEW RENM FOUND!11111111111");
			PhysicalRegister entry =UNIFIED_REGISTER_FILE.get(register);
//			entry.setAllocated(0);
//			System.out.println("OLD REGISTER _______________________>>>>>>>"+ register +"   "+ entry.getAllocated());
			RETIREMENT_RENAME_TABLE.remove(dest);

			RETIREMENT_RENAME_TABLE.put(dest,pr);
		}
	}


	public static String nextAvailablePhysicalRegister(Map<String, String> RENAME_TABLE)
	{


		Iterator it = UNIFIED_REGISTER_FILE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, PhysicalRegister> pair = (Map.Entry<String, PhysicalRegister>)it.next();

			if (pair.getValue().getAllocated() == 0) {

				pair.getValue().setAllocated(1);
				return pair.getKey();
			}
		}
		return null;

//		for (Map.Entry<String, PhysicalRegister> entry : UNIFIED_REGISTER_FILE.entrySet()) {
//		    String key = entry.getKey();
//
//		    PhysicalRegister pr = entry.getValue();
//
//		  //  System.out.println(key+"  "+value);
//		    if(pr.getAllocated()==0)
//		    {
//		    	pr.setAllocated(1);
//		    	return key;
//
//		    }
//
//		}
//		    return null;
	}

	public static String getMapping(String dest)
	{
		return RENAME_TABLE.get(dest);
	}


	private static void doExecution() {
		//System.out.println("hello");
		doSelection();
		executeMemory2();
		executeMemory1();
		executeMultiply();
		executeInteger2();
		executeInteger1();
		executeBranch();
	}

	private static void doSelection() {
		removeSquashInstructionsFromIQ();

		Instruction integerInstruction = selectIntegerInstructionFromIQ();
		Instruction multiplyInstruction = selectMultiplyInstructionFromIQ();
		Instruction memoryInstruction = selectLoadStoreForExecutionFromIQ();
		Instruction branchInstruction = selectBranchForExecutionFromIQ();

		if(integerInstruction != null) {
			//Go for execution
			issueQueueIntegerFuLatch.add(integerInstruction);
		}

		if(multiplyInstruction != null) {
//			//Go for execution
			issueQueueMultiplyFuLatch.add(multiplyInstruction);
		}
//
		if(memoryInstruction != null) {
			//Go for execution
			issueQueueMemoryFuLatch.add(memoryInstruction);
		}

		if(branchInstruction != null) {
			//Go for execution
			issueQueueBranchFuLatch.add(branchInstruction);
		}

	}


	private static Instruction selectIntegerInstructionFromIQ() {
		int instIndex = -1;
		Instruction selectedInstruction = null;

		for(int i = 0; i < ISSUE_QUEUE.size(); i++) {
			Instruction inst = ISSUE_QUEUE.get(i);
			String fuType = inst.getFuType().getValue();

			if(inst.isValid() && INTEGER_FU.equalsIgnoreCase(fuType)) {
				instIndex = i;
				break;
			}
		}

		if(instIndex != -1) {
			selectedInstruction = ISSUE_QUEUE.remove(instIndex);
		}

		return selectedInstruction;
	}

	private static Instruction selectMultiplyInstructionFromIQ() {
		int instIndex = -1;
		Instruction selectedInstruction = null;
		//System.out.println("MULTILY SELECTED:");
		for(int i = 0; i < ISSUE_QUEUE.size(); i++) {
			Instruction inst = ISSUE_QUEUE.get(i);
			String fuType = inst.getFuType().getValue();

			if(inst.isValid() && MULTIPLY_FU.equalsIgnoreCase(fuType)) {
				instIndex = i;
				break;
			}
		}

		if(instIndex != -1) {
			selectedInstruction = ISSUE_QUEUE.remove(instIndex);
		}

		return selectedInstruction;
	}

	private static void executeInteger1()
	{
		if(issueQueueIntegerFuLatch.isEmpty()) {
			printQueue.add("--");
			executeStage1Value = "--";
			return;
		}


		Instruction intInst = issueQueueIntegerFuLatch.poll();
		integer1integer2Latch.add(intInst);
		printQueue.add(intInst.getStringRepresentation());
		executeStage1Value = intInst.getStringRepresentation();

	}

	private static void executeInteger2()
	{
		if(integer1integer2Latch.isEmpty()) {
			printQueue.add("--");
			executeStage2Value = "--";
			return;
		}

		Instruction intInst = integer1integer2Latch.poll();
		String opCode = intInst.getOpCode().getValue();
		int result = 0;
		switch (opCode) {
			case MOV_INSTRUCTION:
			case MOVC_INSTRUCTION:
				result = intInst.getSrc1Value() + 0;
				break;
			case ADD_INSTRUCTION:
		//		System.out.println("In here");
				result = intInst.getSrc1Value() + intInst.getSrc2Value();
				break;
			case SUB_INSTRUCTION:
				result = intInst.getSrc1Value() - intInst.getSrc2Value();
				break;
			case AND_INSTRUCTION:
				result = intInst.getSrc1Value() & intInst.getSrc2Value();
				break;
			case OR_INSTRUCTION:
				result = intInst.getSrc1Value() | intInst.getSrc2Value();
				break;
			case EX_OR_INSTRUCTION:
				result = intInst.getSrc1Value() ^ intInst.getSrc2Value();
				break;

			default:
				break;
		}

		intInst.setDestinationValue(result);
		intInst.setDestReady(true);
		intInst.setValid(true);
		//	System.out.println("Result-->"+ intInst.getDestinationValue());
		String dest=intInst.getStringRepresentation().split(SPACE)[1];
		//	System.out.println("Destinatiom"+dest);
		String reg= RENAME_TABLE.get(dest);
		PhysicalRegister pr=UNIFIED_REGISTER_FILE.get(intInst.getDestRegName());
		//	System.out.println(pr.getValue());
		pr.setValue(result);
	//	System.out.println("value"+pr.getValue());

		integerForwardingLatch.add(intInst);
		printQueue.add(intInst.getStringRepresentation());
		executeStage2Value = intInst.getStringRepresentation();
//		System.out.println(intInst.getStringRepresentation());
//		System.out.println(intInst.getDestRegName()+" " + intInst.getSrc1RegName()+" "+intInst.getSrc2RegName());


	}


	public static void doCommit()
	{
//		System.out.println("----------------cycle----------------");
		int headInstructionId = ROB.getHeadIndex();
		boolean isReadyToCommit = false;
		for(Instruction inst : ROB) {
			if(inst.getRobSlotId() == headInstructionId) {
				//Cannot remove entry from ROB while iterating
				//So set a flag and remove entry at head out of iteration
				isReadyToCommit = inst.isDestReady();
				break;
			}
		}

		if(isReadyToCommit) {
			Instruction instruction = ROB.remove();
			//if(isBranchInstruction(instruction)) {
			//		printQueue.add("--");
			//} else
			{
				int destValue = instruction.getDestinationValue();
				String destRegName = instruction.getDestRegName();

				//Dealloc
				List<String> l1 = new ArrayList<>();
				List<String> l2 = new ArrayList<>();
				Iterator it = RENAME_TABLE.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> pair = (Map.Entry<String, String>)it.next();
					l1.add(pair.getValue());
				}
				Iterator it2 = RETIREMENT_RENAME_TABLE.entrySet().iterator();
				while (it2.hasNext()) {
					Map.Entry<String, String> pair = (Map.Entry<String, String>)it2.next();
					l2.add(pair.getValue());
				}
				for (String x : l1){
					if (!l2.contains(x))
						l2.add(x);
				}

				for (String z: l2
						) {
			//		System.out.println(z + "Intersection9092eawjbdiuwhdwbd");
				}

				Iterator itZ = UNIFIED_REGISTER_FILE.entrySet().iterator();
				while (itZ.hasNext()) {
					Map.Entry<String, PhysicalRegister> pair = (Map.Entry<String, PhysicalRegister>)itZ.next();
					if(!l2.contains(pair.getKey()))
						pair.getValue().setAllocated(0);
				}
				//Dealloc


				updateRetirementRenameTable(RENAME_TABLE, RETIREMENT_RENAME_TABLE, instruction);
//				System.out.println("COMMITED");
//				updateRenameTable(instruction, RENAME_TABLE, true);
//				if(STORE_INSTRUCTION.equalsIgnoreCase(instruction.getOpCode().getValue())) {
//					printQueue.add("--");
//				} else {
					printQueue.add(destRegName + " = " + destValue);
				commitStageValue = destRegName + " = " + destValue;
//				}
			}
		}
		else {
			printQueue.add("--");
			commitStageValue = "--";
		}
	}

	private static void doForwarding() {
		if(!memoryForwardingLatch.isEmpty()) {
				forwardExecutionResults(memoryForwardingLatch.poll());
		} else if(!integerForwardingLatch.isEmpty()) {
	//			System.out.println("IN frwdng latch");
			forwardExecutionResults(integerForwardingLatch.poll());
		} else if(!multiplyForwardingLatch.isEmpty()) {
				forwardExecutionResults(multiplyForwardingLatch.poll());
		} else if(!branchForwardingLatch.isEmpty()){
				forwardExecutionResults(branchForwardingLatch.poll());
		}
	}

	private static void forwardExecutionResults(Instruction instruction){
		for(Instruction I: ISSUE_QUEUE ){
			if(instruction.getDestRegName().equals(I.getSrc1RegName())){
				I.setSrc1Value(instruction.getDestinationValue());
			//	System.out.println("Frwd value1 -->" +I.getSrc1Value());
				I.setSrc1Ready(true);
			}
			if(instruction.getDestRegName().equals(I.getSrc2RegName())){
				I.setSrc2Value(instruction.getDestinationValue());
		//		System.out.println("Frwd value2 -->" +I.getSrc2Value());
				I.setSrc2Ready(true);
			}
//			for(Instruction robInst: ROB){
//				if(!robInst.isSrc1Ready()) {
//					if(robInst.getSrc1Value() == instruction.getRobSlotId()) {
//						robInst.setSrc1Value(instruction.getDestinationValue());
//					//	robInst.setSrc1Ready(true);
//					//	robInst.setValid(true);
//					}
//				 }
//			}
			if(I.getOpCode().getValue().equals(STORE_INSTRUCTION)){
		//		System.out.println("------------xxxxxxxxxxxxxxxxxxxx---------->IN store forward");
		//		System.out.println("instruction" + instruction.getStringRepresentation());
		//		System.out.println("IQ instruction"  + I.getStringRepresentation());
		//		System.out.println("===================================="+I.getDestRegName()+I.getNoOfSources());
				if(instruction.getDestRegName().equals(I.getDestRegName())){
					int value=instruction.getDestinationValue();
					if(value!=-1)
					{
						I.setDestinationValue(instruction.getDestinationValue());
		//				System.out.println("Frwd destvalue -->" +I.getDestinationValue());
						I.setDestReady(true);
						//	I.setValid(true);
					}

				}
			}

//				if(I.getNoOfSources()==0||I.getNoOfSources()==1){
//					if(I.isSrc1Ready()==true){
//						System.out.println("Probably branch!!");
//						I.setValid(true);
//					}
//				}
//				if(I.getNoOfSources()==2){
//					if(I.isSrc1Ready()==true && I.isSrc2Ready()==true && I.isDestReady()==true){
//						System.out.println("Set Valid");
//						I.setValid(true);
//					}
//				}
//			}
//			else{
//					if(I.isSrc1Ready()==true && I.isSrc2Ready()==true && I.isDestReady()==true){
//						System.out.println("Set Valid");
//						I.setValid(true);
//					}
//			}
//

			if(I.getNoOfSources()==0||I.getNoOfSources()==1)
			{

				if(I.isSrc1Ready()==true)
				{
				//	System.out.println("Probably branch!!");
					I.setValid(true);
				}
			}
			else if(I.getNoOfSources()==2){
				if(I.isSrc1Ready()==true && I.isSrc2Ready()==true){
			//		System.out.println("Set Valid");
					I.setValid(true);
				}
			}
//			else if(I.getNoOfSources()==3)
//			{
//				System.out.println("Only srewttwegkjgrjsgkjdthrvjrdk");
//			}
			else{
				if(I.isSrc1Ready()==true && I.isSrc2Ready()==true && I.isDestReady()==true){
		//			System.out.println("Set Valid");
					I.setValid(true);
				}
			}
		}
	}

	private static void executeMultiply() {
		if(issueQueueMultiplyFuLatch.isEmpty()) {
			printQueue.add("--");
			multiplyStageValue = "--";
			return;
		}
//		System.out.println("Execute Multiply");
		Instruction mulInst = issueQueueMultiplyFuLatch.poll();
		if(mulInst.isToBeSqaushed()) {
			printQueue.add("--");
			multiplyStageValue = "--";
			return;
		}

		int latencyCount = mulInst.getMultiplyLatencyCount();
		int result=0;
		if(latencyCount == 0) {
			isMultiplyFuFree = false;
			result = mulInst.getSrc1Value() * mulInst.getSrc2Value();
			mulInst.setDestinationValue(result);
		}

		latencyCount++;
		mulInst.setMultiplyLatencyCount(latencyCount);
		if(latencyCount == 4) {
			isMultiplyFuFree = true;
			mulInst.setDestReady(true);
			mulInst.setValid(true);
			multiplyForwardingLatch.add(mulInst);

			//	System.out.println("Result-->"+ mulInst.getDestinationValue());
			String dest=mulInst.getStringRepresentation().split(SPACE)[1];
			//	System.out.println("Destinatiom"+dest);
			String reg= RENAME_TABLE.get(dest);
			PhysicalRegister pr=UNIFIED_REGISTER_FILE.get(mulInst.getDestRegName());
			//	System.out.println(pr.getValue());
			pr.setValue(mulInst.getDestinationValue());
			//	System.out.println(pr.getValue());



		} else {
			issueQueueMultiplyFuLatch.add(mulInst);
		}

		printQueue.add(mulInst.getStringRepresentation());
		multiplyStageValue = mulInst.getStringRepresentation();
	}

	public static void printRename(Map<String, String> RENAME_TABLE)
	{
		for (Map.Entry<String, String> entry : RENAME_TABLE.entrySet()) {
			String key = entry.getKey();
			String pr =entry.getValue();
			System.out.println("ARCH"+" " +key+"Physical "+ " "+pr);

		}


	}

	private static boolean isBranchInstruction(Instruction instruction) {
		String opCode = instruction.getOpCode().getValue();
		return (BZ_INSTRUCTION.equalsIgnoreCase(opCode)
				|| BNZ_INSTRUCTION.equalsIgnoreCase(opCode)
				|| JUMP_INSTRUCTION.equalsIgnoreCase(opCode));
	}

	private static Instruction selectBranchForExecutionFromIQ() {
		int instIndex = -1;
		Instruction selectedInstruction = null;
		for(int i = 0; i < ISSUE_QUEUE.size(); i++) {
			Instruction inst = ISSUE_QUEUE.get(i);
			String fuType = inst.getFuType().getValue();
			if(inst.isValid() && BRANCH_FU.equalsIgnoreCase(fuType)) {
				instIndex = i;
				break;
			}
		}

		if(instIndex != -1) {
			selectedInstruction = ISSUE_QUEUE.remove(instIndex);
		}

		return selectedInstruction;
	}

	private static void executeBranch() {
		// TODO Auto-generated method stub
		if(issueQueueBranchFuLatch.isEmpty()) {
			printQueue.add("--");
			branchingStageValue = "--";
			return;
		}

		Instruction intInst = issueQueueBranchFuLatch.poll();
		if(intInst.isToBeSqaushed()) {
			printQueue.add("--");
			branchingStageValue = "--";
			return;
		}

		String opCode = intInst.getOpCode().getValue();
		int result = 0;
//		System.out.println("In brnch======================" + intInst.getSrc1Value());

		switch(opCode){
			case BZ_INSTRUCTION:
				if(intInst.getSrc1Value() == 0) {

					//Flush wrongly fetched instructions
					flushWrongPredictedInstructions(intInst);
					PC = intInst.getPc() + Integer.parseInt(intInst.getDestRegName());
					BRANCH_PREDICTED=true;
				}
				break;

			case BNZ_INSTRUCTION:
				if(intInst.getSrc1Value() != 0) {
					flushWrongPredictedInstructions(intInst);
					PC = intInst.getPc() + Integer.parseInt(intInst.getDestRegName());
					BRANCH_PREDICTED=true;
				}
				break;

			case JUMP_INSTRUCTION:
				PC = intInst.getSrc1Value() + Integer.parseInt(intInst.getSrc1RegName());
				break;

			case BAL_INSTRUCTION:
				PC = intInst.getSrc1Value() + Integer.parseInt(intInst.getSrc1RegName());
				result = intInst.getPc(); // current PC value has to be saved in X register (will be done in commit)
				intInst.setDestRegName("X");
				break;

			default:
				break;
		}

		intInst.setDestinationValue(result);
		intInst.setDestReady(true);
		intInst.setValid(true);

		branchForwardingLatch.add(intInst);
		printQueue.add(intInst.getStringRepresentation());
		branchingStageValue = intInst.getStringRepresentation();

	}
	private static void flushWrongPredictedInstructions(Instruction branchInstruction) {
		int bzInsRobId = branchInstruction.getRobSlotId();
		bzInsRobId = incrementCircularQueueIndex(bzInsRobId);

		for(Instruction robInst : ROB) {
			if(bzInsRobId == ROB.getNextSlotIndex()) {
				break;
			}

			if(robInst.getRobSlotId() == bzInsRobId) {
				robInst.setToBeSqaushed(true);
			}

			bzInsRobId = incrementCircularQueueIndex(bzInsRobId);
		}

		//Clear instructions that are already fetched

		for(Instruction i : fetchDecodeLatch) {
			i.setToBeSqaushed(true);
		}
	}
	private static int incrementCircularQueueIndex(int currIndex) {
		if(currIndex == (ROB.capacity() - 1)) {
			return 0;
		}

		return ++currIndex;
	}

	private static void removeSquashInstructionsFromIQ() {
		List<Integer> instToBeSquashed = new ArrayList<>();
		for(int i = 0; i < ISSUE_QUEUE.size(); i++) {
			Instruction inst = ISSUE_QUEUE.get(i);
			if(inst.isToBeSqaushed()) {
				instToBeSquashed.add(i);
			}
		}

		for(Integer i : instToBeSquashed) {
			ISSUE_QUEUE.remove(i);
		}
	}

	public static void displaySimulationResult() {
		// TODO Auto-generated method stub
		display(printList, UNIFIED_REGISTER_FILE, MEMORY_ARRAY,ISSUE_QUEUE,ROB);
	}

	public static void setURFSize(int n){
		UNIFIED_REGISTER_FILE.clear();
		for(int i=0; i<n; i++) {
			String regName = "P" + String.format("%02d",i);
	//		System.out.println(regName);
			PhysicalRegister pr= new PhysicalRegister();
			pr.setAllocated(0);
			pr.setValue(-1);
			UNIFIED_REGISTER_FILE.put(regName, pr);
		}
	}

	public static void printMapTables(){
		print_Map_Tables(RENAME_TABLE,RETIREMENT_RENAME_TABLE);
	}

	public static void printIQ(){
		print_IQ(ISSUE_QUEUE);
	}

	public static void printROB(){
		print_ROB(ROB);
	}

	public static void printURF(){
		print_URF(UNIFIED_REGISTER_FILE);
	}

	public static void printMemory(int a1,int a2){
		int mem1=a1;
		int mem2=a2;
		print_Memory(MEMORY_ARRAY,mem1,mem2);
	}

	public static void printStats(){

	}

	private static void executeMemory1() {
		if(issueQueueMemoryFuLatch.isEmpty()) {
		//	System.out.println("mem2Latch empty");
			printQueue.add("Stall");
			memoryStage1Value = "--";
			return;
		}

		Instruction loadStoreInst = issueQueueMemoryFuLatch.poll();
	//	System.out.println("executeMemory1() "+loadStoreInst.getStringRepresentation());
		if(loadStoreInst.isToBeSqaushed()) {
			printQueue.add("--");
			memoryStage1Value = "--";
			return;
		}



		memory1memory2Latch.add(loadStoreInst);
		printQueue.add(loadStoreInst.getStringRepresentation());
		memoryStage1Value = loadStoreInst.getStringRepresentation();
	}

	private static void executeMemory2() {
		if(memory1memory2Latch.isEmpty()) {
			//System.out.println("empty memory1 latch");
			printQueue.add("Stall");
			memoryStage2Value = "--";
			return;
		}

		Instruction loadStoreInst = memory1memory2Latch.poll();
	//	System.out.println("----->executeMemory2() "+loadStoreInst.getStringRepresentation());
		if(loadStoreInst.isToBeSqaushed()) {
			printQueue.add("--");
			memoryStage2Value = "--";
			return;
		}
		//	if(LOAD_INSTRUCTION.equalsIgnoreCase(loadStoreInst.getOpCode().getValue())){
		//	loadStoreInst.setDestReady(true);
		//	loadStoreInst.setValid(true);
		//	}
		if(LOAD_INSTRUCTION.equalsIgnoreCase(loadStoreInst.getOpCode().getValue())) {
			int src1 = loadStoreInst.getSrc1Value();
			int src2 = loadStoreInst.getSrc2Value();
			int addr = src1 + src2;
			int value = MEMORY_ARRAY[addr];
			loadStoreInst.setDestinationValue(value);

			//String dest=loadStoreInst.getDestRegName();

			String dest=loadStoreInst.getStringRepresentation().split(SPACE)[1];
			String reg= RENAME_TABLE.get(dest);
			PhysicalRegister pr=UNIFIED_REGISTER_FILE.get(reg);
			pr.setValue(value);
			//	System.out.println("LOAD value :"+pr.getValue());

			//UNIFIED_REGISTER_FILE.put//Update/Insert to Register file
			memoryForwardingLatch.add(loadStoreInst);
		} else {//Store instruction
			int src1 = loadStoreInst.getSrc1Value();
			int src2 = loadStoreInst.getSrc2Value();

			int addr = src1 + src2;
		//	System.out.println("mem2() src1 "+src1);
		//	System.out.println("mem2() src2 "+src2);
		//	System.out.println("mem2() result "+loadStoreInst.getDestinationValue());
			MEMORY_ARRAY[addr] = loadStoreInst.getDestinationValue();//Update/Insert memory location
			//	System.out.println("Stored Memory Value :"+MEMORY_ARRAY[addr]);
			//	System.out.println(loadStoreInst.getDestRegName()+ loadStoreInst.getSrc1RegName()+loadStoreInst.getSrc2RegName());
			//No forwarding required in case of Store
		}

		printQueue.add(loadStoreInst.getStringRepresentation());
		memoryStage2Value = loadStoreInst.getStringRepresentation();
	}

	private static Instruction selectLoadStoreForExecutionFromIQ() {

		int instIndex = -1;

		Instruction selectedInstruction = null;
		for(int i = 0; i < ISSUE_QUEUE.size(); i++) {
			//		System.out.println("IQ for store/load");
			Instruction inst = ISSUE_QUEUE.get(i);
			//		System.out.println(inst.getStringRepresentation());
			String fuType = inst.getFuType().getValue();
			//		System.out.println("futype " + fuType);
			//		System.out.println(inst.isValid());
			if(inst.isValid() && MEMORY_FU.equalsIgnoreCase(fuType)) {
			//	System.out.println("true");
				instIndex = i;
				break;
			}
		}

		if(instIndex != -1) {
			selectedInstruction = ISSUE_QUEUE.remove(instIndex);
		}
	//	System.out.println();
		return selectedInstruction;

	}

}