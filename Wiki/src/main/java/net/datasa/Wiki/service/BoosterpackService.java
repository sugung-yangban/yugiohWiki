package net.datasa.Wiki.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datasa.Wiki.domain.dto.boosterpackResponse;
import net.datasa.Wiki.domain.entity.boosterpack;
import net.datasa.Wiki.repository.BoosterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoosterpackService {
private final BoosterRepository boosterRepository;
public boosterpackResponse select(String cardname){
	boosterpack boosterpack = boosterRepository.findByCardname(cardname).orElse(null);
	
	boosterpackResponse response = new boosterpackResponse();
	response.setId(boosterpack.getId());
	response.setCardid(boosterpack.getCardid());
	response.setCardname(boosterpack.getCardname());
	response.setRarity(boosterpack.getRarity());
	response.setLevel(boosterpack.getLevel());
	response.setAtk(boosterpack.getAtk());
	response.setDef(boosterpack.getDef());
	response.setText(boosterpack.getText());
	response.setCardNumber(boosterpack.getCardNumber());
	return response;
}
public List<boosterpackResponse> selectAll(String sortField, String sortDir){
	Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
List<boosterpack> boosterpacks = boosterRepository.findAll(sort);
List<boosterpackResponse> responseList =new ArrayList<>();
for(boosterpack entity : boosterpacks){
	boosterpackResponse response = new boosterpackResponse();
	response.setPackname(entity.getPackname());
	response.setId(entity.getId());
	response.setCardid(entity.getCardid());
	response.setRarity(entity.getRarity());
	response.setCardtype(entity.getCardtype());
	response.setType(entity.getType());
	response.setCardname(entity.getCardname());
	response.setElement(entity.getElement());
	response.setRace(entity.getRace());
	response.setLevel(entity.getLevel());
	response.setAtk(entity.getAtk());
	response.setDef(entity.getDef());
	response.setText(entity.getText());
	response.setReleasedate(entity.getReleasedate());
	response.setCardNumber(entity.getCardNumber());
	responseList.add(response);
}
return responseList;
}
public Page<boosterpackResponse> getPaging(int page, String keyword, String race, String element, String levelStr, String sortStr, String cardtype, String type) {
	Sort sort = Sort.by(Sort.Direction.ASC, "id");
	if(sortStr != null && !sortStr.isEmpty()){
		switch (sortStr){
			case "atk_desc": sort = Sort.by(Sort.Direction.DESC, "atk"); break;
			case "atk_asc": sort = Sort.by(Sort.Direction.ASC, "atk"); break;
			case "def_desc": sort = Sort.by(Sort.Direction.DESC, "def"); break;
			case "def_asc": sort = Sort.by(Sort.Direction.ASC, "def"); break;
			case "newest": sort = Sort.by(Sort.Direction.DESC, "releasedate"); break;
		}
	}
	Pageable pageable = PageRequest.of(page, 21, sort);
	String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? null: keyword;
	String searchRace = (race == null || race.equals("all") || race.isEmpty()) ? null: race;
	String searchElement = (element == null || element.equals("all") || element.isEmpty()) ? null: element;
	Integer searchLevel = null;
	if(levelStr != null && !levelStr.equals("all") && !levelStr.isEmpty()) {
		try {
			searchLevel = Integer.parseInt(levelStr);
		} catch (NumberFormatException e){
		
		}
	}
	String searchCardtype = (cardtype == null || cardtype.trim().isEmpty()) ? "all" : cardtype;
	String searchType = (type == null || type.trim().isEmpty()) ? "all" : type;
	Page<boosterpack> entityPage = boosterRepository.findByComplexCondition(searchKeyword, searchRace, searchElement, searchLevel, searchCardtype, searchType, pageable);
	return entityPage.map(entity -> {
		boosterpackResponse dto = new boosterpackResponse();
		dto.setPackname(entity.getPackname());
		dto.setId(entity.getId());
		dto.setCardname(entity.getCardname());
		dto.setCardid(entity.getCardid());
		dto.setRarity(entity.getRarity());
		dto.setCardtype(entity.getCardtype());
		dto.setType(entity.getType());
		dto.setElement(entity.getElement());
		dto.setRace(entity.getRace());
		dto.setLevel(entity.getLevel());
		dto.setAtk(entity.getAtk());
		dto.setDef(entity.getDef());
		dto.setText(entity.getText());
		dto.setReleasedate(entity.getReleasedate());
		dto.setCardNumber(entity.getCardNumber());
		return dto;
	});
	
}
}
