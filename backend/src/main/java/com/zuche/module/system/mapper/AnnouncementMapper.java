package com.zuche.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.module.system.dto.AnnouncementQueryDTO;
import com.zuche.module.system.entity.Announcement;
import com.zuche.module.system.vo.AnnouncementStatsVO;
import com.zuche.module.system.vo.AnnouncementVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 公告查询
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {

    IPage<AnnouncementVO> selectAnnouncementPage(Page<AnnouncementVO> page,
                                                 @Param("q") AnnouncementQueryDTO query);

    AnnouncementVO selectAnnouncementById(@Param("id") Long id);

    AnnouncementStatsVO selectStats();

    /** 前台：已发布公告，按发布时间倒序 */
    List<AnnouncementVO> selectPublished(@Param("limit") int limit);

    /** 浏览量 +1：用 UPDATE 自增而不是「读出来 +1 再写回」，并发下不会丢计数 */
    int increaseViewCount(@Param("id") Long id);
}
