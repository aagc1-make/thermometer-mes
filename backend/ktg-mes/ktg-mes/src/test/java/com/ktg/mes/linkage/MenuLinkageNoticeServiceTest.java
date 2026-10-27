package com.ktg.mes.linkage;

import com.ktg.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MenuLinkageNoticeServiceTest {

    @Mock
    private MenuLinkageNoticeMapper mapper;

    @Test
    void marksConfiguredMenuUnread() {
        MenuLinkageNoticeService service = new MenuLinkageNoticeService(mapper);

        service.markUnread(MenuLinkageNoticePaths.IQC);

        verify(mapper).upsertUnread(MenuLinkageNoticePaths.IQC);
    }

    @Test
    void rejectsUnknownMenuPath() {
        MenuLinkageNoticeService service = new MenuLinkageNoticeService(mapper);

        assertThrows(ServiceException.class, () -> service.markUnread("/not/a/menu"));
    }

    @Test
    void returnsUnreadMenuPaths() {
        MenuLinkageNoticeService service = new MenuLinkageNoticeService(mapper);
        when(mapper.selectUnreadPaths()).thenReturn(Arrays.asList(
                MenuLinkageNoticePaths.IQC, MenuLinkageNoticePaths.OQC));

        assertEquals(Arrays.asList(MenuLinkageNoticePaths.IQC, MenuLinkageNoticePaths.OQC),
                service.listUnreadPaths());
    }

    @Test
    void marksOnlyRequestedMenuRead() {
        MenuLinkageNoticeService service = new MenuLinkageNoticeService(mapper);

        service.markRead(MenuLinkageNoticePaths.ITEM_RECPT);

        verify(mapper).markRead(MenuLinkageNoticePaths.ITEM_RECPT);
    }
}
