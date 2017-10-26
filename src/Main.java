import java.io.File;
import java.util.Scanner;
import Processor.APEX;
import Utilities.Utility;
import static Utilities.Constants.*;

public class Main {
	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("Instruction file name absent!!!");
			System.exit(1);
		}

		Scanner scan = new Scanner(System.in);
		File file = new File(args[0]);

		while(true) {
			Utility.displaySimulatorMenu();
			int option = scan.nextInt();

			switch (option) {
			case INITIALIZE:
				APEX.init(file);
				break;
				
			case SET_URF_SIZE:
				System.out.print("Enter size of registers : ");
				int n = scan.nextInt();
				APEX.setURFSize(n);
				break;

			case SIMULATE:
				System.out.print("Enter number of cycles : ");
				int cycleCount = scan.nextInt();
				APEX.simulate(cycleCount);
				break;

			case DISPLAY:
				APEX.displaySimulationResult();
				break;
				
			case PRINT_MAP_TABLES:
				APEX.printMapTables();
				break;
				
			case PRINT_IQ:
				APEX.printIQ();
				break;
				
			case PRINT_ROB:
				APEX.printROB();
				break;
				
			case PRINT_URF:
				APEX.printURF();
				break;
				
			case PRINT_MEMORY:
				System.out.print("Enter starting memory address: ");
				int a1=scan.nextInt();
				System.out.print("Enter end memory address: ");
				int a2=scan.nextInt();
				APEX.printMemory(a1,a2);
				break;
				
			case PRINT_STATS:
				APEX.printStats();
				APEX.displaySimulationResult();
				
				break;

			case EXIT:
				scan.close();
				System.exit(0);
				break;

			default:
				break;
			}
		}
	}

}
