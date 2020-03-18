import java.util.*;
import java.io.*;

public class lab2 {
    static boolean verbose = false; // was verbose invoked?

    public static void main(String[] args) throws IOException {
        // main
        // Array of processes
        ArrayList<Process> processes = new ArrayList<>();
        // Input file
        String input = "";

        // Locate input & set verbose bool
        if (args[0].contains("verbose")) {
            verbose = true;
            input = args[1];
        } else {
            input = args[0];
        }

        processes = read_input(input); 

        // FCFS
        Scheduler.print_org_input(processes);
        Scheduler.print_sorted_input(processes);
        Scheduler.FCFS(processes, verbose);
        Process.print_process(processes);
        Scheduler.print_summary(processes);

        // RR
        Scheduler.print_org_input(processes);
        Scheduler.print_sorted_input(processes);
        Scheduler.RR(processes, verbose);
        Process.print_process(processes);
        Scheduler.print_summary(processes);

        // LCFS
        Scheduler.print_org_input(processes);
        Scheduler.print_sorted_input(processes);
        Scheduler.LCFS(processes, verbose);
        Process.print_process(processes);
        Scheduler.print_summary(processes);

        // HPRN
        Scheduler.print_org_input(processes);
        Scheduler.print_sorted_input(processes);
        Scheduler.HPRN(processes, verbose);
        Process.print_process(processes);
        Scheduler.print_summary(processes);
    }

    static ArrayList<Process> read_input(String input) {
        // ArrayList to return
        ArrayList<Process> processes = new ArrayList<>();
        // Create File obj
        File file = new File(input);
        // Initialize Scanner
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // # of processes
        int n = scanner.nextInt();
        String line = scanner.nextLine().replaceAll("[^0-9]", " ");
        scanner = new Scanner(line);
        // Add Process obj to ArrayList
        for (int i = 0; i < n; i++) {
            // (1, 3, 2, 1) or (1 2 3 4) (2222)
            int A = scanner.nextInt();
            int B = scanner.nextInt();
            int C = scanner.nextInt();
            int M = scanner.nextInt();

            processes.add(new Process(A, B, C, M, i));
        }
        scanner.close();
        return processes;
    }

}

class Process {
    int A; // Arrival time
    int B; // Used to obtain CPU burst time
    int C; // Total CPU time needed
    int M; // Used to obtain IO burst time

    int remaining_time;
    int finishing_time;
    int turnaround_time; // finishing time - A
    int io_time; // time in blocked state
    int waiting_time; // time in ready state

    int cpu_burst;
    int io_burst;

    String state;
    int quantum = 2;
    int index;
    int ready_cycle; // Which cycle did the Process become ready?

    float r; // HPRN penalty ratio
    float T; // HPRN Numerator
    float cpu_util; // HPRN Denominator
    int arrival_cycle; // Used for HPRN

    // Constructor
    Process(int A, int B, int C, int M, int index) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.M = M;
        // Initialize remaining time to total time
        this.remaining_time = C;
        // Initialize state
        this.state = "unstarted";
        this.index = index;
    }

    static void print_process(ArrayList<Process> processes) {
        int i = 0; // Used for process labeling
        for (Process p : processes) {
            System.out.println("Process " + i + ":");
            System.out.println("\t(A,B,C,M) = " + "(" + p.A + "," + p.B + "," + p.C + "," + p.M + ")");
            System.out.println("\tFinishing time: " + p.finishing_time);
            System.out.println("\tTurnaround time: " + p.turnaround_time);
            System.out.println("\tI/O time: " + p.io_time);
            System.out.println("\tWaiting time: " + p.waiting_time);

            i++;
        }
    }

    static void reset_process(ArrayList<Process> processes) {
        for (Process p : processes) {
            p.remaining_time = p.C;
            p.state = "unstarted";
            p.cpu_burst = 0;
            p.io_burst = 0;
            p.finishing_time = 0;
            p.io_time = 0;
            p.turnaround_time = 0;
            p.waiting_time = 0;
        }
    }
}

