function metricValue(data, name, key) {
  return data.metrics[name]?.values?.[key] ?? 'n/a';
}

const endpointMetrics = [
  ['adoption_list', 'http_req_duration{name:adoption_list}'],
  ['adoption_detail', 'http_req_duration{name:adoption_detail}'],
  ['auth_login', 'http_req_duration{name:auth_login}'],
  ['chat_auth_login', 'http_req_duration{name:chat_auth_login}'],
  ['db_read_adoption_list', 'http_req_duration{name:db_read_adoption_list}'],
  ['db_read_adoption_detail', 'http_req_duration{name:db_read_adoption_detail}'],
  ['db_read_comments', 'http_req_duration{name:db_read_comments}'],
  ['db_read_writer_posts', 'http_req_duration{name:db_read_writer_posts}'],
  ['db_write_auth_login', 'http_req_duration{name:db_write_auth_login}'],
  ['db_write_writer_posts', 'http_req_duration{name:db_write_writer_posts}'],
  ['db_write_status_reserved', 'http_req_duration{name:db_write_status_reserved}'],
  ['db_write_status_proceeding', 'http_req_duration{name:db_write_status_proceeding}'],
  ['storage_auth_login', 'http_req_duration{name:storage_auth_login}'],
  ['storage_post_create', 'http_req_duration{name:storage_post_create}'],
  ['storage_post_delete', 'http_req_duration{name:storage_post_delete}'],
  ['storage_mixed_auth_login', 'http_req_duration{name:storage_mixed_auth_login}'],
  ['storage_mixed_post_create', 'http_req_duration{name:storage_mixed_post_create}'],
  ['storage_mixed_post_update', 'http_req_duration{name:storage_mixed_post_update}'],
  ['storage_mixed_post_delete', 'http_req_duration{name:storage_mixed_post_delete}'],
  ['chat_websocket_connecting', 'ws_connecting{name:chat_websocket}'],
];

export function createSummary(name) {
  return function handleSummary(data) {
    const lines = [
      `test=${name}`,
      `http_req_failed.rate=${metricValue(data, 'http_req_failed', 'rate')}`,
      `http_req_duration.p95=${metricValue(data, 'http_req_duration', 'p(95)')}`,
      `http_req_duration.p99=${metricValue(data, 'http_req_duration', 'p(99)')}`,
      `checks.rate=${metricValue(data, 'checks', 'rate')}`,
      ...endpointMetrics.flatMap(([label, metricName]) => [
        `${label}.p95=${metricValue(data, metricName, 'p(95)')}`,
        `${label}.p99=${metricValue(data, metricName, 'p(99)')}`,
      ]),
      '',
    ];

    return {
      stdout: lines.join('\n'),
      [`results/${name}-summary.json`]: JSON.stringify(data, null, 2),
    };
  };
}
