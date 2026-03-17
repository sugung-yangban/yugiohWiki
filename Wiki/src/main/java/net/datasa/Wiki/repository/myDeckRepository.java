package net.datasa.Wiki.repository;

import net.datasa.Wiki.domain.entity.myDeck;
import net.datasa.Wiki.domain.entity.wikiMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface myDeckRepository extends JpaRepository<myDeck, Integer> {
	
	Optional<myDeck> findByMember_MemberIdAndDeckName(String memberId, String deckName);
	
	List<myDeck> findByMember (wikiMember member);
	
	List<myDeck> findByMember_MemberId(String member);
}
