package com.melodify.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SimulatedPayNotifyDTO {

	@NotBlank
	private String notifyId;

	@NotBlank
	private String orderNo;

	@NotNull
	private Integer amountCent;

	@NotNull
	private Long timestamp;

	@NotBlank
	private String signature;
}
