import java.lang.String;
import java.util.ArrayList;
import java.util.List;

/**
 * TaskPrioritizer class that returns the most urgent
 * available task
 *
 * @author Saloni Shah
 */
public class TaskPrioritizer {

    private TaskHeap taskHeap; // custom heap for tasks
    private List<Dependency> dependencyList; // list of all dependencies
    private int insertionCounter; // track insertion order

    /**
     * Constructor to initialize the TaskPrioritizer
     */
    public TaskPrioritizer() {
        this.taskHeap = new TaskHeap();
        this.dependencyList = new ArrayList<>();
        this.insertionCounter = 0;
    }

     /**
     * A method to add a new task
     *
     * @param taskId       The string taskId of the task we want to add
     * @param urgencyLevel The integer urgencyLevel of the task we want to add
     * @param dependencies The array of taskIds of tasks the added task depends on
     */
    public void add(String taskId, int urgencyLevel, String[] dependencies) {
        // check if the task already exists
        if (taskHeap.contains(taskId)) {
            return;
        }
        // create the new task with its insertion order
        Task newTask = new Task(taskId, urgencyLevel, dependencies, insertionCounter++);
        taskHeap.insert(newTask);

        // add the task dependencies
        for (String dep : dependencies) {
            dependencyList.add(new Dependency(dep, taskId));
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
        Task task = taskHeap.get(taskId);
        if (task != null) {
            task.urgencyLevel = newUrgencyLevel;
            taskHeap.update(task);
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
        // find the greatest priority task
        Task bestTask = null;
        for (Task task : taskHeap.getAllTasks()) {
            if (task.isResolved) {
                continue;
            }
            boolean canResolve = true;
            for (String dep : task.dependencies) {
                if (!isTaskResolved(dep)) {
                    canResolve = false;
                    break;
                }
            }
            if (canResolve) {
                if (bestTask == null || taskHeap.compare(task, bestTask) < 0) {
                    bestTask = task;
                }
            }
        }

        if (bestTask != null) {
            bestTask.isResolved = true;
            return bestTask.taskId;
        }

        return null;
    }

    /**
     * Checks if a task is resolved
     * 
     * @param taskId - the task to check
     * @return true if the task is resolved, false otherwise
     */
    private boolean isTaskResolved(String taskId) {
        Task task = taskHeap.get(taskId);
        return task != null && task.isResolved;
    }

    /**
     * Class to store all info related to a Task
     */
    private static class Task {
        String taskId;
        int urgencyLevel;
        String[] dependencies;
        boolean isResolved;
        int insertionOrder;

        /**
         * A constructor to initialize a Task
         */
        public Task(String taskId, int urgencyLevel, String[] dependencies, int insertionOrder) {
            this.taskId = taskId;
            this.urgencyLevel = urgencyLevel;
            this.dependencies = dependencies;
            this.isResolved = false;
            this.insertionOrder = insertionOrder;
        }
    }

    /**
     * Class to store info related to a dependency
     */
    private static class Dependency {
        String dependencyTaskId;
        String dependentTaskId;

        /**
         * A constructor to initialize a Dependency
         */
        public Dependency(String dependencyTaskId, String dependentTaskId) {
            this.dependencyTaskId = dependencyTaskId;
            this.dependentTaskId = dependentTaskId;
        }
    }

    /**
     * Heap to store Tasks
     */
    private static class TaskHeap {
        private ArrayList<Task> heap;
        private TaskMap indexMap;

        /**
         * Constructor to initialize the TaskHeap
         */
        public TaskHeap() {
            this.heap = new ArrayList<>();
            this.indexMap = new TaskMap();
        }

        /**
         * Insert a new task and call the heap function
         * 
         * @param task - the task to insert
         */
        public void insert(Task task) {
            heap.add(task);
            int index = heap.size() - 1;
            indexMap.put(task.taskId, index);
            percolateUp(index);
        }

        /**
         * Update a task's position in the heap after its urgency changes
         * 
         * @param task - the task to update
         */
        public void update(Task task) {
            int index = indexMap.get(task.taskId);
            if (!percolateUp(index)) {
                percolateDown(index);
            }
        }

        /**
         * Returns true if a task with the given taskId exists
         * 
         * @param taskId - the task id to check
         * @return true if the task exists, false otherwise
         */
        public boolean contains(String taskId) {
            return indexMap.containsKey(taskId);
        }

        /**
         * Get a task by its id
         * 
         * @param taskId - the id of the task
         * @return the task
         */
        public Task get(String taskId) {
            int index = indexMap.get(taskId);
            if (index >= 0 && index < heap.size()) {
                return heap.get(index);
            }
            return null;
        }

        /**
         * Returns the whole heap
         * 
         * @return the heap
         */
        public List<Task> getAllTasks() {
            return heap;
        }

        /**
         * Compare two tasks: higher urgency first; if equal, earlier insertion
         * 
         * @param t1 - the first task to compare
         * @param t2 - the second task to compare
         * @return a negative, 0, or positive int depending on the order of the two tasks
         */
        public int compare(Task t1, Task t2) {
            if (t1.urgencyLevel != t2.urgencyLevel) {
                return Integer.compare(t2.urgencyLevel, t1.urgencyLevel);
            }
            return Integer.compare(t1.insertionOrder, t2.insertionOrder);
        }

        /**
         * Percolate up the task at index i. Returns true if a swap occurred
         * 
         * @param i - the index of the task
         * @return true if there was a swap, false otherwise
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
         * Percolate down the task at index i.
         * 
         * @param i - the index of the task
         */
        private void percolateDown(int i) {
            int n = heap.size();
            while (true) {
                int left = 2 * i + 1;
                int right = 2 * i + 2;
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
         * Swaps 2 elements
         * 
         * @param i - the index of the first element to swap
         * @param j - the index of the second element to swap
         */
        private void swap(int i, int j) {
            Task temp = heap.get(i);
            heap.set(i, heap.get(j));
            heap.set(j, temp);
            indexMap.put(heap.get(i).taskId, i);
            indexMap.put(heap.get(j).taskId, j);
        }
    }

    /**
     * Hashmap that uses linear probing
     */
    private static class TaskMap {
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

        public TaskMap() {
            capacity = 16;
            table = new Entry[capacity];
            size = 0;
        }

        /**
         * Hash function
         * 
         * @param key - the key to hash
         * @return an integer representing the hash code of the key
         */
        private int hash(String key) {
            int h = key.hashCode();
            return (h & 0x7fffffff) % capacity;
        }

        /**
         * Puts a key-value pair into the map
         * 
         * @param key - the key to put into the map
         * @param value - the value to put into the map
         */
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

        /**
         * Returns the value associated with the key, or -1 if not found
         * 
         * @param key - the key to get the value of
         * @return the value associated with the key
         */
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

        /**
         * Checks if the key exists in the map
         * 
         * @param key - the key to check
         * @return true if the key exists, false otherwise
         */
        public boolean containsKey(String key) {
            return get(key) != -1;
        }

        /**
         * Resizes the table after the load factor is exceeded
         */
        private void resize() {
            Entry[] oldTable = table;
            capacity *= 2;
            table = new Entry[capacity];
            size = 0;
            for (Entry entry : oldTable) {
                if (entry != null) {
                    put(entry.key, entry.value);
                }
            }
        }
    }
}