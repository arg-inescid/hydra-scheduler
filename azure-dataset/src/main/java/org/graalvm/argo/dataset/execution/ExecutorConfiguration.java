package org.graalvm.argo.dataset.execution;

import org.graalvm.argo.dataset.multilang.FunctionLanguage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ExecutorConfiguration {

    final String functionRuntime;
    public final String invocationCollocation;
    public final String functionIsolation;
    /**
     * If true, then print timestamps instead of issuing requests.
     */
    private final boolean debug;
    private final String lambdaManagerAddress;

    private final Map<FunctionLanguage, FunctionConfiguration[]> functionConfigs;

    ExecutorConfiguration(String functionRuntime, String invocationCollocation, String functionIsolation, boolean debug, String lambdaManagerAddress) {
        this.functionRuntime = functionRuntime;
        this.invocationCollocation = invocationCollocation;
        this.functionIsolation = functionIsolation;
        this.debug = debug;
        this.lambdaManagerAddress = lambdaManagerAddress;
        this.functionConfigs = new HashMap<>();
        initFunctionConfigs();
    }

    private void initFunctionConfigs() {
        FunctionConfiguration[] javaConfigs;
        FunctionConfiguration[] javaScriptConfigs;
        FunctionConfiguration[] pythonConfigs;
        if (Environment.GRAALVISOR_RUNTIME.equals(this.functionRuntime)) {
            // Add function configs for Graalvisor.
            javaConfigs = new FunctionConfiguration[] {
                    new FunctionConfiguration(Environment.JV_HELLOWORLD_NAME, Environment.GV_JV_HELLOWORLD_CODE, Environment.GV_JV_HELLOWORLD_ENTRYPOINT, Environment.JV_HELLOWORLD_PARAMETERS, Environment.JV_HELLOWORLD_MEMORY, Environment.JV_HELLOWORLD_DURATION, Environment.GV_JV_SANDBOX),
                    new FunctionConfiguration(Environment.JV_FILEHASHING_NAME, Environment.GV_JV_FILEHASHING_CODE, Environment.GV_JV_FILEHASHING_ENTRYPOINT, Environment.JV_FILEHASHING_PARAMETERS, Environment.JV_FILEHASHING_MEMORY, Environment.JV_FILEHASHING_DURATION, Environment.GV_JV_SANDBOX),
                    new FunctionConfiguration(Environment.JV_HTTPREQUEST_NAME, Environment.GV_JV_HTTPREQUEST_CODE, Environment.GV_JV_HTTPREQUEST_ENTRYPOINT, Environment.JV_HTTPREQUEST_PARAMETERS, Environment.JV_HTTPREQUEST_MEMORY, Environment.JV_HTTPREQUEST_DURATION, Environment.GV_JV_SANDBOX)
            };
            javaScriptConfigs = new FunctionConfiguration[] {
                    new FunctionConfiguration(Environment.JS_HELLOWORLD_NAME, Environment.GV_JS_HELLOWORLD_CODE, Environment.GV_JS_HELLOWORLD_ENTRYPOINT, Environment.JS_HELLOWORLD_PARAMETERS, Environment.JS_HELLOWORLD_MEMORY, Environment.JS_HELLOWORLD_DURATION, Environment.GV_JS_SANDBOX, Environment.GV_JS_HELLOWORLD_SVMID),
                    new FunctionConfiguration(Environment.JS_UPLOADER_NAME, Environment.GV_JS_UPLOADER_CODE, Environment.GV_JS_UPLOADER_ENTRYPOINT, Environment.JS_UPLOADER_PARAMETERS, Environment.JS_UPLOADER_MEMORY, Environment.JS_UPLOADER_DURATION, Environment.GV_JS_SANDBOX, Environment.GV_JS_UPLOADER_SVMID),
                    new FunctionConfiguration(Environment.JS_DYNAMICHTML_NAME, Environment.GV_JS_DYNAMICHTML_CODE, Environment.GV_JS_DYNAMICHTML_ENTRYPOINT, Environment.JS_DYNAMICHTML_PARAMETERS, Environment.JS_DYNAMICHTML_MEMORY, Environment.JS_DYNAMICHTML_DURATION, Environment.GV_JS_SANDBOX, Environment.GV_JS_DYNAMICHTML_SVMID)
            };
            pythonConfigs = new FunctionConfiguration[] {
                    new FunctionConfiguration(Environment.PY_HELLOWORLD_NAME, Environment.GV_PY_HELLOWORLD_CODE, Environment.GV_PY_HELLOWORLD_ENTRYPOINT, Environment.PY_HELLOWORLD_PARAMETERS, Environment.PY_HELLOWORLD_MEMORY, Environment.PY_HELLOWORLD_DURATION, Environment.GV_PY_SANDBOX, Environment.GV_PY_HELLOWORLD_SVMID),
                    new FunctionConfiguration(Environment.PY_UPLOADER_NAME, Environment.GV_PY_UPLOADER_CODE, Environment.GV_PY_UPLOADER_ENTRYPOINT, Environment.PY_UPLOADER_PARAMETERS, Environment.PY_UPLOADER_MEMORY, Environment.PY_UPLOADER_DURATION, Environment.GV_PY_SANDBOX, Environment.GV_PY_UPLOADER_SVMID),
                    new FunctionConfiguration(Environment.PY_COMPRESSION_NAME, Environment.GV_PY_COMPRESSION_CODE, Environment.GV_PY_COMPRESSION_ENTRYPOINT, Environment.PY_COMPRESSION_PARAMETERS, Environment.PY_COMPRESSION_MEMORY, Environment.PY_COMPRESSION_DURATION, Environment.GV_PY_SANDBOX, Environment.GV_PY_COMPRESSION_SVMID)
            };
        } else if (Environment.GRAALOS_RUNTIME.equals(this.functionRuntime)) {
            javaConfigs = new FunctionConfiguration[]{
                    // Function code location, entrypoint, and parameters don't matter at this moment.
                    new FunctionConfiguration(Environment.JV_HELLOWORLD_NAME, Environment.GV_JV_HELLOWORLD_CODE, Environment.GV_JV_HELLOWORLD_ENTRYPOINT, Environment.JV_HELLOWORLD_PARAMETERS, Environment.JV_HELLOWORLD_MEMORY, Environment.JV_HELLOWORLD_DURATION)
            };
            javaScriptConfigs = null;
            pythonConfigs = null;
        } else {
            // Add function configs for OpenWhisk.
            javaConfigs = new FunctionConfiguration[] {
                    new FunctionConfiguration(Environment.JV_HELLOWORLD_NAME, Environment.OW_JV_HELLOWORLD_CODE, Environment.OW_JV_HELLOWORLD_ENTRYPOINT, Environment.JV_HELLOWORLD_PARAMETERS, Environment.JV_HELLOWORLD_MEMORY, Environment.JV_HELLOWORLD_DURATION),
                    new FunctionConfiguration(Environment.JV_FILEHASHING_NAME, Environment.OW_JV_FILEHASHING_CODE, Environment.OW_JV_FILEHASHING_ENTRYPOINT, Environment.JV_FILEHASHING_PARAMETERS, Environment.JV_FILEHASHING_MEMORY, Environment.JV_FILEHASHING_DURATION),
                    new FunctionConfiguration(Environment.JV_HTTPREQUEST_NAME, Environment.OW_JV_HTTPREQUEST_CODE, Environment.OW_JV_HTTPREQUEST_ENTRYPOINT, Environment.JV_HTTPREQUEST_PARAMETERS, Environment.JV_HTTPREQUEST_MEMORY, Environment.JV_HTTPREQUEST_DURATION)
            };
            javaScriptConfigs = new FunctionConfiguration[] {
                    new FunctionConfiguration(Environment.JS_HELLOWORLD_NAME, Environment.OW_JS_HELLOWORLD_CODE, Environment.OW_JS_HELLOWORLD_ENTRYPOINT, Environment.JS_HELLOWORLD_PARAMETERS, Environment.JS_HELLOWORLD_MEMORY, Environment.JS_HELLOWORLD_DURATION),
                    new FunctionConfiguration(Environment.JS_UPLOADER_NAME, Environment.OW_JS_UPLOADER_CODE, Environment.OW_JS_UPLOADER_ENTRYPOINT, Environment.JS_UPLOADER_PARAMETERS, Environment.JS_UPLOADER_MEMORY, Environment.JS_UPLOADER_DURATION),
                    new FunctionConfiguration(Environment.JS_DYNAMICHTML_NAME, Environment.OW_JS_DYNAMICHTML_CODE, Environment.OW_JS_DYNAMICHTML_ENTRYPOINT, Environment.JS_DYNAMICHTML_PARAMETERS, Environment.JS_DYNAMICHTML_MEMORY, Environment.JS_DYNAMICHTML_DURATION)
            };
            pythonConfigs = new FunctionConfiguration[] {
                    new FunctionConfiguration(Environment.PY_HELLOWORLD_NAME, Environment.OW_PY_HELLOWORLD_CODE, Environment.OW_PY_HELLOWORLD_ENTRYPOINT, Environment.PY_HELLOWORLD_PARAMETERS, Environment.PY_HELLOWORLD_MEMORY, Environment.PY_HELLOWORLD_DURATION),
                    new FunctionConfiguration(Environment.PY_UPLOADER_NAME, Environment.OW_PY_UPLOADER_CODE, Environment.OW_PY_UPLOADER_ENTRYPOINT, Environment.PY_UPLOADER_PARAMETERS, Environment.PY_UPLOADER_MEMORY, Environment.PY_UPLOADER_DURATION),
                    new FunctionConfiguration(Environment.PY_COMPRESSION_NAME, Environment.OW_PY_COMPRESSION_CODE, Environment.OW_PY_COMPRESSION_ENTRYPOINT, Environment.PY_COMPRESSION_PARAMETERS, Environment.PY_COMPRESSION_MEMORY, Environment.PY_COMPRESSION_DURATION)
            };
        }
        functionConfigs.put(FunctionLanguage.JAVA, javaConfigs);
        functionConfigs.put(FunctionLanguage.JAVASCRIPT, javaScriptConfigs);
        functionConfigs.put(FunctionLanguage.PYTHON, pythonConfigs);
    }

    public boolean isDebugMode() {
        return debug;
    }

    public String getLambdaManagerAddress() {
        return lambdaManagerAddress;
    }

    public FunctionConfiguration getFunctionConfiguration(FunctionLanguage language, int functionId) {
        return functionConfigs.get(language)[functionId];
    }

    public class FunctionConfiguration {
        public final String functionName;
        // Contains path to the code instead of the code itself due to LocalFunctionStorage in Lambda Manager.
        final String code;
        final String entryPoint;
        final String payload;
        public final int memory;
        public final int duration;
        final String gvSandbox;
        final String svmId;

        private FunctionConfiguration(String functionName, String code, String entryPoint, String payload, int memory, int duration) {
            this(functionName, code, entryPoint, payload, memory, duration, null, null);
        }

        private FunctionConfiguration(String functionName, String code, String entryPoint, String payload, int memory, int duration, String gvSandbox) {
            this(functionName, code, entryPoint, payload, memory, duration, gvSandbox, null);
        }

        private FunctionConfiguration(String functionName, String code, String entryPoint, String payload, int memory, int duration, String gvSandbox, String svmId) {
            this.functionName = functionName;
            this.code = code;
            this.entryPoint = entryPoint;
            this.payload = payload;
            this.memory = memory;
            this.duration = duration;
            this.gvSandbox = gvSandbox;
            this.svmId = svmId;
        }
    }
}
