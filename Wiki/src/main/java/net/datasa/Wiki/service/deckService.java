package net.datasa.Wiki.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.datasa.Wiki.domain.dto.DeckSaveDTO;
import net.datasa.Wiki.domain.dto.boosterpackResponse;
import net.datasa.Wiki.domain.entity.boosterpack;
import net.datasa.Wiki.domain.entity.deckCard;
import net.datasa.Wiki.domain.entity.myDeck;
import net.datasa.Wiki.domain.entity.wikiMember;
import net.datasa.Wiki.repository.BoosterRepository;
import net.datasa.Wiki.repository.deckCardRepository;
import net.datasa.Wiki.repository.myDeckRepository;
import net.datasa.Wiki.repository.wikiMemberRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class deckService {
	private final BoosterRepository boosterRepository;
	private final deckCardRepository deckCardRepository;
	private final myDeckRepository myDeckRepository;
	private final wikiMemberRepository wikiMemberRepository;
	
	public void createDeck(String loginId, DeckSaveDTO deckDTO){
		wikiMember member = wikiMemberRepository.findById(loginId).orElseThrow(() -> new RuntimeException("회원정보 확인 불가"));
		
		Optional<myDeck> existingDeck = myDeckRepository.findByMember_MemberIdAndDeckName(loginId, deckDTO.getName());
		
		myDeck savedDeck;
		
		if (existingDeck.isPresent()){
			savedDeck = existingDeck.get();
			deckCardRepository.deleteByMyDeck(savedDeck);
			deckCardRepository.flush();
		} else {
			myDeck newDeck = new myDeck();
			newDeck.setDeckName(deckDTO.getName());
			newDeck.setMember(member);
			
			savedDeck = myDeckRepository.save(newDeck);
		}
		
		List<Integer> cardIdList = deckDTO.getCardIds();
		
		if (cardIdList != null && !cardIdList.isEmpty()) {
			for (Integer cardId : cardIdList) {
				boosterpack card = boosterRepository.findById(cardId).orElseThrow(()-> new RuntimeException("해당 카드번호는 존재하지않습니다"));
				deckCard deckCardEntity = new deckCard();
				deckCardEntity.setMyDeck(savedDeck);
				deckCardEntity.setCard(card);
				
				deckCardRepository.save(deckCardEntity);
			}
		}
		
	}
	
	public List<myDeck> getMyDecks(String  loginId){
		wikiMember member = wikiMemberRepository.findById(loginId).orElseThrow(() -> new RuntimeException("로그인 불가"));
		return myDeckRepository.findByMember(member);
	}
	
	public List<boosterpackResponse> getDeckCards(Integer deckId) {
		myDeck deck = myDeckRepository.findById(deckId).orElseThrow(() -> new RuntimeException("덱을 찾을 수 없습니다."));
		
		List<deckCard> deckCards = deckCardRepository.findByMyDeck(deck);
		List<boosterpackResponse> responseList = new ArrayList<>();
		
		for (deckCard dc : deckCards) {
			boosterpack card = dc.getCard();
			boosterpackResponse dto = boosterpackResponse.builder()
					.id(card.getId())
					.cardname(card.getCardname())
					.atk(card.getAtk())
					.def(card.getDef())
					.cardNumber(card.getCardNumber())
					.element(card.getElement())
					.race(card.getRace())
					.level(card.getLevel())
					.text(card.getText())
					.type(card.getType())
					.cardtype(card.getCardtype())
					.build();
			responseList.add(dto);
		}
		return responseList;
	}
	public void deletedeck(Integer deckId, String loginId){
		myDeck deck =  myDeckRepository.findById(deckId).orElseThrow(() -> new RuntimeException("덱을 찾을 수 없습니다"));
		
		if(!deck.getMember().getMemberId().equals(loginId)){
			throw new RuntimeException("삭제권한이 없습니다");
		}
		List<deckCard> cards = deckCardRepository.findByMyDeck(deck);
		deckCardRepository.deleteAll(cards);
		myDeckRepository.delete(deck);
	}
}
