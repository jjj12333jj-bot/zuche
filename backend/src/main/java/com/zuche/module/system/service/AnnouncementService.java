package com.zuche.module.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.system.dto.AnnouncementQueryDTO;
import com.zuche.module.system.dto.AnnouncementSaveDTO;
import com.zuche.module.system.entity.Announcement;
import com.zuche.module.system.mapper.AnnouncementMapper;
import com.zuche.module.system.vo.AnnouncementStatsVO;
import com.zuche.module.system.vo.AnnouncementVO;
import com.zuche.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 公告管理
 *
 * <p>核心是「草稿 / 发布」两态：运营写公告时先存草稿，确认无误再点发布；
 * 一旦发布就记录发布时间，前台按发布时间倒序展示，重新发布即刷新时间、相当于置顶。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private static final Map<String, String> TYPE_LABEL = Map.of(
            "NOTICE", "公告",
            "ACTIVITY", "活动",
            "RULE", "规则"
    );

    private static final String DEFAULT_TYPE = "NOTICE";

    private final AnnouncementMapper announcementMapper;

    /* ==================== 后台查询 ==================== */

    public PageResult<AnnouncementVO> page(AnnouncementQueryDTO query) {
        Page<AnnouncementVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<AnnouncementVO> result = announcementMapper.selectAnnouncementPage(page, query);
        result.getRecords().forEach(this::decorate);
        return PageResult.of(result);
    }

    public AnnouncementStatsVO stats() {
        AnnouncementStatsVO vo = announcementMapper.selectStats();
        if (vo == null) {
            vo = new AnnouncementStatsVO();
        }
        vo.setTotal(nz(vo.getTotal()));
        vo.setPublished(nz(vo.getPublished()));
        vo.setDraft(nz(vo.getDraft()));
        vo.setViewTotal(vo.getViewTotal() == null ? 0L : vo.getViewTotal());
        return vo;
    }

    public AnnouncementVO detail(Long id) {
        AnnouncementVO vo = announcementMapper.selectAnnouncementById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在或已删除");
        }
        return decorate(vo);
    }

    /* ==================== 后台写操作 ==================== */

    @Transactional(rollbackFor = Exception.class)
    public AnnouncementVO create(AnnouncementSaveDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        Announcement entity = new Announcement();
        entity.setTitle(dto.getTitle().trim());
        entity.setContent(dto.getContent().trim());
        entity.setType(dto.getType() == null ? DEFAULT_TYPE : dto.getType());
        // 不传状态一律按草稿处理，避免「手滑一点就发全站」
        entity.setStatus(Integer.valueOf(1).equals(dto.getStatus()) ? 1 : 0);
        entity.setViewCount(0);
        entity.setPublisherId(UserContext.getUserId());
        entity.setPublishTime(entity.getStatus() == 1 ? now : null);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        announcementMapper.insert(entity);

        log.info("新增公告: id={}, title={}, status={}", entity.getId(), entity.getTitle(), entity.getStatus());
        return detail(entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public AnnouncementVO update(Long id, AnnouncementSaveDTO dto) {
        Announcement exists = requirement(id);

        Announcement update = new Announcement();
        update.setId(id);
        update.setTitle(dto.getTitle().trim());
        update.setContent(dto.getContent().trim());
        update.setType(dto.getType() == null ? exists.getType() : dto.getType());
        update.setStatus(dto.getStatus() == null ? exists.getStatus() : (Integer.valueOf(1).equals(dto.getStatus()) ? 1 : 0));
        // 草稿改发布才补时间；已发布的编辑正文不改变「它是什么时候上线的」
        if (Integer.valueOf(1).equals(update.getStatus()) && exists.getPublishTime() == null) {
            update.setPublishTime(LocalDateTime.now());
        }
        update.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(update);

        log.info("编辑公告: id={}", id);
        return detail(id);
    }

    /**
     * 发布 / 下架
     *
     * <p>下架不清空 publish_time：运营复盘要看「这条挂了多久」；
     * 重新发布则刷新为当前时间，前台按发布时间倒序，效果就是重新置顶。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementVO toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0（下架）或 1（发布）");
        }
        Announcement exists = requirement(id);
        if (Integer.valueOf(status).equals(exists.getStatus())) {
            return detail(id);
        }
        Announcement update = new Announcement();
        update.setId(id);
        update.setStatus(status);
        if (status == 1) {
            update.setPublishTime(LocalDateTime.now());
        }
        update.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(update);

        log.info("公告状态变更: id={}, {} -> {}", id, exists.getStatus(), status);
        return detail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Announcement exists = requirement(id);
        announcementMapper.deleteById(id);
        log.info("删除公告: id={}, title={}", id, exists.getTitle());
    }

    /* ==================== 前台 ==================== */

    /** 前台公告栏：只取已发布，默认 5 条，最多 20 条 */
    public List<AnnouncementVO> published(Integer limit) {
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 20);
        List<AnnouncementVO> list = announcementMapper.selectPublished(size);
        list.forEach(this::decorate);
        return list;
    }

    /**
     * 前台详情：只对已发布公告计数
     *
     * <p>先校验再自增。反过来的话，草稿或已删除的公告每被访问一次也会涨浏览量，
     * 运营看到「草稿有 300 浏览」会以为发出去过。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public AnnouncementVO publicDetail(Long id) {
        AnnouncementVO vo = announcementMapper.selectAnnouncementById(id);
        if (vo == null || !Integer.valueOf(1).equals(vo.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在或已下架");
        }
        announcementMapper.increaseViewCount(id);
        // 内存里补上这一次，避免再查一遍库
        vo.setViewCount(nz(vo.getViewCount()) + 1);
        return decorate(vo);
    }

    /* ==================== 内部 ==================== */

    private Announcement requirement(Long id) {
        Announcement exists = announcementMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在或已删除");
        }
        return exists;
    }

    private AnnouncementVO decorate(AnnouncementVO vo) {
        if (vo == null) {
            return null;
        }
        vo.setTypeLabel(label(TYPE_LABEL, vo.getType()));
        vo.setStatusLabel(Integer.valueOf(1).equals(vo.getStatus()) ? "已发布" : "草稿");
        vo.setViewCount(nz(vo.getViewCount()));
        return vo;
    }

    /** 空安全字典翻译：字典里没有的码原样返回，为 null 时返回 null */
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
