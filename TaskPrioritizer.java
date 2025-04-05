import java.util.ArrayList;
import java.util.List;

/**
 * TaskPrioritizer class that returns the most urgent
 * available task
 *
 * @author Saloni Shah
 */
public class TaskPrioritizer {

    private TaskHashTable taskTable; // hash table with chaining
    private ReadyHeap readyHeap; // heap for tasks that have no dependencies
    private int insertionCounter; // track order of insertion

    /**
     * Constructor to initialize the TaskPrioritizer
     */
    public TaskPrioritizer() {
        taskTable = new TaskHashTable(1000003);
        readyHeap = new ReadyHeap();
        insertionCounter = 0;
    }

    /**
     * A method to add a new task
     *
     * @param taskId       The string taskId of the task we want to add
     * @param urgencyLevel The integer urgencyLevel of the task we want to add
     * @param dependencies The array of taskIds of tasks the added task depends on
     */
    public void add(String taskId, int urgencyLevel, String[] dependencies) {
        Task t = taskTable.get(taskId);
        if (t != null) {
            if (t.added) {
                return;
            } else {
                t.urgencyLevel = urgencyLevel;
                t.insertionOrder = insertionCounter++;
                t.added = true;
            }
        } else {
            t = new Task(taskId, urgencyLevel, insertionCounter++, true);
            taskTable.put(t);
        }
    
        for (String depId : dependencies) {
            Task dep = taskTable.get(depId);
            if (dep == null) {
                dep = new Task(depId, 0, 0, false);
                taskTable.put(dep);
            }
            t.dependsOn.add(dep);
            dep.isNeededFor.add(t);
            if (dep.added && !dep.resolved) {
                t.unresolvedCount++;
            }
        }
        if (t.unresolvedCount == 0) {
            readyHeap.insert(t);
        }
    }

    /**
     * A method to change the urgency of a task
     *
     * @param taskId       The string taskId of the task we want to change the
     *                     urgency of
     * @param urgencyLevel The new integer urgencyLevel of the task
     */
    public void update(String taskId, int newUrgencyLevel) {
        Task t = taskTable.get(taskId);
        if (t == null || !t.added || t.resolved) {
            return;
        }
        t.urgencyLevel = newUrgencyLevel;
        if (t.unresolvedCount == 0) {
            readyHeap.update(t);
        }
    }

    /**
    * A method to resolve the greatest urgency task which has had all of its
    * dependencies satisfied
    *
    * @return The taskId of the resolved task
    * @return null if there are no unresolved tasks left
    */
    public String resolve() {
        if (readyHeap.isEmpty()) {
            return null;
        }
        Task t = readyHeap.extractTop();
        t.resolved = true;
        for (Task dependent : t.isNeededFor) {
            if (dependent.added && !dependent.resolved) {
                if (dependent.unresolvedCount > 0) {
                    dependent.unresolvedCount--;
                    if (dependent.unresolvedCount == 0) {
                        readyHeap.insert(dependent);
                    }
                }
            }
        }
        return t.taskId;
    }

    /**
     * Class for each Task
     */
    private static class Task {
        String taskId;
        int urgencyLevel;
        int insertionOrder;
        List<Task> dependsOn;
        List<Task> isNeededFor;
        int unresolvedCount;
        boolean added;
        boolean resolved;

        public Task(String taskId, int urgencyLevel, int insertionOrder, boolean added) {
            this.taskId = taskId;
            this.urgencyLevel = urgencyLevel;
            this.insertionOrder = insertionOrder;
            this.added = added;
            this.resolved = false;
            this.dependsOn = new ArrayList<>();
            this.isNeededFor = new ArrayList<>();
            this.unresolvedCount = 0;
        }
    }

    /**
     * Class for Tasks that are ready to be resolved.
     */
    private static class ReadyHeap {
        private ArrayList<Task> heap;
        private ReadyIndexMap indexMap;

        public ReadyHeap() {
            heap = new ArrayList<>();
            indexMap = new ReadyIndexMap();
        }

        public void insert(Task t) {
            heap.add(t);
            int index = heap.size() - 1;
            indexMap.put(t.taskId, index);
            percolateUp(index);
        }

        public void update(Task t) {
            int index = indexMap.get(t.taskId);
            if (index == -1)
                return;
            if (!percolateUp(index)) {
                percolateDown(index);
            }
        }

        public Task extractTop() {
            if (heap.isEmpty())
                return null;
            Task top = heap.get(0);
            Task last = heap.remove(heap.size() - 1);
            indexMap.remove(top.taskId);
            if (!heap.isEmpty()) {
                heap.set(0, last);
                indexMap.put(last.taskId, 0);
                percolateDown(0);
            }
            return top;
        }

