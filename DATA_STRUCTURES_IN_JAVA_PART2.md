# 📚 Data Structures in Java - Part 2

> **Continuation: HashMap, HashSet, TreeMap, TreeSet, PriorityQueue, and Advanced Topics**

---

## 6️⃣ HashMap {#hashmap}

### Definition:
Hash table-based implementation of the Map interface. Stores key-value pairs with no duplicate keys.

### Characteristics:
- ✅ **Key-Value Pairs** - Each entry has a unique key
- ✅ **No Duplicate Keys** - Keys must be unique
- ✅ **Allows Null** - One null key and multiple null values
- ✅ **Fast Lookup** - O(1) average time for get/put
- ❌ **No Order** - Does not maintain insertion order
- ❌ **Not Thread-Safe** - Use `ConcurrentHashMap` for thread safety

### Internal Working:

```
HashMap Structure:
┌─────────────────────────────────┐
│ Bucket Array (default size: 16) │
├─────────────────────────────────┤
│ [0] → null                      │
│ [1] → Entry(key1, value1)       │
│ [2] → Entry(key2, value2) → Entry(key3, value3) (collision chain)
│ [3] → null                      │
│ ...                             │
└─────────────────────────────────┘

Hash Function: index = hashCode(key) % bucketSize
Load Factor: 0.75 (resize when 75% full)
```

### Declaration and Operations:

```java
import java.util.HashMap;
import java.util.Map;

public class HashMapExample {
    public static void main(String[] args) {
        // Create HashMap
        HashMap<String, Integer> map = new HashMap<>();

        // Put key-value pairs
        map.put("Apple", 100);
        map.put("Banana", 50);
        map.put("Orange", 75);
        map.put("Apple", 120);  // Updates existing key

        // Get value by key
        Integer applePrice = map.get("Apple");  // 120

        // Get with default value
        Integer grapePrice = map.getOrDefault("Grape", 0);  // 0

        // Check if key exists
        boolean hasApple = map.containsKey("Apple");  // true

        // Check if value exists
        boolean has100 = map.containsValue(100);  // false (was updated to 120)

        // Remove entry
        Integer removed = map.remove("Banana");  // 50

        // Size
        int size = map.size();  // 2

        // Check if empty
        boolean isEmpty = map.isEmpty();  // false

        // Iterate over keys
        for (String key : map.keySet()) {
            System.out.println(key + " = " + map.get(key));
        }

        // Iterate over values
        for (Integer value : map.values()) {
            System.out.println(value);
        }

        // Iterate over entries
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }

        // Java 8+ forEach
        map.forEach((key, value) -> System.out.println(key + " = " + value));

        // Compute if absent
        map.computeIfAbsent("Grape", k -> 90);

        // Merge
        map.merge("Apple", 10, (oldVal, newVal) -> oldVal + newVal);  // 130

        // Clear all entries
        map.clear();
    }
}
```

### Real-World Example - Word Frequency Counter:

```java
import java.util.HashMap;
import java.util.Map;

public class WordFrequencyCounter {

    public static Map<String, Integer> countWords(String text) {
        Map<String, Integer> wordCount = new HashMap<>();

        // Split text into words
        String[] words = text.toLowerCase().split("\\s+");

        for (String word : words) {
            // Remove punctuation
            word = word.replaceAll("[^a-zA-Z]", "");

            if (!word.isEmpty()) {
                // Increment count
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }

        return wordCount;
    }

    public static void main(String[] args) {
        String text = "Java is great. Java is powerful. Java is everywhere!";

        Map<String, Integer> frequency = countWords(text);

        // Display results
        frequency.forEach((word, count) ->
            System.out.println(word + ": " + count));

        // Find most frequent word
        String mostFrequent = frequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");

        System.out.println("\nMost frequent word: " + mostFrequent);
    }
}


### Time Complexity:
| Operation | Average | Worst Case | Notes |
|-----------|---------|------------|-------|
| get(key) | O(1) | O(n) | Worst case when all keys hash to same bucket |
| put(key, value) | O(1) | O(n) | Worst case with collisions |
| remove(key) | O(1) | O(n) | Worst case with collisions |
| containsKey(key) | O(1) | O(n) | Worst case with collisions |

### HashMap vs Hashtable:
| Feature | HashMap | Hashtable |
|---------|---------|-----------|
| Thread-Safe | No ❌ | Yes ✅ |
| Null Keys | Allowed ✅ | Not allowed ❌ |
| Null Values | Allowed ✅ | Not allowed ❌ |
| Performance | Faster ✅ | Slower ❌ |
| Legacy | Modern ✅ | Legacy ❌ |

### When to Use HashMap:
- ✅ Fast key-value lookups needed
- ✅ Unique keys required
- ✅ Order doesn't matter
- ✅ Caching/memoization
- ❌ Avoid when order matters (use LinkedHashMap)
- ❌ Avoid when sorted keys needed (use TreeMap)

---

## 7️⃣ HashSet {#hashset}

### Definition:
Set implementation backed by a HashMap. Stores unique elements with no duplicates.

### Characteristics:
- ✅ **No Duplicates** - Automatically removes duplicates
- ✅ **Allows Null** - Can store one null element
- ✅ **Fast Operations** - O(1) for add, remove, contains
- ❌ **No Order** - Does not maintain insertion order
- ❌ **No Index Access** - Cannot access by index

### Internal Working:
```
HashSet internally uses HashMap:
HashSet<E> → HashMap<E, Object>
  Element  →  Key, Dummy Value (PRESENT)
