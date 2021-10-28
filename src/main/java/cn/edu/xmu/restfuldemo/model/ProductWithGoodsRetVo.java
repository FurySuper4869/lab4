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
@ApiModel(description = "货品视图对象")
public class ProductWithGoodsRetVo implements VoObject,Serializable {

    private Integer id;

    private String name;

    private String skuSn;

    private String detail;

    private String imageUrl;

    private Integer originalPrice;

    private Long price;

    private Integer inventory;

    private Integer state;

    private Integer weight;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private GoodsInProductRetVo goods;

    private Boolean disable;

    private Boolean shareable;

    public ProductWithGoodsRetVo(Product product,Goods goods){
        this.id=product.getId();
        this.name=product.getName();
        this.skuSn=null;
        this.detail=null;
        this.imageUrl=null;
        this.originalPrice=product.getOriginalPrice();
        this.price=null;
        this.inventory=null;
        this.state=product.getProductPo().getState();
        this.weight=product.getWeight();
        this.gmtCreate=null;
        this.gmtModified=null;
        this.disable=null;
        this.shareable=null;
        this.goods=new GoodsInProductRetVo(goods);
    }

    @Override
    public Object createVo() {
        return this;
    }
}
