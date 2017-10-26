package Utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import Processor.FunctionalUnitType;
import Processor.Instruction;
import Processor.InstructionType;
import Processor.PhysicalRegister;
import Processor.RenameTableEntry;

public class Utility {

    public static void display(List<String> printList, Map<String, PhysicalRegister> registerFileMap, Integer[] memoryArray,
                               List<Instruction> issueQueue, CircularQueue rob) {
        displayHeader();
//        int size = printQueue.size();
//        int noOfCycles = size / 9;
//
//        for(int i = 0; i < noOfCycles; i++) {
//            System.out.print("|" + inOrderPrint(printQueue.poll()) + "|");
//            System.out.print(inOrderPrint(printQueue.poll()) + "|");
//            System.out.print(inOrderPrint(printQueue.poll()) + "|");
//            System.out.print(inOrderPrint(printQueue.poll()) + "|");
//            System.out.print(inOrderPrint(printQueue.poll()) + "|");
//            System.out.print(inOrderPrint(printQueue.poll()) + "|");
//            System.out.print(inOrderPrint(printQueue.poll()) + "|");
//            System.out.print(inOrderPrint(printQueue.poll()) + "|");
//            System.out.println(inOrderPrint(printQueue.poll()) + "|");
//            System.out.println(repeat("-", 153));
//        }

        for (String str: printList
             ) {
            System.out.println(str);
        }
        if(issueQueue.isEmpty()) {
            System.out.println("\nAll instructions processed from IQ.");
        } else {
            System.out.println("\nInstructions pending in IQ");
            boolean isPresent = false;
            for(Instruction iqEntry : issueQueue) {
                if(!iqEntry.isToBeSqaushed()) {
                    System.out.println(iqEntry.getStringRepresentation());
                    isPresent = true;
                }
            }
            if(!isPresent) {
                System.out.println("\nAll instructions processed from IQ.");
            }
        }


        if(rob.isEmpty()) {
            System.out.println("\nAll instructions processed from RoB");
        } else {
            System.out.println("\nInstructions pending in RoB for commit");
            boolean isPresent = false;
            for(Instruction instruction : rob) {
                if(!instruction.isToBeSqaushed()) {
                    System.out.println(instruction.getStringRepresentation());
                    isPresent = true;
                }
            }
            if(!isPresent) {
                System.out.println("\nAll instructions processed from RoB");
            }
        }

//        System.out.println("\nRegister Content");
//        Iterator it = registerFileMap.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry<String, PhysicalRegister> pair = (Map.Entry<String, PhysicalRegister>)it.next();
//            System.out.println(pair.getKey()+" "+pair.getValue().getValue());
//        }

        System.out.println("\n\nMemory Content");
        for(int i = 0; i < 10; i++) {
            System.out.print(inOrderPrint(String.valueOf(i), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i]), 5) + ",");
            System.out.print(inOrderPrint(String.valueOf(i + 10), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i + 10]), 5) + ",");
            System.out.print(inOrderPrint(String.valueOf(i + 20), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i + 20]), 5) + ",");
            System.out.print(inOrderPrint(String.valueOf(i + 30), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i + 30]), 5) + ",");
            System.out.print(inOrderPrint(String.valueOf(i + 40), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i + 40]), 5) + ",");
            System.out.print(inOrderPrint(String.valueOf(i + 50), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i + 50]), 5) + ",");
            System.out.print(inOrderPrint(String.valueOf(i + 60), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i + 60]), 5) + ",");
            System.out.print(inOrderPrint(String.valueOf(i + 70), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i + 70]), 5) + ",");
            System.out.print(inOrderPrint(String.valueOf(i + 80), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i + 80]), 5) + ",");
            System.out.println(inOrderPrint(String.valueOf(i + 90), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i + 90]), 5));
        }
    }

    public static void displaySimulatorMenu() {
        System.out.print("\nAPEX Simulator Menu:\n1.  INITIALIZE\n2.  SET_URF_SIZE\n3.  SIMULATE\n4.  DISPLAY\n5.  PRINT_MAP_TABLES\n6.  PRINT_IQ\n7.  PRINT_ROB\n8.  PRINT_URF\n9.  PRINT_MEMORY\n10. PRINT_STATS\n11. EXIT\nEnter your choice: ");
    }

    //display menu methods newly added
    public static void print_Map_Tables(Map<String, String> renameTable,Map<String, String> registerAliasTable){

        System.out.println("\nFront rename table: ");
        for (Map.Entry<String, String> entry : renameTable.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
        }

        System.out.println("\nBack-end register alias table: ");
        for (Map.Entry<String, String> entry : registerAliasTable.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
        }
    }

    public static void print_IQ(List<Instruction> issueQueue){
        if(issueQueue.isEmpty()) {
            System.out.println("\nAll instructions processed from IQ.");
        } else {
            System.out.println("\nInstructions pending in IQ");
            boolean isPresent = false;
            for(Instruction iqEntry : issueQueue) {
                if(!iqEntry.isToBeSqaushed()) {
                    System.out.println(iqEntry.getStringRepresentation());
                    isPresent = true;
                }
            }
            if(!isPresent) {
                System.out.println("\nAll instructions processed from IQ.");
            }
        }

    }

    public static void print_ROB(CircularQueue rob){
        if(rob.isEmpty()) {
            System.out.println("\nAll instructions processed from RoB");
        } else {
            System.out.println("\nInstructions pending in RoB for commit");
            boolean isPresent = false;
            for(Instruction instruction : rob) {
                if(!instruction.isToBeSqaushed()) {
                    System.out.println(instruction.getStringRepresentation());
                    isPresent = true;
                }
            }
            if(!isPresent) {
                System.out.println("\nAll instructions processed from RoB");
            }
        }
    }

    public static void print_URF(Map<String, PhysicalRegister> URF){


        List<String> registerNames = new ArrayList<>();
        registerNames.addAll(URF.keySet());

        for (Map.Entry<String, PhysicalRegister> entry : URF.entrySet()) {
            PhysicalRegister pr = entry.getValue();
            System.out.println("Key: " + entry.getKey() + ". Value: " + pr.getValue() + ". Allocated: " + pr.getAllocated());
        }
    }



    public static void print_Memory(Integer[] memoryArray,int a1,int a2){
        System.out.println("\n\nMemory Content");
        for(int i = a1; i <= a2; i++) {
            System.out.print(inOrderPrint(String.valueOf(i), 5) + ":" + inOrderPrint(String.valueOf(memoryArray[i]), 5) + "|");
        }
    }

    public static void print_Stats(){

    }

    //end