```

### Declaration and Operations:

```java
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class HashSetExample {
    public static void main(String[] args) {
        // Create HashSet
        HashSet<String> set = new HashSet<>();

        // Add elements
        set.add("Apple");
        set.add("Banana");
        set.add("Orange");
        set.add("Apple");  // Duplicate - will be ignored

        // Check if contains
        boolean hasApple = set.contains("Apple");  // true

        // Remove element
        boolean removed = set.remove("Banana");  // true

        // Size
        int size = set.size();  // 2

        // Check if empty
        boolean isEmpty = set.isEmpty();  // false

        // Iterate
        for (String fruit : set) {
            System.out.println(fruit);
        }

        // Java 8+ forEach
        set.forEach(System.out::println);

        // Convert array to set (removes duplicates)
        Integer[] arr = {1, 2, 3, 2, 4, 3, 5};
        Set<Integer> uniqueNumbers = new HashSet<>(Arrays.asList(arr));
        System.out.println(uniqueNumbers);  // [1, 2, 3, 4, 5]

        // Set operations
        HashSet<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        HashSet<Integer> set2 = new HashSet<>(Arrays.asList(3, 4, 5, 6));

        // Union
        HashSet<Integer> union = new HashSet<>(set1);
        union.addAll(set2);  // [1, 2, 3, 4, 5, 6]

        // Intersection
        HashSet<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);  // [3, 4]

        // Difference
        HashSet<Integer> difference = new HashSet<>(set1);
        difference.removeAll(set2);  // [1, 2]

        // Clear all elements
        set.clear();
    }
}
```

### Real-World Example - Remove Duplicates:

```java
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class DuplicateRemover {

    public static <T> List<T> removeDuplicates(List<T> list) {
        return new ArrayList<>(new HashSet<>(list));
    }

    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 2, 4, 3, 5, 1);
        List<Integer> unique = removeDuplicates(numbers);
        System.out.println("Original: " + numbers);
        System.out.println("Unique: " + unique);

        List<String> words = List.of("apple", "banana", "apple", "orange", "banana");
        List<String> uniqueWords = removeDuplicates(words);
        System.out.println("Original: " + words);
        System.out.println("Unique: " + uniqueWords);
    }
}
```

### Real-World Example - Find Common Elements:

```java
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class CommonElementsFinder {

    public static <T> Set<T> findCommon(T[] arr1, T[] arr2) {
        Set<T> set1 = new HashSet<>(Arrays.asList(arr1));
        Set<T> set2 = new HashSet<>(Arrays.asList(arr2));

        set1.retainAll(set2);  // Intersection
        return set1;
    }

    public static void main(String[] args) {
        Integer[] array1 = {1, 2, 3, 4, 5};
        Integer[] array2 = {4, 5, 6, 7, 8};

        Set<Integer> common = findCommon(array1, array2);
        System.out.println("Common elements: " + common);  // [4, 5]

        String[] names1 = {"Alice", "Bob", "Charlie", "David"};
        String[] names2 = {"Bob", "David", "Eve", "Frank"};

        Set<String> commonNames = findCommon(names1, names2);
        System.out.println("Common names: " + commonNames);  // [Bob, David]
    }
}
```

### Time Complexity:
| Operation | Average | Worst Case |
|-----------|---------|------------|
| add(element) | O(1) | O(n) |
| remove(element) | O(1) | O(n) |
| contains(element) | O(1) | O(n) |
| size() | O(1) | O(1) |

### When to Use HashSet:
- ✅ Need to store unique elements
- ✅ Fast lookup/insertion/deletion
- ✅ Remove duplicates from collection
- ✅ Set operations (union, intersection)
- ❌ Avoid when order matters (use LinkedHashSet)
- ❌ Avoid when sorted order needed (use TreeSet)

---

## 8️⃣ TreeMap {#treemap}

### Definition:
Red-Black tree-based implementation of NavigableMap. Stores key-value pairs in sorted order.

### Characteristics:
- ✅ **Sorted Order** - Keys are sorted (natural or custom order)
- ✅ **NavigableMap** - Provides navigation methods
- ✅ **No Null Keys** - Does not allow null keys
- ✅ **Allows Null Values** - Can have null values
- ❌ **Slower** - O(log n) operations vs O(1) for HashMap

### Declaration and Operations:

```java
import java.util.TreeMap;
import java.util.Map;
import java.util.Comparator;

