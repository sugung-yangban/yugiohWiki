package net.datasa.Wiki.domain.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class boosterpackRequest {
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
}
