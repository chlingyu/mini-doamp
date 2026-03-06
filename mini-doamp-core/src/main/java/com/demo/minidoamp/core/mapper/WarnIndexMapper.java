package com.demo.minidoamp.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.minidoamp.api.vo.WarnIndexTypeSummaryVO;
import com.demo.minidoamp.core.entity.WarnIndex;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WarnIndexMapper extends BaseMapper<WarnIndex> {

    List<WarnIndexTypeSummaryVO> selectTypeSummary();
}
