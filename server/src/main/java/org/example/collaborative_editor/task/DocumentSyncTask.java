package org.example.collaborative_editor.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collaborative_editor.service.DocumentService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 * 文档同步定时任务
 * 负责将 Redis 中的文档内容同步到 MySQL
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentSyncTask {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DocumentService documentService;

    /**
     * 每 10 秒执行一次同步
     */
    @Scheduled(cron = "0/10 * * * * ?")
    public void syncDocumentContent() {
        // 1. 获取所有脏文档 ID
        Set<Object> dirtyDocIds = redisTemplate.opsForSet().members("dirty_docs");

        if (CollectionUtils.isEmpty(dirtyDocIds)) {
            return;
        }

        log.info("开始同步文档内容，共 {} 个文档需要同步: {}", dirtyDocIds.size(), dirtyDocIds);

        for (Object idObj : dirtyDocIds) {
            String docId = (String) idObj;
            try {
                // 2. 从 Redis 获取最新内容
                String content = (String) redisTemplate.opsForValue().get("doc:" + docId);

                if (content != null) {
                    // 3. 保存到 MySQL
                    documentService.saveContent(docId, content);

                    // 4. 从脏集合中移除（同步成功）
                    redisTemplate.opsForSet().remove("dirty_docs", docId);
                    log.info("文档 {} 同步成功", docId);
                } else {
                    log.warn("文档 {} 内容为空，跳过同步", docId);
                }
            } catch (Exception e) {
                log.error("文档 {} 同步失败", docId, e);
            }
        }
    }
}
