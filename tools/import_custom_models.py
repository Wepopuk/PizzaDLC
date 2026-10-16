"""Convert the supplied GLBs into indexed, six-bone skinned Minecraft meshes.

Requires numpy and Pillow. Original files are read-only. glTF node transforms,
inverse bind matrices, skin weights and base-colour materials are baked offline.
Finger/forearm joints follow their parent limb; the source rigs are retargeted to
Minecraft's head, body, arms and legs, with arms lowered from the source T-pose.
"""
import io
import json
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/pizzadlc/models"
PARTS = ["body", "head", "right_arm", "left_arm", "right_leg", "left_leg"]
TYPES = {5120: "i1", 5121: "u1", 5122: "<i2", 5123: "<u2", 5125: "<u4", 5126: "<f4"}
WIDTHS = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}


class Glb:
    def __init__(self, path):
        self.path = path
        raw = path.read_bytes()
        magic, version, size = struct.unpack_from("<III", raw)
        assert magic == 0x46546C67 and version == 2 and size == len(raw)
        offset = 12
        while offset < size:
            length, kind = struct.unpack_from("<II", raw, offset)
            chunk = raw[offset + 8:offset + 8 + length]
            if kind == 0x4E4F534A:
                self.doc = json.loads(chunk)
            elif kind == 0x004E4942:
                self.binary = chunk
            offset += 8 + length
        self.parents = {child: i for i, n in enumerate(self.doc["nodes"]) for child in n.get("children", [])}
        self.worlds = {}
        for i in range(len(self.doc["nodes"])):
            self.world(i)

    def accessor(self, index):
        a = self.doc["accessors"][index]
        assert "sparse" not in a, "Sparse accessors are not used by these assets"
        view = self.doc["bufferViews"][a["bufferView"]]
        assert view.get("buffer", 0) == 0
        dtype = np.dtype(TYPES[a["componentType"]])
        width = WIDTHS[a["type"]]
        result = np.ndarray((a["count"], width), dtype=dtype, buffer=self.binary,
                            offset=view.get("byteOffset", 0) + a.get("byteOffset", 0),
                            strides=(view.get("byteStride", dtype.itemsize * width), dtype.itemsize)).copy()
        if a.get("normalized") and a["componentType"] != 5126:
            limits = np.iinfo(dtype)
            result = np.maximum(result.astype(float) / limits.max, -1)
        return result

    def world(self, index):
        if index in self.worlds:
            return self.worlds[index]
        n = self.doc["nodes"][index]
        if "matrix" in n:
            local = np.array(n["matrix"]).reshape(4, 4).T
        else:
            x, y, z, w = n.get("rotation", [0, 0, 0, 1])
            rot = np.array([[1-2*y*y-2*z*z, 2*x*y-2*z*w, 2*x*z+2*y*w],
                            [2*x*y+2*z*w, 1-2*x*x-2*z*z, 2*y*z-2*x*w],
                            [2*x*z-2*y*w, 2*y*z+2*x*w, 1-2*x*x-2*y*y]])
            local = np.eye(4)
            local[:3, :3] = rot @ np.diag(n.get("scale", [1, 1, 1]))
            local[:3, 3] = n.get("translation", [0, 0, 0])
        parent = self.parents.get(index)
        self.worlds[index] = (self.world(parent) @ local) if parent is not None else local
        return self.worlds[index]

    def find(self, token, joints):
        return next(i for i in joints if token in self.doc["nodes"][i].get("name", ""))

    def group(self, node, anchors):
        while node is not None:
            if node in anchors:
                return anchors[node]
            node = self.parents.get(node)
        return 0

    def image(self, index):
        image = self.doc["images"][index]
        if "bufferView" in image:
            view = self.doc["bufferViews"][image["bufferView"]]
            start = view.get("byteOffset", 0)
            return Image.open(io.BytesIO(self.binary[start:start + view["byteLength"]])).convert("RGBA")
        return Image.open(self.path.parent / image["uri"]).convert("RGBA")


def rotation_between(source, target):
    a, b = source / np.linalg.norm(source), target / np.linalg.norm(target)
    cross = np.cross(a, b)
    c = np.dot(a, b)
    if c > .999999:
        return np.eye(3)
    assert c > -.999999, "Antiparallel arm needs an explicit rotation axis"
    x, y, z = cross
    skew = np.array([[0, -z, y], [z, 0, -x], [-y, x, 0]])
    return np.eye(3) + skew + skew @ skew / (1 + c)


