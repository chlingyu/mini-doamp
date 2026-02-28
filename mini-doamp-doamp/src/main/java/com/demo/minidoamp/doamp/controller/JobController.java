package com.demo.minidoamp.doamp.controller;

import com.demo.minidoamp.api.R;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.core.entity.JobExecLog;
import com.demo.minidoamp.doamp.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/job")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        List<Map<String, Object>> jobs = Arrays.asList(
                jobDef("warn_check", "warnCheckHandler", "0 0/5 * * * ?", "预警规则检查"),
                jobDef("sop_generate", "sopTaskGenerateHandler", "0 0 8 * * ?", "SOP任务定时生成"),
                jobDef("msg_compensation", "msgCompensationHandler", "0 0/1 * * * ?", "消息补偿重试")
        );
        return R.ok(jobs);
    }

    @GetMapping("/log")
    public R<PageResponse<JobExecLog>> log(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String jobName) {
        return R.ok(jobService.logPage(pageNum, pageSize, jobName));
    }

    private Map<String, Object> jobDef(String name, String handler, String cron, String desc) {
        Map<String, Object> m = new HashMap<>();
        m.put("jobName", name);
        m.put("handler", handler);
        m.put("cronExpr", cron);
        m.put("description", desc);
        return m;
    }
}
