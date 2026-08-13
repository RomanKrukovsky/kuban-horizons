import { connectBlockbench } from "./blockbench_mcp_client.mjs";

const PROJECT_NAME = "kuban_genie_concept_mesh";
const PROJECT_PATH = "/Users/romanmolodyko/Documents/kuban-horizon/kuban_genie_concept_mesh.bbmodel";
const SILHOUETTE_TEXTURE = "silhouette_base";
const REQUIRED_GROUPS = [
  "body",
  "head",
  "hair",
  "arm_l",
  "arm_r",
  "hips",
  ...Array.from({ length: 10 }, (_, index) => `tail_${String(index + 1).padStart(2, "0")}`),
];
const TAIL_CENTERS = [
  [0, -2.25, 0],
  [1, -6.55, 0.8],
  [2.3, -10.75, 1.5],
  [2.7, -15.15, 1.3],
  [1.7, -19.45, 0.4],
  [0.2, -23.55, -0.6],
  [-1, -27.75, -1.2],
  [-1.2, -32.25, -0.8],
  [-0.2, -36.55, 0.1],
  [1.2, -40.65, 0.9],
];
const TAIL_SEGMENT_LENGTH = 5;

function textResult(result, toolName) {
  const textItem = result.content?.find((item) => item.type === "text");
  if (!textItem || typeof textItem.text !== "string") {
    throw new Error(`${toolName} did not return a text result`);
  }
  return textItem.text;
}

