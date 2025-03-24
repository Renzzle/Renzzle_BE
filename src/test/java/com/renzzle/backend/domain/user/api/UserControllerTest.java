package com.renzzle.backend.domain.user.api;

import com.renzzle.backend.domain.user.api.response.UserResponse;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.domain.user.service.UserService;
import com.renzzle.backend.global.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getUserTest() throws Exception {
        // set user information in SecurityContext
        UserEntity userEntity = UserEntity.builder().id(1L).build();
        UserDetailsImpl userDetails = new UserDetailsImpl(userEntity, "", new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        UserResponse userResponse = new UserResponse(
                1L,
                "tintintest46@mail.com",
                "tintin",
                1300
        );
        Mockito.when(userService.getUserResponse(Mockito.eq(userEntity))).thenReturn(userResponse);

        // GET /api/user request test by using MockMvc
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.id").value(1L))
                .andExpect(jsonPath("$.response.email").value("tintintest46@mail.com"))
                .andExpect(jsonPath("$.response.nickname").value("tintin"));

        // check if userService.getUser() is called
        Mockito.verify(userService).getUserResponse(userEntity);
    }

}