# Canonical Scoring Specification

## 13 Categories and Weights
| Category | Weight | Focus |
|---|---|---|
| Punchlines | 12 | Setup, surprise, impact, precision, memorability |
| Wordplay | 10 | Puns, double entendres, homophones, figurative language |
| Rhyme quality | 10 | End rhymes, internal rhymes, phonetic precision |
| Multisyllabic rhyme quality | 6 | Complexity, syllabic matching, natural cadence |
| Flow | 8 | Cadence implied by rhythm, syllabic balance, line structure |
| Creativity | 10 | Distinctive angles, novel imagery, concept execution |
| Originality | 8 | Freshness within the specific battle context |
| Relevance | 6 | Adherence to context, theme, and battle environment |
| Rebuttal quality | 10 | Direct counters, callbacks, reframing (Response Quality in Turn 1) |
| Setup and payoff | 6 | Structural cohesion leading to punchlines |
| Thematic consistency | 5 | Adherence to mandatory theme/constraints |
| Crowd appeal | 4 | Energy, showmanship, memorability |
| Overall impact | 5 | Holistic balance without overriding category scores |
| **Total** | **100** | Full range 0.00 to 100.00 |

## Calculation Formula
- Each category is scored on an integer scale 0 to 10.
- Contribution = `(category_score / 10) * category_weight`.
- Verse Score = `sum(contributions)`.
- Final Battle Score = `sum(weighted_round_scores) / sum(round_weights)`.
- Non-final rounds: Weight = 1.0. Final configured round: Weight = 1.5.
- Winner determined by 2-decimal comparison: difference >= 0.01 wins.
- Tiebreaker: Final round score comparison; if still equal, declared Draw.
