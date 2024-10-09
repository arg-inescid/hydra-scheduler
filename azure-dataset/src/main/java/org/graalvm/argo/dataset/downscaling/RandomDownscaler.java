package org.graalvm.argo.dataset.downscaling;

import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.SplittableRandom;

public class RandomDownscaler {

    private static final SplittableRandom random = new SplittableRandom();

    public static void main(String[] args) {
        Options options = prepareOptions();
        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            String inputFilePath = cmd.getOptionValue("input");
            String outputFilePath = cmd.getOptionValue("trace", inputFilePath);
            int downscaleFactor = Integer.parseInt(cmd.getOptionValue("downscaleFactor"));
            List<String> result = downscale(inputFilePath, downscaleFactor);
            writeInvocationsToFile(result, outputFilePath);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> downscale(String inputFilePath, int downscaleFactor) {
        List<String> acceptedInvocations = new LinkedList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            acceptedInvocations.add(br.readLine()); // To skip the header and add it to the resulting list.
            while ((line = br.readLine()) != null) {
                boolean accept = random.nextInt(downscaleFactor) == 0;
                if (accept) {
                    acceptedInvocations.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return acceptedInvocations;
    }

    private static void writeInvocationsToFile(List<String> invocations, String outputFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, false))) {
            for (String line : invocations) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Options prepareOptions() {
        Options options = new Options();
        Option input = new Option("i", "input", true, "Input invocation trace file path.");
        input.setRequired(true);
        options.addOption(input);
        Option output = new Option("t", "trace", true, "Output invocation trace file path.");
        output.setRequired(true);
        options.addOption(output);
        Option downscaleFactor = new Option("df", "downscaleFactor", true, "Downscale factor (i.e., pick 1 invocation in X, where X is the value of this option).");
        downscaleFactor.setRequired(true);
        options.addOption(downscaleFactor);
        return options;
    }
}
