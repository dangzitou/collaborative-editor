package org.example.collaborative_editor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collaborative_editor.constant.MessageConstant;
import org.example.collaborative_editor.constant.StatusConstant;
import org.example.collaborative_editor.entity.Document;
import org.example.collaborative_editor.exception.BusinessException;
import org.example.collaborative_editor.mapper.DocumentMapper;
import org.example.collaborative_editor.service.DocumentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;

    @Override
    @Transactional
    public Document createDocument(String title, Long userId) {
        Document document = new Document();
        document.setDocId(UUID.randomUUID().toString());
        document.setTitle(title);
        document.setOwnerId(userId);
        document.setStatus(StatusConstant.ENABLE);
        document.setContent(""); // 初始内容为空

        documentMapper.insert(document);
        return document;
    }

    @Override
    public Document getDocument(String docId) {
        Document document = documentMapper.getByDocId(docId);
        if (document == null) {
            throw new BusinessException(MessageConstant.DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    @Override
    public void saveContent(String docId, String content) {
        Document document = documentMapper.getByDocId(docId);
        if (document != null) {
            document.setContent(content);
            documentMapper.update(document);
        }
    }

    @Override
    public java.util.List<Document> listDocuments(Long userId) {
        return documentMapper.listByOwnerId(userId);
    }
}
