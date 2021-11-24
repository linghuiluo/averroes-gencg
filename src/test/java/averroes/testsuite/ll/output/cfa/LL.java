package averroes.testsuite.ll.output.cfa;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * LinkedList class copied from java.util.collections. Eliminated the use of generics, and uses of
 * the transient keyword". Copied declarations of modCount, iterator() and listIterator() from
 * abstract superclasses to make the code self-contained.
 */
public class LL implements Iterable {
  private static final long serialVersionUID = 876323262645176354L;
  protected int modCount = 0;
  int size = 0;
  /**
   * Pointer to first node. Invariant: (first == null && last == null) || (first.prev == null &&
   * first.item != null)
   */
  Node first;
  /**
   * Pointer to last node. Invariant: (first == null && last == null) || (last.next == null &&
   * last.item != null)
   */
  Node last;

  /** Constructs an empty list. */
  public LL() {}

  /**
   * Constructs a list containing the elements of the specified collection, in the order they are
   * returned by the collection's iterator.
   *
   * @param c the collection whose elements are to be placed into this list
   * @throws NullPointerException if the specified collection is null
   */
  public LL(Collection c) {
    this();
    addAll(c);
  }

  public Iterator iterator() {
    return listIterator();
  }

  public ListIterator listIterator() {
    return listIterator(0);
  }

  /** Links e as first element. */
  private void linkFirst(Object e) {
    final Node f = first;
    final Node newNode = new Node(null, e, f);
    first = newNode;
    if (f == null) last = newNode;
    else f.prev = newNode;
    size++;
    modCount++;
  }

  /** Links e as last element. */
  void linkLast(Object e) {
    final Node l = last;
    final Node newNode = new Node(l, e, null);
    last = newNode;
    if (l == null) first = newNode;
    else l.next = newNode;
    size++;
    modCount++;
  }

  /** Inserts element e before non-null Node succ. */
  void linkBefore(Object e, Node succ) {
    // assert succ != null;
    final Node pred = succ.prev;
    final Node newNode = new Node(pred, e, succ);
    succ.prev = newNode;
    if (pred == null) first = newNode;
    else pred.next = newNode;
    size++;
    modCount++;
  }

  /** Unlinks non-null first node f. */
  private Object unlinkFirst(Node f) {
    // assert f == first && f != null;
    final Object element = f.item;
    final Node next = f.next;
    f.item = null;
    f.next = null; // help GC
    first = next;
    if (next == null) last = null;
    else next.prev = null;
    size--;
    modCount++;
    return element;
  }

  /** Unlinks non-null last node l. */
  private Object unlinkLast(Node l) {
    // assert l == last && l != null;
    final Object element = l.item;
    final Node prev = l.prev;
    l.item = null;
    l.prev = null; // help GC
    last = prev;
    if (prev == null) first = null;
    else prev.next = null;
    size--;
    modCount++;
    return element;
  }

  /** Unlinks non-null node x. */
  Object unlink(Node x) {
    // assert x != null;
    final Object element = x.item;
    final Node next = x.next;
    final Node prev = x.prev;

    if (prev == null) {
      first = next;
    } else {
      prev.next = next;
      x.prev = null;
    }

    if (next == null) {
      last = prev;
    } else {
      next.prev = prev;
      x.next = null;
    }

    x.item = null;
    size--;
    modCount++;
    return element;
  }

  /**
   * Returns the first element in this list.
   *
   * @return the first element in this list
   * @throws NoSuchElementException if this list is empty
   */
  public Object getFirst() {
    final Node f = first;
    if (f == null) throw new NoSuchElementException();
    return f.item;
  }

  /**
   * Returns the last element in this list.
   *
   * @return the last element in this list
   * @throws NoSuchElementException if this list is empty
   */
  public Object getLast() {
    final Node l = last;
    if (l == null) throw new NoSuchElementException();
    return l.item;
  }

