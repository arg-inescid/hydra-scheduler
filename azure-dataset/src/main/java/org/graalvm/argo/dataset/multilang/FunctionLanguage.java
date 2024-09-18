package org.graalvm.argo.dataset.multilang;

public enum FunctionLanguage {
    JAVA("java"),
    JAVASCRIPT("javascript"),
    PYTHON("python");

    private final String language;

    FunctionLanguage(String language) {
        this.language = language;
    }

    public static FunctionLanguage fromString(String text) {
        for (FunctionLanguage b : FunctionLanguage.values()) {
            if (b.language.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalStateException("Function language not found: " + text);
    }

    @Override
    public String toString() {
        return this.language;
    }
}
