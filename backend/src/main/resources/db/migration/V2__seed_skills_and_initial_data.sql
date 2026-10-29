-- ====================================================================
-- SkillSync Database Seed Data - V2__seed_skills_and_initial_data.sql
-- ====================================================================

-- 1. SEED SKILLS
INSERT INTO skills (name, category, description) VALUES
-- Backend
('Java', 'BACKEND', 'Core Java programming language and standard libraries'),
('Spring Boot', 'BACKEND', 'Enterprise Java microservice and web development framework'),
('Python', 'BACKEND', 'General purpose programming language for backend and scripting'),
('Node.js', 'BACKEND', 'JavaScript runtime for server-side event-driven applications'),
('Express.js', 'BACKEND', 'Fast, unopinionated, minimalist web framework for Node.js'),
('Go', 'BACKEND', 'Statically typed, compiled language designed for concurrency and microservices'),
('C++', 'BACKEND', 'High-performance systems programming and algorithm implementation'),
('Rust', 'BACKEND', 'Memory-safe systems programming language'),

-- Frontend
('React', 'FRONTEND', 'Declarative, component-based frontend JavaScript library'),
('TypeScript', 'FRONTEND', 'Typed superset of JavaScript providing compile-time type safety'),
('Next.js', 'FRONTEND', 'React framework for server-rendered and static web applications'),
('Vue.js', 'FRONTEND', 'Progressive JavaScript framework for building user interfaces'),
('Tailwind CSS', 'FRONTEND', 'Utility-first CSS framework for rapid UI styling'),
('HTML5/CSS3', 'FRONTEND', 'Semantic web markup and modern CSS styling specifications'),

-- Database
('PostgreSQL', 'DATABASE', 'Powerful open-source object-relational database system'),
('MySQL', 'DATABASE', 'Popular open-source relational database management system'),
('MongoDB', 'DATABASE', 'Document-oriented NoSQL database for flexible data models'),
('Redis', 'DATABASE', 'In-memory key-value data store used for caching and sessions'),

-- DevOps & Cloud
('Docker', 'DEVOPS', 'OS-level virtualization for building and shipping containers'),
('Kubernetes', 'DEVOPS', 'Automated container orchestration, scaling, and management'),
('AWS', 'DEVOPS', 'Amazon Web Services cloud computing infrastructure'),
('GitHub Actions', 'DEVOPS', 'CI/CD pipeline automation directly integrated in GitHub'),
('Linux', 'DEVOPS', 'Unix-like operating system command line, administration, and bash scripting'),

-- AI & Machine Learning
('Machine Learning', 'AI_ML', 'Statistical algorithms and predictive modeling'),
('Deep Learning', 'AI_ML', 'Neural networks, computer vision, and NLP architectures'),
('PyTorch', 'AI_ML', 'Open source machine learning framework based on the Torch library'),
('TensorFlow', 'AI_ML', 'End-to-end open source machine learning platform'),
('Data Analysis', 'AI_ML', 'Pandas, NumPy, and scientific data manipulation in Python'),

-- Mobile
('Flutter', 'MOBILE', 'Cross-platform native mobile app development in Dart'),
('React Native', 'MOBILE', 'Cross-platform mobile apps for iOS and Android using React'),
('Android / Kotlin', 'MOBILE', 'Native Android application development with Kotlin and Jetpack'),
('iOS / Swift', 'MOBILE', 'Native iOS application development with Apple Swift'),

-- Design & UI/UX
('Figma', 'DESIGN', 'Collaborative interface design, prototyping, and design systems'),
('UI/UX Design', 'DESIGN', 'User research, wireframing, usability testing, and interaction design'),

-- System Design & Architecture
('System Design', 'SYSTEM_DESIGN', 'Distributed systems architecture, scalability, and API design'),
('RESTful APIs', 'SYSTEM_DESIGN', 'Standardized HTTP-based stateless resource endpoints');
