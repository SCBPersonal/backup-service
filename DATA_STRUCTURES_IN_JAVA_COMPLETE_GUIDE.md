# 📚 Data Structures in Java - Complete Guide

> **Comprehensive guide covering all major data structures in Java with detailed explanations, time complexity analysis, and practical examples**

---

## 📋 Table of Contents

1. [Introduction to Data Structures](#introduction)
2. [Arrays](#arrays)
3. [ArrayList](#arraylist)
4. [LinkedList](#linkedlist)
5. [Stack](#stack)
6. [Queue](#queue)
7. [HashMap](#hashmap)
8. [HashSet](#hashset)
9. [TreeMap](#treemap)
10. [TreeSet](#treeset)
11. [PriorityQueue](#priorityqueue)
12. [Deque](#deque)
13. [Time Complexity Comparison](#time-complexity)
14. [Best Practices](#best-practices)

---

## 🎯 Introduction to Data Structures {#introduction}

**Data Structure** is a way of organizing and storing data in a computer so that it can be accessed and modified efficiently.

### Why Data Structures Matter:
- ✅ **Performance Optimization** - Choose the right structure for better speed
- ✅ **Memory Efficiency** - Reduce memory footprint
- ✅ **Code Maintainability** - Write cleaner, more organized code
- ✅ **Problem Solving** - Essential for algorithms and interviews

### Java Collections Framework Hierarchy:

```
Collection (Interface)
├── List (Interface)
│   ├── ArrayList (Class)
│   ├── LinkedList (Class)
│   └── Vector (Class)
├── Set (Interface)
│   ├── HashSet (Class)
│   ├── LinkedHashSet (Class)
│   └── TreeSet (Class)
└── Queue (Interface)
    ├── PriorityQueue (Class)
    ├── LinkedList (Class)
    └── Deque (Interface)
        └── ArrayDeque (Class)

Map (Interface)
├── HashMap (Class)
├── LinkedHashMap (Class)
└── TreeMap (Class)
```

---

## 1️⃣ Arrays {#arrays}

### Definition:
Fixed-size, contiguous memory structure that stores elements of the same type.

### Characteristics:
- ✅ **Fixed Size** - Size cannot change after creation
- ✅ **Index-based Access** - O(1) access time
- ✅ **Homogeneous** - All elements must be of the same type
- ❌ **No Built-in Methods** - Limited functionality

### Declaration and Initialization:

```java
// Method 1: Declare and allocate
int[] numbers = new int[5];

// Method 2: Declare and initialize
int[] numbers = {1, 2, 3, 4, 5};

// Method 3: Using new keyword with values
int[] numbers = new int[]{1, 2, 3, 4, 5};

// Multi-dimensional array
int[][] matrix = new int[3][3];
int[][] matrix2 = {{1, 2}, {3, 4}, {5, 6}};
```

### Common Operations:

```java
public class ArrayExample {
    public static void main(String[] args) {
        // Create array
        int[] arr = {10, 20, 30, 40, 50};

        // Access element
        System.out.println("First element: " + arr[0]); // 10

        // Modify element
        arr[2] = 35;

        // Get length
        System.out.println("Length: " + arr.length); // 5

        // Iterate using for loop
        for (int i = 0; i < arr.length; i++) {
            System.out.println(arr[i]);
        }

        // Iterate using enhanced for loop
        for (int num : arr) {
            System.out.println(num);
        }

        // Using Arrays utility class
        Arrays.sort(arr);                    // Sort array
        int index = Arrays.binarySearch(arr, 30); // Binary search
        String str = Arrays.toString(arr);   // Convert to string
        int[] copy = Arrays.copyOf(arr, arr.length); // Copy array
    }
}
```

### Time Complexity:
| Operation | Time Complexity |
|-----------|----------------|
| Access by index | O(1) |
| Search (unsorted) | O(n) |
| Search (sorted) | O(log n) - binary search |
| Insert at end | O(1) |
| Insert at beginning | O(n) |
| Delete | O(n) |

### Use Cases:
- ✅ When size is known and fixed
- ✅ Fast random access needed
- ✅ Memory-efficient storage
- ❌ Avoid when frequent insertions/deletions needed

---

## 2️⃣ ArrayList {#arraylist}

### Definition:
Dynamic array that can grow or shrink in size. Part of `java.util` package.

### Characteristics:
- ✅ **Dynamic Size** - Automatically resizes


### Real-World Example - Student Management:

```java
import java.util.ArrayList;

class Student {
    String name;
    int rollNo;
    double marks;

    Student(String name, int rollNo, double marks) {
        this.name = name;
        this.rollNo = rollNo;
        this.marks = marks;
    }

    @Override
    public String toString() {
        return "Student{name='" + name + "', rollNo=" + rollNo + ", marks=" + marks + "}";
    }
}

public class StudentManagement {
    public static void main(String[] args) {
        ArrayList<Student> students = new ArrayList<>();

        // Add students
        students.add(new Student("Alice", 101, 85.5));
        students.add(new Student("Bob", 102, 92.0));
        students.add(new Student("Charlie", 103, 78.5));

        // Find student by roll number
        Student found = students.stream()
            .filter(s -> s.rollNo == 102)
            .findFirst()
            .orElse(null);

        // Calculate average marks
        double avgMarks = students.stream()
            .mapToDouble(s -> s.marks)
            .average()
            .orElse(0.0);

        System.out.println("Average Marks: " + avgMarks);

        // Sort by marks (descending)
        students.sort((s1, s2) -> Double.compare(s2.marks, s1.marks));

        // Display all students
        students.forEach(System.out::println);
    }
}
```

### Time Complexity:
| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| get(index) | O(1) | Direct array access |
| add(element) | O(1) amortized | O(n) when resizing |
| add(index, element) | O(n) | Shift elements |
| remove(index) | O(n) | Shift elements |
| contains(element) | O(n) | Linear search |
| size() | O(1) | Stored as variable |

### When to Use ArrayList:
- ✅ Frequent random access by index
- ✅ Iteration over elements
- ✅ Size changes infrequently
- ❌ Avoid for frequent insertions/deletions in middle

---

## 3️⃣ LinkedList {#linkedlist}

### Definition:
Doubly-linked list implementation. Each element (node) contains data and references to next and previous nodes.

### Characteristics:
- ✅ **Dynamic Size** - Grows/shrinks easily
- ✅ **Efficient Insertions/Deletions** - O(1) at beginning/end
- ✅ **Implements List and Deque** - Can be used as list, stack, or queue
- ❌ **Slow Random Access** - O(n) to access by index
- ❌ **More Memory** - Stores references to next/previous nodes

### Internal Structure:

```
Node Structure:
┌─────────┬──────┬─────────┐
│ prev    │ data │ next    │
└─────────┴──────┴─────────┘

LinkedList:
null ← [Node1] ↔ [Node2] ↔ [Node3] → null
```

### Declaration and Operations:

```java
import java.util.LinkedList;

public class LinkedListExample {
    public static void main(String[] args) {
        LinkedList<String> list = new LinkedList<>();

        // Add elements
        list.add("A");              // Add at end
        list.addFirst("Z");         // Add at beginning
        list.addLast("B");          // Add at end
        list.add(1, "M");           // Add at index

        // Access elements
        String first = list.getFirst();
        String last = list.getLast();
        String atIndex = list.get(2);

        // Remove elements
        list.removeFirst();         // Remove first
        list.removeLast();          // Remove last
        list.remove(1);             // Remove at index
        list.remove("A");           // Remove by value

        // Peek operations (don't remove)
        String peekFirst = list.peekFirst();
        String peekLast = list.peekLast();

        // Poll operations (remove and return)
        String pollFirst = list.pollFirst();
        String pollLast = list.pollLast();

        // Use as Stack (LIFO)
        list.push("X");             // Add to front
        String popped = list.pop(); // Remove from front

        // Use as Queue (FIFO)
        list.offer("Y");            // Add to end
        String polled = list.poll(); // Remove from front

        // Iterate
        for (String item : list) {
            System.out.println(item);
        }
    }
}
```

### Real-World Example - Browser History:

```java
import java.util.LinkedList;

class BrowserHistory {
    private LinkedList<String> history;
    private int currentIndex;

    public BrowserHistory(String homepage) {
        history = new LinkedList<>();
        history.add(homepage);
        currentIndex = 0;
    }

    public void visit(String url) {
        // Remove all forward history
        while (history.size() > currentIndex + 1) {
            history.removeLast();
        }
        history.add(url);
        currentIndex++;
    }

    public String back(int steps) {
        currentIndex = Math.max(0, currentIndex - steps);
        return history.get(currentIndex);
    }

    public String forward(int steps) {
        currentIndex = Math.min(history.size() - 1, currentIndex + steps);
        return history.get(currentIndex);
    }

    public void displayHistory() {
        for (int i = 0; i < history.size(); i++) {
            String marker = (i == currentIndex) ? " <-- Current" : "";
            System.out.println(history.get(i) + marker);
        }
    }
}

public class BrowserHistoryDemo {
    public static void main(String[] args) {
        BrowserHistory browser = new BrowserHistory("google.com");
        browser.visit("youtube.com");
        browser.visit("facebook.com");
        browser.visit("twitter.com");

        System.out.println("Current: " + browser.back(2));  // youtube.com
        System.out.println("Current: " + browser.forward(1)); // facebook.com

        browser.displayHistory();
    }
}
```

### Time Complexity:
| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| get(index) | O(n) | Must traverse from head/tail |
| addFirst() | O(1) | Direct pointer update |
| addLast() | O(1) | Direct pointer update |
| add(index, element) | O(n) | Must traverse to index |
| removeFirst() | O(1) | Direct pointer update |
| removeLast() | O(1) | Direct pointer update |
| contains(element) | O(n) | Linear search |

### ArrayList vs LinkedList:
| Feature | ArrayList | LinkedList |
|---------|-----------|------------|
| Random Access | O(1) ✅ | O(n) ❌ |
| Insert at Beginning | O(n) ❌ | O(1) ✅ |
| Insert at End | O(1) ✅ | O(1) ✅ |
| Memory Overhead | Low ✅ | High ❌ |
| Cache Performance | Better ✅ | Worse ❌ |

### When to Use LinkedList:
- ✅ Frequent insertions/deletions at beginning/end
- ✅ Implementing stack or queue
- ✅ Don't need random access
- ❌ Avoid when frequent random access needed

---

## 4️⃣ Stack {#stack}

### Definition:
LIFO (Last-In-First-Out) data structure. Elements are added and removed from the same end (top).

### Characteristics:
- ✅ **LIFO Order** - Last element added is first to be removed
- ✅ **Three Main Operations** - push, pop, peek
- ✅ **Legacy Class** - Part of `java.util.Stack`
- ⚠️ **Prefer Deque** - `ArrayDeque` is recommended over `Stack`

### Stack Operations:

```
Push:           Pop:            Peek:
┌───┐           ┌───┐           ┌───┐
│ 4 │ ← push   │   │ ← pop     │ 4 │ ← peek (no remove)
├───┤           ├───┤           ├───┤
│ 3 │           │ 3 │           │ 3 │
├───┤           ├───┤           ├───┤
│ 2 │           │ 2 │           │ 2 │
├───┤           ├───┤           ├───┤
│ 1 │           │ 1 │           │ 1 │
└───┘           └───┘           └───┘
```

### Implementation Examples:

```java
import java.util.Stack;
import java.util.ArrayDeque;
import java.util.Deque;

public class StackExample {
    public static void main(String[] args) {
        // Using legacy Stack class
        Stack<Integer> stack = new Stack<>();

        // Push elements
        stack.push(10);
        stack.push(20);
        stack.push(30);

        // Peek (view top without removing)
        int top = stack.peek();  // 30

        // Pop (remove and return top)
        int popped = stack.pop(); // 30

        // Check if empty
        boolean isEmpty = stack.isEmpty();

        // Search (returns 1-based position from top)
        int position = stack.search(10); // 2

        // Modern approach using Deque (RECOMMENDED)
        Deque<Integer> stackDeque = new ArrayDeque<>();
        stackDeque.push(10);
        stackDeque.push(20);
        stackDeque.push(30);

        int topDeque = stackDeque.peek();
        int poppedDeque = stackDeque.pop();
    }
}
```

### Real-World Example - Expression Evaluation:

```java
import java.util.Stack;

public class ExpressionEvaluator {

    // Check if parentheses are balanced
    public static boolean isBalanced(String expression) {
        Stack<Character> stack = new Stack<>();

        for (char ch : expression.toCharArray()) {
            if (ch == '(' || ch == '{' || ch == '[') {
                stack.push(ch);
            } else if (ch == ')' || ch == '}' || ch == ']') {
                if (stack.isEmpty()) return false;

                char top = stack.pop();
                if ((ch == ')' && top != '(') ||
                    (ch == '}' && top != '{') ||
                    (ch == ']' && top != '[')) {
                    return false;
                }
            }
        }

        return stack.isEmpty();
    }

    public static void main(String[] args) {
        System.out.println(isBalanced("({[]})")); // true
        System.out.println(isBalanced("({[})")); // false
        System.out.println(isBalanced("((()))")); // true
        System.out.println(isBalanced("(()")); // false
    }
}
```

### Time Complexity:
| Operation | Time Complexity |
|-----------|----------------|
| push() | O(1) |
| pop() | O(1) |
| peek() | O(1) |
| search() | O(n) |
| isEmpty() | O(1) |

### Use Cases:
- ✅ Function call stack (recursion)
- ✅ Undo/Redo operations
- ✅ Expression evaluation
- ✅ Backtracking algorithms
- ✅ Browser back button

---

## 5️⃣ Queue {#queue}

### Definition:
FIFO (First-In-First-Out) data structure. Elements are added at the rear and removed from the front.

### Characteristics:
- ✅ **FIFO Order** - First element added is first to be removed
- ✅ **Interface** - Queue is an interface, not a class
- ✅ **Common Implementations** - LinkedList, ArrayDeque, PriorityQueue
- ✅ **Main Operations** - offer, poll, peek

### Queue Operations:

```
Enqueue (offer):              Dequeue (poll):
Front → [1][2][3] ← Rear     Front → [2][3] ← Rear
              ↑ add here            ↑ remove here
```

### Implementation Examples:

```java
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayDeque;

public class QueueExample {
    public static void main(String[] args) {
        // Using LinkedList
        Queue<String> queue = new LinkedList<>();

        // Add elements (offer is preferred over add)
        queue.offer("First");
        queue.offer("Second");
        queue.offer("Third");

        // Peek (view front without removing)
        String front = queue.peek();  // "First"

        // Poll (remove and return front)
        String removed = queue.poll(); // "First"

        // Check if empty
        boolean isEmpty = queue.isEmpty();

        // Size
        int size = queue.size();

        // Using ArrayDeque (better performance)
        Queue<Integer> queueDeque = new ArrayDeque<>();
        queueDeque.offer(10);
        queueDeque.offer(20);
        queueDeque.offer(30);

        // Iterate
        for (Integer num : queueDeque) {
            System.out.println(num);
        }
    }
}
```

### Real-World Example - Print Queue:

```java
import java.util.Queue;
import java.util.LinkedList;

class PrintJob {
    String documentName;
    int pages;

    PrintJob(String documentName, int pages) {
        this.documentName = documentName;
        this.pages = pages;
    }

    @Override
    public String toString() {
        return documentName + " (" + pages + " pages)";
    }
}

public class PrinterQueue {
    private Queue<PrintJob> printQueue;

    public PrinterQueue() {
        printQueue = new LinkedList<>();
    }

    public void addJob(PrintJob job) {
        printQueue.offer(job);
        System.out.println("Added to queue: " + job);
    }

    public void processNextJob() {
        if (printQueue.isEmpty()) {
            System.out.println("No jobs in queue");
            return;
        }

        PrintJob job = printQueue.poll();
        System.out.println("Printing: " + job);
        // Simulate printing
        try {
            Thread.sleep(job.pages * 100); // 100ms per page
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Completed: " + job);
    }

    public void displayQueue() {
        System.out.println("Current queue: " + printQueue);
    }

    public static void main(String[] args) {
        PrinterQueue printer = new PrinterQueue();

        printer.addJob(new PrintJob("Report.pdf", 5));
        printer.addJob(new PrintJob("Invoice.pdf", 2));
        printer.addJob(new PrintJob("Letter.pdf", 1));

        printer.displayQueue();

        printer.processNextJob();
        printer.processNextJob();

        printer.displayQueue();
    }
}
```

### Time Complexity:
| Operation | Time Complexity |
|-----------|----------------|
| offer() | O(1) |
| poll() | O(1) |
| peek() | O(1) |
| size() | O(1) |
| contains() | O(n) |

### Queue vs Stack:
| Feature | Queue | Stack |
|---------|-------|-------|
| Order | FIFO | LIFO |
| Add | Rear (offer) | Top (push) |
| Remove | Front (poll) | Top (pop) |
| Use Case | Task scheduling | Undo operations |

### Use Cases:
- ✅ Task scheduling
- ✅ Breadth-First Search (BFS)
- ✅ Print queue
- ✅ Message queues
- ✅ Request handling

---

### Declaration and Initialization:

```java
import java.util.ArrayList;
import java.util.Arrays;

// Generic ArrayList
ArrayList<String> list = new ArrayList<>();

// With initial capacity
ArrayList<Integer> numbers = new ArrayList<>(100);

// Initialize with values
ArrayList<String> fruits = new ArrayList<>(Arrays.asList("Apple", "Banana", "Orange"));
```

### Common Operations:

```java
import java.util.ArrayList;
import java.util.Collections;

public class ArrayListExample {
    public static void main(String[] args) {
        ArrayList<String> list = new ArrayList<>();

        // Add elements
        list.add("Java");           // Add at end
        list.add("Python");
        list.add(1, "C++");        // Add at specific index

        // Access elements
        String first = list.get(0);  // Get element at index

        // Modify elements
        list.set(1, "JavaScript");   // Replace element at index

        // Remove elements
        list.remove(0);              // Remove by index
        list.remove("Python");       // Remove by value

        // Check operations
        boolean contains = list.contains("Java");
        int size = list.size();
        boolean isEmpty = list.isEmpty();

        // Iterate
        for (String lang : list) {
            System.out.println(lang);
        }

        // Using lambda (Java 8+)
        list.forEach(lang -> System.out.println(lang));

        // Sorting
        Collections.sort(list);

        // Clear all elements
        list.clear();
    }
}
```

