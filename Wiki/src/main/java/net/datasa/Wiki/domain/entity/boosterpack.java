package net.datasa.Wiki.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Entity// JPA가 관리하는 엔티티 객체로 등록
@Getter
@Setter
@ToString
@Table(name = "boosterpack") // boosterpack 테이블과 매핑
public class boosterpack {
	@Column(name = "pack_name", length = 100)
	private String packname;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;
	@Column(name = "card_id")
	private Integer cardid;
	@Column(name = "rarity", length = 20)
	private String rarity;
	@Column(name = "card_type", length = 20)
	private String cardtype;
	@Column(name = "type", length = 20)
	private String type;
	@Column(name = "card_name", length = 100, nullable = false)
	private String cardname;
	@Column(name = "element", length = 20)
	private String element;
	@Column(name = "race", length = 50)
	private String race;
	@Column(name = "atk")
	private Integer atk;
	@Column(name = "level")
	private Integer level;
	@Column(name = "def")
	private Integer def;
	@Column(name = "text", length = 300)
	private String text;
	@Column(name = "release_date")
	private LocalDate releasedate;
	@Column(name = "cardNumber")
	private Integer cardNumber;
}
