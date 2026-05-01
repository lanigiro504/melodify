package com.melodify.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.entity.PaymentNotifyLog;
import com.melodify.mapper.PaymentNotifyLogMapper;
import com.melodify.service.PaymentNotifyLogService;
import org.springframework.stereotype.Service;

@Service
public class PaymentNotifyLogServiceImpl extends ServiceImpl<PaymentNotifyLogMapper, PaymentNotifyLog>
		implements PaymentNotifyLogService {
}
