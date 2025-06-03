package org.graalvm.argo.dataset.multilang;

import java.util.*;

public class BenchmarkDuration {

    private static final SplittableRandom random = new SplittableRandom();

    private static final Map<FunctionLanguage, TreeMap<Integer, List<String>>> benchmarks = new HashMap<>();

    static {
        benchmarks.put(FunctionLanguage.JAVA, new TreeMap<>());
        benchmarks.put(FunctionLanguage.JAVASCRIPT, new TreeMap<>());
        benchmarks.put(FunctionLanguage.PYTHON, new TreeMap<>());

        addBenchmark(FunctionLanguage.JAVA, "jvhw", 5);
        addBenchmark(FunctionLanguage.JAVA, "jvfh", 8);
        addBenchmark(FunctionLanguage.JAVA, "jvcl", 4430);
        addBenchmark(FunctionLanguage.JAVA, "jvhr", 7);
        addBenchmark(FunctionLanguage.JAVA, "jvvp", 25175);

        addBenchmark(FunctionLanguage.PYTHON, "pyhw", 5);
        addBenchmark(FunctionLanguage.PYTHON, "pyms", 12);
        addBenchmark(FunctionLanguage.PYTHON, "pybf", 10);
        addBenchmark(FunctionLanguage.PYTHON, "pypr", 12);
        addBenchmark(FunctionLanguage.PYTHON, "pydn", 32);
        addBenchmark(FunctionLanguage.PYTHON, "pydh", 14);
        addBenchmark(FunctionLanguage.PYTHON, "pyco", 61);
        addBenchmark(FunctionLanguage.PYTHON, "pyth", 21);
        addBenchmark(FunctionLanguage.PYTHON, "pyvp", 3052);
        addBenchmark(FunctionLanguage.PYTHON, "pyup", 17);

        addBenchmark(FunctionLanguage.JAVASCRIPT, "jshw", 5);
        addBenchmark(FunctionLanguage.JAVASCRIPT, "jsdh", 5);
        addBenchmark(FunctionLanguage.JAVASCRIPT, "jsth", 25);
        addBenchmark(FunctionLanguage.JAVASCRIPT, "jsup", 10);
    }

    private static void addBenchmark(FunctionLanguage language, String name, int duration) {
        benchmarks.get(language).computeIfAbsent(duration, k -> new ArrayList<>()).add(name);
    }

    static String getBenchmark(FunctionLanguage language, int functionDuration) {
        // Find the closest duration keys.
        Integer floor = benchmarks.get(language).floorKey(functionDuration);
        Integer ceiling = benchmarks.get(language).ceilingKey(functionDuration);

        // Determine the closest duration.
        Integer closestDuration = (floor == null) ? ceiling :
                                  (ceiling == null) ? floor :
                                          // Defaults to floor if the distance between the floor and the ceiling is equal. Change <= to < to favor ceiling.
                                          (Math.abs(functionDuration - floor) <= Math.abs(functionDuration - ceiling) ? floor : ceiling);

        if (closestDuration == null) {
            throw new IllegalArgumentException("No valid benchmark found for the specified duration: " + functionDuration);
        }

        // Retrieve associated benchmark names and pick randomly.
        List<String> candidates = benchmarks.get(language).get(closestDuration);
        return candidates.get(random.nextInt(candidates.size()));
    }
}
