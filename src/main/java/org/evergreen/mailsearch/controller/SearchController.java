package org.evergreen.mailsearch.controller;

import org.evergreen.mailsearch.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/")
    public String home() {
        return "search";
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "query", required = false, defaultValue = "") String queryString,
                         @RequestParam(defaultValue = "all") String dateRange,
                         @RequestParam(defaultValue = "all") String category,
                         Model model) {

        Map<String, Object> searchResult = searchService.search(queryString, dateRange, category);

        model.addAttribute("query", queryString);
        model.addAttribute("dateRange", dateRange);
        model.addAttribute("category", category);
        model.addAttribute("emails", searchResult.get("emails"));
        model.addAttribute("totalHits", searchResult.get("totalHits"));

        return "search";
    }
}
