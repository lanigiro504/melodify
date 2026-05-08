package com.melodify.controller.explore;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.model.vo.MusicExploreItemVO;
import com.melodify.service.PublicExploreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 控制器纯单元测试：不启动 Spring 容器，避免 @MapperScan 与 MyBatis 依赖。
 */
@ExtendWith(MockitoExtension.class)
class PublicExploreControllerWebMvcTest {

	@Mock
	private PublicExploreService publicExploreService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new PublicExploreController(publicExploreService))
				.build();
	}

	@Test
	void exploreDelegatesToService() throws Exception {
		IPage<MusicExploreItemVO> page = new Page<>(1, 12, 0);
		when(publicExploreService.pagePublicAssets(eq(1L), eq(12L), isNull(), eq("NEWEST"), isNull()))
				.thenReturn(page);

		mockMvc.perform(get("/api/public/explore/assets")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.total").value(0));
	}
}
