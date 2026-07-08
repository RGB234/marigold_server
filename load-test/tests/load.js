import { adoptionScenario, authScenario, chatScenario } from '../main.js';
import { createSummary } from '../summary.js';
import { loadThresholds } from '../thresholds.js';

export const options = {
  thresholds: loadThresholds,
  scenarios: {
    adoption_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10m', target: 75 },
        { duration: '30m', target: 75 },
        { duration: '5m', target: 0 },
      ],
      exec: 'adoptionScenario',
    },
    auth_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10m', target: 5 },
        { duration: '30m', target: 5 },
        { duration: '5m', target: 0 },
      ],
      exec: 'authScenario',
    },
    chat_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10m', target: 20 },
        { duration: '30m', target: 20 },
        { duration: '5m', target: 0 },
      ],
      exec: 'chatScenario',
    },
  },
};

export { adoptionScenario, authScenario, chatScenario };
export const handleSummary = createSummary('load');
