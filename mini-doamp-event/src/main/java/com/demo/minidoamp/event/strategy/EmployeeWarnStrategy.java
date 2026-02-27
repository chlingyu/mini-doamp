package com.demo.minidoamp.event.strategy;

import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.IndexGroupMapper;
import org.springframework.stereotype.Component;

@Component
public class EmployeeWarnStrategy extends AbstractGroupWarnStrategy {

    public EmployeeWarnStrategy(IndexGroupMapper indexGroupMapper) {
        super(indexGroupMapper);
    }

    @Override
    public IndexType getType() { return IndexType.EMPLOYEE; }

    @Override
    protected String getGroupType() { return "EMPLOYEE"; }
}