function jsonResult(result, toolName) {
  const text = textResult(result, toolName);
  try {
    return JSON.parse(text);
  } catch (error) {
    throw new Error(`${toolName} returned invalid JSON: ${error.message}`);
  }
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function vectorBetween(from, to) {
  return to.map((value, index) => value - from[index]);
}

function vectorLength(vector) {
  return Math.hypot(...vector);
}

function midpoint(from, to) {
  return from.map((value, index) => (value + to[index]) / 2);
}

function rotationFromYAxis(direction) {
  const length = vectorLength(direction);
  const upward = direction[1] < 0 ? direction.map((value) => -value / length) : direction.map((value) => value / length);
  const rotationX = Math.atan2(upward[2], upward[1]) * 180 / Math.PI;
  const rotationZ = Math.atan2(-upward[0], Math.hypot(upward[1], upward[2])) * 180 / Math.PI;
  return [rotationX, 0, rotationZ];
}

function cylinderBetween(name, from, to, diameter, sides = 16) {
  const direction = vectorBetween(from, to);
  return {
    name,
    position: midpoint(from, to),
    height: vectorLength(direction),
    diameter,
    sides,
    rotation: rotationFromYAxis(direction),
    capped: true,
  };
}

function tailRotation(index) {
  const from = TAIL_CENTERS[Math.max(0, index - 1)];
  const to = TAIL_CENTERS[Math.min(TAIL_CENTERS.length - 1, index + 1)];
  return rotationFromYAxis(vectorBetween(from, to));
}

function validateTailOverlap() {
  for (let index = 1; index < TAIL_CENTERS.length; index += 1) {
    const spacing = vectorLength(vectorBetween(TAIL_CENTERS[index - 1], TAIL_CENTERS[index]));
    const overlap = (TAIL_SEGMENT_LENGTH - spacing) / TAIL_SEGMENT_LENGTH;
    assert(overlap >= 0.08 && overlap <= 0.12, `Tail overlap ${index}/${index + 1} is ${(overlap * 100).toFixed(2)}%`);
  }
}

function collectOutlineGroups(value, groups = new Set()) {
  if (Array.isArray(value)) {
    for (const item of value) {
      collectOutlineGroups(item, groups);
    }
    return groups;
  }
  if (value && typeof value === "object") {
    if (value.type === "group" && typeof value.name === "string") {
      groups.add(value.name);
    }
    for (const child of Object.values(value)) {
      collectOutlineGroups(child, groups);
    }
  }
  return groups;
}

async function addGroup(client, name, origin) {
  await client.callTool("add_group", {
    name,
    origin,
    rotation: [0, 0, 0],
    parent: "root",
    visibility: true,
    autouv: "0",
    selected: false,
    shade: true,
  });
}

async function createMainGroups(client) {
  const origins = {
    body: [0, 22, 0],
    head: [0, 40, 0],
    hair: [0, 44, 0],
    arm_l: [-5, 33, 0],
    arm_r: [5, 33, 0],
    hips: [0, 8, 0],
  };
  for (const name of REQUIRED_GROUPS) {
    const tailIndex = name.startsWith("tail_") ? Number(name.slice(-2)) - 1 : -1;
    await addGroup(client, name, tailIndex >= 0 ? TAIL_CENTERS[tailIndex] : origins[name]);
  }
}

async function createTorsoMesh(client) {
  const meshName = "torso_silhouette";
  await client.callTool("place_mesh", {
    elements: [{
      name: meshName,
      position: [0, 22, 0],
      rotation: [0, 0, 0],
      scale: [1, 1, 1],
      vertices: [
        [-3.2, -4, -2.8],
        [3.2, -4, -2.8],
        [5.2, 4, -3.2],
        [-5.2, 4, -3.2],
        [-3.2, -4, 2.8],
        [3.2, -4, 2.8],
        [5.2, 4, 3.2],
        [-5.2, 4, 3.2],
      ],
    }],
    texture: SILHOUETTE_TEXTURE,
    group: "body",
  });

  const vertexKeys = jsonResult(await client.callTool("risky_eval", {
    code: `Object.keys(Mesh.all.find(mesh => mesh.name === ${JSON.stringify(meshName)}).vertices)`,
  }), "risky_eval");
  assert(Array.isArray(vertexKeys) && vertexKeys.length === 8, "Torso mesh must have eight vertices");

  const faces = [
    [0, 1, 2, 3],
    [4, 7, 6, 5],
    [0, 4, 5, 1],
    [3, 2, 6, 7],
    [0, 3, 7, 4],
    [1, 5, 6, 2],
  ];
  for (const face of faces) {
    await client.callTool("create_mesh_face", {
      mesh_id: meshName,
      vertices: face.map((index) => vertexKeys[index]),
      texture: SILHOUETTE_TEXTURE,
    });
  }
}

async function createBody(client) {
  await client.callTool("create_sphere", {
    elements: [{ name: "chest", position: [0, 32, 0], diameter: 12.4, sides: 20, rotation: [0, 0, 0], align_edges: true }],
    texture: SILHOUETTE_TEXTURE,
    group: "body",
  });
  await client.callTool("create_cylinder", {
    elements: [
      { name: "waist", position: [0, 22, 0], height: 8.5, diameter: 6.4, sides: 20, rotation: [0, 0, 0], capped: true },
      { name: "neck", position: [0, 39, 0], height: 2.4, diameter: 3.2, sides: 16, rotation: [0, 0, 0], capped: true },
    ],
    texture: SILHOUETTE_TEXTURE,
    group: "body",
  });
  await createTorsoMesh(client);
}

async function createHeadAndHair(client) {
  await client.callTool("create_sphere", {
    elements: [{ name: "head_base", position: [0, 43.7, 0], diameter: 8, sides: 24, rotation: [0, 0, 0], align_edges: true }],
    texture: SILHOUETTE_TEXTURE,
    group: "head",
  });
  await client.callTool("create_sphere", {
    elements: [{ name: "hair_blockout", position: [0, 43.8, 0.35], diameter: 8.4, sides: 24, rotation: [0, 0, 0], align_edges: true }],
    texture: SILHOUETTE_TEXTURE,
    group: "hair",
  });
}

async function createHips(client) {
  await client.callTool("create_sphere", {
    elements: [{ name: "hips_mass", position: [0, 12.2, 0], diameter: 12.4, sides: 20, rotation: [0, 0, 0], align_edges: true }],
    texture: SILHOUETTE_TEXTURE,
    group: "hips",
  });
  await client.callTool("create_cylinder", {
    elements: [{ name: "pelvis_to_tail", position: [0, 4, 0], height: 8, diameter: 7, sides: 20, rotation: [0, 0, 0], capped: true }],
    texture: SILHOUETTE_TEXTURE,
    group: "hips",
  });
}

async function createArm(client, side) {
  const sign = side === "l" ? -1 : 1;
  const group = `arm_${side}`;
  const shoulder = [sign * 5.6, 32.5, 0];
  const elbow = [sign * 9.4, 30.9, 0.15];
  const wrist = [sign * 12.2, 28, 0.05];
  const hand = [sign * 13, 27.2, 0];

  await client.callTool("create_sphere", {
    elements: [
      { name: `shoulder_${side}`, position: shoulder, diameter: 3.2, sides: 16, rotation: [0, 0, 0], align_edges: true },
      { name: `hand_${side}`, position: hand, diameter: 2, sides: 16, rotation: [0, 0, 0], align_edges: true },
    ],
    texture: SILHOUETTE_TEXTURE,
    group,
  });
  await client.callTool("create_cylinder", {
    elements: [
      cylinderBetween(`upper_arm_${side}`, shoulder, elbow, 2.8),
      cylinderBetween(`forearm_${side}`, elbow, wrist, 2.35),
    ],
    texture: SILHOUETTE_TEXTURE,
    group,
  });
}

async function createTail(client) {
  for (let index = 0; index < TAIL_CENTERS.length; index += 1) {
    const suffix = String(index + 1).padStart(2, "0");
    await client.callTool("create_cylinder", {
      elements: [{
        name: `tail_segment_${suffix}`,
        position: TAIL_CENTERS[index],
        height: TAIL_SEGMENT_LENGTH,
        diameter: 7 - index * 0.5,
        sides: 20,
        rotation: tailRotation(index),
        capped: true,
      }],
      texture: SILHOUETTE_TEXTURE,
      group: `tail_${suffix}`,
    });
  }
}

async function main() {
  validateTailOverlap();
  const client = await connectBlockbench();

  await client.callTool("create_project", { name: PROJECT_NAME, format: "free" });
  await client.callTool("save_checkpoint", { name: "empty genie mesh" });

  const emptyInfo = jsonResult(await client.callTool("get_project_info", {}), "get_project_info");
  assert(emptyInfo.project?.name === PROJECT_NAME, `Unexpected project name: ${String(emptyInfo.project?.name)}`);
  assert(emptyInfo.format?.id === "free", `Unexpected project format: ${String(emptyInfo.format?.id)}`);
  assert(emptyInfo.counts?.outliner_elements === 0, "New project is not empty");

  await client.callTool("create_texture", {
    name: SILHOUETTE_TEXTURE,
    width: 16,
    height: 16,
    fill_color: "#8E6AA8",
    layer_name: "Silhouette",
  });
  await createMainGroups(client);
  await createBody(client);
  await createHeadAndHair(client);
  await createHips(client);
  await createArm(client, "l");
  await createArm(client, "r");
  await createTail(client);

  const outline = jsonResult(await client.callTool("list_outline", {
    include_cubes: false,
    include_meshes: true,
  }), "list_outline");
  const outlineGroups = collectOutlineGroups(outline);
  const missingGroups = REQUIRED_GROUPS.filter((name) => !outlineGroups.has(name));
  assert(missingGroups.length === 0, `Missing outline groups: ${missingGroups.join(", ")}`);

  const projectInfo = jsonResult(await client.callTool("get_project_info", {}), "get_project_info");
  assert(projectInfo.counts?.groups === REQUIRED_GROUPS.length, `Expected ${REQUIRED_GROUPS.length} groups, got ${String(projectInfo.counts?.groups)}`);
  assert(projectInfo.counts?.meshes >= 24, `Expected at least 24 meshes, got ${String(projectInfo.counts?.meshes)}`);

  const exportFormats = jsonResult(await client.callTool("list_export_formats", {
    only_current_format: false,
  }), "list_export_formats");
  const projectCodec = exportFormats.codecs?.find((codec) => codec.id === "project")
    ?? exportFormats.codecs?.find((codec) => codec.extension === "bbmodel" && codec.has_compile);
  assert(projectCodec, "Blockbench project codec was not found");

  await client.callTool("export_model", {
    codec_id: projectCodec.id,
    path: PROJECT_PATH,
    max_content_length: 0,
  });

  console.log(JSON.stringify({
    project: projectInfo.project?.name,
    format: projectInfo.format?.id,
    counts: projectInfo.counts,
    groups: [...outlineGroups].sort(),
    codec: projectCodec.id,
    output: PROJECT_PATH,
  }, null, 2));
}

await main();
