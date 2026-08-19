import { adoptionScenario, authScenario, chatScenario } from '../main.js';
import { createSummary } from '../summary.js';
import { smokeThresholds, summaryTrendStats } from '../thresholds.js';

export const options = {
  summaryTrendStats,
  thresholds: smokeThresholds,
  scenarios: {
    adoption_smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '80s',
      exec: 'adoptionScenario',
    },
    auth_smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '80s',
      exec: 'authScenario',
    },
    chat_smoke: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 1,
      maxDuration: '80s',
      exec: 'chatScenario',
    },
  },
};

export { adoptionScenario, authScenario, chatScenario };
export const handleSummary = createSummary('smoke');
