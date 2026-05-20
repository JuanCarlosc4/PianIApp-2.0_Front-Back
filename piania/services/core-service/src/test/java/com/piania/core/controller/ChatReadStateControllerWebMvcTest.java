package com.piania.core.controller;

import com.piania.core.service.ChatReadStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatReadStateController.class)
@Import(com.piania.core.config.SecurityConfig.class)
class ChatReadStateControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatReadStateService chatReadStateService;

    @MockBean
    private com.piania.core.security.InternalAuthenticationFilter internalAuthenticationFilter;

    @MockBean
    private com.piania.core.security.AuditFilter auditFilter;

    @MockBean
    private com.piania.core.security.CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockBean
    private com.piania.core.security.CustomAccessDeniedHandler customAccessDeniedHandler;

    @Test
    void getMyState_returns200() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user@example.com", "N/A"));

        when(chatReadStateService.getMyState(eq(1L), eq("user@example.com")))
                .thenReturn(new com.piania.core.dto.chat.ChatReadStateResponse(1L, "user@example.com", 10L));

        mockMvc.perform(get("/chat-read-state/me/1")
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
