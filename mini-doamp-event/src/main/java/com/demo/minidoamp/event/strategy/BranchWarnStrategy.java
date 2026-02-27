package com.demo.minidoamp.event.strategy;

import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.IndexGroupMapper;
import org.springframework.stereotype.Component;

@Component
public class BranchWarnStrategy extends AbstractGroupWarnStrategy {

    public BranchWarnStrategy(IndexGroupMapper indexGroupMapper) {
        super(indexGroupMapper);
    }

    @Override
    public IndexType getType() { return IndexType.BRANCH; }

    @Override
    protected String getGroupType() { return "BRANCH"; }
}
