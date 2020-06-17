import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

public class BTree {
    private int degree; /* t, branching factor... why do we think this is degree..? */
    private BTreeNode root; 
    private int sequenceLength; /* this needs to be written to the file */
	private RandomAccessFile file;
	private String mode = "rw"; /* mode for RandomAccessFile, options: r, rw, rws, rwd */
	private String filename;
	private int numNodes; /* number of nodes written */

/**
 * Constructor of the BTree
 * @param degree
 */
    public BTree(int degree) {
		this.degree = degree;
		this.root = null;
		numNodes = 0;
        /**
         * I know more stuff has to go in here but not sure what all we want in here.
         */
	}
	
	/**
     * Another possible constructor
     * @param degree - degree of the BTree.
     * @param file - the filename to write into.
     * @param length - length of DNA sequence.
	 */
    public BTree(int degree, String filename, int length)
    {	
    	//root = new BTreeNode
		this.degree = degree;
		this.filename = filename;
		sequenceLength = length; 
		this.root = new BTreeNode(this.degree,0,0);
		numNodes = 1;
    }


    /**
     * Returns the degree
     */
    public int getDegree()
    {
        return this.degree;
    }

    /**
     * Returns the value of the node
     */
    public long get(long key)
    {
        return key;
    }
    
    /**
     * returns root of the BTree
     * @return
     */
    public BTreeNode getRoot()
    {
        return root;
	}
	
	/**
	 * Set a given node to be the root of the BTree
	 * @param node
	 */
	public void setRoot(BTreeNode node)
	{
		this.root = node;
	}
    
    
    /**
     * Creates new node and splits the contents of specified child node into new node
	 * 
     * @param x BTreeNode Node whose child is to be split
     * @param i int Number of child to be split
     * @throws IOException 
     */
    public void splitChild(BTreeNode x, int i, BTreeNode y) throws IOException
	{    	
		BTreeNode z = new  BTreeNode(degree,x.getLocation(),numNodes*getMaxBytes());
		numNodes++;
		z.setLeaf(y.isLeaf());
		z.setSize(degree-1);
		for(int j= 0; j<degree-1; j++)
		{
			z.getObjects().add(j, y.getObjects().get(j+degree));
		}
		for(int j=0; j<degree-1;j++) {
			y.getObjects().removeLast();
		}
		if(!y.isLeaf())
		{
			for(int j=0; j<degree; j++)
			{
				z.getChildren().add(j, y.getChildren().get(j+degree));
			}
			for(int j=0; j<degree; j++) {
				y.getChildren().removeLast();
			}
			
		}
		
		x.getChildren().add(i+1, z.getLocation()); 
		x.getObjects().add(i, y.getObjects().remove(degree-1));
		x.setSize(x.getSize()+1);
		y.setSize(degree-1);
		diskWrite(y);
		diskWrite(z);
		diskWrite(x);	
	}
	
    public int getNumNodes() {
		return numNodes;
	}

	/**
     * Inserts a tree object into a non-full leaf node
     * @param x BTreeNode where insertion is to begin
     * @param k TreeObject to be inserted
	 * @throws IOExpection should be caught by driver
     */
	public void insertNonFull(BTreeNode x, TreeObject k) throws IOException
	{
		int i = x.getSize();
		if(x.isLeaf())
		{
			while(i > 0 && k.getKey() < x.getObjects().get(i-1).getKey())//this is a problem
			{
				i -= 1;
			}
			x.getObjects().add(i, k);
			x.setSize(x.getSize()+1);
			diskWrite(x);
		}
		else
		{
			while(i > 0 && k.getKey() < x.getObjects().get(i-1).getKey())
			{
				i -= 1;
			}
			i+=1;
			BTreeNode temp = diskRead(x.getChildren().get(i-1));
			if(temp.getSize() == 2*degree-1)
			{
				
				splitChild(x,i-1,temp);
				if(k.getKey()>x.getObjects().get(i-1).getKey())
				{
					i += 1;
				}
			}
			BTreeNode temp2 = diskRead(x.getChildren().get(i-1));
			insertNonFull(temp2,k);
		}
	}
	
	/**
	 * General insert method for BTree
	 * @param k TreeObject to be inserted
	 * @throws IOException 
	 */
	public void insert(TreeObject k) throws IOException
	{
		BTreeNode r = root;

		int checkValue = incrementSearch(r, k.getKey());
		if(checkValue == 0) {
			if(r.getSize() == 2*degree-1)
			{
				
				BTreeNode s = new BTreeNode(degree,0,numNodes*getMaxBytes());
				numNodes++;
				root = s;
				s.setLeaf(false);
				s.setSize(0);
				s.getChildren().add(0,r.getLocation());
				r.setParent(s.getLocation());
				splitChild(s,0,r);
				insertNonFull(s,k);

			}
			else
			{
				insertNonFull(r,k);

			}
		
		}
	}