  /**
   * Removes and returns the first element from this list.
   *
   * @return the first element from this list
   * @throws NoSuchElementException if this list is empty
   */
  public Object removeFirst() {
    final Node f = first;
    if (f == null) throw new NoSuchElementException();
    return unlinkFirst(f);
  }

  /**
   * Removes and returns the last element from this list.
   *
   * @return the last element from this list
   * @throws NoSuchElementException if this list is empty
   */
  public Object removeLast() {
    final Node l = last;
    if (l == null) throw new NoSuchElementException();
    return unlinkLast(l);
  }

  /**
   * Inserts the specified element at the beginning of this list.
   *
   * @param e the element to add
   */
  public void addFirst(Object e) {
    linkFirst(e);
  }

  /**
   * Appends the specified element to the end of this list.
   *
   * <p>This method is equivalent to {@link #add}.
   *
   * @param e the element to add
   */
  public void addLast(Object e) {
    linkLast(e);
  }

  /**
   * Returns {@code true} if this list contains the specified element. More formally, returns {@code
   * true} if and only if this list contains at least one element {@code e} such that
   * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
   *
   * @param o element whose presence in this list is to be tested
   * @return {@code true} if this list contains the specified element
   */
  public boolean contains(Object o) {
    return indexOf(o) != -1;
  }

  /**
   * Returns the number of elements in this list.
   *
   * @return the number of elements in this list
   */
  public int size() {
    return size;
  }

  /**
   * Appends the specified element to the end of this list.
   *
   * <p>This method is equivalent to {@link #addLast}.
   *
   * @param e element to be appended to this list
   * @return {@code true} (as specified by {@link Collection#add})
   */
  public boolean add(Object e) {
    linkLast(e);
    return true;
  }

