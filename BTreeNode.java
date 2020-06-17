import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * BTreeNode class
 * @author: Clayton Fields, Michael Elliott
 * initial date: 4/19/2020
 */


 /* each node has:
  * 	ordered list of children pointers
			each child pointer is an int, the byte offset in the file
		parent node pointer
			byte offset of parent, an integer
		location
			integer byte location in file. 
		leaf: boolean, true if it's a leaf node, else false
  */
public class BTreeNode{
	/* Both objects and children must be in non-descending order, I asked JHyeh 4/20/2020 */
	/* Objects needs to be iterable for BTree search */
	private LinkedList<TreeObject> objects; /* represents the keys, in order, in the node */
	private LinkedList<Integer> children; /* represents the child pointers, in order, below this node */
	private int N; /* number of keys in the node, same as objects.length */
	private boolean leaf;
	private int degree; /* property of the table, each node needs it */
	private int location; /* needed for cache, byte offset in the file of this node */
	private int parent; /* parent pointer */
	private int byteTotal; /* only needed for reading and writing operations, total bytes in node */
	private int numChildren;
	/**
	 * BTreeNode constructor
	 * @param t: the degree of the node
	 * @param parent_pointer: pointer to parent, if 0 this node is root
	 * @param location: the byte offset location of the node in the file
     */
	public BTreeNode(int t, int parent_pointer) {
		N = 0;
		objects = new LinkedList<TreeObject>();
		children = new LinkedList<Integer>();
		leaf = true;
		degree = t;
		parent = parent_pointer;
		byteTotal = 0;
		numChildren = 0;
	}
	/**
	 * Overloaded constructor, includes location
	 * @param t: degree of the node
	 * @param parent_pointer: pointer to parent (byte offset in file), 0 is root 
	 * @param location: location of the node in the file, a byte offset
	 */
	public BTreeNode(int t, int parent_pointer, int location) {
		N = 0;
		objects = new LinkedList<TreeObject>();
		children = new LinkedList<Integer>();
		leaf = true;
		degree = t;
		parent = parent_pointer;
		this.location = location;
		numChildren = 0;
		
	}

	public void setChildren(int i) {
		numChildren  = i;
	}
	
	public int getNumChildren() {
		return numChildren;
	}
	
	/**
	 * Getter method for list of objects contained in node
	 * @return linked list of TreeObjects
	 */
	public LinkedList<TreeObject> getObjects() {
		return objects;
	}

	/**
	 * Getter method for list of child pointers
	 * @return linked list of integers signifying addresses of child nodes
	 */
	public LinkedList<Integer> getChildren() {
		return children;
	}

	/**
	 * Getter method for size attribute
	 * @return integer number of objects in node
	 */
	public int getSize() {
		return N;
	}

	/**
	 * Getter method for leaf attribute
	 * @return True if node has no child nodes
	 */
	public boolean isLeaf() {
		return leaf;
	}
	
	/**
	 * Setter method for leaf attribute
	 * @param leaf
	 */
	public void setLeaf(boolean leaf) {
		this.leaf = leaf;
	}


	/**
	 * isFull method tells whether or not the node is full
	 * as defined by the BTree property:
	 * 		each node can have at most 2*t children
	 * @return true if full, else false
	 */
	public boolean isFull() {
		boolean retVal = true; /* assume full */
		if (N < 2*degree) {
			retVal = false;
		}
		return retVal;
	}
	/* we do not need remove/delete methods */
	
	/**
	 * Setter method for the size attribute
	 * @param i integer value 
	 */
	public void setSize(int i) {
		N = i;
	}
	/**
	 * get the parent address of this node
	 * @return integer byte offset in the file of the parent node
	 */
	public int getParent() {
		return this.parent;
	}

	public void setParent(int address) {
		this.parent = address;
	}
	
	/**
	 * @return byte offset in the file of this node
	 */
	public int getLocation() {
		return this.location;
	}

