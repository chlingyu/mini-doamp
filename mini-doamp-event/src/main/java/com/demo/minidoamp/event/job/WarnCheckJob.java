package com.demo.minidoamp.event.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.minidoamp.core.entity.JobExecLog;
import com.demo.minidoamp.core.entity.WarnRule;
import com.demo.minidoamp.core.mapper.JobExecLogMapper;
import com.demo.minidoamp.core.mapper.WarnRuleMapper;
import com.demo.minidoamp.event.engine.WarnEngine;
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
public class WarnCheckJob {

    private final WarnRuleMapper ruleMapper;
    private final WarnEngine warnEngine;
    private final LockProvider lockProvider;
    private final JobExecLogMapper jobExecLogMapper;

    @XxlJob("warnCheckHandler")
    public void execute() {
        List<WarnRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<WarnRule>().eq(WarnRule::getStatus, 1));
        log.info("WarnCheckJob start, enabled rules={}", rules.size());

        for (WarnRule rule : rules) {
            String lockName = "warn_check_" + rule.getId();
            LockConfiguration lockConfig = new LockConfiguration(
                    Instant.now(), lockName, Duration.ofMinutes(10), Duration.ofSeconds(30));

            Optional<SimpleLock> lock = lockProvider.lock(lockConfig);
            if (!lock.isPresent()) {
                log.debug("WarnCheckJob skip ruleId={}, lock held by another instance", rule.getId());
                continue;
            }

            long start = System.currentTimeMillis();
            JobExecLog execLog = new JobExecLog();
            execLog.setJobName("warn_check");
            execLog.setJobParam("ruleId=" + rule.getId());
            execLog.setCreateTime(LocalDateTime.now());

            try {
                int count = warnEngine.check(rule.getId()).size();
                execLog.setStatus(1);
                execLog.setMessage("triggered " + count + " records");
            } catch (Exception e) {
                execLog.setStatus(0);
                execLog.setMessage(e.getMessage());
                log.error("WarnCheckJob failed ruleId={}", rule.getId(), e);
            } finally {
                execLog.setCostMs(System.currentTimeMillis() - start);
                jobExecLogMapper.insert(execLog);
                lock.get().unlock();
            }
        }
    }
}
