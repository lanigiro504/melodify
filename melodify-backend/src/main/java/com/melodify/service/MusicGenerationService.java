package com.melodify.service;

import com.melodify.model.dto.MusicGenerateRequestDTO;
import com.melodify.model.vo.MusicGenerateSubmitVO;

/**
 * 编排「扣积分 → 写任务 → 事务提交后异步收尾」的领域服务。
 */
public interface MusicGenerationService {

	/**
	 * 提交生成：扣费与任务插入同事务；真实或模拟的远端调用应在事务结束后触发。
	 */
	MusicGenerateSubmitVO submitGeneration(Long userId, MusicGenerateRequestDTO dto);
}
