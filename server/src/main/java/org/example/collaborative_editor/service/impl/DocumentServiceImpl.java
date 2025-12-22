package org.example.collaborative_editor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collaborative_editor.common.Result;
import org.example.collaborative_editor.context.BaseContext;
import org.example.collaborative_editor.constant.MessageConstant;
import org.example.collaborative_editor.constant.StatusConstant;
import org.example.collaborative_editor.entity.Document;
import org.example.collaborative_editor.exception.BusinessException;
import org.example.collaborative_editor.entity.Collaborator;
import org.example.collaborative_editor.mapper.CollaboratorMapper;
import org.example.collaborative_editor.mapper.DocumentMapper;
import org.example.collaborative_editor.service.DocumentService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.example.collaborative_editor.vo.DocumentVO;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final CollaboratorMapper collaboratorMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public Document createDocument(String title, Long userId) {
        Document document = Document.builder()
                .title(title)
                .docId(UUID.randomUUID().toString())
                .ownerId(userId)
                .status(StatusConstant.ENABLE)
                .content("")
                .build(); // 默认内容为空
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
    public java.util.List<DocumentVO> listDocuments(Long userId) {
        // 1. 获取自己创建的文档
        List<DocumentVO> myDocs = documentMapper.listByOwnerId(userId);

        // 2. 获取参与协作的文档
        List<String> collabDocIds = collaboratorMapper.listDocIdsByUserId(userId);
        List<DocumentVO> collabDocs = new ArrayList<>();
        if (collabDocIds != null && !collabDocIds.isEmpty()) {
            collabDocs = documentMapper.listByIds(collabDocIds);
        }

        // 3. 合并列表
        List<DocumentVO> result = new ArrayList<>();
        if (myDocs != null)
            result.addAll(myDocs);
        if (collabDocs != null)
            result.addAll(collabDocs);

        // 按时间排序（简单处理，如果需要严格排序可以在内存中sort）
        result.sort((d1, d2) -> d2.getUpdateTime().compareTo(d1.getUpdateTime()));

        return result;
    }

    @Override
    public void deleteDocument(String docId) {
        Document document = documentMapper.getByDocId(docId);
        if (document == null) {
            throw new BusinessException(MessageConstant.DOCUMENT_NOT_FOUND);
        }

        Long currentUserId = BaseContext.getCurrentId();
        if (!document.getOwnerId().equals(currentUserId)) {
            throw new BusinessException(MessageConstant.DOCUMENT_NO_PERMISSION);
        }

        document.setStatus(StatusConstant.DISABLE);
        documentMapper.update(document);

        // 删除Redis缓存
        redisTemplate.delete("doc:" + docId);
        redisTemplate.opsForSet().remove("dirty_docs", docId);

        // 广播删除消息
        org.example.collaborative_editor.ws.EditorServer.broadcastSystemMessage(
                docId,
                org.example.collaborative_editor.constant.WsMessageType.DOC_DELETED,
                "Document deleted");
    }

    @Override
    public String createInviteCode(String docId) {
        // 检查文档是否存在
        Document document = documentMapper.getByDocId(docId);
        if (document == null) {
            throw new BusinessException(MessageConstant.DOCUMENT_NOT_FOUND);
        }
        // 检查权限（只有拥有者可以生成邀请码）
        Long currentUserId = BaseContext.getCurrentId();
        if (!document.getOwnerId().equals(currentUserId)) {
            throw new BusinessException(MessageConstant.DOCUMENT_NO_PERMISSION);
        }

        // 生成随机码
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        // 存入Redis，有效期24小时
        redisTemplate.opsForValue().set("invite:" + code, docId, 24, TimeUnit.HOURS);
        return code;
    }

    @Override
    @Transactional
    public Document joinByInviteCode(String code) {
        // 验证邀请码
        String docId = (String) redisTemplate.opsForValue().get("invite:" + code);
        if (docId == null) {
            throw new BusinessException(MessageConstant.INVITE_CODE_INVALID);
        }

        // 检查文档
        Document document = documentMapper.getByDocId(docId);
        if (document == null) {
            throw new BusinessException(MessageConstant.DOCUMENT_NOT_FOUND);
        }

        Long currentUserId = BaseContext.getCurrentId();
        // 如果是拥有者，直接返回
        if (document.getOwnerId().equals(currentUserId)) {
            return document;
        }

        // 检查是否已经是协作者
        Collaborator collaborator = collaboratorMapper.getByDocIdAndUserId(docId, currentUserId);
        if (collaborator == null) {
            // 添加协作者记录
            collaborator = Collaborator.builder()
                    .docId(docId)
                    .userId(currentUserId)
                    .createTime(LocalDateTime.now())
                    .build();
            collaboratorMapper.insert(collaborator);
        }

        return document;
    }

    @Override
    public void updateTitle(String docId, String title) {
        Document document = documentMapper.getByDocId(docId);
        if (document == null) {
            throw new BusinessException(MessageConstant.DOCUMENT_NOT_FOUND);
        }
        
        // 权限检查：只有所有者可以修改标题
        Long currentUserId = BaseContext.getCurrentId();
        if (!document.getOwnerId().equals(currentUserId)) {
            throw new BusinessException(MessageConstant.DOCUMENT_NO_PERMISSION);
        }

        document.setTitle(title);
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.update(document);
    }
}
