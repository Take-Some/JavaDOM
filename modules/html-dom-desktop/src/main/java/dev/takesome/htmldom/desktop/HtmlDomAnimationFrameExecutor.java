package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssKeyframesRule;
import dev.takesome.htmldom.css.animation.UiCssAnimationDescriptor;
import dev.takesome.htmldom.css.animation.UiCssAnimationTimeline;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.logging.HtmlDomLog;
import dev.takesome.htmldom.logging.HtmlDomLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Computes pure CSS animation frames outside the Swing mutation path when multiple targets are active. */
final class HtmlDomAnimationFrameExecutor implements AutoCloseable {
    private static final HtmlDomLogger LOG = HtmlDomLog.logger(HtmlDomAnimationFrameExecutor.class);
    private static final int PARALLEL_TARGET_THRESHOLD = 2;
    private static final long WORKER_KEEP_ALIVE_MS = 1_500L;
    private static final long DECISION_LOG_INTERVAL_MS = 1_000L;

    private final UiCssAnimationTimeline serialTimeline = new UiCssAnimationTimeline();
    private final ThreadLocal<UiCssAnimationTimeline> workerTimelines = ThreadLocal.withInitial(UiCssAnimationTimeline::new);
    private final int maxWorkerThreads;
    private final ThreadPoolExecutor executor;
    private volatile boolean closed;
    private volatile int lastRequestedWorkers;
    private volatile int lastAnimationTargets;
    private volatile long workerFallbacks;
    private volatile long serialFrames;
    private volatile long parallelFrames;
    private volatile long lastDecisionLogMs;

    HtmlDomAnimationFrameExecutor(int maxWorkerThreads) {
        this.maxWorkerThreads = Math.max(1, Math.min(32, maxWorkerThreads));
        this.executor = this.maxWorkerThreads <= 1 ? null : new ThreadPoolExecutor(
                0,
                this.maxWorkerThreads,
                WORKER_KEEP_ALIVE_MS,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new AnimationThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    List<FrameResult> computeFrames(Collection<FrameRequest> requests, Map<String, UiCssKeyframesRule> keyframes, long nowMs) {
        if (requests == null || requests.isEmpty() || keyframes == null || keyframes.isEmpty()) {
            lastAnimationTargets = requests == null ? 0 : requests.size();
            lastRequestedWorkers = 0;
            logDecision("idle", lastAnimationTargets, 0);
            return List.of();
        }
        int requestCount = requests.size();
        int requestedWorkers = requestedWorkers(requestCount);
        lastAnimationTargets = requestCount;
        lastRequestedWorkers = requestedWorkers;
        if (executor == null || closed || requestedWorkers <= 1) {
            List<FrameResult> frames = computeSerial(requests, keyframes, nowMs);
            serialFrames += frames.size();
            logDecision("serial", requestCount, requestedWorkers);
            return frames;
        }
        ArrayList<FrameRequest> snapshot = new ArrayList<>(requests);
        ArrayList<Future<List<FrameResult>>> futures = new ArrayList<>(requestedWorkers);
        for (int worker = 0; worker < requestedWorkers; worker++) {
            int start = worker * snapshot.size() / requestedWorkers;
            int end = (worker + 1) * snapshot.size() / requestedWorkers;
            if (start >= end) continue;
            List<FrameRequest> partition = List.copyOf(snapshot.subList(start, end));
            futures.add(executor.submit(() -> computeSerial(partition, keyframes, nowMs)));
        }
        ArrayList<FrameResult> out = new ArrayList<>(snapshot.size());
        for (Future<List<FrameResult>> future : futures) {
            try {
                out.addAll(future.get());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                workerFallbacks++;
                List<FrameResult> frames = computeSerial(snapshot, keyframes, nowMs);
                serialFrames += frames.size();
                logDecision("fallback-interrupted", requestCount, requestedWorkers);
                return frames;
            } catch (ExecutionException failed) {
                workerFallbacks++;
                List<FrameResult> frames = computeSerial(snapshot, keyframes, nowMs);
                serialFrames += frames.size();
                logDecision("fallback-error", requestCount, requestedWorkers);
                return frames;
            }
        }
        parallelFrames += out.size();
        logDecision("parallel", requestCount, requestedWorkers);
        return out;
    }

    int workerThreads() {
        return maxWorkerThreads;
    }

    int maxWorkerThreads() {
        return maxWorkerThreads;
    }

    int lastRequestedWorkers() {
        return lastRequestedWorkers;
    }

    int liveWorkerThreads() {
        return executor == null ? 0 : executor.getPoolSize();
    }

    boolean parallelEnabled() {
        return executor != null && !closed;
    }

    Stats stats() {
        return new Stats(
                lastRequestedWorkers,
                liveWorkerThreads(),
                lastAnimationTargets,
                workerFallbacks,
                serialFrames,
                parallelFrames
        );
    }

    private int requestedWorkers(int requestCount) {
        if (requestCount < PARALLEL_TARGET_THRESHOLD) return 1;
        return Math.max(1, Math.min(maxWorkerThreads, requestCount));
    }

    private void logDecision(String mode, int targets, int requestedWorkers) {
        if (!LOG.debugEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastDecisionLogMs < DECISION_LOG_INTERVAL_MS) return;
        lastDecisionLogMs = now;
        Stats stats = stats();
        LOG.debug(
                "HtmlDom animation executor targets={} requestedWorkers={} liveWorkers={} mode={} workerFallbacks={} serialFrames={} parallelFrames={}",
                targets,
                requestedWorkers,
                stats.liveWorkers(),
                mode,
                stats.workerFallbacks(),
                stats.serialFrames(),
                stats.parallelFrames()
        );
    }

    private List<FrameResult> computeSerial(Collection<FrameRequest> requests, Map<String, UiCssKeyframesRule> keyframes, long nowMs) {
        ArrayList<FrameResult> out = new ArrayList<>(requests.size());
        for (FrameRequest request : requests) out.add(compute(request, keyframes, nowMs));
        return out;
    }

    private FrameResult compute(FrameRequest request, Map<String, UiCssKeyframesRule> keyframes, long nowMs) {
        UiCssAnimationTimeline timeline = Thread.currentThread().getName().startsWith("HtmlDom-Animation-")
                ? workerTimelines.get()
                : serialTimeline;
        Map<String, String> frame = timeline.frame(request.animations(), keyframes, nowMs);
        return new FrameResult(request.element(), frame == null || frame.isEmpty() ? Map.of() : Map.copyOf(frame));
    }

    @Override public void close() {
        closed = true;
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(250L, TimeUnit.MILLISECONDS)) executor.shutdownNow();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        } finally {
            workerTimelines.remove();
        }
    }

    record Stats(int requestedWorkers, int liveWorkers, int animationTargets, long workerFallbacks, long serialFrames, long parallelFrames) { }

    record FrameRequest(UiDomElement element, List<UiCssAnimationDescriptor> animations) {
        FrameRequest {
            Objects.requireNonNull(element, "element");
            animations = animations == null ? List.of() : List.copyOf(animations);
        }
    }

    record FrameResult(UiDomElement element, Map<String, String> frame) {
        FrameResult {
            Objects.requireNonNull(element, "element");
            frame = frame == null ? Map.of() : Map.copyOf(frame);
        }
    }

    private static final class AnimationThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "HtmlDom-Animation-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
            return thread;
        }
    }
}
