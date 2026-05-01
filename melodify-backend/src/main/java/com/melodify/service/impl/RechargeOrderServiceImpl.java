package com.melodify.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.melodify.common.exception.BizException;
import com.melodify.entity.PaymentNotifyLog;
import com.melodify.entity.PointLog;
import com.melodify.entity.PointProduct;
import com.melodify.entity.RechargeOrder;
import com.melodify.entity.SysUser;
import com.melodify.mapper.RechargeOrderMapper;
import com.melodify.model.dto.SimulatedPayNotifyDTO;
import com.melodify.model.vo.RechargeOrderVO;
import com.melodify.service.PaymentNotifyLogService;
import com.melodify.service.PointLogService;
import com.melodify.service.PointProductService;
import com.melodify.service.RechargeOrderService;
import com.melodify.service.SysUserService;
import com.melodify.support.BizIds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RechargeOrderServiceImpl extends ServiceImpl<RechargeOrderMapper, RechargeOrder>
		implements RechargeOrderService {

	private static final int STATUS_PENDING = 0;
	private static final int STATUS_PAID = 1;
	private static final long NOTIFY_TTL_SECONDS = 300;
	private static final String SIGN_SECRET = "melodify-simulated-pay-secret";

	private final PointProductService pointProductService;
	private final PaymentNotifyLogService paymentNotifyLogService;
	private final SysUserService sysUserService;
	private final PointLogService pointLogService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public RechargeOrderVO createOrder(Long userId, Long productId) {
		PointProduct product = pointProductService.getById(productId);
		if (product == null || !Integer.valueOf(1).equals(product.getStatus())) {
			throw new BizException(404, "积分商品不存在或已下架");
		}
		RechargeOrder order = new RechargeOrder();
		order.setOrderNo("MO" + BizIds.uuidCompact());
		order.setUserId(userId);
		order.setProductId(product.getId());
		order.setProductName(product.getProductName());
		order.setPoints(product.getPoints());
		order.setAmountCent(product.getPriceCent());
		order.setStatus(STATUS_PENDING);
		save(order);
		return RechargeOrderVO.from(order);
	}

	@Override
	public SimulatedPayNotifyDTO buildSimulatedNotify(String orderNo, Long userId) {
		RechargeOrder order = lambdaQuery().eq(RechargeOrder::getOrderNo, orderNo).one();
		if (order == null || userId == null || !userId.equals(order.getUserId())) {
			throw new BizException(404, "订单不存在");
		}
		SimulatedPayNotifyDTO dto = new SimulatedPayNotifyDTO();
		dto.setNotifyId(BizIds.uuidCompact());
		dto.setOrderNo(order.getOrderNo());
		dto.setAmountCent(order.getAmountCent());
		dto.setTimestamp(Instant.now().getEpochSecond());
		dto.setSignature(sign(dto));
		return dto;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public RechargeOrderVO handleSimulatedNotify(SimulatedPayNotifyDTO dto) {
		if (paymentNotifyLogService.lambdaQuery().eq(PaymentNotifyLog::getNotifyId, dto.getNotifyId()).exists()) {
			throw new BizException(409, "重复支付通知");
		}
		verifyNotify(dto);
		RechargeOrder order = lambdaQuery().eq(RechargeOrder::getOrderNo, dto.getOrderNo()).one();
		if (order == null) {
			throw new BizException(404, "订单不存在");
		}
		if (!order.getAmountCent().equals(dto.getAmountCent())) {
			throw new BizException(400, "支付金额不匹配");
		}
		if (Integer.valueOf(STATUS_PAID).equals(order.getStatus())) {
			saveNotify(dto, true, "订单已支付，幂等返回");
			return RechargeOrderVO.from(order);
		}
		boolean transitioned = update(Wrappers.<RechargeOrder>lambdaUpdate()
				.eq(RechargeOrder::getId, order.getId())
				.eq(RechargeOrder::getStatus, STATUS_PENDING)
				.set(RechargeOrder::getStatus, STATUS_PAID)
				.set(RechargeOrder::getPaidAt, LocalDateTime.now()));
		if (!transitioned) {
			throw new BizException(409, "订单状态已变化");
		}
		creditPoints(order);
		saveNotify(dto, true, "支付成功");
		return RechargeOrderVO.from(getById(order.getId()));
	}

	private void verifyNotify(SimulatedPayNotifyDTO dto) {
		long now = Instant.now().getEpochSecond();
		if (Math.abs(now - dto.getTimestamp()) > NOTIFY_TTL_SECONDS) {
			throw new BizException(400, "支付通知已过期");
		}
		String expected = sign(dto);
		if (!constantTimeEquals(expected, dto.getSignature())) {
			throw new BizException(400, "支付签名错误");
		}
	}

	private void creditPoints(RechargeOrder order) {
		boolean updated = sysUserService.update(Wrappers.<SysUser>lambdaUpdate()
				.eq(SysUser::getId, order.getUserId())
				.setSql("points = points + " + order.getPoints()));
		if (!updated) {
			throw new BizException(500, "积分到账失败");
		}
		SysUser user = sysUserService.getById(order.getUserId());
		PointLog log = new PointLog();
		log.setLogId(BizIds.uuidCompact());
		log.setUserId(order.getUserId());
		log.setChangeType(1);
		log.setAmount(order.getPoints());
		log.setBalance(user != null && user.getPoints() != null ? user.getPoints() : 0);
		log.setBizType("recharge");
		log.setBizId(order.getOrderNo());
		log.setRemark("模拟支付充值到账");
		pointLogService.save(log);
	}

	private void saveNotify(SimulatedPayNotifyDTO dto, boolean ok, String message) {
		PaymentNotifyLog log = new PaymentNotifyLog();
		log.setNotifyId(dto.getNotifyId());
		log.setOrderNo(dto.getOrderNo());
		log.setSignature(dto.getSignature());
		log.setPayload(Map.of("amountCent", dto.getAmountCent(), "timestamp", dto.getTimestamp()));
		log.setStatus(ok ? 1 : 0);
		log.setMessage(message);
		paymentNotifyLogService.save(log);
	}

	private static String sign(SimulatedPayNotifyDTO dto) {
		String plain = dto.getNotifyId() + "|" + dto.getOrderNo() + "|" + dto.getAmountCent() + "|"
				+ dto.getTimestamp();
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(SIGN_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception ex) {
			throw new IllegalStateException("模拟支付签名失败", ex);
		}
	}

	private static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null || a.length() != b.length()) {
			return false;
		}
		int diff = 0;
		for (int i = 0; i < a.length(); i++) {
			diff |= a.charAt(i) ^ b.charAt(i);
		}
		return diff == 0;
	}
}
