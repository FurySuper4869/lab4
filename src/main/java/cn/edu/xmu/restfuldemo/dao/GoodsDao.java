package cn.edu.xmu.restfuldemo.dao;

import cn.edu.xmu.restfuldemo.mapper.GoodsMapper;
import cn.edu.xmu.restfuldemo.model.*;
import cn.edu.xmu.restfuldemo.util.RedisUtil;
import cn.edu.xmu.restfuldemo.util.ResponseCode;
import cn.edu.xmu.restfuldemo.util.ReturnObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static cn.edu.xmu.restfuldemo.util.Common.*;
/**
 * @author Ming Qiu
 **/
@Repository
public class GoodsDao {

    private Logger logger = LoggerFactory.getLogger(GoodsDao.class);

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${restfuldemo.goods.expiretime}")
    private long goodsTimeout;

    /**
     * 用GoodsPo对象找Goods对象
     * @param goodsPo 条件对象，所有条件为AND，仅有索引的值可以作为条件
     * @param withProduct 是否带关联的Product（有性能问题，不要一次返回太多Goods对象）
     * @return  Goods对象列表，带关联的Product返回
     */
    public ReturnObject<List<Goods>> findGoods(GoodsPo goodsPo, Boolean withProduct){
        logger.info("findGoods: goodsPo =" + goodsPo+" withProduct = "+withProduct);

        List<Goods> retGoods = null;
        String key = null;
        if (null != goodsPo.getId() && withProduct){
            key = "g_"+goodsPo.getId();
            Goods goods = (Goods) redisUtil.get(key);
            if (null != goods){
                logger.info("findGoods: hit redis cache, key = "+key);
                retGoods = new ArrayList<>(1);
                retGoods.add(goods);
                return new ReturnObject<>(retGoods);
            }
        }

        List<GoodsPo> goodsPos = goodsMapper.findGoods(goodsPo);
        logger.info("findGoods: goodsPos =" + goodsPos);
        retGoods = new ArrayList<>(goodsPos.size());
        ProductPo productPo = null;

        if (withProduct) {
            productPo = new ProductPo();
        }

        for (GoodsPo goodsItem : goodsPos) {
            Goods item = new Goods(goodsItem);
            if (withProduct) {
                productPo.setGoodsId(goodsItem.getId());
                List<ProductPo> productPos = goodsMapper.findProduct(productPo);
                List<Product> productList = new ArrayList<>(productPos.size());
                for (ProductPo productItem : productPos) {
                    Product product = new Product(productItem);
                    product = getEffectivePrice(product);
                    productList.add(product);
                }
                item.setProductList(productList);
            }
            retGoods.add(item);
        }

        if (null != goodsPo.getId() && withProduct){
            logger.info("findGoods: put into redis cache, key = "+key);
            if (retGoods.size() != 0) {
                redisUtil.set(key, retGoods.get(0), goodsTimeout);
            }else{
                redisUtil.set(key, null, goodsTimeout);
            }
        }

        logger.info("findGoods: retGoods = "+retGoods +", withProduct ="+withProduct);
        return new ReturnObject<>(retGoods);
    }

    /**
     * 用GoodsPo对象找Goods对象
     * @param goodsPo 条件对象，所有条件为AND，仅有索引的值可以作为条件
     * @return  Goods对象列表，带关联的Product返回
     */
    public ReturnObject<List<Goods>> findGoodsWithProduct(GoodsPo goodsPo){
        List<GoodsPo> goodsPos = goodsMapper.findGoodsWithProduct(goodsPo);
        List<Goods> retGoods = new ArrayList<>(goodsPos.size());
        for (GoodsPo goodsItem : goodsPos){
            List<ProductPo> productPos = goodsItem.getProductList();
            Goods item = new Goods(goodsItem);
            List<Product> productList = new ArrayList<>(productPos.size());
            for (ProductPo productItem : productPos) {
                Product product = new Product(productItem);
                product = getEffectivePrice(product);
                productList.add(product);
            }
            item.setProductList(productList);
            retGoods.add(item);
        }
        return new ReturnObject<>(retGoods);
    }

    /**
     * 获得规格的当前有效价格和库存
     * @param product 规格对象
     * @return 规格对象
     */
    private Product getEffectivePrice(Product product){
        List<PriceStockPo> priceList = goodsMapper.findEffectPrice(product.getId());
        if (priceList.size()!=0){
            PriceStockPo priceStockPo = priceList.get(0);
            product.setPriceStockPo(priceStockPo);
        }
        return product;
    }
    /**
     * 创建Goods对象
     * @param goods 传入的Goods对象
     * @return 返回对象ReturnObj
     */
    public ReturnObject<Goods> createGoods(Goods goods){
        GoodsPo goodsPo = goods.gotGoodsPo();
        String seqNum = genSeqNum();
        goodsPo.setGoodsSn("G"+seqNum);
        int ret = goodsMapper.createGoods(goodsPo);
        if (goods.getProductList() != null) {
            for (Product product : goods.getProductList()) {
                ProductPo productPo = product.getProductPo();
                productPo.setProductSn("P"+seqNum+"-"+productPo.getProductSn());
                productPo.setGoodsId(goodsPo.getId());
                ret = goodsMapper.createProduct(productPo);
            }
        }
        ReturnObject<Goods> returnObject = new ReturnObject<>(goods);
        return returnObject;
    }

