package com.knowledge.service;

import com.knowledge.dto.RelationSaveDTO;
import com.knowledge.vo.RelationVO;

import java.util.List;

public interface RelationService {

    void addRelation(Long knowledgeId, RelationSaveDTO dto);

    List<RelationVO> listRelations(Long knowledgeId);

    void removeRelation(Long relationId);
}
