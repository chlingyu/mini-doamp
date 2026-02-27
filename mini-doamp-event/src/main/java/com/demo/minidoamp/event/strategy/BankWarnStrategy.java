package com.demo.minidoamp.event.strategy;

import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.IndexGroupMapper;
import org.springframework.stereotype.Component;

@Component
public class BankWarnStrategy extends AbstractGroupWarnStrategy {

    public BankWarnStrategy(IndexGroupMapper indexGroupMapper) {
        super(indexGroupMapper);
    }

    @Override
    public IndexType getType() { return IndexType.BANK; }

    @Override
    protected String getGroupType() { return "BANK"; }
}