def linear_to_srgb(value):
    return np.where(value <= .0031308, value * 12.92, 1.055 * np.maximum(value, 0) ** (1/2.4) - .055)


def material_texture(glb, material, dest):
    pbr = material.get("pbrMetallicRoughness", {})
    factor = np.array(pbr.get("baseColorFactor", [1, 1, 1, 1]))
    texture = pbr.get("baseColorTexture")
    if texture:
        image = glb.image(glb.doc["textures"][texture["index"]]["source"])
        assert texture.get("texCoord", 0) == 0
        pixels = np.array(image).astype(float) / 255
        rgb = pixels[..., :3]
        linear = np.where(rgb <= .04045, rgb / 12.92, ((rgb + .055) / 1.055) ** 2.4)
        pixels[..., :3] = linear_to_srgb(linear * factor[:3])
        pixels[..., 3] *= factor[3]
        image = Image.fromarray(np.uint8(np.clip(np.rint(pixels * 255), 0, 255)))
    else:
        color = [*np.uint8(np.clip(np.rint(linear_to_srgb(factor[:3]) * 255), 0, 255)), int(factor[3] * 255)]
        image = Image.new("RGBA", (2, 2), tuple(map(int, color)))
    if material.get("alphaMode", "OPAQUE") == "OPAQUE":
        image.putalpha(255)
    image.save(dest)