	/**
	 * reads in a BTreeNode from the file.
	 * @param address: byte offset, the byte location in the file of our node
	 * @return BTreeNode we are reading in
	 * @throws IOException: should be caught by driver
	 */
	public BTreeNode diskRead(int address) throws IOException
	{
		file = new RandomAccessFile(filename, mode);
		file.seek(address);
		BTreeNode node = new BTreeNode(degree,0);
		node.readNode(file);
		file.close();
		return node;
	}

	/**
	 * writes the given node, in binary, to a file
	 * @param node: BTreeNode to write
	 * @throws IOException caught by driver
	 */
	public void diskWrite(BTreeNode node) throws IOException
	{
		
		file = new RandomAccessFile(filename, mode);
		node.writeNode(file);
		file.close();
	}
	
	/* can return 2 different values with javafx.util.Pair or by returning an array of Objects */
	/**
	 * Search: Recursively searches down the given node for the given key.
	 * If the key value is found in the node, the TreeObjects frequency is incremented.
	 * @param node: given node to search
	 * @param key: the key to search for
	 * @return node if found, else null
	 * @throws IOException: should be caught in driver class
	 */
	public int search(BTreeNode node, long key) throws IOException
	{	
		int retFreq = 0; 
		Long keyLong = key; /* used for .equals method */
		int i = 0;
		while(i < node.getSize() && key > node.getObjects().get(i).getKey()) {
			i++;
		}
		if(i < node.getSize() && keyLong.equals((Long)node.getObjects().get(i).getKey())) {
			return node.getObjects().get(i).getFreq(); /* in-class notes say it should return node and i */
		}
		if(node.isLeaf()) {
			return retFreq;
		}
		else {
			int offset = node.getChildren().get(i);
			node = diskRead(offset);
			return search(node, key);
		}
	}

	/**
	 * Method for writing the important BTree data to the footer of
	 * the BTree file.
	 * The header is (in order of appearance in file): 
	 * root location (int), degree (int), sequenceLength (int)
	 * This produces an initial byte offset in the file of 12 bytes.
	 * @throws FileNotFoundException caught by driver class
	 * @throws IOException caught by driver class
	 */ 
	public void writeFooter() throws FileNotFoundException, IOException
	{
		/* store t, k, and root location in head of file
		t, k, and rootLocation are ints */
		ByteBuffer buffer = ByteBuffer.allocate(12);
		buffer.putInt(root.getLocation());
		buffer.putInt(degree); // is t degree
		buffer.putInt(sequenceLength); // sequenceLength is k 
		buffer.flip();
		file = new RandomAccessFile(filename, mode);
		file.seek(file.length()); // could be off by 1, I think this is correct
		file.write(buffer.array());
		buffer.clear();
		file.close();
	}
	
	
	/**
	 * Recursive method traverses all nodes and prints key and freq in order 
	 * for a subtree rooted with this node 
	 * @param node: root of tree to be traversed
	 * @throws IOException
	 */
    private void traverseNodePrint(BTreeNode node) throws IOException { 
  
        // There are n keys and n+1 children, traverse through n keys 
        // and first n children 
        int i = 0; 
        for (i = 0; i < node.getSize(); i++) { 
  
            // If this is not leaf, then before printing key[i], 
            // traverse the subtree rooted with child C[i]. 
            if (node.isLeaf() == false) { 
                traverseNodePrint(diskRead(node.getChildren().get(i)));
			} 
			System.out.println(node.getObjects().get(i).getKeyAsString(sequenceLength) + ": "+node.getObjects().get(i).getFreq()); 
        } 
  
        // Print the subtree rooted with last child 
        if (node.isLeaf()== false) {
			traverseNodePrint(diskRead(node.getChildren().get(i)));
		}
    } 
    
    /**
     * traverses the tree in level order and prints keys and freq for each object
     * @throws IOException
     */
    public void traverseTreePrint() throws IOException {
    	if(root != null) {
    		traverseNodePrint(root);
    	}
    }
    
    /**
	 * Recursive method traverses all nodes and appends the key and freq 
	 * to a dump file in order for a subtree rooted with this node 
	 * @param node: root of tree to be traversed
	 * @throws IOException
	 */

