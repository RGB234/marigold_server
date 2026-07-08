import { adoptionScenario, authScenario, chatScenario } from '../main.js';
import { createSummary } from '../summary.js';
import { smokeThresholds } from '../thresholds.js';

export const options = {
  thresholds: smokeThresholds,
  scenarios: {
    adoption_smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      exec: 'adoptionScenario',
    },
    auth_smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      exec: 'authScenario',
    },
    chat_smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      exec: 'chatScenario',
    },
  },
};

export { adoptionScenario, authScenario, chatScenario };
export const handleSummary = createSummary('smoke');
