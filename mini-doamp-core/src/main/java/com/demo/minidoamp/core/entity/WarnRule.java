package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_warn_rule")
public class WarnRule extends BaseEntity {

    private String ruleName;
    private Long indexId;
    private String notifyType;
    private String receiverIds;
    private String cronExpr;
    private Integer status;

    @TableLogic
    private Integer deleted;
}
