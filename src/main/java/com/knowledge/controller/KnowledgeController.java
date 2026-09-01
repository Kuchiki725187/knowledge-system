package com.knowledge.controller;

import com.knowledge.common.page.PageVO;
import com.knowledge.common.result.Result;
import com.knowledge.dto.KnowledgePageDTO;
import com.knowledge.dto.KnowledgeSaveDTO;
import com.knowledge.dto.RelationSaveDTO;
import com.knowledge.service.KnowledgeService;
import com.knowledge.service.RelationService;
import com.knowledge.vo.KnowledgeBaseVO;
import com.knowledge.vo.KnowledgeDetailVO;
import com.knowledge.vo.KnowledgeVO;
import com.knowledge.vo.RelationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final RelationService relationService;

    @PostMapping
    public Result<KnowledgeBaseVO> create(@RequestBody @Valid KnowledgeSaveDTO dto) {
        return Result.ok(knowledgeService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeBaseVO> update(@PathVariable Long id, @RequestBody @Valid KnowledgeSaveDTO dto) {
        return Result.ok(knowledgeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDetailVO> detail(@PathVariable Long id) {
        return Result.ok(knowledgeService.getDetail(id));
    }

    @GetMapping("/page")
    public Result<PageVO<KnowledgeVO>> page(KnowledgePageDTO dto) {
        return Result.ok(knowledgeService.page(dto));
    }

    /**
     * 建立知识关联
     */
    @PostMapping("/{id}/relations")
    public Result<Void> addRelation(@PathVariable Long id, @RequestBody @Valid RelationSaveDTO dto) {
        relationService.addRelation(id, dto);
        return Result.ok();
    }

    /**
     * 查询知识的全部关联
     */
    @GetMapping("/{id}/relations")
    public Result<List<RelationVO>> listRelations(@PathVariable Long id) {
        return Result.ok(relationService.listRelations(id));
    }

    /**
     * 删除一条关联
     */
    @DeleteMapping("/relations/{relationId}")
    public Result<Void> removeRelation(@PathVariable Long relationId) {
        relationService.removeRelation(relationId);
        return Result.ok();
    }
}
