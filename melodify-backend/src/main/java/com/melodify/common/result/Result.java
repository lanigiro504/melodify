package com.melodify.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 返回结构。
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

	public static final int CODE_SUCCESS = 200;

	private int code;
	private String message;
	private T data;

	public static <T> Result<T> success() {
		return success(null);
	}

	public static <T> Result<T> success(T data) {
		return new Result<>(CODE_SUCCESS, "success", data);
	}

	public static <T> Result<T> success(String message, T data) {
		return new Result<>(CODE_SUCCESS, message, data);
	}

	public static <T> Result<T> error(String message) {
		return error(500, message);
	}

	public static <T> Result<T> error(int code, String message) {
		return new Result<>(code, message, null);
	}
}
