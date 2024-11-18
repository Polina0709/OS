import java.util.*;
import java.util.concurrent.*;

class ComputationManager {
    static class ComputationComponent {
        private final Callable<Double> computation;
        private long timeLimit = Long.MAX_VALUE;

        public ComputationComponent(Callable<Double> computation) {
            this.computation = computation;
        }

        public void setTimeLimit(long timeLimit) {
            this.timeLimit = timeLimit;
        }

        public long getTimeLimit() {
            return timeLimit;
        }

        public Double compute() throws Exception {
            return computation.call();
        }
    }

    Map<Double, List<ComputationComponent>> groups = new HashMap<>();

    public void addGroup(double x) {
        if (!groups.containsKey(x)) {
            groups.put(x, new ArrayList<>());
            System.out.println("Group with x = " + x + " created.");
        } else {
            System.out.println("Group with x = " + x + " already exists.");
        }
    }

    public void addComponent(double groupX, Callable<Double> computation, long timeLimit) {
        if (groups.containsKey(groupX)) {
            ComputationComponent component = new ComputationComponent(computation);
            component.setTimeLimit(timeLimit);
            groups.get(groupX).add(component);
            System.out.println("Component added to group with x = " + groupX);
        } else {
            System.out.println("Group with x = " + groupX + " does not exist.");
        }
    }

    public ComputationComponent getComponent(double groupX, int componentIndex) {
        List<ComputationComponent> components = groups.get(groupX);
        if (components != null && componentIndex > 0 && componentIndex <= components.size()) {
            return components.get(componentIndex - 1);
        }
        return null;
    }

