package net.datasa.Wiki.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameCard {
	private int id;
	private String uniqueId;
	private Integer originalId;
	private String name;
	private String description;
	private int atk;
	private int def;
	private int level;
	private String element;
	private String race;
	private String cardType;
	private String monsterType;
	private String imageUrl;
	private String reqType;
	private String reqValue;
	@Builder.Default
	private boolean hasAttacked = false;
	@Builder.Default
	private List<GameCard> equippedCards = new ArrayList<>();
	private String specialRule;
	
	public boolean isCleanable() {
		if ("Monster".equalsIgnoreCase(this.cardType)){
			return true;
		}
		
		if (this.monsterType != null) {
			String type = this.monsterType.toLowerCase();
			return !type.contains("field") && !type.contains("equip");
		}
		return true;
	}

}
