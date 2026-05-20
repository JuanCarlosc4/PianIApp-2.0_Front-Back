package com.piania.core.exception;

import com.piania.core.controller.UserSettingsController;
import com.piania.core.service.UserSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserSettingsController.class)
@Import({GlobalExceptionHandler.class})
@org.springframework.test.context.ContextConfiguration(
        classes = {
                UserSettingsController.class,
                GlobalExceptionHandler.class
        }
)
class GlobalExceptionHandlerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSettingsService userSettingsService;

    @MockitoBean
    private com.piania.core.mapper.UserSettingsMapper userSettingsMapper;

    @Test
    void notFoundException_isMappedTo404() throws Exception {
        when(userSettingsService.getByUserEmail("a@piania.com"))
                .thenThrow(new NotFoundException("User settings not found"));

        mockMvc.perform(get("/piania/settings").with(user("a@piania.com").roles("USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User settings not found"));
    }

    @Test
    void badRequestException_isMappedTo400() throws Exception {
        when(userSettingsService.getByUserEmail("a@piania.com"))
                .thenThrow(new BadRequestException("Bad request"));

        mockMvc.perform(get("/piania/settings").with(user("a@piania.com").roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Bad request"));
    }

    @Test
    void forbiddenException_isMappedTo403() throws Exception {
        when(userSettingsService.getByUserEmail("a@piania.com"))
                .thenThrow(new ForbiddenException("Forbidden"));

        mockMvc.perform(get("/piania/settings").with(user("a@piania.com").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void validationErrors_areMappedTo400WithErrorsMap() throws Exception {
        mockMvc.perform(put("/piania/settings")
                        .with(user("a@piania.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                        {
                          "language": "",
                          "cookiesAccepted": true,
                          "adsEnabled": true,
                          "darkMode": false
                        }
                        """
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.language").exists());
    }

    @Test
    void genericException_isMappedTo500() throws Exception {
        when(userSettingsService.getByUserEmail("a@piania.com"))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/piania/settings")
                        .with(user("a@piania.com").roles("USER"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("RuntimeException: boom"));
    }
}
