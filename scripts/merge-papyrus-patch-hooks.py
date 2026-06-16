#!/usr/bin/env python3
"""Merge Papyrus hook lines from main patch files into version-specific patches."""

from __future__ import annotations

import sys
from pathlib import Path


def extract_papyrus_blocks(content: str) -> list[tuple[str | None, list[str]]]:
    lines = content.splitlines()
    blocks: list[tuple[str | None, list[str]]] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if not (
            line.startswith("+")
            and not line.startswith("+++")
            and ("Papyrus" in line or "io.papermc.paper.anticheat" in line)
        ):
            i += 1
            continue

        block: list[str] = []
        anchor: str | None = None
        start = i
        while i < len(lines):
            current = lines[i]
            if i > start and current.startswith("@@"):
                break
            if current.startswith("+") and not current.startswith("+++"):
                if "Papyrus" in current or "io.papermc.paper.anticheat" in current:
                    block.append(current[1:])
                elif block:
                    block.append(current[1:])
                i += 1
                continue
            if block:
                break
            i += 1
            continue

        for j in range(start - 1, max(start - 30, -1), -1):
            prev = lines[j]
            if prev.startswith("@@"):
                break
            if prev.startswith(("+", " ", "-")) and not prev.startswith("+++"):
                anchor = prev[1:]
                break

        if block:
            blocks.append((anchor, block))
        if i == start:
            i += 1
    return blocks


def line_body(line: str) -> str:
    if line.startswith(("+", " ", "-")):
        return line[1:]
    return line


def find_unique_anchor_index(result_lines: list[str], anchor: str) -> int | None:
    matches = [
        idx
        for idx, target_line in enumerate(result_lines)
        if line_body(target_line).rstrip() == anchor.rstrip()
    ]
    if len(matches) == 1:
        return matches[0]
    return None


def merge_patch(main_path: Path, target_path: Path) -> bool:
    main_content = main_path.read_text()
    target_content = target_path.read_text()
    result_lines = target_content.splitlines()
    changed = False

    for anchor, block in extract_papyrus_blocks(main_content):
        block_text = "\n".join(block)
        if block_text in target_content:
            continue

        if anchor is None:
            print(
                f"WARNING: missing anchor in {target_path.name} for block starting: {block[0]!r}",
                file=sys.stderr,
            )
            continue

        anchor_idx = find_unique_anchor_index(result_lines, anchor)
        if anchor_idx is None:
            print(
                f"WARNING: anchor is missing or ambiguous in {target_path.name}: {anchor!r}",
                file=sys.stderr,
            )
            continue

        insert_at = anchor_idx + 1
        for offset, block_line in enumerate(block):
            prefixed = "+" + block_line
            if any(line_body(existing).rstrip() == block_line.rstrip() for existing in result_lines):
                continue
            result_lines.insert(insert_at + offset, prefixed)
            changed = True

    if changed:
        target_path.write_text("\n".join(result_lines) + "\n")
    return changed


def main() -> int:
    if len(sys.argv) < 3:
        print("Usage: merge-papyrus-patch-hooks.py <main-patch> <target-patch>", file=sys.stderr)
        return 1

    main_path = Path(sys.argv[1])
    target_path = Path(sys.argv[2])
    if not main_path.is_file():
        print(f"Missing main patch: {main_path}", file=sys.stderr)
        return 1
    if not target_path.is_file():
        print(f"Missing target patch: {target_path}", file=sys.stderr)
        return 1

    merge_patch(main_path, target_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
