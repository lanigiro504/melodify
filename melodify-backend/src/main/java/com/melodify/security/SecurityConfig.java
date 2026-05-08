package com.melodify.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security HTTP 安全配置：JWT 过滤器 + 路由级权限。
 * <ul>
 *   <li>注册/登录放行；其余 {@code /api/client/**} 必须带有效 Bearer。</li>
 *   <li>{@code /api/admin/**} 仅允许 {@code ROLE_ADMIN}（JWT 中角色键 {@code admin} 映射而来）。</li>
 *   <li>无服务端 Session，适合前后端分离部署。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final AuthenticationEntryPoint restAuthenticationEntryPoint;
	private final AccessDeniedHandler restAccessDeniedHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/client/auth/register", "/api/client/auth/login")
						.permitAll()
						.requestMatchers(HttpMethod.POST, "/api/payments/simulated/notify")
						.permitAll()
						.requestMatchers(HttpMethod.POST, "/api/callback/suno")
						.permitAll()
						.requestMatchers("/ws/**")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/public/**")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/media/audio/**")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/media/avatar/**")
						.permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/client/**").authenticated()
						.anyRequest().authenticated())
				.exceptionHandling(e -> e
						.authenticationEntryPoint(restAuthenticationEntryPoint)
						.accessDeniedHandler(restAccessDeniedHandler))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
