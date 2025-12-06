package org.example.collaborative_editor.service;

import org.example.collaborative_editor.entity.Document;

public interface DocumentService {

    /**
     * 创建文档
     */
    Document createDocument(String title, Long userId);

    /**
     * 获取文档内容
     */
    Document getDocument(String docId);

    /**
     * 保存文档内容
     */
    void saveContent(String docId, String content);

    /**
     * 获取用户文档列表
     */
    java.util.List<Document> listDocuments(Long userId);

    /**
     * 删除文档
     */
    void deleteDocument(String docId);
}
