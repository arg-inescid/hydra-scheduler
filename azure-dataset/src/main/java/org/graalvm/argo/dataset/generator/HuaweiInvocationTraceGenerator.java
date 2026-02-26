package org.graalvm.argo.dataset.generator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.graalvm.argo.dataset.Invocation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates per-invocation traces from Huawei private dataset (per-minute).
 *
 * Expected input directories (relative to repo root by default):
 *  - input/huawei_private_minute/requests_minute/day_XXX.csv
 *  - input/huawei_private_minute/function_delay_minute/day_XXX.csv
 *  - input/huawei_private_minute/memory_limit_minute/day_XXX.csv
 *
 * Output schema:
 *  HashOwner,HashFunction,AverageAllocatedMb,AverageDuration,Timestamp
 */
public class HuaweiInvocationTraceGenerator {

    private static final String DELIMITER = ",";
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_DAY = 1440;
    private static final int SECONDS_PER_DAY = 86_400;

    private static Options prepareOptions() {
        Options options = new Options();
        Option day = new Option("d", "day", true, "Input dataset day (000-234).");
        day.setRequired(true);
        options.addOption(day);
        Option startDay = new Option(null, "startDay", true, "Start dataset day (000-234).");
        startDay.setRequired(false);
        options.addOption(startDay);
        Option endDay = new Option(null, "endDay", true, "End dataset day (000-234).");
        endDay.setRequired(false);
        options.addOption(endDay);
        Option output = new Option("t", "trace", true, "Output invocation trace file path.");
        output.setRequired(true);
        options.addOption(output);
        Option inputDir = new Option("i", "inputDir", true, "Input Huawei dataset base directory.");
        inputDir.setRequired(false);
        options.addOption(inputDir);
        Option firstMinute = new Option("b", "bmin", true, "First minute to include from the trace ([0,1439]).");
        firstMinute.setRequired(false);
        options.addOption(firstMinute);
        Option lastMinute = new Option("e", "emin", true, "Last minute to include from the trace ([0,1439]).");
        lastMinute.setRequired(false);
        options.addOption(lastMinute);
        Option maxMemory = new Option("m", "mem", true, "Maximum memory (in MBs) used by generated trace.");
        maxMemory.setRequired(false);
        options.addOption(maxMemory);
        Option maxConcInv = new Option("i", "cinv", true, "Maximum number of concurrent invocations in the generated trace.");
        maxConcInv.setRequired(false);
        options.addOption(maxConcInv);
        Option maxFunctions = new Option("f", "functions", true, "Maximum number of functions in the generated trace.");
        maxFunctions.setRequired(false);
        options.addOption(maxFunctions);
        Option maxUsers = new Option("u", "users", true, "Maximum number of users present in the generated trace.");
        maxUsers.setRequired(false);
        options.addOption(maxUsers);
        return options;
    }

