package net.datasa.Wiki.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datasa.Wiki.domain.entity.boosterpack;
import net.datasa.Wiki.repository.BoosterRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
public class mainController {
	private final BoosterRepository boosterRepository;
	@GetMapping({" ","/","main"})
	public String main(Model model) {
		List<String> targetRarities = Arrays.asList("SecretRare","UltraRare");
		boosterpack latestRareCard = boosterRepository.findTop1ByRarityInOrderByReleasedateDesc(targetRarities);
		List<boosterpack> finalCardList;
		
		if (latestRareCard != null){
			LocalDate latestDate = latestRareCard.getReleasedate();
			log.info("가장 최근의 레어카드 출시일 발견: {}", latestDate);
			finalCardList = boosterRepository.findByReleasedateAndRarityIn(latestDate, targetRarities);
		} else {
			finalCardList = List.of();
		}
		model.addAttribute("rareCards",finalCardList);
		log.debug("실행");
		return "wikimain";
	}
}
