function metricValue(data, name, key) {
  return data.metrics[name]?.values?.[key] ?? 'n/a';
}

export function createSummary(name) {
  return function handleSummary(data) {
    const lines = [
      `test=${name}`,
      `http_req_failed.rate=${metricValue(data, 'http_req_failed', 'rate')}`,
      `http_req_duration.p95=${metricValue(data, 'http_req_duration', 'p(95)')}`,
      `http_req_duration.p99=${metricValue(data, 'http_req_duration', 'p(99)')}`,
      `checks.rate=${metricValue(data, 'checks', 'rate')}`,
      '',
    ];

    return {
      stdout: lines.join('\n'),
      [`results/${name}-summary.json`]: JSON.stringify(data, null, 2),
    };
  };
}