    private void traverseNodeDump(BTreeNode node, String dumpFileName, FileWriter fw) throws IOException {
		//FileWriter fw = new FileWriter(dumpFileName, true);    
    	// There are n keys and n+1 children, traverse through n keys 
        // and first n children 
        int i = 0;
        for (i = 0; i < node.getSize(); i++) { 
  
            // If this is not leaf, then before printing key[i], 
            // traverse the subtree rooted with child C[i]. 
            if (node.isLeaf() == false) { 
                traverseNodeDump(diskRead(node.getChildren().get(i)),dumpFileName, fw);
            } 
            fw.write(node.getObjects().get(i).getKeyAsString(sequenceLength) + ": "+node.getObjects().get(i).getFreq()+"\n"); 
		}	
        // Print the subtree rooted with last child 
       	if (node.isLeaf() == false) {
			traverseNodeDump(diskRead(node.getChildren().get(i)),dumpFileName, fw);
		}	
			
        //fw.close();
    }
    
    public void traverseTreeDump() throws IOException{
    	String dumpFileName = filename + ".dump"; 
    	File dumpFile = new File(dumpFileName);
		dumpFile.createNewFile();
		FileWriter fw = new FileWriter(dumpFileName, true);
    	if(root != null) {
    		traverseNodeDump(root, dumpFileName, fw);
		}
		fw.close();
    }
    
    /**
     * Overloaded method allows for Cache storage of BTreeNodes
     * Creates new node and splits the contents of specified child node into new node
     * @param x BTreeNode Node whose child is to be split
     * @param i int Number of child to be split
     * * @param c Cache for holding BTreeNodes in local memory
     * @throws IOException 
     */
    public void splitChild(BTreeNode x, int i, Cache c) throws IOException
	{
    	/*
    	 * Add diskwrite or addNode at appropriate place
    	 */
		BTreeNode y = null;
		
		
		BTreeNode z = new  BTreeNode(degree,0,numNodes*getMaxBytes());
		numNodes++;
		if(c.nodeHit(x.getChildren().get(i))) {
			y=c.getNode(x.getChildren().get(i));
		}else {
			y = diskRead(x.getChildren().get(i));
			c.addNode(y);
		}
		z.setLeaf(y.isLeaf());
		z.setSize(degree-1);
		for(int j= 0; j<degree-1; j++)
		{
			z.getObjects().set(j, y.getObjects().get(j+1));
		}
		if(!y.isLeaf())
		{
			for(int j=0; j<degree; j++)
			{
				z.getChildren().set(j, y.getChildren().get(j+1));
			}
		}
		y.setSize(degree-1);
		for(int j = x.getSize(); j>=i; j--)
		{
			x.getChildren().set(j+1, x.getChildren().get(j));
		}
		x.getChildren().set(i, z.getLocation());
		for(int j = x.getSize()-1; j>=i-1; j--)
		{
			x.getObjects().set(j+1, x.getObjects().get(j));
		}
		x.getObjects().set(i, y.getObjects().get(degree));
		x.setSize(x.getSize()+1);
		
	}
	
    
    /**
     * * Overloaded method allows for Cache storage of BTreeNodes
     * Inserts a tree object into a non-full leaf node
     * @param x BTreeNode where insertion is to begin
     * @param k TreeObject to be inserted
     * * @param c Cache for holding BTreeNodes in local memory
	 * @throws IOExpection should be caught by driver
     */
    public void insertNonFull(BTreeNode x, TreeObject k, Cache c) throws IOException
	{
		int i = x.getSize();
		if(x.isLeaf())
		{
			while(i > 0 && k.getKey() < x.getObjects().get(i-1).getKey())
			{
				i -= 1;
			}
			x.getObjects().add(i, k);
			x.setSize(x.getSize()+1);
			diskWrite(x);
		}
		else
		{
			while(i > 0 && k.getKey() < x.getObjects().get(i-1).getKey())
			{
				i -= 1;
			}
			i+=1;
			BTreeNode temp;
			if(c.nodeHit(x.getChildren().get(i-1))) {
				temp = c.getNode(x.getChildren().get(i-1));
			}else {
				temp = diskRead(x.getChildren().get(i-1));

			}	
			
			if(temp.getSize() == 2*degree-1)
			{
				
				splitChild(x,i-1,temp);
				if(k.getKey()>x.getObjects().get(i-1).getKey())
				{
					i += 1;
				}
			}
			BTreeNode temp2 = diskRead(x.getChildren().get(i-1));
			insertNonFull(temp2,k,c);
		}
	}
	
