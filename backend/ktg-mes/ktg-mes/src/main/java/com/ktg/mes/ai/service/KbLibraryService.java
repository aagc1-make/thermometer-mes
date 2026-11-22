package com.ktg.mes.ai.service;

import com.ktg.common.constant.HttpStatus;
import com.ktg.common.exception.ServiceException;
import com.ktg.mes.ai.domain.KbLibrarySummary;
import com.ktg.mes.ai.mapper.KbLibraryMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KbLibraryService {
    private final KbLibraryMapper libraryMapper;

    public KbLibraryService(KbLibraryMapper libraryMapper) {
        this.libraryMapper = libraryMapper;
    }

    public List<KbLibrarySummary> listVisible(Long userId) {
        return libraryMapper.selectVisibleByUserId(userId);
    }

    public List<KbLibrarySummary> listAdminSummaries() {
        return libraryMapper.selectAdminSummaries();
    }

    public void create(Long deptId, String name) {
        if (deptId == null || deptId <= 0) {
            throw new ServiceException("请选择有效部门", HttpStatus.BAD_REQUEST);
        }
        String libraryName = name == null ? "" : name.trim();
        if (libraryName.isEmpty() || libraryName.codePointCount(0, libraryName.length()) > 120) {
            throw new ServiceException("知识库名称须为 1 至 120 个字符", HttpStatus.BAD_REQUEST);
        }

        try {
            if (libraryMapper.insertForActiveDepartment(deptId, libraryName) == 0) {
                throw new ServiceException("部门不存在或已停用", HttpStatus.NOT_FOUND);
            }
        } catch (DuplicateKeyException e) {
            throw new ServiceException("该部门已有知识库", HttpStatus.CONFLICT);
        }
    }
}
