package org.graalvm.argo.dataset.execution;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.graalvm.argo.dataset.execution.mw.MultiWorkerInvocationTraceExecutor;

public class ExecutorEntryPoint {

    public static void main(String[] args) {
        Options options = prepareOptions();
        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            String inputFilePath = cmd.getOptionValue("input");
            String executionMode = cmd.getOptionValue("executionMode");
            boolean debug = cmd.hasOption("debug");
            String lambdaManagerAddress = cmd.getOptionValue("lambdaManagerAddress", "localhost:30009");
            ExecutorConfiguration config = new ExecutorConfiguration(executionMode, debug, lambdaManagerAddress);
            boolean multiWorker = cmd.hasOption("multiWorker");
            InvocationTraceExecutor executor = multiWorker ? new MultiWorkerInvocationTraceExecutor(config) : new InvocationTraceExecutor(config);
            executor.execute(inputFilePath);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", options);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Options prepareOptions() {
        Options options = new Options();
        Option input = new Option("i", "input", true, "Input invocation trace file path.");
        input.setRequired(true);
        options.addOption(input);
        Option executionMode = new Option("m", "executionMode", true, "Execution mode.");
        executionMode.setRequired(true);
        options.addOption(executionMode);
        Option debug = new Option("d", "debug", false, "Just print requests instead of sending them.");
        debug.setRequired(false);
        options.addOption(debug);
        Option lambdaManagerAddress = new Option("lm", "lambdaManagerAddress", true, "Full address of the lambda manager.");
        lambdaManagerAddress.setRequired(false);
        options.addOption(lambdaManagerAddress);
        Option multiWorker = new Option("mw", "multiWorker", false, "Experimental support for top-level simulating scheduler.");
        multiWorker.setRequired(false);
        options.addOption(multiWorker);
        return options;
    }

}
