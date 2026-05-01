package com.melodify.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRechargeOrderDTO {

	@NotNull
	private Long productId;
}
