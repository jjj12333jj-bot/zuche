package com.zuche.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.system.dto.BannerQueryDTO;
import com.zuche.module.system.entity.Banner;
import com.zuche.module.system.vo.BannerStatsVO;
import com.zuche.module.system.vo.BannerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 轮播图查询
 */
@Mapper
public interface BannerMapper extends BaseMapper<Banner> {

    IPage<BannerVO> selectBannerPage(Page<BannerVO> page, @Param("q") BannerQueryDTO query);

    BannerStatsVO selectStats();

    /** 前台：某位置下显示中的轮播图，按 sort_no 升序 */
    List<BannerVO> selectVisible(@Param("position") String position);

    /** 该位置当前最大排序号，新增时自动排到末尾 */
    int maxSortNo(@Param("position") String position);
}
