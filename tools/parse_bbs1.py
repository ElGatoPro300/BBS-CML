#!/usr/bin/env python3
"""Parse BBS1 DataStorage film files and analyze replay keyframes."""

import gzip
import math
import struct
import sys
from pathlib import Path

TYPE_MAP = 0
TYPE_LIST = 1
TYPE_STRING = 2
TYPE_BYTE = 3
TYPE_SHORT = 4
TYPE_INT = 5
TYPE_FLOAT = 6
TYPE_LONG = 7
TYPE_DOUBLE = 8
TYPE_BYTE_ARRAY = 9
TYPE_SHORT_ARRAY = 10
TYPE_INT_ARRAY = 11


class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0
        self.key_map: dict[int, str] = {}

    def read(self, n: int) -> bytes:
        chunk = self.data[self.pos : self.pos + n]
        if len(chunk) < n:
            raise EOFError(f"need {n} bytes at {self.pos}")
        self.pos += n
        return chunk

    def read_byte(self) -> int:
        return self.read(1)[0]

    def read_bool(self) -> bool:
        return self.read_byte() != 0

    def read_short(self) -> int:
        return struct.unpack(">h", self.read(2))[0]

    def read_ushort(self) -> int:
        return struct.unpack(">H", self.read(2))[0]

    def read_int(self) -> int:
        return struct.unpack(">i", self.read(4))[0]

    def read_float(self) -> float:
        return struct.unpack(">f", self.read(4))[0]

    def read_long(self) -> int:
        return struct.unpack(">q", self.read(8))[0]

    def read_double(self) -> float:
        return struct.unpack(">d", self.read(8))[0]

    def read_utf(self) -> str:
        return self.read(self.read_ushort()).decode("utf-8")

    def read_key_index(self) -> int:
        kt = self.key_type
        if kt == 0:
            return self.read_byte()
        if kt == 1:
            return self.read_ushort()
        return self.read_int()

    def read_key(self) -> str:
        return self.key_map[self.read_key_index()]

    def load_key_table(self):
        self.key_type = self.read_byte()
        count = self.read_key_index()
        self.key_map = {}
        for _ in range(count):
            idx = self.read_key_index()
            self.key_map[idx] = self.read_utf()


def read_type(r: Reader):
    t = r.read_byte()
    if t == TYPE_MAP:
        n = r.read_int()
        m = {}
        for _ in range(n):
            k = r.read_key()
            m[k] = read_type(r)
        return m
    if t == TYPE_LIST:
        n = r.read_int()
        return [read_type(r) for _ in range(n)]
    if t == TYPE_STRING:
        return r.read_utf()
    if t == TYPE_BYTE:
        return r.read_byte()
    if t == TYPE_SHORT:
        return r.read_short()
    if t == TYPE_INT:
        return r.read_int()
    if t == TYPE_FLOAT:
        return r.read_float()
    if t == TYPE_LONG:
        return r.read_long()
    if t == TYPE_DOUBLE:
        return r.read_double()
    if t == TYPE_BYTE_ARRAY:
        return r.read(r.read_int())
    if t == TYPE_SHORT_ARRAY:
        n = r.read_int()
        return list(struct.unpack(">" + "h" * n, r.read(2 * n)))
    if t == TYPE_INT_ARRAY:
        n = r.read_int()
        return list(struct.unpack(">" + "i" * n, r.read(4 * n)))
    raise ValueError(f"unknown type {t}")


def load_bbs1(path: Path):
    raw = path.read_bytes()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    if raw[:4] != b"BBS1":
        raise ValueError(f"bad header {raw[:4]!r}")
    r = Reader(raw[4:])
    r.load_key_table()
    return read_type(r)


def as_num(v, default=0.0):
    if isinstance(v, (int, float)):
        return float(v)
    return default


def interp_linear(kfs, tick):
    if not kfs:
        return 0.0
    kfs = sorted(kfs, key=lambda k: k["tick"])
    if tick <= kfs[0]["tick"]:
        return as_num(kfs[0]["value"])
    if tick >= kfs[-1]["tick"]:
        return as_num(kfs[-1]["value"])
    for i in range(len(kfs) - 1):
        a, b = kfs[i], kfs[i + 1]
        t0, t1 = a["tick"], b["tick"]
        if t0 <= tick <= t1:
            if t1 == t0:
                return as_num(a["value"])
            x = (tick - t0) / (t1 - t0)
            return as_num(a["value"]) * (1 - x) + as_num(b["value"]) * x
    return as_num(kfs[-1]["value"])


def limb_speed(dx, dz):
    delta = math.hypot(dx, 0.0, dz)
    return min(delta * 4.0, 1.0)


def channel_kfs(kf_map, name):
    ch = kf_map.get(name)
    if not isinstance(ch, list):
        return []
    out = []
    for item in ch:
        if isinstance(item, dict) and "tick" in item:
            out.append({"tick": float(item["tick"]), "value": item.get("value", 0)})
    return out


