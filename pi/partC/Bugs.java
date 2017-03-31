

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.io.File;
import java.util.Scanner;
import java.util.Collections;
import java.text.DecimalFormat;

class Bugs {

	/* Constants that are determined by command line arguments in pipair */
	
	public static double T_SUPPORT = 3; //default support value
	public static double T_CONFIDENCE = 65; //default confidence value
	public static String filename; //name of bitecode file that is used


	/* Our main function. This function takes care of intializing our hashmaps
	*  and call other functions
	*/
	public static void main(String[] args) {

		/* hashmap that contains our functions and where they are used
		*  Example: A = {scope1, scope2, scope3}
		*/
		Map<String, List<String>> functions = new HashMap<String, List<String>>();
		

		/* hashmap that contains our accumulated pairs of functions and where they are used
		*  Example: {A,B} = {Scope1, scope2}
		*/
		Map<List<String>, List<String>> pairs = new HashMap<List<String>, List<String>>();


		/* hashmap that contains our callers and what functions they call
		*  Example: Scope1 = {A, B, scope2}
		*/
		Map<String, List<String>> callers = new HashMap<String, List<String>>();


		// Get command line arguments from user
		getCmndLineArguments(args);


		// Get the callgraph generated from pipair
		String input = readFromFile(filename);
	
		//split our input into lines
		String[] lines = input.split("\n");

		//Get all functions and their callers and put in their places 
		getScopeForFunctions(lines, callers, functions);

		//Generate all pairs of functions and the callers that call the pair
		generatePairs(callers, pairs);

		//Generate bugs by comparing callers of pairs and callers of functions
		generateBugs(pairs, functions);
	}



	/* This function uses the args array to find command line arguments input by the user,
	*  in this order: filename, T_SUPPORT, T_CONFIDENCE.
	*  If only the filename is clarified, the other variables stay default
	*/
	public static void getCmndLineArguments(String[] args) {

		//check first if filename is given
		if(args.length < 1) {
			System.out.println("You need to input the name of a bytecode file");
		}
		else {
			filename = args[0];

			//user needs to input BOTH support and confidence
			if(args.length == 3) {
				T_SUPPORT = Double.parseDouble(args[1]);
				T_CONFIDENCE = Double.parseDouble(args[2]);
			}
		}
	}

	/* This function takes care of generating the bugs for us. Before it's called we have generated
	*  all pairs and all call sites for both pairs and functions. We compare the support and the confidence
	*  and construct a string with all possible bugs
	*/
	public static void generateBugs(Map<List<String>, List<String>> pairs, Map<String, List<String>> functions) {

		double pairSupport = 0; //support for pairs
		double funcSupport = 0; //support for functions
		String firstKey; //first function in a pair
		String secondKey; // second function in a pair
		String bug; //our bug

		// list of functions where each pair is called
		List<String> pairList = new ArrayList<String>();

		//list of functions where each function is called
		List<String> funcList = new ArrayList<String>();

		//the string we construct that prints out all bugs
		String bugString = "";


		//iterate through our pairs hashmap
		for(Map.Entry<List<String>, List<String>> entry : pairs.entrySet()) {
			/* Here our support for a pair is just the size of the list of the places where
			*  it is called.
			*  We then don't do anything if that support is smaller than T_SUPPORT
			*/
			pairSupport = entry.getValue().size();
			if(pairSupport >= T_SUPPORT) {

				firstKey = entry.getKey().get(0); //first element of pair
				secondKey = entry.getKey().get(1); //second element of pair

				pairList = entry.getValue(); //list of functions where this pair is called

				//First we compare the callers of the first key in the pair, to the callers of the pair and generate bugs
				funcList = functions.get(firstKey); 
				funcSupport = funcList.size();

				/* To find where a function is called and a pair is not we create a temporary list
				*  from our function list and remove all the functions from our pair list
				*  What remains are functions that aren't in the pairlist
				*/
				List<String> temp = new ArrayList<String>(funcList);
				temp.removeAll(pairList);
				DecimalFormat df = new DecimalFormat("#.00");

				double confidence = (pairSupport/funcSupport) * 100;

				//here we construct our bugstring
				if(temp.size() > 0 && confidence >= T_CONFIDENCE) {
					bug = temp.get(0);
					bugString += "bug: " + firstKey + " in " + bug;
					bugString += ", pair: (" + firstKey + ", " + secondKey + "), ";
					bugString += "support: " + (int) pairSupport + ", confidence: ";
					bugString +=  (df.format(confidence) )  +  "%\n";
				}
				
				//Then we compare the callers of the second key in the pair, to the callers of the pair and generate bugs
				//same as above but with secondKey
				funcList = functions.get(secondKey);
				funcSupport = funcList.size();
				List<String> temp2 = new ArrayList<String>(funcList);
				temp2.removeAll(pairList);
				confidence = (pairSupport/funcSupport) * 100;

				//construct our bugstring
				if(temp2.size() > 0 && confidence >= T_CONFIDENCE) {
					bug = temp2.get(0);
					bugString += "bug: " + secondKey + " in " + bug;
					bugString += ", pair: (" + firstKey + ", " + secondKey + "), ";
					bugString += "support: " + (int) pairSupport + ", confidence: ";
					bugString += df.format(confidence) +  "%\n";
				}
				
			}
		}

		//in the end we simply print out our bugstring
		System.out.print(bugString);		
	}

