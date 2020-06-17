import java.util.LinkedList;

/**
 * Cache represents cache in computer's memory
 * 
 * @author claytonfields
 *
 * @param <T>
 */

public class Cache {

	private LinkedList<BTreeNode> list;
	int size;

	/**
	 * Creates an empty cache.
	 * 
	 * @param size
	 */
	public Cache(int size) {
		this.size = size;
		list = new LinkedList<BTreeNode>();

	}

	/**
	 * Searches cache for element, moves to front if found. Adds element to front if
	 * not found. Returns true if element is in cache, false if not.
	 * 
	 * @param element
	 * @return boolean
	 */
	public boolean getObject(BTreeNode element) {
		int index = list.indexOf(element);
		if (index == -1) {
			addObject(element);
			return false;
		} else {
			// T retval = list.get(index);
			list.addFirst(list.remove(index));
			return true;
		}
	}

	/**
	 * Searches cache for object, if there it moves to front. If not there the
	 * object is added to the front. Last item is removed if cache is full.
	 * 
	 * @param element
	 */
	public void addObject(BTreeNode element) {
		int index = list.indexOf(element);
		if (index == -1) {
			if (list.size() >= size) {
				list.removeLast();
			}
			list.addFirst(element);

		} else {
			list.addFirst(list.remove(index));

		}
	}
	
	public BTreeNode getNode(int address) {
		//int index = list.indexOf(element);
		int i=0;
		while(i<list.size() && address != list.get(i).getLocation())
		{
			i++;
		}
		if(i==list.size()) {
			return null;
		}
		list.addFirst(list.remove(i));
		return list.getFirst();
		
	}
	
	public void addNode(BTreeNode node) {
		int i = 0;
		while(i<list.size() && node.getLocation() != list.get(i).getLocation()) {
			i++;
		}
		if(i==list.size()) {
			list.add(node);
			if(list.size()==size) {
				list.removeLast();
			}
		} else {
			list.addFirst(list.remove(i));
		}
	}
	

	/**
	 * 
	 * @param element
	 */

	public void addSimple(BTreeNode element) {
		if (list.size() >= size) {
			list.removeLast();
		}
		list.addFirst(element);
	}

	/**
	 * Removes and returns specified element if it is in the cache. Returns null if
	 * it is in the cache.
	 * 
	 * @param element
	 * @return T
	 */
	public BTreeNode removeObject(BTreeNode element) {
		int index = list.indexOf(element);
		if (index == -1) {
			return null;
		} else {
			return list.get(index);
		}
	}

	/**
	 * Returns true if element is in cache, false if not.
	 * 
	 * @param element
	 * @return
	 */
	public boolean hit(BTreeNode element) {
		int index = list.indexOf(element);
		if (index == -1) {
			return false;
		} else {
			return true;
		}
	}

	public boolean nodeHit(int address) {
		boolean hit = false;
		for(int i = 0; i<list.size();i++) {
			if(list.get(i).getLocation() == address) {
				hit = true;
			}
		}
		return hit;
	}
	
	
	/**
	 * Clears the cache.
	 */
	public void clearCache() {
		list.clear();
	}

	/**
	 * Returns string representation of cache.
	 * 
	 * @return
	 */
	public String toString() {
		String str = "cache(";
		for (int i = 0; i < list.size(); i++) {
			str += list.get(i);
			str += ", ";
		}
		str = str.substring(0, str.length() - 2);
		str = str + ")";
		return str;
	}
}
