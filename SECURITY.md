# Security Policy

## Supported Versions

Security updates are provided **only** for the following versions of Shizuku:

| Version | Supported |
| ------- | --------- |
| Latest stable release | ✅* |
| Latest debug / prerelease | ✅ |
| Older versions | ❌ |

Reports affecting unsupported versions may be closed without action.

---

## Reporting a Vulnerability

If you discover a security vulnerability in Shizuku, please report it **privately** and practice responsible disclosure. [See how](https://docs.github.com/en/code-security/how-tos/report-and-fix-vulnerabilities/report-a-vulnerability/privately-reporting-a-security-vulnerability).

Shizuku provides access to **ADB-level and/or root-level privileges**, so security issues may have a high impact.

When reporting a vulnerability, you may include any of the following:

- Detailed step-by-step reproduction instructions (including granular or 1-by-2 steps)
- Screenshots or screen recordings
- Logs or crash output
- Proof-of-concept (PoC) code
- Test applications or scripts demonstrating the issue

Providing detailed information helps us reproduce and fix the issue more efficiently.

---

## Testing Before Reporting

To reduce duplicate reports and false positives, please verify the issue under the following conditions:

1. Confirm the issue reproduces on the **latest stable release**
2. **When possible, also test on the latest prerelease (debug) build**

If the issue does **not** reproduce on the prerelease build, it may already be fixed.

---

## Responsible Disclosure

Due to the privileged nature of Shizuku, vulnerabilities may be easily exploitable if disclosed prematurely.

Please follow these disclosure guidelines:

- Do **not** publicly disclose vulnerability details immediately
- Wait **at least 1 month after a release containing the fix** before sharing technical details that could reasonably lead to exploitation
  - This includes exploit write-ups, abuse techniques, code snippets, videos, or tutorials that demonstrate real-world attacks

This delay allows users adequate time to update and helps reduce the risk of active exploitation.

---

## What Not to Report

- Issues affecting only unsupported versions
- Vulnerabilities that require modifying Shizuku itself to be exploitable
- Reports without any reasonable security impact

---

## Bug Bounty

Shizuku does **not** offer a bug bounty or monetary rewards.

However, **responsible security reporters will be credited in release notes** or other public acknowledgements, at the maintainer's discretion.

---

## Acknowledgements

We appreciate the efforts of security researchers and community members who help improve Shizuku's security. Responsible disclosure helps keep users safe and the ecosystem healthy.
