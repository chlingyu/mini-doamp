package com.demo.minidoamp.doamp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.minidoamp.api.dto.response.PageResponse;
import com.demo.minidoamp.api.ErrorCode;
import com.demo.minidoamp.api.BusinessException;
import com.demo.minidoamp.core.adapter.DatabaseAdapter;
import com.demo.minidoamp.core.adapter.DatabaseAdapterFactory;
import com.demo.minidoamp.core.entity.JobExecLog;
import com.demo.minidoamp.core.mapper.JobExecLogMapper;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobExecLogMapper jobExecLogMapper;
    private final JdbcTemplate jdbcTemplate;
    private final DatabaseIdProvider databaseIdProvider;
    private final DataSource dataSource;
    private final DatabaseAdapterFactory adapterFactory;

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

    public PageResponse<JobExecLog> nativeLogPage(int pageNum, int pageSize, String jobName) {
        int normalizedPageNum = Math.max(pageNum, 1);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (normalizedPageNum - 1) * normalizedPageSize;

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM t_job_exec_log WHERE 1=1");
        StringBuilder querySql = new StringBuilder("SELECT id, job_name, job_param, status, message, cost_ms, create_time FROM t_job_exec_log WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(jobName)) {
            countSql.append(" AND job_name = ?");
            querySql.append(" AND job_name = ?");
            args.add(jobName);
        }
        querySql.append(" ORDER BY create_time DESC");

        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, args.toArray());
        if (total == null || total == 0) {
            return PageResponse.of(new ArrayList<>(), 0, normalizedPageNum, normalizedPageSize);
        }

        String databaseId = resolveDatabaseId();
        DatabaseAdapter adapter = adapterFactory.getAdapter(databaseId);
        String pagedSql = adapter.paginate(querySql.toString(), offset, normalizedPageSize);
        List<JobExecLog> records = jdbcTemplate.query(pagedSql, this::mapJobExecLog, args.toArray());
        return PageResponse.of(records, total, normalizedPageNum, normalizedPageSize);
    }

    private String resolveDatabaseId() {
        try {
            return databaseIdProvider.getDatabaseId(dataSource);
        } catch (SQLException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private JobExecLog mapJobExecLog(ResultSet rs, int rowNum) throws SQLException {
        JobExecLog log = new JobExecLog();
        log.setId(rs.getLong("id"));
        log.setJobName(rs.getString("job_name"));
        log.setJobParam(rs.getString("job_param"));
        log.setStatus(rs.getInt("status"));
        log.setMessage(rs.getString("message"));
        log.setCostMs(rs.getLong("cost_ms"));
        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            log.setCreateTime(createTime.toLocalDateTime());
        }
        return log;
    }
}
