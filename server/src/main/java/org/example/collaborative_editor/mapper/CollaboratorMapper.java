package org.example.collaborative_editor.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.collaborative_editor.entity.Collaborator;

import java.util.List;

@Mapper
public interface CollaboratorMapper {

    @Insert("insert into collaborator (doc_id, user_id, create_time) values (#{docId}, #{userId}, #{createTime})")
    void insert(Collaborator collaborator);

    @Select("select * from collaborator where doc_id = #{docId} and user_id = #{userId}")
    Collaborator getByDocIdAndUserId(String docId, Long userId);

    @Select("select doc_id from collaborator where user_id = #{userId}")
    List<String> listDocIdsByUserId(Long userId);
}
