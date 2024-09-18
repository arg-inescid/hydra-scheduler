package org.graalvm.argo.dataset.execution.mw.memory;

public class MemoryManagerFactories {

    public abstract static class AbstractMemoryManagerFactory {
        public abstract AbstractMemoryManager createMemoryManager();
    }

    public static class OwnerCollocationMemoryManagerFactory extends AbstractMemoryManagerFactory {
        @Override
        public AbstractMemoryManager createMemoryManager() {
            return new OwnerCollocationMemoryManager();
        }
    }

    public static class SingleFunctionMemoryManagerFactory extends AbstractMemoryManagerFactory {
        @Override
        public AbstractMemoryManager createMemoryManager() {
            return new SingleFunctionMemoryManager();
        }
    }

    public static class SingleInvocationMemoryManagerFactory extends AbstractMemoryManagerFactory {
        @Override
        public AbstractMemoryManager createMemoryManager() {
            return new SingleInvocationMemoryManager();
        }
    }
}
