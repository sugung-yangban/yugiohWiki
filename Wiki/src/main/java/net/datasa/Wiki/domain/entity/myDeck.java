package net.datasa.Wiki.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EntityListeners(AuditingEntityListener.class)
@Table(name = "my_Deck")
public class myDeck {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer deckId;
	@Column(nullable = false)
	private String deckName;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="member_id")
	private wikiMember member;
	@CreatedDate
	private LocalDateTime createdDate;
	@OneToMany(mappedBy = "myDeck", cascade = CascadeType.REMOVE)
	private List<deckCard> deckCards = new ArrayList<>();
}
