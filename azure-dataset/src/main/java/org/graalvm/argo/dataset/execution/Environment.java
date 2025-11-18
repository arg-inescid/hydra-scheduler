package org.graalvm.argo.dataset.execution;

public class Environment {

    // General configuration.
    public final static int WORKER_COUNT = 100;
    public final static int MAX_MEMORY_PER_WORKER_MB = 106496;
    // Negate if you don't want to insert real worker.
    public final static int REAL_WORKER_INDEX = 6;
    public final static String REAL_WORKER_TRACE_OUTPUT = "/tmp/lse_trace.csv";
    // Statistics about used memory and #VMs in each worker (theoretic, calculated by memory managers).
    public final static boolean COLLECT_STATISTICS = false;
    public final static String GLOBAL_STATISTICS_OUTPUT = "/tmp/lse_statistics.json";
    public final static int STATISTICS_INTERVAL_MS = 1000;
    // Period in which we check if the scheduler keeps up with the trace timestamps.
    public static final long WAIT_PERIOD_MS = 10;

    // Fake worker configuration.
    public static final String FAKE_WORKER_HOST = "localhost";
    public final static int FAKE_WORKER_FIRST_PORT = 50010;

    public static final int VM_MEMORY = 1024;

    private static final String ARGO_HOME_PATH = System.getenv("ARGO_HOME");
    private static final String HOST_ADDRESS = "http://172.18.0.1:8000";
    private static final String URL_SNAP = "{\"url\":\"" + HOST_ADDRESS + "/snap.png\"}";

    public static final String HYDRA_RUNTIME = "hydra";
    public static final String GRAALOS_RUNTIME = "graalos";

    // Hydra benchmarks.
    // Python
    public static final String GV_PY_HELLOWORLD_CODE = ARGO_HOME_PATH + "/../benchmarks/src/python/gv-hello-world/build/libhelloworld.so";
    public static final String GV_PY_HELLOWORLD_ENTRYPOINT = "com.helloworld.HelloWorld";
    public static final String GV_PY_HELLOWORLD_SVMID = "4";
    public static final String GV_PY_UPLOADER_CODE = ARGO_HOME_PATH + "/../benchmarks/src/python/gv-uploader/build/libuploader.so";
    public static final String GV_PY_UPLOADER_ENTRYPOINT = "com.uploader.Uploader";
    public static final String GV_PY_UPLOADER_SVMID = "5";
    public static final String GV_PY_COMPRESSION_CODE = ARGO_HOME_PATH + "/../benchmarks/src/python/gv-compression/build/libcompression.so";
    public static final String GV_PY_COMPRESSION_ENTRYPOINT = "com.compression.Compression";
    public static final String GV_PY_COMPRESSION_SVMID = "6";
    public static final String GV_PY_SANDBOX = "context-snapshot";
    // JavaScript
    public static final String GV_JS_HELLOWORLD_CODE = ARGO_HOME_PATH + "/../benchmarks/src/javascript/gv-hello-world/build/libhelloworld.so";
    public static final String GV_JS_HELLOWORLD_ENTRYPOINT = "com.helloworld.HelloWorld";
    public static final String GV_JS_HELLOWORLD_SVMID = "1";
    public static final String GV_JS_UPLOADER_CODE = ARGO_HOME_PATH + "/../benchmarks/src/javascript/gv-uploader/build/libuploader.so";
    public static final String GV_JS_UPLOADER_ENTRYPOINT = "com.uploader.Uploader";
    public static final String GV_JS_UPLOADER_SVMID = "2";
    public static final String GV_JS_DYNAMICHTML_CODE = ARGO_HOME_PATH + "/../benchmarks/src/javascript/gv-dynamic-html/build/libdynamichtml.so";
    public static final String GV_JS_DYNAMICHTML_ENTRYPOINT = "com.dynamichtml.DynamicHTML";
    public static final String GV_JS_DYNAMICHTML_SVMID = "3";
    public static final String GV_JS_SANDBOX = "context-snapshot";
    // Java
    public static final String GV_JV_HELLOWORLD_CODE = ARGO_HOME_PATH + "/../benchmarks/src/java/gv-hello-world/build/libhelloworld.so";
    public static final String GV_JV_HELLOWORLD_ENTRYPOINT = "com.hello_world.HelloWorld";
    public static final String GV_JV_FILEHASHING_CODE = ARGO_HOME_PATH + "/../benchmarks/src/java/gv-file-hashing/build/libfilehashing.so";
    public static final String GV_JV_FILEHASHING_ENTRYPOINT = "com.filehashing.FileHashing";
    public static final String GV_JV_HTTPREQUEST_CODE = ARGO_HOME_PATH + "/../benchmarks/src/java/gv-httprequest/build/libhttprequest.so";
    public static final String GV_JV_HTTPREQUEST_ENTRYPOINT = "com.httprequest.HttpRequest";
    public static final String GV_JV_SANDBOX = "isolate";

