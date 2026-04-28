package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;

import java.util.List;
import java.util.Map;

public class RubricFamily {

    private final Map<String, RubricDimension> dimensions;
    private final List<MappingRule> rules;
    private final String defaultRubricId;

    public RubricFamily(Map<String, RubricDimension> dimensions, List<MappingRule> rules, String defaultRubricId) {
        this.dimensions = Map.copyOf(dimensions);
        this.rules = List.copyOf(rules);
        this.defaultRubricId = defaultRubricId;
    }

    public RubricDimension getDimension(String ref) {
        return dimensions.get(ref);
    }

    public Map<String, RubricDimension> getDimensions() {
        return dimensions;
    }

    public String resolve(RubricResolutionContext ctx) {
        for (MappingRule rule : rules) {
            if (rule.matches(ctx)) {
                return rule.use();
            }
        }
        return defaultRubricId;
    }

    public record MappingRule(
            Boolean resumeTrack,
            List<String> categories,
            String feedbackPerspective,
            String use
    ) {
        public boolean matches(RubricResolutionContext ctx) {
            if (resumeTrack != null && resumeTrack) {
                return ctx.resumeTrack();
            }
            if (categories != null && !categories.isEmpty()) {
                if (ctx.category() == null) {
                    return false;
                }
                return categories.contains(ctx.category().name());
            }
            if (feedbackPerspective != null) {
                if (ctx.feedbackPerspective() == null) {
                    return false;
                }
                return feedbackPerspective.equals(ctx.feedbackPerspective().name());
            }
            return false;
        }
    }

    public record RubricResolutionContext(
            boolean resumeTrack,
            QuestionSetCategory category,
            FeedbackPerspective feedbackPerspective
    ) {}
}
