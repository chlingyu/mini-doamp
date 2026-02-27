package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_warn_index")
public class WarnIndex extends BaseEntity {

    private String indexCode;
    private String indexName;
    private String indexType;
    private String dataTable;
    private String dataColumn;
    private String groupColumn;
    private String customSql;
    private Integer status;
    private String remark;

    @TableLogic
    private Integer deleted;
}