    public void runGroup(ComputationManager manager, double groupX) {
        if (!manager.groups.containsKey(groupX)) {
            System.out.println("Group with x = " + groupX + " does not exist.");
            return;
        }

        System.out.println("Computing ...");
        List<ComputationComponent> components = manager.groups.get(groupX);
        ExecutorService executor = Executors.newFixedThreadPool(components.size());
        List<Future<Double>> results = new ArrayList<>();
        final boolean[] interrupted = {false};

        Scanner scanner = new Scanner(System.in);

        try {
            for (ComputationComponent component : components) {
                Future<Double> future = executor.submit(() -> {
                    try {
                        return component.compute();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                results.add(future);
            }

            Thread interruptionThread = new Thread(() -> {
                try {
                    scanner.nextLine();
                    interrupted[0] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            interruptionThread.start();

            for (int i = 0; i < results.size(); i++) {
                if (interrupted[0]) {
                    System.out.println("Execution interrupted by user.");
                    break;
                }
                try {
                    Thread.sleep(1000);
                    Double result = results.get(i).get(components.get(i).getTimeLimit(), TimeUnit.MILLISECONDS);
                    System.out.println("Component " + (i + 1) + " finished!");
                } catch (TimeoutException e) {
                    System.out.println("Component " + (i + 1) + " timed out.");
                } catch (Exception e) {
                    System.out.println("Component " + (i + 1) + " failed: " + e.getMessage());
                }
            }

            if (interrupted[0]) {
                System.out.println("Computation interrupted. You can add more components to the group or resume execution.");
                printAvailableCommands();

                while (true) {
                    System.out.println("Enter command:");
                    String command = scanner.nextLine();
                    if (command.equalsIgnoreCase("add component")) {

                        System.out.println("Available components to add:");
                        System.out.println("1. Factorial");
                        System.out.println("2. Square root");
                        System.out.println("3. Square");
                        System.out.print("Choose component type (1, 2, or 3): ");
                        int componentChoice = scanner.nextInt();
                        scanner.nextLine();

                        if (manager.groups.containsKey(groupX)) {
                            switch (componentChoice) {
                                case 1:
                                    manager.addComponent(groupX, () -> ComputationManager.factorial(groupX), Long.MAX_VALUE);
                                    break;
                                case 2:
                                    manager.addComponent(groupX, () -> Math.sqrt(groupX), Long.MAX_VALUE);
                                    break;
                                case 3:
                                    manager.addComponent(groupX, () -> Math.pow(groupX, 2), Long.MAX_VALUE);
                                    break;
                                default:
                                    System.out.println("Invalid component type.");
                            }
                        } else {
                            System.out.println("Group with x = " + groupX + " does not exist.");
                        }
                    } else if (command.equalsIgnoreCase("resume")) {

                        System.out.println("Resuming computation...");
                        resumeComputation(manager, groupX);
                        break;
                    } else if (command.equalsIgnoreCase("exit")) {
                        System.out.println("Exiting...");
                        return;
                    } else {
                        System.out.println("Invalid command.");
                    }
                }
            } else {

                Thread.sleep(1000);
                System.out.println("Computation finished!");
                printFinalResults(results, components);
            }

        } catch (InterruptedException e) {
            System.out.println("Execution interrupted.");
        } finally {
            executor.shutdown();
        }
    }

    private void printFinalResults(List<Future<Double>> results, List<ComputationComponent> components) {
        System.out.println("Results:");
        for (int i = 0; i < results.size(); i++) {
            try {
                Double result = results.get(i).get(components.get(i).getTimeLimit(), TimeUnit.MILLISECONDS);
                System.out.println("Component " + (i + 1) + ": " + result);
            } catch (Exception e) {
                System.out.println("Component " + (i + 1) + ": No result due to error or timeout.");
            }
        }
    }

    private void printAvailableCommands() {
        System.out.println("\nAvailable commands after interruption:");
        System.out.println("1. add component - Add a new component to the group.");
        System.out.println("2. resume - Resume the computation.");
        System.out.println("3. exit - Exit the program.");
    }

    private void resumeComputation(ComputationManager manager, double groupX) {
        List<ComputationComponent> components = manager.groups.get(groupX);
        ExecutorService executor = Executors.newFixedThreadPool(components.size());
        List<Future<Double>> results = new ArrayList<>();

        try {
            for (ComputationComponent component : components) {
                Future<Double> future = executor.submit(() -> {
                    try {
                        return component.compute();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                results.add(future);
            }

            for (int i = 0; i < results.size(); i++) {
                try {

                    Double result = results.get(i).get(components.get(i).getTimeLimit(), TimeUnit.MILLISECONDS);
                    System.out.println("Component " + (i + 1) + " finished with result: " + result);
                } catch (TimeoutException e) {
                    System.out.println("Component " + (i + 1) + " timed out.");
                } catch (Exception e) {
                    System.out.println("Component " + (i + 1) + " failed: " + e.getMessage());
                }
            }

            System.out.println("Computation finished!");
            System.out.println("Results:");

            for (int i = 0; i < results.size(); i++) {
                try {
                    Double result = results.get(i).get(components.get(i).getTimeLimit(), TimeUnit.MILLISECONDS);
                    System.out.println("Component " + (i + 1) + ": " + result);
                } catch (Exception e) {
                    System.out.println("Component " + (i + 1) + ": No result due to error or timeout.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    public static double factorial(double x) {
        if (x < 0) {
            throw new IllegalArgumentException("Factorial is not defined for negative numbers.");
        }
        if (x == 0 || x == 1) {
            return 1;
        }
        double result = 1;
        for (int i = 2; i <= x; i++) {
            result *= i;
        }
        return result;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ComputationManager manager = new ComputationManager();

        while (true) {
            System.out.println("\nCommands:");
            System.out.println("1. add group <x>");
            System.out.println("2. add component <x>");
            System.out.println("3. new <component symbol> limit <time>");
            System.out.println("4. group <x> limit <time>");
            System.out.println("5. run group <x>");
            System.out.println("6. exit");
            System.out.print("\nEnter command: ");

            String command = scanner.nextLine();
            String[] parts = command.split(" ");

            switch (parts[0]) {
                case "add":
                    if (parts.length == 3 && parts[1].equals("group")) {
                        try {
                            double x = Double.parseDouble(parts[2]);
                            manager.addGroup(x);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid group parameter. Please enter a number.");
                        }
                    } else if (parts.length == 3 && parts[1].equals("component")) {
                        try {
                            double x = Double.parseDouble(parts[2]);
                            if (manager.groups.containsKey(x)) {
                                System.out.println("Available components:");
                                System.out.println("1. Factorial");
                                System.out.println("2. Square root");
                                System.out.println("3. Square");
                                System.out.print("Choose component type: ");
                                int choice = scanner.nextInt();
                                scanner.nextLine();

                                switch (choice) {
                                    case 1:
                                        manager.addComponent(x, () -> ComputationManager.factorial(x), Long.MAX_VALUE);
                                        break;
                                    case 2:
                                        manager.addComponent(x, () -> Math.sqrt(x), Long.MAX_VALUE);
                                        break;
                                    case 3:
                                        manager.addComponent(x, () -> Math.pow(x, 2), Long.MAX_VALUE);
                                        break;
                                    default:
                                        System.out.println("Invalid component type.");
                                }
                            } else {
                                System.out.println("Group with x = " + x + " does not exist.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid component parameter. Please enter a number.");
                        }
                    } else {
                        System.out.println("Invalid command. Use: add group <x> or add component <x>");
                    }
                    break;

                case "new":
                    if (parts.length == 4 && parts[2].equals("limit")) {
                        try {
                            int componentSymbol = Integer.parseInt(parts[1]);
                            long timeLimit = Long.parseLong(parts[3]);

                            System.out.print("Enter group x to assign the limit to component: ");
                            double groupX = scanner.nextDouble();
                            scanner.nextLine();

                            if (manager.groups.containsKey(groupX)) {
                                ComputationManager.ComputationComponent component = manager.getComponent(groupX, componentSymbol);
                                if (component != null) {
                                    component.setTimeLimit(timeLimit);
                                    System.out.println("Time limit of " + timeLimit + "ms assigned to component " + componentSymbol + " in group with x = " + groupX);
                                } else {
                                    System.out.println("Component " + componentSymbol + " does not exist in group with x = " + groupX);
                                }
                            } else {
                                System.out.println("Group with x = " + groupX + " does not exist.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Use: new <component symbol> limit <time>");
                        }
                    } else {
                        System.out.println("Invalid command format. Use: new <component symbol> limit <time>");
                    }
                    break;

                case "group":
                    if (parts.length == 4 && parts[2].equals("limit")) {
                        try {
                            double x = Double.parseDouble(parts[1]);
                            long timeLimit = Long.parseLong(parts[3]);

                            if (manager.groups.containsKey(x)) {
                                System.out.println("Setting time limit for all components in group " + x);
                                manager.groups.get(x).forEach(component -> component.setTimeLimit(timeLimit));
                            } else {
                                System.out.println("Group with x = " + x + " does not exist.");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter valid numbers for group and time limit.");
                        }
                    } else {
                        System.out.println("Invalid command format. Use: group <x> limit <time>");
                    }
                    break;

                case "run":
                    if (parts.length == 3 && parts[1].equals("group")) {
                        try {
                            double x = Double.parseDouble(parts[2]);
                            manager.runGroup(manager, x); 
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid group parameter. Please enter a number.");
                        }
                    } else {
                        System.out.println("Invalid command format. Use: run group <x>");
                    }
                    break;

                case "exit":
                    System.out.println("Exiting...");
                    return;

                default:
                    System.out.println("Unknown command. Please try again.");
            }
        }
    }
}







