package com.demo.minidoamp.event.strategy;

import com.demo.minidoamp.core.enums.IndexType;
import com.demo.minidoamp.core.mapper.IndexGroupMapper;
import org.springframework.stereotype.Component;

@Component
public class ChannelWarnStrategy extends AbstractGroupWarnStrategy {

    public ChannelWarnStrategy(IndexGroupMapper indexGroupMapper) {
        super(indexGroupMapper);
    }

    @Override
    public IndexType getType() { return IndexType.CHANNEL; }

    @Override
    protected String getGroupType() { return "CHANNEL"; }
}
