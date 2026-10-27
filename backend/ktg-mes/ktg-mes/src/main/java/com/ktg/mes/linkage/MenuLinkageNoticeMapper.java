package com.ktg.mes.linkage;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MenuLinkageNoticeMapper {

    int upsertUnread(@Param("menuPath") String menuPath);

    List<String> selectUnreadPaths();

    int markRead(@Param("menuPath") String menuPath);
}
