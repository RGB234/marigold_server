export const summaryTrendStats = ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'];

const endpointDurationThresholds = {
  'http_req_duration{name:adoption_list}': ['max>=0'],
  'http_req_duration{name:adoption_detail}': ['max>=0'],
  'http_req_duration{name:auth_login}': ['max>=0'],
  'http_req_duration{name:chat_auth_login}': ['max>=0'],
  'http_req_duration{name:db_read_adoption_list}': ['max>=0'],
  'http_req_duration{name:db_read_adoption_detail}': ['max>=0'],
  'http_req_duration{name:db_read_comments}': ['max>=0'],
  'http_req_duration{name:db_read_writer_posts}': ['max>=0'],
  'http_req_duration{name:db_write_auth_login}': ['max>=0'],
  'http_req_duration{name:db_write_writer_posts}': ['max>=0'],
  'http_req_duration{name:db_write_status_reserved}': ['max>=0'],
  'http_req_duration{name:db_write_status_proceeding}': ['max>=0'],
  'http_req_duration{name:storage_auth_login}': ['max>=0'],
  'http_req_duration{name:storage_post_create}': ['max>=0'],
  'http_req_duration{name:storage_post_delete}': ['max>=0'],
  'http_req_duration{name:storage_mixed_auth_login}': ['max>=0'],
  'http_req_duration{name:storage_mixed_post_create}': ['max>=0'],
  'http_req_duration{name:storage_mixed_post_update}': ['max>=0'],
  'http_req_duration{name:storage_mixed_post_delete}': ['max>=0'],
  'ws_connecting{name:chat_websocket}': ['max>=0'],
};

export const smokeThresholds = {
  checks: ['rate>0.95'],
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<1000'],
  ...endpointDurationThresholds,
};

export const loadThresholds = {
  checks: ['rate>0.99'],
  http_req_failed: ['rate<0.01'],
  http_req_duration: ['p(95)<500', 'p(99)<2000'],
  ...endpointDurationThresholds,
};

export const stressThresholds = {
  checks: ['rate>0.95'],
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<2000', 'p(99)<5000'],
  ...endpointDurationThresholds,
};

export const spikeThresholds = {
  checks: ['rate>0.95'],
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<2000', 'p(99)<5000'],
  ...endpointDurationThresholds,
};
