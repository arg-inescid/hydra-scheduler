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
            String functionRuntime = cmd.getOptionValue("functionRuntime");
            String invocationCollocation = cmd.getOptionValue("invocationCollocation");
            String functionIsolation = cmd.getOptionValue("functionIsolation");
            boolean debug = cmd.hasOption("debug");
            String lambdaManagerAddress = cmd.getOptionValue("lambdaManagerAddress", "localhost:30009");
            ExecutorConfiguration config = new ExecutorConfiguration(functionRuntime, invocationCollocation, functionIsolation, debug, lambdaManagerAddress);
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
        Option functionRuntime = new Option("fr", "functionRuntime", true, "Function runtime.");
        functionRuntime.setRequired(true);
        options.addOption(functionRuntime);
        Option invocationCollocation = new Option("ic", "invocationCollocation", true, "Collocation of invocations in a single worker.");
        invocationCollocation.setRequired(true);
        options.addOption(invocationCollocation);
        Option functionIsolation = new Option("fi", "functionIsolation", true, "Isolation of functions across several workers.");
        functionIsolation.setRequired(true);
        options.addOption(functionIsolation);
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
