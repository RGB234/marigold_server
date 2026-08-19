import { dbWriteScenario } from '../scenarios/db.js';
import {
  DB_WRITE_RATE,
  IO_MAX_VUS,
  IO_PRE_ALLOCATED_VUS,
  IO_TEST_DURATION,
} from '../config.js';
import { createSummary } from '../summary.js';
import { loadThresholds, summaryTrendStats } from '../thresholds.js';

export const options = {
  summaryTrendStats,
  thresholds: loadThresholds,
  scenarios: {
    db_write: {
      executor: 'constant-arrival-rate',
      rate: DB_WRITE_RATE,
      timeUnit: '1s',
      duration: IO_TEST_DURATION,
      preAllocatedVUs: IO_PRE_ALLOCATED_VUS,
      maxVUs: IO_MAX_VUS,
      exec: 'dbWriteScenario',
    },
  },
};

export { dbWriteScenario };
export const handleSummary = createSummary('db-write');
