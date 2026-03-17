package net.datasa.Wiki.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class GameRoom {
	private String roomId;
	private String status;
	
	private PlayerState user;
	private PlayerState cpu;
	
	private String currentTurnOwner;
	private String phase;
	
	private int turnCount;
	private int levelLimit;
	
	private String winner;
	
	@Builder.Default
	private List<String> logs = new ArrayList<>();
	
	public void switchTurn(){
		
		if (user.getMonsterZone() != null) user.getMonsterZone().setHasAttacked(false);
		if (cpu.getMonsterZone() != null) cpu.getMonsterZone().setHasAttacked(false);
		
		if ("CPU".equals(currentTurnOwner)){
			while(cpu.getHand().size() > 7){
				int randomIndex = (int) (Math.random() * cpu.getHand().size());
				GameCard discarded = cpu.getHand().remove(randomIndex);
				cpu.getCemetery().add(discarded);
				this.addLog("CPU가 패 상한을 초과하여 1장을 묘지로 버렸습니다");
			}
		}
		
		if ("USER".equals(currentTurnOwner)){
			currentTurnOwner = "CPU";
		} else {
			currentTurnOwner = "USER";
		}
		turnCount++;
		this.levelLimit = (turnCount + 1) / 2;
		
		this.addLog(turnCount + "턴 시작 ( 현재 최대 레벨은 " + levelLimit + "입니다 )");
	}
	
	public void addLog(String message){
		this.logs.add(message);
		if (this.logs.size() > 50) this.logs.remove(0);
	}
	public PlayerState getCurrentPlayer() {
		return "USER".equals(currentTurnOwner) ?  user:cpu;
	}
	public PlayerState getOpponentPlayer() {
		return "USER".equals(currentTurnOwner) ? cpu : user;
	}
}
