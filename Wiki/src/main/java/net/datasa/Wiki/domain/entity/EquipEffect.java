package net.datasa.Wiki.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class EquipEffect {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer equipEffectId;
	
	@Column(nullable = false)
	private Integer spellId;
	
	@Column(nullable = false)
	private String reqType;
	
	@Column(nullable = false)
	private String reqValue;
	
	@Column(nullable = false)
	private Integer atkCh;
	
	@Column(nullable = false)
	private Integer defCh;
	
	@Column(nullable = true)
	private String specialRule;
}
