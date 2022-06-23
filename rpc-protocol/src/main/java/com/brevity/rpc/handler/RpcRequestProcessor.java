package com.brevity.rpc.handler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 业务线程池
 */
public class RpcRequestProcessor {

    private static ThreadPoolExecutor threadPoolExecutor;

    private static int corePoolSize = 100;
    private static int maxPoolSize = 100;
    private static int queueSize = 500;

    public static void init(int corePoolSize, int maxPoolSize, int queueSize) {
        RpcRequestProcessor.corePoolSize = corePoolSize;
        RpcRequestProcessor.maxPoolSize = maxPoolSize;
        RpcRequestProcessor.queueSize = queueSize;
    }

    public static void submitRequest(Runnable task) {
        if (threadPoolExecutor == null) {
            synchronized (RpcRequestProcessor.class) {
                if (threadPoolExecutor == null) {
                    threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L,
                            TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSize));
                }
            }
        }
        threadPoolExecutor.submit(task);
    }

    public static void close() {
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();
        }
    }
}
