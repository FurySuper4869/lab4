package cn.edu.xmu.restfuldemo.controller;

import cn.edu.xmu.restfuldemo.model.GoodsRetVo;
import cn.edu.xmu.restfuldemo.model.ProductWithGoodsRetVo;
import cn.edu.xmu.restfuldemo.model.VoObject;
import cn.edu.xmu.restfuldemo.service.GoodsService;
import cn.edu.xmu.restfuldemo.util.ResponseCode;
import cn.edu.xmu.restfuldemo.util.ResponseUtil;
import cn.edu.xmu.restfuldemo.util.ReturnObject;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@Api(value = "货品API", tags = "货品API")
@RestController /*Restful的Controller对象*/
@RequestMapping(value = "/products", produces = "application/json;charset=UTF-8")
public class ProductController {

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private HttpServletResponse httpServletResponse;

    @GetMapping("2/{id}")
    public Object getProducrById_NoRedis(@PathVariable("id") Integer id) {
        ReturnObject<VoObject> returnObject =  goodsService.findProductWithGoods(id);
        ResponseCode code = returnObject.getCode();
        switch (code){
            case RESOURCE_ID_NOTEXIST:
                httpServletResponse.setStatus(HttpStatus.NOT_FOUND.value());
                return ResponseUtil.fail(returnObject.getCode(), returnObject.getErrmsg());
            case OK:
                ProductWithGoodsRetVo productWithGoodsRetVo = (ProductWithGoodsRetVo) returnObject.getData().createVo();
                return ResponseUtil.ok(productWithGoodsRetVo);
            default:
                return ResponseUtil.fail(code);
        }
    }

    @GetMapping("1/{id}")
    public Object getProducrById_Redis(@PathVariable("id") Integer id) {
        ReturnObject<VoObject> returnObject =  goodsService.findProductWithGoodsWithRedis(id);
        ResponseCode code = returnObject.getCode();
        switch (code){
            case RESOURCE_ID_NOTEXIST:
                httpServletResponse.setStatus(HttpStatus.NOT_FOUND.value());
                return ResponseUtil.fail(returnObject.getCode(), returnObject.getErrmsg());
            case OK:
                ProductWithGoodsRetVo productWithGoodsRetVo = (ProductWithGoodsRetVo) returnObject.getData().createVo();
                return ResponseUtil.ok(productWithGoodsRetVo);
            default:
                return ResponseUtil.fail(code);
        }
    }
}