class Scheduler {
    // Summary data
    static int finishing_time;
    static float CPU_utilization;
    static float IO_utilization;
    static float throughput;
    static float avg_turnaround_time;
    static float avg_waiting_time;

    static int random_count = 0; // used for randomOS()

    static final double EPSILON = .001;

    static int randomOS(int U) throws IOException {
        // Open random numbers file
        File file = new File("random-numbers.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Read random_count'th line
        for (int i = 0; i < random_count; i++) {
            reader.readLine();
        }
        // Increment for next use
        random_count++;

        return (1 + (Integer.parseInt(reader.readLine()) % U));
    }

    // Prints original input
    static void print_org_input(ArrayList<Process> processes) {
        System.out.print("The original input was: ");
        System.out.print(processes.size() + " ");
        for (Process process : processes) {
            System.out.print("(" + process.A + " " + process.B + " " + process.C + " " + process.M + ") ");
        }
        System.out.println();
    }

    // Sort and print sorted input
    static void print_sorted_input(ArrayList<Process> processes) {
        // Java 8 one-liner to sort objects based on int property
        Collections.sort(processes, (p1, p2) -> p1.A - p2.A);

        System.out.print("The (sorted) input is: ");
        System.out.print(processes.size() + " ");
        for (Process process : processes) {
            System.out.print("(" + process.A + " " + process.B + " " + process.C + " " + process.M + ") ");
        }
        System.out.println();
    }

    // Return status for verbose
    static String verbose_status(ArrayList<Process> processes, boolean RR) {
        String result = "";

        for (Process p : processes) {
            String state = p.state;
            int burst = 0;
            switch (state) {
                case "blocked":
                    burst = p.io_burst;
                    break;
                case "running":
                    if (RR == true) {
                        burst = Math.min(p.cpu_burst, p.quantum);
                    } else {
                        burst = p.cpu_burst;
                    }

                    break;
                default:
                    burst = 0;
                    break;
            }

            result += String.format("%12s", state) + String.format("%3d", burst);
        }

        return result;
    }

    static void print_summary(ArrayList<Process> processes) {
        System.out.println("Summary Data:");
        System.out.println("\tFinishing time: " + finishing_time);
        System.out.printf("\tCPU Utilization: %.6f\n", CPU_utilization);
        System.out.printf("\tI/O Utilization: %.6f\n", IO_utilization);
        System.out.printf("\tThroughput: %.6f processes per hundred cycles\n",
                (100.0 * processes.size() / finishing_time));

        for (Process p : processes) {
            avg_turnaround_time += p.turnaround_time;
            avg_waiting_time += p.waiting_time;
        }

        avg_turnaround_time /= processes.size();
        avg_waiting_time /= processes.size();

        System.out.printf("\tAverage turnaround time: %.6f\n", avg_turnaround_time);
        System.out.printf("\tAverage waiting time: %.6f\n", avg_waiting_time);
        System.out.println();
    }

    static void FCFS(ArrayList<Process> processes, boolean verbose) throws IOException {
        // Reset processes
        Process.reset_process(processes);
        // Reset summary data & random_count
        reset_summary();

        int cycle = 0; // Track cycle count
        int num = processes.size(); // # of processes
        Process runningProcess = null; // running process

        String vb = "\nThis detailed printout gives the state and remaining burst for each process\n\n";

        Queue<Process> unstarted = new LinkedList<>();
        ArrayList<Process> ready = new ArrayList<>();
        ArrayList<Process> blocked = new ArrayList<>();

        // Add processes to queue
        for (Process p : processes) {
            unstarted.add(p);
        }

        while (num > 0) {
            vb += "Before Cycle" + String.format("%5d", cycle) + ":" + verbose_status(processes, false) + ".\n";

            // Blocked Processes
            if (blocked.size() != 0) {
                for (Iterator<Process> it = blocked.iterator(); it.hasNext();) {
                    Process b = it.next();
                    b.io_burst--;
                    if (b.io_burst == 0) {
                        b.state = "ready";
                        b.ready_cycle = cycle;
                        ready.add(b);
                        it.remove();
                    }
                }
            }

            // Arriving processes
            while (unstarted.peek() != null && unstarted.peek().A <= cycle) {
                unstarted.peek().state = "ready";
                unstarted.peek().ready_cycle = cycle;
                ready.add(unstarted.poll());
            }

            // Running Processes
            if (runningProcess == null) {
                sort_ready_RR(ready);
                runningProcess = pick(ready);
            } else {
                runningProcess.cpu_burst--;
                runningProcess.remaining_time--;

                // Handle tie-breaks
                if (runningProcess.remaining_time == 0) {
                    // terminate
                    runningProcess.state = "terminated";
                    runningProcess.finishing_time = cycle;
                    runningProcess.turnaround_time = runningProcess.finishing_time - runningProcess.A;
                    num--;
                    sort_ready_RR(ready);
                    runningProcess = pick(ready);
                } else if (runningProcess.cpu_burst == 0) {
                    // blocked
                    runningProcess.state = "blocked";
                    runningProcess.io_time += runningProcess.io_burst;
                    blocked.add(runningProcess);
                    sort_ready_RR(ready);
                    runningProcess = pick(ready);
                }
            }

            // Calculate summary data
            // Calculate wait time (time in ready state)
            boolean cpu_used = false, io_used = false;
            for (Process p : processes) {
                switch (p.state) {
                    case "ready":
                        p.waiting_time++;
                        break;
                    case "blocked":
                        io_used = true;
                        break;
                    case "running":
                        cpu_used = true;
                        break;
                    default:
                        break;
                }
            }

            if (cpu_used == true)
                CPU_utilization++;

            if (io_used == true)
                IO_utilization++;

            // Next cycle
            cycle++;
        }

        // For summary data
        finishing_time = cycle - 1;
        CPU_utilization /= finishing_time;
        IO_utilization /= finishing_time;

        // Print verbose
        if (verbose == true)
            System.out.print(vb);

        System.out.println("The scheduling algorithm used was First Come First Served\n");
    }

    // Select one of ready processes (used for RR)
    static Process RR_pick(ArrayList<Process> ready) throws IOException {
        Process runningProcess = null;
        if (ready.size() != 0) {
            runningProcess = ready.get(0);
            runningProcess.state = "running";
            runningProcess.quantum = 2;
            // Calculate cpu burst if never calculated
            if (runningProcess.cpu_burst == 0)
                runningProcess.cpu_burst = Math.min(randomOS(runningProcess.B), runningProcess.remaining_time);
            // Calculate io burst if never calculated
            if (runningProcess.io_burst == 0)
                runningProcess.io_burst = runningProcess.cpu_burst * runningProcess.M;

            // Remove
            ready.remove(0);
        }

        return runningProcess;
    }

    // Pick element from ready (used for LCFS)
    static Process pick(ArrayList<Process> ready) throws IOException {
        Process runningProcess = null;
        if (ready.size() != 0) {
            runningProcess = ready.get(0);
            runningProcess.state = "running";

            // Calculate cpu burst
            runningProcess.cpu_burst = Math.min(randomOS(runningProcess.B), runningProcess.remaining_time);
            // Calculate io burst
            runningProcess.io_burst = runningProcess.cpu_burst * runningProcess.M;

            // Remove
            ready.remove(0);
        }

        return runningProcess;
    }

    static void sort_ready_RR(ArrayList<Process> ready) {
        // Sort ready list based on A
        // If A is equal, then sort based on order of input
        for (int i = 0; i < ready.size() - 1; i++) {
            for (int j = i + 1; j < ready.size(); j++) {
                if (ready.get(i).ready_cycle > ready.get(j).ready_cycle) {
                    Collections.swap(ready, i, j);
                } else if (ready.get(i).ready_cycle == ready.get(j).ready_cycle) {
                    if (ready.get(i).A > ready.get(j).A) {
                        Collections.swap(ready, i, j);
                    } else if (ready.get(i).A == ready.get(j).A && ready.get(i).index > ready.get(j).index) {
                        Collections.swap(ready, i, j);
                    }
                }
            }
        }
    }

    // Used to sort ready list for LCFS
    static void sort_ready_LCFS(ArrayList<Process> ready) {
        // Sort ready list based on A
        // If A is equal, then sort based on order of input
        for (int i = 0; i < ready.size() - 1; i++) {
            for (int j = i + 1; j < ready.size(); j++) {
                if (ready.get(i).ready_cycle < ready.get(j).ready_cycle) {
                    Collections.swap(ready, i, j);
                } else if (ready.get(i).ready_cycle == ready.get(j).ready_cycle) {
                    if (ready.get(i).A > ready.get(j).A) {
                        Collections.swap(ready, i, j);
                    } else if (ready.get(i).A == ready.get(j).A && ready.get(i).index > ready.get(j).index) {
                        Collections.swap(ready, i, j);
                    }
                }
            }
        }
    }

    static void sort_ready_HPRN(ArrayList<Process> ready) {
        // Sort ready list based on A
        // If A is equal, then sort based on order of input
        for (Process p : ready) {
            p.r = (p.T - p.arrival_cycle) / Math.max(1, p.cpu_util);
        }

        for (int i = 0; i < ready.size() - 1; i++) {
            for (int j = i + 1; j < ready.size(); j++) {
                if (ready.get(i).r < ready.get(j).r) {
                    Collections.swap(ready, i, j);
                } else if (Math.abs(ready.get(i).r - ready.get(j).r) < EPSILON) {
                    if (ready.get(i).ready_cycle == ready.get(j).ready_cycle) {
                        if (ready.get(i).A > ready.get(j).A) {
                            Collections.swap(ready, i, j);
                        } else if (ready.get(i).A == ready.get(j).A && ready.get(i).index > ready.get(j).index) {
                            Collections.swap(ready, i, j);
                        }
                    }
                }
            }
        }
    }

    // Reset summary data
    static void reset_summary() {
        finishing_time = 0;
        CPU_utilization = 0;
        IO_utilization = 0;
        throughput = 0;
        avg_turnaround_time = 0;
        avg_waiting_time = 0;
        random_count = 0; // Reset random_count
    }

    static void RR(ArrayList<Process> processes, boolean verbose) throws IOException {
        // Reset processes
        Process.reset_process(processes);
        // Reset summary data & random_count
        reset_summary();

        int cycle = 0; // Track cycle count
        int num = processes.size(); // # of processes
        Process runningProcess = null; // running process

        String vb = "\nThis detailed printout gives the state and remaining burst for each process\n\n";

        // Use Queue data structure (FIFO)
        Queue<Process> unstarted = new LinkedList<>();
        ArrayList<Process> ready = new ArrayList<>(); // Used to sort ready
        ArrayList<Process> blocked = new ArrayList<>();

        // Add processes to queue
        for (Process p : processes) {
            unstarted.add(p);
        }

        while (num > 0) {
            vb += "Before Cycle" + String.format("%5d", cycle) + ":" + verbose_status(processes, true) + ".\n";

            // Blocked processes
            if (blocked.size() != 0) {
                for (Iterator<Process> it = blocked.iterator(); it.hasNext();) {
                    Process b = it.next();
                    b.io_burst--;
                    if (b.io_burst == 0) {
                        b.state = "ready";
                        b.ready_cycle = cycle;
                        ready.add(b);
                        it.remove();
                    }
                }
            }

            // Arriving processes
            while (unstarted.peek() != null && unstarted.peek().A <= cycle) {
                unstarted.peek().state = "ready";
                unstarted.peek().ready_cycle = cycle;
                ready.add(unstarted.poll());
            }

            // Running processes
            if (runningProcess == null) {
                // Pick running process from ready queue
                sort_ready_RR(ready);
                runningProcess = RR_pick(ready);
            } else {
                runningProcess.cpu_burst--;
                runningProcess.remaining_time--;
                runningProcess.quantum--;

                // Handle tie-breaks
                if (runningProcess.remaining_time == 0) {
                    // terminate
                    runningProcess.state = "terminated";
                    runningProcess.finishing_time = cycle;
                    runningProcess.turnaround_time = runningProcess.finishing_time - runningProcess.A;
                    num--;
                    sort_ready_RR(ready);
                    runningProcess = RR_pick(ready);
                } else if (runningProcess.cpu_burst == 0) {
                    // blocked
                    runningProcess.state = "blocked";
                    runningProcess.io_time += runningProcess.io_burst;
                    blocked.add(runningProcess);
                    sort_ready_RR(ready);
                    runningProcess = RR_pick(ready);
                } else if (runningProcess.quantum == 0) {
                    // pre-empted
                    runningProcess.state = "ready";
                    runningProcess.ready_cycle = cycle;
                    ready.add(runningProcess);
                    sort_ready_RR(ready);
                    runningProcess = RR_pick(ready);
                }
            }

            // Calculate summary data
            // Calculate wait time (time in ready state)
            boolean cpu_used = false, io_used = false;
            for (Process p : processes) {
                switch (p.state) {
                    case "ready":
                        p.waiting_time++;
                        break;
                    case "blocked":
                        io_used = true;
                        break;
                    case "running":
                        cpu_used = true;
                        break;
                    default:
                        break;
                }
            }

            if (cpu_used == true)
                CPU_utilization++;

            if (io_used == true)
                IO_utilization++;

            // Next cycle
            cycle++;
        }

        // For summary data
        finishing_time = cycle - 1;
        CPU_utilization /= finishing_time;
        IO_utilization /= finishing_time;

        // Print verbose
        if (verbose == true)
            System.out.print(vb);

        System.out.println("The scheduling algorithm used was Round Robin\n");
    }

    static void LCFS(ArrayList<Process> processes, boolean verbose) throws IOException {
        // Reset processes
        Process.reset_process(processes);
        // Reset summary data & random_count
        reset_summary();

        int cycle = 0; // Track cycle count
        int num = processes.size(); // # of processes
        Process runningProcess = null; // running process

        String vb = "\nThis detailed printout gives the state and remaining burst for each process\n\n";

        Queue<Process> unstarted = new LinkedList<>();
        ArrayList<Process> ready = new ArrayList<>();
        ArrayList<Process> blocked = new ArrayList<>();

        // Add processes to queue
        for (Process p : processes) {
            unstarted.add(p);
        }

        while (num > 0) {
            vb += "Before Cycle" + String.format("%5d", cycle) + ":" + verbose_status(processes, false) + ".\n";

            // Blocked Processes
            if (blocked.size() != 0) {
                for (Iterator<Process> it = blocked.iterator(); it.hasNext();) {
                    Process b = it.next();
                    b.io_burst--;
                    if (b.io_burst == 0) {
                        b.state = "ready";
                        b.ready_cycle = cycle;
                        ready.add(b);
                        it.remove();
                    }
                }
            }

            // Arriving processes
            while (unstarted.peek() != null && unstarted.peek().A <= cycle) {
                unstarted.peek().state = "ready";
                unstarted.peek().ready_cycle = cycle;
                ready.add(unstarted.poll());
            }

            // Running Processes
            if (runningProcess == null) {
                sort_ready_LCFS(ready);
                runningProcess = pick(ready);
            } else {
                runningProcess.cpu_burst--;
                runningProcess.remaining_time--;

                // Handle tie-breaks
                if (runningProcess.remaining_time == 0) {
                    // terminate
                    runningProcess.state = "terminated";
                    runningProcess.finishing_time = cycle;
                    runningProcess.turnaround_time = runningProcess.finishing_time - runningProcess.A;
                    num--;
                    sort_ready_LCFS(ready);
                    runningProcess = pick(ready);
                } else if (runningProcess.cpu_burst == 0) {
                    // blocked
                    runningProcess.state = "blocked";
                    runningProcess.io_time += runningProcess.io_burst;
                    blocked.add(runningProcess);
                    sort_ready_LCFS(ready);
                    runningProcess = pick(ready);
                }
            }

            // Calculate summary data
            // Calculate wait time (time in ready state)
            boolean cpu_used = false, io_used = false;
            for (Process p : processes) {
                switch (p.state) {
                    case "ready":
                        p.waiting_time++;
                        break;
                    case "blocked":
                        io_used = true;
                        break;
                    case "running":
                        cpu_used = true;
                        break;
                    default:
                        break;
                }
            }

            if (cpu_used == true)
                CPU_utilization++;

            if (io_used == true)
                IO_utilization++;

            // Next cycle
            cycle++;
        }

        // For summary data
        finishing_time = cycle - 1;
        CPU_utilization /= finishing_time;
        IO_utilization /= finishing_time;

        // Print verbose
        if (verbose == true)
            System.out.print(vb);

        System.out.println("The scheduling algorithm used was Last Come First Served\n");
    }

    static void HPRN(ArrayList<Process> processes, boolean verbose) throws IOException {
        // Reset processes
        Process.reset_process(processes);
        // Reset summary data & random_count
        reset_summary();

        int cycle = 0; // Track cycle count
        int num = processes.size(); // # of processes
        Process runningProcess = null; // running process

        String vb = "\nThis detailed printout gives the state and remaining burst for each process\n\n";

        Queue<Process> unstarted = new LinkedList<>();
        ArrayList<Process> ready = new ArrayList<>();
        ArrayList<Process> blocked = new ArrayList<>();

        // Add processes to queue
        for (Process p : processes) {
            unstarted.add(p);
        }

        while (num > 0) {
            // For each process, let r = T/t; where T is the wall clock time this process
            // has been in system and t is the running time of the process to date.
            // We call r the penalty ratio and run the process having the highest r value.

            vb += "Before Cycle" + String.format("%5d", cycle) + ":" + verbose_status(processes, false) + ".\n";

            // Set wall clock time processes have been in system
            for (Process p : processes) {
                p.T = cycle;
            }

            // Blocked Processes
            if (blocked.size() != 0) {
                for (Iterator<Process> it = blocked.iterator(); it.hasNext();) {
                    Process b = it.next();
                    b.io_burst--;
                    if (b.io_burst == 0) {
                        b.state = "ready";
                        b.ready_cycle = cycle;
                        ready.add(b);
                        it.remove();
                    }
                }
            }

            // Arriving processes
            while (unstarted.peek() != null && unstarted.peek().A <= cycle) {
                unstarted.peek().state = "ready";
                unstarted.peek().ready_cycle = cycle;
                unstarted.peek().arrival_cycle = cycle;
                ready.add(unstarted.poll());
            }

            // Running Processes
            if (runningProcess == null) {
                sort_ready_HPRN(ready);
                runningProcess = pick(ready);
            } else {
                runningProcess.cpu_burst--;
                runningProcess.remaining_time--;
                runningProcess.cpu_util++; // HPRN numerator

                // Handle tie-breaks
                if (runningProcess.remaining_time == 0) {
                    // terminate
                    runningProcess.state = "terminated";
                    runningProcess.finishing_time = cycle;
                    runningProcess.turnaround_time = runningProcess.finishing_time - runningProcess.A;
                    num--;
                    sort_ready_HPRN(ready);
                    runningProcess = pick(ready);
                } else if (runningProcess.cpu_burst == 0) {
                    // blocked
                    runningProcess.state = "blocked";
                    runningProcess.io_time += runningProcess.io_burst;
                    blocked.add(runningProcess);
                    sort_ready_HPRN(ready);
                    runningProcess = pick(ready);
                }
            }

            // Calculate summary data
            // Calculate wait time (time in ready state)
            boolean cpu_used = false, io_used = false;
            for (Process p : processes) {
                switch (p.state) {
                    case "ready":
                        p.waiting_time++;
                        break;
                    case "blocked":
                        io_used = true;
                        break;
                    case "running":
                        cpu_used = true;
                        break;
                    default:
                        break;
                }
            }

            if (cpu_used == true)
                CPU_utilization++;

            if (io_used == true)
                IO_utilization++;

            // Next cycle
            cycle++;
        }

        // For summary data
        finishing_time = cycle - 1;
        CPU_utilization /= finishing_time;
        IO_utilization /= finishing_time;

        // Print verbose
        if (verbose == true)
            System.out.print(vb);

        System.out.println("The scheduling algorithm used was HPRN\n");
    }
}
