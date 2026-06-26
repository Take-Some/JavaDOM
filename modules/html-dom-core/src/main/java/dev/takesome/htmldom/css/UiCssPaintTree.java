package dev.takesome.htmldom.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Flattened paint tree in final paint order plus parent/child lookup for DevTools. */
public final class UiCssPaintTree {
    private final List<UiCssPaintNode> order;
    private final Map<Integer, UiCssPaintNode> byNodeId;
    private final Map<Integer, List<UiCssPaintNode>> byParentId;

    UiCssPaintTree(List<UiCssPaintNode> order) {
        ArrayList<UiCssPaintNode> safeOrder = new ArrayList<>(order == null ? List.of() : order);
        this.order = List.copyOf(safeOrder);
        LinkedHashMap<Integer, UiCssPaintNode> nodes = new LinkedHashMap<>();
        LinkedHashMap<Integer, ArrayList<UiCssPaintNode>> children = new LinkedHashMap<>();
        for (UiCssPaintNode node : safeOrder) {
            nodes.put(node.nodeId(), node);
            children.computeIfAbsent(node.parentNodeId(), ignored -> new ArrayList<>()).add(node);
        }
        this.byNodeId = Collections.unmodifiableMap(nodes);
        LinkedHashMap<Integer, List<UiCssPaintNode>> frozen = new LinkedHashMap<>();
        for (Map.Entry<Integer, ArrayList<UiCssPaintNode>> entry : children.entrySet()) frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
        this.byParentId = Collections.unmodifiableMap(frozen);
    }

    public List<UiCssPaintNode> order() {
        return order;
    }

    public Optional<UiCssPaintNode> node(int nodeId) {
        return Optional.ofNullable(byNodeId.get(nodeId));
    }

    public List<UiCssPaintNode> childrenOf(int parentNodeId) {
        return byParentId.getOrDefault(parentNodeId, List.of());
    }

    public Map<Integer, UiCssPaintNode> nodes() {
        return byNodeId;
    }
}
