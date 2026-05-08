package com.melodify.common.exception;

import com.melodify.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 将常见异常转为统一 {@link Result}，避免栈信息直接暴露给前端（生产环境可结合日志记录完整堆栈）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	public static final int CODE_BAD_REQUEST = 400;
	public static final int CODE_UNAUTHORIZED = 401;
	public static final int CODE_CONFLICT = 409;

	@ExceptionHandler(BizException.class)
	public Result<Void> handleBiz(BizException e) {
		return Result.error(e.getCode(), e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Result<Void> handleValid(MethodArgumentNotValidException e) {
		String msg = e.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining("；"));
		return Result.error(CODE_BAD_REQUEST, msg.isEmpty() ? "参数校验失败" : msg);
	}

	@ExceptionHandler(BindException.class)
	public Result<Void> handleBind(BindException e) {
		String msg = e.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining("；"));
		return Result.error(CODE_BAD_REQUEST, msg.isEmpty() ? "参数绑定失败" : msg);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
		return Result.error(CODE_BAD_REQUEST, "请求体格式非法或缺失");
	}

	/**
	 * 用户名唯一等数据库约束冲突时转换为友好提示。
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public Result<Void> handleDataIntegrity(DataIntegrityViolationException e) {
		log.warn("数据完整性约束冲突: {}", e.getMostSpecificCause().getMessage());
		return Result.error(CODE_CONFLICT, "数据冲突，请检查是否重复注册或违反约束");
	}

	@ExceptionHandler(Exception.class)
	public Result<Void> handleOthers(Exception e) {
		log.error("未处理异常", e);
		return Result.error(500, "系统繁忙，请稍后再试");
	}
}
