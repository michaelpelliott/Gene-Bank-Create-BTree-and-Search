import java.io.File;
import java.util.*;

/**
 * GeneBankCreateBTree Creates a BTree out of a user specified .gbk file.
 * 
 * @author Michael Elliott, Clayton Fields, Jackson Edwards initial date:
 *         4/20/2020
 */


public class GeneBankCreateBTree {
	static final int BLOCKSIZE = 4096;
	static final int CHILDBYTES = 4; /* size of child pointer in a node, in bytes */
	static final int TREEBYTES = 12; /* size of TreeObject pointer in a node, in bytes */
	static final int NODEMETADATA = 25; /* accurate as of 4/29 */
	private static int k;
	public static void main(String[] args) {
		if (args.length == 4 || args.length == 5 || args.length == 6) {
			try {
				// if cache == 0, no cache; if cache == 1, use a cache
				int cache = Integer.parseInt(args[0]);
				int cacheSize=0;
				int debug=0;
				Cache c = null;
				boolean useCache = (cache==1);
				boolean dump = false;
				if(useCache) {
					if(args.length < 5) {
						usage();
					}
					else {
						cacheSize = Integer.parseInt(args[4]);
					}
				}
				int k = Integer.parseInt(args[3]);
				if (k <= 0 || k >= 32) {
					usage();
				}
				if (cache != 0 && cache != 1) { // t = 1 is a binary search tree,
					usage();
				}
				// degree of the tree
				int degree = Integer.parseInt(args[1]);
				if (degree < 0 || degree == 1) {
					usage();
				}
				if (degree == 0) {
					degree = optimalDegree();
				}
				if(args.length != 4) {
					if (args.length == 5) {
						if (cache == 0) { /* it's debug level that is being used */
							debug = Integer.parseInt(args[4]);
							if (debug != 1 && debug != 0) {
								usage();
							}
							dump=false;
						} else { /* it's cache size being used */
							cacheSize = Integer.parseInt(args[4]);
							if (cacheSize <= 0) {
								usage();
							} else {
								useCache = true;
							}
						}
					} 
					else if (args.length == 6) { /* args.length == 6 */
						/* both cache size AND debug level used */
						debug = Integer.parseInt(args[4]);
						if (debug != 1) {
							usage();
						}
						dump = true;
						cacheSize = Integer.parseInt(args[5]);
						if (cacheSize <= 0) {
							usage();
						}
						useCache = true;				
					}
				}
				
				
				
				
				
				/*
				 * Parse .gbk file into a BTree of subsequence represented by a long
				 */
				File gbkFile = new File(args[2]); // .gbk file being read
				String fileName = args[2]+".data.btree."+k+"."+degree;
				BTree tree = new BTree(degree, fileName, k);
				Scanner scan = new Scanner(gbkFile);
				String line = "";
				if(useCache) {
					c = new Cache(cacheSize);
				}
				while (scan.hasNextLine()) {//scan lines until origin is found
					scan.useDelimiter("\\s|\\n");
					if(scan.hasNext()) {
						line = scan.next();	
						if(line.equalsIgnoreCase("ORIGIN")) {
							line = scan.nextLine();
							scan.useDelimiter("");//Hopefully this will set scanner to get all the characters
							LinkedList<String> seqList = new LinkedList<String>();
						
							boolean full = false;
							int i = 0;
							String x = "";
							while(scan.hasNext() && !x.equals("/")) {//Iterate through until "/" is reached
								x = scan.next();
								x = x.toLowerCase();
								boolean readyToInsert = false;
								if(x.equalsIgnoreCase("n")) {//Clear seq if "n" is next()
									seqList.clear();
									full = false;
									readyToInsert = false;
									i=0;
								}
								boolean firstFlag = true; /*first time through, don't scan.next() below */
								if(!full) {//Fill seq list if it isn't full
									
									while(scan.hasNext() && !full) {
										/* x already has a value in it the first time it gets here */
										if(firstFlag == false) {
											x = scan.next();
											x = x.toLowerCase();
										}
										firstFlag = false;
										if(x.equalsIgnoreCase("a") || x.equalsIgnoreCase("c") || x.equalsIgnoreCase("g") || x.equalsIgnoreCase("t")) {
										seqList.add(i,x);
										i++;
										}else if(x.equalsIgnoreCase("n")) {
											seqList.clear();
											full = false;
											i=0;
										}
										if(seqList.size()==k) {
											full=true;
											readyToInsert=true;
										}
									}
								}
								else {//List is full, remove first base and add next base
									if(x.equalsIgnoreCase("a") || x.equalsIgnoreCase("c") || x.equalsIgnoreCase("g") || x.equalsIgnoreCase("t")) {
									seqList.removeFirst();
									seqList.addLast(x);
									readyToInsert=true;
									}
								}
								if(readyToInsert) {//List contains subsequence for insert
									String seqString = listToString(seqList);
									long seqLong = stringToLong(seqString);
									TreeObject key = new TreeObject(seqLong);
									if(useCache) {
										tree.insert(key,c);
									}else {
										tree.insert(key);
									}
								}
							}
						}
					}
					else {
						scan.nextLine();
					}
				}
				scan.close();
				

				/* Print results to console or dump file */
				if(debug==0) {
					tree.traverseTreePrint();
				} else {
					tree.traverseTreeDump();
				}
				/* the last thing we do before exiting CreateBTree is write the footer to the file */
				tree.writeFooter();


			} catch (Exception e) {
				// this print message is for troubleshooting, can be deleted before turn in
				System.err.println(e.toString());
				e.printStackTrace();
				usage();
			}
		} else {
			usage();
		}
	}

