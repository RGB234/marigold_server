import { adoptionScenario, authScenario, chatScenario } from '../main.js';
import { createSummary } from '../summary.js';
import { spikeThresholds, summaryTrendStats } from '../thresholds.js';

export const options = {
  summaryTrendStats,
  thresholds: spikeThresholds,
  scenarios: {
    adoption_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 15 },
        { duration: '30s', target: 225 },
        { duration: '3m', target: 225 },
        { duration: '30s', target: 15 },
        { duration: '10m', target: 15 },
        { duration: '1m', target: 0 },
      ],
      exec: 'adoptionScenario',
    },
    auth_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 1 },
        { duration: '30s', target: 15 },
        { duration: '3m', target: 15 },
        { duration: '30s', target: 1 },
        { duration: '10m', target: 1 },
        { duration: '1m', target: 0 },
      ],
      exec: 'authScenario',
    },
    chat_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5m', target: 4 },
        { duration: '30s', target: 60 },
        { duration: '3m', target: 60 },
        { duration: '30s', target: 4 },
        { duration: '10m', target: 4 },
        { duration: '1m', target: 0 },
      ],
      exec: 'chatScenario',
    },
  },
};

export { adoptionScenario, authScenario, chatScenario };
export const handleSummary = createSummary('spike');
