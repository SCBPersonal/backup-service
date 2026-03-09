# 📚 Java Data Structures - Quick Reference Guide

> **Complete reference for all major Java data structures with examples, time complexity, and use cases**

---

## 📊 Data Structures Comparison Table

| Data Structure | Ordered | Sorted | Duplicates | Null | Thread-Safe | Access Time | Search Time | Insert Time | Delete Time |
|----------------|---------|--------|------------|------|-------------|-------------|-------------|-------------|-------------|
| **Array** | ✅ | ❌ | ✅ | ✅ | ✅ | O(1) | O(n) | O(n) | O(n) |
| **ArrayList** | ✅ | ❌ | ✅ | ✅ | ❌ | O(1) | O(n) | O(1)* | O(n) |
| **LinkedList** | ✅ | ❌ | ✅ | ✅ | ❌ | O(n) | O(n) | O(1) | O(1) |
| **Stack** | ✅ (LIFO) | ❌ | ✅ | ✅ | ✅ | O(1) | O(n) | O(1) | O(1) |
| **Queue** | ✅ (FIFO) | ❌ | ✅ | ❌ | ❌ | O(1) | O(n) | O(1) | O(1) |
| **HashMap** | ❌ | ❌ | Values | 1 key | ❌ | O(1) | O(1) | O(1) | O(1) |
| **LinkedHashMap** | ✅ | ❌ | Values | 1 key | ❌ | O(1) | O(1) | O(1) | O(1) |
| **TreeMap** | ✅ | ✅ | Values | ❌ | ❌ | O(log n) | O(log n) | O(log n) | O(log n) |
| **HashSet** | ❌ | ❌ | ❌ | 1 null | ❌ | O(1) | O(1) | O(1) | O(1) |
| **LinkedHashSet** | ✅ | ❌ | ❌ | 1 null | ❌ | O(1) | O(1) | O(1) | O(1) |
| **TreeSet** | ✅ | ✅ | ❌ | ❌ | ❌ | O(log n) | O(log n) | O(log n) | O(log n) |
| **PriorityQueue** | ❌ | ✅ (heap) | ✅ | ❌ | ❌ | O(1) | O(n) | O(log n) | O(log n) |
| **ArrayDeque** | ✅ | ❌ | ✅ | ❌ | ❌ | O(1) | O(n) | O(1) | O(1) |

*Amortized time complexity

---

## 🎯 When to Use Which Data Structure

### Use **Array** when:
- ✅ Size is fixed and known in advance
- ✅ Need fast random access by index
- ✅ Memory efficiency is critical
- ✅ Working with primitive types

### Use **ArrayList** when:
- ✅ Need dynamic resizing
- ✅ Frequent random access by index
- ✅ Iteration is common
- ✅ Insertions/deletions are mostly at the end

### Use **LinkedList** when:
- ✅ Frequent insertions/deletions at beginning or middle
- ✅ Implementing stack or queue
- ✅ Don't need random access
- ✅ Memory overhead is acceptable

### Use **Stack** when:
- ✅ Need LIFO (Last-In-First-Out) behavior
- ✅ Implementing undo/redo functionality
- ✅ Expression evaluation
- ✅ Backtracking algorithms
- ⚠️ **Prefer ArrayDeque over Stack class**

### Use **Queue** when:
- ✅ Need FIFO (First-In-First-Out) behavior
- ✅ Task scheduling
- ✅ Breadth-First Search (BFS)
- ✅ Request handling

### Use **HashMap** when:
- ✅ Need fast key-value lookups
- ✅ Keys are unique
- ✅ Order doesn't matter
- ✅ Caching/memoization

### Use **LinkedHashMap** when:
- ✅ Need HashMap with insertion order
- ✅ Implementing LRU cache
- ✅ Predictable iteration order needed

### Use **TreeMap** when:
- ✅ Need sorted key-value pairs
- ✅ Range queries needed
- ✅ Navigation operations (floor, ceiling, etc.)
- ✅ Sorted iteration required

### Use **HashSet** when:
- ✅ Need unique elements
- ✅ Fast lookup/insertion/deletion
- ✅ Remove duplicates
- ✅ Set operations (union, intersection)

