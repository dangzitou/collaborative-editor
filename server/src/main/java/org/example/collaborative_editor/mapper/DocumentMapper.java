package org.example.collaborative_editor.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.collaborative_editor.annotation.AutoFill;
import org.example.collaborative_editor.entity.Document;
import org.example.collaborative_editor.enumeration.OperationType;

import org.example.collaborative_editor.vo.DocumentVO;

@Mapper
public interface DocumentMapper {

    /**
     * 插入文档
     */
    @AutoFill(OperationType.INSERT)
    void insert(Document document);

    /**
     * 根据docId查询文档
     */
    @Select("select * from document where doc_id = #{docId} and status = 1")
    Document getByDocId(String docId);

    /**
     * 更新文档内容
     */
    @AutoFill(OperationType.UPDATE)
    void update(Document document);

    /**
     * 查询用户文档列表
     */
    java.util.List<DocumentVO> listByOwnerId(Long ownerId);

    /**
     * 根据ID列表查询文档
     */
    java.util.List<DocumentVO> listByIds(java.util.List<String> docIds);
}
