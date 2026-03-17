package net.datasa.Wiki.repository;

import net.datasa.Wiki.domain.entity.deckCard;
import net.datasa.Wiki.domain.entity.myDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface deckCardRepository extends JpaRepository<deckCard,Integer> {
	List<deckCard> findByMyDeck(myDeck deck);
	void deleteByMyDeck(myDeck myDeck);
}
