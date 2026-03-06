package com.demo.minidoamp.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.minidoamp.api.vo.WarnTrendVO;
import com.demo.minidoamp.core.entity.WarnRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WarnRecordMapper extends BaseMapper<WarnRecord> {

    List<WarnTrendVO> selectTrend(@Param("startTime") LocalDateTime startTime,
                                  @Param("limit") int limit);
}
