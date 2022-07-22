package org.example.longpulling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.BigObject;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 模拟客户端拉取配置时的长轮询操作：
 * - getConfig：客户端拉取服务端的配置，服务端开启长轮询，使用AsyncContext进行异步阻塞。
 * - publishConfig：当服务端有配置被改变时，通过某种机制（例如：由redis的订阅发布模式）来进行通知，最终执行publishConfig方法，将服务器的配置推送给客户端，其本质实现的是http推拉结合的配置通知。
 */
@RestController
@RequestMapping("/config")
@Slf4j
public class ConfigController {

    // 定时任务，阻塞的最大超时时间。
    private ScheduledExecutorService timeoutChecker = new ScheduledThreadPoolExecutor(1, threadFactory);

    private static final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("longPolling-timeout-checker-%d").build();

    // 缓存 长轮询请求时的 AsyncContext，目的是 在 配置变化时能够获取到该对象，从而输出响应给客户端
    private static final Multimap<String, AsyncTask> dataIdContext = Multimaps.synchronizedSetMultimap(HashMultimap.create());

    @Resource
    private ObjectMapper objectMapper;

    private Map<String, Object> leap = new ConcurrentHashMap<>();

    @GetMapping("/cpu")
    public String testCpu() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000000; i++) {
                    String key = Math.random() + i + "";
                    BigObject bigObject = new BigObject();
                    bigObject.setId((long) i);
                    bigObject.setName(key);

                    byte[] bytes = new byte[1024];
                    bigObject.setContent(bytes);
                    for (int j = 0; j < 1023; j++) {
                        bytes[j] = (byte) ThreadLocalRandom.current().nextInt(250);
                    }

                    leap.put(key, bigObject);
                }
            }
        });

        thread.setName("cpu-test-thread");
        thread.setPriority(1);
        thread.start();

        return "OK";
    }

    /**
     * 长轮询阻塞客户端请求
     */
    @GetMapping("/get")
    @Async
    public void getConfig(HttpServletRequest request, HttpServletResponse response) {
        String serviceName = request.getParameter("serviceName");

        // 开启异步：startAsync()会直接利用原有的请求与响应对象来创建AsyncContext，startAsync(ServletRequest request,ServletResponse response)可以传入自行创建的请求、响应封装对象；
        // startAsync() 开始异步阻塞客户端连接，servlet线程在执行完方法块代码后即被回收（此时客户端请求是被阻塞的）
        AsyncContext asyncContext = request.startAsync(request, response);
        AsyncTask asyncTask = new AsyncTask(asyncContext, true);

        // 维护 serviceName 和异步请求上下文的关联
        dataIdContext.put(serviceName, asyncTask);

        // 启动定时器，30s 后写入 304 响应
        timeoutChecker.schedule(() -> {
            //触发定时后，判断任务是否被执行，即isTimeout为true（没有被执行）
            //则返回客户端304的状态码-即无修改。
            if (asyncTask.isTimeout()) {
                //清除缓存中的任务
                if (dataIdContext.remove(serviceName, asyncTask)) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    // 调用 AsyncContext.complete() 来解除对客户端请求的阻塞
                    asyncTask.getAsyncContext().complete();
                }
            }
        }, 30, TimeUnit.SECONDS);
    }

    /**
     * 模拟有配置变更后，将配置响应给客户端
     * 这里的响应对象 是 长轮询 请求对应的响应对象
     *
     * @param serviceName
     */
    @GetMapping("/publish")
    public void publishConfig(String serviceName) throws IOException {

        // 模拟配置变化(这里直接new了一个配置，表示有配置变化了)
        ConfigEntity configEntity = new ConfigEntity(serviceName, System.currentTimeMillis());
        String configInfo = objectMapper.writeValueAsString(configEntity);

        // 移除AsyncTask的缓存
        Collection<AsyncTask> asyncTasks = dataIdContext.removeAll(serviceName);

        if (CollectionUtils.isEmpty(asyncTasks)) return;

        // 为每一个AsyncContext设置200的状态码以及响应数据。
        for (AsyncTask asyncTask : asyncTasks) {
            // 表明未超时，已经进行处理了。
            asyncTask.setTimeout(false);

            HttpServletResponse response = (HttpServletResponse) asyncTask.getAsyncContext().getResponse();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");

            // 推送结果，注意，这里的response是 拉取请求时 里面缓存的 response
            response.getWriter().println(configInfo);

            // 调用 AsyncContext.complete() 来解除对客户端 拉取请求 的阻塞
            asyncTask.getAsyncContext().complete();
        }
    }

    // 自定义任务对象，主要目的是为了 缓存 AsyncContext， 建立 长轮询和推送 的关联
    @Data
    private static class AsyncTask {
        // 长轮询请求的上下文，包含请求和响应体
        private AsyncContext asyncContext;

        // 超时标记
        private boolean timeout;

        public AsyncTask(AsyncContext asyncContext, boolean timeout) {
            this.asyncContext = asyncContext;
            this.timeout = timeout;
        }
    }

}