//    public static void displayHeader() {
//        System.out.println(repeat("-", 153));
//        System.out.println("|" + inOrderPrint("FETCH") + "|" + inOrderPrint("DECODE1")+ "|" + inOrderPrint("DECODE2") + "|" +
//                inOrderPrint("ALU") + "|" + inOrderPrint("MUL") + "|" + inOrderPrint("BRANCH") + "|" + inOrderPrint("LSFU") + "|" +
//                inOrderPrint("MEMORY1")+ "|" + inOrderPrint("Commit") + "|");
//        System.out.println(repeat("-", 153));
//    }

    public static void displayHeader() {
//        System.out.println(repeat("-", 170));
        System.out.println("#" + inOrderPrint("FETCH") + "#" + inOrderPrint("DECODE - 1") + "#" + inOrderPrint("DECODE - 2") + "#" + inOrderPrint("EXECUTE-1") + "#" +
                inOrderPrint("EXECUTE-2") + "#" + inOrderPrint("MULTIPLY") + "#" + inOrderPrint("MEMORY-1") + "#" +  inOrderPrint("MEMORY-2") + "#" + inOrderPrint("BRANCHING") + "#" +
                inOrderPrint("Commit") + "#");
//        System.out.println(repeat("-", 170));
    }

    public static String repeat(String data, int numberOfRepeatation) {
        String temp = data;
        for(int i = 0; i < numberOfRepeatation; i++)
            temp += data;
        return temp;
    }

    public static String inOrderPrint(String data, int space) {
        int dataLen = data.length();
        int extraSpace = space - dataLen;
        if(extraSpace <= 0 )
            return data;

        int padding = extraSpace / 2;
        String formattedData = "";
        for(int i = 0; i < padding; i++) {
            formattedData += " ";
        }
        formattedData += data;

        int newLength = formattedData.length();
        for(int i = newLength; i < space; i++) {
            formattedData += " ";
        }

        return formattedData;
    }

    public static String inOrderPrint(String data) {
        int space = 16;
        return inOrderPrint(data, space);
    }

    public static void echo(String data) {
        System.out.println(data);
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
            instructObj.setSrc2RegName(parts[3]);
            instructObj.setNoOfSources(2);
        }

        return instructObj;
    }



    public static void dispatchToRob(Instruction instruction, CircularQueue ROB) {
        int lastRobSlotID = ROB.getNextSlotIndex();
        instruction.setRobSlotId(lastRobSlotID);
        ROB.add(instruction);
    }

    public static void dispatchToIQ(Instruction instruction, List<Instruction> ISSUE_QUEUE) {
        ISSUE_QUEUE.add(instruction);
    }


    public static Instruction getEntryFromROBBySlotId(int slotId, CircularQueue ROB) {
        Iterator<Instruction> iterator = ROB.iterator();

        while(iterator.hasNext()) {
            Instruction instruction = iterator.next();
            if(instruction.getRobSlotId() == slotId) {
                return instruction;
            }
        }
        return null;
    }

    public static void updateRenameTable(Instruction instruction, Map<String, RenameTableEntry> RENAME_TABLE, boolean isCommitted) {
        RenameTableEntry entry = RENAME_TABLE.get(instruction.getDestRegName());
        if(isCommitted) {
            //src_bit = 0
            entry.setSrcBit(0);
            entry.setRegisterSrc(null);
        } else {
            //src_bit = 1
            //src = ROB_Slot_ID
            entry.setSrcBit(1);
            entry.setRegisterSrc(String.valueOf(instruction.getRobSlotId()));
        }
    }
}