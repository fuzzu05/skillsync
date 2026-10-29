/**
 * SkillSync Load & Performance Test Suite (Node.js Native Runner)
 * 
 * Works out-of-the-box with Node.js 18+ (No external npm packages required).
 * Supports modes: smoke, baseline, gradual, burst, stress, soak
 *
 * Usage:
 *   node load-tests/skillsync-load-test.js --mode=smoke --baseUrl=http://localhost:8081
 *   node load-tests/skillsync-load-test.js --mode=baseline --vus=10 --duration=30s
 *   node load-tests/skillsync-load-test.js --mode=gradual --maxVus=100
 *   node load-tests/skillsync-load-test.js --mode=burst --burstVus=50
 */

const BASE_URL = process.env.BASE_URL || getArg('--baseUrl') || 'http://localhost:8081';
const MODE = getArg('--mode') || 'smoke';
const DURATION_SEC = parseInt(getArg('--duration') || '15', 10);
const TARGET_VUS = parseInt(getArg('--vus') || '5', 10);

function getArg(flag) {
  const match = process.argv.find(a => a.startsWith(`${flag}=`));
  return match ? match.split('=')[1] : null;
}

const metrics = {
  totalRequests: 0,
  successfulRequests: 0,
  failedRequests: 0,
  statusCounts: {},
  latencies: [],
  errors: []
};

async function timedFetch(url, options = {}) {
  const start = performance.now();
  metrics.totalRequests++;
  try {
    const res = await fetch(url, options);
    const duration = performance.now() - start;
    metrics.latencies.push(duration);
    metrics.statusCounts[res.status] = (metrics.statusCounts[res.status] || 0) + 1;
    if (res.ok) {
      metrics.successfulRequests++;
    } else {
      metrics.failedRequests++;
    }
    const text = await res.text();
    let json = null;
    try { json = JSON.parse(text); } catch (_) { }
    return { status: res.status, ok: res.ok, duration, body: json || text };
  } catch (err) {
    const duration = performance.now() - start;
    metrics.failedRequests++;
    metrics.errors.push(err.message);
    return { status: 0, ok: false, duration, error: err.message };
  }
}

