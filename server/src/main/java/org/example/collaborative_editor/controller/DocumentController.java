package org.example.collaborative_editor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collaborative_editor.common.Result;
import org.example.collaborative_editor.context.BaseContext;
import org.example.collaborative_editor.dto.CreateDocumentRequest;
import org.example.collaborative_editor.entity.Document;
import org.example.collaborative_editor.service.DocumentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doc")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 创建文档
     */
    @PostMapping
    public Result<Document> createDocument(@RequestBody CreateDocumentRequest request) {
        log.info("创建文档: {}", request.getTitle());
        Long userId = BaseContext.getCurrentId();
        Document document = documentService.createDocument(request.getTitle(), userId);
        return Result.success(document);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{docId}")
    public Result<Document> getDocument(@PathVariable String docId) {
        log.info("获取文档: {}", docId);
        Document document = documentService.getDocument(docId);
        return Result.success(document);
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/list")
    public Result<java.util.List<Document>> listDocuments() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取文档列表, userId: {}", userId);
        return Result.success(documentService.listDocuments(userId));
    }
}