def convert(id_, path, tokens):
    glb = Glb(path)
    joints = glb.doc["skins"][0]["joints"]
    roots = [glb.find(token, joints) for token in tokens[:6]]
    hands = [glb.find(token, joints) for token in tokens[6:]]
    anchors = {node: group for group, node in enumerate(roots)}
    source_pivots = np.array([glb.world(i)[:3, 3] for i in roots])
    source_hands = np.array([glb.world(i)[:3, 3] for i in hands])
    print(id_, "source pivots", source_pivots.round(3).tolist())
    batches = []
    scene = glb.doc["scenes"][glb.doc.get("scene", 0)]
    active = set()
    def visit(node):
        active.add(node)
        for child in glb.doc["nodes"][node].get("children", []):
            visit(child)
    for node in scene["nodes"]:
        visit(node)
    for node in sorted(active):
        n = glb.doc["nodes"][node]
        if "mesh" not in n:
            continue
        skin = glb.doc["skins"][n["skin"]]
        inverse = glb.accessor(skin["inverseBindMatrices"]).reshape(-1, 4, 4).transpose(0, 2, 1)
        transforms = np.array([glb.world(j) @ inverse[i] for i, j in enumerate(skin["joints"])])
        groups = np.array([glb.group(j, anchors) for j in skin["joints"]])
        for prim in glb.doc["meshes"][n["mesh"]]["primitives"]:
            assert prim.get("mode", 4) == 4, "Only triangle primitives supported"
            attrs = prim["attributes"]
            positions = glb.accessor(attrs["POSITION"])
            normals = glb.accessor(attrs["NORMAL"])
            uv = glb.accessor(attrs["TEXCOORD_0"]) if "TEXCOORD_0" in attrs else np.zeros((len(positions), 2))
            bone_ids = glb.accessor(attrs["JOINTS_0"]).astype(int)
            weights = glb.accessor(attrs["WEIGHTS_0"])
            weights /= weights.sum(axis=1, keepdims=True)
            matrices = transforms[bone_ids]
            world_pos = np.einsum("nijk,nk,ni->nj", matrices[:, :, :3, :], np.c_[positions, np.ones(len(positions))], weights)
            normal_matrices = np.linalg.inv(matrices[:, :, :3, :3]).transpose(0, 1, 3, 2)
            world_normal = np.einsum("nijk,nk,ni->nj", normal_matrices, normals, weights)
            six_weights = np.zeros((len(positions), 6))
            for part in range(6):
                six_weights[:, part] = np.sum(weights * (groups[bone_ids] == part), axis=1)
            indices = glb.accessor(prim["indices"]).reshape(-1).astype(np.int32)
            batches.append(dict(pos=world_pos, normal=world_normal, uv=uv, weights=six_weights,
                                indices=indices, material=prim.get("material", 0)))

    # The Putin export has an armature object displaced in X while its vertex
    # coordinates are centred on the origin. Align its rig to the torso first.
    all_positions = np.concatenate([b["pos"] for b in batches])
    body_positions = np.concatenate([b["pos"][b["weights"][:, 0] > .9] for b in batches])
    torso_x = np.median(body_positions[:, 0])
    height = np.ptp(all_positions[:, 1])
    if abs(torso_x - source_pivots[0, 0]) > height * .25:
        shift = torso_x - source_pivots[0, 0]
        source_pivots[:, 0] += shift
        source_hands[:, 0] += shift
        print(id_, "corrected armature X offset", round(float(shift), 4))

    # Lower both source arms with weighted deformation, retaining shoulder seams.
    rotations = [np.eye(3) for _ in range(6)]
    for arm in (2, 3):
        vector = source_hands[arm - 2] - source_pivots[arm]
        outward = -1 if vector[0] < 0 else 1
        rotations[arm] = rotation_between(vector, np.array([outward * .12, -1, 0]))
    for b in batches:
        p, norm = np.zeros_like(b["pos"]), np.zeros_like(b["normal"])
        for part in range(6):
            moved = (b["pos"] - source_pivots[part]) @ rotations[part].T + source_pivots[part]
            p += b["weights"][:, part:part+1] * moved
            norm += b["weights"][:, part:part+1] * (b["normal"] @ rotations[part].T)
        b["pos"], b["normal"] = p, norm
    for arm in (2, 3):
        source_hands[arm - 2] = rotations[arm] @ (source_hands[arm - 2] - source_pivots[arm]) + source_pivots[arm]

    bounds = np.concatenate([b["pos"] for b in batches])
    low, high = bounds.min(axis=0), bounds.max(axis=0)
    scale = 1.8 / (high[1] - low[1])
    # Minecraft model coordinates: right arm at negative X, feet at Y=1.5.
    xsign = -1 if source_pivots[2, 0] > source_pivots[3, 0] else 1
    axes = np.diag([xsign, -1, -1])
    center = np.array([source_pivots[0, 0], low[1], source_pivots[0, 2]])
    def normalized(points):
        return (points - center) @ axes * scale + [0, 1.5, 0]
    pivots = normalized(source_pivots)
    hand_positions = normalized(source_hands)
    output = ASSETS / id_
    output.mkdir(parents=True, exist_ok=True)
    materials = glb.doc.get("materials", [{}])
    for i, material in enumerate(materials):
        material_texture(glb, material, output / f"material_{i}.png")
    for b in batches:
        b["pos"] = normalized(b["pos"])
        b["normal"] = b["normal"] @ axes
        b["normal"] /= np.maximum(np.linalg.norm(b["normal"], axis=1, keepdims=True), 1e-12)
        if np.linalg.det(axes) < 0:
            b["indices"] = b["indices"].reshape(-1, 3)[:, [0, 2, 1]].reshape(-1)
    with (output / "mesh.bin").open("wb") as out:
        out.write(b"CSM1")
        out.write(np.asarray(pivots, dtype=">f4").tobytes())
        out.write(np.asarray(hand_positions, dtype=">f4").tobytes())
        out.write(struct.pack(">I", len(batches)))
        for b in batches:
            vertices = np.c_[b["pos"], b["uv"], b["normal"], b["weights"]]
            assert np.isfinite(vertices).all() and np.allclose(b["weights"].sum(axis=1), 1)
            out.write(struct.pack(">III", b["material"], len(vertices), len(b["indices"])))
            out.write(np.asarray(vertices, dtype=">f4").tobytes())
            out.write(np.asarray(b["indices"], dtype=">i4").tobytes())
    summary = {"source": str(path.relative_to(ROOT)), "parts": PARTS, "height": 1.8,
               "vertices": sum(len(b["pos"]) for b in batches),
               "triangles": sum(len(b["indices"]) // 3 for b in batches),
               "materials": len(materials), "pivots": pivots.tolist(),
               "bounds": [np.concatenate([b["pos"] for b in batches]).min(axis=0).tolist(),
                          np.concatenate([b["pos"] for b in batches]).max(axis=0).tolist()]}
    (output / "model.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(id_, summary["vertices"], "vertices,", summary["triangles"], "triangles; bounds", summary["bounds"])
    return batches, materials


def main():
    convert("putin", ROOT / "pasting/putin/source/Putin.glb",
            ["Bip01_Pelvis", "Bip01_Neck1", "Bip01_R_UpperArm", "Bip01_L_UpperArm",
             "Bip01_R_Thigh", "Bip01_L_Thigh", "Bip01_R_Hand", "Bip01_L_Hand"])
    convert("boykisser", ROOT / "pasting/boykisser.glb",
            ["spine_01", "spine.004_05", "upper_arm.R", "upper_arm.L",
             "thigh.R", "thigh.L", "hand.R", "hand.L"])


if __name__ == "__main__":
    main()