def analyze_replay(rep, idx):
    form = rep.get("form", {})
    form_id = form.get("id") if isinstance(form, dict) else form
    actor = bool(rep.get("actor", 0))
    shadow = bool(rep.get("shadow", 1))
    shadow_size = rep.get("shadow_size", 0.5)
    shadow_opacity = rep.get("shadow_opacity", 1.0)

    kf_root = rep.get("keyframes", {})
    if not isinstance(kf_root, dict):
        return None

    # keyframes may be nested under value groups
    def flatten_kf(node, acc=None):
        acc = acc or {}
        if not isinstance(node, dict):
            return acc
        for k, v in node.items():
            if isinstance(v, list) and v and isinstance(v[0], dict) and "tick" in v[0]:
                acc[k] = v
            elif isinstance(v, dict):
                flatten_kf(v, acc)
        return acc

    kf = flatten_kf(kf_root)
    xs = channel_kfs(kf, "x")
    ys = channel_kfs(kf, "y")
    zs = channel_kfs(kf, "z")
    sprint = channel_kfs(kf, "sprinting")
    grounded = channel_kfs(kf, "grounded")
    vx = channel_kfs(kf, "vX")
    vy = channel_kfs(kf, "vY")
    vz = channel_kfs(kf, "vZ")
    shadow_kf = channel_kfs(kf, "shadow_size")
    shadow_op_kf = channel_kfs(kf, "shadow_opacity")

    max_tick = 0
    for ch in (xs, ys, zs, sprint, grounded, vx, vy, vz):
        if ch:
            max_tick = max(max_tick, int(ch[-1]["tick"]))

    issues = []
    walk_ticks = []
    for t in range(1, max_tick + 1):
        x = interp_linear(xs, t)
        z = interp_linear(zs, t)
        px = interp_linear(xs, t - 1)
        pz = interp_linear(zs, t - 1)
        spd = limb_speed(x - px, z - pz)
        spr = interp_linear(sprint, t) > 0.5
        grd = interp_linear(grounded, t) > 0.5
        horiz = math.hypot(x - px, z - pz)
        if horiz > 0.01:
            walk_ticks.append((t, spd, spr, grd, horiz, x - px, z - pz))

    sprint_while_still = []
    for t, spd, spr, grd, horiz, dx, dz in walk_ticks:
        if spr and horiz < 0.001:
            sprint_while_still.append(t)
    if sprint_while_still:
        issues.append(f"sprint ON while still at ticks {sprint_while_still[:10]}... ({len(sprint_while_still)} total)")

    moving_airborne = [t for t, _, _, grd, h, _, _ in walk_ticks if not grd and h > 0.01]
    if moving_airborne:
        issues.append(f"grounded=false while moving at {len(moving_airborne)} ticks (e.g. {moving_airborne[:8]})")

    clamped_speed = [t for t, spd, _, _, h, _, _ in walk_ticks if h > 0.26 and spd >= 0.999]
    if clamped_speed:
        issues.append(f"limb speed clamped to 1.0 at {len(clamped_speed)} walk ticks (delta>0.25)")

    zero_speed_move = [t for t, spd, _, _, h, _, _ in walk_ticks if h > 0.05 and spd < 0.01]
    if zero_speed_move:
        issues.append(f"moving (delta>{0.05}) but limbSpeed~0 at {len(zero_speed_move)} ticks")

    # velocity track anomalies
    vel_spikes = []
    for t in range(1, max_tick + 1):
        tvx = interp_linear(vx, t)
        tvz = interp_linear(vz, t)
        if math.hypot(tvx, tvz) > 2.0:
            vel_spikes.append((t, tvx, tvz))
    if vel_spikes:
        issues.append(f"velocity spikes |v|>2: {vel_spikes[:5]} ... ({len(vel_spikes)} total)")

    return {
        "index": idx,
        "label": rep.get("label", ""),
        "form_id": form_id,
        "actor": actor,
        "shadow": shadow,
        "shadow_size": shadow_size,
        "shadow_opacity": shadow_opacity,
        "shadow_kf_count": len(shadow_kf),
        "shadow_op_kf_count": len(shadow_op_kf),
        "max_tick": max_tick,
        "x_kfs": len(xs),
        "walk_ticks": len(walk_ticks),
        "issues": issues,
        "sample_walk": walk_ticks[:5],
        "sample_end": walk_ticks[-3:] if walk_ticks else [],
    }


def main():
    path = Path(sys.argv[1] if len(sys.argv) > 1 else r"c:\Users\Usuario\Downloads\ad_uncompressed.bin")
    root = load_bbs1(path)
    print(f"Loaded {path.name}, top keys: {list(root.keys()) if isinstance(root, dict) else type(root)}")

    replays = root.get("replays", [])
    if not isinstance(replays, list):
        print("No replays list")
        return

    for i, rep in enumerate(replays):
        if not isinstance(rep, dict):
            continue
        info = analyze_replay(rep, i)
        if not info:
            continue
        print("\n" + "=" * 60)
        print(f"Replay #{i}: {info['label']!r} form={info['form_id']!r} actor={info['actor']}")
        print(f"  shadow={info['shadow']} size={info['shadow_size']} opacity={info['shadow_opacity']}")
        print(f"  keyframes: x={info['x_kfs']} max_tick={info['max_tick']} walk_ticks={info['walk_ticks']}")
        print(f"  shadow keyframes: size={info['shadow_kf_count']} opacity={info['shadow_op_kf_count']}")
        if info["sample_walk"]:
            print(f"  first walk samples (tick,speed,sprint,grounded,delta):")
            for row in info["sample_walk"]:
                print(f"    t={row[0]:.0f} spd={row[1]:.3f} sprint={row[2]} grounded={row[3]} horiz={row[4]:.4f} dx={row[5]:.4f} dz={row[6]:.4f}")
        for issue in info["issues"]:
            print(f"  ISSUE: {issue}")
        if not info["issues"]:
            print("  No obvious keyframe anomalies detected")


if __name__ == "__main__":
    main()
