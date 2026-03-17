package net.datasa.Wiki.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
/*
* 조회결과를 담는다*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class boosterpackResponse {
	private String packname;
	private Integer id;
	private Integer cardid;
	private String rarity;
	private String cardtype;
	private String type;
	private String cardname;
	private String element;
	private String race;
	private Integer level;
	private Integer atk;
	private Integer def;
	private String text;
	private LocalDate releasedate;
	private Integer cardNumber;
}

