import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.05'],
  },
};

const jobTypes = [
  {
    type: 'resize_image',
    payload: { url: 'https://example.com/img.jpg', width: 800, height: 600 },
  },
  {
    type: 'send_email',
    payload: { to: 'k6@example.com', subject: 'k6 test', body: 'Hello' },
  },
  {
    type: 'generate_report',
    payload: { reportType: 'weekly' },
  },
];

export default function () {
  const payload = JSON.stringify(randomItem(jobTypes));
  const params = { headers: { 'Content-Type': 'application/json' } };

  const res = http.post('http://localhost:8080/api/v1/jobs', payload, params);

  check(res, {
    'status is 202': (r) => r.status === 202,
    'has jobId': (r) => r.json('jobId') !== undefined,
  });

  sleep(1);
}
