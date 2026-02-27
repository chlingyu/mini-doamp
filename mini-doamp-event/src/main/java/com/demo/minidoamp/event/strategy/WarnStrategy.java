package com.demo.minidoamp.event.strategy;

import com.demo.minidoamp.core.entity.WarnIndex;
import com.demo.minidoamp.core.entity.WarnRecord;
import com.demo.minidoamp.core.entity.WarnThreshold;
import com.demo.minidoamp.core.enums.IndexType;

import java.util.List;

public interface WarnStrategy {

    IndexType getType();

    List<WarnRecord> check(WarnIndex index, List<WarnThreshold> thresholds);
}