public class TreeMapExample {
    public static void main(String[] args) {
        // Natural ordering (ascending)
        TreeMap<Integer, String> map = new TreeMap<>();

        map.put(3, "Three");
        map.put(1, "One");
        map.put(4, "Four");
        map.put(2, "Two");

        System.out.println(map);  // {1=One, 2=Two, 3=Three, 4=Four}

        // Custom ordering (descending)
        TreeMap<Integer, String> descMap = new TreeMap<>(Comparator.reverseOrder());
        descMap.putAll(map);
        System.out.println(descMap);  // {4=Four, 3=Three, 2=Two, 1=One}

        // Navigation methods
        Map.Entry<Integer, String> firstEntry = map.firstEntry();  // 1=One
        Map.Entry<Integer, String> lastEntry = map.lastEntry();    // 4=Four

        Integer firstKey = map.firstKey();  // 1
        Integer lastKey = map.lastKey();    // 4

        // Floor and Ceiling
        Integer floorKey = map.floorKey(2);    // 2 (≤ 2)
        Integer ceilingKey = map.ceilingKey(2); // 2 (≥ 2)

        // Lower and Higher
        Integer lowerKey = map.lowerKey(2);    // 1 (< 2)
        Integer higherKey = map.higherKey(2);  // 3 (> 2)

        // SubMap
        Map<Integer, String> subMap = map.subMap(2, 4);  // {2=Two, 3=Three}

        // HeadMap and TailMap
        Map<Integer, String> headMap = map.headMap(3);  // {1=One, 2=Two}
        Map<Integer, String> tailMap = map.tailMap(3);  // {3=Three, 4=Four}

        // Poll (remove and return)
        Map.Entry<Integer, String> polledFirst = map.pollFirstEntry();
        Map.Entry<Integer, String> polledLast = map.pollLastEntry();
    }
}
```

```

### Real-World Example - Student Grade Book:

```java
import java.util.HashMap;
import java.util.Map;

class Student {
    String name;
    int rollNo;

    Student(String name, int rollNo) {
        this.name = name;
        this.rollNo = rollNo;
    }

    @Override
    public String toString() {
        return name + " (" + rollNo + ")";
    }
}

public class GradeBook {
    private Map<Integer, Map<String, Double>> gradeBook;

    public GradeBook() {
        gradeBook = new HashMap<>();
    }

    public void addGrade(int rollNo, String subject, double marks) {
        gradeBook.computeIfAbsent(rollNo, k -> new HashMap<>())
                 .put(subject, marks);
    }

    public double getGrade(int rollNo, String subject) {
        return gradeBook.getOrDefault(rollNo, new HashMap<>())
                       .getOrDefault(subject, 0.0);
    }

    public double getAverage(int rollNo) {
        Map<String, Double> grades = gradeBook.get(rollNo);
        if (grades == null || grades.isEmpty()) return 0.0;

        return grades.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
    }

    public void displayGrades(int rollNo) {
        Map<String, Double> grades = gradeBook.get(rollNo);
        if (grades == null) {
            System.out.println("No grades found for roll number: " + rollNo);
            return;
        }

        System.out.println("Grades for Roll No " + rollNo + ":");
        grades.forEach((subject, marks) ->
            System.out.println("  " + subject + ": " + marks));
        System.out.println("  Average: " + getAverage(rollNo));
    }

    public static void main(String[] args) {
        GradeBook gradeBook = new GradeBook();

        gradeBook.addGrade(101, "Math", 85.5);
        gradeBook.addGrade(101, "Science", 92.0);
        gradeBook.addGrade(101, "English", 78.5);

        gradeBook.addGrade(102, "Math", 90.0);
        gradeBook.addGrade(102, "Science", 88.5);

        gradeBook.displayGrades(101);
        gradeBook.displayGrades(102);
    }
}
```

