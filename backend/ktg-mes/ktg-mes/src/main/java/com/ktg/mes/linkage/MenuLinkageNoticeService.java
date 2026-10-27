package com.ktg.mes.linkage;

import com.ktg.common.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MenuLinkageNoticeService {

    private static final Set<String> CONFIGURED_PATHS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    MenuLinkageNoticePaths.IQC,
                    MenuLinkageNoticePaths.ITEM_RECPT,
                    MenuLinkageNoticePaths.IPQC,
                    MenuLinkageNoticePaths.PRODUCT_SALES,
                    MenuLinkageNoticePaths.OQC)));

    private final MenuLinkageNoticeMapper mapper;

    public MenuLinkageNoticeService(MenuLinkageNoticeMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void markUnread(String menuPath) {
        assertConfigured(menuPath);
        mapper.upsertUnread(menuPath);
    }

    @Transactional(readOnly = true)
    public List<String> listUnreadPaths() {
        return mapper.selectUnreadPaths();
    }

    @Transactional
    public void markRead(String menuPath) {
        assertConfigured(menuPath);
        mapper.markRead(menuPath);
    }

    private void assertConfigured(String menuPath) {
        if (!CONFIGURED_PATHS.contains(menuPath)) {
            throw new ServiceException("未配置的菜单提醒路径");
        }
    }
}
