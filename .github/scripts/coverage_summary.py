#!/usr/bin/env python3
import sys
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET

def find_jacoco_files(root: Path):
    return list(root.glob('**/target/site/jacoco/jacoco.xml'))

def derive_module_name(f: Path) -> str:
    """
    Derive a stable module name from a JaCoCo report path.
    Always return a single path component (directory name), never a full path.
    """
    try:
        parts = f.parts
        if 'target' in parts:
            idx = parts.index('target')
            if idx > 0:
                return parts[idx - 1]
        # Fallback when target is missing or malformed path shape
        return f.parent.parent.name or f.parent.name or "unknown-module"
    except Exception:
        return f.parent.parent.name or f.parent.name or "unknown-module"

def parse_files(files, metric='LINE'):
    total_missed = 0
    total_covered = 0
    per_module = []
    for f in files:
        try:
            tree = ET.parse(f)
            root = tree.getroot()
            missed = 0
            covered = 0
            for counter in root.findall('counter'):
                if counter.get('type') == metric:
                    missed += int(counter.get('missed', '0'))
                    covered += int(counter.get('covered', '0'))
            total_missed += missed
            total_covered += covered
            module = derive_module_name(f)
            per_module.append((module, missed, covered))
        except Exception:
            continue
    return total_missed, total_covered, per_module

def render_markdown(total_missed, total_covered, per_module, metric='LINE'):
    total = total_missed + total_covered
    pct = (total_covered / total * 100) if total > 0 else 0.0
    lines = []
    lines.append(f"## Coverage summary ({metric})")
    lines.append("")
    lines.append(f"**Total:** {pct:.1f}% ({total_covered}/{total} covered)")
    lines.append("")
    if per_module:
        lines.append("**Per-module:**")
        lines.append("")
        lines.append("| Module | Missed | Covered | % |")
        lines.append("|---|---:|---:|---:|")
        for mod, missed, covered in sorted(per_module):
            t = missed + covered
            p = (covered / t * 100) if t > 0 else 0.0
            lines.append(f"| {mod} | {missed} | {covered} | {p:.1f}% |")
    return '\n'.join(lines) + '\n'

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--root', default='.', help='Repository root to search for jacoco XMLs')
    p.add_argument('--metric', default='LINE', help='JaCoCo metric to use (LINE or INSTRUCTION)')
    p.add_argument('--output', default='coverage-summary.md', help='Output markdown file')
    args = p.parse_args()

    root = Path(args.root)
    files = find_jacoco_files(root)
    total_missed, total_covered, per_module = parse_files(files, args.metric)
    md = render_markdown(total_missed, total_covered, per_module, args.metric)
    Path(args.output).write_text(md)
    print(md)

if __name__ == '__main__':
    main()
