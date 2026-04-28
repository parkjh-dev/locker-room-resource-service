package com.lockerroom.resourceservice.infrastructure.security;

import com.lockerroom.resourceservice.infrastructure.security.CurrentUserIdArgumentResolver;

import com.lockerroom.resourceservice.infrastructure.security.CurrentUserId;

import com.lockerroom.resourceservice.infrastructure.exceptions.CustomException;
import com.lockerroom.resourceservice.infrastructure.exceptions.ErrorCode;
import com.lockerroom.resourceservice.user.model.entity.User;
import com.lockerroom.resourceservice.common.model.enums.Role;
import com.lockerroom.resourceservice.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserIdArgumentResolverTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private NativeWebRequest webRequest;

    private CurrentUserIdArgumentResolver resolver;

    private static final String KEYCLOAK_ID = "kc-test-uuid";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        resolver = new CurrentUserIdArgumentResolver(userRepository);
    }

    private void setUpSecurityContext(String subject) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private CurrentUserId mockAnnotation(boolean required) {
        CurrentUserId annotation = mock(CurrentUserId.class);
        when(annotation.required()).thenReturn(required);
        when(methodParameter.getParameterAnnotation(CurrentUserId.class)).thenReturn(annotation);
        return annotation;
    }

    @Nested
    @DisplayName("supportsParameter")
    class SupportsParameter {

        @Test
        @DisplayName("should return true when parameter has @CurrentUserId and is Long type")
        void supportsParameter_annotatedLong_returnsTrue() {
            when(methodParameter.hasParameterAnnotation(CurrentUserId.class)).thenReturn(true);
            when(methodParameter.getParameterType()).thenReturn((Class) Long.class);

            assertThat(resolver.supportsParameter(methodParameter)).isTrue();
        }

        @Test
        @DisplayName("should return false when parameter is not annotated")
        void supportsParameter_notAnnotated_returnsFalse() {
            when(methodParameter.hasParameterAnnotation(CurrentUserId.class)).thenReturn(false);

            assertThat(resolver.supportsParameter(methodParameter)).isFalse();
        }
    }

    @Nested
    @DisplayName("resolveArgument")
    class ResolveArgument {

        @Test
        @DisplayName("should return userId when JWT has valid sub claim and user exists")
        void resolveArgument_happyPath_returnsUserId() {
            // given
            mockAnnotation(true);
            setUpSecurityContext(KEYCLOAK_ID);

            User user = User.builder()
                    .id(USER_ID)
                    .keycloakId(KEYCLOAK_ID)
                    .email("test@example.com")
                    .nickname("testUser")
                    .role(Role.USER)
                    .build();
            when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));

            // when
            Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

            // then
            assertThat(result).isEqualTo(USER_ID);
            verify(userRepository).findByKeycloakId(KEYCLOAK_ID);

            clearSecurityContext();
        }

        @Test
        @DisplayName("should throw UNAUTHORIZED when no authentication and required=true")
        void resolveArgument_noAuthRequired_throwsUnauthorized() {
            // given
            mockAnnotation(true);
            clearSecurityContext();

            // when & then
            assertThatThrownBy(() -> resolver.resolveArgument(methodParameter, null, webRequest, null))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }

        @Test
        @DisplayName("should return null when no authentication and required=false")
        void resolveArgument_noAuthOptional_returnsNull() {
            // given
            mockAnnotation(false);
            clearSecurityContext();

            // when
            Object result = resolver.resolveArgument(methodParameter, null, webRequest, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should throw USER_NOT_FOUND when JWT valid but user does not exist")
        void resolveArgument_userNotFound_throwsException() {
            // given
            mockAnnotation(true);
            setUpSecurityContext(KEYCLOAK_ID);
            when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> resolver.resolveArgument(methodParameter, null, webRequest, null))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            clearSecurityContext();
        }
    }
}