  /**
   * Removes the first occurrence of the specified element from this list, if it is present. If this
   * list does not contain the element, it is unchanged. More formally, removes the element with the
   * lowest index {@code i} such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt> (if such an element
   * exists). Returns {@code true} if this list contained the specified element (or equivalently, if
   * this list changed as a result of the call).
   *
   * @param o element to be removed from this list, if present
   * @return {@code true} if this list contained the specified element
   */
  public boolean remove(Object o) {
    if (o == null) {
      for (Node x = first; x != null; x = x.next) {
        if (x.item == null) {
          unlink(x);
          return true;
        }
      }
    } else {
      for (Node x = first; x != null; x = x.next) {
        if (o.equals(x.item)) {
          unlink(x);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Appends all of the elements in the specified collection to the end of this list, in the order
   * that they are returned by the specified collection's iterator. The behavior of this operation
   * is undefined if the specified collection is modified while the operation is in progress. (Note
   * that this will occur if the specified collection is this list, and it's nonempty.)
   *
   * @param c collection containing elements to be added to this list
   * @return {@code true} if this list changed as a result of the call
   * @throws NullPointerException if the specified collection is null
   */
  public boolean addAll(Collection c) {
    return addAll(size, c);
  }

  /**
   * Inserts all of the elements in the specified collection into this list, starting at the
   * specified position. Shifts the element currently at that position (if any) and any subsequent
   * elements to the right (increases their indices). The new elements will appear in the list in
   * the order that they are returned by the specified collection's iterator.
   *
   * @param index index at which to insert the first element from the specified collection
   * @param c collection containing elements to be added to this list
   * @return {@code true} if this list changed as a result of the call
   * @throws IndexOutOfBoundsException {@inheritDoc}
   * @throws NullPointerException if the specified collection is null
   */
  public boolean addAll(int index, Collection c) {
    checkPositionIndex(index);

    Object[] a = c.toArray();
    int numNew = a.length;
    if (numNew == 0) return false;

    Node pred, succ;
    if (index == size) {
      succ = null;
      pred = last;
    } else {
      succ = node(index);
      pred = succ.prev;
    }

    for (Object o : a) {
      @SuppressWarnings("unchecked")
      Object e = o;
      Node newNode = new Node(pred, e, null);
      if (pred == null) first = newNode;
      else pred.next = newNode;
      pred = newNode;
    }

    if (succ == null) {
      last = pred;
    } else {
      pred.next = succ;
      succ.prev = pred;
    }

    size += numNew;
    modCount++;
    return true;
  }

  // Positional Access Operations

  /** Removes all of the elements from this list. The list will be empty after this call returns. */
  public void clear() {
    // Clearing all of the links between nodes is "unnecessary", but:
    // - helps a generational GC if the discarded nodes inhabit
    // more than one generation
    // - is sure to free memory even if there is a reachable Iterator
    for (Node x = first; x != null; ) {
      Node next = x.next;
      x.item = null;
      x.next = null;
      x.prev = null;
      x = next;
    }
    first = last = null;
    size = 0;
    modCount++;
  }

  /**
   * Returns the element at the specified position in this list.
   *
   * @param index index of the element to return
   * @return the element at the specified position in this list
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  public Object get(int index) {
    checkElementIndex(index);
    return node(index).item;
  }

  /**
   * Replaces the element at the specified position in this list with the specified element.
   *
   * @param index index of the element to replace
   * @param element element to be stored at the specified position
   * @return the element previously at the specified position
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  public Object set(int index, Object element) {
    checkElementIndex(index);
    Node x = node(index);
    Object oldVal = x.item;
    x.item = element;
    return oldVal;
  }

  /**
   * Inserts the specified element at the specified position in this list. Shifts the element
   * currently at that position (if any) and any subsequent elements to the right (adds one to their
   * indices).
   *
   * @param index index at which the specified element is to be inserted
   * @param element element to be inserted
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  public void add(int index, Object element) {
    checkPositionIndex(index);

    if (index == size) linkLast(element);
    else linkBefore(element, node(index));
  }

  /**
   * Removes the element at the specified position in this list. Shifts any subsequent elements to
   * the left (subtracts one from their indices). Returns the element that was removed from the
   * list.
   *
   * @param index the index of the element to be removed
   * @return the element previously at the specified position
   * @throws IndexOutOfBoundsException {@inheritDoc}
   */
  public Object remove(int index) {
    checkElementIndex(index);
    return unlink(node(index));
  }

  /** Tells if the argument is the index of an existing element. */
  private boolean isElementIndex(int index) {
    return index >= 0 && index < size;
  }

  /** Tells if the argument is the index of a valid position for an iterator or an add operation. */
  private boolean isPositionIndex(int index) {
    return index >= 0 && index <= size;
  }

  /**
   * Constructs an IndexOutOfBoundsException detail message. Of the many possible refactorings of
   * the error handling code, this "outlining" performs best with both server and client VMs.
   */
  private String outOfBoundsMsg(int index) {
    return "Index: " + index + ", Size: " + size;
  }

  private void checkElementIndex(int index) {
    if (!isElementIndex(index)) throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
  }

  private void checkPositionIndex(int index) {
    if (!isPositionIndex(index)) throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
  }

  // Search Operations

  /** Returns the (non-null) Node at the specified element index. */
  Node node(int index) {
    // assert isElementIndex(index);

    if (index < (size >> 1)) {
      Node x = first;
      for (int i = 0; i < index; i++) x = x.next;
      return x;
    } else {
      Node x = last;
      for (int i = size - 1; i > index; i--) x = x.prev;
      return x;
    }
  }

  /**
   * Returns the index of the first occurrence of the specified element in this list, or -1 if this
   * list does not contain the element. More formally, returns the lowest index {@code i} such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>, or -1 if there is no
   * such index.
   *
   * @param o element to search for
   * @return the index of the first occurrence of the specified element in this list, or -1 if this
   *     list does not contain the element
   */
  public int indexOf(Object o) {
    int index = 0;
    if (o == null) {
      for (Node x = first; x != null; x = x.next) {
        if (x.item == null) return index;
        index++;
      }
    } else {
      for (Node x = first; x != null; x = x.next) {
        if (o.equals(x.item)) return index;
        index++;
      }
    }
    return -1;
  }

  // Queue operations.

  /**
   * Returns the index of the last occurrence of the specified element in this list, or -1 if this
   * list does not contain the element. More formally, returns the highest index {@code i} such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>, or -1 if there is no
   * such index.
   *
   * @param o element to search for
   * @return the index of the last occurrence of the specified element in this list, or -1 if this
   *     list does not contain the element
   */
  public int lastIndexOf(Object o) {
    int index = size;
    if (o == null) {
      for (Node x = last; x != null; x = x.prev) {
        index--;
        if (x.item == null) return index;
      }
    } else {
      for (Node x = last; x != null; x = x.prev) {
        index--;
        if (o.equals(x.item)) return index;
      }
    }
    return -1;
  }

  /**
   * Retrieves, but does not remove, the head (first element) of this list.
   *
   * @return the head of this list, or {@code null} if this list is empty
   * @since 1.5
   */
  public Object peek() {
    final Node f = first;
    return (f == null) ? null : f.item;
  }

  /**
   * Retrieves, but does not remove, the head (first element) of this list.
   *
   * @return the head of this list
   * @throws NoSuchElementException if this list is empty
   * @since 1.5
   */
  public Object element() {
    return getFirst();
  }

  /**
   * Retrieves and removes the head (first element) of this list.
   *
   * @return the head of this list, or {@code null} if this list is empty
   * @since 1.5
   */
  public Object poll() {
    final Node f = first;
    return (f == null) ? null : unlinkFirst(f);
  }

  /**
   * Retrieves and removes the head (first element) of this list.
   *
   * @return the head of this list
   * @throws NoSuchElementException if this list is empty
   * @since 1.5
   */
  public Object remove() {
    return removeFirst();
  }

  // Deque operations

  /**
   * Adds the specified element as the tail (last element) of this list.
   *
   * @param e the element to add
   * @return {@code true} (as specified by {@link Queue#offer})
   * @since 1.5
   */
  public boolean offer(Object e) {
    return add(e);
  }

  /**
   * Inserts the specified element at the front of this list.
   *
   * @param e the element to insert
   * @return {@code true} (as specified by {@link Deque#offerFirst})
   * @since 1.6
   */
  public boolean offerFirst(Object e) {
    addFirst(e);
    return true;
  }

  /**
   * Inserts the specified element at the end of this list.
   *
   * @param e the element to insert
   * @return {@code true} (as specified by {@link Deque#offerLast})
   * @since 1.6
   */
  public boolean offerLast(Object e) {
    addLast(e);
    return true;
  }

  /**
   * Retrieves, but does not remove, the first element of this list, or returns {@code null} if this
   * list is empty.
   *
   * @return the first element of this list, or {@code null} if this list is empty
   * @since 1.6
   */
  public Object peekFirst() {
    final Node f = first;
    return (f == null) ? null : f.item;
  }

  /**
   * Retrieves, but does not remove, the last element of this list, or returns {@code null} if this
   * list is empty.
   *
   * @return the last element of this list, or {@code null} if this list is empty
   * @since 1.6
   */
  public Object peekLast() {
    final Node l = last;
    return (l == null) ? null : l.item;
  }

  /**
   * Retrieves and removes the first element of this list, or returns {@code null} if this list is
   * empty.
   *
   * @return the first element of this list, or {@code null} if this list is empty
   * @since 1.6
   */
  public Object pollFirst() {
    final Node f = first;
    return (f == null) ? null : unlinkFirst(f);
  }

  /**
   * Retrieves and removes the last element of this list, or returns {@code null} if this list is
   * empty.
   *
   * @return the last element of this list, or {@code null} if this list is empty
   * @since 1.6
   */
  public Object pollLast() {
    final Node l = last;
    return (l == null) ? null : unlinkLast(l);
  }

  /**
   * Pushes an element onto the stack represented by this list. In other words, inserts the element
   * at the front of this list.
   *
   * <p>This method is equivalent to {@link #addFirst}.
   *
   * @param e the element to push
   * @since 1.6
   */
  public void push(Object e) {
    addFirst(e);
  }

  /**
   * Pops an element from the stack represented by this list. In other words, removes and returns
   * the first element of this list.
   *
   * <p>This method is equivalent to {@link #removeFirst()}.
   *
   * @return the element at the front of this list (which is the top of the stack represented by
   *     this list)
   * @throws NoSuchElementException if this list is empty
   * @since 1.6
   */
  public Object pop() {
    return removeFirst();
  }

  /**
   * Removes the first occurrence of the specified element in this list (when traversing the list
   * from head to tail). If the list does not contain the element, it is unchanged.
   *
   * @param o element to be removed from this list, if present
   * @return {@code true} if the list contained the specified element
   * @since 1.6
   */
  public boolean removeFirstOccurrence(Object o) {
    return remove(o);
  }

  /**
   * Removes the last occurrence of the specified element in this list (when traversing the list
   * from head to tail). If the list does not contain the element, it is unchanged.
   *
   * @param o element to be removed from this list, if present
   * @return {@code true} if the list contained the specified element
   * @since 1.6
   */
  public boolean removeLastOccurrence(Object o) {
    if (o == null) {
      for (Node x = last; x != null; x = x.prev) {
        if (x.item == null) {
          unlink(x);
          return true;
        }
      }
    } else {
      for (Node x = last; x != null; x = x.prev) {
        if (o.equals(x.item)) {
          unlink(x);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns a list-iterator of the elements in this list (in proper sequence), starting at the
   * specified position in the list. Obeys the general contract of {@code List.listIterator(int)}.
   *
   * <p>The list-iterator is <i>fail-fast</i>: if the list is structurally modified at any time
   * after the Iterator is created, in any way except through the list-iterator's own {@code remove}
   * or {@code add} methods, the list-iterator will throw a {@code ConcurrentModificationException}.
   * Thus, in the face of concurrent modification, the iterator fails quickly and cleanly, rather
   * than risking arbitrary, non-deterministic behavior at an undetermined time in the future.
   *
   * @param index index of the first element to be returned from the list-iterator (by a call to
   *     {@code next})
   * @return a ListIterator of the elements in this list (in proper sequence), starting at the
   *     specified position in the list
   * @throws IndexOutOfBoundsException {@inheritDoc}
   * @see List#listIterator(int)
   */
  public ListIterator listIterator(int index) {
    checkPositionIndex(index);
    return new ListItr(index);
  }

  /** @since 1.6 */
  public Iterator descendingIterator() {
    return new DescendingIterator();
  }

  @SuppressWarnings("unchecked")
  private LL superClone() {
    try {
      return (LL) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new InternalError(e);
    }
  }

  /**
   * Returns a shallow copy of this {@code LinkedList}. (The elements themselves are not cloned.)
   *
   * @return a shallow copy of this {@code LinkedList} instance
   */
  public Object clone() {
    LL clone = superClone();

    // Put clone into "virgin" state
    clone.first = clone.last = null;
    clone.size = 0;
    clone.modCount = 0;

    // Initialize clone with our elements
    for (Node x = first; x != null; x = x.next) clone.add(x.item);

    return clone;
  }

  /**
   * Returns an array containing all of the elements in this list in proper sequence (from first to
   * last element).
   *
   * <p>The returned array will be "safe" in that no references to it are maintained by this list.
   * (In other words, this method must allocate a new array). The caller is thus free to modify the
   * returned array.
   *
   * <p>This method acts as bridge between array-based and collection-based APIs.
   *
   * @return an array containing all of the elements in this list in proper sequence
   */
  public Object[] toArray() {
    Object[] result = new Object[size];
    int i = 0;
    for (Node x = first; x != null; x = x.next) result[i++] = x.item;
    return result;
  }

  /**
   * Returns an array containing all of the elements in this list in proper sequence (from first to
   * last element); the runtime type of the returned array is that of the specified array. If the
   * list fits in the specified array, it is returned therein. Otherwise, a new array is allocated
   * with the runtime type of the specified array and the size of this list.
   *
   * <p>If the list fits in the specified array with room to spare (i.e., the array has more
   * elements than the list), the element in the array immediately following the end of the list is
   * set to {@code null}. (This is useful in determining the length of the list <i>only</i> if the
   * caller knows that the list does not contain any null elements.)
   *
   * <p>Like the {@link #toArray()} method, this method acts as bridge between array-based and
   * collection-based APIs. Further, this method allows precise control over the runtime type of the
   * output array, and may, under certain circumstances, be used to save allocation costs.
   *
   * <p>Suppose {@code x} is a list known to contain only strings. The following code can be used to
   * dump the list into a newly allocated array of {@code String}:
   *
   * <pre>
   * String[] y = x.toArray(new String[0]);
   * </pre>
   *
   * Note that {@code toArray(new Object[0])} is identical in function to {@code toArray()}.
   *
   * @param a the array into which the elements of the list are to be stored, if it is big enough;
   *     otherwise, a new array of the same runtime type is allocated for this purpose.
   * @return an array containing the elements of the list
   * @throws ArrayStoreException if the runtime type of the specified array is not a supertype of
   *     the runtime type of every element in this list
   * @throws NullPointerException if the specified array is null
   */
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a) {
    if (a.length < size)
      a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
    int i = 0;
    Object[] result = a;
    for (Node x = first; x != null; x = x.next) result[i++] = x.item;

    if (a.length > size) a[size] = null;

    return a;
  }

  /**
   * Saves the state of this {@code LinkedList} instance to a stream (that is, serializes it).
   *
   * @serialData The size of the list (the number of elements it contains) is emitted (int),
   *     followed by all of its elements (each an Object) in the proper order.
   */
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    // Write out any hidden serialization magic
    s.defaultWriteObject();

    // Write out size
    s.writeInt(size);

    // Write out all elements in the proper order.
    for (Node x = first; x != null; x = x.next) s.writeObject(x.item);
  }

  /** Reconstitutes this {@code LinkedList} instance from a stream (that is, deserializes it). */
  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream s)
      throws java.io.IOException, ClassNotFoundException {
    // Read in any hidden serialization magic
    s.defaultReadObject();

    // Read in size
    int size = s.readInt();

    // Read in all elements in the proper order.
    for (int i = 0; i < size; i++) linkLast(s.readObject());
  }

  /**
   * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em> and <em>fail-fast</em>
   * {@link Spliterator} over the elements in this list.
   *
   * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and {@link Spliterator#ORDERED}.
   * Overriding implementations should document the reporting of additional characteristic values.
   *
   * @implNote The {@code Spliterator} additionally reports {@link Spliterator#SUBSIZED} and
   *     implements {@code trySplit} to permit limited parallelism..
   * @return a {@code Spliterator} over the elements in this list
   * @since 1.8
   */
  public Spliterator spliterator() {
    return new LLSpliterator(this, -1, 0);
  }

  private static class Node {
    Object item;
    Node next;
    Node prev;

    Node(Node prev, Object element, Node next) {
      this.item = element;
      this.next = next;
      this.prev = prev;
    }
  }

  /** A customized variant of Spliterators.IteratorSpliterator */
  static final class LLSpliterator implements Spliterator {
    static final int BATCH_UNIT = 1 << 10; // batch array size increment
    static final int MAX_BATCH = 1 << 25; // max batch array size;
    final LL list; // null OK unless traversed
    Node current; // current node; null until initialized
    int est; // size estimate; -1 until first needed
    int expectedModCount; // initialized when est set
    int batch; // batch size for splits

    LLSpliterator(LL list, int est, int expectedModCount) {
      this.list = list;
      this.est = est;
      this.expectedModCount = expectedModCount;
    }

    final int getEst() {
      int s; // force initialization
      final LL lst;
      if ((s = est) < 0) {
        if ((lst = list) == null) s = est = 0;
        else {
          expectedModCount = lst.modCount;
          current = lst.first;
          s = est = lst.size;
        }
      }
      return s;
    }

    public long estimateSize() {
      return getEst();
    }

    public Spliterator trySplit() {
      Node p;
      int s = getEst();
      if (s > 1 && (p = current) != null) {
        int n = batch + BATCH_UNIT;
        if (n > s) n = s;
        if (n > MAX_BATCH) n = MAX_BATCH;
        Object[] a = new Object[n];
        int j = 0;
        do {
          a[j++] = p.item;
        } while ((p = p.next) != null && j < n);
        current = p;
        batch = j;
        est = s - j;
        return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
      }
      return null;
    }

    public void forEachRemaining(Consumer action) {
      Node p;
      int n;
      if (action == null) throw new NullPointerException();
      if ((n = getEst()) > 0 && (p = current) != null) {
        current = null;
        est = 0;
        do {
          Object e = p.item;
          p = p.next;
          action.accept(e);
        } while (p != null && --n > 0);
      }
      if (list.modCount != expectedModCount) throw new ConcurrentModificationException();
    }

    public boolean tryAdvance(Consumer action) {
      Node p;
      if (action == null) throw new NullPointerException();
      if (getEst() > 0 && (p = current) != null) {
        --est;
        Object e = p.item;
        current = p.next;
        action.accept(e);
        if (list.modCount != expectedModCount) throw new ConcurrentModificationException();
        return true;
      }
      return false;
    }

    public int characteristics() {
      return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
    }
  }

  private class ListItr implements ListIterator {
    private Node lastReturned = null;
    private Node next;
    private int nextIndex;
    private int expectedModCount = modCount;

    ListItr(int index) {
      // assert isPositionIndex(index);
      next = (index == size) ? null : node(index);
      nextIndex = index;
    }

    public boolean hasNext() {
      return nextIndex < size;
    }

    public Object next() {
      checkForComodification();
      if (!hasNext()) throw new NoSuchElementException();

      lastReturned = next;
      next = next.next;
      nextIndex++;
      return lastReturned.item;
    }

    public boolean hasPrevious() {
      return nextIndex > 0;
    }

    public Object previous() {
      checkForComodification();
      if (!hasPrevious()) throw new NoSuchElementException();

      lastReturned = next = (next == null) ? last : next.prev;
      nextIndex--;
      return lastReturned.item;
    }

    public int nextIndex() {
      return nextIndex;
    }

    public int previousIndex() {
      return nextIndex - 1;
    }

    public void remove() {
      checkForComodification();
      if (lastReturned == null) throw new IllegalStateException();

      Node lastNext = lastReturned.next;
      unlink(lastReturned);
      if (next == lastReturned) next = lastNext;
      else nextIndex--;
      lastReturned = null;
      expectedModCount++;
    }

    public void set(Object e) {
      if (lastReturned == null) throw new IllegalStateException();
      checkForComodification();
      lastReturned.item = e;
    }

    public void add(Object e) {
      checkForComodification();
      lastReturned = null;
      if (next == null) linkLast(e);
      else linkBefore(e, next);
      nextIndex++;
      expectedModCount++;
    }

    public void forEachRemaining(Consumer action) {
      Objects.requireNonNull(action);
      while (modCount == expectedModCount && nextIndex < size) {
        action.accept(next.item);
        lastReturned = next;
        next = next.next;
        nextIndex++;
      }
      checkForComodification();
    }

    final void checkForComodification() {
      if (modCount != expectedModCount) throw new ConcurrentModificationException();
    }
  }

  /** Adapter to provide descending iterators via ListItr.previous */
  private class DescendingIterator implements Iterator {
    private final ListItr itr = new ListItr(size());

    public boolean hasNext() {
      return itr.hasPrevious();
    }

    public Object next() {
      return itr.previous();
    }

    public void remove() {
      itr.remove();
    }
  }
}
