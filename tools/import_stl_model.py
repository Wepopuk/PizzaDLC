"""Import pasting/3.stl as a simplified, rigid Custom Models mesh.

Requires numpy, Pillow and fast-simplification (tools/.model-import-deps is
supported). STL carries no texture, UVs or skeleton; use a neutral grey material
and preserve the complete statue, without inventing limb cuts or a humanoid rig.
"""
import json
import struct
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/.model-import-deps"))
import fast_simplification


def main():
    source = ROOT / "pasting/3.stl"
    with source.open("rb") as stream:
        header = stream.read(84)
    count = struct.unpack_from("<I", header, 80)[0]
    if source.stat().st_size != 84 + count * 50:
        raise ValueError("Expected a valid binary STL")
    dtype = np.dtype([("normal", "<f4", (3,)), ("vertices", "<f4", (3, 3)), ("attribute", "<u2")])
    raw = np.memmap(source, dtype=dtype, mode="r", offset=84, shape=(count,))
    corners = np.ascontiguousarray(raw["vertices"]).reshape(-1, 3)
    if not np.isfinite(corners).all():
        raise ValueError("STL has nonfinite coordinates")
    original_bounds = [corners.min(axis=0).tolist(), corners.max(axis=0).tolist()]
    print(f"Read {count:,} source triangles; welding shared vertices", flush=True)
    vertices, mapping = np.unique(corners, axis=0, return_inverse=True)
    indices = mapping.reshape(-1, 3).astype(np.int32)
    del corners, mapping, raw
    print(f"Welded to {len(vertices):,} vertices; quadric simplification to 40,000 triangles", flush=True)
    vertices, indices = fast_simplification.simplify(vertices, indices, target_count=40_000, agg=7)
    # The supplied print model is Z-up. Convert to Minecraft's Y-down space.
    # Front faces source -Y; retain the same anatomical X and map Y to Z.
    low, high = vertices.min(axis=0), vertices.max(axis=0)
    scale = 1.8 / (high[2] - low[2])
    centered = vertices - [(low[0] + high[0]) / 2, (low[1] + high[1]) / 2, low[2]]
    positions = np.c_[centered[:, 0], -centered[:, 2], centered[:, 1]] * scale + [0, 1.5, 0]
    # Area-weighted vertex normals keep the simplified surface smooth.
    triangles = positions[indices]
    face_normals = np.cross(triangles[:, 1] - triangles[:, 0], triangles[:, 2] - triangles[:, 0])
    valid = np.linalg.norm(face_normals, axis=1) > 1e-12
    indices, face_normals = indices[valid], face_normals[valid]
    used, remap = np.unique(indices, return_inverse=True)
    positions = positions[used]
    indices = remap.reshape(-1, 3).astype(np.int32)
    normals = np.zeros_like(positions)
    for corner in range(3):
        np.add.at(normals, indices[:, corner], face_normals)
    lengths = np.linalg.norm(normals, axis=1, keepdims=True)
    cancelled = lengths[:, 0] < 1e-12
    if np.any(cancelled):
        # Opposite faces in a print mesh can cancel at a welded vertex.
        # Use an incident, nondegenerate face normal for those vertices.
        incident = np.full(len(positions), -1, dtype=np.int32)
        for corner in range(3):
            np.maximum.at(incident, indices[:, corner], np.arange(len(indices)))
        normals[cancelled] = face_normals[incident[cancelled]]
        lengths = np.linalg.norm(normals, axis=1, keepdims=True)
    normals /= lengths
    # Same indexed mesh format as the GLB imports, with every vertex rigid.
    packed = np.zeros((len(positions), 14))
    packed[:, :3], packed[:, 5:8], packed[:, 8] = positions, normals, 1
    pivots = np.zeros((6, 3))
    hands = np.zeros((2, 3))
    target = ROOT / "src/main/resources/assets/pizzadlc/models/hitler"
    target.mkdir(parents=True, exist_ok=True)
    with (target / "mesh.bin").open("wb") as out:
        out.write(b"CSM1")
        out.write(pivots.astype(">f4").tobytes())
        out.write(hands.astype(">f4").tobytes())
        out.write(struct.pack(">IIII", 1, 0, len(positions), indices.size))
        out.write(packed.astype(">f4").tobytes())
        out.write(indices.astype(">i4").tobytes())
    Image.new("RGBA", (2, 2), (190, 190, 190, 255)).save(target / "material_0.png")
    info = {"source": "pasting/3.stl", "rigid": True, "sourceTriangles": count,
            "vertices": len(positions), "triangles": len(indices), "materials": 1,
            "height": 1.8, "sourceBounds": original_bounds,
            "bounds": [positions.min(axis=0).tolist(), positions.max(axis=0).tolist()],
            "simplifier": "fast-simplification 0.2.0, quadric error, target 40000"}
    (target / "model.json").write_text(json.dumps(info, indent=2) + "\n", encoding="utf-8")
    print(f"Imported Hitler: {len(positions):,} vertices, {len(indices):,} triangles", flush=True)


if __name__ == "__main__":
    main()
