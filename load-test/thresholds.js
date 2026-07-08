export const smokeThresholds = {
  checks: ['rate>0.95'],
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<1000'],
};

export const loadThresholds = {
  checks: ['rate>0.99'],
  http_req_failed: ['rate<0.01'],
  http_req_duration: ['p(95)<500', 'p(99)<2000'],
};

export const stressThresholds = {
  checks: ['rate>0.95'],
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<2000', 'p(99)<5000'],
};

export const spikeThresholds = {
  checks: ['rate>0.95'],
  http_req_failed: ['rate<0.05'],
  http_req_duration: ['p(95)<2000', 'p(99)<5000'],
};
