package net.datasa.Wiki.service;

import lombok.RequiredArgsConstructor;
import net.datasa.Wiki.domain.dto.GameCard;
import net.datasa.Wiki.domain.dto.GameRoom;
import net.datasa.Wiki.domain.dto.PlayerState;
import net.datasa.Wiki.domain.entity.*;
import net.datasa.Wiki.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GameService {
	private final myDeckRepository myDeckRepository;
	private final deckCardRepository deckCardRepository;
	private final BoosterRepository boosterRepository;
	private final Map<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
	private final FusionRecipeRepository fusionRecipeRepository;
	private final Map<String, GameRoom> gameRoom = new HashMap<>();
	private final EquipEffectRepository equipEffectRepository;
	
	public GameRoom createGame(String userId, Integer deckId, String difficulty){
		List<GameCard> userDeck = loadDeck(deckId);
		PlayerState userState = initPlayer(userId, userDeck);
		
		String cpuAccountId = "CPU_AI";
		List<myDeck> allCpuDecks = myDeckRepository.findByMember_MemberId(cpuAccountId);
		
		List<myDeck> filteredDecks = allCpuDecks.stream()
				.filter(deck -> deck.getDeckName().toUpperCase().contains(difficulty.toUpperCase()))
				.collect(Collectors.toList());
		
		if (filteredDecks.isEmpty()){
			throw new RuntimeException("해당 난이도의 CPU덱이 준비되지 않았습니다");
		}
		
		myDeck selectedCpuDeck = filteredDecks.get(new Random().nextInt(filteredDecks.size()));
		
		List<GameCard> cpuDeck = loadDeck(selectedCpuDeck.getDeckId());
		PlayerState cpuState = initPlayer("CPU",cpuDeck);
		
		String roomId = UUID.randomUUID().toString();
		GameRoom room = GameRoom.builder()
				.roomId(roomId)
				.status("RPS")
				.user(userState)
				.cpu(cpuState)
				.turnCount(0)
				.levelLimit(1)
				.logs(new ArrayList<>())
				.build();
		
		room.addLog("게임이 시작되었습니다. 가위바위보를 진행 해 주세요");
		gameRooms.put(roomId,room);
		
		return room;
	}
	
	public GameRoom processRps(String roomId, String userChoice){
		GameRoom room = getRoom(roomId);
		
		String[] rps = {"가위", "바위", "보"};
		String cpuChoice = rps[new Random().nextInt(3)];
		
		room.addLog("User:" + userChoice + " vs CPU: " + cpuChoice);
		boolean userWin = isUserWinRps(userChoice, cpuChoice);
		
		if (userWin){
			room.setCurrentTurnOwner("USER");
			room.addLog("User 선공!");
		} else {
			room.setCurrentTurnOwner("CPU");
			room.addLog("CPU 선공!");
		}
		
		room.setStatus("PLAYING");
		room.setTurnCount(1);
		room.setLevelLimit(1);
		
		drawCard(room.getUser(), 5);
		drawCard(room.getCpu(),5);
		
		if ("CPU".equals(room.getCurrentTurnOwner())){
			playCpuTurn(room);
		}
	
		return room;
	}
	
	public GameRoom summonMonster(String roomId, String cardUniqueId){
		GameRoom room = getRoom(roomId);
		PlayerState player = room.getUser();
		
		if (!"USER".equals(room.getCurrentTurnOwner())){
			throw new RuntimeException("당신의 턴이 아닙니다");
		}
		
		if (player.getMonsterZone() != null){
			sendMonsterToCemetery(player,player.getMonsterZone());
			room.addLog("기존 몬스터 [" + player.getMonsterZone().getName() + "]를 묘지로 보내고 새로운 소환을 준비합니다.");
		}
		
		GameCard cardToSummon = player.getHand().stream()
				.filter(c->c.getUniqueId().equals(cardUniqueId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("패에 없는 카드입니다"));
		
		if (cardToSummon.getLevel() > room.getLevelLimit()) {
			throw new RuntimeException("레벨 제한(" + room.getLevelLimit()+")보다 높은 몬스터입니다.");
		}
		player.getHand().remove(cardToSummon);
		player.setMonsterZone(cardToSummon);
		room.addLog("User가 "+cardToSummon.getName()+"을(를) 소환했습니다.");
		
		return room;
	}
	
	public GameRoom battle(String roomId){
		GameRoom room = getRoom(roomId);
		
		if (!"USER".equals(room.getCurrentTurnOwner())) throw new RuntimeException("내 턴이 아닙니다.");
		if (room.getUser().getMonsterZone()==null) throw new RuntimeException("공격할 몬스터가 없습니다");
		
		if(room.getTurnCount() == 1){
			throw new RuntimeException("선공의 첫 턴에는 공격할 수 없습니다.");
		}
		
		if (room.getUser().getMonsterZone().isHasAttacked()){
			throw new RuntimeException("이미 공격을 마친 몬스터입니다");
		}
		
		GameCard myMon = room.getUser().getMonsterZone();
		if (myMon.getEquippedCards() != null){
			for (GameCard equip : myMon.getEquippedCards()){
				String rule = equip.getSpecialRule();
				if (rule != null){
					switch (rule.toUpperCase()){
						case "CANNOT_ATTACK":
							throw new RuntimeException("[" + equip.getName() + "]의 효과로 인해 전투불가입니다");
					}
				}
			}
		}
		
		executeBattle(room, room.getUser(), room.getCpu());
		
		return room;
	}
	
	public  GameRoom endTurn(String roomId){
		GameRoom room = getRoom(roomId);
		PlayerState currentUser = "USER".equals(room.getCurrentPlayer()) ? room.getUser() : room.getCpu();
		GameCard[] activateMonsters = { room.getUser().getMonsterZone(), room.getCpu().getMonsterZone()};
		
		for (GameCard activeMon : activateMonsters) {
			if (activeMon != null && activeMon.getEquippedCards() != null) {
				for (GameCard equip : activeMon.getEquippedCards()) {
					String rule = equip.getSpecialRule();
					System.out.println("장착마법" + equip.getName() + "의 룰 텍스트: " + rule);
					if (rule != null && !rule.trim().isEmpty()) {
						String cleanRule = rule.trim().toUpperCase();
						switch (rule.toUpperCase()) {
							case "TURN-DECREASE_200":
								equip.setAtk(equip.getAtk() - 200);
								room.addLog("턴 경과: [" + equip.getName() + "]의 부작용으로 장착몬스터의 공격력이 200 감소했습니다");
								break;
							case "TURN-DECREASE_300":
								equip.setAtk(equip.getAtk() - 300);
								room.addLog("턴 경과: [" + equip.getName() + "]의 부작용으로 장착몬스터의 공격력이 300 감소했습니다");
								break;
						}
					}
				}
			}
		}
		
		room.switchTurn();
		
		if ("CPU".equals(room.getCurrentTurnOwner())) {
			playCpuTurn(room);
		}
		
		return room;
	}
	
	private void playCpuTurn(GameRoom room){
		PlayerState cpu = room.getCpu();
		PlayerState user = room.getUser();
		room.addLog("Cpu턴 시작");
		
		drawCard(cpu, 1);
		int cpuBuff = calculateHandBuff(cpu, cpu.getMonsterZone());
		int userBuff = calculateHandBuff(user, user.getMonsterZone());
		
		int cpuEquipAtk = getEquipAtkBuff(cpu.getMonsterZone());
		int userEquipDef = getEquipDefBuff(user.getMonsterZone());
		
		int[] cpuFieldBuffs = getFieldSpellBuffs(room, cpu.getMonsterZone());
		int[] userFieldBuffs = getFieldSpellBuffs(room, user.getMonsterZone());
		
		int userFinalDef = 0;
		if (user.getMonsterZone() != null){
			userFinalDef = user.getMonsterZone().getDef() + userBuff + userEquipDef + userFieldBuffs[1];
		}
		
		int cpuFinalAtk = 0;
		if (cpu.getMonsterZone() != null){
			cpuFinalAtk = cpu.getMonsterZone().getAtk() + cpuBuff + cpuEquipAtk + cpuFieldBuffs[0];
		}
		
		int fusionTargetAtk = Math.max(userFinalDef, cpuFinalAtk);
		System.out.println("CPU 엑스트라 덱 장수:" + cpu.getExtradeck().size());
		System.out.println("CPU 패 장수:" + cpu.getHand().size());
		System.out.println("융합 몬스터가 넘어야 할 최소 타점:" + fusionTargetAtk);
		System.out.println("CPU 패 목록:");
		cpu.getHand().forEach(c -> System.out.println("["+c.getName()+"]"));
		System.out.println("CPU엑스트라 덱 목록:");
		cpu.getExtradeck().forEach(c -> System.out.println("[ " + c.getName() + "(공:" + c.getAtk() + ")]"));
		boolean fusionSuccess = cpuTryFusionSummon(room,cpu,fusionTargetAtk);
		
		if (!fusionSuccess){
			boolean needNormalSummon = false;
			
			if (cpu.getMonsterZone() == null){
				needNormalSummon = true;
			} else if (user.getMonsterZone() != null && cpuFinalAtk <= userFinalDef){
				needNormalSummon = true;
			}
			
			if (needNormalSummon) {
				final int finalTargetAtk = userFinalDef;
				GameCard bestMonster = cpu.getHand().stream()
						.filter(c -> "Monster".equals(c.getCardType()))
						.filter(c -> c.getLevel() <= room.getLevelLimit())
						.filter(c -> cpu.getMonsterZone() == null || (c.getAtk() + cpuBuff + getFieldSpellBuffs(room, c)[0]) > finalTargetAtk)
						.max(Comparator.comparingInt(GameCard::getAtk))
						.orElse(null);
				
				if (bestMonster != null){
					if (cpu.getMonsterZone() != null){
						sendMonsterToCemetery(cpu, cpu.getMonsterZone());
						room.addLog("CPU가 한계를 느끼고 [ "+ cpu.getMonsterZone().getName()+ "]을(를) 묘지로 보냈습니다");
						cpu.setMonsterZone(null);
					}
					cpu.getHand().remove(bestMonster);
					cpu.setMonsterZone(bestMonster);
					room.addLog("CPU가 [ "+bestMonster.getName()+" ]을(를) 소환했습니다.");
					
					cpuFinalAtk = bestMonster.getAtk() + cpuBuff + getFieldSpellBuffs(room, bestMonster)[0];
				}
			}
		}
		
		List<GameCard> fieldSpells = cpu.getHand().stream()
				.filter(c -> c.getCardType() != null && (c.getCardType().contains("Magic") || c.getCardType().contains("Spell")))
				.filter(c -> c.getMonsterType() != null && c.getMonsterType().toLowerCase().contains("field"))
				.collect(Collectors.toList());
		
		if (!fieldSpells.isEmpty()){
			GameCard fieldSpellToUse = fieldSpells.get(0);
			
			if (cpu.getFieldZone() != null){
				cpu.getCemetery().add(cpu.getFieldZone());
			}
			
			List<EquipEffect> effects = equipEffectRepository.findBySpellId(fieldSpellToUse.getId());
			if (!effects.isEmpty()){
				fieldSpellToUse.setAtk(effects.get(0).getAtkCh());
				fieldSpellToUse.setDef(effects.get(0).getDefCh());
				fieldSpellToUse.setReqType(effects.get(0).getReqType());
				fieldSpellToUse.setReqValue(effects.get(0).getReqValue());
			} else {
				fieldSpellToUse.setAtk(0); fieldSpellToUse.setDef(0);
			}
			
			cpu.getHand().remove(fieldSpellToUse);
			cpu.setFieldZone(fieldSpellToUse);
			room.addLog("CPU가 필드마법 [" + fieldSpellToUse.getName() + "]을(를) 발동했습니다");
			
			cpuFieldBuffs = getFieldSpellBuffs(room, cpu.getMonsterZone());
			userFieldBuffs = getFieldSpellBuffs(room, user.getMonsterZone());
		}
		
		if (cpu.getMonsterZone() != null){
			List<GameCard> equipSpells = cpu.getHand().stream()
					.filter(c -> c.getCardType() != null && (c.getCardType().contains("Magic") || c.getCardType().contains("Spell")))
					.filter(c -> c.getMonsterType() != null && c.getMonsterType().toLowerCase().contains("equip"))
					.collect(Collectors.toList());
			
			for (GameCard equip : equipSpells){
				if (canTargetForEquip(equip, cpu.getMonsterZone(), true)){
					List<EquipEffect> effects = equipEffectRepository.findBySpellId(equip.getId());
					if(!effects.isEmpty()){
						equip.setAtk(effects.get(0).getAtkCh());
						equip.setDef(effects.get(0).getDefCh());
						equip.setSpecialRule(effects.get(0).getSpecialRule());
					} else {
						equip.setAtk(0); equip.setDef(0);
					}
					
					cpu.getHand().remove(equip);
					cpu.getMonsterZone().getEquippedCards().add(equip);
					room.addLog("CPU가 ["+ equip.getName() + "]을(를) "+cpu.getMonsterZone().getName()+" 에게 장착했습니다");
					cpuEquipAtk = getEquipAtkBuff(cpu.getMonsterZone());
				}
			}
		}
		
		if (user.getMonsterZone() != null){
			userFinalDef = user.getMonsterZone().getDef() + userBuff + userEquipDef + userFieldBuffs[1];
		}
		if (cpu.getMonsterZone() != null){
			cpuFinalAtk = cpu.getMonsterZone().getAtk() + cpuBuff + cpuEquipAtk + cpuFieldBuffs[0];
		}
		
		if (cpu.getMonsterZone() != null){
			boolean shouldAttack =false;
			
			if(room.getTurnCount() == 1){
				shouldAttack = false;
				room.addLog("선공 첫 턴이므로 CPU는 공격 선언이 불가 합니다.");
			} else {
				if (user.getMonsterZone() == null ){
					shouldAttack = true;
				} else if (cpuFinalAtk > userFinalDef) {
					shouldAttack = true;
				}
			}
			
			if (shouldAttack && cpu.getMonsterZone().getEquippedCards() != null){
				for (GameCard equip : cpu.getMonsterZone().getEquippedCards()){
					String rule = equip.getSpecialRule();
					if (rule != null && "CANNOT-ATTACK".equals(rule.toUpperCase())){
						shouldAttack = false;
						room.addLog("CPU몬스터는 [" + equip.getName() + "] 의 효과로 인해 공격 불가합니다.");
						break;
					}
				}
			}
			
			if (shouldAttack){
				executeBattle(room, cpu, user);
			} else if(room.getTurnCount() > 1) {
				room.addLog("CPU가 공격을 포기했습니다.");
			}
			
			
		} else {
			room.addLog("CPU는 꺼낼 수 있는 몬스터가 없어 턴을 넘깁니다");
		}
		
		room.switchTurn();
		
		drawCard(user, 1);
		room.addLog("User 턴 시작");
	}
	
	private void executeBattle(GameRoom room, PlayerState attacker, PlayerState defender){
		GameCard myMon = attacker.getMonsterZone();
		GameCard enemyMon = defender.getMonsterZone();
		
		myMon.setHasAttacked(true);
		
		int attackerBuff = calculateHandBuff(attacker,myMon);
		int defenderBuff = calculateHandBuff(defender,enemyMon);
		
		int attackerEquipAtk = getEquipAtkBuff(myMon);
		int defenderEquipDef = getEquipDefBuff(enemyMon);
		
		int[] attackerFieldBuffs = getFieldSpellBuffs(room, myMon);
		int[] defenderFieldBuffs = getFieldSpellBuffs(room, enemyMon);
		
		int finalAtk = myMon.getAtk() + attackerBuff + attackerEquipAtk + attackerFieldBuffs[0];
		
		if (enemyMon == null){
			defender.takeDamage(finalAtk);
			room.addLog(attacker.getPlayerId() + "의 직접공격! \n데미지: "+ finalAtk);
		} else {
			int finalDef = enemyMon.getDef() + defenderBuff + defenderEquipDef + defenderFieldBuffs[1];
			int diff = finalAtk - finalDef;
			
			if (diff>0){
				defender.takeDamage(diff);
				room.addLog("전투승리! 상대에게 " + diff + " 데미지");
				sendMonsterToCemetery(defender, enemyMon);
				defender.setMonsterZone(null);
			} else if (diff < 0) {
				attacker.takeDamage(Math.abs(diff));
				room.addLog("공격 실패.. " + Math.abs(diff) + "의 반사데미지를 입었습니다.");
				sendMonsterToCemetery(attacker, myMon);
				attacker.setMonsterZone(null);
			} else {
				room.addLog("무승부. 아무일도 일어나지 않습니다");
				sendMonsterToCemetery(attacker, myMon);
				attacker.setMonsterZone(null);
				sendMonsterToCemetery(defender,enemyMon);
				defender.setMonsterZone(null);
			}
		}
		
		checkWinCondition(room);
	}
	
	public GameRoom getRoom(String roomId){
		return gameRooms.get(roomId);
	}
	
	private void drawCard(PlayerState player, int count) {
		for (int i = 0; i < count; i++) {
			if (!player.getDeck().isEmpty()){
				GameCard card = player.getDeck().remove(0);
				player.getHand().add(card);
			}
		}
	}
	
	private PlayerState initPlayer(String playerId, List<GameCard> allCards){
		List<GameCard> mainDeck = new ArrayList<>();
		List<GameCard> extraDeck = new ArrayList<>();
		
		for(GameCard card : allCards){
			if (card.getMonsterType() != null){
				String mType = card.getMonsterType().toLowerCase();
				
				if (mType.contains("fusion")||
						mType.contains("synchro")||
						mType.contains("xyz")||
						mType.contains("link")){
					extraDeck.add(card);
					continue;
				}
			}
			mainDeck.add(card);
		}
		
		Collections.shuffle(mainDeck);
		
		return PlayerState.builder()
				.playerId(playerId)
				.hp(8000)
				.maxHp(80000)
				.deck(mainDeck)
				.hand(new ArrayList<>())
				.cemetery(new ArrayList<>())
				.extradeck(extraDeck)
				.build();
	}
	
	private List<GameCard> loadDeck(Integer deckId){
		myDeck deckEntity = myDeckRepository.findById(deckId).orElseThrow();
		List<deckCard> cards = deckCardRepository.findByMyDeck(deckEntity);
		
		return cards.stream().map(dc -> {
			boosterpack original = dc.getCard();
			return GameCard.builder()
					.uniqueId(UUID.randomUUID().toString())
					.originalId(original.getCardNumber())
					.id(original.getId())
					.name(original.getCardname())
					.atk(original.getAtk() == null ? 0: original.getAtk())
					.def(original.getDef() == null ? 0: original.getDef())
					.level(original.getLevel()  == null ? 0: original.getLevel())
					.cardType(original.getCardtype())
					.monsterType(original.getType())
					.race(original.getRace())
					.element(original.getElement())
					.build();
					
		}).collect(Collectors.toList());
	}
	
	private boolean isUserWinRps(String user, String cpu){
		if (user.equals(cpu)) return false;
		if (user.equals("바위")) return cpu.equals("가위");
		if (user.equals("보")) return cpu.equals("바위");
		if (user.equals("가위")) return cpu.equals("보");
		return false;
	}
	
	private void checkWinCondition(GameRoom room){
		if (room.getUser().isDead()){
			if (room.getUser().getHp() < 0) room.getUser().setHp(0);
			
			room.setStatus("CPU_WIN");
			room.addLog("유저의 패배.. ");
		} else if (room.getCpu().isDead()){
			if (room.getCpu().getHp() < 0) room.getCpu().setHp(0);
			
			room.setStatus("USER_WIN");
			room.addLog("듀얼 승리!");
		}
	}
	
	public GameRoom surrender(String roomId){
		GameRoom room = getRoom(roomId);
		room.getUser().setHp(0);
		room.setStatus("CPU_WIN");
		room.addLog("유저가 항복을 선언했습니다. CPU 승리");
		return room;
	}
	
	public GameRoom fusionSummon(String roomId, String targetUniqueId, List<String> materialUniqueIds){
		GameRoom room = getRoom(roomId);
		PlayerState player = room.getUser();
		
		if (!"USER".equals(room.getCurrentTurnOwner())) throw new RuntimeException("본인의 턴이 아닙니다");
		
		GameCard targetCard = player.getExtradeck().stream()
				.filter(c -> c.getUniqueId().equals(targetUniqueId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("엑스트라 덱에 대상 카드가 없습니다"));
		
		List<FusionRecipe> recipes= fusionRecipeRepository.findByFusionMonsterId(targetCard.getId());
		if (recipes == null || recipes.isEmpty()){
			throw new RuntimeException("이 몬스터는 아직 융합 레시피가 등록되지 않았습니다");
		}
		
		List<GameCard> selectedMaterials = new ArrayList<>();
		for (String matId : materialUniqueIds) {
			GameCard card = player.getHand().stream()
					.filter(c -> c.getUniqueId().equals(matId)).findFirst()
					.orElse(null);
			
			if (card == null && player.getMonsterZone() != null && player.getMonsterZone().getUniqueId().equals(matId)) {
				card = player.getMonsterZone();
			}
			
			if (card==null){
				throw new RuntimeException("선택한 소재를 패나 필드에서 찾을 수 없습니다");
			}
			selectedMaterials.add(card);
		}
		
		List<GameCard> tempMaterials = new ArrayList<>(selectedMaterials);
		
		for (FusionRecipe fusionRecipe : recipes) {
			int matched = 0;
			
			Iterator<GameCard> iterator = tempMaterials.iterator();
			while (iterator.hasNext()) {
				GameCard card = iterator.next();
				boolean isMatch = false;
				
				switch (fusionRecipe.getReqType()) {
					case "ID":
						isMatch = String.valueOf(card.getId()).equals(fusionRecipe.getReqValue());
						break;
					case "RACE":
						isMatch = fusionRecipe.getReqValue().equals(card.getRace());
						break;
					case "ELEMENT":
						isMatch = fusionRecipe.getReqValue().equals(card.getElement());
						break;
				}
				
				if (isMatch) {
					matched++;
					iterator.remove();
					if (matched == fusionRecipe.getReqCount()) break;
				}
			}
			
			if (matched < fusionRecipe.getReqCount()) {
				throw new RuntimeException("융합 조건 불충족: [" + fusionRecipe.getReqValue() + "] 조건의 카드가 부족합니다");
			}
		}
		
		for (GameCard mat : selectedMaterials) {
			if (player.getHand().contains(mat)){
				player.getHand().remove(mat);
				player.getCemetery().add(mat);
			} else if (player.getMonsterZone() != null && player.getMonsterZone().getUniqueId().equals(mat.getUniqueId())){
				sendMonsterToCemetery(player,player.getMonsterZone());
				player.setMonsterZone(null);
			}
		}
		
		if (player.getMonsterZone() != null){
			sendMonsterToCemetery(player,player.getMonsterZone());
			room.addLog("기존 몬스터 [" +  player.getMonsterZone().getName() + "] 을 묘지로 보내고 융합소환 합니다");
			player.setMonsterZone(null);
		}
		
		
		if (!tempMaterials.isEmpty()){
			throw new RuntimeException("필요없는 융합소재가 포함되어있습니다. 정확한 수량을 선택해주세요");
		}
		
		player.getExtradeck().remove(targetCard);
		player.setMonsterZone(targetCard);
			
		room.addLog("융합 소환 성공! " + targetCard.getName() + "특수 소환!");
		return room;
		
	}
	
	public List<String> getValidFusionMaterials(String roomId, String targetUniqueId){
		GameRoom room = getRoom(roomId);
		PlayerState player = room.getUser();
		
		GameCard targetCard = player.getExtradeck().stream()
				.filter(c -> c.getUniqueId().equals(targetUniqueId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("엑스트라 덱에 대상 카드가 없습니다"));
	
		List<FusionRecipe> recipes = fusionRecipeRepository.findByFusionMonsterId(targetCard.getId());
		if (recipes == null || recipes.isEmpty()) return Collections.emptyList();
		
		List<String> valiMaterialIds = new ArrayList<>();
		
		List<GameCard> allAvailableCards = new ArrayList<>(player.getHand());
		if (player.getMonsterZone() != null){
			allAvailableCards.add(player.getMonsterZone());
		}
	
		for(GameCard card : allAvailableCards){
			for (FusionRecipe recipe : recipes){
				boolean isMatch = false;
				switch (recipe.getReqType()){
					case "ID":
						isMatch = String.valueOf(card.getId()).equals(recipe.getReqValue());
						break;
					case "RACE":
						isMatch = recipe.getReqValue().equals(card.getRace());
						break;
					case "ELEMENT":
						isMatch = recipe.getReqValue().equals(card.getElement());
						break;
				}
				
				if (isMatch){
					valiMaterialIds.add(card.getUniqueId());
					break;
				}
			}
		}
		return valiMaterialIds;
	}
	
	public List<String> getSummonableExtraDeckMonsters(String roomId){
		GameRoom room = getRoom(roomId);
		PlayerState player = room.getUser();
		List<String> summonableIds = new ArrayList<>();
		
		if(!"USER".equals(room.getCurrentTurnOwner())) return summonableIds;
		
		List<GameCard> allAvailableCards = new ArrayList<>(player.getHand());
		if (player.getMonsterZone() != null){
			allAvailableCards.add(player.getMonsterZone());
		}
		
		for (GameCard targetCard : player.getExtradeck()){
			List<FusionRecipe> recipes = fusionRecipeRepository.findByFusionMonsterId(targetCard.getId());
			if (recipes == null || recipes.isEmpty()) continue;
			
			List<GameCard> temMaterials = new ArrayList<>(allAvailableCards);
			boolean canSummon = true;
			
			for(FusionRecipe recipe : recipes){
				int matched = 0;
				Iterator<GameCard> iterator = temMaterials.iterator();
				while(iterator.hasNext()){
					GameCard card = iterator.next();
					boolean isMatch = false;
					switch (recipe.getReqType()){
						case "ID": isMatch = String.valueOf(card.getId()).equals(recipe.getReqValue()); break;
						case "RACE": isMatch = recipe.getReqValue().equals(card.getRace()); break;
						case "ELEMENT": isMatch = recipe.getReqValue().equals(card.getElement()); break;
					}
					if (isMatch){
						matched++;
						iterator.remove();
						if (matched == recipe.getReqCount()) break;
					}
				}
				if (matched  < recipe.getReqCount()){
					canSummon = false;
					break;
				}
			}
			if (canSummon){
				summonableIds.add(targetCard.getUniqueId());
			}
		}
		return summonableIds;
	}
	
	public GameRoom discardCard(String roomId, String cardUniqueId){
		GameRoom room = getRoom(roomId);
		PlayerState player = room.getUser();
		
		if (!"USER".equals(room.getCurrentTurnOwner())) {
			throw new RuntimeException("본인의 턴이 아닙니다.");
		}
		
		GameCard cardToDiscard = player.getHand().stream()
				.filter(c -> c.getUniqueId().equals(cardUniqueId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("패에 없는 카드입니다."));
		player.getHand().remove(cardToDiscard);
		player.getCemetery().add(cardToDiscard);
		
		room.addLog("패 제한 초과로 인해 [" + cardToDiscard.getName() + "]을(를) 묘지로 보냈습니다");
		return room;
	}
	
	public boolean cpuTryFusionSummon(GameRoom room, PlayerState cpu, int targetAtkToBeat){
		List<GameCard> availableCards = new ArrayList<>(cpu.getHand());
		System.out.println("CPU 엑스트라 덱 장수: "+cpu.getExtradeck().size());
		System.out.println("CPU 패 몬스터 목록: ");
		cpu.getHand().forEach(c -> System.out.println("["+c.getName() + " (ID: "+c.getId()+")]"));
		if (cpu.getMonsterZone() != null){
			availableCards.add(cpu.getMonsterZone());
		}
		GameCard bestFusion = null;
		List<GameCard> bestMaterials = null;
		
		for (GameCard targetCard : cpu.getExtradeck()){
			if (targetCard.getAtk() <= targetAtkToBeat) continue;
			
			List<FusionRecipe> recipes = fusionRecipeRepository.findByFusionMonsterId(targetCard.getId());
			if (recipes == null || recipes.isEmpty()) continue;
			
			List<GameCard> tempMaterials = new ArrayList<>(availableCards);
			List<GameCard> selectedMaterials = new ArrayList<>();
			boolean canSummon = true;
			
			for (FusionRecipe recipe : recipes){
				int matched = 0;
				Iterator<GameCard> iterator = tempMaterials.iterator();
				while (iterator.hasNext()){
					GameCard card = iterator.next();
					boolean isMatch = false;
					switch (recipe.getReqType()){
						case "ID" : isMatch = String.valueOf(card.getId()).equals(recipe.getReqValue()); break;
						case "RACE" : isMatch = recipe.getReqValue().equals(card.getRace()); break;
						case "ELEMENT": isMatch = recipe.getReqValue().equals(card.getElement()); break;
					}
					if (isMatch){
						matched++;
						selectedMaterials.add(card);
						iterator.remove();
						if (matched == recipe.getReqCount()) break;
					}
				}
				if (matched < recipe.getReqCount()){
					canSummon = false;
					break;
				}
			}
			if (canSummon){
				if (bestFusion==null||targetCard.getAtk()>bestFusion.getAtk()){
					bestFusion = targetCard;
					bestMaterials = selectedMaterials;
				}
			}
		}
		if (bestFusion != null){
			for (GameCard mat : bestMaterials){
				if (cpu.getHand().contains(mat)){
					cpu.getHand().remove(mat);
				} else if (cpu.getMonsterZone() != null && cpu.getMonsterZone().getUniqueId().equals(mat.getUniqueId())){
					cpu.setMonsterZone(null);
				}
				cpu.getCemetery().add(mat);
			}
			if (cpu.getMonsterZone() != null){
				cpu.getCemetery().add(cpu.getMonsterZone());
				cpu.setMonsterZone(null);
			}
			cpu.getExtradeck().remove(bestFusion);
			cpu.setMonsterZone(bestFusion);
			room.addLog("조건 충족! 융합 소환으로 [" + bestFusion.getName() + "] 등장!");
			return true;
		}
		
		return false;
	}
	
	private int calculateHandBuff(PlayerState player, GameCard fieldMonster){
		if (fieldMonster == null) return 0;
		
		List<GameCard> handMonsters = player.getHand().stream()
				.filter(c -> "Monster".equals(c.getCardType()))
				.collect(Collectors.toList());
		
		if (handMonsters.isEmpty()) return 0;
		
		String fieldRace = fieldMonster.getRace();
		String fieldElement = fieldMonster.getElement();
		
		boolean allSameRace = true;
		boolean allSameElement = true;
		
		for (GameCard handMon : handMonsters){
			if (fieldRace == null || !fieldRace.equals(handMon.getRace())){
				allSameRace = false;
			}
			if (fieldElement == null || !fieldElement.equals(handMon.getElement())){
				allSameElement = false;
			}
		}
		
		if (allSameElement && allSameRace) return 750;
		if (allSameElement || allSameRace) return 500;
		
		return 0;
	}
	
	public boolean canTargetForEquip(GameCard spell, GameCard targetMonster, boolean isMyMonster){
		if (targetMonster == null) return false;
		
		List<EquipEffect> effects = equipEffectRepository.findBySpellId(spell.getId());
		
		if (effects.isEmpty()) return true;
		
		for (EquipEffect effect : effects){
			boolean isMatch = false;
			String reqType = effect.getReqType();
			
			if (reqType == null){
				return true;
			}
			
			switch (effect.getReqType().toUpperCase()){
				case "RACE":
					isMatch = targetMonster.getRace() != null && targetMonster.getRace().equals(effect.getReqValue());
					break;
				case "ELEMENT":
					isMatch = targetMonster.getElement() != null && targetMonster.getElement().equals(effect.getReqValue());
					break;
				case "NOT_RACE":
					isMatch = targetMonster.getRace() != null && !targetMonster.getRace().equals(effect.getReqValue());
					break;
				case "NOT_ELEMENT":
					isMatch = targetMonster.getElement() != null && !targetMonster.getElement().equals(effect.getReqValue());
					break;
				case "CONTROL":
					if("MIN".equalsIgnoreCase(effect.getReqValue())){
						isMatch = isMyMonster;
					} else if ("OPPONENT".equalsIgnoreCase(effect.getReqValue())) {
						isMatch = !isMyMonster;
					}
					break;
			}
			
			if (isMatch) return true;
		}
		return false;
	}
	
	public GameRoom equipSpell(String roomId, String spellUniqueId, String targetMonsterUniqueId){
		GameRoom room = getRoom(roomId);
		PlayerState player = room.getUser();
		PlayerState cpu = room.getCpu();
		
		if (!"USER".equals(room.getCurrentTurnOwner())){
			throw new RuntimeException("본인의 턴이 아닙니다.");
		}
		
		GameCard spellCard = player.getHand().stream()
				.filter(c -> c.getUniqueId().equals(spellUniqueId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("패에 해당 카드가 없습니다"));
		
		String cType = spellCard.getCardType() != null ? spellCard.getCardType().toLowerCase():"";
		if (!(cType.contains("magic") || cType.contains("spell") || cType.contains("마법"))){
			throw new RuntimeException("이 카드는 마법카드가 아닙니다");
		}
		
		String dType = spellCard.getMonsterType() != null ? spellCard.getMonsterType().toLowerCase() : "";
		if (!dType.contains("equip")){
			throw new RuntimeException("이 카드는 장착 마법이 아닙니다. (현재 타입: " + spellCard.getMonsterType() + ")");
		}
		
		GameCard targetMonster = null;
		boolean isMyMonster = false;
		if (player.getMonsterZone() != null && player.getMonsterZone().getUniqueId().equals(targetMonsterUniqueId)){
			targetMonster = player.getMonsterZone();
			isMyMonster= true;
		} else if (cpu.getMonsterZone() != null && cpu.getMonsterZone().getUniqueId().equals(targetMonsterUniqueId)){
			targetMonster = cpu.getMonsterZone();
			isMyMonster = false;
		}
		
		if (targetMonster == null){
			throw new RuntimeException("대상이 되는 몬스터를 필드에서 찾을 수 없습니다");
		}
		
		if (!canTargetForEquip(spellCard, targetMonster,isMyMonster)){
			throw new RuntimeException("이 몬스터는 장착 할 수 없습니다. (종족/속성 불일치)");
		}
		
		List<EquipEffect> effects = equipEffectRepository.findBySpellId(spellCard.getId());
		if (!effects.isEmpty()){
			spellCard.setAtk(effects.get(0).getAtkCh());
			spellCard.setDef(effects.get(0).getDefCh());
			spellCard.setSpecialRule(effects.get(0).getSpecialRule());
			
			System.out.println(spellCard.getName() + "장착완료");
			System.out.println("세팅된 특수룰 "+spellCard.getSpecialRule());
		} else {
			spellCard.setAtk(0);
			spellCard.setDef(0);
		}
		
		player.getHand().remove(spellCard);
		targetMonster.getEquippedCards().add(spellCard);
		
		room.addLog(" [" + spellCard.getName() + "]을(를) ["+ targetMonster.getName() + "]에 장착했습니다!");
		return room;
		
	}
	
	private int getEquipAtkBuff(GameCard monster){
		if (monster == null || monster.getEquippedCards() == null) return 0;
		return monster.getEquippedCards().stream().mapToInt(GameCard::getAtk).sum();
	}
	
	private int getEquipDefBuff(GameCard monster){
		if (monster == null || monster.getEquippedCards() == null) return 0;
		return monster.getEquippedCards().stream().mapToInt(GameCard::getDef).sum();
	}
	
	private void sendMonsterToCemetery(PlayerState player, GameCard monster){
		if (monster == null) return;
		
		player.getCemetery().add(monster);
		
		if (monster.getEquippedCards() != null && !monster.getEquippedCards().isEmpty()){
			player.getCemetery().addAll(monster.getEquippedCards());
			monster.getEquippedCards().clear();
		}
	}
	
	public GameRoom activateFieldSpell(String roomId, String spellUniqueId){
		GameRoom room = getRoom(roomId);
		PlayerState player = room.getUser();
		
		if (!"USER".equals(room.getCurrentTurnOwner())){
			throw new RuntimeException("본인의 턴이 아닙니다");
		}
		
		GameCard spellCard = player.getHand().stream()
				.filter(c -> c.getUniqueId().equals(spellUniqueId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("패에 해당 카드가 없습니다"));
		
		String cType = spellCard.getCardType() != null ? spellCard.getCardType().toLowerCase() : "";
		if (!(cType.contains("magic") || cType.contains("spell") || cType.contains("마법"))){
			throw new RuntimeException("이 카드는 마법카드가 아닙니다.");
		}
		
		if(player.getFieldZone() != null){
			player.getCemetery().add(player.getFieldZone());
			room.addLog("기존 필드 마법 ["+player.getFieldZone().getName()+"]이(가) 파괴되었습니다");
		}
		List<EquipEffect> effects = equipEffectRepository.findBySpellId(spellCard.getId());
		System.out.println("발동한 마법 카드 이름" + spellCard.getName());
		System.out.println("조회에 사용한 카드 ID (PK) "+spellCard.getId());
		System.out.println("DB에서 찾은 효과 개수: "+effects.size());
		if (!effects.isEmpty()){
			System.out.println("DB에서 찾은 효과. 공버프: " + effects.get(0).getAtkCh() + ", 수버프: "+effects.get(0).getDefCh());
			spellCard.setAtk(effects.get(0).getAtkCh());
			spellCard.setDef(effects.get(0).getDefCh());
			spellCard.setReqType(effects.get(0).getReqType());
			spellCard.setReqValue(effects.get(0).getReqValue());
		} else {
			System.out.println("DB에서 찾지 못했습니다");
			spellCard.setAtk(0);
			spellCard.setDef(0);
		}
		
		player.getHand().remove(spellCard);
		player.setFieldZone(spellCard);
		
		room.addLog("필드 마법 ["+spellCard.getName()+"] 발동");
		
		return room;
	}
	
	private int[] getFieldSpellBuffs(GameRoom room, GameCard monster){
		int[] buffs = {0,0};
		if (monster==null) return buffs;
		
		GameCard[] activeSpells = { room.getUser().getFieldZone(), room.getCpu().getFieldZone()};
		
		for (GameCard fieldSpell : activeSpells){
			if (fieldSpell == null) continue;
			List<EquipEffect> effects = equipEffectRepository.findBySpellId(fieldSpell.getId());
			
			for (EquipEffect effect : effects){
				boolean isMatch = false;
				String reqType = effect.getReqType();
				if (reqType == null || reqType.trim().isEmpty()){
					isMatch = true;
				} else if ("RACE".equalsIgnoreCase(effect.getReqType()) && effect.getReqValue().equals(monster.getRace())){
					isMatch = true;
				} else if ("ELEMENT".equalsIgnoreCase(effect.getReqType()) && effect.getReqValue().equals(monster.getElement())){
					isMatch = true;
				} else if ("NOT_RACE".equalsIgnoreCase(reqType) && !effect.getReqValue().equals(monster.getRace())){
					isMatch = true;
				} else if ("NOT_ELEMENT".equalsIgnoreCase(reqType) && !effect.getReqValue().equals(monster.getElement())) {
					isMatch = true;
				}
				
				
				if(isMatch){
					buffs[0] += effect.getAtkCh();
					buffs[1] += effect.getDefCh();
				}
			}
		}
		return buffs;
	}
}


























