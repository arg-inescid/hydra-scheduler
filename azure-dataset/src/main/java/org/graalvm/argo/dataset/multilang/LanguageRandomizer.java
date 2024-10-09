package org.graalvm.argo.dataset.multilang;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.graalvm.argo.dataset.Invocation;
import org.graalvm.argo.dataset.generator.InvocationTraceGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

public class LanguageRandomizer {

    // Source: https://newrelic.com/resources/report/serverless-benchmark-report-aws-lambda-2020
    private static final int JAVASCRIPT_PERC = 48; // 54
    private static final int JAVASCRIPT_FUNC = 3;
    private static final int PYTHON_PERC = 24;     // 38
    private static final int PYTHON_FUNC = 3;
    private static final int JAVA_PERC = 28;       // 8
    private static final int JAVA_FUNC = 3;

    private static final SplittableRandom random = new SplittableRandom();

    public static void main(String[] args) {
        Options options = prepareOptions();
        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            String inputFilePath = cmd.getOptionValue("input");
            String outputFilePath = cmd.getOptionValue("trace", inputFilePath);
            processLanguages(inputFilePath, outputFilePath);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processLanguages(String inputFilePath, String outputFilePath) {
        List<Invocation> invocations = getInvocations(inputFilePath);
        Map<String, FunctionRecord> languagesFunction = getLanguages(invocations);
        writeInvocationsToFile(invocations, languagesFunction, outputFilePath);
    }

    private static void writeInvocationsToFile(List<Invocation> invocations, Map<String, FunctionRecord> languagesFunction, String outputFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, false))) {
            writer.write("HashOwner,HashFunction,AverageAllocatedMb,AverageDuration,Timestamp,Language,Function");
            writer.newLine();
            for (Invocation invocation : invocations) {
                writer.write(String.format("%s,%s", invocation.toString(), languagesFunction.get(invocation.getFunction()).toString()));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, FunctionRecord> getLanguages(List<Invocation> invocations) {
        int invocationsNumber = invocations.size();
        Map<String, FunctionRecord> result = new HashMap<>();

        Map<String, Long> invocationsFunction = invocations.stream()
                .collect(Collectors.groupingBy(Invocation::getFunction, Collectors.counting()));
        List<Map.Entry<String, Long>> functionsList = new ArrayList<>(invocationsFunction.entrySet());
        Collections.shuffle(functionsList);

        int currentInvocationsNumber = 0;
        int jsThreshold = (int) (invocationsNumber * ((double) JAVASCRIPT_PERC / 100));
        int pyThreshold = (int) (invocationsNumber * ((double) PYTHON_PERC / 100)) + jsThreshold;
        int jvThreshold = (int) (invocationsNumber * ((double) JAVA_PERC / 100)) + pyThreshold;
        for (Map.Entry<String, Long> functionEntry : functionsList) {
            currentInvocationsNumber += functionEntry.getValue();
            if (currentInvocationsNumber <= jsThreshold) {
                FunctionRecord record = new FunctionRecord(FunctionLanguage.JAVASCRIPT, random.nextInt(JAVASCRIPT_FUNC));
                result.put(functionEntry.getKey(), record);
            } else if (currentInvocationsNumber <= pyThreshold) {
                FunctionRecord record = new FunctionRecord(FunctionLanguage.PYTHON, random.nextInt(PYTHON_FUNC));
                result.put(functionEntry.getKey(), record);
            } else {
                FunctionRecord record = new FunctionRecord(FunctionLanguage.JAVA, random.nextInt(JAVA_FUNC));
                result.put(functionEntry.getKey(), record);
            }
        }
        printStatistics(invocationsNumber, result);
        return result;
    }

    private static void printStatistics(int invocationsNumber, Map<String, FunctionRecord> result) {
        System.out.println("Total invocations: " + invocationsNumber);
        System.out.println("Expected number of JS invocations: " + (int) (invocationsNumber * ((double) JAVASCRIPT_PERC / 100)));
        System.out.println("Expected number of PY invocations: " + (int) (invocationsNumber * ((double) PYTHON_PERC / 100)));
        System.out.println("Expected number of JV invocations: " + (int) (invocationsNumber * ((double) JAVA_PERC / 100)));
        Map<FunctionLanguage, Long> collected = result.entrySet().stream().collect(Collectors.groupingBy(x -> x.getValue().language, Collectors.counting()));
        System.out.println("Number of JS functions: " + collected.get(FunctionLanguage.JAVASCRIPT));
        System.out.println("Number of PY functions: " + collected.get(FunctionLanguage.PYTHON));
        System.out.println("Number of JV functions: " + collected.get(FunctionLanguage.JAVA));
    }

    private static List<Invocation> getInvocations(String inputFilePath) {
        List<Invocation> invocations = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            String[] splitRow;
            br.readLine(); // To skip the header
            while ((line = br.readLine()) != null) {
                splitRow = line.split(InvocationTraceGenerator.DELIMITER);
                String owner = splitRow[0];
                String function = splitRow[1];
                int allocatedMemoryMb = Integer.parseInt(splitRow[2]);
                int duration = Integer.parseInt(splitRow[3]);
                int timestamp = Integer.parseInt(splitRow[4]);
                invocations.add(new Invocation(owner, function, allocatedMemoryMb, duration, timestamp));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return invocations;
    }

    private static Options prepareOptions() {
        Options options = new Options();
        Option input = new Option("i", "input", true, "Input invocation trace file path.");
        input.setRequired(true);
        options.addOption(input);
        Option output = new Option("t", "trace", true, "Output invocation trace file path.");
        output.setRequired(true);
        options.addOption(output);
        return options;
    }
}
