package com.zuche.module.system.service;

import com.zuche.module.system.entity.SysMenu;
import com.zuche.module.system.mapper.SysMenuMapper;
import com.zuche.module.system.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单服务：把扁平菜单组装成树
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;

    public List<MenuVO> menuTree(Long userId) {
        List<SysMenu> flat = menuMapper.selectMenusByUserId(userId);

        Map<Long, MenuVO> index = new LinkedHashMap<>();
        for (SysMenu menu : flat) {
            MenuVO vo = new MenuVO();
            vo.setId(menu.getId());
            vo.setParentId(menu.getParentId());
            vo.setMenuName(menu.getMenuName());
            vo.setMenuType(menu.getMenuType());
            vo.setPath(menu.getPath());
            vo.setComponent(menu.getComponent());
            vo.setPerms(menu.getPerms());
            vo.setIcon(menu.getIcon());
            vo.setSortNo(menu.getSortNo());
            index.put(vo.getId(), vo);
        }

        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO vo : index.values()) {
            MenuVO parent = vo.getParentId() == null ? null : index.get(vo.getParentId());
            if (parent == null) {
                // parentId=0 或父菜单未授权时，作为根节点展示，避免菜单丢失
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
            }
        }
        sort(roots);
        return roots;
    }

    private void sort(List<MenuVO> nodes) {
        nodes.sort(Comparator.comparing(MenuVO::getSortNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuVO::getId));
        nodes.forEach(node -> sort(node.getChildren()));
    }
}