### Use **LinkedHashSet** when:
- ✅ Need HashSet with insertion order
- ✅ Unique elements with predictable iteration

### Use **TreeSet** when:
- ✅ Need sorted unique elements
- ✅ Range queries on elements
- ✅ Navigation operations needed

### Use **PriorityQueue** when:
- ✅ Need elements in priority order
- ✅ Implementing heap-based algorithms
- ✅ Finding min/max efficiently
- ✅ Dijkstra's algorithm, Huffman coding

### Use **ArrayDeque** when:
- ✅ Need double-ended queue
- ✅ Implementing stack (better than Stack class)
- ✅ Implementing queue (better than LinkedList)
- ✅ No random access needed

---

## 💡 Common Patterns and Examples

### Pattern 1: Remove Duplicates
```java
// Using HashSet
List<Integer> list = Arrays.asList(1, 2, 3, 2, 4, 3, 5);
List<Integer> unique = new ArrayList<>(new HashSet<>(list));
```

### Pattern 2: Frequency Counter
```java
// Using HashMap
Map<String, Integer> frequency = new HashMap<>();
for (String word : words) {
    frequency.put(word, frequency.getOrDefault(word, 0) + 1);
}
```

### Pattern 3: LRU Cache
```java
// Using LinkedHashMap
LinkedHashMap<K, V> cache = new LinkedHashMap<>(capacity, 0.75f, true) {
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
};
```

### Pattern 4: Top K Elements
```java
// Using PriorityQueue
PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);
for (int num : nums) {
    minHeap.offer(num);
    if (minHeap.size() > k) minHeap.poll();
}
```

### Pattern 5: Sliding Window
```java
// Using Deque
Deque<Integer> deque = new ArrayDeque<>();
for (int i = 0; i < nums.length; i++) {
    // Remove elements outside window
    while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
        deque.pollFirst();
    }
    // Process current element
    deque.offerLast(i);
}
```

### Pattern 6: Two Sum
```java
// Using HashMap
Map<Integer, Integer> map = new HashMap<>();
for (int i = 0; i < nums.length; i++) {
    int complement = target - nums[i];
    if (map.containsKey(complement)) {
        return new int[]{map.get(complement), i};
    }
    map.put(nums[i], i);
}
```

---

## 🔍 Interview Tips

### Time Complexity Cheat Sheet:
- **O(1)** - Constant: HashMap get/put, Array access
- **O(log n)** - Logarithmic: TreeMap operations, Binary search
- **O(n)** - Linear: ArrayList search, LinkedList access
- **O(n log n)** - Linearithmic: Sorting (Collections.sort)
- **O(n²)** - Quadratic: Nested loops, Bubble sort

### Space Complexity Tips:
- Arrays and ArrayList: O(n)
- LinkedList: O(n) with extra overhead for node pointers
- HashMap/HashSet: O(n) with load factor overhead
- TreeMap/TreeSet: O(n) with tree structure overhead

### Common Mistakes to Avoid:
1. ❌ Using `==` instead of `.equals()` for object comparison
2. ❌ Modifying collection while iterating (use Iterator.remove())
3. ❌ Not overriding `hashCode()` when overriding `equals()`
4. ❌ Using Stack class instead of ArrayDeque
5. ❌ Not considering null values in HashMap/HashSet
6. ❌ Assuming HashMap maintains order
7. ❌ Using ArrayList for frequent insertions at beginning

---

## 📖 Best Practices

1. **Choose the right data structure** based on your use case
2. **Specify initial capacity** for collections when size is known
3. **Use generics** to ensure type safety
4. **Prefer interfaces** over concrete classes (List vs ArrayList)
5. **Use enhanced for-loop** or streams for iteration
6. **Consider thread-safety** requirements
7. **Override equals() and hashCode()** for custom objects in HashMap/HashSet
8. **Use immutable collections** when possible (Collections.unmodifiableList)
9. **Prefer ArrayDeque** over Stack and LinkedList for stack/queue operations
10. **Use try-with-resources** for collections that implement AutoCloseable

---

**End of Quick Reference Guide** ✅