	/**
	 * usage() prints out a helpful usage message and exits the program
	 * 
	 * @return usage of the program
	 */
	private static void usage() {
		String str = "java GeneBankCreateBTree <0/1(no/with Cache)> <degree> <gbk file> "
				+ "<sequence length>  [<cache size>] [<debug level>]";
		System.err.println(str);
		System.exit(1);
	}

	/**
	 * optimalDegree() calculates the optimal (minimal) degree for t considering a
	 * block of memory is 4096 bytes. All values are in units of bytes. Note: this
	 * relies upon integer division taking the floor
	 * 
	 * @return t: optimal degree for the BTree
	 */
	private static int optimalDegree() {
		int t = (BLOCKSIZE - NODEMETADATA + TREEBYTES - CHILDBYTES) / (2 * (TREEBYTES + CHILDBYTES));
		return t;
	}
	
	/**
	 * listToString() converts a list of strings representing dna bases to a single string
	 * @param list: LinkedList<String> 
	 * @return String containg all of the bases
	 */
	private static String listToString(LinkedList<String> list) {
		StringBuilder str = new StringBuilder();
		for(int i=0;i<list.size();i++) {
			str.append(list.get(i));
		}
		return str.toString();
		
	}
	
	/**
	 * stringToLong() converts a dna substring to long for storage in BTree
	 * @param seq: String representing a dna substring
	 * @return long representing a dna substring
	 */
	private static long stringToLong(String seq) {
		StringBuilder str = new StringBuilder();
		for(int i=0; i<seq.length();i++) {
			if(seq.charAt(i)=='a') {
				str.append("00");
			}
			else if(seq.charAt(i)=='t') {
				str.append("11");
			}
			else if(seq.charAt(i)=='c') {
				str.append("01");
			}
			else if(seq.charAt(i)=='g') {
				str.append("10");

			}
		}
		StringBuilder str2 = new StringBuilder(removeLeadingZeros(str.toString()));
		long retVal = 0;
		int len = str2.length();
		String subStr;
		if(len == 1) {
			subStr = str2.substring(len-1,len);
			int temp = Integer.parseInt(subStr);
			if(temp == 0) {
				retVal = 0;
			}
			else if(temp == 1) {
				retVal = 1;
			}
			else {
				System.err.println("\n\tOur stringToLong method don't work so good\n");
			}
		}
		else {
			retVal = (long)Math.pow(2,len-1); // most significant digit
			int counter = 1;
			for (int i = len-2; i >= 0; i--) {
				subStr = str2.substring(counter,counter+1);
				int temp = Integer.parseInt(subStr);
				if(temp == 1) {
					retVal += (long) Math.pow(2,i);
				}
				counter++;
			}
		}
		return retVal;	
	}

	/**
	 *  Removes leading zero's from a String of binary characters
	 * @param inStr
	 * @return
	 */
	private static String removeLeadingZeros(String inStr) {
        int i = 0;
        while( i < inStr.length() && inStr.charAt(i) == '0') {
            i++;
        }
        StringBuffer buf = new StringBuffer(inStr);
        buf.replace(0,i,"");
        return buf.toString();
    }	
}