	/**
	 * Set the byte offset in the file, of this node.
	 * The location of the node is set by whatever method writes it to file.
	 * @param location
	 */
	public void setLocation(int location) {
		this.location = location;
	}

	/**
	 * getByteTotal calculates the size of the node in bytes
	 * 12 bytes per node (8 for long key, 4 for int frequency)
	 * 4 bytes per child pointer (int)
	 * 4 bytes per each int: location, N, degree, parent
	 * 1 byte for boolean leaf (writes as 1 byte)
	 * @return
	 */
	public int getByteTotal() {
		return N*12 + children.size()*4 + 6*4 + 1; 
	}

	/**
	 * Calculates the maximal size of the node with it's degree
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
	 * writes the BTreeNode out to a buffer, then to a file in binary
	 * order of writing:
	 * N, location, degree, parent, leaf, number of children, each child pointer, each TreeObject
	 * writeTreeObject writes the key, then frequency of the TreeObject
	 * @param raf: random access file without seek set
	 * @throws IOException caught by driver class
	 */
	public void writeNode(RandomAccessFile raf) throws IOException
	{
		int maxBytes = getMaxBytes();
		int actualBytes = getByteTotal(); /* number of bytes req'd to place in the buffer; boolean = 1 byte*/
		int numJunkBytes = maxBytes - actualBytes; // number of bytes to fill in at the end to write correctly
		ByteBuffer buffer = ByteBuffer.allocate(maxBytes); /* 4 is size of byteTotal itself*/ 
		buffer.clear(); /* one website said to do this... maybe it's redundant, but maybe it works */
		buffer.putInt(actualBytes).putInt(N).putInt(location).putInt(degree).putInt(parent);
		byte bool = (byte) (leaf == true ? 1 : 0); /* leaf == true : 1; leaf == false : 0 */
		buffer.put(bool).putInt(children.size());
		for(int i = 0; i < children.size(); i++) {
			buffer.putInt(children.get(i));
		}
		for(int i = 0; i < N; i++) {
			objects.get(i).writeTreeObject(buffer);
		}
		/* write out the junk bytes */
		for(int i = 0; i < numJunkBytes; i++) {
			buffer.put((byte)0);
		}
		buffer.flip(); /* this is supposed to keep the buffer in order for proper writing */
		// buffer has all information needed at this point, and is ready to write
		raf.seek(location); /* set file write location */
		raf.write(buffer.array()); /* write to file */
		buffer.clear(); /* maybe unnecessary */
	}

	/**
	 * readNode reads in a node from a RandomAccessFile that already has the location set
	 * Strategy for reading: 2 reads
	 * 		first read returns the total number of bytes to read into to make the node
	 *		second read reads all of the node into the buffer, to be turned into the nodes
	 *		attributes and properties.
	 * @param raf random access file to read from
	 * @throws IOException thrown by RAF, caught by driver
	 */
	public void readNode(RandomAccessFile raf) throws IOException
	{
		byteTotal = raf.readInt();
		ByteBuffer buffer = ByteBuffer.allocate(byteTotal-4); /* -4 because byteTotal has already been removed from the node */
		buffer.clear();
		raf.read(buffer.array());
		buffer.rewind(); /* possible source of error, might need to be buffer.rewind(), but i think it's correct to flip after reading, before writing */
		N = buffer.getInt();
		location = buffer.getInt();
		degree = buffer.getInt();
		parent = buffer.getInt();
		byte bool = buffer.get();
		leaf = (boolean) (bool == 1 ? true : false);
		int numChild = buffer.getInt();
		for( int i = 0; i < numChild; i++) {
			getChildren().add(buffer.getInt()); /* can use getChildren().set(i,buffer.getInt()) if this is buggy */
		}
		for( int i = 0; i < N; i++) {
			long key = buffer.getLong();
			int freq = buffer.getInt();
			TreeObject treeObj = new TreeObject(key,freq);
			objects.add(treeObj);
		}
		if(buffer.hasRemaining()) {
			System.out.println("BTreeNode class here. Your buffer still has things left in it, which is probably not a good thing.");
		}
	}
}