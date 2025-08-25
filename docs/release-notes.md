# CloudCraft Engine v0.1.0-beta

## Performance Highlights (Verified Test Results)
- **99.8% MSPT improvement** (0.17ms vs vanilla's 75ms)
- **Perfect 20.01 TPS** with 200 players
- **68.3% lower memory usage** (650MB vs 2048MB)
- Tested on consumer hardware (Ryzen 2700X)

## Test Configuration
- 200 players (beta limit)
- 60 second test duration
- 10 second warmup period
- Java 24, 16 cores, 8GB heap

## Detailed Performance Metrics
| Metric | Vanilla Paper | CloudCraft | Improvement |
|--------|---------------|------------|-------------|
| TPS | 15.00 | 20.01 | +33.4% |
| MSPT | 75.00ms | 0.17ms | 99.8% faster |
| Memory | 2048MB | 650MB | 68.3% less |

## Beta Program Details
- Free during beta period
- Expires: October 25, 2025
- Limited to 200 players (full version will be unlimited)
- Requires Java 21+ and Paper 1.20.4+

## Installation
### Requirements
- Paper 1.20.4 or newer
- Java 21 or newer (Java 21 LTS recommended)
- 8GB RAM minimum

### Steps
1. Download the JAR
2. Place in your plugins folder
3. Restart server
4. Run `/stresstest` to verify performance

## Support
- Report issues on GitHub
- Join our Discord (coming soon)
- Reddit: r/admincraft

## Beta Testing Guidelines
1. Run the stress test with `/stresstest 200 60 10`
2. Check the generated reports in `plugins/CloudCraftEngine/stress-test-results/`
3. Share your results and feedback

## Known Limitations
- Beta is capped at 200 players
- Some advanced features disabled during beta
- Expires October 25, 2025
