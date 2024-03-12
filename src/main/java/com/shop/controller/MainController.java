package com.shop.controller;

import com.shop.dto.ItemSearchDto;
import com.shop.dto.MainItemDto;
import com.shop.service.ItemService;
import com.shop.service.ProductMaker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MainController {
    private final ItemService itemService;
    private final ProductMaker productMaker;

    @GetMapping("/")
    public String root(ItemSearchDto itemSearchDto, Integer page, Model model) {
        Pageable pageable = PageRequest.of(Optional.ofNullable(page).orElse(0), 12);
        if (itemSearchDto.getSearchQuery() == null) {
            itemSearchDto.setSearchQuery("");
        }
        Page<MainItemDto> items = itemService.getMainItemPage(itemSearchDto, pageable);
        model.addAttribute("items", items);
        model.addAttribute("itemSearchDto", itemSearchDto);
        model.addAttribute("maxPage", pageable.getPageSize());
        return "main/main";
    }

    @GetMapping("/aboutus")
    public String aboutus() {
        return "main/aboutus";
    }

    @GetMapping("/admin/make/{search}")
    public ResponseEntity<?> make(@PathVariable String search) {
        try {
            productMaker.make(search);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok().body("성공");
    }
}
