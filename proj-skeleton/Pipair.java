

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.io.File;
import java.util.Scanner;
import java.util.Collections;

class Pipair {
	public static double T_SUPPORT = 3;
	public static double T_CONFIDENCE = 65;
	public static String filename;

	public static void main(String[] args) {
		//hashmap that contains our functions and their uses

		Map<String, List<String>> functions = new HashMap<String, List<String>>();
		Map<List<String>, List<String>> pairs = new HashMap<List<String>, List<String>>();
		Map<String, List<String>> callers = new HashMap<String, List<String>>();

		if(args.length < 1) {
			System.out.println("You need to input bytecode file");
		}
		else {
			filename = args[0];
			if(args.length == 3) {
				T_SUPPORT = Double.parseDouble(args[1]);
				T_CONFIDENCE = Double.parseDouble(args[2]);
			}
		}
		//get the callgraph generated by opt
		String input = readFromFile(filename);
	
		//split our input into lines
		String[] lines = input.split("\n");

		getScopeForFunctions(lines, callers, functions);
		
		generatePairs(callers, pairs);
		generateBugs(pairs, functions);
	}

	public static void generateBugs(Map<List<String>, List<String>> pairs, Map<String, List<String>> functions) {

		double pairSupport = 0;
		double funcSupport = 0;
		String firstKey;
		String secondKey;
		String bug;
		List<String> pairList = new ArrayList<String>();
		List<String> funcList = new ArrayList<String>();

		String bugString = "";

		for(Map.Entry<List<String>, List<String>> entry : pairs.entrySet()) {
			pairSupport = entry.getValue().size();
			if(pairSupport >= T_SUPPORT) {
				firstKey = entry.getKey().get(0);
				secondKey = entry.getKey().get(1);

				pairList = entry.getValue();

				//first check firstkey
				funcList = functions.get(firstKey);
				funcSupport = funcList.size();
				List<String> temp = new ArrayList<String>(funcList);
				temp.removeAll(pairList);
				double confidence = (pairSupport/funcSupport) * 100;
				if(temp.size() > 0 && confidence >= T_CONFIDENCE && funcSupport >= T_SUPPORT) {
					bug = temp.get(0);
					bugString += "Bug: " + firstKey + "in " + bug;
					bugString += ", pair: (" + firstKey + ", " + secondKey + "), ";
					bugString += "support: " + (int) pairSupport + ", confidence: ";
					bugString +=  (confidence )  +  "%\n";
				}
				
				//then secondKey
				funcList = functions.get(secondKey);
				funcSupport = funcList.size();
				List<String> temp2 = new ArrayList<String>(funcList);
				temp2.removeAll(pairList);
				confidence = (pairSupport/funcSupport) * 100;
				if(temp2.size() > 0 && confidence >= T_CONFIDENCE && funcSupport >= T_SUPPORT) {
					bug = temp2.get(0);
					bugString += "Bug: " + secondKey + "in " + bug;
					bugString += ", pair: (" + firstKey + ", " + secondKey + "), ";
					bugString += "support: " + (int) pairSupport + ", confidence: ";
					bugString += (confidence) +  "%\n";
				}
				
			}
		}
		System.out.println(bugString);		
	}
	public static void getScopeForFunctions(String[] lines, Map<String, List<String>> callers, Map<String, List<String>> functions) {
		
		String caller = "";

		for(int i = 1; i < lines.length-1; i++) {
			String[] words = lines[i].split(" ");
			String last = words[words.length -1];
			String callee = "";
			int calls = 0;

			if(lines[i].contains("function") && !(lines[i].contains("null")) && !(lines[i].contains("0x0"))) {
				if(lines[i].startsWith("Call")) {
					caller = lines[i].split("'")[1];
				}
				else {
					callee = last;
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


	public static void generatePairs(Map<String, List<String>> c, Map<List<String>, List<String>> pairs) {
		for(Map.Entry<String, List<String>> entry : c.entrySet()) {
			List<String> cList = entry.getValue();
			String caller = entry.getKey();
			if((cList.size() > 1)) {
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