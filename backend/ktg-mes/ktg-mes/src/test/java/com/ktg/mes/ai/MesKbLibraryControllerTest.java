package com.ktg.mes.ai;

import com.ktg.common.core.domain.entity.SysRole;
import com.ktg.common.core.domain.entity.SysUser;
import com.ktg.common.core.domain.model.LoginUser;
import com.ktg.framework.web.exception.GlobalExceptionHandler;
import com.ktg.framework.web.exception.ForbiddenRequestAudit;
import com.ktg.framework.web.service.PermissionService;
import com.ktg.mes.ai.controller.MesKbLibraryController;
import com.ktg.mes.ai.domain.KbLibrarySummary;
import com.ktg.mes.ai.mapper.KbLibraryMapper;
import com.ktg.mes.ai.service.KbLibraryService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class MesKbLibraryControllerTest {
    private static final String LIBRARIES = "/mes/ai/kb/libraries";
    private AnnotationConfigWebApplicationContext context;
    private KbLibraryMapper mapper;
    private MockMvc mvc;

    @Before
    public void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(WebConfig.class);
        context.refresh();
        mapper = context.getBean(KbLibraryMapper.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @After
    public void cleanup() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    public void listUsesAuthenticatedUserAndReturnsEmptyForUnassignedUsers() throws Exception {
        KbLibrarySummary library = new KbLibrarySummary();
        library.setLibraryId(9L);
        library.setDeptId(5L);
        library.setDeptName("质量部");
        library.setName("质量资料");
        when(mapper.selectVisibleByUserId(28L)).thenReturn(Collections.singletonList(library));

        login(28L, "employee");
        mvc.perform(get(LIBRARIES))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].libraryId").value(9))
            .andExpect(jsonPath("$.data[0].deptId").value(5))
            .andExpect(jsonPath("$.data[0].deptName").value("质量部"))
            .andExpect(jsonPath("$.data[0].name").value("质量资料"));

        login(1L, "admin");
        mvc.perform(get(LIBRARIES)).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    public void adminCannotSupplyAnotherDepartmentWhenListing() throws Exception {
        login(1L, "admin");
        mvc.perform(get(LIBRARIES).param("deptId", "5")).andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(mapper);
        verify(context.getBean(ForbiddenRequestAudit.class)).record(any(), eq("只能查看当前账号所属部门的知识库"));
    }

    @Test
    public void administratorCanReadManagementMetadataWhileMemberCannot() throws Exception {
        KbLibrarySummary summary = new KbLibrarySummary();
        summary.setDeptId(5L);
        summary.setName("质量资料");
        when(mapper.selectAdminSummaries()).thenReturn(Collections.singletonList(summary));

        login(28L, "employee");
        mvc.perform(get(LIBRARIES + "/admin")).andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(mapper);

        login(1L, "admin");
        mvc.perform(get(LIBRARIES + "/admin"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].deptId").value(5))
            .andExpect(jsonPath("$.data[0].name").value("质量资料"));
        verify(mapper).selectAdminSummaries();
    }

    @Test
    public void anonymousCannotListLibraries() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "test", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        mvc.perform(get(LIBRARIES)).andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(mapper);
    }

    @Test
    public void regularEmployeeCannotCreateLibrary() throws Exception {
        login(28L, "employee");
        create(5, "质量资料").andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(mapper);
        verify(context.getBean(ForbiddenRequestAudit.class)).record(any(), any());
    }

    @Test
    public void adminCanCreateForActiveDepartmentAndReceivesConflictOnDuplicate() throws Exception {
        when(mapper.insertForActiveDepartment(5L, "质量资料"))
            .thenReturn(1)
            .thenThrow(new DuplicateKeyException("unique department"));

        login(1L, "admin");
        create(5, "  质量资料  ").andExpect(jsonPath("$.code").value(200));
        create(5, "质量资料").andExpect(jsonPath("$.code").value(409));
    }

    @Test
    public void adminCannotCreateForAMissingOrInactiveDepartment() throws Exception {
        login(1L, "admin");
        create(5, "质量资料").andExpect(jsonPath("$.msg").value("部门不存在或已停用"));
    }

    @Test
    public void invalidInputDoesNotReachDatabase() throws Exception {
        login(1L, "admin");
        create(0, "质量资料").andExpect(jsonPath("$.code").value(400));
        create(5, "   ").andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(mapper);
    }

    private ResultActions create(long deptId, String name) throws Exception {
        return mvc.perform(post(LIBRARIES).contentType(MediaType.APPLICATION_JSON)
            .content("{\"deptId\":" + deptId + ",\"name\":\"" + name + "\"}"));
    }

    private static void login(Long userId, String roleKey) {
        SysRole role = new SysRole();
        role.setRoleKey(roleKey);
        SysUser user = new SysUser(userId);
        user.setRoles(Collections.singletonList(role));
        LoginUser principal = new LoginUser(userId, 99L, user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            principal, null, Collections.emptyList()));
    }

    @Configuration
    @EnableWebMvc
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    @Import({MesKbLibraryController.class, KbLibraryService.class, PermissionService.class, GlobalExceptionHandler.class})
    public static class WebConfig {
        @Bean
        public ForbiddenRequestAudit forbiddenRequestAudit() {
            return mock(ForbiddenRequestAudit.class);
        }

        @Bean
        public KbLibraryMapper kbLibraryMapper() {
            return mock(KbLibraryMapper.class);
        }
    }
}
