package net.datasa.Wiki.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="deck_card")
@Data
public class deckCard {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "deck_id")
	private myDeck myDeck;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="card_id")
	private boosterpack card;
}
