package com.knowledge.controller;

import com.knowledge.common.page.PageVO;
import com.knowledge.common.result.Result;
import com.knowledge.service.PlazaService;
import com.knowledge.vo.PlazaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/plaza")
@RequiredArgsConstructor
public class PlazaController {

    private final PlazaService plazaService;

    /**
     * 知识大厅分页(登录用户即可访问)
     */
    @GetMapping("/page")
    public Result<PageVO<PlazaVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(plazaService.page(page, size));
    }
}
