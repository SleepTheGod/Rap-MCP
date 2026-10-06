import { z } from 'zod';

// ==========================================
// CANONICAL MODES & ENUMS
// ==========================================
export const BattleModeSchema = z.enum([
  'Classic',
  'Freestyle',
  'Themed',
  'Constraint',
  'Championship'
]);
export type BattleMode = z.infer<typeof BattleModeSchema>;

export const DifficultySchema = z.enum([
  'Rookie',
  'Competitor',
  'Elite',
  'Champion'
]);
export type Difficulty = z.infer<typeof DifficultySchema>;

export const CompetitorSchema = z.enum(['User', 'AI']);
export type Competitor = z.infer<typeof CompetitorSchema>;

export const BattleStatusSchema = z.enum([
  'Created',
  'Waiting',
  'UserTurn',
  'AiTurn',
  'Judging',
  'RoundComplete',
  'Completed',
  'Cancelled',
  'Failed'
]);
export type BattleStatus = z.infer<typeof BattleStatusSchema>;

export const TurnStatusSchema = z.enum([
  'Pending',
  'Active',
  'Submitted',
  'TimedOut',
  'Failed',
  'Cancelled'
]);
export type TurnStatus = z.infer<typeof TurnStatusSchema>;

export const SubmissionStatusSchema = z.enum([
  'Accepted',
  'Rejected',
  'TimedOut',
  'Failed'
]);
export type SubmissionStatus = z.infer<typeof SubmissionStatusSchema>;

export const RoundStatusSchema = z.enum([
  'Pending',
  'UserTurn',
  'AiTurn',
  'Judging',
  'Complete',
  'Failed'
]);
export type RoundStatus = z.infer<typeof RoundStatusSchema>;

// ==========================================
// CANONICAL SCORING CATEGORIES & WEIGHTS (SUM = 100)
// ==========================================
export const SCORING_CATEGORIES = [
  'Punchlines',
  'Wordplay',
  'Rhyme quality',
  'Multisyllabic rhyme quality',
  'Flow',
  'Creativity',
  'Originality',
  'Relevance',
  'Rebuttal quality',
  'Setup and payoff',
  'Thematic consistency',
  'Crowd appeal',
  'Overall impact'
] as const;

export type ScoringCategory = typeof SCORING_CATEGORIES[number];

export const CATEGORY_WEIGHTS: Record<ScoringCategory, number> = {
  'Punchlines': 12,
  'Wordplay': 10,
  'Rhyme quality': 10,
  'Multisyllabic rhyme quality': 6,
  'Flow': 8,
  'Creativity': 10,
  'Originality': 8,
  'Relevance': 6,
  'Rebuttal quality': 10,
  'Setup and payoff': 6,
  'Thematic consistency': 5,
  'Crowd appeal': 4,
  'Overall impact': 5
};

// Validate that sum is exactly 100
export const TOTAL_CATEGORY_WEIGHTS = Object.values(CATEGORY_WEIGHTS).reduce((a, b) => a + b, 0);

// ==========================================
// SCORING CALCULATION HELPERS
// ==========================================
export interface CategoryScoreInput {
  category: ScoringCategory;
  score: number; // 0 to 10
  explanation: string;
}

export interface ComputedCategoryScore {
  category: ScoringCategory;
  score: number;
  weight: number;
  weightedContribution: number;
  explanation: string;
}

export function computeVerseScore(categoryScores: CategoryScoreInput[]): {
  normalizedScore: number;
  breakdown: ComputedCategoryScore[];
} {
  let totalScore = 0;
  const breakdown: ComputedCategoryScore[] = categoryScores.map((cs) => {
    const weight = CATEGORY_WEIGHTS[cs.category] ?? 0;
    const clampedScore = Math.max(0, Math.min(10, Math.round(cs.score)));
    const weightedContribution = Number(((clampedScore / 10) * weight).toFixed(4));
    totalScore += weightedContribution;
    return {
      category: cs.category,
      score: clampedScore,
      weight,
      weightedContribution: Number(weightedContribution.toFixed(2)),
      explanation: cs.explanation
    };
  });

  return {
    normalizedScore: Number(totalScore.toFixed(2)),
    breakdown
  };
}

export function computeBattleFinalScore(
  roundScores: Array<{ roundNumber: number; roundWeight: number; score: number }>
): number {
  if (roundScores.length === 0) return 0;
  let totalWeighted = 0;
  let totalWeights = 0;
  for (const r of roundScores) {
    totalWeighted += r.score * r.roundWeight;
    totalWeights += r.roundWeight;
  }
  return totalWeights > 0 ? Number((totalWeighted / totalWeights).toFixed(2)) : 0;
}

export function determineWinner(
  userScore: number,
  aiScore: number,
  finalRoundUserScore?: number,
  finalRoundAiScore?: number
): 'User' | 'AI' | 'Draw' {
  const diff = Number((userScore - aiScore).toFixed(2));
  if (diff >= 0.01) return 'User';
  if (diff <= -0.01) return 'AI';
  
  // Tiebreaker Rule 43: Compare final round scores
  if (finalRoundUserScore !== undefined && finalRoundAiScore !== undefined) {
    const finalRoundDiff = Number((finalRoundUserScore - finalRoundAiScore).toFixed(2));
    if (finalRoundDiff >= 0.01) return 'User';
    if (finalRoundDiff <= -0.01) return 'AI';
  }
  return 'Draw';
}

// ==========================================
// SCHEMAS & INTERFACES
// ==========================================
export const BattleConfigurationSchema = z.object({
  roundCount: z.union([z.literal(3), z.literal(5), z.literal(7)]).default(5),
  verseMinimumLines: z.number().int().min(1).default(1),
  verseMaximumLines: z.number().int().max(16).default(16),
  verseMaximumCharacters: z.number().int().max(16000).default(16000),
  turnTimeoutSeconds: z.number().int().default(90),
  mode: BattleModeSchema.default('Classic'),
  difficulty: DifficultySchema.default('Competitor'),
  theme: z.string().optional(),
  constraints: z.array(z.string()).default([]),
  startingSide: CompetitorSchema.default('User'),
  finalRoundWeight: z.number().default(1.5),
  enabledScoringCategories: z.array(z.string()).default([...SCORING_CATEGORIES]),
  scoringVersion: z.string().default('1.0.0'),
  judgeVersion: z.string().default('1.0.0'),
  aiGenerationVersion: z.string().default('1.0.0'),
  battleRulesVersion: z.string().default('1.0.0')
});
export type BattleConfiguration = z.infer<typeof BattleConfigurationSchema>;

export const VerseSubmissionSchema = z.object({
  battleId: z.string().uuid(),
  roundNumber: z.number().int().min(1),
  content: z.string().min(1),
  idempotencyKey: z.string().optional()
});
export type VerseSubmission = z.infer<typeof VerseSubmissionSchema>;

export const MCPToolNameSchema = z.enum([
  'start_battle',
  'configure_battle',
  'submit_verse',
  'generate_ai_response',
  'evaluate_verse',
  'get_battle_state',
  'get_current_round',
  'get_scores',
  'get_statistics',
  'end_battle',
  'restart_battle',
  'get_history',
  'get_transcript'
]);
export type MCPToolName = z.infer<typeof MCPToolNameSchema>;

export const MCPScopeSchema = z.enum([
  'battle.read',
  'battle.create',
  'battle.configure',
  'battle.submit',
  'battle.generate',
  'battle.judge',
  'battle.end',
  'battle.restart',
  'battle.history',
  'battle.statistics'
]);
export type MCPScope = z.infer<typeof MCPScopeSchema>;
