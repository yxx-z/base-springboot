package com.yxx.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * 与数据库实体和具体业务模块无关的导航菜单树构建器。
 *
 * <p>角色只需要直接关联业务菜单，构建器会补齐可见祖先。隐藏祖先不会出现在导航结果中，
 * 但其可见子节点会提升到最近的可见祖先；如果不存在可见祖先，则提升为根节点。</p>
 */
public final class NavigationTreeBuilder {

    private NavigationTreeBuilder() {
    }

    /**
     * 根据已启用菜单和角色选中菜单构建不可变导航树。
     *
     * @param enabledNodes 所有已启用菜单，必须同时包含可见和隐藏节点
     * @param selectedIds 角色直接关联的菜单主键
     * @param idGetter 菜单主键读取器
     * @param parentIdGetter 父菜单主键读取器
     * @param visiblePredicate 菜单可见性判断器
     * @param sortGetter 同级排序值读取器
     * @param responseFactory 将菜单和子节点转换为响应对象的工厂
     */
    public static <ID, N, R> List<R> build(
            Collection<N> enabledNodes,
            Collection<ID> selectedIds,
            Function<N, ID> idGetter,
            Function<N, ID> parentIdGetter,
            Predicate<N> visiblePredicate,
            ToIntFunction<N> sortGetter,
            BiFunction<N, List<R>, R> responseFactory) {
        if (enabledNodes == null || enabledNodes.isEmpty()
                || selectedIds == null || selectedIds.isEmpty()) {
            return List.of();
        }

        Map<ID, N> nodeIndex = new HashMap<>();
        for (N node : enabledNodes) {
            nodeIndex.put(idGetter.apply(node), node);
        }

        // 先收集选中菜单的完整祖先链。隐藏节点参与寻路，但最终不会出现在导航响应中。
        Set<ID> involvedIds = new HashSet<>();
        for (ID selectedId : selectedIds) {
            collectAncestors(selectedId, nodeIndex, idGetter, parentIdGetter, involvedIds);
        }

        Map<ID, List<N>> childrenByVisibleParent = new HashMap<>();
        List<N> roots = new ArrayList<>();
        for (ID involvedId : involvedIds) {
            N node = nodeIndex.get(involvedId);
            if (node == null || !visiblePredicate.test(node)) {
                continue;
            }
            ID visibleParentId = findNearestVisibleParent(
                    node, involvedIds, nodeIndex, idGetter, parentIdGetter, visiblePredicate);
            if (visibleParentId == null) {
                roots.add(node);
            } else {
                childrenByVisibleParent.computeIfAbsent(visibleParentId, ignored -> new ArrayList<>())
                        .add(node);
            }
        }

        Comparator<N> comparator = Comparator.comparingInt(sortGetter)
                .thenComparing(node -> String.valueOf(idGetter.apply(node)));
        roots.sort(comparator);
        childrenByVisibleParent.values().forEach(children -> children.sort(comparator));
        return roots.stream()
                .map(root -> toResponse(root, idGetter, childrenByVisibleParent, responseFactory))
                .toList();
    }

    private static <ID, N> void collectAncestors(
            ID startId,
            Map<ID, N> nodeIndex,
            Function<N, ID> idGetter,
            Function<N, ID> parentIdGetter,
            Set<ID> involvedIds) {
        ID currentId = startId;
        Set<ID> path = new HashSet<>();
        while (currentId != null && path.add(currentId)) {
            N node = nodeIndex.get(currentId);
            if (node == null) {
                return;
            }
            involvedIds.add(idGetter.apply(node));
            currentId = parentIdGetter.apply(node);
        }
        if (currentId != null) {
            throw new IllegalStateException("菜单数据存在循环父子关系，menuId=" + startId);
        }
    }

    private static <ID, N> ID findNearestVisibleParent(
            N node,
            Set<ID> involvedIds,
            Map<ID, N> nodeIndex,
            Function<N, ID> idGetter,
            Function<N, ID> parentIdGetter,
            Predicate<N> visiblePredicate) {
        ID parentId = parentIdGetter.apply(node);
        Set<ID> path = new HashSet<>();
        path.add(idGetter.apply(node));
        while (parentId != null && path.add(parentId)) {
            N parent = nodeIndex.get(parentId);
            if (parent == null) {
                return null;
            }
            if (involvedIds.contains(parentId) && visiblePredicate.test(parent)) {
                return parentId;
            }
            parentId = parentIdGetter.apply(parent);
        }
        if (parentId != null) {
            throw new IllegalStateException("菜单数据存在循环父子关系，menuId=" + idGetter.apply(node));
        }
        return null;
    }

    private static <ID, N, R> R toResponse(
            N node,
            Function<N, ID> idGetter,
            Map<ID, List<N>> childrenByVisibleParent,
            BiFunction<N, List<R>, R> responseFactory) {
        List<R> children = childrenByVisibleParent.getOrDefault(idGetter.apply(node), List.of())
                .stream()
                .map(child -> toResponse(child, idGetter, childrenByVisibleParent, responseFactory))
                .toList();
        return responseFactory.apply(node, children);
    }
}
