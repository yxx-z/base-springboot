package com.yxx.common.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 导航菜单树构建规则测试。 */
class NavigationTreeBuilderTest {

    @Test
    void shouldPromoteVisibleChildAcrossHiddenParent() {
        List<MenuNode> menus = List.of(
                new MenuNode(1, null, true, 10, "管理中心"),
                new MenuNode(2, 1, false, 10, "隐藏容器"),
                new MenuNode(3, 2, true, 10, "用户管理"));

        List<MenuResult> result = build(menus, List.of(3));

        assertEquals(1, result.size());
        assertEquals("管理中心", result.get(0).name());
        assertEquals(List.of("用户管理"),
                result.get(0).children().stream().map(MenuResult::name).toList());
    }

    @Test
    void shouldPromoteVisibleChildToRootWhenAllAncestorsAreHidden() {
        List<MenuNode> menus = List.of(
                new MenuNode(1, null, false, 10, "隐藏根节点"),
                new MenuNode(2, 1, true, 10, "个人资料"));

        List<MenuResult> result = build(menus, List.of(2));

        assertEquals(List.of("个人资料"), result.stream().map(MenuResult::name).toList());
    }

    @Test
    void shouldRejectCircularMenuRelationship() {
        List<MenuNode> menus = List.of(
                new MenuNode(1, 2, true, 10, "菜单一"),
                new MenuNode(2, 1, true, 20, "菜单二"));

        assertThrows(IllegalStateException.class, () -> build(menus, List.of(1)));
    }

    private List<MenuResult> build(List<MenuNode> menus, List<Integer> selectedIds) {
        return NavigationTreeBuilder.build(
                menus, selectedIds,
                MenuNode::id, MenuNode::parentId, MenuNode::visible, MenuNode::sort,
                (node, children) -> new MenuResult(node.name(), children));
    }

    private record MenuNode(Integer id, Integer parentId, boolean visible, int sort, String name) {
    }

    private record MenuResult(String name, List<MenuResult> children) {
    }
}
