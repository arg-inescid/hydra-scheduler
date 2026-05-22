package org.graalvm.argo.dataset.generator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.graalvm.argo.dataset.Invocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * This class generates an invocation trace from the azure dataset. Given a set
 * of options, it will generate a file with one invocation per line.
 */
public class AzureInvocationTraceGenerator extends AbstractInvocationTraceGenerator {

    public static final String DELIMITER = AbstractInvocationTraceGenerator.DELIMITER;
    private static final int MINUTES_COLUMN_OFFSET = 3;

    private final List<Invocation> invocations = new ArrayList<>();
    private final Map<String, Owner> owners = new HashMap<>(2048);
    private int skipped = 0;

    public static void main(String[] args) throws Exception {
        new AzureInvocationTraceGenerator().run(args);
    }

    @Override
    protected void addSourceOptions(Options options) {
        Option input = new Option("d", "day", true, "Input dataset day (eg., d03).");
        input.setRequired(true);
        options.addOption(input);

        Option firstMinute = new Option("b", "bmin", true, "First minute to include from the trace ([1,1440]).");
        firstMinute.setRequired(false);
        options.addOption(firstMinute);

        Option lastMinute = new Option("e", "emin", true, "Last minute to include from the trace ([1,1440]).");
        lastMinute.setRequired(false);
        options.addOption(lastMinute);
    }

    @Override
    protected List<Invocation> loadInvocations(CommandLine cmd) {
        String day = cmd.getOptionValue("day");
        int firstMinute = Integer.parseInt(cmd.getOptionValue("bmin", "701"));
        int lastMinute = Integer.parseInt(cmd.getOptionValue("emin", "710"));

        FunctionInfoStorage.fillFunctionData(day);
        processDay(day, firstMinute, lastMinute);
        return invocations;
    }

    private void processFunction(String line, int firstMinute, int lastMinute) {
        String[] splitRow = line.split(DELIMITER);
        String owner = splitRow[0];
        String app = splitRow[1];
        String function = splitRow[2];

        /* If there is no record about this function about avg duration or memory, then skip */
        if (!FunctionInfoStorage.DURATIONS.containsKey(function) || !FunctionInfoStorage.MEMORIES.containsKey(app)) {
            ++skipped;
            return;
        }

        int memory = FunctionInfoStorage.MEMORIES.get(app);
        int duration = FunctionInfoStorage.DURATIONS.get(function);
        int currentMinute = firstMinute;
        int invocationCount = 0;
        while (currentMinute <= lastMinute) {
            int invocationsForMinute = Integer.parseInt(splitRow[currentMinute + MINUTES_COLUMN_OFFSET]);
            invocationCount += invocationsForMinute;
            int minBeginningMs = (currentMinute - 1) * 60000;
            int minEndMs = minBeginningMs + 60000;
            for (int i = 0; i < invocationsForMinute; ++i) {
                int timestamp = ThreadLocalRandom.current().nextInt(minBeginningMs, minEndMs);
                invocations.add(new Invocation(owner, function, memory, duration, timestamp));
            }
            ++currentMinute;
        }
        if (invocationCount > 0) {
            if (!owners.containsKey(owner)) {
                owners.put(owner, new Owner(owner));
            }
            Owner currentOwner = owners.get(owner);
            currentOwner.addFunction(function);
            currentOwner.addInvocations(invocationCount);
        }
    }

    /*
     * Read data from the CSV file, generate timestamps for the desired time frame.
     * File expected syntax: HashOwner, HashApp, HashFunction, Trigger, 1, 2, 3...
     */
    private void processDay(String datasetId, int firstMinute, int lastMinute) {
        try {
            File file = new File("input/invocations_per_function_md.anon." + datasetId + ".csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            br.readLine(); // To skip the header
            int fcounter = 1;

            while ((line = br.readLine()) != null) {
                processFunction(line, firstMinute, lastMinute);
                System.out.println("Processed function " + fcounter++);
            }
            System.out.println("Skipped " + skipped + " functions due to lack of information.");
            br.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        /* At this point, we have the unordered list of all invocations */
        invocations.sort(Comparator.comparingInt(Invocation::getTimestamp));
        System.out.println("Finished sorting.");
    }

    @Override
    protected void downscaleByUser(List<Invocation> invocations, int maxUsers) {
        Set<String> selectedOwners = owners.values().stream()
                .sorted(Comparator.comparingInt(Owner::getFunctions).reversed())
                .limit(maxUsers).map(Owner::getOwnerHash).collect(Collectors.toSet());
        invocations.removeIf(i -> !selectedOwners.contains(i.getOwner()));
    }
}