	/**
	 *  * Overloaded insert method for BTree
	 * @param k TreeObject to be inserted
	 * @throws IOException 
	 * @param c Cache for holding BTreeNodes in local memory
	 */
	public void insert(TreeObject k, Cache c) throws IOException
	{
		BTreeNode r = root;

		int checkValue = incrementSearch(r, k.getKey(),c);
		if(checkValue == 0) {
			if(r.getSize() == 2*degree-1)
			{
				
				BTreeNode s = new BTreeNode(degree,0,numNodes*getMaxBytes());
				numNodes++;
				root = s;
				s.setLeaf(false);
				s.setSize(0);
				s.getChildren().add(0,r.getLocation());
				r.setParent(s.getLocation());
				splitChild(s,0,r);
				insertNonFull(s,k,c);

			}
			else
			{
				insertNonFull(r,k,c);

			}
		
		}
	}
	
	/**
	 * Overloaded search method provides cache fuctionality
	 * Search: Recursively searches down the given node for the given key,
	 * returning the frequency of the node if it's found, else zero
	 * @param node: given node to search
	 * @param key: the key to search for
	 * @return frequency of key value if it exists in BTree already, else zero
	 * @throws IOException: should be caught in driver class
	 */
	public int search(BTreeNode node, long key, Cache c) throws IOException
	{	
		int retFreq = 0; 
		Long keyLong = key; /* used for .equals method */
		int i = 0;
		while(i < node.getSize() && key > node.getObjects().get(i).getKey()) {
			i++;
		}
		if(i <= node.getSize() && keyLong.equals((Long)node.getObjects().get(i).getKey())) {
			return node.getObjects().get(i).getFreq(); /* in-class notes say it should return node and i */
		}
		if(node.isLeaf()) {
			return retFreq;
		}
		else {
			int offset = node.getChildren().get(i);
			if(c.nodeHit(offset)) {
				node = c.getNode(offset);
			}else {
				node = diskRead(offset);
			}		
			search(node, key);
		}
		/* should never get to this point... could be useful for error checking */
		return retFreq;
	}

	/**
	 * Calculates the maximal size of a node with it's degree
	 * 12 bytes per object (2t-1) (8 for long key, 4 for int frequency)
	 * 4 bytes per child pointer (2t) (int)
	 * 4 bytes per each int: location, N, degree, parent
	 * 1 byte for boolean leaf (writes as 1 byte)
	 * @return
	 */
	public int getMaxBytes() {
		return (2*degree-1)*12 + 2*degree*4 + 6*4 + 1; 
	}

	/**
	 * nodeSearch: Recursively searches down the given node for the given key.
	 * If the key value is found in the node, the TreeObjects frequency is incremented.
	 * @param node: given node to search
	 * @param key: the key to search for
	 * @return node if found, else null
	 * @throws IOException: should be caught in driver class
	 */
	private int incrementSearch(BTreeNode node, long key) throws IOException
	{	
		int retFreq = 0;
		Long keyLong = key;
		int i = 0;
		while(i < node.getSize() && key > node.getObjects().get(i).getKey()) {
			i++;
		}
		if(i < node.getSize() && keyLong.equals((Long)node.getObjects().get(i).getKey())) {
			node.getObjects().get(i).addToFreq();
			diskWrite(node);
			return node.getObjects().get(i).getFreq(); /* in-class notes say it should return node and i */
		}
		else if(node.isLeaf()) {
			return retFreq;
		}
		else {
			int offset = node.getChildren().get(i);
			node = diskRead(offset);
			return incrementSearch(node, key);
		}
	}
	
	/**
	 * nodeSearch: Recursively searches down the given node for the given key.
	 * If the key value is found in the node, the TreeObjects frequency is incremented.
	 * @param node: given node to search
	 * @param key: the key to search for
	 * @return node if found, else null
	 * @throws IOException: should be caught in driver class
	 */
	private int incrementSearch(BTreeNode node, long key, Cache c) throws IOException
	{	
		int retFreq = 0;
		Long keyLong = key;
		int i = 0;
		while(i < node.getSize() && key > node.getObjects().get(i).getKey()) {
			i++;
		}
		if(i < node.getSize() && keyLong.equals((Long)node.getObjects().get(i).getKey())) {
			node.getObjects().get(i).addToFreq();
			diskWrite(node);
			return node.getObjects().get(i).getFreq(); /* in-class notes say it should return node and i */
		}
		else if(node.isLeaf()) {
			return retFreq;
		}
		else {
			int offset = node.getChildren().get(i);
			if(c.nodeHit(offset)) {
				node = c.getNode(offset);
			}else {
				node = diskRead(offset);
			}		
			return incrementSearch(node, key);
		}
	}

	
}