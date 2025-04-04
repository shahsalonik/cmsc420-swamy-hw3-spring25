import java.lang.String;
import java.util.List;
import java.util.LinkedList;

/**
 * TaskPrioritizer class that returns the most urgent
 * available task
 *
 * @author Saloni Shah
 */
public class TaskPrioritizer {

    private List<Task> taskList; // holds tasks sorted by urgency
    private List<Dependency> dependencyList; // list of all the dependencies
    private int insertionCounter; // need to track insertion order

    /**
     * Constructor to initialize the TaskPrioritizer
     */
    public TaskPrioritizer() {
        this.taskList = new LinkedList<>();
        this.dependencyList = new LinkedList<>();
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
        for (Task task : taskList) {
            if (task.taskId.equals(taskId)) {
                return;
            }
        }

        // create the new task and add it to the list
        Task newTask = new Task(taskId, urgencyLevel, dependencies, insertionCounter++);
        taskList.add(newTask);

        // add the task dependencies
        for (String dep : dependencies) {
            dependencyList.add(new Dependency(dep, taskId));
        }

        sortTasks();
    }

    /**
     * A method to change the urgency of a task
     *
     * @param taskId       The string taskId of the task we want to change the
     *                     urgency of
     * @param urgencyLevel The new integer urgencyLevel of the task
     */
    public void update(String taskId, int newUrgencyLevel) {
        for (Task task : taskList) {
            if (task.taskId.equals(taskId)) {
                task.urgencyLevel = newUrgencyLevel;
                break;
            }
        }

        sortTasks();
    }

    /**
     * A method to resolve the greatest urgency task which has had all of its
     * dependencies satisfied
     *
     * @return The taskId of the resolved task
     * @return null if there are no unresolved tasks left
     */
    public String resolve() {
        for (Task task : taskList) {
            if (task.isResolved) {
                continue;
            }

            // check to see if the task can even be resolved
            boolean canResolve = true;
            for (String dep : task.dependencies) {
                if (!isTaskResolved(dep)) {
                    canResolve = false;
                    break;
                }
            }

            // perform resolution
            if (canResolve) {
                task.isResolved = true;
                return task.taskId;
            }
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
        for (Task task : taskList) {
            if (task.taskId.equals(taskId)) {
                return task.isResolved;
            }
        }
        return false;
    }

    /**
     * Sorts tasks by urgency or insertion order
     */
    private void sortTasks() {
        taskList.sort((task1, task2) -> {
            if (task1.urgencyLevel != task2.urgencyLevel) {
                return Integer.compare(task2.urgencyLevel, task1.urgencyLevel); 
            }
            return Integer.compare(task1.insertionOrder, task2.insertionOrder);
        });
    }

    /**
     * Class to store information about each task
     */
    private static class Task {
        String taskId;
        int urgencyLevel;
        String[] dependencies;
        boolean isResolved;
        int insertionOrder;

        Task(String taskId, int urgencyLevel, String[] dependencies, int insertionOrder) {
            this.taskId = taskId;
            this.urgencyLevel = urgencyLevel;
            this.dependencies = dependencies;
            this.isResolved = false;
            this.insertionOrder = insertionOrder;
        }
    }

    /**
     * Class to keep track of the dependencies
     */
    private static class Dependency {
        String dependentTaskId;
        String dependencyTaskId;

        Dependency(String dependentTaskId, String dependencyTaskId) {
            this.dependentTaskId = dependentTaskId;
            this.dependencyTaskId = dependencyTaskId;
        }
    }
}