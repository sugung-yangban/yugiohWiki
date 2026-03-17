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
public class PlayerState {
	private String playerId;
	private int hp;
	private int maxHp;
	
	@Builder.Default
	private List<GameCard> deck = new ArrayList<>();
	@Builder.Default
	private List<GameCard> hand = new ArrayList<>();
	@Builder.Default
	private List<GameCard> extradeck = new ArrayList<>();
	@Builder.Default
	private List<GameCard> cemetery = new ArrayList<>();
	
	private GameCard monsterZone;
	private GameCard spellZone;
	private GameCard fieldZone;
	
	public boolean isDead(){
		return hp <= 0 || deck.isEmpty();
	}
	
	public void takeDamage(int damage){
		this.hp -= damage;
		if (this.hp<0) this.hp = 0;
	}
	
	public void cleanUpField(){
		if (this.monsterZone != null) {
			this.cemetery.add(this.monsterZone);
			this.monsterZone = null;
		}
	}
}
