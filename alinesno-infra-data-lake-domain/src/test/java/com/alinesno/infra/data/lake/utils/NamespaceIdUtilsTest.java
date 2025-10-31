package com.alinesno.infra.data.lake.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 NamespaceIdUtils.getId()
 */
public class NamespaceIdUtilsTest {

    @Test
    public void testGenerate1000Ids() {
        final int COUNT = 1000*10000;
        List<String> ids = new ArrayList<>(COUNT);
        Set<String> unique = new HashSet<>(COUNT);

        for (int i = 0; i < COUNT; i++) {
            String id = NamespaceIdUtils.getId();
            assertNotNull(id, "生成的 id 不应为 null");
            // 长度固定为 8
            assertEquals(8, id.length(), "id 长度应为 8，实际: " + id);
            // 仅包含 0-9 A-Z a-z
            assertTrue(id.matches("^[0-9A-Za-z]{8}$"), "id 包含非法字符: " + id);

            ids.add(id);
            unique.add(id);
        }

        int uniqueCount = unique.size();
        System.out.println("生成总数: " + COUNT);
        System.out.println("去重后唯一数: " + uniqueCount);
        System.out.println("是否全部唯一: " + (uniqueCount == COUNT));
        System.out.println("示例 20 个 ID：");
        for (int i = 0; i < Math.min(20, ids.size()); i++) {
            System.out.println((i + 1) + ": " + ids.get(i));
        }

        // 断言没有重复
        assertEquals(COUNT, uniqueCount, "存在重复 id，重复数量: " + (COUNT - uniqueCount));
    }
}