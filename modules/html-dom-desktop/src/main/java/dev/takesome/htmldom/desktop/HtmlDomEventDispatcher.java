package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class HtmlDomEventDispatcher {
    private static final Set<String> NON_BUBBLING = Set.of("mouseenter", "mouseleave", "pointerenter", "pointerleave");
    private static final int MAX_EVENT_LOG = 512;

    private final Map<Integer, ArrayList<RegisteredListener>> listeners = new HashMap<>();
    private final ArrayList<EventLogEntry> eventLog = new ArrayList<>();
    private long eventSequence;

    public ListenerRegistration addEventListener(UiDomElement target, String type, Handler handler) {
        return addEventListener(target, type, false, handler);
    }

    public ListenerRegistration addEventListener(UiDomElement target, String type, boolean capture, Handler handler) {
        if (target == null) return ListenerRegistration.EMPTY;
        String key = normalizeType(type);
        if (key.isBlank() || handler == null) return ListenerRegistration.EMPTY;
        RegisteredListener listener = new RegisteredListener(target.nodeId(), key, capture, handler);
        listeners.computeIfAbsent(target.nodeId(), ignored -> new ArrayList<>()).add(listener);
        return new ListenerRegistration(() -> removeListener(listener));
    }

    public void removeEventListeners(UiDomElement target) {
        if (target != null) listeners.remove(target.nodeId());
    }

    public void clear() {
        listeners.clear();
        eventLog.clear();
    }

    public List<EventLogEntry> eventLog() {
        return List.copyOf(eventLog);
    }

    public DomEvent dispatch(String type, UiDomElement target) {
        return dispatch(type, target, "", 0L, null);
    }

    public DomEvent dispatch(String type, UiDomElement target, Handler handler) {
        return dispatch(type, target, "", 0L, handler);
    }

    public DomEvent dispatchTransitionEnd(UiDomElement target, String propertyName, long elapsedMs) {
        return dispatchTransitionEnd(target, propertyName, elapsedMs, null);
    }

    public DomEvent dispatchTransitionEnd(UiDomElement target, String propertyName, long elapsedMs, Handler handler) {
        return dispatch("transitionend", target, propertyName, elapsedMs, handler);
    }

    private DomEvent dispatch(String type, UiDomElement target, String propertyName, long elapsedMs, Handler handler) {
        String eventType = normalizeType(type);
        if (target == null || eventType.isBlank()) return new DomEvent(eventType, target, target, propertyName, elapsedMs, DomEvent.NONE, List.of(), new EventState());
        if (NON_BUBBLING.contains(eventType)) return dispatchAtTarget(eventType, target, propertyName, elapsedMs, handler);
        return dispatchBubbling(eventType, target, propertyName, elapsedMs, handler);
    }

    private DomEvent dispatchAtTarget(String type, UiDomElement target, String propertyName, long elapsedMs, Handler handler) {
        List<UiDomElement> path = eventPath(target);
        EventState state = new EventState();
        DomEvent event = new DomEvent(type, target, target, propertyName, elapsedMs, DomEvent.AT_TARGET, path, state);
        invokeListeners(event, true);
        if (!event.immediateStopped()) invokeListeners(event, false);
        if (handler != null && !event.immediateStopped()) handler.handle(event);
        appendLog(event, false);
        return event;
    }

    private DomEvent dispatchBubbling(String type, UiDomElement target, String propertyName, long elapsedMs, Handler handler) {
        List<UiDomElement> path = eventPath(target);
        EventState state = new EventState();
        DomEvent last = new DomEvent(type, target, target, propertyName, elapsedMs, DomEvent.NONE, path, state);

        for (int i = path.size() - 1; i >= 1; i--) {
            DomEvent event = new DomEvent(type, target, path.get(i), propertyName, elapsedMs, DomEvent.CAPTURING_PHASE, path, state);
            last = event;
            invokeListeners(event, true);
            if (event.stopped()) {
                appendLog(event, true);
                return event;
            }
        }

        DomEvent atTarget = new DomEvent(type, target, target, propertyName, elapsedMs, DomEvent.AT_TARGET, path, state);
        last = atTarget;
        invokeListeners(atTarget, true);
        if (atTarget.stopped()) {
            appendLog(atTarget, true);
            return atTarget;
        }
        invokeListeners(atTarget, false);
        if (!atTarget.immediateStopped() && handler != null) handler.handle(atTarget);
        if (atTarget.stopped()) {
            appendLog(atTarget, true);
            return atTarget;
        }

        for (int i = 1; i < path.size(); i++) {
            DomEvent event = new DomEvent(type, target, path.get(i), propertyName, elapsedMs, DomEvent.BUBBLING_PHASE, path, state);
            last = event;
            invokeListeners(event, false);
            if (!event.immediateStopped() && handler != null) handler.handle(event);
            if (event.stopped()) {
                appendLog(event, true);
                return event;
            }
        }
        appendLog(last, true);
        return last;
    }

    private List<UiDomElement> eventPath(UiDomElement target) {
        ArrayList<UiDomElement> out = new ArrayList<>();
        UiDomElement current = target;
        while (current != null) {
            out.add(current);
            current = current.parent();
        }
        return List.copyOf(out);
    }

    private void invokeListeners(DomEvent event, boolean capture) {
        List<RegisteredListener> registered = listeners.getOrDefault(event.currentTarget().nodeId(), new ArrayList<>());
        if (registered.isEmpty()) return;
        ArrayList<RegisteredListener> snapshot = new ArrayList<>(registered);
        for (RegisteredListener listener : snapshot) {
            if (!listener.type.equals(event.type()) || listener.capture != capture || event.immediateStopped()) continue;
            listener.handler.handle(event);
        }
    }

    private void removeListener(RegisteredListener listener) {
        ArrayList<RegisteredListener> list = listeners.get(listener.nodeId);
        if (list == null) return;
        list.remove(listener);
        if (list.isEmpty()) listeners.remove(listener.nodeId);
    }

    private void appendLog(DomEvent event, boolean bubbles) {
        String path = event.composedPath().stream().map(this::selector).reduce((a, b) -> a + " > " + b).orElse("");
        eventLog.add(new EventLogEntry(++eventSequence, System.currentTimeMillis(), event.type(), event.target() == null ? -1 : event.target().nodeId(), selector(event.target()), selector(event.currentTarget()), event.eventPhase(), event.propertyName(), event.elapsedMs(), event.defaultPrevented(), event.stopped(), bubbles, path));
        while (eventLog.size() > MAX_EVENT_LOG) eventLog.remove(0);
    }

    private String selector(UiDomElement element) {
        if (element == null) return "";
        String classes = String.join(".", element.classList().values());
        return element.tagName() + (element.id().isBlank() ? "" : "#" + element.id()) + (classes.isBlank() ? "" : "." + classes);
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    public interface Handler { void handle(DomEvent event); }

    public static final class ListenerRegistration implements AutoCloseable {
        private static final ListenerRegistration EMPTY = new ListenerRegistration(null);
        private final Runnable remover;
        private boolean removed;

        private ListenerRegistration(Runnable remover) {
            this.remover = remover;
        }

        public void remove() {
            if (removed) return;
            removed = true;
            if (remover != null) remover.run();
        }

        @Override public void close() {
            remove();
        }
    }

    private static final class RegisteredListener {
        private final int nodeId;
        private final String type;
        private final boolean capture;
        private final Handler handler;

        private RegisteredListener(int nodeId, String type, boolean capture, Handler handler) {
            this.nodeId = nodeId;
            this.type = type;
            this.capture = capture;
            this.handler = handler;
        }

        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof RegisteredListener that)) return false;
            return nodeId == that.nodeId && capture == that.capture && Objects.equals(type, that.type) && handler == that.handler;
        }

        @Override public int hashCode() {
            return Objects.hash(nodeId, type, capture, System.identityHashCode(handler));
        }
    }

    private static final class EventState {
        boolean stopped;
        boolean immediateStopped;
        boolean defaultPrevented;
    }

    public record EventLogEntry(
            long sequence,
            long timestampMillis,
            String type,
            int targetNodeId,
            String targetSelector,
            String currentTargetSelector,
            int eventPhase,
            String propertyName,
            long elapsedMs,
            boolean defaultPrevented,
            boolean stopped,
            boolean bubbles,
            String composedPath
    ) { }

    public static final class DomEvent {
        public static final int NONE = 0;
        public static final int CAPTURING_PHASE = 1;
        public static final int AT_TARGET = 2;
        public static final int BUBBLING_PHASE = 3;

        private final String type;
        private final UiDomElement target;
        private final UiDomElement currentTarget;
        private final String propertyName;
        private final long elapsedMs;
        private final int eventPhase;
        private final List<UiDomElement> composedPath;
        private final EventState state;

        private DomEvent(String type, UiDomElement target, UiDomElement currentTarget, String propertyName, long elapsedMs, int eventPhase, List<UiDomElement> composedPath, EventState state) {
            this.type = type == null ? "" : type;
            this.target = target;
            this.currentTarget = currentTarget;
            this.propertyName = propertyName == null ? "" : propertyName;
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.eventPhase = eventPhase;
            this.composedPath = composedPath == null ? List.of() : List.copyOf(composedPath);
            this.state = state == null ? new EventState() : state;
        }

        public String type() { return type; }
        public UiDomElement target() { return target; }
        public UiDomElement currentTarget() { return currentTarget; }
        public String propertyName() { return propertyName; }
        public long elapsedMs() { return elapsedMs; }
        public double elapsedTime() { return elapsedMs / 1000.0; }
        public int eventPhase() { return eventPhase; }
        public List<UiDomElement> composedPath() { return composedPath; }
        public boolean stopped() { return state.stopped; }
        public boolean immediateStopped() { return state.immediateStopped; }
        public boolean defaultPrevented() { return state.defaultPrevented; }
        public void stopPropagation() { state.stopped = true; }
        public void stopImmediatePropagation() { state.immediateStopped = true; state.stopped = true; }
        public void preventDefault() { state.defaultPrevented = true; }
    }
}
