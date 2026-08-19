import { storageMixedScenario } from '../scenarios/storage.js';
import {
  IO_MAX_VUS,
  IO_PRE_ALLOCATED_VUS,
  IO_TEST_DURATION,
  STORAGE_MIXED_RATE,
} from '../config.js';
import { createSummary } from '../summary.js';
import { loadThresholds, summaryTrendStats } from '../thresholds.js';

export const options = {
  summaryTrendStats,
  thresholds: loadThresholds,
  scenarios: {
    storage_mixed: {
      executor: 'constant-arrival-rate',
      rate: STORAGE_MIXED_RATE,
      timeUnit: '1s',
      duration: IO_TEST_DURATION,
      preAllocatedVUs: IO_PRE_ALLOCATED_VUS,
      maxVUs: IO_MAX_VUS,
      exec: 'storageMixedScenario',
    },
  },
};

export { storageMixedScenario };
export const handleSummary = createSummary('storage-mixed');
