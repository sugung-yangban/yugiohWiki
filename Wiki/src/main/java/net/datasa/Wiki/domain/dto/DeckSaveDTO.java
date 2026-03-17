package net.datasa.Wiki.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class DeckSaveDTO {
	private String name;
	private List<Integer> cardIds;
}
