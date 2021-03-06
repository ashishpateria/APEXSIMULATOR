package Processor;

import static Utilities.Constants.*;

public enum FunctionalUnitType {

	INTEGER("Integer"),
	MULTIPLY("Multiply"),
	MEMORY("Memory"),
	BRANCH("Branch");

	private String value;

	FunctionalUnitType(String type) {
		this.value = type;
	}

	public String getValue() {
		return this.value;
	}

	public static FunctionalUnitType getFunctionalUnitType(String type) {
		switch (type) {
		case ADD_INSTRUCTION:
		case SUB_INSTRUCTION:
		case MOV_INSTRUCTION:
		case MOVC_INSTRUCTION:
		case EX_OR_INSTRUCTION:
		case AND_INSTRUCTION:
		case OR_INSTRUCTION:
		case HALT_INSTRUCTION:
		
			return INTEGER;

		case LOAD_INSTRUCTION:
		case STORE_INSTRUCTION:
			return MEMORY;

		case MUL_INSTRUCTION:
			return MULTIPLY;
			
		case BZ_INSTRUCTION:
		case BNZ_INSTRUCTION:
		case JUMP_INSTRUCTION:
		case BAL_INSTRUCTION:
			return BRANCH;

		default:
			break;
		}
		return null;
	}

}
