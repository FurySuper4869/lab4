package cn.edu.xmu.restfuldemo.model;

import io.swagger.annotations.ApiModel;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Setter
@Getter
@AllArgsConstructor
@ToString
@NoArgsConstructor
@ApiModel(description = "货品商品属性视图对象")
public class GoodsInProductRetVo implements VoObject,Serializable {

    private Integer id;

    private String name;

    private String goodsSn;

    private String imageUrl;

    private Integer state;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private Boolean disable;

    public GoodsInProductRetVo(Goods goods){
        this.id=goods.getId();
        this.name=goods.getName();
        this.goodsSn=goods.getGoodsSn();
        this.imageUrl=goods.getPicUrl();
        this.state=goods.gotGoodsPo().getState();
        this.gmtCreate=null;
        this.gmtModified=null;
        this.disable=null;
    }

    @Override
    public Object createVo() {
        return this;
    }
}
