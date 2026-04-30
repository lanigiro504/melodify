package com.melodify.controller.client;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.melodify.common.result.Result;
import com.melodify.entity.MusicTask;
import com.melodify.service.MusicTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端：音乐生成任务。路径前缀固定为 {@code /api/client/**}。
 * <p>
 * 面向 C 端用户创建任务、查询进度与结果关联的任务记录。{@code params} 为 JSON，与实体中
 * {@link com.melodify.entity.MusicTask#getParams()} 一致，由 MyBatis-Plus {@code JacksonTypeHandler} 读写。
 * </p>
 * <p>
 * <b>说明：</b>当前未接入登录态，列表与写操作通过必填参数 {@code userId} 区分归属；接入 Spring Security
 * 或会话后，应改为从当前登录用户解析 {@code userId}，并校验路径中的资源是否属于本人。
 * </p>
 */
@RestController
@RequestMapping("/api/client/music-tasks")
@RequiredArgsConstructor
public class ClientMusicTaskController {

	private final MusicTaskService musicTaskService;

	/**
	 * 分页查询某用户的音乐任务，按创建时间倒序。
	 *
	 * @param userId  用户主键（登录对接后由服务端注入，勿信任前端篡改）
	 * @param current 当前页
	 * @param size    每页条数
	 */
	@GetMapping("/page")
	public Result<IPage<MusicTask>> page(
			@RequestParam Long userId,
			@RequestParam(defaultValue = "1") long current,
			@RequestParam(defaultValue = "10") long size) {
		LambdaQueryWrapper<MusicTask> wrapper = new LambdaQueryWrapper<MusicTask>()
				.eq(MusicTask::getUserId, userId)
				.orderByDesc(MusicTask::getCreateTime);
		return Result.success(musicTaskService.page(new Page<>(current, size), wrapper));
	}

	/**
	 * 查询单条任务详情。生产环境需校验 {@code userId} 与记录的归属一致。
	 */
	@GetMapping("/{id}")
	public Result<MusicTask> getById(@PathVariable Long id) {
		return Result.success(musicTaskService.getById(id));
	}

	/**
	 * 提交新任务。请求体需包含 {@code userId}、业务侧生成的唯一 {@code taskId}（对应列 {@code task_id}）、
	 * {@code modelCode} 等；创建时间与更新时间由填充处理器写入。
	 */
	@PostMapping
	public Result<Boolean> create(@RequestBody MusicTask body) {
		return Result.success(musicTaskService.save(body));
	}

	/**
	 * 更新任务（如状态、错误信息、开始/结束时间）。由异步生成服务或管理流程调用时需做好权限控制。
	 */
	@PutMapping("/{id}")
	public Result<Boolean> update(@PathVariable Long id, @RequestBody MusicTask body) {
		body.setId(id);
		return Result.success(musicTaskService.updateById(body));
	}

	/**
	 * 删除任务记录（若表未配置逻辑删除则为物理删除）。
	 */
	@DeleteMapping("/{id}")
	public Result<Boolean> remove(@PathVariable Long id) {
		return Result.success(musicTaskService.removeById(id));
	}
}
