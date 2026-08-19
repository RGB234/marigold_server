import { storageUploadScenario } from '../scenarios/storage.js';
import {
  IO_MAX_VUS,
  IO_PRE_ALLOCATED_VUS,
  IO_TEST_DURATION,
  STORAGE_UPLOAD_RATE,
} from '../config.js';
import { createSummary } from '../summary.js';
import { loadThresholds, summaryTrendStats } from '../thresholds.js';

export const options = {
  summaryTrendStats,
  thresholds: loadThresholds,
  scenarios: {
    storage_upload: {
      executor: 'constant-arrival-rate',
      rate: STORAGE_UPLOAD_RATE,
      timeUnit: '1s',
      duration: IO_TEST_DURATION,
      preAllocatedVUs: IO_PRE_ALLOCATED_VUS,
      maxVUs: IO_MAX_VUS,
      exec: 'storageUploadScenario',
    },
  },
};

export { storageUploadScenario };
export const handleSummary = createSummary('storage-upload');
