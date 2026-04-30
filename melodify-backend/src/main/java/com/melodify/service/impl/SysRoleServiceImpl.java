package com.melodify.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.SysRole;
import com.melodify.mapper.SysRoleMapper;
import com.melodify.service.SysRoleService;
import org.springframework.stereotype.Service;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
}
