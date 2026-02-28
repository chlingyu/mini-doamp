package com.demo.minidoamp.sop.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.JobExecLog;
import com.demo.minidoamp.core.entity.SopTaskTemplate;
import com.demo.minidoamp.core.mapper.JobExecLogMapper;
import com.demo.minidoamp.core.mapper.SopTaskTemplateMapper;
import com.demo.minidoamp.sop.service.SopTaskService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SopTaskGenerateJob {

    private final SopTaskTemplateMapper templateMapper;
    private final LockProvider lockProvider;
    private final JobExecLogMapper jobExecLogMapper;
    private final SopTaskService sopTaskService;

    @XxlJob("sopTaskGenerateHandler")
    public void execute() {
        List<SopTaskTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<SopTaskTemplate>()
                        .eq(SopTaskTemplate::getTriggerType, "CRON")
                        .eq(SopTaskTemplate::getStatus, 1));
        log.info("SopTaskGenerateJob start, cron templates={}", templates.size());

        for (SopTaskTemplate tpl : templates) {
            String lockName = "sop_generate_" + tpl.getId();
            LockConfiguration lockConfig = new LockConfiguration(
                    Instant.now(), lockName, Duration.ofMinutes(10), Duration.ofSeconds(30));

            Optional<SimpleLock> lock = lockProvider.lock(lockConfig);
            if (!lock.isPresent()) {
                log.debug("SopTaskGenerateJob skip templateId={}, lock held", tpl.getId());
                continue;
            }

            long start = System.currentTimeMillis();
            JobExecLog execLog = new JobExecLog();
            execLog.setJobName("sop_generate");
            execLog.setJobParam("templateId=" + tpl.getId());
            execLog.setCreateTime(LocalDateTime.now());

            try {
                Long taskId = sopTaskService.createTaskByTemplate(tpl);
                log.info("SopTaskGenerateJob templateId={}, created taskId={}", tpl.getId(), taskId);
                execLog.setStatus(1);
                execLog.setMessage("taskId=" + taskId);
            } catch (Exception e) {
                execLog.setStatus(0);
                execLog.setMessage(e.getMessage());
                log.error("SopTaskGenerateJob failed templateId={}", tpl.getId(), e);
            } finally {
                execLog.setCostMs(System.currentTimeMillis() - start);
                jobExecLogMapper.insert(execLog);
                lock.get().unlock();
            }
        }
    }
}
