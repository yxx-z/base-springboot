package com.yxx.common.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 通用树形结构构建工具。
 *
 * <p>构建过程保持输入顺序，并检测重复主键和循环引用，避免错误数据导致无限递归。</p>
 */
public final class TreeUtil {

    private TreeUtil() {
    }

    public static <T, K> List<T> buildTree(
            List<T> source,
            Function<T, K> parentKeyFunction,
            Function<T, K> keyFunction,
            BiConsumer<T, List<T>> childrenConsumer,
            K rootParentValue) {
        return buildTree(source, parentKeyFunction, keyFunction, childrenConsumer, rootParentValue, null);
    }

    public static <T, K, S extends Comparable<? super S>> List<T> buildAscTree(
            List<T> source,
            Function<T, K> parentKeyFunction,
            Function<T, K> keyFunction,
            BiConsumer<T, List<T>> childrenConsumer,
            K rootParentValue,
            Function<T, S> sortFunction) {
        return buildTree(source, parentKeyFunction, keyFunction, childrenConsumer,
                rootParentValue, Comparator.comparing(sortFunction));
    }

    public static <T, K, S extends Comparable<? super S>> List<T> buildDescTree(
            List<T> source,
            Function<T, K> parentKeyFunction,
            Function<T, K> keyFunction,
            BiConsumer<T, List<T>> childrenConsumer,
            K rootParentValue,
            Function<T, S> sortFunction) {
        return buildTree(source, parentKeyFunction, keyFunction, childrenConsumer,
                rootParentValue, Comparator.comparing(sortFunction).reversed());
    }

    private static <T, K> List<T> buildTree(
            List<T> source,
            Function<T, K> parentKeyFunction,
            Function<T, K> keyFunction,
            BiConsumer<T, List<T>> childrenConsumer,
            K rootParentValue,
            Comparator<T> comparator) {
        Objects.requireNonNull(source, "树形数据不能为空");
        Objects.requireNonNull(parentKeyFunction, "父节点函数不能为空");
        Objects.requireNonNull(keyFunction, "主键函数不能为空");
        Objects.requireNonNull(childrenConsumer, "子节点设置函数不能为空");

        Map<K, T> nodes = new LinkedHashMap<>();
        Map<K, List<K>> childKeys = new LinkedHashMap<>();
        List<K> rootKeys = new ArrayList<>();

        for (T node : source) {
            K key = Objects.requireNonNull(keyFunction.apply(node), "节点主键不能为空");
            if (nodes.putIfAbsent(key, node) != null) {
                throw new IllegalArgumentException("存在重复的树节点主键：" + key);
            }
            K parentKey = parentKeyFunction.apply(node);
            if (Objects.equals(rootParentValue, parentKey)) {
                rootKeys.add(key);
            } else if (parentKey != null) {
                childKeys.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(key);
            }
        }

        List<T> roots = new ArrayList<>();
        for (K rootKey : rootKeys) {
            roots.add(attachChildren(rootKey, nodes, childKeys, childrenConsumer, comparator, new HashSet<>()));
        }
        sort(roots, comparator);
        return roots;
    }

    private static <T, K> T attachChildren(
            K key,
            Map<K, T> nodes,
            Map<K, List<K>> childKeys,
            BiConsumer<T, List<T>> childrenConsumer,
            Comparator<T> comparator,
            Set<K> visiting) {
        if (!visiting.add(key)) {
            throw new IllegalArgumentException("树节点存在循环引用：" + key);
        }

        T node = Objects.requireNonNull(nodes.get(key), "找不到树节点：" + key);
        List<T> children = new ArrayList<>();
        for (K childKey : childKeys.getOrDefault(key, List.of())) {
            children.add(attachChildren(childKey, nodes, childKeys, childrenConsumer,
                    comparator, new HashSet<>(visiting)));
        }
        sort(children, comparator);
        childrenConsumer.accept(node, children);
        return node;
    }

    private static <T> void sort(List<T> values, Comparator<T> comparator) {
        if (comparator != null) {
            values.sort(comparator);
        }
    }
}
