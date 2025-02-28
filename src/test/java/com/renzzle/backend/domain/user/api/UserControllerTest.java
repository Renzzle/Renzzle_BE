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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(UserController.class)
//@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(username = "tintin", roles = "USER")
    void getUser_ShouldReturnUserDetails() throws Exception {

        // UserResponse 생성 - setup에서 하던 일을 여기로 옮김
        UserResponse userResponse = new UserResponse(
                1L,
                "tintintest46@mail.com",
                "tintin",
                null,
                null
        );

        // UserEntity로 변환
        UserEntity userEntity = UserEntity.builder()
                .id(1L)
                .email("tintintest46@mail.com")
                .nickname("tintin")
                .build();

        // UserDetailsImpl 생성 - 적절한 파라미터 전달
        UserDetailsImpl userDetails = new UserDetailsImpl(userEntity, "password", List.of("ROLE_USER"));

        Mockito.when(userService.getUser(anyLong())).thenReturn(userResponse);

        // SecurityContext에 사용자 정보 수동 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );

        Mockito.when(userService.getUser(anyLong())).thenReturn(userResponse);

        // MockMvc를 사용하여 GET 요청 테스트
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isOk())  // 상태 코드 200 (OK) 체크
                .andExpect(jsonPath("$.response.id").value(1L))  // 응답 데이터의 ID 확인
                .andExpect(jsonPath("$.response.email").value("tintintest46@mail.com"))  // 이메일 확인
                .andExpect(jsonPath("$.response.nickname").value("tintin"));  // 닉네임 확인

        // userService.getUser() 호출 여부 확인
        Mockito.verify(userService).getUser(anyLong());
    }


}