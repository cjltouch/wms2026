package com.example.wms.business.basedata.brand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.brand.dto.req.BrandPageReq;
import com.example.wms.business.basedata.brand.dto.req.BrandSaveReq;
import com.example.wms.business.basedata.brand.entity.WmsBrand;
import com.example.wms.business.basedata.brand.mapper.WmsBrandMapper;
import com.example.wms.business.basedata.brand.service.WmsBrandService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WmsBrandServiceImpl extends ServiceImpl<WmsBrandMapper, WmsBrand> implements WmsBrandService {

    @Override
    public PageRsp<WmsBrand> pageBrand(BrandPageReq req) {
        LambdaQueryWrapper<WmsBrand> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getBrandName())) {
            wrapper.like(WmsBrand::getBrandName, req.getBrandName());
        }
        if (StringUtils.hasText(req.getBrandCode())) {
            wrapper.like(WmsBrand::getBrandCode, req.getBrandCode());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsBrand::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(WmsBrand::getSort);
        Page<WmsBrand> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public List<WmsBrand> listAll() {
        return this.list(new LambdaQueryWrapper<WmsBrand>()
                .eq(WmsBrand::getStatus, "0")
                .orderByAsc(WmsBrand::getSort));
    }

    @Override
    public void saveBrand(BrandSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsBrand::getBrandCode, req.getBrandCode())
                .count();
        if (count > 0) {
            throw new BizException("品牌编码已存在");
        }
        WmsBrand brand = new WmsBrand();
        brand.setBrandName(req.getBrandName());
        brand.setBrandCode(req.getBrandCode());
        brand.setFirstLetter(req.getFirstLetter());
        brand.setLogo(req.getLogo());
        brand.setBanner(req.getBanner());
        brand.setDescription(req.getDescription());
        brand.setSort(req.getSort() == null ? 0 : req.getSort());
        brand.setStatus(req.getStatus() == null ? "0" : req.getStatus());
        this.save(brand);
    }

    @Override
    public void updateBrand(BrandSaveReq req) {
        if (req.getBrandId() == null) {
            throw new BizException("品牌ID不能为空");
        }
        WmsBrand exist = this.getById(req.getBrandId());
        if (exist == null) {
            throw new BizException("品牌不存在");
        }
        long count = this.lambdaQuery()
                .eq(WmsBrand::getBrandCode, req.getBrandCode())
                .ne(WmsBrand::getBrandId, req.getBrandId())
                .count();
        if (count > 0) {
            throw new BizException("品牌编码已存在");
        }
        exist.setBrandName(req.getBrandName());
        exist.setBrandCode(req.getBrandCode());
        exist.setFirstLetter(req.getFirstLetter());
        exist.setLogo(req.getLogo());
        exist.setBanner(req.getBanner());
        exist.setDescription(req.getDescription());
        exist.setSort(req.getSort() == null ? 0 : req.getSort());
        exist.setStatus(req.getStatus());
        this.updateById(exist);
    }

    @Override
    public void deleteBrand(Long brandId) {
        this.removeById(brandId);
    }

    @Override
    public void batchDeleteBrand(List<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return;
        }
        this.removeByIds(brandIds);
    }
}