    /**
     * 修改商品信息
     * @param goods 传入的Goods对象
     * @return 返回对象ReturnObj
     */
    public ReturnObject<Object> modiGoods(Goods goods){
        GoodsPo goodsPo = goods.gotGoodsPo();
        ReturnObject<Object> retObj = null;
        int ret = goodsMapper.updateGoods(goodsPo);
        redisUtil.del("g_"+goods.getId());
        if (ret == 0 ){
            retObj = new ReturnObject<>(ResponseCode.RESOURCE_ID_NOTEXIST);
        } else {
            retObj = new ReturnObject<>();
        }
        return retObj;
    }

    /**
     * 删除商品，连带规格
     * @param id 商品id
     * @return
     */
    public ReturnObject<Object> deleteGoods(Integer id) {
        ReturnObject<Object> retObj = null;
        int ret = goodsMapper.deleteGoods(id);
        redisUtil.del("g_"+id);
        goodsMapper.deleteProductByGoodsId(id);
        if (ret == 0) {
            retObj = new ReturnObject<>(ResponseCode.RESOURCE_ID_NOTEXIST);
        } else {
            retObj = new ReturnObject<>();
        }
        return retObj;
    }

    public ReturnObject<List<ProductWithGoodsRetVo>> findProductWithGoods(ProductPo productPo){
        List<ProductPo> productPos=goodsMapper.findProduct(productPo);

        List<ProductWithGoodsRetVo> retProductWithGoods=new ArrayList<>(productPos.size());


        GoodsPo goodsPo=new GoodsPo();
        //goodsPo.setId(productPos.get(0).getGoodsId());

        for(ProductPo productItem:productPos){
            Product item=new Product(productItem);
            goodsPo.setId(item.getGoodsId());
            List<GoodsPo> goodsPos=goodsMapper.findGoods(goodsPo);
            Goods goods=null;
            if(goodsPos.size()!=0)
                goods=new Goods(goodsPos.get(0));
            //Goods goods=new Goods(goodsMapper.findGoods(goodsPo).get(0));
            retProductWithGoods.add(new ProductWithGoodsRetVo(item,goods));
        }

        return new ReturnObject<>(retProductWithGoods);
    }

    public ReturnObject<List<ProductWithGoodsRetVo>> findProductWithGoodsWithRedis(ProductPo productPo){
        List<ProductWithGoodsRetVo> retProductWithGoods=null;

        String key1="p_"+productPo.getId();
        ProductWithGoodsRetVo productWithGoodsRetVo=(ProductWithGoodsRetVo)redisUtil.get(key1);
        if(productWithGoodsRetVo!=null){
            retProductWithGoods=new ArrayList<>(1);
            retProductWithGoods.add(productWithGoodsRetVo);
            return new ReturnObject<>(retProductWithGoods);
        }

        List<ProductPo> productPos=goodsMapper.findProduct(productPo);
        retProductWithGoods=new ArrayList<>(productPos.size());

        GoodsPo goodsPo=new GoodsPo();
        //goodsPo.setId(productPos.get(0).getGoodsId());

        for(ProductPo productItem:productPos){
            Product item=new Product(productItem);

            String key2="pg_"+item.getGoodsId();
            Goods goods=(Goods)redisUtil.get(key2);
            if(goods!=null){
                retProductWithGoods.add(new ProductWithGoodsRetVo(item,goods));
                continue;
            }

            goodsPo.setId(item.getGoodsId());
            List<GoodsPo> goodsPos=goodsMapper.findGoods(goodsPo);

            if(goodsPos.size()!=0)
                goods=new Goods(goodsPos.get(0));

            if(goods!=null)
                redisUtil.set(key2,goods,goodsTimeout);
            else
                redisUtil.set(key2,null,goodsTimeout);

            //Goods goods=new Goods(goodsMapper.findGoods(goodsPo).get(0));
            retProductWithGoods.add(new ProductWithGoodsRetVo(item,goods));
        }

        if(retProductWithGoods.size()!=0)
            redisUtil.set(key1,retProductWithGoods.get(0),goodsTimeout);
        else
            redisUtil.set(key1,null,goodsTimeout);

        return new ReturnObject<>(retProductWithGoods);
    }

}