async function registerAndLoginUser(emailPrefix) {
  const randomSuffix = Math.floor(Math.random() * 1000000);
  const email = `${emailPrefix}_${randomSuffix}@test.com`;
  const password = 'Password@123';

  // 1. Register
  const regRes = await timedFetch(`${BASE_URL}/api/v1/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email,
      password,
      fullName: 'Perf Test Student',
      department: 'Computer Science',
      yearOfStudy: 3
    })
  });

  if (!regRes.ok) {
    return null;
  }

  // 2. Login
  const loginRes = await timedFetch(`${BASE_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  if (loginRes.ok && loginRes.body && loginRes.body.token) {
    return { token: loginRes.body.token, email };
  }
  return null;
}

async function simulateUserWorkflow(userSession, stopTime) {
  const headers = userSession ? {
    'Authorization': `Bearer ${userSession.token}`,
    'Content-Type': 'application/json'
  } : {};

  while (Date.now() < stopTime) {
    // 1. Health check
    await timedFetch(`${BASE_URL}/api/v1/health`);

    // 2. Browse skills catalogue
    await timedFetch(`${BASE_URL}/api/v1/skills`);

    if (userSession) {
      // 3. View user profile
      await timedFetch(`${BASE_URL}/api/v1/users/me`, { headers });

      // 4. View user skills
      await timedFetch(`${BASE_URL}/api/v1/users/me/skills`, { headers });

      // 5. Browse projects
      await timedFetch(`${BASE_URL}/api/v1/projects?page=0&size=10`, { headers });

      // 6. Project recommendations
      await timedFetch(`${BASE_URL}/api/v1/recommendations/projects`, { headers });

      // 7. Browse events
      await timedFetch(`${BASE_URL}/api/v1/events?upcomingOnly=true`, { headers });

      // 8. Check unread notifications
      await timedFetch(`${BASE_URL}/api/v1/notifications?unreadOnly=true`, { headers });

      // 9. Check conversations
      await timedFetch(`${BASE_URL}/api/v1/conversations`, { headers });
    }

    // Brief think time between workflow loops (50ms - 200ms)
    await new Promise(r => setTimeout(r, 50 + Math.random() * 150));
  }
}

async function runBenchmark() {
  console.log(`====================================================`);
  console.log(` SkillSync Performance Test Runner (Node.js Native) `);
  console.log(` Target Base URL : ${BASE_URL}`);
  console.log(` Execution Mode  : ${MODE.toUpperCase()}`);
  console.log(`====================================================\n`);

  // Verify health first
  console.log(`[1/4] Checking target server health...`);
  const health = await timedFetch(`${BASE_URL}/api/v1/health`);
  if (!health.ok) {
    console.error(`[ERROR] Server health check failed at ${BASE_URL}/api/v1/health with status ${health.status}`);
    console.error(`Ensure SkillSync is started on port 8081 before running load tests.`);
    process.exit(1);
  }
  console.log(`[PASS] Server is UP (${health.duration.toFixed(2)} ms).\n`);

  let vus = TARGET_VUS;
  let durationMs = DURATION_SEC * 1000;

  if (MODE === 'smoke') {
    vus = 2;
    durationMs = 5000;
  } else if (MODE === 'baseline') {
    vus = 10;
    durationMs = 15000;
  } else if (MODE === 'burst') {
    vus = parseInt(getArg('--burstVus') || '30', 10);
    durationMs = 8000;
  } else if (MODE === 'stress') {
    vus = parseInt(getArg('--maxVus') || '50', 10);
    durationMs = 20000;
  } else if (MODE === 'soak') {
    vus = 15;
    durationMs = 60000;
  }

  console.log(`[2/4] Initializing ${vus} concurrent Virtual User sessions...`);
  const userSessions = [];
  for (let i = 0; i < vus; i++) {
    const session = await registerAndLoginUser(`perf_vu_${i}`);
    userSessions.push(session);
  }
  console.log(`[PASS] Created ${userSessions.filter(Boolean).length}/${vus} authenticated virtual user sessions.\n`);

  console.log(`[3/4] Running ${MODE.toUpperCase()} workload: ${vus} VUs for ${(durationMs / 1000).toFixed(0)}s...`);
  const startTime = Date.now();
  const stopTime = startTime + durationMs;

  const workers = userSessions.map(session => simulateUserWorkflow(session, stopTime));
  await Promise.all(workers);

  const totalTimeSec = (Date.now() - startTime) / 1000;
  console.log(`\n[4/4] Workload completed in ${totalTimeSec.toFixed(2)}s.\n`);

  // Compute metrics
  metrics.latencies.sort((a, b) => a - b);
  const count = metrics.latencies.length;
  const p50 = count ? metrics.latencies[Math.floor(count * 0.50)].toFixed(2) : '0';
  const p90 = count ? metrics.latencies[Math.floor(count * 0.90)].toFixed(2) : '0';
  const p95 = count ? metrics.latencies[Math.floor(count * 0.95)].toFixed(2) : '0';
  const p99 = count ? metrics.latencies[Math.floor(count * 0.99)].toFixed(2) : '0';
  const mean = count ? (metrics.latencies.reduce((a, b) => a + b, 0) / count).toFixed(2) : '0';
  const rps = (metrics.totalRequests / totalTimeSec).toFixed(2);

  console.log(`==================== RESULTS REPORT ====================`);
  console.log(`Total Requests      : ${metrics.totalRequests}`);
  console.log(`Successful Requests : ${metrics.successfulRequests}`);
  console.log(`Failed Requests     : ${metrics.failedRequests}`);
  console.log(`Throughput (RPS)    : ${rps} req/sec`);
  console.log(`HTTP Statuses       : ${JSON.stringify(metrics.statusCounts)}`);
  console.log(`Latency (Mean)      : ${mean} ms`);
  console.log(`Latency (p50 / Med) : ${p50} ms`);
  console.log(`Latency (p90)       : ${p90} ms`);
  console.log(`Latency (p95)       : ${p95} ms`);
  console.log(`Latency (p99)       : ${p99} ms`);
  if (metrics.errors.length > 0) {
    console.log(`Sample Errors       : ${metrics.errors.slice(0, 5).join(' | ')}`);
  }
  console.log(`========================================================\n`);
}

runBenchmark().catch(err => {
  console.error("Fatal error running benchmark:", err);
  process.exit(1);
});
