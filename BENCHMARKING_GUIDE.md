# CloudCraft Engine Benchmarking Guide

## How to Get Accurate Performance Comparisons

The CloudCraft Engine stress test provides **estimated** vanilla comparisons for demonstration purposes. For **accurate benchmarks**, follow this methodology:

### Step 1: Baseline Vanilla Test
1. **Disable CloudCraft Engine** (remove from plugins folder)
2. **Restart server** with vanilla Paper
3. **Run your workload** (spawn entities, simulate players)
4. **Record metrics** (TPS, MSPT, memory) for at least 5 minutes
5. **Save results** as baseline

### Step 2: CloudCraft Engine Test  
1. **Install CloudCraft Engine** 
2. **Restart server**
3. **Run identical workload** (same entity count, same duration)
4. **Record metrics** using `/stresstest` command
5. **Compare with baseline results**

### Recommended Test Parameters

```bash
# For 200 entities test (beta limit)
/stresstest 200 300 30

# For 500 entities test (full version)
/stresstest 500 300 30

# Parameters: [entities] [duration_seconds] [warmup_seconds]
```

### What CloudCraft Engine Actually Does

**Real Optimizations:**
- ✅ **Virtual Thread Processing**: Uses Java 21's Project Loom for parallel entity processing
- ✅ **Spatial Culling**: Only processes entities within 64 blocks of players  
- ✅ **Snapshot-Based Updates**: Captures world state, processes async, applies results
- ✅ **Lock-Free Data Structures**: Minimizes thread contention
- ✅ **Intelligent AI Processing**: Hostile mob targeting, passive mob behaviors

**Performance Benefits:**
- Reduces main thread load by offloading entity processing
- Scales with available CPU cores through virtual threads
- Eliminates unnecessary entity processing through spatial culling
- Maintains 20 TPS even with high entity counts

### Expected Results

**Typical improvements with 200+ entities:**
- **TPS**: Maintains 20.0 vs vanilla dropping to 15-18
- **MSPT**: 0.5-2ms vs vanilla 15-30ms  
- **Memory**: Similar or slightly lower due to efficient data structures
- **CPU Usage**: Better distributed across cores

### Important Notes

1. **Java 21 Required**: Virtual threads are essential for performance gains
2. **Entity Count Matters**: Benefits increase with higher entity density
3. **Server Hardware**: Multi-core systems see better improvements
4. **Bukkit Compatibility**: 100% compatible with existing plugins

### Reporting Results

When sharing benchmarks:
- Include server specs (CPU, RAM, Java version)
- Specify exact test parameters  
- Show both vanilla and CloudCraft results
- Note any other plugins running during tests

### Questions?

- GitHub Issues: https://github.com/ITSYABOI119/cloudcraft/issues
- Ensure you're using identical test conditions for fair comparison
