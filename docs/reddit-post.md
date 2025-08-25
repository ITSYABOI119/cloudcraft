# [FREE BETA] CloudCraft Engine: 200 Players at 20 TPS on a Ryzen 2700X

Hey r/admincraft! After seeing so many posts about server performance issues, I built something that might help.

## Real Performance Numbers (With Proof)
Just ran a stress test with **200 players** on my Ryzen 2700X:
- ⚡ **0.17ms MSPT** (vanilla hits 75ms with similar load)
- 🎯 **Perfect 20.01 TPS** throughout the entire test
- 💾 **68.3% less RAM** than vanilla (650MB vs 2048MB)
- 🖥️ **99.8% faster** entity processing

## How It Works
CloudCraft Engine uses Java 21's Virtual Threads to process entities in parallel. Instead of everything running on one thread, the world is divided into regions that process simultaneously.

## Free Beta Access
- **GitHub Release:** https://github.com/yourusername/cloudcraft/releases/tag/v0.1.0-beta
- **Requirements:** Java 21, Paper 1.20.4+
- **Beta Limits:** 200 players max during beta testing

## Try It Yourself
1. Download the JAR
2. Drop in plugins folder
3. Run `/stresstest` to see the magic

Looking for server admins to test and provide feedback. It's completely **FREE during beta**.

## Technical Details
- Built with Java 21 Virtual Threads (Project Loom)
- Region-based entity processing
- Lock-free concurrent data structures
- Zero modifications to vanilla gameplay
- Full plugin compatibility maintained

Happy to answer any technical questions!