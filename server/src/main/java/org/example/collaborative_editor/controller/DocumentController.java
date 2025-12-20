package org.example.collaborative_editor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collaborative_editor.common.Result;
import org.example.collaborative_editor.context.BaseContext;
import org.example.collaborative_editor.dto.CreateDocumentRequest;
import org.example.collaborative_editor.entity.Document;
import org.example.collaborative_editor.service.DocumentService;
import org.springframework.web.bind.annotation.*;

import org.example.collaborative_editor.vo.DocumentVO;

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
    public Result<java.util.List<DocumentVO>> listDocuments() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取文档列表, userId: {}", userId);
        return Result.success(documentService.listDocuments(userId));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{docId}")
    public Result<Void> deleteDocument(@PathVariable String docId) {
        log.info("删除文档: {}", docId);
        documentService.deleteDocument(docId);
        return Result.success();
    }

    /**
     * 生成邀请码
     */
    @PostMapping("/{docId}/invite")
    public Result<String> createInviteCode(@PathVariable String docId) {
        log.info("生成邀请码: {}", docId);
        return Result.success(documentService.createInviteCode(docId));
    }

    /**
     * 加入协作
     */
    @PostMapping("/join")
    public Result<Document> joinByInviteCode(@RequestBody java.util.Map<String, String> body) {
        String code = body.get("code");
        log.info("加入协作: {}", code);
        return Result.success(documentService.joinByInviteCode(code));
    }
}
