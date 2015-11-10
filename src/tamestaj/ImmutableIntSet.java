package tamestaj;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

// This is only useful for small sets!

@SuppressWarnings("unused")
final class ImmutableIntSet implements Iterable<Integer> {
	private int size;
	private int[] elements;
	private int hashCode;
    private IntSetIterator iterator;

    static final ImmutableIntSet EMPTY = new ImmutableIntSet(0, new int[0]);
	
	private class IntSetIterator implements Iterator<Integer> {
		private int index;

		public boolean hasNext() {
			return index < size;
		}
		public Integer next() {
			return elements[index++];
		}
		public void remove() {
		}
	}

    ImmutableIntSet(Set<Integer> set) {
        int[] elements = new int[set.size()];
        int index = 0;
        for (int element : set) {
            elements[index] = element;
            index++;
        }
        Arrays.sort(elements);

        size = elements.length;
        this.elements = elements;
        hashCode = size;
        for (int i = 0; i < size; i++) {
            hashCode = hashCode * 31 + elements[i];
        }
    }

    ImmutableIntSet(int element) {
		this(1, new int[] {element});
	}

    ImmutableIntSet() {
        this(0, new int[] { });
    }
	
	private ImmutableIntSet(int size, int[] elements) {
		this.size = size;
		this.elements = elements;
        hashCode = size;
        for (int i = 0; i < size; i++) {
            hashCode = hashCode * 31 + elements[i];
        }
	}

    int getElement(int index) { return elements[index]; }
    int getSize() {
        return size;
    }
    boolean isSingleton() { return size == 1; }
    int getMinimum() { return elements[0]; }
    int getMaximum() { return elements[size - 1]; }

    boolean contains(int element) { return Arrays.binarySearch(elements, element) >= 0; }
	
	ImmutableIntSet merge(ImmutableIntSet set) {
		if (set == null || this == set) {
			return this;
		}
		
		int[] a = elements;
		int[] b = set.elements;
		int aSize = size;
		int bSize = set.size; 
		int[] mergedElements = new int[aSize + bSize];
		int aIndex = 0;
		int bIndex = 0;
		int mergedSize = 0;
		for (; mergedSize < mergedElements.length; mergedSize++) {
			if (aIndex < aSize && (bIndex >= bSize || a[aIndex] < b[bIndex])) {
				mergedElements[mergedSize] = a[aIndex];
				aIndex++;
			} else if (bIndex < bSize && (aIndex >= aSize || a[aIndex] > b[bIndex])) {
				mergedElements[mergedSize] = b[bIndex];
				bIndex++;
			} else if (aIndex < aSize && bIndex < bSize) {
				mergedElements[mergedSize] = a[aIndex];
				aIndex++;
				bIndex++;
			} else {
				break;
			}
		}
		
		return new ImmutableIntSet(mergedSize, mergedElements);
	}

    ImmutableIntSet intersect(ImmutableIntSet set) {
        if (size == 0 && set.size == 0) {
            return EMPTY;
        }

        int[] intersectedElements;
        if (size < set.size) {
            intersectedElements = new int[size];
        } else {
            intersectedElements = new int[set.size];
        }

        int i = 0;
        int j = 0;
        int intersectedSize = 0;
        while (i < size && j < set.size) {
            if (elements[i] == set.elements[j]) {
                intersectedElements[intersectedSize] = elements[i];
                intersectedSize++;
                i++;
                j++;
            } else if (elements[i] < set.elements[j]) {
                i++;
            } else {
                j++;
            }
        }

        if (intersectedSize == 0) {
            return EMPTY;
        }
        return new ImmutableIntSet(intersectedSize, intersectedElements);
    }
	
	boolean isSubsetOf(ImmutableIntSet set) {
		if (size > set.size) {
			return false;
		}

        int i = 0;
        int j = 0;
        while (j < set.size) {
            if (elements[i] == set.elements[j]) {
                break;
            }
            j++;
        }
        if (j == set.size) {
            return false;
        }

		while (i < size && j < set.size) {
			if (elements[i] == set.elements[j]) {
				i++;
                j++;
			} else if (elements[i] < set.elements[j]) {
                return false;
            } else {
                i++;
            }
		}
		
		return i == size;
	}
	
	boolean isSubsetOf(Set<Integer> set) {
		for (int i = 0; i < size; i++) {
			if (!set.contains(elements[i])) {
				return false;
			}
		}
		return true;
	}
	
	void addTo(Set<Integer> set) {
		for (int i = 0; i < size; i++) {
			set.add(elements[i]);
		}
	}

    public int hashCode() { return hashCode; }

    public boolean equals(Object obj) {
        if (obj instanceof ImmutableIntSet) {
            ImmutableIntSet set = (ImmutableIntSet) obj;
            if (this == set) {
                return true;
            }

            if (this.size != set.size) {
                return false;
            }
            if (this.elements == set.elements) {
                return true;
            }
            for (int i = 0; i < size; i++) {
                if (elements[i] != set.elements[i]) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }
	
	public String toString() {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < size; i++) {
			sb.append(elements[i]);
			
			if (i < size - 1) {
				sb.append(", ");
			}
		}
		sb.append("]");
		
		return sb.toString();
	}

	public Iterator<Integer> iterator() {
		if (iterator == null) {
			iterator = new IntSetIterator();
		}
		iterator.index = 0;
		
		return iterator;
	}
}
