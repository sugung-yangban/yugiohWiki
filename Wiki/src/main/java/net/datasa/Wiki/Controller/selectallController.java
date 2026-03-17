package net.datasa.Wiki.Controller;

import lombok.RequiredArgsConstructor;
import net.datasa.Wiki.domain.dto.boosterpackResponse;
import net.datasa.Wiki.service.BoosterpackService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("showme")
@RequiredArgsConstructor
public class selectallController {
	private final BoosterpackService boosterpackService;
	
	@GetMapping("/selectAll")
	public String showAll(Model model, @RequestParam(value = "sortField", defaultValue = "id") String sortField,
	@RequestParam(value = "sortDir", defaultValue = "asc") String sortDir){
		List<boosterpackResponse> boosterpackResponses =boosterpackService.selectAll(sortField,sortDir);
		model.addAttribute("boosterpackResponses", boosterpackResponses);
		model.addAttribute("sortField", sortField);
		model.addAttribute("sortDir",sortDir);
		model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
		return "selectAll";
	}
    @GetMapping("/search")
    public String search(){
        return "search";
    }
    @GetMapping("/searchResult")
    public String searchResult(@RequestParam("cardname") String cardname, Model model){
		boosterpackResponse response = null;
		try {
			response = boosterpackService.select(cardname);
		} catch (Exception e){
		}
		
        model.addAttribute("response", response);
        return "searchResult";
    }
}
