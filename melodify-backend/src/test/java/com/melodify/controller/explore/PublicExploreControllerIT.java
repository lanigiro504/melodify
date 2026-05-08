package com.melodify.controller.explore;

import com.melodify.support.AbstractMysqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公开探索接口：与 {@code /docs/init.sql} 种子数据无关时也应返回 200 与统一 {@code Result} 结构。
 */
@AutoConfigureMockMvc
class PublicExploreControllerIT extends AbstractMysqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exploreAssetsReturnsWrappedSuccess() throws Exception {
		mockMvc.perform(get("/api/public/explore/assets")
						.param("current", "1")
						.param("size", "12")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.records").isArray());
	}
}