    public static void main(String[] args) throws Exception {
        Options options = prepareOptions();
        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            String outputFilePath = cmd.getOptionValue("trace");
            String inputBaseDir = cmd.getOptionValue("inputDir", "input/huawei_private_minute");
            int firstMinute = Integer.parseInt(cmd.getOptionValue("bmin", "0"));
            int lastMinute = Integer.parseInt(cmd.getOptionValue("emin", "1439"));
            int maxMemory = Integer.parseInt(cmd.getOptionValue("mem", "0"));
            int maxConcInv = Integer.parseInt(cmd.getOptionValue("cinv", "0"));
            int maxFunctions = Integer.parseInt(cmd.getOptionValue("functions", "0"));
            int maxUsers = Integer.parseInt(cmd.getOptionValue("users", "0"));

            Integer startDay = cmd.hasOption("startDay") ? parseDay(cmd.getOptionValue("startDay")) : null;
            Integer endDay = cmd.hasOption("endDay") ? parseDay(cmd.getOptionValue("endDay")) : null;
            int singleDay = parseDay(cmd.getOptionValue("day"));
            if (startDay == null || endDay == null) {
                startDay = singleDay;
                endDay = singleDay;
            }

            validateMinuteRange(firstMinute, lastMinute);
            List<Invocation> invocations = loadInvocations(inputBaseDir, startDay, endDay, firstMinute, lastMinute);

            if (maxFunctions != 0) {
                System.out.println("Number of invocations *before* filter by number of functions: " + invocations.size());
                downscaleByFunctions(invocations, maxFunctions);
                System.out.println("Number of invocations *after* filter by number of functions: " + invocations.size());
            }

            if (maxConcInv != 0) {
                System.out.println("Number of invocations *before* filter by concurrent invocations: " + invocations.size());
                downscaleByConcurrentInvocations(invocations, maxConcInv);
                System.out.println("Number of invocations *after* filter by concurrent invocations: " + invocations.size());
            }

            if (maxUsers != 0) {
                System.out.println("Number of invocations *before* filter by number of users: " + invocations.size());
                downscaleByUser(invocations, maxUsers);
                System.out.println("Number of invocations *after* filter by number of users: " + invocations.size());
            }

            if (maxMemory != 0) {
                System.out.println("Number of invocations *before* filter by memory: " + invocations.size());
                downscaleByMemory(invocations, maxMemory);
                System.out.println("Number of invocations *after* filter by memory: " + invocations.size());
            }

            writeInvocations(outputFilePath, invocations);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", options);
        }
    }

    private static int parseDay(String dayStr) throws ParseException {
        try {
            return Integer.parseInt(dayStr);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Invalid day value: " + dayStr);
        }
    }

    private static void validateMinuteRange(int firstMinute, int lastMinute) throws ParseException {
        if (firstMinute < 0 || lastMinute >= MINUTES_PER_DAY || firstMinute > lastMinute) {
            throw new ParseException("Invalid minute range: [" + firstMinute + "," + lastMinute + "]");
        }
    }

    private static List<Invocation> loadInvocations(String inputBaseDir,
                                                    int startDay,
                                                    int endDay,
                                                    int firstMinute,
                                                    int lastMinute) throws IOException {
        List<Invocation> invocations = new ArrayList<>();
        for (int day = startDay; day <= endDay; day++) {
            processDay(invocations, inputBaseDir, day, firstMinute, lastMinute);
        }
        invocations.sort(Comparator.comparingInt(Invocation::getTimestamp));
        return invocations;
    }

    private static void processDay(List<Invocation> invocations,
                                   String inputBaseDir,
                                   int day,
                                   int firstMinute,
                                   int lastMinute) throws IOException {
        String dayFile = String.format("day_%03d.csv", day);
        File reqFile = new File(inputBaseDir, "requests_minute/" + dayFile);
        File delayFile = new File(inputBaseDir, "function_delay_minute/" + dayFile);
        File memFile = new File(inputBaseDir, "memory_limit_minute/" + dayFile);

        if (!reqFile.exists() || !delayFile.exists() || !memFile.exists()) {
            throw new IOException("Missing input files for day " + day + " under " + inputBaseDir);
        }

        int dayStartSeconds = day * SECONDS_PER_DAY;
        int dayEndSeconds = dayStartSeconds + SECONDS_PER_DAY;
        try (BufferedReader reqReader = new BufferedReader(new FileReader(reqFile));
             BufferedReader delayReader = new BufferedReader(new FileReader(delayFile));
             BufferedReader memReader = new BufferedReader(new FileReader(memFile))) {

            String reqHeader = reqReader.readLine();
            String delayHeader = delayReader.readLine();
            String memHeader = memReader.readLine();

            if (reqHeader == null || delayHeader == null || memHeader == null) {
                throw new IOException("Empty header in one of the files for day " + day);
            }

            int functionCount = reqHeader.split(DELIMITER, -1).length - 2;
            int delayFunctionCount = delayHeader.split(DELIMITER, -1).length - 2;
            int memFunctionCount = memHeader.split(DELIMITER, -1).length - 2;
            if (functionCount != delayFunctionCount || functionCount != memFunctionCount) {
                throw new IOException("Header mismatch for day " + day + ": different function counts.");
            }

            String reqLine;
            String delayLine;
            String memLine;
            while ((reqLine = reqReader.readLine()) != null &&
                   (delayLine = delayReader.readLine()) != null &&
                   (memLine = memReader.readLine()) != null) {

                String[] reqRow = reqLine.split(DELIMITER, -1);
                String[] delayRow = delayLine.split(DELIMITER, -1);
                String[] memRow = memLine.split(DELIMITER, -1);

                int timeSeconds = Integer.parseInt(reqRow[1]);
                if (timeSeconds < dayStartSeconds || timeSeconds >= dayEndSeconds) {
                    continue;
                }
                int minuteIndex = (timeSeconds - dayStartSeconds) / SECONDS_PER_MINUTE;
                if (minuteIndex < firstMinute || minuteIndex > lastMinute) {
                    continue;
                }

                for (int functionId = 0; functionId < functionCount; functionId++) {
                    String countStr = reqRow[2 + functionId];
                    if (countStr == null || countStr.isEmpty()) {
                        continue;
                    }
                    int count = (int) Double.parseDouble(countStr);
                    if (count <= 0) {
                        continue;
                    }

                    String delayStr = delayRow[2 + functionId];
                    String memStr = memRow[2 + functionId];
                    if (delayStr == null || delayStr.isEmpty() || memStr == null || memStr.isEmpty()) {
                        continue;
                    }

                    int durationMs = (int) Math.round(Double.parseDouble(delayStr));
                    int memoryMb = (int) Math.round(Double.parseDouble(memStr));

                    String owner = "owner_" + functionId;
                    String function = "f_" + functionId;

                    for (int i = 0; i < count; i++) {
                        int randomOffsetMs = ThreadLocalRandom.current().nextInt(SECONDS_PER_MINUTE * 1000);
                        long timestampMs = (long) timeSeconds * 1000L + randomOffsetMs;
                        invocations.add(new Invocation(owner, function, memoryMb, durationMs, (int) timestampMs));
                    }
                }
            }
        }
    }

    private static void writeInvocations(String outputFilePath, List<Invocation> invocations) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, false))) {
            writer.write("HashOwner,HashFunction,AverageAllocatedMb,AverageDuration,Timestamp");
            writer.newLine();
            if (invocations.isEmpty()) {
                return;
            }
            int firstTimestamp = invocations.get(0).getTimestamp();
            for (Invocation invocation : invocations) {
                writer.write(invocation.toString(firstTimestamp));
                writer.newLine();
            }
        }
    }

    /* Remove invocations that go over the maximum number of concurrent invocations. */
    private static void downscaleByConcurrentInvocations(List<Invocation> invocations, int maxConcInv) {
        List<Invocation> activeInvocations = new ArrayList<>();
        ListIterator<Invocation> iter = invocations.listIterator();
        while (iter.hasNext()) {
            Invocation currentInvocation = iter.next();
            int currentInvocationTimestamp = currentInvocation.getTimestamp();

            activeInvocations.removeIf(f -> currentInvocationTimestamp >= f.getEndTimestamp());
            long currentInvokes = activeInvocations.size();

            if (currentInvokes + 1 <= maxConcInv) {
                activeInvocations.add(currentInvocation);
            } else {
                iter.remove();
            }
        }
    }

    /* Remove invocations that are not from the N first functions that appear in the trace. */
    private static void downscaleByFunctions(List<Invocation> invocations, int maxFunctions) {
        Set<String> selectedFunctions = new HashSet<>();
        Map<String, Long> invocationsFunction = invocations.stream()
                .collect(Collectors.groupingBy(Invocation::getFunction, Collectors.counting()));
        int skipFunctions = (invocationsFunction.size() - maxFunctions) / 2;
        invocationsFunction.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .skip(skipFunctions)
                .limit(maxFunctions)
                .forEach(entry -> selectedFunctions.add(entry.getKey()));
        invocations.removeIf(i -> !selectedFunctions.contains(i.getFunction()));
    }

    /* Remove invocations that are not from the N more popular users. */
    private static void downscaleByUser(List<Invocation> invocations, int maxUsers) {
        Map<String, Set<String>> ownerFunctions = new HashMap<>();
        for (Invocation inv : invocations) {
            ownerFunctions.computeIfAbsent(inv.getOwner(), k -> new HashSet<>()).add(inv.getFunction());
        }
        Set<String> selectedOwners = ownerFunctions.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(maxUsers)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        invocations.removeIf(i -> !selectedOwners.contains(i.getOwner()));
    }

    /* Remove invocations that go over the maximum memory. */
    private static void downscaleByMemory(List<Invocation> invocations, int maxMemory) {
        List<Invocation> activeInvocations = new ArrayList<>();
        ListIterator<Invocation> iter = invocations.listIterator();
        while (iter.hasNext()) {
            Invocation currentInvocation = iter.next();
            int currentInvocationTimestamp = currentInvocation.getTimestamp();

            activeInvocations.removeIf(f -> currentInvocationTimestamp >= f.getEndTimestamp());
            int currentConsumption = activeInvocations.stream().mapToInt(Invocation::getMemory).sum();

            if (currentConsumption + currentInvocation.getMemory() <= maxMemory) {
                activeInvocations.add(currentInvocation);
            } else {
                iter.remove();
            }
        }
    }
}
