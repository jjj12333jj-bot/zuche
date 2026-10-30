package com.zuche.module.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.system.dto.BannerQueryDTO;
import com.zuche.module.system.dto.BannerSaveDTO;
import com.zuche.module.system.entity.Banner;
import com.zuche.module.system.mapper.BannerMapper;
import com.zuche.module.system.vo.BannerStatsVO;
import com.zuche.module.system.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 轮播图管理
 *
 * <p>运营只关心两件事：放第几位、什么时候不该出现。所以排序号允许手工填，
 * 不填就自动排到该位置末尾；「隐藏」而不是「删除」是常规动作——
 * 活动结束先把图藏起来，下次还能复用。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BannerService {

    private static final Map<String, String> POSITION_LABEL = Map.of(
            "HOME", "首页",
            "PORTAL", "列表页"
    );

    private static final String DEFAULT_POSITION = "HOME";

    private final BannerMapper bannerMapper;

    /* ==================== 查询 ==================== */

    public PageResult<BannerVO> page(BannerQueryDTO query) {
        Page<BannerVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<BannerVO> result = bannerMapper.selectBannerPage(page, query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result);
    }

    public BannerStatsVO stats() {
        BannerStatsVO vo = bannerMapper.selectStats();
        if (vo == null) {
            vo = new BannerStatsVO();
        }
        vo.setTotal(nz(vo.getTotal()));
        vo.setShowing(nz(vo.getShowing()));
        vo.setHidden(nz(vo.getHidden()));
        return vo;
    }

    public BannerVO detail(Long id) {
        Banner entity = requirement(id);
        return decorate(toVO(entity));
    }

    /* ==================== 写操作 ==================== */

    @Transactional(rollbackFor = Exception.class)
    public BannerVO create(BannerSaveDTO dto) {
        String position = dto.getPosition() == null ? DEFAULT_POSITION : dto.getPosition();
        LocalDateTime now = LocalDateTime.now();

        Banner entity = new Banner();
        apply(dto, entity);
        entity.setPosition(position);
        // 没填排序号就排到该位置最后，避免所有新图都挤在第 0 位
        entity.setSortNo(dto.getSortNo() == null ? bannerMapper.maxSortNo(position) + 1 : dto.getSortNo());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        bannerMapper.insert(entity);

        log.info("新增轮播图: id={}, title={}, position={}, sort={}",
                entity.getId(), entity.getTitle(), position, entity.getSortNo());
        return detail(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public BannerVO update(Long id, BannerSaveDTO dto) {
        Banner exists = requirement(id);
        Banner update = new Banner();
        update.setId(id);
        apply(dto, update);
        update.setPosition(dto.getPosition() == null ? exists.getPosition() : dto.getPosition());
        update.setSortNo(dto.getSortNo() == null ? exists.getSortNo() : dto.getSortNo());
        update.setStatus(dto.getStatus() == null ? exists.getStatus() : dto.getStatus());
        update.setUpdateTime(LocalDateTime.now());
        bannerMapper.updateById(update);

        log.info("编辑轮播图: id={}", id);
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public BannerVO toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0（隐藏）或 1（显示）");
        }
        Banner exists = requirement(id);
        if (Integer.valueOf(status).equals(exists.getStatus())) {
            return detail(id);
        }
        Banner update = new Banner();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        bannerMapper.updateById(update);

        log.info("轮播图状态变更: id={}, {} -> {}", id, exists.getStatus(), status);
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Banner exists = requirement(id);
        bannerMapper.deleteById(id);
        log.info("删除轮播图: id={}, title={}", id, exists.getTitle());
    }

    /* ==================== 前台 ==================== */

    public List<BannerVO> visible(String position) {
        List<BannerVO> list = bannerMapper.selectVisible(position == null || position.isBlank() ? null : position);
        list.forEach(this::decorate);
        return list;
    }

    /* ==================== 内部 ==================== */

    private void apply(BannerSaveDTO dto, Banner target) {
        target.setTitle(dto.getTitle().trim());
        target.setImageUrl(dto.getImageUrl().trim());
        // 空串归一成 null：前端判断「有没有跳转」只看是否为 null，两种空值会各写一套
        String link = dto.getLinkUrl();
        target.setLinkUrl(link == null || link.isBlank() ? null : link.trim());
    }

    private Banner requirement(Long id) {
        Banner exists = bannerMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "轮播图不存在或已删除");
        }
        return exists;
    }

    private BannerVO toVO(Banner entity) {
        BannerVO vo = new BannerVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setImageUrl(entity.getImageUrl());
        vo.setLinkUrl(entity.getLinkUrl());
        vo.setPosition(entity.getPosition());
        vo.setSortNo(entity.getSortNo());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private BannerVO decorate(BannerVO vo) {
        if (vo == null) {
            return null;
        }
        vo.setPositionLabel(label(POSITION_LABEL, vo.getPosition()));
        vo.setStatusLabel(Integer.valueOf(1).equals(vo.getStatus()) ? "显示中" : "已隐藏");
        vo.setSortNo(nz(vo.getSortNo()));
        return vo;
    }

    private static String label(Map<String, String> dict, String code) {
        if (code == null) {
            return null;
        }
        return dict.getOrDefault(code, code);
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
