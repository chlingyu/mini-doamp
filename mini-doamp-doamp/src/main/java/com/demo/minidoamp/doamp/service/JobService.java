package com.demo.minidoamp.doamp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.core.entity.JobExecLog;
import com.demo.minidoamp.core.mapper.JobExecLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobExecLogMapper jobExecLogMapper;

    public PageResponse<JobExecLog> logPage(int pageNum, int pageSize, String jobName) {
        Page<JobExecLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobExecLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(jobName)) {
            wrapper.eq(JobExecLog::getJobName, jobName);
        }
        wrapper.orderByDesc(JobExecLog::getCreateTime);
        jobExecLogMapper.selectPage(page, wrapper);
        return PageResponse.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }
}