    // OpenWhisk benchmarks.
    // Python
    public static final String OW_PY_HELLOWORLD_CODE = ARGO_HOME_PATH + "/../benchmarks/src/python/cr-hello-world/init.json";
    public static final String OW_PY_HELLOWORLD_ENTRYPOINT = "main";
    public static final String OW_PY_UPLOADER_CODE = ARGO_HOME_PATH + "/../benchmarks/src/python/cr-uploader/init.json";
    public static final String OW_PY_UPLOADER_ENTRYPOINT = "main";
    public static final String OW_PY_COMPRESSION_CODE = ARGO_HOME_PATH + "/../benchmarks/src/python/cr-compression/init.json";
    public static final String OW_PY_COMPRESSION_ENTRYPOINT = "main";
    // JavaScript
    public static final String OW_JS_HELLOWORLD_CODE = ARGO_HOME_PATH + "/../benchmarks/src/javascript/cr-hello-world/init.json";
    public static final String OW_JS_HELLOWORLD_ENTRYPOINT = "main";
    public static final String OW_JS_UPLOADER_CODE = ARGO_HOME_PATH + "/../benchmarks/src/javascript/cr-uploader/init.json";
    public static final String OW_JS_UPLOADER_ENTRYPOINT = "main";
    public static final String OW_JS_DYNAMICHTML_CODE = ARGO_HOME_PATH + "/../benchmarks/src/javascript/cr-dynamic-html/init.json";
    public static final String OW_JS_DYNAMICHTML_ENTRYPOINT = "main";
    // Java
    public static final String OW_JV_HELLOWORLD_CODE = ARGO_HOME_PATH + "/../benchmarks/src/java/cr-hello-world/init.json";
    public static final String OW_JV_HELLOWORLD_ENTRYPOINT = "Hello";
    public static final String OW_JV_FILEHASHING_CODE = ARGO_HOME_PATH + "/../benchmarks/src/java/cr-file-hashing/init.json";
    public static final String OW_JV_FILEHASHING_ENTRYPOINT = "Main";
    public static final String OW_JV_HTTPREQUEST_CODE = ARGO_HOME_PATH + "/../benchmarks/src/java/cr-httprequest/init.json";
    public static final String OW_JV_HTTPREQUEST_ENTRYPOINT = "Main";

    // Invocation parameters for benchmarks.
    // Python
    public static final String PY_HELLOWORLD_PARAMETERS = "{ }";
    public static final String PY_UPLOADER_PARAMETERS = URL_SNAP;
    public static final String PY_COMPRESSION_PARAMETERS = "{\"url\":\"" + HOST_ADDRESS + "/video.mp4\"}";
    // JavaScript
    public static final String JS_HELLOWORLD_PARAMETERS = "{ }";
    public static final String JS_UPLOADER_PARAMETERS = URL_SNAP;
    public static final String JS_DYNAMICHTML_PARAMETERS = "{\"url\":\"" + HOST_ADDRESS + "/template.html\",\"username\":\"rbruno\",\"nsize\":\"10\"}";
    // Java
    public static final String JV_HELLOWORLD_PARAMETERS = "{\"name\":\"rbruno\"}";
    public static final String JV_FILEHASHING_PARAMETERS = URL_SNAP;
    public static final String JV_HTTPREQUEST_PARAMETERS = URL_SNAP;

    // Benchmark function names.
    // Python
    public static final String PY_HELLOWORLD_NAME = "pyhw";
    public static final String PY_UPLOADER_NAME = "pyup";
    public static final String PY_COMPRESSION_NAME = "pyco";
    // JavaScript
    public static final String JS_HELLOWORLD_NAME = "jshw";
    public static final String JS_UPLOADER_NAME = "jsup";
    public static final String JS_DYNAMICHTML_NAME = "jsdh";
    // Java
    public static final String JV_HELLOWORLD_NAME = "jvhw";
    public static final String JV_FILEHASHING_NAME = "jvfh";
    public static final String JV_HTTPREQUEST_NAME = "jvhr";

    // Function sizes in MB (only matters for collocatable Hydra).
    // Python (NOTE: should be 512, but we simulate it to be of the same size)
    public static final int PY_HELLOWORLD_MEMORY = 256;
    public static final int PY_UPLOADER_MEMORY = 256;
    public static final int PY_COMPRESSION_MEMORY = 256;
    // JavaScript
    public static final int JS_HELLOWORLD_MEMORY = 256;
    public static final int JS_UPLOADER_MEMORY = 256;
    public static final int JS_DYNAMICHTML_MEMORY = 256;
    // Java
    public static final int JV_HELLOWORLD_MEMORY = 256;
    public static final int JV_FILEHASHING_MEMORY = 256;
    public static final int JV_HTTPREQUEST_MEMORY = 256;

    // Legacy GenericApp configuration.
    public static final String GV_JV_GENERICAPP_CODE = ARGO_HOME_PATH + "/../benchmarks/src/java/gv-genericapp/build/libgenericapp.so";
    public static final String GV_JV_GENERICAPP_ENTRYPOINT = "com.genericapp.GenericApp";
    public static final String GV_JV_GENERICAPP_PARAMETERS_TEMPLATE = "{\"memory\":\"%d\",\"duration\":\"%d\"}";
}
