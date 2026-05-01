package com.melodify;

import com.melodify.config.JwtProperties;
import com.melodify.config.MusicGenerationProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot 应用入口。
 * <p>
 * {@code exclude UserDetailsServiceAutoConfiguration}：
 * 仅使用 JWT 鉴权，不启用 Spring Boot 默认的内存用户账号，避免开发与日志中出现无关的默认密码。</p>
 */
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@MapperScan("com.melodify.mapper")
@EnableConfigurationProperties({ JwtProperties.class, MusicGenerationProperties.class })
public class MelodifyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MelodifyBackendApplication.class, args);
	}

}
