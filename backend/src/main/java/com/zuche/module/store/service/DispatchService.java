package com.zuche.module.store.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zuche.common.BusinessException;
import com.zuche.common.PageResult;
import com.zuche.common.ResultCode;
import com.zuche.module.store.dto.DispatchAuditDTO;
import com.zuche.module.store.dto.DispatchCreateDTO;
import com.zuche.module.store.dto.DispatchQueryDTO;
import com.zuche.module.store.entity.DispatchApply;
import com.zuche.module.store.mapper.DispatchApplyMapper;
import com.zuche.module.store.mapper.StoreVehicleMapper;
import com.zuche.module.store.vo.DispatchVO;
import com.zuche.module.vehicle.entity.Vehicle;
import com.zuche.module.vehicle.vo.StoreOptionVO;
import com.zuche.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 调车申请与审批
 *
 * <p><b>流程</b>：网点发起（车在自己店里）→ 平台管理员审批 → 通过则车辆归属迁移。</p>
 *
 * <p>为什么审批权在平台而不是「调入方点同意就行」：
 * 车是调出方的资产，两个网点私下互相同意就能搬车，等于把资产台账交给了一线；
 * 而且调车往往伴随运力失衡，平台需要在全局层面拒绝「把车从缺车店调走」这种申请。
 * 所以申请由调出方提，审批权收在 SYS_ADMIN。</p>
 *
 * <p><b>状态机</b>：PENDING → APPROVED / REJECTED / CANCELLED，终态不可再变。</p>
 */
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String CANCELLED = "CANCELLED";

    private static final Map<String, String> STATUS_LABELS = Map.of(
            PENDING, "待审批",
            APPROVED, "已通过",
            REJECTED, "已驳回",
            CANCELLED, "已撤回");

    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StoreVehicleMapper storeVehicleMapper;
    private final DispatchApplyMapper dispatchApplyMapper;

    /* ==================== 网点端 ==================== */

    /** 本网点相关申请：direction=OUT 我调出的，IN 调入我的，为空两个方向都看 */
    public PageResult<DispatchVO> pageOfStore(DispatchQueryDTO query) {
        Long storeId = StoreContext.requireStoreId();
        return doPage(storeId, query, false);
    }

    /** 平台端：全部申请 */
    public PageResult<DispatchVO> pageOfAdmin(DispatchQueryDTO query) {
        return doPage(null, query, true);
    }

    /** 可调入的兄弟网点（排除自己） */
    public List<StoreOptionVO> otherStores() {
        return storeVehicleMapper.selectOtherStores(StoreContext.requireStoreId());
    }

    /** 发起调车申请 */
    @Transactional(rollbackFor = Exception.class)
    public String create(DispatchCreateDTO dto) {
        Long storeId = StoreContext.requireStoreId();
        Long userId = StoreContext.requireUserId();

        if (storeId.equals(dto.getToStoreId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "调入网点不能是本网点");
        }
        if (storeVehicleMapper.selectOtherStores(storeId).stream()
                .noneMatch(s -> s.getId().equals(dto.getToStoreId()))) {
            throw new BusinessException(ResultCode.NOT_FOUND, "调入网点不存在或已停用");
        }

        Vehicle vehicle = storeVehicleMapper.selectVehicleForUpdate(dto.getVehicleId());
        if (vehicle == null || !storeId.equals(vehicle.getStoreId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "本网点不存在该车辆");
        }
        if (vehicle.getStatus() == null || vehicle.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "只有「可租」状态的车辆才能发起调车，当前状态：" + vehicleStatusText(vehicle.getStatus()));
        }
        if (storeVehicleMapper.countPendingApply(vehicle.getId()) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车辆已有待审批的调车申请，请等待平台处理或先撤回");
        }

        DispatchApply apply = new DispatchApply();
        apply.setApplyNo(nextApplyNo());
        apply.setVehicleId(vehicle.getId());
        apply.setFromStoreId(storeId);
        apply.setToStoreId(dto.getToStoreId());
        apply.setApplyUserId(userId);
        apply.setReason(dto.getReason().trim());
        apply.setStatus(PENDING);
        dispatchApplyMapper.insert(apply);

        return "调车申请已提交（单号 " + apply.getApplyNo() + "），等待平台审批";
    }

    /** 撤回：只有发起网点、且还在待审批时才能撤 */
    @Transactional(rollbackFor = Exception.class)
    public String cancel(Long id) {
        Long storeId = StoreContext.requireStoreId();
        DispatchApply apply = dispatchApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "调车申请不存在");
        }
        if (!storeId.equals(apply.getFromStoreId()) && !storeId.equals(apply.getToStoreId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "本网点不存在该调车申请");
        }
        if (!PENDING.equals(apply.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该申请已处理（" + dispatchStatusText(apply.getStatus()) + "），无法撤回");
        }

        DispatchApply update = new DispatchApply();
        update.setId(id);
        update.setStatus(CANCELLED);
        dispatchApplyMapper.updateById(update);
        return "调车申请 " + apply.getApplyNo() + " 已撤回";
    }

    /* ==================== 平台端 ==================== */

    /**
     * 审批通过：迁移车辆归属
     *
     * <p>迁移用 CAS 写法（{@code store_id = from AND status = 1}），
     * 审批期间车要是被租走了，影响行数为 0，这里直接拒绝，
     * 而不是把一辆正在开的车搬到别的网点。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public String approve(Long id, DispatchAuditDTO dto) {
        DispatchApply apply = requirePending(id);

        Vehicle vehicle = storeVehicleMapper.selectVehicleForUpdate(apply.getVehicleId());
        if (vehicle == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申请对应的车辆已不存在");
        }
        if (!apply.getFromStoreId().equals(vehicle.getStoreId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车辆已不在调出网点名下，申请自动失效，请驳回后由网点重新发起");
        }
        if (vehicle.getStatus() == null || vehicle.getStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该车辆当前为「" + vehicleStatusText(vehicle.getStatus()) + "」，不能调拨");
        }
        int moved = storeVehicleMapper.moveToStore(vehicle.getId(), apply.getFromStoreId(), apply.getToStoreId());
        if (moved == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "车辆状态已变化，请刷新后重新审批");
        }

        DispatchApply update = new DispatchApply();
        update.setId(id);
        update.setStatus(APPROVED);
        update.setAuditUserId(UserContext.requireUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(trimToNull(dto == null ? null : dto.getRemark()));
        dispatchApplyMapper.updateById(update);

        return "已通过：" + vehicle.getLicensePlate() + " 归属由网点 " + apply.getFromStoreId()
                + " 迁移至 " + apply.getToStoreId();
    }

    /** 审批驳回：原因必填——不写理由的驳回等于让网点无限重提 */
    @Transactional(rollbackFor = Exception.class)
    public String reject(Long id, DispatchAuditDTO dto) {
        DispatchApply apply = requirePending(id);
        String remark = trimToNull(dto == null ? null : dto.getRemark());
        if (remark == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "驳回必须填写原因，便于网点调整后再提");
        }

        DispatchApply update = new DispatchApply();
        update.setId(id);
        update.setStatus(REJECTED);
        update.setAuditUserId(UserContext.requireUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(remark);
        dispatchApplyMapper.updateById(update);

        return "已驳回调车申请 " + apply.getApplyNo();
    }

    /* ==================== 内部 ==================== */

    private PageResult<DispatchVO> doPage(Long storeId, DispatchQueryDTO query, boolean forAdmin) {
        query.setKeyword(trimToNull(query.getKeyword()));
        if (query.getDirection() != null && !query.getDirection().isBlank()) {
            String direction = query.getDirection().trim().toUpperCase();
            query.setDirection("OUT".equals(direction) || "IN".equals(direction) ? direction : null);
        } else {
            query.setDirection(null);
        }

        Page<DispatchVO> page = new Page<>(query.getCurrent(), query.getPageSize());
        IPage<DispatchVO> result = storeVehicleMapper.selectDispatchPage(page, storeId, query);
        result.getRecords().forEach(vo -> {
            vo.setStatusLabel(STATUS_LABELS.getOrDefault(vo.getStatus(), vo.getStatus()));
            boolean pending = PENDING.equals(vo.getStatus());
            vo.setCanAudit(forAdmin && pending);
            vo.setCanCancel(!forAdmin && pending && storeId != null && storeId.equals(vo.getFromStoreId()));
        });
        return PageResult.of(result);
    }

    private static String dispatchStatusText(String status) {
        return STATUS_LABELS.getOrDefault(status, status);
    }

    private DispatchApply requirePending(Long id) {
        DispatchApply apply = dispatchApplyMapper.selectById(id);
        if (apply == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "调车申请不存在");
        }
        if (!PENDING.equals(apply.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该申请已处理（" + dispatchStatusText(apply.getStatus()) + "），不能重复审批");
        }
        return apply;
    }

    /** D + yyyyMMdd + 4 位流水，唯一索引兜底重复 */
    private String nextApplyNo() {
        String prefix = "D" + LocalDate.now().format(NO_DATE);
        String max = storeVehicleMapper.selectMaxApplyNo(prefix);
        int seq = 1;
        if (max != null && max.length() > prefix.length()) {
            try {
                seq = Integer.parseInt(max.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private static String vehicleStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "已下架";
            case 1 -> "可租";
            case 2 -> "已租出";
            case 3 -> "维保中";
            default -> "未知";
        };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
