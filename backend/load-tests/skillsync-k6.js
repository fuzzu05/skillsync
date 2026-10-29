import http from 'k6/http';
import { check, sleep } from 'k6';

// Configurable options for k6 stages
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Baseline load
    { duration: '1m', target: 50 },    // Gradual ramp-up
    { duration: '1m', target: 100 },   // Moderate load
    { duration: '30s', target: 250 },  // High load spike
    { duration: '30s', target: 0 },    // Cool-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.01'],    // Error rate below 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export function setup() {
  // Pre-seed an authenticated user for browsing
  const email = `k6_runner_${Date.now()}@test.com`;
  const password = 'Password@123';

  // 1. Register
  http.post(`${BASE_URL}/api/v1/auth/register`, JSON.stringify({
    email,
    password,
    fullName: 'k6 Benchmark Runner',
    department: 'Computer Science',
    yearOfStudy: 4,
  }), { headers: { 'Content-Type': 'application/json' } });

  // 2. Login
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email,
    password,
  }), { headers: { 'Content-Type': 'application/json' } });

  const token = loginRes.json('token');
  return { token };
}

export default function (data) {
  const headers = {
    'Authorization': `Bearer ${data.token}`,
    'Content-Type': 'application/json',
  };

  // 1. Health endpoint (public)
  const healthRes = http.get(`${BASE_URL}/api/v1/health`);
  check(healthRes, { 'Health status is 200': (r) => r.status === 200 });

  // 2. Browse Skills (public)
  const skillsRes = http.get(`${BASE_URL}/api/v1/skills`);
  check(skillsRes, { 'Skills status is 200': (r) => r.status === 200 });

  // 3. User Profile (authenticated)
  const profileRes = http.get(`${BASE_URL}/api/v1/users/me`, { headers });
  check(profileRes, { 'Profile status is 200': (r) => r.status === 200 });

  // 4. Browse Projects (authenticated)
  const projectsRes = http.get(`${BASE_URL}/api/v1/projects?page=0&size=10`, { headers });
  check(projectsRes, { 'Projects status is 200': (r) => r.status === 200 });

  // 5. Project Recommendations (authenticated)
  const recRes = http.get(`${BASE_URL}/api/v1/recommendations/projects`, { headers });
  check(recRes, { 'Recommendations status is 200': (r) => r.status === 200 });

  // 6. Upcoming Events (authenticated)
  const eventsRes = http.get(`${BASE_URL}/api/v1/events?upcomingOnly=true`, { headers });
  check(eventsRes, { 'Events status is 200': (r) => r.status === 200 });

  // 7. Unread Notifications (authenticated)
  const notifRes = http.get(`${BASE_URL}/api/v1/notifications?unreadOnly=true`, { headers });
  check(notifRes, { 'Notifications status is 200': (r) => r.status === 200 });

  // 8. Conversations list (authenticated)
  const convRes = http.get(`${BASE_URL}/api/v1/conversations`, { headers });
  check(convRes, { 'Conversations status is 200': (r) => r.status === 200 });

  sleep(1); // 1s think time
}