        public boolean isEmpty() {
            return heap.isEmpty();
        }

        private boolean percolateUp(int i) {
            boolean swapped = false;
            while (i > 0) {
                int parent = (i - 1) / 2;
                if (compare(heap.get(i), heap.get(parent)) < 0) {
                    swap(i, parent);
                    i = parent;
                    swapped = true;
                } else {
                    break;
                }
            }
            return swapped;
        }

        private void percolateDown(int i) {
            int n = heap.size();
            while (true) {
                int left = 2 * i + 1, right = 2 * i + 2;
                int smallest = i;
                if (left < n && compare(heap.get(left), heap.get(smallest)) < 0) {
                    smallest = left;
                }
                if (right < n && compare(heap.get(right), heap.get(smallest)) < 0) {
                    smallest = right;
                }
                if (smallest != i) {
                    swap(i, smallest);
                    i = smallest;
                } else {
                    break;
                }
            }
        }

        private void swap(int i, int j) {
            Task tmp = heap.get(i);
            heap.set(i, heap.get(j));
            heap.set(j, tmp);
            indexMap.put(heap.get(i).taskId, i);
            indexMap.put(heap.get(j).taskId, j);
        }

        private int compare(Task t1, Task t2) {
            if (t1.urgencyLevel != t2.urgencyLevel) {
                return Integer.compare(t2.urgencyLevel, t1.urgencyLevel);
            }
            return Integer.compare(t1.insertionOrder, t2.insertionOrder);
        }
    }

    /**
     * Class to map task ID to index in the heap
     */
    private static class ReadyIndexMap {
        private static class Entry {
            String key;
            int value;
            Entry(String key, int value) {
                this.key = key;
                this.value = value;
            }
        }
        private Entry[] table;
        private int capacity;
        private int size;
        private static final double LOAD_FACTOR = 0.75;

        public ReadyIndexMap() {
            capacity = 16;
            table = new Entry[capacity];
            size = 0;
        }

        private int hash(String key) {
            int h = key.hashCode();
            return (h & 0x7fffffff) % capacity;
        }

        public void put(String key, int value) {
            if (size >= capacity * LOAD_FACTOR) {
                resize();
            }
            int index = hash(key);
            while (table[index] != null) {
                if (table[index].key.equals(key)) {
                    table[index].value = value;
                    return;
                }
                index = (index + 1) % capacity;
            }
            table[index] = new Entry(key, value);
            size++;
        }

        public int get(String key) {
            int index = hash(key);
            while (table[index] != null) {
                if (table[index].key.equals(key)) {
                    return table[index].value;
                }
                index = (index + 1) % capacity;
            }
            return -1;
        }

        public void remove(String key) {
            int index = hash(key);
            while (table[index] != null) {
                if (table[index].key.equals(key)) {
                    table[index] = null;
                    size--;
                    index = (index + 1) % capacity;
                    while (table[index] != null) {
                        Entry entry = table[index];
                        table[index] = null;
                        size--;
                        put(entry.key, entry.value);
                        index = (index + 1) % capacity;
                    }
                    return;
                }
                index = (index + 1) % capacity;
            }
        }

        private void resize() {
            Entry[] oldTable = table;
            capacity *= 2;
            table = new Entry[capacity];
            size = 0;
            for (Entry e : oldTable) {
                if (e != null)
                    put(e.key, e.value);
            }
        }
    }

    /**
     * Class to store tasks in a hash table using chaining.
     */
    private static class TaskHashTable {
        private Bucket[] table;
        private int capacity;

        private static class Bucket {
            Node head;
        }

        private static class Node {
            Task task;
            Node next;
            Node(Task task) {
                this.task = task;
            }
        }

        public TaskHashTable(int capacity) {
            this.capacity = capacity;
            table = new Bucket[capacity];
            for (int i = 0; i < capacity; i++) {
                table[i] = new Bucket();
            }
        }

        private int hash(String key) {
            int h = key.hashCode();
            if (h < 0)
                h = -h;
            return h % capacity;
        }

        public Task get(String key) {
            int idx = hash(key);
            Node cur = table[idx].head;
            while (cur != null) {
                if (cur.task.taskId.equals(key))
                    return cur.task;
                cur = cur.next;
            }
            return null;
        }

        public void put(Task task) {
            int idx = hash(task.taskId);
            Node newNode = new Node(task);
            newNode.next = table[idx].head;
            table[idx].head = newNode;
        }

        public boolean contains(String key) {
            return get(key) != null;
        }
    }
}