	/* This function constructs our hashmap for storing functions and their callers and callees */
	public static void getScopeForFunctions(String[] lines, Map<String, List<String>> callers, Map<String, List<String>> functions) {
		
		String caller = "";

		//We've already split our input into line so we go through it line by line
		for(int i = 1; i < lines.length-1; i++) {
			String[] words = lines[i].split(" ");
			String last = words[words.length -1];
			String callee = "";

			//Get only the lines which contain information about functions and callees and put them in the correct hashmap
			if(lines[i].contains("function") && !(lines[i].contains("null")) && !(lines[i].contains("0x0"))) {
				//if  a line begins with "Call" we know its a caller
				if(lines[i].startsWith("Call")) {
					caller = lines[i].split("'")[1];
				}
				else {
					callee = last.split("'")[1];
					if(callers.get(caller) == null) {
						List<String> callees = new ArrayList<String>();
						callees.add(callee);
						callers.put(caller, callees);
					}
					else if(!callers.get(caller).contains(callee)) {
							callers.get(caller).add(callee);
					}
					if(functions.get(callee) == null) {
						List<String> func = new ArrayList<String>();
						func.add(caller);
						functions.put(callee, func);
					}
					else if(!functions.get(callee).contains(caller)) {
						functions.get(callee).add(caller);
					}
				}
			}
		}

	}


	/* This function generates all pairs that are called together in a function.
	*  For each pair we put an entry into a hashmap with the pair as the key
	*  And a list of callsites as the value
	*/
	public static void generatePairs(Map<String, List<String>> c, Map<List<String>, List<String>> pairs) {
		//Iterate through complete list of callers
		for(Map.Entry<String, List<String>> entry : c.entrySet()) {
			List<String> cList = entry.getValue();
			String caller = entry.getKey();
			if((cList.size() > 1)) {
				//Iterate through the values of each callees of given caller and accumulate a list of pairs
				for(int i = 0; i < cList.size() -1 ; i++) {
					for(int j = i+1; j < cList.size(); j++) {
						List<String> keys = new ArrayList<String>();
						keys.add(cList.get(i)); keys.add(cList.get(j));
						Collections.sort(keys);
						if(pairs.get(keys) == null) {
							List<String> callers = new ArrayList<String>();
							callers.add(caller);
							pairs.put(keys, callers);
						}
						else {
							pairs.get(keys).add(caller);
						}
					}
				}
			}
		}
	}



	/* This function reads input from a file with name given by the variable filepath.
	*  Its a simple scanner function similar to one found on StackOverflow:
	*  http://stackoverflow.com/a/25992945
	*/
	public static String readFromFile(String filepath) {
		
		String result = "";
		try {
			File file = new File(filepath);
			Scanner input = new Scanner(file);

			while(input.hasNextLine()){
				String line = input.nextLine() + "\n";
				result += line;
			}
			input.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		return result;
	}
}
