import java.nio.ByteBuffer;

public class TreeObject {
    private long key;
    private int freq;

    /**
     * I think we need frequency to begin at 1, the first instance of the object
     * @param key - key of the object
     */
    public TreeObject(long key)
    {
        this.key = key;
        this.freq = 1;
    }
    
    /**
     * 
     * @param key the key of the object
     * @param freq the frequency of the object
     */
    public TreeObject(long key, int freq) {
        this.key = key;
        this.freq = freq;
    }
    /**
     * returns the key of the object
     * @return
     */
    public long getKey()
    {
        return this.key;
    }
    
    /**
     * set the key of the TreeObject
     * @param setKey
     */
    public void setKey(long setKey) {
        key = setKey;
    }
    /**
     * returns the frequency of the object
     */
    public int getFreq()
    {
        return this.freq;
    }

    /**
     * set the frequency of the TreeObject
     * @param setFreq
     */
    public void setFreq(int setFreq) {
        freq = setFreq;
    }
    /**
     * compares one key to another
     * @param obj
     * @return
     */
    public int compareTo(TreeObject obj)
    {
        if (key < obj.key)
        {
            return -1;
        }
        if (key > obj.key)
        {
            return 1;
        }
        else
            return 0;
    }
    /**
     * adds one to the frequency of the TreeObject
     */
    public void addToFreq() {
        this.freq++;
    }
    
    /**
     * writes the TreeObject to a ByteBuffer, to be written to a RAF later.
     * @param buff ByteBuffer that will reduce writing operations
     */
    public void writeTreeObject(ByteBuffer buff)
    {
        buff.putLong(key);
        buff.putInt(freq);
    }
        /**
     * 
     * @param seqLength
     * @return
     */
    public String getKeyAsString(int seqLength){
        String str = binaryStringMaker(this.key,seqLength);
    	return binaryStringToDNA(str);
    }
    /**
     * takes in a long and the length to return a converted string.
     * @param data
     * @param seqLength
     * @return
     */
    private String binaryStringToDNA(String str) {
        // str already has correct leading zeros
        String returnString = "";
        StringBuffer buf = new StringBuffer(str);
        for(int i = 0; i < buf.length(); i+=2) {
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
     * Goal is to return a string of 0's and 1's, a binary representation
     * of the long. The sequence length will restore leading zeros
     * so that this can be used to recover ATCG code from genome files.
     * @param key
     * @param seqLen
     * @return
     */
    private static String binaryStringMaker(long key, int seqLen) {
        /* seqLen  valid for 1 <= seqLen <= 31 but lets just get it working first */
        StringBuilder endStr = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        int msdLoc = 0;
        long temp = 0;
        if(temp == key) {
            /* string builder for seqLen and key = 0; */
            msdLoc++;
            endStr.append(0); // endStr: '0'
        }
        else if(key == 1) {
            /* key == 1, string builder for seqLen and key = 1 */
            temp = 1;
            msdLoc++;
            endStr.append(1); // endStr: '1'
        }
        else {
            int i = 0;
            while(2*temp <= key) {
                temp = (long) Math.pow(2,i);
                i++;
            }
            msdLoc = i;
            i--;
            endStr.append(1); // endStr: '1'

            while(i > 0) {  // might be >=
                if(temp + (long)Math.pow(2,i-1) <= key) {
                    /* then we append a 1, add the (long) portion to temp for good */
                    endStr.append(1);
                    temp += (long)Math.pow(2,i-1);
                }
                else {
                    endStr.append(0);
                }
                i--;
            }
        }
        if(temp != key) {
            System.out.println("\n\n temp != key, somethings broke\n\n");
            System.out.println("\n\n temp = " + temp + "\tkey = " + key);
        }
        int numZeros = (2*seqLen - msdLoc); // the 2 comes from only encoding A,T,C,G so our binary options are 00 = A, 11 = T, 01 = C, 10 = G
        for(int j = 0; j < numZeros; j++) {
            sb.append(0);
        }
        // at this point sb has the correct number of leading zeros and we just need to get the 
        // 1's and 0's after the msd, and add them to the string builder
        sb.append(endStr);
        return sb.toString();
    }

}
