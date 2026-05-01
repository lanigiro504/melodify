package com.melodify.model.dto;

import com.melodify.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功后返回给前端的数据：Bearer 令牌 + 用户快照（密码已置空）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

	private String token;

	private SysUser user;
}
