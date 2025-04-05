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
    private TaskHeap taskHeap; // heap for tasks that have no dependencies
    private int insertionCounter; // track order of insertion

    /**
     * Constructor to initialize the TaskPrioritizer
     */
    public TaskPrioritizer() {
        taskTable = new TaskHashTable(1000003);
        taskHeap = new TaskHeap();
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
        // does nothing if the task was already added, otherwise updates information associated with it
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

        // processes each of the dependencies of the task
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
            taskHeap.insert(t);
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
            taskHeap.update(t);
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
        if (taskHeap.isEmpty()) {
            return null;
        }
        // remove the task with the highest priority from the top of the heap
        Task t = taskHeap.extractTop();
        t.resolved = true;
        // go through all of the tasks that depend on this task and update accordingly
        for (Task dependent : t.isNeededFor) {
            if (dependent.added && !dependent.resolved) {
                if (dependent.unresolvedCount > 0) {
                    dependent.unresolvedCount--;
                    if (dependent.unresolvedCount == 0) {
                        taskHeap.insert(dependent);
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
        int taskHeapIndex;

        /**
         * Constructor to initialize a Task
         */
        public Task(String taskId, int urgencyLevel, int insertionOrder, boolean added) {
            this.taskId = taskId;
            this.urgencyLevel = urgencyLevel;
            this.insertionOrder = insertionOrder;
            this.added = added;
            this.resolved = false;
            this.dependsOn = new ArrayList<>();
            this.isNeededFor = new ArrayList<>();
            this.unresolvedCount = 0;
            this.taskHeapIndex = -1;
        }
    }

    /**
     * Class for Tasks that are ready to be resolved.
     */
    private static class TaskHeap {
        private ArrayList<Task> heap;

        /**
         * Constructor to initilialize the heap
         */
        public TaskHeap() {
            heap = new ArrayList<>();
        }

        /**
         * Inserts a task into the heap.
         * 
         * @param t - the task to insert
         */
        public void insert(Task t) {
            heap.add(t);
            int index = heap.size() - 1;
            t.taskHeapIndex = index;
            percolateUp(index);
        }

        /**
         * Updates the position of the task in the heap.
         * 
         * @param t - the task to update
         */
        public void update(Task t) {
            int index = t.taskHeapIndex;
            if (index == -1) {
                return;
            }
            if (!percolateUp(index)) {
                percolateDown(index);
            }
        }

        /**
         * Removes the task with the greatest urgency.
         * 
         * @return the task with the greatest urgency.
         */
        public Task extractTop() {
            if (heap.isEmpty()) {
                return null;
            }
            Task top = heap.get(0);
            Task last = heap.remove(heap.size() - 1);
            top.taskHeapIndex = -1;
            if (!heap.isEmpty()) {
                heap.set(0, last);
                last.taskHeapIndex = 0;
                percolateDown(0);
            }
            return top;
        }

        /**
         * Checks if the heap is empty.
         * 
         * @return true if the heap is empty, false otherwise.
         */
        public boolean isEmpty() {
            return heap.isEmpty();
        }

        /**
         * Maintains the heap ordering of a task at a particular index.
         * 
         * @param i - the index of the task
         * @return true if the task was moved, false otherwise.
         */
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

        /**
         * Maintains the heap ordering of a task at a particular index.
         * 
         * @param i - the index of the task
         * @return true if the task was moved, false otherwise.
         */
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

        /**
         * Swaps 2 tasks at the indices i and j
         * 
         * @param i - the task at index i
         * @param j - the task at index j
         */
        private void swap(int i, int j) {
            Task tmp = heap.get(i);
            heap.set(i, heap.get(j));
            heap.set(j, tmp);
            heap.get(i).taskHeapIndex = i;
            heap.get(j).taskHeapIndex = j;
        }

        /**
         * Compares 2 tasks based first on urgency level and then on insertion order.
         * 
         * @param t1 - the first task to compare
         * @param t2 - the second task to compare
         * @return negative int, 0, or a positive int depending on the ordering of the tasks
         */
        private int compare(Task t1, Task t2) {
            if (t1.urgencyLevel != t2.urgencyLevel) {
                return Integer.compare(t2.urgencyLevel, t1.urgencyLevel);
            }
            return Integer.compare(t1.insertionOrder, t2.insertionOrder);
        }
    }

    /**
     * Class to store tasks in a hash table using chaining.
     */
    private static class TaskHashTable {
        private Bucket[] table;
        private int capacity;

        /**
         * Where each LinkedList of Noes will be stored.
         */
        private static class Bucket {
            Node head;
        }

        /**
         * Stores information for chaining.
         */
        private static class Node {
            Task task;
            Node next;
            Node(Task task) {
                this.task = task;
            }
        }

        /**
         * Constructor to initialize the hash table
         */
        public TaskHashTable(int capacity) {
            this.capacity = capacity;
            table = new Bucket[capacity];
            for (int i = 0; i < capacity; i++) {
                table[i] = new Bucket();
            }
        }

        /**
         * Hashcode function
         * 
         * @param key - the key to hash
         * @return an int representing the hash function
         */
        private int hash(String key) {
            int h = key.hashCode();
            if (h < 0) {
                h = -h;
            }
            return h % capacity;
        }

        /**
         * Return the key in a certain Bucket.
         * 
         * @param key - the key to return
         * @return the Task associated with that taskId
         */
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

        /**
         * Puts a Task in the Bucket.
         * 
         * @param task - the task to insert
         */
        public void put(Task task) {
            int idx = hash(task.taskId);
            Node newNode = new Node(task);
            newNode.next = table[idx].head;
            table[idx].head = newNode;
        }

        /**
         * Checks if the key is in the table.
         * 
         * @param key - the key to check for
         * @return true if the key exists, false otherwise.
         */
        public boolean contains(String key) {
            return get(key) != null;
        }
    }
}
