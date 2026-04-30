package com.melodify.common.exception;

import lombok.Getter;

/**
 * 业务异常，携带对外可展示的 HTTP 语义化错误码（由全局异常处理器写入 {@link com.melodify.common.result.Result#getCode()}）。
 */
@Getter
public class BizException extends RuntimeException {

	private final int code;

	public BizException(int code, String message) {
		super(message);
		this.code = code;
	}
}
