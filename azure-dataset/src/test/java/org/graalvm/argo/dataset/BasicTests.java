package org.graalvm.argo.dataset;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BasicTests {

    private static InvocationTraceSimulator simulator;

    @BeforeAll
    static void init() {
        simulator = new InvocationTraceSimulator();
    }

    private static void printOutput(List<OutputEntry> output) {
        for (OutputEntry entry : output) {
            System.out.println(entry);
        }
    }

    @Test
    void singleInvocation() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        // Ownser, Function, Memory, Duration, Timestamp.
        invocations.add(new Invocation("owner1", "function1", 128, 50, 100));
        List<OutputEntry> output = simulator.simulateInvocations(invocations, 600000, 1000);
        assertNotNull(output);
        printOutput(output);
        assert(output.size() == 1);
        OutputEntry entry = output.get(0);
        assert(entry.timestamp == 100);
        assert(entry.runningInvocations == 1);
        assert(entry.runningUsers == 1);
        assert(entry.runningFunctions == 1);
        assert(entry.runningInvocationsFootprint == 128);
        assert(entry.cachedFunctions == 0);
        assert(entry.cachedUsers == 0);
        assert(entry.cachedInvocationsFootprint == 0);
        assert(entry.coldStarts == 1);
        assert(entry.invocationsProcessed == 1);
    }

    @Test
    void twoInvocationsSameUserSameFunction() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        invocations.add(new Invocation("owner1", "function1", 128, 50, 100));
        invocations.add(new Invocation("owner1", "function1", 128, 50, 125));
        List<OutputEntry> output = simulator.simulateInvocations(invocations, 600000, 1000);
        assertNotNull(output);
        printOutput(output);
        assert(output.size() == 1);

        OutputEntry firstentry = output.get(0);
        assert(firstentry.timestamp == 125);
        assert(firstentry.runningInvocations == 2);
        assert(firstentry.runningUsers == 1);
        assert(firstentry.runningFunctions == 1);
        assert(firstentry.runningInvocationsFootprint == 256);
        assert(firstentry.cachedFunctions == 0);
        assert(firstentry.cachedUsers == 0);
        assert(firstentry.cachedInvocationsFootprint == 0);
        assert(firstentry.coldStarts == 2);
        assert(firstentry.invocationsProcessed == 2);
    }


    @Test
    void twoInvocationsSameUserDifferentFunctions() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        invocations.add(new Invocation("owner1", "function1", 128, 50, 100));
        invocations.add(new Invocation("owner1", "function2", 128, 50, 125));
        List<OutputEntry> output = simulator.simulateInvocations(invocations, 600000, 1000);
        assertNotNull(output);
        printOutput(output);
        assert(output.size() == 1);

        OutputEntry firstentry = output.get(0);
        assert(firstentry.timestamp == 125);
        assert(firstentry.runningInvocations == 2);
        assert(firstentry.runningUsers == 1);
        assert(firstentry.runningFunctions == 2);
        assert(firstentry.runningInvocationsFootprint == 256);
        assert(firstentry.cachedFunctions == 0);
        assert(firstentry.cachedUsers == 0);
        assert(firstentry.cachedInvocationsFootprint == 0);
        assert(firstentry.coldStarts == 2);
        assert(firstentry.invocationsProcessed == 2);
    }

    @Test
    void twoInvocationsDifferentUser() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        invocations.add(new Invocation("owner1", "function1", 128, 50, 100));
        invocations.add(new Invocation("owner2", "function2", 128, 50, 125));
        List<OutputEntry> output = simulator.simulateInvocations(invocations, 600000, 1000);
        assertNotNull(output);
        printOutput(output);
        assert(output.size() == 1);

        OutputEntry firstentry = output.get(0);
        assert(firstentry.timestamp == 125);
        assert(firstentry.runningInvocations == 2);
        assert(firstentry.runningUsers == 2);
        assert(firstentry.runningFunctions == 2);
        assert(firstentry.runningInvocationsFootprint == 256);
        assert(firstentry.cachedFunctions == 0);
        assert(firstentry.cachedUsers == 0);
        assert(firstentry.cachedInvocationsFootprint == 0);
        assert(firstentry.coldStarts == 2);
        assert(firstentry.invocationsProcessed == 2);
    }

    @Test
    void twoInvocationsColdWarm() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        invocations.add(new Invocation("owner1", "function1", 128, 50, 100));
        invocations.add(new Invocation("owner1", "function1", 128, 50, 200));
        List<OutputEntry> output = simulator.simulateInvocations(invocations, 600000, 1000);
        assertNotNull(output);
        printOutput(output);
        assert(output.size() == 1);

        OutputEntry firstentry = output.get(0);
        assert(firstentry.timestamp == 200);
        assert(firstentry.runningInvocations == 1);
        assert(firstentry.runningUsers == 1);
        assert(firstentry.runningFunctions == 1);
        assert(firstentry.runningInvocationsFootprint == 128);
        assert(firstentry.cachedFunctions == 0);
        assert(firstentry.cachedUsers == 0);
        assert(firstentry.cachedInvocationsFootprint == 0);
        assert(firstentry.coldStarts == 1);
        assert(firstentry.invocationsProcessed == 2);
    }

    @Test
    void twoInvocationsCached() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        invocations.add(new Invocation("owner1", "function1", 128, 50, 100));
        invocations.add(new Invocation("owner1", "function2", 128, 50, 200));
        List<OutputEntry> output = simulator.simulateInvocations(invocations, 600000, 1000);
        assertNotNull(output);
        printOutput(output);
        assert(output.size() == 1);

        OutputEntry firstentry = output.get(0);
        assert(firstentry.timestamp == 200);
        assert(firstentry.runningInvocations == 1);
        assert(firstentry.runningUsers == 1);
        assert(firstentry.runningFunctions == 1);
        assert(firstentry.runningInvocationsFootprint == 128);
        assert(firstentry.cachedFunctions == 1);
        assert(firstentry.cachedUsers == 0);
        assert(firstentry.cachedInvocationsFootprint == 128);
        assert(firstentry.coldStarts == 2);
        assert(firstentry.invocationsProcessed == 2);
    }

    @Test
    void threeInvocationsCachedWarm() throws Exception {
        List<Invocation> invocations = new ArrayList<>();
        invocations.add(new Invocation("owner1", "function1", 128, 50, 100));
        invocations.add(new Invocation("owner1", "function2", 128, 50, 200));
        invocations.add(new Invocation("owner1", "function1", 128, 50, 300));
        List<OutputEntry> output = simulator.simulateInvocations(invocations, 600000, 1000);
        assertNotNull(output);
        printOutput(output);
        assert(output.size() == 1);

        OutputEntry firstentry = output.get(0);
        assert(firstentry.timestamp == 300);
        assert(firstentry.runningInvocations == 1);
        assert(firstentry.runningUsers == 1);
        assert(firstentry.runningFunctions == 1);
        assert(firstentry.runningInvocationsFootprint == 128);
        assert(firstentry.cachedFunctions == 1);
        assert(firstentry.cachedUsers == 0);
        assert(firstentry.cachedInvocationsFootprint == 128);
        assert(firstentry.coldStarts == 2);
        assert(firstentry.invocationsProcessed == 3);
    }

    // TODO - add a test for keepalive, check that we kick functions out of the cache.
}
