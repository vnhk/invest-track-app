package com.bervan.investtrack.api;

import com.bervan.budget.BudgetEntryTag;
import com.bervan.budget.entry.BudgetEntryService;
import com.bervan.core.model.DefaultCustomMapper;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Set;

@Service
public class BudgetTagToDtoMapper implements DefaultCustomMapper<Set<BudgetEntryTag>, String> {

    private final BudgetEntryService budgetEntryService;

    public BudgetTagToDtoMapper(BudgetEntryService budgetEntryService) {
        this.budgetEntryService = budgetEntryService;
    }

    @Override
    public String map(Set<BudgetEntryTag> obj, Field fromField, Field toField) {
        if (obj == null) return null;

        if (obj.isEmpty()) return null;

        StringBuilder tagsBuilder = new StringBuilder();
        for (BudgetEntryTag tag : obj) {
            if (tag.getName() != null && !tag.getName().isBlank()) {
                if (tagsBuilder.length() > 0) {
                    tagsBuilder.append(",");
                }
                tagsBuilder.append(tag.getName());
            }
        }
        return tagsBuilder.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<Set<BudgetEntryTag>> getFrom() {
        return (Class<Set<BudgetEntryTag>>) (Class<?>) Set.class;
    }

    @Override
    public Class<String> getTo() {
        return String.class;
    }
}
