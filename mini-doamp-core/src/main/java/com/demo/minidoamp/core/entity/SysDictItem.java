package com.demo.minidoamp.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_sys_dict_item")
public class SysDictItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dictId;
    private String itemValue;
    private String itemLabel;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
}
