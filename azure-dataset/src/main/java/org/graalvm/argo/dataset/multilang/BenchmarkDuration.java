package org.graalvm.argo.dataset.multilang;

import java.util.*;

public class BenchmarkDuration {

    private static final SplittableRandom random = new SplittableRandom();

    private static final Map<FunctionLanguage, TreeMap<Integer, List<String>>> benchmarks = new HashMap<>();

    static {
        benchmarks.put(FunctionLanguage.JAVA, new TreeMap<>());

        addBenchmark(FunctionLanguage.JAVA, "dn", 20);
        addBenchmark(FunctionLanguage.JAVA, "dh", 200);
        addBenchmark(FunctionLanguage.JAVA, "bf", 12);
        addBenchmark(FunctionLanguage.JAVA, "co", 100);
        addBenchmark(FunctionLanguage.JAVA, "ms", 7);
        addBenchmark(FunctionLanguage.JAVA, "pr", 8);
        addBenchmark(FunctionLanguage.JAVA, "up", 15);

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
