import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Scanner;



/**
 * GeneBankSearch class
 * Searchs a gene bank BTree file. 
 * Always keep the root in local memory.
 * @author: Michael Elliott, Clayton Fields, Jackson Edwards
 * initial date: 4/20/2020
 */
public class GeneBankSearch {
    private static final String MODE = "rw"; // for RandomAccessFile, r, rw, rws, rwd are the options
    private static final int TREEMETADATA = 12;
    private static int sequenceLength;

    public static void main(String[] args) {
        if (args.length == 3 || args.length == 4 || args.length == 5) {
            try {
                int debug = -1;// -1 is used to catch default behavior if debug is not specified, then debug ==
                               // 0;
                boolean useCache = false;
                int cacheSize = 0;
                int cache = Integer.parseInt(args[0]);
                boolean debugBool = false; /* assume 0 debug, print to stdout */
                if (cache == 0) {
                    useCache = false;
                } else if (cache == 1) {
                    useCache = true;
                } else {
                    usage();
                }
                String bTreeName = args[1]; // used to detect degree
                File bTreeFile = new File(bTreeName);
                String queryName = args[2]; // used to detect degree
                File queryFile = new File(queryName);
                if (args.length > 3) {
                    if (args.length == 4) {
                        if (cache == 0) {
                            debug = Integer.parseInt(args[3]);
                            if (debug == 0) {

                            } else if (debug == 1) {
                                debugBool = true;
                            } else { // debug != 0 or 1
                                usage();
                            }
                        } else {
                            // cache == 1
                            useCache = true;
                            cacheSize = Integer.parseInt(args[3]);
                            if (cacheSize <= 0) {
                                usage();
                            }
                        }
                    } else { // args.length == 5
                        debug = Integer.parseInt(args[3]);
                        if (debug == 0) {
                        } else if (debug == 1) {
                            debugBool = true;
                        } else {
                            usage();
                        }
                        cacheSize = Integer.parseInt(args[4]);
                        if (cacheSize <= 0) {
                            usage();
                        }
                        useCache = true;
                    }
                }
                if (debug == -1) {
                    debug = 0;
                    debugBool = false;
                }
                sequenceLength = sequenceLengthMatch(bTreeName, queryName);
                if (sequenceLength < 0) {
                    System.err.println("sequenceLengthMatch returned -1");
                    usage();
                }
                
                /* bTreeFile : queryFile */
                RandomAccessFile treeRAF = new RandomAccessFile(bTreeFile,MODE);
                FileInputStream queryStream = new FileInputStream(queryFile);
                /* set file pointer location to the footer */
                treeRAF.seek(treeRAF.length()-TREEMETADATA);   
                ByteBuffer byteBuf = ByteBuffer.allocate(TREEMETADATA);
                /* read footer */
                treeRAF.read(byteBuf.array());
                
                int rootLoc = byteBuf.getInt(); 
                int treeDegree = byteBuf.getInt();
                int treeSeqLen = byteBuf.getInt();
                byteBuf.clear();
                /* build root node from rootLoc */
                BTreeNode root = new BTreeNode(treeDegree,0);
                treeRAF.seek(rootLoc);
                root.readNode(treeRAF); 
                if(treeSeqLen != sequenceLength) {
                    System.err.println("sequenceLength does not equal treeDegree read from file!!!!!");
                }
                BTree bTree = new BTree(treeDegree, args[1], treeSeqLen);
                bTree.setRoot(root);

                Cache c = new Cache(cacheSize);              
                Scanner scan = new Scanner(queryStream);
                while(scan.hasNext()) {
                    String seq = scan.next();
                    int freq;
                    Long convertedToLong = stringToLong(seq);
                    if(useCache) {
                    	freq = bTree.search(root,convertedToLong,c); 
                    } else {
                    	freq = bTree.search(root,convertedToLong); 
                    }

                    if(freq != 0) {
                        if(debug == 0) {
                            System.out.println(seq.toLowerCase() + ": " + freq); 
                        }
                        else if(debug == 1){
                            System.out.println("We have not implemented this...");
                        }
                    }

                }

                scan.close();
                treeRAF.close();
                queryStream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.toString());
                usage();
            }
        }
        else {
            usage();
        }

    }
    /**
     * usage() prints out a helpful usage message
     * and exits the program
     * @return usage of the program
     */
    private static void usage() {
        String str = "java GeneBankSearch <0/1(no/with Cache)> <btree file> <query file> " +
            "[<cache size>] [<debug level>]";
        System.err.println(str);
        System.exit(1);
    }
    /**
     * Ensures that the two files have identical sequence lengths (k values)
     * so as to be compatible
     * If either file name breaks conventions (k <= 0 or k >= 32) method
     * prints a usage message.
     * @param bTreeName name of BTree file, format: xyz.gbk.btree.data.k.t
     * @param queryName name of query file, format: queryk
     * @return -1 if not compatible, else returns the sequence length
     */
    private static int sequenceLengthMatch(String bTreeName, String queryName) {
        int retVal = -1;
        StringBuffer bTreeNameBuf = new StringBuffer(bTreeName);
        StringBuffer queryNameBuf = new StringBuffer(queryName);
        int queryLen = 0;   // size of query sequences
        int bTreeLen = 0;   // k from bTreeName
        bTreeNameBuf.reverse(); // now the buffer appears: t.k.atad..... we can access k easier, it's at position 2
        if(queryNameBuf.length() == 6 ){
            char queryLenChar = queryNameBuf.charAt(5);
            queryLen = Character.getNumericValue(queryLenChar);  
        } 
        else if( queryNameBuf.length() == 7) {
            char queryLenChar1 = queryNameBuf.charAt(5);
            char queryLenChar2 = queryNameBuf.charAt(6);
            StringBuilder lenStrBuild = new StringBuilder();
            lenStrBuild.append(queryLenChar1);
            lenStrBuild.append(queryLenChar2);
            String lenStr = lenStrBuild.toString();
            queryLen = Integer.parseInt(lenStr);
        } else {
            usage();
        }
        if(queryLen <= 0 || queryLen >= 32) { // probably shouldn't have made it this far if this is true
            usage();
        }
        /* for btree file name: get charAt(3), if it's '.',  0 <= k <= 9, else k > 9 */
        char bTreeNumChar3 = bTreeNameBuf.charAt(3);
        char bTreeNumChar4 = bTreeNameBuf.charAt(4);
        if(Character.toString(bTreeNumChar3).equals(".")) {
            char ch = bTreeNameBuf.charAt(2);
            bTreeLen = Character.getNumericValue(ch);
        }
        else if(Character.toString(bTreeNumChar4).equals(".")) {
            char ch1 = bTreeNameBuf.charAt(2);
            char ch2 = bTreeNameBuf.charAt(3);
            StringBuilder bLenStrBuild = new StringBuilder();
            bLenStrBuild.append(ch1);
            bLenStrBuild.append(ch2);
            String bLenStr = bLenStrBuild.toString();
            bTreeLen = Integer.parseInt(bLenStr);
        }
        else {
            usage();
        }
        if(bTreeLen <= 0 || bTreeLen >= 32) { // probably shouldn't have made it this far if this is the true
            usage();
        }
        if(bTreeLen != queryLen) {
            System.err.println("BTreeFile sequence length must be equal to query file sequence length!");
            usage();
        } 
        else {
            retVal = bTreeLen;
        }
        return retVal;
    }
    
    /**
	 * stringToLong() converts a dna substring to long for storage in BTree
	 * @param seq: String representing a dna substring
	 * @return long representing a dna substring
	 */
	private static long stringToLong(String seq) {
        StringBuilder str = new StringBuilder();
        seq = seq.toLowerCase();
		for(int i=0; i<seq.length();i++) {
			if(seq.charAt(i) == 'a') {
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
		return Long.parseUnsignedLong(str.toString());//parse lone removes leading zeros
    }
    
	private static String longToString(long data) {
		String result = "";
		String returnString = "";
		result = Long.toString(data);
		result = leadingZeroCheck(result);
        /* result now has the correct number of leading zeros */
        StringBuffer buf = new StringBuffer(result);
        for(int i = 0; i < result.length(); i+=2) {
            String temp = buf.substring(i,i+2);
            if(temp.equals("00")){
                returnString+='a';
            } else if(temp.equals("11")) {
                returnString+='t';
            } else if(temp.equals("10")){
                returnString+='g';
            } else {
                returnString+='c';
            }
        }
		return returnString;
	}
	/**
	 * Checks to see if there is missing zeros at the beginning of the sequence.
	 * @param data
	 * @return - returns the corrected value of the parsed data.
	 */
	private static String leadingZeroCheck(String data) {
		String ourData = data;
		//if data length is not sequence length times 2
		if(ourData.length() != (sequenceLength*2)) {
			String temp = data;
			ourData = "";
			int offset = ((sequenceLength*2) - temp.length());
			for (int i = 0; i < offset;i++) {
				ourData+="0";
			}
			ourData+=temp;
		}
		return ourData;
	}
}
