import { dbMixedReadScenario, dbMixedWriteScenario } from '../scenarios/db.js';
import {
  DB_MIXED_RATE,
  IO_MAX_VUS,
  IO_PRE_ALLOCATED_VUS,
  IO_TEST_DURATION,
} from '../config.js';
import { createSummary } from '../summary.js';
import { loadThresholds, summaryTrendStats } from '../thresholds.js';

const readRate = Math.max(1, Math.floor(DB_MIXED_RATE * 0.8));
const writeRate = Math.max(1, DB_MIXED_RATE - readRate);

export const options = {
  summaryTrendStats,
  thresholds: loadThresholds,
  scenarios: {
    db_mixed_read: {
      executor: 'constant-arrival-rate',
      rate: readRate,
      timeUnit: '1s',
      duration: IO_TEST_DURATION,
      preAllocatedVUs: IO_PRE_ALLOCATED_VUS,
      maxVUs: IO_MAX_VUS,
      exec: 'dbMixedReadScenario',
    },
    db_mixed_write: {
      executor: 'constant-arrival-rate',
      rate: writeRate,
      timeUnit: '1s',
      duration: IO_TEST_DURATION,
      preAllocatedVUs: IO_PRE_ALLOCATED_VUS,
      maxVUs: IO_MAX_VUS,
      exec: 'dbMixedWriteScenario',
    },
  },
};

export { dbMixedReadScenario, dbMixedWriteScenario };
export const handleSummary = createSummary('db-mixed');
