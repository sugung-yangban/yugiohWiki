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
public class FusionRecipe {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer recipeId;
	
	@Column(nullable = false)
	private Integer fusionMonsterId;
	
	@Column(nullable = false)
	private String reqType;
	
	@Column(nullable = false)
	private String reqValue;
	
	@Column(nullable = false)
	private Integer reqCount;
}
