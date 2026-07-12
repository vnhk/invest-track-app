package com.bervan.investtrack.api;

import com.bervan.budget.BudgetEntryTag;
import com.bervan.budget.entry.BudgetEntry;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.core.model.CustomMapper;
import com.bervan.core.model.DefaultCustomMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

@Service
public class BudgetTagDtoToModelMapper implements DefaultCustomMapper<String, Set<BudgetEntryTag>> {

    private final BudgetEntryService budgetEntryService;

    public BudgetTagDtoToModelMapper(BudgetEntryService budgetEntryService) {
        this.budgetEntryService = budgetEntryService;
    }

    @Override
    public Set<BudgetEntryTag> map(String obj, Field fromField, Field toField) {
        if (obj == null) return new HashSet<>();

        if (obj.isBlank()) return new HashSet<>();
        if (obj.isEmpty()) return new HashSet<>();
        if (obj.equals(",")) return new HashSet<>();

        String[] tags = obj.split(",");
        Set<BudgetEntryTag> res = new HashSet<>();

        for (String tagName : tags) {
            tagName = tagName.trim();
            if (tagName.isBlank()) continue;

            BudgetEntryTag found = findOrCreateTag(tagName);
            res.add(found);
        }

        return res;
    }

    private BudgetEntryTag findOrCreateTag(String tagName) {
        // Search in all existing entries for this tag name
        Set<BudgetEntry> allEntries = budgetEntryService.load(PageRequest.of(0, Integer.MAX_VALUE));
        for (BudgetEntry e : allEntries) {
            for (BudgetEntryTag tag : e.getTags()) {
                if (tagName.equals(tag.getName())) {
                    // Return existing tag - it can be shared between entries
                    return tag;
                }
            }
        }

        // Create new tag if not found
        BudgetEntryTag newTag = new BudgetEntryTag();
        newTag.setName(tagName);
        return newTag;
    }

    @Override
    public Class<String> getFrom() {
        return String.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Set<BudgetEntryTag>> getTo() {
        return (Class<Set<BudgetEntryTag>>) (Class<?>) Set.class;
    }
}
