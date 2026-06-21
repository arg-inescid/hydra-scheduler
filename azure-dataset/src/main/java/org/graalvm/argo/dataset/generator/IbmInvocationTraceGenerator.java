package org.graalvm.argo.dataset.generator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.graalvm.argo.dataset.Invocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generates per-invocation traces from converted IBM Cloud Code Engine weekly CSVs.
 *
 * Expected CSV schema:
 *  NamespaceHash,AppHash,AppContainerRequestMemory,AppExecTimes,InvocationTimes
 *
 * The converter writes AppContainerRequestMemory in GB. The generated trace stores memory in MB.
 */
public class IbmInvocationTraceGenerator extends AbstractInvocationTraceGenerator {

    private static final int MINUTES_PER_WEEK = 7 * 24 * 60;
    private static final int MS_PER_MINUTE = 60_000;

    private static final int OWNER_COLUMN = 0;
    private static final int FUNCTION_COLUMN = 1;
    private static final int MEMORY_GB_COLUMN = 2;
    private static final int DURATION_MS_COLUMN = 3;
    private static final int TIMESTAMP_MS_COLUMN = 4;

    public static void main(String[] args) throws Exception {
        new IbmInvocationTraceGenerator().run(args);
    }

    @Override
    protected void addSourceOptions(Options options) {
        Option week = new Option("w", "week", true, "Input IBM dataset week number (default: 1).");
        week.setRequired(false);
        options.addOption(week);

        Option startWeek = new Option(null, "startWeek", true, "Start IBM dataset week number.");
        startWeek.setRequired(false);
        options.addOption(startWeek);

        Option endWeek = new Option(null, "endWeek", true, "End IBM dataset week number.");
        endWeek.setRequired(false);
        options.addOption(endWeek);

        Option inputDir = new Option(null, "inputDir", true, "Input IBM dataset data directory.");
        inputDir.setRequired(false);
        options.addOption(inputDir);

        Option firstMinute = new Option("b", "bmin", true, "First minute to include from the weekly trace ([0,10079]).");
        firstMinute.setRequired(false);
        options.addOption(firstMinute);

        Option lastMinute = new Option("e", "emin", true, "Last minute to include from the weekly trace ([0,10079]).");
        lastMinute.setRequired(false);
        options.addOption(lastMinute);
    }

    @Override
    protected List<Invocation> loadInvocations(CommandLine cmd) throws IOException, ParseException {
        int firstMinute = Integer.parseInt(cmd.getOptionValue("bmin", "0"));
        int lastMinute = Integer.parseInt(cmd.getOptionValue("emin", "9"));
        validateMinuteRange(firstMinute, lastMinute);

        int singleWeek = parseWeek(cmd.getOptionValue("week", "1"));
        Integer startWeek = cmd.hasOption("startWeek") ? parseWeek(cmd.getOptionValue("startWeek")) : null;
        Integer endWeek = cmd.hasOption("endWeek") ? parseWeek(cmd.getOptionValue("endWeek")) : null;
        if (startWeek == null || endWeek == null) {
            startWeek = singleWeek;
            endWeek = singleWeek;
        }
        validateWeekRange(startWeek, endWeek);

        String inputDir = cmd.getOptionValue("inputDir", "input/ibm_cloud_code_engine/data");
        return loadInvocations(inputDir, startWeek, endWeek, firstMinute, lastMinute);
    }

    private int parseWeek(String weekStr) throws ParseException {
        try {
            return Integer.parseInt(weekStr);
        } catch (NumberFormatException nfe) {
            throw new ParseException("Invalid week value: " + weekStr);
        }
    }

    private void validateWeekRange(int startWeek, int endWeek) throws ParseException {
        if (startWeek < 1 || endWeek < startWeek) {
            throw new ParseException("Invalid week range: [" + startWeek + "," + endWeek + "]");
        }
    }

    private void validateMinuteRange(int firstMinute, int lastMinute) throws ParseException {
        if (firstMinute < 0 || lastMinute >= MINUTES_PER_WEEK || firstMinute > lastMinute) {
            throw new ParseException("Invalid minute range: [" + firstMinute + "," + lastMinute + "]");
        }
    }

    private List<Invocation> loadInvocations(String inputDir,
                                             int startWeek,
                                             int endWeek,
                                             int firstMinute,
                                             int lastMinute) throws IOException {
        List<Invocation> invocations = new ArrayList<>();
        for (int week = startWeek; week <= endWeek; week++) {
            File inputFile = new File(inputDir, "week_" + week + ".csv");
            if (!inputFile.exists()) {
                throw new IOException("Missing IBM CSV input file: " + inputFile);
            }
            processWeek(invocations, inputFile, week, firstMinute, lastMinute);
        }
        invocations.sort(Comparator.comparingInt(Invocation::getTimestamp));
        return invocations;
    }

    private void processWeek(List<Invocation> invocations,
                             File inputFile,
                             int week,
                             int firstMinute,
                             int lastMinute) throws IOException {
        int startTimestampMs = firstMinute * MS_PER_MINUTE;
        int endTimestampMs = ((lastMinute + 1) * MS_PER_MINUTE) - 1;
        int weekOffsetMs = (week - 1) * MINUTES_PER_WEEK * MS_PER_MINUTE;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String header = reader.readLine();
            validateHeader(header, inputFile);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(SOURCE_DELIMITER, -1);
                if (row.length != 5) {
                    continue;
                }

                int timestampMs = parseRoundedInt(row[TIMESTAMP_MS_COLUMN]);
                if (timestampMs < startTimestampMs) {
                    continue;
                }
                if (timestampMs > endTimestampMs) {
                    break;
                }

                int memoryMb = parseMemoryMb(row[MEMORY_GB_COLUMN]);
                int durationMs = parseRoundedInt(row[DURATION_MS_COLUMN]);
                if (memoryMb <= 0 || durationMs <= 0) {
                    continue;
                }

                invocations.add(new Invocation(
                        row[OWNER_COLUMN],
                        row[FUNCTION_COLUMN],
                        memoryMb,
                        durationMs,
                        weekOffsetMs + timestampMs));
            }
        }
    }

    private void validateHeader(String header, File inputFile) throws IOException {
        String expected = "NamespaceHash,AppHash,AppContainerRequestMemory,AppExecTimes,InvocationTimes";
        if (header == null || !expected.equals(header.trim())) {
            throw new IOException("Unexpected IBM CSV header in " + inputFile + ": " + header);
        }
    }

    private int parseMemoryMb(String value) {
        return (int) Math.round(Double.parseDouble(value) * 1024);
    }

    private int parseRoundedInt(String value) {
        return (int) Math.round(Double.parseDouble(value));
    }
}
