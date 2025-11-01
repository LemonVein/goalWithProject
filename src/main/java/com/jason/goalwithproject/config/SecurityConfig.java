package com.jason.goalwithproject.config;

import com.jason.goalwithproject.service.CustomOAuth2UserService;
import com.jason.goalwithproject.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.support.MultipartFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public FilterRegistrationBean<MultipartFilter> multipartFilter() {
        MultipartFilter multipartFilter = new MultipartFilter();
        multipartFilter.setMultipartResolverBeanName("multipartResolver");

        FilterRegistrationBean<MultipartFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(multipartFilter);
        registrationBean.setOrder(-102); // JwtAuthenticationFilter보다 앞에 오게 설정
        return registrationBean;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login/oauth2/code/*", "/oauth2/authorization/*").permitAll()
                        .requestMatchers("/api/user/login", "/api/user/kakao-login", "/api/user/google-login", "/api/user/register", "/api/user/refresh").permitAll()
                        .requestMatchers("/api/quest/{questId}/reactions").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
