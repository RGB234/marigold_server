import { adoptionScenario, authScenario, chatScenario } from '../main.js';
import { createSummary } from '../summary.js';
import { stressThresholds, summaryTrendStats } from '../thresholds.js';

export const options = {
  summaryTrendStats,
  thresholds: stressThresholds,
  scenarios: {
    adoption_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 40 },
        { duration: '10m', target: 75 },
        { duration: '10m', target: 150 },
        { duration: '10m', target: 300 },
        { duration: '5m', target: 0 },
      ],
      exec: 'adoptionScenario',
    },
    auth_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 3 },
        { duration: '10m', target: 5 },
        { duration: '10m', target: 10 },
        { duration: '10m', target: 20 },
        { duration: '5m', target: 0 },
      ],
      exec: 'authScenario',
    },
    chat_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 10 },
        { duration: '10m', target: 20 },
        { duration: '10m', target: 40 },
        { duration: '10m', target: 80 },
        { duration: '5m', target: 0 },
      ],
      exec: 'chatScenario',
    },
  },
};

export { adoptionScenario, authScenario, chatScenario };
export const handleSummary = createSummary('stress');
