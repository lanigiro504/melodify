package com.melodify.security;

import com.melodify.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

	@Mock
	private JwtProperties jwtProperties;

	@InjectMocks
	private JwtService jwtService;

	@Test
	void createAndParse_roundTrip() {
		when(jwtProperties.getSecret()).thenReturn("test-secret-must-be-long-enough-for-hs256-algorithm");
		when(jwtProperties.getExpirationHours()).thenReturn(1);

		String token = jwtService.createAccessToken(42L, "common");
		Claims claims = jwtService.parseClaims(token);

		assertThat(claims.getSubject()).isEqualTo("42");
		assertThat(claims.get(MelodifyUserPrincipal.CLAIM_ROLE_KEY, String.class)).isEqualTo("common");
		assertThat(jwtService.toPrincipal(claims).getUserId()).isEqualTo(42L);
	}
}
